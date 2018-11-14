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
package gov.llnl.gnem.apps.coda.common.gui.plotting;

import java.awt.Color;

import llnl.gnem.core.gui.plotting.plotobject.SymbolStyle;

public class PlotPoint {

    private Double x;
    private Double y;
    private SymbolStyle style;
    private Color color;

    public PlotPoint(Double x, Double y, SymbolStyle style, Color color) {
        super();
        this.x = x;
        this.y = y;
        this.style = style;
        this.color = color;
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

    public SymbolStyle getStyle() {
        return style;
    }

    public PlotPoint setStyle(SymbolStyle style) {
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((color == null) ? 0 : color.hashCode());
        result = prime * result + ((style == null) ? 0 : style.hashCode());
        result = prime * result + ((x == null) ? 0 : x.hashCode());
        result = prime * result + ((y == null) ? 0 : y.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PlotPoint other = (PlotPoint) obj;
        if (color == null) {
            if (other.color != null) {
                return false;
            }
        } else if (!color.equals(other.color)) {
            return false;
        }
        if (style != other.style) {
            return false;
        }
        if (x == null) {
            if (other.x != null) {
                return false;
            }
        } else if (!x.equals(other.x)) {
            return false;
        }
        if (y == null) {
            if (other.y != null) {
                return false;
            }
        } else if (!y.equals(other.y)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PlotPoint [x=" + x + ", y=" + y + ", style=" + style + ", color=" + color + "]";
    }

}
