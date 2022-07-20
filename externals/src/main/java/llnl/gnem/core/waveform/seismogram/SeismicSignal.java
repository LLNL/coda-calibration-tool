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

import llnl.gnem.core.util.Epoch;
import llnl.gnem.core.util.Passband;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.seriesMathHelpers.DiscontinuityCollection;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;

public interface SeismicSignal {

    /**
     * Add a scalar to the time series of this CssSeismogram
     *
     * @param value
     *            The scalar value to be added to the time series
     */
    public void AddScalar(double value);

    /**
     * Replaces each point in this Seismogram with its log10 value.
     */
    public void Log10();

    /**
     * Multiply the time series values of this CssSeismogram by a scalar
     * constant.
     *
     * @param value
     *            The scalar value with which to multiply the time series values
     */
    public void MultiplyScalar(double value);

    /**
     * Remove the mean of the time series of this CssSeismogram
     */
    public void RemoveMean();

    /**
     * Remove the median value of the time series of this CssSeismogram
     */
    public void RemoveMedian();

    /**
     * Replaces each point in the Seismogram its signed sqrt
     * <p>
     * </p>
     * Note: values LT 0 are returned -1* sqrt(abs(value)).
     */
    public void SignedSqrt();

    /**
     * Replaces each point in the Seismogram its signed square value
     * <p>
     * </p>
     * Note: values LT 0 are returned -1* value*value.
     */
    public void SignedSquare();

    /**
     * Convert to single bit (+1, -1 or 0)
     */
    public void Signum();

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
    public void Smooth(int halfwidth);

    /**
     * Replaces each point in the Seismogram with its sqrt
     * <p>
     * </p>
     * Note: values LT 0 are returned 0.
     */
    public void Sqrt();

    /**
     * Replaces each point in the Seismogram with its square value
     */
    public void Square();

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
    public void Taper(double TaperPercent);

    public double computeExtremeStat();

    public boolean contains(Epoch epoch, boolean allowGaps);

    /**
     * Differentiate the time series of this CssSeismogram. First two points are
     * differentiated using a forward-difference with error Oh2, Last two points
     * using a backward-difference operator with error Oh2. Remaining points
     * differentiated using a central-difference operator with order Oh4 (page
     * 397 - 399 of Applied Numerical Methods for Digital Computation by James
     * et al. ) Must be at least 4 points in series for this to work.
     */
    public void differentiate();

    public void filter(double lc, double hc);

    public void filter(double lc, double hc, boolean twoPass);

    /**
     * Apply a Butterworth filter to the time series data of this CssSeismogram.
     *
     * @param order
     *            The order of the filter to be applied
     * @param passband
     *            The passband of the filter. Passband is one of
     *            Passband.LOW_PASS, Passband.HIGH_PASS, Passband.BAND_PASS,
     *            Passband.BAND_REJECT
     * @param cutoff1
     *            For BAND_PASS and BAND_REJECT filters this is the low corner
     *            of the filter. For HIGH_PASS and LOW_PASS filters, this is the
     *            single corner frequency
     * @param cutoff2
     *            For BAND_PASS and BAND_REJECT filters, this is the
     *            high-frequency corner. For other filters, this argument is
     *            ignored.
     * @param two_pass
     *            When true, the filter is applied in both forward and reverse
     *            directions to achieve zero-phase.
     */
    public void filter(int order, Passband passband, double cutoff1, double cutoff2, boolean two_pass);

    public DiscontinuityCollection findDiscontinuities(int winLength, double factor);

    public double getDelta();

    public float[] getData();

    public double getDistinctValueRatio(int numSamples);

    /**
     * Gets the endtime attribute of the CssSeismogram object
     *
     * @return The endtime value
     */
    public TimeT getEndtime();

    public Epoch getEpoch();

    public float getExtremum();

    public String getIdentifier();

    public void setIdentifier(String identifier);

    public int getIndexForTime(double epochtime);

    public int getJdate();

    public int getLength();

    public double getLengthInSeconds();

    /**
     * Gets the maximum value of the time series of the CssSeismogram object
     *
     * @return The max value
     */
    public float getMax();

