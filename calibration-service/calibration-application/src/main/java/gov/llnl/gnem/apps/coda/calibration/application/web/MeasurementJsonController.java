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
package gov.llnl.gnem.apps.coda.calibration.application.web;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

@RestController
@RequestMapping(value = "/api/v1/measurement", name = "MeasurementJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeasurementJsonController {

    private static final Logger log = LoggerFactory.getLogger(MeasurementJsonController.class);

    private CalibrationService service;

    @Autowired
    public MeasurementJsonController(CalibrationService service) {
        this.service = service;
    }

    @GetMapping(value = "/measure-mws/{autoPickingEnabled}", name = "measureMws")
    public ResponseEntity<?> measureMws(@PathVariable(name = "autoPickingEnabled", required = false) Boolean autoPickingEnabled) {
        return measureMw(autoPickingEnabled, null, null);
    }

    @PostMapping(value = "/measure-mws/{autoPickingEnabled}", name = "measureMws")
    public ResponseEntity<?> measureMws(@PathVariable(name = "autoPickingEnabled", required = false) Boolean autoPickingEnabled, @RequestBody List<String> eventIds) {
        return measureMw(autoPickingEnabled, eventIds, null);
    }

    @PostMapping(value = "/measure-mws-from-stacks/{autoPickingEnabled}", name = "measureMwsFromStacks")
    public ResponseEntity<?> measureMwsFromStacks(@PathVariable(name = "autoPickingEnabled", required = false) Boolean autoPickingEnabled, @RequestBody List<Waveform> stacks) {
        return measureMw(autoPickingEnabled, null, stacks);
    }

    private ResponseEntity<?> measureMw(Boolean autoPickingEnabled, List<String> evids, List<Waveform> stacks) {
        if (autoPickingEnabled == null) {
            autoPickingEnabled = Boolean.FALSE;
        }

        ResponseEntity<?> resp = null;
        try {
            Result<List<MeasuredMwDetails>> measuredMws;
            if (evids != null && !evids.isEmpty()) {
                measuredMws = service.makeMwMeasurements(autoPickingEnabled, new HashSet<>(evids)).get(500, TimeUnit.SECONDS);
            } else if (stacks != null && !stacks.isEmpty()) {
                measuredMws = service.makeMwMeasurements(autoPickingEnabled, stacks).get(500, TimeUnit.SECONDS);
            } else {
                measuredMws = service.makeMwMeasurements(autoPickingEnabled).get(500, TimeUnit.SECONDS);
            }
            if (measuredMws != null) {
                if (measuredMws.isSuccess()) {
                    resp = ResponseEntity.ok().body(measuredMws.getResultPayload().orElseGet(() -> Collections.emptyList()));
                } else {
                    String errorMessage = "";
                    if (measuredMws.getErrors() != null && !measuredMws.getErrors().isEmpty()) {
                        Exception exception = measuredMws.getErrors().get(0);
                        if (exception instanceof RejectedExecutionException) {
                            resp = ResponseEntity.status(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED).body(errorMessage);
                        }
                    } else {
                        resp = ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMessage);
                    }
                }
            } else {
                resp = ResponseEntity.badRequest().build();
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            resp = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        return resp == null ? ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build() : resp;
    }

}
