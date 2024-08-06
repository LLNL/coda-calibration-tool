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
package gov.llnl.gnem.apps.coda.common.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.common.service.api.Listener;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Service
public class NotificationServiceImpl implements NotificationService {
    private List<Listener<?>> listeners = new ArrayList<>();
    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @SuppressWarnings("unchecked")
    @Override
    public <T> void post(T event) {
        if (event != null) {
            for (Listener<?> listener : listeners) {
                if (listener.getType().isInstance(event)) {
                    try {
                        ((Listener<T>) listener).apply(event);
                    } catch (RuntimeException ex) {
                        log.warn(ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }
    }

    @Override
    public void register(Listener<?> listener) {
        listeners.add(listener);
    }
}
