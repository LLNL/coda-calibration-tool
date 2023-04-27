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
package gov.llnl.gnem.apps.coda.spectra.model.domain.messaging;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResult;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResultJoint;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatiosReport;

public class SpectraRatiosReportDTO {

    private Map<Integer, Map<Integer, Map<Integer, SpectraRatioPairDetails>>> data;
    private Map<Integer, EventPair> eventMap;
    private Map<Integer, Station> stationMap;
    private Map<Integer, FrequencyBand> bandMap;
    private Map<Integer, SpectraRatioPairInversionResult> inversionEstimates;
    private Map<Integer, SpectraRatioPairInversionResultJoint> jointInversionEstimates;

    public SpectraRatiosReportDTO() {
        data = new HashMap<>();
        inversionEstimates = new HashMap<>();
        jointInversionEstimates = new HashMap<>();
        eventMap = new HashMap<>();
        stationMap = new HashMap<>();
        bandMap = new HashMap<>();
    }

    public SpectraRatiosReportDTO(SpectraRatiosReport report) {
        this();
        for (Entry<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> events : report.getData().entrySet()) {
            Integer eventKey = events.getKey().hashCode();
            eventMap.put(eventKey, events.getKey());
            for (Entry<Station, Map<FrequencyBand, SpectraRatioPairDetails>> station : events.getValue().entrySet()) {
                Integer stationKey = station.getKey().hashCode();
                stationMap.put(stationKey, station.getKey());
                for (Entry<FrequencyBand, SpectraRatioPairDetails> bands : station.getValue().entrySet()) {
                    Integer bandKey = bands.getKey().hashCode();
                    bandMap.put(bandKey, bands.getKey());
                    data.computeIfAbsent(eventKey, k -> new HashMap<>())
                        .computeIfAbsent(stationKey, k -> new HashMap<>())
                        .put(bandKey, report.getData().get(events.getKey()).get(station.getKey()).get(bands.getKey()));
                }
            }
        }
        for (Entry<EventPair, SpectraRatioPairInversionResult> events : report.getInversionEstimates().entrySet()) {
            Integer eventKey = events.getKey().hashCode();
            eventMap.put(eventKey, events.getKey());
            inversionEstimates.put(eventKey, events.getValue());
        }
        for (Entry<EventPair, SpectraRatioPairInversionResultJoint> events : report.getJointInversionEstimates().entrySet()) {
            Integer eventKey = events.getKey().hashCode();
            eventMap.put(eventKey, events.getKey());
            jointInversionEstimates.put(eventKey, events.getValue());
        }
    }

    @JsonIgnore
    public SpectraRatiosReport getReport() {
        Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> reportData = new HashMap<>();
        Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversion = new HashMap<>();
        Map<EventPair, SpectraRatioPairInversionResult> inversion = new HashMap<>();
        for (Entry<Integer, Map<Integer, Map<Integer, SpectraRatioPairDetails>>> events : data.entrySet()) {
            EventPair eventKey = eventMap.get(events.getKey());
            inversion.put(eventKey, inversionEstimates.get(events.getKey()));
            jointInversion.put(eventKey, jointInversionEstimates.get(events.getKey()));
            for (Entry<Integer, Map<Integer, SpectraRatioPairDetails>> station : events.getValue().entrySet()) {
                Station stationKey = stationMap.get(station.getKey());
                for (Entry<Integer, SpectraRatioPairDetails> bands : station.getValue().entrySet()) {
                    FrequencyBand bandKey = bandMap.get(bands.getKey());

                    reportData.computeIfAbsent(eventKey, k -> new HashMap<>())
                              .computeIfAbsent(stationKey, k -> new HashMap<>())
                              .put(bandKey, data.get(events.getKey()).get(station.getKey()).get(bands.getKey()));
                }
            }
        }
        return new SpectraRatiosReport().setData(reportData).setJointInversionEstimates(jointInversion).setInversionEstimates(inversion);
    }

    public Map<Integer, Map<Integer, Map<Integer, SpectraRatioPairDetails>>> getData() {
        return data;
    }

    public void setData(Map<Integer, Map<Integer, Map<Integer, SpectraRatioPairDetails>>> data) {
        this.data = data;
    }

    public Map<Integer, EventPair> getEventMap() {
        return eventMap;
    }

    public void setEventMap(Map<Integer, EventPair> eventMap) {
        this.eventMap = eventMap;
    }

    public Map<Integer, Station> getStationMap() {
        return stationMap;
    }

    public void setStationMap(Map<Integer, Station> stationMap) {
        this.stationMap = stationMap;
    }

    public Map<Integer, FrequencyBand> getBandMap() {
        return bandMap;
    }

    public void setBandMap(Map<Integer, FrequencyBand> bandMap) {
        this.bandMap = bandMap;
    }

    public Map<Integer, SpectraRatioPairInversionResult> getInversionEstimates() {
        return inversionEstimates;
    }

    public void setInversionEstimates(Map<Integer, SpectraRatioPairInversionResult> inversionEstimates) {
        this.inversionEstimates = inversionEstimates;
    }

    public Map<Integer, SpectraRatioPairInversionResultJoint> getJointInversionEstimates() {
        return jointInversionEstimates;
    }

    public void setJointInversionEstimates(Map<Integer, SpectraRatioPairInversionResultJoint> jointInversionEstimates) {
        this.jointInversionEstimates = jointInversionEstimates;
    }

    @Override
    public int hashCode() {
        return Objects.hash(bandMap, data, eventMap, inversionEstimates, jointInversionEstimates, stationMap);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpectraRatiosReportDTO)) {
            return false;
        }
        SpectraRatiosReportDTO other = (SpectraRatiosReportDTO) obj;
        return Objects.equals(bandMap, other.bandMap)
                && Objects.equals(data, other.data)
                && Objects.equals(eventMap, other.eventMap)
                && Objects.equals(inversionEstimates, other.inversionEstimates)
                && Objects.equals(jointInversionEstimates, other.jointInversionEstimates)
                && Objects.equals(stationMap, other.stationMap);
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("SpectraRatiosReportDTO [data=")
               .append(data != null ? toString(data.entrySet(), maxLen) : null)
               .append(", eventMap=")
               .append(eventMap != null ? toString(eventMap.entrySet(), maxLen) : null)
               .append(", stationMap=")
               .append(stationMap != null ? toString(stationMap.entrySet(), maxLen) : null)
               .append(", bandMap=")
               .append(bandMap != null ? toString(bandMap.entrySet(), maxLen) : null)
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

}