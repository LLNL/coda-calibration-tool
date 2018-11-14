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
package gov.llnl.gnem.apps.coda.envelope.gui;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.envelope.gui.controllers.WaveformLoadingController;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.input.TransferMode;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Component
public class EnvelopeGuiController {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeGuiController.class);

    @FXML
    private Node rootElement;

    private WaveformLoadingController waveformLoadingController;

    private DirectoryChooser sacDirFileChooser = new DirectoryChooser();
    private FileChooser sacFileChooser = new FileChooser();

    private EventBus bus;
    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    @Autowired
    public EnvelopeGuiController(WaveformLoadingController waveformLoadingController, EventBus bus) throws IOException {
        super();
        this.waveformLoadingController = waveformLoadingController;
        this.bus = bus;
        sacDirFileChooser.setTitle("SAC File Directory");
        sacFileChooser.getExtensionFilters().addAll(new ExtensionFilter("Sac files (.sac)", "*.sac"), new ExtensionFilter("All files", "*.*"));
    }

    @FXML
    private void openWaveformLoadingWindow() {
        Optional.ofNullable(sacFileChooser.showOpenMultipleDialog(rootElement.getScene().getWindow())).ifPresent(waveformLoadingController::loadFiles);
    }

    @FXML
    private void openFailureReportDisplay() {
        bus.post(new ShowFailureReportEvent());
    }

    @FXML
    private void openWaveformDirectorySavingWindow() {
        Optional.ofNullable(sacDirFileChooser.showDialog(rootElement.getScene().getWindow())).ifPresent(waveformLoadingController::saveToDirectory);
    }

    @FXML
    private void openWaveformDirectoryLoadingWindow() {
        Optional.ofNullable(sacDirFileChooser.showDialog(rootElement.getScene().getWindow())).map(Collections::singletonList).ifPresent(waveformLoadingController::loadFiles);
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
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        bus.register(this);
    }

    @PreDestroy
    private void cleanUp() {
        service.shutdownNow();
    }
}
