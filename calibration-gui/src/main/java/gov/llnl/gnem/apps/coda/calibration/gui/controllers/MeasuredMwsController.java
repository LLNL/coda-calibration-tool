/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.File;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.gui.util.FileDialogs;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwReportByEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.controllers.ProgressGui;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressListener;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import llnl.gnem.core.gui.plotting.PlotObjectClicked;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;

//TODO: This is effectively a subset of SiteController. Revisit and see if we can factor out common functions into a third shared class at some point.
@Component
public class MeasuredMwsController implements MapListeningController, RefreshableController {

    private static final Logger log = LoggerFactory.getLogger(MeasuredMwsController.class);

    private static final String X_AXIS_LABEL = "center freq";

    @FXML
    private StackPane measuredMws;

    @FXML
    private SwingNode fitPlotSwingNode;
    private SpectralPlot fitPlot;

    @FXML
    private ComboBox<String> evidCombo;

    @FXML
    private TableView<MeasuredMwDetails> eventTable;

    @FXML
    private TableView<LabeledPlotPoint> iconTable;

    @FXML
    private TableColumn<MeasuredMwDetails, String> evidCol;

    @FXML
    private TableColumn<MeasuredMwDetails, String> measuredMwCol;

    @FXML
    private TableColumn<MeasuredMwDetails, String> measuredStressCol;

    @FXML
    private TableColumn<MeasuredMwDetails, Integer> dataCountCol;

    @FXML
    private TableColumn<LabeledPlotPoint, ImageView> iconCol;

    @FXML
    private TableColumn<LabeledPlotPoint, String> stationCol;

    @FXML
    private TextField eventTime;

    @FXML
    private TextField eventLoc;

    private SpectraClient spectraClient;
    private ParameterClient paramClient;
    private CalibrationClient calibrationClient;
    private ParamExporter paramExporter;
    private WaveformClient waveformClient;
    private GeoMap mapImpl;
    private EventBus bus;
    private MapPlottingUtilities iconFactory;

    private List<SpectraMeasurement> spectralMeasurements = new ArrayList<>();
    private Map<String, Spectra> fitSpectra = new HashMap<>();
    private ObservableList<String> evids = FXCollections.observableArrayList();
    private ObservableList<MeasuredMwDetails> mwParameters = FXCollections.observableArrayList();
    private ObservableList<LabeledPlotPoint> stationSymbols = FXCollections.observableArrayList();

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;

    protected Map<Point2D.Double, SpectraMeasurement> fitSymbolMap = new ConcurrentHashMap<>();

    private Map<String, List<PlotPoint>> plotPointMap = new HashMap<>();
    private final List<PlotPoint> selectedPoints = new ArrayList<>();

    private SymbolStyleMapFactory symbolStyleMapFactory;

    private Map<String, PlotPoint> symbolStyleMap;

    private MenuItem exclude;
    private MenuItem include;
    private ContextMenu menu;

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private ProgressGui progressGui;

    private final AtomicReference<Double> minFreq = new AtomicReference<>(1.0);
    private final AtomicReference<Double> maxFreq = new AtomicReference<>(-0.0);

    @FXML
    private Button xAxisShrink;
    private boolean shouldXAxisShrink = false;
    private Label xAxisShrinkOn;
    private Label xAxisShrinkOff;

