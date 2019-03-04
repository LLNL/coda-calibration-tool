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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;

@Component
public class ShapeController implements MapListeningController, RefreshableController {

    private enum SHAPE_DATA_TYPE {
        VELOCITY, BETA, GAMMA
    }

    private static final double VEL_Y_MIN = 0;
    private static final double VEL_Y_MAX = 6;
    private static final double BETA_Y_MIN = -0.12;
    private static final double BETA_Y_MAX = 0.01;
    private static final double GAMMA_Y_MIN = 0.0;
    private static final double GAMMA_Y_MAX = 3.5;
    private static final int XAXIS_MIN = 0;
    private static final int XAXIS_MAX = 5000;
    private static final double LINE_SEGMENTS = 250.0;

    private Map<FrequencyBand, List<PeakVelocityMeasurement>> velocityDistancePairsFreqMap = new HashMap<>();
    private Map<FrequencyBand, List<ShapeMeasurement>> shapeDistancePairsFreqMap = new HashMap<>();
    private Map<FrequencyBand, SharedFrequencyBandParameters> modelCurveMap = new TreeMap<>();
    private EventStaFreqStringComparator eventStaFreqComparator = new EventStaFreqStringComparator();

    @FXML
    private StackPane shape;

    @FXML
    private ComboBox<SHAPE_DATA_TYPE> dataTypeCombo;

    @FXML
    private ComboBox<FrequencyBand> frequencyBandCombo;

    @FXML
    private LineChart<Number, Number> mainFitPlot;

    @FXML
    private NumberAxis yAxis;

    @FXML
    private NumberAxis xAxis;

    private ParameterClient paramClient;
    private PeakVelocityClient velocityClient;
    private ShapeMeasurementClient shapeClient;
    private NumberFormat dfmt = NumberFormatFactory.twoDecimalOneLeadingZero();

    private ObservableList<Data<Number, Number>> modelData = FXCollections.observableArrayList();
    private Series<Number, Number> modelCurveSeries = new Series<>(modelData);

    private ObservableList<Data<Number, Number>> pointData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> selectedData = FXCollections.observableArrayList();
    private ObservableList<Data<Number, Number>> highlightedData = FXCollections.observableArrayList();
    private Series<Number, Number> pointSeries = new Series<>(pointData);
    private Series<Number, Number> selectedSeries = new Series<>(selectedData);
    private Series<Number, Number> highlightedSeries = new Series<>(highlightedData);
    private ObservableList<Series<Number, Number>> series = FXCollections.observableArrayList();
    private EventBus bus;
    private GeoMap mapImpl;
    private MapPlottingUtilities iconFactory;
    private Map<String, List<Data<Number, Number>>> pointMap = new HashMap<>();

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;
    private FrequencyBand selectedBand;

