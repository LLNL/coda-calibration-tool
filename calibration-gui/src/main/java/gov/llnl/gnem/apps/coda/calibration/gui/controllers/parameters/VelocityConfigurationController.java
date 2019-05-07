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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers.parameters;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

@Component
public class VelocityConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(VelocityConfigurationController.class);

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    @FXML
    private TableView<VelocityConfiguration> velocityConfTableView;

    @FXML
    TableColumn<VelocityConfiguration, String> distanceThresholdCol;

    @FXML
    TableColumn<VelocityConfiguration, String> gt1Col;

    @FXML
    TableColumn<VelocityConfiguration, String> gt2Col;

    @FXML
    TableColumn<VelocityConfiguration, String> lt1Col;

    @FXML
    TableColumn<VelocityConfiguration, String> lt2Col;

    @FXML
    TableColumn<VelocityConfiguration, String> phaseSpeedCol;

    private ObservableList<VelocityConfiguration> velData = FXCollections.observableArrayList();

    private ParameterClient client;

    @Autowired
    public VelocityConfigurationController(ParameterClient client) {
        this.client = client;
    }

    @FXML
    private void reloadData(ActionEvent e) {
        requestData();
    }

    @FXML
    public void initialize() {
        CellBindingUtils.attachTextCellFactories(distanceThresholdCol, VelocityConfiguration::getDistanceThresholdInKm, dfmt2);
        CellBindingUtils.attachTextCellFactories(gt1Col, VelocityConfiguration::getGroupVelocity1InKmsGtDistance, dfmt2);
        CellBindingUtils.attachTextCellFactories(gt2Col, VelocityConfiguration::getGroupVelocity2InKmsGtDistance, dfmt2);
        CellBindingUtils.attachTextCellFactories(lt1Col, VelocityConfiguration::getGroupVelocity1InKmsLtDistance, dfmt2);
        CellBindingUtils.attachTextCellFactories(lt2Col, VelocityConfiguration::getGroupVelocity2InKmsLtDistance, dfmt2);
        CellBindingUtils.attachTextCellFactories(phaseSpeedCol, VelocityConfiguration::getPhaseSpeedInKms, dfmt2);
        velocityConfTableView.setItems(velData);
        requestData();
    }


    protected void requestData() {
        velData.clear();
        client.getVelocityConfiguration()
              .filter(Objects::nonNull)
              .filter(value -> null != value.getId())
              .doOnTerminate(() -> velocityConfTableView.sort())
              .subscribe(value -> velData.add(value), err -> log.trace(err.getMessage(), err));

        Platform.runLater(() -> {
            Optional.ofNullable(velocityConfTableView).ifPresent(v -> v.refresh());
        });
    }
}
