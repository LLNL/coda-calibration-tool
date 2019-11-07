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
package gov.llnl.gnem.apps.coda.calibration.gui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.controllers.CodaParamLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.DataController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.EnvelopeLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.MapListeningController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.MeasuredMwsController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.PathController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.ReferenceEventLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.ShapeController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.SiteController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.parameters.ParametersController;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.events.CalibrationStageShownEvent;
import gov.llnl.gnem.apps.coda.calibration.gui.events.MapIconActivationCallback;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.WaveformGui;
import gov.llnl.gnem.apps.coda.calibration.gui.util.CalibrationProgressListener;
import gov.llnl.gnem.apps.coda.calibration.gui.util.FileDialogs;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationStatusEvent.Status;
import gov.llnl.gnem.apps.coda.common.gui.controllers.ProgressGui;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Component
public class CodaGuiController {

    private static final Logger log = LoggerFactory.getLogger(CodaGuiController.class);

    @FXML
    private Node rootElement;

    private WaveformGui waveformGui;
    private DataController data;
    private ShapeController shape;
    private PathController path;
    private SiteController site;
    private MeasuredMwsController measuredMws;

    @FXML
    private Tab dataTab;

    @FXML
    private Tab paramTab;

    @FXML
    private Tab shapeTab;

    @FXML
    private Tab pathTab;

    @FXML
    private Tab siteTab;

    @FXML
    private Tab measuredMwsTab;

    private Runnable dataRefresh;
    private Runnable paramRefresh;
    private Runnable shapeRefresh;
    private Runnable pathRefresh;
    private Runnable siteRefresh;
    private Runnable measuredMwsRefresh;
    private Runnable activeTabRefresh;

    @FXML
    private Button showMapButton;

    @FXML
    private Button refreshButton;

    @FXML
    private CheckMenuItem waveformFocus;

    private Label activeMapIcon;

    private Label showMapIcon;

    private GeoMap mapController;

    private WaveformClient waveformClient;

    private EnvelopeLoadingController envelopeLoadingController;

    private CodaParamLoadingController codaParamLoadingController;

    private ReferenceEventLoadingController refEventLoadingController;

    private CalibrationClient calibrationClient;

    private DirectoryChooser sacDirFileChooser = new DirectoryChooser();
    private FileChooser sacFileChooser = new FileChooser();
    private FileChooser codaParamsFileChooser = new FileChooser();
    private FileChooser codaJsonParamsFileChooser = new FileChooser();
    private FileChooser referenceEventFileChooser = new FileChooser();
    private FileChooser psModelFileChooser = new FileChooser();
    private FileChooser fiModelFileChooser = new FileChooser();
    private final ExtensionFilter allFilesFilter = new ExtensionFilter("All Files", "*.*");

    private ParamExporter paramExporter;

    private EventBus bus;

    private boolean initialized = false;

    private ProgressGui loadingGui;

    private Map<Long, ProgressMonitor> monitors = new HashMap<>();

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("CodaGui-Scheduled");
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    public CodaGuiController(GeoMap mapController, WaveformClient waveformClient, EnvelopeLoadingController waveformLoadingController, CodaParamLoadingController codaParamLoadingController,
            ReferenceEventLoadingController refEventLoadingController, CalibrationClient calibrationClient, ParamExporter paramExporter, WaveformGui waveformGui, DataController data,
            ParametersController param, ShapeController shape, PathController path, SiteController site, MeasuredMwsController measuredMws, EventBus bus) {
        super();
        this.mapController = mapController;
        this.waveformClient = waveformClient;
        this.envelopeLoadingController = waveformLoadingController;
        this.codaParamLoadingController = codaParamLoadingController;
        this.refEventLoadingController = refEventLoadingController;
        this.calibrationClient = calibrationClient;
        this.paramExporter = paramExporter;
        this.waveformGui = waveformGui;
        this.data = data;
        this.shape = shape;
        this.path = path;
        this.site = site;
        this.measuredMws = measuredMws;
        this.bus = bus;
        bus.register(this);

        dataRefresh = data.getRefreshFunction();
        paramRefresh = param.getRefreshFunction();
        shapeRefresh = shape.getRefreshFunction();
        pathRefresh = path.getRefreshFunction();
        siteRefresh = site.getRefreshFunction();
        measuredMwsRefresh = measuredMws.getRefreshFunction();
        activeTabRefresh = dataRefresh;

        sacDirFileChooser.setTitle("Coda STACK File Directory");
        sacFileChooser.getExtensionFilters().add(new ExtensionFilter("Coda STACK Files (.sac,.env)", "*.sac", "*.env"));
        sacFileChooser.getExtensionFilters().add(allFilesFilter);

        codaJsonParamsFileChooser.getExtensionFilters().add(new ExtensionFilter("Coda Parameters File (.json)", "*.json"));
        codaJsonParamsFileChooser.getExtensionFilters().add(allFilesFilter);

        codaParamsFileChooser.getExtensionFilters().add(new ExtensionFilter("Coda Parameters File (.param)", "*.param"));
        codaParamsFileChooser.getExtensionFilters().add(allFilesFilter);

        psModelFileChooser.getExtensionFilters().add(new ExtensionFilter("MDAC Ps Model File (.txt,.dat)", "*ps*.txt", "*ps*.dat"));
        psModelFileChooser.getExtensionFilters().add(allFilesFilter);

        fiModelFileChooser.getExtensionFilters().add(new ExtensionFilter("MDAC Ps Model File (.txt,.dat)", "*ps*.txt", "*ps*.dat"));
        fiModelFileChooser.getExtensionFilters().add(allFilesFilter);

        referenceEventFileChooser.getExtensionFilters().add(new ExtensionFilter("Reference Event Files (.txt,.dat)", "*.txt", "*.dat"));
        referenceEventFileChooser.getExtensionFilters().add(allFilesFilter);
    }

