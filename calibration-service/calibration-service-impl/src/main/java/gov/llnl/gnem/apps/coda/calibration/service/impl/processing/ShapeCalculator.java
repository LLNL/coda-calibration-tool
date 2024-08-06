/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.model.domain.EnvelopeFit;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class ShapeCalculator {

    private static final Logger log = LoggerFactory.getLogger(ShapeCalculator.class);

    private WaveformToTimeSeriesConverter converter;

    @Autowired
    public ShapeCalculator(WaveformToTimeSeriesConverter converter) {
        this.converter = converter;
    }

    public List<ShapeMeasurement> fitShapelineToMeasuredEnvelopes(Collection<Entry<PeakVelocityMeasurement, WaveformPick>> filteredVelocityMeasurements,
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters, ShapeFitterConstraints constraints, boolean autoPickingEnabled) {
        List<ShapeMeasurement> measuredShapes = new ArrayList<>();
        if (filteredVelocityMeasurements == null || filteredVelocityMeasurements.isEmpty()) {
            // TODO: Feedback about this to the user
            return measuredShapes;
        }

        // Loop through each entry and try to fit a line using the max
        // amplitude prediction for that frequency band from the velocity model

        measuredShapes = filteredVelocityMeasurements.parallelStream().filter(Objects::nonNull).map(filteredVelocityMeasurement -> {
            try {
                CalibrationCurveFitter curveFitter = new CalibrationCurveFitter();
                PeakVelocityMeasurement velocityMeasurement = filteredVelocityMeasurement.getKey();
                WaveformPick endPick = filteredVelocityMeasurement.getValue();

                boolean isAutoPicked = velocityMeasurement.getWaveform().getAssociatedPicks().stream().anyMatch(wp -> wp.getPickName().equalsIgnoreCase(PICK_TYPES.AP.name()));
                boolean shouldAutoPick = autoPickingEnabled && isAutoPicked;

                FrequencyBand freqBand = new FrequencyBand(velocityMeasurement.getWaveform().getLowFrequency(), velocityMeasurement.getWaveform().getHighFrequency());
                SharedFrequencyBandParameters frequencyBandParameter = frequencyBandParameters.get(freqBand);

                if (frequencyBandParameter == null) {
                    // TODO: Feedback to user
                    log.info("Unable to find frequency band parameters for band {} given input measurement {}; this measurement will be skipped", freqBand, velocityMeasurement);
                    return null;
                }

                double distance = velocityMeasurement.getDistance();

                TimeT originTime = new TimeT(velocityMeasurement.getWaveform().getEvent().getOriginTime());
                TimeT endTime;

                Double maxTimeRaw = velocityMeasurement.getTime();

                Double velocity = frequencyBandParameter.getVelocity0() - (frequencyBandParameter.getVelocity1() / (frequencyBandParameter.getVelocity2() + distance));

                double travelTimeRaw = 0.0;
                if (velocity != 0.0) {
                    travelTimeRaw = (distance / velocity);
                }

                Double timeDifference = maxTimeRaw - travelTimeRaw;
                TimeT travelTime = originTime.add(travelTimeRaw);

                endTime = originTime.add(endPick.getPickTimeSecFromOrigin());

                if (travelTime.ge(endTime)) {
                    log.trace("Encountered F pick with time before expected Coda start while processing {}; processing will skip this file", velocityMeasurement);
                    return null;
                }

                //TODO: Make synthetic creation/fit part of the optimization pass
                TimeSeries seis = converter.convert(velocityMeasurement.getWaveform());

                seis.cut(travelTime, endTime);
                if (seis.getSamprate() > 1.0) {
                    seis.interpolate(1.0);
                }
                if (constraints != null && constraints.getFittingPointCount() > 0 && seis.getNsamp() > constraints.getFittingPointCount()) {
                    double samprate = (constraints.getFittingPointCount() / (double) seis.getNsamp()) * seis.getSamprate();
                    seis.interpolate(samprate);
                }

                if (frequencyBandParameter.getMinLength() > 0 && seis.getLengthInSeconds() < frequencyBandParameter.getMinLength()) {
                    log.trace(
                            "Encountered a too small window length while processing {} with length {} and minimum window of {}; processing will skip this file",
                                velocityMeasurement,
                                seis.getLengthInSeconds(),
                                frequencyBandParameter.getMinLength());
                    return null;
                } else if (frequencyBandParameter.getMaxLength() > 0 && seis.getLengthInSeconds() > frequencyBandParameter.getMaxLength()) {
                    log.trace(
                            "Encountered a too large window length while processing {} with length {} and maximum window of {}; processing will continue on a truncated envelope",
                                velocityMeasurement,
                                seis.getLengthInSeconds(),
                                frequencyBandParameter.getMaxLength());
                    seis.cutAfter(travelTime.add(frequencyBandParameter.getMaxLength()));
                }

                EnvelopeFit curve = curveFitter.fitCodaCMAES(seis.getData(), seis.getSamprate(), constraints, frequencyBandParameter.getMinLength(), shouldAutoPick);

                if (shouldAutoPick) {
                    //Ensure pick is persisted back to waveform
                    double end = curve.getEndTime();
                    if (end >= 0d) {
                        endPick.setPickTimeSecFromOrigin(end + seis.getTime().subtractD(originTime));
                    }
                }

                return new ShapeMeasurement().setDistance(distance)
                                             .setWaveform(velocityMeasurement.getWaveform())
                                             .setV0(frequencyBandParameter.getVelocity0())
                                             .setV1(frequencyBandParameter.getVelocity1())
                                             .setV2(frequencyBandParameter.getVelocity2())
                                             .setMeasuredGamma(curve.getGamma())
                                             .setMeasuredBeta(curve.getBeta())
                                             .setMeasuredIntercept(curve.getIntercept())
                                             .setMeasuredError(curve.getError())
                                             .setMeasuredTime(travelTime.getDate())
                                             .setTimeDifference(timeDifference);
            } catch (IllegalArgumentException | NullPointerException e) {
                log.warn("Error generating shape {}", e.getMessage());
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList());

        if (measuredShapes == null) {
            measuredShapes = new ArrayList<>(0);
        }
        return measuredShapes;
    }
}