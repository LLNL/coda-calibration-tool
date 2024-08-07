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
import java.util.function.Consumer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.GuiApplication.ApplicationMode;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.CodaParamLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.DataController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.EnvelopeLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.EventTableController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.MapListeningController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.MeasuredMwsController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.PathController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.ReferenceEventLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.RefreshableController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.ScreenshotEnabledController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.ShapeController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.SiteController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.SpectraRatioLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.parameters.ParametersController;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.events.CalibrationStageShownEvent;
import gov.llnl.gnem.apps.coda.calibration.gui.events.MapIconActivationCallback;
import gov.llnl.gnem.apps.coda.calibration.gui.events.MapPolygonChangeHandler;
import gov.llnl.gnem.apps.coda.calibration.gui.events.UpdateMapPolygonEvent;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.CertLeafletMapController;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.LeafletMapController;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.WaveformGui;
import gov.llnl.gnem.apps.coda.calibration.gui.util.CalibrationProgressListener;
import gov.llnl.gnem.apps.coda.calibration.gui.util.FileDialogs;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationStatusEvent.Status;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent;
import gov.llnl.gnem.apps.coda.common.gui.controllers.ProgressGui;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.envelope.gui.EnvelopeGuiController;
import gov.llnl.gnem.apps.coda.envelope.gui.LoadRatioEventsGuiController;
import gov.llnl.gnem.apps.coda.spectra.gui.RatioStatusProgressListener;
import gov.llnl.gnem.apps.coda.spectra.gui.SpectraRatioGuiController;
import jakarta.annotation.PreDestroy;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Component
public class CodaGuiController {

    private static final Logger log = LoggerFactory.getLogger(CodaGuiController.class);

    private static final String SCREENSHOT_TITLE = "CERT_Screenshot";

    private static final String ABOUT_TEXT = "Version 1.0.22";

    @FXML
    private Node rootElement;

    private LeafletMapController cctMapController;
    private CertLeafletMapController certMapController;
    private WaveformGui waveformGui;
    private LoadRatioEventsGuiController ratioLoadGui;
    private DataController data;
    private ParametersController param;
    private ShapeController shape;
    private PathController path;
    private SiteController site;
    private SpectraRatioGuiController spectraGui;
    private MeasuredMwsController measuredMws;

    @FXML
    private TabPane mainTabPane;

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

    @FXML
    private Button showMapButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button centerMapBtn;

    @FXML
    private Button snapshotButton;

    @FXML
    private CheckMenuItem waveformFocus;

    private Consumer<File> activeTabScreenshot;

    private Runnable activeTabRefresh;

    private EnvelopeGuiController envelopeGui;

    private Label activeMapIcon;

    private Label showMapIcon;

    private Label refreshLabel;

    private WaveformClient waveformClient;

    private ParameterClient configClient;

    private EnvelopeLoadingController envelopeLoadingController;

    private CodaParamLoadingController codaParamLoadingController;

    private ReferenceEventLoadingController refEventLoadingController;

    private SpectraRatioLoadingController spectraRatioLoadingController;

    private CalibrationClient calibrationClient;

    private DirectoryChooser sacDirFileChooser = new DirectoryChooser();
    private DirectoryChooser screenshotFolderChooser = new DirectoryChooser();

    private FileChooser sacFileChooser = new FileChooser();
    private FileChooser codaParamsFileChooser = new FileChooser();
    private FileChooser codaJsonParamsFileChooser = new FileChooser();
    private FileChooser referenceEventFileChooser = new FileChooser();
    private FileChooser psModelFileChooser = new FileChooser();
    private FileChooser fiModelFileChooser = new FileChooser();
    private DirectoryChooser ratioDirFileChooser = new DirectoryChooser();
    private FileChooser ratioFileChooser = new FileChooser();
    private final ExtensionFilter allFilesFilter = new ExtensionFilter("All Files", "*.*");

    private ParamExporter paramExporter;

    private EventBus bus;

    private ProgressGui loadingGui;

