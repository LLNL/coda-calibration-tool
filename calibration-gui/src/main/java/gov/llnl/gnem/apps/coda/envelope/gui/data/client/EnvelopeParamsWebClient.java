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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeParamsClient;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import reactor.core.publisher.Mono;

@Component
public class EnvelopeParamsWebClient implements EnvelopeParamsClient {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeParamsWebClient.class);

    private WebClient client;

    @Autowired
    public EnvelopeParamsWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<String> postEnvelopeJobConfiguration(EnvelopeJobConfiguration config) {
        return client.post()
                     .uri("/envelopes/job-configuration/update")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(config)
                     .retrieve()
                     .onStatus(code -> !HttpStatus.OK.equals(code), resp -> {
                         return Mono.just(new LightweightIllegalStateException(resp.toString()));
                     })
                     .toBodilessEntity()
                     .map(resp -> resp.toString());
    }

    @Override
    public Mono<EnvelopeJobConfiguration> getEnvelopeJobConfiguration() {
        return client.get().uri("/envelopes/job-configuration").accept(MediaType.APPLICATION_JSON).retrieve().onStatus(code -> !HttpStatus.OK.equals(code), resp -> {
            return Mono.just(new LightweightIllegalStateException(resp.toString()));
        }).bodyToMono(EnvelopeJobConfiguration.class);
    }
}
