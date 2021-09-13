/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.mapping.api.GeoBox;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShape;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconStyles;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.api.Line;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.concurrent.Worker;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

public class LeafletMap {

    private static final LeafletIcon POLYGON_OUT_ICON = new LeafletIcon(null, null, IconTypes.POLYGON_OUT);
    private static final LeafletIcon POLYGON_IN_ICON = new LeafletIcon(null, null, IconTypes.POLYGON_IN);

    private static final Logger log = LoggerFactory.getLogger(LeafletMap.class);

    private WebView webView;
    private final ObservableSet<Icon> icons = FXCollections.synchronizedObservableSet(FXCollections.observableSet(new HashSet<>()));
    private final ObservableSet<GeoShape> shapes = FXCollections.observableSet(new HashSet<>());
    private Pane parent;
    private final Set<WMSLayerDescriptor> layers = new HashSet<>();
    private final AtomicBoolean mapReady = new AtomicBoolean(false);
    private final Map<String, BiConsumer<Boolean, String>> callbackMap = new HashMap<>();
    private IconCallbackHandler iconCallbackHandler;
    private PolygonChangeCallbackHandler polygonChangeCallbackHandler;
    private final List<Consumer<MapCallbackEvent>> eventCallbacks = new ArrayList<>();
    private ContextMenu contextMenu;
    private final MenuItem reload = new MenuItem("Reload");
    private final MenuItem include = new MenuItem("Include");
    private final MenuItem exclude = new MenuItem("Exclude");
    private final MenuItem excludeOutPolygon = new MenuItem("Exclude outside");
    private final MenuItem includeOutPolygon = new MenuItem("Include outside");
    private final MenuItem excludeInPolygon = new MenuItem("Exclude inside");
    private final MenuItem includeInPolygon = new MenuItem("Include inside");

    public class IconCallbackHandler {
        private final BiConsumer<Boolean, String> iconCallbackHandler;

        public IconCallbackHandler(final BiConsumer<Boolean, String> iconCallbackHandler) {
            this.iconCallbackHandler = iconCallbackHandler;
        }

        public void accept(final Boolean selected, final String value) {
            iconCallbackHandler.accept(selected, value);
        }
    }

    public class PolygonChangeCallbackHandler {
        private final Consumer<String> polygonChangeCallbackHandler;

        public PolygonChangeCallbackHandler(final Consumer<String> polygonChangeCallbackHandler) {
            this.polygonChangeCallbackHandler = polygonChangeCallbackHandler;
        }

        public void accept(final String value) {
            polygonChangeCallbackHandler.accept(value);
        }
    }

