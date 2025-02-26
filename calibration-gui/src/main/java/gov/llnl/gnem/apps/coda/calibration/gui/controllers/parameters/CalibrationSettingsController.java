/*
* Copyright (c) 2024, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings;
import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings.DistanceCalcMethod;
import gov.llnl.gnem.apps.coda.common.gui.data.client.DistanceCalculator;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;

@Component
public class CalibrationSettingsController {

    private static final Logger log = LoggerFactory.getLogger(CalibrationSettingsController.class);

    @FXML
    private ChoiceBox<String> distanceCalcMethodBox;

    private ParameterClient client;

    private DistanceCalculator distanceCalc;

    @Autowired
    public CalibrationSettingsController(ParameterClient client, DistanceCalculator distanceCalc) {
        this.client = client;
        this.distanceCalc = distanceCalc;
    }

    @FXML
    private void reloadData(ActionEvent e) {
        requestData();
    }

    @FXML
    public void initialize() {

        distanceCalcMethodBox.getSelectionModel().selectedItemProperty().addListener((obs, old, event) -> {
            if (event != null) {
                CalibrationSettings settings = new CalibrationSettings();
                settings.setDistanceCalcMethod(event);
                client.updateCalibrationSettings(settings);
            }
        });

        // Initialize the calc method selection drop-down
        if (distanceCalcMethodBox.getItems().size() < 1) {
            for (DistanceCalcMethod method : DistanceCalcMethod.values()) {
                distanceCalcMethodBox.getItems().add(method.getValue());
            }
        }

        setCalibrationMethodDropDown();
    }

    protected void requestData() {
        distanceCalc.updateCalcMethod();
        setCalibrationMethodDropDown();
    }

    private void setCalibrationMethodDropDown() {
        CalibrationSettings settings = client.getCalibrationSettings().filter(Objects::nonNull).block(Duration.of(10l, ChronoUnit.SECONDS));

        // Load preferred distance calc method and set the drop-down to match
        if (settings != null) {
            String calcMethod = settings.getDistanceCalcMethod();
            ObservableList<String> items = distanceCalcMethodBox.getItems();
            Integer idx = items.indexOf(calcMethod);

            Platform.runLater(() -> {
                distanceCalcMethodBox.getSelectionModel().select(idx);
            });
        }
    }
}
