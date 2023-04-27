/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.time.ZoneId;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.gui.util.FileDialogs;
import gov.llnl.gnem.apps.coda.calibration.gui.util.TextWrappingTableCell;
import gov.llnl.gnem.apps.coda.calibration.model.domain.EventSpectraReport;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.events.WaveformSelectionEvent;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.HiddenHeaderTableView;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.SpectraMeasurementChangeEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.TickFormat;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.ColorMaps;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import llnl.gnem.core.util.TimeT;

/**
 * The AbstractMeasurementController defines the common shared displays used in
 * both the Site and Measurement views.
 *
 * Generally containers all logic, data access, and plotting code necessary to
 * display a seismic source spectra for each event along with summary figures
 * for the dataset as a whole.
 *
 */
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

    @FXML
    protected StackPane energyVsMomentPane;
    private BasicPlot energyVsMomentPlot;

    @FXML
    protected StackPane apparentStressVsMomentPane;
    private BasicPlot apparentStressVsMomentPlot;

    @FXML
    protected StackPane cornerFreqVsMomentPane;
    private BasicPlot cornerFreqVsMomentPlot;

    protected StackPane spectraPlotPanel;

    @FXML
    protected ComboBox<String> evidCombo;

    @FXML
    protected HiddenHeaderTableView<Pair<String, String>> summaryTable;

    @FXML
    protected TableColumn<Pair<String, String>, String> summaryNameCol;

    @FXML
    protected TableColumn<Pair<String, String>, String> summaryValueCol;

    private final ObservableList<Pair<String, String>> summaryValues = FXCollections.observableArrayList();

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
    private Map<String, PlotPoint> symbolStyleMap = new HashMap<>();

    private final GeoMap mapImpl;

    private final MapPlottingUtilities iconFactory;

    private MenuItem exclude;
    private MenuItem include;
    protected ContextMenu menu;

    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalForcedOneLeadingZero();
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

    @FXML
    protected Button exportSpectraBtn;

    private boolean isVisible = false;

    protected List<SpectraPlotController> spectraControllers = new ArrayList<>(1);

    protected final PlotFactory plotFactory;
    private final EventBus bus;

    private ParamExporter paramExporter;

    @Value("${show-energy-uq-summary:false}")
    private boolean showEnergyUQ = false;

    // TODO: Break this up into components so this isn't so incredibly huge.
    protected AbstractMeasurementController(final SpectraClient spectraClient, final ParameterClient paramClient, final EventClient referenceEventClient, final WaveformClient waveformClient,
            final SymbolStyleMapFactory styleFactory, final GeoMap map, final MapPlottingUtilities iconFactory, final ParamExporter paramExporter, final PlotFactory plotFactory, final EventBus bus) {
        this.spectraClient = spectraClient;
        this.paramClient = paramClient;
        this.referenceEventClient = referenceEventClient;
        this.waveformClient = waveformClient;
        this.symbolStyleMapFactory = styleFactory;
        this.mapImpl = map;
        this.paramExporter = paramExporter;
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
        evidCombo.setVisibleRowCount(5);

        configureAxisShrink(xAxisShrink, () -> {
            shouldXAxisShrink = !shouldXAxisShrink;
            return shouldXAxisShrink;
        }, Axis.Type.LOG_X);

        configureAxisShrink(yAxisShrink, () -> {
            shouldYAxisShrink = !shouldYAxisShrink;
            return shouldYAxisShrink;
        }, Axis.Type.Y);

        final Label label = new Label("\uE2C4");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        exportSpectraBtn.setGraphic(label);
        exportSpectraBtn.setContentDisplay(ContentDisplay.CENTER);

        mwPlot = plotFactory.basicPlot();
        mwPlot.getTitle().setText("Mw");
        mwPlot.getTitle().setFontSize(16);
        mwPlot.addAxes(plotFactory.axis(Axis.Type.X, "Measured"), plotFactory.axis(Axis.Type.Y, "Comparison"));
        final AxisLimits mwXaxis = new AxisLimits(Axis.Type.X, 0.0, 10.0);
        final AxisLimits mwYaxis = new AxisLimits(Axis.Type.Y, 0.0, 10.0);
        mwPlot.setAxisLimits(mwXaxis, mwYaxis);
        mwPlot.setMargin(30, 40, 50, 50);
        mwPlot.attachToDisplayNode(mwPlotPane);

        stressPlot = plotFactory.basicPlot();
        stressPlot.getTitle().setText("Apparent Stress (MPa)");
        stressPlot.getTitle().setFontSize(16);
        stressPlot.addAxes(plotFactory.axis(Axis.Type.LOG_X, "Measured"), plotFactory.axis(Axis.Type.LOG_Y, "Comparison"));
        final AxisLimits stressXaxis = new AxisLimits(Axis.Type.X, 1.0, 1.0);
        final AxisLimits stressYaxis = new AxisLimits(Axis.Type.Y, 1.0, 1.0);
        stressPlot.setAxisLimits(stressXaxis, stressYaxis);
        stressPlot.setMargin(30, 40, 70, 50);
        stressPlot.attachToDisplayNode(stressPlotPane);

        sdPlot = plotFactory.basicPlot();
        sdPlot.getTitle().setText("Site correction overview (# stations)");
        sdPlot.getTitle().setFontSize(16);
        sdPlot.addAxes(plotFactory.axis(Axis.Type.LOG_X, "Frequency (Hz)"), plotFactory.axis(Axis.Type.Y, "Standard Deviation"));
        sdPlot.setAxisLimits(new AxisLimits(Axis.Type.LOG_X, 0.0, 1.0), new AxisLimits(Axis.Type.Y, 0.0, 2.0));
        sdPlot.showLegend(false);
        sdPlot.setMargin(30, 40, 50, null);
        sdPlot.attachToDisplayNode(sdPlotPane);

        energyVsMomentPlot = plotFactory.basicPlot();
        energyVsMomentPlot.getTitle().setText("Energy/Moment vs Moment");
        energyVsMomentPlot.getTitle().setFontSize(16);
        energyVsMomentPlot.getTitle().setYOffset(0.92);
        energyVsMomentPlot.addAxes(plotFactory.axis(Axis.Type.X, "log10 Mo (N-m)"), plotFactory.axis(Axis.Type.Y, "Energy (log J)/log10 Mo (N-m)"));

        Axis rightAxis = new BasicAxis(Axis.Type.X_TOP, "Mw");
        rightAxis.setTickFormat(TickFormat.LOG10_DYNE_CM_TO_MW);
        energyVsMomentPlot.addAxes(rightAxis);
        energyVsMomentPlot.showLegend(false);
        energyVsMomentPlot.setMargin(70, 50, 50, 50);
        energyVsMomentPlot.attachToDisplayNode(energyVsMomentPane);

        apparentStressVsMomentPlot = plotFactory.basicPlot();
        apparentStressVsMomentPlot.getTitle().setText("Apparent Stress vs Moment");
        apparentStressVsMomentPlot.getTitle().setFontSize(16);
        apparentStressVsMomentPlot.getTitle().setYOffset(0.92);
        apparentStressVsMomentPlot.addAxes(plotFactory.axis(Axis.Type.X, "log10 Mo (N-m)"), plotFactory.axis(Axis.Type.LOG_Y, "App. Stress (MPa)"));
        apparentStressVsMomentPlot.setAxisLimits(new AxisLimits(Axis.Type.LOG_Y, Math.log10(0.01), Math.log10(100.0)));
        rightAxis = new BasicAxis(Axis.Type.X_TOP, "Mw");
        rightAxis.setTickFormat(TickFormat.LOG10_DYNE_CM_TO_MW);
        apparentStressVsMomentPlot.addAxes(rightAxis);
        apparentStressVsMomentPlot.showLegend(false);
        apparentStressVsMomentPlot.setMargin(70, 40, 50, 50);
        apparentStressVsMomentPlot.attachToDisplayNode(apparentStressVsMomentPane);

        cornerFreqVsMomentPlot = plotFactory.basicPlot();
        cornerFreqVsMomentPlot.getTitle().setText("Moment vs Corner Frequency");
        cornerFreqVsMomentPlot.getTitle().setFontSize(16);
        cornerFreqVsMomentPlot.addAxes(plotFactory.axis(Axis.Type.LOG_X, "Corner Freq (Hz)"), plotFactory.axis(Axis.Type.Y, "log10 Mo (N-m)"));
        rightAxis = new BasicAxis(Axis.Type.Y_RIGHT, "Mw");
        rightAxis.setTickFormat(TickFormat.LOG10_DYNE_CM_TO_MW);
        cornerFreqVsMomentPlot.addAxes(rightAxis);
        cornerFreqVsMomentPlot.showLegend(true);
        cornerFreqVsMomentPlot.setMargin(30, 50, 50, 50);
        cornerFreqVsMomentPlot.attachToDisplayNode(cornerFreqVsMomentPane);

        evidCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && isVisible) {
                refreshView();
            }
        });

        menu = new ContextMenu();
        include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);

        summaryTable.setItems(summaryValues);
        summaryTable.setRowFactory(table -> new TableRow<Pair<String, String>>() {
            @Override
            protected void updateItem(Pair<String, String> entry, boolean b) {
                super.updateItem(entry, b);
                if (entry != null && entry.getX() != null && entry.getX().contains("(Model Fit)")) {
                    setStyle("-fx-font-weight:bold;");
                }
            }
        });

        CellBindingUtils.attachTextCellFactoriesString(summaryNameCol, Pair::getX);
        CellBindingUtils.attachTextCellFactoriesString(summaryValueCol, Pair::getY);
        summaryNameCol.setCellFactory(param -> new TextWrappingTableCell());
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
            spc.getSpectraDataMap().clear();
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
        boolean likelyPoorlyConstrained = false;
        MeasuredMwDetails mwDetails = null;

        if (evidCombo != null && evidCombo.getSelectionModel().getSelectedIndex() > 0) {
            filteredMeasurements = filterToEvent(evidCombo.getSelectionModel().getSelectedItem(), spectralMeasurements);
            fittingSpectra = getFitSpectra();
            final Spectra referenceSpectra = spectraClient.getReferenceSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            fittingSpectra.add(referenceSpectra);
            final Spectra validationSpectra = spectraClient.getValidationSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2));
            fittingSpectra.add(validationSpectra);

            if (filteredMeasurements != null && !filteredMeasurements.isEmpty() && filteredMeasurements.get(0).getWaveform() != null) {
                final Event event = filteredMeasurements.get(0).getWaveform().getEvent();
                String date = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneId.of("UTC")).format(event.getOriginTime().toInstant());
                String jDay = Integer.toString(new TimeT(event.getOriginTime()).getJdate()).substring(4);
                summaryValues.add(new Pair<>("Evid", event.getEventId()));
                summaryValues.add(new Pair<>("Date", String.format("%s (%s)", date, jDay)));
                summaryValues.add(new Pair<>("Origin Time", DateTimeFormatter.ISO_TIME.withZone(ZoneId.of("UTC")).format(event.getOriginTime().toInstant())));
                summaryValues.add(new Pair<>("Latitude", dfmt4.format(event.getLatitude())));
                summaryValues.add(new Pair<>("Longitude", dfmt4.format(event.getLongitude())));
                summaryValues.add(new Pair<>("Depth (km)", dfmt2.format(event.getDepth())));
                summaryValues.add(null); // Adds space between sections

                if (mwParameters != null && !mwParameters.isEmpty()) {
                    mwDetails = mwParameters.stream().filter(mwDetail -> event.getEventId().equalsIgnoreCase(mwDetail.getEventId())).findAny().orElseGet(MeasuredMwDetails::new);
                    likelyPoorlyConstrained = mwDetails.isLikelyPoorlyConstrained();
                }
            } else {
                filteredMeasurements = Collections.emptyList();
            }

            if (mwDetails != null && mwDetails.getEventId() != null) {
                final Spectra fitSpectra = fittingSpectra.get(0);
                summaryValues.add(
                        new Pair<>("Mw (Model Fit)",
                                   dfmt2.format(fitSpectra.getMw())
                                           + " ["
                                           + dfmt2.format(mwDetails.getMw2Min() - fitSpectra.getMw())
                                           + ", "
                                           + dfmt2.format(mwDetails.getMw2Max() - fitSpectra.getMw())
                                           + "]"));

                if (fitSpectra.getApparentStress() > 0.0) {
                    summaryValues.add(
                            new Pair<>("Apparent Stress (Model Fit)",
                                       dfmt2.format(fitSpectra.getApparentStress())
                                               + " MPa ["
                                               + dfmt2.format(mwDetails.getApparentStress2Min() - fitSpectra.getApparentStress())
                                               + ", "
                                               + dfmt2.format(mwDetails.getApparentStress2Max() - fitSpectra.getApparentStress())
                                               + "]"));
                }

                summaryValues.add(
                        new Pair<>("Energy (Model Fit)",
                                   dfmt2.format(fitSpectra.getLogTotalEnergyMDAC())
                                           + " ["
                                           + dfmt2.format(mwDetails.getLogTotalEnergyMDAC2Min() - fitSpectra.getLogTotalEnergyMDAC())
                                           + ", "
                                           + dfmt2.format(mwDetails.getLogTotalEnergyMDAC2Max() - fitSpectra.getLogTotalEnergyMDAC())
                                           + "] log J"));

                if (showEnergyUQ) {

                    summaryValues.add(
                            new Pair<>("Total Energy",
                                       dfmt2.format(fitSpectra.getLogTotalEnergy())
                                               + " ["
                                               + dfmt2.format(mwDetails.getLogTotalEnergy2Min() - fitSpectra.getLogTotalEnergy())
                                               + ", "
                                               + dfmt2.format(mwDetails.getLogTotalEnergy2Max() - fitSpectra.getLogTotalEnergy())
                                               + "] log J"));

                    summaryValues.add(
                            new Pair<>("Me (Total Energy)",
                                       dfmt2.format(mwDetails.getMe())
                                               + " ["
                                               + dfmt2.format(mwDetails.getMe2Min() - mwDetails.getMe())
                                               + ", "
                                               + dfmt2.format(mwDetails.getMe2Max() - mwDetails.getMe())
                                               + "]"));

                    summaryValues.add(
                            new Pair<>("Observed Apparent Stress",
                                       dfmt2.format(fitSpectra.getObsAppStress())
                                               + " MPa ["
                                               + dfmt2.format(mwDetails.getObsAppStress2Min() - fitSpectra.getObsAppStress())
                                               + ", "
                                               + dfmt2.format(mwDetails.getObsAppStress2Max() - fitSpectra.getObsAppStress())
                                               + "]"));
                } else {
                    summaryValues.add(new Pair<>("Total Energy", dfmt2.format(fitSpectra.getLogTotalEnergy()) + " log J"));

                    summaryValues.add(new Pair<>("Me (Total Energy)", dfmt2.format(mwDetails.getMe())));

                    summaryValues.add(new Pair<>("Observed Apparent Stress", dfmt2.format(fitSpectra.getObsAppStress()) + " MPa"));
                }

                summaryValues.add(new Pair<>("Observed / Total Energy", dfmt2.format(100.0 * (Math.pow(10, fitSpectra.getObsEnergy()) / Math.pow(10, fitSpectra.getLogTotalEnergy()))) + " %"));
                summaryValues.add(
                        new Pair<>("Extrapolated / Total Energy", dfmt2.format(100.0 - (100.0 * (Math.pow(10, fitSpectra.getObsEnergy()) / Math.pow(10, fitSpectra.getLogTotalEnergy())))) + " %"));
            }

            if (referenceSpectra != null && SPECTRA_TYPES.REF.equals(referenceSpectra.getType())) {
                summaryValues.add(new Pair<>("Reference Mw", dfmt2.format(referenceSpectra.getMw())));
                if (referenceSpectra.getApparentStress() > 0.0) {
                    summaryValues.add(new Pair<>("Reference Apparent Stress", dfmt2.format(referenceSpectra.getApparentStress()) + " MPa"));
                }
            }

            if (validationSpectra != null && SPECTRA_TYPES.VAL.equals(validationSpectra.getType())) {
                summaryValues.add(new Pair<>("Validation Mw", dfmt2.format(validationSpectra.getMw())));
                if (validationSpectra.getApparentStress() > 0.0) {
                    summaryValues.add(new Pair<>("Validation Apparent Stress", dfmt2.format(validationSpectra.getApparentStress()) + " MPa"));
                }
            }

            if (mwDetails != null && mwDetails.getEventId() != null) {
                summaryValues.add(null);
                summaryValues.add(new Pair<>("Iterations", Integer.toString(mwDetails.getIterations())));
                summaryValues.add(new Pair<>("Data Count", Integer.toString(mwDetails.getDataCount())));
            }
        } else {
            filteredMeasurements = spectralMeasurements;
            fittingSpectra = null;
        }

        final List<SpectraMeasurement> selectedEventMeasurements = filteredMeasurements;

        final boolean showPoorlyConstrainedBanner = likelyPoorlyConstrained;

        spectraControllers.forEach(spc -> {
            if (fittingSpectra != null && spc.shouldShowFits()) {
                spc.getSpectralPlot().plotXYdata(toPlotPoints(selectedEventMeasurements, spc.getDataFunc()), fittingSpectra, null);
            } else {
                spc.getSpectralPlot().plotXYdata(toPlotPoints(selectedEventMeasurements, spc.getDataFunc()), null, null);
            }
            spc.getSpectraDataMap().putAll(mapSpectraToPoint(selectedEventMeasurements, spc.getDataFunc()));

            spc.showConstraintWarningBanner(showPoorlyConstrainedBanner);
        });
        mapMeasurements(selectedEventMeasurements);

        minY.set(100.0);
        maxY.set(0.0);
        //Dyne-cm to Newton meters
        final DoubleSummaryStatistics stats = selectedEventMeasurements.stream()
                                                                       .filter(Objects::nonNull)
                                                                       .map(val -> val.getPathAndSiteCorrected() - 7.0)
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
                                                   //Dyne-cm to nm for plot, in log
                                                   spectra -> new Point2D(centerFreq(spectra.getWaveform().getLowFrequency(), spectra.getWaveform().getHighFrequency()), func.apply(spectra) - 7.0),
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
                                                                                                         func.apply(spectra) - 7.0,
                                                                                                         //Dyne-cm to nm for plot, in log
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

    protected PlotPoint getPlotPoint(final String key, final boolean active) {
        final PlotPoint pp;
        if (symbolStyleMap.containsKey(key)) {
            pp = new PlotPoint(symbolStyleMap.get(key));
            if (!active) {
                pp.setColor(Color.GRAY);
            }
        } else {
            pp = new PlotPoint();
            pp.setColor(Color.GRAY);
            pp.setStyle(SymbolStyles.X);
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

    protected double centerFreq(final Double lowFrequency, final Double highFrequency) {
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

                    cornerFreqVsMomentPlot.clear();
                    apparentStressVsMomentPlot.clear();
                    energyVsMomentPlot.clear();

                    final List<MeasuredMwDetails> evs = getEvents();
                    final List<Symbol> mwPlotSymbols = new ArrayList<>();
                    final List<Symbol> stressPlotSymbols = new ArrayList<>();

                    double minMw = 10.0;
                    double maxMw = 0.0;
                    double minEnergy = -7;
                    double maxEnergy = -1;
                    double minStress = 0.01;
                    double maxStress = 100.0;
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

                            double m0 = (1.5 * mw) + 9.1;
                            if (ev.getApparentStressInMpa() != null && ev.getApparentStressInMpa() != 0.0) {
                                apparentStressVsMomentPlot.addPlotObject(
                                        plotFactory.createSymbol(SymbolStyles.CIRCLE, "", m0, ev.getApparentStressInMpa(), Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false));
                            }
                            if (ev.getCornerFreq() != null && ev.getCornerFreq() != 0.0) {
                                Symbol symbol = plotFactory.createSymbol(SymbolStyles.CIRCLE, "Data", ev.getCornerFreq(), m0, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false);
                                symbol.showInLegend(false);
                                cornerFreqVsMomentPlot.addPlotObject(symbol);
                            }
                            if (ev.getTotalEnergy() != null && ev.getTotalEnergy() != 0.0) {
                                final double energy = ev.getTotalEnergy() - m0;
                                if (energy < minEnergy) {
                                    minEnergy = energy;
                                }
                                if (energy > maxEnergy) {
                                    maxEnergy = energy;
                                }
                                Symbol symbol = plotFactory.createSymbol(SymbolStyles.CIRCLE, "Data", m0, energy, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false);
                                symbol.showInLegend(false);
                                energyVsMomentPlot.addPlotObject(symbol);
                            }

                            if (ev.getRefMw() != null && ev.getRefMw() != 0.0) {
                                final Double ref = ev.getRefMw();
                                if (ref < minMw) {
                                    minMw = ref;
                                }
                                if (ref > maxMw) {
                                    maxMw = ref;
                                }

                                final Symbol mwSym = plotFactory.createSymbol(SymbolStyles.CIRCLE, "Ref.", mw, ref, Color.RED, Color.RED, Color.RED, ev.getEventId(), false);
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

                                final Symbol valSym = plotFactory.createSymbol(SymbolStyles.SQUARE, "Val.", mw, valMw, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false);
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

                                    final Symbol stressSym = plotFactory.createSymbol(SymbolStyles.CIRCLE, "Ref.", stress, refStress, Color.RED, Color.RED, Color.RED, ev.getEventId(), false);
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

                                    final Symbol valSym = plotFactory.createSymbol(SymbolStyles.SQUARE, "Val.", stress, valStress, Color.BLACK, Color.BLACK, Color.BLACK, ev.getEventId(), false);
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

                    maxEnergy = maxEnergy + Math.abs(maxEnergy * .1);
                    if (minEnergy > maxEnergy) {
                        minEnergy = maxEnergy - .1;
                    } else {
                        minEnergy = minEnergy - Math.abs(minEnergy * .1);
                    }

                    energyVsMomentPlot.setAxisLimits(new AxisLimits(Axis.Type.Y, minEnergy, maxEnergy));

                    double minM0 = (1.5 * minMw) + 9.1;
                    double maxM0 = (1.5 * maxMw) + 9.1;
                    double C = 5 * Math.pow(10, -5);
                    Line energyConstantLine = plotFactory.line(new double[] { minM0, maxM0 }, new double[] { Math.log10(C), Math.log10(C) }, Color.BLACK, LineStyles.SOLID, 2);
                    energyConstantLine.setName("1.5 MPa");
                    energyConstantLine.showInLegend(true);
                    energyVsMomentPlot.addPlotObject(energyConstantLine);

                    energyConstantLine = plotFactory.line(new double[] { minM0, maxM0 }, new double[] { Math.log10(C) - 1.0, Math.log10(C) - 1.0 }, Color.BLACK, LineStyles.DASH, 2);
                    energyConstantLine.setName("0.15 MPa");
                    energyVsMomentPlot.addPlotObject(energyConstantLine);

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

                    //f0=4.9x10^6*vs*(Drop/M0)^1/3

                    Line stressConstantLine = createStressConstantLine(minM0, maxM0, 0.1, LineStyles.DASH);
                    stressConstantLine.setName("0.1 MPa");
                    stressConstantLine.showInLegend(true);
                    cornerFreqVsMomentPlot.addPlotObject(stressConstantLine);

                    stressConstantLine = plotFactory.line(new double[] { minM0, maxM0 }, new double[] { 0.1, 0.1 }, Color.BLACK, LineStyles.DASH, 2);
                    stressConstantLine.setName("0.1 MPa");
                    apparentStressVsMomentPlot.addPlotObject(stressConstantLine);

                    stressConstantLine = createStressConstantLine(minM0, maxM0, 1.0, LineStyles.SOLID);
                    stressConstantLine.setName("1.0 MPa");
                    stressConstantLine.showInLegend(true);
                    cornerFreqVsMomentPlot.addPlotObject(stressConstantLine);

                    stressConstantLine = plotFactory.line(new double[] { minM0, maxM0 }, new double[] { 1.0, 1.0 }, Color.BLACK, LineStyles.SOLID, 2);
                    stressConstantLine.setName("1.0 MPa");
                    apparentStressVsMomentPlot.addPlotObject(stressConstantLine);

                    stressConstantLine = createStressConstantLine(minM0, maxM0, 10.0, LineStyles.DOT);
                    stressConstantLine.setName("10.0 MPa");
                    stressConstantLine.showInLegend(true);
                    cornerFreqVsMomentPlot.addPlotObject(stressConstantLine);

                    stressConstantLine = plotFactory.line(new double[] { minM0, maxM0 }, new double[] { 10.0, 10.0 }, Color.BLACK, LineStyles.DOT, 2);
                    stressConstantLine.setName("10.0 MPa");
                    apparentStressVsMomentPlot.addPlotObject(stressConstantLine);

                    apparentStressVsMomentPlot.replot();
                    cornerFreqVsMomentPlot.replot();
                    energyVsMomentPlot.replot();
                });

                //Wastes a little compute but KISS
                Map<String, PlotPoint> styleMap = symbolStyleMapFactory.build(spectralMeasurements, specMeas -> specMeas.getWaveform().getStream().getStation().getStationName());
                styleMap.entrySet().forEach(e -> symbolStyleMap.putIfAbsent(e.getKey(), e.getValue()));

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

                    sdPlot.setAxisLimits(new AxisLimits(Axis.Type.LOG_X, Math.log10(minCenterFreq), Math.log10(maxCenterFreq)), new AxisLimits(Axis.Type.Y, minSite, maxSite));
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

    private Line createStressConstantLine(double minM0, double maxM0, double stressMpa, LineStyles style) {
        double drop = 4.3 * stressMpa;
        double vs = 3.2;
        double f0const = (4.9 * 2.5) * Math.pow(10, 6) * vs;
        //Formula is in dyne so adding in + 7 to get back there from n-m
        Line stressConstantLine = plotFactory.line(
                new double[] { f0const * Math.pow(drop / Math.pow(10, minM0 + 7.0), 1.0 / 3.0), f0const * Math.pow(drop / Math.pow(10, maxM0 + 7.0), 1.0 / 3.0) },
                    new double[] { minM0, maxM0 },
                    Color.BLACK,
                    style,
                    2);
        return stressConstantLine;
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
            summaryValues.clear();
            mapImpl.clearIcons();
            plotSpectra();
            summaryTable.refresh();
        }
    }

    @Override
    public Runnable getRefreshFunction() {
        return this::reloadData;
    }

    public void exportSpectra() {
        Platform.runLater(() -> {
            if (evidCombo.getValue() != null) {
                final File file = FileDialogs.openFileSaveDialog(evidCombo.getValue(), "-Spectra.json", spectraPlotPanel.getScene().getWindow());
                if (file != null && FileDialogs.ensureFileIsWritable(file)) {
                    final String filePath = file.getAbsolutePath();
                    List<EventSpectraReport> formattedExportValues = getFormattedValues(evidCombo.getValue(), spectralMeasurements);
                    paramExporter.writeSpectra(Paths.get(FilenameUtils.getFullPath(filePath)), FilenameUtils.getName(filePath), formattedExportValues);
                }
            }
        });
    }

    private List<EventSpectraReport> getFormattedValues(String eventId, List<SpectraMeasurement> measurements) {
        Map<String, EventSpectraReport> valuesMap = new HashMap<>();
        if (eventId != null && measurements != null) {
            List<SpectraMeasurement> filteredMeasurements = measurements;
            if (!eventId.equalsIgnoreCase("All")) {
                filteredMeasurements = filterToEvent(eventId, measurements);
            }
            Map<String, Map<Double, Double>> averageValues = new HashMap<>();
            for (SpectraMeasurement meas : filteredMeasurements) {
                String key = meas.getWaveform().getEvent().getEventId()
                        + "-"
                        + meas.getWaveform().getStream().getStation().getNetworkName()
                        + "-"
                        + meas.getWaveform().getStream().getStation().getStationName();
                valuesMap.computeIfAbsent(
                        key,
                            k -> new EventSpectraReport(meas.getWaveform().getEvent().getEventId(),
                                                        meas.getWaveform().getStream().getStation().getNetworkName(),
                                                        meas.getWaveform().getStream().getStation().getStationName(),
                                                        new ArrayList<>()));

                Double freq = centerFreq(meas.getWaveform());
                valuesMap.get(key).add(new Pair<>(freq, meas.getPathAndSiteCorrected()));
                averageValues.computeIfAbsent(meas.getWaveform().getEvent().getEventId(), k -> new TreeMap<>()).merge(freq, meas.getPathAndSiteCorrected(), (l, r) -> (l + r) / 2.0);
            }

            for (Entry<String, Map<Double, Double>> average : averageValues.entrySet()) {
                valuesMap.put(
                        "Average-" + average.getKey(),
                            new EventSpectraReport(average.getKey(),
                                                   null,
                                                   "Average",
                                                   average.getValue().entrySet().stream().map(e -> new Pair<>(e.getKey(), e.getValue())).collect(Collectors.toList())));
            }
        }
        return new ArrayList<>(valuesMap.values());
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

                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "_Energy_vs_Moment" + timestamp + ".svg"), energyVsMomentPlot.getSVG().getBytes());
                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "_CornerFreq_vs_Moment" + timestamp + ".svg"), cornerFreqVsMomentPlot.getSVG().getBytes());
                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "_Apparent_Stress_vs_Moment" + timestamp + ".svg"), apparentStressVsMomentPlot.getSVG().getBytes());
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
        //Dyne-cm to Newton-meter
        final Point2D xyPoint = new Point2D(point.getX(), point.getY() - 7.0);
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

    @Subscribe
    private void listener(final SpectraMeasurementChangeEvent changeEvent) {
        final List<Long> nonNull = changeEvent.getIds().stream().filter(Objects::nonNull).collect(Collectors.toList());
        synchronized (spectralMeasurements) {
            final Map<Long, SpectraMeasurement> activeMeasurements = spectralMeasurements.stream().collect(Collectors.toMap(x -> x.getWaveform().getId(), Function.identity()));
            final List<SpectraMeasurement> results = spectraClient.getMeasuredSpectraMetadataByIds(nonNull).collect(Collectors.toList()).block(Duration.ofSeconds(10l));
            if (results != null) {
                if (changeEvent.isAddOrUpdate()) {
                    results.forEach(md -> {
                        final SpectraMeasurement measurement = activeMeasurements.get(md.getWaveform().getId());
                        if (measurement != null) {
                            measurement.setPathAndSiteCorrected(md.getPathAndSiteCorrected());
                            measurement.setRawAtMeasurementTime(md.getRawAtMeasurementTime());
                            measurement.setRawAtStart(md.getRawAtStart());
                        }
                    });
                } else if (changeEvent.isDelete()) {
                    results.forEach(md -> {
                        final SpectraMeasurement measurement = activeMeasurements.remove(md.getWaveform().getId());
                        if (measurement != null) {
                            spectralMeasurements.remove(measurement);
                        }
                    });
                }
            }
        }
    }

    protected void handlePlotObjectClicked(final PlotObjectClick poc, final Function<Point2D, SpectraMeasurement> measurementFunc) {
        List<Point2D> points = poc.getPlotPoints();
        Set<Waveform> waveforms = new HashSet<>();
        List<Waveform> selectedData = spectraControllers.stream()
                                                        .flatMap(spc -> spc.getSpectralPlot().getSelectedPoints().stream())
                                                        .map(measurementFunc::apply)
                                                        .map(SpectraMeasurement::getWaveform)
                                                        .collect(Collectors.toList());

        if (poc.getMouseEvent().isPrimaryButtonDown() || selectedData.isEmpty()) {
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
                            //Dyne-cm to Newton-meter
                            Point2D testPoint = new Point2D(point.getX(), point.getY() - 7.0);
                            final boolean existsInPlot = symbolMap.containsKey(testPoint);
                            if (existsInPlot) {
                                plot.selectPoint(testPoint);
                            }
                        }
                    }
                }
            }

            spectraControllers.forEach(spc -> spc.getSpectralPlot().getSubplot().replot());
            if (poc.getMouseEvent().isPrimaryButtonDown()) {
                showWaveformPopup(waveforms.stream().map(Waveform::getId).collect(Collectors.toSet()).toArray(new Long[0]));
                Platform.runLater(() -> menu.hide());
            } else {
                showContextMenu(waveforms, points, poc.getMouseEvent(), this::setSymbolsActive);
            }
        } else if (poc.getMouseEvent().isSecondaryButtonDown()) {
            waveforms.addAll(selectedData);
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
}
