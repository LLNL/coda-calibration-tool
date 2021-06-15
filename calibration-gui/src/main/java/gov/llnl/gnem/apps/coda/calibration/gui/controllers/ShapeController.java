/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
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
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.Type;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;

@Component
public class ShapeController implements MapListeningController, RefreshableController, ScreenshotEnabledController {

    private static final Logger log = LoggerFactory.getLogger(ShapeController.class);
    private static final double LINE_SEGMENTS = 250.0;
    private static final double VEL_Y_MIN = 0;
    private static final double VEL_Y_MAX = 6;
    private static final double BETA_Y_MIN = -0.12;
    private static final double BETA_Y_MAX = 0.01;
    private static final double GAMMA_Y_MIN = -1.0;
    private static final double GAMMA_Y_MAX = 3.5;
    private final AtomicReference<Double> minX = new AtomicReference<>(0.0);
    private final AtomicReference<Double> maxX = new AtomicReference<>(0.0);

    private final Map<FrequencyBand, List<PeakVelocityMeasurement>> velocityDistancePairsFreqMap = new HashMap<>();
    private final Map<FrequencyBand, List<ShapeMeasurement>> shapeDistancePairsFreqMap = new HashMap<>();
    private final Map<FrequencyBand, SharedFrequencyBandParameters> modelCurveMap = new TreeMap<>();
    private final Map<Long, Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>>> waveformIdMapping = new HashMap<>();
    private final EventStaFreqStringComparator eventStaFreqComparator = new EventStaFreqStringComparator();

    @FXML
    private StackPane shape;

    @FXML
    private ComboBox<FrequencyBand> frequencyBandCombo;

    @FXML
    private StackPane plotContainer;

    private BasicPlot fitPlot;
    private BasicPlot velFitPlot;
    private BasicPlot betaFitPlot;
    private BasicPlot gammaFitPlot;

    @FXML
    private Button yAxisShrink;

    private final ParameterClient paramClient;
    private final PeakVelocityClient velocityClient;
    private final ShapeMeasurementClient shapeClient;
    private final WaveformClient waveformClient;
    private final NumberFormat dfmt = NumberFormatFactory.twoDecimalOneLeadingZero();

    private final EventBus bus;
    private final GeoMap mapImpl;
    private final MapPlottingUtilities iconFactory;

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;
    private FrequencyBand selectedBand;
    private MenuItem exclude;
    private MenuItem include;
    private ContextMenu menu;
    private boolean shouldYAxisShrink = false;
    private boolean isVisible = false;
    private final PlotFactory plotFactory;

    private final Line velModelCurveSeries;
    private final List<Symbol> velPointData = new ArrayList<>(0);
    private final List<Symbol> velSelectedData = new ArrayList<>(0);
    private final List<Symbol> velHighlightedData = new ArrayList<>(0);

    private final Line betaModelCurveSeries;
    private final List<Symbol> betaPointData = new ArrayList<>(0);
    private final List<Symbol> betaSelectedData = new ArrayList<>(0);
    private final List<Symbol> betaHighlightedData = new ArrayList<>(0);

    private final Line gammaModelCurveSeries;
    private final List<Symbol> gammaPointData = new ArrayList<>(0);
    private final List<Symbol> gammaSelectedData = new ArrayList<>(0);
    private final List<Symbol> gammaHighlightedData = new ArrayList<>(0);

    private final Map<String, List<Symbol>> velPointMap = new HashMap<>(0);
    private final Map<String, List<Symbol>> betaPointMap = new HashMap<>(0);
    private final Map<String, List<Symbol>> gammaPointMap = new HashMap<>(0);
    private final Map<Point2D, BasicPlot> pointPlotMap = new HashMap<>(0);
    private final Map<Point2D, Symbol> pointSymbolMap = new HashMap<>();
    private final Map<Point2D, Waveform> pointWaveformMap = new ConcurrentHashMap<>();

