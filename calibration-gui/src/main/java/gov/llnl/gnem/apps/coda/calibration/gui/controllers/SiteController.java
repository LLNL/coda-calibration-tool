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

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.TickFormat;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import reactor.core.scheduler.Schedulers;

@Component
public class SiteController extends AbstractMeasurementController {

    private static final String X_AXIS_LABEL = "center freq (Hz)";

    private static final String DISPLAY_NAME = "Site";

    @FXML
    private StackPane sitePane;

    @FXML
    private StackPane rawPlotNode;

    @FXML
    private StackPane pathPlotNode;

    @FXML
    private StackPane sitePlotNode;

    @Autowired
    private SiteController(final SpectraClient spectraClient, final ParameterClient paramClient, final EventClient referenceEventClient, final WaveformClient waveformClient,
            final SymbolStyleMapFactory styleFactory, final GeoMap map, final MapPlottingUtilities iconFactory, final PlotFactory plotFactory, final EventBus bus) {
        super(spectraClient, paramClient, referenceEventClient, waveformClient, styleFactory, map, iconFactory, plotFactory, bus);
    }

    @Override
    @FXML
    public void initialize() {
        spectraPlotPanel = sitePane;
        super.initialize();

        final SpectraPlotController raw = new SpectraPlotController(SpectraMeasurement::getRawAtMeasurementTime);
        SpectralPlot plot = raw.getSpectralPlot();
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(raw::getSpectraMeasurementMap));
        plot.setLabels("Raw Plot", X_AXIS_LABEL, "log10(non-dim)");
        plot.setAutoCalculateYaxisRange(true);
        rawPlotNode.getChildren().add(plot);

        final SpectraPlotController path = new SpectraPlotController(SpectraMeasurement::getPathCorrected);
        plot = path.getSpectralPlot();
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(path::getSpectraMeasurementMap));
        plot.setLabels("Path Corrected", X_AXIS_LABEL, "log10(non-dim)");
        plot.setAutoCalculateYaxisRange(true);
        pathPlotNode.getChildren().add(plot);

        final SpectraPlotController site = new SpectraPlotController(SpectraMeasurement::getPathAndSiteCorrected);
        plot = site.getSpectralPlot();
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(site::getSpectraMeasurementMap));
        plot.setLabels("Moment Rate Spectra", X_AXIS_LABEL, "log10(dyne-cm)");
        final Axis rightAxis = new BasicAxis(Axis.Type.Y_RIGHT, "Mw");
        rightAxis.setTickFormat(TickFormat.LOG10_DYNE_CM_TO_MW);
        plot.getSubplot().addAxes(rightAxis);
        site.setShowCornerFrequencies(true);
        site.setYAxisResizable(true);
        site.setShouldShowFits(true);
        sitePlotNode.getChildren().add(plot);

        spectraControllers.add(raw);
        spectraControllers.add(path);
        spectraControllers.add(site);
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
    protected List<MeasuredMwDetails> getEvents() {
        return referenceEventClient.getMeasuredEventDetails()
                                   .filter(ev -> ev.getEventId() != null)
                                   .collect(Collectors.toList())
                                   .subscribeOn(Schedulers.boundedElastic())
                                   .block(Duration.ofSeconds(10l));
    }
}
