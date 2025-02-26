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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Worker;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.FillModes;
import llnl.gnem.core.gui.plotting.api.HoverModes;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.Rectangle;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.Title;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
import llnl.gnem.core.gui.plotting.events.PlotAnnotationMove;
import llnl.gnem.core.gui.plotting.events.PlotAxisChange;
import llnl.gnem.core.gui.plotting.events.PlotFreqLevelChange;
import llnl.gnem.core.gui.plotting.events.PlotMouseEvent;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;
import llnl.gnem.core.gui.plotting.events.PlotShapeMove;
import llnl.gnem.core.gui.plotting.fx.utils.FxUtils;
import llnl.gnem.core.gui.plotting.fx.utils.MouseEventHelpers;
import llnl.gnem.core.gui.plotting.plotly.PlotTrace.Style;

public class PlotlyPlot implements BasicPlot {

    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(PlotlyPlot.class);
    private final PropertyChangeSupport propertyChange = new PropertyChangeSupport(this);

    private transient WebView webView;
    protected transient WebEngine engine;
    private transient StackPane view;
    private static final String SHAPES = "shapes";
    protected final PlotData plotData;
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
    private int rows = 1;
    private int columns = 1;
    private boolean clickToPickEnabled = false;
    private boolean showFreqLevelButtons = false;

    private static FileChooser chooser = null;
    private static final SimpleObjectProperty<File> lastKnownDirectoryProperty = new SimpleObjectProperty<>();
    private static final Object fileChooserLock = new Object();

