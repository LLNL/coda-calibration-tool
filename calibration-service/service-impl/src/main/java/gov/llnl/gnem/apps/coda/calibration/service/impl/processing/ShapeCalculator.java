/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformPick;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class ShapeCalculator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WaveformToTimeSeriesConverter converter;

    @Autowired
    public ShapeCalculator(WaveformToTimeSeriesConverter converter) {
        this.converter = converter;
    }

    public List<ShapeMeasurement> fitShapelineToMeasuredEnvelopes(Collection<Entry<PeakVelocityMeasurement, WaveformPick>> filteredVelocityMeasurements,
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters) {

        List<ShapeMeasurement> measuredShapes = new ArrayList<>();
        if (filteredVelocityMeasurements == null || filteredVelocityMeasurements.isEmpty()) {
            // TODO: Feedback about this to the user
            return measuredShapes;
        }

        CalibrationCurveFitter curveFitter = new CalibrationCurveFitter();

        // Loop through each entry and try to fit a line using the max
        // amplitude prediction for that frequency band from the velocity model

        for (Entry<PeakVelocityMeasurement, WaveformPick> filteredVelocityMeasurement : filteredVelocityMeasurements) {
            PeakVelocityMeasurement velocityMeasurement = filteredVelocityMeasurement.getKey();
            WaveformPick endPick = filteredVelocityMeasurement.getValue();

            FrequencyBand freqBand = new FrequencyBand(velocityMeasurement.getWaveform().getLowFrequency(), velocityMeasurement.getWaveform().getHighFrequency());
            SharedFrequencyBandParameters frequencyBandParameter = frequencyBandParameters.get(freqBand);

            if (frequencyBandParameter == null) {
                // TODO: Feedback to user
                log.trace("Unable to find frequency band parameters for band {} given input measurement {}; this measurement will be skipped", freqBand, velocityMeasurement);
                continue;
            }

            double distance = velocityMeasurement.getDistance();

            TimeT originTime = new TimeT(velocityMeasurement.getWaveform().getEvent().getOriginTime());
            TimeT travelTime;
            TimeT endTime;

            Double maxTimeRaw = velocityMeasurement.getTime();

            Double velocity = frequencyBandParameter.getVelocity0() - (frequencyBandParameter.getVelocity1() / (frequencyBandParameter.getVelocity2() + distance));

            final Double dt = 1.0;
            Double travelTimeRaw = (distance / (velocity + dt));
            travelTime = originTime.add(travelTimeRaw);

            Double timeDifference = maxTimeRaw - travelTimeRaw;

            endTime = originTime.add(endPick.getPickTimeSecFromOrigin());

            if (travelTime.ge(endTime)) {
                log.trace("Encountered F pick with time before expected Coda start while processing {}; processing will skip this file", velocityMeasurement);
                continue;
            }
            TimeSeries synthSeis = converter.convert(velocityMeasurement.getWaveform());
            try {
                synthSeis.cut(travelTime, endTime);

                if (frequencyBandParameter.getMinLength() > 0 && synthSeis.getLengthInSeconds() < frequencyBandParameter.getMinLength()) {
                    log.trace("Encountered a too small window length while processing {} with length {} and minimum window of {}; processing will skip this file",
                              velocityMeasurement,
                              synthSeis.getLengthInSeconds(),
                              frequencyBandParameter.getMinLength());
                    continue;
                } else if (frequencyBandParameter.getMaxLength() > 0 && synthSeis.getLengthInSeconds() > frequencyBandParameter.getMaxLength()) {
                    log.trace("Encountered a too large window length while processing {} with length {} and maxium window of {}; processing will continue on a truncated envelope",
                              velocityMeasurement,
                              synthSeis.getLengthInSeconds(),
                              frequencyBandParameter.getMaxLength());
                    synthSeis.cutAfter(travelTime.add(frequencyBandParameter.getMaxLength()));
                }

                double[] curve = curveFitter.fitCodaStraightLine(synthSeis.getData());
                measuredShapes.add(new ShapeMeasurement().setDistance(distance)
                                                         .setWaveform(velocityMeasurement.getWaveform())
                                                         .setV0(frequencyBandParameter.getVelocity0())
                                                         .setV1(frequencyBandParameter.getVelocity1())
                                                         .setV2(frequencyBandParameter.getVelocity2())
                                                         .setMeasuredGamma(curve[0])
                                                         .setMeasuredBeta(curve[1])
                                                         .setTimeDifference(timeDifference));
            } catch (IllegalArgumentException e) {
                log.info("Error generating shape {}", e.getMessage());
            }
        }

        return measuredShapes;
    }
}