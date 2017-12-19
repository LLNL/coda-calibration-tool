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

import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;

@Component
public class SharedBandController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    @FXML
    private TableView<SharedFrequencyBandParameters> codaSharedTableView;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> lowFreqCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> highFreqCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> v0Col;
    @FXML
    TableColumn<SharedFrequencyBandParameters, String> v1Col;
    @FXML
    TableColumn<SharedFrequencyBandParameters, String> v2Col;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> b0Col;
    @FXML
    TableColumn<SharedFrequencyBandParameters, String> b1Col;
    @FXML
    TableColumn<SharedFrequencyBandParameters, String> b2Col;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> g0Col;
    @FXML
    TableColumn<SharedFrequencyBandParameters, String> g1Col;
    @FXML
    TableColumn<SharedFrequencyBandParameters, String> g2Col;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> minSnrCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> s1Col;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> s2Col;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> xcCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> xtCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> qCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> minLengthCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> maxLengthCol;

    @FXML
    TableColumn<SharedFrequencyBandParameters, String> measureTimeCol;

    private ObservableList<SharedFrequencyBandParameters> sharedFbData = FXCollections.observableArrayList();

    private ParameterClient client;

    @Autowired
    public SharedBandController(ParameterClient client) {
        this.client = client;
    }

    @FXML
    private void reloadTable(ActionEvent e) {
        requestData();
    }

    @FXML
    public void initialize() {

        lowFreqCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                       .map(CellDataFeatures::getValue)
                                                                                       .map(SharedFrequencyBandParameters::getLowFrequency)
                                                                                       .filter(Objects::nonNull)
                                                                                       .map(dfmt2::format)
                                                                                       .orElseGet(String::new)));
        lowFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        highFreqCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                        .map(CellDataFeatures::getValue)
                                                                                        .map(SharedFrequencyBandParameters::getHighFrequency)
                                                                                        .filter(Objects::nonNull)
                                                                                        .map(dfmt2::format)
                                                                                        .orElseGet(String::new)));
        highFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        v0Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getVelocity0)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        v1Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getVelocity1)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        v2Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getVelocity2)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        b0Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getBeta0)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        b1Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getBeta1)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        b2Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getBeta2)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        g0Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getGamma0)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        g1Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getGamma1)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        g2Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getGamma2)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt4::format)
                                                                                  .orElseGet(String::new)));

        minSnrCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                      .map(CellDataFeatures::getValue)
                                                                                      .map(SharedFrequencyBandParameters::getMinSnr)
                                                                                      .filter(Objects::nonNull)
                                                                                      .map(dfmt2::format)
                                                                                      .orElseGet(String::new)));

        s1Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getS1)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt2::format)
                                                                                  .orElseGet(String::new)));

        s2Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getS2)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt2::format)
                                                                                  .orElseGet(String::new)));

        xcCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getXc)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt2::format)
                                                                                  .orElseGet(String::new)));

        xtCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(SharedFrequencyBandParameters::getXt)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt2::format)
                                                                                  .orElseGet(String::new)));

        qCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                 .map(CellDataFeatures::getValue)
                                                                                 .map(SharedFrequencyBandParameters::getQ)
                                                                                 .filter(Objects::nonNull)
                                                                                 .map(dfmt2::format)
                                                                                 .orElseGet(String::new)));

        minLengthCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                         .map(CellDataFeatures::getValue)
                                                                                         .map(SharedFrequencyBandParameters::getMinLength)
                                                                                         .filter(Objects::nonNull)
                                                                                         .map(dfmt2::format)
                                                                                         .orElseGet(String::new)));

        maxLengthCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                         .map(CellDataFeatures::getValue)
                                                                                         .map(SharedFrequencyBandParameters::getMaxLength)
                                                                                         .filter(Objects::nonNull)
                                                                                         .map(dfmt2::format)
                                                                                         .orElseGet(String::new)));

        measureTimeCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                           .map(CellDataFeatures::getValue)
                                                                                           .map(SharedFrequencyBandParameters::getMeasurementTime)
                                                                                           .filter(Objects::nonNull)
                                                                                           .map(dfmt2::format)
                                                                                           .orElseGet(String::new)));

        codaSharedTableView.setItems(sharedFbData);
    }

    @PostConstruct
    private void onSpringStartupFinished() {
        requestData();
    }

    protected void requestData() {
        sharedFbData.clear();
        client.getSharedFrequencyBandParameters().filter(Objects::nonNull).filter(value -> null != value.getId()).subscribe(value -> sharedFbData.add(value), err -> log.trace(err.getMessage(), err));
    }
}
