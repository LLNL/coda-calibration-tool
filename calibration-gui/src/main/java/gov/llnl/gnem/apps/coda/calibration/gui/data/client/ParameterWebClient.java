/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ParameterWebClient implements ParameterClient {

    private WebClient client;

    @Autowired
    public ParameterWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Mono<String> setSharedFrequencyBandParameter(SharedFrequencyBandParameters parameters) throws JsonProcessingException {
        return client.post()
                     .uri("/params/shared-fb-parameters/update")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Mono<String> removeSharedFrequencyBandParameter(SharedFrequencyBandParameters parameters) {
        return client.post()
                     .uri("/params/shared-fb-parameters/delete")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Flux<SharedFrequencyBandParameters> getSharedFrequencyBandParameters() {
        return client.get()
                     .uri("/params/shared-fb-parameters/")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(SharedFrequencyBandParameters.class))
                     .onErrorReturn(new SharedFrequencyBandParameters());

    }

    @Override
    public Mono<String> setSiteSpecificFrequencyBandParameter(List<SiteFrequencyBandParameters> parameters) throws JsonProcessingException {
        return client.post()
                     .uri("/params/site-fb-parameters/batch")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Flux<SiteFrequencyBandParameters> getSiteSpecificFrequencyBandParameters() {
        return client.get()
                     .uri("/params/site-fb-parameters/")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(SiteFrequencyBandParameters.class))
                     .onErrorReturn(new SiteFrequencyBandParameters());
    }

    @Override
    public Mono<String> setPsParameter(MdacParametersPS parameters) throws JsonProcessingException {
        return client.post()
                     .uri("/params/ps/update")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Mono<String> removePsParameter(MdacParametersPS parameters) {
        return client.post()
                     .uri("/params/ps/delete")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Flux<MdacParametersPS> getPsParameters() {
        return client.get().uri("/params/ps/").accept(MediaType.APPLICATION_JSON).exchange().flatMapMany(response -> response.bodyToFlux(MdacParametersPS.class)).onErrorReturn(new MdacParametersPS());
    }

    @Override
    public Mono<String> setFiParameter(MdacParametersFI parameters) throws JsonProcessingException {
        return client.post()
                     .uri("/params/fi/update")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Mono<String> removeFiParameter(MdacParametersFI parameters) {
        return client.post()
                     .uri("/params/fi/delete")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(parameters)
                     .exchange()
                     .flatMap(resp -> resp.bodyToMono(String.class));
    }

    @Override
    public Flux<MdacParametersFI> getFiParameters() {
        return client.get().uri("/params/fi/").accept(MediaType.APPLICATION_JSON).exchange().flatMapMany(response -> response.bodyToFlux(MdacParametersFI.class)).onErrorReturn(new MdacParametersFI());
    }

    @Override
    public Mono<SharedFrequencyBandParameters> getSharedFrequencyBandParametersForFrequency(FrequencyBand frequencyBand) {
        return client.post()
                     .uri("/params/shared-fb-parameters/find-by-band")
                     .contentType(MediaType.APPLICATION_JSON)
                     .accept(MediaType.APPLICATION_JSON)
                     .bodyValue(frequencyBand)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(SharedFrequencyBandParameters.class));
    }

    @Override
    public Mono<VelocityConfiguration> getVelocityConfiguration() {
        return client.get()
                     .uri("/config/velocity/")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(VelocityConfiguration.class))
                     .onErrorReturn(new VelocityConfiguration());
    }

    @Override
    public Mono<String> updateVelocityConfiguration(VelocityConfiguration velConf) {
        return client.post().uri("/config/velocity/update").accept(MediaType.APPLICATION_JSON).bodyValue(velConf).exchange().flatMap(response -> response.bodyToMono(String.class)).onErrorReturn("");
    }

    @Override
    public Mono<ShapeFitterConstraints> getShapeFitterConstraints() {
        return client.get()
                     .uri("/config/shape/")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(ShapeFitterConstraints.class))
                     .onErrorReturn(new ShapeFitterConstraints());
    }

    @Override
    public Mono<String> updateShapeFitterConstraints(ShapeFitterConstraints conf) {
        return client.post().uri("/config/shape/update").accept(MediaType.APPLICATION_JSON).bodyValue(conf).exchange().flatMap(response -> response.bodyToMono(String.class)).onErrorReturn("");
    }

    @Override
    public Mono<String> updateMapPolygon(String rawGeoJSON) {
        return client.post().uri("/config/polygon/update").accept(MediaType.APPLICATION_JSON).bodyValue(rawGeoJSON).exchange().flatMap(response -> response.bodyToMono(String.class)).onErrorReturn("");
    }

    @Override
    public Mono<String> getMapPolygon() {
        return client.get()
                .uri("/config/polygon/")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .flatMap(response -> response.bodyToMono(String.class))
                .onErrorReturn("");
    }    
}
