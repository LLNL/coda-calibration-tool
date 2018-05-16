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

import java.awt.Color;
import java.awt.MouseInfo;
import java.awt.Point;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.IconFactory;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Location;
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import javafx.embed.swing.SwingNode;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import llnl.gnem.core.gui.plotting.MouseOverPlotObject;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.plotobject.Circle;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Square;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.TriangleDn;
import llnl.gnem.core.gui.plotting.plotobject.TriangleUp;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.Geometry.EModel;

@Component
public class PathController {

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
    StackPane mapParent;

    @FXML
    SwingNode stationPlotSwingNode;

    @FXML
    SwingNode sdPlotSwingNode;

    private SpectraClient spectraMeasurementClient;

    private gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Map mapImpl;

    private IconFactory iconFactory;

    private ObservableSet<Station> stations = FXCollections.synchronizedObservableSet(FXCollections.observableSet(new TreeSet<>((lhs, rhs) -> lhs.getStationName().compareTo(rhs.getStationName()))));
    private Tooltip sdPlotTooltip;
    private Tooltip stationPlotTooltip;

    @Autowired
    public PathController(SpectraClient spectraMeasurementClient, gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Map mapImpl, IconFactory iconFactory) {
        super();
        this.spectraMeasurementClient = spectraMeasurementClient;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
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
                        if (po != null && po instanceof Symbol) {
                            Platform.runLater(() -> {
                                stationPlotTooltip.setText(((Symbol) po).getText());
                                Point p = MouseInfo.getPointerInfo().getLocation();
                                stationPlotTooltip.show(stationPlotSwingNode, p.getX() + 10, p.getY() + 10);
                            });
                        }
                    } else if (stationPlotTooltip.isShowing()) {
                        Platform.runLater(() -> stationPlotTooltip.hide());
                    }
                }
            });

            stationPlot.getTitle().setText("Before/After Correction");
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
                        if (po != null && po instanceof Symbol) {
                            Platform.runLater(() -> {
                                sdPlotTooltip.setText(((Symbol) po).getText());
                                Point p = MouseInfo.getPointerInfo().getLocation();
                                sdPlotTooltip.show(sdPlotSwingNode, p.getX() + 10, p.getY() + 10);
                            });
                        }
                    } else if (sdPlotTooltip.isShowing()) {
                        Platform.runLater(() -> sdPlotTooltip.hide());
                    }
                }
            });
            sdPlot.getTitle().setText("Overall StdDev for Frequency Band");
            sdPlot.setYaxisVisibility(true);
            sdPlot.setShowPickTooltips(true);

            stationPlotSwingNode.setContent(stationPlot);
            sdPlotSwingNode.setContent(sdPlot);
        });

        frequencyBandComboBox.setCellFactory(fb -> getFBCell());
        frequencyBandComboBox.setButtonCell(getFBCell());
        frequencyBandComboBox.valueProperty().addListener(e -> {
            plotPaths();
            plotBeforeAfter();
            plotSd();
        });

        station1ComboBox.setCellFactory(station -> getStationCell());
        station1ComboBox.setButtonCell(getStationCell());

        station2ComboBox.setCellFactory(station -> getStationCell());
        station2ComboBox.setButtonCell(getStationCell());

        station1ComboBox.valueProperty().addListener(e -> plotBeforeAfter());
        station2ComboBox.valueProperty().addListener(e -> plotBeforeAfter());

        mapImpl.attach(mapParent);
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

    @FXML
    private void reloadData(ActionEvent e) {
        reloadData();
    }

    private void reloadData() {
        mapImpl.clearIcons();
        measurementsFreqBandMap.clear();
        stations.clear();
        station1ComboBox.getItems().clear();
        station2ComboBox.getItems().clear();

        frequencyBandComboBox.getItems().clear();
        measurementsFreqBandMap.putAll(spectraMeasurementClient.getMeasuredSpectra()
                                                               .filter(Objects::nonNull)
                                                               .filter(spectra -> spectra.getWaveform() != null)
                                                               .toStream()
                                                               .collect(Collectors.groupingBy(spectra -> new FrequencyBand(spectra.getWaveform().getLowFrequency(),
                                                                                                                           spectra.getWaveform().getHighFrequency()))));

        stations.addAll(measurementsFreqBandMap.values().parallelStream().flatMap(List::parallelStream).map(spectra -> spectra.getWaveform().getStream().getStation()).collect(Collectors.toList()));

        frequencyBandComboBox.getItems().addAll(measurementsFreqBandMap.keySet());
        frequencyBandComboBox.getSelectionModel().selectFirst();

        station1ComboBox.getItems().addAll(stations);
        station1ComboBox.getSelectionModel().selectFirst();

        station2ComboBox.getItems().addAll(stations);
        station2ComboBox.getSelectionModel().selectFirst();

        plotPaths();
        plotBeforeAfter();
        plotSd();
    }

    private void plotPaths() {
        if (measurementsFreqBandMap != null && !measurementsFreqBandMap.isEmpty()) {
            mapImpl.clearIcons();
            SwingUtilities.invokeLater(() -> {
                List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());
                if (measurements != null) {
                    mapImpl.addIcons(measurements.parallelStream()
                                                 .filter(meas -> meas.getWaveform() != null && meas.getWaveform().getStream() != null)
                                                 .map(meas -> meas.getWaveform().getStream().getStation())
                                                 .filter(Objects::nonNull)
                                                 .distinct()
                                                 .map(station -> iconFactory.newIcon(IconTypes.TRIANGLE_UP, new Location(station.getLatitude(), station.getLongitude()), station.getStationName()))
                                                 .collect(Collectors.toSet()));

                    mapImpl.addIcons(measurements.parallelStream()
                                                 .filter(meas -> meas.getWaveform() != null)
                                                 .map(meas -> meas.getWaveform().getEvent())
                                                 .filter(Objects::nonNull)
                                                 .map(event -> iconFactory.newIcon(new Location(event.getLatitude(), event.getLongitude()), event.getEventId()))
                                                 .collect(Collectors.toSet()));

                }
            });

        }
    }

    private void plotSd() {
        if (measurementsFreqBandMap != null && !measurementsFreqBandMap.isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                sdPlot.clear();
                JSubplot plot = sdPlot.addSubplot();
                Double xmin = null;
                Double xmax = null;
                Double ymin = 0.0;
                Double ymax = 2.0;

                Map<Pair<Station, Station>, DescriptiveStatistics> beforeStatsStaPair = new HashMap<>();
                Map<Pair<Station, Station>, DescriptiveStatistics> afterStatsStaPair = new HashMap<>();
                Map<Pair<Station, Station>, Double> distanceStaPair = new HashMap<>();

                List<SpectraMeasurement> measurements = measurementsFreqBandMap.get(frequencyBandComboBox.getSelectionModel().getSelectedItem());
                if (measurements != null) {
                    DescriptiveStatistics overallBeforeStats = new DescriptiveStatistics();
                    DescriptiveStatistics overallAfterStats = new DescriptiveStatistics();

                    for (SpectraMeasurement firstMeasurement : measurements) {
                        for (SpectraMeasurement secondMeasurement : measurements) {
                            if (firstMeasurement.getWaveform().getEvent().equals(secondMeasurement.getWaveform().getEvent())) {

                                Station firstStation = firstMeasurement.getWaveform().getStream().getStation();
                                Station secondStation = secondMeasurement.getWaveform().getStream().getStation();
                                if (!firstStation.equals(secondStation)) {

                                    Pair<Station, Station> staPair = new Pair<>(firstStation, secondStation);
                                    DescriptiveStatistics beforeStats = new DescriptiveStatistics();
                                    DescriptiveStatistics afterStats = new DescriptiveStatistics();

                                    if (!beforeStatsStaPair.containsKey(staPair)) {
                                        beforeStatsStaPair.put(staPair, beforeStats);
                                        afterStatsStaPair.put(staPair, afterStats);
                                        distanceStaPair.put(staPair,
                                                            EModel.getDistanceWGS84(firstStation.getLatitude(),
                                                                                    firstStation.getLongitude(),
                                                                                    secondStation.getLatitude(),
                                                                                    secondStation.getLongitude()));
                                    } else {
                                        beforeStats = beforeStatsStaPair.get(staPair);
                                        afterStats = afterStatsStaPair.get(staPair);
                                    }

                                    double before = firstMeasurement.getRawAtMeasurementTime() - secondMeasurement.getRawAtMeasurementTime();
                                    double after = firstMeasurement.getPathCorrected() - secondMeasurement.getPathCorrected();

                                    beforeStats.addValue(before);
                                    afterStats.addValue(after);

                                    overallBeforeStats.addValue(before);
                                    overallAfterStats.addValue(after);
                                }
                            }
                        }
                    }

                    for (Entry<Pair<Station, Station>, DescriptiveStatistics> staPairEntry : beforeStatsStaPair.entrySet()) {

                        Pair<Station, Station> staPair = staPairEntry.getKey();
                        if (Double.isNaN(staPairEntry.getValue().getStandardDeviation()) || staPairEntry.getValue().getStandardDeviation() == 0.0) {
                            continue;
                        }
                        String staPairDisplayName = staPair.getFirst().getStationName() + " " + staPair.getSecond().getStationName();
                        Square plotObj = new Square(distanceStaPair.get(staPair),
                                                    staPairEntry.getValue().getStandardDeviation(),
                                                    4.0,
                                                    Color.RED,
                                                    Color.RED,
                                                    Color.RED,
                                                    staPairDisplayName,
                                                    true,
                                                    false,
                                                    0);
                        plotObj.setText(staPairDisplayName + " " + staPairEntry.getValue().getN());

                        Circle plotObj2 = new Circle(distanceStaPair.get(staPair),
                                                     afterStatsStaPair.get(staPair).getStandardDeviation(),
                                                     4.0,
                                                     Color.BLUE,
                                                     Color.BLUE,
                                                     Color.BLUE,
                                                     staPairDisplayName,
                                                     true,
                                                     false,
                                                     0);
                        plotObj2.setText(staPairDisplayName + " " + afterStatsStaPair.get(staPair).getN());

                        if (xmax == null) {
                            xmax = plotObj.getXcenter();
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
                    }
                    if (xmax != null) {
                        plot.SetAxisLimits(xmin - (xmin * .1) - .1, xmax + (xmax * .1) + .1, ymin - (ymin * .1) - .1, ymax + (ymax * .1) + .1);
                    }
                    String labelText = "StdDev(Before) = " + dfmt2.format(overallBeforeStats.getStandardDeviation()) + " StdDev(After) = " + dfmt2.format(overallAfterStats.getStandardDeviation());
                    sdPlot.getXaxis().setVisible(false);
                    sdPlot.getXaxis().setLabelText(labelText);
                    sdPlot.getXaxis().setVisible(true);
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
                                                                        Color.RED,
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
                                                                         Color.BLUE,
                                                                         Color.BLUE,
                                                                         Color.BLUE,
                                                                         secondStation.getStationName(),
                                                                         true,
                                                                         false,
                                                                         0);
                                    plotObj2.setText(firstMeasurement.getWaveform().getEvent().getEventId());

                                    if (xmax == null) {
                                        xmax = plotObj.getXcenter();
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
                                }
                            }
                        }
                    }

                    
                    double paddedXmin = xmin - (xmin * .1);
                    double paddedXmax = xmax + (xmax * .1);
                    if (xmax != null) {
                        plot.SetAxisLimits(paddedXmin, paddedXmax, paddedXmin, paddedXmax);
                    }
                    int points = 50;
                    double dx = (plot.getXaxis().getMax()) / (points - 1);
                    float[] xy = new float[points];
                    for (int i = 0; i < points; i++) {
                        xy[i] = (float) (0 + (dx * i));
                    }
                    Line line = new Line(xy, xy, Color.black, PaintMode.COPY, PenStyle.DASH, 2);

                    plot.AddPlotObject(line, 1);
                    
                    if (stationDistance == null) {
                        stationDistance = 0.0;
                    }
                    String labelText = "StdDev(Before) = " + dfmt2.format(beforeStats.getStandardDeviation()) + " StdDev(After) = " + dfmt2.format(afterStats.getStandardDeviation())
                            + " Station Distance " + dfmt2.format(stationDistance) + " (km)";
                    stationPlot.getXaxis().setVisible(false);
                    stationPlot.getXaxis().setLabelText(labelText);
                    stationPlot.getXaxis().setVisible(true);
                }
            });
        }
    }
}
