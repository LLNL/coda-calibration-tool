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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import java.awt.Color;

import llnl.gnem.core.gui.plotting.plotobject.SymbolStyle;

/**
 *
 * User: Eric Matzel Date: Oct 23, 2007
 */
public class PlotProperties {
    private SymbolStyle symbolStyle = SymbolStyle.DIAMOND;

    private double symbolSize = 3;

    private Color symbolEdgeColor = Color.black;

    private Color symbolFillColor = Color.red;

    private double minYAxisValue = -1.0;

    private double maxYAxisValue = 1.0;

    private double minXAxisValue = -1.0;

    private double maxXAxisValue = 1.0;

    private boolean autoCalculateYaxisRange = true;

    private boolean autoCalculateXaxisRange = true;

    public boolean getAutoCalculateXaxisRange() {
        return autoCalculateXaxisRange;
    }

    public boolean getAutoCalculateYaxisRange() {
        return autoCalculateYaxisRange;
    }

    public double getMaxXAxisValue() {
        return maxXAxisValue;
    }

    public double getMaxYAxisValue() {
        return maxYAxisValue;
    }

    public double getMinXAxisValue() {
        return minXAxisValue;
    }

    public double getMinYAxisValue() {
        return minYAxisValue;
    }

    public Color getSymbolEdgeColor() {
        return symbolEdgeColor;
    }

    public Color getSymbolFillColor() {
        return symbolFillColor;
    }

    public double getSymbolSize() {
        return symbolSize;
    }

    public SymbolStyle getSymbolStyle() {
        return symbolStyle;
    }

    public void setAutoCalculateXaxisRange(boolean autoCalculateXaxisRange) {
        this.autoCalculateXaxisRange = autoCalculateXaxisRange;
    }
    public void setAutoCalculateYaxisRange(boolean autoCalculateYaxisRange) {
        this.autoCalculateYaxisRange = autoCalculateYaxisRange;
    }
    public void setMaxXAxisValue(double maxXAxisValue) {
        this.maxXAxisValue = maxXAxisValue;
    }
    public void setMaxYAxisValue(double maxYAxisValue) {
        this.maxYAxisValue = maxYAxisValue;
    }

    public void setMinXAxisValue(double minXAxisValue) {
        this.minXAxisValue = minXAxisValue;
    }
    public void setMinYAxisValue(double minYAxisValue) {
        this.minYAxisValue = minYAxisValue;
    }
    public void setSymbolEdgeColor(Color symbolEdgeColor) {
        this.symbolEdgeColor = symbolEdgeColor;
    }
    public void setSymbolFillColor(Color symbolFillColor) {
        this.symbolFillColor = symbolFillColor;
    }
    public void setSymbolSize(double symbolSize) {
        this.symbolSize = symbolSize;
    }
    public void setSymbolStyle(SymbolStyle symbolStyle) {
        this.symbolStyle = symbolStyle;
    }
}
