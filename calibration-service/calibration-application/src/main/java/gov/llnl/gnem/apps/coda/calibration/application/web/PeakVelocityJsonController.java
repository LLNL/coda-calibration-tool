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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurementMetadata;
import gov.llnl.gnem.apps.coda.calibration.service.api.PeakVelocityMeasurementService;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/peak-velocity-measurements", "/api/v1/peak-velocity-measurements/" }, name = "PeakVelocityJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class PeakVelocityJsonController {

    private PeakVelocityMeasurementService service;

    @Autowired
    public PeakVelocityJsonController(PeakVelocityMeasurementService service) {
        this.service = service;
    }

    @GetMapping(name = "getMeasurements", path = { "/all", "/all/" })
    public List<PeakVelocityMeasurement> getMeasurements() {
        return service.findAll();
    }

    @GetMapping(name = "getMeasurementsMetadataOnly", path = { "/metadata/all", "/metadata/all/" })
    public List<PeakVelocityMeasurementMetadata> getMeasurementsMetadataOnly() {
        return service.findAllMetadataOnly();
    }

    @GetMapping(name = "getMeasurementsMetadataOnly", path = "/metadata/by-id/{id}")
    public PeakVelocityMeasurementMetadata getByWaveformIdMetadataOnly(@PathVariable(name = "id", required = true) Long id) {
        return service.findByWaveformIdMetadataOnly(id);
    }

    public PeakVelocityMeasurementService getService() {
        return service;
    }

    public void setService(PeakVelocityMeasurementService service) {
        this.service = service;
    }

}
