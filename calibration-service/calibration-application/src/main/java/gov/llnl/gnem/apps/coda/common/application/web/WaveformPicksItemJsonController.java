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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
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

import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformPickService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = "/api/v1/waveform-picks/{waveformPick}", name = "WaveformPicksItemJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class WaveformPicksItemJsonController {

    private WaveformPickService waveformPickService;

    public WaveformPickService getWaveformPickService() {
        return waveformPickService;
    }

    public void setWaveformPickService(WaveformPickService waveformPickService) {
        this.waveformPickService = waveformPickService;
    }

    @Autowired
    public WaveformPicksItemJsonController(WaveformPickService waveformPickService) {
        this.waveformPickService = waveformPickService;
    }

    @ModelAttribute
    public WaveformPick getWaveformPick(@PathVariable("waveformPick") Long id) {
        WaveformPick waveformPick = waveformPickService.findOne(id);
        if (waveformPick == null) {
            throw new IllegalStateException(String.format("WaveformPick with identifier '%s' not found", id));
        }
        return waveformPick;
    }

    @GetMapping(name = "show")
    public ResponseEntity<?> show(@ModelAttribute WaveformPick waveformPick) {
        return ResponseEntity.ok(waveformPick);
    }

    public static UriComponents showURI(WaveformPick waveformPick) {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(WaveformPicksItemJsonController.class).show(waveformPick)).buildAndExpand(waveformPick.getId()).encode();
    }

    @PutMapping(name = "update")
    public ResponseEntity<?> update(@ModelAttribute WaveformPick storedWaveformPick, @Valid @RequestBody WaveformPick waveformPick, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }
        waveformPick.setId(storedWaveformPick.getId());
        getWaveformPickService().save(waveformPick);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping(name = "delete")
    public ResponseEntity<?> delete(@ModelAttribute WaveformPick waveformPick) {
        getWaveformPickService().delete(waveformPick);
        return ResponseEntity.ok().build();
    }
}
