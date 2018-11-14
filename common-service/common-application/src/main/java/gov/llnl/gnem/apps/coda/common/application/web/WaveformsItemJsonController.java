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
package gov.llnl.gnem.apps.coda.common.application.web;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import io.springlets.web.NotFoundException;

@RestController
@RequestMapping(value = "/api/v1/single-waveform", name = "WaveformsItemJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class WaveformsItemJsonController {

    /**
     * 
     * @param waveformService
     */
    @Autowired
    public WaveformsItemJsonController(WaveformService waveformService) {
        this.waveformService = waveformService;
    }

    /**
     * 
     * @param id
     * @return Waveform
     */
    @GetMapping(value = "{id}", name = "getWaveform")
    public Waveform getWaveform(@PathVariable("id") Long id) {
        Waveform waveform = waveformService.findOne(id);
        if (waveform == null) {
            throw new NotFoundException(String.format("Waveform with identifier '%s' not found", id));
        }
        return waveform;
    }
    
    /**
     * 
     * @param storedWaveform
     * @param waveform
     * @param result
     * @return ResponseEntity
     */
    @PostMapping(name = "update")
    public ResponseEntity<?> update(@Valid @RequestBody Waveform waveform, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        Waveform wave = getWaveformService().save(waveform);
        return ResponseEntity.ok(wave);
    }

    @DeleteMapping(name = "delete")
    public ResponseEntity<?> delete(@ModelAttribute Waveform waveform) {
        getWaveformService().delete(waveform);
        return ResponseEntity.ok().build();
    }

    private WaveformService waveformService;

    public WaveformService getWaveformService() {
        return waveformService;
    }

    public void setWaveformService(WaveformService waveformService) {
        this.waveformService = waveformService;
    }
}
