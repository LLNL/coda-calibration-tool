/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.calibration.model.domain.util.PICK_TYPES;
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.Legend;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickMovedState;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.VPickLine;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.waveform.SeriesPlot;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class CodaWaveformPlot extends SeriesPlot {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private Map<VPickLine, WaveformPick> pickLineMap = new HashMap<>();

    private WaveformClient waveformClient;

    private ParameterClient paramClient;

    private ShapeMeasurementClient shapeClient;

    public CodaWaveformPlot(String xLabel, WaveformClient waveformClient, ShapeMeasurementClient shapeClient, ParameterClient paramClient, TimeSeries... seismograms) {
        super(xLabel, seismograms);
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramClient = paramClient;
    }

    private static final long serialVersionUID = 1L;

    public void setWaveform(Waveform waveform) {
        setWaveform(waveform, null);
    }

    public void setWaveform(SyntheticCoda synth) {
        setWaveform(synth.getSourceWaveform(), synth);
    }

    public void setWaveform(Waveform waveform, SyntheticCoda synth) {
        this.clear();
        pickLineMap.clear();

        if (waveform.getSegment() != null) {
            getXaxis().setLabelText("");
            paramClient.getSharedFrequencyBandParametersForFrequency(new FrequencyBand(waveform.getLowFrequency(), waveform.getHighFrequency())).subscribe(params -> {
                shapeClient.getMeasuredShape(waveform.getId()).subscribe(shape -> {
                    TimeT beginTime = new TimeT(waveform.getBeginTime());
                    float[] waveformSegment = doublesToFloats(waveform.getSegment());
                    TimeSeries series = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                    this.addSeismogram(series);
                    JSubplot subplot = this.getSubplot(series);
                    subplot.setYlimits(subplot.getYaxis().getMin() - 1.0, subplot.getYaxis().getMax() + 1.0);
                    Legend legend = new Legend(getTitle().getFontName(), getTitle().getFontSize(), HorizPinEdge.RIGHT, VertPinEdge.TOP, 5, 5);
                    legend.addLabeledLine(waveform.getStream().getStation().getStationName() + "_" + waveform.getEvent().getEventId() + "_" + waveform.getLowFrequency() + "_"
                            + waveform.getHighFrequency(), new Line(0, series.getDelta(), series.getData(), 1));
                    subplot.AddPlotObject(legend);

                    List<WaveformPick> picks = waveform.getAssociatedPicks();
                    if (picks != null) {
                        //1221: Plotting throws a runtime error if a pick is before/after the bounds of the waveform so we need to check that
                        for (WaveformPick pick : picks) {
                            double pickTime = new TimeT(waveform.getEvent().getOriginTime()).getEpochTime() + pick.getPickTimeSecFromOrigin();
                            if (pickTime >= new TimeT(waveform.getBeginTime()).getEpochTime() && pickTime <= new TimeT(waveform.getEndTime()).getEpochTime()) {
                                Collection<VPickLine> pickLines = this.addPick(pick.getPickName(), pickTime);
                                for (VPickLine pickLine : pickLines) {
                                    pickLine.setDraggable(true);
                                    pickLineMap.put(pickLine, pick);
                                }
                            }
                        }
                    }

                    if (shape != null && shape.getId() != null) {
                        try {
                            series = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                            series.interpolate(1.0);
                            float[] fitSegment = new float[series.getData().length];

                            double gamma = shape.getMeasuredGamma();
                            double beta = shape.getMeasuredBeta();
                            double intercept = shape.getMeasuredIntercept();

                            int timeShift = (int) (new TimeT(shape.getMeasuredTime()).subtractD(beginTime) - 0.5);
                            for (int i = 0; i < series.getData().length; i++) {
                                fitSegment[i] = (float) (intercept - gamma * Math.log10(i + 1) + beta * (i + 1));
                            }

                            TimeSeries fitSeries = new TimeSeries(fitSegment, series.getSamprate(), series.getTime());
                            subplot.AddPlotObject(createLine(timeShift, 0.0, fitSeries, Color.GRAY));
                            repaint();
                        } catch (IllegalArgumentException e) {
                            log.warn(e.getMessage(), e);
                        }
                    }

                    if (synth != null && params != null) {
                        try {
                            series = new TimeSeries(waveformSegment, waveform.getSampleRate(), beginTime);
                            float[] synthSegment = doublesToFloats(synth.getSegment());
                            TimeSeries synthSeries = new TimeSeries(synthSegment, synth.getSampleRate(), new TimeT(synth.getBeginTime()));
                            WaveformPick endPick = null;
                            for (WaveformPick p : waveform.getAssociatedPicks()) {
                                if (PICK_TYPES.F.name().equals(p.getPickType())) {
                                    endPick = p;
                                    break;
                                }
                            }
                            TimeT endTime;
                            if (endPick != null && endPick.getPickTimeSecFromOrigin() > 0 && waveform.getEvent() != null) {
                                endTime = new TimeT(waveform.getEvent().getOriginTime()).add(endPick.getPickTimeSecFromOrigin());
                            } else {
                                endTime = new TimeT(synth.getEndTime());
                            }

                            series.interpolate(synthSeries.getSamprate());

                            Station station = synth.getSourceWaveform().getStream().getStation();
                            Event event = synth.getSourceWaveform().getEvent();

                            double distance = EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude());
                            double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + distance);
                            if (vr == 0.0) {
                                vr = 1.0;
                            }
                            TimeT originTime = new TimeT(event.getOriginTime());
                            TimeT startTime = originTime.add(distance / vr);

                            if (startTime.lt(endTime)) {
                                series.cut(startTime, endTime);
                                synthSeries.cut(startTime, endTime);
    
                                TimeSeries diffSeis = series.subtract(synthSeries);
    
                                int timeShift = (int) (startTime.subtractD(beginTime) - 0.5);
    
                                double median = diffSeis.getMedian();
                                double baz = EModel.getBAZ(station.getLatitude(), station.getLongitude(), event.getLatitude(), event.getLongitude());
    
                                getXaxis().setLabelText(getXaxis().getLabelText() + "Shift: " + dfmt4.format(median) + ", Distance: " + dfmt4.format(distance) + ", BAz: " + dfmt4.format(baz));
                                subplot.AddPlotObject(createLine(timeShift, median, synthSeries, Color.GREEN));
                                repaint();
                            }
                        } catch (IllegalArgumentException e) {
                            log.warn(e.getMessage(), e);
                        }
                    }
                });
            });
        }
    }

    private PlotObject createLine(int timeShift, double valueShift, TimeSeries timeSeries, Color lineColor) {

        Line line = new Line(timeShift, timeSeries.getDelta(), SeriesMath.Add(timeSeries.getData(), valueShift), 1);
        line.setColor(lineColor);
        line.setWidth(3);
        return line;
    }

    /* (non-Javadoc)
     * @see llnl.gnem.core.gui.waveform.WaveformPlot#handlePickMovedState(java.lang.Object)T
     */
    @Override
    protected void handlePickMovedState(Object obj) {
        super.handlePickMovedState(obj);
        PickMovedState pms = (PickMovedState) obj;
        VPickLine vpl = pms.getPickLine();

        if (vpl != null) {
            try {
                WaveformPick pick = pickLineMap.get(vpl);
                if (pick != null) {
                    pick.setPickTimeSecFromOrigin((float) (vpl.getXval() - new TimeT(pick.getWaveform().getEvent().getOriginTime()).subtractD(new TimeT(pick.getWaveform().getBeginTime()))));
                    waveformClient.postWaveform(pick.getWaveform()).subscribe(this::setWaveform);
                }
            } catch (ClassCastException | JsonProcessingException e) {
                log.info("Error updating Waveform, {}", e);
            }
        }
    }

    public static float[] doublesToFloats(Double[] x) {
        float[] xfloats = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            xfloats[i] = x[i].floatValue();
        }
        return xfloats;
    }
}