    /**
     * Gets the maximum value of the series and the time offset it occurs at.
     *
     * @return The (time offset at the max in seconds, the max value) of the
     *         series
     */
    public double[] getMaxTime();

    /**
     * Gets the mean value of the time series of the CssSeismogram object
     *
     * @return The mean value
     */
    public double getMean();

    /**
     * Gets the median value of the time series of the CssSeismogram object
     *
     * @return The median value
     */
    double getMedian();

    /**
     * Gets the minimum value of the time series of the BasicSeismogram object
     *
     * @return The min value
     */
    public float getMin();

    public int getNsamp();

    /**
     * Gets the Nyquist Frequency of the CssSeismogram
     *
     * @return The nyquistFreq value
     */
    public double getNyquistFreq();

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
    public double getPeakToPeakAmplitude(double period);

    public int getPointsIn(TimeT start, TimeT end);

    public int getPointsIn(double timeRange);

    public double getPower();

    /**
     * Gets the RMS value of the CssSeismogram's time series.
     *
     * @return The RMS value
     */
    public double getRMS();

    /**
     * Gets the range of the time series
     *
     * @return The statistical range for all values in the data
     */
    public double getRange();

    /**
     * Gets the samprate attribute of the CssSeismogram object
     *
     * @return The samprate value
     */
    double getSamprate();

    /**
     * Gets the segment Length in seconds of the CssSeismogram object
     *
     * @return The segmentLength value
     */
    public double getSegmentLength();

    public double getSnr(double pickEpochTime, double preSeconds, double postSeconds);

    public double getSnr(double pickEpochTime, Epoch epoch, double preSeconds, double postSeconds);

    /**
     * Gets the variance of the time series of the CssSeismogram object
     *
     * @return The variance value
     */
    public double getStDev();

    /**
     * Gets the sum of the time series values of this CssSeismogram
     *
     * @return The sum of the time series values
     */
    public double getSum();

    /**
     * Gets the start time of the CssSeismogram as a TimeT object
     *
     * @return The time value
     */
    public TimeT getTime();

    /**
     * Gets the start time of the CssSeismogram as a double holding the epoch
     * time of the start.
     *
     * @return The CssSeismogram start epoch time value
     */
    public double getTimeAsDouble();

    /**
     * Get the value at a specific point in time
     *
     * @param epochtime
     *            The time expressed as a double epoch time.
     * @return the value at the requested time
     */
    public float getValueAt(double epochtime);

    /**
     * Gets the variance of the time series of the CssSeismogram object
     *
     * @return The variance value
     */
    public double getVariance();

    public boolean isEmpty();

    public void normalize();

    /**
     * Computed the median in O(n) time in contrast to standard O(n lg n) time.
     * The only tradeoff is that the median is not the average of the two center
     * points for series with even length. Rather, the median is arbitrarily
     * selected from amongst one of those two approximate centers.
     *
     * @return The median or pseudo-median for even length series
     */
    public double quickMedian();

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
    public void removeGlitches(double Threshhold);

    /**
     * Remove a linear trend from the time series data of this CssSeismogram.
     */
    public void removeTrend();

    /**
     * Reverse the data series. This method is used in cross correlation
     * routines. Note none of the times are being reset. The user must be
     * careful to understand the implications
     */
    public void reverse();

    public void scaleTo(double min, double max);

    public void setMaximumRange(double maxRange);

    public void setSamprate(double samprate);

    public void triangleTaper(double taperPercent);

    public MinMax getMinMax();

    /**
     * This method returns what the zero time offset of the series is for this
     * segment. This should define where your time axis should set 0 at in the
     * trace relative to the begin time of the segment.
     *
     * @return double Offset defining time in seconds that the segment should be
     *         shifted.
     */
    public double getZeroTimeOffsetSeconds();

    /**
     * This method sets what the zero time offset of the series is for this
     * segment. This should define where your time axis should set 0 at in the
     * trace relative to the begin time of the segment.
     *
     * @param zeroTimeOffsetSeconds
     *            Offset defining time in seconds that the segment should be
     *            shifted.
     */
    public void setZeroTimeOffsetSeconds(double zeroTimeOffsetSeconds);
}
