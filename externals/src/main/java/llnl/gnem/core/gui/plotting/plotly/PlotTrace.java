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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.util.RawValue;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.fx.utils.FxUtils;

public class PlotTrace {

    private static final String COLOR = "color";
    private static final String WIDTH = "width";
    private static final String LINE = "line";
    private static final String MARKER = "marker";
    private static final String CONTOUR = "contours";
    private final Style type;
    private Color fillColor;
    private Color edgeColor;
    private Integer pxSize;
    private String styleName;
    private String seriesName;
    private final ObjectMapper mapper;
    private String colorMap;
    private String colorBarTitle = "";
    private Double colorBarX = 0.0;
    private Double colorBarY = 0.3;
    private Double colorBarLength = 0.7;
    private Integer zIndex;
    private Boolean showLegend;
    private String legendGroup;
    private String hoverTemplate;
    private Boolean legendOnly;
    private Boolean draggable;
    private String xAxisId;
    private String yAxisId;
    private boolean isAnnotationLogX = false;

    public enum Style {
        LINE("lines", "scatter", 0), SCATTER_MARKER_AND_LINE("lines+markers", "scatter", 1), SCATTER_MARKER("markers", "scatter", 2), VERTICAL_LINE("line", "shapes", 3), HEATMAP("heatmap", "heatmap",
                4), CONTOUR("contour", "contour",
                        5), HISTOGRAM_2D_CONTOUR("histogram2dcontour", "histogram2dcontour", 5), HISTOGRAM_X("histogram_x", "histogram", 6), HISTOGRAM_Y("histogram_y", "histogram", 6);

        private final String mode;
        private final String type;
        private final int order;

        private Style(final String mode, final String type, final int order) {
            this.mode = mode;
            this.type = type;
            this.order = order;
        }

        public String getMode() {
            return mode;
        }

        public String getType() {
            return type;
        }

        public int getOrder() {
            return order;
        }
    }

    public PlotTrace(final Style type) {
        this.type = type;
        this.mapper = new ObjectMapper();
    }

    public Style getType() {
        return type;
    }

    public void setFillColor(final Color fillColor) {
        this.fillColor = fillColor;
    }

    public void setEdgeColor(final Color edgeColor) {
        this.edgeColor = edgeColor;
    }

    public void setStyleName(final String styleName) {
        this.styleName = styleName;
    }

    public Integer getPxSize() {
        return pxSize;
    }

    public void setPxSize(final Integer pxSize) {
        this.pxSize = pxSize;
    }

    public String getSeriesName() {
        return seriesName;
    }

    public void setSeriesName(final String seriesName) {
        this.seriesName = seriesName;
    }

    public Color getFillColor() {
        return fillColor;
    }

    public Color getEdgeColor() {
        return edgeColor;
    }

    public String getStyleName() {
        return styleName;
    }

    public void setShowLegend(final Boolean showLegend) {
        this.showLegend = showLegend;
    }

    public void setLegendGroup(final String legendGroup) {
        this.legendGroup = legendGroup;
    }

    public Integer getzIndex() {
        return zIndex;
    }

    public void setzIndex(final Integer zIndex) {
        this.zIndex = zIndex;
    }

    public void setColorMap(final String colorMap) {
        this.colorMap = colorMap;
    }

    public String getColorMap() {
        return colorMap;
    }

    public String getColorBarTitle() {
        return colorBarTitle;
    }

    public void setColorBarTitle(String colorBarTitle) {
        this.colorBarTitle = colorBarTitle;
    }

    public Double getColorBarX() {
        return colorBarX;
    }

    public void setColorBarX(Double colorBarX) {
        this.colorBarX = colorBarX;
    }

    public Double getColorBarY() {
        return colorBarY;
    }

    public void setColorBarY(Double colorBarY) {
        this.colorBarY = colorBarY;
    }

    public Double getColorBarLength() {
        return colorBarLength;
    }

    public void setColorBarLength(Double colorBarLength) {
        this.colorBarLength = colorBarLength;
    }

    public void setLegendOnly(final Boolean legendOnly) {
        this.legendOnly = legendOnly;
    }

    public void setDraggable(final Boolean draggable) {
        this.draggable = draggable;
    }

    public Boolean isDraggable() {
        return draggable;
    }

    public void setXaxisId(String xAxisId) {
        this.xAxisId = xAxisId;
    }

    public String getXaxisId() {
        return xAxisId;
    }

    public void setYaxisId(String yAxisId) {
        this.yAxisId = yAxisId;
    }

    public String getYaxisId() {
        return yAxisId;
    }

    public boolean isAnnotationLogX() {
        return isAnnotationLogX;
    }

