/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
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
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.ColorMaps;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;
import llnl.gnem.core.util.TimeT;

public abstract class AbstractMeasurementController implements MapListeningController, RefreshableController, ScreenshotEnabledController {

    private static final Integer VALIDATION_Z_ORDER = 0;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @FXML
    protected Tab resultsTab;

    @FXML
    protected StackPane mwPlotPane;
    private BasicPlot mwPlot;

    @FXML
    protected StackPane stressPlotPane;
    private BasicPlot stressPlot;

    @FXML
    protected StackPane sdPlotPane;
    private BasicPlot sdPlot;

    protected StackPane spectraPlotPanel;

    @FXML
    protected ComboBox<String> evidCombo;

    @FXML
    protected TableView<MeasuredMwDetails> eventTable;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> evidCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> dateCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> mwCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> obsEnergyCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyMDACCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyRatioCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyStressCol;

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
    protected TableColumn<MeasuredMwDetails, Integer> stationCountCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> bandCoverageCol;

    @FXML
    protected TableColumn<LabeledPlotPoint, String> stationCol;

    @FXML
    protected TextField eventTime;

    @FXML
    protected TextField eventLoc;

    @FXML
    protected TextField obsTotalEnergy;

    @FXML
    protected TextField obsEnergy;

    protected List<SpectraMeasurement> spectralMeasurements = new ArrayList<>();
    private final ObservableList<String> evids = FXCollections.observableArrayList();

    protected SpectraClient spectraClient;
    protected ParameterClient paramClient;
    protected EventClient referenceEventClient;
    protected WaveformClient waveformClient;

    protected ObservableList<MeasuredMwDetails> mwParameters = FXCollections.observableArrayList();

    private final ObservableList<LabeledPlotPoint> stationSymbols = FXCollections.observableArrayList();
    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;

    private final Map<String, List<LabeledPlotPoint>> plotPointMap = new HashMap<>();

    private final SymbolStyleMapFactory symbolStyleMapFactory;
    private Map<String, PlotPoint> symbolStyleMap;

    private final GeoMap mapImpl;

    private final MapPlottingUtilities iconFactory;

    private MenuItem exclude;
    private MenuItem include;
    protected ContextMenu menu;

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();
    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

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

    private final PlotFactory plotFactory;
    private final EventBus bus;

    // TODO: Break this up into components so this isn't so incredibly huge.
    protected AbstractMeasurementController(final SpectraClient spectraClient, final ParameterClient paramClient, final EventClient referenceEventClient, final WaveformClient waveformClient,
            final SymbolStyleMapFactory styleFactory, final GeoMap map, final MapPlottingUtilities iconFactory, final PlotFactory plotFactory, final EventBus bus) {
        this.spectraClient = spectraClient;
        this.paramClient = paramClient;
        this.referenceEventClient = referenceEventClient;
        this.waveformClient = waveformClient;
        this.symbolStyleMapFactory = styleFactory;
        this.mapImpl = map;
        this.plotFactory = plotFactory;
        this.bus = bus;
        this.iconFactory = iconFactory;

        eventSelectionCallback = this::selectDataByCriteria;

        stationSelectionCallback = this::selectDataByCriteria;

        this.bus.register(this);
    }

    protected abstract String getDisplayName();

    protected abstract List<Spectra> getFitSpectra();

    protected abstract void setActive(Set<Waveform> waveforms, List<Point2D> points, boolean active, BiConsumer<List<Point2D>, Boolean> activationFunc);

    protected abstract List<SpectraMeasurement> getSpectraData();

    protected abstract void runGuiUpdate(Runnable runnable) throws InvocationTargetException, InterruptedException;

    protected abstract List<MeasuredMwDetails> getEvents();

