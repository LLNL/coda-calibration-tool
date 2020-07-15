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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class SpectraWebClient implements SpectraClient {

    private WebClient client;

    @Autowired
    public SpectraWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Flux<SpectraMeasurement> getMeasuredSpectra() {
        return client.get()
                     .uri("/spectra-measurements/all")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(SpectraMeasurement.class))
                     .onErrorReturn(new SpectraMeasurement());
    }

    @Override
    public Flux<SpectraMeasurement> getMeasuredSpectraMetadata() {
        return client.get()
                     .uri("/spectra-measurements/metadata/all")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(SpectraMeasurement.class))
                     .onErrorReturn(new SpectraMeasurement());
    }

    @Override
    public Mono<Spectra> getReferenceSpectra(String eventId) {
        return client.post()
                     .uri("/spectra-measurements/reference-spectra")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(eventId)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(Spectra.class))
                     .onErrorReturn(new Spectra());
    }
    
    @Override
    public Mono<Spectra> getValidationSpectra(String eventId) {
        return client.post()
                     .uri("/spectra-measurements/validation-spectra")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(eventId)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(Spectra.class))
                     .onErrorReturn(new Spectra());
    }    

    @Override
    public Mono<List<Spectra>> getFitSpectra(String eventId) {
        return client.post()
                     .uri("/spectra-measurements/fit-spectra")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(eventId)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(new ParameterizedTypeReference<List<Spectra>>() {
                     }))
                     .onErrorReturn(new ArrayList<Spectra>());
    }
}
