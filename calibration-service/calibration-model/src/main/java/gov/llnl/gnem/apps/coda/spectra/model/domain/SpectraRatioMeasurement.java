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

import gov.llnl.gnem.apps.coda.common.model.domain.Event;

public class SpectraRatioMeasurement {

    private Event event;
    private Double value;
    private Double centerFrequency;

    public SpectraRatioMeasurement(Event event, Double value, Double centerFrequency) {
        this.event = event;
        this.value = value;
        this.centerFrequency = centerFrequency;
    }

    public Event getEvent() {
        return event;
    }

    public void setEventID(Event event) {
        this.event = event;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getCenterFrequency() {
        return centerFrequency;
    }

    public void setCenterFrequency(Double centerFrequency) {
        this.centerFrequency = centerFrequency;
    }

    @Override
    public int hashCode() {
        return Objects.hash(centerFrequency, event, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpectraRatioMeasurement)) {
            return false;
        }
        SpectraRatioMeasurement other = (SpectraRatioMeasurement) obj;
        return Objects.equals(centerFrequency, other.centerFrequency) && Objects.equals(event, other.event) && Objects.equals(value, other.value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectraRatioMeasurement [eventID=").append(event).append(", value=").append(value).append(", centerFrequency=").append(centerFrequency).append("]");
        return builder.toString();
    }

}