    @Autowired
    private ShapeController(ParameterClient paramClient, PeakVelocityClient velocityClient, ShapeMeasurementClient shapeClient, GeoMap map, MapPlottingUtilities iconFactory, EventBus bus) {
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
        this.shapeClient = shapeClient;
        this.bus = bus;
        this.mapImpl = map;
        this.iconFactory = iconFactory;

        eventSelectionCallback = (selected, eventId) -> {
            selectDataByCriteria(bus, selected, eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            selectDataByCriteria(bus, selected, stationId);
        };
    }

    private void selectDataByCriteria(EventBus bus, Boolean selected, String key) {
        List<Data<Number, Number>> selection = pointMap.get(key);
        if (selection != null && !selection.isEmpty()) {
            if (selected) {
                highlightedData.clear();
                selectedData.addAll(selection.stream().map(d -> new Data<Number, Number>(d.getXValue(), d.getYValue())).collect(Collectors.toList()));
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

        mainFitPlot.setAnimated(false);

        yAxis.setAutoRanging(false);
        xAxis.setAutoRanging(true);
        pointSeries.setName("Measurements");
        modelCurveSeries.setName("Model");

        dataTypeCombo.getItems().addAll(SHAPE_DATA_TYPE.values());

        dataTypeCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (oldValue != newValue) {
                adjustAxis(newValue);
                refreshView();
            }
        });

        dataTypeCombo.getSelectionModel().select(SHAPE_DATA_TYPE.VELOCITY);

        frequencyBandCombo.setCellFactory(fb -> getFBCell());
        frequencyBandCombo.setButtonCell(getFBCell());
        frequencyBandCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                selectedBand = newValue;
                refreshView();
            }
        });

        pointSeries.setData(pointData);
        modelCurveSeries.setData(modelData);
        selectedSeries.setData(selectedData);
        series.add(pointSeries);
        series.add(modelCurveSeries);
        series.add(selectedSeries);
        series.add(highlightedSeries);
        mainFitPlot.setData(series);
        mainFitPlot.setLegendVisible(false);
        modelCurveSeries.getNode().setMouseTransparent(true);
        selectedSeries.getNode().setMouseTransparent(true);
        highlightedSeries.getNode().setMouseTransparent(true);
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

    private void adjustAxis(SHAPE_DATA_TYPE newValue) {
        switch (newValue) {
        case VELOCITY:
            yAxis.setLowerBound(VEL_Y_MIN);
            yAxis.setUpperBound(VEL_Y_MAX);
            yAxis.setTickUnit((VEL_Y_MAX - VEL_Y_MIN) / 6.0);
            break;
        case BETA:
            yAxis.setLowerBound(BETA_Y_MIN);
            yAxis.setUpperBound(BETA_Y_MAX);
            yAxis.setTickUnit((BETA_Y_MAX - BETA_Y_MIN) / 6.0);
            break;
        case GAMMA:
            yAxis.setLowerBound(GAMMA_Y_MIN);
            yAxis.setUpperBound(GAMMA_Y_MAX);
            yAxis.setTickUnit((GAMMA_Y_MAX - GAMMA_Y_MIN) / 6.0);
            break;
        default:
            break;
        }
    }

    private void plotData() {
        FrequencyBand selectedFrequency = null;
        if (frequencyBandCombo != null && !frequencyBandCombo.isDisabled()) {
            selectedFrequency = frequencyBandCombo.getSelectionModel().getSelectedItem();
        }
        adjustAxis(dataTypeCombo.getSelectionModel().getSelectedItem());

        switch (dataTypeCombo.getSelectionModel().getSelectedItem()) {
        case VELOCITY:
            plotVelocity(selectedFrequency);
            break;
        case BETA:
            plotBeta(selectedFrequency);
            break;
        case GAMMA:
            plotGamma(selectedFrequency);
            break;
        default:
            break;
        }
    }

    private void plotGamma(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            Function<Number, Data<Number, Number>> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getGamma0() - (model.getGamma1() / (model.getGamma2() + ((double) i))));

            Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier = createValueSupplier(shapeDistancePairsFreqMap);
            Function<ShapeMeasurement, Waveform> mapFunc = ShapeMeasurement::getWaveform;
            mapValues(selectedFrequency, valueSupplier, mapFunc);
            plot(selectedFrequency, valueSupplier, val -> new Data<>(val.getDistance(), val.getMeasuredGamma()), val -> showWaveformPopup(val.getWaveform()), curvePointProducer, mapFunc);
        }
    }

    private void plotBeta(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            Function<Number, Data<Number, Number>> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getBeta0() - (model.getBeta1() / (model.getBeta2() + ((double) i))));

            Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier = createValueSupplier(shapeDistancePairsFreqMap);
            Function<ShapeMeasurement, Waveform> mapFunc = ShapeMeasurement::getWaveform;
            mapValues(selectedFrequency, valueSupplier, mapFunc);
            plot(selectedFrequency, valueSupplier, val -> new Data<>(val.getDistance(), val.getMeasuredBeta()), val -> showWaveformPopup(val.getWaveform()), curvePointProducer, mapFunc);
        }
    }

    private void plotVelocity(final FrequencyBand selectedFrequency) {
        if (selectedFrequency != null) {
            Function<Number, Data<Number, Number>> curvePointProducer = createCurvePointProducer(
                    selectedFrequency,
                        (model, i) -> model.getVelocity0() - (model.getVelocity1() / (model.getVelocity2() + ((double) i))));

            Function<FrequencyBand, List<PeakVelocityMeasurement>> valueSupplier = createValueSupplier(velocityDistancePairsFreqMap);
            Function<PeakVelocityMeasurement, Waveform> mapFunc = PeakVelocityMeasurement::getWaveform;
            mapValues(selectedFrequency, valueSupplier, mapFunc);
            plot(selectedFrequency, valueSupplier, val -> new Data<>(val.getDistance(), val.getVelocity()), val -> showWaveformPopup(val.getWaveform()), curvePointProducer, mapFunc);
        }
    }

    private <T> void mapValues(final FrequencyBand selectedFrequency, Function<FrequencyBand, List<T>> valueSupplier, Function<T, Waveform> mapFunc) {
        Optional.ofNullable(mapMeasurements(valueSupplier.apply(selectedFrequency).stream().map(mapFunc).filter(Objects::nonNull).collect(Collectors.toList()))).ifPresent(mapImpl::addIcons);
    }

    private <T> Function<FrequencyBand, List<T>> createValueSupplier(Map<FrequencyBand, List<T>> valueMap) {
        Function<FrequencyBand, List<T>> valueSupplier;
        if (valueMap != null) {
            valueSupplier = freq -> Optional.ofNullable(valueMap.get(freq)).orElse(new ArrayList<>(0));
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

    private <T> void plot(final FrequencyBand selectedFrequency, Function<FrequencyBand, List<T>> valueSupplier, Function<T, Data<Number, Number>> dataPointSupplier,
            Function<T, EventHandler<Event>> mouseClickedCallback, Function<Number, Data<Number, Number>> curveProducer, Function<T, Waveform> mapFunc) {
        if (selectedFrequency != null) {
            pointMap.clear();
            modelData.clear();
            pointData.clear();
            selectedData.clear();
            highlightedData.clear();

            AtomicReference<Integer> minX = new AtomicReference<Integer>(0);
            AtomicReference<Integer> maxX = new AtomicReference<Integer>(0);
            AtomicReference<Double> minY = new AtomicReference<Double>(0d);
            AtomicReference<Double> maxY = new AtomicReference<Double>(0d);

            Optional.ofNullable(valueSupplier.apply(selectedFrequency)).ifPresent(values -> values.forEach(val -> {
                Data<Number, Number> data = dataPointSupplier.apply(val);
                pointData.add(data);
                if (data.getXValue().doubleValue() > maxX.get()) {
                    maxX.set(data.getXValue().intValue());
                }
                if (data.getXValue().doubleValue() < minX.get()) {
                    minX.set(data.getXValue().intValue());
                }

                if (data.getXValue().doubleValue() > maxY.get()) {
                    maxY.set(data.getYValue().doubleValue());
                }
                if (data.getYValue().doubleValue() < minY.get()) {
                    minY.set(data.getYValue().doubleValue());
                }

                data.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedCallback.apply(val));
                data.getNode().toBack();
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
            }));

            if (maxX.get() > XAXIS_MAX || maxX.get() == XAXIS_MIN) {
                maxX.set(XAXIS_MAX);
            }

            if (yAxis.getLowerBound() > minY.get()) {
                yAxis.setLowerBound(1.1 * minY.get());
            }

            if (yAxis.getUpperBound() < maxY.get()) {
                yAxis.setUpperBound(1.1 * maxY.get());
            }

            if (curveProducer != null) {
                for (double i = minX.get(); i <= maxX.get(); i = i + ((maxX.get() - minX.get()) / LINE_SEGMENTS)) {
                    Data<Number, Number> data = curveProducer.apply(i);
                    modelData.add(data);
                    data.getNode().setMouseTransparent(true);
                }
            }
        }
    }

    private Collection<Icon> mapMeasurements(List<Waveform> waveforms) {
        return waveforms.stream().flatMap(w -> {
            List<Icon> icons = new ArrayList<>();
            if (w.getStream() != null && w.getStream().getStation() != null) {
                Station station = w.getStream().getStation();
                icons.add(iconFactory.createStationIcon(station).setIconSelectionCallback(stationSelectionCallback));
            }
            if (w.getEvent() != null) {
                icons.add(iconFactory.createEventIcon(w.getEvent()).setIconSelectionCallback(eventSelectionCallback));
            }
            return icons.stream();
        }).collect(Collectors.toList());
    }

    private EventHandler<Event> showWaveformPopup(Waveform waveform) {
        return event -> {
            highlightedData.clear();
            List<Data<Number, Number>> selection = pointMap.get(waveform.getEvent().getEventId() + waveform.getStream().getStation().getStationName());
            if (selection != null && selection.size() == 1) {
                highlightedData.add(new Data<Number, Number>(selection.get(0).getXValue(), selection.get(0).getYValue()));
                highlightedData.forEach(x -> x.getNode().setMouseTransparent(true));
            }
            bus.post(new WaveformSelectionEvent(waveform.getId()));
        };
    }

    private void reloadData() {
        velocityDistancePairsFreqMap.clear();
        shapeDistancePairsFreqMap.clear();
        modelCurveMap.clear();

        frequencyBandCombo.getItems().clear();

        paramClient.getSharedFrequencyBandParameters().subscribe(sfb -> modelCurveMap.put(new FrequencyBand(sfb.getLowFrequency(), sfb.getHighFrequency()), sfb));
        velocityDistancePairsFreqMap.putAll(
                velocityClient.getMeasuredPeakVelocities()
                              .toStream()
                              .filter(Objects::nonNull)
                              .filter(pvm -> pvm.getWaveform() != null)
                              .collect(Collectors.groupingBy(pvm -> new FrequencyBand(pvm.getWaveform().getLowFrequency(), pvm.getWaveform().getHighFrequency()))));

        shapeDistancePairsFreqMap.putAll(
                shapeClient.getMeasuredShapes()
                           .toStream()
                           .filter(Objects::nonNull)
                           .filter(shape -> shape.getWaveform() != null)
                           .collect(Collectors.groupingBy(shape -> new FrequencyBand(shape.getWaveform().getLowFrequency(), shape.getWaveform().getHighFrequency()))));

        frequencyBandCombo.getItems().addAll(modelCurveMap.keySet());
        frequencyBandCombo.getSelectionModel().selectFirst();

        refreshView();
    }

    @Override
    public void refreshView() {
        mapImpl.clearIcons();
        plotData();
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> reloadData();
    }

}
