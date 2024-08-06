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

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/params/fi", "/api/v1/params/fi/" }, name = "MdacParametersCollectionJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class MdacParametersFiCollectionJsonController {

    private MdacParametersFiService mdacParametersService;

    public MdacParametersFiService getMdacParametersService() {
        return mdacParametersService;
    }

    public void setMdacParametersService(MdacParametersFiService mdacParametersService) {
        this.mdacParametersService = mdacParametersService;
    }

    @Autowired
    public MdacParametersFiCollectionJsonController(MdacParametersFiService mdacParametersService) {
        this.mdacParametersService = mdacParametersService;
    }

    @GetMapping(name = "list")
    public ResponseEntity<List<MdacParametersFI>> list() {
        List<MdacParametersFI> mdacParameters = getMdacParametersService().findAll();
        return ResponseEntity.ok(mdacParameters);
    }

    public static UriComponents listURI() {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(MdacParametersFiCollectionJsonController.class).list()).build().encode();
    }

    @PostMapping(name = "create")
    public ResponseEntity<?> create(@Valid @RequestBody MdacParametersFI mdacParameters, BindingResult result) {

        if (mdacParameters.getId() != null || mdacParameters.getVersion() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        MdacParametersFI newMdacParameters = getMdacParametersService().save(mdacParameters);
        UriComponents showURI = MdacParametersFiItemJsonController.showURI(newMdacParameters);

        return ResponseEntity.created(showURI.toUri()).build();
    }

    @PostMapping(value = { "/batch", "/batch/" }, name = "createBatch")
    public ResponseEntity<?> createBatch(@Valid @RequestBody Collection<MdacParametersFI> mdacParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getMdacParametersService().save(mdacParameters);

        return ResponseEntity.created(listURI().toUri()).build();
    }

    @PostMapping(value = { "/update", "/update/" }, name = "update")
    public ResponseEntity<?> update(@Valid @RequestBody MdacParametersFI entry, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getMdacParametersService().update(entry);

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = { "/batch", "/batch/" }, name = "updateBatch")
    public ResponseEntity<?> updateBatch(@Valid @RequestBody Collection<MdacParametersFI> mdacParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getMdacParametersService().save(mdacParameters);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/batch/{ids}", name = "deleteBatch")
    public ResponseEntity<?> deleteBatch(@PathVariable("ids") Collection<Long> ids) {

        getMdacParametersService().delete(ids);

        return ResponseEntity.ok().build();
    }
}
