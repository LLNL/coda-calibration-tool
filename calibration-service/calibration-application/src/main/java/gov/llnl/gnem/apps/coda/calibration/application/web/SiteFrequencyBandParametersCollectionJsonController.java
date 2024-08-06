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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.util.UriComponents;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import jakarta.validation.Valid;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/params/site-fb-parameters",
        "/api/v1/params/site-fb-parameters/" }, name = "SiteFrequencyBandParametersCollectionJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class SiteFrequencyBandParametersCollectionJsonController {

    private SiteFrequencyBandParametersService siteFrequencyBandParametersService;

    public SiteFrequencyBandParametersService getSiteFrequencyBandParametersService() {
        return siteFrequencyBandParametersService;
    }

    public void setSiteFrequencyBandParametersService(SiteFrequencyBandParametersService siteFrequencyBandParametersService) {
        this.siteFrequencyBandParametersService = siteFrequencyBandParametersService;
    }

    @Autowired
    public SiteFrequencyBandParametersCollectionJsonController(SiteFrequencyBandParametersService siteFrequencyBandParametersService) {
        this.siteFrequencyBandParametersService = siteFrequencyBandParametersService;
    }

    @GetMapping(name = "list")
    public ResponseEntity<List<SiteFrequencyBandParameters>> list() {
        List<SiteFrequencyBandParameters> siteFrequencyBandParameters = getSiteFrequencyBandParametersService().findAll();
        return ResponseEntity.ok(siteFrequencyBandParameters);
    }

    public static UriComponents listURI() {
        return MvcUriComponentsBuilder.fromMethodCall(MvcUriComponentsBuilder.on(SiteFrequencyBandParametersCollectionJsonController.class).list()).build().encode();
    }

    @PostMapping(name = "create")
    public ResponseEntity<?> create(@Valid @RequestBody SiteFrequencyBandParameters siteFrequencyBandParameters, BindingResult result) {

        if (siteFrequencyBandParameters.getId() != null || siteFrequencyBandParameters.getVersion() != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        SiteFrequencyBandParameters newSiteFrequencyBandParameters = getSiteFrequencyBandParametersService().save(siteFrequencyBandParameters);
        return ResponseEntity.status(HttpStatus.OK).body(newSiteFrequencyBandParameters.getId());
    }

    @PostMapping(value = { "/batch", "/batch/" }, name = "updateBatch")
    public ResponseEntity<?> updateBatch(@Valid @RequestBody Collection<SiteFrequencyBandParameters> siteFrequencyBandParameters, BindingResult result) {

        if (result.hasErrors()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        getSiteFrequencyBandParametersService().save(siteFrequencyBandParameters);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping(value = "/batch/{ids}", name = "deleteBatch")
    public ResponseEntity<?> deleteBatch(@PathVariable("ids") Collection<Long> ids) {

        getSiteFrequencyBandParametersService().delete(ids);

        return ResponseEntity.ok().build();
    }
}