    public void setAnnotationLogX(boolean isAnnotationLogX) {
        this.isAnnotationLogX = isAnnotationLogX;
    }

    public String toJson() {
        return getJSONObject().toString();
    }

    public ObjectNode getJSONObject() {
        final ObjectNode node = mapper.createObjectNode();
        node.put("mode", type.getMode());
        node.put("type", type.getType());

        if (seriesName != null) {
            node.put("name", seriesName);
        }

        if (xAxisId != null) {
            node.put("xaxis", xAxisId);
        }

        if (yAxisId != null) {
            node.put("yaxis", yAxisId);
        }

        if (colorMap != null) {
            switch (type) {
            case CONTOUR:
            case HEATMAP:
            case HISTOGRAM_2D_CONTOUR:
                if (colorMap != null && colorMap.startsWith("[[")) {
                    node.putRawValue("colorscale", new RawValue(colorMap));
                } else {
                    node.put("colorscale", colorMap);
                }
                node.withObjectProperty("colorbar").put("y", colorBarY);
                node.withObjectProperty("colorbar").put("x", colorBarX);
                node.withObjectProperty("colorbar").put("len", colorBarLength);
                node.withObjectProperty("colorbar").withObjectProperty("title").put("text", colorBarTitle);
                node.put("showscale", true);
                node.put("reversescale", true);
                node.withObjectProperty(CONTOUR).put("coloring", "heatmap");
                if (colorMap != null && colorMap.startsWith("[[")) {
                    node.withObjectProperty(MARKER).putRawValue("colorscale", new RawValue(colorMap));
                } else {
                    node.withObjectProperty(MARKER).put("colorscale", colorMap);
                }
                node.withObjectProperty(MARKER).put("showscale", true);
                node.withObjectProperty(MARKER).put("reversescale", true);
                break;
            case SCATTER_MARKER_AND_LINE:
            case SCATTER_MARKER:
                if (colorMap != null && colorMap.startsWith("[[")) {
                    node.withObjectProperty(MARKER).putRawValue("colorscale", new RawValue(colorMap));
                } else {
                    node.withObjectProperty(MARKER).put("colorscale", colorMap);
                }
                node.withObjectProperty(MARKER).put("showscale", false);
                node.withObjectProperty(MARKER).put("reversescale", true);
                break;
            case VERTICAL_LINE:
            case LINE:
                node.withObjectProperty(MARKER).withObjectProperty(LINE).put("colorscale", colorMap);
                break;
            default:
                break;
            }
        }

        if (fillColor != null) {
            node.withObjectProperty(MARKER).put(COLOR, FxUtils.toWebHexColorString(fillColor));
        }
        if (edgeColor != null) {
            node.withObjectProperty(MARKER).withObjectProperty(LINE).put(COLOR, FxUtils.toWebHexColorString(edgeColor));
            node.withObjectProperty(MARKER).withObjectProperty(LINE).put(WIDTH, 1);
        }
        if (showLegend != null) {
            node.put("showlegend", showLegend);
        }
        if (legendGroup != null) {
            node.put("legendgroup", legendGroup);
        }
        if (hoverTemplate != null) {
            node.put("hovertemplate", hoverTemplate);
        }
        if (Boolean.TRUE.equals(legendOnly)) {
            if (legendGroup != null) {
                node.put("visible", true);
            } else {
                node.put("visible", "legendonly");
            }
        }
        if (pxSize != null) {
            switch (type) {
            case SCATTER_MARKER_AND_LINE:
                node.withObjectProperty(MARKER).put("size", pxSize);
                node.withObjectProperty(LINE).put(WIDTH, 3);
                break;
            case SCATTER_MARKER:
                node.withObjectProperty(MARKER).put("size", pxSize);
                break;
            case VERTICAL_LINE:
            case LINE:
                node.withObjectProperty(LINE).put(WIDTH, pxSize);
                break;
            default:
                break;
            }
        }
        if (styleName != null) {
            switch (type) {
            case SCATTER_MARKER_AND_LINE:
                node.withObjectProperty(LINE).put("dash", "solid");
                node.withObjectProperty(MARKER).put("symbol", styleName);
                break;
            case SCATTER_MARKER:
                node.withObjectProperty(MARKER).put("symbol", styleName);
                break;
            case VERTICAL_LINE:
            case LINE:
                node.withObjectProperty(LINE).put("dash", styleName);
                break;
            default:
                break;
            }
        }

        if (type.getType().equals("shapes")) {
            if (edgeColor != null) {
                node.withObjectProperty(LINE).put(COLOR, FxUtils.toWebHexColorString(edgeColor));
            }
            if (fillColor != null) {
                node.put("fillcolor", FxUtils.toWebHexColorString(fillColor));
            }
        }
        return node;
    }
}
