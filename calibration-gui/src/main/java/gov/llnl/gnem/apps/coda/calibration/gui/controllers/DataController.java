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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;

@Component
public class DataController implements MapListeningController, RefreshableController {

    private static final Logger log = LoggerFactory.getLogger(DataController.class);

    @FXML
    private MenuItem importWaveforms;

    @FXML
    private TableView<Waveform> tableView;

    @FXML
    private CheckBox selectAllCheckbox;

    @FXML
    private TableColumn<Waveform, String> stationCol;

    @FXML
    private TableColumn<Waveform, String> eventCol;

    @FXML
    private TableColumn<Waveform, String> lowFreqCol;

    @FXML
    private TableColumn<Waveform, String> highFreqCol;

    private ObservableList<Waveform> listData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private GeoMap mapImpl;

    private MapPlottingUtilities iconFactory;

    private WaveformClient client;

    private EventBus bus;

    private NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    private EventStaFreqStringComparator eventStaFreqComparator = new EventStaFreqStringComparator();

    private ListChangeListener<? super Waveform> tableChangeListener;

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;

    @Autowired
    public DataController(WaveformClient client, GeoMap mapImpl, MapPlottingUtilities iconFactory, EventBus bus) {
        super();
        this.client = client;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
        this.bus = bus;
        bus.register(this);
        tableChangeListener = buildTableListener();

        eventSelectionCallback = (selected, eventId) -> {
            selectDataByCriteria(bus, selected, (w) -> w.getEvent() != null && w.getEvent().getEventId().equalsIgnoreCase(eventId));
        };

        stationSelectionCallback = (selected, stationId) -> {
            selectDataByCriteria(bus, selected, (w) -> w.getStream() != null && w.getStream().getStation() != null && w.getStream().getStation().getStationName().equalsIgnoreCase(stationId));
        };
    }

    private void selectDataByCriteria(EventBus bus, Boolean selected, Function<Waveform, Boolean> matchCriteria) {
        List<Waveform> selection = new ArrayList<>();
        List<Integer> selectionIndices = new ArrayList<>();
        tableView.getSelectionModel().clearSelection();
        if (selected) {
            for (int i = 0; i < listData.size(); i++) {
                Waveform w = listData.get(i);
                if (matchCriteria.apply(w)) {
                    selection.add(w);
                    tableView.getSelectionModel().select(i);
                }
            }
            if (!selection.isEmpty()) {
                selection.sort(eventStaFreqComparator);
                Long[] ids = selection.stream().sequential().map(w -> w.getId()).collect(Collectors.toList()).toArray(new Long[0]);
                bus.post(new WaveformSelectionEvent(ids));
            }
        } else {
            selection.addAll(tableView.getSelectionModel().getSelectedItems());
            selectionIndices.addAll(tableView.getSelectionModel().getSelectedIndices());
            for (int i = 0; i < selection.size(); i++) {
                if (matchCriteria.apply(selection.get(i))) {
                    tableView.getSelectionModel().clearSelection(selectionIndices.get(i));
                }
            }
        }
    }

    private ListChangeListener<? super Waveform> buildTableListener() {
        return (ListChangeListener<Waveform>) change -> {
            List<Waveform> selection = new ArrayList<>();
            selection.addAll(tableView.getSelectionModel().getSelectedItems());
            selection.sort(eventStaFreqComparator);
            Long[] ids = selection.stream().sequential().map(w -> w.getId()).collect(Collectors.toList()).toArray(new Long[0]);
            bus.post(new WaveformSelectionEvent(ids));
        };
    }

    @FXML
    private void reloadTable(ActionEvent e) {
        CompletableFuture.runAsync(getRefreshFunction());
    }

    @FXML
    public void initialize() {
        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        eventCol.setCellValueFactory(
                x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(Waveform::getEvent).map(Event::getEventId).orElseGet(String::new)));
        eventCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachTextCellFactories(lowFreqCol, Waveform::getLowFrequency, dfmt2);
        CellBindingUtils.attachTextCellFactories(highFreqCol, Waveform::getHighFrequency, dfmt2);

        stationCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(Waveform::getStream).map(Stream::getStation).map(Station::getStationName).orElseGet(String::new)));

        tableView.getSelectionModel().getSelectedItems().addListener(tableChangeListener);
        //Workaround for https://bugs.openjdk.java.net/browse/JDK-8095943, for now we just clear the selection to avoid dumping a stack trace in the logs and mucking up event bubbling
        tableView.setOnSort(event -> {
            if (tableView.getSelectionModel().getSelectedIndices().size() > 1) {
                tableView.getSelectionModel().clearSelection();
            }
        });
        tableView.setItems(listData);
    }

    @Override
    public void refreshView() {
        if (listData.isEmpty()) {
            CompletableFuture.runAsync(getRefreshFunction());
        } else {
            CompletableFuture.runAsync(() -> {
                mapImpl.clearIcons();
                listData.forEach(waveform -> mapImpl.addIcons(genIconsFromData(waveform)));
            });
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> requestData();
    }

    private void requestData() {
        listData.clear();
        mapImpl.clearIcons();
        client.getUniqueEventStationMetadataForStacks().filter(Objects::nonNull).doOnComplete(() -> {
            tableView.sort();
            refreshView();
        }).subscribe(waveform -> {
            listData.add(waveform);
        }, err -> log.error(err.getMessage(), err));
    }

    protected List<Icon> genIconsFromData(Waveform waveform) {
        List<Icon> icons = new ArrayList<>();
        if (waveform != null && waveform.getStream() != null && waveform.getEvent() != null) {
            icons.add(iconFactory.createEventIcon(waveform.getEvent()).setIconSelectionCallback(eventSelectionCallback));
            icons.add(iconFactory.createStationIcon(waveform.getStream().getStation()).setIconSelectionCallback(stationSelectionCallback));
        }
        return icons;
    }

}
