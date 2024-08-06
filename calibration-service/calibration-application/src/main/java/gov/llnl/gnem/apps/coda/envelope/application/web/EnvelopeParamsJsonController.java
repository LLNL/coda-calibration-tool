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
package gov.llnl.gnem.apps.coda.envelope.application.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeParamsService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/envelopes/job-configuration", "/api/v1/envelopes/job-configuration/" }, name = "EnvelopeJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeParamsJsonController {

    private EnvelopeParamsService service;

    @Autowired
    public EnvelopeParamsJsonController(EnvelopeParamsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<EnvelopeJobConfiguration> getCurrentConfiguration() {
        EnvelopeJobConfiguration currentConfiguration = service.getConfiguration();
        return ResponseEntity.ok(currentConfiguration);
    }

    @PostMapping(value = { "/update", "/update/" }, name = "update")
    public ResponseEntity<?> update(@Valid @RequestBody EnvelopeJobConfiguration jobConf, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        service.setConfiguration(jobConf);
        return ResponseEntity.ok().build();
    }
}
