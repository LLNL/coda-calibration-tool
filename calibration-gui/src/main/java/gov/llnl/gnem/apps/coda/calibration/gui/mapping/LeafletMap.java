/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.gui.mapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Map;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

@Component
@Scope("prototype")
@ConfigurationProperties("leaflet")
public class LeafletMap implements Map {

    private WebView webView;
    private ObservableSet<Icon> icons;
    private boolean debugEnabled = false;

    public LeafletMap() {
        icons = FXCollections.observableSet(new HashSet<>());

        Platform.runLater(() -> {
            webView = new WebView();
            webView.getEngine().setJavaScriptEnabled(true);
            if (debugEnabled) {
                webView.getEngine().load(getClass().getResource("/leaflet/leaflet-debug.html").toExternalForm());
            } else {
                webView.getEngine().load(getClass().getResource("/leaflet/leaflet.html").toExternalForm());
            }
        });
    }

    @Override
    public boolean addIcon(Icon icon) {
        return icons.add(icon);
    }

    @Override
    public boolean removeIcon(Icon icon) {
        return icons.remove(icon);
    }

    @Override
    public void attach(StackPane parent) {
        if (parent != null) {
            parent.getChildren().add(webView);
        }
        icons.addListener((SetChangeListener<Icon>) change -> {
            clearIconLayer();
            addIconsToMap(change.getSet());
        });
    }

    protected void clearIconLayer() {
        Platform.runLater(() -> webView.getEngine().executeScript("iconGroup.clearLayers()"));
    }

    protected void addIconsToMap(Collection<? extends Icon> icons) {
        // TODO: Need to save object references in a JavaScript map and keep IDs
        // in sync on our end to add/remove efficiently. For
        // now we can probably get away with clearing and re-adding the layer.
        List<? extends Icon> iconCollection = new ArrayList<>(icons);
        Platform.runLater(() -> {
            StringBuilder sb = new StringBuilder();
            for (Icon icon : iconCollection) {
                sb.append(createJsIconRepresentation(icon));
            }
            webView.getEngine().executeScript(sb.toString());
        });
    }

    private String createJsIconRepresentation(Icon icon) {
        StringBuilder sb = new StringBuilder();
        switch (icon.getType()) {
        case TRIANGLE_UP:
            sb.append("L.marker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(",");
            sb.append(icon.getLocation().getLongitude());
            sb.append("], {icon: L.icon({");
            sb.append("iconUrl: 'images/triangle-up.png',");
            sb.append("iconSize: [12, 12],");
            sb.append("iconAnchor: [6, 6],");
            sb.append("popupAnchor: [-3, -3]");
            sb.append("})}).bindPopup('");
            sb.append(icon.getFriendlyName());
            sb.append("').addTo(iconGroup);");
            break;
        case CIRCLE:
        case DEFAULT:
            sb.append("L.circleMarker([");
            sb.append(icon.getLocation().getLatitude());
            sb.append(",");
            sb.append(icon.getLocation().getLongitude());
            sb.append("], { radius: 5, color: 'black', fillColor: '#ff0000', opacity: 1, fillOpacity: 1 })");
            sb.append(".bindPopup('");
            sb.append(icon.getFriendlyName());
            sb.append("').addTo(iconGroup);");
            break;
        }
        return sb.toString();
    }

    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    @Override
    public void clearIcons() {
        icons.clear();
    }

    @Override
    public void addIcons(Collection<Icon> icons) {
        addIconsToMap(icons);
    }
}
