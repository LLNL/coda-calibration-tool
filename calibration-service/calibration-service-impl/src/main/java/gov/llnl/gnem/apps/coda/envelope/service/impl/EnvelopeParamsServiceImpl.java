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
package gov.llnl.gnem.apps.coda.envelope.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeParamsService;

@Service
@Scope("singleton")
public class EnvelopeParamsServiceImpl implements EnvelopeParamsService {

    private EnvelopeJobConfiguration config;
    private static final Object lock = new Object();

    @Autowired
    private EnvelopeParamsServiceImpl(EnvelopeJobConfiguration config) {
        super();
        this.config = config;
    }

    @Override
    public EnvelopeJobConfiguration getConfiguration() {
        synchronized (lock) {
            return config;
        }
    }

    @Override
    public void setConfiguration(EnvelopeJobConfiguration envConf) {
        synchronized (lock) {
            if (envConf != null) {
                config = envConf;
            }
        }
    }
}
