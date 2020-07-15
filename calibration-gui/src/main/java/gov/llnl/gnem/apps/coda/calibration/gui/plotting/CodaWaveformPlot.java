/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
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
import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.VertAlignment;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickMovedState;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.VPickLine;
import llnl.gnem.core.gui.plotting.plotobject.AbstractLine;
import llnl.gnem.core.gui.plotting.plotobject.JDataRectangle;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PinnedText;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.waveform.SeriesPlot;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class CodaWaveformPlot extends SeriesPlot {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_LINE_WIDTH = 3;

    private static final Logger log = LoggerFactory.getLogger(CodaWaveformPlot.class);

    private NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private Map<VPickLine, WaveformPick> pickLineMap = new HashMap<>();

    private WaveformClient waveformClient;

    private ParameterClient paramClient;

    private ShapeMeasurementClient shapeClient;

    private PeakVelocityClient velocityClient;

    private SyntheticCoda synthetic;

    private String plotIdentifier;

    private enum PLOT_ORDERING {
        BACKGROUND(0), NOISE_BOX(1), WAVEFORM(2), NOISE_LINE(3), SHAPE_FIT(4), MODEL_FIT(5), PICKS(6);

        private int zorder;

        private PLOT_ORDERING(int zorder) {
            this.zorder = zorder;
        }

        public int getZOrder() {
            return zorder;
        }
    }

    public CodaWaveformPlot(WaveformClient waveformClient, ShapeMeasurementClient shapeClient, ParameterClient paramClient, PeakVelocityClient velocityClient, TimeSeries... seismograms) {
        super("Time (seconds from origin)", seismograms);
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
    }

    public void setWaveform(Waveform waveform) {
        setWaveform(waveform, null);
    }

    public void setWaveform(SyntheticCoda synth) {
        setWaveform(synth.getSourceWaveform(), synth);
    }

    public void setWaveform(Waveform waveform, SyntheticCoda synth) {
        this.clear();
        pickLineMap.clear();

        if (waveform != null && waveform.hasData() && waveform.getBeginTime() != null) {
            final TimeT beginTime = new TimeT(waveform.getBeginTime());
            final float[] waveformSegment = doublesToFloats(waveform.getSegment());
            final TimeSeries rawSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
            this.addSeismogram(rawSeries);

            JSubplot subplot = this.getSubplot(rawSeries);
            subplot.setYlimits(subplot.getYaxis().getMin() - 1.0, subplot.getYaxis().getMax() + 1.0);
            Station station = waveform.getStream().getStation();
            Event event = waveform.getEvent();

            double distance = EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude());
            double baz = EModel.getBAZ(station.getLatitude(), station.getLongitude(), event.getLatitude(), event.getLongitude());
            plotIdentifier = waveform.getEvent().getEventId() + "_" + waveform.getStream().getStation().getStationName() + "_" + waveform.getLowFrequency() + "_" + waveform.getHighFrequency();
            String labelText = plotIdentifier + "; Distance: " + dfmt4.format(distance) + "km BAz: " + dfmt4.format(baz) + "° ";

            PinnedText legend = createLegend(labelText);
            PlotObject legendRef = subplot.AddPlotObject(legend);

            List<WaveformPick> picks = waveform.getAssociatedPicks();
            double beginEpochTime = new TimeT(waveform.getBeginTime()).getEpochTime();
            double endEpochTime = new TimeT(waveform.getEndTime()).getEpochTime();
            if (picks != null) {
                // 1221: Plotting throws a runtime error if
                // a pick is before/after the bounds of the
                // waveform so we need to check that
                for (WaveformPick pick : picks) {
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
                    Collection<VPickLine> pickLines = this.addPick(pick.getPickName(), pickTime);
                    for (VPickLine pickLine : pickLines) {
                        pickLine.setDraggable(true);
                        if (!pickLine.getText().equalsIgnoreCase("f")) {
                            pickLine.setColor(Color.LIGHT_GRAY);
                        }
                        pickLineMap.put(pickLine, pick);
                    }
                }
            }

            shapeClient.getMeasuredShape(waveform.getId()).subscribe(shape -> {
                if (shape != null && shape.getId() != null) {
                    try {
                        TimeSeries interpolatedSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                        if (interpolatedSeries.getSamprate() > 1.0) {
                            interpolatedSeries.interpolate(1.0);
                        }
                        float[] interpolatedData = interpolatedSeries.getData();
                        float[] fitSegment = new float[interpolatedData.length];

                        double gamma = shape.getMeasuredGamma();
                        double beta = shape.getMeasuredBeta();
                        double intercept = shape.getMeasuredIntercept();

                        int timeShift = (int) (new TimeT(shape.getMeasuredTime()).subtractD(beginTime) - 0.5);
                        double sampleRate = interpolatedSeries.getSamprate();
                        for (int i = 0; i < interpolatedData.length; i++) {
                            double t = (i / sampleRate) + 1.0;
                            fitSegment[i] = (float) (intercept - gamma * Math.log10(t) + beta * (t));
                        }

                        TimeSeries fitSeries = new TimeSeries(fitSegment, interpolatedSeries.getSamprate(), interpolatedSeries.getTime());
                        subplot.AddPlotObject(createLine(timeShift, 0.0, fitSeries, Color.GRAY), PLOT_ORDERING.SHAPE_FIT.getZOrder());
                        repaint();
                    } catch (IllegalArgumentException e) {
                        log.warn(e.getMessage(), e);
                    }
                }
            });

            paramClient.getSharedFrequencyBandParametersForFrequency(new FrequencyBand(waveform.getLowFrequency(), waveform.getHighFrequency())).subscribe(params -> {
                if (params != null) {
                    try {
                        velocityClient.getNoiseForWaveform(waveform.getId()).subscribe(measurement -> {
                            if (measurement != null && measurement.getNoiseEndSecondsFromOrigin() != 0.0) {
                                int lineLength = (int) (waveform.getSegmentLength() / waveform.getSampleRate()) + 10;
                                subplot.AddPlotObject(createFixedLine(measurement.getNoiseLevel(), lineLength, Color.BLACK, PenStyle.DASH), PLOT_ORDERING.NOISE_LINE.getZOrder());
                                subplot.AddPlotObject(createFixedLine(measurement.getNoiseLevel() + params.getMinSnr(), lineLength, Color.BLACK, PenStyle.SOLID), PLOT_ORDERING.NOISE_LINE.getZOrder());
                                repaint();
                            }
                        });

                        if (synth != null) {
                            plotSynthetic(waveform, synth, beginTime, waveformSegment, subplot, event, distance, labelText, legendRef, params);
                            this.synthetic = synth;
                        } else if (this.synthetic != null && synthetic.getSourceWaveform() != null && synthetic.getSourceWaveform().getId().equals(waveform.getId())) {
                            plotSynthetic(waveform, synthetic, beginTime, waveformSegment, subplot, event, distance, labelText, legendRef, params);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn(e.getMessage(), e);
                    }
                }

                repaint();
            });
        } else {
            plotIdentifier = "";
        }
    }

    private void plotSynthetic(Waveform waveform, SyntheticCoda synth, final TimeT beginTime, final float[] waveformSegment, JSubplot subplot, Event event, double distance, String labelText,
            PlotObject legendRef, SharedFrequencyBandParameters params) {
        TimeSeries interpolatedSeries = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
        float[] synthSegment = doublesToFloats(synth.getSegment());
        TimeSeries synthSeriesBeforeEndMarker = new TimeSeries(synthSegment, synth.getSampleRate(), new TimeT(synth.getBeginTime()));
        TimeSeries synthSeriesRemaining = new TimeSeries(synthSegment, synth.getSampleRate(), new TimeT(synth.getBeginTime()));
        WaveformPick endPick = null;
        for (WaveformPick p : waveform.getAssociatedPicks()) {
            if (p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim())) {
                endPick = p;
                break;
            }
        }
        TimeT endTime;
        TimeT originTime;
        if (event != null) {
            originTime = new TimeT(event.getOriginTime());
        } else {
            originTime = new TimeT(synth.getBeginTime());
        }

        if (endPick != null && event != null) {
            endTime = new TimeT(originTime).add(endPick.getPickTimeSecFromOrigin());
        } else {
            endTime = new TimeT(synth.getEndTime());
        }

        interpolatedSeries.interpolate(synthSeriesBeforeEndMarker.getSamprate());

        double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + distance);
        if (vr == 0.0) {
            vr = 1.0;
        }
        TimeT startTime;
        TimeT trimTime = originTime.add(distance / vr);
        if (trimTime.lt(endTime)) {
            TimeSeries trimmedWaveform = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
            try {
                trimmedWaveform.cutBefore(trimTime);
                trimmedWaveform.cutAfter(trimTime.add(30.0));
                startTime = new TimeT(trimTime.getEpochTime() + trimmedWaveform.getMaxTime()[0]);
            } catch (IllegalArgumentException e) {
                startTime = trimTime;
            }

            if (startTime.lt(endTime)) {
                interpolatedSeries.cut(startTime, endTime);
                synthSeriesBeforeEndMarker.cut(startTime, endTime);
                if (synthSeriesBeforeEndMarker.getLength() > 1) {
                    TimeSeries diffSeis = interpolatedSeries.subtract(synthSeriesBeforeEndMarker);
                    int synthStartTimeShift = (int) (startTime.subtractD(beginTime) + 0.5);
                    double median = diffSeis.getMedian();

                    subplot.DeletePlotObject(legendRef);
                    subplot.AddPlotObject(createLegend(labelText + "Shift: " + dfmt4.format(median)));
                    subplot.AddPlotObject(createLine(synthStartTimeShift, median, synthSeriesBeforeEndMarker, Color.GREEN), PLOT_ORDERING.MODEL_FIT.getZOrder());

                    if (endTime.lt(synthSeriesRemaining.getEndtime())) {
                        synthSeriesRemaining.cutBefore(endTime);
                        int remainingStartTimeShift = (int) (endTime.subtractD(beginTime) + 0.5);
                        if (synthSeriesRemaining.getLength() > 1) {
                            subplot.AddPlotObject(createLine(remainingStartTimeShift, median, synthSeriesRemaining, Color.GREEN, DEFAULT_LINE_WIDTH, PenStyle.DASH),
                                                  PLOT_ORDERING.MODEL_FIT.getZOrder());
                        }
                    }
                    repaint();
                }
            }
        }
    }

    private PinnedText createLegend(String text) {
        return new PinnedText(5d, 5d, text, HorizPinEdge.RIGHT, VertPinEdge.TOP, getTitle().getFontName(), getTitle().getFontSize(), Color.black, HorizAlignment.RIGHT, VertAlignment.TOP);
    }

    private PlotObject createLine(int timeShift, double valueShift, TimeSeries timeSeries, Color lineColor) {
        return createLine(timeShift, valueShift, timeSeries, lineColor, DEFAULT_LINE_WIDTH, PenStyle.SOLID);
    }

    private PlotObject createLine(int timeShift, double valueShift, TimeSeries timeSeries, Color lineColor, int width, PenStyle style) {
        Line line = new Line(timeShift, timeSeries.getDelta(), SeriesMath.add(timeSeries.getData(), valueShift), DEFAULT_LINE_WIDTH);
        line.setPenStyle(style);
        line.setColor(lineColor);
        line.setWidth(width);
        return line;
    }

    //Only used for Waveforms
    @Override
    public AbstractLine addLine(TimeSeries seismogram, Color lineColor) {
        Line line = new Line(0.0, seismogram.getDelta(), seismogram.getData(), DEFAULT_LINE_WIDTH);
        line.setColor(lineColor);
        getSubplot(seismogram).AddPlotObject(line, PLOT_ORDERING.WAVEFORM.getZOrder());
        return line;
    }

    private PlotObject createFixedLine(double value, int length, Color lineColor, PenStyle penStyle) {
        float[] data = new float[length];
        Arrays.fill(data, (float) value);
        Line line = new Line(0, 1.0, data, 1);
        line.setColor(lineColor);
        line.setPenStyle(penStyle);
        line.setWidth(1);
        return line;
    }

    //Only used for Picks
    @Override
    protected void addPlotObject(JSubplot subplot, PlotObject object) {
        subplot.AddPlotObject(object, PLOT_ORDERING.PICKS.getZOrder());
    }

    private PlotObject createRectangle(int timeStart, int timeEnd, int heightMin, int heightMax, Color pink) {
        JDataRectangle rect = new JDataRectangle(timeStart, heightMin, timeEnd - timeStart, heightMax - heightMin);
        rect.setFillColor(pink);
        return rect;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * llnl.gnem.core.gui.waveform.WaveformPlot#handlePickMovedState(java.lang.
     * Object)T
     */
    @Override
    protected void handlePickMovedState(Object obj) {
        super.handlePickMovedState(obj);
        PickMovedState pms = (PickMovedState) obj;
        VPickLine vpl = pms.getPickLine();

        if (vpl != null) {
            try {
                WaveformPick pick = pickLineMap.get(vpl);
                if (pick != null && pick.getWaveform() != null) {
                    pick.setPickTimeSecFromOrigin((float) (vpl.getXval() - new TimeT(pick.getWaveform().getEvent().getOriginTime()).subtractD(new TimeT(pick.getWaveform().getBeginTime()))));
                    if (pick.getPickName() != null && PICK_TYPES.F.getPhase().equalsIgnoreCase(pick.getPickName().trim())) {
                        pick.getWaveform()
                            .setAssociatedPicks(pick.getWaveform()
                                                    .getAssociatedPicks()
                                                    .stream()
                                                    .filter(p -> p.getPickName() != null && !PICK_TYPES.AP.getPhase().equalsIgnoreCase(p.getPickName().trim()))
                                                    .collect(Collectors.toList()));
                    }
                    waveformClient.postWaveform(pick.getWaveform()).subscribe(this::setWaveform);
                }
            } catch (ClassCastException | JsonProcessingException e) {
                log.info("Error updating Waveform, {}", e);
            }
        }
    }

    public static float[] doublesToFloats(double[] x) {
        float[] xfloats = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            xfloats[i] = (float) x[i];
        }
        return xfloats;
    }

    public String getPlotIdentifier() {
        return plotIdentifier;
    }
}
