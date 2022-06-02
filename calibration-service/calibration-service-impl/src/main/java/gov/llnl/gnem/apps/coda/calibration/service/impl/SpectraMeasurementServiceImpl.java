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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurementMetadata;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.repository.MeasuredMwsRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.ReferenceMwParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.SpectraMeasurementRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.ValidationMwParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.EnergyInfo;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;

@Service
@Transactional
public class SpectraMeasurementServiceImpl implements SpectraMeasurementService {

    private SpectraCalculator spectraCalc;

    private SpectraMeasurementRepository spectraRepo;

    private ReferenceMwParametersRepository referenceEventRepo;

    private ValidationMwParametersRepository validationEventRepo;

    private MeasuredMwsRepository measuredEventRepo;

    @Value("${show-stress-bounds-in-uq-spectra:false}")
    private boolean showStressBoundsInUQSpectra = false;

    @Autowired
    public SpectraMeasurementServiceImpl(SpectraMeasurementRepository spectraRepo, SpectraCalculator spectraCalc, MeasuredMwsRepository measuredEventRepo,
            ReferenceMwParametersRepository referenceEventRepo, ValidationMwParametersRepository validationEventRepo) {
        this.spectraRepo = spectraRepo;
        this.spectraCalc = spectraCalc;
        this.measuredEventRepo = measuredEventRepo;
        this.referenceEventRepo = referenceEventRepo;
        this.validationEventRepo = validationEventRepo;
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
    public List<SpectraMeasurementMetadata> findAllMetadataOnly(Iterable<Long> ids) {
        return spectraRepo.findAllMetadataById(ids);
    }

    @Override
    public List<SpectraMeasurement> findAll() {
        return spectraRepo.findAll();
    }

    @Override
    public List<SpectraMeasurementMetadata> findAllMetadataOnly() {
        return spectraRepo.findAllMetadataOnly();
    }

    @Override
    public long count() {
        return spectraRepo.count();
    }

    @Override
    public List<SpectraMeasurement> measureSpectra(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            VelocityConfiguration velocityConfig) {
        List<SpectraMeasurement> measurements = spectraCalc.measureAmplitudes(generatedSynthetics, frequencyBandParameterMap, velocityConfig);
        if (!measurements.isEmpty()) {
            spectraRepo.deleteAllInBatch();
            measurements = spectraRepo.saveAll(measurements);
        }
        return measurements;
    }

    @Override
    public List<SpectraMeasurement> measureSpectra(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            VelocityConfiguration velocityConfig, Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap) {
        List<SpectraMeasurement> measurements = spectraCalc.measureAmplitudes(generatedSynthetics, frequencyBandParameterMap, velocityConfig, frequencyBandSiteParameterMap);
        if (!measurements.isEmpty()) {
            spectraRepo.deleteAllInBatch();
            measurements = spectraRepo.saveAll(measurements);
        }
        return measurements;
    }

    @Override
    public Spectra computeReferenceSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase) {
        Spectra refSpectra = new Spectra();
        ReferenceMwParameters refEvent = referenceEventRepo.findOneByEventId(eventId);
        if (refEvent != null) {
            refSpectra = spectraCalc.computeReferenceSpectra(refEvent, frequencyBands, selectedPhase);
        }
        return refSpectra;
    }

    @Override
    public Spectra computeValidationSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase) {
        Spectra valSpectra = new Spectra();
        ValidationMwParameters valEvent = validationEventRepo.findOneByEventId(eventId);
        if (valEvent != null) {
            valSpectra = spectraCalc.computeSpecificSpectra(valEvent.getMw(), valEvent.getApparentStressInMpa(), null, frequencyBands, selectedPhase, SPECTRA_TYPES.VAL);
        }
        return valSpectra;
    }

    @Override
    public List<Spectra> getFitSpectraForEventId(String eventId, List<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase) {
        List<Spectra> spectra = new ArrayList<>();
        MeasuredMwParameters event = measuredEventRepo.findOneByEventId(eventId);
        if (event != null) {
            EnergyInfo eInfo = new EnergyInfo(event.getObsEnergy(), event.getLogTotalEnergy(), event.getLogTotalEnergyMDAC(), event.getEnergyRatio(), event.getObsAppStress());
            spectra.add(spectraCalc.computeFitSpectra(event, frequencyBands, selectedPhase));
            if (showStressBoundsInUQSpectra) {
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw1Max(), event.getApparentStress1Max(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ1));
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw1Min(), event.getApparentStress1Min(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ1));
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw2Max(), event.getApparentStress2Max(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ2));
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw2Min(), event.getApparentStress2Min(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ2));
            } else {
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw1Max(), event.getApparentStressInMpa(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ1));
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw1Min(), event.getApparentStressInMpa(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ1));
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw2Max(), event.getApparentStressInMpa(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ2));
                spectra.add(spectraCalc.computeSpecificSpectra(event.getMw2Min(), event.getApparentStressInMpa(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ2));
            }
        }
        return spectra;
    }
}
