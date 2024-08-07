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

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraMeasurementService;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

@RestController
@CrossOrigin
@RequestMapping(value = { "/api/v1/spectra-measurements", "/api/v1/spectra-measurements/" }, name = "SpectraMeasurementJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class SpectraMeasurementJsonController {

    private SpectraMeasurementService service;
    private SharedFrequencyBandParametersService sharedParamsService;

    @Autowired
    public SpectraMeasurementJsonController(SpectraMeasurementService service, SharedFrequencyBandParametersService sharedParamsService) {
        this.service = service;
        this.sharedParamsService = sharedParamsService;
    }

    @GetMapping(name = "getMeasurements", path = { "/all", "/all/" })
    public List<SpectraMeasurement> getMeasurements() {
        return service.findAll();
    }

    @GetMapping(name = "getMeasurementsMetadata", path = { "/metadata/all", "/metadata/all/" })
    public List<SpectraMeasurement> getMeasurementsMetadata() {
        return service.findAllMetadataOnly().stream().map(SpectraMeasurement::new).collect(Collectors.toList());
    }

    @PostMapping(name = "getMeasurementsMetadataById", path = { "/metadata/by-ids", "/metadata/by-ids/" })
    public List<SpectraMeasurement> getMeasurementsMetadataByIds(@RequestBody List<Long> ids) {
        return service.findAllMetadataOnly(ids).stream().map(SpectraMeasurement::new).collect(Collectors.toList());
    }

    @PostMapping(value = { "/reference-spectra", "/reference-spectra/" }, name = "computeSpectraForEventId")
    public ResponseEntity<?> computeSpectraForEventId(@RequestBody String eventId, BindingResult result) {
        //FIXME: Accept a phase to use!
        Spectra theoreticalSpectra = service.computeReferenceSpectraForEventId(eventId, sharedParamsService.getFrequencyBands(), PICK_TYPES.LG);
        return ResponseEntity.ok(theoreticalSpectra);
    }

    @PostMapping(value = { "/validation-spectra", "/validation-spectra/" }, name = "computeValidationSpectraForEventId")
    public ResponseEntity<?> computeValidationSpectraForEventId(@RequestBody String eventId, BindingResult result) {
        //FIXME: Accept a phase to use!
        Spectra theoreticalSpectra = service.computeValidationSpectraForEventId(eventId, sharedParamsService.getFrequencyBands(), PICK_TYPES.LG);
        return ResponseEntity.ok(theoreticalSpectra);
    }

    @PostMapping(value = { "/compute-spectra", "/compute-spectra/" }, name = "computeSpectra")
    public ResponseEntity<?> computeSpectra(@RequestBody Double moment, @RequestBody Double apparentStress, @RequestBody Double start, @RequestBody Double stop, @RequestBody Integer count,
            BindingResult result) {
        Spectra theoreticalSpectra = service.getSpecificSpectra(moment, apparentStress, start, stop, count);
        return ResponseEntity.ok(theoreticalSpectra);
    }

    @PostMapping(value = { "/fit-spectra", "/fit-spectra/" }, name = "getFitSpectraForEventId")
    public ResponseEntity<?> getFitSpectraForEventId(@RequestBody String eventId, BindingResult result) {
        //FIXME: Accept a phase to use!
        List<Spectra> fitSpectra = service.getFitSpectraForEventId(eventId, sharedParamsService.getFrequencyBands(), PICK_TYPES.LG);
        return ResponseEntity.ok(fitSpectra);
    }

    public SpectraMeasurementService getService() {
        return service;
    }

    public void setService(SpectraMeasurementService service) {
        this.service = service;
    }

}
