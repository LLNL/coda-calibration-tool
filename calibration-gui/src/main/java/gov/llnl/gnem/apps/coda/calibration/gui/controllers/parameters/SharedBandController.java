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

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.calibration.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;

@Component
public class SharedBandController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

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
    private void reloadTable(Event e) {
        requestData();
    }

    @FXML
    private void postUpdate(CellEditEvent<?, ?> e) {
        sharedFbData.forEach(fb -> {
            try {
                client.postSharedFrequencyBandParameters(fb);
            } catch (JsonProcessingException e1) {
                log.error(e1.getMessage(), e1);
            }
        });
        sharedFbData.clear();
        requestData();
    }

    @FXML
    public void initialize() {

        CellBindingUtils.attachEditableTextCellFactories(lowFreqCol, SharedFrequencyBandParameters::getLowFrequency, SharedFrequencyBandParameters::setLowFrequency);
        lowFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachEditableTextCellFactories(highFreqCol, SharedFrequencyBandParameters::getHighFrequency, SharedFrequencyBandParameters::setHighFrequency);
        highFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachTextCellFactories(v0Col, SharedFrequencyBandParameters::getVelocity0, dfmt4);
        CellBindingUtils.attachTextCellFactories(v1Col, SharedFrequencyBandParameters::getVelocity1, dfmt4);
        CellBindingUtils.attachTextCellFactories(v2Col, SharedFrequencyBandParameters::getVelocity2, dfmt4);
        CellBindingUtils.attachTextCellFactories(b0Col, SharedFrequencyBandParameters::getBeta0, dfmt4);
        CellBindingUtils.attachTextCellFactories(b1Col, SharedFrequencyBandParameters::getBeta1, dfmt4);
        CellBindingUtils.attachTextCellFactories(b2Col, SharedFrequencyBandParameters::getBeta2, dfmt4);
        CellBindingUtils.attachTextCellFactories(g0Col, SharedFrequencyBandParameters::getGamma0, dfmt4);
        CellBindingUtils.attachTextCellFactories(g1Col, SharedFrequencyBandParameters::getGamma1, dfmt4);
        CellBindingUtils.attachTextCellFactories(g2Col, SharedFrequencyBandParameters::getGamma2, dfmt4);

        CellBindingUtils.attachEditableTextCellFactories(minSnrCol, SharedFrequencyBandParameters::getMinSnr, SharedFrequencyBandParameters::setMinSnr);

        CellBindingUtils.attachTextCellFactories(s1Col, SharedFrequencyBandParameters::getS1);
        CellBindingUtils.attachTextCellFactories(s2Col, SharedFrequencyBandParameters::getS2);
        CellBindingUtils.attachTextCellFactories(xcCol, SharedFrequencyBandParameters::getXc);
        CellBindingUtils.attachTextCellFactories(xtCol, SharedFrequencyBandParameters::getXt);
        CellBindingUtils.attachTextCellFactories(qCol, SharedFrequencyBandParameters::getQ);

        CellBindingUtils.attachEditableTextCellFactories(minLengthCol, SharedFrequencyBandParameters::getMinLength, SharedFrequencyBandParameters::setMinLength);
        CellBindingUtils.attachEditableTextCellFactories(maxLengthCol, SharedFrequencyBandParameters::getMaxLength, SharedFrequencyBandParameters::setMaxLength);
        CellBindingUtils.attachEditableTextCellFactories(measureTimeCol, SharedFrequencyBandParameters::getMeasurementTime, SharedFrequencyBandParameters::setMeasurementTime);

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
