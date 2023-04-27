/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318
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
package gov.llnl.gnem.apps.coda.spectra.model.domain;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

public class SpectraRatioMeasurementJob {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private List<String> smallEventIds = null;
    private List<String> largeEventIds = null;
    private List<Waveform> smallStacks = null;
    private List<Waveform> largeStacks = null;
    private Boolean autopickingEnabled = Boolean.FALSE;
    private Boolean persistResults = Boolean.FALSE;

    public List<String> getSmallEventIds() {
        return smallEventIds;
    }

    public SpectraRatioMeasurementJob setSmallEventIds(List<String> eventIds) {
        this.smallEventIds = eventIds;
        return this;
    }

    public List<Waveform> getSmallStacks() {
        return smallStacks;
    }

    public SpectraRatioMeasurementJob setSmallStacks(List<Waveform> stacks) {
        this.smallStacks = stacks;
        return this;
    }

    public List<String> getLargeEventIds() {
        return largeEventIds;
    }

    public SpectraRatioMeasurementJob setLargeEventIds(List<String> eventIds) {
        this.largeEventIds = eventIds;
        return this;
    }

    public List<Waveform> getLargeStacks() {
        return largeStacks;
    }

    public SpectraRatioMeasurementJob setLargeStacks(List<Waveform> stacks) {
        this.largeStacks = stacks;
        return this;
    }

    public Boolean getAutopickingEnabled() {
        return autopickingEnabled;
    }

    public SpectraRatioMeasurementJob setAutopickingEnabled(Boolean autopickingEnabled) {
        this.autopickingEnabled = autopickingEnabled;
        return this;
    }

    public Boolean getPersistResults() {
        return persistResults;
    }

    public SpectraRatioMeasurementJob setPersistResults(Boolean persistResults) {
        this.persistResults = persistResults;
        return this;
    }

}
