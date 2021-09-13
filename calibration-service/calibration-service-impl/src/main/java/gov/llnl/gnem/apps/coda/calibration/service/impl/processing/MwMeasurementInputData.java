/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;

public class MwMeasurementInputData {
    private Map<Event, Map<FrequencyBand, SummaryStatistics>> evidMap;
    private Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights;
    private MdacParametersPS mdacPs;
    private Map<String, Integer> stationCount;
    private Map<String, Double> bandCoverageMetric;

    public MwMeasurementInputData(Map<Event, Map<FrequencyBand, SummaryStatistics>> evidMap, Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights, MdacParametersPS mdacPs,
            Map<String, Integer> stationCount, Map<String, Double> bandCoverageMetric) {
        this.evidMap = evidMap;
        this.eventWeights = eventWeights;
        this.mdacPs = mdacPs;
        this.stationCount = stationCount;
        this.bandCoverageMetric = bandCoverageMetric;
    }

    public Map<Event, Map<FrequencyBand, SummaryStatistics>> getEvidMap() {
        return evidMap;
    }

    public Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> getEventWeights() {
        return eventWeights;
    }

    public void setEventWeights(Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights) {
        this.eventWeights = eventWeights;
    }

    public MdacParametersPS getMdacPs() {
        return mdacPs;
    }

    public void setMdacPs(MdacParametersPS mdacPs) {
        this.mdacPs = mdacPs;
    }

    public Map<String, Integer> getStationCount() {
        return stationCount;
    }

    public void setStationCount(Map<String, Integer> stationCount) {
        this.stationCount = stationCount;
    }

    public Map<String, Double> getBandCoverageMetric() {
        return bandCoverageMetric;
    }

    public void setBandCoverageMetric(Map<String, Double> bandCoverageMetric) {
        this.bandCoverageMetric = bandCoverageMetric;
    }
}