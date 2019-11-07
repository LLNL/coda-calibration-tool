/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.gui.util;

import java.io.File;
import java.io.IOException;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Window;

public class FileDialogs {
    public static File openFileSaveDialog(String filename, String extension, Window window) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open Resource File");
        fileChooser.getExtensionFilters().addAll(new ExtensionFilter("Output Format", "*" + extension));
        fileChooser.setInitialFileName(filename + extension);
        File selectedFile = fileChooser.showSaveDialog(window);
        return selectedFile;
    }

    public static boolean ensureFileIsWritable(File selectedFile) {
        boolean existsAndWritable = true;
        try {
            if (selectedFile == null) {
                existsAndWritable = false;
            } else {
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
                    existsAndWritable = false;
                }
            }
        } catch (IOException e1) {
            fileIoErrorAlert(e1);
            existsAndWritable = false;
        }

        return existsAndWritable;
    }

    public static void fileIoErrorAlert(IOException e) {
        Platform.runLater(() -> {
            Alert alert = new Alert(AlertType.ERROR);
            alert.setTitle("File IO Error");
            alert.setHeaderText("Unable to export results");
            alert.setContentText(e.getMessage());
            alert.show();
        });
    }
}
