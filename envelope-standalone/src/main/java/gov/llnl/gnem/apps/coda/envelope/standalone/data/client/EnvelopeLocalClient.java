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
package gov.llnl.gnem.apps.coda.envelope.standalone.data.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeClient;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeCreationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class EnvelopeLocalClient implements EnvelopeClient {

    private EnvelopeCreationService service;
    private WaveformService waveformService;

    private static final Logger log = LoggerFactory.getLogger(EnvelopeLocalClient.class);

    @Autowired
    public EnvelopeLocalClient(EnvelopeCreationService service, WaveformService waveformService) {
        this.service = service;
        this.waveformService = waveformService;
    }

    @Override
    public Mono<Waveform> getEnvelopeFromId(Long id) {
        return Mono.just(Optional.ofNullable(waveformService.findOne(id)).orElseGet(() -> new Waveform()));
    }

    @Override
    public Flux<Waveform> getEnvelopesMatchingAllFields(Waveform segment) {
        return Flux.fromIterable(waveformService.getByExampleAllDistinctMatching(segment)).onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getAllEnvelopes() {
        return Flux.fromIterable(waveformService.findAll()).onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> postEnvelopes(Long sessionId, List<Waveform> segments) {
        return postEnvelopes(sessionId, segments, null);
    }

    @Override
    public Flux<Waveform> postEnvelopes(Long sessionId, List<Waveform> segments, EnvelopeJobConfiguration conf) {
        return Flux.fromIterable(service.createEnvelopes(sessionId, segments, conf, false).getResultPayload().orElseGet(() -> new ArrayList<>()));
    }

}
