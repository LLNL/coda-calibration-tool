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
package llnl.gnem.core.waveform.seismogram;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.math3.complex.Complex;

import com.google.common.base.Objects;

import llnl.gnem.core.util.ApplicationLogger;
import llnl.gnem.core.util.Epoch;
import llnl.gnem.core.util.PairT;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.randomNumbers.RandomAlgorithm;
import llnl.gnem.core.util.randomNumbers.RandomAlgorithmFactory;
import llnl.gnem.core.util.seriesMathHelpers.DiscontinuityCollection;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;
import llnl.gnem.core.util.seriesMathHelpers.SampleStatistics;
import llnl.gnem.core.util.seriesMathHelpers.SampleStatistics.Order;
import llnl.gnem.core.waveform.io.BinaryData;
import llnl.gnem.core.waveform.merge.MergeException;

/**
 * User: dodge1 Date: Mar 23, 2006
 */
public class TimeSeries implements Comparable<TimeSeries>, Serializable, Cloneable, SeismicSignal {

    private float[] data;
    private final Collection<Epoch> dataGaps; // TODO these are not being properly updated for in-place modifications
    private final Collection<TimeSeries.SeriesListener> listeners;
    private double samprate = 1.0;
    private SampleStatistics statistics;
    private TimeT time;

    /**
     * no-arg constructor only for Serialization
     */
    public TimeSeries() {
        this.dataGaps = new ArrayList<>();
        this.listeners = new ArrayList<>();
    }

    public TimeSeries(float[] data, double samprate, TimeT time) {
        this(data, samprate, time, true);
    }

    public TimeSeries(BinaryData data, double samprate, TimeT time) {
        this(data.getFloatData(), samprate, time, false);
    }

    public TimeSeries(TimeSeries s) {
        data = s.data.clone();
        samprate = s.samprate;
        time = new TimeT(s.time);
        this.dataGaps = new ArrayList<>(s.dataGaps);
        listeners = new ArrayList<>(s.listeners);
        statistics = null;
    }

    private TimeSeries(float[] data, double samprate, TimeT time, boolean clone) {
        this.data = clone ? data.clone() : data;
        this.samprate = samprate;
        this.time = new TimeT(time);
        dataGaps = findDataGaps(data, time, samprate);
        listeners = new ArrayList<>();
        statistics = null;
    }

    public static final double EPSILON = 0.0000001;
    private static double ALLOWABLE_SAMPLE_RATE_ERROR = 0.005;
    private static final int MIN_WINDOW_SAMPLES = 10;
    private static final long serialVersionUID = 1L;

    /**
     * Gets the peakToPeakAmplitude attribute of an input timeseries at the
     * specified period. Advances a sliding window of length period through the
     * time series a point at a time. At each position, the Peak-To-Peak range
     * of the current window is computed. The maximum value of all these values
     * is returned.
     *
     * @param period
     *            The period in seconds at which to compute the value.
     * @param data
     *            An array of floats whose Peak-To-Peak value is to be
     *            determined.
     * @param sampleInterval
     *            The sample interval of the data in the data array.
     * @return The maximum Peak-To-Peak value for the array.
     */
    public static double getPeakToPeakAmplitude(float[] data, double sampleInterval, double period) {
        int N = data.length;
        if (N < 1) {
            throw new IllegalArgumentException("Cannot compute PeakToPeak amplitude on empty array.");
        }
        double result = 0.0;
        int SampsInWindow = (int) Math.round(period / sampleInterval) + 1;
        int LastWindowStart = N - SampsInWindow;
        if (LastWindowStart <= 0) {
            return getWindowPeakToPeak(data, 0, N);
        } else {
            for (int j = 0; j <= LastWindowStart; ++j) {
                double p2p = getWindowPeakToPeak(data, j, SampsInWindow);
                if (p2p > result) {
                    result = p2p;
                }
            }
        }
        return result;
    }

    public static <T extends TimeSeries> PairT<T, T> rotateTraces(T seis1, T seis2, double theta) {
        float[] data1 = seis1.getData();
        float[] data2 = seis2.getData();
        SeriesMath.rotate(data1, data2, theta, true, true);
        seis1.setData(data1);
        seis2.setData(data2);
        return new PairT<>(seis1, seis2);
    }

    public static void setSampleRateErrorThreshold(double value) {
        ALLOWABLE_SAMPLE_RATE_ERROR = value;
    }

    private static Collection<Epoch> findDataGaps(float[] data, TimeT time, double samprate) {
        Collection<Epoch> result = new ArrayList<>();
        int minGapLength = 5;
        boolean inGap = false;
        int gapStart = -1;
        int gapEnd = -1;
        for (int j = 0; j < data.length; ++j) {
            float value = data[j];
            if (value == 0.0f) {
                if (!inGap) {
                    inGap = true;
                    gapStart = j;
                }
            } else {
                if (inGap) {
                    gapEnd = j - 1;
                    if (gapEnd >= 0 && gapStart >= 0 && gapEnd - gapStart >= minGapLength) {
                        result.add(makeGapEpoch(time, samprate, gapStart, gapEnd));
                    }
                }
                inGap = false;
                gapStart = -1;
                gapEnd = -1;
            }
        }
        return result;

    }

    private static double[] getAbsValues(Complex[] spectrum) {
        double[] result = new double[spectrum.length];
        for (int j = 0; j < result.length; ++j) {
            result[j] = spectrum[j].abs();
        }
        return result;
    }

    private static double getWindowPeakToPeak(float[] data, int idx, int Nsamps) {
        float min = Float.MAX_VALUE;
        float max = -min;
        for (int j = 0; j < Nsamps; ++j) {
            if (min > data[j + idx]) {
                min = data[j + idx];
            }
            if (max < data[j + idx]) {
                max = data[j + idx];
            }
        }
        return max - min;
    }

    private static Epoch makeGapEpoch(TimeT time, double samprate, int gapStart, int gapEnd) {
        double startOffset = gapStart / samprate;
        double endOffset = gapEnd / samprate;
        TimeT start = new TimeT(time.getEpochTime() + startOffset);
        TimeT end = new TimeT(time.getEpochTime() + endOffset);
        return new Epoch(start, end);
    }

