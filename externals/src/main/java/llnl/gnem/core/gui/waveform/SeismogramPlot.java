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
package llnl.gnem.core.gui.waveform;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.Legend;
import llnl.gnem.core.gui.plotting.MouseMode;
import llnl.gnem.core.gui.plotting.PickCreationInfo;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.ZoomType;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickTextPosition;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.VPickLine;
import llnl.gnem.core.gui.plotting.plotobject.AbstractLine;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.waveform.seismogram.SeismicSignal;
import llnl.gnem.core.waveform.seismogram.TimeSeries.SeriesListener;

/**
 *
 * @author addair1
 * @param <S>
 */
public abstract class SeismogramPlot<S extends SeismicSignal> extends WaveformPlot implements SeriesListener {

    private static final long serialVersionUID = 1L;
    private List<S> seismograms;
    private Map<S, JSubplot> subplots;
    private final Map<String, Double> picks;
    private DisplayMode mode;
    private SecureRandom random;
    private int alpha;
    private boolean showLegend;
    private final List<Color> primaryColors;

    private enum DisplayMode {

        SEPARATE, OVERLAID
    }

    public SeismogramPlot(String xLabel) {
        super(MouseMode.SELECT_ZOOM, xLabel);
        seismograms = new ArrayList<>();
        subplots = new HashMap<>();
        picks = new HashMap<>();
        mode = DisplayMode.SEPARATE;
        random = new SecureRandom();
        alpha = 255;
        showLegend = false;
        addKeyListener(new PickModeListener());
        setZoomType(ZoomType.ZOOM_ALL);

        Color[] predefined = { new Color(0x4169e1), Color.red, Color.green };
        primaryColors = Arrays.asList(predefined);
    }

    public SeismogramPlot(String xLabel, S... seismograms) {
        this(xLabel);
        for (S seismogram : seismograms) {
            addSeismogram(seismogram);
        }
    }

    public void replot(S... seismograms) {
        clear();
        plot(seismograms);
    }

    public void plot(S... seismograms) {
        for (S seismogram : seismograms) {
            addSeismogram(seismogram);
        }
    }

    public void showLegend(boolean visible) {
        showLegend = visible;
        plotAll();
    }

    public void overlay(double opacity) {
        alpha = (int) (255 * opacity);
        overlay();
    }

    public void overlay() {
        mode = DisplayMode.OVERLAID;
        plotAll();
    }

    public void separate() {
        mode = DisplayMode.SEPARATE;
        plotAll();
    }

    public void addPick(double time) {
        addPick("?", time);
    }

    public Collection<VPickLine> addPick(String phase, double time) {
        Collection<VPickLine> pickLines = new ArrayList<>();
        picks.put(phase, time);
        for (S seismogram : seismograms) {
            VPickLine vpl = plotPick(seismogram, phase, time);
            pickLines.add(vpl);
        }
        repaint();
        return pickLines;
    }

    public void setPrimaryColor(Color color) {
        primaryColors.set(0, color);
        plotAll();
    }

    public void setBackgroundColor(Color color) {
        for (JSubplot subplot : subplots.values()) {
            subplot.getPlotRegion().setBackgroundColor(color);
        }
        repaint();
    }

    public void setTitle(String title) {
        getTitle().setText(title);
    }

    protected double getTime(S seismogram) {
        return seismogram.getTimeAsDouble();
    }

    protected double getValue(S seismogram, double time) {
        return seismogram.getValueAt(time);
    }

    protected void addPlotObject(JSubplot subplot, PlotObject object) {
        subplot.AddPlotObject(object);
    }

    protected void addSeismogram(S seismogram) {
        seismograms.add(seismogram);
        plotAll();
    }

