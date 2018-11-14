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
package gov.llnl.gnem.apps.coda.calibration.model.domain.mixins;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;

@JsonIgnoreProperties(value = { "id", "version" })
public class SharedFrequencyBandParametersFileMixin {

    private FrequencyBand frequencyBand = new FrequencyBand();

    @JsonProperty("snrThresholdAboveNoiseLog10")
    private double minSnr;

    @JsonProperty("minWindowLengthSec")
    private double minLength;

    @JsonProperty("maxWindowLengthSec")
    private double maxLength;

    @JsonProperty("measurementTimeSec")
    private double measurementTime = 100d;

    @JsonProperty("lowFreqHz")
    public double getLowFrequency() {
        return this.frequencyBand.getLowFrequency();
    }

    @JsonProperty("lowFreqHz")
    public SharedFrequencyBandParametersFileMixin setLowFrequency(double lowFrequency) {
        this.frequencyBand.setLowFrequency(lowFrequency);
        return this;
    }

    @JsonProperty("highFreqHz")
    public double getHighFrequency() {
        return this.frequencyBand.getHighFrequency();
    }

    @JsonProperty("highFreqHz")
    public SharedFrequencyBandParametersFileMixin setHighFrequency(double highFrequency) {
        this.frequencyBand.setHighFrequency(highFrequency);
        return this;
    }

    public double getMinSnr() {
        return this.minSnr;
    }

    public SharedFrequencyBandParametersFileMixin setMinSnr(double minSnr) {
        this.minSnr = minSnr;
        return this;
    }

    public double getMinLength() {
        return minLength;
    }

    public SharedFrequencyBandParametersFileMixin setMinLength(double minLength) {
        this.minLength = minLength;
        return this;
    }

    public double getMaxLength() {
        return maxLength;
    }

    public SharedFrequencyBandParametersFileMixin setMaxLength(double maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public double getMeasurementTime() {
        return measurementTime;
    }

    public SharedFrequencyBandParametersFileMixin setMeasurementTime(double measurementTime) {
        this.measurementTime = measurementTime;
        return this;
    }
}
