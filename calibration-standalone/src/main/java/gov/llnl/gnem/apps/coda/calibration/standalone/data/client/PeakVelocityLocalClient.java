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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurementMetadata;
import gov.llnl.gnem.apps.coda.calibration.service.api.PeakVelocityMeasurementService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class PeakVelocityLocalClient implements PeakVelocityClient {

    private PeakVelocityMeasurementService service;

    @Autowired
    public PeakVelocityLocalClient(PeakVelocityMeasurementService service) {
        this.service = service;
    }

    @Override
    public Flux<PeakVelocityMeasurement> getMeasuredPeakVelocities() {
        return Flux.fromIterable(service.findAll()).onErrorReturn(new PeakVelocityMeasurement());
    }

    @Override
    public Flux<PeakVelocityMeasurement> getMeasuredPeakVelocitiesMetadata() {
        return Flux.fromIterable(service.findAllMetadataOnly()).map(md -> new PeakVelocityMeasurement(md)).onErrorReturn(new PeakVelocityMeasurement());
    }

    @Override
    public Mono<PeakVelocityMeasurement> getNoiseForWaveform(Long id) {
        PeakVelocityMeasurementMetadata metadata = service.findByWaveformIdMetadataOnly(id);
        if (metadata != null) {
            PeakVelocityMeasurement data = new PeakVelocityMeasurement(metadata);
            return Mono.just(data);
        }
        return Mono.just(new PeakVelocityMeasurement());
    }
}
