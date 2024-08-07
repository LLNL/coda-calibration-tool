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

import java.util.HashMap;
import java.util.Map;

import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import gov.llnl.gnem.apps.coda.common.model.util.MESSAGE_HEADERS;

public class TypingMessageTemplate {

    private SimpMessagingTemplate template;

    public TypingMessageTemplate(SimpMessagingTemplate template) {
        this.template = template;
        MappingJackson2MessageConverter messageConverter = new MappingJackson2MessageConverter();
        messageConverter.getObjectMapper().registerModule(new Jdk8Module());
        this.template.setMessageConverter(messageConverter);
    }

    public void convertAndSend(String destination, Object payload) throws MessagingException {
        Map<String, Object> headers = new HashMap<>();
        headers.put(MESSAGE_HEADERS.PAYLOAD_CLASSNAME, payload.getClass().getName());
        template.convertAndSend(destination, payload, headers);
    }
}
