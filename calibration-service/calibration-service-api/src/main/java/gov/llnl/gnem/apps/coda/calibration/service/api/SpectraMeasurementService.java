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
package gov.llnl.gnem.apps.coda.calibration.service.api;

import java.util.List;
import java.util.Map;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurementMetadata;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

public interface SpectraMeasurementService {

    public List<SpectraMeasurement> measureSpectra(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            VelocityConfiguration velocityConfig);

    public List<SpectraMeasurement> measureSpectra(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            VelocityConfiguration velocityConfig, Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap);

    public SpectraMeasurement findOne(Long id);

    public SpectraMeasurement findOneForUpdate(Long id);

    public List<SpectraMeasurement> findAll(Iterable<Long> ids);

    public List<SpectraMeasurement> findAll();

    public List<SpectraMeasurementMetadata> findAllMetadataOnly();

    public List<SpectraMeasurementMetadata> findAllMetadataOnly(Iterable<Long> ids);

    public long count();

    public Spectra computeReferenceSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase);

    public Spectra computeValidationSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase);

    public List<Spectra> getFitSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase);

    public Spectra getSpecificSpectra(double moment, double apparentStress, double start, double stop, int count);

}
