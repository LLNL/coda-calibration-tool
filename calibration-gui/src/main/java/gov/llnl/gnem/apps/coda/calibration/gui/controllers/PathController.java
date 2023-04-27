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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;
import llnl.gnem.core.util.Geometry.EModel;

@Component
public class PathController implements MapListeningController, RefreshableController, ScreenshotEnabledController {

    private static final Integer AFTER_Z_INDEX = 1;
    private static final Integer BEFORE_Z_INDEX = 0;

    private static final Logger log = LoggerFactory.getLogger(PathController.class);

    private final Map<FrequencyBand, List<SpectraMeasurement>> measurementsFreqBandMap = new TreeMap<>();
    private final Map<Long, List<SpectraMeasurement>> measurementsWaveformIdMap = new HashMap<>();

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    @FXML
    private ComboBox<FrequencyBand> frequencyBandComboBox;

    @FXML
    private ComboBox<Station> station1ComboBox;

    @FXML
    private ComboBox<Station> station2ComboBox;

    @FXML
    private Pane stationPlotPane;

    @FXML
    private Pane sdPlotPane;

    @FXML
    private Spinner<Integer> stationCountFilterField;

    private BasicPlot sdPlot;
    private BasicPlot stationPlot;
    private Axis stationXaxis;
    private Axis stationYaxis;

    @FXML
    private Pane path;

    private final SpectraClient spectraMeasurementClient;
    private final WaveformClient waveformClient;

    private final GeoMap mapImpl;

    private final MapPlottingUtilities mappingUtilities;

    private final ObservableSet<Station> stations = FXCollections.synchronizedObservableSet(
            FXCollections.observableSet(new TreeSet<>((lhs, rhs) -> lhs.getStationName().compareTo(rhs.getStationName()))));

    private final Map<Point2D, List<Waveform>> sdSymbolMap = new HashMap<>();
    private final Map<Point2D, List<Waveform>> stationSymbolMap = new HashMap<>();
    private final Map<String, List<Symbol>> stationWaveformMap = new HashMap<>();

    private final EventBus bus;
    private final Comparator<? super Waveform> evStaComparator = new EventStaFreqStringComparator();

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;
    private final List<Symbol> selectedSymbols = new ArrayList<>();
    private MenuItem exclude;
    private MenuItem include;
    private ContextMenu menu;
    private boolean isVisible = false;

    private final PlotFactory plotFactory;

    @Autowired
    public PathController(final SpectraClient spectraMeasurementClient, final WaveformClient waveformClient, final EventBus bus, final GeoMap mapImpl, final MapPlottingUtilities mappingUtilities,
            final PlotFactory plotFactory) {
        this.spectraMeasurementClient = spectraMeasurementClient;
        this.waveformClient = waveformClient;
        this.mapImpl = mapImpl;
        this.mappingUtilities = mappingUtilities;
        this.bus = bus;
        this.plotFactory = plotFactory;

        eventSelectionCallback = this::selectDataByCriteria;
        stationSelectionCallback = this::selectDataByCriteria;

        this.bus.register(this);
    }

    private void selectDataByCriteria(final Boolean selected, final String key) {
        if (Boolean.TRUE.equals(selected)) {
            final List<Symbol> stationSymbols = stationWaveformMap.get(key);
            if (!selectedSymbols.isEmpty()) {
                deselectSymbols(selectedSymbols);
            }
            selectSymbols(stationSymbols);
        } else {
            final List<Symbol> stationSymbols = stationWaveformMap.get(key);
            deselectSymbols(stationSymbols);
        }
    }

    private void selectSymbols(final List<Symbol> stationSymbols) {
        if (stationSymbols != null && !stationSymbols.isEmpty()) {
            selectedSymbols.addAll(stationSymbols);
            stationSymbols.forEach(po -> {
                stationPlot.removePlotObject(po);
                po.setZindex(1000);
                po.setFillColor(Color.YELLOW);
                stationPlot.addPlotObject(po);
            });
            stationPlot.replot();
        }
    }

