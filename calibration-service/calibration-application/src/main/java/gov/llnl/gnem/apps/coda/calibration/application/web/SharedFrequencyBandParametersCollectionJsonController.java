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

import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/params/shared-fb-parameters",
        "/api/v1/params/shared-fb-parameters/" }, name = "SharedFrequencyBandParametersCollectionJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class SharedFrequencyBandParametersCollectionJsonController {

    private SharedFrequencyBandParametersService sharedFrequencyBandParametersService;

    public SharedFrequencyBandParametersService getSharedFrequencyBandParametersService() {
        return sharedFrequencyBandParametersService;
    }

    public void setSharedFrequencyBandParametersService(SharedFrequencyBandParametersService sharedFrequencyBandParametersService) {
        this.sharedFrequencyBandParametersService = sharedFrequencyBandParametersService;
    }

    @Autowired
    public SharedFrequencyBandParametersCollectionJsonController(SharedFrequencyBandParametersService sharedFrequencyBandParametersService) {
        this.sharedFrequencyBandParametersService = sharedFrequencyBandParametersService;
    }

    @GetMapping(name = "list")
    public ResponseEntity<List<SharedFrequencyBandParameters>> list() {
        List<SharedFrequencyBandParameters> sharedFrequencyBandParameters = getSharedFrequencyBandParametersService().findAll();
        return ResponseEntity.ok(sharedFrequencyBandParameters);
    }

    public static UriComponents listURI() {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(SharedFrequencyBandParametersCollectionJsonController.class).list()).build().encode();
    }

    @PostMapping(name = "create")
    public ResponseEntity<?> create(@Valid @RequestBody SharedFrequencyBandParameters sharedFrequencyBandParameters, BindingResult result) {

        if (sharedFrequencyBandParameters.getId() != null || sharedFrequencyBandParameters.getVersion() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        SharedFrequencyBandParameters newSharedFrequencyBandParameters = getSharedFrequencyBandParametersService().update(sharedFrequencyBandParameters);
        UriComponents showURI = SharedFrequencyBandParametersItemJsonController.showURI(newSharedFrequencyBandParameters);

        return ResponseEntity.created(showURI.toUri()).build();
    }

    @PostMapping(value = { "/update", "/update/" }, name = "update")
    public ResponseEntity<?> update(@Valid @RequestBody SharedFrequencyBandParameters sharedFbParams, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getSharedFrequencyBandParametersService().update(sharedFbParams);

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = { "/delete", "/delete/" }, name = "delete")
    public ResponseEntity<?> delete(@Valid @RequestBody SharedFrequencyBandParameters sharedFbParams, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        getSharedFrequencyBandParametersService().delete(sharedFbParams);

        return ResponseEntity.ok().build();
    }

    @PostMapping(value = { "/find-by-band", "/find-by-band/" }, name = "findByBand")
    public ResponseEntity<?> findByBand(@Valid @RequestBody FrequencyBand band, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        return ResponseEntity.ok(getSharedFrequencyBandParametersService().findByFrequencyBand(band));
    }

    @PutMapping(value = { "/batch", "/batch/" }, name = "createBatch")
    public ResponseEntity<?> createBatch(@Valid @RequestBody Collection<SharedFrequencyBandParameters> sharedFrequencyBandParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        for (SharedFrequencyBandParameters sfb : sharedFrequencyBandParameters) {
            getSharedFrequencyBandParametersService().update(sfb);
        }

        return ResponseEntity.created(listURI().toUri()).build();
    }

    @PostMapping(value = { "/batch", "/batch/" }, name = "updateBatch")
    public ResponseEntity<?> updateBatch(@Valid @RequestBody Collection<SharedFrequencyBandParameters> sharedFrequencyBandParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        for (SharedFrequencyBandParameters sfb : sharedFrequencyBandParameters) {
            getSharedFrequencyBandParametersService().update(sfb);
        }

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/batch/{ids}", name = "deleteBatch")
    public ResponseEntity<?> deleteBatch(@PathVariable("ids") Collection<Long> ids) {

        getSharedFrequencyBandParametersService().delete(ids);

        return ResponseEntity.ok().build();
    }
}
