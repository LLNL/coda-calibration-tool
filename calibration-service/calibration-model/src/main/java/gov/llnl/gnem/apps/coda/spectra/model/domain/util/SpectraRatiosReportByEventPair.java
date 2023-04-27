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
package gov.llnl.gnem.apps.coda.spectra.model.domain.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResult;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResultJoint;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatiosReport;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;

public class SpectraRatiosReportByEventPair {

    private SpectraRatiosReport report;

    public SpectraRatiosReportByEventPair() {
        report = new SpectraRatiosReport();
    }

    public SpectraRatiosReportByEventPair(SpectraRatiosReport report) {
        this.report = report;
    }

    public List<EventPair> getEventPairs() {
        List<EventPair> eventPairs = new ArrayList<>(0);
        if (!report.getData().isEmpty()) {
            eventPairs.addAll(report.getData().keySet());
        }
        return eventPairs;
    }

    public SpectraRatiosReportByEventPair setInversionResults(Map<EventPair, SpectraRatioPairInversionResult> inversionEstimates) {
        report.setInversionEstimates(inversionEstimates);
        return this;
    }

    public SpectraRatiosReportByEventPair setJointInversionResults(Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionEstimates) {
        report.setJointInversionEstimates(jointInversionEstimates);
        return this;
    }

    public Map<EventPair, SpectraRatioPairInversionResult> getInversionResults() {
        return report.getInversionEstimates();
    }

    public SpectraRatiosReportByEventPair setRatiosReportByEventPair(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData) {
        report.setData(ratioData);
        return this;
    }

    public List<Station> getStationsForEventPair(EventPair eventPair) {
        List<Station> stations = new ArrayList<>(0);
        Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> data = report.getData().get(eventPair);
        if (data != null) {
            stations.addAll(data.keySet());
        }
        return stations;
    }

    public SpectraRatiosReport getReport() {
        return report;
    }

    public List<SpectraRatioPairDetails> getRatiosList(EventPair eventPair) {
        List<SpectraRatioPairDetails> data = new ArrayList<>(0);
        if (report.getData().containsKey(eventPair)) {
            data.addAll(report.getData().get(eventPair).values().stream().flatMap(fbv -> fbv.values().stream()).collect(Collectors.toList()));
        }
        return data;
    }

}
