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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;

public class SpectraRatiosReport {

    private Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> data;
    private Map<EventPair, SpectraRatioPairInversionResult> inversionEstimates;
    private Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionEstimates;

    public SpectraRatiosReport() {
        data = new HashMap<>(0);
        inversionEstimates = new HashMap<>(0);
        jointInversionEstimates = new HashMap<>(0);
    }

    public Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> getData() {
        return data;
    }

    public SpectraRatiosReport setData(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> data) {
        this.data = data;
        return this;
    }

    public SpectraRatiosReport setInversionEstimates(Map<EventPair, SpectraRatioPairInversionResult> inversionEstimates) {
        this.inversionEstimates = inversionEstimates;
        return this;
    }

    public Map<EventPair, SpectraRatioPairInversionResult> getInversionEstimates() {
        return inversionEstimates;
    }

    public Map<EventPair, SpectraRatioPairInversionResultJoint> getJointInversionEstimates() {
        return jointInversionEstimates;
    }

    public SpectraRatiosReport setJointInversionEstimates(Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionEstimates) {
        this.jointInversionEstimates = jointInversionEstimates;
        return this;
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("SpectraRatiosReport [data=")
               .append(data != null ? toString(data.entrySet(), maxLen) : null)
               .append(", inversionEstimates=")
               .append(inversionEstimates != null ? toString(inversionEstimates.entrySet(), maxLen) : null)
               .append(", jointInversionEstimates=")
               .append(jointInversionEstimates != null ? toString(jointInversionEstimates.entrySet(), maxLen) : null)
               .append("]");
        return builder.toString();
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(data, inversionEstimates, jointInversionEstimates);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpectraRatiosReport)) {
            return false;
        }
        SpectraRatiosReport other = (SpectraRatiosReport) obj;
        return Objects.equals(data, other.data) && Objects.equals(inversionEstimates, other.inversionEstimates) && Objects.equals(jointInversionEstimates, other.jointInversionEstimates);
    }

}