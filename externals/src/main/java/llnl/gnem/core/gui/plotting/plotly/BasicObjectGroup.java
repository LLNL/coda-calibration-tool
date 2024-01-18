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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.ObjectGroup;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.PlottingUtils;
import llnl.gnem.core.gui.plotting.api.Rectangle;
import llnl.gnem.core.gui.plotting.api.VerticalLine;

public class BasicObjectGroup implements ObjectGroup {

    private PlotObject legendObject;
    private List<PlotObject> groupObjects;
    private Map<String, PlotObject> originalObjects;
    private String groupName;
    private String hoverName;

    private Color groupEdgeColor;
    private Color groupFillColor;
    private String colorMap;
    private String hoverTemplate;
    private LineStyles groupLineStyle;
    private int pxThickness;
    private boolean plotted;

    public BasicObjectGroup(PlotFactory plotFactory, String groupName) {
        this.groupObjects = new ArrayList<>();
        this.originalObjects = new HashMap<>();
        this.groupName = groupName;
        this.hoverName = groupName;
        this.hoverTemplate = null;
        this.groupEdgeColor = Color.BLACK;
        this.groupFillColor = Color.BLACK;
        this.groupLineStyle = LineStyles.SOLID;
        this.pxThickness = 1;
        this.legendObject = PlottingUtils.legendOnlyLine(groupName, plotFactory, groupFillColor, groupLineStyle);
        this.plotted = false;
    }

    @Override
    public String getGroupName() {
        return groupName;
    }

    @Override
    public ObjectGroup setGroupName(String groupName) {
        this.groupName = groupName;
        this.legendObject.setName(groupName);

        if (this.groupObjects != null) {
            this.groupObjects.forEach(object -> {
                object.setLegendGrouping(groupName);
            });
        }

        return this;
    }

    @Override
    public String getHoverName() {
        return hoverName;
    }

    @Override
    public ObjectGroup setHoverName(String hoverName) {
        this.hoverName = hoverName;

        this.groupObjects.forEach(object -> {
            updateHoverTemplate(object);
        });

        return this;
    }

    @Override
    public Color getEdgeColor() {
        return this.groupEdgeColor;
    }

    @Override
    public ObjectGroup setEdgeColor(Color color) {
        this.groupEdgeColor = color;
        this.legendObject.setEdgeColor(groupEdgeColor);

        if (this.groupObjects != null) {
            this.groupObjects.forEach(object -> {
                object.setEdgeColor(color);
            });
        }

        return this;
    }

    @Override
    public Color getFillColor() {
        return this.groupFillColor;
    }

    @Override
    public ObjectGroup setFillColor(Color color) {
        this.groupFillColor = color;
        this.legendObject.setFillColor(groupFillColor);

        if (this.groupObjects != null) {
            this.groupObjects.forEach(object -> {
                object.setFillColor(color);
            });
        }
        return this;
    }

    @Override
    public int getPxThickness() {
        return this.pxThickness;
    }

    @Override
    public ObjectGroup setPxThickness(int pxThickness) {
        this.pxThickness = pxThickness;

        if (this.groupObjects != null) {
            this.groupObjects.forEach(object -> {
                if (object instanceof Line) {
                    Line line = (Line) object;
                    line.setPxThickness(pxThickness);
                } else if (object instanceof Rectangle) {
                    Rectangle rect = (Rectangle) object;
                    rect.setPxWidth(pxThickness);
                } else if (object instanceof VerticalLine) {
                    VerticalLine vert = (VerticalLine) object;
                    vert.setPxWidth(pxThickness);
                }
            });
        }
        return this;
    }

    @Override
    public LineStyles getLineStyle() {
        return this.groupLineStyle;
    }

    @Override
    public ObjectGroup setLineStyle(LineStyles lineStyle) {
        this.groupLineStyle = lineStyle;

        if (this.groupObjects != null) {
            this.groupObjects.forEach(object -> {
                if (object instanceof Line) {
                    Line line = (Line) object;
                    line.setStyle(lineStyle);
                }
            });
        }
        return this;
    }

    @Override
    public String getColorMap() {
        return this.colorMap;
    }

    @Override
    public ObjectGroup setColorMap(String colorMap) {
        this.colorMap = colorMap;

        if (this.groupObjects != null) {
            this.groupObjects.forEach(object -> {
                if (object instanceof Line) {
                    Line line = (Line) object;
                    line.setColorMap(colorMap);
                }
            });
        }
        return this;
    }

    @Override
    public String getHoverTemplate() {
        return hoverTemplate;
    }

    @Override
    public ObjectGroup setHoverTemplate(String hoverTemplate) {
        this.hoverTemplate = hoverTemplate;

        this.groupObjects.forEach(object -> {
            updateHoverTemplate(object);
        });

        return this;
    }

    @Override
    public ObjectGroup addPlotObject(final PlotObject object) {
        PlotObject newObj;
        if (object instanceof BasicLine) {
            newObj = new BasicLine((BasicLine) object);
        } else if (object instanceof Rectangle) {
            newObj = new Rectangle((Rectangle) object);
        } else if (object instanceof VerticalLine) {
            newObj = new VerticalLine((VerticalLine) object);
        } else if (object instanceof BasicSymbol) {
            newObj = new BasicSymbol((BasicSymbol) object);
        } else {
            return this; // Don't add if it is not one of the above objects
        }

        newObj.setName("GroupObj" + groupObjects.size());
        originalObjects.put(newObj.getName(), object); // Save original object
        newObj.setLegendGrouping(groupName);
        newObj.setLegendOnly(false);
        newObj.showInLegend(false);

        updateHoverTemplate(newObj);
        this.groupObjects.add(newObj);
        return this;
    }

