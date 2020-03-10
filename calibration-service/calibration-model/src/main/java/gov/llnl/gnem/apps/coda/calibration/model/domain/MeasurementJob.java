/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

public class MeasurementJob {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private List<String> eventIds = null;
    private List<Waveform> stacks = null;
    private Boolean autopickingEnabled = Boolean.FALSE;
    private Boolean persistResults = Boolean.FALSE;

    public List<String> getEventIds() {
        return eventIds;
    }

    public MeasurementJob setEventIds(List<String> eventIds) {
        this.eventIds = eventIds;
        return this;
    }

    public List<Waveform> getStacks() {
        return stacks;
    }

    public MeasurementJob setStacks(List<Waveform> stacks) {
        this.stacks = stacks;
        return this;
    }

    public Boolean getAutopickingEnabled() {
        return autopickingEnabled;
    }

    public MeasurementJob setAutopickingEnabled(Boolean autopickingEnabled) {
        this.autopickingEnabled = autopickingEnabled;
        return this;
    }

    public Boolean getPersistResults() {
        return persistResults;
    }

    public MeasurementJob setPersistResults(Boolean persistResults) {
        this.persistResults = persistResults;
        return this;
    }

}
