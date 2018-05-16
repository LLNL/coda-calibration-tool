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
import java.util.List;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.service.api.EndTimePicker;

@Component
public class CodaConsensusEndTimePicker implements EndTimePicker {

    private static final double BAD_PICK = -100.0;

    @Override
    public double getEndTime(float[] subsection, float[] synthSubsection, double sampleRate, double startTimeEpochSeconds, int startOffset, double minLengthSec, double maxLengthSec, double minimumSnr,
            double noiseAmp, double centerFreq, double distance) {
        List<Double> snrPicks = new ArrayList<>();
        if (centerFreq >= 1.0) {
            snrPicks.add(getSnrEndPick(subsection, sampleRate, startOffset, minLengthSec, maxLengthSec, minimumSnr, noiseAmp, 20, centerFreq));
        }

        List<Double> syntheticPicks = new ArrayList<>();
        if (centerFreq <= 3.0) {
            syntheticPicks.add(getSyntheticEndPick(subsection, synthSubsection, sampleRate, startOffset, centerFreq));
        }

        Double snrAggregatePick = getFilteredGeometricMean(snrPicks, minLengthSec, maxLengthSec);

        Double syntheticAggregatePick = getFilteredGeometricMean(syntheticPicks, minLengthSec, maxLengthSec);

        List<Double> endPicks = new ArrayList<>();
        if (!Double.isNaN(snrAggregatePick)) {
            endPicks.add(snrAggregatePick);
        }

        if (!Double.isNaN(syntheticAggregatePick)) {
            endPicks.add(syntheticAggregatePick);
        }

        Double overallPick = getFilteredGeometricMean(endPicks, minLengthSec, maxLengthSec);

        if (Double.isNaN(overallPick)) {
            if (!endPicks.isEmpty()) {
                overallPick = endPicks.get(0);
            } else {
                overallPick = BAD_PICK;
            }
        }

        return startTimeEpochSeconds + (overallPick * sampleRate);
    }

    /**
     * @param data
     *            List<Double> of values
     * @param min
     *            The minimum allowable value to be added to the calculation of
     *            the mean. Values below this are discarded.
     * @param max
     *            The maximum allowable value to be added to the calculation of
     *            the mean. <b>{@link Math}.min(value, max)</b> is added to the
     *            sum.
     * @return The geometric mean of all values that met the min/max criteria
     *         specified or {@link Double}.NaN if calculating a mean value is
     *         not possible.
     */
    private static Double getFilteredGeometricMean(final List<Double> data, final double min, final double max) {
        if (data != null && data.size() == 1) {
            return data.get(0);
        }
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double value : data) {
            if (value > min) {
                stats.addValue(Math.min(value, max));
            }
        }
        return stats.getGeometricMean();
    }

    private Double getSyntheticEndPick(final float[] subsection, final float[] synthSubsection, final double samprate, int startOffset, final double centerFreq) {

        double windowSize;
        double overlap;
        double slopemax;
        double minLength;

        if (centerFreq <= 0.25) {
            windowSize = 90.0;
            overlap = 10.0;
            slopemax = 2.3;
            minLength = 200.0;
        } else if (centerFreq <= 0.85) {
            windowSize = 60.0;
            overlap = 10.0;
            slopemax = 2.0;
            minLength = 120.0;
        } else if (centerFreq <= 3.5) {
            windowSize = 50.0;
            overlap = 10.0;
            slopemax = 2.1;
            minLength = 90.0;
        } else {
            windowSize = 10.0;
            overlap = 2.0;
            slopemax = 2.8;
            minLength = 10.0;
        }

        boolean done = false;

        int winLength = (int) (windowSize * samprate);
        double slopeTimePick = BAD_PICK;

        int dataLength = subsection.length > synthSubsection.length ? synthSubsection.length : subsection.length;

        SimpleRegression obs = new SimpleRegression();
        SimpleRegression synth = new SimpleRegression();
        DescriptiveStatistics obsStats = new DescriptiveStatistics();

        int ii = 0;
        int stepCount = 0;
        while (!done) {

            if (ii + winLength < dataLength) {
                obs.clear();
                synth.clear();
                obsStats.clear();

                for (int i = ii; i < ii + winLength; i++) {
                    obs.addData(i, subsection[i]);
                    synth.addData(i, synthSubsection[i]);
                    obsStats.addValue(subsection[i]);
                }
                ii += (winLength - (overlap * samprate));

                if (ii >= startOffset) {
                    double minToMax = obsStats.getMin() / obsStats.getMax();
                    double diffSlope = Math.abs(synth.getSlope() - obs.getSlope());
                    double ratioSlope = diffSlope / Math.abs(obs.getSlope());
                    double synInt = synth.getIntercept() + obsStats.getMean();
                    double offset = obs.getIntercept() / synInt;

                    if (stepCount > 0 && (ratioSlope > slopemax || obs.getSlope() < -0.05)) {
                        slopeTimePick = ii;
                        done = true;
                    } else if (stepCount > 0 && (offset > 1.8 || offset < -0.2 || minToMax > 1.0 || minToMax < -0.2)) {
                        slopeTimePick = ii;
                        done = true;
                    }
                }
                stepCount++;

            } else {
                slopeTimePick = subsection.length;
                done = true;
            }
        }
        return slopeTimePick;

    }

    private Double getSnrEndPick(final float[] subsection, final double samprate, int startOffset, final double minlength, final double maxlength, final double minimumSnr, final double noiseAmp,
            final int windowSize, final double centerFreq) {
        // end defined by the point at which the signal drops below a
        // minimum level (e.g. 2x noise amplitude)
        int ii = 0;

        boolean done = false;

        int winLength = (int) ((windowSize * samprate) + 2);

        if (centerFreq <= 0.25) {
            winLength = (int) (winLength * 2.5);
        } else if (centerFreq <= 0.85) {
            winLength = (int) (winLength * 2.0);
        } else if (centerFreq <= 3.5) {
            winLength = (int) (winLength * 1.5);
        }

        DescriptiveStatistics stats = new DescriptiveStatistics(winLength);
        DescriptiveStatistics shortObs = new DescriptiveStatistics(5);
        double snrTimePick = BAD_PICK;

        while (!done) {
            ++ii;
            if (ii >= subsection.length) {
                if (ii / samprate >= minlength && ii / samprate <= maxlength) {
                    snrTimePick = ii / samprate;
                }
                break;
            }

            if (ii >= startOffset) {
                shortObs.addValue(subsection[ii]);

                if (subsection[ii] > (1.10 * shortObs.getMean())) {
                    snrTimePick = (ii - 1) / samprate;
                    break;
                }

                stats.addValue(subsection[ii]);

                if (ii / samprate >= maxlength) {
                    snrTimePick = maxlength;
                    done = true;
                }

                if (ii > winLength && (stats.getMean() - noiseAmp) < minimumSnr) {
                    if (((ii - startOffset) / samprate) > minlength) {
                        snrTimePick = (ii - 1) / samprate;
                    }
                    done = true;
                }
            }
        }
        return snrTimePick;
    }
}
