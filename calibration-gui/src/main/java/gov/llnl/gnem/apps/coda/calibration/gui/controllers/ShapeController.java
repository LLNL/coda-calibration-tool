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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.calibration.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
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
public class ShapeController {

    private enum SHAPE_DATA_TYPE {
        VELOCITY, BETA, GAMMA
    }

    private static final double VEL_Y_MIN = 0;
    private static final double VEL_Y_MAX = 6;
    private static final double BETA_Y_MIN = -0.06;
    private static final double BETA_Y_MAX = 0.01;
    private static final double GAMMA_Y_MIN = 0.0;
    private static final double GAMMA_Y_MAX = 3.5;
    private static final int XAXIS_MIN = 0;
    private static final int XAXIS_MAX = 1600;
    private static final double LINE_SEGMENTS = 250.0;

    private Map<FrequencyBand, List<PeakVelocityMeasurement>> velocityDistancePairsFreqMap = new HashMap<>();
    private Map<FrequencyBand, List<ShapeMeasurement>> shapeDistancePairsFreqMap = new HashMap<>();
    private Map<FrequencyBand, SharedFrequencyBandParameters> modelCurveMap = new TreeMap<>();

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
    private Series<Number, Number> pointSeries = new Series<>(pointData);
    private ObservableList<Series<Number, Number>> series = FXCollections.observableArrayList();
    private EventBus bus;

