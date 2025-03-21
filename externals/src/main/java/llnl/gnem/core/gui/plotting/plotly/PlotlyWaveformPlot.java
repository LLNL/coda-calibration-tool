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
package llnl.gnem.core.gui.plotting.plotly;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
import llnl.gnem.core.gui.plotting.events.PlotAxisChange;
import llnl.gnem.core.gui.plotting.events.PlotShapeMove;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;
import llnl.gnem.core.waveform.seismogram.SeismicSignal;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class PlotlyWaveformPlot extends PlotlyPlot {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PlotlyWaveformPlot.class);

    private final transient List<SeismicSignal> seismograms;
    private final Map<String, Double> sharedPicks;
    private int lastEventId = -1;

    public PlotlyWaveformPlot() {
        super();
        seismograms = new ArrayList<>();
        sharedPicks = new HashMap<>();
        this.addPlotObjectObserver(this::update);
    }

    public PlotlyWaveformPlot(final SeismicSignal... seismograms) {
        this();
        for (final SeismicSignal seismogram : seismograms) {
            addSeismogram(seismogram);
        }
    }

    public void replot(final SeismicSignal... seismograms) {
        clear();
        plot(seismograms);
    }

    public void plot(final SeismicSignal... seismograms) {
        for (final SeismicSignal seismogram : seismograms) {
            addSeismogram(seismogram);
        }
    }

    public void addPickToAll(final double time) {
        addPickToAll("?", time);
    }

    public Collection<VerticalLine> addPickToAll(final String label, final double time) {
        final Collection<VerticalLine> pickLines = new ArrayList<>();
        sharedPicks.put(label, time);
        for (final SeismicSignal seismogram : seismograms) {
            final VerticalLine vpl = plotPick(seismogram, label, time);
            pickLines.add(vpl);
        }
        replot();
        return pickLines;
    }

    public void setTitle(final String title) {
        getTitle().setText(title);
    }

    public MinMax getMinMaxWithinSection(double xMin, double xMax) {
        SeismicSignal rawSeries = seismograms.get(0);
        // Get the min/max y values within the subsection of the xAxis
        final double startTime = Math.abs(seismograms.get(0).getZeroTimeOffsetSeconds());

        TimeSeries zoomedSection = new TimeSeries(rawSeries.getData(), rawSeries.getSamprate(), rawSeries.getTime());
        zoomedSection.cut(xMin + startTime, xMax + startTime);

        // The y-axis min and max are adjusted with 10% relative padding
        final MinMax yzoomRange = zoomedSection.getMinMax();
        double min = yzoomRange.getMin();
        min = min - Math.abs(yzoomRange.getRange() * 0.1);
        double max = yzoomRange.getMax();
        max = max + Math.abs(yzoomRange.getRange() * 0.1);

        return new MinMax(min, max);
    }

    protected double getTime(final SeismicSignal seismogram) {
        return seismogram.getTimeAsDouble();
    }

    protected double getValue(final SeismicSignal seismogram, final double time) {
        return seismogram.getValueAt(time);
    }

    protected void addSeismogram(final SeismicSignal seismogram, final Integer zOrder) {
        seismograms.add(seismogram);
        plotAll(zOrder);
    }

    protected void addSeismogram(final SeismicSignal seismogram) {
        addSeismogram(seismogram, null);
    }

    private void plotAll(Integer zOrder) {
        double maxLength = 0.0;
        for (final SeismicSignal seismogram : seismograms) {
            maxLength = Math.max(maxLength, seismogram.getSegmentLength());
            final Line line = new BasicLine(seismogram.getIdentifier(), seismogram.getZeroTimeOffsetSeconds(), seismogram.getDelta(), seismogram.getData(), Color.BLUE, LineStyles.SOLID, 1);
            line.setName(seismogram.getIdentifier() + "");
            if (zOrder != null) {
                line.setZindex(zOrder);
            }
            this.addPlotObject(line);
        }

        plotPicks();

        replot();
    }

    protected final Collection<SeismicSignal> getSeismograms() {
        return seismograms;
    }

    private void plotPicks() {
        for (final Entry<String, Double> phaseEntry : sharedPicks.entrySet()) {
            for (final SeismicSignal seismogram : seismograms) {
                plotPick(seismogram, phaseEntry.getKey(), phaseEntry.getValue());
            }
        }
    }

    private VerticalLine plotPick(final SeismicSignal seismogram, final String phase, double time) {
        int pickLineWidth = 3;
        boolean draggable = false;
        //Weed out NaNs and +-Inf and mark as "bad"
        if (!Double.isFinite(time)) {
            time = -100;
        }

        final Color pickColor;
        if ("f".equalsIgnoreCase(phase)) {
            pickColor = Color.rgb(255, 0, 0, 0.7);
            draggable = true;
        } else if ("cs".equalsIgnoreCase(phase) || "ucs".equalsIgnoreCase(phase)) {
            pickColor = Color.rgb(0, 0, 0, 0.7);
            draggable = true;
        } else {
            pickColor = Color.rgb(165, 165, 165, 0.7); // Light grey with transparency
        }
        final double startTime = getTime(seismogram);
        final double endTime = seismogram.getEndtime().subtractD(seismogram.getTime());
        double xValue = time - startTime;
        //If the xValue is sufficiently negative/positive we want to clamp it to the start/end time
        // just to make sure it plots and is interactable.
        double minTime = seismogram.getZeroTimeOffsetSeconds();
        if (xValue < minTime) {
            xValue = minTime;
        } else if (xValue > endTime) {
            xValue = endTime;
        }
        final VerticalLine vpl = new VerticalLine(xValue, 90, phase, pickColor, pickLineWidth, draggable, false);
        vpl.setDraggable(draggable);
        addPlotObject(vpl);
        return vpl;
    }

    @Override
    public void clear() {
        seismograms.clear();
        sharedPicks.clear();
        super.clear();
    }

    public void update(final PropertyChangeEvent observable) {
        if (observable.getNewValue() instanceof PlotShapeMove) {
            PlotShapeMove move = (PlotShapeMove) observable.getNewValue();
            //We check this just to screen out stray extra events causing extra
            //work.
            int curEventId = move.hashCode();
            synchronized (this) {
                if (lastEventId != curEventId) {
                    lastEventId = curEventId;
                    handlePickMovedState(move);
                }
            }
        } else if (observable.getNewValue() instanceof PlotAxisChange) {
            PlotAxisChange change = (PlotAxisChange) observable.getNewValue();
            int curEventId = change.hashCode();
            synchronized (this) {
                if (lastEventId != curEventId) {
                    lastEventId = curEventId;
                    handleAxisChange(change);
                }
            }
        } else {
            handleOtherChange(observable.getNewValue());
        }
    }

    protected void handlePickMovedState(PlotShapeMove move) {
        //nop
    }

    protected void handleAxisChange(PlotAxisChange state) {
        //nop
    }

    protected void handleOtherChange(Object change) {
        //nop
    }
}
