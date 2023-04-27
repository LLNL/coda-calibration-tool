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
package gov.llnl.gnem.apps.coda.spectra.model.domain;

import java.util.Objects;

public class MomentCornerEstimate {

    private String eventId;
    private double moment;
    private double cornerFreq;

    public MomentCornerEstimate(String eventId, double moment, double cornerFreq) {
        this.eventId = eventId;
        this.moment = moment;
        this.cornerFreq = cornerFreq;
    }

    public double getMoment() {
        return moment;
    }

    public void setMoment(double moment) {
        this.moment = moment;
    }

    public double getCornerFreq() {
        return cornerFreq;
    }

    public void setCornerFreq(double cornerFreq) {
        this.cornerFreq = cornerFreq;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cornerFreq, eventId, moment);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MomentCornerEstimate)) {
            return false;
        }
        MomentCornerEstimate other = (MomentCornerEstimate) obj;
        return Double.doubleToLongBits(cornerFreq) == Double.doubleToLongBits(other.cornerFreq)
                && Objects.equals(eventId, other.eventId)
                && Double.doubleToLongBits(moment) == Double.doubleToLongBits(other.moment);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MomentCornerEstimate [eventId=").append(eventId).append(", moment=").append(moment).append(", cornerFreq=").append(cornerFreq).append("]");
        return builder.toString();
    }
}
