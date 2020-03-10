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
import gov.llnl.gnem.apps.coda.calibration.gui.util.TimeLatchedGetSet;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;

@Component
public class ShapeConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(ShapeConfigurationController.class);

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    @FXML
    private TableView<ShapeFitterConstraints> shapeVelConfTableView;

    @FXML
    private TableView<ShapeFitterConstraints> shapeBetaConfTableView;

    @FXML
    private TableView<ShapeFitterConstraints> shapeGammaConfTableView;

    @FXML
    private TableView<ShapeFitterConstraints> shapeMiscConfTableView;

    @FXML
    TableColumn<ShapeFitterConstraints, String> maxVP1;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minVP1;
    @FXML
    TableColumn<ShapeFitterConstraints, String> v0reg;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxVP2;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minVP2;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxVP3;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minVP3;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxBP1;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minBP1;
    @FXML
    TableColumn<ShapeFitterConstraints, String> b0reg;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxBP2;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minBP2;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxBP3;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minBP3;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxGP1;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minGP1;
    @FXML
    TableColumn<ShapeFitterConstraints, String> g0reg;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxGP2;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minGP2;
    @FXML
    TableColumn<ShapeFitterConstraints, String> g1reg;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxGP3;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minGP3;
    @FXML
    TableColumn<ShapeFitterConstraints, String> yvvMin;
    @FXML
    TableColumn<ShapeFitterConstraints, String> yvvMax;
    @FXML
    TableColumn<ShapeFitterConstraints, String> vDistMax;
    @FXML
    TableColumn<ShapeFitterConstraints, String> vDistMin;
    @FXML
    TableColumn<ShapeFitterConstraints, String> ybbMin;
    @FXML
    TableColumn<ShapeFitterConstraints, String> ybbMax;
    @FXML
    TableColumn<ShapeFitterConstraints, String> bDistMax;
    @FXML
    TableColumn<ShapeFitterConstraints, String> bDistMin;
    @FXML
    TableColumn<ShapeFitterConstraints, String> yggMin;
    @FXML
    TableColumn<ShapeFitterConstraints, String> yggMax;
    @FXML
    TableColumn<ShapeFitterConstraints, String> gDistMin;
    @FXML
    TableColumn<ShapeFitterConstraints, String> gDistMax;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minIntercept;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxIntercept;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minBeta;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxBeta;
    @FXML
    TableColumn<ShapeFitterConstraints, String> minGamma;
    @FXML
    TableColumn<ShapeFitterConstraints, String> maxGamma;
    @FXML
    TableColumn<ShapeFitterConstraints, String> iterations;
    @FXML
    TableColumn<ShapeFitterConstraints, String> fittingPointCount;

    private ObservableList<ShapeFitterConstraints> data = FXCollections.observableArrayList();

    private ParameterClient client;

    private TimeLatchedGetSet scheduler;

    @Autowired
    public ShapeConfigurationController(ParameterClient client) {
        this.client = client;
        this.scheduler = new TimeLatchedGetSet(() -> requestData(), () -> data.forEach(d -> {
            client.updateShapeFitterConstraints(d).subscribe();
        }));
    }

    @FXML
    private void postUpdate(CellEditEvent<?, ?> event) {
        scheduler.set();
    }

    @FXML
    private void reloadData(ActionEvent e) {
        requestData();
    }

    @FXML
    public void initialize() {
        CellBindingUtils.attachEditableTextCellFactories(maxVP1, ShapeFitterConstraints::getMaxVP1, ShapeFitterConstraints::setMaxVP1, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minVP1, ShapeFitterConstraints::getMinVP1, ShapeFitterConstraints::setMinVP1, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(v0reg, ShapeFitterConstraints::getV0reg, ShapeFitterConstraints::setV0reg, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxVP2, ShapeFitterConstraints::getMaxVP2, ShapeFitterConstraints::setMaxVP2, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minVP2, ShapeFitterConstraints::getMinVP2, ShapeFitterConstraints::setMinVP2, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxVP3, ShapeFitterConstraints::getMaxVP3, ShapeFitterConstraints::setMaxVP3, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minVP3, ShapeFitterConstraints::getMinVP3, ShapeFitterConstraints::setMinVP3, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxBP1, ShapeFitterConstraints::getMaxBP1, ShapeFitterConstraints::setMaxBP1, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minBP1, ShapeFitterConstraints::getMinBP1, ShapeFitterConstraints::setMinBP1, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(b0reg, ShapeFitterConstraints::getB0reg, ShapeFitterConstraints::setB0reg, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxBP2, ShapeFitterConstraints::getMaxBP2, ShapeFitterConstraints::setMaxBP2, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minBP2, ShapeFitterConstraints::getMinBP2, ShapeFitterConstraints::setMinBP2, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxBP3, ShapeFitterConstraints::getMaxBP3, ShapeFitterConstraints::setMaxBP3, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minBP3, ShapeFitterConstraints::getMinBP3, ShapeFitterConstraints::setMinBP3, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxGP1, ShapeFitterConstraints::getMaxGP1, ShapeFitterConstraints::setMaxGP1, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minGP1, ShapeFitterConstraints::getMinGP1, ShapeFitterConstraints::setMinGP1, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(g0reg, ShapeFitterConstraints::getG0reg, ShapeFitterConstraints::setG0reg, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxGP2, ShapeFitterConstraints::getMaxGP2, ShapeFitterConstraints::setMaxGP2, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minGP2, ShapeFitterConstraints::getMinGP2, ShapeFitterConstraints::setMinGP2, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(g1reg, ShapeFitterConstraints::getG1reg, ShapeFitterConstraints::setG1reg, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxGP3, ShapeFitterConstraints::getMaxGP3, ShapeFitterConstraints::setMaxGP3, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minGP3, ShapeFitterConstraints::getMinGP3, ShapeFitterConstraints::setMinGP3, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(yvvMin, ShapeFitterConstraints::getYvvMin, ShapeFitterConstraints::setYvvMin, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(yvvMax, ShapeFitterConstraints::getYvvMax, ShapeFitterConstraints::setYvvMax, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(vDistMax, ShapeFitterConstraints::getvDistMax, ShapeFitterConstraints::setvDistMax, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(vDistMin, ShapeFitterConstraints::getvDistMin, ShapeFitterConstraints::setvDistMin, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(ybbMin, ShapeFitterConstraints::getYbbMin, ShapeFitterConstraints::setYbbMin, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(ybbMax, ShapeFitterConstraints::getYbbMax, ShapeFitterConstraints::setYbbMax, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(bDistMax, ShapeFitterConstraints::getbDistMax, ShapeFitterConstraints::setbDistMax, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(bDistMin, ShapeFitterConstraints::getbDistMin, ShapeFitterConstraints::setbDistMin, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(yggMin, ShapeFitterConstraints::getYggMin, ShapeFitterConstraints::setYggMin, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(yggMax, ShapeFitterConstraints::getYggMax, ShapeFitterConstraints::setYggMax, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(gDistMin, ShapeFitterConstraints::getgDistMin, ShapeFitterConstraints::setgDistMin, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(gDistMax, ShapeFitterConstraints::getgDistMax, ShapeFitterConstraints::setgDistMax, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minIntercept, ShapeFitterConstraints::getMinIntercept, ShapeFitterConstraints::setMinIntercept, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxIntercept, ShapeFitterConstraints::getMaxIntercept, ShapeFitterConstraints::setMaxIntercept, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minBeta, ShapeFitterConstraints::getMinBeta, ShapeFitterConstraints::setMinBeta, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxBeta, ShapeFitterConstraints::getMaxBeta, ShapeFitterConstraints::setMaxBeta, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(minGamma, ShapeFitterConstraints::getMinGamma, ShapeFitterConstraints::setMinGamma, dfmt2);
        CellBindingUtils.attachEditableTextCellFactories(maxGamma, ShapeFitterConstraints::getMaxGamma, ShapeFitterConstraints::setMaxGamma, dfmt2);
        CellBindingUtils.attachEditableIntegerCellFactories(iterations, ShapeFitterConstraints::getIterations, ShapeFitterConstraints::setIterations);
        CellBindingUtils.attachEditableIntegerCellFactories(fittingPointCount, ShapeFitterConstraints::getFittingPointCount, ShapeFitterConstraints::setFittingPointCount);

        shapeVelConfTableView.setItems(data);
        shapeBetaConfTableView.setItems(data);
        shapeGammaConfTableView.setItems(data);
        shapeMiscConfTableView.setItems(data);
        requestData();
    }

    protected void requestData() {
        data.clear();
        client.getShapeFitterConstraints().filter(Objects::nonNull).filter(value -> null != value.getId()).doOnTerminate(() -> {
            Optional.ofNullable(shapeVelConfTableView).ifPresent(TableView::sort);
            Optional.ofNullable(shapeBetaConfTableView).ifPresent(TableView::sort);
            Optional.ofNullable(shapeGammaConfTableView).ifPresent(TableView::sort);
            Optional.ofNullable(shapeMiscConfTableView).ifPresent(TableView::sort);
        }).subscribe(value -> data.add(value), err -> log.trace(err.getMessage(), err));

        Platform.runLater(() -> {
            Optional.ofNullable(shapeVelConfTableView).ifPresent(v -> v.refresh());
            Optional.ofNullable(shapeBetaConfTableView).ifPresent(v -> v.refresh());
            Optional.ofNullable(shapeGammaConfTableView).ifPresent(v -> v.refresh());
            Optional.ofNullable(shapeMiscConfTableView).ifPresent(v -> v.refresh());
        });
    }
}
