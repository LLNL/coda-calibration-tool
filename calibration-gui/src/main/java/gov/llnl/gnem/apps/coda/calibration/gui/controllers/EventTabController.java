/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Symbol;
import reactor.core.scheduler.Schedulers;

@Component
public class EventTabController extends EventTableController {

    private static final String AVERAGE_LABEL = "Average";
    private static final String DISPLAY_NAME = "Event_Table";

    /* @FXML
    private StackPane sitePane;*/

    @Autowired
    private EventTabController(final SpectraClient spectraClient, final ParameterClient paramClient, final EventClient referenceEventClient, final WaveformClient waveformClient,
            final SymbolStyleMapFactory styleFactory, final GeoMap map, final MapPlottingUtilities iconFactory, final ParamExporter paramExporter, final PlotFactory plotFactory, final EventBus bus) {
        super(spectraClient, paramClient, referenceEventClient, waveformClient, styleFactory, plotFactory, bus);
    }

    @Override
    @FXML
    public void initialize() {
        super.initialize();
    }

    @Override
    protected void runGuiUpdate(final Runnable runnable) throws InvocationTargetException, InterruptedException {
        Platform.runLater(runnable);
    }

    @Override
    protected void reloadData() {
        super.reloadData();
        List<SiteFrequencyBandParameters> siteTerms = paramClient.getSiteSpecificFrequencyBandParameters().filter(Objects::nonNull).collectList().block(Duration.of(10l, ChronoUnit.SECONDS));

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

        symbols.addAll(averageSymbols.values());
        siteTerms.addAll(averageSiteTerms.values());
    }

    @Override
    protected List<MeasuredMwDetails> getEvents() {
        return referenceEventClient.getMeasuredEventDetails()
                                   .filter(ev -> ev.getEventId() != null)
                                   .collect(Collectors.toList())
                                   .subscribeOn(Schedulers.boundedElastic())
                                   .block(Duration.ofSeconds(10l));
    }

    public Consumer<File> getScreenshotFunction(Tab tab) {
        return folder -> {
            final String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
            SnapshotUtils.writePng(folder, new Pair<>(getDisplayName(), tab.getContent()), timestamp);
        };
    }

    protected String getDisplayName() {
        return DISPLAY_NAME;
    }
}
