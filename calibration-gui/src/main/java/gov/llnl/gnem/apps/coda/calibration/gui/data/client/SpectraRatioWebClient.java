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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraRatioClient;
import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioMeasurementJob;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.SpectraRatiosReportDTO;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SpectraRatioWebClient implements SpectraRatioClient {

    private static final Logger log = LoggerFactory.getLogger(SpectraRatioWebClient.class);

    private WebClient client;

    @Autowired
    public SpectraRatioWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<SpectraRatiosReportByEventPair> makeSpectraRatioMeasurementsFromWaveforms(Boolean autoPickingEnabled, Boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds) {
        return client.post()
                     .uri("/spectra-ratios/measure-spectra-ratio-from-waveforms")
                     .bodyValue(
                             new SpectraRatioMeasurementJob().setAutoPickingEnabled(autoPickingEnabled)
                                                             .setPersistResults(persistResults)
                                                             .setSmallEventIds(smallEventIds)
                                                             .setLargeEventIds(largeEventIds))
                     .retrieve()
                     .bodyToMono(SpectraRatiosReportDTO.class)
                     .map(SpectraRatiosReportDTO::getReport)
                     .map(SpectraRatiosReportByEventPair::new);
    }

    @Override
    public Mono<SpectraRatiosReportByEventPair> makeSpectraRatioMeasurementsFromRatioData(Set<String> smallEventIds, Set<String> largeEventIds, List<RatioEventData> ratioEventData) {
        return client.post()
                     .uri("/spectra-ratios/measure-spectra-ratio-from-ratios-data")
                     .bodyValue(new SpectraRatioMeasurementJob().setSmallEventIds(smallEventIds).setLargeEventIds(largeEventIds).setRatioEventData(ratioEventData))
                     .retrieve()
                     .bodyToMono(SpectraRatiosReportDTO.class)
                     .map(SpectraRatiosReportDTO::getReport)
                     .map(SpectraRatiosReportByEventPair::new);
    }

    @Override
    public Mono<SpectraRatioPairDetails> updateRatio(SpectraRatioPairDetails ratio) {
        return client.post().uri("/spectra-ratios/update-ratio").bodyValue(ratio).retrieve().bodyToMono(SpectraRatioPairDetails.class);
    }

    @Override
    public Flux<SpectraRatioPairDetails> getRatios() {
        return client.get().uri("/spectra-ratios/all").retrieve().bodyToFlux(SpectraRatioPairDetails.class);
    }

    @Override
    public Flux<SpectraRatioPairDetailsMetadata> getRatiosMetadata() {
        return client.get().uri("/spectra-ratios/all-metadata-only").retrieve().bodyToFlux(SpectraRatioPairDetailsMetadata.class);
    }

    @Override
    public Flux<String> loadRatioMetadata(long id, List<SpectraRatioPairDetailsMetadata> ratios) {
        return client.post().uri("/spectra-ratios/load-ratios-metadata").bodyValue(ratios).retrieve().bodyToFlux(String.class);
    }
}
