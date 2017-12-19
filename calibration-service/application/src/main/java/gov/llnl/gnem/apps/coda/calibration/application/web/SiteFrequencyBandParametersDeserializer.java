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
package gov.llnl.gnem.apps.coda.calibration.application.web;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.boot.jackson.JsonObjectDeserializer;
import org.springframework.context.annotation.Lazy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;

@JsonComponent
public class SiteFrequencyBandParametersDeserializer extends JsonObjectDeserializer<SiteFrequencyBandParameters> {

    private SiteFrequencyBandParametersService siteFrequencyBandParametersService;

    final ObjectReader mapper = new ObjectMapper().readerFor(SiteFrequencyBandParameters.class);

    @Autowired
    public SiteFrequencyBandParametersDeserializer(@Lazy SiteFrequencyBandParametersService siteFrequencyBandParametersService) {
        this.siteFrequencyBandParametersService = siteFrequencyBandParametersService;
    }

    public SiteFrequencyBandParametersService getSiteFrequencyBandParametersService() {
        return siteFrequencyBandParametersService;
    }

    public void setSiteFrequencyBandParametersService(SiteFrequencyBandParametersService siteFrequencyBandParametersService) {
        this.siteFrequencyBandParametersService = siteFrequencyBandParametersService;
    }

    public SiteFrequencyBandParameters deserializeObject(JsonParser jsonParser, DeserializationContext context, ObjectCodec codec, JsonNode tree) throws IOException {
        return mapper.readValue(tree);
    }
}
