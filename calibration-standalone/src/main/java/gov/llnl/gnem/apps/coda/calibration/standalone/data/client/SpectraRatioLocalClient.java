/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.standalone.data.client;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraRatioClient;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraRatioPairDetailsService;
import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatiosReport;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;
import reactor.core.publisher.Mono;

@Component
@Primary
public class SpectraRatioLocalClient implements SpectraRatioClient {

    private static final Logger log = LoggerFactory.getLogger(SpectraRatioLocalClient.class);
    private SpectraRatioPairDetailsService ratioDetailsService;

    @Autowired
    public SpectraRatioLocalClient(SpectraRatioPairDetailsService ratioDetailsService) {
        this.ratioDetailsService = ratioDetailsService;
    }

    @Override
    public Mono<SpectraRatiosReportByEventPair> makeSpectraRatioMeasurementsFromWaveforms(Boolean autoPickingEnabled, Boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds) {
        log.trace("Making local call to measure spectra ratio from waveforms");
        try {
            return Mono.just(
                    new SpectraRatiosReportByEventPair(ratioDetailsService.makeSpectraRatioMeasurementsFromWaveforms(autoPickingEnabled, persistResults, smallEventIds, largeEventIds)
                                                                          .get(1l, TimeUnit.DAYS)
                                                                          .getResultPayload()
                                                                          .orElseGet(SpectraRatiosReport::new)));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<SpectraRatiosReportByEventPair> makeSpectraRatioMeasurementsFromRatioData(Set<String> smallEventIds, Set<String> largeEventIds, List<RatioEventData> ratioEventData) {
        log.trace("Making local call to measure spectra ratio from from ratio data");
        try {
            return Mono.just(
                    new SpectraRatiosReportByEventPair(ratioDetailsService.makeSpectraRatioMeasurementsFromRatioData(smallEventIds, largeEventIds, ratioEventData)
                                                                          .get(1l, TimeUnit.DAYS)
                                                                          .getResultPayload()
                                                                          .orElseGet(SpectraRatiosReport::new)));
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
            return Mono.empty();
        }
    }
}
