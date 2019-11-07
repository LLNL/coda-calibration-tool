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
package gov.llnl.gnem.apps.coda.common.gui.data.client;

import java.lang.reflect.Type;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.events.SocketDisconnectEvent;
import gov.llnl.gnem.apps.coda.common.model.util.MESSAGE_HEADERS;

@Component
public class EventBusStompSessionHandler extends StompSessionHandlerAdapter {
    private static final Logger log = LoggerFactory.getLogger(EventBusStompSessionHandler.class);
    private EventBus bus;

    @Autowired
    public EventBusStompSessionHandler(EventBus bus) {
        super();
        this.bus = bus;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        Type c;
        if (headers.containsKey(MESSAGE_HEADERS.PAYLOAD_CLASSNAME)) {
            try {
                c = Class.forName(headers.getFirst(MESSAGE_HEADERS.PAYLOAD_CLASSNAME));
            } catch (ReflectiveOperationException e) {
                c = super.getPayloadType(headers);
            }
        } else {
            c = super.getPayloadType(headers);
        }
        return c;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        if (payload != null) {
            log.trace("Message {}", payload);
            bus.post(payload);
        }
    }

    @Override
    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
        super.afterConnected(session, connectedHeaders);
        log.trace("Connected {}{}", session, connectedHeaders);
    }

    @Override
    public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload, Throwable exception) {
        super.handleException(session, command, headers, payload, exception);
        log.trace("StompException {}{}{}{}{}", session, command, headers, payload, exception);
    }

    @Override
    public void handleTransportError(StompSession session, Throwable exception) {
        super.handleTransportError(session, exception);
        log.trace("StompTransportError {}{}", session, exception);
        if (exception instanceof ConnectionLostException) {
            bus.post(new SocketDisconnectEvent());
        }
    }
}
