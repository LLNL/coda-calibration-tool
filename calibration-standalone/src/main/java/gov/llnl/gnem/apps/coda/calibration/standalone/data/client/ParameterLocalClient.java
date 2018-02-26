/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class ParameterLocalClient implements ParameterClient {

    private SharedFrequencyBandParametersService sharedParamsService;
    private SiteFrequencyBandParametersService siteParamsService;
    private MdacParametersFiService mdacFiService;
    private MdacParametersPsService mdacPsService;

    @Autowired
    public ParameterLocalClient(SharedFrequencyBandParametersService sharedParamsService, SiteFrequencyBandParametersService siteParamsService, MdacParametersFiService mdacFiService,
            MdacParametersPsService mdacPsService) {
        this.sharedParamsService = sharedParamsService;
        this.siteParamsService = siteParamsService;
        this.mdacFiService = mdacFiService;
        this.mdacPsService = mdacPsService;
    }

    @Override
    public Mono<String> postSharedFrequencyBandParameters(SharedFrequencyBandParameters parameters) throws JsonProcessingException {
        return Mono.just(Optional.ofNullable(sharedParamsService.update(parameters)).orElse(new SharedFrequencyBandParameters()).toString());
    }

    @Override
    public Flux<SharedFrequencyBandParameters> getSharedFrequencyBandParameters() {
        return Flux.fromIterable(sharedParamsService.findAll()).onErrorReturn(new SharedFrequencyBandParameters());
    }

    @Override
    public Mono<String> postSiteSpecificFrequencyBandParameters(SiteFrequencyBandParameters parameters) throws JsonProcessingException {
        return Mono.just(siteParamsService.save(parameters).toString());
    }

    @Override
    public Flux<SiteFrequencyBandParameters> getSiteSpecificFrequencyBandParameters() {
        return Flux.fromIterable(siteParamsService.findAll()).onErrorReturn(new SiteFrequencyBandParameters());
    }

    @Override
    public Mono<String> postPsParameters(MdacParametersPS parameters) throws JsonProcessingException {
        return Mono.just(mdacPsService.update(parameters).toString());
    }

    @Override
    public Flux<MdacParametersPS> getPsParameters() {
        return Flux.fromIterable(mdacPsService.findAll()).onErrorReturn(new MdacParametersPS());
    }

    @Override
    public Mono<String> postFiParameters(MdacParametersFI parameters) throws JsonProcessingException {
        return Mono.just(mdacFiService.update(parameters).toString());
    }

    @Override
    public Flux<MdacParametersFI> getFiParameters() {
        return Flux.fromIterable(mdacFiService.findAll()).onErrorReturn(new MdacParametersFI());
    }

    @Override
    public Mono<SharedFrequencyBandParameters> getSharedFrequencyBandParametersForFrequency(FrequencyBand frequencyBand) {
        return Mono.just(Optional.ofNullable(sharedParamsService.findByFrequencyBand(frequencyBand)).orElse(new SharedFrequencyBandParameters()));
    }

}
