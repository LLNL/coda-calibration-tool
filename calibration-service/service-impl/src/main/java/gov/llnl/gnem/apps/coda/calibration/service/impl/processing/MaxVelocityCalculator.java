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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class MaxVelocityCalculator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private WaveformToTimeSeriesConverter converter;

    // TODO: Make these configurable
    final static double groupVelocity1Greater300 = 4.7;
    final static double groupVelocity2Greater300 = 2.3;
    final static double groupVelocity1Less300 = 3.9;
    final static double groupVelocity2Less300 = 1.9;

    @Autowired
    public MaxVelocityCalculator(WaveformToTimeSeriesConverter converter) {
        this.converter = converter;
    }

    public Collection<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms) {
        return waveforms.stream().parallel().map(rawWaveform -> {
            TimeSeries waveform = converter.convert(rawWaveform);
            double distance = EModel.getDistanceWGS84(rawWaveform.getEvent().getLatitude(),
                                                      rawWaveform.getEvent().getLongitude(),
                                                      rawWaveform.getStream().getStation().getLatitude(),
                                                      rawWaveform.getStream().getStation().getLongitude());
            TimeT origintime = new TimeT(rawWaveform.getEvent().getOriginTime());
            TimeT starttime;
            TimeT endtime;
            if (distance >= 300) {
                starttime = origintime.add(distance / groupVelocity1Greater300);
                endtime = origintime.add(distance / groupVelocity2Greater300);
            } else {
                starttime = origintime.add(distance / groupVelocity1Less300);
                endtime = origintime.add(distance / groupVelocity2Less300);
            }

            // The envelope is in log10.
            try {
                double noiseamp = Math.abs(WaveformUtils.getNoiseWindow(distance, origintime, waveform).getMean());

                // cut the coda window portion of the seismograms
                waveform.cut(starttime, endtime);

                double min_length = 50.;
                if (endtime.subtract(starttime).getEpochTime() < min_length) {
                    log.trace("Coda window length too short: {}", endtime.subtract(starttime).getEpochTime());
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

                // the envelope noise is in log10 units.
                double snrPeak = peakS[1] - noiseamp;
                return new PeakVelocityMeasurement().setWaveform(rawWaveform).setSnr(snrPeak).setVelocity(velocity).setDistance(distance).setTime(peakS[0]).setAmplitude(peakS[1]);
            } catch (IllegalArgumentException ill) {
                log.info("Unable to compute maximum velocity, this stack will be skipped. {} {}.", ill.getMessage(), rawWaveform);
                return new PeakVelocityMeasurement();
            }
        }).filter(measurement -> measurement.getWaveform() != null).collect(Collectors.toList());
    }
}
