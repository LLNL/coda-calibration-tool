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

import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;

@Component
public class SiteBandController {

    private static final Logger log = LoggerFactory.getLogger(SiteBandController.class);

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    @FXML
    private TableView<SiteFrequencyBandParameters> codaSiteTableView;

    @FXML
    TableColumn<SiteFrequencyBandParameters, String> stationCol;

    @FXML
    TableColumn<SiteFrequencyBandParameters, String> siteLowFreqCol;

    @FXML
    TableColumn<SiteFrequencyBandParameters, String> siteHighFreqCol;

    @FXML
    TableColumn<SiteFrequencyBandParameters, String> siteCorrectionCol;

    private ObservableList<SiteFrequencyBandParameters> siteFbData = FXCollections.observableArrayList();

    private ParameterClient client;

    @Autowired
    public SiteBandController(ParameterClient client) {
        this.client = client;
    }

    @FXML
    private void reloadData(ActionEvent e) {
        requestData();
    }

    @FXML
    public void initialize() {

        stationCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x)
                                      .map(CellDataFeatures::getValue)
                                      .map(SiteFrequencyBandParameters::getStation)
                                      .filter(Objects::nonNull)
                                      .map(Station::getStationName)
                                      .filter(Objects::nonNull)
                                      .orElseGet(String::new)));

        siteLowFreqCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x)
                                      .map(CellDataFeatures::getValue)
                                      .map(SiteFrequencyBandParameters::getLowFrequency)
                                      .filter(Objects::nonNull)
                                      .map(dfmt2::format)
                                      .orElseGet(String::new)));
        siteLowFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        siteHighFreqCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x)
                                      .map(CellDataFeatures::getValue)
                                      .map(SiteFrequencyBandParameters::getHighFrequency)
                                      .filter(Objects::nonNull)
                                      .map(dfmt2::format)
                                      .orElseGet(String::new)));
        siteHighFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        siteCorrectionCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(SiteFrequencyBandParameters::getSiteTerm).filter(Objects::nonNull).map(dfmt2::format).orElseGet(String::new)));

        codaSiteTableView.setItems(siteFbData);
    }

    protected void requestData() {
        siteFbData.clear();
        client.getSiteSpecificFrequencyBandParameters()
              .filter(Objects::nonNull)
              .filter(value -> null != value.getId())
              .doOnComplete(() -> codaSiteTableView.sort())
              .subscribe(value -> siteFbData.add(value), err -> log.trace(err.getMessage(), err));
    }
}
