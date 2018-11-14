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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import gov.llnl.gnem.apps.coda.calibration.model.messaging.BandParametersDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.MdacDataChangeEvent;
import gov.llnl.gnem.apps.coda.common.application.web.TypingMessageTemplate;
import gov.llnl.gnem.apps.coda.common.service.api.Listener;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Controller
public class CalibrationNotificationStompController {

    @Autowired
    public CalibrationNotificationStompController(SimpMessagingTemplate template, NotificationService notificationService) {
        final TypingMessageTemplate typingTemplate = new TypingMessageTemplate(template);

        notificationService.register(new Listener<CalibrationStatusEvent>() {
            @Override
            public void apply(CalibrationStatusEvent event) {
                typingTemplate.convertAndSend("/topic/calibration-events", event);
            }

            @Override
            public Class<CalibrationStatusEvent> getType() {
                return CalibrationStatusEvent.class;
            }
        });

        notificationService.register(new Listener<MdacDataChangeEvent>() {
            @Override
            public void apply(MdacDataChangeEvent event) {
                typingTemplate.convertAndSend("/topic/calibration-events", event);
            }

            @Override
            public Class<MdacDataChangeEvent> getType() {
                return MdacDataChangeEvent.class;
            }
        });

        notificationService.register(new Listener<BandParametersDataChangeEvent>() {
            @Override
            public void apply(BandParametersDataChangeEvent event) {
                typingTemplate.convertAndSend("/topic/calibration-events", event);
            }

            @Override
            public Class<BandParametersDataChangeEvent> getType() {
                return BandParametersDataChangeEvent.class;
            }
        });
    }
}
