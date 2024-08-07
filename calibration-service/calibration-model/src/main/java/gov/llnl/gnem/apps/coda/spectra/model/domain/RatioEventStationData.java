/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

package gov.llnl.gnem.apps.coda.spectra.model.domain;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RatioEventStationData {
    @JsonProperty("stationName")
    private String stationName;
    @JsonProperty("freq")
    private List<Double> freq;
    @JsonProperty("amp")
    private List<Double> amp;

    public RatioEventStationData() {
        this.stationName = null;
        this.freq = null;
        this.amp = null;
    }

    public RatioEventStationData(String stationName, List<Double> frequencyData, List<Double> amplitudeData) {
        this.stationName = stationName;
        this.freq = frequencyData;
        this.amp = amplitudeData;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public List<Double> getFrequencyData() {
        return freq;
    }

    public void setFrequencyData(List<Double> frequencyData) {
        this.freq = frequencyData;
    }

    public List<Double> getAmplitudeData() {
        return amp;
    }

    public void setAmplitudeData(List<Double> amplitudeData) {
        this.amp = amplitudeData;
    }
}
