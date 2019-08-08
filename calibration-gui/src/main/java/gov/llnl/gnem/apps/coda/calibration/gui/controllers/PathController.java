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

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.CommonGuiUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.MouseOverPlotObject;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.PlotObjectClicked;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.plotobject.Circle;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Square;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.TriangleDn;
import llnl.gnem.core.gui.plotting.plotobject.TriangleUp;
import llnl.gnem.core.util.Geometry.EModel;

@Component
public class PathController implements MapListeningController, RefreshableController {

    private JMultiAxisPlot stationPlot;
    private JMultiAxisPlot sdPlot;

    private Map<FrequencyBand, List<SpectraMeasurement>> measurementsFreqBandMap = new TreeMap<>();

    private NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();

    @FXML
    private ComboBox<FrequencyBand> frequencyBandComboBox;

    @FXML
    private ComboBox<Station> station1ComboBox;

    @FXML
    private ComboBox<Station> station2ComboBox;

    @FXML
    SwingNode stationPlotSwingNode;

    @FXML
    SwingNode sdPlotSwingNode;

    @FXML
    Pane path;

    private SpectraClient spectraMeasurementClient;
    private WaveformClient waveformClient;

    private GeoMap mapImpl;

    private MapPlottingUtilities mappingUtilities;

    private ObservableSet<Station> stations = FXCollections.synchronizedObservableSet(FXCollections.observableSet(new TreeSet<>((lhs, rhs) -> lhs.getStationName().compareTo(rhs.getStationName()))));

    private Map<Point2D.Double, List<Waveform>> sdSymbolMap = new HashMap<>();
    private Map<Point2D.Double, List<Waveform>> stationSymbolMap = new HashMap<>();
    private Map<String, List<Symbol>> stationWaveformMap = new HashMap<>();

    private Tooltip sdPlotTooltip;
    private Tooltip stationPlotTooltip;
    private EventBus bus;
    private Comparator<? super Waveform> evStaComparator = new EventStaFreqStringComparator();

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;
    private final List<Symbol> selectedSymbols = new ArrayList<>();
    private MenuItem exclude;
    private MenuItem include;
    private ContextMenu menu;

