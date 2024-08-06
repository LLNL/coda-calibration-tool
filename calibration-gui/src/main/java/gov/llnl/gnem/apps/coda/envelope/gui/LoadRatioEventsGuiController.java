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
package gov.llnl.gnem.apps.coda.envelope.gui;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import jakarta.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraRatioClient;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioMeasurementEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent.Status;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraEvent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Component
public class LoadRatioEventsGuiController {

    private static final Logger log = LoggerFactory.getLogger(LoadRatioEventsGuiController.class);

    private Alert alertPopup;

    @FXML
    private StackPane loadGuiRoot;

    @FXML
    private TableView<SpectraEvent> tableView;

    @FXML
    private TableColumn<SpectraEvent, String> evidCol;

    @FXML
    private TableColumn<SpectraEvent, String> dateCol;

    @FXML
    private TableColumn<SpectraEvent, String> stationNameCol;

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

    @FXML
    Button loadBtn;

    private LoadRatioEventsJSON ratioEventsLoader;

    private FileChooser ratioEventFileChooser = new FileChooser();

    private ObservableList<SpectraEvent> listData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private EventBus bus;
    private boolean calculationProcessing = false;
    private long currentRatioStatusEventId = 0l;
    private SpectraRatioClient spectraRatioClient;

    private ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Ratio-Load-Scheduled");
        thread.setDaemon(true);
        return thread;
    });

    private ScheduledExecutorService guiService = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("Ratio-Load-Gui-Service");
        thread.setDaemon(true);
        return thread;
    });

    private Stage stage;

    @Autowired
    public LoadRatioEventsGuiController(SpectraRatioClient spectraRatioClient, EventBus bus, LoadRatioEventsJSON ratioEventsLoader, ConfigurableApplicationContext springContext) throws IOException {
        this.bus = bus;
        this.ratioEventsLoader = ratioEventsLoader;
        this.spectraRatioClient = spectraRatioClient;

        ratioEventFileChooser.getExtensionFilters().addAll(new ExtensionFilter("JSON Event Data", "*.json"), new ExtensionFilter("All files", "*.*"));
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/LoadRatioEventsGui.fxml"));
            fxmlLoader.setController(this);
            fxmlLoader.setControllerFactory(springContext::getBean);
            stage = new Stage(StageStyle.DECORATED);
            try {
                loadGuiRoot = fxmlLoader.load();
                Scene scene = new Scene(loadGuiRoot);
                stage.setScene(scene);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @FXML
    private void openFailureReportDisplay() {
        bus.post(new ShowFailureReportEvent());
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
    private void loadFiles(final ActionEvent e) {
        Optional.ofNullable(ratioEventFileChooser.showOpenMultipleDialog(loadGuiRoot.getScene().getWindow())).ifPresent(this::handleDroppedFiles);
    }

    @FXML
    private void calculateSpectraRatio() {

        List<RatioEventData> ratioEventDataLoaded = ratioEventsLoader.getLoadedRatioEventData();

        // Prevent multiple clicks of the calculate button causing ratios to be calculated too many times
        if (calculationProcessing || ratioEventDataLoaded == null) {
            return;
        }

        Set<String> smallEventIds = this.listData.stream().filter(SpectraEvent::isDenominator).map(SpectraEvent::getEventID).collect(Collectors.toSet());

        Set<String> largeEventIds = this.listData.stream().filter(SpectraEvent::isNumerator).map(SpectraEvent::getEventID).collect(Collectors.toSet());

        if (smallEventIds.size() > 0 && largeEventIds.size() > 0) {

            calculationProcessing = true;
            Platform.runLater(() -> calcRatioBtn.setDisable(true));

            spectraRatioClient.makeSpectraRatioMeasurementsFromRatioData(smallEventIds, largeEventIds, ratioEventDataLoaded).doOnError(err -> {
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

    @FXML
    private void clearRatioEvents() {
        log.info("Clear ratio events table.");
        ratioEventsLoader.clearLoadedData();
        listData.clear();
    }

    private void handleDroppedFiles(List<File> files) {
        boolean hasJson = files.stream().anyMatch(f -> f.getName().toLowerCase(Locale.ENGLISH).endsWith(".json"));
        if (hasJson) {
            ratioEventsLoader.loadFiles(files, () -> {
                List<RatioEventData> loadedJsonData = ratioEventsLoader.getLoadedRatioEventData();
                if (loadedJsonData != null) {
                    listData.clear();
                    listData.setAll(loadedJsonData.stream().map(data -> new SpectraEvent(data.getEventId())).collect(Collectors.toList()));
                }
            });
        }
    }

    @FXML
    public void initialize() {
        bus.register(this);
        this.alertPopup = new Alert(Alert.AlertType.INFORMATION);
        alertPopup.setTitle("Notice");
        alertPopup.setHeaderText(null);
        alertPopup.setContentText("You need to select at least 1 numerator event and 1 denominator event to calculate ratios.");

        tableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        evidCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(SpectraEvent::getEventID).orElseGet(String::new)));
        evidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        dateCol.setCellValueFactory(
                x -> Bindings.createStringBinding(
                        () -> Optional.ofNullable(x)
                                      .map(CellDataFeatures::getValue)
                                      .map(event -> ratioEventsLoader.getRatioEventByEventId(event.getEventID()).getDate().toString())
                                      .orElseGet(String::new)));

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

        loadGuiRoot.setOnDragOver(event -> {
            if (event.getGestureSource() != loadGuiRoot && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
                event.consume();
            }
        });

        loadGuiRoot.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getGestureSource() != loadGuiRoot && event.getDragboard().hasFiles()) {
                handleDroppedFiles(event.getDragboard().getFiles());
                success = true;
            }
            event.setDropCompleted(success);
            event.consume();
        });

        bus.register(this);
    }

    @PreDestroy
    private void cleanUp() {
        service.shutdownNow();
    }

    public void toFront() {
        stage.show();
        stage.toFront();
    }
}
