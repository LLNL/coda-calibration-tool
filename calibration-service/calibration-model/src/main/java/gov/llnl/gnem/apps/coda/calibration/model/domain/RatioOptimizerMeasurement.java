/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.util.Objects;

public class RatioOptimizerMeasurement {

    private final float fit;
    private final Double momentA;
    private final Double stressA;
    private final Double cornerFreqA;
    private final Double momentB;
    private final Double stressB;
    private final Double cornerFreqB;

    public RatioOptimizerMeasurement(float sum, Double momentA, Double stressA, Double cornerFreqA, Double momentB, Double stressB, Double cornerFreqB) {
        this.fit = sum;
        this.momentA = momentA;
        this.stressA = stressA;
        this.cornerFreqA = cornerFreqA;
        this.momentB = momentB;
        this.stressB = stressB;
        this.cornerFreqB = cornerFreqB;
    }

    public float getFit() {
        return fit;
    }

    public Double getMomentA() {
        return momentA;
    }

    public Double getStressA() {
        return stressA;
    }

    public Double getCornerFreqA() {
        return cornerFreqA;
    }

    public Double getMomentB() {
        return momentB;
    }

    public Double getStressB() {
        return stressB;
    }

    public Double getCornerFreqB() {
        return cornerFreqB;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cornerFreqA, cornerFreqB, fit, momentA, momentB, stressA, stressB);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RatioOptimizerMeasurement)) {
            return false;
        }
        RatioOptimizerMeasurement other = (RatioOptimizerMeasurement) obj;
        return Objects.equals(cornerFreqA, other.cornerFreqA)
                && Objects.equals(cornerFreqB, other.cornerFreqB)
                && Objects.equals(fit, other.fit)
                && Objects.equals(momentA, other.momentA)
                && Objects.equals(momentB, other.momentB)
                && Objects.equals(stressA, other.stressA)
                && Objects.equals(stressB, other.stressB);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RatioOptimizerMeasurement [fit=")
               .append(fit)
               .append(", momentA=")
               .append(momentA)
               .append(", stressA=")
               .append(stressA)
               .append(", cornerFreqA=")
               .append(cornerFreqA)
               .append(", momentB=")
               .append(momentB)
               .append(", stressB=")
               .append(stressB)
               .append(", cornerFreqB=")
               .append(cornerFreqB)
               .append("]");
        return builder.toString();
    }

}