    private void deselectSymbols(final List<Symbol> stationSymbols) {
        if (stationSymbols != null && !stationSymbols.isEmpty()) {
            final List<Symbol> symbols = new ArrayList<>(stationSymbols);
            symbols.forEach(po -> {
                stationPlot.removePlotObject(po);
                po.setZindex(null);
                if (SymbolStyles.TRIANGLE_UP.equals(po.getStyle())) {
                    po.setFillColor(Color.RED);
                } else if (SymbolStyles.TRIANGLE_DOWN.equals(po.getStyle())) {
                    po.setFillColor(Color.BLUE);
                }
                stationPlot.addPlotObject(po);
            });
            stationPlot.replot();
        }
    }

    @FXML
    public void initialize() {
        stationPlot = plotFactory.basicPlot();
        stationXaxis = plotFactory.axis(Axis.Type.X, "");
        stationYaxis = plotFactory.axis(Axis.Type.Y, "");
        stationPlot.addAxes(stationXaxis, stationYaxis);
        stationCountFilterField.setTooltip(new Tooltip("Minimum number of common stations required to be included in the overall plot"));
        stationCountFilterField.valueProperty().addListener((o, n, v) -> {
            plotSd();
            sdPlot.replot();
        });

        stationPlot.addPlotObjectObserver(evt -> {
            Object po = evt.getNewValue();
            if (po instanceof PlotObjectClick && ((PlotObjectClick) po).getPlotPoints() != null) {
                List<Waveform> waveforms = new ArrayList<>();
                for (Point2D point : ((PlotObjectClick) po).getPlotPoints()) {
                    List<Waveform> mappedWaveforms = stationSymbolMap.get(point);
                    if (mappedWaveforms != null) {
                        waveforms.addAll(mappedWaveforms);
                    }
                }
                handlePlotObjectClicked((PlotObjectClick) po, waveforms);
            }
        });

        sdPlot = plotFactory.basicPlot();
        sdPlot.addPlotObjectObserver(evt -> {
            Object po = evt.getNewValue();
            if (po instanceof PlotObjectClick && ((PlotObjectClick) po).getPlotPoints() != null) {
                List<Waveform> waveforms = new ArrayList<>();
                for (Point2D point : ((PlotObjectClick) po).getPlotPoints()) {
                    List<Waveform> mappedWaveforms = sdSymbolMap.get(point);
                    if (mappedWaveforms != null) {
                        waveforms.addAll(mappedWaveforms);
                    }
                }
                handlePlotObjectClicked((PlotObjectClick) po, waveforms);
            }
        });

        sdPlot.addAxes(plotFactory.axis(Axis.Type.X, "Inter-Station Distance (km)"), plotFactory.axis(Axis.Type.Y, "σ(|deviation|)"));

        sdPlot.setSymbolSize(8);
        sdPlot.getTitle().setFontSize(16);
        sdPlot.setMargin(30, 40, 50, null);

        stationPlot.setSymbolSize(10);
        stationPlot.getTitle().setFontSize(16);
        stationPlot.setMargin(30, 40, 50, null);

        stationPlot.attachToDisplayNode(stationPlotPane);
        sdPlot.attachToDisplayNode(sdPlotPane);

        frequencyBandComboBox.setCellFactory(fb -> getFBCell());
        frequencyBandComboBox.setButtonCell(getFBCell());
        frequencyBandComboBox.valueProperty().addListener(e -> {
            refreshView();
        });

        station1ComboBox.setCellFactory(station -> getStationCell());
        station1ComboBox.setButtonCell(getStationCell());

        station2ComboBox.setCellFactory(station -> getStationCell());
        station2ComboBox.setButtonCell(getStationCell());

        station1ComboBox.valueProperty().addListener(e -> refreshView());
        station2ComboBox.valueProperty().addListener(e -> refreshView());

        menu = new ContextMenu();
        include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);

