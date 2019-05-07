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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShape;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Service
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class LeafletMapController implements GeoMap {

    @FXML
    private StackPane view;

    private LeafletMap mapImpl;

    private Stage stage;

    private static final Logger log = LoggerFactory.getLogger(LeafletMapController.class);

    private LeafletMapController(@Autowired(required = false) MapProperties mapProps) {
        Platform.runLater(() -> {
            mapImpl = new LeafletMap();
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/MapView.fxml"));
            fxmlLoader.setController(this);
            stage = new Stage(StageStyle.DECORATED);
            try {
                Parent root = fxmlLoader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
                mapImpl.attach(view);
                if (mapProps != null) {
                    mapProps.getLayers().forEach(mapImpl::addLayer);
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public void hide() {
        Platform.runLater(() -> {
            stage.hide();
        });
    }

    @Override
    public void show() {
        Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        });
    }

    @Override
    public long getIconCount() {
        return mapImpl.getIconCount();
    }

    @Override
    public void clearIcons() {
        mapImpl.clearIcons();
    }

    @Override
    public void addLayer(WMSLayerDescriptor layer) {
        mapImpl.addLayer(layer);
    }

    @Override
    public boolean addIcon(Icon icon) {
        return mapImpl.addIcon(icon);
    }

    @Override
    public boolean removeIcon(Icon icon) {
        return mapImpl.removeIcon(icon);
    }

    @Override
    public void addIcons(Collection<Icon> icons) {
        mapImpl.addIcons(icons);
    }

    @Override
    public void removeIcons(Collection<Icon> icons) {
        mapImpl.removeIcons(icons);
    }

    @Override
    public void addShape(GeoShape shape) {
        mapImpl.addShape(shape);
    }

    @Override
    public void removeShape(GeoShape shape) {
        mapImpl.removeShape(shape);
    }

    @Override
    public void fitViewToActiveShapes() {
        mapImpl.fitViewToActiveShapes();
    }

    @Override
    public void registerEventCallback(Consumer<MapCallbackEvent> callback) {
        mapImpl.registerEventCallback(callback);
    }

    @Override
    public void removeEventCallback(Consumer<MapCallbackEvent> callback) {
        mapImpl.removeEventCallback(callback);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((mapImpl == null) ? 0 : mapImpl.hashCode());
        result = prime * result + ((view == null) ? 0 : view.hashCode());
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
        LeafletMapController other = (LeafletMapController) obj;
        if (mapImpl == null) {
            if (other.mapImpl != null) {
                return false;
            }
        } else if (!mapImpl.equals(other.mapImpl)) {
            return false;
        }
        if (view == null) {
            if (other.view != null) {
                return false;
            }
        } else if (!view.equals(other.view)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(view).append("\", \"").append(mapImpl).append('\"');
        return builder.toString();
    }

}