    private void updateHoverTemplate(PlotObject object) {
        if (this.getHoverTemplate() != null) {
            PlotObject original = originalObjects.get(object.getName());
            if (object instanceof Line) {
                if (this.getHoverTemplate() != null) {
                    ((Line) object).setHoverTemplate(this.getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else if (((Line) original).getHoverTemplate() != null) {
                    ((Line) object).setHoverTemplate(((Line) original).getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else {
                    ((Line) object).setHoverTemplate("(%{x}, %{y})<extra>" + hoverName + "</extra>");
                }
            } else if (object instanceof Rectangle) {
                if (this.getHoverTemplate() != null) {
                    ((Rectangle) object).setHoverTemplate(this.getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else if (((Rectangle) original).getHoverTemplate() != null) {
                    ((Rectangle) object).setHoverTemplate(((Rectangle) original).getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else {
                    ((Rectangle) object).setHoverTemplate("(%{x}, %{y})<extra>" + hoverName + "</extra>");
                }
            } else if (object instanceof VerticalLine) {
                if (this.getHoverTemplate() != null) {
                    ((VerticalLine) object).setHoverTemplate(this.getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else if (((VerticalLine) original).getHoverTemplate() != null) {
                    ((VerticalLine) object).setHoverTemplate(((VerticalLine) original).getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else {
                    ((VerticalLine) object).setHoverTemplate("(%{x}, %{y})<extra>" + hoverName + "</extra>");
                }
            } else if (object instanceof BasicSymbol) {
                if (this.getHoverTemplate() != null) {
                    ((BasicSymbol) object).setHoverTemplate(this.getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else if (((BasicSymbol) original).getHoverTemplate() != null) {
                    ((BasicSymbol) object).setHoverTemplate(((BasicSymbol) original).getHoverTemplate() + "<extra>" + hoverName + "</extra>");
                } else {
                    ((BasicSymbol) object).setHoverTemplate("(%{x}, %{y})<extra>" + hoverName + "</extra>");
                }
            }
        }
    }

    @Override
    public PlotObject getPlotObject(int index) {
        if (index < 0 || index >= this.groupObjects.size()) {
            return null;
        }
        return this.groupObjects.get(index);
    }

    @Override
    public PlotObject getLegendObject() {
        return legendObject;
    }

    @Override
    public List<PlotObject> getPlotObjects() {
        return this.groupObjects;
    }

    @Override
    public ObjectGroup setPlotObjects(PlotObject... objects) {
        for (PlotObject object : objects) {
            this.addPlotObject(object);
        }
        return this;
    }

    @Override
    public void plotGroup(BasicPlot plot) {
        if (!plotted) {
            for (PlotObject object : this.groupObjects) {
                plot.addPlotObject(object);
            }
            plot.addPlotObject(this.legendObject);

            this.plotted = true;
        }
    }

    @Override
    public void removeGroupFromPlot(BasicPlot plot) {
        if (this.plotted) {
            if (this.legendObject != null) {
                plot.removePlotObject(this.legendObject);
            }
            for (PlotObject object : this.groupObjects) {
                plot.removePlotObject(object);
            }
            this.plotted = false;
        }
    }

    @Override
    public List<PlotObject> clearGroup() {
        if (this.legendObject != null) {
            this.legendObject = null;
        }
        this.groupObjects.clear();
        return this.originalObjects.values().stream().collect(Collectors.toList());
    }

    @Override
    public int hashCode() {
        return Objects.hash(colorMap, groupEdgeColor, groupFillColor, groupLineStyle, groupName, hoverName, hoverTemplate, legendObject, groupObjects, originalObjects, plotted, pxThickness);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof BasicObjectGroup)) {
            return false;
        }
        BasicObjectGroup other = (BasicObjectGroup) obj;
        return Objects.equals(colorMap, other.colorMap)
                && Objects.equals(groupEdgeColor, other.groupEdgeColor)
                && Objects.equals(groupFillColor, other.groupFillColor)
                && groupLineStyle == other.groupLineStyle
                && Objects.equals(groupName, other.groupName)
                && Objects.equals(hoverName, other.hoverName)
                && Objects.equals(hoverTemplate, other.hoverTemplate)
                && Objects.equals(legendObject, other.legendObject)
                && Objects.equals(groupObjects, other.groupObjects)
                && Objects.equals(originalObjects, other.originalObjects)
                && plotted == other.plotted
                && pxThickness == other.pxThickness;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BasicObjectGroup [legendObject=")
               .append(legendObject)
               .append(", objects=")
               .append(groupObjects)
               .append(", originalObjects=")
               .append(originalObjects)
               .append(", groupName=")
               .append(groupName)
               .append(", hoverName=")
               .append(hoverName)
               .append(", groupEdgeColor=")
               .append(groupEdgeColor)
               .append(", groupFillColor=")
               .append(groupFillColor)
               .append(", colorMap=")
               .append(colorMap)
               .append(", hoverTemplate=")
               .append(hoverTemplate)
               .append(", groupLineStyle=")
               .append(groupLineStyle)
               .append(", pxThickness=")
               .append(pxThickness)
               .append(", plotted=")
               .append(plotted)
               .append("]");
        return builder.toString();
    }
}
