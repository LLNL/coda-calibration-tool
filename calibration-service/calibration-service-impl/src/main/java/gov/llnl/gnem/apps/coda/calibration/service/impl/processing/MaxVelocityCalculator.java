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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class MaxVelocityCalculator {

    private static final Logger log = LoggerFactory.getLogger(MaxVelocityCalculator.class);
    private WaveformToTimeSeriesConverter converter;
    private VelocityConfiguration velConf;

    @Autowired
    public MaxVelocityCalculator(VelocityConfiguration velConf, WaveformToTimeSeriesConverter converter) {
        this.converter = converter;
        this.velConf = velConf;
    }

    public Stream<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms) {
        return computeMaximumVelocity(
                waveforms,
                velConf.getGroupVelocity1InKmsGtDistance(),
                velConf.getGroupVelocity2InKmsGtDistance(),
                velConf.getGroupVelocity1InKmsLtDistance(),
                velConf.getGroupVelocity2InKmsLtDistance(),
                velConf.getDistanceThresholdInKm());
    }

    public Stream<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms, VelocityConfiguration velocityConfiguration) {
        if (velocityConfiguration != null) {
            return computeMaximumVelocity(
                    waveforms,
                        velocityConfiguration.getGroupVelocity1InKmsGtDistance(),
                        velocityConfiguration.getGroupVelocity2InKmsGtDistance(),
                        velocityConfiguration.getGroupVelocity1InKmsLtDistance(),
                        velocityConfiguration.getGroupVelocity2InKmsLtDistance(),
                        velocityConfiguration.getDistanceThresholdInKm());
        } else {
            return computeMaximumVelocity(waveforms);
        }
    }

    private Stream<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms, double gv1GtDistanceThreshold, double gv2GtDistanceThreshold, double gv1LtDistanceThreshold,
            double gv2LtDistanceThreshold, double thresholdInKm) {
        return waveforms.stream().parallel().map(rawWaveform -> {
            TimeSeries waveform = converter.convert(rawWaveform);
            double distance = EModel.getDistanceWGS84(
                    rawWaveform.getEvent().getLatitude(),
                        rawWaveform.getEvent().getLongitude(),
                        rawWaveform.getStream().getStation().getLatitude(),
                        rawWaveform.getStream().getStation().getLongitude());
            TimeT origintime = new TimeT(rawWaveform.getEvent().getOriginTime());
            TimeT starttime;
            TimeT endtime;

            if (distance >= thresholdInKm) {
                starttime = origintime.add(distance / gv1GtDistanceThreshold);
                endtime = origintime.add(distance / gv2GtDistanceThreshold);
            } else {
                starttime = origintime.add(distance / gv1LtDistanceThreshold);
                endtime = origintime.add(distance / gv2LtDistanceThreshold);
            }

            // The envelope is in log10.
            try {
                // cut the coda window portion of the seismograms
                waveform.cut(starttime, endtime);

                double min_length = 25.;
                if (endtime.subtract(starttime).getEpochTime() < min_length) {
                    log.debug("Coda window length too short: {}", endtime.subtract(starttime).getEpochTime());
                }

                // peakS[0] time in seconds for
                // reference peakS[1] max amplitude
                double[] peakS = WaveformUtils.getMaxTime(waveform, origintime);

                // +1 to avoid divide by zero slamming this to +Infinity when
                // the first sample is the largest
                if (peakS[0] == 0.0) {
                    peakS[0] = 1.0;
                }

                double velocity = distance / peakS[0];

                double noise = WaveformUtils.getNoiseFloor(rawWaveform.getSegment());

                // the envelope noise is in log10 units.
                double snrPeak = peakS[1] - noise;
                return new PeakVelocityMeasurement().setWaveform(rawWaveform)
                                                    .setNoiseStartSecondsFromOrigin(0d)
                                                    .setNoiseEndSecondsFromOrigin(20d)
                                                    .setNoiseLevel(noise)
                                                    .setSnr(snrPeak)
                                                    .setVelocity(velocity)
                                                    .setDistance(distance)
                                                    .setTime(peakS[0])
                                                    .setAmplitude(peakS[1]);
            } catch (IllegalArgumentException ill) {
                log.info("Unable to compute maximum velocity, this stack will be skipped. {} {}.", ill.getMessage(), rawWaveform);
                return new PeakVelocityMeasurement();
            }
        }).filter(measurement -> measurement.getWaveform() != null);
    }
}
