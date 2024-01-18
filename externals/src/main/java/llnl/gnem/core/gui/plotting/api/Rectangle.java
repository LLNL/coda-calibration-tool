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
package llnl.gnem.core.gui.plotting.api;

import java.util.Objects;

import javafx.scene.paint.Color;

public class Rectangle implements PlotObject {

    private boolean canDrag = false;
    private double x1;
    private double x2;
    private double ratioY;
    private int pxWidth;
    private Color edgeColor;
    private Color fillColor;
    private String text;
    private Integer zIndex;
    private boolean logScaleX;
    private String hoverTemplate;
    private Boolean showInLegend;
    private String legendGroup;
    private Boolean legendOnly;

    public Rectangle(Rectangle other) {
        this.canDrag = other.canDrag;
        this.x1 = other.x1;
        this.x2 = other.x2;
        this.ratioY = other.ratioY;
        this.pxWidth = other.pxWidth;
        this.edgeColor = other.edgeColor;
        this.fillColor = other.fillColor;
        this.text = other.text;
        this.zIndex = other.zIndex;
        this.logScaleX = other.logScaleX;
        this.showInLegend = other.showInLegend;
        this.legendGroup = other.legendGroup;
        this.legendOnly = other.legendOnly;

    }

    public Rectangle(final double x1, final double x2, final double yRatio, final String label, final Color color) {
        this.x1 = x1;
        this.x2 = x2;
        this.pxWidth = 2;
        this.ratioY = yRatio;
        this.text = label;
        this.edgeColor = color;
        this.fillColor = Color.rgb(128, 128, 128, 0.0); // No fill by default
        this.canDrag = false;
        this.logScaleX = false;
    }

    public Rectangle(final double x1, final double x2, final int pxWidth, final double yRatio, final String label, final Color edgeColor, final Color fillColor, final boolean draggable,
            boolean logScaleX) {
        this.x1 = x1;
        this.x2 = x2;
        this.pxWidth = pxWidth;
        this.ratioY = yRatio;
        this.text = label;
        this.edgeColor = edgeColor;
        this.fillColor = fillColor;
        this.canDrag = draggable;
        this.logScaleX = logScaleX;
    }

    public double getX1() {
        return x1;
    }

    public double getX2() {
        return x2;
    }

    public void setX1(final double x) {
        this.x1 = x;
    }

    public void setX2(final double x) {
        this.x2 = x;
    }

    public double getRatioY() {
        return ratioY;
    }

    public void setRatioY(final double yRatio) {
        this.ratioY = yRatio;
    }

    public boolean isDraggable() {
        return canDrag;
    }

    public Rectangle setDraggable(final boolean dragEnabled) {
        canDrag = dragEnabled;
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
        return edgeColor;
    }

    @Override
    public PlotObject setEdgeColor(final Color color) {
        this.edgeColor = color;
        return this;
    }

    public int getPxWidth() {
        return pxWidth;
    }

    public void setPxWidth(final int value) {
        pxWidth = value;
    }

    public String getText() {
        return text;
    }

    public void setText(final String value) {
        text = value;
    }

    @Override
    public String getName() {
        return text;
    }

    @Override
    public PlotObject setName(final String name) {
        this.text = name;
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
        return Integer.toString(hashCode());
    }

    public boolean isLogScaleX() {
        return logScaleX;
    }

    public Rectangle setLogScaleX(boolean logScaleX) {
        this.logScaleX = logScaleX;
        return this;
    }

    public String getHoverTemplate() {
        return hoverTemplate;
    }

    public void setHoverTemplate(String hoverTemplate) {
        this.hoverTemplate = hoverTemplate;
    }

    public Boolean getShowInLegend() {
        return showInLegend;
    }

    @Override
    public PlotObject showInLegend(Boolean showInLegend) {
        this.showInLegend = showInLegend;
        return this;
    }

    @Override
    public String getLegendGrouping() {
        return legendGroup;
    }

    @Override
    public PlotObject setLegendGrouping(String legendGroup) {
        this.legendGroup = legendGroup;
        return this;
    }

    @Override
    public Boolean getLegendOnly() {
        return legendOnly;
    }

    @Override
    public PlotObject setLegendOnly(Boolean legendOnly) {
        this.legendOnly = legendOnly;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(canDrag, edgeColor, fillColor, hoverTemplate, legendGroup, legendOnly, logScaleX, pxWidth, ratioY, showInLegend, text, x1, x2, zIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Rectangle)) {
            return false;
        }
        Rectangle other = (Rectangle) obj;
        return canDrag == other.canDrag
                && Objects.equals(edgeColor, other.edgeColor)
                && Objects.equals(fillColor, other.fillColor)
                && Objects.equals(hoverTemplate, other.hoverTemplate)
                && Objects.equals(legendGroup, other.legendGroup)
                && Objects.equals(legendOnly, other.legendOnly)
                && logScaleX == other.logScaleX
                && pxWidth == other.pxWidth
                && Double.doubleToLongBits(ratioY) == Double.doubleToLongBits(other.ratioY)
                && Objects.equals(showInLegend, other.showInLegend)
                && Objects.equals(text, other.text)
                && Double.doubleToLongBits(x1) == Double.doubleToLongBits(other.x1)
                && Double.doubleToLongBits(x2) == Double.doubleToLongBits(other.x2)
                && Objects.equals(zIndex, other.zIndex);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Rectangle [canDrag=")
               .append(canDrag)
               .append(", x1=")
               .append(x1)
               .append(", x2=")
               .append(x2)
               .append(", ratioY=")
               .append(ratioY)
               .append(", pxWidth=")
               .append(pxWidth)
               .append(", edgeColor=")
               .append(edgeColor)
               .append(", fillColor=")
               .append(fillColor)
               .append(", text=")
               .append(text)
               .append(", zIndex=")
               .append(zIndex)
               .append(", logScaleX=")
               .append(logScaleX)
               .append(", hoverTemplate=")
               .append(hoverTemplate)
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