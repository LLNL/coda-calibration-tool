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
package llnl.gnem.core.gui.waveform.plotPrefs;

import java.awt.Color;
import java.awt.Font;
import java.io.Serializable;

import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickTextPosition;

/**
 * Created by: dodge1 Date: Nov 23, 2004
 */
public class PlotPresentationPrefs implements Serializable {

    private static final long serialVersionUID = -6935800982118442836L;
    protected static final Color PLOT_BACKGROUND_COLOR = new Color(0.96F, 0.96F, 0.96F);
    private int maxSymbolsToPlot = 200;
    private boolean limitPlottedSymbols = true;
    private boolean plotLineSymbols = true;
    private final DrawingRegionPrefs borderPrefs;
    private final DrawingRegionPrefs plotRegionPrefs;
    private final AxisPrefs xAxisPrefs;
    private final AxisPrefs yAxisPrefs;
    private Color traceColor;
    private boolean plotTracesAtSameScale;
    private final Color selectedTraceColor;
    private final PickPrefs pickPrefs;
    private final PickPrefs predPickPrefs;


    public PlotPresentationPrefs() {
        borderPrefs = new DrawingRegionPrefs();
        plotRegionPrefs = new DrawingRegionPrefs();
        xAxisPrefs = new AxisPrefs();
        yAxisPrefs = new AxisPrefs();

        traceColor = Color.blue;
        selectedTraceColor = Color.GREEN;
        plotTracesAtSameScale = false;
        pickPrefs = new PickPrefs();
        predPickPrefs = new PickPrefs(.8, 5.0, Color.LIGHT_GRAY, 1, 10, PickTextPosition.TOP, PenStyle.DASH);
    }

    public Color getTraceColor() {
        return traceColor;
    }

    public void setTraceColor(Color traceColor) {
        this.traceColor = traceColor;
    }

    public Color getSelectedTraceColor() {
        return selectedTraceColor;
    }

    /**
     * @return the maxSymbolsToPlot
     */
    public int getMaxSymbolsToPlot() {
        return maxSymbolsToPlot;
    }

    /**
     * @param maxSymbolsToPlot the maxSymbolsToPlot to set
     */
    public void setMaxSymbolsToPlot(int maxSymbolsToPlot) {
        this.maxSymbolsToPlot = maxSymbolsToPlot;
    }

    /**
     * @return the limitPlottedSymbols
     */
    public boolean isLimitPlottedSymbols() {
        return limitPlottedSymbols;
    }

    /**
     * @param limitPlottedSymbols the limitPlottedSymbols to set
     */
    public void setLimitPlottedSymbols(boolean limitPlottedSymbols) {
        this.limitPlottedSymbols = limitPlottedSymbols;
    }

    /**
     * @return the plotLineSymbols
     */
    public boolean isPlotLineSymbols() {
        return plotLineSymbols;
    }

    /**
     * @param plotLineSymbols the plotLineSymbols to set
     */
    public void setPlotLineSymbols(boolean plotLineSymbols) {
        this.plotLineSymbols = plotLineSymbols;
    }

    /**
     * @return the borderPrefs
     */
    public DrawingRegionPrefs getBorderPrefs() {
        return borderPrefs;
    }

    /**
     * @return the plotRegionPrefs
     */
    public DrawingRegionPrefs getPlotRegionPrefs() {
        return plotRegionPrefs;
    }

    /**
     * @return the xAxisPrefs
     */
    public AxisPrefs getxAxisPrefs() {
        return xAxisPrefs;
    }

    /**
     * @return the yAxisPrefs
     */
    public AxisPrefs getyAxisPrefs() {
        return yAxisPrefs;
    }

    /**
     *
     * @return titleColor get the title color
     */
    public Color getTitleColor() {
        return borderPrefs.getFontColor();
    }

    /**
     * @param titleColor the titleColor to set
     */
    public void setTitleColor(Color titleColor) {
        borderPrefs.setFontColor(titleColor);
    }

    /**
     *
     * @return titleFont
     */
    public Font getTitleFont() {
        return borderPrefs.getFont();
    }

    /**
     *
     * @param font
     */
    public void setTitleFont(Font font) {
        borderPrefs.setFont(font);
    }

    /**
     *
     * @return titleFontSize
     */
    public int getTitleFontSize() {
        Font font = getTitleFont();
        return font.getSize();
    }

    /**
     *
     * @param titleFontSize
     */
    public void setTitleFontSize(int titleFontSize) {
        Font font = getTitleFont();
        String fontName = font.getName();
        int fontStyle = font.getStyle();
        Font newFont = new Font(fontName, fontStyle, titleFontSize);
        setTitleFont(newFont);
    }


    public void setPlotTracesAtSameScale(boolean enabled) {
        this.plotTracesAtSameScale = enabled;
    }

    public boolean isPlotTracesAtSameScale() {
        return this.plotTracesAtSameScale;
    }

    /**
     * @return the pickPrefs
     */
    public PickPrefs getPickPrefs() {
        return pickPrefs;
    }

    /**
     * @return the predPickPrefs
     */
    public PickPrefs getPredPickPrefs() {
        return predPickPrefs;
    }
}
