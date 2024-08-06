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

import java.util.List;
import java.util.Objects;

import gov.llnl.gnem.apps.coda.common.model.domain.Pair;

public class EventSpectraReport {

    private String eventId;
    private String network;
    private String stationName;
    private List<Pair<Double, Double>> xyVals;

    public EventSpectraReport(String eventId, String network, String stationName, List<Pair<Double, Double>> xyVals) {
        this.eventId = eventId;
        this.network = network;
        this.stationName = stationName;
        this.xyVals = xyVals;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getNetwork() {
        return network;
    }

    public void setNetwork(String network) {
        this.network = network;
    }

    public String getStationName() {
        return stationName;
    }

    public void setStationName(String stationName) {
        this.stationName = stationName;
    }

    public List<Pair<Double, Double>> getXyVals() {
        return xyVals;
    }

    public void setXyVals(List<Pair<Double, Double>> xyVals) {
        this.xyVals = xyVals;
    }

    public void add(Pair<Double, Double> pair) {
        this.xyVals.add(pair);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventId, network, stationName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EventSpectraReport)) {
            return false;
        }
        EventSpectraReport other = (EventSpectraReport) obj;
        return Objects.equals(eventId, other.eventId) && Objects.equals(network, other.network) && Objects.equals(stationName, other.stationName);
    }

}
