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
package gov.llnl.gnem.apps.coda.common.gui.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import gov.llnl.gnem.apps.coda.common.model.messaging.Progress;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class ProgressMonitor extends VBox implements Observer {
    private Button cancelButton = new Button();

    @FXML
    private ProgressBar progressBar;

    @FXML
    private Label label;

    private String displayableName;
    private String progressStage;
    private final ObservableList<Runnable> cancelFunctions = FXCollections.observableArrayList();

    public ProgressMonitor(String displayableName, ProgressListener progressListener) {
        this.displayableName = displayableName;
        this.progressStage = "";
        progressListener.addObserver(this);

        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/ProgressMonitor.fxml"));
            fxmlLoader.setController(this);
            Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
            try {
                Parent root = fxmlLoader.load();
                this.getChildren().add(root);

                if (progressListener.getProgress() >= 0.0) {
                    progressBar.setProgress(progressListener.getProgress());
                } else {
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                }
                label.setText("");

                cancelButton.setAlignment(Pos.CENTER);
                cancelButton.setPadding(new Insets(0, 0, 0, 0));
                cancelButton.setPrefSize(25.0, 25.0);
                cancelButton.setText("X");
                cancelButton.setTextAlignment(TextAlignment.CENTER);
                cancelButton.setOnAction((ActionEvent event) -> {
                    List<Runnable> funcs = new ArrayList<>(cancelFunctions.size());
                    funcs.addAll(cancelFunctions);
                    funcs.forEach(Runnable::run);
                });
                cancelButton.visibleProperty().bind(Bindings.size(cancelFunctions).greaterThan(0));

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public void update(Observable o, Object event) {
        if (event instanceof Progress) {
            Progress progress = (Progress) event;
            Platform.runLater(() -> {
                if (progress.getTotal() >= 0.0) {
                    label.setText(progress.getCurrent() + "/" + progress.getTotal() + getProgressStage());
                    progressBar.setProgress(progress.getProgress());
                } else {
                    progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
                    label.setText("");
                }
            });
        }
    }

    private String getProgressStage() {
        if (progressStage.isEmpty()) {
            return progressStage;
        } else {
            return " | " + progressStage;
        }
    }

    public String getDisplayableName() {
        return displayableName;
    }

    public ProgressMonitor setProgressStage(String progressStage) {
        this.progressStage = progressStage;
        return this;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }

    public void addCancelCallback(Runnable cancelFunction) {
        cancelFunctions.add(cancelFunction);
    }

    public void clearCancelCallbacks() {
        cancelFunctions.clear();
    }

    public Button getCancelButton() {
        return cancelButton;
    }
}
