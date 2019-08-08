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
package gov.llnl.gnem.apps.coda.calibration.standalone.data.client;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import reactor.core.publisher.Mono;

@Component
@Primary
public class CalibrationLocalClient implements CalibrationClient {

    private static final Logger log = LoggerFactory.getLogger(CalibrationLocalClient.class);
    private CalibrationService service;

    @Autowired
    public CalibrationLocalClient(CalibrationService service) {
        this.service = service;
    }

    @Override
    public Mono<String> runCalibration(Boolean autoPickingEnabled) {
        return Mono.just(Boolean.toString(service.startCalibration(autoPickingEnabled)));
    }

    @Override
    public Mono<List<MeasuredMwDetails>> makeMwMeasurements(Boolean autoPickingEnabled) {
        try {
            return Mono.just(service.makeMwMeasurements(autoPickingEnabled).get(100l, TimeUnit.SECONDS).getResultPayload().get());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<List<MeasuredMwDetails>> makeMwMeasurements(Boolean autoPickingEnabled, List<String> eventIds) {
        try {
            return Mono.just(service.makeMwMeasurements(autoPickingEnabled, new HashSet<>(eventIds)).get(100l, TimeUnit.SECONDS).getResultPayload().get());
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            log.error(e.getMessage(), e);
            return Mono.empty();
        }
    }

    @Override
    public Mono<String> clearData() {
        return Mono.just(Boolean.toString(service.clearData()));
    }
}