    @FXML
    private void openWaveformLoadingWindow() {
        Optional.ofNullable(sacFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(envelopeLoadingController::loadFiles);
    }

    @FXML
    private void showMapWindow() {
        Platform.runLater(() -> {
            if (mapController != null) {
                mapController.show();
                mapController.fitViewToActiveShapes();
            }
        });
    }

    @FXML
    private void openWaveformDisplay() {
        waveformGui.toFront();
    }

    @FXML
    private void openFailureReportDisplay() {
        bus.post(new ShowFailureReportEvent());
    }

    @FXML
    private void openCalibrationDataSavingWindow(ActionEvent e) {
        //Save all parameters to an archive file and prompt the user about where to save it.
        File selectedFile = FileDialogs.openFileSaveDialog("Calibration_Data", ".zip", rootElement.getScene().getWindow());
        File exportArchive;

        if (selectedFile != null) {
            try {
                if (FileDialogs.ensureFileIsWritable(selectedFile)) {
                    exportArchive = paramExporter.createExportArchive();
                    if (exportArchive != null) {
                        Files.move(exportArchive.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } catch (IOException e1) {
                FileDialogs.fileIoErrorAlert(e1);
            }
        }
    }

    @FXML
    private void openWaveformDirectorySavingWindow() {
        Optional.ofNullable(sacDirFileChooser.showDialog(rootElement.getScene().getWindow())).ifPresent(envelopeLoadingController::saveToDirectory);
    }

    @FXML
    private void openMeasuredMwDirectorySavingWindow() {
        measuredMws.exportMws();
    }

    @FXML
    private void openWaveformDirectoryLoadingWindow() {
        Optional.ofNullable(sacDirFileChooser.showDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(envelopeLoadingController::loadFiles);
    }

    @FXML
    private void openReferenceEventLoadingWindow() {
        Optional.ofNullable(referenceEventFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(refEventLoadingController::loadFiles);
    }

    @FXML
    private void openCodaJsonParamWindow() {
        Optional.ofNullable(codaJsonParamsFileChooser.showOpenDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(codaParamLoadingController::loadFiles);
    }

    @FXML
    private void openCodaParamWindow() {
        Optional.ofNullable(codaParamsFileChooser.showOpenDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(codaParamLoadingController::loadFiles);
    }

    @FXML
    private void openMdacPsWindow() {
        Optional.ofNullable(psModelFileChooser.showOpenDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(codaParamLoadingController::loadFiles);
    }

    @FXML
    private void openMdacFiWindow() {
        Optional.ofNullable(fiModelFileChooser.showOpenDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(codaParamLoadingController::loadFiles);
    }

    @FXML
    private void runCalibration() {
        calibrationClient.runCalibration(Boolean.FALSE).subscribe(value -> log.trace(value), err -> log.trace(err.getMessage(), err));
    }

    @FXML
    private void measureMws() {
        measuredMws.getRefreshFunction().run();
    }

    @FXML
    private void clearData() {
        calibrationClient.clearData().subscribe(value -> log.trace(value), err -> log.trace(err.getMessage(), err), () -> dataRefresh.run());
    }

    @FXML
    private void runAutoPickingCalibration() {
        calibrationClient.runCalibration(Boolean.TRUE).subscribe(value -> log.trace(value), err -> log.trace(err.getMessage(), err));
    }

    @FXML
    public void initialize() {

        waveformFocus.selectedProperty().bindBidirectional(waveformGui.focusProperty());

        mapController.registerEventCallback(new MapIconActivationCallback(waveformClient));

        activeMapIcon = makeMapLabel();
        showMapIcon = makeMapLabel();

        addMapEnabledTabListeners(dataTab, data, dataRefresh);
        data.setVisible(true);

        paramTab.setOnSelectionChanged(e -> {
            if (paramTab.isSelected()) {
                mapController.clearIcons();
                activeTabRefresh = paramRefresh;
            }
        });

        addMapEnabledTabListeners(shapeTab, shape, shapeRefresh);
        addMapEnabledTabListeners(pathTab, path, pathRefresh);
        addMapEnabledTabListeners(siteTab, site, siteRefresh);
        addMapEnabledTabListeners(measuredMwsTab, measuredMws, measuredMwsRefresh);

        rootElement.setOnDragOver(event -> {
            if (event.getGestureSource() != rootElement && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });

        rootElement.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getGestureSource() != rootElement && event.getDragboard().hasFiles()) {
                envelopeLoadingController.loadFiles(event.getDragboard().getFiles());
                codaParamLoadingController.loadFiles(event.getDragboard().getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        try {
            loadingGui = new ProgressGui();
        } catch (IllegalStateException e) {
            log.error("Unable to instantiate loading display {}", e.getMessage(), e);
        }

    }

    private void addMapEnabledTabListeners(Tab tab, MapListeningController controller, Runnable runnable) {
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                controller.setVisible(true);
                controller.refreshView();
                tab.setGraphic(activeMapIcon);
                activeTabRefresh = runnable;
            } else {
                tab.setGraphic(null);
                controller.setVisible(false);
            }
        });
    }

    @FXML
    private void refreshTab(ActionEvent e) {
        activeTabRefresh.run();
    }

    private Label makeMapLabel() {
        Label mapLabel = new Label("\uE55B");
        mapLabel.getStyleClass().add("material-icons-medium");
        mapLabel.setMaxHeight(16);
        mapLabel.setMinWidth(16);
        return mapLabel;
    }

    private Label makeRefreshLabel() {
        Label label = new Label("\uE5D5");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        return label;
    }

    //TODO: Move this to a controller
    @Subscribe
    private void listener(CalibrationStatusEvent event) {
        if (!monitors.containsKey(event.getId()) && event.getStatus() == Status.STARTING) {
            CalibrationProgressListener eventMonitor = new CalibrationProgressListener(bus, event);
            ProgressMonitor monitor = new ProgressMonitor("Calibration Progress " + event.getId(), eventMonitor);
            monitors.put(event.getId(), monitor);
            loadingGui.addProgressMonitor(monitor);
            loadingGui.show();
        }

        if (event.getStatus() == Status.COMPLETE || event.getStatus() == Status.ERROR) {
            final ProgressMonitor monitor = monitors.remove(event.getId());
            if (monitor != null) {
                monitor.setProgressStage("Finished");
                service.schedule(() -> loadingGui.removeProgressMonitor(monitor), 15, TimeUnit.MINUTES);
            }
        } else {
            ProgressMonitor monitor = monitors.get(event.getId());
            if (monitor != null) {
                switch (event.getStatus()) {
                case PEAK_STARTING:
                    monitor.setProgressStage("Peak starting");
                    break;
                case SHAPE_STARTING:
                    monitor.setProgressStage("Shape starting");
                    break;
                case PATH_STARTING:
                    monitor.setProgressStage("Path starting");
                    break;
                case SITE_STARTING:
                    monitor.setProgressStage("Site starting");
                    break;
                default:
                    break;
                }
            }
        }
    }

    @Subscribe
    private void listener(CalibrationStageShownEvent evt) {
        if (!initialized) {
            Platform.runLater(() -> {
                dataTab.setGraphic(activeMapIcon);
                if (showMapButton.getGraphic() == null) {
                    showMapButton.setGraphic(showMapIcon);
                }
                if (refreshButton.getGraphic() == null) {
                    refreshButton.setGraphic(makeRefreshLabel());
                }
                initialized = true;
            });
        }
    }

    @PreDestroy
    private void cleanUp() {
        service.shutdownNow();
    }
}
