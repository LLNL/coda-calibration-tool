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

import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.SpectraMeasurementChangeEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TableView;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.ColorMaps;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;

/**
 * The EventTableController defines the event table used in both the CERT and
 * CCT views.
 *
 */
public class EventTableController implements RefreshableController {

    private static final Integer VALIDATION_Z_ORDER = 0;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

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
    protected TableColumn<MeasuredMwDetails, String> totalEnergyUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyMDACCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyMDACUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyMDACUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyMDACUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> totalEnergyMDACUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyRatioCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyStressCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyStressUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyStressUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyStressUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> energyStressUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> stressCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMwCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMeCol;

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
    protected TableColumn<MeasuredMwDetails, String> measuredMeUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMeUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMeUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredMeUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredStressUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredStressUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredStressUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredStressUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredCornerFreqUq1LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredCornerFreqUq1HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredCornerFreqUq2LowCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> measuredCornerFreqUq2HighCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, Integer> iterationsCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, Integer> dataCountCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, Integer> stationCountCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> bandCoverageCol;

    @FXML
    protected TableColumn<MeasuredMwDetails, String> likelyPoorlyConstrainedCol;

    @FXML
    protected TableColumn<LabeledPlotPoint, String> stationCol;

    protected List<SpectraMeasurement> spectralMeasurements = new ArrayList<>();
    private final ObservableList<String> evids = FXCollections.observableArrayList();

    protected SpectraClient spectraClient;
    protected ParameterClient paramClient;
    protected EventClient referenceEventClient;
    protected WaveformClient waveformClient;

    protected ObservableList<MeasuredMwDetails> mwParameters = FXCollections.observableArrayList();

    private final ObservableList<LabeledPlotPoint> stationSymbols = FXCollections.observableArrayList();

    private final SymbolStyleMapFactory symbolStyleMapFactory;
    private Map<String, PlotPoint> symbolStyleMap = new HashMap<>();

    protected ContextMenu menu;

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();

    protected List<SpectraPlotController> spectraControllers = new ArrayList<>(1);

    protected final PlotFactory plotFactory;
    private final EventBus bus;

    @Value("${show-energy-uq-summary:false}")
    private boolean showEnergyUQ = false;

    // TODO: Break this up into components so this isn't so incredibly huge.
    protected EventTableController(final SpectraClient spectraClient, final ParameterClient paramClient, final EventClient referenceEventClient, final WaveformClient waveformClient,
            final SymbolStyleMapFactory styleFactory, final PlotFactory plotFactory, final EventBus bus) {
        this.spectraClient = spectraClient;
        this.paramClient = paramClient;
        this.referenceEventClient = referenceEventClient;
        this.waveformClient = waveformClient;
        this.symbolStyleMapFactory = styleFactory;
        this.plotFactory = plotFactory;
        this.bus = bus;

        this.bus.register(this);
    }

    protected void runGuiUpdate(Runnable runnable) throws InvocationTargetException, InterruptedException {
    }

    protected List<MeasuredMwDetails> getEvents() {
        return null;
    }

    public void initialize() {

        final Label label = new Label("\uE2C4");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);

        CellBindingUtils.attachTextCellFactoriesString(evidCol, MeasuredMwDetails::getEventId);
        evidCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachTextCellFactoriesString(dateCol, MeasuredMwDetails::getDatetime);

