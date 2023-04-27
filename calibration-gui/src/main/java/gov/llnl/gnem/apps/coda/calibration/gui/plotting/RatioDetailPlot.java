/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool.
*
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
//package gov.llnl.gnem.apps.coda.calibration.gui.plotting;
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatioPairOperator;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.Rectangle;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
import llnl.gnem.core.gui.plotting.events.PlotAxisChange;
import llnl.gnem.core.gui.plotting.events.PlotShapeMove;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import llnl.gnem.core.gui.plotting.plotly.BasicLine;
import llnl.gnem.core.gui.plotting.plotly.PlotlyWaveformPlot;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class RatioDetailPlot extends PlotlyWaveformPlot {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_LINE_WIDTH = 2;
    private static final int DRAGGABLE_LINE_WIDTH = 4;

    private static final Logger log = LoggerFactory.getLogger(RatioDetailPlot.class);
    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();
    private String plotIdentifier;

    private final Axis xAxis;

    private final Axis yAxis;

    private boolean alignPeaks = true;

    private double diffAvg;

    private final PropertyChangeSupport axisProperty = new PropertyChangeSupport(this);
    private final PropertyChangeSupport cutSegmentProperty = new PropertyChangeSupport(this);

    private PropertyChangeListener axisChangeListener = null;
    private PropertyChangeListener cutSegmentChangeListener = null;

    private SpectraRatioPairOperator ratioDetails;
    private TimeSeries numeratorSeries;
    private TimeSeries denominatorSeries;

    private final Color numeratorColor = Color.DARKBLUE;
    private final Color numeratorCutColor = Color.BLUE;
    private final Color denominatorColor = Color.DARKRED;
    private final Color denominatorCutColor = Color.RED;
    private final Color diffColor = Color.PURPLE;
    private final Color verticalLineColor1 = Color.DARKBLUE;
    private final Color verticalLineColor2 = Color.DARKRED;
    private final Color ratioValColor = Color.DARKGRAY;

    private final String NUMERATOR_START_CUT_LABEL = "Numer Start Cut";
    private final String DENOMINATOR_START_CUT_LABEL = "Denom Start Cut";
    private final String NUMERATOR_END_CUT_LABEL = "Numer End Cut";
    private final String DENOMINATOR_END_CUT_LABEL = "Denom End Cut";
    private final String DENOMINATOR_CUT_LABEL = "Denominator Cut";
    private final String NUMERATOR_CUT_LABEL = "Numerator Cut";

    private double peakOffset = 0.0;

    private double plotPadding = 0.05; // Percent of padding for top and bottom in plots

    private enum PLOT_ORDERING {
        BACKGROUND(0), NOISE_BOX(1), NUMER_WAVEFORM(2), DENOM_WAVEFORM(3), DIFF_WAVEFORM(4), PICK_LINES(5);

        private int zorder;

        private PLOT_ORDERING(final int zorder) {
            this.zorder = zorder;
        }

        public int getZOrder() {
            return zorder;
        }
    }

    public RatioDetailPlot() {
        xAxis = new BasicAxis(Axis.Type.X, "Time (seconds from origin)");
        yAxis = new BasicAxis(Axis.Type.Y, "log10(amplitude)");
        this.addAxes(xAxis, yAxis);
    }

    public RatioDetailPlot(final SpectraRatioPairOperator ratioDetails, boolean alignPeaks) {
        xAxis = new BasicAxis(Axis.Type.X, "Time (seconds from origin)");
        yAxis = new BasicAxis(Axis.Type.Y, "log10(amplitude)");
        this.addAxes(xAxis, yAxis);
        this.alignPeaks = alignPeaks;
        setRatioDetails(ratioDetails);
    }

    public void setRatioDetails(final SpectraRatioPairOperator ratioDetails) {
        this.ratioDetails = ratioDetails;
        Waveform numeratorWave = ratioDetails.getNumerWaveform();
        Waveform denominatorWave = ratioDetails.getDenomWaveform();
        this.numeratorSeries = getTimeSeriesFromWaveform(numeratorWave);
        this.denominatorSeries = getTimeSeriesFromWaveform(denominatorWave);
    }

    public SpectraRatioPairOperator getRatioDetails() {
        return this.ratioDetails;
    }

    public void plotRatio() {
        if (this.ratioDetails == null) {
            return;
        }

        clear(); // Clear previous waveforms and picks
        this.diffAvg = ratioDetails.getDiffAvg();
        Waveform numeratorWave = ratioDetails.getNumerWaveform();
        Waveform denominatorWave = ratioDetails.getDenomWaveform();

        if (numeratorWave != null && denominatorWave != null) {
            String numEventId = numeratorWave.getEvent().getEventId();
            String denEventId = denominatorWave.getEvent().getEventId();
            String stationName = ratioDetails.getStation().getStationName();
            String frequency = String.valueOf(ratioDetails.getFrequency().getLowFrequency());
            plotIdentifier = String.format("%s_over_%s-%s_%s", numEventId, denEventId, stationName, frequency);

            double offsetAmountToUse = 0.0; // Note this offset is only applied to the denominator wave
            peakOffset = ratioDetails.getDenomStartCutSec() - ratioDetails.getNumerStartCutSec();

            // If alignPeaks is true we need to move the denominator by peakOffset
            if (this.alignPeaks) {
                offsetAmountToUse = peakOffset;
            }

            // Label the peak offset
            if (peakOffset != 0.0) {
                double numerPeak = numeratorSeries.getMax();
                double denomPeak = denominatorSeries.getMax();
                double peakOffsetLabelY = (numerPeak + denomPeak) / 2;

                if (peakOffset < 0) {
                    addPlotObject(
                            createHorizontalSegment(
                                    String.format("True Time Offset: %s s", dfmt4.format(peakOffset)),
                                        peakOffsetLabelY,
                                        ratioDetails.getDenomStartCutSec(),
                                        ratioDetails.getNumerStartCutSec(),
                                        ratioValColor,
                                        LineStyles.DASH_DOT),
                                PLOT_ORDERING.PICK_LINES.getZOrder());
                } else {
                    addPlotObject(
                            createHorizontalSegment(
                                    String.format("True Time Offset: %s s", dfmt4.format(peakOffset)),
                                        peakOffsetLabelY,
                                        ratioDetails.getNumerStartCutSec(),
                                        ratioDetails.getDenomStartCutSec(),
                                        ratioValColor,
                                        LineStyles.DASH_DOT),
                                PLOT_ORDERING.PICK_LINES.getZOrder());
                }

                if (alignPeaks) {
                    addPlotObject(new VerticalLine(ratioDetails.getDenomStartCutSec(), 100, "Denominator Actual Start Cut Time", ratioValColor, 1, false, false));
                }
            }

            List<double[]> plotSegments = new ArrayList<>();
            plotSegments.add(numeratorWave.getSegment());
            plotSegments.add(denominatorWave.getSegment());
            plotSegments.add(ratioDetails.getDiffSegment());

            final MinMax minMax = getMinMaxFromSegments(plotSegments);
            double min = minMax.getMin();
            min = min - Math.abs(minMax.getRange() * plotPadding);
            double max = minMax.getMax();
            max = max + Math.abs(minMax.getRange() * plotPadding);

            // Adjust zoom level if zoomed
            if (this.isZoomed()) {
                try {
                    // Get the min/max y values within the subsection of the xAxis
                    final double xMin = this.getxAxis().getMin();
                    final double xMax = this.getxAxis().getMax();
                    final double numerStartTime = Math.abs(ratioDetails.getNumerWaveStartSec());
                    final double denomStartTime = Math.abs(ratioDetails.getDenomWaveStartSec());

                    TimeSeries zoomedNumerSection = new TimeSeries(numeratorSeries);
                    TimeSeries zoomedDenomSection = new TimeSeries(denominatorSeries);
                    zoomedNumerSection.cut(xMin + numerStartTime, xMax + numerStartTime);
                    zoomedDenomSection.cut(xMin + denomStartTime, xMax + denomStartTime);

                    List<double[]> zoomedSegments = new ArrayList<>();
                    zoomedSegments.add(floatsToDoubles(zoomedNumerSection.getData()));
                    zoomedSegments.add(floatsToDoubles(zoomedDenomSection.getData()));
                    zoomedSegments.add(ratioDetails.getDiffSegment());

                    final MinMax yZoomRange = getMinMaxFromSegments(zoomedSegments);
                    min = yZoomRange.getMin();
                    min = min - Math.abs(yZoomRange.getRange() * plotPadding);
                    max = yZoomRange.getMax();
                    max = max + Math.abs(yZoomRange.getRange() * plotPadding);

                    this.xAxis.setMin(xMin);
                    this.xAxis.setMax(xMax);
                } catch (Exception e) {
                    // Don't zoom
                    this.resetAxisLimits();
                }
            }

            this.yAxis.setMin(min);
            this.yAxis.setMax(max);

            plotWaveform(numeratorWave, 0.0, "Numerator Wave", PLOT_ORDERING.NUMER_WAVEFORM.getZOrder(), numeratorColor);
            plotWaveform(denominatorWave, offsetAmountToUse, "Denominator Wave", PLOT_ORDERING.DENOM_WAVEFORM.getZOrder(), denominatorColor);

            final float[] diffSegment = doublesToFloats(ratioDetails.getDiffSegment());
            final float[] numerSegment = doublesToFloats(ratioDetails.getNumeratorCutSegment());
            final float[] denomSegment = doublesToFloats(ratioDetails.getDenominatorCutSegment());

            final TimeT numerOriginTime = new TimeT(ratioDetails.getNumeratorEventOriginTime());
            final TimeT denomOriginTime = new TimeT(ratioDetails.getDenominatorEventOriginTime());
            final TimeT numerStartCutTime = new TimeT(ratioDetails.getNumerStartCutSec()).add(numerOriginTime);
            final TimeT denomStartCutTime = new TimeT(ratioDetails.getDenomStartCutSec()).add(denomOriginTime);

            plotTimeSeries(
                    numerSegment,
                        numeratorWave.getSampleRate(),
                        numerOriginTime,
                        numerStartCutTime,
                        "Numerator Cut",
                        PLOT_ORDERING.DIFF_WAVEFORM.getZOrder(),
                        numeratorCutColor,
                        true,
                        !this.alignPeaks,
                        false);
            plotTimeSeries(
                    denomSegment,
                        denominatorWave.getSampleRate(),
                        denomOriginTime.add(offsetAmountToUse),
                        denomStartCutTime,
                        "Denominator Cut",
                        PLOT_ORDERING.DIFF_WAVEFORM.getZOrder(),
                        denominatorCutColor,
                        false,
                        !this.alignPeaks,
                        false);
            if (this.alignPeaks) {
                plotTimeSeries(diffSegment, numeratorWave.getSampleRate(), numerOriginTime, numerStartCutTime, "Diff Wave", PLOT_ORDERING.DIFF_WAVEFORM.getZOrder(), diffColor, true, true, true);
            }
            replot();
        }
    }

    public void plotDiffRatio() {
        if (this.ratioDetails == null) {
            return;
        }

        this.clearAxes();
        this.showLegend(false);
        this.setMargin(5, 5, 5, 5);
        Waveform numeratorWave = ratioDetails.getNumerWaveform();

        if (numeratorWave != null) {
            clear(); // Clear previous waveforms and picks
            this.diffAvg = ratioDetails.getDiffAvg();

            List<double[]> plotSegments = new ArrayList<>();
            plotSegments.add(ratioDetails.getDiffSegment());

            final MinMax minMax = getMinMaxFromSegments(plotSegments);
            double min = minMax.getMin();
            min = min - Math.abs(minMax.getRange());
            double max = minMax.getMax();
            max = max + Math.abs(minMax.getRange());

            this.yAxis.setMin(min);
            this.yAxis.setMax(max);

            final float[] diffSegment = doublesToFloats(ratioDetails.getDiffSegment());
            final TimeT numerOriginTime = new TimeT(ratioDetails.getNumeratorEventOriginTime());
            final TimeT numerStartCutTime = new TimeT(ratioDetails.getNumerStartCutSec()).add(numerOriginTime);

            plotTimeSeries(diffSegment, numeratorWave.getSampleRate(), numerOriginTime, numerStartCutTime, "Diff Wave", PLOT_ORDERING.DIFF_WAVEFORM.getZOrder(), diffColor, true, true, true);
            replot();
        }
    }

    public void plotWaveform(final Waveform waveform, final double peakOffset, final String waveformName, int zOrder, final Color lineColor) {
        if (waveform != null && waveform.hasData() && waveform.getBeginTime() != null) {
            final TimeT beginTime = new TimeT(waveform.getBeginTime());
            final TimeT originTime = new TimeT(waveform.getEvent().getOriginTime()).add(peakOffset);

            final float[] waveformSegment = doublesToFloats(waveform.getSegment());
            plotTimeSeries(waveformSegment, waveform.getSampleRate(), originTime, beginTime, waveformName, zOrder, lineColor, false, false, false);
        }
    }

    private void plotTimeSeries(float[] segment, double sampleRate, TimeT originTime, TimeT beginTime, String identifier, int zOrder, Color lineColor, boolean isNumerator, boolean plotStartEndLines,
            boolean plotRatioValue) {
        final TimeSeries rawSeries = new TimeSeries(segment, sampleRate, beginTime);
        double originTimeZeroOffset = beginTime.subtractD(originTime);

        rawSeries.setIdentifier(identifier);
        rawSeries.setZeroTimeOffsetSeconds(originTimeZeroOffset);
        addLine(rawSeries, zOrder, lineColor);

        double startTime = beginTime.subtractD(originTime);
        double endTime = rawSeries.getLengthInSeconds() + startTime;

        if (plotStartEndLines && peakOffset != 0.0) {

            if (!this.alignPeaks) {
                Color rectangleColor = verticalLineColor2;
                String title = DENOMINATOR_CUT_LABEL;
                if (isNumerator) {
                    rectangleColor = verticalLineColor1;
                    title = NUMERATOR_CUT_LABEL;
                }
                final Rectangle rect = new Rectangle(startTime,
                                                     endTime,
                                                     DRAGGABLE_LINE_WIDTH,
                                                     100,
                                                     title,
                                                     rectangleColor.deriveColor(0, 1, 1, 0.9),
                                                     rectangleColor.deriveColor(0, 1, 1, 0.05),
                                                     true,
                                                     false);
                addPlotObject(rect);
            } else {
                Color verticalColor = verticalLineColor2;
                String startTitle = DENOMINATOR_START_CUT_LABEL;
                String endTitle = DENOMINATOR_END_CUT_LABEL;
                if (isNumerator) {
                    verticalColor = verticalLineColor1;
                    startTitle = NUMERATOR_START_CUT_LABEL;
                    endTitle = NUMERATOR_END_CUT_LABEL;
                }
                final VerticalLine line1 = new VerticalLine(startTime, 100, startTitle, verticalColor.deriveColor(0, 1, 1, 0.8), DRAGGABLE_LINE_WIDTH, true, false);
                final VerticalLine line2 = new VerticalLine(endTime, 100, endTitle, verticalColor.deriveColor(0, 1, 1, 0.8), DRAGGABLE_LINE_WIDTH, true, false);

                addPlotObject(line1);
                addPlotObject(line2);
            }

        }
        if (plotRatioValue) {
            addPlotObject(
                    createHorizontalSegment(String.format("Ratio Value: %s", dfmt4.format(diffAvg)), diffAvg, startTime, endTime, ratioValColor, LineStyles.DASH_DOT),
                        PLOT_ORDERING.PICK_LINES.getZOrder());
        }
    }

    public Line addLine(final TimeSeries timeSeries, final int zOrder, final Color lineColor) {
        final Line line = new BasicLine(timeSeries.getIdentifier(),
                                        timeSeries.getZeroTimeOffsetSeconds(),
                                        timeSeries.getDelta(),
                                        timeSeries.getData(),
                                        lineColor,
                                        LineStyles.SOLID,
                                        DEFAULT_LINE_WIDTH);
        addPlotObject(line, zOrder);
        return line;
    }

    private TimeSeries getTimeSeriesFromWaveform(Waveform wave) {
        final TimeT denomBeginTime = new TimeT(wave.getBeginTime());
        final float[] waveSegment = doublesToFloats(wave.getSegment());
        return new TimeSeries(waveSegment, wave.getSampleRate(), denomBeginTime);
    }

    public static float[] doublesToFloats(double[] x) {
        float[] xfloats = new float[x.length];
        IntStream.range(0, x.length).parallel().forEach(i -> xfloats[i] = (float) x[i]);
        return xfloats;
    }

    public static double[] floatsToDoubles(float[] x) {
        double[] xdoubles = new double[x.length];
        IntStream.range(0, x.length).parallel().forEach(i -> xdoubles[i] = x[i]);
        return xdoubles;
    }

    private MinMax getMinMaxFromSegments(List<double[]> segments) {
        Double min = Double.POSITIVE_INFINITY;
        Double max = Double.NEGATIVE_INFINITY;

        for (double[] segment : segments) {
            for (double value : segment) {
                if (value < min) {
                    min = value;
                }
                if (value > max) {
                    max = value;
                }
            }
        }

        return new MinMax(min, max);
    }

    /***
     * Adjust the start cut time for both numerator and denominator waves using
     * the xValue
     *
     * @param xValue
     *            The time in seconds to adjust the cuts by (5.0 would move the
     *            cuts forward 5 seconds, -3.0 would move back 3 seconds.
     */
    private void adjustStartCuts(double xValue) {

        // Calculate the start cut times
        double currentTime = ratioDetails.getNumerStartCutSec();

        double startDiff = currentTime - xValue;
        double newNumerStartCutSec = ratioDetails.getNumerStartCutSec() - startDiff;
        double newDenomStartCutSec = ratioDetails.getDenomStartCutSec() - startDiff;

        TimeT numerOriginTime = new TimeT(ratioDetails.getNumeratorEventOriginTime());
        TimeT denomOriginTime = new TimeT(ratioDetails.getDenominatorEventOriginTime());

        // If start cut times are before wave start times, set to start of wave
        int numerStartIdx = 0;
        int denomStartIdx = 0;

        if (newNumerStartCutSec > ratioDetails.getNumerEndCutSec()) {
            newNumerStartCutSec = ratioDetails.getNumerEndCutSec() - 1;
        } else if (newNumerStartCutSec < ratioDetails.getNumerWaveStartSec()) {
            newNumerStartCutSec = ratioDetails.getNumerWaveStartSec();
        } else {
            double newNumerTime = new TimeT(newNumerStartCutSec).add(numerOriginTime).getEpochTime();
            numerStartIdx = numeratorSeries.getIndexForTime(newNumerTime);
        }

        if (newDenomStartCutSec > ratioDetails.getDenomEndCutSec()) {
            newDenomStartCutSec = ratioDetails.getDenomEndCutSec() - 1;
        } else if (newDenomStartCutSec < ratioDetails.getDenomWaveStartSec()) {
            newDenomStartCutSec = ratioDetails.getDenomWaveStartSec();
        } else {
            double newDenomTime = new TimeT(newDenomStartCutSec).add(denomOriginTime).getEpochTime();
            denomStartIdx = denominatorSeries.getIndexForTime(newDenomTime);
        }

        this.ratioDetails.setNumerStartCutSec(newNumerStartCutSec);
        this.ratioDetails.setDenomStartCutSec(newDenomStartCutSec);
        this.ratioDetails.updateCutTimesAndRecalculateDiff(numerStartIdx, denomStartIdx, ratioDetails.getNumerEndCutIdx(), ratioDetails.getDenomEndCutIdx());
    }

    /***
     * Adjust the end cut time for both numerator and denominator waves using
     * the xValue
     *
     * @param xValue
     *            The time in seconds to adjust the cuts by (5.0 would move the
     *            cuts forward 5 seconds, -3.0 would move back 3 seconds.
     */
    private void adjustEndCuts(double xValue) {

        // Calculate the end cut times
        double currentTime = ratioDetails.getNumerEndCutSec();

        double endDiff = currentTime - xValue;
        double newNumerEndCutSec = ratioDetails.getNumerEndCutSec() - endDiff;
        double newDenomEndCutSec = ratioDetails.getDenomEndCutSec() - endDiff;

        TimeT numerOriginTime = new TimeT(ratioDetails.getNumeratorEventOriginTime());
        TimeT denomOriginTime = new TimeT(ratioDetails.getDenominatorEventOriginTime());

        // If end cut times are after wave end times, set to end of wave
        int numerEndIdx = numeratorSeries.getLength() - 1;
        int denomEndIdx = denominatorSeries.getLength() - 1;

        if (newNumerEndCutSec > ratioDetails.getNumerWaveEndSec()) {
            newNumerEndCutSec = ratioDetails.getNumerWaveEndSec();
        } else if (newNumerEndCutSec < ratioDetails.getNumerStartCutSec()) {
            newNumerEndCutSec = ratioDetails.getNumerStartCutSec() + 1;
        } else {
            double newNumerTime = new TimeT(newNumerEndCutSec).add(numerOriginTime).getEpochTime();
            numerEndIdx = numeratorSeries.getIndexForTime(newNumerTime);
        }

        if (newDenomEndCutSec > ratioDetails.getDenomWaveEndSec()) {
            newDenomEndCutSec = ratioDetails.getDenomWaveEndSec();
        } else if (newDenomEndCutSec < ratioDetails.getDenomStartCutSec()) {
            newDenomEndCutSec = ratioDetails.getDenomStartCutSec() + 1;
        } else {
            double newDenomTime = new TimeT(newDenomEndCutSec).add(denomOriginTime).getEpochTime();
            denomEndIdx = denominatorSeries.getIndexForTime(newDenomTime);
        }

        this.ratioDetails.setNumerEndCutSec(newNumerEndCutSec);
        this.ratioDetails.setDenomEndCutSec(newDenomEndCutSec);
        this.ratioDetails.updateCutTimesAndRecalculateDiff(ratioDetails.getNumerStartCutIdx(), ratioDetails.getDenomStartCutIdx(), numerEndIdx, denomEndIdx);
    }

    /***
     * Shift the start time of the specified cut while keeping the length the
     * same. The start time will be adjusted to keep the entire cut within the
     * waveform
     *
     * @param xValue
     *            The time that the start of the cut should be
     * @param shiftNumerator
     *            If true the numerator cut will be shifted otherwise the
     *            denominator will be shifted
     */
    private void shiftCutByTime(double xValue, boolean shiftNumerator) {
        double cutLengthSec;
        double minTime;
        double maxTime;

        // Calculate time boundary
        if (shiftNumerator) {
            cutLengthSec = ratioDetails.getNumerEndCutSec() - ratioDetails.getNumerStartCutSec();
            minTime = ratioDetails.getNumerWaveStartSec();
            maxTime = ratioDetails.getNumerWaveEndSec() - cutLengthSec;
        } else {
            cutLengthSec = ratioDetails.getDenomEndCutSec() - ratioDetails.getDenomStartCutSec();
            minTime = ratioDetails.getDenomWaveStartSec();
            maxTime = ratioDetails.getDenomWaveEndSec() - cutLengthSec;
        }

        // Set the new time and make sure it's in-bounds
        double newStartTime = xValue;
        if (newStartTime < minTime) {
            newStartTime = minTime;
        } else if (newStartTime > maxTime) {
            newStartTime = maxTime;
        }

        // Get new end cut time
        double newEndTime = newStartTime + cutLengthSec;

        // Update cut using new times
        TimeT waveOriginTime = null;
        int newStartIdx = 0;
        int newEndIdx = 0;
        if (shiftNumerator) {
            // Shift numerator cut
            ratioDetails.setNumerStartCutSec(newStartTime);
            ratioDetails.setNumerEndCutSec(newEndTime);
            waveOriginTime = new TimeT(ratioDetails.getNumeratorEventOriginTime());
            newStartIdx = numeratorSeries.getIndexForTime(new TimeT(newStartTime).add(waveOriginTime).getEpochTime());
            newEndIdx = numeratorSeries.getIndexForTime(new TimeT(newEndTime).add(waveOriginTime).getEpochTime());
            ratioDetails.updateCutTimesAndRecalculateDiff(newStartIdx, ratioDetails.getDenomStartCutIdx(), newEndIdx, ratioDetails.getDenomEndCutIdx());
        } else {
            // Shift denominator cut
            ratioDetails.setDenomStartCutSec(newStartTime);
            ratioDetails.setDenomEndCutSec(newEndTime);
            waveOriginTime = new TimeT(ratioDetails.getDenominatorEventOriginTime());
            newStartIdx = denominatorSeries.getIndexForTime(new TimeT(newStartTime).add(waveOriginTime).getEpochTime());
            newEndIdx = denominatorSeries.getIndexForTime(new TimeT(newEndTime).add(waveOriginTime).getEpochTime());
            ratioDetails.updateCutTimesAndRecalculateDiff(ratioDetails.getNumerStartCutIdx(), newStartIdx, ratioDetails.getNumerEndCutIdx(), newEndIdx);
        }
    }

    /***
     * Reset the start and end cuts for the ratio plot to be the original
     * values.
     */
    public void resetCuts() {
        TimeT numerOriginTime = new TimeT(ratioDetails.getNumeratorEventOriginTime());
        TimeT denomOriginTime = new TimeT(ratioDetails.getDenominatorEventOriginTime());

        this.ratioDetails.resetToPeakAndFMarkerCut();

        int numerStartIdx = numeratorSeries.getIndexForTime(new TimeT(ratioDetails.getNumerStartCutSec()).add(numerOriginTime).getEpochTime());
        int denomStartIdx = denominatorSeries.getIndexForTime(new TimeT(ratioDetails.getDenomStartCutSec()).add(denomOriginTime).getEpochTime());
        int numerEndIdx = numeratorSeries.getIndexForTime(new TimeT(ratioDetails.getNumerEndCutSec()).add(numerOriginTime).getEpochTime());
        int denomEndIdx = denominatorSeries.getIndexForTime(new TimeT(ratioDetails.getDenomEndCutSec()).add(denomOriginTime).getEpochTime());

        this.ratioDetails.updateCutTimesAndRecalculateDiff(numerStartIdx, denomStartIdx, numerEndIdx, denomEndIdx);
        if (this.cutSegmentChangeListener != null) {
            CompletableFuture.runAsync(() -> {
                cutSegmentProperty.firePropertyChange(new PropertyChangeEvent(this, "segment_change", null, null));
            });
        }
    }

    @Override
    protected void handlePickMovedState(PlotShapeMove move) {
        if (move.getName() != null) {
            try {
                switch (move.getName()) {
                case NUMERATOR_START_CUT_LABEL:
                    log.trace("Numerator start cut moved.");
                    adjustStartCuts(move.getX0());
                    break;
                case NUMERATOR_END_CUT_LABEL:
                    log.trace("Numerator end cut moved.");
                    adjustEndCuts(move.getX0());
                    break;
                case NUMERATOR_CUT_LABEL:
                    log.trace("Numerator cut moved.");
                    shiftCutByTime(move.getX0(), true);
                    break;
                case DENOMINATOR_CUT_LABEL:
                    log.trace("Denominator cut moved.");
                    shiftCutByTime(move.getX0(), false);
                    break;
                default:
                    log.trace("No cut moved.");
                }
            } catch (ClassCastException e) {
                log.info("Error updating Waveform, {}", move.getName(), e);
            }
        }

        if (this.cutSegmentChangeListener != null) {
            CompletableFuture.runAsync(() -> {
                cutSegmentProperty.firePropertyChange(new PropertyChangeEvent(this, "segment_change", null, move));
            });
        }
    }

    public void setCutSegmentChangeListener(PropertyChangeListener cutSegmentChange) {
        // Remove existing listener before adding another one
        if (this.cutSegmentChangeListener != null) {
            this.cutSegmentProperty.removePropertyChangeListener("segment_change", this.cutSegmentChangeListener);
        }

        this.cutSegmentChangeListener = cutSegmentChange;
        this.cutSegmentProperty.addPropertyChangeListener("segment_change", this.cutSegmentChangeListener);
    }

    @Override
    protected void handleAxisChange(PlotAxisChange change) {
        if (this.axisChangeListener != null) {
            CompletableFuture.runAsync(() -> {
                axisProperty.firePropertyChange(new PropertyChangeEvent(this, "axis_change", null, change));
            });
        }

        if (change.isReset()) {
            this.resetAxisLimits();
            return;
        }

        setAxisLimits(change.getAxisLimits().getFirst(), change.getAxisLimits().getSecond());
    }

    public void setAxisChangeListener(PropertyChangeListener axisChange) {
        // Remove existing listener before adding another one
        if (this.axisChangeListener != null) {
            this.axisProperty.removePropertyChangeListener("axis_change", this.axisChangeListener);
        }

        this.axisChangeListener = axisChange;
        this.axisProperty.addPropertyChangeListener("axis_change", this.axisChangeListener);
    }

    public void resetAxisLimits() {
        this.setAxisLimits(new AxisLimits(Axis.Type.X, 0.0, 0.0), new AxisLimits(Axis.Type.Y, 0.0, 0.0));
    }

    public boolean isZoomed() {
        return this.xAxis.getMin() != this.xAxis.getMax();
    }

    private Line createHorizontalSegment(final String label, final double yValue, final double xStart, final double xEnd, final Color lineColor, final LineStyles lineStyle) {
        final float[] data = new float[2];
        data[0] = (float) yValue;
        data[1] = (float) yValue;
        double length = xEnd - xStart;
        return new BasicLine(label, xStart, length, data, lineColor, lineStyle, 2);
    }

    public String getPlotIdentifier() {
        return plotIdentifier;
    }

    public Axis getxAxis() {
        return xAxis;
    }

    public Axis getyAxis() {
        return yAxis;
    }

    public boolean isAlignPeaks() {
        return alignPeaks;
    }

    public void setAlignPeaks(boolean alignPeaks) {
        this.alignPeaks = alignPeaks;
    }

    private void addPlotObject(final PlotObject object, final int zOrder) {
        if (object != null) {
            object.setZindex(zOrder);
        }
        addPlotObject(object);
    }
}
