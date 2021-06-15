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
package gov.llnl.gnem.apps.coda.common.gui.plotting;

import java.util.Objects;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;

public class PlotPoint {

    private Double x;
    private Double y;
    private SymbolStyles style;
    private Color color;
    private Color edgeColor;

    public PlotPoint(Double x, Double y, SymbolStyles style, Color color, Color edgeColor) {
        super();
        this.x = x;
        this.y = y;
        this.style = style;
        this.color = color;
        this.edgeColor = edgeColor;
    }

    public PlotPoint(PlotPoint pp) {
        super();
        if (pp != null) {
            this.x = pp.getX();
            this.y = pp.getY();
            this.style = pp.getStyle();
            this.color = pp.getColor();
            this.edgeColor = pp.getEdgeColor();
        }
    }

    public PlotPoint() {
        super();
    }

    public Double getX() {
        return x;
    }

    public PlotPoint setX(Double x) {
        this.x = x;
        return this;
    }

    public Double getY() {
        return y;
    }

    public PlotPoint setY(Double y) {
        this.y = y;
        return this;
    }

    public SymbolStyles getStyle() {
        return style;
    }

    public PlotPoint setStyle(SymbolStyles style) {
        this.style = style;
        return this;
    }

    public Color getColor() {
        return color;
    }

    public PlotPoint setColor(Color color) {
        this.color = color;
        return this;
    }

    public Color getEdgeColor() {
        return edgeColor;
    }

    public PlotPoint setEdgeColor(Color edgeColor) {
        this.edgeColor = edgeColor;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, edgeColor, style, x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlotPoint)) {
            return false;
        }
        PlotPoint other = (PlotPoint) obj;
        return Objects.equals(color, other.color) && Objects.equals(edgeColor, other.edgeColor) && style == other.style && Objects.equals(x, other.x) && Objects.equals(y, other.y);
    }

    @Override
    public String toString() {
        return "PlotPoint [x=" + x + ", y=" + y + ", style=" + style + ", color=" + color + ", edgeColor=" + edgeColor + "]";
    }
}
