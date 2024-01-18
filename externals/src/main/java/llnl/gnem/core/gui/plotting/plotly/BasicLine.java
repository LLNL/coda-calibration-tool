/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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

import java.util.Arrays;
import java.util.Objects;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.FillModes;
import llnl.gnem.core.gui.plotting.api.HoverModes;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;

public class BasicLine implements Line {

    private double[] x;
    private double[] y;
    private double[] color = new double[0];
    private double[] errorMin;
    private double[] errorMax;
    private Color fillColor;
    private LineStyles style;
    private FillModes fillMode;
    private HoverModes hoverMode;
    private int pxThickness;
    private String name;
    private String colorMap;
    private Boolean showInLegend;
    private String legendGroup;
    private Boolean legendOnly;
    private Boolean useHorizontalErrorBars;
    private String hoverTemplate;
    private Integer zIndex;

    public BasicLine(final Line other) {
        this.x = other.getX();
        this.y = other.getY();
        this.color = other.getColor();
        this.errorMin = other.getErrorData();
        this.errorMax = other.getErrorDataMinus();
        this.fillColor = other.getFillColor();
        this.style = other.getStyle();
        this.pxThickness = other.getPxThickness();
        this.name = other.getName();
        this.colorMap = other.getColorMap();
        this.showInLegend = other.shouldShowInLegend();
        this.useHorizontalErrorBars = other.getUseHorizontalErrorBars();
        this.hoverTemplate = other.getHoverTemplate();
        this.hoverMode = other.getHoverMode();
        this.fillMode = other.getFillMode();
        this.legendGroup = other.getLegendGrouping();
        this.legendOnly = other.getLegendOnly();
        this.zIndex = other.getZindex();
    }

    public BasicLine(final double[] xVals, final double[] yVals, final Color color, final LineStyles style, final int pxThickness) {
        this.x = xVals;
        this.y = yVals;
        this.fillColor = color;
        this.style = style;
        this.pxThickness = pxThickness;
        this.useHorizontalErrorBars = false;
        this.errorMin = new double[0];
        this.errorMax = new double[0];
    }

    public BasicLine(final double[] xVals, final double[] yVals, final double[] errorMin, final double[] errorMax, final Color color, final LineStyles style, final int pxThickness) {
        this.x = xVals;
        this.y = yVals;
        this.fillColor = color;
        this.style = style;
        this.pxThickness = pxThickness;
        this.useHorizontalErrorBars = false;
        this.errorMin = errorMin;
        this.errorMax = errorMax;
    }

    public BasicLine(final String name, final double xStart, final double xIncrement, final float[] data, final Color blue, final LineStyles solid, final int lineWidth) {
        this.name = name;
        fillColor = blue;
        style = solid;
        pxThickness = lineWidth;
        this.useHorizontalErrorBars = false;
        this.errorMin = new double[0];
        this.errorMax = new double[0];
        this.x = new double[data.length];
        this.y = new double[data.length];
        for (int t = 0; t < data.length; t++) {
            x[t] = xStart + (t * xIncrement);
            y[t] = data[t];
        }
    }

    @Override
    public double[] getX() {
        return x;
    }

    @Override
    public PlotObject setX(final double[] xVals) {
        this.x = xVals;
        return this;
    }

    @Override
    public double[] getY() {
        return y;
    }

    @Override
    public PlotObject setY(final double[] yVals) {
        this.y = yVals;
        return this;
    }

    @Override
    public double[] getErrorData() {
        return errorMin;
    }

    @Override
    public PlotObject setErrorData(final double[] errorMin) {
        this.errorMin = errorMin;
        return this;
    }

    @Override
    public double[] getErrorDataMinus() {
        return errorMax;
    }

    @Override
    public PlotObject setErrorDataMinus(final double[] errorMax) {
        this.errorMax = errorMax;
        return this;
    }

    @Override
    public Color getFillColor() {
        return fillColor;
    }

    @Override
    public PlotObject setFillColor(final Color color) {
        this.fillColor = color;
        return this;
    }

    @Override
    public Color getEdgeColor() {
        return getFillColor();
    }

    @Override
    public PlotObject setEdgeColor(final Color color) {
        return this;
    }

    @Override
    public LineStyles getStyle() {
        return style;
    }

    @Override
    public PlotObject setStyle(final LineStyles style) {
        this.style = style;
        return this;
    }

    @Override
    public HoverModes getHoverMode() {
        return hoverMode;
    }

    @Override
    public PlotObject setHoverMode(HoverModes hoverMode) {
        this.hoverMode = hoverMode;
        return this;
    }

    @Override
    public FillModes getFillMode() {
        return fillMode;
    }

    @Override
    public PlotObject setFillMode(FillModes fillMode) {
        this.fillMode = fillMode;
        return this;
    }

    @Override
    public int getPxThickness() {
        return pxThickness;
    }

