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
import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Observable;
import java.util.Observer;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ReferenceEventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
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
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.embed.swing.SwingNode;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
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
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.SymbolDef;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;
import llnl.gnem.core.gui.plotting.plotobject.SymbolStyle;
import reactor.core.scheduler.Schedulers;

@Component
public class SiteController implements MapListeningController, RefreshableController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    private static final String X_AXIS_LABEL = "center freq";

    private static final int MAX_LEGEND_COLORS = 8;

    @FXML
    private SwingNode mwPlotSwingNode;
    private JMultiAxisPlot mwPlot;
    private JSubplot mwPlotFigure;
    private Line mwZeroLine;

    @FXML
    private SwingNode stressPlotSwingNode;
    private JMultiAxisPlot stressPlot;
    private JSubplot stressPlotFigure;
    private Line stressZeroLine;

    @FXML
    private SwingNode sdPlotSwingNode;
    private JMultiAxisPlot sdPlot;
    private JSubplot sdPlotFigure;

    @FXML
    private SwingNode rawPlotSwingNode;
    private SpectralPlot rawPlot;

    @FXML
    private SwingNode pathPlotSwingNode;
    private SpectralPlot pathPlot;

    @FXML
    private SwingNode sitePlotSwingNode;
    private SpectralPlot sitePlot;

    @FXML
    private ComboBox<String> evidCombo;

    @FXML
    private TableView<MeasuredMwDetails> eventTable;

    @FXML
    private TableView<LabeledPlotPoint> iconTable;

    @FXML
    private TableColumn<MeasuredMwDetails, String> evidCol;

    @FXML
    private TableColumn<MeasuredMwDetails, String> mwCol;

    @FXML
    private TableColumn<MeasuredMwDetails, String> stressCol;

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

    private SpectraClient spectraClient;
    private List<SpectraMeasurement> spectralMeasurements = new ArrayList<>();
    private ObservableList<String> evids = FXCollections.observableArrayList();

    private ReferenceEventClient referenceEventClient;
    private ObservableList<MeasuredMwDetails> mwParameters = FXCollections.observableArrayList();

    private WaveformClient waveformClient;

    private ObservableList<LabeledPlotPoint> stationSymbols = FXCollections.observableArrayList();
    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;

    private EventBus bus;

    protected Map<Point2D.Double, SpectraMeasurement> rawSymbolMap = new ConcurrentHashMap<>();
    protected Map<Point2D.Double, SpectraMeasurement> pathSymbolMap = new ConcurrentHashMap<>();
    protected Map<Point2D.Double, SpectraMeasurement> siteSymbolMap = new ConcurrentHashMap<>();

    private Map<String, List<PlotPoint>> plotPointMap = new HashMap<>();
    private final List<PlotPoint> selectedPoints = new ArrayList<>();

    private SymbolStyleMapFactory symbolStyleMapFactory;

    private Map<String, PlotPoint> symbolStyleMap;

    private GeoMap mapImpl;

    private MapPlottingUtilities iconFactory;

    private MenuItem exclude;
    private MenuItem include;
    private ContextMenu menu;

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    private ColorMap colorMap = new ViridisColorMap();

    @Autowired
    private SiteController(SpectraClient spectraClient, ReferenceEventClient referenceEventClient, WaveformClient waveformClient, SymbolStyleMapFactory styleFactory, GeoMap map,
            MapPlottingUtilities iconFactory, EventBus bus) {
        this.spectraClient = spectraClient;
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
    }

    @FXML
    public void initialize() {
        evidCombo.setItems(evids);

        SwingUtilities.invokeLater(() -> {

            rawPlot = new SpectralPlot();
            rawPlot.addPlotObjectObserver(new Observer() {
                @Override
                public void update(Observable observable, Object obj) {
                    handlePlotObjectClicked(obj, sym -> rawSymbolMap.get(getPoint2D(sym)));
                }
            });
            rawPlotSwingNode.setContent(rawPlot);

            pathPlot = new SpectralPlot();
            pathPlot.addPlotObjectObserver(new Observer() {
                @Override
                public void update(Observable observable, Object obj) {
                    handlePlotObjectClicked(obj, sym -> pathSymbolMap.get(getPoint2D(sym)));
                }
            });

            pathPlotSwingNode.setContent(pathPlot);

            sitePlot = new SpectralPlot();
            sitePlot.addPlotObjectObserver(new Observer() {
                @Override
                public void update(Observable observable, Object obj) {
                    handlePlotObjectClicked(obj, sym -> siteSymbolMap.get(getPoint2D(sym)));
                }
            });
            sitePlotSwingNode.setContent(sitePlot);

            rawPlot.setLabels("Raw Plot", X_AXIS_LABEL, "log10(?)");
            rawPlot.setYaxisVisibility(true);
            rawPlot.setAllXlimits(0.0, 0.0);
            rawPlot.setDefaultYMin(-2.0);
            rawPlot.setDefaultYMax(7.0);

            pathPlot.setLabels("Path Corrected", X_AXIS_LABEL, "log10(?)");
            pathPlot.setYaxisVisibility(true);
            pathPlot.setAllXlimits(0.0, 0.0);
            pathPlot.setDefaultYMin(-2.0);
            pathPlot.setDefaultYMax(7.0);

            sitePlot.setLabels("Site Corrected", X_AXIS_LABEL, "log10(amplitude)");
            sitePlot.setYaxisVisibility(true);

            mwPlot = new JMultiAxisPlot();
            mwPlotFigure = mwPlot.addSubplot();

            mwPlot.getTitle().setText("Mw comparison");
            mwPlot.getXaxis().setLabelText("Measured");
            mwPlot.setYaxisVisibility(true);
            mwPlotSwingNode.setContent(mwPlot);

            mwPlotFigure.getYaxis().setLabelOffset(2d * mwPlot.getXaxis().getLabelOffset());
            mwPlotFigure.setAxisLimits(0.0, 10.0, 0.0, 10.0);
            mwPlotFigure.getYaxis().setLabelText("Reference");

            stressPlot = new JMultiAxisPlot();
            stressPlotFigure = stressPlot.addSubplot();

            stressPlot.getTitle().setText("Stress comparison");
            stressPlot.getXaxis().setLabelText("Measured");
            stressPlot.setYaxisVisibility(true);
            stressPlotSwingNode.setContent(stressPlot);

            stressPlotFigure.getYaxis().setLabelOffset(2d * stressPlot.getXaxis().getLabelOffset());
            stressPlotFigure.setAxisLimits(0.0, 10.0, 0.0, 10.0);
            stressPlotFigure.getYaxis().setLabelText("Reference");

            sdPlot = new JMultiAxisPlot();
            sdPlotFigure = sdPlot.addSubplot();

            sdPlot.getTitle().setText("Site correction overview");
            sdPlot.getXaxis().setLabelText("Frequency");
            sdPlot.setYaxisVisibility(true);
            sdPlotSwingNode.setContent(sdPlot);

            sdPlotFigure.getYaxis().setLabelOffset(2d * sdPlot.getXaxis().getLabelOffset());
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
        rawPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
        pathPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
        sitePlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
    }

    protected Object getPoint2D(Symbol sym) {
        return new Point2D.Double(sym.getXcenter(), sym.getYcenter());
    }

    private void showWaveformPopup(Waveform waveform) {
        bus.post(new WaveformSelectionEvent(waveform.getId()));
    }

    private void plotSpectra() {
        clearSpectraPlots();
        rawSymbolMap.clear();
        pathSymbolMap.clear();
        siteSymbolMap.clear();
        plotPointMap.clear();
        List<SpectraMeasurement> filteredMeasurements;
        if (evidCombo != null && evidCombo.getSelectionModel().getSelectedIndex() > 0) {
            filteredMeasurements = filterToEvent(evidCombo.getSelectionModel().getSelectedItem(), spectralMeasurements);
            Spectra referenceSpectra = spectraClient.getReferenceSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            Spectra theoreticalSpectra = spectraClient.getFitSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            rawPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getRawAtMeasurementTime), Boolean.TRUE);
            pathPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathCorrected), Boolean.TRUE);
            sitePlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected), Boolean.TRUE, referenceSpectra, theoreticalSpectra);
        } else {
            filteredMeasurements = spectralMeasurements;
            rawPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getRawAtMeasurementTime), Boolean.FALSE);
            pathPlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathCorrected), Boolean.FALSE);
            sitePlot.plotXYdata(toPlotPoints(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected), Boolean.FALSE);
        }
        rawSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getRawAtMeasurementTime));
        pathSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getPathCorrected));
        siteSymbolMap.putAll(mapSpectraToPoint(filteredMeasurements, SpectraMeasurement::getPathAndSiteCorrected));
        mapImpl.addIcons(mapMeasurements(filteredMeasurements));

    }

    private Collection<Icon> mapMeasurements(List<SpectraMeasurement> filteredMeasurements) {
        return filteredMeasurements.stream().map(meas -> meas.getWaveform()).filter(Objects::nonNull).flatMap(w -> {
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

    private void clearSpectraPlots() {
        rawPlot.clearPlot();
        pathPlot.clearPlot();
        sitePlot.clearPlot();
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
        clearSpectraPlots();

        mwParameters.clear();
        try {
            SwingUtilities.invokeAndWait(() -> {
                mwPlotFigure.Clear();
                stressPlotFigure.Clear();
                sdPlotFigure.Clear();
                mwPlotFigure.AddPlotObject(mwZeroLine);
                stressPlotFigure.AddPlotObject(stressZeroLine);
                List<MeasuredMwDetails> evs = referenceEventClient.getMeasuredEventDetails()
                                                                  .filter(ev -> ev.getEventId() != null)
                                                                  .collect(Collectors.toList())
                                                                  .subscribeOn(Schedulers.elastic())
                                                                  .block(Duration.ofSeconds(10l));

                double minMw = 10.0;
                double maxMw = 0.0;
                double minStress = 1.0;
                double maxStress = 0.0;
                for (MeasuredMwDetails ev : evs) {
                    mwParameters.add(ev);
                    if (ev.getMw() != null && ev.getMw() != 0.0 && ev.getRefMw() != null && ev.getRefMw() != 0.0) {
                        double mw = ev.getMw();
                        double ref = ev.getRefMw();
                        if (mw < minMw) {
                            minMw = mw;
                        }
                        if (mw > maxMw) {
                            maxMw = mw;
                        }
                        if (ref < minMw) {
                            minMw = ref;
                        }
                        if (ref > maxMw) {
                            maxMw = ref;
                        }

                        double stress = ev.getApparentStressInMpa();
                        double refStress = ev.getRefApparentStressInMpa();
                        if (stress < minStress) {
                            minStress = stress;
                        }
                        if (stress > maxStress) {
                            maxStress = stress;
                        }
                        if (refStress < minStress) {
                            minStress = refStress;
                        }
                        if (refStress > maxStress) {
                            maxStress = refStress;
                        }

                        Circle mwSym = new Circle(mw, ref, 2.0, Color.RED, Color.RED, Color.RED, ev.getEventId(), true, false, 6.0);
                        mwPlotFigure.AddPlotObject(mwSym);

                        Circle stressSym = new Circle(stress, refStress, 2.0, Color.RED, Color.RED, Color.RED, ev.getEventId(), true, false, 6.0);
                        stressPlotFigure.AddPlotObject(stressSym);
                    }
                }

                maxMw = maxMw + .1;
                minMw = minMw > maxMw ? minMw = maxMw - .1 : minMw - .1;

                mwPlotFigure.setAxisLimits(minMw, maxMw, minMw, maxMw);

                maxStress = maxStress + .1;
                minStress = minStress > maxStress ? minStress = maxStress - .1 : minStress - .1;

                stressPlotFigure.setAxisLimits(minStress, maxStress, minStress, maxStress);
            });

            spectralMeasurements.clear();
            stationSymbols.clear();

            evids.clear();
            evids.add("All");

            spectralMeasurements.addAll(
                    spectraClient.getMeasuredSpectraMetadata()
                                 .filter(Objects::nonNull)
                                 .filter(spectra -> spectra.getWaveform() != null && spectra.getWaveform().getEvent() != null && spectra.getWaveform().getStream() != null)
                                 .toStream()
                                 .collect(Collectors.toList()));

            symbolStyleMap = symbolStyleMapFactory.build(spectralMeasurements, new Function<SpectraMeasurement, String>() {
                @Override
                public String apply(SpectraMeasurement t) {
                    return t.getWaveform().getStream().getStation().getStationName();
                }
            });
            stationSymbols.addAll(symbolStyleMap.entrySet().stream().map(e -> new LabeledPlotPoint(e.getKey(), e.getValue())).collect(Collectors.toList()));

            evids.addAll(spectralMeasurements.stream().map(spec -> spec.getWaveform().getEvent().getEventId()).distinct().sorted(new MaybeNumericStringComparator()).collect(Collectors.toList()));
            eventTable.sort();

            SwingUtilities.invokeAndWait(() -> {
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

            mwPlot.repaint();
            stressPlot.repaint();
            sdPlot.repaint();
        } catch (InvocationTargetException ex) {
            //nop
        } catch (InterruptedException ex) {
            log.warn("Swing interrupt during re-plotting of site controller", ex);
            Thread.currentThread().interrupt();
        }
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
        mapImpl.clearIcons();
        plotSpectra();
        Platform.runLater(() -> {
            rawPlot.repaint();
            pathPlot.repaint();
            sitePlot.repaint();
        });
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
        boolean existsInPlot = rawSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            rawPlot.selectPoint(xyPoint);
        }

        existsInPlot = pathSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            pathPlot.selectPoint(xyPoint);
        }

        existsInPlot = siteSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            sitePlot.selectPoint(xyPoint);
        }
    }

    private void deselectPoint(PlotPoint point) {
        Point2D.Double xyPoint = new Point2D.Double(point.getX(), point.getY());
        boolean existsInPlot = rawSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            rawPlot.deselectPoint(xyPoint);
        }

        existsInPlot = pathSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            pathPlot.deselectPoint(xyPoint);
        }

        existsInPlot = siteSymbolMap.containsKey(xyPoint);
        if (existsInPlot) {
            sitePlot.deselectPoint(xyPoint);
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
                rawPlot.repaint();
                pathPlot.repaint();
                sitePlot.repaint();
            });
        });
    }

    private void showContextMenu(List<Waveform> waveforms, List<Symbol> plotObjects, MouseEvent t, BiConsumer<List<Symbol>, Boolean> activationFunc) {
        Platform.runLater(() -> {
            include.setOnAction(evt -> setActive(waveforms, plotObjects, true, activationFunc));
            exclude.setOnAction(evt -> setActive(waveforms, plotObjects, false, activationFunc));
            menu.show(sitePlotSwingNode, t.getXOnScreen(), t.getYOnScreen());
        });
    }

    private void setActive(List<Waveform> waveforms, List<Symbol> plotObjects, boolean active, BiConsumer<List<Symbol>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().map(w -> w.getId()).collect(Collectors.toList()), active).subscribe(s -> activationFunc.accept(plotObjects, active));
    }

    private void handlePlotObjectClicked(Object obj, Function<Symbol, SpectraMeasurement> measurementFunc) {
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
}