    public LeafletMap() {
        iconCallbackHandler = new IconCallbackHandler((selected, id) -> {
            final BiConsumer<Boolean, String> callback = callbackMap.get(id);
            if (callback != null) {
                callback.accept(selected, id);
            }
        });

        polygonChangeCallbackHandler = new PolygonChangeCallbackHandler(geoJSON -> {
            final List<Consumer<MapCallbackEvent>> callbacks = new ArrayList<>(eventCallbacks);
            final MapCallbackEvent event = new MapCallbackEvent(null, MAP_CALLBACK_EVENT_TYPE.POLYGON_CHANGE, true, geoJSON);
            callbacks.forEach(cb -> cb.accept(event));
        });

        Platform.runLater(() -> {
            webView = new WebView();
            webView.getEngine().setJavaScriptEnabled(true);
            webView.setContextMenuEnabled(false);
            contextMenu = new ContextMenu();
            reload.setOnAction(e -> webView.getEngine().reload());

            webView.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
                if (MouseButton.SECONDARY == event.getButton()) {
                    final WebEngine engine = webView.getEngine();
                    contextMenu.getItems().clear();
                    final Object activeIconId = engine.executeScript("getActiveIcon();");
                    final Object activePolygonId = engine.executeScript("getActivePolygon();");
                    if (activeIconId instanceof String) {
                        icons.stream().filter(icon -> icon.getId().equalsIgnoreCase((String) activeIconId)).findFirst().ifPresent(icon -> {
                            include.setOnAction(e -> invokeActivationCallbacks(icon, true));
                            exclude.setOnAction(e -> invokeActivationCallbacks(icon, false));
                            contextMenu.getItems().addAll(include, exclude);
                            contextMenu.show(webView, event.getScreenX(), event.getScreenY());
                        });
                    } else if (activePolygonId instanceof Boolean && (boolean) activePolygonId) {
                        excludeOutPolygon.setOnAction(e -> invokeActivationCallbacks(POLYGON_OUT_ICON, false));
                        includeOutPolygon.setOnAction(e -> invokeActivationCallbacks(POLYGON_OUT_ICON, true));
                        excludeInPolygon.setOnAction(e -> invokeActivationCallbacks(POLYGON_IN_ICON, false));
                        includeInPolygon.setOnAction(e -> invokeActivationCallbacks(POLYGON_IN_ICON, true));
                        contextMenu.getItems().addAll(excludeOutPolygon, includeOutPolygon, excludeInPolygon, includeInPolygon);
                        contextMenu.show(webView, event.getScreenX(), event.getScreenY());
                    } else {
                        contextMenu.getItems().addAll(reload);
                        contextMenu.show(webView, event.getScreenX(), event.getScreenY());
                    }
                } else {
                    contextMenu.hide();
                }
            });
            webView.getEngine().getLoadWorker().stateProperty().addListener((obs, o, n) -> {
                if (n == Worker.State.SUCCEEDED) {
                    layers.forEach(this::addLayerToMap);
                    final JSObject wind = (JSObject) webView.getEngine().executeScript("window");
                    wind.setMember("iconCallbackHandler", iconCallbackHandler);
                    wind.setMember("polygonChangeCallbackHandler", polygonChangeCallbackHandler);
                    mapReady.set(true);
                    return;
                }
            });

            webView.getEngine().load(getClass().getResource("/leaflet/leaflet.html").toExternalForm());
            if (parent != null) {
                parent.getChildren().add(webView);
            }
        });
    }

    private void invokeActivationCallbacks(final Icon icon, final boolean active) {
        final List<Consumer<MapCallbackEvent>> callbacks = new ArrayList<>(eventCallbacks);
        final MapCallbackEvent event = new MapCallbackEvent(icon, MAP_CALLBACK_EVENT_TYPE.ACTIVATION, active);
        callbacks.forEach(cb -> cb.accept(event));
    }

    public void registerEventCallback(final Consumer<MapCallbackEvent> callback) {
        eventCallbacks.add(callback);
    }

    public void removeEventCallback(final Consumer<MapCallbackEvent> callback) {
        eventCallbacks.remove(callback);
    }

    public long getIconCount() {
        return icons.size();
    }

    public void attach(final Pane parent) {
        if (parent != null) {
            if (this.parent != null && webView != null) {
                this.parent.getChildren().remove(webView);
            }
            this.parent = parent;
            if (webView != null) {
                parent.getChildren().add(webView);
            }
        }
    }

    public void clearIcons() {
        icons.clear();
        callbackMap.clear();
        clearIconLayer();
    }

    private void clearIconLayer() {
        if (mapReady.get()) {
            Platform.runLater(() -> webView.getEngine().executeScript("clearIcons();"));
        }
    }

    public void addLayer(final WMSLayerDescriptor layer) {
        if (layer != null) {
            layers.add(layer);
            if (mapReady.get()) {
                addLayerToMap(layer);
            }
        }
    }

    public void addLayerToMap(final WMSLayerDescriptor layer) {
        Platform.runLater(() -> {
            final StringBuilder sb = new StringBuilder();

            sb.append("layerControl.addOverlay(");
            sb.append("L.tileLayer.wms('");
            sb.append(layer.getUrl());
            sb.append("', { layers: '");
            for (int i = 0; i < layer.getLayers().size(); i++) {
                sb.append(layer.getLayers().get(i));
                if (i != layer.getLayers().size() - 1) {
                    sb.append(':');
                }
            }
            sb.append("'}),\"");
            sb.append(layer.getName());
            sb.append("\");");
            webView.getEngine().executeScript(sb.toString());
        });
    }

    public boolean addIcon(final Icon icon) {
        if (mapReady.get()) {
            addIconsToMap(Collections.singleton(icon));
        }
        return icons.add(icon);
    }

    public boolean removeIcon(final Icon icon) {
        if (mapReady.get()) {
            removeIconsFromMap(Collections.singleton(icon));
        }
        callbackMap.remove(icon.getId());
        return icons.remove(icon);
    }

    public void addIcons(final Collection<Icon> icons) {
        this.icons.addAll(icons);
        if (mapReady.get()) {
            addIconsToMap(icons);
        }
    }

    public void removeIcons(final Collection<Icon> icons) {
        this.icons.removeAll(icons);
        icons.forEach(icon -> callbackMap.remove(icon.getId()));
        if (mapReady.get()) {
            removeIconsFromMap(icons);
        }
    }

    private void addIconsToMap(final Collection<? extends Icon> icons) {
        final List<? extends Icon> iconCollection = new ArrayList<>(icons);
        Platform.runLater(() -> {
            final StringBuilder sb = new StringBuilder();
            for (final Icon icon : iconCollection) {
                sb.append(createJsIconRepresentation(icon));
                if (icon.getIconSelectionCallback() != null) {
                    callbackMap.put(icon.getId(), icon.getIconSelectionCallback());
                }
            }
            webView.getEngine().executeScript(sb.toString());
        });
    }

    private void removeIconsFromMap(final Collection<? extends Icon> icons) {
        Platform.runLater(() -> icons.forEach(icon -> {
            webView.getEngine().executeScript("removeIcon(\"" + icon.getId() + "\");");
        }));
    }

    public void addShape(final GeoShape shape) {
        if (mapReady.get()) {
            addShapesToMap(Collections.singleton(shape));
        }
        shapes.add(shape);
    }

    public void removeShape(final GeoShape shape) {
        if (mapReady.get()) {
            removeShapesFromMap(Collections.singleton(shape));
        }
        shapes.remove(shape);
    }

    private void addShapesToMap(final Collection<? extends GeoShape> shapes) {
        final List<? extends GeoShape> shapeCollection = new ArrayList<>(shapes);
        Platform.runLater(() -> {
            final StringBuilder sb = new StringBuilder();
            for (final GeoShape shape : shapeCollection) {
                sb.append(createJsShapeRepresentation(shape));
            }
            webView.getEngine().executeScript(sb.toString());
        });
    }

    public void fitViewToActiveShapes() {
        Platform.runLater(() -> webView.getEngine().executeScript("fitViewToActiveShapes();"));
    }

    private void removeShapesFromMap(final Collection<? extends GeoShape> shapes) {
        Platform.runLater(() -> shapes.forEach(shape -> {
            webView.getEngine().executeScript("removeShape(\"" + shape.getId() + "\");");
        }));
    }

    private String createJsShapeRepresentation(final GeoShape shape) {
        //TODO: Encapsulate this better...
        final StringBuilder sb = new StringBuilder();

        if (shape instanceof Line) {
            final Line line = (Line) shape;
            sb.append("if (!markers.has(\"");
            sb.append(line.getId());
            sb.append("\")) {");
            sb.append("marker = L.polyline([");
            sb.append('[');
            sb.append(line.getStartLocation().getLatitude());
            sb.append(',');
            sb.append(line.getStartLocation().getLongitude());
            sb.append(']');
            sb.append(',');
            sb.append('[');
            sb.append(line.getEndLocation().getLatitude());
            sb.append(',');
            sb.append(line.getEndLocation().getLongitude());
            sb.append(']');
            sb.append(']');
            sb.append(", {color: 'black', interactive: false, weight: 1, pane: 'background-pane', bubblingMouseEvents: false, smoothFactor: 1}");
            sb.append(')');
            sb.append(".addTo(lineGroup);");
            sb.append("marker._uid = \"");
            sb.append(shape.getId());
            sb.append("\"; markers.set(marker._uid, lineGroup.getLayerId(marker)); }");
        }
        return sb.toString();
    }

    private String createJsIconRepresentation(final Icon icon) {
        //TODO: Encapsulate this better...
        final StringBuilder sb = new StringBuilder();

        sb.append("if (!markers.has(\"");
        sb.append(icon.getId());
        sb.append("\")) {");

        switch (icon.getType()) {
        case TRIANGLE_UP:
            sb.append("marker = L.shapeMarker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(',');
            sb.append(icon.getLocation().getLongitude());
            sb.append("], {");
            sb.append("shape: 'triangle-up',");
            sb.append(getTriangleStyle(icon.getStyle()));
            sb.append("radius: 4");
            sb.append("})");
            break;
        case CIRCLE:
        case DEFAULT:
            sb.append("marker = L.circleMarker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(',');
            sb.append(icon.getLocation().getLongitude());
            sb.append(']');
            sb.append(getCircleStyle(icon.getStyle()));
            sb.append(')');
            break;
        default:
            return "";
        }
        if (icon.getIconSelectionCallback() != null) {
            sb.append(addCallbacks(icon.getId(), icon.getFriendlyName()));
        }
        sb.append(".addTo(iconGroup);");
        sb.append("marker._uid = \"");
        sb.append(icon.getId());
        sb.append("\"; markers.set(marker._uid, iconGroup.getLayerId(marker)); }");
        return sb.toString();
    }

    private String addCallbacks(final String id, final String name) {
        return ".on('click', function() { iconCallbackHandler.accept(true, \""
                + id
                + "\"); }).bindPopup('"
                + name
                + "').on('popupclose', function() { iconCallbackHandler.accept(false, \""
                + id
                + "\"); }).on('mouseover', function() { if (\""
                + id
                + "\" !== mouseoverIconId) { mouseoverIconId = \""
                + id
                + "\"; }}).on('mouseout', function() { if (\""
                + id
                + "\" === mouseoverIconId) { mouseoverIconId = null; }})";
    }

    private String getTriangleStyle(final IconStyles style) {
        String jsonStyle;
        switch (style) {
        case FOCUSED:
            jsonStyle = "color: 'white', fillColor: 'white', opacity: 1, fillOpacity: 1, lineJoin: 'mitre', pane: 'important-event-pane', interactive: false,";
            break;
        case BACKGROUND:
            jsonStyle = "color: 'gray', fillColor: 'gray', opacity: 1, fillOpacity: 1, lineJoin: 'mitre',";
            break;
        case DEFAULT:
        default:
            jsonStyle = "color: 'yellow', fillColor: 'yellow', opacity: 1, fillOpacity: 1, lineJoin: 'mitre',";
            break;
        }
        return jsonStyle;
    }

    private String getCircleStyle(final IconStyles style) {
        String jsonStyle;
        switch (style) {
        case FOCUSED:
            jsonStyle = ", { radius: 5, color: 'black', fillColor: '#ffffff', opacity: 1, fillOpacity: 1, pane: 'important-event-pane', interactive: false }";
            break;
        case BACKGROUND:
            jsonStyle = ", { radius: 5, color: 'black', fillColor: '#505050', opacity: 1, fillOpacity: 1 }";
            break;
        case DEFAULT:
        default:
            jsonStyle = ", { radius: 5, color: 'black', fillColor: '#ff0000', opacity: 1, fillOpacity: 1 }";
            break;
        }
        return jsonStyle;
    }

    public WebView getWebView() {
        return webView;
    }

    public String getSvgLayer() {
        return (String) webView.getEngine().executeScript("getSvgLayer();");
    }

    public void fitToBounds(final GeoBox bounds) {
        webView.getEngine().executeScript("fitBounds([[" + bounds.getMinX() + "," + bounds.getMinY() + "], [" + bounds.getMaxX() + "," + bounds.getMaxY() + "]]);");
    }

    public GeoBox getMapBounds() {
        GeoBox bounds;
        try {
            final Double mapXne = (Double) webView.getEngine().executeScript("getMapBoundXne();");
            final Double mapYne = (Double) webView.getEngine().executeScript("getMapBoundYne();");
            final Double mapXsw = (Double) webView.getEngine().executeScript("getMapBoundXsw();");
            final Double mapYsw = (Double) webView.getEngine().executeScript("getMapBoundYsw();");
            bounds = new GeoBox(mapXne, mapYne, mapXsw, mapYsw);
        } catch (ClassCastException | NullPointerException e) {
            bounds = null;
            log.debug("Problem attempting to get bounds from map : {}", e.getLocalizedMessage(), e);
        }
        return bounds;
    }

    public void setShowOverlay(final boolean showOverlay) {
        if (showOverlay) {
            webView.getEngine().executeScript("showOverlay();");
        } else {
            webView.getEngine().executeScript("hideOverlay();");
        }
    }

    public Boolean hasVisibleTileLayers() {
        return (Boolean) webView.getEngine().executeScript("hasVisibleTiles();");
    }

    public String getPolygonGeoJSON() {
        return (String) webView.getEngine().executeScript("getPolygonGeoJSON();");
    }

    public void setPolygonGeoJSON(final String geoJSON) {
        webView.getEngine().executeScript("setPolygonGeoJSON('" + geoJSON + "');");
    }
}
