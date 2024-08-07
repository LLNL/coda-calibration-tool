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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/calibration", "/api/v1/calibration/" }, name = "CalibrationJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class CalibrationJsonController {

    private CalibrationService service;

    @Autowired
    public CalibrationJsonController(CalibrationService service) {
        this.service = service;
    }

    @GetMapping(value = "/start/{autoPickingEnabled}", name = "runCalibration")
    public ResponseEntity<?> runCalibration(@PathVariable(name = "autoPickingEnabled", required = false) Boolean autoPickingEnabled) {
        if (autoPickingEnabled == null) {
            autoPickingEnabled = Boolean.FALSE;
        }
        if (getService().startCalibration(autoPickingEnabled)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping(value = { "/cancel", "/cancel/" }, name = "cancelCalibration")
    public ResponseEntity<?> cancelCalibration(@Valid @RequestBody Long id, BindingResult result) {
        if (getService().cancelCalibration(id)) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping(value = { "/clear-data", "/clear-data/" }, name = "clearData")
    public ResponseEntity<?> clearData() {
        if (getService().clearData()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().build();
        }
    }

    //TODO: A little gross API wise to cram this here but will do for now. Revisit in V2
    @PostMapping(value = { "/toggle-validation/batch-by-evids", "/toggle-validation/batch-by-evids/" }, name = "toggleValidationByEvids")
    public ResponseEntity<?> deleteBatchByEvids(@Valid @RequestBody List<String> eventIds, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        return ResponseEntity.ok(service.toggleAllByEventIds(eventIds));
    }

    public CalibrationService getService() {
        return service;
    }

    public void setService(CalibrationService service) {
        this.service = service;
    }

}
