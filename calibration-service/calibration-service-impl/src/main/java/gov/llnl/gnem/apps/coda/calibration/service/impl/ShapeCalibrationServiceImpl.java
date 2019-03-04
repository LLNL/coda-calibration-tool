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

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.service.api.EndTimePicker;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.CalibrationCurveFitter;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.ShapeCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformPickService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
public class ShapeCalibrationServiceImpl implements ShapeCalibrationService {

    private static final Logger log = LoggerFactory.getLogger(ShapeCalibrationServiceImpl.class);

    private static final double BAD = 0d;

    private ShapeMeasurementService shapeMeasurementService;
    private ShapeCalculator shapeCalc;
    private EndTimePicker endTimePicker;
    private WaveformToTimeSeriesConverter converter;
    private WaveformService waveService;
    private WaveformPickService pickService;

    @Autowired
    public ShapeCalibrationServiceImpl(ShapeMeasurementService shapeMeasurementService, ShapeCalculator shapeCalc, EndTimePicker endTimePicker, WaveformService waveService,
            WaveformPickService pickService) {
        this.shapeMeasurementService = shapeMeasurementService;
        this.shapeCalc = shapeCalc;
        this.endTimePicker = endTimePicker;
        this.converter = new WaveformToTimeSeriesConverter();
        this.waveService = waveService;
        this.pickService = pickService;
    }

    @Override
    public Map<FrequencyBand, SharedFrequencyBandParameters> measureShapes(Collection<PeakVelocityMeasurement> velocityMeasurements,
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters, boolean autoPickingEnabled) {
        if (frequencyBandParameters.isEmpty()) {
            // TODO: Propagate warning to the status API
            log.warn("No frequency band parameters available, unable to compute shape parameters without them!");
            return new HashMap<>();
        }
        final CalibrationCurveFitter fitter = new CalibrationCurveFitter();

        Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandCurveFits = fitter.fitAllVelocity(
                velocityMeasurements.stream()
                                    .filter(vel -> vel.getWaveform() != null)
                                    .collect(Collectors.groupingBy(vel -> new FrequencyBand(vel.getWaveform().getLowFrequency(), vel.getWaveform().getHighFrequency()))),
                    frequencyBandParameters);

        // 1) If auto-picking is enabled attempt to pick any envelopes that
        // don't already have F-picks in this set
        if (autoPickingEnabled) {
            velocityMeasurements = autoPickWaveforms(velocityMeasurements, frequencyBandParameters);
            velocityMeasurements.forEach(v -> pickService.save(waveService.save(v.getWaveform()).getAssociatedPicks()));
        }

        // 2) Filter to only measurements with an end pick
        Collection<Entry<PeakVelocityMeasurement, WaveformPick>> filteredVelocityMeasurements = filterMeasurementsToEndPickedOnly(velocityMeasurements);

        // 3) For every Waveform remaining, measure picks based on start
        // (computed from velocity) and end (from 'End'/'F' picks)
        List<ShapeMeasurement> betaAndGammaMeasurements = shapeCalc.fitShapelineToMeasuredEnvelopes(filteredVelocityMeasurements, frequencyBandCurveFits);

        // Shape measurements are intermediary results so rather than trying to
        // merge them we want to just drop them wholesale if they exist and
        // replace them with the new data set.
        // TODO: Need to only delete these for the current project
        shapeMeasurementService.deleteAll();

        Map<FrequencyBand, List<ShapeMeasurement>> frequencyBandShapeMeasurementMap = shapeMeasurementService.save(betaAndGammaMeasurements)
                                                                                                             .stream()
                                                                                                             .filter(meas -> meas.getWaveform() != null)
                                                                                                             .collect(
                                                                                                                     Collectors.groupingBy(
                                                                                                                             meas -> new FrequencyBand(meas.getWaveform().getLowFrequency(),
                                                                                                                                                       meas.getWaveform().getHighFrequency())));

        frequencyBandCurveFits = fitter.fitAllBeta(frequencyBandShapeMeasurementMap, frequencyBandCurveFits);
        frequencyBandCurveFits = fitter.fitAllGamma(frequencyBandShapeMeasurementMap, frequencyBandCurveFits);
        return frequencyBandCurveFits;
    }

