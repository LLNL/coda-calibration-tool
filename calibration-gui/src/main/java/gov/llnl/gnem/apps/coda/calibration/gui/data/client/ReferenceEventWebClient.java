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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ReferenceEventClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ReferenceEventWebClient implements ReferenceEventClient {

    private WebClient client;

    @Autowired
    public ReferenceEventWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Flux<ReferenceMwParameters> getReferenceEvents() {
        return client.get()
                     .uri("/reference-events")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(ReferenceMwParameters.class))
                     .onErrorReturn(new ReferenceMwParameters());
    }

    @Override
    public Mono<String> postReferenceEvents(List<ReferenceMwParameters> refEvents) throws JsonProcessingException {
        return client.post()
                     .uri("/reference-events/batch")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .syncBody(refEvents)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Flux<MeasuredMwParameters> getMeasuredEvents() {
        return client.get()
                     .uri("/measured-mws")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(MeasuredMwParameters.class))
                     .onErrorReturn(new MeasuredMwParameters());
    }

    @Override
    public Mono<Event> getEvent(String eventId) {
        return client.get().uri("/events/" + eventId).accept(MediaType.APPLICATION_JSON).exchange().flatMap(response -> response.bodyToMono(Event.class)).onErrorReturn(new Event());
    }

}
