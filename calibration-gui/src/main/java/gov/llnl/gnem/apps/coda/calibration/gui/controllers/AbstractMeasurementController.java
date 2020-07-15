/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
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
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.gui.util.Axis;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.PlotObjectClicked;
import llnl.gnem.core.gui.plotting.SymbolLegend;
import llnl.gnem.core.gui.plotting.SymbolLegend.SymbolTextPair;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.color.ColorMap;
import llnl.gnem.core.gui.plotting.color.ViridisColorMap;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.plotobject.Circle;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Square;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.SymbolDef;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;
import llnl.gnem.core.gui.plotting.plotobject.SymbolStyle;

public abstract class AbstractMeasurementController implements MapListeningController, RefreshableController, ScreenshotEnabledController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final int MAX_LEGEND_COLORS = 8;
    private static final Boolean SHOW_LEGEND = Boolean.TRUE;
    private static final Boolean HIDE_LEGEND = Boolean.FALSE;

    @FXML
    protected Tab resultsTab;

    @FXML
    protected SwingNode mwPlotSwingNode;
    private JMultiAxisPlot mwPlot;
    private JSubplot mwPlotFigure;
    private Line mwZeroLine;

    @FXML
    protected SwingNode stressPlotSwingNode;
    private JMultiAxisPlot stressPlot;
    private JSubplot stressPlotFigure;
    private Line stressZeroLine;

    @FXML
    protected SwingNode sdPlotSwingNode;
    private JMultiAxisPlot sdPlot;
    private JSubplot sdPlotFigure;

    protected StackPane spectraPlotPanel;

    @FXML
    protected ComboBox<String> evidCombo;

    @FXML
    protected TableView<MeasuredMwDetails> eventTable;

    @FXML
    protected TableView<LabeledPlotPoint> iconTable;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> evidCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> mwCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> stressCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMwCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredStressCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> valMwCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> valStressCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> mistfitCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredCornerFreqCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMwUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMwUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMwUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMwUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, Integer> iterationsCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, Integer> dataCountCol;

    @FXML
    protected TableColumn<LabeledPlotPoint, ImageView> iconCol;

    @FXML
    protected TableColumn<LabeledPlotPoint, String> stationCol;

    @FXML
    protected TextField eventTime;

    @FXML
    protected TextField eventLoc;

    protected List<SpectraMeasurement> spectralMeasurements = new ArrayList<>();
    private ObservableList<String> evids = FXCollections.observableArrayList();

    protected SpectraClient spectraClient;
    private ParameterClient paramClient;
    protected EventClient referenceEventClient;
    protected WaveformClient waveformClient;

    protected ObservableList<MeasuredMwDetails> mwParameters = FXCollections.observableArrayList();

    private ObservableList<LabeledPlotPoint> stationSymbols = FXCollections.observableArrayList();
    private BiConsumer<Boolean, String> eventSelectionCallback;
    private BiConsumer<Boolean, String> stationSelectionCallback;

    private Map<String, List<PlotPoint>> plotPointMap = new HashMap<>();
    private final List<PlotPoint> selectedPoints = new ArrayList<>();

    private SymbolStyleMapFactory symbolStyleMapFactory;
    private Map<String, PlotPoint> symbolStyleMap;

    private GeoMap mapImpl;

    private MapPlottingUtilities iconFactory;

    private MenuItem exclude;
    private MenuItem include;
    protected ContextMenu menu;

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private ColorMap colorMap = new ViridisColorMap();
    private final AtomicReference<Double> minFreq = new AtomicReference<>(1.0);
    private final AtomicReference<Double> maxFreq = new AtomicReference<>(-0.0);

    private final AtomicReference<Double> minY = new AtomicReference<>(1.0);
    private final AtomicReference<Double> maxY = new AtomicReference<>(-0.0);

    @FXML
    protected Button xAxisShrink;
    private boolean shouldXAxisShrink = false;

    @FXML
    protected Button yAxisShrink;
    private boolean shouldYAxisShrink = false;

    private boolean isVisible = false;

    protected List<SpectraPlotController> spectraControllers = new ArrayList<>(1);
    private EventBus bus;

    protected EventHandler<javafx.scene.input.MouseEvent> menuHideHandler = (evt) -> {
        if (MouseButton.SECONDARY != evt.getButton()) {
            menu.hide();
        }
    };

    //TODO: Break this up into components so this isn't so incredibly huge.
    public AbstractMeasurementController(SpectraClient spectraClient, ParameterClient paramClient, EventClient referenceEventClient, WaveformClient waveformClient, SymbolStyleMapFactory styleFactory,
            GeoMap map, MapPlottingUtilities iconFactory, EventBus bus) {
        this.spectraClient = spectraClient;
        this.paramClient = paramClient;
        this.referenceEventClient = referenceEventClient;
        this.waveformClient = waveformClient;
        this.symbolStyleMapFactory = styleFactory;
        this.mapImpl = map;
        this.bus = bus;
        this.iconFactory = iconFactory;

        eventSelectionCallback = (selected, eventId) -> {
            selectDataByCriteria(selected, eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            selectDataByCriteria(selected, stationId);
        };

        this.bus.register(this);
    }

    protected abstract String getDisplayName();

    protected abstract List<Spectra> getFitSpectra();

    protected abstract void setActive(List<Waveform> waveforms, List<Symbol> plotObjects, boolean active, BiConsumer<List<Symbol>, Boolean> activationFunc);

    protected abstract List<SpectraMeasurement> getSpectraData();

    protected abstract void runGuiUpdate(Runnable runnable) throws InvocationTargetException, InterruptedException;

    protected abstract List<MeasuredMwDetails> getEvents();

    public void initialize() {
        evidCombo.setItems(evids);

        configureAxisShrink(xAxisShrink, () -> {
            shouldXAxisShrink = !shouldXAxisShrink;
            return shouldXAxisShrink;
        }, Axis.X);

        configureAxisShrink(yAxisShrink, () -> {
            shouldYAxisShrink = !shouldYAxisShrink;
            return shouldYAxisShrink;
        }, Axis.Y);

        SwingUtilities.invokeLater(() -> {
            mwPlot = new JMultiAxisPlot();
            mwPlotFigure = mwPlot.addSubplot();

            mwPlot.getTitle().setText("Mw comparison");
            mwPlot.getXaxis().setLabelText("Measured");
            mwPlot.setYaxisVisibility(true);
            mwPlotSwingNode.setContent(mwPlot);

            mwPlotFigure.getYaxis().setLabelOffset(2.5d * mwPlot.getXaxis().getLabelOffset());
            mwPlotFigure.setAxisLimits(0.0, 10.0, 0.0, 10.0);
            mwPlotFigure.getYaxis().setLabelText("Reference (red), Validation (black)");

            stressPlot = new JMultiAxisPlot();
            stressPlotFigure = stressPlot.addSubplot();

            stressPlot.getTitle().setText("Stress comparison");
            stressPlot.getXaxis().setLabelText("Measured");
            stressPlot.setYaxisVisibility(true);
            stressPlotSwingNode.setContent(stressPlot);

            stressPlotFigure.getYaxis().setLabelOffset(2.5d * stressPlot.getXaxis().getLabelOffset());
            stressPlotFigure.setAxisLimits(0.0, 10.0, 0.0, 10.0);
            stressPlotFigure.getYaxis().setLabelText("Reference (red), Validation (black)");

            sdPlot = new JMultiAxisPlot();
            sdPlotFigure = sdPlot.addSubplot();

            sdPlot.getTitle().setText("Site correction overview");
            sdPlot.getXaxis().setLabelText("Frequency");
            sdPlot.setYaxisVisibility(true);
            sdPlotSwingNode.setContent(sdPlot);

            sdPlotFigure.getYaxis().setLabelOffset(2.5d * sdPlot.getXaxis().getLabelOffset());
            sdPlotFigure.setAxisLimits(0.0, 10.0, 0.0, 2.0);
            sdPlotFigure.getYaxis().setLabelText("Standard Deviation");

            int points = 50;
            double dx = 20.0 / (points - 1);
            float[] xy = new float[points];
            for (int i = 0; i < points; i++) {
                xy[i] = (float) (-5.0 + (dx * i));
            }
            mwZeroLine = new Line(xy, xy, Color.LIGHT_GRAY, PaintMode.COPY, PenStyle.DASH, 2);
            stressZeroLine = new Line(xy, xy, Color.LIGHT_GRAY, PaintMode.COPY, PenStyle.DASH, 2);
            mwPlotFigure.AddPlotObject(mwZeroLine);
            stressPlotFigure.AddPlotObject(stressZeroLine);
        });

        evidCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                refreshView();
            }
        });

        CellBindingUtils.attachTextCellFactoriesString(evidCol, MeasuredMwDetails::getEventId);
        evidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachTextCellFactories(mwCol, MeasuredMwDetails::getRefMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(stressCol, MeasuredMwDetails::getRefApparentStressInMpa, dfmt4);

        CellBindingUtils.attachTextCellFactories(valMwCol, MeasuredMwDetails::getValMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(valStressCol, MeasuredMwDetails::getValApparentStressInMpa, dfmt4);

        CellBindingUtils.attachTextCellFactories(measuredMwCol, MeasuredMwDetails::getMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredStressCol, MeasuredMwDetails::getApparentStressInMpa, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredCornerFreqCol, MeasuredMwDetails::getCornerFreq, dfmt4);

        CellBindingUtils.attachTextCellFactories(mistfitCol, MeasuredMwDetails::getMisfit, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq1LowCol, MeasuredMwDetails::getMw1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq1HighCol, MeasuredMwDetails::getMw1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq2LowCol, MeasuredMwDetails::getMw2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq2HighCol, MeasuredMwDetails::getMw2Max, dfmt4);

        iterationsCol.setCellValueFactory(x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getIterations).orElseGet(() -> 0))
                                                       .asObject());

        dataCountCol.setCellValueFactory(x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getDataCount).orElseGet(() -> 0))
                                                      .asObject());

        iconCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(pp -> {
            ImageView imView = new ImageView(SwingFXUtils.toFXImage(SymbolFactory.createSymbol(pp.getStyle(), 0, 0, 2, pp.getColor(), pp.getColor(), pp.getColor(), "", true, false, 10.0)
                                                                                 .getBufferedImage(256),
                                                                    null));
            imView.setFitHeight(12);
            imView.setFitWidth(12);
            return imView;
        }).orElseGet(() -> new ImageView())));

        stationCol.setCellValueFactory(x -> Bindings.createStringBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(LabeledPlotPoint::getLabel).orElseGet(String::new)));

        eventTable.setItems(mwParameters);
        iconTable.setItems(stationSymbols);

        iconCol.prefWidthProperty().bind(iconTable.widthProperty().multiply(0.3));
        stationCol.prefWidthProperty().bind(iconTable.widthProperty().multiply(0.7));

        menu = new ContextMenu();
        include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);
    }

    protected Object getPoint2D(Symbol sym) {
        return new Point2D.Double(sym.getXcenter(), sym.getYcenter());
    }

    private void showWaveformPopup(Waveform waveform) {
        bus.post(new WaveformSelectionEvent(waveform.getId()));
    }

    private void plotSpectra() {
        clearSpectraPlots();
        spectraControllers.forEach(spc -> spc.getSymbolMap().clear());
        plotPointMap.clear();
        List<SpectraMeasurement> filteredMeasurements;

        spectraControllers.forEach(spc -> {
            SpectralPlot plot = spc.getSpectralPlot();
            plot.setAutoCalculateXaxisRange(shouldXAxisShrink);
            if (!shouldXAxisShrink) {
                plot.setAllXlimits(minFreq.get(), maxFreq.get());
            }
        });

        final List<Spectra> fittingSpectra;
        if (evidCombo != null && evidCombo.getSelectionModel().getSelectedIndex() > 0) {
            filteredMeasurements = filterToEvent(evidCombo.getSelectionModel().getSelectedItem(), spectralMeasurements);
            fittingSpectra = getFitSpectra();
            Spectra referenceSpectra = spectraClient.getReferenceSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            fittingSpectra.add(referenceSpectra);
            Spectra validationSpectra = spectraClient.getValidationSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            fittingSpectra.add(validationSpectra);
            if (filteredMeasurements != null && filteredMeasurements.size() > 0 && filteredMeasurements.get(0).getWaveform() != null) {
                Event event = filteredMeasurements.get(0).getWaveform().getEvent();
                eventTime.setText("Date: " + DateTimeFormatter.ISO_INSTANT.format(event.getOriginTime().toInstant()));
                eventLoc.setText("Lat: " + dfmt4.format(event.getLatitude()) + " Lon: " + dfmt4.format(event.getLongitude()));
                eventTime.setVisible(true);
                eventLoc.setVisible(true);
            }
        } else {
            eventTime.setVisible(false);
            eventLoc.setVisible(false);
            filteredMeasurements = spectralMeasurements;
            fittingSpectra = null;
        }

        spectraControllers.forEach(spc -> {
            if (fittingSpectra != null) {
                spc.getSpectralPlot().plotXYdata(toPlotPoints(filteredMeasurements, spc.getDataFunc()), SHOW_LEGEND, fittingSpectra);
            } else {
                spc.getSpectralPlot().plotXYdata(toPlotPoints(filteredMeasurements, spc.getDataFunc()), HIDE_LEGEND);
            }
            spc.getSymbolMap().putAll(mapSpectraToPoint(filteredMeasurements, spc.getDataFunc()));
        });
        mapMeasurements(filteredMeasurements);

        minY.set(100.0);
        maxY.set(0.0);
        DoubleSummaryStatistics stats = filteredMeasurements.stream()
                                                            .filter(Objects::nonNull)
                                                            .map(spec -> spec.getPathAndSiteCorrected())
                                                            .filter(v -> v != 0.0)
                                                            .collect(Collectors.summarizingDouble(Double::doubleValue));
        maxY.set(stats.getMax() + .1);
        minY.set(stats.getMin() - .1);

        spectraControllers.forEach(spc -> spc.setYAxisResize(shouldYAxisShrink, minY.get(), maxY.get()));
    }

    private void mapMeasurements(List<SpectraMeasurement> measurements) {
        if (measurements != null) {
            mapImpl.addIcons(iconFactory.genIconsFromWaveforms(eventSelectionCallback,
                                                               stationSelectionCallback,
                                                               measurements.stream().map(m -> m.getWaveform()).filter(Objects::nonNull).collect(Collectors.toList())));
        }
    }

    protected void clearSpectraPlots() {
        spectraControllers.forEach(spc -> spc.getSpectralPlot().clearPlot());
    }

    private List<SpectraMeasurement> filterToEvent(String selectedItem, List<SpectraMeasurement> spectralMeasurements) {
        return spectralMeasurements.stream().filter(spec -> selectedItem.equalsIgnoreCase(spec.getWaveform().getEvent().getEventId())).collect(Collectors.toList());
    }

    private Map<Point2D.Double, SpectraMeasurement> mapSpectraToPoint(List<SpectraMeasurement> spectralMeasurements, Function<SpectraMeasurement, Double> func) {
        return spectralMeasurements.stream()
                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                   .collect(Collectors.toMap(spectra -> new Point2D.Double(Math.log10(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency())),
                                                                                           func.apply(spectra)),
                                                             Function.identity(),
                                                             (a, b) -> b,
                                                             HashMap::new));
    }

    private List<PlotPoint> toPlotPoints(List<SpectraMeasurement> spectralMeasurements, Function<SpectraMeasurement, Double> func) {
        List<PlotPoint> list = spectralMeasurements.stream()
                                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                                   .filter(spectra -> spectra != null && spectra.getWaveform() != null && spectra.getWaveform().getStream() != null
                                                           && spectra.getWaveform().getStream().getStation() != null)
                                                   .map(spectra -> {
                                                       String key = spectra.getWaveform().getStream().getStation().getStationName();
                                                       PlotPoint pp = getPlotPoint(key, spectra.getWaveform().isActive());
                                                       PlotPoint point = new LabeledPlotPoint(key,
                                                                                              new PlotPoint(Math.log10(centerFreq(spectra.getWaveform().getLowFrequency(),
                                                                                                                                  spectra.getWaveform().getHighFrequency())),
                                                                                                            func.apply(spectra),
                                                                                                            pp.getStyle(),
                                                                                                            pp.getColor()));
                                                       if (hasEventAndStation(spectra)) {
                                                           plotPointMap.computeIfAbsent(spectra.getWaveform().getEvent().getEventId(), k -> new ArrayList<>()).add(point);
                                                           plotPointMap.computeIfAbsent(spectra.getWaveform().getStream().getStation().getStationName(), k -> new ArrayList<>()).add(point);
                                                       }
                                                       return point;
                                                   })
                                                   .collect(Collectors.toList());
        return list;
    }

    private PlotPoint getPlotPoint(String key, boolean active) {
        PlotPoint pp = new PlotPoint(symbolStyleMap.get(key));
        if (!active) {
            pp.setColor(Color.GRAY);
        }
        return pp;
    }

    private boolean hasEventAndStation(SpectraMeasurement spectra) {
        return spectra != null && spectra.getWaveform() != null && spectra.getWaveform().getEvent() != null && spectra.getWaveform().getEvent().getEventId() != null
                && spectra.getWaveform().getStream() != null && spectra.getWaveform().getStream().getStation() != null && spectra.getWaveform().getStream().getStation().getStationName() != null;
    }

    private double centerFreq(Double lowFrequency, Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    protected void reloadData() {
        try {
            runGuiUpdate(() -> clearSpectraPlots());
            maxFreq.set(-0.0);
            minFreq.set(1.0);

            DoubleSummaryStatistics stats = paramClient.getSharedFrequencyBandParameters()
                                                       .filter(Objects::nonNull)
                                                       .collectList()
                                                       .block(Duration.of(10l, ChronoUnit.SECONDS))
                                                       .stream()
                                                       .map(sfb -> Math.log10(centerFreq(sfb.getLowFrequency(), sfb.getHighFrequency())))
                                                       .collect(Collectors.summarizingDouble(Double::doubleValue));
            maxFreq.set(stats.getMax());
            minFreq.set(stats.getMin());

            preloadData();
            spectralMeasurements.clear();
            spectralMeasurements.addAll(getSpectraData());

            runGuiUpdate(() -> {
                mwParameters.clear();
                mwPlotFigure.Clear();
                stressPlotFigure.Clear();
                sdPlotFigure.Clear();
                mwPlotFigure.AddPlotObject(mwZeroLine);
                stressPlotFigure.AddPlotObject(stressZeroLine);
                List<MeasuredMwDetails> evs = getEvents();

                double minMw = 10.0;
                double maxMw = 0.0;
                double minStress = 1.0;
                double maxStress = 0.0;
                for (MeasuredMwDetails ev : evs) {
                    mwParameters.add(ev);

                    if (ev.getMw() != null && ev.getMw() != 0.0) {
                        Double mw = ev.getMw();
                        if (mw < minMw) {
                            minMw = mw;
                        }
                        if (mw > maxMw) {
                            maxMw = mw;
                        }

                        Double valMw = ev.getValMw();
                        if (valMw != null && valMw != 0.0) {
                            if (valMw < minMw) {
                                minMw = valMw;
                            }
                            if (valMw > maxMw) {
                                maxMw = valMw;
                            }

                            Square valSym = new Square(mw, valMw, 2.2, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), true, false, 6.0);
                            mwPlotFigure.AddPlotObject(valSym);
                        }

                        if (ev.getRefMw() != null && ev.getRefMw() != 0.0) {
                            Double ref = ev.getRefMw();
                            if (ref < minMw) {
                                minMw = ref;
                            }
                            if (ref > maxMw) {
                                maxMw = ref;
                            }

                            Circle mwSym = new Circle(mw, ref, 2.0, Color.RED, Color.RED, Color.RED, ev.getEventId(), true, false, 6.0);
                            mwPlotFigure.AddPlotObject(mwSym);
                        }

                        Double stress = ev.getApparentStressInMpa();
                        Double refStress = ev.getRefApparentStressInMpa();

                        if (stress != null) {
                            if (stress < minStress) {
                                minStress = stress;
                            }
                            if (stress > maxStress) {
                                maxStress = stress;
                            }

                            if (refStress == null) {
                                refStress = 0.0;
                            }

                            Double valStress = ev.getValApparentStressInMpa();
                            if (valStress != null && valStress != 0.0) {
                                if (valStress < minStress) {
                                    minStress = valStress;
                                }
                                if (valStress > maxStress) {
                                    maxStress = valStress;
                                }

                                Square valSym = new Square(stress, valStress, 2.2, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), true, false, 6.0);
                                stressPlotFigure.AddPlotObject(valSym);
                            }

                            if (refStress != null && refStress != 0.0) {
                                if (refStress < minStress) {
                                    minStress = refStress;
                                }
                                if (refStress > maxStress) {
                                    maxStress = refStress;
                                }

                                Circle stressSym = new Circle(stress, refStress, 2.0, Color.RED, Color.RED, Color.RED, ev.getEventId(), true, false, 6.0);
                                stressPlotFigure.AddPlotObject(stressSym);
                            }
                        }
                    }
                }

                maxMw = maxMw + .1;
                minMw = minMw > maxMw ? minMw = maxMw - .1 : minMw - .1;

                mwPlotFigure.setAxisLimits(minMw, maxMw, minMw, maxMw);

                maxStress = maxStress + .1;
                minStress = minStress > maxStress ? minStress = maxStress - .1 : minStress - .1;

                stressPlotFigure.setAxisLimits(minStress, maxStress, minStress, maxStress);
            });

            symbolStyleMap = symbolStyleMapFactory.build(spectralMeasurements, new Function<SpectraMeasurement, String>() {
                @Override
                public String apply(SpectraMeasurement t) {
                    return t.getWaveform().getStream().getStation().getStationName();
                }
            });

            runGuiUpdate(() -> {
                stationSymbols.clear();
                stationSymbols.addAll(symbolStyleMap.entrySet().stream().map(e -> new LabeledPlotPoint(e.getKey(), e.getValue())).collect(Collectors.toList()));

                Platform.runLater(() -> {
                    evids.clear();
                    evids.add("All");
                    evids.addAll(spectralMeasurements.stream()
                                                     .map(spec -> spec.getWaveform().getEvent().getEventId())
                                                     .distinct()
                                                     .sorted(new MaybeNumericStringComparator())
                                                     .collect(Collectors.toList()));
                    eventTable.sort();
                });

                Map<String, Map<Double, SummaryStatistics>> evidStats = new HashMap<>();

                double minSite = 1E2;
                double maxSite = -1E2;
                double minFreq = 1E2;
                double maxFreq = -1E2;
                int minStations = 2;
                int maxStations = 3;

                for (SpectraMeasurement meas : spectralMeasurements) {
                    String evid = meas.getWaveform().getEvent().getEventId();
                    Double freq = centerFreq(meas.getWaveform());
                    evidStats.computeIfAbsent(evid, key -> new HashMap<>()).computeIfAbsent(freq, (key) -> new SummaryStatistics()).addValue(meas.getPathAndSiteCorrected());
                }

                for (Map<Double, SummaryStatistics> freqStats : evidStats.values()) {
                    for (Entry<Double, SummaryStatistics> entry : freqStats.entrySet()) {
                        double site = entry.getValue().getStandardDeviation();
                        if (entry.getValue() != null && entry.getValue().getN() > 1) {
                            if (maxStations < entry.getValue().getN()) {
                                maxStations = (int) entry.getValue().getN();
                            }
                            if (site < minSite) {
                                minSite = site;
                            }
                            if (site > maxSite) {
                                maxSite = site;
                            }
                            if (entry.getKey() < minFreq) {
                                minFreq = entry.getKey();
                            }
                            if (entry.getKey() > maxFreq) {
                                maxFreq = entry.getKey();
                            }
                        }
                    }
                }

                colorMap.setRange(minStations, maxStations);

                for (Map<Double, SummaryStatistics> freqStats : evidStats.values()) {
                    for (Entry<Double, SummaryStatistics> entry : freqStats.entrySet()) {
                        double site = entry.getValue().getStandardDeviation();
                        if (entry.getValue() != null && entry.getValue().getN() > 1) {
                            Color color = colorMap.getColor(entry.getValue().getN());
                            Circle sdSym = new Circle(entry.getKey(), site, 2.0, color, color, color, "", true, false, 6.0);
                            sdPlotFigure.AddPlotObject(sdSym);
                        }
                    }
                }

                maxSite = maxSite + .1;
                minSite = minSite > maxSite ? minSite = maxSite - .1 : minSite - .1;

                maxFreq = maxFreq + 5.0;
                minFreq = minFreq > maxFreq ? minFreq = maxFreq - .1 : minFreq - 1.0;

                sdPlotFigure.setAxisLimits(minFreq, maxFreq, minSite, maxSite);
                sdPlotFigure.AddPlotObject(createColorLegend(minStations, maxStations));
            });

            runGuiUpdate(() -> {
                mwPlot.repaint();
                stressPlot.repaint();
                sdPlot.repaint();
            });
        } catch (InvocationTargetException ex) {
            //nop
        } catch (InterruptedException ex) {
            log.warn("Swing interrupt during re-plotting of controller", ex);
            Thread.currentThread().interrupt();
        }
    }

    protected void preloadData() {
        // Placeholder to allow children to overload any pre-fetching needed before data calls
    }

    private PlotObject createColorLegend(int minVal, int maxVal) {
        int range = maxVal - minVal;
        int legendEntries = range;
        if (legendEntries > MAX_LEGEND_COLORS) {
            legendEntries = MAX_LEGEND_COLORS;
        }
        List<SymbolTextPair> legendSymbols = new ArrayList<>(range);

        Color color = colorMap.getColor(minVal);
        legendSymbols.add(new SymbolTextPair(Integer.toString(minVal), new SymbolDef(SymbolStyle.CIRCLE, 1.0, color, color)));
        if (legendEntries > 2) {
            int i = minVal + 1;
            while (i < legendEntries + minVal) {
                color = colorMap.getColor(i);
                legendSymbols.add(new SymbolTextPair(Integer.toString(i), new SymbolDef(SymbolStyle.CIRCLE, 1.0, color, color)));
                i = i + (range / (legendEntries - 1));
            }
        }
        color = colorMap.getColor(maxVal);
        legendSymbols.add(new SymbolTextPair(maxVal + "+", new SymbolDef(SymbolStyle.CIRCLE, 1.0, color, color)));
        return new SymbolLegend(legendSymbols, sdPlot.getTitle().getFontName(), 8.0, HorizPinEdge.RIGHT, VertPinEdge.TOP, 1, 1);
    }

    private Double centerFreq(Waveform waveform) {
        return ((waveform.getHighFrequency() - waveform.getLowFrequency()) / 2.0) + waveform.getLowFrequency();
    }

    @Override
    public void refreshView() {
        if (isVisible) {
            mapImpl.clearIcons();
            plotSpectra();
            Platform.runLater(() -> {
                spectraControllers.forEach(spc -> spc.getSpectralPlot().repaint());
            });
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> reloadData();
    }

    @Override
    public Consumer<File> getScreenshotFunction() {
        return (folder) -> {
            String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
            try {
                if (resultsTab.isSelected() && evidCombo.getValue() != null) {
                    SnapshotUtils.writePng(folder, new Pair<>(getDisplayName(), resultsTab.getContent()), timestamp);
                    spectraControllers.forEach(spc -> {
                        SpectralPlot plot = spc.getSpectralPlot();
                        try {
                            plot.exportSVG(folder + File.separator + getDisplayName() + "_" + plot.getTitle() + "_" + evidCombo.getValue() + timestamp + ".svg");
                        } catch (UnsupportedEncodingException | FileNotFoundException | SVGGraphics2DIOException e) {
                            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
                        }
                    });
                } else {
                    mwPlot.exportSVG(folder + File.separator + getDisplayName() + "_Mw" + timestamp + ".svg");
                    stressPlot.exportSVG(folder + File.separator + getDisplayName() + "Site_Stress" + timestamp + ".svg");
                    sdPlot.exportSVG(folder + File.separator + getDisplayName() + "_Station_Event_SD" + timestamp + ".svg");
                }
            } catch (UnsupportedEncodingException | FileNotFoundException | SVGGraphics2DIOException e) {
                log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
            }
        };
    }

    private void selectDataByCriteria(Boolean selected, String key) {
        List<PlotPoint> points = plotPointMap.get(key);
        if (selected) {
            selectPoints(points);
        } else {
            deselectPoints(points);
        }
    }

    private void selectPoints(List<PlotPoint> points) {
        if (points != null && !points.isEmpty()) {
            if (!selectedPoints.isEmpty()) {
                deselectPoints(selectedPoints);
            }
            selectedPoints.addAll(points);
            SwingUtilities.invokeLater(() -> {
                points.forEach(point -> {
                    selectPoint(point);
                });
            });
        }
    }

    private void deselectPoints(List<PlotPoint> selected) {
        if (selected != null && !selected.isEmpty()) {
            List<PlotPoint> points = new ArrayList<>(selected.size());
            points.addAll(selected);
            selectedPoints.clear();
            SwingUtilities.invokeLater(() -> {
                points.forEach(point -> {
                    deselectPoint(point);
                });
            });
        }
    }

    private void selectPoint(PlotPoint point) {
        runIfSymbolExists(point, (plot, xy) -> plot.selectPoint(xy));
    }

    private void deselectPoint(PlotPoint point) {
        runIfSymbolExists(point, (plot, xy) -> plot.deselectPoint(xy));
    }

    private void runIfSymbolExists(PlotPoint point, BiConsumer<SpectralPlot, Point2D.Double> xyPointFunction) {
        Point2D.Double xyPoint = new Point2D.Double(point.getX(), point.getY());
        spectraControllers.forEach(spc -> {
            Map<Point2D.Double, SpectraMeasurement> symbolMap = spc.getSymbolMap();
            SpectralPlot plot = spc.getSpectralPlot();
            boolean existsInPlot = symbolMap.containsKey(xyPoint);
            if (existsInPlot) {
                xyPointFunction.accept(plot, xyPoint);
            }
        });
    }

    protected void setSymbolsActive(List<Symbol> objs, Boolean active) {
        SwingUtilities.invokeLater(() -> {
            objs.forEach(sym -> {
                if (active) {
                    sym.setFillColor(sym.getEdgeColor());
                    sym.setEdgeColor(Color.BLACK);
                } else {
                    sym.setEdgeColor(sym.getFillColor());
                    sym.setFillColor(Color.GRAY);
                }
            });
            Platform.runLater(() -> {
                spectraControllers.forEach(spc -> spc.getSpectralPlot().repaint());
            });
        });
    }

    private void showContextMenu(List<Waveform> waveforms, List<Symbol> plotObjects, MouseEvent t, BiConsumer<List<Symbol>, Boolean> activationFunc) {
        Platform.runLater(() -> {
            include.setOnAction(evt -> setActive(waveforms, plotObjects, true, activationFunc));
            exclude.setOnAction(evt -> setActive(waveforms, plotObjects, false, activationFunc));
            menu.show(spectraPlotPanel, t.getXOnScreen(), t.getYOnScreen());
        });
    }

    @Subscribe
    private void listener(WaveformChangeEvent wce) {
        List<Long> nonNull = wce.getIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
        synchronized (spectralMeasurements) {
            Map<Long, SpectraMeasurement> activeMeasurements = spectralMeasurements.stream().collect(Collectors.toMap(x -> x.getWaveform().getId(), Function.identity()));
            if (wce.isAddOrUpdate()) {
                waveformClient.getWaveformMetadataFromIds(nonNull).collect(Collectors.toList()).block(Duration.ofSeconds(10l)).forEach(md -> {
                    SpectraMeasurement measurement = activeMeasurements.get(md.getId());
                    if (measurement != null) {
                        measurement.getWaveform().setActive(md.isActive());
                    }
                });
            } else if (wce.isDelete()) {
                nonNull.forEach(id -> {
                    SpectraMeasurement measurement = activeMeasurements.remove(id);
                    if (measurement != null) {
                        spectralMeasurements.remove(measurement);
                    }
                });
            }
        }
        refreshView();
    }

    protected void handlePlotObjectClicked(Object obj, Function<Symbol, SpectraMeasurement> measurementFunc) {
        if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
            PlotObjectClicked poc = (PlotObjectClicked) obj;
            PlotObject po = poc.getPlotObject();
            if (po instanceof Symbol) {
                SpectraMeasurement spectra = measurementFunc.apply((Symbol) po);
                if (spectra != null && spectra.getWaveform() != null) {
                    if (SwingUtilities.isLeftMouseButton(poc.getMouseEvent())) {
                        selectPoints(plotPointMap.get(((Symbol) po).getText()));
                        if (spectra != null) {
                            showWaveformPopup(spectra.getWaveform());
                        }
                        Platform.runLater(() -> menu.hide());
                    } else if (SwingUtilities.isRightMouseButton(poc.getMouseEvent())) {
                        showContextMenu(Collections.singletonList(spectra.getWaveform()), Collections.singletonList((Symbol) po), poc.getMouseEvent(), (objs, active) -> {
                            setSymbolsActive(objs, active);
                        });
                    }
                }
            }
        }
    }

    @Override
    public void setVisible(boolean visible) {
        isVisible = visible;
    }

    private void configureAxisShrink(Button axisShrinkButton, Supplier<Boolean> toggleAxisShrink, Axis axis) {
        Label axisShrinkOn = new Label("><");
        axisShrinkOn.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        axisShrinkOn.setPadding(Insets.EMPTY);
        Label axisShrinkOff = new Label("<>");
        axisShrinkOff.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        axisShrinkOff.setPadding(Insets.EMPTY);

        if (Axis.Y == axis) {
            axisShrinkOn.setRotate(90.0);
            axisShrinkOff.setRotate(90.0);
        }

        axisShrinkButton.setGraphic(axisShrinkOn);
        axisShrinkButton.prefHeightProperty().bind(evidCombo.heightProperty());
        axisShrinkButton.setPadding(new Insets(axisShrinkButton.getPadding().getTop(), 0, axisShrinkButton.getPadding().getBottom(), 0));

        axisShrinkButton.setOnAction(e -> {
            if (toggleAxisShrink.get()) {
                axisShrinkButton.setGraphic(axisShrinkOff);
            } else {
                axisShrinkButton.setGraphic(axisShrinkOn);
            }
            refreshView();
        });
    }

    protected Observer getPlotpointObserver(Supplier<Map<Point2D.Double, SpectraMeasurement>> symbolMapSupplier) {
        return new Observer() {
            @Override
            public void update(Observable observable, Object obj) {
                handlePlotObjectClicked(obj, sym -> symbolMapSupplier.get().get(getPoint2D(sym)));
            }
        };
    }

    @FXML
    private void clearRefEvents() {
        removeRefEvents(mwParameters);
    }

    @FXML
    private void removeRefEvents() {
        List<MeasuredMwDetails> evs = new ArrayList<>(eventTable.getSelectionModel().getSelectedIndices().size());
        eventTable.getSelectionModel().getSelectedIndices().forEach(i -> evs.add(mwParameters.get(i)));
        removeRefEvents(evs);
    }

    @FXML
    private void toggleValidationEvent() {
        List<MeasuredMwDetails> evs = new ArrayList<>(eventTable.getSelectionModel().getSelectedIndices().size());
        eventTable.getSelectionModel().getSelectedIndices().forEach(i -> evs.add(mwParameters.get(i)));
        if (evs != null && !evs.isEmpty()) {
            referenceEventClient.toggleValidationEventsByEventId(evs.stream().map(mwd -> mwd.getEventId()).distinct().collect(Collectors.toList()))
                                .doOnComplete(() -> Platform.runLater(() -> reloadData()))
                                .subscribe();
        }
    }

    private void removeRefEvents(List<MeasuredMwDetails> evs) {
        if (evs != null && !evs.isEmpty()) {
            referenceEventClient.removeReferenceEventsByEventId(evs.stream().map(mwd -> mwd.getEventId()).distinct().collect(Collectors.toList()))
                                .doOnSuccess((v) -> Platform.runLater(() -> reloadData()))
                                .subscribe();
        }
    }
}
