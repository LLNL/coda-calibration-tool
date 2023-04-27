/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.TickFormat;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import reactor.core.scheduler.Schedulers;

@Component
public class SiteController extends AbstractMeasurementController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String AVERAGE_LABEL = "Average";

    private static final String X_AXIS_LABEL = "center freq (Hz)";

    private static final String DISPLAY_NAME = "Site";

    @FXML
    private Tab siteTermsTab;

    @FXML
    private StackPane sitePane;

    @FXML
    private StackPane rawPlotNode;

    @FXML
    private StackPane sitePlotNode;

    @FXML
    private StackPane siteTermsPlotPane;
    private BasicPlot siteTermsPlot;

    @FXML
    private ComboBox<String> siteTermStationCombo;
    private ObservableList<String> stationList;

    @FXML
    private StackPane relativeSiteTermsPlotPane;
    private BasicPlot relativeSiteTermsPlot;

    @FXML
    private TitledPane rawTitledPane;

    @FXML
    private SplitPane rawSplitPane;

    private List<SiteFrequencyBandParameters> siteTerms;

    @Autowired
    private SiteController(final SpectraClient spectraClient, final ParameterClient paramClient, final EventClient referenceEventClient, final WaveformClient waveformClient,
            final SymbolStyleMapFactory styleFactory, final GeoMap map, final MapPlottingUtilities iconFactory, final ParamExporter paramExporter, final PlotFactory plotFactory, final EventBus bus) {
        super(spectraClient, paramClient, referenceEventClient, waveformClient, styleFactory, map, iconFactory, paramExporter, plotFactory, bus);
    }

    @Override
    @FXML
    public void initialize() {
        spectraPlotPanel = sitePane;
        stationList = FXCollections.observableArrayList();
        super.initialize();

        final SpectraPlotController raw = new SpectraPlotController(SpectraMeasurement::getRawAtMeasurementTime);
        SpectralPlot plot = raw.getSpectralPlot();
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(raw::getSpectraDataMap));
        plot.setLabels("Raw Plot", X_AXIS_LABEL, "log10(non-dim)");
        plot.setAutoCalculateYaxisRange(true);
        rawPlotNode.getChildren().add(plot);

        final SpectraPlotController site = new SpectraPlotController(SpectraMeasurement::getPathAndSiteCorrected);
        plot = site.getSpectralPlot();
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(site::getSpectraDataMap));
        plot.setLabels("Moment Rate Spectra", X_AXIS_LABEL, "log10(N-m)");
        plot.getSubplot().setMargin(60, 40, 50, null);
        final Axis rightAxis = new BasicAxis(Axis.Type.Y_RIGHT, "Mw");
        rightAxis.setTickFormat(TickFormat.LOG10_DYNE_CM_TO_MW);
        plot.getSubplot().addAxes(rightAxis);
        site.setShowCornerFrequencies(true);
        site.setYAxisResizable(true);
        site.setShouldShowFits(true);
        sitePlotNode.getChildren().add(plot);

        spectraControllers.add(raw);
        spectraControllers.add(site);

        rawTitledPane.expandedProperty().addListener((v, o, n) -> {
            if (n != null) {
                if (n.booleanValue()) {
                    rawSplitPane.setDividerPositions(0.5);
                } else {
                    rawSplitPane.setDividerPositions(1.0);
                }
            }
        });

        siteTermsPlot = plotFactory.lineAndMarkerScatterPlot();
        siteTermsPlot.getTitle().setText("Site and transfer function corrections");
        siteTermsPlot.getTitle().setFontSize(16);
        siteTermsPlot.setSymbolSize(12);
        siteTermsPlot.addAxes(plotFactory.axis(Axis.Type.LOG_X, "Frequency (Hz)"), plotFactory.axis(Axis.Type.Y, "Site and transfer correction log10(N-m)"));
        siteTermsPlot.setAxisLimits(new AxisLimits(Axis.Type.X, 0.0, 10.0), new AxisLimits(Axis.Type.Y, 0.0, 2.0));
        siteTermsPlot.setMargin(30, 40, 50, null);
        siteTermsPlot.attachToDisplayNode(siteTermsPlotPane);

        siteTermStationCombo.setItems(stationList);
        siteTermStationCombo.setVisibleRowCount(5);
        siteTermStationCombo.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue) && siteTermStationCombo.isVisible()) {
                refreshRelativeSitePlot(newValue);
            }
        });

        relativeSiteTermsPlot = plotFactory.lineAndMarkerScatterPlot();
        relativeSiteTermsPlot.getTitle().setText("Relative site terms");
        relativeSiteTermsPlot.getTitle().setFontSize(16);
        relativeSiteTermsPlot.setSymbolSize(12);
        relativeSiteTermsPlot.addAxes(plotFactory.axis(Axis.Type.LOG_X, "Frequency (Hz)"), plotFactory.axis(Axis.Type.Y, "Relative Amplification (log10)"));
        relativeSiteTermsPlot.setAxisLimits(new AxisLimits(Axis.Type.X, 0.0, 10.0), new AxisLimits(Axis.Type.Y, 0.0, 2.0));
        relativeSiteTermsPlot.setMargin(60, 40, 50, null);
        relativeSiteTermsPlotPane.setPickOnBounds(false);
        relativeSiteTermsPlot.attachToDisplayNode(relativeSiteTermsPlotPane);
    }

    @Override
    protected String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected List<Spectra> getFitSpectra() {
        return new ArrayList<>(spectraClient.getFitSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2)));
    }

    @Override
    protected void setActive(final Set<Waveform> waveforms, final List<Point2D> points, final boolean active, final BiConsumer<List<Point2D>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().map(Waveform::getId).collect(Collectors.toList()), active).subscribe(s -> activationFunc.accept(points, active));
    }

    @Override
    protected List<SpectraMeasurement> getSpectraData() {
        return spectraClient.getMeasuredSpectraMetadata()
                            .filter(Objects::nonNull)
                            .filter(spectra -> spectra.getWaveform() != null && spectra.getWaveform().getEvent() != null && spectra.getWaveform().getStream() != null)
                            .toStream()
                            .collect(Collectors.toList());
    }

    @Override
    protected void runGuiUpdate(final Runnable runnable) throws InvocationTargetException, InterruptedException {
        Platform.runLater(runnable);
    }

    @Override
    protected void reloadData() {
        super.reloadData();
        siteTerms = paramClient.getSiteSpecificFrequencyBandParameters().filter(Objects::nonNull).collectList().block(Duration.of(10l, ChronoUnit.SECONDS));

        Set<String> stationSet = new TreeSet<>();
        List<Symbol> symbols = new ArrayList<>();

        double minSite = 1E2;
        double maxSite = -1E2;
        double minCenterFreq = 1E2;
        double maxCenterFreq = -1E2;

        Map<Double, Symbol> averageSymbols = new TreeMap<>();
        Map<Double, SiteFrequencyBandParameters> averageSiteTerms = new TreeMap<>();

        siteTerms.sort((l, r) -> Double.compare(l.getLowFrequency(), r.getLowFrequency()));
        for (SiteFrequencyBandParameters val : siteTerms) {
            if (val != null && val.getStation() != null) {
                final String key = val.getStation().getStationName();
                stationSet.add(key);
                final PlotPoint pp = getPlotPoint(key, true);
                double centerFreq = centerFreq(val.getLowFrequency(), val.getHighFrequency());
                //Dyne-cm to nm for plot, in log
                double site = val.getSiteTerm() - 7.0;

                if (site < minSite) {
                    minSite = site;
                }
                if (site > maxSite) {
                    maxSite = site;
                }
                if (centerFreq < minCenterFreq) {
                    minCenterFreq = centerFreq;
                }
                if (centerFreq > maxCenterFreq) {
                    maxCenterFreq = centerFreq;
                }

                final LabeledPlotPoint point = new LabeledPlotPoint(key, new PlotPoint(centerFreq, site, pp.getStyle(), pp.getColor(), pp.getColor()));
                symbols.add(plotFactory.createSymbol(point.getStyle(), key, point.getX(), point.getY(), point.getColor(), Color.BLACK, point.getColor(), key, true));

                averageSymbols.merge(
                        val.getLowFrequency(),
                            plotFactory.createSymbol(point.getStyle(), AVERAGE_LABEL, point.getX(), point.getY(), Color.BLACK, Color.BLACK, Color.BLACK, key, true),
                            (l, r) -> {
                                l.setY((l.getY() + r.getY()) / 2.0);
                                l.setText(AVERAGE_LABEL);
                                return l;
                            });

                averageSiteTerms.compute(val.getLowFrequency(), (k, v) -> {
                    SiteFrequencyBandParameters avg;
                    if (v == null) {
                        avg = new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(val).setSiteTerm(site);
                        avg.getStation().setStationName(AVERAGE_LABEL);
                    } else {
                        avg = v;
                    }
                    avg.setSiteTerm((avg.getSiteTerm() + val.getSiteTerm()) / 2.0);
                    return avg;
                });

            }
        }

        maxSite = maxSite + .1;
        if (minSite > maxSite) {
            minSite = maxSite - .1;
        } else {
            minSite = minSite - .1;
        }

        maxCenterFreq = maxCenterFreq + .1;
        if (minCenterFreq > maxCenterFreq) {
            minCenterFreq = maxCenterFreq - .1;
        } else {
            minCenterFreq = minCenterFreq - 1.0;
        }
        final double minX = minCenterFreq;
        final double maxX = maxCenterFreq;
        final double minY = minSite;
        final double maxY = maxSite;

        symbols.addAll(averageSymbols.values());
        siteTerms.addAll(averageSiteTerms.values());

        try {
            runGuiUpdate(() -> {
                siteTermsPlot.clear();
                relativeSiteTermsPlot.clear();
                stationList.clear();

                for (Symbol symbol : symbols) {
                    siteTermsPlot.addPlotObject(symbol);
                }
                siteTermsPlot.setAxisLimits(new AxisLimits(Axis.Type.X, minX, maxX), new AxisLimits(Axis.Type.Y, minY, maxY));
                siteTermsPlot.replot();

                stationList.add(AVERAGE_LABEL);
                stationList.addAll(stationSet);
                if (!stationList.isEmpty()) {
                    siteTermStationCombo.getSelectionModel().select(0);
                }
            });
        } catch (InvocationTargetException | InterruptedException e) {
            //nop
        }
    }

    private void refreshRelativeSitePlot(String stationName) {
        if (siteTerms != null) {
            try {
                double minSite = Double.MAX_VALUE;
                double maxSite = -Double.MAX_VALUE;
                double minCenterFreq = Double.MAX_VALUE;
                double maxCenterFreq = -Double.MAX_VALUE;

                siteTerms.sort((l, r) -> Double.compare(l.getLowFrequency(), r.getLowFrequency()));

                List<Symbol> symbols = new ArrayList<>();
                if (stationName != null) {
                    List<SiteFrequencyBandParameters> relativeTerms = siteTerms.stream()
                                                                               .filter(sfb -> stationName.equalsIgnoreCase(sfb.getStation().getStationName()))
                                                                               .sorted((l, r) -> Double.compare(l.getLowFrequency(), r.getLowFrequency()))
                                                                               .collect(Collectors.toList());

                    if (relativeTerms != null && !relativeTerms.isEmpty()) {
                        for (SiteFrequencyBandParameters val : siteTerms) {
                            if (val != null && val.getStation() != null) {
                                final String key = val.getStation().getStationName();
                                final PlotPoint pp = getPlotPoint(key, true);
                                double centerFreq = centerFreq(val.getLowFrequency(), val.getHighFrequency());

                                SiteFrequencyBandParameters relativeTerm = relativeTerms.stream().filter(sfb -> val.getLowFrequency() == sfb.getLowFrequency()).findAny().orElse(null);
                                if (relativeTerm != null) {
                                    //For plotting purposes they want to invert the Y axis
                                    double site = (val.getSiteTerm() - relativeTerm.getSiteTerm()) * -1.0;

                                    if (site < minSite) {
                                        minSite = site;
                                    }
                                    if (site > maxSite) {
                                        maxSite = site;
                                    }
                                    if (centerFreq < minCenterFreq) {
                                        minCenterFreq = centerFreq;
                                    }
                                    if (centerFreq > maxCenterFreq) {
                                        maxCenterFreq = centerFreq;
                                    }

                                    final LabeledPlotPoint point = new LabeledPlotPoint(key, new PlotPoint(centerFreq, site, pp.getStyle(), pp.getColor(), pp.getColor()));
                                    symbols.add(plotFactory.createSymbol(point.getStyle(), key, point.getX(), point.getY(), point.getColor(), Color.BLACK, point.getColor(), key, true));
                                }
                            }
                        }

                        maxSite = maxSite + .1;
                        if (minSite > maxSite) {
                            minSite = maxSite - .1;
                        } else {
                            minSite = minSite - .1;
                        }

                        maxCenterFreq = maxCenterFreq + .1;
                        if (minCenterFreq > maxCenterFreq) {
                            minCenterFreq = maxCenterFreq - .1;
                        } else {
                            minCenterFreq = minCenterFreq - 1.0;
                        }
                    }
                }

                final double minX = minCenterFreq;
                final double maxX = maxCenterFreq;
                final double minY = minSite;
                final double maxY = maxSite;

                runGuiUpdate(() -> {
                    relativeSiteTermsPlot.clear();

                    for (Symbol symbol : symbols) {
                        relativeSiteTermsPlot.addPlotObject(symbol);
                    }
                    relativeSiteTermsPlot.setAxisLimits(new AxisLimits(Axis.Type.X, minX, maxX), new AxisLimits(Axis.Type.Y, minY, maxY));
                    relativeSiteTermsPlot.replot();

                });
            } catch (InvocationTargetException | InterruptedException interrupted) {
                // NOP
            }
        }
    }

    @Override
    protected List<MeasuredMwDetails> getEvents() {
        return referenceEventClient.getMeasuredEventDetails()
                                   .filter(ev -> ev.getEventId() != null)
                                   .collect(Collectors.toList())
                                   .subscribeOn(Schedulers.boundedElastic())
                                   .block(Duration.ofSeconds(10l));
    }

    @Override
    public Consumer<File> getScreenshotFunction() {
        return folder -> {
            if (siteTermsTab.isSelected() && siteTermStationCombo.getValue() != null) {
                final String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
                SnapshotUtils.writePng(folder, new Pair<>(getDisplayName(), siteTermsTab.getContent()), timestamp);
                try {
                    Files.write(Paths.get(folder + File.separator + getDisplayName() + "_Site-Transfer_" + siteTermStationCombo.getValue() + timestamp + ".svg"), siteTermsPlot.getSVG().getBytes());

                    Files.write(
                            Paths.get(folder + File.separator + getDisplayName() + "_Relative-Site_" + siteTermStationCombo.getValue() + timestamp + ".svg"),
                                relativeSiteTermsPlot.getSVG().getBytes());
                } catch (final IOException e) {
                    log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
                }
            } else {
                super.getScreenshotFunction().accept(folder);
            }
        };
    }
}
