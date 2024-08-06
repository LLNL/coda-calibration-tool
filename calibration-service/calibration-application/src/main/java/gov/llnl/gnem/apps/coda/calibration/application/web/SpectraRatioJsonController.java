/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraRatioPairDetailsService;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioMeasurementJob;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatiosReport;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.SpectraRatiosReportDTO;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/spectra-ratios", "/api/v1/spectra-ratios/" }, name = "SpectraRatioJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class SpectraRatioJsonController {

    private static final Logger log = LoggerFactory.getLogger(SpectraRatioJsonController.class);

    private SpectraRatioPairDetailsService service;

    @Autowired
    public SpectraRatioJsonController(SpectraRatioPairDetailsService service) {
        this.service = service;
    }

    @GetMapping(name = "getMeasurements", path = { "/all", "/all/" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SpectraRatioPairDetails> getMeasurements() {
        return service.findAll();
    }

    @GetMapping(name = "getMeasurementsMetadata", path = { "/all-metadata-only", "/all-metadata-only/" }, produces = MediaType.APPLICATION_JSON_VALUE)
    public List<SpectraRatioPairDetailsMetadata> getMeasurementsMetadata() {
        return service.findAllMetadataOnly();
    }

    @PostMapping(value = { "/measure-spectra-ratio-from-waveforms", "/measure-spectra-ratio-from-waveforms/" }, name = "measureSpectraRatio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> measureSpectraRatioFromWaveforms(@RequestBody SpectraRatioMeasurementJob job) {
        return measureSpectraRatioFromWaveforms(job.getAutoPickingEnabled(), job.getPersistResults(), job.getSmallEventIds(), job.getLargeEventIds());
    }

    @PostMapping(value = { "/measure-spectra-ratio-from-ratio-data", "/measure-spectra-ratio-from-ratio-data/" }, name = "measureSpectraRatio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> measureSpectraRatioFromRatioData(@RequestBody SpectraRatioMeasurementJob job) {
        return measureSpectraRatioFromRatioData(job.getSmallEventIds(), job.getLargeEventIds(), job.getRatioEventData());
    }

    @PostMapping(value = { "/update-ratio", "/update-ratio/" }, name = "updateSpectraRatio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> updateSpectraRatio(@RequestBody SpectraRatioPairDetails ratio) {
        try {
            return ResponseEntity.ok().body(service.update(ratio));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getLocalizedMessage());
        }
    }

    @PostMapping(value = { "/load-ratios-metadata", "/load-ratios-metadata/" }, name = "loadRatioMetadata", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> loadRatioMetadata(@RequestBody List<SpectraRatioPairDetailsMetadata> ratios) {
        try {
            return ResponseEntity.ok().body(service.loadRatioMetadata(ratios));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getLocalizedMessage());
        }
    }

    private ResponseEntity<?> measureSpectraRatioFromRatioData(Set<String> smallEventIds, Set<String> largeEventIds, List<RatioEventData> ratioEventData) {
        log.trace("Received request to measure spectra ratio");

        ResponseEntity<?> resp = null;
        Result<SpectraRatiosReport> mwsRatiosReport = null;
        try {
            if (smallEventIds != null && !smallEventIds.isEmpty() && largeEventIds != null && !largeEventIds.isEmpty()) {
                mwsRatiosReport = service.makeSpectraRatioMeasurementsFromRatioData(smallEventIds, largeEventIds, ratioEventData).get(4, TimeUnit.HOURS);
            }
            if (mwsRatiosReport != null) {
                if (mwsRatiosReport.isSuccess()) {
                    resp = ResponseEntity.ok().body(new SpectraRatiosReportDTO(mwsRatiosReport.getResultPayload().orElseGet(SpectraRatiosReport::new)));
                } else {
                    String errorMessage = "";
                    if (mwsRatiosReport.getErrors() != null && !mwsRatiosReport.getErrors().isEmpty()) {
                        Exception exception = mwsRatiosReport.getErrors().get(0);
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

    private ResponseEntity<?> measureSpectraRatioFromWaveforms(Boolean autoPickingEnabled, Boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds) {
        log.trace("Received request to measure spectra ratio");

        if (persistResults == null) {
            persistResults = Boolean.FALSE;
        }

        ResponseEntity<?> resp = null;
        Result<SpectraRatiosReport> mwsRatiosReport = null;
        try {
            if (smallEventIds != null && !smallEventIds.isEmpty() && largeEventIds != null && !largeEventIds.isEmpty()) {
                mwsRatiosReport = service.makeSpectraRatioMeasurementsFromWaveforms(autoPickingEnabled, persistResults, smallEventIds, largeEventIds).get(4, TimeUnit.HOURS);
            }
            if (mwsRatiosReport != null) {
                if (mwsRatiosReport.isSuccess()) {
                    resp = ResponseEntity.ok().body(new SpectraRatiosReportDTO(mwsRatiosReport.getResultPayload().orElseGet(SpectraRatiosReport::new)));
                } else {
                    String errorMessage = "";
                    if (mwsRatiosReport.getErrors() != null && !mwsRatiosReport.getErrors().isEmpty()) {
                        Exception exception = mwsRatiosReport.getErrors().get(0);
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
