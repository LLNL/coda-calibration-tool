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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;

@RestController
@RequestMapping(value = "/api/v1/events", name = "EventsJsonController", produces = MediaType.APPLICATION_JSON_VALUE)
public class EventsJsonController {

    private WaveformService service;

    @Autowired
    public EventsJsonController(WaveformService service) {
        this.service = service;
    }

    @GetMapping(name = "getEvent", value = "/{id}")
    public Event getEvent(@PathVariable("id") String eventId) {
        return service.findEventById(eventId);
    }

    @GetMapping(name = "getUniqueEventIds", value = "/unique-ids")
    public List<String> getUniqueEventIds() {
        return service.getUniqueEventIds();
    }
}
