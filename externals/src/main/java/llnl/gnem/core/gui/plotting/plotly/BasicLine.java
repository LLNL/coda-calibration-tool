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

import java.util.Arrays;
import java.util.Objects;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;

public class BasicLine implements Line {

    private double[] x;
    private double[] y;
    private double[] color = new double[0];
    private Color fillColor;
    private LineStyles style;
    private int pxThickness;
    private String name;
    private String colorMap;
    private Boolean showInLegend;
    private String legendGroup;
    private Boolean legendOnly;
    private Integer zIndex;

    public BasicLine(final double[] xVals, final double[] yVals, final Color color, final LineStyles style, final int pxThickness) {
        this.x = xVals;
        this.y = yVals;
        this.fillColor = color;
        this.style = style;
        this.pxThickness = pxThickness;
    }

    public BasicLine(final String name, final double xStart, final double xIncrement, final float[] data, final Color blue, final LineStyles solid, final int lineWidth) {
        this.name = name;
        fillColor = blue;
        style = solid;
        pxThickness = lineWidth;
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
    public void setX(final double[] xVals) {
        this.x = xVals;
    }

    @Override
    public double[] getY() {
        return y;
    }

    @Override
    public void setY(final double[] yVals) {
        this.y = yVals;
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
    public void setStyle(final LineStyles style) {
        this.style = style;
    }

    @Override
    public int getPxThickness() {
        return pxThickness;
    }

    @Override
    public void setPxThickness(final int pxThickness) {
        this.pxThickness = pxThickness;
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
    public void setColor(final double[] colorVals) {
        color = colorVals;
    }

    @Override
    public void setColorMap(final String colorMap) {
        this.colorMap = colorMap;
    }

    @Override
    public String getColorMap() {
        return colorMap;
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
        result = prime * result + Arrays.hashCode(x);
        result = prime * result + Arrays.hashCode(y);
        result = prime * result + Objects.hash(colorMap, fillColor, legendGroup, legendOnly, name, pxThickness, showInLegend, style);
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BasicLine)) {
            return false;
        }
        final BasicLine other = (BasicLine) obj;
        return Arrays.equals(color, other.color)
                && Objects.equals(colorMap, other.colorMap)
                && Objects.equals(fillColor, other.fillColor)
                && Objects.equals(legendGroup, other.legendGroup)
                && Objects.equals(name, other.name)
                && pxThickness == other.pxThickness
                && Objects.equals(showInLegend, other.showInLegend)
                && style == other.style
                && Arrays.equals(x, other.x)
                && Arrays.equals(y, other.y);
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        final StringBuilder builder = new StringBuilder();
        builder.append("BasicLine [x=")
               .append(x != null ? Arrays.toString(Arrays.copyOf(x, Math.min(x.length, maxLen))) : null)
               .append(", y=")
               .append(y != null ? Arrays.toString(Arrays.copyOf(y, Math.min(y.length, maxLen))) : null)
               .append(", color=")
               .append(color != null ? Arrays.toString(Arrays.copyOf(color, Math.min(color.length, maxLen))) : null)
               .append(", fillColor=")
               .append(fillColor)
               .append(", style=")
               .append(style)
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
               .append("]");
        return builder.toString();
    }

}
