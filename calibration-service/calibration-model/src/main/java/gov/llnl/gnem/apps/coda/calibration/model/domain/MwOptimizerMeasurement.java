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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.util.Objects;

public class MwOptimizerMeasurement {

    private final Double fit;
    private final Double mw;
    private final Double stress;
    private final Double cornerFreq;

    public MwOptimizerMeasurement(Double fit, Double mw, Double stress, Double cornerFreq) {
        this.fit = fit;
        this.mw = mw;
        this.stress = stress;
        this.cornerFreq = cornerFreq;
    }

    public Double getFit() {
        return fit;
    }

    public Double getMw() {
        return mw;
    }

    public Double getStress() {
        return stress;
    }

    public Double getCornerFreq() {
        return cornerFreq;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cornerFreq, fit, mw, stress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MwOptimizerMeasurement)) {
            return false;
        }
        MwOptimizerMeasurement other = (MwOptimizerMeasurement) obj;
        return Objects.equals(cornerFreq, other.cornerFreq) && Objects.equals(fit, other.fit) && Objects.equals(mw, other.mw) && Objects.equals(stress, other.stress);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MwOptimizerMeasurement [fit=").append(fit).append(", mw=").append(mw).append(", stress=").append(stress).append(", cornerFreq=").append(cornerFreq).append("]");
        return builder.toString();
    }

}
