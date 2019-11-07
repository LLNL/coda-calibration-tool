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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers.parameters;

import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.util.TimeLatchedGetSet;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.MdacDataChangeEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;

@Component
public class ModelController {

    private static final Logger log = LoggerFactory.getLogger(ModelController.class);

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

    private EventBus bus;

    private TimeLatchedGetSet scheduler;

    @Autowired
    public ModelController(ParameterClient client, EventBus bus) {
        this.client = client;
        this.bus = bus;
        this.bus.register(this);

        this.scheduler = new TimeLatchedGetSet(() -> requestData(), () -> {
            fiData.forEach(p -> {
                try {
                    client.setFiParameter(p).subscribe();
                } catch (JsonProcessingException e1) {
                    log.error(e1.getMessage(), e1);
                }
            });
            psData.forEach(p -> {
                try {
                    client.setPsParameter(p).subscribe();
                } catch (JsonProcessingException e1) {
                    log.error(e1.getMessage(), e1);
                }
            });
        });
    }

    @PostConstruct
    private void onSpringStartupFinished() {
        requestData();
    }

    @FXML
    private void postUpdate(CellEditEvent<?, ?> event) {
        scheduler.set();
    }

    @Subscribe
    private void updateNotification(MdacDataChangeEvent event) {
        scheduler.get();
    }

    protected void requestData() {
        fiData.clear();
        psData.clear();

        client.getFiParameters()
              .filter(Objects::nonNull)
              .filter(value -> null != value.getId())
              .doOnComplete(() -> Optional.ofNullable(fiTableView).ifPresent(TableView::sort))
              .subscribe(value -> fiData.add(value), err -> log.trace(err.getMessage(), err));

        client.getPsParameters()
              .filter(Objects::nonNull)
              .filter(value -> null != value.getId())
              .doOnComplete(() -> Optional.ofNullable(psTableView).ifPresent(TableView::sort))
              .subscribe(value -> psData.add(value), err -> log.trace(err.getMessage(), err));

        Platform.runLater(() -> {
            Optional.ofNullable(fiTableView).ifPresent(v -> v.refresh());
            Optional.ofNullable(psTableView).ifPresent(v -> v.refresh());
        });
    }

    @FXML
    public void initialize() {

        CellBindingUtils.attachTextCellFactoriesString(phaseCol, MdacParametersPS::getPhase);
        CellBindingUtils.attachEditableTextCellFactories(q0Col, MdacParametersPS::getQ0, MdacParametersPS::setQ0);
        CellBindingUtils.attachEditableTextCellFactories(delQ0Col, MdacParametersPS::getDelQ0, MdacParametersPS::setDelQ0);
        CellBindingUtils.attachEditableTextCellFactories(gammaCol, MdacParametersPS::getGamma0, MdacParametersPS::setGamma0);
        CellBindingUtils.attachEditableTextCellFactories(delGammaCol, MdacParametersPS::getDelGamma0, MdacParametersPS::setDelGamma0);
        CellBindingUtils.attachEditableTextCellFactories(u0Col, MdacParametersPS::getU0, MdacParametersPS::setU0);
        CellBindingUtils.attachEditableTextCellFactories(etaCol, MdacParametersPS::getEta, MdacParametersPS::setEta);
        CellBindingUtils.attachEditableTextCellFactories(delEtaCol, MdacParametersPS::getDelEta, MdacParametersPS::setDelEta);
        CellBindingUtils.attachEditableTextCellFactories(distCritCol, MdacParametersPS::getDistCrit, MdacParametersPS::setDistCrit);
        CellBindingUtils.attachEditableTextCellFactories(snrCol, MdacParametersPS::getSnr, MdacParametersPS::setSnr);
        CellBindingUtils.attachEditableTextCellFactories(sigmaCol, MdacParametersFI::getSigma, MdacParametersFI::setSigma);
        CellBindingUtils.attachEditableTextCellFactories(delSigmaCol, MdacParametersFI::getDelSigma, MdacParametersFI::setDelSigma);
        CellBindingUtils.attachEditableTextCellFactories(psiCol, MdacParametersFI::getPsi, MdacParametersFI::setPsi);
        CellBindingUtils.attachEditableTextCellFactories(delPsiCol, MdacParametersFI::getDelPsi, MdacParametersFI::setDelPsi);
        CellBindingUtils.attachEditableTextCellFactories(zetaCol, MdacParametersFI::getZeta, MdacParametersFI::setZeta);
        CellBindingUtils.attachEditableTextCellFactories(m0RefCol, MdacParametersFI::getM0ref, MdacParametersFI::setM0ref);
        CellBindingUtils.attachEditableTextCellFactories(alphasCol, MdacParametersFI::getAlphas, MdacParametersFI::setAlphas);
        CellBindingUtils.attachEditableTextCellFactories(betasCol, MdacParametersFI::getBetas, MdacParametersFI::setBetas);
        CellBindingUtils.attachEditableTextCellFactories(rhosCol, MdacParametersFI::getRhos, MdacParametersFI::setRhos);
        CellBindingUtils.attachEditableTextCellFactories(radpatPCol, MdacParametersFI::getRadPatP, MdacParametersFI::setRadPatP);
        CellBindingUtils.attachEditableTextCellFactories(radpatSCol, MdacParametersFI::getRadPatS, MdacParametersFI::setRadPatS);
        CellBindingUtils.attachEditableTextCellFactories(alpharCol, MdacParametersFI::getAlphaR, MdacParametersFI::setAlphaR);
        CellBindingUtils.attachEditableTextCellFactories(betarCol, MdacParametersFI::getBetaR, MdacParametersFI::setBetaR);
        CellBindingUtils.attachEditableTextCellFactories(rhorCol, MdacParametersFI::getRhor, MdacParametersFI::setRhor);

        fiTableView.setItems(fiData);
        psTableView.setItems(psData);
    }
}
