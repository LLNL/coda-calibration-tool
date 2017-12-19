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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers.parameters;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.SwftStyleParamExporter;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;

@Component
public class ParametersController {

    @FXML
    private StackPane parameters;

    @FXML
    private SharedBandController sharedBandController;

    @FXML
    private ModelController modelController;

    @FXML
    private SiteBandController siteBandController;

    private SwftStyleParamExporter paramExporter;

    @Autowired
    public ParametersController(SwftStyleParamExporter paramExporter) {
        this.paramExporter = paramExporter;
    }

    @FXML
    private void reloadData(ActionEvent e) {
        sharedBandController.requestData();
        modelController.requestData();
        siteBandController.requestData();
    }

    @FXML
    private void exportData(ActionEvent e) {
        //Save all parameters to an archive file and prompt the user about where to save it.
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Archive File", "*.zip"));
        fileChooser.setInitialFileName("Calibration_Parameters.zip");
        File selectedFile = fileChooser.showSaveDialog(parameters.getScene().getWindow());

        if (selectedFile != null) {

            File exportArchive;
            try {
                if (!selectedFile.exists()) {
                    selectedFile.createNewFile();
                } else if (!selectedFile.canWrite()) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(AlertType.ERROR);
                        alert.setTitle("File Permissions Error");
                        alert.setHeaderText("Unable to write to file or directory.");
                        alert.setContentText("Unable to write file, do you have write permissions on the selected directory?");
                        alert.show();
                    });
                }
                exportArchive = paramExporter.createExportArchive();
                Files.move(exportArchive.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e1) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(AlertType.ERROR);
                    alert.setTitle("File IO Error");
                    alert.setHeaderText("Unable to export results");
                    alert.setContentText(e1.getMessage());
                    alert.show();
                });
            }
        }
    }
}