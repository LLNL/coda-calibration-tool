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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeMeasurementService;

@RestController
@RequestMapping(value = "/api/v1/shape-measurements/", name = "ShapeMeasurementJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShapeMeasurementJsonController {

    private ShapeMeasurementService service;

    @Autowired
    public ShapeMeasurementJsonController(ShapeMeasurementService service) {
        this.service = service;
    }

    @GetMapping(path = "/all/", name = "getMeasurements")
    public List<ShapeMeasurement> getMeasurements() {
        return service.findAll();
    }

    @GetMapping("/byWaveformId/{id}")
    public ShapeMeasurement getMeasurementByWaveformId(@PathVariable Long id) {
        return service.findOneByWaveformId(id);
    }

    public ShapeMeasurementService getService() {
        return service;
    }

    public void setService(ShapeMeasurementService service) {
        this.service = service;
    }

}
