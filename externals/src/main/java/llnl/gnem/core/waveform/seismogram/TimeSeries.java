/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.Locale;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;

import llnl.gnem.core.signalprocessing.filter.ButterworthFilter;
import llnl.gnem.core.signalprocessing.filter.IIRFilter;
import llnl.gnem.core.util.Epoch;
import llnl.gnem.core.util.PairT;
import llnl.gnem.core.util.Passband;
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

public class TimeSeries implements Comparable<TimeSeries>, Serializable, Cloneable, SeismicSignal {
    private static final Logger log = LoggerFactory.getLogger(TimeSeries.class);
    public static final double EPSILON = 0.0000001;
    private static final int MIN_WINDOW_SAMPLES = 10;
    private static final long serialVersionUID = 1L;

    private float[] data;
    private final Collection<Epoch> dataGaps; // TODO these are not being properly updated for in-place modifications
    private final Collection<TimeSeries.SeriesListener> listeners;
    private double samprate = 1.0;
    private SampleStatistics statistics;
    private TimeT time;
    private double timeOffset;

    private double allowableSampleRateError = 0.005;
    private String identifier;

    /**
     * no-arg constructor only for Serialization
     */
    public TimeSeries() {
        this.dataGaps = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.timeOffset = 0.0;
    }

    public TimeSeries(final float[] data, final double samprate, final TimeT time) {
        this(data, samprate, time, true);
    }

    public TimeSeries(final BinaryData data, final double samprate, final TimeT time) {
        this(data.getFloatData(), samprate, time, false);
    }

    public TimeSeries(final TimeSeries s) {
        this.identifier = s.getIdentifier();
        data = s.data.clone();
        samprate = s.samprate;
        time = new TimeT(s.time);
        this.dataGaps = new ArrayList<>(s.dataGaps);
        listeners = new ArrayList<>(s.listeners);
        statistics = null;
        this.timeOffset = s.getZeroTimeOffsetSeconds();
    }

    private TimeSeries(final float[] data, final double samprate, final TimeT time, final boolean clone) {
        this.data = clone ? data.clone() : data;
        this.samprate = samprate;
        this.time = new TimeT(time);
        dataGaps = findDataGaps(data, time, samprate);
        listeners = new ArrayList<>();
        statistics = null;
        timeOffset = 0.0;
    }

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
    public static double getPeakToPeakAmplitude(final float[] data, final double sampleInterval, final double period) {
        final int N = data.length;
        if (N < 1) {
            throw new IllegalArgumentException("Cannot compute PeakToPeak amplitude on empty array.");
        }
        double result = 0.0;
        final int SampsInWindow = (int) Math.round(period / sampleInterval) + 1;
        final int LastWindowStart = N - SampsInWindow;
        if (LastWindowStart <= 0) {
            return getWindowPeakToPeak(data, 0, N);
        } else {
            for (int j = 0; j <= LastWindowStart; ++j) {
                final double p2p = getWindowPeakToPeak(data, j, SampsInWindow);
                if (p2p > result) {
                    result = p2p;
                }
            }
        }
        return result;
    }

    public static <T extends TimeSeries> PairT<T, T> rotateTraces(final T seis1, final T seis2, final double theta) {
        final float[] data1 = seis1.getData();
        final float[] data2 = seis2.getData();
        SeriesMath.rotate(data1, data2, theta, true, true);
        seis1.setData(data1);
        seis2.setData(data2);
        return new PairT<>(seis1, seis2);
    }

    public void setSampleRateErrorThreshold(final double value) {
        allowableSampleRateError = value;
    }

