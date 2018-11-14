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

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import io.springlets.web.NotFoundException;

@RestController
@RequestMapping(value = "/api/v1/params/fi/{fiParameters}", name = "MdacParametersFiItemJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class MdacParametersFiItemJsonController {

    private MdacParametersFiService fiParametersService;

    public MdacParametersFiService getMdacParametersService() {
        return fiParametersService;
    }

    public void setMdacParametersService(MdacParametersFiService fiParametersService) {
        this.fiParametersService = fiParametersService;
    }

    @Autowired
    public MdacParametersFiItemJsonController(MdacParametersFiService fiParametersService) {
        this.fiParametersService = fiParametersService;
    }

    @ModelAttribute
    public MdacParametersFI getMdacParameters(@PathVariable("fiParameters") Long id) {
        MdacParametersFI fiParameters = fiParametersService.findOne(id);
        if (fiParameters == null) {
            throw new NotFoundException(String.format("MdacParameters with identifier '%s' not found", id));
        }
        return fiParameters;
    }

    @GetMapping(name = "show")
    public ResponseEntity<?> show(@ModelAttribute MdacParametersFI fiParameters) {
        return ResponseEntity.ok(fiParameters);
    }

    public static UriComponents showURI(MdacParametersFI fiParameters) {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(MdacParametersFiItemJsonController.class).show(fiParameters)).buildAndExpand(fiParameters.getId()).encode();
    }

    @PutMapping(name = "update")
    public ResponseEntity<?> update(@ModelAttribute MdacParametersFI storedMdacParameters, @Valid @RequestBody MdacParametersFI fiParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        fiParameters.setId(storedMdacParameters.getId());
        getMdacParametersService().save(fiParameters);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(name = "delete")
    public ResponseEntity<?> delete(@ModelAttribute MdacParametersFI fiParameters) {
        getMdacParametersService().delete(fiParameters);
        return ResponseEntity.ok().build();
    }
}