    private static boolean merge(float[] data1, float[] data2, float[] newData, int destOffset, boolean ignoreMismatch) {
        try {
            // First copy samples from the earlier-starting trace to the destination trace. Nothing to merge here.
            int maxIdx = Math.min(destOffset, data2.length);
            System.arraycopy(data2, 0, newData, 0, maxIdx);

            // Now merge the overlapped portions...
            int maxMergeIndexBound = Math.min(destOffset + data1.length, data2.length);
            for (int j = destOffset; j < maxMergeIndexBound; ++j) {
                newData[j] = mergeSamples(data2[j], data1[j - destOffset], ignoreMismatch);
            }

            // Copy remaining samples from the input arrays (whichever has samples after their common samples)
            if (maxMergeIndexBound < data2.length) {
                System.arraycopy(data2, maxMergeIndexBound, newData, maxMergeIndexBound, data2.length - maxMergeIndexBound);
            } else if (maxMergeIndexBound < data1.length + destOffset) {
                for (int j = maxMergeIndexBound; j < data1.length + destOffset; ++j) {
                    int idx = j - destOffset;
                    if (idx >= 0 && idx < data1.length && j < newData.length) {
                        newData[j] = data1[idx];
                    }
                }
            }
            return true;
        } catch (MergeException e) {
            return false;
        }
    }

    private static float mergeSamples(float value1, float value2, boolean ignoreMismatch) throws MergeException {
        if (value1 == value2) {
            return value1;
        } else if (value1 == 0) {
            return value2;
        } else if (value2 == 0) {
            return value1;
        } else {
            if (ignoreMismatch) {
                return (value1 + value2) / 2;
            } else {
                throw new MergeException(String.format("Overlapped data have different values (%f vs %f)!", value1, value2));
            }
        }
    }

    /**
     * A method to add this seismogram to another equal length seismogram
     *
     * A check is made to ensure that the epoch times are aligned.
     *
     * @param otherseis
     *            the other SacSeismogram object
     * @return
     */
    public boolean AddAlignedSeismogram(TimeSeries otherseis) {
        if (otherseis == null) {
            return false;
        }

        if (!rateIsComparable(otherseis)) {
            return false;
        }

        TimeT starttime = getTime();
        TimeT endtime = getEndtime();

        try {
            float[] otherdata = otherseis.getSubSection(starttime, endtime);

            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = data[ii] + otherdata[ii];
            }
        } catch (Exception e) {
            return false;
        }

