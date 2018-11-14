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

import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import io.springlets.web.NotFoundException;

@RestController
@RequestMapping(value = "/api/v1/synthetics/{id}", name = "SyntheticCodasItemJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class SyntheticItemJsonController {

    private SyntheticService syntheticService;

    @Autowired
    public SyntheticItemJsonController(SyntheticService syntheticService) {
        this.syntheticService = syntheticService;
    }

    @ModelAttribute
    public SyntheticCoda getSyntheticCoda(@PathVariable("id") Long id) {
        SyntheticCoda synthetic = syntheticService.findOne((Long) id);
        if (synthetic == null) {
            synthetic = syntheticService.findOneByWaveformId((Long) id);
            if (synthetic == null) {
                throw new NotFoundException(String.format("SyntheticCoda with identifier '%s' not found", id));
            }
        }
        return synthetic;
    }

    @GetMapping(name = "show")
    public ResponseEntity<?> show(@ModelAttribute SyntheticCoda synthetic) {
        return ResponseEntity.ok(synthetic);
    }

    public static UriComponents showURI(SyntheticCoda synthetic) {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(SyntheticItemJsonController.class).show(synthetic)).buildAndExpand(synthetic.getId()).encode();
    }

    @PutMapping(name = "update")
    public ResponseEntity<?> update(@ModelAttribute SyntheticCoda storedSyntheticCoda, @Valid @RequestBody SyntheticCoda synthetic, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        synthetic.setId(storedSyntheticCoda.getId());
        getSyntheticService().save(synthetic);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(name = "delete")
    public ResponseEntity<?> delete(@ModelAttribute SyntheticCoda synthetic) {
        getSyntheticService().delete(synthetic);
        return ResponseEntity.ok().build();
    }

    public SyntheticService getSyntheticService() {
        return syntheticService;
    }

    public void setSyntheticService(SyntheticService syntheticService) {
        this.syntheticService = syntheticService;
    }
}
