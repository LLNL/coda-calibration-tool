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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.repository.MeasuredMwsRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.ReferenceMwParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.SpectraMeasurementRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

@Service
@Transactional
public class SpectraMeasurementServiceImpl implements SpectraMeasurementService {

    private SpectraCalculator spectraCalc;

    private SpectraMeasurementRepository spectraRepo;

    private ReferenceMwParametersRepository referenceEventRepo;
    
    private MeasuredMwsRepository measuredEventRepo;

    @Autowired
    public SpectraMeasurementServiceImpl(SpectraMeasurementRepository spectraRepo, SpectraCalculator spectraCalc,MeasuredMwsRepository measuredEventRepo, ReferenceMwParametersRepository referenceEventRepo) {
        this.spectraRepo = spectraRepo;
        this.spectraCalc = spectraCalc;
        this.measuredEventRepo = measuredEventRepo;
        this.referenceEventRepo = referenceEventRepo;
    }

    @Override
    public SpectraMeasurement findOne(Long id) {
        return spectraRepo.findOneDetached(id);
    }

    @Override
    public SpectraMeasurement findOneForUpdate(Long id) {
        return spectraRepo.findOneDetached(id);
    }

    @Override
    public List<SpectraMeasurement> findAll(Iterable<Long> ids) {
        return spectraRepo.findAllById(ids);
    }

    @Override
    public List<SpectraMeasurement> findAll() {
        return spectraRepo.findAll();
    }

    @Override
    public long count() {
        return spectraRepo.count();
    }

    @Override
    public List<SpectraMeasurement> measureSpectra(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap, Boolean autoPickingEnabled) {
        List<SpectraMeasurement> measurements = spectraCalc.measureAmplitudes(generatedSynthetics, frequencyBandParameterMap, autoPickingEnabled);
        if (!measurements.isEmpty()) {
            spectraRepo.deleteAllInBatch();
            measurements = spectraRepo.saveAll(measurements);
        }
        return measurements;
    }

    @Override
    public List<SpectraMeasurement> measureSpectra(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap, Boolean autoPickingEnabled,
            Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap) {
        List<SpectraMeasurement> measurements = spectraCalc.measureAmplitudes(generatedSynthetics, frequencyBandParameterMap, autoPickingEnabled, frequencyBandSiteParameterMap);
        if (!measurements.isEmpty()) {
            spectraRepo.deleteAllInBatch();
            measurements = spectraRepo.saveAll(measurements);
        }
        return measurements;
    }

    @Override
    public Spectra computeSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase) {
        Spectra refSpectra = new Spectra();
        ReferenceMwParameters refEvent = referenceEventRepo.findOneByEventId(eventId);
        if (refEvent != null) {
            refSpectra = spectraCalc.computeReferenceSpectra(refEvent, frequencyBands, selectedPhase);
        }
        return refSpectra;
    }

    @Override
    public Spectra getFitSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase) {
            Spectra fitSpectra = new Spectra();
            MeasuredMwParameters event = measuredEventRepo.findOneByEventId(eventId);
            if (event != null) {
                fitSpectra = spectraCalc.computeFitSpectra(event, frequencyBands, selectedPhase);
            }
            return fitSpectra;
        }
}
