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
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.service.api.EndTimePicker;

@Component
public class CodaSNREndTimePicker implements EndTimePicker {

    private static final double BAD_PICK = -100.0;

    @Override
    public double getEndTime(float[] waveform, double sampleRate, double startTimeEpochSeconds, int startOffset, double minLengthSec, double maxLengthSec, double minimumSnr) {

        Double snrPick = getSnrEndPick(waveform, sampleRate, startOffset, minLengthSec, maxLengthSec, minimumSnr, 20);

        Double overallPick;
        if (!Double.isNaN(snrPick)) {
            overallPick = snrPick;
        } else {
            overallPick = BAD_PICK;
        }

        return startTimeEpochSeconds + overallPick;
    }

    private Double getSnrEndPick(final float[] waveform, final double sampleRate, int startOffset, final double minLengthSec, final double maxLengthSec, final double minimumSnr,
            final int windowSize) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (float value : waveform) {
            stats.addValue(value);
        }
        Double noise = stats.getPercentile(50);
        DescriptiveStatistics obs = new DescriptiveStatistics((int) (windowSize * sampleRate));
        DescriptiveStatistics spike = new DescriptiveStatistics((int) (5 * sampleRate));
        double snrTimePick = BAD_PICK;

        int minSamples = (int) (minLengthSec * sampleRate);
        int maxSamples = (int) (maxLengthSec * sampleRate);

        if (waveform.length > startOffset && waveform.length - startOffset > minSamples) {
            int stopIdx = waveform.length > maxSamples ? maxSamples : waveform.length;
            for (int i = startOffset; i < stopIdx; i++) {
                obs.addValue(waveform[i]);
                spike.addValue(waveform[i]);
                if (obs.getN() >= windowSize) {
                    if (obs.getMean() - minimumSnr <= noise || spike.getMean() > (obs.getMean() + 1d)) {
                        break;
                    } else {
                        snrTimePick = (double) (i-spike.getN()) / sampleRate;
                    }
                }
            }
        }

        return snrTimePick;
    }
}