    private Map<Long, ProgressMonitor> calibrationMonitors = new HashMap<>();
    private Map<Long, ProgressMonitor> ratioMonitors = new HashMap<>();

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("CodaGui-Scheduled");
        thread.setDaemon(true);
        return thread;
    });

    private Stage manualStage;

    private Dialog<Void> aboutDialog;

    private Environment env;

    private WebView manualWebview;

    private HostnameVerifier hostnameVerifier;

    private Runnable eventTableRefreshFunction;

    @Autowired
    public CodaGuiController(LeafletMapController cctMapController, CertLeafletMapController certMapController, WaveformClient waveformClient, EnvelopeLoadingController waveformLoadingController,
            CodaParamLoadingController codaParamLoadingController, ReferenceEventLoadingController refEventLoadingController, SpectraRatioLoadingController spectraRatioLoadingController,
            CalibrationClient calibrationClient, ParamExporter paramExporter, LoadRatioEventsGuiController ratioLoadGui, WaveformGui waveformGui, DataController data, EventTableController eventTable,
            ParametersController param, ShapeController shape, PathController path, SiteController site, MeasuredMwsController measuredMws, ParameterClient configClient,
            EnvelopeGuiController envelopeGui, SpectraRatioGuiController spectraGui, HostnameVerifier hostnameVerifier, SSLContext sslContext, Environment env, EventBus bus) {
        this.waveformClient = waveformClient;
        this.cctMapController = cctMapController;
        this.certMapController = certMapController;
        this.envelopeLoadingController = waveformLoadingController;
        this.codaParamLoadingController = codaParamLoadingController;
        this.refEventLoadingController = refEventLoadingController;
        this.spectraRatioLoadingController = spectraRatioLoadingController;
        this.calibrationClient = calibrationClient;
        this.paramExporter = paramExporter;
        this.waveformGui = waveformGui;
        this.ratioLoadGui = ratioLoadGui;
        this.spectraGui = spectraGui;
        this.data = data;
        this.param = param;
        this.shape = shape;
        this.path = path;
        this.site = site;
        this.measuredMws = measuredMws;
        this.configClient = configClient;
        this.envelopeGui = envelopeGui;
        this.env = env;
        this.bus = bus;
        bus.register(this);

        //This is here just to make sure this class is built before the downstream classes so the HTTPS
        // connections get the right SSLContext factories
        this.hostnameVerifier = hostnameVerifier;

        activeTabRefresh = data.getRefreshFunction();

        sacDirFileChooser.setTitle("Coda STACK File Directory");
        screenshotFolderChooser.setTitle("Screenshot Export Folder");

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

        ratioFileChooser.setTitle("Load Coda Ratio Project File");
        ratioFileChooser.getExtensionFilters().add(new ExtensionFilter("Coda Ratio Project File (.json)", "*.json"));
        ratioFileChooser.getExtensionFilters().add(allFilesFilter);

        ratioDirFileChooser.setTitle("Directory to save Coda Ratio Project");

        eventTableRefreshFunction = () -> this.bus.post(new RefreshEventTableAction());
    }

    @FXML
    private void changeAppMode() {
        GuiApplication.changeApplicationMode();

        if (GuiApplication.getStartupMode() == ApplicationMode.CCT) {
            addEnabledTabListeners(shapeTab, shape);
            addEnabledTabListeners(pathTab, path);
            addEnabledTabListeners(siteTab, site);
            addEnabledTabListeners(measuredMwsTab, measuredMws);
            Platform.runLater(() -> {
                dataTab.setGraphic(activeMapIcon);
                showMapButton.setGraphic(showMapIcon);
                refreshButton.setGraphic(refreshLabel);
            });
        } else {
            activeTabRefresh = data.getRefreshFunction();

            spectraGui.loadEnvelopes();
            siteTab.setOnSelectionChanged(e -> {
                if (siteTab.isSelected()) {
                    activeTabRefresh = eventTableRefreshFunction;
                    activeTabRefresh.run();
                    activeTabScreenshot = basicPngScreenshot(siteTab);
                } else {
                    siteTab.setGraphic(null);
                }
            });

            Platform.runLater(() -> {
                dataTab.setGraphic(activeMapIcon);
                mainTabPane.getTabs().remove(shapeTab);
                mainTabPane.getTabs().remove(pathTab);
                mainTabPane.getTabs().remove(measuredMwsTab);
            });
        }
    }

    @FXML
    private void showMapWindow() {
        Platform.runLater(() -> {
            if (GuiApplication.getStartupMode() == ApplicationMode.CCT) {
                if (cctMapController != null) {
                    cctMapController.show();
                    cctMapController.fitViewToActiveShapes();
                }
            } else if (certMapController != null) {
                certMapController.show();
                certMapController.fitViewToActiveShapes();
            }
        });
    }

    @FXML
    private void centerMap() {
        Platform.runLater(() -> {
            if (GuiApplication.getStartupMode() == ApplicationMode.CERT && certMapController != null) {
                certMapController.show();
                certMapController.fitViewToActiveShapes();
            }
        });
    }

    @FXML
    private void openRatioSpectraFileTool(ActionEvent e) {
        ratioLoadGui.toFront();
    }

    @FXML
    private void openAbout(ActionEvent e) {
        if (aboutDialog == null) {
            Dialog<Void> aboutDialog = new Dialog<>();
            aboutDialog.getDialogPane().setPrefWidth(800);
            aboutDialog.getDialogPane().setPrefHeight(500);
            aboutDialog.setTitle("About");
            aboutDialog.setHeaderText(ABOUT_TEXT);

            TextArea licenses = new TextArea();

            licenses.setEditable(false);
            licenses.setText(AboutText.LICENSE_INFO);
            aboutDialog.getDialogPane().setContent(licenses);
            aboutDialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
            aboutDialog.show();
        }
    }

    @FXML
    private void openManual(ActionEvent e) {
        Platform.runLater(() -> {
            if (manualStage == null) {
                manualStage = new Stage(StageStyle.DECORATED);
                manualStage.setTitle("Coda Calibration Tool Documentation");
                try {
                    manualStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/coda_256x256.png")));
                } catch (NullPointerException npe) {
                    log.trace("Unable to load icon for manual scene. {}", npe);
                }

                manualWebview = new WebView();

                manualWebview.getEngine().setJavaScriptEnabled(true);
                manualWebview.getEngine().getLoadWorker().stateProperty().addListener((o, ov, nv) -> {
                    if (nv == Worker.State.FAILED) {
                        log.error("WebView Failed: ", manualWebview.getEngine().getLoadWorker().getException());
                    }
                });

                AnchorPane pane = new AnchorPane(manualWebview);
                Scene scene = new Scene(pane, 1280, 720);
                pane.prefWidthProperty().bind(scene.widthProperty());
                pane.prefHeightProperty().bind(scene.heightProperty());

                manualWebview.prefWidthProperty().bind(pane.widthProperty());
                manualWebview.prefHeightProperty().bind(pane.heightProperty());
                manualStage.setScene(scene);
            }
            manualWebview.getEngine().load("https://" + env.getProperty("server.address") + ":" + env.getProperty("server.port") + "/index.html");

            manualStage.show();
            manualStage.toFront();
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
    private void openWaveformLoadingWindow() {
        Optional.ofNullable(sacFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(envelopeLoadingController::loadFiles);
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
            } catch (IOException ex) {
                FileDialogs.fileIoErrorAlert(ex);
            }
        }
    }

    @FXML
    private void openRatiosLoadingWindow() {
        Optional.ofNullable(ratioFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(spectraRatioLoadingController::loadFiles);
    }

    @FXML
    private void openRatiosSavingWindow() {
        Optional.ofNullable(ratioDirFileChooser.showDialog(rootElement.getScene().getWindow())).ifPresent(spectraRatioLoadingController::saveToDirectory);
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
        calibrationClient.runCalibration(Boolean.FALSE).doOnError(err -> log.trace(err.getMessage(), err)).subscribe();
    }

    @FXML
    private void measureMws() {
        Platform.runLater(measuredMws::reload);
    }

    @FXML
    private void clearData() {
        calibrationClient.clearData().subscribe(val -> {
        }, err -> log.trace(err.getMessage(), err), () -> data.getRefreshFunction().run());
        spectraGui.loadEnvelopes();
        activeTabRefresh.run();
    }

    @FXML
    private void clearAutopicks() {
        waveformClient.clearAutoPicks().subscribe(val -> {
        }, err -> log.trace(err.getMessage(), err), () -> data.getRefreshFunction().run());
    }

    @FXML
    private void runAutoPickingCalibration() {
        calibrationClient.runCalibration(Boolean.TRUE).subscribe(value -> {
        }, err -> log.trace(err.getMessage(), err));
    }

    @FXML
    public void initialize() {

        waveformFocus.selectedProperty().bindBidirectional(waveformGui.focusProperty());

        certMapController.registerEventCallback(new MapIconActivationCallback(waveformClient));
        certMapController.registerEventCallback(new MapPolygonChangeHandler(configClient));

        cctMapController.registerEventCallback(new MapIconActivationCallback(waveformClient));
        cctMapController.registerEventCallback(new MapPolygonChangeHandler(configClient));

        activeMapIcon = makeMapLabel();
        showMapIcon = makeMapLabel();
        refreshLabel = makeRefreshLabel();

        snapshotButton.setGraphic(makeSnapshotLabel());
        snapshotButton.setContentDisplay(ContentDisplay.CENTER);

        activeTabScreenshot = folder -> SnapshotUtils.writePng(folder, new Pair<>(dataTab.getText(), dataTab.getContent()));

        paramTab.setOnSelectionChanged(e -> {
            if (paramTab.isSelected()) {
                cctMapController.clearIcons();
                certMapController.clearIcons();
                activeTabRefresh = param.getRefreshFunction();
                activeTabScreenshot = folder -> SnapshotUtils.writePng(folder, new Pair<>(paramTab.getText(), paramTab.getContent()));
            }
        });

        data.setVisible(true);
        addEnabledTabListeners(dataTab, data);

        if (GuiApplication.getStartupMode() == ApplicationMode.CCT) {
            dataTab.setGraphic(activeMapIcon);
            addEnabledTabListeners(shapeTab, shape);
            addEnabledTabListeners(pathTab, path);
            addEnabledTabListeners(siteTab, site);
            addEnabledTabListeners(measuredMwsTab, measuredMws);
        } else {
            siteTab.setOnSelectionChanged(e -> {
                if (siteTab.isSelected()) {
                    activeTabRefresh = eventTableRefreshFunction;
                    activeTabRefresh.run();
                    activeTabScreenshot = basicPngScreenshot(siteTab);
                } else {
                    siteTab.setGraphic(null);
                }
            });
            Platform.runLater(() -> {
                dataTab.setGraphic(activeMapIcon);
                mainTabPane.getTabs().remove(shapeTab);
                mainTabPane.getTabs().remove(pathTab);
                mainTabPane.getTabs().remove(measuredMwsTab);
            });
        }

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

                if (GuiApplication.getStartupMode() == ApplicationMode.CERT) {
                    envelopeLoadingController.setCompletionCallback(() -> {
                        spectraGui.loadEnvelopes();
                        if (certMapController != null) {
                            certMapController.show();
                            certMapController.fitViewToActiveShapes();
                        }
                    });
                }

                codaParamLoadingController.loadFiles(event.getDragboard().getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        try {
            loadingGui = ProgressGui.getInstance();
        } catch (IllegalStateException e) {
            log.error("Unable to instantiate loading display {}", e.getMessage(), e);
        }

    }

    private void addEnabledTabListeners(Tab tab, MapListeningController controller) {
        tab.setOnSelectionChanged(e -> {
            if (tab.isSelected()) {
                controller.setVisible(true);
                controller.refreshView();
                tab.setGraphic(activeMapIcon);
                if (controller instanceof RefreshableController) {
                    activeTabRefresh = () -> {
                        ((RefreshableController) controller).getRefreshFunction().run();
                        eventTableRefreshFunction.run();
                    };
                } else {
                    activeTabRefresh = () -> controller.refreshView();
                }
                if (controller instanceof ScreenshotEnabledController) {
                    activeTabScreenshot = ((ScreenshotEnabledController) controller).getScreenshotFunction();
                } else {
                    activeTabScreenshot = basicPngScreenshot(tab);
                }
            } else {
                tab.setGraphic(null);
                controller.setVisible(false);
            }
        });
    }

    private Consumer<File> basicPngScreenshot(Tab tab) {
        return folder -> SnapshotUtils.writePng(folder, new Pair<>(tab.getText(), tab.getContent()));
    }

    @FXML
    private void refreshTab(ActionEvent e) {
        activeTabRefresh.run();
        if (rootElement.getParent() != null) {
            rootElement.getParent().requestLayout();
        }
    }

    @FXML
    private void openEnvelopeTool(ActionEvent e) {
        envelopeGui.toFront();
    }

    @FXML
    private void snapshotTab(ActionEvent e) {
        File folder = screenshotFolderChooser.showDialog(rootElement.getScene().getWindow());
        try {
            if (folder != null && folder.exists() && folder.isDirectory() && folder.canWrite()) {
                screenshotFolderChooser.setInitialDirectory(folder);
                if (GuiApplication.getStartupMode() == ApplicationMode.CERT) {
                    SnapshotUtils.writePng(folder, new Pair<>(SCREENSHOT_TITLE + SnapshotUtils.getTimestampWithLeadingSeparator(), rootElement));
                }
                Platform.runLater(() -> activeTabScreenshot.accept(folder));
            }
        } catch (SecurityException ex) {
            log.warn("Exception trying to write screenshots to folder {} : {}", folder, ex.getLocalizedMessage(), ex);
        }
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

    private Label makeSnapshotLabel() {
        Label label = new Label("\uE3B0");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        return label;
    }

    @Subscribe
    private void listener(UpdateMapPolygonEvent event) {
        if (event != null && event.getGeoJSON() != null && !event.getGeoJSON().isEmpty()) {
            if (certMapController != null) {
                certMapController.setPolygonGeoJSON(event.getGeoJSON());
            }
            if (cctMapController != null) {
                cctMapController.setPolygonGeoJSON(event.getGeoJSON());
            }
        }
    }

    //TODO: Move this to a controller
    @Subscribe
    private void listener(CalibrationStatusEvent event) {
        if (!calibrationMonitors.containsKey(event.getId()) && event.getStatus() == Status.STARTING) {
            CalibrationProgressListener eventMonitor = new CalibrationProgressListener(bus, event);
            ProgressMonitor monitor = new ProgressMonitor("Calibration Progress " + event.getId(), eventMonitor);
            monitor.addCancelCallback(() -> calibrationClient.cancelCalibration(event.getId()).subscribe());
            calibrationMonitors.put(event.getId(), monitor);
            loadingGui.addProgressMonitor(monitor);
            loadingGui.show();
        }

        if (event.getStatus() == Status.COMPLETE || event.getStatus() == Status.ERROR) {
            final ProgressMonitor monitor = calibrationMonitors.remove(event.getId());
            if (monitor != null) {
                monitor.setProgressStage("Finished");
                monitor.clearCancelCallbacks();
                service.schedule(() -> loadingGui.removeProgressMonitor(monitor), 15, TimeUnit.MINUTES);
            }
        } else {
            ProgressMonitor monitor = calibrationMonitors.get(event.getId());
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
    private void listener(RatioStatusEvent event) {

        ProgressMonitor monitor = ratioMonitors.get(event.getId());

        if (monitor == null && event.getStatus() == RatioStatusEvent.Status.STARTING) {
            RatioStatusProgressListener eventMonitor = new RatioStatusProgressListener(bus, event);
            ProgressMonitor newMonitor = new ProgressMonitor("Ratio Measurement Progress " + event.getId(), eventMonitor);
            loadingGui.addProgressMonitor(newMonitor);
            ratioMonitors.put(event.getId(), newMonitor);
            service.schedule(() -> loadingGui.removeProgressMonitor(monitor), 15, TimeUnit.MINUTES);
            loadingGui.show();
        }

        if (monitor != null && (event.getStatus() == RatioStatusEvent.Status.COMPLETE || event.getStatus() == RatioStatusEvent.Status.ERROR)) {
            monitor.setProgressStage("Finished");
            service.schedule(() -> loadingGui.removeProgressMonitor(monitor), 15, TimeUnit.MINUTES);
        } else if (monitor != null) {
            switch (event.getStatus()) {
            case STARTING:
                monitor.setProgressStage("Starting...");
                break;
            case PROCESSING:
                monitor.setProgressStage("Processing...");
                break;
            default:
                break;
            }
        }
    }

    @Subscribe
    private void listener(CalibrationStageShownEvent evt) {
        Platform.runLater(() -> {
            if (showMapButton.getGraphic() == null) {
                showMapButton.setGraphic(showMapIcon);
            }
            if (refreshButton.getGraphic() == null) {
                refreshButton.setGraphic(refreshLabel);
            }
        });
    }

    @PreDestroy
    private void cleanUp() {
        service.shutdownNow();
    }
}