    @Autowired
    private ShapeController(final ParameterClient paramClient, final PeakVelocityClient velocityClient, final ShapeMeasurementClient shapeClient, final WaveformClient waveformClient, final GeoMap map,
            final MapPlottingUtilities iconFactory, final PlotFactory plotFactory, final EventBus bus) {
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
        this.shapeClient = shapeClient;
        this.waveformClient = waveformClient;
        this.plotFactory = plotFactory;
        this.bus = bus;
        this.mapImpl = map;
        this.iconFactory = iconFactory;

        velModelCurveSeries = createModelLine(plotFactory);
        betaModelCurveSeries = createModelLine(plotFactory);
        gammaModelCurveSeries = createModelLine(plotFactory);

        eventSelectionCallback = (selected, eventId) -> selectDataByCriteria(bus, selected, eventId);

        stationSelectionCallback = (selected, stationId) -> selectDataByCriteria(bus, selected, stationId);

        this.bus.register(this);
    }

    private Line createModelLine(final PlotFactory plotFactory) {
        Line line = plotFactory.line(new double[0], new double[0], Color.BLUE, LineStyles.SOLID, 3);
        line.showInLegend(false);
        line.setLegendGrouping("Model");
        return line;
    }

    private void selectDataByCriteria(final EventBus bus, final Boolean selected, final String key) {
        selectDataByCriteria(bus, selected, Collections.singletonList(key));
    }

    private void selectDataByCriteria(final EventBus bus, final Boolean selected, final List<String> keys) {
        selectDataByCriteria(bus, selected, keys, velPointMap, velHighlightedData, velSelectedData);
        selectDataByCriteria(bus, selected, keys, betaPointMap, betaHighlightedData, betaSelectedData);
        selectDataByCriteria(bus, selected, keys, gammaPointMap, gammaHighlightedData, gammaSelectedData);
        fitPlot.replot();
    }

    private void selectDataByCriteria(final EventBus bus, final Boolean selected, final List<String> keys, final Map<String, List<Symbol>> pointMap, final List<Symbol> highlightedData,
            final List<Symbol> selectedData) {
        CompletableFuture.runAsync(() -> {
            resetSymbols(selectedData);
            selectedData.clear();
            List<Long> ids = new ArrayList<>();
            for (String key : keys) {
                final List<Symbol> selection = pointMap.get(key);
                if ((selection != null && !selection.isEmpty()) && Boolean.TRUE.equals(selected)) {
                    resetSymbols(highlightedData);
                    highlightedData.clear();
                    selectedData.addAll(selection);
                    selectedData.forEach(sym -> {
                        BasicPlot plot = pointPlotMap.get(new Point2D(sym.getX(), sym.getY()));
                        plot.removePlotObject(sym);
                        sym.setFillColor(Color.WHITE);
                        sym.setEdgeColor(Color.GRAY);
                        plot.addPlotObject(sym);
                    });

                    final List<PeakVelocityMeasurement> data = velocityDistancePairsFreqMap.get(selectedBand);
                    if (data != null && !data.isEmpty()) {
                        final List<Long> matchedIds = data.stream()
                                                          .sequential()
                                                          .filter(
                                                                  pv -> pv != null
                                                                          && pv.getWaveform() != null
                                                                          && ((eventExists(pv.getWaveform()) && pv.getWaveform().getEvent().getEventId().equalsIgnoreCase(key))
                                                                                  || (stationExists(pv.getWaveform())
                                                                                          && pv.getWaveform().getStream().getStation().getStationName().equalsIgnoreCase(key))))
                                                          .sorted((pv1, pv2) -> eventStaFreqComparator.compare(pv1.getWaveform(), pv2.getWaveform()))
                                                          .map(pv -> pv.getWaveform().getId())
                                                          .collect(Collectors.toList());
                        ids.addAll(matchedIds);
                    }
                }
            }
            if (!ids.isEmpty()) {
                bus.post(new WaveformSelectionEvent(ids.toArray(new Long[0])));
            }
        });
    }

