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

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ValidationMwParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@Primary
public class EventLocalClient implements EventClient {

    private ReferenceMwParametersService refEventService;
    private ValidationMwParametersService valEventService;
    private MeasuredMwsService measureService;
    private WaveformService waveformService;
    private CalibrationService calService;

    @Autowired
    public EventLocalClient(ReferenceMwParametersService refEventService, ValidationMwParametersService valEventService, MeasuredMwsService measureService, CalibrationService calService,
            WaveformService waveformService) {
        this.refEventService = refEventService;
        this.valEventService = valEventService;
        this.measureService = measureService;
        this.calService = calService;
        this.waveformService = waveformService;
    }

    @Override
    public Flux<ReferenceMwParameters> getReferenceEvents() {
        return Flux.fromIterable(refEventService.findAll()).filter(Objects::nonNull).onErrorReturn(new ReferenceMwParameters());
    }

    @Override
    public Mono<String> postReferenceEvents(List<ReferenceMwParameters> refEvents) throws JsonProcessingException {
        return Mono.just(refEventService.save(refEvents).toString());
    }

    @Override
    public Mono<Void> removeReferenceEventsByEventId(List<String> evids) {
        refEventService.deleteAllByEventIds(evids);
        return Mono.empty();
    }

    @Override
    public Flux<ValidationMwParameters> getValidationEvents() {
        return Flux.fromIterable(valEventService.findAll()).filter(Objects::nonNull).onErrorReturn(new ValidationMwParameters());
    }

    @Override
    public Mono<String> postValidationEvents(List<ValidationMwParameters> valEvents) throws JsonProcessingException {
        return Mono.just(valEventService.save(valEvents).toString());
    }

    @Override
    public Mono<Void> removeValidationEventsByEventId(List<String> evids) {
        valEventService.deleteAllByEventIds(evids);
        return Mono.empty();
    }

    @Override
    public Flux<String> toggleValidationEventsByEventId(List<String> evids) {
        return Flux.fromIterable(calService.toggleAllByEventIds(evids));
    }

    @Override
    public Flux<MeasuredMwParameters> getMeasuredEvents() {
        return Flux.fromIterable(measureService.findAll()).filter(Objects::nonNull).onErrorReturn(new MeasuredMwParameters());
    }

    @Override
    public Mono<Event> getEvent(String eventId) {
        Event event = waveformService.findEventById(eventId);
        if (event == null) {
            event = new Event();
        }
        return Mono.just(event);
    }

    @Override
    public Flux<MeasuredMwDetails> getMeasuredEventDetails() {
        return Flux.fromIterable(measureService.findAllDetails()).filter(Objects::nonNull).onErrorReturn(new MeasuredMwDetails());
    }

    @Override
    public Flux<String> getUniqueEventIds() {
        return Flux.fromIterable(waveformService.getUniqueEventIds());
    }
}
