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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class WaveformGui {

    private static final Logger log = LoggerFactory.getLogger(WaveformGui.class);

    private Parent root;
    private Scene scene;
    private Stage stage;

    @FXML
    private SwingNode waveformPlotNode;

    @FXML
    private Button snapshotButton;

    private CodaWaveformPlotManager waveformPlotManager;
    private WaveformClient waveformClient;
    private ShapeMeasurementClient shapeClient;
    private ParameterClient paramsClient;
    private PeakVelocityClient peakVelocityClient;
    private GeoMap map;
    private MapPlottingUtilities mapPlotUtilities;
    private Property<Boolean> shouldFocus = new SimpleBooleanProperty(false);
    private DirectoryChooser screenshotFolderChooser = new DirectoryChooser();

    @Autowired
    public WaveformGui(WaveformClient waveformClient, ShapeMeasurementClient shapeClient, ParameterClient paramsClient, PeakVelocityClient peakVelocityClient, GeoMap map,
            MapPlottingUtilities mapPlotUtilities, EventBus bus) {
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramsClient = paramsClient;
        this.peakVelocityClient = peakVelocityClient;
        this.map = map;
        this.mapPlotUtilities = mapPlotUtilities;
        bus.register(this);
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/WaveformGui.fxml"));
            fxmlLoader.setController(this);
            stage = new Stage(StageStyle.DECORATED);
            Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
            try {
                root = fxmlLoader.load();
                scene = new Scene(root);
                stage.setScene(scene);

                stage.setOnHiding(e -> {
                    hide();
                });

                stage.setOnShowing(e -> {
                    show();
                });

                waveformPlotNode.setOnKeyReleased(new EventHandler<KeyEvent>() {
                    @Override
                    public void handle(KeyEvent event) {
                        waveformPlotManager.triggerKeyEvent(event);
                    }
                });
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });

    }

    @Subscribe
    private void listener(WaveformSelectionEvent event) {
        if (waveformPlotManager != null) {
            if (event != null && event.getWaveformIDs() != null && !event.getWaveformIDs().isEmpty()) {
                waveformPlotManager.setOrderedWaveformIDs(event.getWaveformIDs());
                show();
                repaintWaveformWindow();
            }
        }
    }

    private void repaintWaveformWindow() {
        Platform.runLater(() -> waveformPlotNode.autosize());
    }

    @FXML
    public void initialize() {
        Label label = new Label("\uE3B0");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        snapshotButton.setGraphic(label);
        snapshotButton.setContentDisplay(ContentDisplay.CENTER);
        screenshotFolderChooser.setTitle("Screenshot Export Folder");
        SwingUtilities.invokeLater(() -> {
            waveformPlotManager = new CodaWaveformPlotManager(waveformClient, shapeClient, paramsClient, peakVelocityClient, map, mapPlotUtilities);
            waveformPlotNode.setContent(waveformPlotManager);
        });
    }

    @FXML
    private void screenshotPlots(ActionEvent e) {
        File folder = screenshotFolderChooser.showDialog(scene.getWindow());
        try {
            if (folder != null && folder.exists() && folder.isDirectory() && folder.canWrite()) {
                screenshotFolderChooser.setInitialDirectory(folder);
                Platform.runLater(() -> waveformPlotManager.exportScreenshots(folder));
            }
        } catch (SecurityException ex) {
            log.warn("Exception trying to write screenshots to folder {} : {}", folder, ex.getLocalizedMessage(), ex);
        }
    }

    public void hide() {
        Platform.runLater(() -> {
            stage.hide();
            if (waveformPlotManager != null) {
                waveformPlotManager.setVisible(false);
            }
        });
    }

    public void show() {
        Platform.runLater(() -> {
            boolean showing = stage.isShowing();
            stage.show();
            if (waveformPlotManager != null) {
                waveformPlotManager.setVisible(true);
            }
            if (!showing || shouldFocus.getValue()) {
                stage.toFront();
            }
        });
    }

    public void toFront() {
        show();
        Platform.runLater(() -> {
            stage.toFront();
        });
    }

    public Property<Boolean> focusProperty() {
        return shouldFocus;
    }
}
