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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwReportByEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasurementJob;
import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

@RestController
@RequestMapping(value = "/api/v1/measurement", name = "MeasurementJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeasurementJsonController {

    private static final Logger log = LoggerFactory.getLogger(MeasurementJsonController.class);

    private CalibrationService calibrationService;

    @Autowired
    public MeasurementJsonController(CalibrationService service) {
        this.calibrationService = service;
    }

    @PostMapping(value = "/measure-mws", name = "measureMws")
    public ResponseEntity<?> measureMws(@RequestBody MeasurementJob job) {
        return measureMw(job.getAutopickingEnabled(), job.getPersistResults(), job.getEventIds(), job.getStacks());
    }

    private ResponseEntity<?> measureMw(Boolean autoPickingEnabled, Boolean persistResults, List<String> evids, List<Waveform> stacks) {
        if (autoPickingEnabled == null) {
            autoPickingEnabled = Boolean.FALSE;
        }
        if (persistResults == null) {
            persistResults = Boolean.FALSE;
        }

        ResponseEntity<?> resp = null;
        try {
            Result<MeasuredMwReportByEvent> measuredMws;
            if (evids != null && !evids.isEmpty()) {
                measuredMws = calibrationService.makeMwMeasurements(autoPickingEnabled, persistResults, new HashSet<>(evids)).get(4, TimeUnit.HOURS);
            } else if (stacks != null && !stacks.isEmpty()) {
                measuredMws = calibrationService.makeMwMeasurements(autoPickingEnabled, persistResults, stacks).get(4, TimeUnit.HOURS);
            } else {
                measuredMws = calibrationService.makeMwMeasurements(autoPickingEnabled, persistResults).get(4, TimeUnit.HOURS);
            }
            if (measuredMws != null) {
                if (measuredMws.isSuccess()) {
                    resp = ResponseEntity.ok().body(measuredMws.getResultPayload().orElseGet(MeasuredMwReportByEvent::new));
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