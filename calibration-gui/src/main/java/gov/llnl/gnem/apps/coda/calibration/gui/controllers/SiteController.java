/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import javafx.embed.swing.SwingNode;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import reactor.core.scheduler.Schedulers;

@Component
public class SiteController extends AbstractMeasurementController {

    private static final Logger log = LoggerFactory.getLogger(SiteController.class);

    private static final String X_AXIS_LABEL = "center freq";

    private static final String displayName = "Site";
    
    @FXML
    private StackPane site;

    @FXML
    private SwingNode rawPlotSwingNode;

    @FXML
    private SwingNode pathPlotSwingNode;

    @FXML
    private SwingNode sitePlotSwingNode;

    @Autowired
    private SiteController(SpectraClient spectraClient, ParameterClient paramClient, EventClient referenceEventClient, WaveformClient waveformClient, SymbolStyleMapFactory styleFactory,
            GeoMap map, MapPlottingUtilities iconFactory, EventBus bus) {
        super(spectraClient, paramClient, referenceEventClient, waveformClient, styleFactory, map, iconFactory, bus);
    }

    @Override
    @FXML
    public void initialize() {
        spectraPlotPanel = site;
        super.initialize();

        SwingUtilities.invokeLater(() -> {
            final SpectraPlotController raw = new SpectraPlotController(SpectraMeasurement::getRawAtMeasurementTime);
            SpectralPlot plot = raw.getSpectralPlot();
            plot.addPlotObjectObserver(getPlotpointObserver(() -> raw.getSymbolMap()));
            plot.setLabels("Raw Plot", X_AXIS_LABEL, "log10(non-dim)");
            plot.setYaxisVisibility(true);
            plot.setAllXlimits(0.0, 0.0);
            plot.setDefaultYMin(-2.0);
            plot.setDefaultYMax(7.0);
            rawPlotSwingNode.setContent(plot);

            final SpectraPlotController path = new SpectraPlotController(SpectraMeasurement::getPathCorrected);
            plot = path.getSpectralPlot();
            plot.addPlotObjectObserver(getPlotpointObserver(() -> path.getSymbolMap()));
            plot.setLabels("Path Corrected", X_AXIS_LABEL, "log10(non-dim)");
            plot.setYaxisVisibility(true);
            plot.setAllXlimits(0.0, 0.0);
            plot.setDefaultYMin(-2.0);
            plot.setDefaultYMax(7.0);
            pathPlotSwingNode.setContent(plot);

            final SpectraPlotController site = new SpectraPlotController(SpectraMeasurement::getPathAndSiteCorrected);
            plot = site.getSpectralPlot();
            plot.addPlotObjectObserver(getPlotpointObserver(() -> site.getSymbolMap()));
            plot.setLabels("Moment Rate Spectra", X_AXIS_LABEL, "log10(dyne-cm)");
            plot.setYaxisVisibility(true);
            site.setYAxisResizable(true);
            sitePlotSwingNode.setContent(plot);

            rawPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
            pathPlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);
            sitePlotSwingNode.addEventHandler(javafx.scene.input.MouseEvent.MOUSE_CLICKED, menuHideHandler);

            spectraControllers.add(raw);
            spectraControllers.add(path);
            spectraControllers.add(site);
        });
    }

    @Override
    protected String getDisplayName() {
        return displayName;
    }

    @Override
    protected List<Spectra> getFitSpectra() {
        return new ArrayList<>(spectraClient.getFitSpectra(evidCombo.getSelectionModel().getSelectedItem()).block(Duration.ofSeconds(2)));
    }

    @Override
    protected void setActive(List<Waveform> waveforms, List<Symbol> plotObjects, boolean active, BiConsumer<List<Symbol>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().map(w -> w.getId()).collect(Collectors.toList()), active).subscribe(s -> activationFunc.accept(plotObjects, active));
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
    protected void runGuiUpdate(Runnable runnable) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(runnable);
    }

    @Override
    protected List<MeasuredMwDetails> getEvents() {
        return referenceEventClient.getMeasuredEventDetails()
        .filter(ev -> ev.getEventId() != null)
        .collect(Collectors.toList())
        .subscribeOn(Schedulers.elastic())
        .block(Duration.ofSeconds(10l));
    }
}
