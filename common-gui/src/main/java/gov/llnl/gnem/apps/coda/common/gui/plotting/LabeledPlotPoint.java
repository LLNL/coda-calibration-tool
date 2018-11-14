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

public class LabeledPlotPoint {

    private String label;
    private PlotPoint point;

    public LabeledPlotPoint(String label, PlotPoint point) {
        super();
        this.label = label;
        this.point = point;
    }

    public Double getX() {
        return point.getX();
    }

    public void setX(Double x) {
        point.setX(x);
    }

    public Double getY() {
        return point.getY();
    }

    public void setY(Double y) {
        point.setY(y);
    }

    public SymbolStyle getStyle() {
        return point.getStyle();
    }

    public void setStyle(SymbolStyle style) {
        point.setStyle(style);
    }

    public Color getColor() {
        return point.getColor();
    }

    public void setColor(Color color) {
        point.setColor(color);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + ((point == null) ? 0 : point.hashCode());
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
        LabeledPlotPoint other = (LabeledPlotPoint) obj;
        if (label == null) {
            if (other.label != null) {
                return false;
            }
        } else if (!label.equals(other.label)) {
            return false;
        }
        if (point == null) {
            if (other.point != null) {
                return false;
            }
        } else if (!point.equals(other.point)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LabledPlotPoint [label=" + label + ", point=" + point + "]";
    }

}
