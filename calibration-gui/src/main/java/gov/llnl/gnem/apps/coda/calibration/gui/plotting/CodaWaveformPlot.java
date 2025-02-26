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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.common.gui.data.client.DistanceCalculator;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import javafx.application.Platform;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
import llnl.gnem.core.gui.plotting.events.PlotAxisChange;
import llnl.gnem.core.gui.plotting.events.PlotShapeMove;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import llnl.gnem.core.gui.plotting.plotly.BasicLine;
import llnl.gnem.core.gui.plotting.plotly.PlotlyWaveformPlot;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class CodaWaveformPlot extends PlotlyWaveformPlot {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_LINE_WIDTH = 3;

    private static final Logger log = LoggerFactory.getLogger(CodaWaveformPlot.class);

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private final Map<String, WaveformPick> pickLineMap = new HashMap<>();

    private final WaveformClient waveformClient;

    private final ParameterClient paramClient;

    private final ShapeMeasurementClient shapeClient;

    private final PeakVelocityClient velocityClient;

    private SyntheticCoda synthetic;

    private String plotIdentifier;

    private final Axis xAxis;

    private final Axis yAxis;

    private VerticalLine groupVelocityLineStart;

    private VerticalLine groupVelocityLineEnd;

    private VerticalLine minWindowLine;

    private VerticalLine maxWindowLine;

    private BooleanSupplier showGroupVelocity;

    private BooleanSupplier showWindowLines;

    private final PropertyChangeSupport axisProperty = new PropertyChangeSupport(this);

    private PropertyChangeListener axisChangeListener = null;

    private double plotPadding = 0.05; // Percent of padding for top and bottom in plots

    private BooleanSupplier showCodaStartLine;

    private DistanceCalculator distanceCalc;

    private enum PLOT_ORDERING {
        BACKGROUND(0), NOISE_BOX(1), WAVEFORM(2), NOISE_LINE(3), SHAPE_FIT(4), MODEL_FIT(5), PICKS(6);

        private int zorder;

        private PLOT_ORDERING(final int zorder) {
            this.zorder = zorder;
        }

        public int getZOrder() {
            return zorder;
        }
    }

    public CodaWaveformPlot(final WaveformClient waveformClient, final ShapeMeasurementClient shapeClient, final ParameterClient paramClient, final PeakVelocityClient velocityClient,
            BooleanSupplier showGroupVelocity, BooleanSupplier showWindowLines, BooleanSupplier showCodaStartLine, DistanceCalculator distanceCalc, final TimeSeries... seismograms) {
        super(seismograms);

        xAxis = new BasicAxis(Axis.Type.X, "Time (seconds from origin)");
        yAxis = new BasicAxis(Axis.Type.Y, "log10(amplitude)");
        this.addAxes(xAxis, yAxis);
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
        this.showGroupVelocity = showGroupVelocity;
        this.showWindowLines = showWindowLines;
        this.showCodaStartLine = showCodaStartLine;
        this.distanceCalc = distanceCalc;
    }

    public void setGroupVelocityVisbility() {
        if (groupVelocityLineStart != null && groupVelocityLineEnd != null) {
            final String script = "setGroupVelocityVisibility(" + showGroupVelocity.getAsBoolean() + ");";
            plotData.setShowGroupVelocity(showGroupVelocity.getAsBoolean());
            if (plotData.getPlotReady().get()) {
                Platform.runLater(() -> {
                    try {
                        engine.executeScript(script);
                    } catch (final Exception e) {
                        log.debug(e.getLocalizedMessage());
                    }
                });
            }
        }
    }

    public void setWindowLineVisbility() {
        if (minWindowLine != null) {
            final String script = "setWindowLineVisibility(" + showWindowLines.getAsBoolean() + ");";
            plotData.setShowWindowLines(showWindowLines.getAsBoolean());
            if (plotData.getPlotReady().get()) {
                Platform.runLater(() -> {
                    try {
                        engine.executeScript(script);
                    } catch (final Exception e) {
                        log.debug(e.getLocalizedMessage());
                    }
                });
            }
        }
    }

    public void setCodaStartLineVisbility() {
        final String script = "setCodaStartLineVisibility(" + showCodaStartLine.getAsBoolean() + ");";
        plotData.setShowCodaStartLine(showCodaStartLine.getAsBoolean());
        if (plotData.getPlotReady().get()) {
            Platform.runLater(() -> {
                try {
                    engine.executeScript(script);
                } catch (final Exception e) {
                    log.debug(e.getLocalizedMessage());
                }
            });
        }
    }

    public void setWaveform(final Waveform waveform) {
        setWaveform(waveform, null);
    }

    public void setWaveform(final SyntheticCoda synth) {
        setWaveform(synth.getSourceWaveform(), synth);
    }

    public void setWaveform(final Waveform waveform, final SyntheticCoda synth) {
        this.clear();
        pickLineMap.clear();

        if (waveform != null && waveform.hasData() && waveform.getBeginTime() != null) {

            final Station station = waveform.getStream().getStation();
            final Event event = waveform.getEvent();
            final TimeT beginTime = new TimeT(waveform.getBeginTime());

            double originTimeZeroOffset = beginTime.subtractD(new TimeT(event.getOriginTime()));

            final float[] waveformSegment = doublesToFloats(waveform.getSegment());
            final TimeSeries rawSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
            rawSeries.setIdentifier("Waveform");
            rawSeries.setZeroTimeOffsetSeconds(originTimeZeroOffset);
            this.addSeismogram(rawSeries, PLOT_ORDERING.WAVEFORM.getZOrder());

            final MinMax minmax = rawSeries.getMinMax();
            double min = minmax.getMin();
            min = min - Math.abs(minmax.getRange() * plotPadding);
            double max = minmax.getMax();
            max = max + Math.abs(minmax.getRange() * plotPadding);

            // Adjust zoom level if zoomed
            if (this.isZoomed()) {
                try {
                    // Get the min/max y values within the subsection of the xAxis
                    final double xMin = this.getxAxis().getMin();
                    final double xMax = this.getxAxis().getMax();
                    final double startTime = Math.abs(rawSeries.getZeroTimeOffsetSeconds());

                    TimeSeries zoomedSection = new TimeSeries(rawSeries);
                    zoomedSection.cut(xMin + startTime, xMax + startTime);

                    // The y-axis min and max are adjusted with 10% relative padding
                    final MinMax yzoomRange = zoomedSection.getMinMax();
                    min = yzoomRange.getMin();
                    min = min - Math.abs(yzoomRange.getRange() * plotPadding);
                    max = yzoomRange.getMax();
                    max = max + Math.abs(yzoomRange.getRange() * plotPadding);

                    xAxis.setMin(xMin);
                    xAxis.setMax(xMax);
                } catch (Exception e) {
                    // Don't zoom
                    this.resetAxisLimits();
                }
            }

            yAxis.setMin(min);
            yAxis.setMax(max);

            final double distance = distanceCalc.getDistanceFunc().apply(DistanceCalculator.getEventCoord(event), DistanceCalculator.getStationCoord(station));

            final double baz = EModel.getBAZ(station.getLatitude(), station.getLongitude(), event.getLatitude(), event.getLongitude());
            plotIdentifier = waveform.getEvent().getEventId() + "_" + waveform.getStream().getStation().getStationName() + "_" + waveform.getLowFrequency() + "_" + waveform.getHighFrequency();
            final String labelText = plotIdentifier + "; Distance: " + dfmt4.format(distance) + "km; BAz(deg): " + dfmt4.format(baz);
            setTitle(labelText);

            setGroupVelocityLines(distance); // Adds group velocity lines to the appropriate location based on distance

            setCodaStartLineVisbility();

            final List<WaveformPick> picks = waveform.getAssociatedPicks();
            final double beginEpochTime = new TimeT(waveform.getBeginTime()).getEpochTime();
            final double endEpochTime = new TimeT(waveform.getEndTime()).getEpochTime();
            if (picks != null) {
                // 1221: Plotting throws a runtime error if
                // a pick is before/after the bounds of the
                // waveform so we need to check that
                for (final WaveformPick pick : picks) {
                    double pickTime;
                    //"'Bad' pick, plot it at begin time
                    if (pick.getPickTimeSecFromOrigin() < 0) {
                        pickTime = beginEpochTime;
                    } else {
                        pickTime = new TimeT(waveform.getEvent().getOriginTime()).getEpochTime() + pick.getPickTimeSecFromOrigin();
                    }
                    if (pickTime < beginEpochTime) {
                        pickTime = beginEpochTime + 5;
                    }
                    if (pickTime >= endEpochTime) {
                        pickTime = endEpochTime;
                    }
                    pickTime = pickTime + originTimeZeroOffset;
                    addPickToAll(pick.getPickName(), pickTime);
                    pickLineMap.put(pick.getPickName(), pick);
                }
                this.replot();
            }

            shapeClient.getMeasuredShape(waveform.getId()).subscribe(shape -> {
                if (shape != null && shape.getId() != null) {
                    try {
                        final TimeSeries interpolatedSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                        interpolatedSeries.cut(beginTime, new TimeT(waveform.getEndTime()).add(originTimeZeroOffset));
                        if (interpolatedSeries.getSamprate() > 1.0) {
                            interpolatedSeries.interpolate(1.0);
                        }
                        double minWaveformValue = interpolatedSeries.getMin();
                        minWaveformValue = minWaveformValue - Math.abs(minWaveformValue * .1);

                        final float[] interpolatedData = interpolatedSeries.getData();
                        final float[] fitSegment = new float[interpolatedData.length];

                        final double gamma = shape.getMeasuredGamma();
                        final double beta = shape.getMeasuredBeta();
                        final double intercept = shape.getMeasuredIntercept();

                        final int timeShift = (int) (new TimeT(shape.getMeasuredTime()).subtractD(beginTime) - 0.5);
                        final double sampleRate = interpolatedSeries.getSamprate();
                        int cutIndex = 0;
                        for (int i = 0; i < interpolatedData.length; i++) {
                            final double t = (i / sampleRate) + 1.0;
                            fitSegment[i] = (float) (intercept - gamma * Math.log10(t) + beta * (t));
                            cutIndex = i;
                            if (fitSegment[i] < minWaveformValue) {
                                break;
                            }
                        }

                        final TimeSeries fitSeries = new TimeSeries(fitSegment, interpolatedSeries.getSamprate(), interpolatedSeries.getTime().add(timeShift));
                        if (cutIndex < interpolatedData.length) {
                            fitSeries.cut(0, cutIndex);
                        }
                        fitSeries.cutAfter(new TimeT(waveform.getEndTime()));
                        fitSeries.setIdentifier("Fit");
                        fitSeries.setZeroTimeOffsetSeconds(originTimeZeroOffset);
                        addPlotObject(createLine(timeShift, 0.0, fitSeries, Color.GRAY), PLOT_ORDERING.SHAPE_FIT.getZOrder());
                        this.replot();
                    } catch (final IllegalArgumentException e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            });

            paramClient.getSharedFrequencyBandParametersForFrequency(new FrequencyBand(waveform.getLowFrequency(), waveform.getHighFrequency())).subscribe(params -> {
                if (params != null) {
                    try {
                        velocityClient.getNoiseForWaveform(waveform.getId()).subscribe(measurement -> {
                            if (measurement != null && measurement.getNoiseEndSecondsFromOrigin() != 0.0) {
                                final int lineLength = (int) (waveform.getSegmentLength() / waveform.getSampleRate()) + 1;
                                final int lineStart = (int) (beginTime.subtractD(new TimeT(event.getOriginTime())));
                                addPlotObject(createFixedLine("Noise level", measurement.getNoiseLevel(), lineStart, lineLength, Color.BLACK, LineStyles.DASH), PLOT_ORDERING.NOISE_LINE.getZOrder());
                                addPlotObject(
                                        createFixedLine("SNR cutoff", measurement.getNoiseLevel() + params.getMinSnr(), lineStart, lineLength, Color.BLACK, LineStyles.SOLID),
                                            PLOT_ORDERING.NOISE_LINE.getZOrder());
                                this.replot();
                            }
                        });

                        if (synth != null) {
                            plotSynthetic(waveform, synth, beginTime, waveformSegment, event, distance, labelText, params);
                            this.synthetic = synth;
                        } else if (this.synthetic != null && synthetic.getSourceWaveform() != null && synthetic.getSourceWaveform().getId().equals(waveform.getId())) {
                            synthetic.setSourceWaveform(waveform);
                            plotSynthetic(waveform, synthetic, beginTime, waveformSegment, event, distance, labelText, params);
                        }

                        // Add max window line
                        setWindowLines(params, waveform, distance, originTimeZeroOffset);

                        this.replot();
                    } catch (final IllegalArgumentException e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            });
        } else {
            plotIdentifier = "";
        }
        this.replot();
    }

    private double calcWindowTime(final double time, final SharedFrequencyBandParameters params, final Waveform waveform, final double distance) {
        return time + distance / (params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + distance));
    }

    private void setWindowLines(final SharedFrequencyBandParameters params, final Waveform waveform, final double distance, final double originTimeOffset) {
        if (params != null) {
            double maxWindowTime = calcWindowTime(params.getMaxLength(), params, waveform, distance);
            double minWindowTime = calcWindowTime(params.getMinLength(), params, waveform, distance);
            double plotLength = waveform.getData().size() / waveform.getSampleRate() + originTimeOffset;
            if (maxWindowTime != 0 && maxWindowTime < plotLength) {
                maxWindowLine = new VerticalLine(maxWindowTime, 90, "Max");
                maxWindowLine.setFillColor(Color.rgb(128, 128, 128, 0.7)); // Dark grey
                addPlotObject(maxWindowLine, PLOT_ORDERING.PICKS.getZOrder());
            }
            minWindowLine = new VerticalLine(minWindowTime, 90, "Min");
            minWindowLine.setFillColor(Color.rgb(128, 128, 128, 0.7)); // Dark grey
            addPlotObject(minWindowLine, PLOT_ORDERING.PICKS.getZOrder());
            setWindowLineVisbility();
        }
    }

    private void setGroupVelocityLines(double distance) {
        paramClient.getVelocityConfiguration().doOnSuccess(veloConfig -> {
            if (veloConfig != null) {
                double critDistance = veloConfig.getDistanceThresholdInKm();
                double startGV1Lt = veloConfig.getGroupVelocity1InKmsLtDistance();
                double endGV2Lt = veloConfig.getGroupVelocity2InKmsLtDistance();
                double startGV1Gt = veloConfig.getGroupVelocity1InKmsGtDistance();
                double endGV2Gt = veloConfig.getGroupVelocity2InKmsGtDistance();

                double startTime = (distance / startGV1Gt);
                double endTime = (distance / endGV2Gt);
                if (distance < critDistance) {
                    startTime = (distance / startGV1Lt);
                    endTime = (distance / endGV2Lt);
                }

                groupVelocityLineStart = new VerticalLine(startTime, 90, "Start");
                groupVelocityLineEnd = new VerticalLine(endTime, 90, "End");

                groupVelocityLineStart.setFillColor(Color.rgb(255, 165, 0, 0.7)); // Orange
                groupVelocityLineEnd.setFillColor(Color.rgb(255, 165, 0, 0.7)); // Orange

                addPlotObject(groupVelocityLineStart, PLOT_ORDERING.PICKS.getZOrder());
                addPlotObject(groupVelocityLineEnd, PLOT_ORDERING.PICKS.getZOrder());

                setGroupVelocityVisbility();
            }
        }).subscribe();
    }

    private void plotSynthetic(final Waveform waveform, final SyntheticCoda synth, final TimeT beginTime, final float[] waveformSegment, final Event event, final double distance,
            final String labelText, final SharedFrequencyBandParameters params) {
        double originTimeZeroOffset = 0;

        TimeT originTime;
        if (event != null) {
            originTimeZeroOffset = beginTime.subtractD(new TimeT(event.getOriginTime()));
            originTime = new TimeT(event.getOriginTime());
        } else {
            originTime = new TimeT(synth.getBeginTime());
        }

        final TimeSeries interpolatedSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
        double minWaveformValue = interpolatedSeries.getMin();
        minWaveformValue = minWaveformValue - Math.abs(minWaveformValue * .1);

        float[] synthSegment = doublesToFloats(synth.getSegment());

        TimeT startTime = new TimeT(synth.getBeginTime());

        final TimeSeries synthSeriesBeforeStartMarker = new TimeSeries(synthSegment, synth.getSampleRate(), startTime);
        synthSeriesBeforeStartMarker.setIdentifier("Synthetic");
        synthSeriesBeforeStartMarker.setZeroTimeOffsetSeconds(originTimeZeroOffset);

        final TimeSeries synthSeriesBeforeEndMarker = new TimeSeries(synthSegment, synth.getSampleRate(), startTime);
        synthSeriesBeforeEndMarker.setIdentifier("Synthetic");
        synthSeriesBeforeEndMarker.setZeroTimeOffsetSeconds(originTimeZeroOffset);

        final TimeSeries synthSeriesRemaining = new TimeSeries(synthSegment, synth.getSampleRate(), startTime);
        synthSeriesRemaining.setIdentifier("Synthetic");
        synthSeriesRemaining.setZeroTimeOffsetSeconds(originTimeZeroOffset);

        WaveformPick endPick = null;
        for (final WaveformPick p : waveform.getAssociatedPicks()) {
            if (p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim())) {
                endPick = p;
                break;
            }
        }
        TimeT endTime;

        if (endPick != null && event != null) {
            endTime = new TimeT(originTime).add(endPick.getPickTimeSecFromOrigin());
        } else {
            endTime = new TimeT(synth.getEndTime());
        }

        interpolatedSeries.interpolate(synthSeriesBeforeEndMarker.getSamprate());

        double deltaOriginEnd = new TimeT(endTime).subtractD(originTime);
        double maxWindowTime = calcWindowTime(params.getMaxLength(), params, waveform, distance);
        if (deltaOriginEnd > maxWindowTime) {
            endTime = endTime.subtract(deltaOriginEnd - maxWindowTime);
        }

        if (startTime.lt(endTime)) {
            interpolatedSeries.cut(startTime, endTime);

            if (synth.getSourceWaveform().getUserStartTime() != null) {
                TimeT codaStartTime = new TimeT(synth.getSourceWaveform().getUserStartTime());
                if (startTime.le(codaStartTime)) {
                    synthSeriesBeforeStartMarker.cut(startTime, codaStartTime);
                    startTime = codaStartTime;
                } else {
                    synthSeriesBeforeStartMarker.setData(new float[0]);
                }
            } else {
                synthSeriesBeforeStartMarker.setData(new float[0]);
            }

            try {
                if (startTime.lt(endTime)) {
                    synthSeriesBeforeEndMarker.cut(startTime, endTime);
                    if (synthSeriesBeforeEndMarker.getLength() > 1) {
                        final TimeSeries diffSeis = interpolatedSeries.subtract(synthSeriesBeforeEndMarker);
                        final int synthStartTimeShift = (int) (startTime.subtractD(beginTime) + 0.5);
                        final double median = diffSeis.getMedian();

                        float[] cutSegment = synthSeriesBeforeEndMarker.getData();
                        for (int i = 0; i < cutSegment.length; i++) {
                            if (i > 0 && cutSegment[i] + median < minWaveformValue) {
                                synthSeriesBeforeEndMarker.cut(0, i);
                                break;
                            }
                        }

                        if (synthSeriesBeforeStartMarker.getLength() > 1) {
                            addPlotObject(
                                    createLine(
                                            (int) (new TimeT(synthSeriesBeforeStartMarker.getTime()).subtractD(beginTime) + 0.5),
                                                median,
                                                synthSeriesBeforeStartMarker,
                                                Color.GREEN,
                                                DEFAULT_LINE_WIDTH,
                                                LineStyles.DOT),
                                        PLOT_ORDERING.MODEL_FIT.getZOrder());
                        }

                        if (synthSeriesBeforeEndMarker.getLength() > 1) {
                            addPlotObject(createLine(synthStartTimeShift, median, synthSeriesBeforeEndMarker, Color.GREEN), PLOT_ORDERING.MODEL_FIT.getZOrder());

                            if (endTime.lt(synthSeriesRemaining.getEndtime())) {
                                synthSeriesRemaining.cutBefore(endTime);
                                final int remainingStartTimeShift = (int) (endTime.subtractD(beginTime) + 0.5);
                                cutSegment = synthSeriesRemaining.getData();
                                for (int i = 0; i < cutSegment.length; i++) {
                                    if (i > 0 && cutSegment[i] + median < minWaveformValue) {
                                        synthSeriesRemaining.cut(0, i);
                                        break;
                                    }
                                }
                                if (synthSeriesRemaining.getLength() > 1) {
                                    addPlotObject(
                                            createLine(remainingStartTimeShift, median, synthSeriesRemaining, Color.GREEN, DEFAULT_LINE_WIDTH, LineStyles.DASH),
                                                PLOT_ORDERING.MODEL_FIT.getZOrder());
                                }
                            }
                        }
                    }
                }
            } catch (IllegalArgumentException ex) {
                log.debug("Unable to cut starting synthetic for start pick {}.", ex);
            }

        }
    }

    private Line createLine(final double timeShift, final double valueShift, final TimeSeries timeSeries, final Color lineColor) {
        return createLine(timeShift, valueShift, timeSeries, lineColor, DEFAULT_LINE_WIDTH, LineStyles.SOLID);
    }

    private Line createLine(final double timeShift, final double valueShift, final TimeSeries seismogram, final Color lineColor, final int width, final LineStyles style) {
        return new BasicLine(seismogram.getIdentifier(),
                             timeShift + seismogram.getZeroTimeOffsetSeconds(),
                             seismogram.getDelta(),
                             SeriesMath.add(seismogram.getData(), valueShift),
                             lineColor,
                             style,
                             width);
    }

    public Line addLine(final TimeSeries seismogram, final Color lineColor) {
        final Line line = new BasicLine(seismogram.getIdentifier(),
                                        seismogram.getZeroTimeOffsetSeconds(),
                                        seismogram.getDelta(),
                                        seismogram.getData(),
                                        lineColor,
                                        LineStyles.SOLID,
                                        DEFAULT_LINE_WIDTH);
        addPlotObject(line, PLOT_ORDERING.WAVEFORM.getZOrder());
        return line;
    }

    private Line createFixedLine(final String label, final double value, final int start, final int length, final Color lineColor, final LineStyles lineStyle) {
        final float[] data = new float[length];
        Arrays.fill(data, (float) value);
        return new BasicLine(label, start, 1.0, data, lineColor, lineStyle, 1);
    }

    @Override
    protected void handlePickMovedState(PlotShapeMove move) {
        if (move.getName() != null) {
            try {
                final WaveformPick pick = pickLineMap.get(move.getName());
                if (pick != null && pick.getWaveform() != null) {
                    pick.setPickTimeSecFromOrigin(move.getX0());
                    if (pick.getPickName() != null && PICK_TYPES.F.getPhase().equalsIgnoreCase(pick.getPickName().trim())) {
                        pick.getWaveform()
                            .setAssociatedPicks(
                                    pick.getWaveform()
                                        .getAssociatedPicks()
                                        .stream()
                                        .filter(p -> p.getPickName() != null && !PICK_TYPES.AP.getPhase().equalsIgnoreCase(p.getPickName().trim()))
                                        .collect(Collectors.toList()));
                        waveformClient.postWaveform(pick.getWaveform()).subscribe(this::setWaveform);
                    } else if (pick.getPickName() != null && PICK_TYPES.CS.getPhase().equalsIgnoreCase(pick.getPickName().trim())) {
                        List<WaveformPick> newPicks = pick.getWaveform().getAssociatedPicks().stream().map(p -> {
                            if (p.getPickName() != null && PICK_TYPES.CS.getPhase().equalsIgnoreCase(p.getPickName().trim())) {
                                Date startTime = new Date();
                                startTime.setTime((long) (p.getWaveform().getEvent().getOriginTime().getTime() + (p.getPickTimeSecFromOrigin() * 1000)));
                                p.setPickName(PICK_TYPES.UCS.getPhase());
                                p.setPickType(PICK_TYPES.UCS.name());
                                p.getWaveform().setUserStartTime(startTime);
                                p.setId(null);
                            }
                            return p;
                        }).collect(Collectors.toList());
                        pick.getWaveform().setAssociatedPicks(newPicks);
                        waveformClient.postWaveform(pick.getWaveform()).subscribe(this::setWaveform);
                    } else if (pick.getPickName() != null && PICK_TYPES.UCS.getPhase().equalsIgnoreCase(pick.getPickName().trim())) {
                        Date startTime = new Date();
                        startTime.setTime((long) (pick.getWaveform().getEvent().getOriginTime().getTime() + (pick.getPickTimeSecFromOrigin() * 1000)));
                        pick.getWaveform().setUserStartTime(startTime);
                        waveformClient.postWaveform(pick.getWaveform()).subscribe(this::setWaveform);
                    }
                }
            } catch (ClassCastException | JsonProcessingException e) {
                log.info("Error updating Waveform, {}", move.getName(), e);
            }
        }
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

    public static float[] doublesToFloats(final double[] x) {
        final float[] xfloats = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            xfloats[i] = (float) x[i];
        }
        return xfloats;
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

    public void resetAxisLimits() {
        this.setAxisLimits(new AxisLimits(Axis.Type.X, 0.0, 0.0), new AxisLimits(Axis.Type.Y, 0.0, 0.0));
    }

    public boolean isZoomed() {
        return this.xAxis.getMin() != this.xAxis.getMax();
    }

    private void addPlotObject(final PlotObject object, final int zOrder) {
        if (object != null) {
            object.setZindex(zOrder);
        }
        addPlotObject(object);
    }

}