    private void plotAll() {
        super.clear();

        Map<JSubplot, Legend> legends = new HashMap<>();

        double maxLength = 0.0;
        Color color = primaryColors.get(0);
        for (int i = 0; i < seismograms.size(); i++) {
            S seismogram = seismograms.get(i);
            maxLength = Math.max(maxLength, seismogram.getSegmentLength());

            if (i == 0 || mode == DisplayMode.SEPARATE) {
                JSubplot subplot = addSubplot(true);
                subplot.getPlotRegion().setFillRegion(prefs.getPlotRegionPrefs().isFillRegion());
                subplot.getPlotRegion().setDrawBox(prefs.getPlotRegionPrefs().isDrawBox());
                subplots.put(seismogram, subplot);

                AbstractLine line = getLine(seismogram, color);
                if (showLegend) {
                    Legend legend = new Legend(getTitle().getFontName(), getTitle().getFontSize(), HorizPinEdge.RIGHT, VertPinEdge.TOP, 5, 5);
                    legend.addLabeledLine(seismogram.getIdentifier() + "", line);
                    legends.put(subplot, legend);
                }
            } else {
                color = nextColor(color);
                AbstractLine line = addLine(seismogram, color);
                if (showLegend) {
                    legends.get(subplots.get(seismograms.get(0))).addLabeledLine(seismogram.getIdentifier() + "", line);
                }
            }
        }
        setAllXlimits(0.0, maxLength);

        plotPicks();
        for (Entry<JSubplot, Legend> entry : legends.entrySet()) {
            entry.getKey().AddPlotObject(entry.getValue());
        }

        repaint();
    }

    private Color nextColor(Color color) {
        if (primaryColors.contains(color)) {
            int index = primaryColors.indexOf(color);
            index++;

            if (index < primaryColors.size()) {
                return primaryColors.get(index);
            }
        }

        float r = random.nextFloat();
        float g = random.nextFloat();
        float b = random.nextFloat();
        return new Color(r, g, b);
    }

    protected final Collection<S> getSeismograms() {
        return seismograms;
    }

    protected final JSubplot getSubplot(S seismogram) {
        return subplots.get(seismogram);
    }

    protected final Color getCorrectedColor(Color color) {
        return mode == DisplayMode.SEPARATE ? color : new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    private void plotPicks() {
        for (Entry<String, Double> phaseEntry : picks.entrySet()) {
            for (S seismogram : seismograms) {
                plotPick(seismogram, phaseEntry.getKey(), phaseEntry.getValue());
            }
        }
    }

    private VPickLine plotPick(S seismogram, String phase, double time) {
        int pickLineWidth = 3;
        boolean draggable = false;
        Color pickColor = new Color(0xDC143C);
        double xValue = time - getTime(seismogram);
        double yValue = getValue(seismogram, time);
        VPickLine vpl = new VPickLine(xValue, yValue, 150, phase, pickColor, pickLineWidth, draggable, 12, PickTextPosition.BOTTOM);
        vpl.setDraggable(false);
        vpl.setVisible(true);

        addPlotObject(subplots.get(seismogram), vpl);
        return vpl;
    }

    @Override
    public void clear() {
        super.clear();
        seismograms.clear();
        picks.clear();
        repaint();
    }

    @Override
    public void dataChanged(float[] data) {
        plotAll();
    }

    @Override
    protected void handlePickCreationInfo(Object obj) {
        PickCreationInfo pci = (PickCreationInfo) obj;

        JSubplot plot = pci.getOwningPlot();
        S seismogram = null;
        for (S seis : seismograms) {
            if (subplots.get(seis) == plot) {
                seismogram = seis;
            }
        }

        if (seismogram != null) {
            addPick(pci.getCoordinate().getWorldC1() + getTime(seismogram));
        }
    }

    private class PickModeListener implements KeyListener {

        private boolean pickMode;

        public PickModeListener() {
            pickMode = false;
        }

        @Override
        public void keyTyped(KeyEvent ke) {
        }

        @Override
        public void keyPressed(KeyEvent ke) {
        }

        @Override
        public void keyReleased(KeyEvent ke) {
            if (ke.getKeyCode() == KeyEvent.VK_P) {
                pickMode = !pickMode;
                setMouseMode(pickMode ? MouseMode.CREATE_PICK : MouseMode.SELECT_REGION);
            }
        }
    }

    protected abstract AbstractLine getLine(S seismogram, Color lineColor);

    protected abstract AbstractLine addLine(S seismogram, Color lineColor);
}
