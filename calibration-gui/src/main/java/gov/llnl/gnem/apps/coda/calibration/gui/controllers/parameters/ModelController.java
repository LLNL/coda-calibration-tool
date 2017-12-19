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
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;

@Component
public class ModelController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    @FXML
    private TableView<MdacParametersFI> fiTableView;

    @FXML
    private TableView<MdacParametersPS> psTableView;

    @FXML
    TableColumn<MdacParametersPS, String> phaseCol;

    @FXML
    TableColumn<MdacParametersPS, String> q0Col;

    @FXML
    TableColumn<MdacParametersPS, String> delQ0Col;

    @FXML
    TableColumn<MdacParametersPS, String> gammaCol;

    @FXML
    TableColumn<MdacParametersPS, String> delGammaCol;

    @FXML
    TableColumn<MdacParametersPS, String> u0Col;

    @FXML
    TableColumn<MdacParametersPS, String> etaCol;

    @FXML
    TableColumn<MdacParametersPS, String> delEtaCol;

    @FXML
    TableColumn<MdacParametersPS, String> distCritCol;

    @FXML
    TableColumn<MdacParametersPS, String> snrCol;

    @FXML
    TableColumn<MdacParametersFI, String> sigmaCol;

    @FXML
    TableColumn<MdacParametersFI, String> delSigmaCol;

    @FXML
    TableColumn<MdacParametersFI, String> psiCol;

    @FXML
    TableColumn<MdacParametersFI, String> delPsiCol;

    @FXML
    TableColumn<MdacParametersFI, String> zetaCol;

    @FXML
    TableColumn<MdacParametersFI, String> m0RefCol;

    @FXML
    TableColumn<MdacParametersFI, String> alphasCol;

    @FXML
    TableColumn<MdacParametersFI, String> betasCol;

    @FXML
    TableColumn<MdacParametersFI, String> rhosCol;

    @FXML
    TableColumn<MdacParametersFI, String> radpatPCol;

    @FXML
    TableColumn<MdacParametersFI, String> radpatSCol;

    @FXML
    TableColumn<MdacParametersFI, String> alpharCol;

    @FXML
    TableColumn<MdacParametersFI, String> betarCol;

    @FXML
    TableColumn<MdacParametersFI, String> rhorCol;

    private ObservableList<MdacParametersFI> fiData = FXCollections.observableArrayList();
    private ObservableList<MdacParametersPS> psData = FXCollections.observableArrayList();

    private ParameterClient client;

    @Autowired
    public ModelController(ParameterClient client) {
        this.client = client;
    }

    @FXML
    private void reloadTable(ActionEvent e) {
        requestData();
    }

    @PostConstruct
    private void onSpringStartupFinished() {
        requestData();
    }

    protected void requestData() {
        fiData.clear();
        psData.clear();

        client.getFiParameters().filter(Objects::nonNull).filter(value -> null != value.getId()).subscribe(value -> fiData.add(value), err -> log.trace(err.getMessage(), err));
        client.getPsParameters().filter(Objects::nonNull).filter(value -> null != value.getId()).subscribe(value -> psData.add(value), err -> log.trace(err.getMessage(), err));
    }

    @FXML
    public void initialize() {

        phaseCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersPS::getPhase)
                                                                                     .filter(Objects::nonNull)
                                                                                     .orElseGet(String::new)));

        q0Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(MdacParametersPS::getQ0)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt2::format)
                                                                                  .orElseGet(String::new)));

        delQ0Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersPS::getDelQ0)
                                                                                     .filter(Objects::nonNull)
                                                                                     .map(dfmt2::format)
                                                                                     .orElseGet(String::new)));

        gammaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersPS::getGamma0)
                                                                                     .filter(Objects::nonNull)
                                                                                     .map(dfmt2::format)
                                                                                     .orElseGet(String::new)));

        delGammaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                        .map(CellDataFeatures::getValue)
                                                                                        .map(MdacParametersPS::getDelGamma0)
                                                                                        .filter(Objects::nonNull)
                                                                                        .map(dfmt2::format)
                                                                                        .orElseGet(String::new)));

        u0Col.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                  .map(CellDataFeatures::getValue)
                                                                                  .map(MdacParametersPS::getU0)
                                                                                  .filter(Objects::nonNull)
                                                                                  .map(dfmt2::format)
                                                                                  .orElseGet(String::new)));

        etaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                   .map(CellDataFeatures::getValue)
                                                                                   .map(MdacParametersPS::getEta)
                                                                                   .filter(Objects::nonNull)
                                                                                   .map(dfmt2::format)
                                                                                   .orElseGet(String::new)));

        delEtaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                      .map(CellDataFeatures::getValue)
                                                                                      .map(MdacParametersPS::getDelEta)
                                                                                      .filter(Objects::nonNull)
                                                                                      .map(dfmt2::format)
                                                                                      .orElseGet(String::new)));

        distCritCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                        .map(CellDataFeatures::getValue)
                                                                                        .map(MdacParametersPS::getDistCrit)
                                                                                        .filter(Objects::nonNull)
                                                                                        .map(dfmt2::format)
                                                                                        .orElseGet(String::new)));

        snrCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                   .map(CellDataFeatures::getValue)
                                                                                   .map(MdacParametersPS::getSnr)
                                                                                   .filter(Objects::nonNull)
                                                                                   .map(dfmt2::format)
                                                                                   .orElseGet(String::new)));

        sigmaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersFI::getSigma)
                                                                                     .filter(Objects::nonNull)
                                                                                     .map(dfmt2::format)
                                                                                     .orElseGet(String::new)));

        delSigmaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                        .map(CellDataFeatures::getValue)
                                                                                        .map(MdacParametersFI::getDelSigma)
                                                                                        .filter(Objects::nonNull)
                                                                                        .map(dfmt2::format)
                                                                                        .orElseGet(String::new)));

        psiCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                   .map(CellDataFeatures::getValue)
                                                                                   .map(MdacParametersFI::getPsi)
                                                                                   .filter(Objects::nonNull)
                                                                                   .map(dfmt2::format)
                                                                                   .orElseGet(String::new)));

        delPsiCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                      .map(CellDataFeatures::getValue)
                                                                                      .map(MdacParametersFI::getDelPsi)
                                                                                      .filter(Objects::nonNull)
                                                                                      .map(dfmt2::format)
                                                                                      .orElseGet(String::new)));

        zetaCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                    .map(CellDataFeatures::getValue)
                                                                                    .map(MdacParametersFI::getZeta)
                                                                                    .filter(Objects::nonNull)
                                                                                    .map(dfmt2::format)
                                                                                    .orElseGet(String::new)));

        m0RefCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersFI::getM0ref)
                                                                                     .filter(Objects::nonNull)
                                                                                     .map(d -> d.toString())
                                                                                     .orElseGet(String::new)));

        alphasCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                      .map(CellDataFeatures::getValue)
                                                                                      .map(MdacParametersFI::getAlphas)
                                                                                      .filter(Objects::nonNull)
                                                                                      .map(dfmt2::format)
                                                                                      .orElseGet(String::new)));

        betasCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersFI::getBetas)
                                                                                     .filter(Objects::nonNull)
                                                                                     .map(dfmt2::format)
                                                                                     .orElseGet(String::new)));

        rhosCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                    .map(CellDataFeatures::getValue)
                                                                                    .map(MdacParametersFI::getRhos)
                                                                                    .filter(Objects::nonNull)
                                                                                    .map(dfmt2::format)
                                                                                    .orElseGet(String::new)));

        radpatPCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                       .map(CellDataFeatures::getValue)
                                                                                       .map(MdacParametersFI::getRadPatP)
                                                                                       .filter(Objects::nonNull)
                                                                                       .map(dfmt2::format)
                                                                                       .orElseGet(String::new)));

        radpatSCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                       .map(CellDataFeatures::getValue)
                                                                                       .map(MdacParametersFI::getRadPatS)
                                                                                       .filter(Objects::nonNull)
                                                                                       .map(dfmt2::format)
                                                                                       .orElseGet(String::new)));

        alpharCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                      .map(CellDataFeatures::getValue)
                                                                                      .map(MdacParametersFI::getAlphaR)
                                                                                      .filter(Objects::nonNull)
                                                                                      .map(dfmt2::format)
                                                                                      .orElseGet(String::new)));

        betarCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(MdacParametersFI::getBetaR)
                                                                                     .filter(Objects::nonNull)
                                                                                     .map(dfmt2::format)
                                                                                     .orElseGet(String::new)));

        rhorCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                    .map(CellDataFeatures::getValue)
                                                                                    .map(MdacParametersFI::getRhor)
                                                                                    .filter(Objects::nonNull)
                                                                                    .map(dfmt2::format)
                                                                                    .orElseGet(String::new)));

        fiTableView.setItems(fiData);
        psTableView.setItems(psData);
    }
}
