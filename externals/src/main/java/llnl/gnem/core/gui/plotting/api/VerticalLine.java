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
package llnl.gnem.core.gui.plotting.api;

import java.util.Objects;

import javafx.scene.paint.Color;

public class VerticalLine implements PlotObject {

    private boolean canDrag = false;
    private double x;
    private double ratioY;
    private Color fillColor;
    private int pxWidth;
    private String text;
    private Integer zIndex;
    private boolean logScaleX;

    public VerticalLine(final double x, final double yRatio, final String label) {
        this.x = x;
        ratioY = yRatio;
        text = label;
        fillColor = Color.GRAY;
        pxWidth = 3;
        canDrag = false;
        logScaleX = false;
    }

    public VerticalLine(final double x, final double yRatio, final String label, final Color color, final int width, final boolean draggable, boolean logScaleX) {
        this.x = x;
        ratioY = yRatio;
        text = label;
        this.fillColor = color;
        pxWidth = width;
        canDrag = draggable;
        this.logScaleX = logScaleX;
    }

    public double getX() {
        return x;
    }

    public void setX(final double x) {
        this.x = x;
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

    public VerticalLine setDraggable(final boolean dragEnabled) {
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
        return getFillColor();
    }

    @Override
    public PlotObject setEdgeColor(final Color color) {
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

    public VerticalLine setLogScaleX(boolean logScaleX) {
        this.logScaleX = logScaleX;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(canDrag, fillColor, pxWidth, ratioY, text, x);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof VerticalLine)) {
            return false;
        }
        final VerticalLine other = (VerticalLine) obj;
        return canDrag == other.canDrag
                && Objects.equals(fillColor, other.fillColor)
                && pxWidth == other.pxWidth
                && Double.doubleToLongBits(ratioY) == Double.doubleToLongBits(other.ratioY)
                && Objects.equals(text, other.text)
                && Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("VerticalLine [canDrag=")
               .append(canDrag)
               .append(", x=")
               .append(x)
               .append(", ratioY=")
               .append(ratioY)
               .append(", fillColor=")
               .append(fillColor)
               .append(", pxWidth=")
               .append(pxWidth)
               .append(", text=")
               .append(text)
               .append("]");
        return builder.toString();
    }
}