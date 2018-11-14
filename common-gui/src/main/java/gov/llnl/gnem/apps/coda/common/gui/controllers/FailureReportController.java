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
package gov.llnl.gnem.apps.coda.common.gui.controllers;

import java.io.IOException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.common.gui.events.EnvelopeLoadStartingEvent;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.model.messaging.PassFailEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Component
public class FailureReportController {

    private static final Logger log = LoggerFactory.getLogger(FailureReportController.class);

    private Parent root;
    private Scene scene;
    private Stage stage;

    @FXML
    TableView<String> tableView;

    @FXML
    TableColumn<String, String> errorCol;

    private ObservableList<String> errors = FXCollections.observableArrayList();

    private EventBus bus;

    @Autowired
    public FailureReportController(EventBus bus) {
        this.bus = bus;
        bus.register(this);
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/FailureReport.fxml"));
            fxmlLoader.setController(this);
            stage = new Stage(StageStyle.DECORATED);
            Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
            try {
                root = fxmlLoader.load();
                scene = new Scene(root);
                stage.setScene(scene);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Subscribe
    private void listener(EnvelopeLoadStartingEvent event) {
        errors.clear();
    }

    @Subscribe
    private void listener(ShowFailureReportEvent event) {
        Platform.runLater(() -> {
            stage.show();
            stage.toFront();
        });
    }

    @Subscribe
    private void listener(PassFailEvent event) {
        if (event.getResult() != null && !event.getResult().isSuccess()) {
            errors.addAll(event.getResult().getErrors().stream().map(e -> e.getMessage()).collect(Collectors.toList()));
        }
    }

    @FXML
    public void initialize() {
        CellBindingUtils.attachTextCellFactoriesString(errorCol, Function.identity());
        tableView.setItems(errors);
    }
}
