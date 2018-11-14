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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client.api;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ParameterClient {

    public Mono<String> setSharedFrequencyBandParameter(SharedFrequencyBandParameters parameters) throws JsonProcessingException;

    public Flux<SharedFrequencyBandParameters> getSharedFrequencyBandParameters();

    public Mono<SharedFrequencyBandParameters> getSharedFrequencyBandParametersForFrequency(FrequencyBand frequencyBand);

    public Mono<String> removeSharedFrequencyBandParameter(SharedFrequencyBandParameters parameters);

    public Mono<String> setSiteSpecificFrequencyBandParameter(SiteFrequencyBandParameters parameters) throws JsonProcessingException;

    public Flux<SiteFrequencyBandParameters> getSiteSpecificFrequencyBandParameters();

    public Mono<String> setPsParameter(MdacParametersPS parameters) throws JsonProcessingException;

    public Flux<MdacParametersPS> getPsParameters();

    public Mono<String> removePsParameter(MdacParametersPS parameters);

    public Mono<String> setFiParameter(MdacParametersFI parameters) throws JsonProcessingException;

    public Flux<MdacParametersFI> getFiParameters();

    public Mono<String> removeFiParameter(MdacParametersFI parameters);

}
