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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.CalibrationCurveFitter;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.ShapeCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;

@Service
public class ShapeCalibrationServiceImpl implements ShapeCalibrationService {

    private static final Logger log = LoggerFactory.getLogger(ShapeCalibrationServiceImpl.class);
    private ShapeMeasurementService shapeMeasurementService;
    private ShapeCalculator shapeCalc;
    private WaveformService waveService;
    private AutopickingServiceImpl picker;

    @Autowired
    public ShapeCalibrationServiceImpl(ShapeMeasurementService shapeMeasurementService, ShapeCalculator shapeCalc, WaveformService waveService, AutopickingServiceImpl picker) {
        this.shapeMeasurementService = shapeMeasurementService;
        this.shapeCalc = shapeCalc;
        this.waveService = waveService;
        this.picker = picker;
    }

    @Override
    public Map<FrequencyBand, SharedFrequencyBandParameters> measureShapes(Collection<PeakVelocityMeasurement> velocityMeasurements,
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters, ShapeFitterConstraints constraints, boolean autoPickingEnabled) throws InterruptedException {
        if (frequencyBandParameters.isEmpty()) {
            // TODO: Propagate warning to the status API
            log.warn("No frequency band parameters available, unable to compute shape parameters without them!");
            return new HashMap<>();
        }
        final CalibrationCurveFitter fitter = new CalibrationCurveFitter();

        ConcurrencyUtils.checkInterrupt();
        Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandCurveFits = fitter.fitAllVelocity(
                velocityMeasurements.stream()
                                    .filter(vel -> vel.getWaveform() != null)
                                    .collect(Collectors.groupingBy(vel -> new FrequencyBand(vel.getWaveform().getLowFrequency(), vel.getWaveform().getHighFrequency()))),
                    frequencyBandParameters,
                    constraints);

        ConcurrencyUtils.checkInterrupt();
        // 1) If auto-picking is enabled attempt to pick any envelopes that
        // don't already have F-picks in this set
        if (autoPickingEnabled) {
            velocityMeasurements = picker.autoPickVelocityMeasuredWaveforms(velocityMeasurements, frequencyBandParameters);
            velocityMeasurements = velocityMeasurements.parallelStream().map(v -> v.setWaveform(waveService.save(v.getWaveform()))).collect(Collectors.toList());
        }
        ConcurrencyUtils.checkInterrupt();

        // 2) Filter to only measurements with an end pick
        Collection<Entry<PeakVelocityMeasurement, WaveformPick>> filteredVelocityMeasurements = filterMeasurementsToEndPickedOnly(velocityMeasurements);

        ConcurrencyUtils.checkInterrupt();
        // 3) For every Waveform remaining, measure picks based on start
        // (computed from velocity) and end (from 'End'/'F' picks)
        List<ShapeMeasurement> betaAndGammaMeasurements = shapeCalc.fitShapelineToMeasuredEnvelopes(filteredVelocityMeasurements, frequencyBandCurveFits, constraints);

        ConcurrencyUtils.checkInterrupt();
        
        // Shape measurements are intermediary results so rather than trying to
        // merge them we want to just drop them wholesale if they exist and
        // replace them with the new data set.
        // TODO: Need to only delete these for the current project
        shapeMeasurementService.deleteAll();

        Map<FrequencyBand, List<ShapeMeasurement>> frequencyBandShapeMeasurementMap = shapeMeasurementService.save(betaAndGammaMeasurements)
                                                                                                             .parallelStream()
                                                                                                             .filter(meas -> meas.getWaveform() != null)
                                                                                                             .collect(
                                                                                                                     Collectors.groupingBy(
                                                                                                                             meas -> new FrequencyBand(meas.getWaveform().getLowFrequency(),
                                                                                                                                                       meas.getWaveform().getHighFrequency())));
        ConcurrencyUtils.checkInterrupt();
        frequencyBandCurveFits = fitter.fitAllBeta(frequencyBandShapeMeasurementMap, frequencyBandCurveFits, constraints);
        ConcurrencyUtils.checkInterrupt();
        frequencyBandCurveFits = fitter.fitAllGamma(frequencyBandShapeMeasurementMap, frequencyBandCurveFits, constraints);
        return frequencyBandCurveFits;
    }

    /**
     * Down-select the given {@link PeakVelocityMeasurement} collection to only
     * ones with 'End' picks defined and associate them into an Entry
     */
    private Collection<Entry<PeakVelocityMeasurement, WaveformPick>> filterMeasurementsToEndPickedOnly(Collection<PeakVelocityMeasurement> velocityMeasurements) {
        return velocityMeasurements.parallelStream().filter(vel -> vel.getWaveform() != null).filter(vel -> vel.getWaveform().getAssociatedPicks() != null).map(vel -> {
            Optional<WaveformPick> pick = vel.getWaveform().getAssociatedPicks().stream().filter(p -> PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType())).findFirst();
            if (pick.isPresent()) {
                return new AbstractMap.SimpleEntry<>(vel, pick.get());
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
