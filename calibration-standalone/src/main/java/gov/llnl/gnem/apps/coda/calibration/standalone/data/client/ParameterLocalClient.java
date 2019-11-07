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
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class ParameterLocalClient implements ParameterClient {

    private static final Logger log = LoggerFactory.getLogger(ParameterLocalClient.class);

    private SharedFrequencyBandParametersService sharedParamsService;
    private SiteFrequencyBandParametersService siteParamsService;
    private MdacParametersFiService mdacFiService;
    private MdacParametersPsService mdacPsService;
    private ConfigurationService configService;

    @Autowired
    public ParameterLocalClient(SharedFrequencyBandParametersService sharedParamsService, SiteFrequencyBandParametersService siteParamsService, MdacParametersFiService mdacFiService,
            MdacParametersPsService mdacPsService, ConfigurationService configService) {
        this.sharedParamsService = sharedParamsService;
        this.siteParamsService = siteParamsService;
        this.mdacFiService = mdacFiService;
        this.mdacPsService = mdacPsService;
        this.configService = configService;
    }

    @Override
    public Mono<String> setSharedFrequencyBandParameter(SharedFrequencyBandParameters parameters) throws JsonProcessingException {
        return Mono.just(Optional.ofNullable(sharedParamsService.update(parameters)).orElseGet(() -> new SharedFrequencyBandParameters()).toString());
    }

    @Override
    public Mono<String> removeSharedFrequencyBandParameter(SharedFrequencyBandParameters parameters) {
        sharedParamsService.delete(parameters);
        return Mono.empty();
    }

    @Override
    public Flux<SharedFrequencyBandParameters> getSharedFrequencyBandParameters() {
        return Flux.fromIterable(sharedParamsService.findAll()).onErrorReturn(new SharedFrequencyBandParameters());
    }

    @Override
    public Mono<String> setSiteSpecificFrequencyBandParameter(List<SiteFrequencyBandParameters> parameters) throws JsonProcessingException {
        return Mono.just(siteParamsService.save(parameters).toString());
    }

    @Override
    public Flux<SiteFrequencyBandParameters> getSiteSpecificFrequencyBandParameters() {
        return Flux.fromIterable(siteParamsService.findAll()).onErrorReturn(new SiteFrequencyBandParameters());
    }

    @Override
    public Mono<String> setPsParameter(MdacParametersPS parameters) throws JsonProcessingException {
        return Mono.just(mdacPsService.update(parameters).toString());
    }

    @Override
    public Mono<String> removePsParameter(MdacParametersPS parameters) {
        mdacPsService.delete(parameters);
        return Mono.empty();
    }

    @Override
    public Flux<MdacParametersPS> getPsParameters() {
        return Flux.fromIterable(mdacPsService.findAll()).onErrorReturn(new MdacParametersPS());
    }

    @Override
    public Mono<String> setFiParameter(MdacParametersFI parameters) throws JsonProcessingException {
        return Mono.just(mdacFiService.update(parameters).toString());
    }

    @Override
    public Mono<String> removeFiParameter(MdacParametersFI parameters) {
        mdacFiService.delete(parameters);
        return Mono.empty();
    }

    @Override
    public Flux<MdacParametersFI> getFiParameters() {
        return Flux.fromIterable(mdacFiService.findAll()).onErrorReturn(new MdacParametersFI());
    }

    @Override
    public Mono<SharedFrequencyBandParameters> getSharedFrequencyBandParametersForFrequency(FrequencyBand frequencyBand) {
        return Mono.just(Optional.ofNullable(sharedParamsService.findByFrequencyBand(frequencyBand)).orElseGet(() -> new SharedFrequencyBandParameters()));
    }

    @Override
    public Mono<VelocityConfiguration> getVelocityConfiguration() {
        return Mono.just(Optional.ofNullable(configService.getVelocityConfiguration()).orElseGet(() -> new VelocityConfiguration()));
    }

    @Override
    public Mono<String> updateVelocityConfiguration(VelocityConfiguration velConf) {
        return Mono.just(Optional.ofNullable(configService.update(velConf)).map(v -> v.toString()).orElseGet(() -> ""));
    }

    @Override
    public Mono<ShapeFitterConstraints> getShapeFitterConstraints() {
        return Mono.just(Optional.ofNullable(configService.getCalibrationShapeFitterConstraints()).orElseGet(() -> new ShapeFitterConstraints()));
    }

    @Override
    public Mono<String> updateShapeFitterConstraints(ShapeFitterConstraints conf) {
        return Mono.just(Optional.ofNullable(configService.update(conf)).map(v -> v.toString()).orElseGet(() -> ""));
    }

}