    @Autowired
    private ShapeController(ParameterClient paramClient, PeakVelocityClient velocityClient, ShapeMeasurementClient shapeClient, EventBus bus) {
        this.paramClient = paramClient;
        this.velocityClient = velocityClient;
        this.shapeClient = shapeClient;
        this.bus = bus;
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
                plotData();
            }
        });

        dataTypeCombo.getSelectionModel().select(SHAPE_DATA_TYPE.VELOCITY);

        frequencyBandCombo.setCellFactory(fb -> getFBCell());
        frequencyBandCombo.setButtonCell(getFBCell());
        frequencyBandCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                plotData();
            }
        });

        pointSeries.setData(pointData);
        modelCurveSeries.setData(modelData);
        series.add(pointSeries);
        series.add(modelCurveSeries);
        mainFitPlot.setData(series);
        modelCurveSeries.getNode().setMouseTransparent(true);
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
        Function<Number, Data<Number, Number>> curvePointProducer = null;

        if (modelCurveMap != null && selectedFrequency != null && modelCurveMap.containsKey(selectedFrequency)) {
            SharedFrequencyBandParameters model = modelCurveMap.get(selectedFrequency);
            curvePointProducer = i -> new Data<>(i, model.getGamma0() - (model.getGamma1() / (model.getGamma2() + ((double) i))));
        }

        Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier;

        if (velocityDistancePairsFreqMap != null) {
            valueSupplier = freq -> shapeDistancePairsFreqMap.get(freq);
        } else {
            valueSupplier = freq -> new ArrayList<>(0);
        }

        plot(selectedFrequency, valueSupplier, val -> new Data<>(val.getDistance(), val.getMeasuredGamma()), val -> showWaveformPopup(val.getWaveform()), curvePointProducer);
    }

    private void plotBeta(final FrequencyBand selectedFrequency) {
        Function<Number, Data<Number, Number>> curvePointProducer = null;

        if (modelCurveMap != null && selectedFrequency != null && modelCurveMap.containsKey(selectedFrequency)) {
            SharedFrequencyBandParameters model = modelCurveMap.get(selectedFrequency);
            curvePointProducer = i -> new Data<>(i, model.getBeta0() - (model.getBeta1() / (model.getBeta2() + ((double) i))));
        }

        Function<FrequencyBand, List<ShapeMeasurement>> valueSupplier;

        if (velocityDistancePairsFreqMap != null) {
            valueSupplier = freq -> shapeDistancePairsFreqMap.get(freq);
        } else {
            valueSupplier = freq -> new ArrayList<>(0);
        }

        plot(selectedFrequency, valueSupplier, val -> new Data<>(val.getDistance(), val.getMeasuredBeta()), val -> showWaveformPopup(val.getWaveform()), curvePointProducer);
    }

    private void plotVelocity(final FrequencyBand selectedFrequency) {
        Function<Number, Data<Number, Number>> curvePointProducer = null;

        if (modelCurveMap != null && selectedFrequency != null && modelCurveMap.containsKey(selectedFrequency)) {
            SharedFrequencyBandParameters model = modelCurveMap.get(selectedFrequency);
            curvePointProducer = i -> new Data<>(i, model.getVelocity0() - (model.getVelocity1() / (model.getVelocity2() + ((double) i))));
        }

        Function<FrequencyBand, List<PeakVelocityMeasurement>> valueSupplier;

        if (velocityDistancePairsFreqMap != null) {
            valueSupplier = freq -> velocityDistancePairsFreqMap.get(freq);
        } else {
            valueSupplier = freq -> new ArrayList<>(0);
        }

        plot(selectedFrequency, valueSupplier, val -> new Data<>(val.getDistance(), val.getVelocity()), val -> showWaveformPopup(val.getWaveform()), curvePointProducer);
    }

    private <T> void plot(final FrequencyBand selectedFrequency, Function<FrequencyBand, List<T>> valueSupplier, Function<T, Data<Number, Number>> dataPointSupplier,
            Function<T, EventHandler<Event>> mouseClickedCallback, Function<Number, Data<Number, Number>> curveProducer) {
        if (selectedFrequency != null) {
            modelData.clear();
            pointData.clear();

            AtomicReference<Integer> min = new AtomicReference<Integer>(XAXIS_MIN);
            AtomicReference<Integer> max = new AtomicReference<Integer>(XAXIS_MIN);

            Optional.ofNullable(valueSupplier.apply(selectedFrequency)).ifPresent(values -> values.forEach(val -> {
                Data<Number, Number> data = dataPointSupplier.apply(val);
                pointData.add(data);
                if (data.getXValue().doubleValue() > max.get()) {
                    max.set(data.getXValue().intValue());
                }
                if (data.getXValue().doubleValue() < min.get()) {
                    min.set(data.getXValue().intValue());
                }
                data.getNode().addEventHandler(MouseEvent.MOUSE_CLICKED, mouseClickedCallback.apply(val));
            }));

            if (max.get() > XAXIS_MAX || max.get() == XAXIS_MIN) {
                max.set(XAXIS_MAX);
            }

            if (curveProducer != null) {
                for (double i = min.get(); i <= max.get(); i = i + ((max.get() - min.get()) / LINE_SEGMENTS)) {
                    Data<Number, Number> data = curveProducer.apply(i);
                    modelData.add(data);
                    data.getNode().setMouseTransparent(true);
                }
            }
        }
    }

    private EventHandler<Event> showWaveformPopup(Waveform waveform) {
        return event -> {
            bus.post(new WaveformSelectionEvent(waveform.getId().toString()));
        };
    }

    @FXML
    private void reloadData(ActionEvent e) {
        reloadData();
    }

    private void reloadData() {
        velocityDistancePairsFreqMap.clear();
        shapeDistancePairsFreqMap.clear();
        modelCurveMap.clear();

        frequencyBandCombo.getItems().clear();

        paramClient.getSharedFrequencyBandParameters().subscribe(sfb -> modelCurveMap.put(new FrequencyBand(sfb.getLowFrequency(), sfb.getHighFrequency()), sfb));
        velocityDistancePairsFreqMap.putAll(velocityClient.getMeasuredPeakVelocities()
                                                          .toStream()
                                                          .filter(Objects::nonNull)
                                                          .filter(pvm -> pvm.getWaveform() != null)
                                                          .collect(Collectors.groupingBy(pvm -> new FrequencyBand(pvm.getWaveform().getLowFrequency(), pvm.getWaveform().getHighFrequency()))));

        shapeDistancePairsFreqMap.putAll(shapeClient.getMeasuredShapes()
                                                    .toStream()
                                                    .filter(Objects::nonNull)
                                                    .filter(shape -> shape.getWaveform() != null)
                                                    .collect(Collectors.groupingBy(shape -> new FrequencyBand(shape.getWaveform().getLowFrequency(), shape.getWaveform().getHighFrequency()))));

        frequencyBandCombo.getItems().addAll(modelCurveMap.keySet());
        frequencyBandCombo.getSelectionModel().selectFirst();

        plotData();
    }
}