    public PlotlyPlot() {
        this(false, new PlotData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER), Color.WHITE, new BasicTitle()), 1, 1);
    }

    public PlotlyPlot(boolean isSubPlot) {
        this(isSubPlot, new PlotData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER), Color.WHITE, new BasicTitle()), 1, 1);
    }

    public PlotlyPlot(boolean isSubPlot, PlotData plotData, int rows, int columns) {
        this.plotData = plotData;
        this.isSubPlot = isSubPlot;
        //TODO: Accept other plot types
        intializePlotData();
        if (!isSubPlot) {
            initializeView();
        }

        synchronized (fileChooserLock) {
            if (chooser == null) {
                chooser = new FileChooser();
                chooser.initialDirectoryProperty().bindBidirectional(lastKnownDirectoryProperty);
            }
        }
    }

    private void intializePlotData() {
        plotData.setMapper(new ObjectMapper());
        plotData.setShowLegend(true);
        plotData.setShowGroupVelocity(false);
        plotData.setShowWindowLines(false);
        plotData.setShowCodaStartLine(false);
        plotData.setAxes(new ArrayList<>(0));
        plotData.setShowHorizontalErrorBars(true);
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
                    wind.setMember("dataExporter", this);
                    wind.setMember("logBridge", this);
                    engine.executeScript("console.log = function(message)\n" + "{\n" + " logBridge.log(message);\n" + "};");
                    engine.executeScript("initPlotDiv();");
                    plotData.getPlotReady().set(true);
                    replot();
                }
            });

            engine.load(getClass().getResource("/plotly.html").toExternalForm());
        });
    }

    @Override
    public void setSubplotLayout(final int columns, final int rows) {
        this.columns = columns;
        this.rows = rows;
    }

    public String exportData() {
        if (plotData.getPlotReady().get()) {
            try {
                chooser.setInitialFileName("raw-data.json");
                File saveTarget = chooser.showSaveDialog(null);
                if (saveTarget != null) {
                    FileUtils.writeStringToFile(saveTarget, getPlotDataJSON(), Charset.defaultCharset());
                    synchronized (fileChooserLock) {
                        lastKnownDirectoryProperty.set(saveTarget.getParentFile());
                    }
                }
            } catch (Exception e) {
                log.trace(e.getLocalizedMessage(), e);
            }
        }
        return "";
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

    public void setClickPickingModeEnabled(boolean clickToPickEnabled) {
        this.clickToPickEnabled = clickToPickEnabled;
    }

    public boolean isClickToPickEnabled() {
        return clickToPickEnabled;
    }

    @Override
    public void setShowFreqLevelButtons(boolean showFreqLevelButtons) {
        this.showFreqLevelButtons = showFreqLevelButtons;
    }

    public boolean isShowFreqLevelButtons() {
        return showFreqLevelButtons;
    }

    public void fireShapeMoveEvent(final String name, final double x0, final double x1, final double y0, final double y1) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "shape_move", null, new PlotShapeMove(name, x0, x1, y0, y1)));
            }
        });
    }

    public void fireAnnotationMoveEvent(String name, double x, double y) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "annotation_move", null, new PlotAnnotationMove(name, x, y)));
            }
        });
    }

    public void fireFreqLevelChange(final boolean lflMode, final double x, final double y, final double xx, final double yy) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "freq_level_mode", null, new PlotFreqLevelChange(lflMode, x, y, xx, yy)));
            }
        });
    }

    public void firePlotClickedEvent(final double plotX, final double plotY, final double clientX, final double clientY) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "plot_clicked", null, new PlotMouseEvent(plotX, plotY, clientX, clientY)));
            }
        });
    }

    public void firePlotMouseMovedEvent(final double plotX, final double plotY, final double clientX, final double clientY) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "plot_mouse_moved", null, new PlotMouseEvent(plotX, plotY, clientX, clientY)));
            }
        });
    }

    public void fireOtherChangeEvent(final String data) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "other_change", null, data));
            }
        });
    }

    public void fireAxisChangeEvent(final boolean reset, final double xMin, final double xMax, final double yMin, final double yMax) {
        CompletableFuture.runAsync(() -> {
            if (propertyChange.getPropertyChangeListeners().length > 0) {
                propertyChange.firePropertyChange(new PropertyChangeEvent(this, "axis_change", null, new PlotAxisChange(reset, xMin, xMax, yMin, yMax)));
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
                            double clickX = (bounds.getMaxX() + bounds.getMinX()) / 2.0;
                            double clickY = (bounds.getMaxY() + bounds.getMinY()) / 2.0;
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
    public void fullReplot() {
        hasChanges.set(true);
        hasPersistentChanges.set(true);
        replot();
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
    public Map<String, PlotObjectData> getPlotTypes() {
        return plotData.getDefaultTypePlots();
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
        //We want these listeners to be singletons since we are using them as
        // A) Only using them for one listener (pick moves)
        // B) Re-using the plot by swapping around data
        PropertyChangeListener[] propertyChangeListeners = propertyChange.getPropertyChangeListeners();
        for (PropertyChangeListener listener : propertyChangeListeners) {
            propertyChange.removePropertyChangeListener(listener);
        }
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

    @Override
    public void setUseHorizontalBottomLegend(boolean useHorizontalBottomLegend) {
        plotData.setUseHorizontalBottomLegend(useHorizontalBottomLegend);
    }

    protected synchronized void addPlotObject(final PlotObject object, final PlotData plot) {
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

            if (vline.getHoverTemplate() != null) {
                data.setHoverTemplate(vline.getHoverTemplate());
            }

            if (data.getTraceStyle() == null) {
                final PlotTrace traceStyle = populateStyle(vline, Style.VERTICAL_LINE, plot);
                traceStyle.setPxSize(vline.getPxWidth());
                traceStyle.setStyleName(LineStyles.SOLID.getStyleName());
                traceStyle.setDraggable(vline.isDraggable());
                traceStyle.setSeriesName(vline.getText());
                traceStyle.setAnnotationLogX(vline.isLogScaleX());
                data.setTraceStyle(traceStyle);
            }
        } else if (object instanceof Rectangle) {
            final Rectangle rect = (Rectangle) object;
            final List<Double> ydata = data.getYdata();

            final double delta = (1.0 - (rect.getRatioY() / 100.0)) / 2.0;
            ydata.clear();
            ydata.add(1.0 - delta);
            ydata.add(delta);

            final List<Double> xdata = data.getXdata();
            xdata.clear();
            xdata.add(rect.getX1());
            xdata.add(rect.getX2());

            if (rect.getHoverTemplate() != null) {
                data.setHoverTemplate(rect.getHoverTemplate());
            }

            if (data.getTraceStyle() == null) {
                final PlotTrace traceStyle = populateStyle(rect, Style.VERTICAL_LINE, plot);
                traceStyle.setPxSize(rect.getPxWidth());
                traceStyle.setStyleName(LineStyles.SOLID.getStyleName());
                traceStyle.setDraggable(rect.isDraggable());
                traceStyle.setSeriesName(rect.getText());
                traceStyle.setAnnotationLogX(rect.isLogScaleX());
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

            final List<Double> errorDataMin = data.getErrorData();
            errorDataMin.clear();
            errorDataMin.addAll(Arrays.stream(line.getErrorData()).boxed().collect(Collectors.toList()));

            final List<Double> errorDataMax = data.getErrorDataMinus();
            errorDataMax.clear();
            errorDataMax.addAll(Arrays.stream(line.getErrorDataMinus()).boxed().collect(Collectors.toList()));

            data.setErrorBarsHorizontal(line.getUseHorizontalErrorBars());

            if (line.getHoverTemplate() != null) {
                data.setHoverTemplate(line.getHoverTemplate());
            }

            if (line.getHoverMode() != null) {
                data.setHoverMode(line.getHoverMode());
            }

            if (line.getFillMode() != null) {
                data.setFillMode(line.getFillMode());
            }

            if (data.getTraceStyle() == null) {
                final PlotTrace traceStyle = populateStyle(line, Style.LINE, plot);
                traceStyle.setPxSize(line.getPxThickness());
                traceStyle.setDraggable(line.getDraggable());
                traceStyle.setSeriesName(line.getName());
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

            if (symbol.getHoverTemplate() != null) {
                data.setHoverTemplate(symbol.getHoverTemplate());
            }

            if (data.getTraceStyle() == null) {
                final PlotTrace traceStyle = populateStyle(symbol, plot.getDefaultTraceStyle().getType(), plot);
                traceStyle.setPxSize(plot.getDefaultTraceStyle().getPxSize());
                traceStyle.setStyleName(symbol.getStyle().getStyleName());
                traceStyle.setColorMap(symbol.getColorMap());
                data.setTraceStyle(traceStyle);
            }
        }
        hasChanges.set(true);
    }

    private PlotTrace populateStyle(final PlotObject object, final Style style, final PlotData plot) {
        final PlotTrace traceStyle = new PlotTrace(style);
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

    protected synchronized void removePlotObject(PlotObject object, PlotData plot) {
        final PlotObjectData data = plot.getDefaultTypePlots().computeIfAbsent(object.getSeriesIdentifier(), k -> new PlotObjectData());
        if (object instanceof VerticalLine || object instanceof Rectangle || object instanceof Line) {
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

    public void setShowGroupVelocity(final boolean showGroupVelocity) {
        this.plotData.setShowGroupVelocity(showGroupVelocity);
    }

    public void setShowWindowLines(final boolean showWindowLines) {
        this.plotData.setShowWindowLines(showWindowLines);
    }

    public void setShowHorizontalErrorBars(final boolean showHorizontalErrorBars) {
        this.plotData.setShowHorizontalErrorBars(showHorizontalErrorBars);
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

                // Hover mode
                final HoverModes hoverInfo = data.getHoverMode();
                if (hoverInfo != null) {
                    trace.put("hoverinfo", hoverInfo.getHoverModeName());
                }

                // Fill mode
                final FillModes fillMode = data.getFillMode();
                if (fillMode != null) {
                    trace.put("fill", fillMode.getFillModeName());
                    trace.put("fillcolor", FxUtils.toWebHexColorString(data.getTraceStyle().getFillColor()));
                }

                // Hover template
                final String hoverTemplate = data.getHoverTemplate();
                if (hoverTemplate != null) {
                    trace.put("hovertemplate", hoverTemplate);
                }

                // Error bars
                final List<Double> errorData = data.getErrorData();
                final List<Double> errorDataMinus = data.getErrorDataMinus();
                if (errorData != null && errorDataMinus != null) {

                    final ObjectNode errorBarNode = plotData.getMapper().createObjectNode();
                    errorBarNode.put("type", "data");
                    if (!errorData.isEmpty()) {
                        final ArrayNode errorDataNode = trace.arrayNode();
                        errorData.forEach(errorDataNode::add);
                        errorBarNode.set("array", errorDataNode);
                    }

                    if (!errorDataMinus.isEmpty()) {
                        final ArrayNode errorDataMinusNode = trace.arrayNode();
                        errorDataMinus.forEach(errorDataMinusNode::add);
                        errorBarNode.set("arrayminus", errorDataMinusNode);
                    }

                    if (errorBarNode.has("array") || errorBarNode.has("arrayminus")) {
                        if (data.useHorizontalErrorBars()) {
                            trace.set("error_x", errorBarNode);
                        } else {
                            trace.set("error_y", errorBarNode);
                        }
                    }
                }

                final List<Double[]> zdata = data.getZdata();
                if (!zdata.isEmpty()) {
                    final ArrayNode zNode = trace.arrayNode();
                    zdata.forEach(zdata2 -> {
                        final ArrayNode zNode2 = trace.arrayNode();
                        for (Double element : zdata2) {
                            zNode2.add(element);
                        }
                        zNode.add(zNode2);
                    });
                    trace.set("z", zNode);
                }

                if (data.getTraceStyle().getType().equals(Style.CONTOUR)) {
                    trace.put("connectgaps", true);
                }

                final List<Double> cData = data.getColorData();
                if (!cData.isEmpty()) {
                    final ArrayNode cNode = trace.arrayNode();
                    cData.forEach(cNode::add);
                    trace.withObjectProperty("marker").set("color", cNode);
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
        layoutNode.put("showGroupVelocity", plotData.shouldShowGroupVelocity());
        layoutNode.put("showWindowLines", plotData.shouldShowWindowLine());
        layoutNode.put("showCodaStartLine", plotData.shouldShowCodaStartLine());
        layoutNode.put("dragmode", plotData.getDragmode());
        if (!subPlots.isEmpty()) {
            // If rows is set to default of 1, update it to match number of subplots
            if (this.rows == 1) {
                this.rows = subPlots.size();
            }
            layoutNode.withObjectProperty("grid").put("rows", rows).put("columns", columns).put("pattern", "independent");
            for (int i = 0; i < subPlots.size(); i++) {
                PlotlyPlot subPlot = subPlots.get(i);
                if (i > 0) {
                    subPlot.setSubPlotId((i + 1));
                }
                ObjectNode subLayout = subPlot.getPlotLayoutNode();
                layoutNode.setAll(subLayout);
            }
        } else {
            layoutNode.put("showlegend", plotData.shouldShowLegend());
            if (plotData.useHorizontalBottomLegend()) {
                layoutNode.withObjectProperty("legend").put("orientation", "h");
                layoutNode.withObjectProperty("legend").put("x", "0.0");
                layoutNode.withObjectProperty("legend").put("y", "-0.5");
            }
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
                case LOG_Y:
                    dataType = "log";
                case Y:
                    axisType = "yaxis";
                    break;
                case Y_RIGHT:
                    axisType = "yaxis2";
                    axisSide = "right";
                    overlaying = "y";
                    anchor = "y";
                    break;
                case X_TOP:
                    axisType = "xaxis2";
                    axisSide = "top";
                    overlaying = "x";
                    anchor = "x";
                    break;
                default:
                    axisType = null;
                    break;
                }

                if (axisType != null) {
                    if (subplotId != null) {
                        axisType = axisType + subplotId;
                    }
                    layoutNode.withObjectProperty(axisType).put("zeroline", false);
                    layoutNode.withObjectProperty(axisType).put("hoverformat", ".2f");
                    layoutNode.withObjectProperty(axisType).withObjectProperty("title").put("text", axis.getText()).withObjectProperty("font").put("size", 12);

                    if (axisSide != null && axisSide.equals("top")) {
                        layoutNode.withObjectProperty(axisType).withObjectProperty("title").put("standoff", 0);
                    }

                    layoutNode.withObjectProperty(axisType).put("linecolor", "black");
                    layoutNode.withObjectProperty(axisType).put("linewidth", 1.25);
                    layoutNode.withObjectProperty(axisType).put("mirror", true);

                    if (dataType != null) {
                        layoutNode.withObjectProperty(axisType).put("type", dataType);
                    }

                    if (axisSide != null) {
                        layoutNode.withObjectProperty(axisType).put("side", axisSide);
                    }
                    if (axis.getMin() != axis.getMax()) {
                        final ArrayNode range = plotData.getMapper().createArrayNode();
                        range.add(axis.getMin());
                        range.add(axis.getMax());
                        layoutNode.withObjectProperty(axisType).set("range", range);
                    } else {
                        layoutNode.withObjectProperty(axisType).remove("range");
                    }

                    if (axis.getTickFormat() != null) {
                        layoutNode.withObjectProperty(axisType).put("tickformat", axis.getTickFormat().getFormat());
                        layoutNode.withObjectProperty(axisType).put("hoverformat", axis.getTickFormat().getFormat());
                    }

                    if (axis.getTickFormatString() != null) {
                        layoutNode.withObjectProperty(axisType).put("tickformat", axis.getTickFormatString());
                    }

                    if (overlaying != null) {
                        layoutNode.withObjectProperty(axisType).put("overlaying", overlaying);
                    }
                    if (anchor != null) {
                        layoutNode.withObjectProperty(axisType).put("anchor", anchor);
                    }
                }
            }

            ObjectNode margin = layoutNode.withObjectProperty("margin");
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
                PlotTrace style = data.getTraceStyle();

                // Create annotations for LFL and HFL lines
                if (style != null && style.isDraggable() && (style.getSeriesName().contains("LFL") || style.getSeriesName().contains("HFL"))) {
                    final ObjectNode annotationNode = plotData.getMapper().createObjectNode();
                    String name = "LFL";

                    // Styling differs between hfl and lfl lines
                    if (style.getSeriesName().contains("HFL")) {
                        name = "HFL";
                        annotationNode.put("x", 0.95).put("ay", 20.0);
                    } else {
                        annotationNode.put("x", 0.05).put("ay", -20.0);
                    }

                    annotationNode.put("yref", "y")
                                  .put("xref", "paper")
                                  .put("ax", 0.0)
                                  .put("y", data.getYdata().get(0))
                                  .put("showarrow", true)
                                  .put("arrowcolor", FxUtils.toWebHexColorString(Color.BLACK))
                                  .put("text", name);

                    annotations.add(annotationNode);
                }

                if (style != null && style.getType() != null && style.getType().getType().equals(SHAPES)) {
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
                                          .withObjectProperty("font")
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
    @Override
    public void setMargin(Integer top, Integer bottom, Integer left, Integer right) {
        this.topMargin = top;
        this.bottomMargin = bottom;
        this.leftMargin = left;
        this.rightMargin = right;
        hasPersistentChanges.set(true);
    }

    @Override
    public void setDragMode(String dragmode) {
        this.plotData.setDragmode(dragmode);
        hasPersistentChanges.set(true);
    }
}
