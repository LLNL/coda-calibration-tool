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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

@Component
public class ShapeController implements MapListeningController, RefreshableController {

    private static final Logger log = LoggerFactory.getLogger(ShapeController.class);

    private static final double X_TICK_LABEL_COUNT = 10.0;
    private static final double Y_TICK_LABEL_COUNT = 6.0;
    private static final double VEL_Y_MIN = 0;
    private static final double VEL_Y_MAX = 6;
    private static final double BETA_Y_MIN = -0.12;
    private static final double BETA_Y_MAX = 0.01;
    private static final double GAMMA_Y_MIN = -1.0;
    private static final double GAMMA_Y_MAX = 3.5;
    private static final double LINE_SEGMENTS = 250.0;
    private final AtomicReference<Integer> minX = new AtomicReference<>(0);
    private final AtomicReference<Integer> maxX = new AtomicReference<>(0);

    private Map<FrequencyBand, List<PeakVelocityMeasurement>> velocityDistancePairsFreqMap = new HashMap<>();
    private Map<FrequencyBand, List<ShapeMeasurement>> shapeDistancePairsFreqMap = new HashMap<>();
    private Map<FrequencyBand, SharedFrequencyBandParameters> modelCurveMap = new TreeMap<>();
    private Map<Long, Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>>> waveformIdMapping = new HashMap<>();
    private EventStaFreqStringComparator eventStaFreqComparator = new EventStaFreqStringComparator();

    @FXML
    private StackPane shape;

    @FXML
    private ComboBox<FrequencyBand> frequencyBandCombo;

    @FXML
    private StackPane velPlotContainer;

    @FXML
    private StackPane betaPlotContainer;

    @FXML
    private StackPane gammaPlotContainer;

    @FXML
    private LineChart<Number, Number> velFitPlot;

    @FXML
    private LineChart<Number, Number> betaFitPlot;

    @FXML
    private LineChart<Number, Number> gammaFitPlot;

    @FXML
    private Button yAxisShrink;

    private ParameterClient paramClient;
    private PeakVelocityClient velocityClient;
    private ShapeMeasurementClient shapeClient;
    private WaveformClient waveformClient;
    private NumberFormat dfmt = NumberFormatFactory.twoDecimalOneLeadingZero();

    private ObservableList<Data<Number, Number>> velModelData = FXCollections.observableArrayList();
    private Series<Number, Number> velModelCurveSeries = new Series<>(velModelData);
    private ObservableList<Data<Number, Number>> velPointData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> velSelectedData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> velHighlightedData = FXCollections.observableArrayList();
    private Series<Number, Number> velPointSeries = new Series<>(velPointData);
    private Series<Number, Number> velSelectedSeries = new Series<>(velSelectedData);
    private Series<Number, Number> velHighlightedSeries = new Series<>(velHighlightedData);
    private ObservableList<Series<Number, Number>> velSeries = FXCollections.observableArrayList();

    private ObservableList<Data<Number, Number>> betaModelData = FXCollections.observableArrayList();
    private Series<Number, Number> betaModelCurveSeries = new Series<>(betaModelData);
    private ObservableList<Data<Number, Number>> betaPointData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> betaSelectedData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> betaHighlightedData = FXCollections.observableArrayList();
    private Series<Number, Number> betaPointSeries = new Series<>(betaPointData);
    private Series<Number, Number> betaSelectedSeries = new Series<>(betaSelectedData);
    private Series<Number, Number> betaHighlightedSeries = new Series<>(betaHighlightedData);
    private ObservableList<Series<Number, Number>> betaSeries = FXCollections.observableArrayList();

    private ObservableList<Data<Number, Number>> gammaModelData = FXCollections.observableArrayList();
    private Series<Number, Number> gammaModelCurveSeries = new Series<>(gammaModelData);
    private ObservableList<Data<Number, Number>> gammaPointData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> gammaSelectedData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> gammaHighlightedData = FXCollections.observableArrayList();
    private Series<Number, Number> gammaPointSeries = new Series<>(gammaPointData);
    private Series<Number, Number> gammaSelectedSeries = new Series<>(gammaSelectedData);
    private Series<Number, Number> gammaHighlightedSeries = new Series<>(gammaHighlightedData);
    private ObservableList<Series<Number, Number>> gammaSeries = FXCollections.observableArrayList();

