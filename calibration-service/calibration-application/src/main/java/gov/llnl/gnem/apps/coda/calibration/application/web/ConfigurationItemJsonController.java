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

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;

@RestController
@RequestMapping(value = "/api/v1/config/", name = "ConfigurationItemJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class ConfigurationItemJsonController {

    private ConfigurationService configService;

    @Autowired
    public ConfigurationItemJsonController(ConfigurationService configService) {
        this.configService = configService;
    }

    @GetMapping(value = "/velocity", name = "getVelocityConfiguration")
    public VelocityConfiguration getVelocityConfiguration() {
        return configService.getVelocityConfiguration();
    }

    @PostMapping(value = "/velocity/update", name = "updateVelocity")
    public ResponseEntity<?> update(@Valid @RequestBody VelocityConfiguration entry, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        configService.update(entry);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/shape", name = "getShapeFitterConstraints")
    public ShapeFitterConstraints getShapeFitterConstraints() {
        return configService.getCalibrationShapeFitterConstraints();
    }

    @PostMapping(value = "/shape/update", name = "updateShapeFitterConstraints")
    public ResponseEntity<?> updateShapeFitterConstraints(@Valid @RequestBody ShapeFitterConstraints entry, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        configService.update(entry);
        return ResponseEntity.ok().build();
    }

    @GetMapping(value = "/polygon", name = "getPolygon")
    public ResponseEntity<String> getPolygon() {
        return ResponseEntity.ok().body(configService.getPolygonGeoJSON());
    }

    @PostMapping(value = "/polygon/update", name = "updatePolygon")
    public ResponseEntity<?> updatePolygon(@Valid @RequestBody String rawGeoJSON, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }        
        return ResponseEntity.ok().body(configService.updatePolygon(rawGeoJSON));
    }
}