        CellBindingUtils.attachTextCellFactories(mwCol, MeasuredMwDetails::getRefMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(stressCol, MeasuredMwDetails::getRefApparentStressInMpa, dfmt4);
        CellBindingUtils.attachTextCellFactories(valMwCol, MeasuredMwDetails::getValMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(valStressCol, MeasuredMwDetails::getValApparentStressInMpa, dfmt4);

        CellBindingUtils.attachTextCellFactories(measuredMwCol, MeasuredMwDetails::getMw, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq1LowCol, MeasuredMwDetails::getMw1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq1HighCol, MeasuredMwDetails::getMw1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq2LowCol, MeasuredMwDetails::getMw2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMwUq2HighCol, MeasuredMwDetails::getMw2Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMeCol, MeasuredMwDetails::getMe, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMeUq1LowCol, MeasuredMwDetails::getMw1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMeUq1HighCol, MeasuredMwDetails::getMw1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMeUq2LowCol, MeasuredMwDetails::getMw2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredMeUq2HighCol, MeasuredMwDetails::getMw2Max, dfmt4);

        CellBindingUtils.attachTextCellFactories(obsEnergyCol, MeasuredMwDetails::getObsEnergy, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyCol, MeasuredMwDetails::getTotalEnergy, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyUq1LowCol, MeasuredMwDetails::getLogTotalEnergy1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyUq1HighCol, MeasuredMwDetails::getLogTotalEnergy1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyUq2LowCol, MeasuredMwDetails::getLogTotalEnergy2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyUq2HighCol, MeasuredMwDetails::getLogTotalEnergy2Max, dfmt4);

        CellBindingUtils.attachTextCellFactories(totalEnergyMDACCol, MeasuredMwDetails::getTotalEnergyMDAC, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyMDACUq1LowCol, MeasuredMwDetails::getLogTotalEnergyMDAC1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyMDACUq1HighCol, MeasuredMwDetails::getLogTotalEnergyMDAC1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyMDACUq2LowCol, MeasuredMwDetails::getLogTotalEnergyMDAC2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(totalEnergyMDACUq2HighCol, MeasuredMwDetails::getLogTotalEnergyMDAC2Max, dfmt4);

        CellBindingUtils.attachTextCellFactories(energyRatioCol, MeasuredMwDetails::getEnergyRatio, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyStressCol, MeasuredMwDetails::getEnergyStress, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyStressUq1LowCol, MeasuredMwDetails::getObsAppStress1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyStressUq1HighCol, MeasuredMwDetails::getObsAppStress1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyStressUq2LowCol, MeasuredMwDetails::getObsAppStress2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(energyStressUq2HighCol, MeasuredMwDetails::getObsAppStress2Max, dfmt4);

        CellBindingUtils.attachTextCellFactories(measuredStressCol, MeasuredMwDetails::getApparentStressInMpa, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredStressUq1LowCol, MeasuredMwDetails::getApparentStress1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredStressUq1HighCol, MeasuredMwDetails::getApparentStress1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredStressUq2LowCol, MeasuredMwDetails::getApparentStress2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredStressUq2HighCol, MeasuredMwDetails::getApparentStress2Max, dfmt4);

        CellBindingUtils.attachTextCellFactories(measuredCornerFreqCol, MeasuredMwDetails::getCornerFreq, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredCornerFreqUq1LowCol, MeasuredMwDetails::getCornerFreq1Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredCornerFreqUq1HighCol, MeasuredMwDetails::getCornerFreq1Max, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredCornerFreqUq2LowCol, MeasuredMwDetails::getCornerFreq2Min, dfmt4);
        CellBindingUtils.attachTextCellFactories(measuredCornerFreqUq2HighCol, MeasuredMwDetails::getCornerFreq2Max, dfmt4);

        CellBindingUtils.attachTextCellFactories(mistfitCol, MeasuredMwDetails::getMisfit, dfmt4);
        CellBindingUtils.attachTextCellFactories(bandCoverageCol, MeasuredMwDetails::getBandCoverage, dfmt4);
        CellBindingUtils.attachTextCellFactoriesString(likelyPoorlyConstrainedCol, mw -> mw.isLikelyPoorlyConstrained().toString());

        stationCountCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getStationCount).orElseGet(() -> 0)).asObject());

        iterationsCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getIterations).orElseGet(() -> 0)).asObject());

        dataCountCol.setCellValueFactory(
                x -> Bindings.createIntegerBinding(() -> Optional.ofNullable(x).map(CellDataFeatures::getValue).map(MeasuredMwDetails::getDataCount).orElseGet(() -> 0)).asObject());

        eventTable.setItems(mwParameters);

        menu = new ContextMenu();
        MenuItem include = new MenuItem("Include Selected");
        menu.getItems().add(include);
        MenuItem exclude = new MenuItem("Exclude Selected");
        menu.getItems().add(exclude);
    }

    protected double centerFreq(final Double lowFrequency, final Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    @Override
    public Runnable getRefreshFunction() {
        return this::reloadData;
    }

    protected void reloadData() {
        try {
            final List<SharedFrequencyBandParameters> results = paramClient.getSharedFrequencyBandParameters().filter(Objects::nonNull).collectList().block(Duration.of(10l, ChronoUnit.SECONDS));

            if (results != null) {
                preloadData();

                runGuiUpdate(() -> {
                    mwParameters.clear();

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

                    maxEnergy = maxEnergy + Math.abs(maxEnergy * .1);
                    if (minEnergy > maxEnergy) {
                        minEnergy = maxEnergy - .1;
                    } else {
                        minEnergy = minEnergy - Math.abs(minEnergy * .1);
                    }

                    maxStress = maxStress + Math.abs(maxStress * .1);
                    if (minStress > maxStress) {
                        minStress = maxStress - .1;
                    } else {
                        minStress = minStress - Math.abs(minStress * .1);
                    }

                    final double[] xy = new double[2];
                    xy[0] = minMw;
                    xy[1] = maxMw;
                    final Line mwZeroLine = plotFactory.line(xy, xy, Color.LIGHTGRAY, LineStyles.DASH, 2);
                    mwZeroLine.setName("");
                    mwZeroLine.showInLegend(false);

                    xy[0] = minStress;
                    xy[1] = maxStress;
                    final Line stressZeroLine = plotFactory.line(xy, xy, Color.LIGHTGRAY, LineStyles.DASH, 2);
                    stressZeroLine.setName("");
                    stressZeroLine.showInLegend(false);
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
