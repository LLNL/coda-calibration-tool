/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.Objects;

public class SyntheticGenerationRequest {
    private Double distanceKm;
    private Double lowFreqHz;
    private Double highFreqHz;
    private Double lengthSeconds;

    public SyntheticGenerationRequest() {
        //nop - for serialization
    }

    public SyntheticGenerationRequest(Double distanceKm, Double lowFreqHz, Double highFreqHz, Double lengthSeconds) {
        this.distanceKm = distanceKm;
        this.lowFreqHz = lowFreqHz;
        this.highFreqHz = highFreqHz;
        this.lengthSeconds = lengthSeconds;
    }

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Double getLowFreqHz() {
        return lowFreqHz;
    }

    public void setLowFreqHz(Double lowFreqHz) {
        this.lowFreqHz = lowFreqHz;
    }

    public Double getHighFreqHz() {
        return highFreqHz;
    }

    public void setHighFreqHz(Double highFreqHz) {
        this.highFreqHz = highFreqHz;
    }

    public Double getLengthSeconds() {
        return lengthSeconds;
    }

    public void setLengthSeconds(Double lengthSeconds) {
        this.lengthSeconds = lengthSeconds;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distanceKm, highFreqHz, lengthSeconds, lowFreqHz);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SyntheticGenerationRequest)) {
            return false;
        }
        SyntheticGenerationRequest other = (SyntheticGenerationRequest) obj;
        return Objects.equals(distanceKm, other.distanceKm)
                && Objects.equals(highFreqHz, other.highFreqHz)
                && Objects.equals(lengthSeconds, other.lengthSeconds)
                && Objects.equals(lowFreqHz, other.lowFreqHz);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SyntheticGenerationRequest [distanceKm=")
               .append(distanceKm)
               .append(", lowFreqHz=")
               .append(lowFreqHz)
               .append(", highFreqHz=")
               .append(highFreqHz)
               .append(", lengthSeconds=")
               .append(lengthSeconds)
               .append("]");
        return builder.toString();
    }

}
