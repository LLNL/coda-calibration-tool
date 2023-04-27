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
package llnl.gnem.core.gui.plotting.api;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.Map;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.plotly.PlotObjectData;

public interface BasicPlot extends Serializable {

    public void setBackgroundColor(Color color);

    public void setSymbolSize(int pxSymbolSize);

    public Title getTitle();

    public void addPlotObjectObserver(PropertyChangeListener observer);

    public void setAxisLimits(AxisLimits... axisLimits);

    public void addPlotObject(PlotObject object);

    public void removePlotObject(PlotObject object);

    public String getSVG();

    public void showLegend(boolean visible);

    public void clear();

    public void attachToDisplayNode(Pane parent);

    public void setColorMap(String colorMap);

    public BasicPlot createSubPlot();

    public void replot();

    public void addAxes(Axis... axes);

    public void clearAxes();

    public void setMargin(Integer top, Integer bottom, Integer left, Integer right);

    public void setUseHorizontalBottomLegend(boolean useHorizontalBottomLegend);

    public Map<String, PlotObjectData> getPlotTypes();

    public void fullReplot();

}