    private EventBus bus;
    private GeoMap mapImpl;
    private MapPlottingUtilities iconFactory;
    private Map<String, List<Data<Number, Number>>> velPointMap = new HashMap<>();
    private Map<String, List<Data<Number, Number>>> betaPointMap = new HashMap<>();
    private Map<String, List<Data<Number, Number>>> gammaPointMap = new HashMap<>();

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;
    private FrequencyBand selectedBand;
    private MenuItem exclude;
    private MenuItem include;
    private ContextMenu menu;
    private boolean shouldYAxisShrink = false;
    private Label yAxisShrinkOn;
    private Label yAxisShrinkOff;

    private boolean isVisible = false;

    @Autowired
    private ShapeController(ParameterClient paramClient, PeakVelocityClient velocityClient, ShapeMeasurementClient shapeClient, WaveformClient waveformClient, GeoMap map,
            MapPlottingUtilities iconFactory, EventBus bus) {
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
        this.shapeClient = shapeClient;
        this.waveformClient = waveformClient;
        this.bus = bus;
        this.mapImpl = map;
        this.iconFactory = iconFactory;

        eventSelectionCallback = (selected, eventId) -> {
            selectDataByCriteria(bus, selected, eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            selectDataByCriteria(bus, selected, stationId);
        };

        this.bus.register(this);
    }

    private void selectDataByCriteria(EventBus bus, Boolean selected, String key) {
        selectDataByCriteria(bus, selected, key, velPointMap, velHighlightedData, velSelectedData);
        selectDataByCriteria(bus, selected, key, betaPointMap, betaHighlightedData, betaSelectedData);
        selectDataByCriteria(bus, selected, key, gammaPointMap, gammaHighlightedData, gammaSelectedData);
    }

    private void selectDataByCriteria(EventBus bus, Boolean selected, String key, Map<String, List<Data<Number, Number>>> pointMap, ObservableList<Data<Number, Number>> highlightedData,
            List<Data<Number, Number>> selectedData) {

        List<Data<Number, Number>> selection = pointMap.get(key);
        if (selection != null && !selection.isEmpty()) {
            if (selected) {
                highlightedData.clear();
                selectedData.addAll(selection.stream().map(d -> new Data<>(d.getXValue(), d.getYValue())).collect(Collectors.toList()));
                selectedData.forEach(x -> x.getNode().setMouseTransparent(true));
                if (velocityDistancePairsFreqMap != null) {
                    List<PeakVelocityMeasurement> data = velocityDistancePairsFreqMap.get(selectedBand);
                    if (data != null && !data.isEmpty()) {
                        Long[] ids = data.stream()
                                         .sequential()
                                         .filter(
                                                 pv -> pv != null
                                                         && pv.getWaveform() != null
                                                         && ((pv.getWaveform().getEvent() != null
                                                                 && pv.getWaveform().getEvent().getEventId() != null
                                                                 && pv.getWaveform().getEvent().getEventId().equalsIgnoreCase(key))
                                                                 || (pv.getWaveform().getStream() != null
                                                                         && pv.getWaveform().getStream().getStation() != null
                                                                         && pv.getWaveform().getStream().getStation().getStationName() != null
                                                                         && pv.getWaveform().getStream().getStation().getStationName().equalsIgnoreCase(key))))
                                         .sorted((pv1, pv2) -> eventStaFreqComparator.compare(pv1.getWaveform(), pv2.getWaveform()))
                                         .map(pv -> pv.getWaveform().getId())
                                         .collect(Collectors.toList())
                                         .toArray(new Long[0]);
                        bus.post(new WaveformSelectionEvent(ids));
                    }
                }
            } else {
                selectedData.clear();
            }
        }
    }

    @FXML
    public void initialize() {
        velFitPlot.setAnimated(false);
        betaFitPlot.setAnimated(false);
        gammaFitPlot.setAnimated(false);

        yAxisShrinkOn = new Label("><");
        yAxisShrinkOn.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        yAxisShrinkOn.setRotate(90.0);
        yAxisShrinkOff = new Label("<>");
        yAxisShrinkOff.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        yAxisShrinkOff.setRotate(90.0);

        yAxisShrink.setGraphic(yAxisShrinkOn);
        double topPad = yAxisShrink.getPadding().getTop();
        double bottomPad = yAxisShrink.getPadding().getBottom();
        yAxisShrink.setPadding(new Insets(topPad, 0, bottomPad, 0));
        yAxisShrink.prefHeightProperty().bind(frequencyBandCombo.heightProperty());
        yAxisShrink.setOnAction(e -> {
            shouldYAxisShrink = !shouldYAxisShrink;
            if (shouldYAxisShrink) {
                yAxisShrink.setGraphic(yAxisShrinkOff);
            } else {
                yAxisShrink.setGraphic(yAxisShrinkOn);
            }
            refreshView();
        });

        frequencyBandCombo.setCellFactory(fb -> getFBCell());
        frequencyBandCombo.setButtonCell(getFBCell());
        frequencyBandCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                selectedBand = newValue;
                refreshView();
            }
        });

        buildPlot(
                velPlotContainer,
                    velSeries,
                    velPointSeries,
                    velPointData,
                    velModelCurveSeries,
                    velModelData,
                    velSelectedSeries,
                    velSelectedData,
                    velHighlightedSeries,
                    velFitPlot,
                    () -> VEL_Y_MIN,
                    () -> VEL_Y_MAX);

        buildPlot(
                betaPlotContainer,
                    betaSeries,
                    betaPointSeries,
                    betaPointData,
                    betaModelCurveSeries,
                    betaModelData,
                    betaSelectedSeries,
                    betaSelectedData,
                    betaHighlightedSeries,
                    betaFitPlot,
                    () -> BETA_Y_MIN,
                    () -> BETA_Y_MAX);

        buildPlot(
                gammaPlotContainer,
                    gammaSeries,
                    gammaPointSeries,
                    gammaPointData,
                    gammaModelCurveSeries,
                    gammaModelData,
                    gammaSelectedSeries,
                    gammaSelectedData,
                    gammaHighlightedSeries,
                    gammaFitPlot,
                    () -> GAMMA_Y_MIN,
                    () -> GAMMA_Y_MAX);

        menu = new ContextMenu();
        include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);
    }

    private void buildPlot(final Pane parent, ObservableList<Series<Number, Number>> series, Series<Number, Number> pointSeries, ObservableList<Data<Number, Number>> pointData,
            Series<Number, Number> modelCurveSeries, ObservableList<Data<Number, Number>> modelData, Series<Number, Number> selectedSeries, ObservableList<Data<Number, Number>> selectedData,
            Series<Number, Number> highlightedSeries, LineChart<Number, Number> fitPlot, Supplier<Double> yLowBounds, Supplier<Double> yHighBounds) {
        pointSeries.setName("Measurements");
        pointSeries.setData(pointData);
        modelCurveSeries.setName("Model");
        modelCurveSeries.setData(modelData);
        selectedSeries.setData(selectedData);
        series.add(pointSeries);
        series.add(modelCurveSeries);
        series.add(selectedSeries);
        series.add(highlightedSeries);
        fitPlot.setData(series);
        fitPlot.setLegendVisible(false);

        modelCurveSeries.getNode().setMouseTransparent(true);
        modelCurveSeries.getNode().toFront();

        selectedSeries.getNode().setMouseTransparent(true);
        highlightedSeries.getNode().setMouseTransparent(true);

        fitPlot.addEventHandler(MouseEvent.MOUSE_CLICKED, (evt) -> {
            if (MouseButton.SECONDARY != evt.getButton()) {
                menu.hide();
            }
        });

        Axis<Number> xAxis = fitPlot.getXAxis();
        Axis<Number> yAxis = fitPlot.getYAxis();

        yAxis.setAutoRanging(false);
        if (yAxis instanceof NumberAxis) {
            ((NumberAxis) yAxis).setLowerBound(yLowBounds.get());
            ((NumberAxis) yAxis).setUpperBound(yHighBounds.get());
            ((NumberAxis) yAxis).setTickUnit((((NumberAxis) yAxis).getUpperBound() - ((NumberAxis) yAxis).getLowerBound()) / Y_TICK_LABEL_COUNT);
        }
        xAxis.setAutoRanging(false);
        if (xAxis instanceof NumberAxis) {
            ((NumberAxis) xAxis).setLowerBound(minX.get());
            ((NumberAxis) xAxis).setUpperBound(maxX.get());
            ((NumberAxis) xAxis).setTickUnit(maxX.get() - minX.get() / X_TICK_LABEL_COUNT);
        }
    }

    private ListCell<FrequencyBand> getFBCell() {
        return new ListCell<FrequencyBand>() {
            @Override
            protected void updateItem(FrequencyBand item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(dfmt.format(item.getLowFrequency()) + "-" + dfmt.format(item.getHighFrequency()));
                }
            }
        };
    }

    private void plotData() {
        FrequencyBand selectedFrequency = null;
        if (frequencyBandCombo != null && !frequencyBandCombo.isDisabled()) {
            selectedFrequency = frequencyBandCombo.getSelectionModel().getSelectedItem();
        }
        minX.set(0);
        maxX.set(0);
        mapValues(
                selectedFrequency,
                    Optional.ofNullable(velocityDistancePairsFreqMap.get(selectedFrequency))
                            .orElseGet(() -> Collections.emptyList())
                            .stream()
                            .map(pvm -> pvm.getWaveform())
                            .collect(Collectors.toList()));
        plotVelocity(selectedFrequency);
        plotBeta(selectedFrequency);
        plotGamma(selectedFrequency);

        rescaleXAxis(velFitPlot);
        rescaleXAxis(betaFitPlot);
        rescaleXAxis(gammaFitPlot);
    }

    private void rescaleXAxis(LineChart<Number, Number> fitPlot) {
        if (fitPlot != null && fitPlot.getXAxis() != null && fitPlot.getXAxis() instanceof NumberAxis) {
            NumberAxis xAxis = (NumberAxis) fitPlot.getXAxis();
            xAxis.setUpperBound(maxX.get());
            xAxis.setLowerBound(minX.get());
            xAxis.setTickUnit(maxX.get() - minX.get() / X_TICK_LABEL_COUNT);
        }
    }

    private void plotVelocity(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            Function<Number, Data<Number, Number>> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getVelocity0() - (model.getVelocity1() / (model.getVelocity2() + ((double) i))));

            Function<FrequencyBand, List<PeakVelocityMeasurement>> valueSupplier = createValueSupplier(velocityDistancePairsFreqMap);
            Function<PeakVelocityMeasurement, Waveform> mapFunc = PeakVelocityMeasurement::getWaveform;
            plot(
                    velPointMap,
                        velPointData,
                        velSelectedData,
                        velHighlightedData,
                        velModelCurveSeries,
                        velFitPlot,
                        selectedFrequency,
                        valueSupplier,
                        val -> new Data<>(val.getDistance(), val.getVelocity()),
                        (data, val) -> clickEventHandler(data, val.getWaveform()),
                        () -> VEL_Y_MIN,
                        () -> VEL_Y_MAX,
                        curvePointProducer,
                        mapFunc);
        }
    }

    private void plotBeta(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            Function<Number, Data<Number, Number>> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getBeta0() - (model.getBeta1() / (model.getBeta2() + ((double) i))));

            Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier = createValueSupplier(shapeDistancePairsFreqMap);
            Function<ShapeMeasurement, Waveform> mapFunc = ShapeMeasurement::getWaveform;
            plot(
                    betaPointMap,
                        betaPointData,
                        betaSelectedData,
                        betaHighlightedData,
                        betaModelCurveSeries,
                        betaFitPlot,
                        selectedFrequency,
                        valueSupplier,
                        val -> new Data<>(val.getDistance(), val.getMeasuredBeta()),
                        (data, val) -> clickEventHandler(data, val.getWaveform()),
                        () -> BETA_Y_MIN,
                        () -> BETA_Y_MAX,
                        curvePointProducer,
                        mapFunc);
        }
    }

    private void plotGamma(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            Function<Number, Data<Number, Number>> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getGamma0() - (model.getGamma1() / (model.getGamma2() + ((double) i))));

            Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier = createValueSupplier(shapeDistancePairsFreqMap);
            Function<ShapeMeasurement, Waveform> mapFunc = ShapeMeasurement::getWaveform;
            plot(
                    gammaPointMap,
                        gammaPointData,
                        gammaSelectedData,
                        gammaHighlightedData,
                        gammaModelCurveSeries,
                        gammaFitPlot,
                        selectedFrequency,
                        valueSupplier,
                        val -> new Data<>(val.getDistance(), val.getMeasuredGamma()),
                        (data, val) -> clickEventHandler(data, val.getWaveform()),
                        () -> GAMMA_Y_MIN,
                        () -> GAMMA_Y_MAX,
                        curvePointProducer,
                        mapFunc);
        }
    }

    private EventHandler<Event> clickEventHandler(Data<Number, Number> data, Waveform waveform) {
        return event -> {
            if (event instanceof MouseEvent) {
                MouseEvent evt = (MouseEvent) event;
                if (MouseButton.PRIMARY == evt.getButton()) {
                    showWaveformPopup(waveform);
                } else if (MouseButton.SECONDARY == evt.getButton()) {
                    showContextMenu(data, waveform, evt);
                }
            }
        };
    }

    private void showContextMenu(Data<Number, Number> data, Waveform waveform, MouseEvent t) {
        include.setOnAction(evt -> includeWaveforms(data, waveform));
        exclude.setOnAction(evt -> excludeWaveforms(data, waveform));
        menu.show(shape, t.getScreenX(), t.getScreenY());
    }

    private void excludeWaveforms(Data<Number, Number> data, Waveform waveform) {
        setActive(data, waveform, false);
    }

    private void includeWaveforms(Data<Number, Number> data, Waveform waveform) {
        setActive(data, waveform, true);
    }

    private void setActive(Data<Number, Number> data, Waveform waveform, boolean active) {
        if (waveformClient != null && waveform != null && waveform.getId() != null) {
            waveformClient.setWaveformsActiveByIds(Collections.singletonList(waveform.getId()), active).subscribe(s -> {
                Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>> pair = waveformIdMapping.get(waveform.getId());
                if (pair != null) {
                    pair.getLeft().forEach(v -> v.getWaveform().setActive(active));
                    pair.getRight().forEach(v -> v.getWaveform().setActive(active));
                    Platform.runLater(() -> refreshView());
                }
            });
        }
    }

    @Subscribe
    private void listener(WaveformChangeEvent wce) {
        List<Long> nonNull = wce.getIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
        synchronized (waveformIdMapping) {
            if (wce.isAddOrUpdate()) {
                List<Waveform> results = waveformClient.getWaveformMetadataFromIds(nonNull).collect(Collectors.toList()).block(Duration.ofSeconds(10l));
                if (results != null) {
                    results.forEach(md -> {
                        Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>> pair = waveformIdMapping.get(md.getId());
                        if (pair != null) {
                            pair.getLeft().forEach(v -> v.getWaveform().setActive(md.isActive()));
                            pair.getRight().forEach(v -> v.getWaveform().setActive(md.isActive()));
                        }
                    });
                }
                Platform.runLater(() -> refreshView());
            }
        }
    }

    private void mapValues(final FrequencyBand selectedFrequency, List<Waveform> waveforms) {
        mapImpl.addIcons(iconFactory.genIconsFromWaveforms(eventSelectionCallback, stationSelectionCallback, waveforms));
    }

    private <T> Function<FrequencyBand, List<T>> createValueSupplier(Map<FrequencyBand, List<T>> valueMap) {
        Function<FrequencyBand, List<T>> valueSupplier;
        if (valueMap != null) {
            valueSupplier = freq -> Optional.ofNullable(valueMap.get(freq)).orElseGet(() -> new ArrayList<>(0));
        } else {
            valueSupplier = freq -> new ArrayList<>(0);
        }
        return valueSupplier;
    }

    private Function<Number, Data<Number, Number>> createCurvePointProducer(final FrequencyBand selectedFrequency, final BiFunction<SharedFrequencyBandParameters, Number, Number> curvePointFunc) {
        Function<Number, Data<Number, Number>> curvePointProducer = null;
        if (modelCurveMap != null && selectedFrequency != null && modelCurveMap.containsKey(selectedFrequency)) {
            SharedFrequencyBandParameters model = modelCurveMap.get(selectedFrequency);
            curvePointProducer = i -> new Data<>(i, curvePointFunc.apply(model, i));
        }
        return curvePointProducer;
    }

    private <T> void plot(Map<String, List<Data<Number, Number>>> pointMap, ObservableList<Data<Number, Number>> pointData, ObservableList<Data<Number, Number>> selectedData,
            ObservableList<Data<Number, Number>> highlightedData, Series<Number, Number> modelCurveSeries, LineChart<Number, Number> fitPlot, final FrequencyBand selectedFrequency,
            Function<FrequencyBand, List<T>> valueSupplier, Function<T, Data<Number, Number>> dataPointSupplier, BiFunction<Data<Number, Number>, T, EventHandler<Event>> mouseClickedCallback,
            Supplier<Double> yLowBounds, Supplier<Double> yHighBounds, Function<Number, Data<Number, Number>> curveProducer, Function<T, Waveform> mapFunc) {
        if (selectedFrequency != null) {
            pointMap.clear();
            pointData.clear();
            selectedData.clear();
            highlightedData.clear();
            ObservableList<Data<Number, Number>> modelData = modelCurveSeries.getData();
            modelData.clear();

            AtomicReference<Double> minY = new AtomicReference<>(0d);
            AtomicReference<Double> maxY = new AtomicReference<>(0d);

            Optional.ofNullable(valueSupplier.apply(selectedFrequency)).ifPresent(values -> values.forEach(val -> {
                Data<Number, Number> data = dataPointSupplier.apply(val);
                if (data != null) {
                    pointData.add(data);
                    updateBounds(minX, maxX, minY, maxY, data);

                    data.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedCallback.apply(data, val));
                    data.extraValueProperty().addListener((obs, o, n) -> {
                        if (n instanceof Boolean && data != null && data.getNode() != null) {
                            if ((Boolean) n) {
                                data.getNode().setStyle("");
                            } else {
                                data.getNode().setStyle("-fx-background-color: white, gray;");
                            }
                        }
                    });

                    data.setExtraValue(mapFunc.apply(val).isActive());
                    Waveform w = mapFunc.apply(val);
                    boolean eventExists = w != null && w.getEvent() != null && w.getEvent().getEventId() != null;
                    boolean stationExists = w != null && w.getStream() != null && w.getStream().getStation() != null && w.getStream().getStation().getStationName() != null;
                    if (eventExists) {
                        pointMap.computeIfAbsent(w.getEvent().getEventId(), k -> new ArrayList<>()).add(data);
                    }
                    if (stationExists) {
                        pointMap.computeIfAbsent(w.getStream().getStation().getStationName(), k -> new ArrayList<>()).add(data);
                    }
                    if (eventExists && stationExists) {
                        pointMap.computeIfAbsent(w.getEvent().getEventId() + w.getStream().getStation().getStationName(), k -> new ArrayList<>()).add(data);
                    }
                }
            }));

            if (curveProducer != null) {
                Integer l = minX.get();
                Integer h = maxX.get();
                if (h - l <= 0) {
                    l = 0;
                    h = 2000;
                }
                for (double i = l; i <= h; i = i + ((h - l) / LINE_SEGMENTS)) {
                    Data<Number, Number> data = curveProducer.apply(i);
                    modelData.add(data);
                    Node node = data.getNode();
                    node.setMouseTransparent(true);
                    node.toFront();
                    updateBounds(minX, maxX, minY, maxY, data);
                }
            }
            modelCurveSeries.getNode().toFront();

            if (fitPlot != null && fitPlot.getYAxis() != null && fitPlot.getYAxis() instanceof NumberAxis) {
                NumberAxis yAxis = (NumberAxis) fitPlot.getYAxis();

                if (!shouldYAxisShrink) {
                    yAxis.setLowerBound(yLowBounds.get());
                    yAxis.setUpperBound(yHighBounds.get());
                    if (yAxis.getLowerBound() > minY.get()) {
                        yAxis.setLowerBound(minY.get());
                    }
                    if (yAxis.getUpperBound() < maxY.get()) {
                        yAxis.setUpperBound(maxY.get());
                    }
                } else {
                    double ly = minY.get();
                    double hy = maxY.get();
                    if (ly != 0) {
                        yAxis.setLowerBound(ly + (.1 * ly));
                    } else if (hy != 0) {
                        yAxis.setLowerBound(-(.1 * hy));
                    } else {
                        yAxis.setLowerBound(-1.0);
                    }

                    if (hy != 0) {
                        yAxis.setUpperBound(hy + (.1 * hy));
                    } else if (ly != 0) {
                        yAxis.setUpperBound(-(.1 * ly));
                    } else {
                        yAxis.setUpperBound(1.0);
                    }
                }
                yAxis.setTickUnit((yAxis.getUpperBound() - yAxis.getLowerBound()) / Y_TICK_LABEL_COUNT);
            }

        }
    }

    private void updateBounds(AtomicReference<Integer> minX, AtomicReference<Integer> maxX, AtomicReference<Double> minY, AtomicReference<Double> maxY, Data<Number, Number> data) {
        if (data.getXValue().doubleValue() > maxX.get()) {
            maxX.set(data.getXValue().intValue());
        }
        if (data.getXValue().doubleValue() < minX.get()) {
            minX.set(data.getXValue().intValue());
        }

        if (data.getYValue().doubleValue() > maxY.get()) {
            maxY.set(data.getYValue().doubleValue());
        }
        if (data.getYValue().doubleValue() < minY.get()) {
            minY.set(data.getYValue().doubleValue());
        }
    }

    private void showWaveformPopup(Waveform waveform) {
        showWaveformPopup(waveform, velHighlightedData, velPointMap);
        showWaveformPopup(waveform, betaHighlightedData, betaPointMap);
        showWaveformPopup(waveform, gammaHighlightedData, gammaPointMap);
    }

    private void showWaveformPopup(Waveform waveform, ObservableList<Data<Number, Number>> highlightedData, Map<String, List<Data<Number, Number>>> pointMap) {
        highlightedData.clear();
        List<Data<Number, Number>> selection = pointMap.get(waveform.getEvent().getEventId() + waveform.getStream().getStation().getStationName());
        if (selection != null && selection.size() == 1) {
            highlightedData.add(new Data<>(selection.get(0).getXValue(), selection.get(0).getYValue()));
            highlightedData.forEach(x -> x.getNode().setMouseTransparent(true));
        }
        bus.post(new WaveformSelectionEvent(waveform.getId()));
    }

    private void reloadData() {
        velocityDistancePairsFreqMap.clear();
        shapeDistancePairsFreqMap.clear();
        waveformIdMapping.clear();
        modelCurveMap.clear();
        frequencyBandCombo.getItems().clear();
        Map<Long, Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>>> shapeWaveformMapping = new HashMap<>();

        CompletableFuture<Void> f1 = CompletableFuture.runAsync(() -> {
            paramClient.getSharedFrequencyBandParameters().toStream().forEach(sfb -> modelCurveMap.put(new FrequencyBand(sfb.getLowFrequency(), sfb.getHighFrequency()), sfb));
        });

        CompletableFuture<Void> f2 = CompletableFuture.runAsync(() -> {
            velocityDistancePairsFreqMap.putAll(
                    velocityClient.getMeasuredPeakVelocitiesMetadata()
                                  .toStream()
                                  .filter(Objects::nonNull)
                                  .filter(pvm -> pvm.getWaveform() != null)
                                  .peek(pvm -> waveformIdMapping.computeIfAbsent(pvm.getWaveform().getId(), k -> getMapping(k)).getLeft().add(pvm))
                                  .collect(Collectors.groupingBy(pvm -> new FrequencyBand(pvm.getWaveform().getLowFrequency(), pvm.getWaveform().getHighFrequency()))));
        });

        CompletableFuture<Void> f3 = CompletableFuture.runAsync(() -> {
            shapeDistancePairsFreqMap.putAll(
                    shapeClient.getMeasuredShapesMetadata()
                               .toStream()
                               .filter(Objects::nonNull)
                               .filter(shape -> shape.getWaveform() != null)
                               .peek(pvm -> shapeWaveformMapping.computeIfAbsent(pvm.getWaveform().getId(), k -> getMapping(k)).getRight().add(pvm))
                               .collect(Collectors.groupingBy(shape -> new FrequencyBand(shape.getWaveform().getLowFrequency(), shape.getWaveform().getHighFrequency()))));
        });

        try {
            //TODO: One more plot in the pile that really could use a task indicator
            //Blocking here as a poor-mans progress bar so the user doesn't spam the refresh button (as much anyway)
            CompletableFuture.allOf(f1, f2, f3).get(100l, TimeUnit.SECONDS);
            for (Entry<Long, Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>>> e : shapeWaveformMapping.entrySet()) {
                waveformIdMapping.computeIfAbsent(e.getKey(), k -> getMapping(k)).getRight().addAll(e.getValue().getRight());
            }
            frequencyBandCombo.getItems().addAll(modelCurveMap.keySet());
            frequencyBandCombo.getSelectionModel().selectFirst();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    }

    private Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>> getMapping(Long key) {
        return new Pair<>(new ArrayList<PeakVelocityMeasurement>(), new ArrayList<ShapeMeasurement>());
    }

    @Override
    public void refreshView() {
        if (isVisible) {
            mapImpl.clearIcons();
            plotData();
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> reloadData();
    }

    @Override
    public void setVisible(boolean visible) {
        isVisible = visible;
    }
}
