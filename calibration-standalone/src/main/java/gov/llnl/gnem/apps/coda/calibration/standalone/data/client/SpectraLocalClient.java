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
package gov.llnl.gnem.apps.coda.calibration.standalone.data.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraMeasurementService;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class SpectraLocalClient implements SpectraClient {

    private SpectraMeasurementService service;
    private SharedFrequencyBandParametersService sharedParamsService;

    @Autowired
    public SpectraLocalClient(SpectraMeasurementService service, SharedFrequencyBandParametersService sharedParamsService) {
        this.service = service;
        this.sharedParamsService = sharedParamsService;
    }

    @Override
    public Flux<SpectraMeasurement> getMeasuredSpectra() {
        return Flux.fromIterable(service.findAll()).onErrorReturn(new SpectraMeasurement());
    }

    @Override
    public Flux<SpectraMeasurement> getMeasuredSpectraMetadata() {
        return Flux.fromIterable(service.findAllMetadataOnly()).map(SpectraMeasurement::new).onErrorReturn(new SpectraMeasurement());
    }

    @Override
    public Flux<SpectraMeasurement> getMeasuredSpectraMetadataByIds(List<Long> ids) {
        return Flux.fromIterable(service.findAllMetadataOnly(ids)).map(SpectraMeasurement::new).onErrorReturn(new SpectraMeasurement());
    }

    @Override
    public Mono<Spectra> getReferenceSpectra(String eventId) {
        //FIXME: Use the calibrations phase for this!
        return Mono.just(Optional.ofNullable(service.computeReferenceSpectraForEventId(eventId, sharedParamsService.getFrequencyBands(), PICK_TYPES.LG)).orElseGet(Spectra::new))
                   .onErrorReturn(new Spectra());
    }

    @Override
    public Mono<Spectra> getValidationSpectra(String eventId) {
        //FIXME: Use the calibrations phase for this!
        return Mono.just(Optional.ofNullable(service.computeValidationSpectraForEventId(eventId, sharedParamsService.getFrequencyBands(), PICK_TYPES.LG)).orElseGet(Spectra::new))
                   .onErrorReturn(new Spectra());
    }

    @Override
    public Mono<List<Spectra>> getFitSpectra(String eventId) {
        //FIXME: Use the calibrations phase for this!
        return Mono.just(Optional.ofNullable(service.getFitSpectraForEventId(eventId, sharedParamsService.getFrequencyBands(), PICK_TYPES.LG)).orElseGet(ArrayList::new))
                   .onErrorReturn(new ArrayList<>());
    }

}
