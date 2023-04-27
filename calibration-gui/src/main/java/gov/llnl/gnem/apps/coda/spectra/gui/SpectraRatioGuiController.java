/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.spectra.gui;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.controllers.RefreshableController;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraRatioClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioMeasurementEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent.Status;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraEvent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

@Component
public class SpectraRatioGuiController implements RefreshableController {

    private static final Logger log = LoggerFactory.getLogger(SpectraRatioGuiController.class);

    @FXML
    private TableView<SpectraEvent> tableView;

    @FXML
    private TableColumn<SpectraEvent, String> eventCol;

    @FXML
    private TableColumn<SpectraEvent, Double> fitMwCol;

    @FXML
    private TableColumn<SpectraEvent, Double> refMwCol;

    @FXML
    private TableColumn<SpectraEvent, String> dateCol;

    @FXML
    private TableColumn<SpectraEvent, CheckBox> numCol;

    @FXML
    private TableColumn<SpectraEvent, CheckBox> denCol;

    @FXML
    private MenuItem setRowsAsNumeratorBtn;

    @FXML
    private MenuItem setRowsAsDenominatorBtn;

    @FXML
    private MenuItem deselectRowsBtn;

    @FXML
    private Button calcRatioBtn;

    private Alert alertPopup;

    private ObservableList<SpectraEvent> listData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private EventClient eventClient;
    private SpectraClient spectraClient;
    private SpectraRatioClient spectraRatioClient;

