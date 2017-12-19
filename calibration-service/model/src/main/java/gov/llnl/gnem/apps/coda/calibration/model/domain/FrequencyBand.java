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

import java.io.Serializable;

import javax.persistence.Embeddable;

@Embeddable
public class FrequencyBand implements Comparable<FrequencyBand>, Serializable {

    private static final long serialVersionUID = 1L;

    private double lowFrequency;
    private double highFrequency;

    public FrequencyBand() {
        super();
    }

    public double getLowFrequency() {
        return lowFrequency;
    }

    public FrequencyBand setLowFrequency(double lowFrequency) {
        this.lowFrequency = lowFrequency;
        return this;
    }

    public double getHighFrequency() {
        return highFrequency;
    }

    public FrequencyBand setHighFrequency(double highFrequency) {
        this.highFrequency = highFrequency;
        return this;
    }

    public FrequencyBand(double lowFrequency, double highFrequency) {
        super();
        this.lowFrequency = lowFrequency;
        this.highFrequency = highFrequency;
    }

    @Override
    public String toString() {
        return "FrequencyBand [lowFrequency=" + lowFrequency + ", highFrequency=" + highFrequency + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(highFrequency);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lowFrequency);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FrequencyBand other = (FrequencyBand) obj;
        if (Double.doubleToLongBits(highFrequency) != Double.doubleToLongBits(other.highFrequency)) {
            return false;
        }
        if (Double.doubleToLongBits(lowFrequency) != Double.doubleToLongBits(other.lowFrequency)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(FrequencyBand obj) {
        if (obj == null) {
            return -1;
        }
        int lowFreq = Double.compare(lowFrequency, obj.getLowFrequency());
        return lowFreq == 0 ? Double.compare(highFrequency, obj.getHighFrequency()) : lowFreq;
    }

}