    @Autowired
    public PathController(SpectraClient spectraMeasurementClient, WaveformClient waveformClient, EventBus bus, GeoMap mapImpl, MapPlottingUtilities mappingUtilities) {
        super();
        this.spectraMeasurementClient = spectraMeasurementClient;
        this.waveformClient = waveformClient;
        this.mapImpl = mapImpl;
        this.mappingUtilities = mappingUtilities;
        this.bus = bus;

        eventSelectionCallback = (selected, eventId) -> {
            selectDataByCriteria(bus, selected, eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            selectDataByCriteria(bus, selected, stationId);
        };
    }

    private void selectDataByCriteria(EventBus bus, Boolean selected, String key) {
        if (selected) {
            List<Symbol> stationSymbols = stationWaveformMap.get(key);
            if (!selectedSymbols.isEmpty()) {
                deselectSymbols(selectedSymbols);
            }
            selectSymbols(stationSymbols);
        } else {
            List<Symbol> stationSymbols = stationWaveformMap.get(key);
            deselectSymbols(stationSymbols);
        }
    }

    private void selectSymbols(List<Symbol> stationSymbols) {
        if (stationSymbols != null && !stationSymbols.isEmpty()) {
            selectedSymbols.addAll(stationSymbols);
            SwingUtilities.invokeLater(() -> {
                stationSymbols.forEach(po -> {
                    JBasicPlot owner = po.getOwner();
                    if (owner != null) {
                        owner.DeletePlotObject(po);
                        owner.AddPlotObject(po, 99);
                    }
                    po.setFillColor(Color.YELLOW);
                });
                stationPlot.repaint();
            });
        }
    }

    private void deselectSymbols(List<Symbol> stationSymbols) {
        if (stationSymbols != null && !stationSymbols.isEmpty()) {
            List<Symbol> symbols = new ArrayList<>(stationSymbols.size());
            symbols.addAll(stationSymbols);
            SwingUtilities.invokeLater(() -> {
                symbols.forEach(po -> {
                    JBasicPlot owner = po.getOwner();
                    if (po instanceof TriangleUp) {
                        po.setFillColor(Color.RED);
                        if (owner != null) {
                            owner.DeletePlotObject(po);
                            owner.AddPlotObject(po, 9);
                        }
                    } else if (po instanceof TriangleDn) {
                        po.setFillColor(Color.BLUE);
                        if (owner != null) {
                            owner.DeletePlotObject(po);
                            owner.AddPlotObject(po, 10);
                        }
                    }
                });
                stationPlot.repaint();
            });
        }
    }

    @FXML
    public void initialize() {
        SwingUtilities.invokeLater(() -> {
            stationPlot = new JMultiAxisPlot();
            stationPlotTooltip = new Tooltip();

            stationPlot.addPlotObjectObserver(new Observer() {

                @Override
                public void update(Observable observable, Object obj) {
                    if (obj instanceof MouseOverPlotObject) {
                        MouseOverPlotObject pos = (MouseOverPlotObject) obj;
                        PlotObject po = pos.getPlotObject();
                        if (po instanceof Symbol) {
                            Platform.runLater(() -> {
                                stationPlotTooltip.setText(((Symbol) po).getText());
                                Point p = CommonGuiUtils.getScaledMouseLocation(stationPlotSwingNode.getScene(), MouseInfo.getPointerInfo());
                                stationPlotTooltip.show(stationPlotSwingNode, p.getX() + 10, p.getY() + 10);
                            });
                        }
                    } else if (stationPlotTooltip.isShowing()) {
                        Platform.runLater(() -> stationPlotTooltip.hide());
                    }

                    if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
                        PlotObjectClicked poc = (PlotObjectClicked) obj;
                        PlotObject po = poc.getPlotObject();
                        if (po instanceof Symbol) {
                            List<Waveform> waveforms = stationSymbolMap.get(new Point2D.Double(((Symbol) po).getXcenter(), ((Symbol) po).getYcenter()));
                            handlePlotObjectClicked(poc, waveforms);
                        }
                    }
                }
            });

            stationPlot.setYaxisVisibility(true);
            stationPlot.setShowPickTooltips(true);

            sdPlot = new JMultiAxisPlot();
            sdPlotTooltip = new Tooltip();

            sdPlot.addPlotObjectObserver(new Observer() {
                @Override
                public void update(Observable observable, Object obj) {
                    if (obj instanceof MouseOverPlotObject) {
                        MouseOverPlotObject pos = (MouseOverPlotObject) obj;
                        PlotObject po = pos.getPlotObject();
                        if (po instanceof Symbol) {
                            Platform.runLater(() -> {
                                sdPlotTooltip.setText(((Symbol) po).getText());
                                Point p = CommonGuiUtils.getScaledMouseLocation(sdPlotSwingNode.getScene(), MouseInfo.getPointerInfo());
                                sdPlotTooltip.show(sdPlotSwingNode, p.getX() + 10, p.getY() + 10);
                            });
                        }
                    } else if (sdPlotTooltip.isShowing()) {
                        Platform.runLater(() -> sdPlotTooltip.hide());
                    }

                    if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
                        PlotObjectClicked poc = (PlotObjectClicked) obj;
                        PlotObject po = poc.getPlotObject();
                        if (po instanceof Symbol) {
                            List<Waveform> waveforms = sdSymbolMap.get(new Point2D.Double(((Symbol) po).getXcenter(), ((Symbol) po).getYcenter()));
                            handlePlotObjectClicked(poc, waveforms);
                        }
                    }
                }
            });
            sdPlot.getXaxis().setLabelText("Distance (km)");
            sdPlot.setYaxisVisibility(true);
            sdPlot.setShowPickTooltips(true);

            stationPlotSwingNode.setContent(stationPlot);
            sdPlotSwingNode.setContent(sdPlot);
        });

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

