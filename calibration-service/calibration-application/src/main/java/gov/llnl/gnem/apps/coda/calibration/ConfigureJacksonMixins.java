/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SharedFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SiteFrequencyBandParametersJsonMixin;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;

@Component
@Configuration
public class ConfigureJacksonMixins {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Bean
    public MappingJackson2HttpMessageConverter configureJackson() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        ObjectMapper mapper = new ObjectMapper();
        mapper.addMixIn(SharedFrequencyBandParameters.class, SharedFrequencyBandParametersFileMixin.class);
        mapper.addMixIn(SiteFrequencyBandParameters.class, SiteFrequencyBandParametersJsonMixin.class);
        converter.setObjectMapper(mapper);
        return converter;
    }
}