    private Collection<PeakVelocityMeasurement> autoPickWaveforms(final Collection<PeakVelocityMeasurement> velocityMeasurements,
            final Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters) {
        return velocityMeasurements.parallelStream().filter(vel -> vel.getWaveform() != null).filter(vel -> vel.getWaveform().getAssociatedPicks() != null).map(vel -> {
            SharedFrequencyBandParameters params = frequencyBandParameters.get(new FrequencyBand(vel.getWaveform().getLowFrequency(), vel.getWaveform().getHighFrequency()));
            Optional<WaveformPick> pick = vel.getWaveform()
                                             .getAssociatedPicks()
                                             .stream()
                                             .filter(p -> p.getPickType() != null && PICK_TYPES.AP.name().equalsIgnoreCase(p.getPickType().trim()))
                                             .findFirst();

            Optional<WaveformPick> endPick = vel.getWaveform()
                                                .getAssociatedPicks()
                                                .stream()
                                                .filter(p -> p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim()))
                                                .findFirst();

            if ((!endPick.isPresent() || pick.isPresent()) && params != null) {
                vel.getWaveform().getAssociatedPicks().forEach(p -> p.setWaveform(null));
                vel.getWaveform().getAssociatedPicks().clear();

                double minlength = params.getMinLength();
                double maxlength = params.getMaxLength();

                double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + vel.getDistance());
                if (vr == 0.0) {
                    vr = 1.0;
                }
                TimeT originTime = new TimeT(vel.getWaveform().getEvent().getOriginTime());
                TimeT startTime;
                TimeT trimTime = originTime.add(vel.getDistance() / vr);
                TimeT beginTime = new TimeT(vel.getWaveform().getBeginTime());
                TimeSeries trimmedWaveform = converter.convert(vel.getWaveform());
                try {
                    trimmedWaveform.cutBefore(trimTime);
                    trimmedWaveform.cutAfter(trimTime.add(30.0));

                    startTime = new TimeT(trimTime.getEpochTime() + trimmedWaveform.getMaxTime()[0]);
                } catch (IllegalArgumentException e) {
                    startTime = trimTime;
                }

                TimeSeries segment = converter.convert(vel.getWaveform());
                segment.interpolate(1.0);

                double stopTime = endTimePicker.getEndTime(
                        segment.getData(),
                            segment.getSamprate(),
                            startTime.getEpochTime(),
                            segment.getIndexForTime(startTime.getEpochTime()),
                            minlength,
                            maxlength,
                            params.getMinSnr(),
                            vel.getNoiseLevel());

                if (new TimeT(stopTime).gt(startTime)) {
                    stopTime = stopTime + beginTime.subtractD(originTime);
                }
                stopTime = new TimeT(stopTime).subtractD(startTime);

                double offset = stopTime - startTime.subtractD(originTime);
                if (offset < minlength) {
                    stopTime = BAD;
                } else if (offset > maxlength) {
                    stopTime = maxlength;
                }

                WaveformPick autoPick = new WaveformPick().setPickType(PICK_TYPES.F.name())
                                                          .setPickName(PICK_TYPES.F.getPhase())
                                                          .setWaveform(vel.getWaveform())
                                                          .setPickTimeSecFromOrigin((float) stopTime);

                WaveformPick startPick = new WaveformPick().setPickType(PICK_TYPES.AP.name())
                                                           .setPickName(PICK_TYPES.AP.getPhase())
                                                           .setWaveform(vel.getWaveform())
                                                           .setPickTimeSecFromOrigin((float) startTime.subtractD(originTime));

                vel.getWaveform().getAssociatedPicks().add(autoPick);
                vel.getWaveform().getAssociatedPicks().add(startPick);

            }

            return vel;
        }).filter(Objects::nonNull).collect(Collectors.toList());
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