        EventHandler<javafx.scene.input.MouseEvent> menuHideHandler = (evt) -> {
            if (MouseButton.SECONDARY != evt.getButton()) {
                menu.hide();
            }
        };
        path.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
        sdPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
        stationPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
    }

    private void setSymbolsActive(List<Waveform> ws, Boolean active) {
        SwingUtilities.invokeLater(() -> {
            ws.stream().flatMap(w -> Optional.ofNullable(stationWaveformMap.get(w.getEvent().getEventId())).orElseGet(() -> new ArrayList<>()).stream()).forEach(sym -> {
                if (active) {
                    sym.setFillColor(sym.getEdgeColor());
                } else {
                    sym.setFillColor(Color.GRAY);
                }
            });
            Platform.runLater(() -> {
                stationPlot.repaint();
            });
        });
    }

    private void showContextMenu(List<Waveform> waveforms, MouseEvent t, BiConsumer<List<Waveform>, Boolean> activationFunc) {
        Platform.runLater(() -> {
            include.setOnAction(evt -> setActive(waveforms, true, activationFunc));
            exclude.setOnAction(evt -> setActive(waveforms, false, activationFunc));
            menu.show(path, t.getXOnScreen(), t.getYOnScreen());
        });
    }

    private void setActive(List<Waveform> waveforms, boolean active, BiConsumer<List<Waveform>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().map(w -> w.getId()).collect(Collectors.toList()), active).subscribe(s -> activationFunc.accept(waveforms, active));
    }

    private void selectSymbolsForWaveforms(Collection<Waveform> sortedSet) {
        if (!selectedSymbols.isEmpty()) {
            deselectSymbols(selectedSymbols);
            selectedSymbols.clear();
        }
        sortedSet.forEach(w -> {
            selectSymbols(stationWaveformMap.get(w.getEvent().getEventId()));
        });
    }

    private void selectWaveforms(Long... waveformIds) {
        bus.post(new WaveformSelectionEvent(waveformIds));
    }

    private ListCell<Station> getStationCell() {
        return new ListCell<Station>() {
            @Override
            protected void updateItem(Station item, boolean empty) {
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
            protected void updateItem(FrequencyBand item, boolean empty) {
                super.updateItem(item, empty);
                if (item != null && !empty) {
                    setText(dfmt2.format(item.getLowFrequency()) + "-" + dfmt2.format(item.getHighFrequency()));
                }
            }
        };
    }

    private void reloadData() {
        measurementsFreqBandMap.clear();
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
        plotPaths();
        plotBeforeAfter();
        plotSd();
        Platform.runLater(() -> {
            stationPlot.repaint();
            sdPlot.repaint();
        });
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> reloadData();
    }

    private void plotPaths() {
        mapImpl.clearIcons();
        if (measurementsFreqBandMap != null && !measurementsFreqBandMap.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());
                if (measurements != null) {
                    Map<Station, List<Event>> stationToEvents = measurements.parallelStream()
                                                                            .filter(meas -> meas.getWaveform() != null && meas.getWaveform().getStream() != null)
                                                                            .map(meas -> meas.getWaveform())
                                                                            .filter(Objects::nonNull)
                                                                            .distinct()
                                                                            .collect(
                                                                                    Collectors.groupingBy(
                                                                                            w -> w.getStream().getStation(),
                                                                                                HashMap::new,
                                                                                                Collectors.mapping(w -> w.getEvent(), Collectors.toList())));

                    mapImpl.addIcons(stationToEvents.keySet().stream().distinct().map(station -> {
                        List<Icon> icons = new ArrayList<>();
                        if (station.equals(station1ComboBox.getSelectionModel().getSelectedItem()) || station.equals(station2ComboBox.getSelectionModel().getSelectedItem())) {
                            icons.add(mappingUtilities.createStationIconForeground(station));
                        }
                        icons.add(mappingUtilities.createStationIconBackground(station).setIconSelectionCallback(stationSelectionCallback));
                        return icons;
                    }).flatMap(icons -> icons.stream()).collect(Collectors.toSet()));

                    mapImpl.addIcons(
                            stationToEvents.values()
                                           .stream()
                                           .flatMap(events -> events.stream().map(event -> mappingUtilities.createEventIcon(event).setIconSelectionCallback(eventSelectionCallback)))
                                           .distinct()
                                           .collect(Collectors.toSet()));

                    stationToEvents.entrySet().stream().flatMap(entry -> {
                        Station station = entry.getKey();
                        return entry.getValue().stream().map(event -> mappingUtilities.createStationToEventLine(station, event));
                    }).forEach(mapImpl::addShape);
                }
            });
        }
    }

    private void plotSd() {
        if (measurementsFreqBandMap != null && !measurementsFreqBandMap.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                sdSymbolMap.clear();
                sdPlot.clear();
                JSubplot plot = sdPlot.addSubplot();
                Double xmin = null;
                Double xmax = null;
                Double ymin = 0.0;
                Double ymax = 2.0;

                Map<Pair<Station, Station>, DescriptiveStatistics> beforeStatsStaPairs = new HashMap<>();
                Map<Pair<Station, Station>, DescriptiveStatistics> afterStatsStaPairs = new HashMap<>();
                Map<Pair<Station, Station>, Double> distanceStaPairs = new HashMap<>();

                List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());

                Map<Pair<Station, Station>, List<Waveform>> sourceMeasurements = new HashMap<>();
                Map<Pair<Station, Station>, Set<String>> used = new HashMap<>();

                if (measurements != null) {
                    DescriptiveStatistics overallBeforeStats = new DescriptiveStatistics();
                    DescriptiveStatistics overallAfterStats = new DescriptiveStatistics();

                    for (SpectraMeasurement firstMeasurement : measurements) {
                        for (SpectraMeasurement secondMeasurement : measurements) {
                            if (!firstMeasurement.equals(secondMeasurement) && firstMeasurement.getWaveform().getEvent().equals(secondMeasurement.getWaveform().getEvent())) {
                                Station firstStation = firstMeasurement.getWaveform().getStream().getStation();
                                Station secondStation = secondMeasurement.getWaveform().getStream().getStation();
                                if (!firstStation.equals(secondStation)) {
                                    Pair<Station, Station> staPair = new Pair<>(firstStation, secondStation);
                                    Pair<Station, Station> staPair2 = new Pair<>(secondStation, firstStation);
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
                                        sourceMeasurements.put(staPair, new ArrayList<Waveform>());
                                        sourceMeasurements.put(staPair2, sourceMeasurements.get(staPair));
                                    }

                                    if (!used.get(staPair).contains(firstMeasurement.getWaveform().getEvent().getEventId())
                                            && !used.get(staPair2).contains(firstMeasurement.getWaveform().getEvent().getEventId())) {
                                        used.get(staPair).add(firstMeasurement.getWaveform().getEvent().getEventId());
                                        beforeStats = beforeStatsStaPairs.get(staPair);
                                        afterStats = afterStatsStaPairs.get(staPair);

                                        double before = Math.abs(firstMeasurement.getRawAtMeasurementTime() - secondMeasurement.getRawAtMeasurementTime());
                                        double after = Math.abs(firstMeasurement.getPathCorrected() - secondMeasurement.getPathCorrected());

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

                    for (Entry<Pair<Station, Station>, Double> distanceStaPair : distanceStaPairs.entrySet()) {
                        Pair<Station, Station> staPair = distanceStaPair.getKey();
                        if (Double.isNaN(beforeStatsStaPairs.get(staPair).getStandardDeviation()) || beforeStatsStaPairs.get(staPair).getStandardDeviation() == 0.0) {
                            continue;
                        }
                        String staPairDisplayName = staPair.getLeft().getStationName() + " " + staPair.getRight().getStationName();
                        Square plotObj = new Square(distanceStaPair.getValue(),
                                                    beforeStatsStaPairs.get(staPair).getStandardDeviation(),
                                                    4.0,
                                                    Color.RED,
                                                    Color.RED,
                                                    Color.RED,
                                                    staPairDisplayName,
                                                    true,
                                                    false,
                                                    0);
                        plotObj.setText(staPairDisplayName + " " + beforeStatsStaPairs.get(staPair).getN());

                        Circle plotObj2 = new Circle(distanceStaPair.getValue(),
                                                     afterStatsStaPairs.get(staPair).getStandardDeviation(),
                                                     4.0,
                                                     Color.BLUE,
                                                     Color.BLUE,
                                                     Color.BLUE,
                                                     staPairDisplayName,
                                                     true,
                                                     false,
                                                     0);
                        plotObj2.setText(staPairDisplayName + " " + afterStatsStaPairs.get(staPair).getN());

                        if (xmax == null) {
                            xmax = plotObj.getXcenter();
                        }
                        if (xmin == null) {
                            xmin = plotObj.getXcenter();
                        }
                        if (plotObj.getXcenter() > xmax) {
                            xmax = plotObj.getXcenter();
                        }
                        if (plotObj.getYcenter() > ymax) {
                            ymax = plotObj.getYcenter();
                        }
                        if (plotObj.getXcenter() < xmin) {
                            xmin = plotObj.getXcenter();
                        }
                        if (plotObj.getYcenter() < ymin) {
                            ymin = plotObj.getYcenter();
                        }
                        plot.AddPlotObject(plotObj, 9);

                        if (plotObj2.getXcenter() > xmax) {
                            xmax = plotObj2.getXcenter();
                        }
                        if (plotObj2.getYcenter() > ymax) {
                            ymax = plotObj2.getYcenter();
                        }
                        if (plotObj2.getXcenter() < xmin) {
                            xmin = plotObj2.getXcenter();
                        }
                        if (plotObj2.getYcenter() < ymin) {
                            ymin = plotObj2.getYcenter();
                        }
                        plot.AddPlotObject(plotObj2, 10);

                        Point2D.Double point1 = new Point2D.Double(plotObj.getXcenter(), plotObj.getYcenter());
                        Point2D.Double point2 = new Point2D.Double(plotObj2.getXcenter(), plotObj2.getYcenter());
                        sdSymbolMap.put(point1, sourceMeasurements.get(staPair));
                        sdSymbolMap.put(point2, sourceMeasurements.get(staPair));
                    }
                    if (xmax != null) {
                        plot.setAxisLimits(xmin - (xmin * .1) - .1, xmax + (xmax * .1) + .1, ymin - (ymin * .1) - .1, ymax + (ymax * .1) + .1);
                    }
                    plot.getYaxis().setVisible(false);
                    plot.getYaxis().setLabelOffset(12d);
                    plot.getYaxis().setLabelText("σ(|deviation|)");
                    plot.getYaxis().setVisible(true);

                    sdPlot.getTitle().setText("σ(Before) = " + dfmt2.format(overallBeforeStats.getStandardDeviation()) + "; σ(After) = " + dfmt2.format(overallAfterStats.getStandardDeviation()));
                }
            });
        }
    }

    private void plotBeforeAfter() {
        if (measurementsFreqBandMap != null && !measurementsFreqBandMap.isEmpty() && !station1ComboBox.getSelectionModel().isEmpty() && !station2ComboBox.getSelectionModel().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());
                if (measurements != null) {
                    DescriptiveStatistics beforeStats = new DescriptiveStatistics();
                    DescriptiveStatistics afterStats = new DescriptiveStatistics();

                    Station firstStation = station1ComboBox.getSelectionModel().getSelectedItem();
                    Station secondStation = station2ComboBox.getSelectionModel().getSelectedItem();
                    Double stationDistance = null;

                    stationSymbolMap.clear();
                    stationWaveformMap.clear();
                    stationPlot.clear();
                    JSubplot plot = stationPlot.addSubplot();
                    Double xmin = null;
                    Double xmax = null;

                    Map<Station, List<SpectraMeasurement>> stationMap = measurements.parallelStream().collect(Collectors.groupingBy(meas -> meas.getWaveform().getStream().getStation()));
                    List<SpectraMeasurement> firstMeasurements = stationMap.get(firstStation);
                    List<SpectraMeasurement> secondMeasurements = stationMap.get(secondStation);

                    if (firstMeasurements != null && !firstMeasurements.isEmpty() && secondMeasurements != null && !secondMeasurements.isEmpty()) {
                        if (stationDistance == null) {
                            stationDistance = EModel.getDistanceWGS84(firstStation.getLatitude(), firstStation.getLongitude(), secondStation.getLatitude(), secondStation.getLongitude());
                        }

                        for (SpectraMeasurement firstMeasurement : firstMeasurements) {
                            for (SpectraMeasurement secondMeasurement : secondMeasurements) {
                                if (firstMeasurement.getWaveform().getEvent().equals(secondMeasurement.getWaveform().getEvent())) {
                                    beforeStats.addValue(firstMeasurement.getRawAtMeasurementTime() - secondMeasurement.getRawAtMeasurementTime());
                                    afterStats.addValue(firstMeasurement.getPathCorrected() - secondMeasurement.getPathCorrected());

                                    TriangleUp plotObj = new TriangleUp(firstMeasurement.getRawAtMeasurementTime(),
                                                                        secondMeasurement.getRawAtMeasurementTime(),
                                                                        5.0,
                                                                        firstMeasurement.getWaveform().isActive() ? Color.RED : Color.GRAY,
                                                                        Color.RED,
                                                                        Color.RED,
                                                                        firstStation.getStationName(),
                                                                        true,
                                                                        false,
                                                                        0);
                                    plotObj.setText(firstMeasurement.getWaveform().getEvent().getEventId());

                                    TriangleDn plotObj2 = new TriangleDn(firstMeasurement.getPathCorrected(),
                                                                         secondMeasurement.getPathCorrected(),
                                                                         5.0,
                                                                         firstMeasurement.getWaveform().isActive() ? Color.BLUE : Color.GRAY,
                                                                         Color.BLUE,
                                                                         Color.BLUE,
                                                                         secondStation.getStationName(),
                                                                         true,
                                                                         false,
                                                                         0);
                                    plotObj2.setText(firstMeasurement.getWaveform().getEvent().getEventId());

                                    if (xmax == null) {
                                        xmax = plotObj.getXcenter();
                                    }
                                    if (xmin == null) {
                                        xmin = plotObj.getXcenter();
                                    }
                                    if (plotObj.getXcenter() > xmax) {
                                        xmax = plotObj.getXcenter();
                                    }
                                    if (plotObj.getYcenter() > xmax) {
                                        xmax = plotObj.getYcenter();
                                    }
                                    if (plotObj.getXcenter() < xmin) {
                                        xmin = plotObj.getXcenter();
                                    }
                                    if (plotObj.getYcenter() < xmin) {
                                        xmin = plotObj.getYcenter();
                                    }
                                    plot.AddPlotObject(plotObj, 9);

                                    if (plotObj2.getXcenter() > xmax) {
                                        xmax = plotObj2.getXcenter();
                                    }
                                    if (plotObj2.getYcenter() > xmax) {
                                        xmax = plotObj2.getYcenter();
                                    }
                                    if (plotObj2.getXcenter() < xmin) {
                                        xmin = plotObj2.getXcenter();
                                    }
                                    if (plotObj2.getYcenter() < xmin) {
                                        xmin = plotObj2.getYcenter();
                                    }
                                    plot.AddPlotObject(plotObj2, 10);

                                    List<Waveform> waveformMetadata = new ArrayList<>(2);
                                    waveformMetadata.add(firstMeasurement.getWaveform());
                                    waveformMetadata.add(secondMeasurement.getWaveform());

                                    Point2D.Double point1 = new Point2D.Double(plotObj.getXcenter(), plotObj.getYcenter());
                                    Point2D.Double point2 = new Point2D.Double(plotObj2.getXcenter(), plotObj2.getYcenter());
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

                    if (xmax == null) {
                        xmax = 1.0;
                    }
                    if (xmin == null) {
                        xmin = 0.0;
                    }
                    double paddedXmin = xmin - Math.abs(xmin * .1);
                    double paddedXmax = xmax + Math.abs(xmax * .1);
                    if (xmax != null) {
                        plot.setAxisLimits(paddedXmin, paddedXmax, paddedXmin, paddedXmax);
                    }
                    int points = 50;
                    double dx = (plot.getXaxis().getMax() - plot.getXaxis().getMin()) / (points - 1);
                    float[] xy = new float[points];
                    for (int i = 0; i < points; i++) {
                        xy[i] = (float) (plot.getXaxis().getMin() + (dx * i));
                    }
                    Line line = new Line(xy, xy, Color.black, PaintMode.COPY, PenStyle.DASH, 2);

                    plot.AddPlotObject(line, 1);

                    if (stationDistance == null) {
                        stationDistance = 0.0;
                    }

                    stationPlot.getXaxis().setVisible(false);
                    stationPlot.getXaxis().setLabelText(firstStation.getStationName());
                    stationPlot.getXaxis().setVisible(true);

                    plot.getYaxis().setVisible(false);
                    plot.getYaxis().setLabelOffset(12d);
                    plot.getYaxis().setLabelText(secondStation.getStationName());
                    plot.getYaxis().setVisible(true);

                    String labelText = "σ(Before) = "
                            + dfmt2.format(beforeStats.getStandardDeviation())
                            + "; σ(After) = "
                            + dfmt2.format(afterStats.getStandardDeviation())
                            + "; Station Distance "
                            + dfmt2.format(stationDistance)
                            + " (km)";
                    stationPlot.getTitle().setText(labelText);
                }
            });
        }
    }

    private void handlePlotObjectClicked(PlotObjectClicked poc, List<Waveform> waveforms) {
        if (waveforms != null) {
            if (SwingUtilities.isLeftMouseButton(poc.getMouseEvent())) {
                TreeSet<Waveform> sortedSet = new TreeSet<>(evStaComparator);
                sortedSet.addAll(waveforms);
                List<Long> ids = sortedSet.stream().sequential().map(w -> w.getId()).collect(Collectors.toList());
                selectSymbolsForWaveforms(sortedSet);
                selectWaveforms(ids.toArray(new Long[0]));
                Platform.runLater(() -> menu.hide());
            } else if (SwingUtilities.isRightMouseButton(poc.getMouseEvent())) {
                showContextMenu(waveforms, poc.getMouseEvent(), (ws, active) -> {
                    setSymbolsActive(ws, active);
                });
            }
        }
    }
}
