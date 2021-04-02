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
package gov.llnl.gnem.apps.coda.common.gui.data.client;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class WaveformWebClient implements WaveformClient {

    private static final Logger log = LoggerFactory.getLogger(WaveformWebClient.class);

    private WebClient client;

    @Autowired
    public WaveformWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<Waveform> getWaveformFromId(Long id) {
        return client.get().uri("/single-waveform/{id}", id).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(Waveform.class);
    }

    @Override
    public Mono<SyntheticCoda> getSyntheticFromWaveformId(Long id) {
        return client.get().uri("/synthetics/single/{id}", id).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(SyntheticCoda.class);
    }

    @Override
    public Mono<Waveform> postWaveform(Waveform segment) throws JsonProcessingException {
        return client.post().uri("/single-waveform").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).bodyValue(segment).retrieve().bodyToMono(Waveform.class);
    }

    @Override
    public Flux<String> postWaveforms(Long sessionId, List<Waveform> segments) {
        return client.post().uri("/waveforms/batch/" + sessionId).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON).bodyValue(segments).retrieve().bodyToFlux(String.class);
    }

    @Override
    public Flux<Waveform> getAllStacks() {
        return client.get()
                     .uri("/waveforms/query/stacks")
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(Waveform.class)
                     .doOnError(e -> log.error(e.getMessage(), e))
                     .onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getAllActiveStacks() {
        return client.get()
                     .uri("/waveforms/query/active-stacks")
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(Waveform.class)
                     .doOnError(e -> log.error(e.getMessage(), e))
                     .onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getUniqueEventStationMetadataForStacks() {
        return client.get()
                     .uri("/waveforms/query/unique-by-event-station")
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(Waveform.class)
                     .filter(w -> w != null && w.getId() != null)
                     .onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getWaveformsFromIds(Collection<Long> ids) {
        return client.get()
                     .uri("/waveforms/batch/{ids}", ids.toString().replaceAll("\\[|\\]", ""))
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(Waveform.class)
                     .filter(Objects::nonNull)
                     .onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getWaveformMetadataFromIds(List<Long> ids) {
        return client.get()
                     .uri("/waveforms/metadata/batch/{ids}", ids.toString().replaceAll("\\[|\\]", ""))
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(Waveform.class)
                     .filter(Objects::nonNull)
                     .onErrorReturn(new Waveform());
    }

    @Override
    public Flux<SyntheticCoda> getSyntheticsFromWaveformIds(Collection<Long> ids) {
        return client.get()
                     .uri("/synthetics/batch/{ids}", ids.toString().replaceAll("\\[|\\]", ""))
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(SyntheticCoda.class)
                     .filter(Objects::nonNull)
                     .onErrorReturn(new SyntheticCoda());
    }

    @Override
    public Flux<String> setWaveformsActiveByIds(List<Long> selectedWaveforms, boolean active) {
        return client.post()
                     .uri("/waveforms/set-active/batch/" + active)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(selectedWaveforms)
                     .retrieve()
                     .bodyToFlux(String.class);
    }

    @Override
    public Flux<String> setWaveformsActiveByEventId(String id, boolean active) {
        return client.post()
                     .uri("/waveforms/set-active/by-event-id/" + active)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(id)
                     .retrieve()
                     .bodyToFlux(String.class);
    }

    @Override
    public Flux<String> setWaveformsActiveByStationName(String id, boolean active) {
        return client.post()
                     .uri("/waveforms/set-active/by-station-name/" + active)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(id)
                     .retrieve()
                     .bodyToFlux(String.class);
    }

    @Override
    public Flux<String> setWaveformsActiveOutsidePolygon(boolean active) {
        return client.post()
                     .uri("/geometry/set-active/waveforms-outside-polygon/" + active)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(String.class);
    }

    @Override
    public Flux<String> setWaveformsActiveInsidePolygon(boolean active) {
        return client.post()
                     .uri("/geometry/set-active/waveforms-inside-polygon/" + active)
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .retrieve()
                     .bodyToFlux(String.class);
    }
}
