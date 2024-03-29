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
package gov.llnl.gnem.apps.coda.common.service.util;

import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.random.EmpiricalDistribution;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class WaveformUtils {
    public static double getNoiseFloor(double[] waveform) {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        double[] values = new double[waveform.length];
        for (int i = 0; i < waveform.length; i++) {
            values[i] = waveform[i];
            stats.addValue(values[i]);
        }

        Double noise = stats.getPercentile(50);

        EmpiricalDistribution dist = new EmpiricalDistribution(100);
        dist.load(values);

        List<SummaryStatistics> bins = dist.getBinStats();
        long lastCount = 0;
        double maxBinVal = noise;
        for (SummaryStatistics bin : bins) {
            if (bin.getN() > lastCount) {
                lastCount = bin.getN();
                maxBinVal = bin.getMean();
            }
        }

        if (maxBinVal < noise) {
            noise = maxBinVal;
        }
        return noise;
    }

    /**
     * This method mimics the getMaxTime method of SacSeismogram, which is
     * <b>NOT</b> purely the same as the methods in the TimeSeries chain. Trying
     * to not use SacSeismogram directly as it will be moving out of Core &gt;
     * 1.1 and will introduce an extra dependency.
     *
     * @param waveform
     * @param originTime
     * @return double[] where 0 is the <i>origin</i> time shifted by the
     *         waveform's beginTime + maxTime and 1 is the amplitude value
     *         recorded at the original maxTime
     */
    public static double[] getMaxTime(TimeSeries waveform, TimeT originTime) {

        double[] maxtime = waveform.getMaxTime();
        TimeT begintime = waveform.getTime();
        TimeT referencetime = originTime;
        TimeT shiftedtime = begintime.add(maxtime[0]);

        double maxreftime = shiftedtime.subtract(referencetime).getEpochTime();
        return new double[] { maxreftime, maxtime[1] };

    }

    public static float[] doublesToFloats(double[] x) {
        float[] xfloats = new float[x.length];
        IntStream.range(0, x.length).parallel().forEach(i -> xfloats[i] = (float) x[i]);
        return xfloats;
    }

    public static double[] floatsToDoubles(float[] x) {
        double[] xdoubles = new double[x.length];
        IntStream.range(0, x.length).parallel().forEach(i -> xdoubles[i] = x[i]);
        return xdoubles;
    }

    public static boolean isValidWaveform(Waveform w) {
        return (w != null
                && w.getEvent() != null
                && w.getLowFrequency() != null
                && w.getHighFrequency() != null
                && w.getEvent().getEventId() != null
                && w.getEvent().getLatitude() != 0.0
                && w.getEvent().getLongitude() != 0.0
                && w.getStream() != null
                && w.getStream().getStation() != null
                && w.getStream().getStation().getStationName() != null
                && w.getStream().getStation().getLatitude() != 0.0
                && w.getStream().getStation().getLongitude() != 0.0);
    }

    public static boolean cutSeismograms(TimeSeries a, TimeSeries b, TimeT startTime, TimeT endTime) {
        boolean completedCut = false;
        a.cut(startTime, endTime);
        b.cut(startTime, endTime);

        // These might be off by some small number of samples due to
        // precision errors during cut and SeriesMath will throw an ArrayBounds if
        // they don't match exactly so we need to double check!
        if (a.getNsamp() != b.getNsamp()) {

            TimeT start = a.getTime();
            TimeT end = a.getEndtime();
            TimeT startA = a.getTime();
            TimeT startB = b.getTime();
            TimeT endA = a.getEndtime();
            TimeT endB = b.getEndtime();

            // choose the latest start time and the earliest end time for
            // the cut window
            if (startA.lt(startB)) {
                start = startB;
            }
            if (endA.gt(endB)) {
                end = endB;
            }
            if (start.ge(end)) {
                // don't continue if the seismograms don't overlap
                return completedCut;
            }

            int begin_index_a = a.getIndexForTime(start.getEpochTime());
            int end_index_a = a.getIndexForTime(end.getEpochTime());

            int begin_index_b = b.getIndexForTime(start.getEpochTime());
            int end_index_b = b.getIndexForTime(end.getEpochTime());

            int nptsa = (end_index_a - begin_index_a) + 1;
            int nptsb = (end_index_b - begin_index_b) + 1;

            int npts = Math.min(nptsa, nptsb);
            // now cut the traces again
            a.cut(begin_index_a, (begin_index_a + npts - 1));
            b.cut(begin_index_b, (begin_index_b + npts - 1));
        }
        completedCut = true;
        return completedCut;
    }

}