    @Override
    public PlotObject setPxThickness(final int pxThickness) {
        this.pxThickness = pxThickness;
        return this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public PlotObject setName(final String name) {
        this.name = name;
        return this;
    }

    @Override
    public double[] getColor() {
        return color;
    }

    @Override
    public PlotObject setColor(final double[] colorVals) {
        color = colorVals;
        return this;
    }

    @Override
    public String getColorMap() {
        return colorMap;
    }

    @Override
    public PlotObject setColorMap(final String colorMap) {
        this.colorMap = colorMap;
        return this;
    }

    @Override
    public Boolean shouldShowInLegend() {
        return showInLegend;
    }

    @Override
    public PlotObject showInLegend(final Boolean showInLegend) {
        this.showInLegend = showInLegend;
        return this;
    }

    @Override
    public boolean getUseHorizontalErrorBars() {
        return useHorizontalErrorBars;
    }

    @Override
    public PlotObject setUseHorizontalErrorBars(final boolean useHorizontalErrorBars) {
        this.useHorizontalErrorBars = useHorizontalErrorBars;
        return this;
    }

    @Override
    public String getHoverTemplate() {
        return this.hoverTemplate;
    }

    @Override
    public PlotObject setHoverTemplate(final String hoverTemplate) {
        this.hoverTemplate = hoverTemplate;
        return this;
    }

    @Override
    public String getLegendGrouping() {
        return legendGroup;
    }

    @Override
    public PlotObject setLegendGrouping(final String legendGroup) {
        this.legendGroup = legendGroup;
        return this;
    }

    @Override
    public Boolean getLegendOnly() {
        return legendOnly;
    }

    @Override
    public PlotObject setLegendOnly(final Boolean legendOnly) {
        this.legendOnly = legendOnly;
        return this;
    }

    @Override
    public Integer getZindex() {
        return zIndex;
    }

    @Override
    public void setZindex(Integer zIndex) {
        this.zIndex = zIndex;
    }

    @Override
    public String getSeriesIdentifier() {
        return Integer.toString(Objects.hash(colorMap, fillColor, legendGroup, legendOnly, name, pxThickness, showInLegend, style));
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(color);
        result = prime * result + Arrays.hashCode(errorMax);
        result = prime * result + Arrays.hashCode(errorMin);
        result = prime * result + Arrays.hashCode(x);
        result = prime * result + Arrays.hashCode(y);
        result = prime * result
                + Objects.hash(colorMap, fillColor, fillMode, hoverMode, hoverTemplate, legendGroup, legendOnly, name, pxThickness, showInLegend, style, useHorizontalErrorBars, zIndex);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BasicLine)) {
            return false;
        }
        BasicLine other = (BasicLine) obj;
        return Arrays.equals(color, other.color)
                && Objects.equals(colorMap, other.colorMap)
                && Arrays.equals(errorMax, other.errorMax)
                && Arrays.equals(errorMin, other.errorMin)
                && Objects.equals(fillColor, other.fillColor)
                && fillMode == other.fillMode
                && hoverMode == other.hoverMode
                && Objects.equals(hoverTemplate, other.hoverTemplate)
                && Objects.equals(legendGroup, other.legendGroup)
                && Objects.equals(legendOnly, other.legendOnly)
                && Objects.equals(name, other.name)
                && pxThickness == other.pxThickness
                && Objects.equals(showInLegend, other.showInLegend)
                && style == other.style
                && Objects.equals(useHorizontalErrorBars, other.useHorizontalErrorBars)
                && Arrays.equals(x, other.x)
                && Arrays.equals(y, other.y)
                && Objects.equals(zIndex, other.zIndex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BasicLine [x=")
               .append(Arrays.toString(x))
               .append(", y=")
               .append(Arrays.toString(y))
               .append(", color=")
               .append(Arrays.toString(color))
               .append(", errorMin=")
               .append(Arrays.toString(errorMin))
               .append(", errorMax=")
               .append(Arrays.toString(errorMax))
               .append(", fillColor=")
               .append(fillColor)
               .append(", style=")
               .append(style)
               .append(", fillMode=")
               .append(fillMode)
               .append(", hoverMode=")
               .append(hoverMode)
               .append(", pxThickness=")
               .append(pxThickness)
               .append(", name=")
               .append(name)
               .append(", colorMap=")
               .append(colorMap)
               .append(", showInLegend=")
               .append(showInLegend)
               .append(", legendGroup=")
               .append(legendGroup)
               .append(", legendOnly=")
               .append(legendOnly)
               .append(", useHorizontalErrorBars=")
               .append(useHorizontalErrorBars)
               .append(", hoverTemplate=")
               .append(hoverTemplate)
               .append(", zIndex=")
               .append(zIndex)
               .append("]");
        return builder.toString();
    }
}
