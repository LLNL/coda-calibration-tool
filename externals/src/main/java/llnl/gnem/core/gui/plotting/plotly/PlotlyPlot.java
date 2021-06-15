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
package llnl.gnem.core.gui.plotting.plotly;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.Title;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;
import llnl.gnem.core.gui.plotting.events.PlotShapeMove;
import llnl.gnem.core.gui.plotting.fx.utils.FxUtils;
import llnl.gnem.core.gui.plotting.fx.utils.MouseEventHelpers;
import llnl.gnem.core.gui.plotting.plotly.PlotlyTrace.Style;

public class PlotlyPlot implements BasicPlot {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PlotlyPlot.class);
    private final PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);

    private transient WebView webView;
    private transient WebEngine engine;
    private transient StackPane view;
    private static final String SHAPES = "shapes";
    private final PlotlyPlotData plotData = new PlotlyPlotData(new PlotlyTrace(PlotlyTrace.Style.SCATTER_MARKER), Color.WHITE, new BasicTitle());
    private final List<PlotlyPlot> subPlots = new ArrayList<>(0);
    private Integer subplotId;
    private Integer topMargin;
    private Integer bottomMargin;
    private Integer leftMargin;
    private Integer rightMargin;

    private transient String cachedPlotData = "";
    private transient String cachedPlotLayout = "";

    private AtomicBoolean hasChanges = new AtomicBoolean(true);
    private AtomicBoolean hasPersistentChanges = new AtomicBoolean(true);

    private transient Comparator<Integer> nullsLastIntComparator = Comparator.nullsLast(Integer::compare);
    private boolean isSubPlot;

    public PlotlyPlot() {
        this(false);
    }

    private PlotlyPlot(boolean isSubPlot) {
        this.isSubPlot = isSubPlot;
        //TODO: Accept other plot types
        intializePlotData();
        if (!isSubPlot) {
            initializeView();
        }
    }

    private void intializePlotData() {
        plotData.setMapper(new ObjectMapper());
        plotData.setShowLegend(true);
        plotData.setAxes(new ArrayList<>(0));
        plotData.setPlotReady(new AtomicBoolean(false));
        plotData.setDefaultTypePlots(new HashMap<>());
    }

    private void initializeView() {
        view = new StackPane();
        view.setMaxSize(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Platform.runLater(() -> {
            webView = new WebView();
            view.getChildren().add(webView);
            webView.setContextMenuEnabled(false);
            engine = webView.getEngine();
            engine.setJavaScriptEnabled(true);

            final Worker<Void> worker = engine.getLoadWorker();
            worker.stateProperty().addListener((obs, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    final netscape.javascript.JSObject wind = (netscape.javascript.JSObject) engine.executeScript("window");
                    wind.setMember("plotTitle", getTitle());
                    wind.setMember("backgroundColor", getBackgroundColor());
                    wind.setMember("plotData", this);
                    wind.setMember("logBridge", this);
                    engine.executeScript("console.log = function(message)\n" + "{\n" + " logBridge.log(message);\n" + "};");
                    plotData.getPlotReady().set(true);
                    replot();
                }
            });

            engine.load(getClass().getResource("/plotly.html").toExternalForm());
        });
    }

    public void log(final String text) {
        log.error(text);
    }

    public boolean hasChanges() {
        return hasChanges.get();
    }

    public boolean hasPersistentChanges() {
        return hasPersistentChanges.get();
    }

    public void fireShapeMoveEvent(final String name, final double x0, final double x1, final double y0, final double y1) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "shape_move", null, new PlotShapeMove(name, x0, x1, y0, y1)));
            }
        });
    }

    public void fireSelectionEvent(final String eventType, final String dataJSON) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                try {
                    PlotPoints points = plotData.getMapper().readerFor(PlotPoints.class).readValue(dataJSON);

                    double[] dataX = points.getX();
                    double[] dataY = points.getY();
                    double[] screenX = points.getScreenX();
                    double[] screenY = points.getScreenY();
                    if (dataX != null && dataY != null && dataX.length == dataY.length && dataX.length > 0) {
                        List<Point2D> points2D = new ArrayList<>(dataX.length);
                        for (int i = 0; i < dataX.length; i++) {
                            points2D.add(new Point2D(dataX[i], dataY[i]));
                        }
                        if (webView != null) {
                            MouseButton button = MouseButton.PRIMARY;
                            if ("right_click".equalsIgnoreCase(eventType)) {
                                button = MouseButton.SECONDARY;
                            }
                            Bounds bounds = webView.localToScreen(webView.getLayoutBounds());
                            double clickX = bounds.getCenterX();
                            double clickY = bounds.getCenterY();
                            if (screenX != null) {
                                clickX = screenX[0] + bounds.getMinX();
                                clickY = screenY[0] + bounds.getMinY();
                            }
                            propertyChange.firePropertyChange(new PropertyChangeEvent(this, eventType, null, new PlotObjectClick(MouseEventHelpers.newClickEvent(button, clickX, clickY), points2D)));
                        }
                    }
                } catch (JacksonException e) {
                    log.info(e.getLocalizedMessage(), e);
                }
            }
        });
    }

    @Override
    public void replot() {
        if (plotData.getPlotReady().get() && !isSubPlot) {
            CompletableFuture.runAsync(() -> {
                rebuildLayoutCache();
                rebuildDataCache();
                Platform.runLater(() -> {
                    try {
                        engine.executeScript("updatePlot();");
                        hasChanges.set(false);
                        hasPersistentChanges.set(false);
                        subPlots.forEach(sub -> {
                            sub.hasChanges.set(false);
                            sub.hasPersistentChanges.set(false);
                        });
                    } catch (final Exception e) {
                        log.debug(e.getLocalizedMessage());
                    }
                });
            });
        }
    }

    @Override
    public void addAxes(Axis... axes) {
        plotData.getAxes().addAll(Arrays.asList(axes));
        hasPersistentChanges.set(true);
    }

    @Override
    public void clearAxes() {
        plotData.getAxes().clear();
        hasPersistentChanges.set(true);
    }

    @Override
    public void attachToDisplayNode(final Pane parent) {
        if (parent != null && (view.getParent() == null || !view.getParent().equals(parent))) {
            parent.getChildren().add(view);
            view.prefHeightProperty().bind(parent.heightProperty());
            view.prefWidthProperty().bind(parent.widthProperty());
        }
    }

    @Override
    public void setBackgroundColor(final Color color) {
        if (color != null) {
            this.plotData.setBackgroundColor(color);
        }
    }

    public Color getBackgroundColor() {
        return plotData.getBackgroundColor();
    }

    @Override
    public void setSymbolSize(final int pxSymbolSize) {
        plotData.getDefaultTraceStyle().setPxSize(pxSymbolSize);
    }

    @Override
    public Title getTitle() {
        return plotData.getPlotTitle();
    }

    @Override
    public void addPlotObjectObserver(final PropertyChangeListener observer) {
        propertyChange.addPropertyChangeListener(observer);
    }

    @Override
    public void setAxisLimits(final AxisLimits... axisLimits) {
        for (final Axis axis : plotData.getAxes()) {
            for (final AxisLimits limit : axisLimits) {
                if (axis.getAxisType().equals(limit.getAxis())) {
                    axis.setMin(limit.getMin());
                    axis.setMax(limit.getMax());
                }
            }
        }
        hasPersistentChanges.set(true);
    }

    @Override
    public void addPlotObject(final PlotObject object) {
        addPlotObject(object, plotData);
    }

    protected synchronized void addPlotObject(final PlotObject object, final PlotlyPlotData plot) {
        final PlotObjectData data = plot.getDefaultTypePlots().computeIfAbsent(object.getSeriesIdentifier(), k -> new PlotObjectData());
        if (object instanceof VerticalLine) {
            final VerticalLine vline = (VerticalLine) object;
            final List<Double> ydata = data.getYdata();
            ydata.clear();

            final double delta = (1.0 - (vline.getRatioY() / 100.0)) / 2.0;
            ydata.add(1.0 - delta);
            ydata.add(delta);

            final List<Double> xdata = data.getXdata();
            xdata.clear();
            xdata.add(vline.getX());
            xdata.add(vline.getX());

            if (data.getTraceStyle() == null) {
                final PlotlyTrace traceStyle = populateStyle(vline, Style.VERTICAL_LINE, plot);
                traceStyle.setPxSize(vline.getPxWidth());
                traceStyle.setStyleName(LineStyles.SOLID.getStyleName());
                traceStyle.setDraggable(vline.isDraggable());
                traceStyle.setSeriesName(vline.getText());
                traceStyle.setAnnotationLogX(vline.isLogScaleX());
                data.setTraceStyle(traceStyle);
            }
        } else if (object instanceof Line) {
            final Line line = (Line) object;
            final List<Double> xdata = data.getXdata();
            xdata.clear();
            xdata.addAll(Arrays.stream(line.getX()).boxed().collect(Collectors.toList()));

            final List<Double> ydata = data.getYdata();
            ydata.clear();
            ydata.addAll(Arrays.stream(line.getY()).boxed().collect(Collectors.toList()));

            final List<Double> cdata = data.getColorData();
            cdata.clear();
            cdata.addAll(Arrays.stream(line.getColor()).boxed().collect(Collectors.toList()));

            if (data.getTraceStyle() == null) {
                final PlotlyTrace traceStyle = populateStyle(line, Style.LINE, plot);
                traceStyle.setPxSize(line.getPxThickness());
                traceStyle.setStyleName(line.getStyle().getStyleName());
                traceStyle.setColorMap(line.getColorMap());
                data.setTraceStyle(traceStyle);
            }
        } else if (object instanceof Symbol) {
            final Symbol symbol = (Symbol) object;
            data.getXdata().add(symbol.getX());
            data.getYdata().add(symbol.getY());
            if (symbol.getText() != null) {
                data.getTextData().add(symbol.getText());
            }
            if (symbol.getColorationValue() != null) {
                data.getColorData().add(symbol.getColorationValue());
            }
            if (data.getTraceStyle() == null) {
                final PlotlyTrace traceStyle = populateStyle(symbol, Style.SCATTER_MARKER, plot);
                traceStyle.setPxSize(plot.getDefaultTraceStyle().getPxSize());
                traceStyle.setStyleName(symbol.getStyle().getStyleName());
                traceStyle.setColorMap(symbol.getColorMap());
                data.setTraceStyle(traceStyle);
            }
        }
        hasChanges.set(true);
    }

    private PlotlyTrace populateStyle(final PlotObject object, final Style style, final PlotlyPlotData plot) {
        final PlotlyTrace traceStyle = new PlotlyTrace(style);
        traceStyle.setColorMap(plot.getDefaultTraceStyle().getColorMap());
        traceStyle.setLegendOnly(object.getLegendOnly());
        traceStyle.setLegendGroup(object.getLegendGrouping());
        traceStyle.setShowLegend(object.shouldShowInLegend());
        traceStyle.setSeriesName(object.getName());
        traceStyle.setFillColor(object.getFillColor());
        traceStyle.setEdgeColor(object.getEdgeColor());
        traceStyle.setzIndex(object.getZindex());
        return traceStyle;
    }

    @Override
    public void removePlotObject(PlotObject object) {
        removePlotObject(object, plotData);
    }

    protected synchronized void removePlotObject(PlotObject object, PlotlyPlotData plot) {
        final PlotObjectData data = plot.getDefaultTypePlots().computeIfAbsent(object.getSeriesIdentifier(), k -> new PlotObjectData());
        if (object instanceof VerticalLine || object instanceof Line) {
            plot.getDefaultTypePlots().remove(object.getSeriesIdentifier());
        } else if (object instanceof Symbol) {
            Symbol sym = (Symbol) object;
            if (data.getXdata() != null && data.getYdata() != null && !data.getXdata().isEmpty() && data.getXdata().size() == data.getYdata().size()) {
                for (int i = 0; i < data.getXdata().size(); i++) {
                    if (data.getXdata().get(i) == sym.getX() && data.getYdata().get(i) == sym.getY()) {
                        data.getXdata().remove(i);
                        data.getYdata().remove(i);
                        if (data.getColorData() != null && !data.getColorData().isEmpty()) {
                            data.getColorData().remove(i);
                        }
                        break;
                    }
                }
            }
        }
        subPlots.forEach(sub -> sub.removePlotObject(object));
        hasChanges.set(true);
    }

    @Override
    public void showLegend(final boolean showLegend) {
        this.plotData.setShowLegend(showLegend);
    }

    @Override
    public void clear() {
        plotData.getDefaultTypePlots().clear();
        hasChanges.set(true);
        hasPersistentChanges.set(true);
        replot();
    }

    @Override
    public String getSVG() {
        return (String) engine.executeScript("getSvg();");
    }

    private List<PlotObjectData> getOrderedPlots(final Collection<PlotObjectData> values) {
        final List<PlotObjectData> orderedPlots = new ArrayList<>(values);
        Collections.sort(orderedPlots, (o1, o2) -> {
            int cmp = 0;
            if (o1.getTraceStyle() != null) {
                if (o2.getTraceStyle() != null) {
                    cmp = nullsLastIntComparator.compare(o1.getTraceStyle().getType().getOrder(), o2.getTraceStyle().getType().getOrder());
                    if (cmp == 0) {
                        cmp = nullsLastIntComparator.compare(o1.getTraceStyle().getzIndex(), o2.getTraceStyle().getzIndex());
                        if (cmp == 0) {
                            cmp = StringUtils.compare(o1.getTraceStyle().getSeriesName(), o2.getTraceStyle().getSeriesName());
                        }
                    }
                } else {
                    cmp = 1;
                }
            } else if (o2.getTraceStyle() != null) {
                cmp = -1;
            }
            return cmp;
        });
        return orderedPlots;
    }

    public String getPlotDataJSON() {
        return cachedPlotData;
    }

    private synchronized void rebuildDataCache() {
        if (hasChanges.get() || subPlots.parallelStream().anyMatch(PlotlyPlot::hasChanges)) {
            final ArrayNode traceNodes = getPlotDataNode();
            cachedPlotData = traceNodes.toString();
        }
    }

    private ArrayNode getPlotDataNode() {
        final ArrayNode traceNodes = plotData.getMapper().createArrayNode();

        if (!subPlots.isEmpty()) {
            for (int i = 0; i < subPlots.size(); i++) {
                PlotlyPlot subPlot = subPlots.get(i);
                if (i > 0) {
                    subPlot.setSubPlotId((i + 1));
                }
                ArrayNode subNodeData = subPlot.getPlotDataNode();
                traceNodes.addAll(subNodeData);
            }
        } else {
            final List<PlotObjectData> orderedPlots = getOrderedPlots(plotData.getDefaultTypePlots().values());

            for (final PlotObjectData data : orderedPlots) {
                if (data.getTraceStyle() != null && data.getTraceStyle().getType() != null && data.getTraceStyle().getType().getType().equals(SHAPES)) {
                    //Skip these since they go in layout.shapes
                    continue;
                }
                final ObjectNode trace = Optional.ofNullable(data.getTraceStyle()).orElse(plotData.getDefaultTraceStyle()).getJSONObject();

                if (subplotId != null && data.getTraceStyle().getXaxisId() == null) {
                    trace.put("xaxis", "x" + subplotId);
                }
                if (subplotId != null && data.getTraceStyle().getYaxisId() == null) {
                    trace.put("yaxis", "y" + subplotId);
                }

                final List<Double> xdata = data.getXdata();
                if (!xdata.isEmpty()) {
                    final ArrayNode xNode = trace.arrayNode();
                    xdata.forEach(xNode::add);
                    trace.set("x", xNode);
                }

                final List<Double> ydata = data.getYdata();
                if (!ydata.isEmpty()) {
                    final ArrayNode yNode = trace.arrayNode();
                    ydata.forEach(yNode::add);
                    trace.set("y", yNode);
                }

                final List<Double> cData = data.getColorData();
                if (!cData.isEmpty()) {
                    final ArrayNode cNode = trace.arrayNode();
                    cData.forEach(cNode::add);
                    trace.with("marker").set("color", cNode);
                }

                final List<String> textData = data.getTextData();
                if (!textData.isEmpty()) {
                    final ArrayNode textNode = trace.arrayNode();
                    textData.forEach(textNode::add);
                    trace.set("text", textNode);
                }

                traceNodes.add(trace);
            }
        }
        return traceNodes;
    }

    private void setSubPlotId(int id) {
        this.subplotId = id;
    }

    public String getPlotLayoutJSON() {
        return cachedPlotLayout;
    }

    private synchronized void rebuildLayoutCache() {
        if (hasPersistentChanges.get() || subPlots.parallelStream().anyMatch(PlotlyPlot::hasPersistentChanges)) {
            final ObjectNode layoutNode = getPlotLayoutNode();
            cachedPlotLayout = layoutNode.toString();
        }
    }

    private ObjectNode getPlotLayoutNode() {
        ObjectNode layoutNode = plotData.getMapper().createObjectNode();
        layoutNode.put("hovermode", "closest");
        if (!subPlots.isEmpty()) {
            layoutNode.with("grid").put("rows", subPlots.size()).put("columns", 1).put("pattern", "independent");
            for (int i = 0; i < subPlots.size(); i++) {
                PlotlyPlot subPlot = subPlots.get(i);
                if (i > 0) {
                    subPlot.setSubPlotId((i + 1));
                }
                ObjectNode subLayout = subPlot.getPlotLayoutNode();
                layoutNode.setAll(subLayout);
            }
        } else {
            layoutNode.put("showlegend", plotData.isShowLegend());
            for (final Axis axis : plotData.getAxes()) {
                String axisType;
                String axisSide = null;
                String dataType = null;
                String overlaying = null;
                String anchor = null;
                switch (axis.getAxisType()) {
                case LOG_X:
                    dataType = "log";
                case X:
                    axisType = "xaxis";
                    break;
                case Y:
                    axisType = "yaxis";
                    break;
                case Y_RIGHT:
                    axisType = "yaxis2";
                    axisSide = "right";
                    overlaying = "y";
                    anchor = "y";
                    break;
                default:
                    axisType = null;
                    break;
                }

                if (axisType != null) {
                    if (subplotId != null) {
                        axisType = axisType + subplotId;
                    }
                    layoutNode.with(axisType).put("zeroline", false);
                    layoutNode.with(axisType).put("hoverformat", ".2f");
                    layoutNode.with(axisType).with("title").put("text", axis.getText()).with("font").put("size", 12);
                    layoutNode.with(axisType).put("linecolor", "black");
                    layoutNode.with(axisType).put("linewidth", 1.25);
                    layoutNode.with(axisType).put("mirror", true);

                    if (dataType != null) {
                        layoutNode.with(axisType).put("type", dataType);
                    }

                    if (axisSide != null) {
                        layoutNode.with(axisType).put("side", axisSide);
                    }
                    if (axis.getMin() != axis.getMax()) {
                        final ArrayNode range = plotData.getMapper().createArrayNode();
                        range.add(axis.getMin());
                        range.add(axis.getMax());
                        layoutNode.with(axisType).set("range", range);
                    } else {
                        layoutNode.with(axisType).remove("range");
                    }

                    if (axis.getTickFormat() != null) {
                        layoutNode.with(axisType).put("tickformat", axis.getTickFormat().getFormat());
                        layoutNode.with(axisType).put("hoverformat", axis.getTickFormat().getFormat());
                    }

                    if (overlaying != null) {
                        layoutNode.with(axisType).put("overlaying", overlaying);
                    }
                    if (anchor != null) {
                        layoutNode.with(axisType).put("anchor", anchor);
                    }
                }
            }

            ObjectNode margin = layoutNode.with("margin");
            if (topMargin != null) {
                margin.put("t", topMargin);
            }
            if (bottomMargin != null) {
                margin.put("b", bottomMargin);
            }
            if (leftMargin != null) {
                margin.put("l", leftMargin);
            }
            if (rightMargin != null) {
                margin.put("r", rightMargin);
            }

            final List<PlotObjectData> orderedPlots = getOrderedPlots(plotData.getDefaultTypePlots().values());

            final ArrayNode shapes = plotData.getMapper().createArrayNode();
            final ArrayNode annotations = plotData.getMapper().createArrayNode();

            for (final PlotObjectData data : orderedPlots) {
                if (data.getTraceStyle() != null && data.getTraceStyle().getType() != null && data.getTraceStyle().getType().getType().equals(SHAPES)) {
                    final PlotlyTrace style = data.getTraceStyle();
                    final ObjectNode shapeNode = style.getJSONObject();
                    final ObjectNode annotationNode = plotData.getMapper().createObjectNode();

                    //TODO only works for the vertical line setup, eventually needs a rework
                    final List<Double> xVvalues = data.getXdata();
                    if (xVvalues != null && xVvalues.size() == 2) {
                        String xAxis = "x";
                        if (subplotId != null) {
                            xAxis = xAxis + subplotId;
                        }
                        shapeNode.put("x0", xVvalues.get(0));
                        shapeNode.put("x1", xVvalues.get(1));
                        shapeNode.put("xref", xAxis);
                        final List<Double> yVvalues = data.getYdata();
                        if (yVvalues != null && yVvalues.size() == 2) {
                            shapeNode.put("y0", yVvalues.get(0)).put("y1", yVvalues.get(1)).put("yref", "paper").put("ysizemode", "scaled");
                            if (Boolean.TRUE.equals(style.isDraggable())) {
                                shapeNode.put("editable", true);
                            }
                            double xVal = xVvalues.get(0);
                            if (style.isAnnotationLogX()) {
                                xVal = Math.log10(xVal);
                            }
                            annotationNode.put("yref", "paper")
                                          .put("yshift", "-20")
                                          .put("ysizemode", "scaled")
                                          .put("y", yVvalues.get(1))
                                          .put("xref", xAxis)
                                          .put("x", xVal)
                                          .put("showarrow", false)
                                          .put("text", style.getSeriesName())
                                          .with("font")
                                          .put("color", FxUtils.toWebHexColorString(style.getFillColor()));
                        }
                        shapes.add(shapeNode);
                        annotations.add(annotationNode);
                    }
                }
            }
            layoutNode.set(SHAPES, shapes);
            layoutNode.set("annotations", annotations);
        }
        return layoutNode;
    }

    @Override
    public void setColorMap(final String colorMap) {
        plotData.getDefaultTraceStyle().setColorMap(colorMap);
    }

    @Override
    public BasicPlot createSubPlot() {
        PlotlyPlot subPlot = new PlotlyPlot(true);
        subPlots.add(subPlot);
        hasPersistentChanges.set(true);
        return subPlot;
    }

    /**
     * Defines the margin for the plot, in pixels. Null parameters are left at
     * default values.
     *
     * @param top
     * @param bottom
     * @param left
     * @param right
     */
    public void setMargin(Integer top, Integer bottom, Integer left, Integer right) {
        this.topMargin = top;
        this.bottomMargin = bottom;
        this.leftMargin = left;
        this.rightMargin = right;
        hasPersistentChanges.set(true);
    }
}
