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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.common.mapping.MapCallbackEvent;
import gov.llnl.gnem.apps.coda.common.mapping.MapProperties;
import gov.llnl.gnem.apps.coda.common.mapping.WMSLayerDescriptor;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShape;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Transform;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;

@Component
@Scope(scopeName = ConfigurableBeanFactory.SCOPE_SINGLETON)
public class CertLeafletMapController implements GeoMap {

    private static final Logger log = LoggerFactory.getLogger(CertLeafletMapController.class);

    @FXML
    private StackPane mapView;

    @FXML
    private ScrollPane viewScroll;

    @FXML
    private Button mapSnapshotButton;

    private LeafletMap mapImpl;

    private MapPlottingUtilities iconFactories;

    private DirectoryChooser screenshotFolderChooser = new DirectoryChooser();

    private MapProperties mapProps;

    private CertLeafletMapController(@Autowired(required = false) MapProperties mapProps, final MapPlottingUtilities iconFactory) {
        this.mapProps = mapProps;
        this.iconFactories = iconFactory;
        this.mapImpl = new LeafletMap();
    }

    @FXML
    public void initialize() {
        ImageView label = new ImageView();
        label.setImage(new Image(getClass().getResource("/fxml/snapshot-icon.png").toExternalForm()));
        label.setFitWidth(16);
        label.setFitHeight(16);
        mapSnapshotButton.setGraphic(label);
        mapSnapshotButton.setContentDisplay(ContentDisplay.CENTER);

        Platform.runLater(() -> {
            mapView.setVisible(true);
            mapImpl.attach(mapView);
            if (mapProps != null) {
                mapProps.getLayers().forEach(mapImpl::addLayer);
            }
        });
    }

    public void replaceIcons(List<Icon> oldIcons, List<Icon> newIcons) {
        mapImpl.removeIcons(oldIcons);
        mapImpl.addIcons(newIcons);
    }

    public void setEventIconsActive(final List<Event> events) {
        Set<Icon> icons = mapImpl.getIcons();
        if (icons != null) {
            List<Icon> iconsToReplace = new ArrayList<>();
            List<Icon> newIconsToUse = new ArrayList<>();

            icons.forEach(i -> {
                events.forEach(e -> {
                    if (i.getFriendlyName().equals(e.getEventId())) {
                        iconsToReplace.add(i);
                        newIconsToUse.add(iconFactories.createEventIconForeground(e));
                    }
                });
            });
            if (iconsToReplace != null) {
                replaceIcons(iconsToReplace, newIconsToUse);
            }
        }
    }

    public void setEventIconsInActive(final List<Event> events) {
        Set<Icon> icons = mapImpl.getIcons();
        if (icons != null) {
            List<Icon> iconsToReplace = new ArrayList<>();
            List<Icon> newIconsToUse = new ArrayList<>();

            icons.forEach(i -> {
                events.forEach(e -> {
                    if (i.getFriendlyName().equals(e.getEventId())) {
                        iconsToReplace.add(i);
                        newIconsToUse.add(iconFactories.createEventIcon(e));
                    }
                });
            });
            if (iconsToReplace != null) {
                replaceIcons(iconsToReplace, newIconsToUse);
            }
        }
    }

    @Override
    public void show() {
        // No op for now
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
    public Set<Icon> getIcons() {
        return mapImpl.getIcons();
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
    public String getPolygonGeoJSON() {
        return mapImpl.getPolygonGeoJSON();
    }

    @Override
    public void setPolygonGeoJSON(String geoJSON) {
        Platform.runLater(() -> {
            mapImpl.setPolygonGeoJSON(geoJSON);
        });
    }

    @Override
    public int hashCode() {
        return Objects.hash(mapImpl, mapView);
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
        CertLeafletMapController other = (CertLeafletMapController) obj;
        if (!Objects.equals(mapImpl, other.mapImpl)) {
            return false;
        }
        if (!Objects.equals(mapView, other.mapView)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(mapView).append("\", \"").append(mapImpl).append('\"');
        return builder.toString();
    }

    @FXML
    private void showMapSnapshotDialog(ActionEvent e) {
        File folder = screenshotFolderChooser.showDialog(mapView.getScene().getWindow());
        try {
            if (folder != null && folder.exists() && folder.isDirectory() && folder.canWrite()) {
                screenshotFolderChooser.setInitialDirectory(folder);
                exportSnapshot(folder);
            }
        } catch (SecurityException ex) {
            log.warn("Exception trying to write screenshots to folder {} : {}", folder, ex.getLocalizedMessage(), ex);
        }
    }

    private void exportSnapshot(File folder) {
        String mapId = folder.getAbsolutePath() + File.separator + "Map_" + Instant.now().toEpochMilli();
        try {
            String svg = mapImpl.getSvgLayer();
            if (svg != null && !svg.trim().isEmpty()) {
                Files.write(Paths.get(mapId + ".svg"), svg.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
        } catch (IOException e) {
            log.warn("Unable to write map svg due to file exception {}", e.getLocalizedMessage(), e);
        }
        writePng(mapId + ".png");
    }

    private Image snapshot(final WebView node) {
        SnapshotParameters params = new SnapshotParameters();
        params.setTransform(Transform.scale(4.0, 4.0));
        Image snapshot = node.snapshot(params, null);
        return snapshot;
    }

    private void writePng(BufferedImage image, String filename) {
        try {
            ImageIO.write(image, "png", new File(filename));
            log.trace("Wrote image to: {}", filename);
        } catch (IOException ex) {
            log.warn(ex.getMessage(), ex);
        } catch (NullPointerException ex) {
            log.warn("Null pointer writing image {} to file {} : {}", image, filename, ex.getMessage(), ex);
        }
    }

    private void writePng(String filename) {
        mapImpl.setShowOverlay(false);
        CompletableFuture.runAsync(() -> {
            //This is really dumb and subject to nasty race conditions but, as of writing (Jan 2020), I can find no good way to get a notification from the JavaFX renderer that it actually executed a render pass.
            //There is a pre/post pulse callback on Scene proposed but is not available in Java 8 and we need to maintain compatibility for the moment.
            //Thus we have to do this song and dance to have a reasonable assumption a pulse has happened on the FX thread.
            //We could try to register and start/stop an AnimationTimer to know for sure but that's considerably more complicated and the fail state for this is "has visible buttons in the screenshot" so eh.
            try {
                Thread.sleep(200l);
            } catch (InterruptedException e) {
                //Nop
            }
            Platform.runLater(() -> {
                try {
                    Boolean visibleLayers = mapImpl.hasVisibleTileLayers();
                    if (visibleLayers) {
                        Image snapshot = snapshot(mapImpl.getWebView());
                        CompletableFuture.runAsync(() -> {
                            if (visibleLayers) {
                                writePng(SwingFXUtils.fromFXImage(snapshot, null), filename);
                            }
                            Platform.runLater(() -> mapImpl.setShowOverlay(true));
                        });
                    } else {
                        mapImpl.setShowOverlay(true);
                    }
                } catch (RuntimeException e) {
                    mapImpl.setShowOverlay(true);
                }
            });
        });
    }
}