    private EventBus bus;
    private boolean calculationProcessing = false;
    private long currentRatioStatusEventId = 0l;

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Ratio-Scheduled");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledExecutorService guiService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Ratio-Gui-Service");
        thread.setDaemon(true);
        return thread;
    });

    private Stage stage;

    public SpectraRatioGuiController(EventClient eventClient, SpectraClient spectraClient, SpectraRatioClient spectraRatioClient, EventBus bus, ConfigurableApplicationContext springContext)
            throws IOException {
        this.eventClient = eventClient;
        this.spectraClient = spectraClient;
        this.spectraRatioClient = spectraRatioClient;
        this.bus = bus;
    }

    @FXML
    private void openFailureReportDisplay() {
        bus.post(new ShowFailureReportEvent());
    }

    private Double getEventFitMw(SpectraEvent event) {
        if (event == null) {
            return null;
        }

        List<Spectra> eventSpectra = new ArrayList<>(spectraClient.getFitSpectra(event.getEventID()).block(Duration.ofSeconds(2)));

        if (eventSpectra.isEmpty()) {
            return null;
        }

        double eventMw = 0.0;
        for (Spectra spectra : eventSpectra) {
            if (spectra.getType() == SPECTRA_TYPES.FIT) {
                eventMw = spectra.getMw();
                break;
            }
        }
        return eventMw;
    }

    private Double getEventRefMw(SpectraEvent event) {
        if (event == null) {
            return null;
        }

        Spectra eventSpectra = spectraClient.getReferenceSpectra(event.getEventID()).block(Duration.ofSeconds(2));

        if (eventSpectra == null || eventSpectra.getMw() < 0.0) {
            return null;
        }

        return eventSpectra.getMw();
    }

    public void loadEnvelopes() {
        requestData();
    }

    @FXML
    public void initialize() {
        bus.register(this);
        this.alertPopup = new Alert(Alert.AlertType.INFORMATION);
        alertPopup.setTitle("Notice");
        alertPopup.setHeaderText(null);
        alertPopup.setContentText("You need to select at least 1 numerator event and 1 denominator event to calculate ratios.");

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        eventCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(SpectraEvent::getEventID).orElseGet(String::new)));
        eventCol.comparatorProperty().set(new MaybeNumericStringComparator());

        fitMwCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(this::getEventFitMw).orElseGet(null)));
        refMwCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(this::getEventRefMw).orElseGet(null)));

        dateCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(event -> eventClient.getEvent(event.getEventID()).block().getOriginTime().toString()).orElseGet(String::new)));

        numCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(event -> {
            CheckBox box = new CheckBox();
            box.setSelected(event.isNumerator());
            box.selectedProperty().addListener((obs, o, n) -> {
                if (n != null && !o.equals(n)) {
                    event.setNumerator(n);
                    tableView.refresh();
                }
            });
            return box;
        }).orElseGet(CheckBox::new)));
        numCol.comparatorProperty().set((c1, c2) -> Boolean.compare(c1.isSelected(), c2.isSelected()));

        denCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(event -> {
            CheckBox box = new CheckBox();
            box.setSelected(event.isDenominator());
            box.selectedProperty().addListener((obs, o, n) -> {
                if (n != null && !o.equals(n)) {
                    event.setDenominator(n);
                    tableView.refresh();
                }
            });
            return box;
        }).orElseGet(CheckBox::new)));
        denCol.comparatorProperty().set((c1, c2) -> Boolean.compare(c1.isSelected(), c2.isSelected()));

        tableView.setItems(listData);
    }

    @PreDestroy
    private void cleanUp() {
        service.shutdownNow();
    }

    public void toFront() {
        stage.show();
        stage.toFront();
    }

    @FXML
    private void setRowsAsNumerator() {
        if (tableView != null) {
            tableView.getSelectionModel().getSelectedItems().forEach(selectedEvent -> {
                CheckBox box = numCol.getCellData(selectedEvent);
                if (box != null) {
                    box.setSelected(true);
                }
            });
        }
    }

    @FXML
    private void setRowsAsDenominator() {
        if (tableView != null) {
            tableView.getSelectionModel().getSelectedItems().forEach(selectedEvent -> {
                CheckBox box = denCol.getCellData(selectedEvent);
                if (box != null) {
                    box.setSelected(true);
                }
            });
        }
    }

    @FXML
    private void deselectRows() {
        if (tableView != null) {
            tableView.getSelectionModel().getSelectedItems().forEach(selectedEvent -> {
                CheckBox numBox = numCol.getCellData(selectedEvent);
                if (numBox != null && numBox.isSelected()) {
                    numBox.setSelected(false);
                }
                CheckBox denBox = denCol.getCellData(selectedEvent);
                if (denBox != null && denBox.isSelected()) {
                    denBox.setSelected(false);
                }
            });
        }
    }

    @FXML
    private void calculateSpectraRatio() {

        // Prevent multiple clicks of the calculate button causing ratios to be calculated too many times
        if (calculationProcessing) {
            return;
        }

        List<String> smallEventIds = this.listData.stream().filter(SpectraEvent::isDenominator).map(SpectraEvent::getEventID).collect(Collectors.toList());

        List<String> largeEventIds = this.listData.stream().filter(SpectraEvent::isNumerator).map(SpectraEvent::getEventID).collect(Collectors.toList());

        if (smallEventIds.size() > 0 && largeEventIds.size() > 0) {

            calculationProcessing = true;
            Platform.runLater(() -> calcRatioBtn.setDisable(true));

            spectraRatioClient.makeSpectraRatioMeasurements(true, true, smallEventIds, largeEventIds).doOnError(err -> {
                bus.post(new RatioStatusEvent(currentRatioStatusEventId, Status.ERROR));
                log.error(err.getMessage());
            }).subscribe(mwRatioReportByEventPair -> bus.post(new RatioMeasurementEvent(currentRatioStatusEventId, new Result<>(true, mwRatioReportByEventPair))));

            // Small delay so plots are loaded before sending completion status
            guiService.schedule(() -> {
                calculationProcessing = false;
                Platform.runLater(() -> calcRatioBtn.setDisable(false));
                bus.post(new RatioStatusEvent(currentRatioStatusEventId, Status.COMPLETE));
            }, 1, TimeUnit.SECONDS);
            log.trace("Received measured spectra ratio.");
        } else {
            alertPopup.show();
        }
    }

    @Subscribe
    private void listener(final RatioStatusEvent event) {
        // Capturing the event id of incoming ratio status event
        // so we can use the id to update it's completion status later
        if ((event != null) && (event.getStatus() == Status.STARTING)) {
            currentRatioStatusEventId = event.getId();
        }
    }

    private void requestData() {
        List<SpectraEvent> uniqueEvents = eventClient.getUniqueEventIds().map(SpectraEvent::new).collectList().block(Duration.ofMinutes(10l));
        synchronized (listData) {
            listData.clear();
            listData.addAll(uniqueEvents);
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return this::requestData;
    }
}
