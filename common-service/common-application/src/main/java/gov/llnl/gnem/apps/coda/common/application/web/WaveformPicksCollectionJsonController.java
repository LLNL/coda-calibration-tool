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

import java.util.Collection;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponents;

import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformPickService;

@RestController
@RequestMapping(value = "/api/v1/waveform-picks", name = "WaveformPicksCollectionJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class WaveformPicksCollectionJsonController {

    private WaveformPickService waveformPickService;

    @Autowired
    public WaveformPicksCollectionJsonController(WaveformPickService waveformPickService) {
        this.waveformPickService = waveformPickService;
    }

    @PostMapping(name = "create")
    public ResponseEntity<?> create(@Valid @RequestBody WaveformPick waveformPick, BindingResult result) {

        if (waveformPick.getId() != null || waveformPick.getVersion() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        WaveformPick newWaveformPick = getWaveformPickService().save(waveformPick);
        UriComponents showURI = WaveformPicksItemJsonController.showURI(newWaveformPick);

        return ResponseEntity.created(showURI.toUri()).build();
    }

    @PostMapping(value = "/batch", name = "createBatch")
    public ResponseEntity<?> createBatch(@Valid @RequestBody Collection<WaveformPick> waveformPicks, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getWaveformPickService().save(waveformPicks);

        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/batch", name = "updateBatch")
    public ResponseEntity<?> updateBatch(@Valid @RequestBody Collection<WaveformPick> waveformPicks, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getWaveformPickService().save(waveformPicks);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/batch/{ids}", name = "deleteBatch")
    public ResponseEntity<Void> deleteBatch(@PathVariable("ids") Collection<Long> ids) {

        getWaveformPickService().delete(ids);

        return ResponseEntity.ok().build();
    }

    public WaveformPickService getWaveformPickService() {
        return waveformPickService;
    }

    public void setWaveformPickService(WaveformPickService waveformPickService) {
        this.waveformPickService = waveformPickService;
    }
}
