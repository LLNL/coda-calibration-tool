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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.service.api.EndTimePicker;

@Component
public class CodaSNREndTimePicker implements EndTimePicker {

    private static final Logger log = LoggerFactory.getLogger(CodaSNREndTimePicker.class);

    private static final double BAD_PICK = -100.0;

    @Override
    public double getEndTime(float[] waveform, double sampleRate, double startTimeEpochSeconds, int startOffset, double minLengthSec, double maxLengthSec, double minimumSnr, double noise) {

        Double snrPick = getSnrEndPick(waveform, sampleRate, startOffset, minLengthSec, maxLengthSec, minimumSnr, noise, 40);

        Double overallPick;
        if (!Double.isNaN(snrPick)) {
            overallPick = snrPick;
        } else {
            overallPick = BAD_PICK;
        }

        return startTimeEpochSeconds + overallPick;
    }

    private Double getSnrEndPick(final float[] waveform, final double sampleRate, int startOffset, final double minLengthSec, final double maxLengthSec, final double minimumSnr, final double noise,
            final int windowSize) {
        int obsWindow = (int) (windowSize * sampleRate);
        int spikeSamples = (int) ((windowSize / 4) * sampleRate);
        DescriptiveStatistics obs = new DescriptiveStatistics(obsWindow);
        DescriptiveStatistics spike = new DescriptiveStatistics(spikeSamples);
        SimpleRegression spikeReg = new SimpleRegression();
        double snrTimePick = BAD_PICK;

        int minSamples = (int) (minLengthSec * sampleRate);
        int maxSamples = (int) (maxLengthSec * sampleRate);
        if (startOffset < 0) {
            startOffset = 0;
        }

        if (waveform.length > startOffset && waveform.length - startOffset > minSamples) {
            int stopIdx = waveform.length > startOffset + maxSamples ? startOffset + maxSamples : waveform.length;
            double noiseThreshold = noise + minimumSnr;
            log.trace("Snr end picker running with stopIdx {}, minSamples {}, maxSamples {}, waveformLength {}, noiseThreshold {}", stopIdx, minSamples, maxSamples, waveform.length, noiseThreshold);
            if (waveform[startOffset] >= noiseThreshold) {
                snrTimePick = startOffset / sampleRate;
                for (int i = startOffset; i < stopIdx; i++) {
                    obs.addValue(waveform[i]);
                    spike.addValue(waveform[i]);
                    if (obs.getN() >= windowSize) {
                        if (obs.getMean() <= noiseThreshold) {
                            for (int j = i - obsWindow; j < i; j++) {
                                if (j > 0 && waveform[j] <= noiseThreshold) {
                                    snrTimePick = j / sampleRate;
                                    break;
                                }
                            }
                            break;
                        } else if (waveform[i] <= noiseThreshold) {
                            snrTimePick = i / sampleRate;
                            break;
                        } else {
                            snrTimePick = i / sampleRate;
                        }
                    }
                    if (spike.getN() > spikeSamples) {
                        double[] spikeVals = spike.getValues();
                        spikeReg.clear();
                        for (int k = 0; k < spikeVals.length; k++) {
                            spikeReg.addData(k, spikeVals[k]);
                        }

                        double spikeSlope = spikeReg.getSlope();
                        if (!Double.isNaN(spikeSlope)) {
                            if (spikeSlope > 0.1) {
                                snrTimePick = (i - spike.getN() - 1) / sampleRate;
                                break;
                            }
                        }
                    }
                }
            }
        }
        log.trace("Snr end picker returning snrTimePick {}", snrTimePick);
        return snrTimePick;
    }

}
