/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

public class EnvelopeFit {

    private Double gamma;
    private Double beta;
    private Double intercept;
    private Double error;

    public Double getGamma() {
        return gamma;
    }

    public EnvelopeFit setGamma(Double gamma) {
        this.gamma = gamma;
        return this;
    }

    public Double getBeta() {
        return beta;
    }

    public EnvelopeFit setBeta(Double beta) {
        this.beta = beta;
        return this;
    }

    public Double getIntercept() {
        return intercept;
    }

    public EnvelopeFit setIntercept(Double intercept) {
        this.intercept = intercept;
        return this;
    }

    public Double getError() {
        return error;
    }

    public EnvelopeFit setError(Double error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return "EnvelopeFit [gamma=" + gamma + ", beta=" + beta + ", intercept=" + intercept + ", error=" + error + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((beta == null) ? 0 : beta.hashCode());
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        result = prime * result + ((gamma == null) ? 0 : gamma.hashCode());
        result = prime * result + ((intercept == null) ? 0 : intercept.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        EnvelopeFit other = (EnvelopeFit) obj;
        if (beta == null) {
            if (other.beta != null)
                return false;
        } else if (!beta.equals(other.beta))
            return false;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        if (gamma == null) {
            if (other.gamma != null)
                return false;
        } else if (!gamma.equals(other.gamma))
            return false;
        if (intercept == null) {
            if (other.intercept != null)
                return false;
        } else if (!intercept.equals(other.intercept))
            return false;
        return true;
    }

}
