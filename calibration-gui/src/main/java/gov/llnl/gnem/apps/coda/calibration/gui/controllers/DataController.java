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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.api.IconFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
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

@Component
public class DataController {

    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    @FXML
    private MenuItem importWaveforms;

    @FXML
    private TableView<Waveform> tableView;

    @FXML
    private TableColumn<Waveform, Boolean> selectionCol;

    @FXML
    private CheckBox selectAllCheckbox;

    @FXML
    private TableColumn<Waveform, String> stationCol;

    @FXML
    private TableColumn<Waveform, String> eventCol;

    private ObservableList<Waveform> listData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private GeoMap mapImpl;

    private IconFactory iconFactory;

    private WaveformClient client;

    private EventBus bus;

    @Autowired
    public DataController(WaveformClient client, GeoMap mapImpl, IconFactory iconFactory, EventBus bus) {
        super();
        this.client = client;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
        this.bus = bus;
        bus.register(this);
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

        eventCol.setCellValueFactory(
                x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(Waveform::getEvent).map(Event::getEventId).orElseGet(String::new)));

        eventCol.comparatorProperty().set(new MaybeNumericStringComparator());

        stationCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(Waveform::getStream).map(Stream::getStation).map(Station::getStationName).orElseGet(String::new)));

        tableView.setItems(listData);
    }

    public void refreshView() {
        if (listData.isEmpty()) {
            requestData();
        } else {
            mapImpl.clearIcons();
            listData.forEach(waveform -> genIconsFromData(waveform));
        }
    }

    public void update() {
        requestData();
    }

    private void requestData() {
        listData.clear();
        mapImpl.clearIcons();
        client.getUniqueEventStationMetadataForStacks().filter(Objects::nonNull).doOnComplete(() -> tableView.sort()).subscribe(waveform -> {
            listData.add(waveform);
            genIconsFromData(waveform);
        }, err -> log.trace(err.getMessage(), err));
    }

    protected void genIconsFromData(Waveform waveform) {
        mapImpl.addIcon(
                iconFactory.newIcon(
                        IconTypes.TRIANGLE_UP,
                            new Location(waveform.getStream().getStation().getLatitude(), waveform.getStream().getStation().getLongitude()),
                            waveform.getStream().getStation().getStationName()));
        mapImpl.addIcon(
                iconFactory.newIcon(
                        waveform.getEvent().getEventId(),
                            IconTypes.CIRCLE,
                            new Location(waveform.getEvent().getLatitude(), waveform.getEvent().getLongitude()),
                            waveform.getEvent().getEventId()));
    }

}