    public void initialize() {
        evidCombo.setItems(evids);

        configureAxisShrink(xAxisShrink, () -> {
            shouldXAxisShrink = !shouldXAxisShrink;
            return shouldXAxisShrink;
        }, Axis.Type.LOG_X);

        configureAxisShrink(yAxisShrink, () -> {
            shouldYAxisShrink = !shouldYAxisShrink;
            return shouldYAxisShrink;
        }, Axis.Type.Y);

        mwPlot = plotFactory.basicPlot();
        mwPlot.getTitle().setText("Mw");
        mwPlot.getTitle().setFontSize(16);
        mwPlot.addAxes(plotFactory.axis(Axis.Type.X, "Measured"), plotFactory.axis(Axis.Type.Y, "Comparison"));
        final AxisLimits mwXaxis = new AxisLimits(Axis.Type.X, 0.0, 10.0);
        final AxisLimits mwYaxis = new AxisLimits(Axis.Type.Y, 0.0, 10.0);
        mwPlot.setAxisLimits(mwXaxis, mwYaxis);
        mwPlot.attachToDisplayNode(mwPlotPane);

        stressPlot = plotFactory.basicPlot();
        stressPlot.getTitle().setText("Stress");
        stressPlot.getTitle().setFontSize(16);
        stressPlot.addAxes(plotFactory.axis(Axis.Type.X, "Measured"), plotFactory.axis(Axis.Type.Y, "Comparison"));
        final AxisLimits stressXaxis = new AxisLimits(Axis.Type.X, 0.0, 10.0);
        final AxisLimits stressYaxis = new AxisLimits(Axis.Type.Y, 0.0, 10.0);
        stressPlot.setAxisLimits(stressXaxis, stressYaxis);
        stressPlot.attachToDisplayNode(stressPlotPane);

        sdPlot = plotFactory.basicPlot();
        sdPlot.getTitle().setText("Site correction overview");
        sdPlot.getTitle().setFontSize(16);
        sdPlot.addAxes(plotFactory.axis(Axis.Type.X, "Frequency (Hz)"), plotFactory.axis(Axis.Type.Y, "Standard Deviation"));
        sdPlot.setAxisLimits(new AxisLimits(Axis.Type.X, 0.0, 10.0), new AxisLimits(Axis.Type.Y, 0.0, 2.0));
        sdPlot.showLegend(false);
        sdPlot.attachToDisplayNode(sdPlotPane);

        evidCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && isVisible) {
                refreshView();
            }
        });

        CellBindingUtils.attachTextCellFactoriesString(evidCol, MeasuredMwDetails::getEventId);
        evidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachTextCellFactoriesString(dateCol, MeasuredMwDetails::getDatetime);

        CellBindingUtils.attachTextCellFactories(mwCol, MeasuredMwDetails::getRefMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(stressCol, MeasuredMwDetails::getRefApparentStressInMpa, dfmt4);

        CellBindingUtils.attachTextCellFactories(valMwCol, MeasuredMwDetails::getValMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(valStressCol, MeasuredMwDetails::getValApparentStressInMpa, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwCol, MeasuredMwDetails::getMw, dfmt4);

        CellBindingUtils.attachTextCellFactories(obsEnergyCol, MeasuredMwDetails::getObsEnergy, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyCol, MeasuredMwDetails::getTotalEnergy, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyMDACCol, MeasuredMwDetails::getTotalEnergyMDAC, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyRatioCol, MeasuredMwDetails::getEnergyRatio, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyStressCol, MeasuredMwDetails::getEnergyStress, dfmt4);

        CellBindingUtils.attachTextCellFactories(measuredStressCol, MeasuredMwDetails::getApparentStressInMpa, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredCornerFreqCol, MeasuredMwDetails::getCornerFreq, dfmt4);

        CellBindingUtils.attachTextCellFactories(mistfitCol, MeasuredMwDetails::getMisfit, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq1LowCol, MeasuredMwDetails::getMw1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq1HighCol, MeasuredMwDetails::getMw1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq2LowCol, MeasuredMwDetails::getMw2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq2HighCol, MeasuredMwDetails::getMw2Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(bandCoverageCol, MeasuredMwDetails::getBandCoverage, dfmt4);

        stationCountCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getStationCount).orElseGet(() -> 0)).asObject());

        iterationsCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getIterations).orElseGet(() -> 0)).asObject());

        dataCountCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getDataCount).orElseGet(() -> 0)).asObject());

        eventTable.setItems(mwParameters);

        menu = new ContextMenu();
        include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);
    }

    protected Point2D getPoint2D(final Symbol sym) {
        return new Point2D(sym.getX(), sym.getY());
    }

    private void showWaveformPopup(final Long... ids) {
        bus.post(new WaveformSelectionEvent(ids));
    }

    private void plotSpectra() {
        clearSpectraPlots();
        spectraControllers.forEach(spc -> {
            spc.getSpectraMeasurementMap().clear();
            spc.getSymbolMap().clear();
        });
        plotPointMap.clear();
        List<SpectraMeasurement> filteredMeasurements;

        spectraControllers.forEach(spc -> {
            final SpectralPlot plot = spc.getSpectralPlot();
            plot.setAutoCalculateXaxisRange(shouldXAxisShrink);
            if (!shouldXAxisShrink) {
                plot.setAllXlimits(minFreq.get(), maxFreq.get());
            } else {
                plot.setAllXlimits();
            }
        });

        final List<Spectra> fittingSpectra;
        if (evidCombo != null && evidCombo.getSelectionModel().getSelectedIndex() > 0) {
            filteredMeasurements = filterToEvent(evidCombo.getSelectionModel().getSelectedItem(), spectralMeasurements);
            fittingSpectra = getFitSpectra();
            final Spectra referenceSpectra = spectraClient.getReferenceSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            fittingSpectra.add(referenceSpectra);
            final Spectra validationSpectra = spectraClient.getValidationSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            fittingSpectra.add(validationSpectra);
            if (filteredMeasurements != null && !filteredMeasurements.isEmpty() && filteredMeasurements.get(0).getWaveform() != null) {
                final Event event = filteredMeasurements.get(0).getWaveform().getEvent();
                final Spectra fitSpectra = fittingSpectra.get(0);
                eventTime.setText(
                        "Date: "
                                + DateTimeFormatter.ISO_INSTANT.format(event.getOriginTime().toInstant())
                                + " Julian Day: "
                                + TimeT.jdateToTimeT(TimeT.EpochToJdate(event.getOriginTime().toInstant().getEpochSecond())).getJDay());
                eventLoc.setText("Lat: " + dfmt4.format(event.getLatitude()) + " Lon: " + dfmt4.format(event.getLongitude()) + " Depth: " + dfmt2.format(event.getDepth()));
                obsEnergy.setText("Observed Energy: " + dfmt4.format(fitSpectra.getObsEnergy()) + " J  MDAC Energy: " + dfmt4.format(fitSpectra.getlogTotalEnergyMDAC()) + " J");
                obsTotalEnergy.setText("Observed Total Energy: " + dfmt4.format(fitSpectra.getLogTotalEnergy()) + " J @ " + dfmt4.format(fitSpectra.getObsAppStress()) + " MPa");
                eventTime.setVisible(true);
                eventLoc.setVisible(true);
                obsEnergy.setVisible(true);
                obsTotalEnergy.setVisible(true);
            } else {
                filteredMeasurements = Collections.emptyList();
            }
        } else {
            eventTime.setVisible(false);
            eventLoc.setVisible(false);
            obsEnergy.setVisible(false);
            obsTotalEnergy.setVisible(false);
            filteredMeasurements = spectralMeasurements;
            fittingSpectra = null;
        }

        final List<SpectraMeasurement> selectedEventMeasurements = filteredMeasurements;

        spectraControllers.forEach(spc -> {
            if (fittingSpectra != null && spc.shouldShowFits()) {
                spc.getSpectralPlot().plotXYdata(toPlotPoints(selectedEventMeasurements, spc.getDataFunc()), fittingSpectra);
            } else {
                spc.getSpectralPlot().plotXYdata(toPlotPoints(selectedEventMeasurements, spc.getDataFunc()), null);
            }
            spc.getSpectraMeasurementMap().putAll(mapSpectraToPoint(selectedEventMeasurements, spc.getDataFunc()));
        });
        mapMeasurements(selectedEventMeasurements);

        minY.set(100.0);
        maxY.set(0.0);
        final DoubleSummaryStatistics stats = selectedEventMeasurements.stream()
                                                                       .filter(Objects::nonNull)
                                                                       .map(SpectraMeasurement::getPathAndSiteCorrected)
                                                                       .filter(v -> v != 0.0)
                                                                       .collect(Collectors.summarizingDouble(Double::doubleValue));
        maxY.set(stats.getMax() + .1);
        minY.set(stats.getMin() - .1);

        spectraControllers.forEach(spc -> spc.setYAxisResize(shouldYAxisShrink, minY.get(), maxY.get()));
    }

    private Map<Point2D, SpectraMeasurement> mapSpectraToPoint(final List<SpectraMeasurement> spectralMeasurements, final Function<SpectraMeasurement, Double> func) {
        return spectralMeasurements.stream()
                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                   .collect(
                                           Collectors.toMap(
                                                   spectra -> new Point2D(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency()), func.apply(spectra)),
                                                       Function.identity(),
                                                       (a, b) -> b,
                                                       HashMap::new));
    }

    private void mapMeasurements(final List<SpectraMeasurement> measurements) {
        if (measurements != null) {
            mapImpl.addIcons(
                    iconFactory.genIconsFromWaveforms(
                            eventSelectionCallback,
                                stationSelectionCallback,
                                measurements.stream().map(SpectraMeasurement::getWaveform).filter(Objects::nonNull).collect(Collectors.toList())));
        }
    }

    protected void clearSpectraPlots() {
        spectraControllers.forEach(spc -> spc.getSpectralPlot().clearPlot());
    }

    private List<SpectraMeasurement> filterToEvent(final String selectedItem, final List<SpectraMeasurement> spectralMeasurements) {
        return spectralMeasurements.stream().filter(spec -> selectedItem.equalsIgnoreCase(spec.getWaveform().getEvent().getEventId())).collect(Collectors.toList());
    }

    private List<PlotPoint> toPlotPoints(final List<SpectraMeasurement> spectralMeasurements, final Function<SpectraMeasurement, Double> func) {
        return spectralMeasurements.stream()
                                   .filter(spectra -> !func.apply(spectra).equals(0.0))
                                   .filter(
                                           spectra -> spectra != null
                                                   && spectra.getWaveform() != null
                                                   && spectra.getWaveform().getStream() != null
                                                   && spectra.getWaveform().getStream().getStation() != null)
                                   .map(spectra -> {
                                       final String key = spectra.getWaveform().getStream().getStation().getStationName();
                                       final PlotPoint pp = getPlotPoint(key, spectra.getWaveform().isActive());
                                       final LabeledPlotPoint point = new LabeledPlotPoint(key,
                                                                                           new PlotPoint(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency()),
                                                                                                         func.apply(spectra),
                                                                                                         pp.getStyle(),
                                                                                                         pp.getColor(),
                                                                                                         pp.getColor()));
                                       if (hasEventAndStation(spectra)) {
                                           plotPointMap.computeIfAbsent(spectra.getWaveform().getEvent().getEventId(), k -> new ArrayList<>()).add(point);
                                           plotPointMap.computeIfAbsent(spectra.getWaveform().getStream().getStation().getStationName(), k -> new ArrayList<>()).add(point);
                                       }
                                       return point;
                                   })
                                   .collect(Collectors.toList());
    }

    private PlotPoint getPlotPoint(final String key, final boolean active) {
        final PlotPoint pp = new PlotPoint(symbolStyleMap.get(key));
        if (!active) {
            pp.setColor(Color.GRAY);
        }
        return pp;
    }

    private boolean hasEventAndStation(final SpectraMeasurement spectra) {
        return spectra != null
                && spectra.getWaveform() != null
                && spectra.getWaveform().getEvent() != null
                && spectra.getWaveform().getEvent().getEventId() != null
                && spectra.getWaveform().getStream() != null
                && spectra.getWaveform().getStream().getStation() != null
                && spectra.getWaveform().getStream().getStation().getStationName() != null;
    }

    private double centerFreq(final Double lowFrequency, final Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    protected void reloadData() {
        try {
            runGuiUpdate(this::clearSpectraPlots);
            maxFreq.set(-0.0);
            minFreq.set(1.0);

            final List<SharedFrequencyBandParameters> results = paramClient.getSharedFrequencyBandParameters().filter(Objects::nonNull).collectList().block(Duration.of(10l, ChronoUnit.SECONDS));

            if (results != null) {
                final DoubleSummaryStatistics stats = results.stream()
                                                             .map(sfb -> Math.log10(centerFreq(sfb.getLowFrequency(), sfb.getHighFrequency())))
                                                             .collect(Collectors.summarizingDouble(Double::doubleValue));
                maxFreq.set(stats.getMax());
                minFreq.set(stats.getMin());

                preloadData();
                spectralMeasurements.clear();
                spectralMeasurements.addAll(getSpectraData());

                runGuiUpdate(() -> {
                    mwParameters.clear();
                    mwPlot.clear();
                    stressPlot.clear();
                    sdPlot.clear();

                    final List<MeasuredMwDetails> evs = getEvents();
                    final List<Symbol> mwPlotSymbols = new ArrayList<>();
                    final List<Symbol> stressPlotSymbols = new ArrayList<>();

                    double minMw = 10.0;
                    double maxMw = 0.0;
                    double minStress = 1.0;
                    double maxStress = 0.0;
                    for (final MeasuredMwDetails ev : evs) {
                        mwParameters.add(ev);

                        if (ev.getMw() != null && ev.getMw() != 0.0) {
                            final Double mw = ev.getMw();
                            if (mw < minMw) {
                                minMw = mw;
                            }
                            if (mw > maxMw) {
                                maxMw = mw;
                            }

                            if (ev.getRefMw() != null && ev.getRefMw() != 0.0) {
                                final Double ref = ev.getRefMw();
                                if (ref < minMw) {
                                    minMw = ref;
                                }
                                if (ref > maxMw) {
                                    maxMw = ref;
                                }

                                final Symbol mwSym = plotFactory.createSymbol(SymbolStyles.CIRCLE, "Reference", mw, ref, Color.RED, Color.RED, Color.RED, ev.getEventId(), false);
                                mwPlotSymbols.add(mwSym);
                            }

                            final Double valMw = ev.getValMw();
                            if (valMw != null && valMw != 0.0) {
                                if (valMw < minMw) {
                                    minMw = valMw;
                                }
                                if (valMw > maxMw) {
                                    maxMw = valMw;
                                }

                                final Symbol valSym = plotFactory.createSymbol(SymbolStyles.SQUARE, "Validation", mw, valMw, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false);
                                valSym.setZindex(VALIDATION_Z_ORDER);
                                mwPlotSymbols.add(valSym);
                            }

                            final Double stress = ev.getApparentStressInMpa();
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

                                if (refStress != null && refStress != 0.0) {
                                    if (refStress < minStress) {
                                        minStress = refStress;
                                    }
                                    if (refStress > maxStress) {
                                        maxStress = refStress;
                                    }

                                    final Symbol stressSym = plotFactory.createSymbol(SymbolStyles.CIRCLE, "Reference", stress, refStress, Color.RED, Color.RED, Color.RED, ev.getEventId(), false);
                                    stressPlotSymbols.add(stressSym);
                                }

                                final Double valStress = ev.getValApparentStressInMpa();
                                if (valStress != null && valStress != 0.0) {
                                    if (valStress < minStress) {
                                        minStress = valStress;
                                    }
                                    if (valStress > maxStress) {
                                        maxStress = valStress;
                                    }

                                    final Symbol valSym = plotFactory.createSymbol(SymbolStyles.SQUARE, "Validation", stress, valStress, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false);
                                    valSym.setZindex(VALIDATION_Z_ORDER);
                                    stressPlotSymbols.add(valSym);
                                }
                            }
                        }
                    }

                    maxMw = maxMw + Math.abs(maxMw * .1);
                    if (minMw > maxMw) {
                        minMw = maxMw - .1;
                    } else {
                        minMw = minMw - Math.abs(minMw * .1);
                    }

                    mwPlot.setAxisLimits(new AxisLimits(Axis.Type.X, minMw, maxMw), new AxisLimits(Axis.Type.Y, minMw, maxMw));

                    maxStress = maxStress + Math.abs(maxStress * .1);
                    if (minStress > maxStress) {
                        minStress = maxStress - .1;
                    } else {
                        minStress = minStress - Math.abs(minStress * .1);
                    }

                    stressPlot.setAxisLimits(new AxisLimits(Axis.Type.X, minStress, maxStress), new AxisLimits(Axis.Type.Y, minStress, maxStress));

                    final double[] xy = new double[2];
                    xy[0] = minMw;
                    xy[1] = maxMw;
                    final Line mwZeroLine = plotFactory.line(xy, xy, Color.LIGHTGRAY, LineStyles.DASH, 2);
                    mwZeroLine.setName("");
                    mwZeroLine.showInLegend(false);
                    mwPlot.addPlotObject(mwZeroLine);

                    xy[0] = minStress;
                    xy[1] = maxStress;
                    final Line stressZeroLine = plotFactory.line(xy, xy, Color.LIGHTGRAY, LineStyles.DASH, 2);
                    stressZeroLine.setName("");
                    stressZeroLine.showInLegend(false);
                    stressPlot.addPlotObject(stressZeroLine);

                    mwPlotSymbols.forEach(mwPlot::addPlotObject);
                    stressPlotSymbols.forEach(stressPlot::addPlotObject);

                    mwPlot.replot();
                    stressPlot.replot();
                });

                symbolStyleMap = symbolStyleMapFactory.build(spectralMeasurements, specMeas -> specMeas.getWaveform().getStream().getStation().getStationName());

                runGuiUpdate(() -> {
                    stationSymbols.clear();
                    stationSymbols.addAll(symbolStyleMap.entrySet().stream().map(e -> new LabeledPlotPoint(e.getKey(), e.getValue())).collect(Collectors.toList()));

                    Platform.runLater(() -> {
                        evids.clear();
                        evids.add("All");
                        evids.addAll(
                                spectralMeasurements.stream()
                                                    .map(spec -> spec.getWaveform().getEvent().getEventId())
                                                    .distinct()
                                                    .sorted(new MaybeNumericStringComparator())
                                                    .collect(Collectors.toList()));
                        eventTable.sort();
                    });

                    final Map<String, Map<Double, SummaryStatistics>> evidStats = new HashMap<>();

                    double minSite = 1E2;
                    double maxSite = -1E2;
                    double minCenterFreq = 1E2;
                    double maxCenterFreq = -1E2;

                    for (final SpectraMeasurement meas : spectralMeasurements) {
                        final String evid = meas.getWaveform().getEvent().getEventId();
                        final Double freq = centerFreq(meas.getWaveform());
                        evidStats.computeIfAbsent(evid, key -> new HashMap<>()).computeIfAbsent(freq, key -> new SummaryStatistics()).addValue(meas.getPathAndSiteCorrected());
                    }

                    for (final Map<Double, SummaryStatistics> freqStats : evidStats.values()) {
                        for (final Entry<Double, SummaryStatistics> entry : freqStats.entrySet()) {
                            final double site = entry.getValue().getStandardDeviation();
                            if (entry.getValue() != null && entry.getValue().getN() > 1) {
                                if (site < minSite) {
                                    minSite = site;
                                }
                                if (site > maxSite) {
                                    maxSite = site;
                                }
                                if (entry.getKey() < minCenterFreq) {
                                    minCenterFreq = entry.getKey();
                                }
                                if (entry.getKey() > maxCenterFreq) {
                                    maxCenterFreq = entry.getKey();
                                }
                            }
                        }
                    }

                    for (final Map<Double, SummaryStatistics> freqStats : evidStats.values()) {
                        for (final Entry<Double, SummaryStatistics> entry : freqStats.entrySet()) {
                            final double site = entry.getValue().getStandardDeviation();
                            if (entry.getValue() != null && entry.getValue().getN() > 1) {
                                final Symbol sdSym = plotFactory.createSymbol(
                                        SymbolStyles.CIRCLE,
                                            Long.toString(entry.getValue().getN()),
                                            entry.getKey(),
                                            site,
                                            null,
                                            null,
                                            null,
                                            Long.toString(entry.getValue().getN()),
                                            false);
                                sdSym.setColorationValue((double) entry.getValue().getN());
                                sdSym.setColorMap(ColorMaps.VIRIDIS.getColorMap());
                                sdPlot.addPlotObject(sdSym);
                            }
                        }
                    }

                    maxSite = maxSite + .1;
                    if (minSite > maxSite) {
                        minSite = maxSite - .1;
                    } else {
                        minSite = Math.max(0.0, minSite - .1);
                    }

                    maxCenterFreq = maxCenterFreq + .1;
                    if (minCenterFreq > maxCenterFreq) {
                        minCenterFreq = maxCenterFreq - .1;
                    } else {
                        minCenterFreq = minCenterFreq - 1.0;
                    }

                    sdPlot.setAxisLimits(new AxisLimits(Axis.Type.X, minCenterFreq, maxCenterFreq), new AxisLimits(Axis.Type.Y, minSite, maxSite));
                    sdPlot.replot();
                });
            }
        } catch (final InvocationTargetException ex) {
            // nop
        } catch (final InterruptedException ex) {
            log.warn("Interrupt during re-plotting of controller", ex);
            Thread.currentThread().interrupt();
        }
    }

    protected void preloadData() {
        // Placeholder to allow children to overload any pre-fetching needed before data
        // calls
    }

    private Double centerFreq(final Waveform waveform) {
        return ((waveform.getHighFrequency() - waveform.getLowFrequency()) / 2.0) + waveform.getLowFrequency();
    }

    @Override
    public void refreshView() {
        if (isVisible) {
            mapImpl.clearIcons();
            plotSpectra();
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
            try {
                if (resultsTab.isSelected() && evidCombo.getValue() != null) {
                    SnapshotUtils.writePng(folder, new Pair<>(getDisplayName(), resultsTab.getContent()), timestamp);
                    spectraControllers.forEach(spc -> {
                        final SpectralPlot plot = spc.getSpectralPlot();
                        try {
                            Files.write(
                                    Paths.get(folder + File.separator + getDisplayName() + "_" + plot.getTitle() + "_" + evidCombo.getValue() + timestamp + ".svg"),
                                        plot.getSubplot().getSVG().getBytes());
                        } catch (final IOException e) {
                            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
                        }
                    });
                } else {
                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "_Mw" + timestamp + ".svg"), mwPlot.getSVG().getBytes());
                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "Site_Stress" + timestamp + ".svg"), stressPlot.getSVG().getBytes());
                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "_Station_Event_SD" + timestamp + ".svg"), sdPlot.getSVG().getBytes());
                }
            } catch (final IOException e) {
                log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
            }
        };
    }

    private void selectDataByCriteria(final boolean selected, final String key) {
        final List<LabeledPlotPoint> points = plotPointMap.get(key);
        if (selected) {
            selectPoints(points);
        } else {
            deselectPoints(points);
        }
    }

    private void selectPoints(final List<LabeledPlotPoint> points) {
        if (points != null && !points.isEmpty()) {
            points.forEach(this::selectPoint);
        }
    }

    private void deselectPoints(final List<LabeledPlotPoint> selected) {
        if (selected != null && !selected.isEmpty()) {
            final List<LabeledPlotPoint> points = new ArrayList<>(selected);
            points.forEach(this::deselectPoint);
        }
    }

    private void selectPoint(final PlotPoint point) {
        runIfSymbolExists(point, SpectralPlot::selectPoint);
    }

    private void deselectPoint(final PlotPoint point) {
        runIfSymbolExists(point, SpectralPlot::deselectPoint);
    }

    private void runIfSymbolExists(final PlotPoint point, final BiConsumer<SpectralPlot, Point2D> xyPointFunction) {
        final Point2D xyPoint = new Point2D(point.getX(), point.getY());
        spectraControllers.forEach(spc -> {
            final Map<Point2D, List<Symbol>> symbolMap = spc.getSymbolMap();
            final SpectralPlot plot = spc.getSpectralPlot();
            final boolean existsInPlot = symbolMap.containsKey(xyPoint);
            if (existsInPlot) {
                xyPointFunction.accept(plot, xyPoint);
                plot.getSubplot().replot();
            }
        });
    }

    protected void setSymbolsActive(final List<Point2D> points, final boolean active) {
        spectraControllers.forEach(spc -> spc.getSpectralPlot().setPointsActive(points, active));
    }

    private void showContextMenu(final Set<Waveform> waveforms, final List<Point2D> points, final MouseEvent t, final BiConsumer<List<Point2D>, Boolean> activationFunc) {
        Platform.runLater(() -> {
            include.setOnAction(evt -> setActive(waveforms, points, true, activationFunc));
            exclude.setOnAction(evt -> setActive(waveforms, points, false, activationFunc));
            menu.show(spectraPlotPanel, t.getScreenX(), t.getScreenY());
        });
    }

    @Subscribe
    private void listener(final WaveformChangeEvent wce) {
        final List<Long> nonNull = wce.getIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
        synchronized (spectralMeasurements) {
            final Map<Long, SpectraMeasurement> activeMeasurements = spectralMeasurements.stream().collect(Collectors.toMap(x -> x.getWaveform().getId(), Function.identity()));
            if (wce.isAddOrUpdate()) {
                final List<Waveform> results = waveformClient.getWaveformMetadataFromIds(nonNull).collect(Collectors.toList()).block(Duration.ofSeconds(10l));
                if (results != null) {
                    results.forEach(md -> {
                        final SpectraMeasurement measurement = activeMeasurements.get(md.getId());
                        if (measurement != null) {
                            measurement.getWaveform().setActive(md.isActive());
                        }
                    });
                }
            } else if (wce.isDelete()) {
                nonNull.forEach(id -> {
                    final SpectraMeasurement measurement = activeMeasurements.remove(id);
                    if (measurement != null) {
                        spectralMeasurements.remove(measurement);
                    }
                });
            }
        }
        refreshView();
    }

    protected void handlePlotObjectClicked(final PlotObjectClick poc, final Function<Point2D, SpectraMeasurement> measurementFunc) {
        List<Point2D> points = poc.getPlotPoints();
        Set<Waveform> waveforms = new HashSet<>();

        // FIXME: This entire scheme is tremendously inefficient and needs a rework at
        // some point.
        for (SpectraPlotController spc : spectraControllers) {
            spc.getSpectralPlot().deselectAllPoints();
        }

        for (Point2D point : points) {
            SpectraMeasurement spectra = measurementFunc.apply(point);
            if (spectra != null && spectra.getWaveform() != null) {
                waveforms.add(spectra.getWaveform());
                for (SpectraPlotController spc : spectraControllers) {
                    if (poc.getMouseEvent().isPrimaryButtonDown()) {
                        final Map<Point2D, List<Symbol>> symbolMap = spc.getSymbolMap();
                        final SpectralPlot plot = spc.getSpectralPlot();
                        final boolean existsInPlot = symbolMap.containsKey(point);
                        if (existsInPlot) {
                            plot.selectPoint(point);
                        }
                    }
                }
            }
        }

        spectraControllers.forEach(spc -> spc.getSpectralPlot().getSubplot().replot());

        if (poc.getMouseEvent().isPrimaryButtonDown()) {
            showWaveformPopup(waveforms.stream().map(Waveform::getId).collect(Collectors.toSet()).toArray(new Long[0]));
            Platform.runLater(() -> menu.hide());
        } else if (poc.getMouseEvent().isSecondaryButtonDown()) {
            showContextMenu(waveforms, points, poc.getMouseEvent(), this::setSymbolsActive);
        }
    }

    @Override
    public void setVisible(final boolean visible) {
        isVisible = visible;
    }

    private void configureAxisShrink(final Button axisShrinkButton, final BooleanSupplier toggleAxisShrink, final Axis.Type axis) {
        final Label axisShrinkOn = new Label("><");
        axisShrinkOn.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        axisShrinkOn.setPadding(Insets.EMPTY);
        final Label axisShrinkOff = new Label("<>");
        axisShrinkOff.setStyle("-fx-font-weight:bold; -fx-font-size: 12px;");
        axisShrinkOff.setPadding(Insets.EMPTY);

        if (Axis.Type.Y == axis) {
            axisShrinkOn.setRotate(90.0);
            axisShrinkOff.setRotate(90.0);
        }

        axisShrinkButton.setGraphic(axisShrinkOn);
        axisShrinkButton.prefHeightProperty().bind(evidCombo.heightProperty());
        axisShrinkButton.setPadding(new Insets(axisShrinkButton.getPadding().getTop(), 0, axisShrinkButton.getPadding().getBottom(), 0));

        axisShrinkButton.setOnAction(e -> {
            if (toggleAxisShrink.getAsBoolean()) {
                axisShrinkButton.setGraphic(axisShrinkOff);
            } else {
                axisShrinkButton.setGraphic(axisShrinkOn);
            }
            refreshView();
        });
    }

    protected PropertyChangeListener getPlotpointObserver(final Supplier<Map<Point2D, SpectraMeasurement>> symbolMapSupplier) {
        return evt -> {
            Object po = evt.getNewValue();
            if (po instanceof PlotObjectClick && ((PlotObjectClick) po).getPlotPoints() != null) {
                handlePlotObjectClicked((PlotObjectClick) po, point -> symbolMapSupplier.get().get(point));
            }
        };
    }

    @FXML
    private void clearRefEvents() {
        removeRefEvents(mwParameters);
    }

    @FXML
    private void removeRefEvents() {
        final List<MeasuredMwDetails> evs = new ArrayList<>(eventTable.getSelectionModel().getSelectedIndices().size());
        eventTable.getSelectionModel().getSelectedIndices().forEach(i -> evs.add(mwParameters.get(i)));
        removeRefEvents(evs);
    }

    @FXML
    private void toggleValidationEvent() {
        final List<MeasuredMwDetails> evs = new ArrayList<>(eventTable.getSelectionModel().getSelectedIndices().size());
        eventTable.getSelectionModel().getSelectedIndices().forEach(i -> evs.add(mwParameters.get(i)));
        if (!evs.isEmpty()) {
            referenceEventClient.toggleValidationEventsByEventId(evs.stream().map(MeasuredMwDetails::getEventId).distinct().collect(Collectors.toList()))
                                .doOnComplete(() -> Platform.runLater(this::reloadData))
                                .subscribe();
        }
    }

    private void removeRefEvents(final List<MeasuredMwDetails> evs) {
        if (evs != null && !evs.isEmpty()) {
            referenceEventClient.removeReferenceEventsByEventId(evs.stream().map(MeasuredMwDetails::getEventId).distinct().collect(Collectors.toList()))
                                .doOnSuccess(v -> Platform.runLater(this::reloadData))
                                .subscribe();
        }
    }
}
