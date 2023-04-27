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
package gov.llnl.gnem.apps.coda.calibration.model.messaging;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;

public class RatioMeasurementEvent {

    private Long id;
    private Result<SpectraRatiosReportByEventPair> ratioMeasurements;

    public RatioMeasurementEvent(Long id) {
        this.id = id;
        this.ratioMeasurements = null;
    }

    @JsonCreator
    public RatioMeasurementEvent(@JsonProperty("id") Long id, @JsonProperty("measurements") Result<SpectraRatiosReportByEventPair> ratioMeasurements) {
        this.id = id;
        this.ratioMeasurements = ratioMeasurements;
    }

    public Long getId() {
        return id;
    }

    public RatioMeasurementEvent setId(Long id) {
        this.id = id;
        return this;
    }

    public SpectraRatiosReportByEventPair getRatioMeasurements() {
        return ratioMeasurements.getResultPayload().get();
    }

    public RatioMeasurementEvent setStatus(Result<SpectraRatiosReportByEventPair> ratioMeasurements) {
        this.ratioMeasurements = ratioMeasurements;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RatioMeasurementEvent [id=");
        builder.append(id);
        builder.append(", ratioMeasurements=");
        builder.append(ratioMeasurements);
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ratioMeasurements);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof RatioMeasurementEvent)) {
            return false;
        }
        RatioMeasurementEvent other = (RatioMeasurementEvent) obj;
        return Objects.equals(id, other.id) && Objects.equals(ratioMeasurements, other.ratioMeasurements);
    }
}
