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

package gov.llnl.gnem.apps.coda.envelope.gui.data.client;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeClient;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJob;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class EnvelopeWebClient implements EnvelopeClient {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeWebClient.class);

    private static final ParameterizedTypeReference<Result<List<Waveform>>> POST_ENV_RETURN_TYPE = new ParameterizedTypeReference<Result<List<Waveform>>>() {
    };

    private WebClient client;

    @Autowired
    public EnvelopeWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<Waveform> getEnvelopeFromId(Long id) {
        return client.get().uri("/single-waveform/{id}", id).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Waveform.class);
    }

    @Override
    public Flux<Waveform> postEnvelopes(Long sessionId, List<Waveform> segments) {
        return postEnvelopes(sessionId, segments, null);
    }

    @Override
    public Flux<Waveform> postEnvelopes(Long sessionId, List<Waveform> segments, EnvelopeJobConfiguration job) {
        return client.post()
                     .uri("/envelopes/create/batch/" + sessionId)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(new EnvelopeJob().setData(segments).setJobConfig(job))
                     .retrieve()
                     .bodyToMono(POST_ENV_RETURN_TYPE)
                     .flatMapMany(results -> {
                         if (results.isSuccess()) {
                             return Flux.fromStream(results.getResultPayload().orElseGet(() -> new ArrayList<Waveform>()).stream());
                         } else {
                             return Flux.empty();
                         }
                     });
    }

    @Override
    public Flux<Waveform> getEnvelopesMatchingAllFields(Waveform segment) {
        return client.post()
                     .uri("/waveforms/query/all")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(segment)
                     .retrieve()
                     .bodyToFlux(Waveform.class)
                     .onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getAllEnvelopes() {
        return client.get().uri("/waveforms/query/all").accept(MediaType.APPLICATION_JSON).retrieve().bodyToFlux(Waveform.class).onErrorReturn(new Waveform());
    }
}