    @FXML
    public void initialize() {
        final Label yAxisShrinkOn = new Label("><");
        yAxisShrinkOn.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        yAxisShrinkOn.setRotate(90.0);
        final Label yAxisShrinkOff = new Label("<>");
        yAxisShrinkOff.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        yAxisShrinkOff.setRotate(90.0);

        yAxisShrink.setGraphic(yAxisShrinkOn);
        final double topPad = yAxisShrink.getPadding().getTop();
        final double bottomPad = yAxisShrink.getPadding().getBottom();
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

        fitPlot = plotFactory.basicPlot();
        fitPlot.getTitle().setText("Shape Parameters");

        Axis xAxis = plotFactory.axis(Type.X, null);
        xAxis.setMin(minX.get());
        xAxis.setMax(maxX.get());
        velFitPlot = fitPlot.createSubPlot();

        Axis axis = plotFactory.axis(Type.Y, "Velocity");
        axis.setMin(VEL_Y_MIN);
        axis.setMax(VEL_Y_MAX);
        velFitPlot.addAxes(axis, xAxis);

        betaFitPlot = fitPlot.createSubPlot();
        axis = plotFactory.axis(Type.Y, "Beta");
        axis.setMin(BETA_Y_MIN);
        axis.setMax(BETA_Y_MAX);
        betaFitPlot.addAxes(axis, xAxis);

        gammaFitPlot = fitPlot.createSubPlot();
        axis = plotFactory.axis(Type.Y, "Gamma");
        axis.setMin(GAMMA_Y_MIN);
        axis.setMax(GAMMA_Y_MAX);
        gammaFitPlot.addAxes(axis);

        xAxis = plotFactory.axis(Type.X, "Distance");
        xAxis.setMin(minX.get());
        xAxis.setMax(maxX.get());
        gammaFitPlot.addAxes(xAxis);

        fitPlot.attachToDisplayNode(plotContainer);

        menu = new ContextMenu();
        include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);

