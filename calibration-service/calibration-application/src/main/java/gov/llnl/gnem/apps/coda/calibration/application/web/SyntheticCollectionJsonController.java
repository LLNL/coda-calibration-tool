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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticCodaGenerationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;

@RestController
@RequestMapping(value = "/api/v1/synthetics", name = "SyntheticCollectionJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class SyntheticCollectionJsonController {

    private static final Logger log = LoggerFactory.getLogger(SyntheticCollectionJsonController.class);

    private SyntheticService syntheticService;

    private SyntheticCodaGenerationService genService;

    @Autowired
    public SyntheticCollectionJsonController(SyntheticService syntheticService, SyntheticCodaGenerationService genService) {
        this.syntheticService = syntheticService;
        this.genService = genService;
    }

    @GetMapping(value = "/all", name = "getAll")
    public ResponseEntity<?> getAll() {
        List<SyntheticCoda> data = syntheticService.findAll();
        if (data == null) {
            data = new ArrayList<>(0);
        }
        return ResponseEntity.ok().body(data);
    }

    @GetMapping(value = "/batch/{ids}", name = "getBatch")
    public ResponseEntity<?> getBatch(@PathVariable("ids") Collection<Long> ids) {
        List<SyntheticCoda> data = new ArrayList<>(ids.size());
        for (Long id : ids) {
            SyntheticCoda synthetic = syntheticService.findOne(id);
            if (synthetic == null) {
                synthetic = syntheticService.findOneByWaveformId(id);
                if (synthetic != null) {
                    data.add(synthetic);
                }
            }
        }

        log.trace("SyntheticCoda with identifiers {} not found", ids);
        return ResponseEntity.ok().body(data);
    }

    @GetMapping(value = "/single/{id}", name = "getSyntheticCoda")
    public ResponseEntity<?> getSyntheticCoda(@PathVariable("id") Long id) {
        SyntheticCoda synthetic = syntheticService.findOne(id);
        if (synthetic == null) {
            synthetic = syntheticService.findOneByWaveformId(id);
            if (synthetic == null) {
                log.trace("SyntheticCoda with identifier {} not found", id);
            }
        }
        return ResponseEntity.ok().body(synthetic);
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

    @PostMapping(value = "/generate", name = "generateSynthetic")
    public ResponseEntity<?> generateSynthetic(@Valid @RequestBody SyntheticGenerationRequest request, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        SyntheticCoda synthetic = genService.generateSynthetic(request.getDistanceKm(), request.getLowFreqHz(), request.getHighFreqHz(), request.getLengthSeconds());
        if (synthetic != null) {
            return ResponseEntity.ok(synthetic);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Service does not recognize that frequency band, did you load the correct parameters?");
        }
    }

    public SyntheticService getSyntheticService() {
        return syntheticService;
    }

    public void setSyntheticService(SyntheticService syntheticService) {
        this.syntheticService = syntheticService;
    }
}
