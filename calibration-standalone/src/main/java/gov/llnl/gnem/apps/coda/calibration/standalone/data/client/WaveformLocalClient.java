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

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class WaveformLocalClient implements WaveformClient {

    private WaveformService service;
    private SyntheticService synthService;

    @Autowired
    public WaveformLocalClient(WaveformService service, SyntheticService synthService) {
        this.service = service;
        this.synthService = synthService;
    }

    @Override
    public Mono<Waveform> getWaveformFromId(Long id) {
        return Mono.just(Optional.ofNullable(service.findOne(id)).orElse(new Waveform()));
    }

    @Override
    public Mono<SyntheticCoda> getSyntheticFromWaveformId(Long id) {
        return Mono.just(Optional.ofNullable(synthService.findOneByWaveformId(id)).orElse(new SyntheticCoda())).onErrorReturn(new SyntheticCoda());
    }

    @Override
    public Mono<Waveform> postWaveform(Waveform segment) throws JsonProcessingException {
        return Mono.just(Optional.ofNullable(service.update(segment)).orElse(new Waveform()));
    }

    @Override
    public Flux<String> postWaveforms(Long sessionId, List<Waveform> segments) {
        return Flux.just(service.update(sessionId, segments).toString());
    }

    @Override
    public Flux<Waveform> getAllStacks() {
        return Flux.fromIterable(service.findAll()).onErrorReturn(new Waveform());
    }

    @Override
    public Flux<Waveform> getUniqueEventStationMetadataForStacks() {
        return Flux.fromIterable(service.getUniqueEventStationStacks());
    }

    @Override
    public Flux<Waveform> getWaveformsFromIds(Collection<Long> ids) {
        return Flux.fromIterable(service.findAll(ids)).onErrorReturn(new Waveform());
    }

    @Override
    public Flux<SyntheticCoda> getSyntheticsFromWaveformIds(Collection<Long> ids) {
        return Flux.fromIterable(synthService.findAllByWaveformId(ids)).onErrorReturn(new SyntheticCoda());
    }

}
