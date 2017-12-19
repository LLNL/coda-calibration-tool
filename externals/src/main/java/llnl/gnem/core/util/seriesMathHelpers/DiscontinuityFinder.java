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
package llnl.gnem.core.util.seriesMathHelpers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import llnl.gnem.core.util.SeriesMath;

/**
 *
 * @author dodge1
 */
public class DiscontinuityFinder {

    private static final int MAX_STACK_DEPTH = 10;

    private static Extremum extractMaxExtremum(ArrayList<Extremum> extremes) {
        double max = -Double.MAX_VALUE;
        int idx = -1;
        for (int j = 0; j < extremes.size(); ++j) {
            if (extremes.get(j).getValue() > max) {
                max = extremes.get(j).getValue();
                idx = j;
            }
        }
        Extremum result = extremes.get(idx);
        extremes.remove(idx);
        return result;
    }

    private static Collection<Extremum> extractNeighbors(Extremum maxExtremum, ArrayList<Extremum> extremes, int winLength, int stackDepth) {
        Collection<Extremum> result = new ArrayList<>();
        if (stackDepth >= MAX_STACK_DEPTH) {
            return result;
        }


        int idx = maxExtremum.getIndex();
        Iterator<Extremum> it = extremes.iterator();
        while (it.hasNext()) {
            Extremum thisExtremum = it.next();
            int distance = Math.abs(idx - thisExtremum.getIndex());
            if (distance <= winLength) {
                result.add(thisExtremum);
                it.remove();
            }
        }


        Collection<Extremum> tmp = new ArrayList<>();

        for (Extremum ext : result) {
            Collection<Extremum> additional = extractNeighbors(ext, extremes, winLength, ++stackDepth);
            if (!additional.isEmpty() && stackDepth <= MAX_STACK_DEPTH) {
                tmp.addAll(additional);
            }
        }
        result.addAll(tmp);

        return result;
    }

    private static double getWindowAverage(float[] tmp, int i, int winLength) {
        if (i < 0) {
            return -1;
        }
        if (i + winLength > tmp.length - 1) {
            return -1;
        }
        double avg = 0;
        for (int j = i; j < i + winLength; ++j) {
            avg += tmp[j];
        }
        return avg / winLength;
    }

    private static void createDiscontinuity(double std, double deviation, int minIdx, int winLength, int maxIdx, float[] tmp, int idxMaxVal, double sampRate, DiscontinuityCollection result) {
        double relDeviation = std > 0 ? deviation / std : 0.0;
        int winStart = minIdx - winLength;
        if (winStart < 0) {
            winStart = 0;
        }
        int winEnd = maxIdx + winLength;
        if (winEnd > tmp.length - 1) {
            winEnd = tmp.length - 1;
        }
        int npts = winEnd - winStart + 1;
        float[] tmpArray = new float[npts];
        System.arraycopy(tmp, winStart, tmpArray, 0, npts);
        SampleStatistics statistics = new SampleStatistics(tmpArray);
        Discontinuity d = new Discontinuity(idxMaxVal, idxMaxVal / sampRate, relDeviation, statistics);
        result.add(d);
    }

    private static class Extremum {

        private final int index;
        private final double value;

        Extremum(int index, double value) {
            this.index = index;
            this.value = value;
        }

        /**
         * @return the index
         */
        public int getIndex() {
            return index;
        }

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        @Override
        public String toString() {
            return String.format("idx =%d, value = %f", index, value);
        }
    }

    private static class LocalExtrema {

        Collection<Extremum> extrema;

        LocalExtrema(Collection<Extremum> data) {
            extrema = new ArrayList<>(data);
        }

        private int getMinIndex() {
            int idx = Integer.MAX_VALUE;
            for (Extremum ext : extrema) {
                if (ext.getIndex() < idx) {
                    idx = ext.getIndex();
                }
            }
            return idx;
        }

        private int getMaxIndex() {
            int idx = -Integer.MAX_VALUE;
            for (Extremum ext : extrema) {
                if (ext.getIndex() > idx) {
                    idx = ext.getIndex();
                }
            }
            return idx;
        }

        private int getMaxValIdx() {
            double maxVal = -Double.MAX_VALUE;
            int idx = -Integer.MAX_VALUE;
            for (Extremum ext : extrema) {
                if (ext.getValue() > maxVal) {
                    idx = ext.getIndex();
                    maxVal = ext.getValue();
                }
            }
            return idx;
        }

        @Override
        public String toString() {
            return String.format("minIdx = %d, maxValIdx = %d, maxIdx = %d", getMinIndex(), getMaxValIdx(), getMaxIndex());
        }
    }


    public static DiscontinuityCollection findDiscontinuities(float[] data, double sampRate, int winLength, double factor) {
        DiscontinuityCollection result = new DiscontinuityCollection();
        if (data.length < 2 * winLength + 1) {
            return result;
        }
        float[] tmp = data.clone();
        SeriesMath.setMaximumRange(tmp, 1.0e5);
        tmp = SeriesMath.getFirstDifference(tmp);
        tmp = SeriesMath.arrayMultiply(tmp, tmp);
        
        
        double avg = SeriesMath.getMean(tmp);
        double var = SeriesMath.getVariance(tmp);
        double std = Math.sqrt(var);
        double threshold = factor * std;
        ArrayList<Extremum> extremes = getExtremes(tmp, avg, threshold);
        if (extremes.isEmpty()) {
            return result;
        }

        Collection<LocalExtrema> allExtrema = new ArrayList<>();
        while (!extremes.isEmpty()) {
            Extremum maxExtremum = extractMaxExtremum(extremes);
            Collection<Extremum> neighbors = extractNeighbors(maxExtremum, extremes, winLength, 1);
            neighbors.add(maxExtremum);
            LocalExtrema le = new LocalExtrema(neighbors);
            allExtrema.add(le);
        }

        for (LocalExtrema ext : allExtrema) {
            int minIdx = ext.getMinIndex();
            int maxIdx = ext.getMaxIndex();
            int idxMaxVal = ext.getMaxValIdx();
            double preAverage = getWindowAverage(tmp, idxMaxVal - winLength, winLength);
            double postAverage = getWindowAverage(tmp, idxMaxVal + 1, winLength);

            double average = 0;
            if (postAverage >= 0 && preAverage >= 0) {
                average = (postAverage + preAverage) / 2;
            } else if (postAverage >= 0) {
                average = postAverage;
            } else if (preAverage >= 0) {
                average = preAverage;
            }
            double deviation = tmp[idxMaxVal] - average;

            if (deviation > threshold) {
                createDiscontinuity(std, deviation, minIdx, winLength, maxIdx, tmp, idxMaxVal, sampRate, result);
            }

        }
        result.computeStatistics();
        return result;
    }

    private static ArrayList<Extremum> getExtremes(float[] tmp, double avg, double threshold) {
        ArrayList<Extremum> extremes = new ArrayList<>();
        for (int j = 0; j < tmp.length; ++j) {
            double deviation = tmp[j] - avg;
            if (deviation >= threshold) {
                extremes.add(new Extremum(j, deviation));
            }
        }
        return extremes;
    }
}
