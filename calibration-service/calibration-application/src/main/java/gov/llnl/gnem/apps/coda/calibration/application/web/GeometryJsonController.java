/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.service.api.GeometryService;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/geometry", "/api/v1/geometry/" }, name = "GeometryJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class GeometryJsonController {

    private static final Logger log = LoggerFactory.getLogger(GeometryJsonController.class);

    private GeometryService service;

    @Autowired
    public GeometryJsonController(GeometryService service) {
        this.service = service;
    }

    @PostMapping(value = "/set-active/waveforms-inside-polygon/{active}", name = "measureMws")
    public ResponseEntity<?> setActiveFlagInsidePolygon(@PathVariable Boolean active) {
        ResponseEntity<?> resp = null;
        try {
            service.setActiveFlagInsidePolygon(active);
            resp = ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            log.trace(ex.getLocalizedMessage());
        }
        return resp == null ? ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build() : resp;
    }

    @PostMapping(value = "/set-active/waveforms-outside-polygon/{active}", name = "measureMws")
    public ResponseEntity<?> setActiveFlagOutsidePolygon(@PathVariable Boolean active) {
        ResponseEntity<?> resp = null;
        try {
            service.setActiveFlagOutsidePolygon(active);
            resp = ResponseEntity.ok().build();
        } catch (RuntimeException ex) {
            log.trace(ex.getLocalizedMessage());
        }
        return resp == null ? ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build() : resp;
    }
}