        final EventHandler<javafx.scene.input.MouseEvent> menuHideHandler = evt -> {
            if (MouseButton.SECONDARY != evt.getButton()) {
                menu.hide();
            }
        };
        path.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
        sdPlotPane.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
        stationPlotPane.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
    }

    private void setSymbolsActive(final List<Waveform> ws, final Boolean active) {
        ws.stream().flatMap(w -> Optional.ofNullable(stationWaveformMap.get(w.getEvent().getEventId())).orElseGet(ArrayList::new).stream()).forEach(sym -> {
            if (Boolean.TRUE.equals(active)) {
                sym.setFillColor(sym.getEdgeColor());
            } else {
                sym.setFillColor(Color.GRAY);
            }
        });
    }

    @Subscribe
    private void listener(final WaveformChangeEvent wce) {
        final List<Long> nonNull = wce.getIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
        synchronized (stationWaveformMap) {
            if (wce.isAddOrUpdate()) {
                final List<Waveform> metadata = waveformClient.getWaveformMetadataFromIds(nonNull).collect(Collectors.toList()).block(Duration.ofSeconds(10l));
                if (metadata != null) {
                    metadata.forEach(w -> {
                        final List<SpectraMeasurement> measurements = measurementsWaveformIdMap.get(w.getId());
                        if (measurements != null) {
                            measurements.forEach(m -> m.getWaveform().setActive(w.getActive()));
                        }
                        if (w != null && w.getEvent() != null && w.getEvent().getEventId() != null) {
                            Optional.ofNullable(stationWaveformMap.get(w.getEvent().getEventId())).orElseGet(ArrayList::new).stream().forEach(sym -> {
                                if (Boolean.TRUE.equals(w.isActive())) {
                                    sym.setFillColor(sym.getEdgeColor());
                                } else {
                                    sym.setFillColor(Color.GRAY);
                                }
                            });
                        }
                    });
                    refreshView();
                }
            }
        }
    }

    private void showContextMenu(final List<Waveform> waveforms, final MouseEvent t, final BiConsumer<List<Waveform>, Boolean> activationFunc) {
        Platform.runLater(() -> {
            include.setOnAction(evt -> setActive(waveforms, true, activationFunc));
            exclude.setOnAction(evt -> setActive(waveforms, false, activationFunc));
            menu.show(path, t.getScreenX(), t.getScreenY());
        });
    }

    private void setActive(final List<Waveform> waveforms, final boolean active, final BiConsumer<List<Waveform>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().map(Waveform::getId).collect(Collectors.toList()), active).subscribe(s -> activationFunc.accept(waveforms, active));
    }

    private void selectSymbolsForWaveforms(final Collection<Waveform> sortedSet) {
        if (!selectedSymbols.isEmpty()) {
            deselectSymbols(selectedSymbols);
            selectedSymbols.clear();
        }
        sortedSet.forEach(w -> {
            selectSymbols(stationWaveformMap.get(w.getEvent().getEventId()));
        });
    }

    private void selectWaveforms(final Long... waveformIds) {
        bus.post(new WaveformSelectionEvent(waveformIds));
    }

    private ListCell<Station> getStationCell() {
        return new ListCell<Station>() {
            @Override
            protected void updateItem(final Station item, final boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(item.getStationName());
                }
            }
        };
    }

    private ListCell<FrequencyBand> getFBCell() {
        return new ListCell<FrequencyBand>() {
            @Override
            protected void updateItem(final FrequencyBand item, final boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(dfmt2.format(item.getLowFrequency()) + "-" + dfmt2.format(item.getHighFrequency()));
                }
            }
        };
    }

    private void reloadData() {
        measurementsFreqBandMap.clear();
        measurementsWaveformIdMap.clear();
        stations.clear();
        station1ComboBox.getItems().clear();
        station2ComboBox.getItems().clear();

        frequencyBandComboBox.getItems().clear();

        measurementsFreqBandMap.putAll(
                spectraMeasurementClient.getMeasuredSpectraMetadata()
                                        .filter(Objects::nonNull)
                                        .filter(spectra -> spectra.getWaveform() != null)
                                        .toStream()
                                        .collect(Collectors.groupingBy(spectra -> new FrequencyBand(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency()))));

        measurementsWaveformIdMap.putAll(measurementsFreqBandMap.values().parallelStream().flatMap(List::parallelStream).collect(Collectors.groupingBy(s -> s.getWaveform().getId())));

        stations.addAll(measurementsFreqBandMap.values().parallelStream().flatMap(List::parallelStream).map(spectra -> spectra.getWaveform().getStream().getStation()).collect(Collectors.toList()));

        frequencyBandComboBox.getItems().addAll(measurementsFreqBandMap.keySet());
        frequencyBandComboBox.getSelectionModel().selectFirst();

        station1ComboBox.getItems().addAll(stations);
        station1ComboBox.getSelectionModel().selectFirst();

        station2ComboBox.getItems().addAll(stations);
        station2ComboBox.getSelectionModel().selectFirst();

        refreshView();
    }

    @Override
    public void refreshView() {
        if (isVisible) {
            selectedSymbols.clear();
            plotPaths();
            plotBeforeAfter();
            plotSd();

            sdPlot.replot();
            stationPlot.replot();
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return this::reloadData;
    }

    @Override
    public Consumer<File> getScreenshotFunction() {
        return folder -> {
            final String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
            SnapshotUtils.writePng(folder, new Pair<>("Path", path), timestamp);
            if (frequencyBandComboBox.getButtonCell() != null && frequencyBandComboBox.getButtonCell().getText() != null) {
                try {
                    Files.write(Paths.get(folder + File.separator + "Path_SD_" + frequencyBandComboBox.getButtonCell().getText() + timestamp + ".svg"), sdPlot.getSVG().getBytes());
                } catch (final IOException e) {
                    log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
                }
                if (station1ComboBox.getButtonCell() != null
                        && station1ComboBox.getButtonCell().getText() != null
                        && station2ComboBox.getButtonCell() != null
                        && station2ComboBox.getButtonCell().getText() != null) {
                    try {
                        Files.write(
                                Paths.get(
                                        folder
                                                + File.separator
                                                + "Path_"
                                                + frequencyBandComboBox.getButtonCell().getText()
                                                + "_"
                                                + station1ComboBox.getButtonCell().getText()
                                                + "_"
                                                + station2ComboBox.getButtonCell().getText()
                                                + timestamp
                                                + ".svg"),
                                    stationPlot.getSVG().getBytes());
                    } catch (final IOException e) {
                        log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
                    }
                }
            }
        };
    }

    private void plotPaths() {
        mapImpl.clearIcons();
        if (!measurementsFreqBandMap.isEmpty() && (frequencyBandComboBox.getSelectionModel() != null && frequencyBandComboBox.getSelectionModel().getSelectedItem() != null)) {
            final List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());
            if (measurements != null) {
                final Map<Station, List<Event>> stationToEvents = measurements.parallelStream()
                                                                              .filter(meas -> meas.getWaveform() != null && meas.getWaveform().getStream() != null)
                                                                              .map(SpectraMeasurement::getWaveform)
                                                                              .filter(Objects::nonNull)
                                                                              .distinct()
                                                                              .collect(
                                                                                      Collectors.groupingBy(
                                                                                              w -> w.getStream().getStation(),
                                                                                                  HashMap::new,
                                                                                                  Collectors.mapping(Waveform::getEvent, Collectors.toList())));

                stationToEvents.entrySet().stream().flatMap(entry -> {
                    final Station station = entry.getKey();
                    return entry.getValue().stream().map(event -> mappingUtilities.createStationToEventLine(station, event));
                }).forEach(mapImpl::addShape);

                mapImpl.addIcons(
                        stationToEvents.keySet()
                                       .stream()
                                       .filter(
                                               station -> station.equals(station1ComboBox.getSelectionModel().getSelectedItem())
                                                       || station.equals(station2ComboBox.getSelectionModel().getSelectedItem()))
                                       .distinct()
                                       .map(mappingUtilities::createStationIconForeground)
                                       .collect(Collectors.toList()));

                mapImpl.addIcons(
                        mappingUtilities.genIconsFromWaveforms(
                                eventSelectionCallback,
                                    stationSelectionCallback,
                                    measurements.stream().map(SpectraMeasurement::getWaveform).collect(Collectors.toList())));
            }
        }
    }

    private void plotSd() {
        if (frequencyBandComboBox.getSelectionModel() != null && frequencyBandComboBox.getSelectionModel().getSelectedItem() != null && !measurementsFreqBandMap.isEmpty()) {
            sdSymbolMap.clear();
            sdPlot.clear();
            Double xmin = null;
            Double xmax = null;
            Double ymin = null;
            Double ymax = null;

            final Map<Pair<Station, Station>, DescriptiveStatistics> beforeStatsStaPairs = new HashMap<>();
            final Map<Pair<Station, Station>, DescriptiveStatistics> afterStatsStaPairs = new HashMap<>();
            final Map<Pair<Station, Station>, Double> distanceStaPairs = new HashMap<>();

            final List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());

            final Map<Pair<Station, Station>, List<Waveform>> sourceMeasurements = new HashMap<>();
            final Map<Pair<Station, Station>, Set<String>> used = new HashMap<>();

            if (measurements != null) {
                final DescriptiveStatistics overallBeforeStats = new DescriptiveStatistics();
                final DescriptiveStatistics overallAfterStats = new DescriptiveStatistics();

                for (final SpectraMeasurement firstMeasurement : measurements) {
                    for (final SpectraMeasurement secondMeasurement : measurements) {
                        if (!firstMeasurement.equals(secondMeasurement)
                                && firstMeasurement.getWaveform().getEvent().getEventId() != null
                                && firstMeasurement.getWaveform().getEvent().equals(secondMeasurement.getWaveform().getEvent())) {
                            final Station firstStation = firstMeasurement.getWaveform().getStream().getStation();
                            final Station secondStation = secondMeasurement.getWaveform().getStream().getStation();
                            if (!firstStation.equals(secondStation)) {
                                final Pair<Station, Station> staPair = new Pair<>(firstStation, secondStation);
                                final Pair<Station, Station> staPair2 = new Pair<>(secondStation, firstStation);
                                DescriptiveStatistics beforeStats = new DescriptiveStatistics();
                                DescriptiveStatistics afterStats = new DescriptiveStatistics();

                                if (!used.containsKey(staPair)) {
                                    used.put(staPair, new HashSet<>());
                                    used.put(staPair2, used.get(staPair));
                                    beforeStatsStaPairs.put(staPair, beforeStats);
                                    afterStatsStaPairs.put(staPair, afterStats);
                                    beforeStatsStaPairs.put(staPair2, beforeStats);
                                    afterStatsStaPairs.put(staPair2, afterStats);
                                    distanceStaPairs.put(
                                            staPair,
                                                EModel.getDistanceWGS84(firstStation.getLatitude(), firstStation.getLongitude(), secondStation.getLatitude(), secondStation.getLongitude()));
                                    distanceStaPairs.put(staPair2, distanceStaPairs.get(staPair));
                                    sourceMeasurements.put(staPair, new ArrayList<>());
                                    sourceMeasurements.put(staPair2, sourceMeasurements.get(staPair));
                                }

                                if (!used.get(staPair).contains(firstMeasurement.getWaveform().getEvent().getEventId())
                                        && !used.get(staPair2).contains(firstMeasurement.getWaveform().getEvent().getEventId())) {
                                    used.get(staPair).add(firstMeasurement.getWaveform().getEvent().getEventId());
                                    beforeStats = beforeStatsStaPairs.get(staPair);
                                    afterStats = afterStatsStaPairs.get(staPair);

                                    final double before = Math.abs(firstMeasurement.getRawAtMeasurementTime() - secondMeasurement.getRawAtMeasurementTime());
                                    final double after = Math.abs(firstMeasurement.getPathCorrected() - secondMeasurement.getPathCorrected());

                                    beforeStats.addValue(before);
                                    afterStats.addValue(after);

                                    overallBeforeStats.addValue(before);
                                    overallAfterStats.addValue(after);

                                    sourceMeasurements.get(staPair).add(firstMeasurement.getWaveform());
                                    sourceMeasurements.get(staPair).add(secondMeasurement.getWaveform());
                                }
                            }
                        }
                    }
                }

                try {
                    for (final Entry<Pair<Station, Station>, Double> distanceStaPair : distanceStaPairs.entrySet()) {
                        final Pair<Station, Station> staPair = distanceStaPair.getKey();
                        if (Double.isNaN(beforeStatsStaPairs.get(staPair).getStandardDeviation()) || beforeStatsStaPairs.get(staPair).getStandardDeviation() == 0.0) {
                            continue;
                        }
                        if (distanceStaPair.getValue() != null && beforeStatsStaPairs.get(staPair).getN() >= stationCountFilterField.getValue()) {
                            final String staPairDisplayName = staPair.getX().getStationName() + " " + staPair.getY().getStationName();
                            final Symbol plotObj = plotFactory.createSymbol(
                                    SymbolStyles.SQUARE,
                                        "Before",
                                        distanceStaPair.getValue(),
                                        beforeStatsStaPairs.get(staPair).getStandardDeviation(),
                                        Color.RED,
                                        Color.RED,
                                        Color.RED,
                                        staPairDisplayName,
                                        false);
                            plotObj.setZindex(BEFORE_Z_INDEX);
                            plotObj.setText(staPairDisplayName + " " + beforeStatsStaPairs.get(staPair).getN());

                            final Symbol plotObj2 = plotFactory.createSymbol(
                                    SymbolStyles.CIRCLE,
                                        "After",
                                        distanceStaPair.getValue(),
                                        afterStatsStaPairs.get(staPair).getStandardDeviation(),
                                        Color.BLUE,
                                        Color.BLUE,
                                        Color.BLUE,
                                        staPairDisplayName,
                                        false);
                            plotObj2.setZindex(AFTER_Z_INDEX);
                            plotObj2.setText(staPairDisplayName + " " + afterStatsStaPairs.get(staPair).getN());

                            if (xmax == null) {
                                xmax = plotObj.getX();
                            }
                            if (xmin == null) {
                                xmin = plotObj.getX();
                            }
                            if (ymax == null) {
                                ymax = plotObj.getY();
                            }
                            if (ymin == null) {
                                ymin = plotObj.getY();
                            }
                            if (plotObj.getX() > xmax) {
                                xmax = plotObj.getX();
                            }
                            if (plotObj.getY() > ymax) {
                                ymax = plotObj.getY();
                            }
                            if (plotObj.getX() < xmin) {
                                xmin = plotObj.getX();
                            }
                            if (plotObj.getY() < ymin) {
                                ymin = plotObj.getY();
                            }
                            sdPlot.addPlotObject(plotObj);

                            if (plotObj2.getX() > xmax) {
                                xmax = plotObj2.getX();
                            }
                            if (plotObj2.getY() > ymax) {
                                ymax = plotObj2.getY();
                            }
                            if (plotObj2.getX() < xmin) {
                                xmin = plotObj2.getX();
                            }
                            if (plotObj2.getY() < ymin) {
                                ymin = plotObj2.getY();
                            }
                            sdPlot.addPlotObject(plotObj2);

                            final Point2D point1 = new Point2D(plotObj.getX(), plotObj.getY());
                            final Point2D point2 = new Point2D(plotObj2.getX(), plotObj2.getY());
                            sdSymbolMap.put(point1, sourceMeasurements.get(staPair));
                            sdSymbolMap.put(point2, sourceMeasurements.get(staPair));
                        }
                    }
                    sdPlot.getTitle().setText("σ(Before) = " + dfmt2.format(overallBeforeStats.getStandardDeviation()) + "; σ(After) = " + dfmt2.format(overallAfterStats.getStandardDeviation()));
                    Double xAxisPaddingPercent = 0.1;
                    Double yAxisPaddingPercent = 0.3;

                    if (xmin != null && xmax != null) {
                        sdPlot.setAxisLimits(
                                new AxisLimits(Axis.Type.X, xmin - ((xmax - xmin) * xAxisPaddingPercent), xmax + ((xmax - xmin) * xAxisPaddingPercent)),
                                    new AxisLimits(Axis.Type.Y, ymin - ((ymax - ymin) * yAxisPaddingPercent), ymax + ((ymax - ymin) * yAxisPaddingPercent)));
                    }
                } catch (NullPointerException npe) {
                    log.error(npe.getLocalizedMessage(), npe);
                }
            }
        }
    }

    private void plotBeforeAfter() {
        if (!measurementsFreqBandMap.isEmpty() && !station1ComboBox.getSelectionModel().isEmpty() && !station2ComboBox.getSelectionModel().isEmpty()) {
            final List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());
            if (measurements != null) {
                final DescriptiveStatistics beforeStats = new DescriptiveStatistics();
                final DescriptiveStatistics afterStats = new DescriptiveStatistics();

                final Station firstStation = station1ComboBox.getSelectionModel().getSelectedItem();
                final Station secondStation = station2ComboBox.getSelectionModel().getSelectedItem();
                Double stationDistance = null;

                stationSymbolMap.clear();
                stationWaveformMap.clear();
                stationPlot.clear();
                Double xmin = null;
                Double xmax = null;

                final Map<Station, List<SpectraMeasurement>> stationMap = measurements.parallelStream().collect(Collectors.groupingBy(meas -> meas.getWaveform().getStream().getStation()));
                final List<SpectraMeasurement> firstMeasurements = stationMap.get(firstStation);
                final List<SpectraMeasurement> secondMeasurements = stationMap.get(secondStation);

                if (firstMeasurements != null && !firstMeasurements.isEmpty() && secondMeasurements != null && !secondMeasurements.isEmpty()) {
                    if (stationDistance == null) {
                        stationDistance = EModel.getDistanceWGS84(firstStation.getLatitude(), firstStation.getLongitude(), secondStation.getLatitude(), secondStation.getLongitude());
                    }

                    for (final SpectraMeasurement firstMeasurement : firstMeasurements) {
                        for (final SpectraMeasurement secondMeasurement : secondMeasurements) {
                            if (firstMeasurement.getWaveform().getEvent().getEventId() != null && firstMeasurement.getWaveform().getEvent().equals(secondMeasurement.getWaveform().getEvent())) {
                                beforeStats.addValue(firstMeasurement.getRawAtMeasurementTime() - secondMeasurement.getRawAtMeasurementTime());
                                afterStats.addValue(firstMeasurement.getPathCorrected() - secondMeasurement.getPathCorrected());

                                final Symbol plotObj = plotFactory.createSymbol(
                                        SymbolStyles.TRIANGLE_UP,
                                            "Before",
                                            firstMeasurement.getRawAtMeasurementTime(),
                                            secondMeasurement.getRawAtMeasurementTime(),
                                            Boolean.TRUE.equals(firstMeasurement.getWaveform().isActive()) ? Color.RED : Color.GRAY,
                                            Color.RED,
                                            Color.RED,
                                            firstStation.getStationName(),
                                            false);
                                plotObj.setZindex(BEFORE_Z_INDEX);
                                plotObj.setText(firstMeasurement.getWaveform().getEvent().getEventId());

                                final Symbol plotObj2 = plotFactory.createSymbol(
                                        SymbolStyles.TRIANGLE_DOWN,
                                            "After",
                                            firstMeasurement.getPathCorrected(),
                                            secondMeasurement.getPathCorrected(),
                                            Boolean.TRUE.equals(firstMeasurement.getWaveform().isActive()) ? Color.BLUE : Color.GRAY,
                                            Color.BLUE,
                                            Color.BLUE,
                                            secondStation.getStationName(),
                                            false);
                                plotObj2.setZindex(AFTER_Z_INDEX);
                                plotObj2.setText(firstMeasurement.getWaveform().getEvent().getEventId());

                                if (xmax == null) {
                                    xmax = plotObj.getX();
                                }
                                if (xmin == null) {
                                    xmin = plotObj.getX();
                                }
                                if (plotObj.getX() > xmax) {
                                    xmax = plotObj.getX();
                                }
                                if (plotObj.getY() > xmax) {
                                    xmax = plotObj.getY();
                                }
                                if (plotObj.getX() < xmin) {
                                    xmin = plotObj.getX();
                                }
                                if (plotObj.getY() < xmin) {
                                    xmin = plotObj.getY();
                                }
                                stationPlot.addPlotObject(plotObj);

                                if (plotObj2.getX() > xmax) {
                                    xmax = plotObj2.getX();
                                }
                                if (plotObj2.getY() > xmax) {
                                    xmax = plotObj2.getY();
                                }
                                if (plotObj2.getX() < xmin) {
                                    xmin = plotObj2.getX();
                                }
                                if (plotObj2.getY() < xmin) {
                                    xmin = plotObj2.getY();
                                }
                                stationPlot.addPlotObject(plotObj2);

                                final List<Waveform> waveformMetadata = new ArrayList<>(2);
                                waveformMetadata.add(firstMeasurement.getWaveform());
                                waveformMetadata.add(secondMeasurement.getWaveform());

                                final Point2D point1 = new Point2D(plotObj.getX(), plotObj.getY());
                                final Point2D point2 = new Point2D(plotObj2.getX(), plotObj2.getY());
                                stationSymbolMap.put(point1, waveformMetadata);
                                stationSymbolMap.put(point2, waveformMetadata);
                                waveformMetadata.forEach(waveform -> {
                                    List<Symbol> entries = stationWaveformMap.computeIfAbsent(waveform.getEvent().getEventId(), key -> new ArrayList<>());
                                    entries.add(plotObj);
                                    entries.add(plotObj2);

                                    entries = stationWaveformMap.computeIfAbsent(waveform.getStream().getStation().getStationName(), key -> new ArrayList<>());
                                    entries.add(plotObj);
                                    entries.add(plotObj2);
                                });
                            }
                        }
                    }
                }

                final double[] xy = new double[2];
                if (xmin == null) {
                    xmin = 0.0;
                }
                if (xmax == null) {
                    xmax = 0.0;
                }
                xy[0] = xmin;
                xy[1] = xmax;
                final Line line = plotFactory.line(xy, xy, Color.BLACK, LineStyles.DASH, 2);
                line.setName("");
                line.showInLegend(false);
                stationPlot.addPlotObject(line);

                if (stationDistance == null) {
                    stationDistance = 0.0;
                }

                stationXaxis.setText(firstStation.getStationName());
                stationYaxis.setText(secondStation.getStationName());

                final String labelText = "σ(Before) = "
                        + dfmt2.format(beforeStats.getStandardDeviation())
                        + "; σ(After) = "
                        + dfmt2.format(afterStats.getStandardDeviation())
                        + "; Station Distance "
                        + dfmt2.format(stationDistance)
                        + " (km)";
                stationPlot.getTitle().setText(labelText);
            }
        }
    }

    private void handlePlotObjectClicked(final PlotObjectClick poc, final List<Waveform> waveforms) {
        if (waveforms != null) {
            if (poc.getMouseEvent().isPrimaryButtonDown()) {
                final TreeSet<Waveform> sortedSet = new TreeSet<>(evStaComparator);
                sortedSet.addAll(waveforms);
                final List<Long> ids = sortedSet.stream().sequential().map(Waveform::getId).collect(Collectors.toList());
                selectSymbolsForWaveforms(sortedSet);
                selectWaveforms(ids.toArray(new Long[0]));
                Platform.runLater(() -> menu.hide());
            } else if (poc.getMouseEvent().isSecondaryButtonDown()) {
                showContextMenu(waveforms, poc.getMouseEvent(), this::setSymbolsActive);
            }
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        isVisible = visible;
    }
}
