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

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import io.springlets.web.NotFoundException;

@RestController
@RequestMapping(value = "/api/v1/params/ps/{psParameters}", name = "MdacParametersPsItemJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class MdacParametersPsItemJsonController {

    private MdacParametersPsService psParametersService;

    public MdacParametersPsService getMdacParametersService() {
        return psParametersService;
    }

    public void setMdacParametersService(MdacParametersPsService psParametersService) {
        this.psParametersService = psParametersService;
    }

    @Autowired
    public MdacParametersPsItemJsonController(MdacParametersPsService psParametersService) {
        this.psParametersService = psParametersService;
    }

    @ModelAttribute
    public MdacParametersPS getMdacParameters(@PathVariable("psParameters") Long id) {
        MdacParametersPS psParameters = psParametersService.findOne(id);
        if (psParameters == null) {
            throw new NotFoundException(String.format("MdacParameters with identifier '%s' not found", id));
        }
        return psParameters;
    }

    @GetMapping(name = "show")
    public ResponseEntity<?> show(@ModelAttribute MdacParametersPS psParameters) {
        return ResponseEntity.ok(psParameters);
    }

    public static UriComponents showURI(MdacParametersPS psParameters) {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(MdacParametersPsItemJsonController.class).show(psParameters)).buildAndExpand(psParameters.getId()).encode();
    }

    @PutMapping(name = "update")
    public ResponseEntity<?> update(@ModelAttribute MdacParametersPS storedMdacParameters, @Valid @RequestBody MdacParametersPS psParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        psParameters.setId(storedMdacParameters.getId());
        getMdacParametersService().save(psParameters);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(name = "delete")
    public ResponseEntity<?> delete(@ModelAttribute MdacParametersPS psParameters) {
        getMdacParametersService().delete(psParameters);
        return ResponseEntity.ok().build();
    }
}
