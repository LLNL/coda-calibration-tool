/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MeasuredMwReportByEvent {
    private Map<String, MeasuredMwDetails> measuredMwDetails;
    private Map<String, List<Spectra>> fitSpectra;
    private Map<String, List<SpectraMeasurementMetadata>> spectraMeasurements;

    public MeasuredMwReportByEvent(Map<String, MeasuredMwDetails> measuredMwDetails, Map<String, List<Spectra>> fitSpectra, Map<String, List<SpectraMeasurementMetadata>> spectraMeasurements) {
        this.measuredMwDetails = measuredMwDetails;
        this.fitSpectra = fitSpectra;
        this.spectraMeasurements = spectraMeasurements;
    }

    public MeasuredMwReportByEvent() {
        measuredMwDetails = new HashMap<>();
        fitSpectra = new HashMap<>();
        spectraMeasurements = new HashMap<>();
    }

    public Map<String, MeasuredMwDetails> getMeasuredMwDetails() {
        return measuredMwDetails;
    }

    public MeasuredMwReportByEvent setMeasuredMwDetails(Map<String, MeasuredMwDetails> measuredMwDetails) {
        this.measuredMwDetails = measuredMwDetails;
        return this;
    }

    public Map<String, List<Spectra>> getFitSpectra() {
        return fitSpectra;
    }

    public MeasuredMwReportByEvent setFitSpectra(Map<String, List<Spectra>> fitSpectra) {
        this.fitSpectra = fitSpectra;
        return this;
    }

    public Map<String, List<SpectraMeasurementMetadata>> getSpectraMeasurements() {
        return spectraMeasurements;
    }

    public MeasuredMwReportByEvent setSpectraMeasurements(Map<String, List<SpectraMeasurementMetadata>> spectraMeasurements) {
        this.spectraMeasurements = spectraMeasurements;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MeasuredMwReportByEvent [measuredMwDetails=")
               .append(measuredMwDetails)
               .append(", fitSpectra=")
               .append(fitSpectra)
               .append(", SpectraMeasurementMetadatas=")
               .append(spectraMeasurements)
               .append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(fitSpectra, measuredMwDetails, spectraMeasurements);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MeasuredMwReportByEvent)) {
            return false;
        }
        MeasuredMwReportByEvent other = (MeasuredMwReportByEvent) obj;
        return Objects.equals(fitSpectra, other.fitSpectra) && Objects.equals(measuredMwDetails, other.measuredMwDetails) && Objects.equals(spectraMeasurements, other.spectraMeasurements);
    }
}