        fitPlot.addPlotObjectObserver(this::handlePlotObjectClicked);
    }

    protected void handlePlotObjectClicked(final PropertyChangeEvent evt) {
        Object po = evt.getNewValue();
        if (po instanceof PlotObjectClick && ((PlotObjectClick) po).getPlotPoints() != null) {
            PlotObjectClick poc = (PlotObjectClick) po;
            List<Point2D> points = poc.getPlotPoints();
            List<Waveform> waveforms = new ArrayList<>();

            for (Point2D point : points) {
                final Waveform selection = pointWaveformMap.get(point);
                if (selection != null) {
                    waveforms.add(selection);
                }
            }
            if (poc.getMouseEvent().isPrimaryButtonDown()) {
                showWaveformPopup(waveforms.toArray(new Waveform[0]));
                Platform.runLater(() -> menu.hide());
            } else if (poc.getMouseEvent().isSecondaryButtonDown()) {
                showContextMenu(waveforms, poc.getMouseEvent());
            }
        }
    }

    private ListCell<FrequencyBand> getFBCell() {
        return new ListCell<FrequencyBand>() {
            @Override
            protected void updateItem(final FrequencyBand item, final boolean empty) {
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
        pointWaveformMap.clear();
        pointSymbolMap.clear();
        pointPlotMap.clear();
        minX.set(0.0);
        maxX.set(0.0);
        mapValues(
                Optional.ofNullable(velocityDistancePairsFreqMap.get(selectedFrequency))
                        .orElseGet(Collections::emptyList)
                        .stream()
                        .map(PeakVelocityMeasurement::getWaveform)
                        .collect(Collectors.toList()));
        plotVelocity(selectedFrequency);
        plotBeta(selectedFrequency);
        plotGamma(selectedFrequency);

        rescaleXAxis(velFitPlot);
        rescaleXAxis(betaFitPlot);
        rescaleXAxis(gammaFitPlot);

        fitPlot.replot();
    }

    private void rescaleXAxis(final BasicPlot plot) {
        plot.setAxisLimits(new AxisLimits(Axis.Type.X, minX.get(), maxX.get()));
    }

    private void plotVelocity(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            final UnaryOperator<Number> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getVelocity0() - (model.getVelocity1() / (model.getVelocity2() + ((double) i))));

            final Function<FrequencyBand, List<PeakVelocityMeasurement>> valueSupplier = createValueSupplier(velocityDistancePairsFreqMap);
            final Function<PeakVelocityMeasurement, Waveform> mapFunc = PeakVelocityMeasurement::getWaveform;
            plot(
                    velPointMap,
                        velPointData,
                        velSelectedData,
                        velHighlightedData,
                        velModelCurveSeries,
                        velFitPlot,
                        selectedFrequency,
                        valueSupplier,
                        val -> plotFactory.createSymbol(SymbolStyles.CIRCLE, "Velocity", val.getDistance(), val.getVelocity(), Color.RED, Color.BLACK, null, null, true),
                        () -> VEL_Y_MIN,
                        () -> VEL_Y_MAX,
                        curvePointProducer,
                        mapFunc);
        }
    }

    private void plotBeta(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            final UnaryOperator<Number> curvePointProducer = createCurvePointProducer(selectedFrequency, (model, i) -> model.getBeta0() - (model.getBeta1() / (model.getBeta2() + ((double) i))));

            final Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier = createValueSupplier(shapeDistancePairsFreqMap);
            final Function<ShapeMeasurement, Waveform> mapFunc = ShapeMeasurement::getWaveform;
            plot(
                    betaPointMap,
                        betaPointData,
                        betaSelectedData,
                        betaHighlightedData,
                        betaModelCurveSeries,
                        betaFitPlot,
                        selectedFrequency,
                        valueSupplier,
                        val -> plotFactory.createSymbol(SymbolStyles.CIRCLE, "Beta", val.getDistance(), val.getMeasuredBeta(), Color.RED, Color.BLACK, null, null, true),
                        () -> BETA_Y_MIN,
                        () -> BETA_Y_MAX,
                        curvePointProducer,
                        mapFunc);
        }
    }

    private void plotGamma(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            final UnaryOperator<Number> curvePointProducer = createCurvePointProducer(selectedFrequency, (model, i) -> model.getGamma0() - (model.getGamma1() / (model.getGamma2() + ((double) i))));

            final Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier = createValueSupplier(shapeDistancePairsFreqMap);
            final Function<ShapeMeasurement, Waveform> mapFunc = ShapeMeasurement::getWaveform;
            plot(
                    gammaPointMap,
                        gammaPointData,
                        gammaSelectedData,
                        gammaHighlightedData,
                        gammaModelCurveSeries,
                        gammaFitPlot,
                        selectedFrequency,
                        valueSupplier,
                        val -> plotFactory.createSymbol(SymbolStyles.CIRCLE, "Gamma", val.getDistance(), val.getMeasuredGamma(), Color.RED, Color.BLACK, null, null, true),
                        () -> GAMMA_Y_MIN,
                        () -> GAMMA_Y_MAX,
                        curvePointProducer,
                        mapFunc);
        }
    }

    private void showContextMenu(final List<Waveform> waveforms, final MouseEvent t) {
        include.setOnAction(evt -> includeWaveforms(waveforms));
        exclude.setOnAction(evt -> excludeWaveforms(waveforms));
        menu.show(shape, t.getScreenX(), t.getScreenY());
    }

    private void excludeWaveforms(final List<Waveform> waveforms) {
        setActive(waveforms, false);
    }

    private void includeWaveforms(final List<Waveform> waveforms) {
        setActive(waveforms, true);
    }

    private void setActive(final List<Waveform> waveforms, final boolean active) {
        if (waveformClient != null && waveforms != null && !waveforms.isEmpty()) {
            waveformClient.setWaveformsActiveByIds(waveforms.stream().map(Waveform::getId).collect(Collectors.toList()), active).subscribe(s -> {
                for (Waveform waveform : waveforms) {
                    final Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>> pair = waveformIdMapping.get(waveform.getId());
                    if (pair != null) {
                        pair.getLeft().forEach(v -> v.getWaveform().setActive(active));
                        pair.getRight().forEach(v -> v.getWaveform().setActive(active));
                    }
                }
                Platform.runLater(this::refreshView);
            });
        }
    }

    @Subscribe
    private void listener(final WaveformChangeEvent wce) {
        final List<Long> nonNull = wce.getIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
        synchronized (waveformIdMapping) {
            if (wce.isAddOrUpdate()) {
                final List<Waveform> results = waveformClient.getWaveformMetadataFromIds(nonNull).collect(Collectors.toList()).block(Duration.ofSeconds(10l));
                if (results != null) {
                    results.forEach(md -> {
                        final Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>> pair = waveformIdMapping.get(md.getId());
                        if (pair != null) {
                            pair.getLeft().forEach(v -> v.getWaveform().setActive(md.isActive()));
                            pair.getRight().forEach(v -> v.getWaveform().setActive(md.isActive()));
                        }
                    });
                }
                Platform.runLater(this::refreshView);
            }
        }
    }

    private void mapValues(final List<Waveform> waveforms) {
        mapImpl.addIcons(iconFactory.genIconsFromWaveforms(eventSelectionCallback, stationSelectionCallback, waveforms));
    }

    private <T> Function<FrequencyBand, List<T>> createValueSupplier(final Map<FrequencyBand, List<T>> valueMap) {
        Function<FrequencyBand, List<T>> valueSupplier;
        if (valueMap != null) {
            valueSupplier = freq -> Optional.ofNullable(valueMap.get(freq)).orElseGet(() -> new ArrayList<>(0));
        } else {
            valueSupplier = freq -> new ArrayList<>(0);
        }
        return valueSupplier;
    }

    private UnaryOperator<Number> createCurvePointProducer(final FrequencyBand selectedFrequency, final BiFunction<SharedFrequencyBandParameters, Number, Number> curvePointFunc) {
        UnaryOperator<Number> curvePointProducer = null;
        if (selectedFrequency != null && modelCurveMap.containsKey(selectedFrequency)) {
            final SharedFrequencyBandParameters model = modelCurveMap.get(selectedFrequency);
            curvePointProducer = i -> curvePointFunc.apply(model, i);
        }
        return curvePointProducer;
    }

    private <T> void plot(final Map<String, List<Symbol>> pointMap, List<Symbol> pointData, List<Symbol> selectedData, List<Symbol> highlightedData, final Line modelCurveSeries,
            final BasicPlot fitPlot, final FrequencyBand selectedFrequency, final Function<FrequencyBand, List<T>> valueSupplier, final Function<T, Symbol> dataPointSupplier,
            final DoubleSupplier yLowBounds, final DoubleSupplier yHighBounds, final UnaryOperator<Number> curveProducer, final Function<T, Waveform> mapFunc) {
        if (selectedFrequency != null && fitPlot != null) {
            fitPlot.clear();
            pointMap.clear();
            pointData.clear();
            selectedData.clear();
            highlightedData.clear();
            final AtomicReference<Double> minY = new AtomicReference<>(0d);
            final AtomicReference<Double> maxY = new AtomicReference<>(0d);

            Optional.ofNullable(valueSupplier.apply(selectedFrequency)).ifPresent(values -> values.forEach(val -> {
                final Symbol data = dataPointSupplier.apply(val);
                if (data != null) {
                    pointData.add(data);
                    updateBounds(minX, maxX, minY, maxY, data.getX(), data.getY());

                    final Waveform w = mapFunc.apply(val);
                    final boolean eventExists = eventExists(w);
                    final boolean stationExists = stationExists(w);
                    if (eventExists) {
                        pointMap.computeIfAbsent(w.getEvent().getEventId(), k -> new ArrayList<>()).add(data);
                    }
                    if (stationExists) {
                        pointMap.computeIfAbsent(w.getStream().getStation().getStationName(), k -> new ArrayList<>()).add(data);
                    }
                    if (eventExists && stationExists) {
                        pointMap.computeIfAbsent(w.getEvent().getEventId() + w.getStream().getStation().getStationName(), k -> new ArrayList<>()).add(data);
                    }
                    pointWaveformMap.put(new Point2D(data.getX(), data.getY()), w);
                }
            }));

            List<Double> modelX = new ArrayList<>();
            List<Double> modelY = new ArrayList<>();

            if (curveProducer != null) {
                Double l = minX.get();
                Double h = maxX.get();
                if (h - l <= 0.0) {
                    l = 0.0;
                    h = 2000.0;
                }
                for (double i = l; i <= h; i = i + ((h - l) / LINE_SEGMENTS)) {
                    final Double yVal = curveProducer.apply(i).doubleValue();
                    modelY.add(yVal);
                    modelX.add(i);
                }
            }

            modelCurveSeries.setX(ArrayUtils.toPrimitive(modelX.toArray(new Double[0])));
            modelCurveSeries.setY(ArrayUtils.toPrimitive(modelY.toArray(new Double[0])));
            fitPlot.addPlotObject(modelCurveSeries);

            pointData.forEach(sym -> {
                fitPlot.addPlotObject(sym);
                pointSymbolMap.put(new Point2D(sym.getX(), sym.getY()), sym);
                pointPlotMap.put(new Point2D(sym.getX(), sym.getY()), fitPlot);
            });

            final AxisLimits yAxis = new AxisLimits(Axis.Type.Y, 0.0, 0.0);
            if (!shouldYAxisShrink) {
                yAxis.setMin(yLowBounds.getAsDouble());
                yAxis.setMax(yHighBounds.getAsDouble());
                if (yAxis.getMin() > minY.get()) {
                    yAxis.setMin(minY.get());
                }
                if (yAxis.getMax() < maxY.get()) {
                    yAxis.setMax(maxY.get());
                }
            } else {
                final double ly = minY.get();
                final double hy = maxY.get();
                if (ly != 0) {
                    yAxis.setMin(ly + (.1 * ly));
                } else if (hy != 0) {
                    yAxis.setMin(-(.1 * hy));
                } else {
                    yAxis.setMin(-1.0);
                }

                if (hy != 0) {
                    yAxis.setMax(hy + (.1 * hy));
                } else if (ly != 0) {
                    yAxis.setMax(-(.1 * ly));
                } else {
                    yAxis.setMax(1.0);
                }
            }
            fitPlot.setAxisLimits(yAxis);
        }
    }

    private boolean stationExists(final Waveform w) {
        return w != null && w.getStream() != null && w.getStream().getStation() != null && w.getStream().getStation().getStationName() != null;
    }

    private boolean eventExists(final Waveform w) {
        return w != null && w.getEvent() != null && w.getEvent().getEventId() != null;
    }

    private void updateBounds(final AtomicReference<Double> minX, final AtomicReference<Double> maxX, final AtomicReference<Double> minY, final AtomicReference<Double> maxY, final double x,
            final double y) {
        if (x > maxX.get()) {
            maxX.set(x + Math.abs(x * .1));
        }
        if (x < minX.get()) {
            minX.set(x - Math.abs(x * .1));
        }

        if (y > maxY.get()) {
            maxY.set(y + Math.abs(y * .1));
        }
        if (y < minY.get()) {
            minY.set(y - Math.abs(y * .1));
        }
    }

    private void showWaveformPopup(final Waveform... waveforms) {
        showWaveformPopup(velHighlightedData, velPointMap, waveforms);
        showWaveformPopup(betaHighlightedData, betaPointMap, waveforms);
        showWaveformPopup(gammaHighlightedData, gammaPointMap, waveforms);
        fitPlot.replot();
    }

    private void showWaveformPopup(final List<Symbol> highlightedData, final Map<String, List<Symbol>> pointMap, final Waveform... waveforms) {
        resetSymbols(highlightedData);
        highlightedData.clear();
        List<Long> waveformIds = new ArrayList<>();
        for (Waveform waveform : waveforms) {
            final List<Symbol> selection = pointMap.get(waveform.getEvent().getEventId() + waveform.getStream().getStation().getStationName());
            if (selection != null && selection.size() == 1) {
                selection.forEach(sym -> {
                    BasicPlot plot = pointPlotMap.get(new Point2D(sym.getX(), sym.getY()));
                    plot.removePlotObject(sym);
                    sym.setFillColor(Color.BLACK);
                    sym.setEdgeColor(Color.BLACK);
                    plot.addPlotObject(sym);
                    highlightedData.add(sym);
                });
                waveformIds.add(waveform.getId());
            }
        }
        if (!waveformIds.isEmpty()) {
            bus.post(new WaveformSelectionEvent(waveformIds.toArray(new Long[0])));
        }
    }

    private void resetSymbols(List<Symbol> symbols) {
        symbols.forEach(sym -> {
            BasicPlot plot = pointPlotMap.get(new Point2D(sym.getX(), sym.getY()));
            plot.removePlotObject(sym);
            sym.setFillColor(Color.RED);
            sym.setEdgeColor(Color.BLACK);
            plot.addPlotObject(sym);
        });
    }

    private void reloadData() {
        velocityDistancePairsFreqMap.clear();
        shapeDistancePairsFreqMap.clear();
        waveformIdMapping.clear();
        modelCurveMap.clear();
        frequencyBandCombo.getItems().clear();
        final Map<Long, Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>>> shapeWaveformMapping = new HashMap<>();

        final CompletableFuture<Void> f1 = CompletableFuture.runAsync(
                () -> paramClient.getSharedFrequencyBandParameters().toStream().forEach(sfb -> modelCurveMap.put(new FrequencyBand(sfb.getLowFrequency(), sfb.getHighFrequency()), sfb)));

        final CompletableFuture<Void> f2 = CompletableFuture.runAsync(
                () -> velocityDistancePairsFreqMap.putAll(
                        velocityClient.getMeasuredPeakVelocitiesMetadata()
                                      .toStream()
                                      .filter(Objects::nonNull)
                                      .filter(pvm -> pvm.getWaveform() != null)
                                      .filter(pvm -> waveformIdMapping.computeIfAbsent(pvm.getWaveform().getId(), this::getMapping).getLeft().add(pvm))
                                      .collect(Collectors.groupingBy(pvm -> new FrequencyBand(pvm.getWaveform().getLowFrequency(), pvm.getWaveform().getHighFrequency())))));

        final CompletableFuture<Void> f3 = CompletableFuture.runAsync(
                () -> shapeDistancePairsFreqMap.putAll(
                        shapeClient.getMeasuredShapesMetadata()
                                   .toStream()
                                   .filter(Objects::nonNull)
                                   .filter(meas -> meas.getWaveform() != null)
                                   .filter(pvm -> shapeWaveformMapping.computeIfAbsent(pvm.getWaveform().getId(), this::getMapping).getRight().add(pvm))
                                   .collect(Collectors.groupingBy(meas -> new FrequencyBand(meas.getWaveform().getLowFrequency(), meas.getWaveform().getHighFrequency())))));

        try {
            //Blocking here as a poor-mans progress bar so the user doesn't spam the refresh button (as much anyway)
            CompletableFuture.allOf(f1, f2, f3).get(100l, TimeUnit.SECONDS);
            for (final Entry<Long, Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>>> e : shapeWaveformMapping.entrySet()) {
                waveformIdMapping.computeIfAbsent(e.getKey(), this::getMapping).getRight().addAll(e.getValue().getRight());
            }
            frequencyBandCombo.getItems().addAll(modelCurveMap.keySet());
            frequencyBandCombo.getSelectionModel().selectFirst();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
        }
    }

    private Pair<List<PeakVelocityMeasurement>, List<ShapeMeasurement>> getMapping(final Long key) {
        return new Pair<>(new ArrayList<>(), new ArrayList<>());
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
        return this::reloadData;
    }

    @Override
    public void setVisible(final boolean visible) {
        isVisible = visible;
    }

    @Override
    public Consumer<File> getScreenshotFunction() {
        return folder -> {
            final String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
            SnapshotUtils.writePng(folder, new Pair<>("Shape_", shape), timestamp);
            if (frequencyBandCombo.getButtonCell() != null && frequencyBandCombo.getButtonCell().getText() != null) {
                try {
                    Files.write(Paths.get(folder + File.separator + "Shape_" + frequencyBandCombo.getButtonCell().getText() + "_" + timestamp + ".svg"), fitPlot.getSVG().getBytes());
                } catch (final IOException e) {
                    log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
                }
            }
        };
    }
}