        onModify();
        return true;
    }

    /**
     * Add a scalar to the time series of this CssSeismogram
     *
     * @param value
     *            The scalar value to be added to the time series
     */
    @Override
    public void AddScalar(double value) {
        SeriesMath.AddScalar(data, value);
        onModify();
    }

    /**
     * A method to add this seismogram to another equal length seismogram Note
     * that the sample rate is not constrained and should be checked if
     * necessary before calling this method
     *
     * @param otherseis
     *            the other CssSeismogram object
     * @return Returns true if the operation was successful.
     */
    public boolean AddSeismogram(TimeSeries otherseis) {
        if (!rateIsComparable(otherseis)) {
            return false;
        }
        if (data.length != otherseis.data.length) {
            return false;
        }

        for (int j = 0; j < data.length; j++) {
            data[j] = data[j] + otherseis.data[j];
        }

        onModify();
        return true;
    }

    /**
     * Replaces each point in this Seismogram with its log10 value.
     */
    @Override
    public void Log10() {
        data = SeriesMath.log10(data);
        onModify();
    }

    /**
     * Multiply the time series values of this CssSeismogram by a scalar
     * constant.
     *
     * @param value
     *            The scalar value with which to multiply the time series values
     */
    @Override
    public void MultiplyScalar(double value) {
        SeriesMath.MultiplyScalar(data, value);
        onModify();
    }

    /**
     * Remove the mean of the time series of this CssSeismogram
     */
    @Override
    public void RemoveMean() {
        SeriesMath.RemoveMean(data);
        onModify();
    }

    /**
     * Remove the median value of the time series of this CssSeismogram
     */
    @Override
    public void RemoveMedian() {
        SeriesMath.removeMedian(data);
        onModify();
    }

    /**
     * Replaces each point in the Seismogram its signed sqrt
     * <p>
     * </p>
     * Note: values LT 0 are returned -1* sqrt(abs(value)).
     */
    @Override
    public void SignedSqrt() {
        data = SeriesMath.signedSqrt(data);
        onModify();
    }

    /**
     * Replaces each point in the Seismogram its signed square value
     * <p>
     * </p>
     * Note: values LT 0 are returned -1* value*value.
     */
    @Override
    public void SignedSquare() {
        data = SeriesMath.signedSquare(data);
        onModify();
    }

    /**
     * Convert to single bit (+1, -1 or 0)
     */
    @Override
    public void Signum() {
        SeriesMath.signum(data);
        onModify();
    }

    /**
     * Smooth the Seismogram using a sliding window of width halfWidth. replaces
     * the data with it's smoothed result
     * <p>
     * </p>
     * Note halfwidth refers to number of samples, not seconds
     *
     * @param halfwidth
     *            half width in samples.
     */
    @Override
    public void Smooth(int halfwidth) {
        data = SeriesMath.MeanSmooth(data, halfwidth);//TODO note the SeriesMath.MeanSmooth() method should replace data with the smoothed version, but this isn't happening. Fix
        onModify();
    }

    /**
     * Replaces each point in the Seismogram with its sqrt
     * <p>
     * </p>
     * Note: values LT 0 are returned 0.
     */
    @Override
    public void Sqrt() {
        data = SeriesMath.sqrt(data);
        onModify();
    }

    /**
     * Replaces each point in the Seismogram with its square value
     */
    @Override
    public void Square() {
        data = SeriesMath.square(data);
        onModify();
    }

    /**
     * Apply a cosine taper to the time series of this seismogram
     *
     * @param TaperPercent
     *            The (one-sided) percent of the time series to which a taper
     *            will be applied. The value ranges from 0 (no taper) to 50 (
     *            The taper extends half the length of the CssSeismogram ).
     *            Since the taper is symmetric, a 50% taper means that all but
     *            the center value of the CssSeismogram will be scaled by some
     *            value less than 1.0.
     */
    @Override
    public void Taper(double TaperPercent) {
        SeriesMath.Taper(data, TaperPercent);
        onModify();
    }

    public void WriteASCIIfile(String filename) throws IOException {
        FileOutputStream out = null;
        BufferedOutputStream bout = null;
        PrintStream pout = null;
        try {
            out = new FileOutputStream(filename);

            bout = new BufferedOutputStream(out);
            pout = new PrintStream(bout);
            for (int j = 0; j < data.length; ++j) {
                pout.println(String.valueOf((j / samprate)) + "   " + data[j]);
            }
        } finally {
            if (pout != null) {
                pout.close();
            }
            if (bout != null) {
                bout.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public TimeSeries add(TimeSeries other) {
        TimeSeries.BivariateFunction f = new TimeSeries.BivariateFunction() {
            @Override
            public double eval(double x, double y) {
                return x + y;
            }
        };
        return intersect(other, f);
    }

    public void addInPlace(TimeSeries other) {
        if (!rateIsComparable(other)) {
            String msg = String.format("Seismograms have different sample rates! (%s)  -  (%s)", this.toString(), other.toString());
            throw new IllegalStateException(msg);
        }
        double myStart = time.getEpochTime();
        double otherStart = other.getTimeAsDouble();
        double earliest = myStart < otherStart ? myStart : otherStart;

        double myEnd = getEndtime().getEpochTime();
        double otherEnd = other.getEndtime().getEpochTime();
        double latest = myEnd > otherEnd ? myEnd : otherEnd;

        double timeRange = latest - earliest;
        long nsamps = Math.round(timeRange * samprate) + 1;

        int myOffset = (int) Math.round((myStart - earliest) * getSamprate());
        int myLast = myOffset + data.length - 1;
        if (myLast > nsamps - 1) {
            nsamps = myLast + 1;
        }
        int otherOffset = (int) Math.round((otherStart - earliest) * samprate);

        int otherLast = otherOffset + other.data.length - 1;
        if (otherLast > nsamps - 1) {
            nsamps = otherLast + 1;
        }
        float[] result = new float[(int) nsamps];
        System.arraycopy(data, 0, result, myOffset, data.length);
        for (int j = 0; j < other.data.length; ++j) {
            result[j + otherOffset] += other.data[j];
        }
        data = result;
        time = new TimeT(earliest);
        onModify();
    }

    public void addListener(TimeSeries.SeriesListener listener) {
        listeners.add(listener);
    }

    public TimeSeries append(TimeSeries other) {
        if (!rateIsComparable(other)) {
            String msg = String.format("Seismograms have different sample rates! (%s)  -  (%s)", this.toString(), other.toString());
            ApplicationLogger.getInstance().log(Level.WARNING, msg);
        }
        double expectedAppendeeStart = this.getEndtime().getEpochTime() + getDelta();
        double actualAppendeeStart = other.getTimeAsDouble();

        int newDataNpts = data.length + other.data.length;
        int newDataOffset = data.length;
        long errorInSamples = Math.round((expectedAppendeeStart - actualAppendeeStart) * samprate);
        newDataNpts -= (int) errorInSamples;
        newDataOffset -= (int) errorInSamples;
        if (newDataNpts < 1) {
            String msg = String.format("After correcting for start time mismatch " + "of %d samples, new data array is less than 1 sample in length", errorInSamples);
            throw new IllegalStateException(msg);
        }
        float[] tmp = new float[newDataNpts];
        System.arraycopy(data, 0, tmp, 0, data.length);

        System.arraycopy(other.data, 0, tmp, newDataOffset, other.data.length);

        if (errorInSamples < 0) {
            int sampsToSet = (int) Math.abs(errorInSamples);
            for (int j = data.length; j < data.length + sampsToSet + 1; ++j) {
                tmp[j] = data[data.length - 1];
            }
        }
        double rate = samprate;
        if (other.samprate != samprate) {
            double otherEndTime = other.getEndtimeAsDouble();
            double newDuration = otherEndTime - time.getEpochTime();
            double newDelta = newDuration / (newDataNpts - 1);
            rate = 1 / newDelta;
        }
        return new TimeSeries(tmp, rate, time);

    }

    @Override
    public int compareTo(TimeSeries other) {
        double diff = time.getEpochTime() - other.time.getEpochTime();
        if (diff < 0) {
            return -1;
        } else if (diff > 0) {
            return 1;
        } else {
            return 0;
        }
    }

    @Override
    public double computeExtremeStat() {
        double v = getStatistics().getMean() - getStatistics().getMedian();
        double range = getStatistics().getRange();
        if (range > 0) {
            return v / range;
        } else {
            return 10000;
        }
    }

    @Override
    public boolean contains(Epoch epoch, boolean allowGaps) {
        Epoch myEpoch = new Epoch(time, getEndtime());
        if (myEpoch.isSuperset(epoch)) {
            if (allowGaps) {
                return true;
            } else {
                for (Epoch gapEpoch : dataGaps) {
                    if (gapEpoch.intersects(epoch)) {
                        return false;
                    }
                }
                return true;
            }
        } else {
            return false;
        }
    }

    public TimeSeries crop(Epoch epoch) {
        return crop(epoch.getTime(), epoch.getEndtime());
    }

    public TimeSeries crop(TimeT start, TimeT end) {
        TimeT currentStart = getTime();
        if (currentStart.gt(start)) {
            start = currentStart;
        }

        TimeT currentEnd = getEndtime();
        if (currentEnd.lt(end)) {
            end = currentEnd;
        }

        return new TimeSeries(getSubSection(start, end), getSamprate(), start);
    }

    public TimeSeries crop(int start, int end) {
        TimeSeries series = new TimeSeries(this);
        series.cut(start, end);
        return series;
    }

    /**
     * Truncate the Seismogram to be a subsection of itself. The time interval
     * to which the Seismogram will be cut is specified by the start and end
     * parameters. The Seismogram start time will be adjusted to conform to the
     * new starting time.
     *
     * @param start
     *            The starting time of the desired subsection. If start is less
     *            than the Seismogram begin time, the begin time will be used.
     *            If start is GT than the Seismogram endtime, then an
     *            IllegalArgumentException will be thrown.
     * @param end
     *            The end time of the desired subsection. If end is GT than the
     *            Seismogram end, then the Seismogram end will be used. If end
     *            is less than start then an IllegalArgumentException will be
     *            thrown.
     */
    public void cut(TimeT start, TimeT end) {
        if (start.ge(end)) {
            throw new IllegalArgumentException("Start time of cut is >= end time of cut.");
        }
        if (start.ge(getEndtime())) {
            throw new IllegalArgumentException("Start time of cut is >= end time of Seismogram.");
        }
        if (end.le(getTime())) {
            throw new IllegalArgumentException("End time of cut is <= start time of Seismogram.");
        }
        TimeT S = new TimeT(start);
        if (S.lt(getTime())) {
            S = getTime();
        }
        TimeT E = new TimeT(end);
        if (E.gt(this.getEndtime())) {
            E = getEndtime();
        }
        double duration = E.getEpochTime() - S.getEpochTime();
        data = getSubSection(S, duration);
        double dataStart = time.getEpochTime();

        int startIndex = (int) Math.round((S.getEpochTime() - dataStart) * samprate);
        TimeT actualNewStart = new TimeT(getTimeAsDouble() + startIndex / samprate);

        this.setTime(actualNewStart);
        onModify();
    }

    /**
     * Truncate the CssSeismogram to be a subsection of itself. The time
     * interval to which the CssSeismogram will be cut is specified by the start
     * and end parameters. The CssSeismogram start time will be adjusted to
     * conform to the new starting time. Note that, in this method, start and
     * end are in seconds relative to the current Seismogram time
     *
     * @param start
     *            The start time in seconds after the start of the uncut
     *            seismogram
     * @param end
     *            The end time in seconds after the start of the uncut
     *            seismogram
     */
    public void cut(double start, double end) {
        TimeT startT = time.add(start);
        TimeT endT = time.add(end);
        cut(startT, endT);
    }

    public void cut(int idx0, int idx1) {
        int maxIdx = data.length - 1;
        if (idx0 < 0 || idx0 > maxIdx) {
            throw new IllegalStateException("Illegal value for start cut index: " + idx0);
        }

        if (idx1 > maxIdx) {
            throw new IllegalStateException("Illegal value for end cut index: " + idx1);
        }
        if (idx1 - idx0 < 0) {
            throw new IllegalStateException("End index  must be >= start index : ");
        }

        int length = idx1 - idx0 + 1;

        float[] tmp = new float[length];
        System.arraycopy(data, idx0, tmp, 0, length);
        data = tmp;

        double deltaT = idx0 / samprate;
        time = new TimeT(time.getEpochTime() + deltaT);
    }

    public void cutAfter(TimeT end) {
        cut(getTime(), end);
    }

    public void cutBefore(TimeT start) {
        cut(start, getEndtime());
    }

    /**
     * Decimate the data (Note this should be interchangeable with the
     * interpolate methods)
     * <p>
     * </p>
     * the data series it is decimated so that only every Nth point is retained
     * where N is the decimationfactor
     * <p>
     * </p>
     * Note the samprate and number of points in the data series changes
     *
     * @param decimationfactor
     *            The amount by which to decimate the series.
     */
    public void decimate(int decimationfactor) {
        if (decimationfactor < 2) {
            return; // decimationfactor of 1 is the original series
        }
        data = SeriesMath.decimate(data, decimationfactor);
        samprate = samprate / decimationfactor;
        onModify();
    }

    /**
     * Differentiate the time series of this CssSeismogram. First two points are
     * differentiated using a forward-difference with error Oh2, Last two points
     * using a backward-difference operator with error Oh2. Remaining points
     * differentiated using a central-difference operator with order Oh4 (page
     * 397 - 399 of Applied Numerical Methods for Digital Computation by James
     * et al. ) Must be at least 4 points in series for this to work.
     */
    @Override
    public void differentiate() {
        SeriesMath.Differentiate(data, samprate);
        onModify();
    }

    public TimeSeries divide(TimeSeries other) {
        TimeSeries.BivariateFunction f = new TimeSeries.BivariateFunction() {
            @Override
            public double eval(double x, double y) {
                return x / y;
            }
        };
        return intersect(other, f);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TimeSeries)) {
            return false;
        }
        TimeSeries other = (TimeSeries) obj;
        double diff = time.getEpochTime() - other.time.getEpochTime();
        if (Math.abs(diff) > EPSILON) {
            return false;
        }

        if (!rateIsComparable(other)) {
            return false;
        }
        if (data.length != other.data.length) {
            return false;
        }
        for (int i = 0; i < data.length; i++) {
            if (Math.abs(data[i] - other.data[i]) > EPSILON) {
                return false;
            }
        }
        if (dataGaps.size() != other.dataGaps.size()) {
            return false;
        }
        return true;
    }

    public void fill(float[] buffer, int inc, double start, double end) {
        int startIndex = getIndexForTime(start);
        int endIndex = getIndexForTime(end);

        int b = 0;
        for (int i = startIndex; i <= endIndex && b < buffer.length; i++) {
            float bestSample = data[i];

            if ((i % inc) == 0) {
                buffer[b] = bestSample;
                b++;
                bestSample = 0.0f;
            }
        }
    }

    @Override
    public DiscontinuityCollection findDiscontinuities(int winLength, double factor) {
        return SeriesMath.findDiscontinuities(data, samprate, winLength, factor);
    }

    /**
     * Gets the time-series data of the CssSeismogram as a float array
     *
     * @return The data array
     */
    public float[] getData() {
        return data.clone();
    }

    public int getDataBytes() {
        // 8 bits per byte
        return data.length * (Float.SIZE / 8);
    }

    @Override
    public double getDelta() {
        return 1 / getSamprate();
    }

    @Override
    public double getDistinctValueRatio(int numSamples) {
        if (data.length < 1) {
            return 0;
        }
        Set<Float> values = new HashSet<>();
        int nsamples = Math.min(numSamples, data.length);
        RandomAlgorithm algorithm = RandomAlgorithmFactory.getAlgorithm();
        for (int j = 0; j < nsamples; ++j) {
            int k = algorithm.getBoundedInt(0, data.length - 1);
            values.add(data[k]);
        }
        return values.size() / (double) nsamples;
    }

    /**
     * Gets the endtime attribute of the CssSeismogram object
     *
     * @return The endtime value
     */
    @Override
    public TimeT getEndtime() {
        return time.add(getSegmentLength());
    }

    public double getEndtimeAsDouble() {
        return getEndtime().getEpochTime();
    }

    @Override
    public Epoch getEpoch() {
        return new Epoch(getTime(), getEndtime());
    }

    @Override
    public float getExtremum() {
        float max = getMax();
        float absmin = Math.abs(getMin());

        float result = 0.f;

        if (max >= absmin) {
            result = max;
        } else if (absmin > max) {
            result = absmin;
        }

        return result;

    }

    @Override
    public int getIdentifier() {
        return -1;
    }

    @Override
    public int getIndexForTime(double epochtime) {
        double dataStart = time.getEpochTime();
        return (int) Math.round((epochtime - dataStart) * samprate);
    }

    @Override
    public int getJdate() {
        return time.getJdate();
    }

    @Override
    public int getLength() {
        return data.length;
    }

    @Override
    public double getLengthInSeconds() {
        return getEpoch().duration();
    }

    /**
     * Gets the maximum value of the time series of the CssSeismogram object
     *
     * @return The max value
     */
    @Override
    public float getMax() {
        return (float) getStatistics().getMax();
    }

    /**
     * Gets the maximum value of the series and the time offset it occurs at.
     *
     * @return The (time offset at the max in seconds, the max value) of the
     *         series
     */
    @Override
    public double[] getMaxTime() {
        PairT<Integer, Float> value = SeriesMath.getMaxIndex(data);

        // maxat[0] is the index of the maximum value (written as a double)
        // data[maxat[0]] = maxvalue;
        double offset = value.getFirst() / samprate;
        return new double[] { offset, value.getSecond() };
    }

    /**
     * Gets the mean value of the time series of the CssSeismogram object
     *
     * @return The mean value
     */
    @Override
    public double getMean() {
        return getStatistics().getMean();
    }

    /**
     * Gets the median value of the time series of the CssSeismogram object
     *
     * @return The median value
     */
    @Override
    public double getMedian() {
        return getStatistics().getMedian();
    }

    /**
     * Gets the minimum value of the time series of the BasicSeismogram object
     *
     * @return The min value
     */
    @Override
    public float getMin() {
        return (float) getStatistics().getMin();
    }

    @Override
    public MinMax getMinMax() {
        return getStatistics().getMinMax();
    }

    public double getNormalizedRMSE(TimeSeries other) {
        double error = getRMSE(other);

        double norm = Math.abs(getMax() - getMin());
        if (norm > 0) {
            error /= norm;
        }

        return error;
    }

    @Override
    public int getNsamp() {
        return data.length;
    }

    public double getNseconds() {
        return data.length / samprate;
    }

    /**
     * Gets the Nyquist Frequency of the CssSeismogram
     *
     * @return The nyquistFreq value
     */
    @Override
    public double getNyquistFreq() {
        return samprate / 2.0;
    }

    /**
     * Gets the peakToPeakAmplitude attribute of the CssSeismogram's timeseries
     * at the specified period. Advances a sliding window of length period
     * through the time series a point at a time. At each position, the
     * Peak-To-Peak range of the current window is computed. The maximum value
     * of all these values is returned.
     *
     * @param period
     *            The period in seconds at which to compute the value.
     * @return The maximum Peak-To-Peak value for the entire seismogram.
     */
    @Override
    public double getPeakToPeakAmplitude(double period) {
        return getPeakToPeakAmplitude(data, 1.0 / samprate, period);
    }

    @Override
    public int getPointsIn(TimeT start, TimeT end) {
        return getPointsIn(end.getEpochTime() - start.getEpochTime());
    }

    @Override
    public int getPointsIn(double timeRange) {
        return (int) Math.round(timeRange * samprate) + 1;
    }

    @Override
    public double getPower() {
        double power = 0.0;
        for (double v : data) {
            power += v * v;
        }

        return power;
    }

    /**
     * Gets the RMS value of the CssSeismogram's time series.
     *
     * @return The RMS value
     */
    @Override
    public double getRMS() {
        return getStatistics().getRMS();
    }

    public double getRMSE(TimeSeries other) {
        float[] t = data;
        float[] o = other.data;

        double error = 0.0;
        for (int i = 0; i < t.length; i++) {
            double residual = Math.abs(t[i] - o[i]);
            error += residual * residual;
        }
        return Math.sqrt(error / t.length);
    }

    /**
     * Gets the range of the time series
     *
     * @return The statistical range for all values in the data
     */
    @Override
    public double getRange() {
        return getStatistics().getRange();
    }

    public double getRange(Epoch epoch) {
        float[] subSection = this.getSubSection(epoch.getTime(), epoch.getEndtime());
        return SeriesMath.getRange(subSection);
    }

    /**
     * Gets the samprate attribute of the CssSeismogram object
     *
     * @return The samprate value
     */
    @Override
    public double getSamprate() {
        return samprate;
    }

    /**
     * Gets the segment Length in seconds of the CssSeismogram object
     *
     * @return The segmentLength value
     */
    @Override
    public double getSegmentLength() {
        if (samprate > 0) {
            if (data.length > 1) {
                return (data.length - 1) / samprate;
            } else {
                return 0.0;
            }
        } else {
            throw new IllegalStateException("Invalid sample rate: " + samprate);
        }
    }

    @Override
    public double getSnr(double pickEpochTime, double preSeconds, double postSeconds) {
        double availablePreSeconds = pickEpochTime - getTimeAsDouble();
        double samples = availablePreSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }
        double availablePostSeconds = getEndtime().getEpochTime() - pickEpochTime;
        samples = availablePostSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }
        return SeriesMath.getSnr(data, samprate, getTime().getEpochTime(), pickEpochTime, preSeconds, postSeconds);
    }

    @Override
    public double getSnr(double pickEpochTime, Epoch epoch, double preSeconds, double postSeconds) {
        double start = Math.max(getTimeAsDouble(), epoch.getStart());
        double end = Math.min(getEndtime().getEpochTime(), epoch.getEnd());

        double availablePreSeconds = pickEpochTime - start;
        double samples = availablePreSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }
        double availablePostSeconds = end - pickEpochTime;
        samples = availablePostSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }

        int pick = getIndexForTime(pickEpochTime);
        int startIndex = getIndexForTime(Math.max(start, pickEpochTime - preSeconds));
        int endIndex = getIndexForTime(Math.min(end, pickEpochTime + postSeconds));
        return SeriesMath.getSnr(data, samprate, pick, startIndex, endIndex);
    }

    /**
     * Gets the variance of the time series of the CssSeismogram object
     *
     * @return The variance value
     */
    @Override
    public double getStDev() {
        return SeriesMath.getStDev(data);
    }

    public SampleStatistics getStatistics() {
        if (statistics == null) {
            statistics = new SampleStatistics(data, Order.FIRST);
        }
        return statistics;
    }

    /**
     * Gets a float array which is a subsection of the Seismogram's time series.
     * <p>
     * </p>
     * The start and end times must be within the Seismogram's time window or an
     * IllegalArgument exception will be thrown
     *
     * @param start
     *            the starting time of the subsection
     * @param end
     *            the ending time of the subsection
     * @return the subsection float[] array
     */
    public float[] getSubSection(TimeT start, TimeT end) {
        return getSubSection(start, end, null);
    }

    public float[] getSubSection(TimeT start, TimeT end, float[] result) {
        double duration = end.getEpochTime() - start.getEpochTime();
        return getSubSection(start.getEpochTime(), duration, result);
    }

    /**
     * Gets a float array which is a subsection of the CssSeismogram's time
     * series. The subsection starts at time start (presumed to be within the
     * CssSeismogram's time series) and has a length of duration.
     *
     * @param start
     *            The starting time of the subsection.
     * @param duration
     *            The duration in seconds of the subsection.
     * @return The subSection array
     */
    public float[] getSubSection(TimeT start, double duration) {
        return getSubSection(start.getEpochTime(), duration);
    }

    /**
     * Gets a float array which is a subsection of the CssSeismogram's time
     * series. The subsection starts at time start (presumed to be within the
     * CssSeismogram's time series) and has a length of duration.
     *
     * @param startEpoch
     *            The starting time expressed as a double epoch time.
     * @param requesteduration
     *            The duration in seconds of the subsection.
     * @return The subSection array
     */
    public float[] getSubSection(double startEpoch, double requesteduration) {
        return getSubSection(startEpoch, requesteduration, null);
    }

    public float[] getSubSection(double startEpoch, double requestedDuration, float[] result) {
        int Nsamps = data.length;
        if (Nsamps >= 1) {
            double duration = Math.abs(requestedDuration);
            int startIndex = getIndexForTime(startEpoch);
            int endIndex = getIndexForTime(startEpoch + duration);

            if (startIndex < 0) {
                startIndex = 0;
            }

            if (endIndex >= data.length) {
                endIndex = data.length - 1;
            }

            int sampsRequired = endIndex - startIndex + 1;
            if (result == null) {
                result = new float[sampsRequired];
            }
            try {
                System.arraycopy(data, startIndex, result, 0, sampsRequired);
            } catch (ArrayIndexOutOfBoundsException ex) {
                String msg = String.format("Requested %d samples from seismogram of length %d from index %d into buffer of length %d", sampsRequired, data.length, startIndex, result.length);
                throw new IllegalStateException(msg, ex);
            }
            return result;
        } else {
            return null;
        }
    }

    /**
     * Get subsection explicitly using the data indices
     *
     * @param startIndex
     * @param sampsRequired
     * @return
     */
    public float[] getSubSection(int startIndex, int sampsRequired) {
        float[] result = new float[sampsRequired];
        try {
            System.arraycopy(data, startIndex, result, 0, sampsRequired);
        } catch (ArrayIndexOutOfBoundsException ex) {
            String msg = String.format("Requested %d samples from seismogram of length %d from index %d into buffer of length %d", sampsRequired, data.length, startIndex, result.length);
            throw new IllegalStateException(msg, ex);
        }
        return result;
    }

    public double getSubsectionStartTime(double startEpoch) {
        int startIndex = getIndexForTime(startEpoch);
        return time.getEpochTime() + startIndex / samprate;
    }

    /**
     * Gets the sum of the time series values of this CssSeismogram
     *
     * @return The sum of the time series values
     */
    @Override
    public double getSum() {
        return SeriesMath.getSum(data);
    }

    /**
     * Gets the start time of the CssSeismogram as a TimeT object
     *
     * @return The time value
     */
    @Override
    public TimeT getTime() {
        return new TimeT(time);
    }

    /**
     * Gets the start time of the CssSeismogram as a double holding the epoch
     * time of the start.
     *
     * @return The CssSeismogram start epoch time value
     */
    @Override
    public double getTimeAsDouble() {
        return time.getEpochTime();
    }

    /**
     * Get the value at a specific point in time
     *
     * @param epochtime
     *            The time expressed as a double epoch time.
     * @return the value at the requested time
     */
    @Override
    public float getValueAt(double epochtime) {
        if (epochtime < time.getEpochTime()) {
            throw new IllegalArgumentException(String.format("Requested time (%s) is before seismogram start time!", new TimeT(epochtime).toString()));
        }
        if (epochtime > this.getEndtime().getEpochTime()) {
            throw new IllegalArgumentException(String.format("Requested time (%s) is after seismogram end time!", new TimeT(epochtime).toString()));

        }
        double unroundedIndex = getUnroundedTimeIndex(epochtime);
        int x1 = (int) unroundedIndex;
        if (unroundedIndex == x1) {
            return data[x1];
        } else {
            int x2 = x1 + 1;
            float y1 = data[x1];
            float y2 = data[x2];
            return (float) (y1 + (unroundedIndex - x1) * (y2 - y1));
        }
    }

    public float getValueAt(int j) {
        return data[j];
    }

    /**
     * Gets the variance of the time series of the CssSeismogram object
     *
     * @return The variance value
     */
    @Override
    public double getVariance() {
        return SeriesMath.getVariance(data);
    }

    /**
     * Check whether the data series has flat segments - where every element of
     * the segment is identical
     *
     * @param minsegmentlength
     *            the shortest number of datapoints that must be identical
     *            before it qualifies as "flat"
     */
    public boolean hasFlatSegments(int minsegmentlength) {
        return SeriesMath.hasFlatSegments(data, minsegmentlength);
        //onModify();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(samprate) * 31 + Arrays.hashCode(data);
    }

    /**
     * Interpolate the data
     *
     * @param newsamprate
     *            is the new sample rate (Hz)
     *            <p>
     *            </p>
     *            Note the samprate changes and the number of points in the data
     *            series changes based on the new desired sample rate
     */
    public void interpolate(double newsamprate) {
        if ((newsamprate > 0.)) {
            data = SeriesMath.interpolate(0., 1. / samprate, data, 1. / newsamprate);
            samprate = newsamprate;
            onModify();
        }
    }

    public boolean isConstant() {
        return SeriesMath.isConstant(data);
    }

    @Override
    public boolean isEmpty() {
        return getNsamp() == 0;
    }

    public boolean isSubset(TimeSeries other) {
        return getTime().ge(other.getTime()) && getEndtime().le(other.getEndtime());
    }

    public TimeSeries multiply(TimeSeries other) {
        TimeSeries.BivariateFunction f = new TimeSeries.BivariateFunction() {
            @Override
            public double eval(double x, double y) {
                return x * y;
            }
        };
        return intersect(other, f);
    }

    @Override
    public void normalize() {
        normalize(TimeSeries.Norm.EXTREMUM);
    }

    /**
     * Normalize all the seismograms
     *
     * Usage : 'normalize', 'normalize (value)' or 'normalize (type)' where
     * (type) is 'mean' 'min' or 'max'
     *
     * Traces are initially demeaned then scaled
     *
     * default - each trace is multiplied by abs(1/extremum) value - where value
     * is a number - the traces are normalized so that the absolute value of the
     * extremum equals the value entered mean - traces are multiplied by the
     * mean of the absolute values of the trace min - traces are multiplied by
     * -1/min max - traces are multiplied by 1/max
     *
     * @param norm
     */
    public void normalize(TimeSeries.Norm norm) {
        switch (norm) {
        case EXTREMUM:
            doNormalize(SeriesMath.getExtremum(data));
        case MEAN:
            doNormalize(SeriesMath.getMean(SeriesMath.abs(data)));
        case MIN:
            doNormalize(-1 * SeriesMath.getMin(data));
        case MAX:
            doNormalize(SeriesMath.getMax(data));
        case DELTA:
            doNormalize(SeriesMath.getMax(data) - SeriesMath.getMin(data));
        case RMS:
            doNormalize(SeriesMath.getRMS(data));
        }
    }

    public void normalize(double value) {
        doNormalize(SeriesMath.getExtremum(data) / value);
    }

    /**
     * Normalize the seismogram based on String input 1. Attempt to parse as a
     * Double valued number and use normalize(double) 2. Attempt to parse as a
     * Norm object and use normalize(Norm) 3. do nothing if neither 1 or 2
     * passes
     *
     * @param value
     *            a String containing either a number or a Norm type e.g.
     *            normalize("10") or normalize("EXTREMUM")
     *
     */
    public void normalize(String value) {
        try {
            // attempt to parse as a Number
            Double dvalue = Double.parseDouble(value);
            normalize(dvalue);
        } catch (Exception e) {
            try {
                // attempt to parse as a Norm type
                TimeSeries.Norm type = TimeSeries.Norm.valueOf(value.toUpperCase());
                normalize(type);
            } catch (Exception ee) {
            }
        }
    }

    public void onModify() {
        // Must recalculate statistics, but do so lazily
        statistics = null;

        for (TimeSeries.SeriesListener listener : listeners) {
            listener.dataChanged(data);
        }
    }

    /**
     * Computed the median in O(n) time in contrast to standard O(n lg n) time.
     * The only tradeoff is that the median is not the average of the two center
     * points for series with even length. Rather, the median is arbitrarily
     * selected from amongst one of those two approximate centers.
     *
     * @return The median or pseudo-median for even length series
     */
    @Override
    public double quickMedian() {
        return getStatistics().getMedian();
    }

    /**
     * Performs a check to see if the sample rates of two timeseries is close
     * enough to equal.
     *
     * @param other
     *            the other timeseries to compare this one with
     * @return true if the sample rates are close enough
     */
    public boolean rateIsComparable(TimeSeries other) {
        double fileOneDelta = this.getDelta();
        double fileTwoDelta = other.getDelta();
        double percentError = 100 * Math.abs((fileOneDelta - fileTwoDelta) / fileOneDelta);
        boolean isSameSampRate = percentError < ALLOWABLE_SAMPLE_RATE_ERROR;
        return isSameSampRate;
    }

    /**
     * remove glitches from the seismogram where glitches are defined by data
     * that exceed a threshhold above the variance defined by a moving window
     * <p>
     * </p>
     * value = Math.abs((data[j] - median)); if (value GT Threshhold *
     * Math.sqrt(variance)) replace data[j] with the median value
     *
     * @param Threshhold
     *            - the threshhold value
     */
    @Override
    public void removeGlitches(double Threshhold) {
        SeriesMath.removeGlitches(data, Threshhold);
        onModify();
    }

    public void removeListener(TimeSeries.SeriesListener listener) {
        listeners.remove(listener);
    }

    /**
     * Remove a linear trend from the time series data of this CssSeismogram.
     */
    @Override
    public void removeTrend() {
        SeriesMath.RemoveTrend(data);
        onModify();
    }

    public void resample(double newRate) {
        data = SeriesMath.interpolate(0., 1. / samprate, data, 1. / newRate);
        samprate = newRate;
        onModify();
    }

    /**
     * Reverse the data series. This method is used in cross correlation
     * routines. Note none of the times are being reset. The user must be
     * careful to understand the implications
     */
    @Override
    public void reverse() {
        SeriesMath.ReverseArray(data);
        onModify();
    }

    public void reverseAt(TimeT mirrorTime) {
        TimeT endtime = getEndtime();
        double shifttime = endtime.subtract(mirrorTime).getEpochTime();

        TimeT newbegintime = mirrorTime.add(-1 * shifttime);
        reverse();
        setTime(newbegintime);
    }

    @Override
    public void scaleTo(double min, double max) {
        double myMax = getStatistics().getMax();
        double myMin = getStatistics().getMin();
        double myRange = myMax - myMin;
        double requiredRange = max - min;
        double scale = requiredRange / myRange;
        SeriesMath.MultiplyScalar(data, scale);
        onModify();
    }

    /**
     * Sets the data array of the CssSeismogram object
     *
     * @param v
     *            The new data value
     */
    public void setData(float[] v) {
        data = v.clone();
        onModify();
    }

    /**
     * Sets the value of the data at a particular point to a value
     *
     * @param v
     * @param index
     */
    public void setDataPoint(float v, int index) {
        data[index] = v;
        onModify();
    }

    /**
     * Sets the data at a collection of points
     *
     * @param v
     * @param startindex
     */
    public void setDataPoints(float[] v, int startindex) {
        int maxindex = getNsamp() - 1;
        int endindex = startindex + v.length - 1;

        if ((startindex < 0) || (startindex > maxindex)) {
            return;
        }
        if (endindex > maxindex) {
            endindex = maxindex;
        }

        int arrayindex = -1;
        // replace all the values in the current array with the new values
        for (int index = startindex; index <= endindex; index++) {
            arrayindex = arrayindex + 1;
            data[index] = v[arrayindex];
        }
        onModify();
    }

    @Override
    public void setMaximumRange(double maxRange) {
        SeriesMath.setMaximumRange(data, maxRange);
    }

    @Override
    public void setSamprate(double samprate) {
        this.samprate = samprate;
    }

    /**
     * Sets the time attribute of the CssSeismogram object
     *
     * @param v
     *            The new time value
     */
    public void setTime(TimeT v) {
        time = new TimeT(v);
    }

    /**
     * Shifts the values of this time series by the number of specified samples
     * and returns the result as a new time series. this method also modifies
     * the origin time of the new time series as appropriate.
     *
     * @param samples
     *            The number of samples to shift. a value less than 0 shifts
     *            data left, and greater than 0 shifts data right.
     * @return a new timeseries, based on this one, shifted by the specified
     *         number of samples. The new time series will have a length
     *         different than that of this instance.
     */
    public TimeSeries shift(int samples) {
        return shift(samples, false);
    }

    /**
     * Shifts the values of this time series by the number of specified samples
     * and returns the result as a new time series. this method also modifies
     * the origin time of the new time series as appropriate.
     *
     * @param samples
     *            The number of samples to shift. a value less than 0 shifts
     *            data left, and greater than 0 shifts data right.
     * @param keepLength
     *            should the original time series data length remain unchanged
     *            regardless of shift?
     * @return a new timeseries, based on this one, shifted by the specified
     *         number of samples
     */
    public TimeSeries shift(int samples, boolean keepLength) {
        TimeSeries timeseries = new TimeSeries(this);

        if (samples == 0) {
            return timeseries;
        }

        int absSamples = Math.abs(samples);
        float[] data1 = timeseries.getData();
        final int originalLength = timeseries.getNsamp();
        if (samples < 0) {
            final int newLength;
            if (!keepLength) {
                newLength = originalLength - absSamples;
            } else {
                newLength = originalLength;
            }

            if (newLength < 1) {
                throw new IllegalArgumentException("Cannot shift by more than series length.");
            }

            // Reference: arraycopy(src,srcPos,dest,destPos,length)
            float[] data2 = new float[newLength];
            final int lengthToCopy = originalLength - absSamples;
            System.arraycopy(data1, absSamples, data2, 0, lengthToCopy);

            if (keepLength) {
                // fill in / pad end of buffer with last sample value
                // Reference:  fill(array,fromIndex,toIndex,value)
                Arrays.fill(data2, lengthToCopy, data2.length - 1, data1[data1.length - 1]);
            }

            TimeT newTime = new TimeT(this.getTimeAsDouble() + (absSamples / this.samprate));

            timeseries = new TimeSeries(data2, samprate, newTime);
        } else {
            final int newLength;
            if (!keepLength) {
                newLength = originalLength + absSamples;
            } else {
                newLength = originalLength;
            }

            float[] data2 = new float[newLength];

            // Reference: arraycopy(src,srcPos,dest,destPos,length)
            final int lengthToCopy = originalLength - absSamples;
            System.arraycopy(data1, 0, data2, absSamples, lengthToCopy);

            // pad the new samples at the start of the buffer with what was the orginal starting sample value
            float startingSampleValue = data1[0];
            Arrays.fill(data2, 0, absSamples, startingSampleValue);

            // adjust the time for the shift
            TimeT newTime = new TimeT(this.getTimeAsDouble() - (absSamples / this.samprate));

            timeseries = new TimeSeries(data2, samprate, newTime);
        }

        return timeseries;
    }

    public void stretch(double interpolationfactor) {
        // TODO is it correct to compare the interpolationfactor to samprate like this?
        if ((interpolationfactor > 0.) && (Math.abs(interpolationfactor - samprate) > EPSILON)) {
            double multiplier = interpolationfactor * samprate;
            data = SeriesMath.interpolate(0., 1. / samprate, data, 1. / multiplier);
            onModify();
        }
    }

    public TimeSeries subtract(TimeSeries other) {
        TimeSeries.BivariateFunction f = new TimeSeries.BivariateFunction() {
            @Override
            public double eval(double x, double y) {
                return x - y;
            }
        };
        return intersect(other, f);
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Samprate = ");
        s.append(getSamprate());
        s.append(", Time = ");
        s.append(getTime());
        s.append(", Nsamps = ");
        s.append(getNsamp());
        return s.toString();
    }

    @Override
    public void triangleTaper(double taperPercent) {
        SeriesMath.triangleTaper(data, taperPercent);
        onModify();
    }

    public TimeSeries trim() {
        int start = 0;
        for (int i = start; data[i] == 0.0 && i < data.length; i++) {
            start = i + 1;
        }

        int end = data.length - 1;
        for (int j = end; data[j] == 0.0 && j >= 0; j--) {
            end = j - 1;
        }

        return crop(start, end);
    }

    public void trimTo(Epoch epoch) {
        if (!(getTime().equals(epoch.getTime()) && getEndtime().equals(epoch.getEndtime()))) {
            cut(epoch.getTime(), epoch.getEndtime());
        }
    }

    public TimeSeries union(TimeSeries other, boolean ignoreMismatch) throws MergeException {
        if (other.isEmpty() || !rateIsComparable(other)) {
            return this;
        } else if (this.isEmpty()) {
            return other;
        } else {
            double start = getTime().getEpochTime();
            double otherStart = other.getTime().getEpochTime();
            double end = getEndtime().getEpochTime();
            double otherEnd = other.getEndtime().getEpochTime();

            double minStart = Math.min(start, otherStart);
            double maxEnd = Math.max(end, otherEnd);
            int npts = getPointsIn(maxEnd - minStart);

            // Ideally only the zero-shift is required, but in case of one-off error, try forward and backward shifts
            int[] shifts = { 0, -1, 1 };
            if (start == minStart) {
                for (int shift : shifts) {
                    int mpts = npts + shift;
                    float[] newData = new float[mpts];
                    int destOffset = (int) Math.round((otherStart - minStart) * other.getSamprate()) + shift;
                    if (merge(other.data, data, newData, destOffset, ignoreMismatch)) {
                        return new TimeSeries(newData, samprate, new TimeT(minStart));
                    }
                }
            } else {
                for (int shift : shifts) {
                    int mpts = npts + shift;
                    float[] newData = new float[mpts];
                    int destOffset = (int) Math.round((start - minStart) * samprate) + shift;
                    if (merge(data, other.data, newData, destOffset, ignoreMismatch)) {
                        return new TimeSeries(newData, samprate, new TimeT(minStart));
                    }
                }
            }

            throw new MergeException("Could not merge segments using shifts of (-1, 0, 1)");
        }
    }

    protected void doNormalize(double scale) {
        // First remove the mean value
        RemoveMean();

        if (scale != 0.f) {
            MultiplyScalar(1 / scale);
        }
    }

    protected TimeSeries intersect(TimeSeries other, TimeSeries.BivariateFunction f) {
        if (!rateIsComparable(other)) {
            other = new TimeSeries(other);
            other.interpolate(samprate);
        }

        TimeT start = new TimeT(Math.max(getTime().getEpochTime(), other.getTime().getEpochTime()));
        TimeT end = new TimeT(Math.min(getEndtime().getEpochTime(), other.getEndtime().getEpochTime()));

        float[] overlap;
        if (start.lt(end)) {
            float[] section = getSubSection(start, end);
            float[] otherSection = other.getSubSection(start, end);

            overlap = new float[Math.min(section.length, otherSection.length)];
            for (int i = 0; i < overlap.length; i++) {
                overlap[i] = (float) f.eval(section[i], otherSection[i]);
            }
        } else {
            overlap = new float[0];
        }

        return new TimeSeries(overlap, samprate, start);
    }

    private double getUnroundedTimeIndex(double epochtime) {
        double dataStart = time.getEpochTime();
        return (epochtime - dataStart) * samprate;
    }

    public enum Norm {

        EXTREMUM, MEAN, MIN, MAX, DELTA, RMS
    }

    public interface SeriesListener {

        public void dataChanged(float[] data);
    }

    protected interface BivariateFunction {

        public double eval(double x, double y);
    }
}
