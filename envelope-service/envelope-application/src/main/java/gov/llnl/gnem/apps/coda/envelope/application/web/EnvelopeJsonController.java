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

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJob;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeCreationService;

@RestController
@RequestMapping(value = "/api/v1/envelopes/create", name = "EnvelopeJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class EnvelopeJsonController {

    private EnvelopeCreationService service;

    @Autowired
    public EnvelopeJsonController(EnvelopeCreationService service) {
        this.service = service;
    }

    @PostMapping(value = "/batch/{sessionId}", name = "createBatch")
    public ResponseEntity<?> createBatch(@PathVariable Long sessionId, @Valid @RequestBody EnvelopeJob job, BindingResult bindResult) {

        if (bindResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindResult);
        }

        Result<List<Waveform>> result = service.createEnvelopes(sessionId, job.getData(), job.getJobConfig());

        BodyBuilder response;
        if (result.isSuccess()) {
            response = ResponseEntity.ok();
        } else {
            response = ResponseEntity.badRequest();
        }
        return response.body(result);
    }
}
