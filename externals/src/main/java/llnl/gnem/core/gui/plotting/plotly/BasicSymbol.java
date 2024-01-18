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

import java.util.Objects;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;

public class BasicSymbol implements Symbol {

    private double x;
    private double y;
    private Color edgeColor;
    private Color fillColor;
    private SymbolStyles style;
    private Color textColor;
    private String text;
    private boolean textVisible;
    private Double colorationValue;
    private String name;
    private String colorMap;
    private String hoverTemplate;
    private Boolean showInLegend;
    private String legendGroup;
    private Integer zIndex;

    public BasicSymbol(BasicSymbol other) {
        this.x = other.x;
        this.y = other.y;
        this.edgeColor = other.edgeColor;
        this.fillColor = other.fillColor;
        this.style = other.style;
        this.textColor = other.textColor;
        this.text = other.text;
        this.textVisible = other.textVisible;
        this.colorationValue = other.colorationValue;
        this.name = other.name;
        this.colorMap = other.colorMap;
        this.hoverTemplate = other.hoverTemplate;
        this.showInLegend = other.showInLegend;
        this.legendGroup = other.legendGroup;
        this.zIndex = other.zIndex;
    }

    public BasicSymbol(final SymbolStyles style, final String name, final double x, final double y, final Color fillColor, final Color edgeColor, final Color textColor, final String text,
            final boolean textVisible) {
        this.style = style;
        this.name = name;
        this.x = x;
        this.y = y;
        this.fillColor = fillColor;
        this.edgeColor = edgeColor;
        this.textColor = textColor;
        this.text = text;
        this.textVisible = textVisible;
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
    public void setColorMap(final String colorMap) {
        this.colorMap = colorMap;
    }

    @Override
    public String getColorMap() {
        return colorMap;
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public void setX(final double x) {
        this.x = x;
    }

    @Override
    public void setY(final double y) {
        this.y = y;
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
        return edgeColor;
    }

    @Override
    public PlotObject setEdgeColor(final Color color) {
        edgeColor = color;
        return this;
    }

    @Override
    public SymbolStyles getStyle() {
        return style;
    }

    @Override
    public void setStyle(final SymbolStyles style) {
        this.style = style;
    }

    @Override
    public Color getTextColor() {
        return textColor;
    }

    @Override
    public void setTextColor(final Color textColor) {
        this.textColor = textColor;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public void setText(final String text) {
        this.text = text;
    }

    @Override
    public boolean isTextVisible() {
        return textVisible;
    }

    @Override
    public void setTextVisible(final boolean textVisible) {
        this.textVisible = textVisible;
    }

    @Override
    public String getHoverTemplate() {
        return hoverTemplate;
    }

    @Override
    public void setHoverTemplate(String hoverTemplate) {
        this.hoverTemplate = hoverTemplate;
    }

    @Override
    public Double getColorationValue() {
        return colorationValue;
    }

    @Override
    public void setColorationValue(final Double colorationValue) {
        this.colorationValue = colorationValue;
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
    public Integer getZindex() {
        return zIndex;
    }

    @Override
    public void setZindex(Integer zIndex) {
        this.zIndex = zIndex;
    }

    @Override
    public String getSeriesIdentifier() {
        return Integer.toString(Objects.hash(style, colorMap, edgeColor, fillColor, textColor));
    }

    @Override
    public int hashCode() {
        return Objects.hash(colorMap, colorationValue, edgeColor, fillColor, legendGroup, name, showInLegend, style, text, textColor, textVisible, x, y);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BasicSymbol)) {
            return false;
        }
        final BasicSymbol other = (BasicSymbol) obj;
        return Objects.equals(colorMap, other.colorMap)
                && Objects.equals(colorationValue, other.colorationValue)
                && Objects.equals(edgeColor, other.edgeColor)
                && Objects.equals(fillColor, other.fillColor)
                && Objects.equals(legendGroup, other.legendGroup)
                && Objects.equals(name, other.name)
                && Objects.equals(showInLegend, other.showInLegend)
                && style == other.style
                && Objects.equals(text, other.text)
                && Objects.equals(textColor, other.textColor)
                && textVisible == other.textVisible
                && Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("BasicSymbol [x=")
               .append(x)
               .append(", y=")
               .append(y)
               .append(", edgeColor=")
               .append(edgeColor)
               .append(", fillColor=")
               .append(fillColor)
               .append(", style=")
               .append(style)
               .append(", textColor=")
               .append(textColor)
               .append(", text=")
               .append(text)
               .append(", textVisible=")
               .append(textVisible)
               .append(", colorationValue=")
               .append(colorationValue)
               .append(", name=")
               .append(name)
               .append(", colorMap=")
               .append(colorMap)
               .append(", showInLegend=")
               .append(showInLegend)
               .append(", legendGroup=")
               .append(legendGroup)
               .append("]");
        return builder.toString();
    }

}
