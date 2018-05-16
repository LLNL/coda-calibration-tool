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

import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class WaveformUtils {

    // TODO: Make these configurable
    // In KM/s
    private static double groupVelocityDenominator = 10.0;
    // In Seconds
    private static long noiseWindowOffset = 20l;

    /**
     * @param distance
     *            in kilometers between event and station recording it. This is
     *            used to compute the predicted start and end arrival time
     *            windows based on the group velocity.
     * @param origintime
     *            event origin time
     * @param segment
     *            the segment of the waveform to use for computing the noise
     *            window
     * @return {@link TimeSeries} containing the calculated noise window offset
     *         from the origin time
     * @throws IllegalArgumentException
     *             if the predicted time windows do not align with the actual
     *             envelope (predicted start > end of the recorded waveform,
     *             etc.)
     */
    public static TimeSeries getNoiseWindow(double distance, TimeT origintime, TimeSeries segment) {

        TimeSeries result1 = new TimeSeries(segment);
        result1.cutBefore(result1.getTime().add(noiseWindowOffset));
        double mean1 = result1.getMean();
        
        TimeSeries result2 = new TimeSeries(segment);
        double groupVelocity = distance / groupVelocityDenominator;
        TimeT noiseStart = origintime.subtract(noiseWindowOffset);
        TimeT noiseEnd = origintime.add(groupVelocity);

        result2.cut(noiseStart, noiseEnd);
        double mean2 = result2.getMean();
        
        return mean1 > mean2 ? result2 : result1;
    }

    /**
     * This method mimics the getMaxTime method of SacSeismogram, which is
     * <b>NOT</b> purely the same as the methods in the TimeSeries chain. Trying
     * to not use SacSeismogram directly as it will be moving out of Core > 1.1
     * and will introduce an extra dependency.
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

    public static float[] doublesToFloats(Double[] x) {
        float[] xfloats = new float[x.length];
        for (int i = 0; i < x.length; i++) {
            xfloats[i] = x[i].floatValue();
        }
        return xfloats;
    }

    public static double[] floatsToDoubles(float[] x) {
        double[] xdoubles = new double[x.length];
        for (int i = 0; i < x.length; i++) {
            xdoubles[i] = x[i];
        }
        return xdoubles;
    }

    public static boolean isValidWaveform(Waveform w) {
        return (w != null && w.getEvent() != null && w.getStream() != null && w.getStream().getStation() != null);
    }
}
