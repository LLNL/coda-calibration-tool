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
package gov.llnl.gnem.apps.coda.calibration.gui;

import java.io.IOException;
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
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.LoadingGui;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.ReferenceEventLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.controllers.WaveformLoadingController;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.WaveformGui;
import gov.llnl.gnem.apps.coda.calibration.gui.util.CalibrationProgressListener;
import gov.llnl.gnem.apps.coda.calibration.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.CalibrationStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.CalibrationStatusEvent.Status;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Component
public class CodaGuiController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @FXML
    private Node rootElement;

    private WaveformLoadingController waveformLoadingController;

    private CodaParamLoadingController codaParamLoadingController;

    private ReferenceEventLoadingController refEventLoadingController;

    private CalibrationClient calibrationClient;

    private DirectoryChooser sacDirFileChooser = new DirectoryChooser();
    private FileChooser sacFileChooser = new FileChooser();
    private FileChooser referenceEventFileChooser = new FileChooser();
    private FileChooser codaParamsFileChooser = new FileChooser();
    private FileChooser psModelFileChooser = new FileChooser();
    private FileChooser fiModelFileChooser = new FileChooser();

    private EventBus bus;

    private LoadingGui loadingGui;
    private WaveformGui waveformGui;

    private Map<Long, ProgressMonitor> monitors = new HashMap<>();

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    public CodaGuiController(WaveformLoadingController waveformLoadingController, CodaParamLoadingController codaParamLoadingController, ReferenceEventLoadingController refEventLoadingController,
            CalibrationClient calibrationClient, WaveformGui waveformGui, EventBus bus) throws IOException {
        super();
        this.waveformLoadingController = waveformLoadingController;
        this.codaParamLoadingController = codaParamLoadingController;
        this.refEventLoadingController = refEventLoadingController;
        this.calibrationClient = calibrationClient;
        this.waveformGui = waveformGui;
        this.bus = bus;
        sacDirFileChooser.setTitle("Coda STACK File Directory");
        sacFileChooser.getExtensionFilters().add(new ExtensionFilter("Coda STACK Files (.sac,.env)", "*.sac", "*.env"));
        sacFileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*"));
        referenceEventFileChooser.getExtensionFilters().add(new ExtensionFilter("Reference Event Files (.txt,.dat)", "*.txt", "*.dat"));
        referenceEventFileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*"));

        codaParamsFileChooser.getExtensionFilters().add(new ExtensionFilter("Coda Parameters File (.param)", "*.param"));
        codaParamsFileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*"));

        psModelFileChooser.getExtensionFilters().add(new ExtensionFilter("MDAC Ps Model File (.txt,.dat)", "*ps*.txt", "*ps*.dat"));
        psModelFileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*"));

        fiModelFileChooser.getExtensionFilters().add(new ExtensionFilter("MDAC Ps Model File (.txt,.dat)", "*ps*.txt", "*ps*.dat"));
        fiModelFileChooser.getExtensionFilters().add(new ExtensionFilter("All Files", "*"));
    }

    @FXML
    private void openWaveformLoadingWindow() {
        Optional.ofNullable(sacFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(waveformLoadingController::loadFiles);
    }

    @FXML
    private void openWaveformDisplay() {
        waveformGui.show();
    }

    @FXML
    private void openWaveformDirectoryLoadingWindow() {
        Optional.ofNullable(sacDirFileChooser.showDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(waveformLoadingController::loadFiles);
    }

    @FXML
    private void openReferenceEventLoadingWindow() {
        Optional.ofNullable(referenceEventFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(refEventLoadingController::loadFiles);
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
        calibrationClient.runCalibration(Boolean.FALSE).subscribe(value -> log.trace(value.toString()), err -> log.trace(err.getMessage(), err));
    }

    @FXML
    private void clearData() {
        calibrationClient.clearData().subscribe(value -> log.trace(value.toString()), err -> log.trace(err.getMessage(), err));
    }

    @FXML
    private void runAutoPickingCalibration() {
        calibrationClient.runCalibration(Boolean.TRUE).subscribe(value -> log.trace(value.toString()), err -> log.trace(err.getMessage(), err));
    }

    @FXML
    public void initialize() {
        rootElement.setOnDragOver(event -> {
            if (event.getGestureSource() != rootElement && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });

        rootElement.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getGestureSource() != rootElement && event.getDragboard().hasFiles()) {
                waveformLoadingController.loadFiles(event.getDragboard().getFiles());
                codaParamLoadingController.loadFiles(event.getDragboard().getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        try {
            loadingGui = new LoadingGui();
        } catch (IllegalStateException e) {
            log.error("Unable to instantiate loading display {}", e.getMessage(), e);
        }
        bus.register(this);
    }

    //TODO: Move this to a controller
    @Subscribe
    private void listener(CalibrationStatusEvent event) {

        if (!monitors.containsKey(event.getId()) && event.getStatus() == Status.STARTING) {
            CalibrationProgressListener eventMonitor = new CalibrationProgressListener(bus, event);
            ProgressMonitor monitor = new ProgressMonitor("Calibration Progress " + event.getId(), eventMonitor);
            monitors.put(event.getId(), monitor);
            loadingGui.addProgressMonitor(monitor);
        }

        if (event.getStatus() == Status.COMPLETE || event.getStatus() == Status.ERROR) {
            final ProgressMonitor monitor = monitors.remove(event.getId());
            service.schedule(() -> loadingGui.removeProgressMonitor(monitor), 15, TimeUnit.MINUTES);
        }
    }

    @PreDestroy
    private void cleanUp() {
        service.shutdownNow();
    }
}