    private static Collection<Epoch> findDataGaps(final float[] data, final TimeT time, final double samprate) {
        final Collection<Epoch> result = new ArrayList<>();
        final int minGapLength = 5;
        boolean inGap = false;
        int gapStart = -1;
        int gapEnd = -1;
        for (int j = 0; j < data.length; ++j) {
            final float value = data[j];
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

    private static double getWindowPeakToPeak(final float[] data, final int idx, final int Nsamps) {
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

    private static Epoch makeGapEpoch(final TimeT time, final double samprate, final int gapStart, final int gapEnd) {
        final double startOffset = gapStart / samprate;
        final double endOffset = gapEnd / samprate;
        final TimeT start = new TimeT(time.getEpochTime() + startOffset);
        final TimeT end = new TimeT(time.getEpochTime() + endOffset);
        return new Epoch(start, end);
    }

    private static boolean merge(final float[] data1, final float[] data2, final float[] newData, final int destOffset, final boolean ignoreMismatch) {
        try {
            // First copy samples from the earlier-starting trace to the destination trace. Nothing to merge here.
            final int maxIdx = Math.min(destOffset, data2.length);
            System.arraycopy(data2, 0, newData, 0, maxIdx);

            // Now merge the overlapped portions...
            final int maxMergeIndexBound = Math.min(destOffset + data1.length, data2.length);
            for (int j = destOffset; j < maxMergeIndexBound; ++j) {
                newData[j] = mergeSamples(data2[j], data1[j - destOffset], ignoreMismatch);
            }

            // Copy remaining samples from the input arrays (whichever has samples after their common samples)
            if (maxMergeIndexBound < data2.length) {
                System.arraycopy(data2, maxMergeIndexBound, newData, maxMergeIndexBound, data2.length - maxMergeIndexBound);
            } else if (maxMergeIndexBound < data1.length + destOffset) {
                for (int j = maxMergeIndexBound; j < data1.length + destOffset; ++j) {
                    final int idx = j - destOffset;
                    if (idx >= 0 && idx < data1.length && j < newData.length) {
                        newData[j] = data1[idx];
                    }
                }
            }
            return true;
        } catch (final MergeException e) {
            return false;
        }
    }

    private static float mergeSamples(final float value1, final float value2, final boolean ignoreMismatch) throws MergeException {
        if (value1 == value2) {
            return value1;
        } else if (value1 == 0) {
            return value2;
        } else if (value2 == 0) {
            return value1;
        } else if (ignoreMismatch) {
            return (value1 + value2) / 2;
        } else {
            throw new MergeException(String.format("Overlapped data have different values (%f vs %f)!", value1, value2));
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
    public boolean AddAlignedSeismogram(final TimeSeries otherseis) {
        if (otherseis == null) {
            return false;
        }

        if (!rateIsComparable(otherseis)) {
            return false;
        }

        final TimeT starttime = getTime();
        final TimeT endtime = getEndtime();

        try {
            final float[] otherdata = otherseis.getSubSection(starttime, endtime);

            for (int ii = 0; ii < data.length; ii++) {
                data[ii] = data[ii] + otherdata[ii];
            }
        } catch (final Exception e) {
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
    public void AddScalar(final double value) {
        SeriesMath.addScalar(data, value);
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
    public boolean AddSeismogram(final TimeSeries otherseis) {
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
    public void MultiplyScalar(final double value) {
        SeriesMath.multiplyScalar(data, value);
        onModify();
    }

    /**
     * Remove the mean of the time series of this CssSeismogram
     */
    @Override
    public void RemoveMean() {
        SeriesMath.removeMean(data);
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
     * Replaces each point in the Seismogram its signed sqrt Note: values LT 0
     * are returned -1* sqrt(abs(value)).
     */
    @Override
    public void SignedSqrt() {
        data = SeriesMath.signedSqrt(data);
        onModify();
    }

    /**
     * Replaces each point in the Seismogram its signed square value Note:
     * values LT 0 are returned -1* value*value.
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
     * the data with it's smoothed result Note halfwidth refers to number of
     * samples, not seconds
     *
     * @param halfwidth
     *            half width in samples.
     */
    @Override
    public void Smooth(final int halfwidth) {
        data = SeriesMath.meanSmooth(data, halfwidth);//TODO note the SeriesMath.MeanSmooth() method should replace data with the smoothed version, but this isn't happening. Fix
        onModify();
    }

    /**
     * Replaces each point in the Seismogram with its sqrt Note: values LT 0 are
     * returned 0.
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
    public void Taper(final double TaperPercent) {
        SeriesMath.taper(data, TaperPercent);
        onModify();
    }

    public void WriteASCIIfile(final String filename) throws IOException {
        try (FileOutputStream out = new FileOutputStream(filename); BufferedOutputStream bout = new BufferedOutputStream(out); PrintStream pout = new PrintStream(bout);) {
            for (int j = 0; j < data.length; ++j) {
                pout.println(String.valueOf((j / samprate)) + "   " + data[j]);
            }
        }
    }

    public TimeSeries add(final TimeSeries other) {
        final TimeSeries.BivariateFunction f = (x, y) -> x + y;
        return intersect(other, f);
    }

    public void addInPlace(final TimeSeries other) {
        if (!rateIsComparable(other)) {
            final String msg = String.format("Seismograms have different sample rates! (%s)  -  (%s)", this.toString(), other.toString());
            throw new IllegalStateException(msg);
        }
        final double myStart = time.getEpochTime();
        final double otherStart = other.getTimeAsDouble();
        final double earliest = myStart < otherStart ? myStart : otherStart;

        final double myEnd = getEndtime().getEpochTime();
        final double otherEnd = other.getEndtime().getEpochTime();
        final double latest = myEnd > otherEnd ? myEnd : otherEnd;

        final double timeRange = latest - earliest;
        long nsamps = Math.round(timeRange * samprate) + 1;

        final int myOffset = (int) Math.round((myStart - earliest) * getSamprate());
        final int myLast = myOffset + data.length - 1;
        if (myLast > nsamps - 1) {
            nsamps = myLast + 1l;
        }
        final int otherOffset = (int) Math.round((otherStart - earliest) * samprate);

        final int otherLast = otherOffset + other.data.length - 1;
        if (otherLast > nsamps - 1) {
            nsamps = otherLast + 1l;
        }
        final float[] result = new float[(int) nsamps];
        System.arraycopy(data, 0, result, myOffset, data.length);
        for (int j = 0; j < other.data.length; ++j) {
            result[j + otherOffset] += other.data[j];
        }
        data = result;
        time = new TimeT(earliest);
        onModify();
    }

    public void addListener(final TimeSeries.SeriesListener listener) {
        listeners.add(listener);
    }

    public TimeSeries append(final TimeSeries other) {
        if (!rateIsComparable(other)) {
            log.warn("Seismograms have different sample rates! {} - {}", this, other);
        }
        final double expectedAppendeeStart = this.getEndtime().getEpochTime() + getDelta();
        final double actualAppendeeStart = other.getTimeAsDouble();

        int newDataNpts = data.length + other.data.length;
        int newDataOffset = data.length;
        final long errorInSamples = Math.round((expectedAppendeeStart - actualAppendeeStart) * samprate);
        newDataNpts -= (int) errorInSamples;
        newDataOffset -= (int) errorInSamples;
        if (newDataNpts < 1) {
            final String msg = String.format("After correcting for start time mismatch " + "of %d samples, new data array is less than 1 sample in length", errorInSamples);
            throw new IllegalStateException(msg);
        }
        final float[] tmp = new float[newDataNpts];
        System.arraycopy(data, 0, tmp, 0, data.length);

        System.arraycopy(other.data, 0, tmp, newDataOffset, other.data.length);

        if (errorInSamples < 0) {
            final int sampsToSet = (int) Math.abs(errorInSamples);
            for (int j = data.length; j < data.length + sampsToSet + 1; ++j) {
                tmp[j] = data[data.length - 1];
            }
        }
        double rate = samprate;
        if (other.samprate != samprate) {
            final double otherEndTime = other.getEndtimeAsDouble();
            final double newDuration = otherEndTime - time.getEpochTime();
            final double newDelta = newDuration / (newDataNpts - 1);
            rate = 1 / newDelta;
        }
        return new TimeSeries(tmp, rate, time);

    }

    @Override
    public int compareTo(final TimeSeries other) {
        final double diff = time.getEpochTime() - other.time.getEpochTime();
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
        final double v = getStatistics().getMean() - getStatistics().getMedian();
        final double range = getStatistics().getRange();
        if (range > 0) {
            return v / range;
        } else {
            return 10000;
        }
    }

    @Override
    public boolean contains(final Epoch epoch, final boolean allowGaps) {
        final Epoch myEpoch = new Epoch(time, getEndtime());
        if (myEpoch.isSuperset(epoch)) {
            if (allowGaps) {
                return true;
            } else {
                for (final Epoch gapEpoch : dataGaps) {
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

    public TimeSeries crop(final Epoch epoch) {
        return crop(epoch.getTime(), epoch.getEndtime());
    }

    public TimeSeries crop(TimeT start, TimeT end) {
        final TimeT currentStart = getTime();
        if (currentStart.gt(start)) {
            start = currentStart;
        }

        final TimeT currentEnd = getEndtime();
        if (currentEnd.lt(end)) {
            end = currentEnd;
        }

        return new TimeSeries(getSubSection(start, end), getSamprate(), start);
    }

    public TimeSeries crop(final int start, final int end) {
        final TimeSeries series = new TimeSeries(this);
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
    public void cut(final TimeT start, final TimeT end) {
        log.trace(
                "Trying to cut seismogram, startcut {}, endcut {}, starttime {}, endtime {}",
                    start.getMilliseconds(),
                    end.getMilliseconds(),
                    getTime().getMilliseconds(),
                    getEndtime().getMilliseconds());
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
        final double duration = E.getEpochTime() - S.getEpochTime();
        data = getSubSection(S, duration);
        final double dataStart = time.getEpochTime();

        final int startIndex = (int) Math.round((S.getEpochTime() - dataStart) * samprate);
        final TimeT actualNewStart = new TimeT(getTimeAsDouble() + startIndex / samprate);

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
    public void cut(final double start, final double end) {
        final TimeT startT = time.add(start);
        final TimeT endT = time.add(end);
        cut(startT, endT);
    }

    public void cut(final int idx0, final int idx1) {
        final int maxIdx = data.length - 1;
        if (idx0 < 0 || idx0 > maxIdx) {
            throw new IllegalStateException("Illegal value for start cut index: " + idx0);
        }

        if (idx1 > maxIdx) {
            throw new IllegalStateException("Illegal value for end cut index: " + idx1);
        }
        if (idx1 - idx0 < 0) {
            throw new IllegalStateException("End index  must be >= start index : ");
        }

        final int length = idx1 - idx0 + 1;

        final float[] tmp = new float[length];
        System.arraycopy(data, idx0, tmp, 0, length);
        data = tmp;

        final double deltaT = idx0 / samprate;
        time = new TimeT(time.getEpochTime() + deltaT);
    }

    public void cutAfter(final TimeT end) {
        cut(getTime(), end);
    }

    public void cutBefore(final TimeT start) {
        cut(start, getEndtime());
    }

    /**
     * <p>
     * Decimate the data (Note this should be interchangeable with the
     * interpolate methods)
     * </p>
     * <p>
     * The data series it is decimated so that only every Nth point is retained
     * where N is the decimationfactor
     * </p>
     * Note the samprate and number of points in the data series changes
     *
     * @param decimationfactor
     *            The amount by which to decimate the series.
     */
    public void decimate(final int decimationfactor) {
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
        SeriesMath.differentiate(data, samprate);
        onModify();
    }

    public TimeSeries divide(final TimeSeries other) {
        final TimeSeries.BivariateFunction f = (x, y) -> x / y;
        return intersect(other, f);
    }

    public void Envelope() {
        this.data = SeriesMath.envelope(data);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof TimeSeries)) {
            return false;
        }
        final TimeSeries other = (TimeSeries) obj;
        final double diff = time.getEpochTime() - other.time.getEpochTime();
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

    public void fill(final float[] buffer, final int inc, final double start, final double end) {
        final int startIndex = getIndexForTime(start);
        final int endIndex = getIndexForTime(end);

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
    public void filter(final double lc, final double hc) {
        filter(lc, hc, false);
    }

    @Override
    public void filter(final double lc, final double hc, final boolean twoPass) {
        filter(2, Passband.BAND_PASS, lc, hc, twoPass);
    }

    @Override
    public void filter(final int order, final Passband passband, final double cutoff1, final double cutoff2, final boolean two_pass) {
        final double dt = 1.0 / samprate;
        final IIRFilter filt = new ButterworthFilter(order, passband, cutoff1, cutoff2, dt);
        filt.initialize();
        filt.filter(data);
        if (two_pass) {
            SeriesMath.reverseArray(data);
            filt.initialize();
            filt.filter(data);
            SeriesMath.reverseArray(data);
        }
        onModify();
    }

    @Override
    public DiscontinuityCollection findDiscontinuities(final int winLength, final double factor) {
        return SeriesMath.findDiscontinuities(data, samprate, winLength, factor);
    }

    /**
     * Gets the time-series data of the CssSeismogram as a float array
     *
     * @return The data array
     */
    @Override
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
    public double getDistinctValueRatio(final int numSamples) {
        if (data.length < 1) {
            return 0;
        }
        final Set<Float> values = new HashSet<>();
        final int nsamples = Math.min(numSamples, data.length);
        final RandomAlgorithm algorithm = RandomAlgorithmFactory.getAlgorithm();
        for (int j = 0; j < nsamples; ++j) {
            final int k = algorithm.getBoundedInt(0, data.length - 1);
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
        final float max = getMax();
        final float absmin = Math.abs(getMin());

        float result;

        if (max >= absmin) {
            result = max;
        } else {
            result = absmin;
        }

        return result;

    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public void setIdentifier(final String identifier) {
        this.identifier = identifier;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        // TODO Auto-generated method stub
        return super.clone();
    }

    @Override
    public int getIndexForTime(final double epochtime) {
        final double dataStart = time.getEpochTime();
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
        final PairT<Integer, Float> value = SeriesMath.getMaxIndex(data);
        final double offset = value.getFirst() / samprate;
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

    public double getNormalizedRMSE(final TimeSeries other) {
        double error = getRMSE(other);

        final double norm = Math.abs(getMax() - getMin());
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
    public double getPeakToPeakAmplitude(final double period) {
        return getPeakToPeakAmplitude(data, 1.0 / samprate, period);
    }

    @Override
    public int getPointsIn(final TimeT start, final TimeT end) {
        return getPointsIn(end.getEpochTime() - start.getEpochTime());
    }

    @Override
    public int getPointsIn(final double timeRange) {
        return (int) Math.round(timeRange * samprate) + 1;
    }

    @Override
    public double getPower() {
        double power = 0.0;
        for (final double v : data) {
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

    public double getRMSE(final TimeSeries other) {
        final float[] t = data;
        final float[] o = other.data;

        double error = 0.0;
        for (int i = 0; i < t.length; i++) {
            final double residual = Math.abs(t[i] - o[i]);
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

    public double getRange(final Epoch epoch) {
        final float[] subSection = this.getSubSection(epoch.getTime(), epoch.getEndtime());
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
    public double getSnr(final double pickEpochTime, final double preSeconds, final double postSeconds) {
        final double availablePreSeconds = pickEpochTime - getTimeAsDouble();
        double samples = availablePreSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }
        final double availablePostSeconds = getEndtime().getEpochTime() - pickEpochTime;
        samples = availablePostSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }
        return SeriesMath.getSnr(data, samprate, getTime().getEpochTime(), pickEpochTime, preSeconds, postSeconds);
    }

    @Override
    public double getSnr(final double pickEpochTime, final Epoch epoch, final double preSeconds, final double postSeconds) {
        final double start = Math.max(getTimeAsDouble(), epoch.getStart());
        final double end = Math.min(getEndtime().getEpochTime(), epoch.getEnd());

        final double availablePreSeconds = pickEpochTime - start;
        double samples = availablePreSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }
        final double availablePostSeconds = end - pickEpochTime;
        samples = availablePostSeconds * samprate;
        if (samples < MIN_WINDOW_SAMPLES) {
            return -1;
        }

        final int pick = getIndexForTime(pickEpochTime);
        final int startIndex = getIndexForTime(Math.max(start, pickEpochTime - preSeconds));
        final int endIndex = getIndexForTime(Math.min(end, pickEpochTime + postSeconds));
        return SeriesMath.getSnr(data, pick, startIndex, endIndex);
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
     * The start and end times must be within the Seismogram's time window or an
     * IllegalArgument exception will be thrown
     *
     * @param start
     *            the starting time of the subsection
     * @param end
     *            the ending time of the subsection
     * @return the subsection float[] array
     */
    public float[] getSubSection(final TimeT start, final TimeT end) {
        return getSubSection(start, end, null);
    }

    public float[] getSubSection(final TimeT start, final TimeT end, final float[] result) {
        final double duration = end.getEpochTime() - start.getEpochTime();
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
    public float[] getSubSection(final TimeT start, final double duration) {
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
    public float[] getSubSection(final double startEpoch, final double requesteduration) {
        return getSubSection(startEpoch, requesteduration, null);
    }

    public float[] getSubSection(final double startEpoch, final double requestedDuration, float[] result) {
        final int Nsamps = data.length;
        if (Nsamps >= 1) {
            final double duration = Math.abs(requestedDuration);
            int startIndex = getIndexForTime(startEpoch);
            int endIndex = getIndexForTime(startEpoch + duration);

            if (startIndex < 0) {
                startIndex = 0;
            }

            if (endIndex >= data.length) {
                endIndex = data.length - 1;
            }

            final int sampsRequired = endIndex - startIndex + 1;
            if (result == null) {
                result = new float[sampsRequired];
            }
            try {
                System.arraycopy(data, startIndex, result, 0, sampsRequired);
            } catch (final ArrayIndexOutOfBoundsException ex) {
                final String msg = String.format("Requested %d samples from seismogram of length %d from index %d into buffer of length %d", sampsRequired, data.length, startIndex, result.length);
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
    public float[] getSubSection(final int startIndex, final int sampsRequired) {
        final float[] result = new float[sampsRequired];
        try {
            System.arraycopy(data, startIndex, result, 0, sampsRequired);
        } catch (final ArrayIndexOutOfBoundsException ex) {
            final String msg = String.format("Requested %d samples from seismogram of length %d from index %d into buffer of length %d", sampsRequired, data.length, startIndex, result.length);
            throw new IllegalStateException(msg, ex);
        }
        return result;
    }

    public double getSubsectionStartTime(final double startEpoch) {
        final int startIndex = getIndexForTime(startEpoch);
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
    public float getValueAt(final double epochtime) {
        if (epochtime < time.getEpochTime()) {
            throw new IllegalArgumentException(String.format("Requested time (%s) is before seismogram start time!", new TimeT(epochtime).toString()));
        }
        if (epochtime > this.getEndtime().getEpochTime()) {
            throw new IllegalArgumentException(String.format("Requested time (%s) is after seismogram end time!", new TimeT(epochtime).toString()));

        }
        final double unroundedIndex = getUnroundedTimeIndex(epochtime);
        final int x1 = (int) unroundedIndex;
        if (unroundedIndex == x1) {
            return data[x1];
        } else {
            final int x2 = x1 + 1;
            final float y1 = data[x1];
            final float y2 = data[x2];
            return (float) (y1 + (unroundedIndex - x1) * (y2 - y1));
        }
    }

    public float getValueAt(final int j) {
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
    public boolean hasFlatSegments(final int minsegmentlength) {
        return SeriesMath.hasFlatSegments(data, minsegmentlength);
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
     *            Note the samprate changes and the number of points in the data
     *            series changes based on the new desired sample rate
     *            </p>
     */
    public void interpolate(final double newsamprate) {
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

    public boolean isSubset(final TimeSeries other) {
        return getTime().ge(other.getTime()) && getEndtime().le(other.getEndtime());
    }

    public TimeSeries multiply(final TimeSeries other) {
        final TimeSeries.BivariateFunction f = (x, y) -> x * y;
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
    public void normalize(final TimeSeries.Norm norm) {
        switch (norm) {
        case EXTREMUM:
            doNormalize(SeriesMath.getExtremum(data));
            break;
        case MEAN:
            doNormalize(SeriesMath.getMean(SeriesMath.abs(data)));
            break;
        case MIN:
            doNormalize(-1 * SeriesMath.getMin(data));
            break;
        case MAX:
            doNormalize(SeriesMath.getMax(data));
            break;
        case DELTA:
            doNormalize(SeriesMath.getMax(data) - SeriesMath.getMin(data));
            break;
        case RMS:
            doNormalize(SeriesMath.getRMS(data));
            break;
        }
    }

    public void normalize(final double value) {
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
    public void normalize(final String value) {
        try {
            // attempt to parse as a Number
            final Double dvalue = Double.parseDouble(value);
            normalize(dvalue);
        } catch (final Exception e) {
            try {
                // attempt to parse as a Norm type
                final TimeSeries.Norm type = TimeSeries.Norm.valueOf(value.toUpperCase(Locale.ENGLISH));
                normalize(type);
            } catch (final Exception ee) {
            }
        }
    }

    public void onModify() {
        // Must recalculate statistics, but do so lazily
        statistics = null;

        for (final TimeSeries.SeriesListener listener : listeners) {
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
    public boolean rateIsComparable(final TimeSeries other) {
        final double fileOneDelta = this.getDelta();
        final double fileTwoDelta = other.getDelta();
        final double percentError = 100 * Math.abs((fileOneDelta - fileTwoDelta) / fileOneDelta);
        final boolean isSameSampRate = percentError < allowableSampleRateError;
        return isSameSampRate;
    }

    /**
     * <p>
     * Remove glitches from the seismogram where glitches are defined by data
     * that exceed a threshhold above the variance defined by a moving window
     * </p>
     * value = Math.abs((data[j] - median)); if (value GT Threshhold *
     * Math.sqrt(variance)) replace data[j] with the median value
     *
     * @param Threshhold
     *            - the threshhold value
     */
    @Override
    public void removeGlitches(final double Threshhold) {
        SeriesMath.removeGlitches(data, Threshhold);
        onModify();
    }

    public void removeListener(final TimeSeries.SeriesListener listener) {
        listeners.remove(listener);
    }

    /**
     * Remove a linear trend from the time series data of this CssSeismogram.
     */
    @Override
    public void removeTrend() {
        SeriesMath.removeTrend(data);
        onModify();
    }

    public void resample(final double newRate) {
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
        SeriesMath.reverseArray(data);
        onModify();
    }

    public void reverseAt(final TimeT mirrorTime) {
        final TimeT endtime = getEndtime();
        final double shifttime = endtime.subtract(mirrorTime).getEpochTime();

        final TimeT newbegintime = mirrorTime.add(-1 * shifttime);
        reverse();
        setTime(newbegintime);
    }

    @Override
    public void scaleTo(final double min, final double max) {
        final double myMax = getStatistics().getMax();
        final double myMin = getStatistics().getMin();
        final double myRange = myMax - myMin;
        final double requiredRange = max - min;
        final double scale = requiredRange / myRange;
        SeriesMath.multiplyScalar(data, scale);
        onModify();
    }

    /**
     * Sets the data array of the CssSeismogram object
     *
     * @param v
     *            The new data value
     */
    public void setData(final float[] v) {
        data = v.clone();
        onModify();
    }

    /**
     * Sets the value of the data at a particular point to a value
     *
     * @param v
     * @param index
     */
    public void setDataPoint(final float v, final int index) {
        data[index] = v;
        onModify();
    }

    /**
     * Sets the data at a collection of points
     *
     * @param v
     * @param startindex
     */
    public void setDataPoints(final float[] v, final int startindex) {
        final int maxindex = getNsamp() - 1;
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
    public void setMaximumRange(final double maxRange) {
        SeriesMath.setMaximumRange(data, maxRange);
    }

    @Override
    public void setSamprate(final double samprate) {
        this.samprate = samprate;
    }

    /**
     * Sets the time attribute of the CssSeismogram object
     *
     * @param v
     *            The new time value
     */
    public void setTime(final TimeT v) {
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
    public TimeSeries shift(final int samples) {
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
    public TimeSeries shift(final int samples, final boolean keepLength) {
        TimeSeries timeseries = new TimeSeries(this);

        if (samples == 0) {
            return timeseries;
        }

        final int absSamples = Math.abs(samples);
        final float[] data1 = timeseries.getData();
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
            final float[] data2 = new float[newLength];
            final int lengthToCopy = originalLength - absSamples;
            System.arraycopy(data1, absSamples, data2, 0, lengthToCopy);

            if (keepLength) {
                // fill in / pad end of buffer with last sample value
                // Reference:  fill(array,fromIndex,toIndex,value)
                Arrays.fill(data2, lengthToCopy, data2.length - 1, data1[data1.length - 1]);
            }

            final TimeT newTime = new TimeT(this.getTimeAsDouble() + (absSamples / this.samprate));

            timeseries = new TimeSeries(data2, samprate, newTime);
        } else {
            final int newLength;
            if (!keepLength) {
                newLength = originalLength + absSamples;
            } else {
                newLength = originalLength;
            }

            final float[] data2 = new float[newLength];

            // Reference: arraycopy(src,srcPos,dest,destPos,length)
            final int lengthToCopy = originalLength - absSamples;
            System.arraycopy(data1, 0, data2, absSamples, lengthToCopy);

            // pad the new samples at the start of the buffer with what was the orginal starting sample value
            final float startingSampleValue = data1[0];
            Arrays.fill(data2, 0, absSamples, startingSampleValue);

            // adjust the time for the shift
            final TimeT newTime = new TimeT(this.getTimeAsDouble() - (absSamples / this.samprate));

            timeseries = new TimeSeries(data2, samprate, newTime);
        }

        return timeseries;
    }

    public void stretch(final double interpolationfactor) {
        // TODO is it correct to compare the interpolationfactor to samprate like this?
        if ((interpolationfactor > 0.) && (Math.abs(interpolationfactor - samprate) > EPSILON)) {
            final double multiplier = interpolationfactor * samprate;
            data = SeriesMath.interpolate(0., 1. / samprate, data, 1. / multiplier);
            onModify();
        }
    }

    public TimeSeries subtract(final TimeSeries other) {
        final TimeSeries.BivariateFunction f = (x, y) -> x - y;
        return intersect(other, f);
    }

    @Override
    public String toString() {
        final StringBuilder s = new StringBuilder("Samprate = ");
        s.append(getSamprate());
        s.append(", Time = ");
        s.append(getTime());
        s.append(", Nsamps = ");
        s.append(getNsamp());
        return s.toString();
    }

    @Override
    public void triangleTaper(final double taperPercent) {
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

    public void trimTo(final Epoch epoch) {
        if (!(getTime().equals(epoch.getTime()) && getEndtime().equals(epoch.getEndtime()))) {
            cut(epoch.getTime(), epoch.getEndtime());
        }
    }

    public TimeSeries union(final TimeSeries other, final boolean ignoreMismatch) throws MergeException {
        if (other.isEmpty() || !rateIsComparable(other)) {
            return this;
        } else if (this.isEmpty()) {
            return other;
        } else {
            final double start = getTime().getEpochTime();
            final double otherStart = other.getTime().getEpochTime();
            final double end = getEndtime().getEpochTime();
            final double otherEnd = other.getEndtime().getEpochTime();

            final double minStart = Math.min(start, otherStart);
            final double maxEnd = Math.max(end, otherEnd);
            final int npts = getPointsIn(maxEnd - minStart);

            // Ideally only the zero-shift is required, but in case of one-off error, try forward and backward shifts
            final int[] shifts = { 0, -1, 1 };
            if (start == minStart) {
                for (final int shift : shifts) {
                    final int mpts = npts + shift;
                    final float[] newData = new float[mpts];
                    final int destOffset = (int) Math.round((otherStart - minStart) * other.getSamprate()) + shift;
                    if (merge(other.data, data, newData, destOffset, ignoreMismatch)) {
                        return new TimeSeries(newData, samprate, new TimeT(minStart));
                    }
                }
            } else {
                for (final int shift : shifts) {
                    final int mpts = npts + shift;
                    final float[] newData = new float[mpts];
                    final int destOffset = (int) Math.round((start - minStart) * samprate) + shift;
                    if (merge(data, other.data, newData, destOffset, ignoreMismatch)) {
                        return new TimeSeries(newData, samprate, new TimeT(minStart));
                    }
                }
            }

            throw new MergeException("Could not merge segments using shifts of (-1, 0, 1)");
        }
    }

    protected void doNormalize(final double scale) {
        // First remove the mean value
        RemoveMean();

        if (scale != 0.f) {
            MultiplyScalar(1 / scale);
        }
    }

    protected TimeSeries intersect(TimeSeries other, final TimeSeries.BivariateFunction f) {
        if (!rateIsComparable(other)) {
            other = new TimeSeries(other);
            other.interpolate(samprate);
        }

        final TimeT start = new TimeT(Math.max(getTime().getEpochTime(), other.getTime().getEpochTime()));
        final TimeT end = new TimeT(Math.min(getEndtime().getEpochTime(), other.getEndtime().getEpochTime()));

        float[] overlap;
        if (start.lt(end)) {
            final float[] section = getSubSection(start, end);
            final float[] otherSection = other.getSubSection(start, end);

            overlap = new float[Math.min(section.length, otherSection.length)];
            for (int i = 0; i < overlap.length; i++) {
                overlap[i] = (float) f.eval(section[i], otherSection[i]);
            }
        } else {
            overlap = new float[0];
        }

        return new TimeSeries(overlap, samprate, start);
    }

    private double getUnroundedTimeIndex(final double epochtime) {
        final double dataStart = time.getEpochTime();
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

    @Override
    public double getZeroTimeOffsetSeconds() {
        return timeOffset;
    }

    @Override
    public void setZeroTimeOffsetSeconds(final double timeOffset) {
        this.timeOffset = timeOffset;
    }
}