    private ThreadPoolExecutor exec = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
        Thread thread = new Thread(r);
        thread.setName("MeasureMwController");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.DiscardPolicy());

    private boolean isVisible = false;

    @Autowired
    private MeasuredMwsController(SpectraClient spectraClient, ParameterClient paramClient, WaveformClient waveformClient, SymbolStyleMapFactory styleFactory, GeoMap map,
            MapPlottingUtilities iconFactory, EventBus bus, ParamExporter paramExporter, CalibrationClient calibrationClient) {
        this.spectraClient = spectraClient;
        this.paramClient = paramClient;
        this.waveformClient = waveformClient;
        this.symbolStyleMapFactory = styleFactory;
        this.mapImpl = map;
        this.bus = bus;
        this.iconFactory = iconFactory;
        this.calibrationClient = calibrationClient;
        this.paramExporter = paramExporter;

        eventSelectionCallback = (selected, eventId) -> {
            selectDataByCriteria(selected, eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            selectDataByCriteria(selected, stationId);
        };

        this.bus.register(this);
    }

    @FXML
    public void initialize() {
        ProgressMonitor pm = new ProgressMonitor("Measuring Mws", new ProgressListener() {
            @Override
            public double getProgress() {
                return -1d;
            }
        });
        progressGui = new ProgressGui();
        progressGui.addProgressMonitor(pm);

        progressGui.initModality(Modality.NONE);
        progressGui.setAlwaysOnTop(true);

        evidCombo.setItems(evids);

        xAxisShrinkOn = new Label("><");
        xAxisShrinkOn.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        xAxisShrinkOn.setPadding(Insets.EMPTY);
        xAxisShrinkOff = new Label("<>");
        xAxisShrinkOff.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        xAxisShrinkOff.setPadding(Insets.EMPTY);
        xAxisShrink.setGraphic(xAxisShrinkOn);
        double topPad = xAxisShrink.getPadding().getTop();
        double bottomPad = xAxisShrink.getPadding().getBottom();
        xAxisShrink.setPadding(new Insets(topPad, 0, bottomPad, 0));
        xAxisShrink.prefHeightProperty().bind(evidCombo.heightProperty());
        xAxisShrink.setOnAction(e -> {
            shouldXAxisShrink = !shouldXAxisShrink;
            if (shouldXAxisShrink) {
                xAxisShrink.setGraphic(xAxisShrinkOff);
            } else {
                xAxisShrink.setGraphic(xAxisShrinkOn);
            }
            refreshView();
        });

        SwingUtilities.invokeLater(() -> {
            fitPlot = new SpectralPlot();
            fitPlot.addPlotObjectObserver(new Observer() {
                @Override
                public void update(Observable observable, Object obj) {
                    handlePlotObjectClicked(obj, sym -> fitSymbolMap.get(getPoint2D(sym)));
                }
            });
            fitPlotSwingNode.setContent(fitPlot);

            fitPlot.setLabels("Mw Spectra", X_AXIS_LABEL, "log10(amplitude)");
            fitPlot.setYaxisVisibility(true);
        });

        evidCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                refreshView();
            }
        });

        CellBindingUtils.attachTextCellFactoriesString(evidCol, MeasuredMwDetails::getEventId);
        evidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachTextCellFactories(measuredMwCol, MeasuredMwDetails::getMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredStressCol, MeasuredMwDetails::getApparentStressInMpa, dfmt4);

        dataCountCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getDataCount).orElseGet(() -> 0)).asObject());

        iconCol.setCellValueFactory(x -> Bindings.createObjectBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(pp -> {
            ImageView imView = new ImageView(SwingFXUtils.toFXImage(
                    SymbolFactory.createSymbol(pp.getStyle(), 0, 0, 2, pp.getColor(), pp.getColor(), pp.getColor(), "", true, false, 10.0).getBufferedImage(256),
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

        EventHandler<javafx.scene.input.MouseEvent> menuHideHandler = (evt) -> {
            if (MouseButton.SECONDARY != evt.getButton()) {
                menu.hide();
            }
        };
        fitPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);

        bus.register(this);
    }

    protected Object getPoint2D(Symbol sym) {
        return new Point2D.Double(sym.getXcenter(), sym.getYcenter());
    }

    private void showWaveformPopup(Waveform waveform) {
        bus.post(new WaveformSelectionEvent(waveform.getId()));
    }

    private void plotSpectra() {
        clearSpectraPlots();
        fitSymbolMap.clear();
        plotPointMap.clear();
        List<SpectraMeasurement> filteredMeasurements;

        fitPlot.setAutoCalculateXaxisRange(shouldXAxisShrink);
        if (!shouldXAxisShrink) {
            fitPlot.setAllXlimits(minFreq.get(), maxFreq.get());
        }

        if (evidCombo != null && evidCombo.getSelectionModel().getSelectedIndex() > 0) {
            filteredMeasurements = filterToEvent(evidCombo.getSelectionModel().getSelectedItem(), spectralMeasurements);
            Spectra referenceSpectra = spectraClient.getReferenceSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            Spectra theoreticalSpectra = fitSpectra.get(evidCombo.getSelectionModel().getSelectedItem());
            fitPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected), Boolean.TRUE, referenceSpectra, theoreticalSpectra);
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
            fitPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected), Boolean.FALSE);
        }
        fitSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected));
        mapMeasurements(filteredMeasurements);
    }

    private void mapMeasurements(List<SpectraMeasurement> measurements) {
        if (measurements != null) {
            mapImpl.addIcons(
                    iconFactory.genIconsFromWaveforms(
                            eventSelectionCallback,
                                stationSelectionCallback,
                                measurements.stream().map(m -> m.getWaveform()).filter(Objects::nonNull).collect(Collectors.toList())));
        }
    }

    private void clearSpectraPlots() {
        fitPlot.clearPlot();
    }

    private List<SpectraMeasurement> filterToEvent(String selectedItem, List<SpectraMeasurement> spectralMeasurements) {
        return spectralMeasurements.stream().filter(spec -> selectedItem.equalsIgnoreCase(spec.getWaveform().getEvent().getEventId())).collect(Collectors.toList());
    }

    private Map<Point2D.Double, SpectraMeasurement> mapSpectraToPoint(List<SpectraMeasurement> spectralMeasurements, Function<SpectraMeasurement, Double> func) {
        return spectralMeasurements.stream()
                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                   .collect(
                                           Collectors.toMap(
                                                   spectra -> new Point2D.Double(Math.log10(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency())),
                                                                                 func.apply(spectra)),
                                                       Function.identity(),
                                                       (a, b) -> b,
                                                       HashMap::new));
    }

    private List<PlotPoint> toPlotPoints(List<SpectraMeasurement> spectralMeasurements, Function<SpectraMeasurement, Double> func) {
        List<PlotPoint> list = spectralMeasurements.stream()
                                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                                   .filter(
                                                           spectra -> spectra != null
                                                                   && spectra.getWaveform() != null
                                                                   && spectra.getWaveform().getStream() != null
                                                                   && spectra.getWaveform().getStream().getStation() != null)
                                                   .map(spectra -> {
                                                       String key = spectra.getWaveform().getStream().getStation().getStationName();
                                                       PlotPoint pp = getPlotPoint(key, spectra.getWaveform().isActive());
                                                       PlotPoint point = new LabeledPlotPoint(key,
                                                                                              new PlotPoint(Math.log10(
                                                                                                      centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency())),
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
        return spectra != null
                && spectra.getWaveform() != null
                && spectra.getWaveform().getEvent() != null
                && spectra.getWaveform().getEvent().getEventId() != null
                && spectra.getWaveform().getStream() != null
                && spectra.getWaveform().getStream().getStation() != null
                && spectra.getWaveform().getStream().getStation().getStationName() != null;
    }

    private double centerFreq(Double lowFrequency, Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    private void reloadData() {
        try {
            exec.submit(() -> {
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

                MeasuredMwReportByEvent mfs = calibrationClient.makeMwMeasurements(Boolean.TRUE)
                                                               .doOnError(err -> log.trace(err.getMessage(), err))
                                                               .doAfterTerminate(() -> Platform.runLater(() -> progressGui.hide()))
                                                               .block(Duration.of(10l, ChronoUnit.SECONDS));

                fitSpectra.putAll(mfs.getFitSpectra());
                spectralMeasurements.addAll(
                        mfs.getSpectraMeasurements()
                           .values()
                           .stream()
                           .flatMap(x -> x.stream())
                           .filter(Objects::nonNull)
                           .filter(spectra -> spectra.getWaveform() != null && spectra.getWaveform().getEvent() != null && spectra.getWaveform().getStream() != null)
                           .map(md -> new SpectraMeasurement(md))
                           .collect(Collectors.toList()));

                symbolStyleMap = symbolStyleMapFactory.build(spectralMeasurements, new Function<SpectraMeasurement, String>() {
                    @Override
                    public String apply(SpectraMeasurement t) {
                        return t.getWaveform().getStream().getStation().getStationName();
                    }
                });

                List<LabeledPlotPoint> symbols = symbolStyleMap.entrySet().stream().map(e -> new LabeledPlotPoint(e.getKey(), e.getValue())).collect(Collectors.toList());
                List<String> uniqueEvids = mfs.getSpectraMeasurements().keySet().stream().distinct().sorted(new MaybeNumericStringComparator()).collect(Collectors.toList());
                Collection<MeasuredMwDetails> mwDetails = mfs.getMeasuredMwDetails().values();

                Platform.runLater(() -> {
                    mwParameters.clear();
                    stationSymbols.clear();
                    stationSymbols.addAll(symbols);
                    evids.addAll(uniqueEvids);
                    mwParameters.addAll(mwDetails);
                    eventTable.sort();
                });
            });

            clearSpectraPlots();
            spectralMeasurements.clear();
            fitSpectra.clear();
            evids.clear();
            evids.add("All");

            progressGui.show();
            progressGui.toFront();
        } catch (RejectedExecutionException e) {
            /*nop*/
        }
    }

    public void exportMws() {
        Platform.runLater(() -> {
            File file = FileDialogs.openFileSaveDialog("Measured_Mws", ".json", measuredMws.getScene().getWindow());
            if (file != null && FileDialogs.ensureFileIsWritable(file)) {
                String filePath = file.getAbsolutePath();
                paramExporter.writeMeasuredMws(Paths.get(FilenameUtils.getFullPath(filePath)), FilenameUtils.getName(filePath), mwParameters);
            }
        });
    }

    @Override
    public void refreshView() {
        if (isVisible) {
            mapImpl.clearIcons();
            plotSpectra();
            Platform.runLater(() -> {
                fitPlot.repaint();
            });
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> reloadData();
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
        Point2D.Double xyPoint = new Point2D.Double(point.getX(), point.getY());
        boolean existsInPlot = fitSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            fitPlot.selectPoint(xyPoint);
        }
    }

    private void deselectPoint(PlotPoint point) {
        Point2D.Double xyPoint = new Point2D.Double(point.getX(), point.getY());
        boolean existsInPlot = fitSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            fitPlot.deselectPoint(xyPoint);
        }
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
                fitPlot.repaint();
            });
        });
    }

    private void showContextMenu(List<Waveform> waveforms, List<Symbol> plotObjects, MouseEvent t, BiConsumer<List<Symbol>, Boolean> activationFunc) {
        Platform.runLater(() -> {
            include.setOnAction(evt -> setActive(waveforms, plotObjects, true, activationFunc));
            exclude.setOnAction(evt -> setActive(waveforms, plotObjects, false, activationFunc));
            menu.show(fitPlotSwingNode, t.getXOnScreen(), t.getYOnScreen());
        });
    }

    private void setActive(List<Waveform> waveforms, List<Symbol> plotObjects, boolean active, BiConsumer<List<Symbol>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().peek(w -> w.setActive(active)).map(w -> w.getId()).collect(Collectors.toList()), active)
                      .subscribe(s -> activationFunc.accept(plotObjects, active));
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

    private void handlePlotObjectClicked(Object obj, Function<Symbol, SpectraMeasurement> measurementFunc) {
        if (obj instanceof PlotObjectClicked && ((PlotObjectClicked) obj).getMouseEvent().getID() == MouseEvent.MOUSE_RELEASED) {
            PlotObjectClicked poc = (PlotObjectClicked) obj;
            PlotObject po = poc.getPlotObject();
            if (po instanceof Symbol) {
                SpectraMeasurement spectra = measurementFunc.apply((Symbol) po);
                String symbolId = ((Symbol) po).getText();
                if (spectra != null && spectra.getWaveform() != null) {
                    if (SwingUtilities.isLeftMouseButton(poc.getMouseEvent()) && symbolId != null) {
                        selectPoints(plotPointMap.get(symbolId));
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
}
