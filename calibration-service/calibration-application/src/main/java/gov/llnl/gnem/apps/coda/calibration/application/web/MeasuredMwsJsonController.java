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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/measured-mws", "/api/v1/measured-mws/" }, name = "MeasuredMwsJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeasuredMwsJsonController {

    private MeasuredMwsService service;

    @Autowired
    public MeasuredMwsJsonController(MeasuredMwsService service) {
        this.service = service;
    }

    @GetMapping(name = "getReferenceEvents", value = "/")
    public List<MeasuredMwParameters> getReferenceEvents() {
        return service.findAll();
    }

    @GetMapping(name = "getEventDetails", value = "/details")
    public List<MeasuredMwDetails> getEventDetails() {
        return service.findAllDetails();
    }

    public MeasuredMwsService getService() {
        return service;
    }

    public void setService(MeasuredMwsService service) {
        this.service = service;
    }

}
