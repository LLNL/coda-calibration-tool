/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.text.NumberFormat;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import llnl.gnem.core.gui.plotting.plotly.BasicLine;
import llnl.gnem.core.gui.plotting.plotly.PlotlyWaveformPlot;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class BasicWaveformPlot extends PlotlyWaveformPlot {

    private static final long serialVersionUID = 1L;

    private static final int DEFAULT_LINE_WIDTH = 3;

    private static final double PADDING_FACTOR = 0.1;

    private static final Logger log = LoggerFactory.getLogger(BasicWaveformPlot.class);

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private final Axis xAxis;

    private final Axis yAxis;

    private MinMax minMax;

    private boolean minMaxSet;

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

    public BasicWaveformPlot(final TimeSeries... seismograms) {
        super(seismograms);
        xAxis = new BasicAxis(Axis.Type.X, "Time (seconds from origin)");
        yAxis = new BasicAxis(Axis.Type.Y, "log10(amplitude)");
        this.addAxes(xAxis, yAxis);
        this.minMax = new MinMax(0, 0);
        this.minMaxSet = false;
    }

    /*public void fitAxes(final double paddingFactor) {
        double xMax = Double.NEGATIVE_INFINITY;
        double xMin = Double.POSITIVE_INFINITY;
        double yMax = Double.NEGATIVE_INFINITY;
        double yMin = Double.POSITIVE_INFINITY;
    
        for(SeismicSignal sig : getSeismograms()) {
            if(sig.getSegmentLength() > xMax) {
                xMax = xMax + sig.getSegmentLength() * paddingFactor;
            }
            if(sig.getMax() > yMax) {
                yMax = sig.getMax();
            }
        }
    }*/

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

    public static float[] doublesToFloats(final double[] x) {
        final float[] xfloats = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            xfloats[i] = (float) x[i];
        }
        return xfloats;
    }

    public Axis getxAxis() {
        return xAxis;
    }

    public Axis getyAxis() {
        return yAxis;
    }

    private void addPlotObject(final PlotObject object, final int zOrder) {
        if (object != null) {
            object.setZindex(zOrder);
        }
        addPlotObject(object);
    }
}
