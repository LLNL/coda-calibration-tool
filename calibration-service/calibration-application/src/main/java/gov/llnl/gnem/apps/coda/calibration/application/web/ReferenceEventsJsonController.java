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
package gov.llnl.gnem.apps.coda.calibration.application.web;

import java.util.Collection;
import java.util.List;

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

import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/reference-events", "/api/v1/reference-events/" }, name = "ReferenceEventsJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class ReferenceEventsJsonController {

    private ReferenceMwParametersService service;

    @Autowired
    public ReferenceEventsJsonController(ReferenceMwParametersService service) {
        this.service = service;
    }

    @GetMapping(name = "getReferenceEvents")
    public List<ReferenceMwParameters> getReferenceEvents() {
        return service.findAll();
    }

    @PostMapping(value = { "/batch", "/batch/" }, name = "createBatch")
    public ResponseEntity<?> createBatch(@Valid @RequestBody Collection<ReferenceMwParameters> referenceEvents, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        service.save(referenceEvents);
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = { "/delete/batch-by-evids", "/delete/batch-by-evids/" }, name = "deleteBatchByEvids")
    public ResponseEntity<?> deleteBatchByEvids(@Valid @RequestBody Collection<String> eventIds, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        service.deleteAllByEventIds(eventIds);
        return ResponseEntity.ok().build();
    }

    public ReferenceMwParametersService getService() {
        return service;
    }

    public void setService(ReferenceMwParametersService service) {
        this.service = service;
    }

}
