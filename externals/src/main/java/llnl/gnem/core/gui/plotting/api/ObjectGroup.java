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

import java.util.List;

import javafx.scene.paint.Color;

public interface ObjectGroup {

    public String getGroupName();

    public ObjectGroup setGroupName(String groupName);

    public String getHoverName();

    public ObjectGroup setHoverName(String hoverName);

    public Color getEdgeColor();

    public ObjectGroup setEdgeColor(Color color);

    public Color getFillColor();

    public ObjectGroup setFillColor(Color color);

    public int getPxThickness();

    public ObjectGroup setPxThickness(int pxThickness);

    public LineStyles getLineStyle();

    public ObjectGroup setLineStyle(LineStyles lineStyle);

    public String getColorMap();

    public ObjectGroup setColorMap(String colorMap);

    public String getHoverTemplate();

    public ObjectGroup setHoverTemplate(String hoverTemplate);

    public ObjectGroup addPlotObject(PlotObject object);

    public PlotObject getPlotObject(int index);

    public PlotObject getLegendObject();

    public List<PlotObject> getPlotObjects();

    public ObjectGroup setPlotObjects(PlotObject... objects);

    public void plotGroup(BasicPlot plot);

    public void removeGroupFromPlot(BasicPlot plot);

    public List<PlotObject> clearGroup();

}
