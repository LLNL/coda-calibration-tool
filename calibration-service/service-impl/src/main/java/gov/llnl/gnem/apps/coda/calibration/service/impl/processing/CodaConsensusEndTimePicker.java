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
    public double getEndTime(float[] subsection, float[] synthSubsection, double sampleRate, double startTimeEpochSeconds, double minLengthSec, double maxLengthSec, double minimumSnr,
            double centerFreq, double distance) {
        List<Double> snrPicks = new ArrayList<>();
        snrPicks.add(getSnrEndPick(subsection, sampleRate, minLengthSec, maxLengthSec, minimumSnr, 10));
        snrPicks.add(getSnrEndPick(subsection, sampleRate, minLengthSec, maxLengthSec, minimumSnr, 15));
        snrPicks.add(getSnrEndPick(subsection, sampleRate, minLengthSec, maxLengthSec, minimumSnr, 20));
        snrPicks.add(getSnrEndPick(subsection, sampleRate, minLengthSec, maxLengthSec, minimumSnr, 40));
        snrPicks.add(getSnrEndPick(subsection, sampleRate, minLengthSec, maxLengthSec, minimumSnr, 60));
        snrPicks.add(getSnrEndPick(subsection, sampleRate, minLengthSec, maxLengthSec, minimumSnr, 80));

        List<Double> syntheticPicks = new ArrayList<>();
        syntheticPicks.add(getSyntheticEndPick(subsection, synthSubsection, sampleRate, centerFreq));

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

        return startTimeEpochSeconds + overallPick;
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
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (Double value : data) {
            if (value > min) {
                stats.addValue(Math.min(value, max));
            }
        }
        return stats.getGeometricMean();
    }

    private Double getSyntheticEndPick(final float[] subsection, final float[] synthSubsection, final double samprate, final double centerFreq) {

        double windowSize;
        double overlap;
        double slopemax;

        if (centerFreq <= 0.25) {
            windowSize = 50.0;
            overlap = 30.0;
            slopemax = 1.3;
        } else if (centerFreq <= 0.85) {
            windowSize = 40.0;
            overlap = 20.0;
            slopemax = 0.7;
        } else if (centerFreq <= 3.5) {
            windowSize = 30.0;
            overlap = 20.0;
            slopemax = 0.4;
        } else if (centerFreq <= 6.5) {
            windowSize = 15.0;
            overlap = 10.0;
            slopemax = 0.5;
        } else {
            windowSize = 10.0;
            overlap = 7.0;
            slopemax = 0.5;
        }

        boolean done = false;

        int winLength = (int) (windowSize * samprate);
        double slopeTimePick = BAD_PICK;

        int dataLength = subsection.length > synthSubsection.length ? synthSubsection.length : subsection.length;

        SimpleRegression obs = new SimpleRegression();
        SimpleRegression synth = new SimpleRegression();

        int ii = 0;
        while (!done) {

            if (ii + winLength < dataLength) {
                obs.clear();
                synth.clear();

                for (int i = ii; i < ii + winLength; i++) {
                    obs.addData(i, subsection[i]);
                    synth.addData(i, synthSubsection[i]);
                }
                ii += (winLength - (overlap * samprate));

                double diffSlope = Math.abs(synth.getSlope() - obs.getSlope());
                double ratioSlope = diffSlope / Math.abs(synth.getSlope());

                if (ratioSlope > slopemax && obs.getSlope() > -0.005) {
                    slopeTimePick = ii;
                    done = true;
                }
            } else {
                slopeTimePick = subsection.length;
                done = true;
            }
        }
        return slopeTimePick;
    }

    private Double getSnrEndPick(final float[] subsection, final double samprate, final double minlength, final double maxlength, final double minimumSnr, final int windowSize) {
        // end defined by the point at which the signal drops below a
        // minimum level (e.g. 2x noise amplitude)
        int ii = 0;

        boolean done = false;

        int winLength = (int) ((windowSize * samprate) + 2);
        DescriptiveStatistics stats = new DescriptiveStatistics(winLength);
        double snrTimePick = 0.0;

        while (!done) {
            ++ii;
            if (ii >= subsection.length) {
                if (ii / samprate >= minlength && ii / samprate <= maxlength) {
                    snrTimePick = ii / samprate;
                }
                break;
            }

            if (subsection[ii] > (.3 + subsection[ii - 1])) {
                snrTimePick = (ii - 1) / samprate;
                break;
            }

            stats.addValue(subsection[ii]);

            if (ii / samprate >= maxlength) {
                snrTimePick = maxlength;
                done = true;
            }

            if (ii > winLength && stats.getMean() < minimumSnr) {
                snrTimePick = (ii - 1) / samprate;
                done = true;
            }
        }
        return snrTimePick;
    }
}
