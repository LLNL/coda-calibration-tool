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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.IconFactory;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Location;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Map;
import gov.llnl.gnem.apps.coda.calibration.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Stream;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;

@Component
public class DataController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @FXML
    private MenuItem importWaveforms;

    @FXML
    private TableView<Waveform> tableView;

    @FXML
    TableColumn<Waveform, Boolean> selectionCol;

    @FXML
    CheckBox selectAllCheckbox;

    @FXML
    TableColumn<Waveform, String> stationCol;

    @FXML
    TableColumn<Waveform, String> eventCol;

    @FXML
    StackPane mapParent;

    private ObservableList<Waveform> listData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private Map mapImpl;

    private IconFactory iconFactory;

    private WaveformClient client;

    @Autowired
    public DataController(WaveformClient client, Map mapImpl, IconFactory iconFactory) {
        super();
        this.client = client;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
    }

    @FXML
    private void reloadTable(ActionEvent e) {
        requestData();
    }

    @FXML
    public void initialize() {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // TODO: Waveform has no "selected" need to change this to look at the
        // selection model by WaveformId.
        selectionCol.setCellValueFactory(new PropertyValueFactory<Waveform, Boolean>("checkBoxValue"));
        selectionCol.setCellFactory(CheckBoxTableCell.forTableColumn(selectionCol));

        eventCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                     .map(CellDataFeatures::getValue)
                                                                                     .map(Waveform::getEvent)
                                                                                     .map(Event::getEventId)
                                                                                     .orElseGet(String::new)));

        eventCol.comparatorProperty().set(new MaybeNumericStringComparator());

        stationCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x)
                                                                                       .map(CellDataFeatures::getValue)
                                                                                       .map(Waveform::getStream)
                                                                                       .map(Stream::getStation)
                                                                                       .map(Station::getStationName)
                                                                                       .orElseGet(String::new)));

        tableView.setItems(listData);

        mapImpl.attach(mapParent);
    }

    private void requestData() {
        listData.clear();
        mapImpl.clearIcons();
        client.getUniqueEventStationStacks().filter(Objects::nonNull).subscribe(waveform -> {
            listData.add(waveform);
            mapImpl.addIcon(iconFactory.newIcon(IconTypes.TRIANGLE_UP,
                                                new Location(waveform.getStream().getStation().getLatitude(), waveform.getStream().getStation().getLongitude()),
                                                waveform.getStream().getStation().getStationName()));
            mapImpl.addIcon(iconFactory.newIcon(new Location(waveform.getEvent().getLatitude(), waveform.getEvent().getLongitude()), waveform.getEvent().getEventId()));
        }, err -> log.trace(err.getMessage(), err));
    }

}
