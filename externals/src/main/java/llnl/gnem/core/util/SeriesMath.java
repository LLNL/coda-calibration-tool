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
package llnl.gnem.core.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.util.ArithmeticUtils;

import llnl.gnem.core.util.NumericalList.DoubleList;
import llnl.gnem.core.util.NumericalList.FloatList;
import llnl.gnem.core.util.seriesMathHelpers.DiscontinuityCollection;
import llnl.gnem.core.util.seriesMathHelpers.DiscontinuityFinder;
import llnl.gnem.core.util.seriesMathHelpers.Glitch;
import llnl.gnem.core.util.seriesMathHelpers.RollingStats;
import llnl.gnem.core.util.seriesMathHelpers.SampleStatistics;

/**
 * A class that provides a number of static methods useful for operating on
 * series data.
 *
 * @author Doug Dodge
 */
public class SeriesMath {

    private static final String LENGTH_MISMATCH = "Length of input x and y arrays is not the same.";
    private static final String EMPTY_ARRAY = "Empty Array!";
    private static final double EPS = 0.00001;
    private static final int R = 0;
    private static final int I = 1;

    /**
     * Element by element addition of two data series of equal length
     *
     * @param data1
     *            - the first data series
     * @param data2
     *            - the second data series
     * @return result[ii] = data1[ii] + data2[ii]
     */
    public static double[] add(double[] data1, double[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        double[] result = new double[data1.length];

        for (int ii = 0; ii < data1.length; ii++) {
            result[ii] = data1[ii] + data2[ii];
        }

        return result;

    }

    /**
     * Add a scalar value to all elements of the array and return a new array
     *
     * @param data
     * @param value
     * @return
     */
    public static double[] add(double[] data, double value) {
        double[] result = new double[data.length];

        for (int ii = 0; ii < data.length; ii++) {
            result[ii] = data[ii] + value;
        }

        return result;
    }

    public static float[] add(float[] data, float value) {
        float[] result = new float[data.length];
        for (int ii = 0; ii < data.length; ii++) {
            result[ii] = data[ii] + value;
        }
        return result;
    }

    public static float[] add(float[] data, double value) {
        return add(data, (float) value);
    }

    /**
     * Add a scalar to the series.
     *
     * @param value
     *            The scalar value to be added to the series
     * @param data
     *            Array containing the series values
     */
    public static void addScalar(float[] data, double value) {
        int NPTS = data.length;
        if (NPTS < 1) {
            return;
        }
        for (int j = 0; j < NPTS; ++j) {
            data[j] += value;
        }
    }

    /**
     * Add a scalar to the series.
     *
     * @param value
     *            The scalar value to be added to the series
     * @param data
     *            Array containing the series values
     */
    public static void addScalar(double[] data, double value) {
        int NPTS = data.length;
        if (NPTS < 1) {
            return;
        }
        for (int j = 0; j < NPTS; ++j) {
            data[j] += value;
        }
    }

    public static void differentiate(float[] data, double samprate) {
        double h = 1.0 / (samprate);
        double h2 = 2 * h;
        double h12 = 12 * h;
        int Nsamps = data.length;
        if (Nsamps < 4) {
            return;
        }
        double[] Tmp = new double[data.length];

        // First do the forward differences...
        for (int j = 0; j < 2; ++j) {
            Tmp[j] = (4 * data[j + 1] - data[j + 2] - 3 * data[j]) / h2;
        }

        // Now do central differences...
        for (int j = 2; j < Nsamps - 2; ++j) {
            Tmp[j] = (8 * data[j + 1] + data[j - 2] - data[j + 2] - 8 * data[j - 1]) / h12;
        }

        // Finally, do the backward differences...
        for (int j = Nsamps - 2; j < Nsamps; ++j) {
            Tmp[j] = (3 * data[j] + data[j - 2] - 4 * data[j - 1]) / h2;
        }
        for (int j = 0; j < Nsamps; ++j) {
            data[j] = (float) Tmp[j];
        }
    }

    public static float[] doubleToFloat(double[] data) {
        float[] result = new float[data.length];

        for (int i = 0; i < data.length; i++) {
            result[i] = (float) data[i];
        }

        return result;
    }

    public static double[] floatToDouble(float[] data) {
        double[] result = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = data[i];
        }
        return result;
    }

    /**
     * Integrate a series in the time domain
     *
     * @param data
     *            Array containing the series values assumed to be evenly
     *            sampled.
     * @param samprate
     *            The sample rate
     */
    public static void Integrate(float[] data, double samprate) {
        double h = 1.0 / (2.0 * samprate);
        int nsamps = data.length;
        double dcur = 0;
        double dlast;
        dlast = 0.0;
        for (int j = 1; j < nsamps; ++j) {
            int j1 = j - 1;
            dcur = dlast + (data[j] + data[j1]) * h;
            data[j1] = (float) dlast;
            dlast = dcur;
        }
        data[nsamps - 1] = (float) dcur;
    }

    /**
     * Integrate a complex spectrum by dividing by j*omega. Leaves the value for
     * the zero frequency unchanged.
     *
     * @param spectrum
     *            The complex spectrum to be integrated.
     * @param dt
     *            The sample interval in the time domain.
     */
    public static void IntegrateInFreqDomain(Complex[] spectrum, double dt) {
        int N = spectrum.length;
        double fny = 1.0 / 2.0 / dt;
        double df = fny / (N - 1);
        double pitwo = 2.0 * Math.PI;
        for (int j = 0; j < N; ++j) {
            double omega = pitwo * j * df;
            if (omega == 0) {
                continue;
            }
            double a = spectrum[j].getReal();
            double b = spectrum[j].getImaginary();
            spectrum[j] = new Complex(b / omega, -a / omega);
        }
    }

    /**
     * A float version of the MeanSmooth method
     *
     * @param data
     *            The data to be smoothed.
     * @param halfWidth
     *            The half-width of the smoothing window.
     * @return The smoothed data.
     */
    public static float[] meanSmooth(float[] data, int halfWidth) {
        double[] Ddata = floatToDouble(data);
        Ddata = meanSmooth(Ddata, halfWidth);
        return doubleToFloat(Ddata);
    }

    /**
     * Smooth the input series using a sliding window of width halfWidth. At
     * each point, the mean is computed for the set of points centered on the
     * point and of width 2 * W -1. The value of the point is then replaced by
     * the mean. At the ends of the series, shorter windows are used as
     * required. The end points are left unchanged. The input series is left
     * unmodified.
     *
     * @param data
     *            The data to be smoothed.
     * @param halfWidth
     *            The half-width of the smoothing window.
     * @return The smoothed data series.
     */
    public static double[] meanSmooth(double[] data, int halfWidth) {
        int N = data.length;
        if (halfWidth > N) {
            throw new IllegalArgumentException("The halfWidth is > than the array length.");
        }
        double[] result = new double[N];
        System.arraycopy(data, 0, result, 0, halfWidth);
        int W = 2 * halfWidth + 1;
        for (int j = halfWidth; j < N - halfWidth; ++j) {
            double sum = 0;
            for (int k = j - halfWidth; k <= j + halfWidth; ++k) {
                sum += data[k];
            }
            result[j] = sum / W;
        }
        System.arraycopy(data, N - halfWidth, result, N - halfWidth, N - (N - halfWidth));
        return result;
    }

    public static double[] multiply(double[] data, double value) {
        double[] result = new double[data.length];

        for (int ii = 0; ii < data.length; ii++) {
            result[ii] = data[ii] * value;
        }
        return result;
    }

    /**
     * Multiply the series values by a scalar constant.
     *
     * @param value
     *            The scalar value with which to multiply the series values
     * @param data
     *            Array containing the series values
     */
    public static void multiplyScalar(float[] data, double value) {
        int NPTS = data.length;
        if (NPTS < 1) {
            return;
        }
        for (int j = 0; j < NPTS; ++j) {
            data[j] *= value;
        }
    }

    /**
     * Multiply the series values by a scalar constant.
     *
     * @param value
     *            The scalar value with which to multiply the series values
     * @param data
     *            Array containing the series values
     */
    public static void multiplyScalar(double[] data, double value) {
        int NPTS = data.length;
        if (NPTS < 1) {
            return;
        }
        for (int j = 0; j < NPTS; ++j) {
            data[j] *= value;
        }
    }

    /**
     * Remove the mean of the series.
     *
     * @param data
     *            Array containing the series values
     */
    public static void removeMean(float[] data) {
        int NPTS = data.length;
        if (NPTS < 1) {
            return;
        }
        double mean = getMean(data);
        for (int j = 0; j < NPTS; j++) {
            data[j] -= mean;
        }
    }

    /**
     * Remove a linear trend from a series
     *
     * @param data
     *            Array containing the series values
     */
    public static void removeTrend(float[] data) {
        int Nsamps = data.length;
        if (Nsamps < 2) {
            return;
        }

        // First determine the slope and intercept of the best fitting line...
        double tmp1;

        // First determine the slope and intercept of the best fitting line...
        double tmp2;
        double tbar = 0.0;
        for (int j = 0; j < Nsamps; ++j) {
            tbar += j;
        }
        tbar /= Nsamps;
        double ybar = getMean(data);
        double SSX = 0.0;
        double SSXY = 0.0;
        for (int j = 0; j < Nsamps; ++j) {
            tmp1 = j - tbar;
            tmp2 = data[j] - ybar;
            SSX += tmp1 * tmp1;
            SSXY += tmp1 * tmp2;
        }
        if (SSX == 0.0) {
            SSX = 1;
        }
        double B1 = SSXY / SSX;

        // The slope
        double B0 = ybar - B1 * tbar;

        // The intercept
        // Now remove the trend.
        for (int j = 0; j < Nsamps; ++j) {
            data[j] -= (B0 + B1 * j);
        }
    }

    /**
     * Reverse the data in an array.
     *
     * @param data
     *            Description of the Parameter
     */
    public static void reverseArray(float[] data) {
        int N = data.length;
        for (int j = 0; j < N / 2; ++j) {
            float tmp = data[j];
            int idx = N - j - 1;
            data[j] = data[idx];
            data[idx] = tmp;
        }
    }

    /**
     * Reverse the data in an array.
     *
     * @param data
     *            Description of the Parameter
     */
    public static void reverseArray(double[] data) {
        int N = data.length;
        for (int j = 0; j < N / 2; ++j) {
            double tmp = data[j];
            int idx = N - j - 1;
            data[j] = data[idx];
            data[idx] = tmp;
        }
    }

    public static double slope(float[] data) {
        int Nsamps = data.length;
        if (Nsamps < 2) {
            return 0.;
        }

        double tmp1;
        double tmp2;
        double tbar = 0.0;

        for (int j = 0; j < Nsamps; ++j) {
            tbar += j;
        }

        tbar /= Nsamps;

        double ybar = getMean(data);

        double SSX = 0.0;
        double SSXY = 0.0;

        for (int j = 0; j < Nsamps; ++j) {
            tmp1 = j - tbar;
            tmp2 = data[j] - ybar;
            SSX += tmp1 * tmp1;
            SSXY += tmp1 * tmp2;
        }
        if (SSX == 0.0) {
            SSX = 1;
        }
        return SSXY / SSX;

    }
    // -------------------------------------------------------------

    /**
     * Element by element addition of two data series of equal length
     *
     * @param data1
     *            - the first data series
     * @param data2
     *            - the second data series
     * @return result[ii] = data1[ii] - data2[ii]
     */
    public static double[] subtract(double[] data1, double[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        double[] result = new double[data1.length];

        for (int ii = 0; ii < data1.length; ii++) {
            result[ii] = data1[ii] - data2[ii];
        }

        return result;

    }

    /**
     * Element by element addition of two data series of equal length
     *
     * @param data1
     *            - the first data series
     * @param data2
     *            - the second data series
     * @return result[ii] = data1[ii] - data2[ii]
     */
    public static float[] subtract(float[] data1, float[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        float[] result = new float[data1.length];

        for (int ii = 0; ii < data1.length; ii++) {
            result[ii] = data1[ii] - data2[ii];
        }

        return result;
    }

    // -------------------------------------------------------------
    /**
     * Apply a cosine taper to an array of floats
     *
     * @param TaperPercent
     *            The (one-sided) percent of the time series to which a taper
     *            will be applied. The value ranges from 0 (no taper) to 50 (
     *            The taper extends half the length of the series ). Since the
     *            taper is symmetric, a 50% taper means that all but the center
     *            value of the series will be scaled by some value less than
     *            1.0.
     * @param data
     *            Array containing the series values
     */
    public static void taper(float[] data, double TaperPercent) {
        int Nsamps = data.length;
        int MinTaperPnts = 5;
        if (Nsamps < 2 * MinTaperPnts) {
            return;
        }
        int TaperPnts = (int) (TaperPercent / 100 * Nsamps);
        if (TaperPnts > Nsamps / 2) {
            TaperPnts = Nsamps / 2;
        }
        if (TaperPnts < MinTaperPnts) {
            TaperPnts = MinTaperPnts;
        }
        double Factor = Math.PI / (TaperPnts);
        double TaperVal;
        for (int j = 0; j < TaperPnts; ++j) {
            TaperVal = (1.0 - Math.cos(j * Factor)) / 2.0;
            data[j] *= TaperVal;
            data[Nsamps - j - 1] *= TaperVal;
        }
    }

    /**
     * convert all the elements of the data series to their absolute value
     *
     * @param data
     * @return
     */
    public static float[] abs(float[] data) {
        float[] result = new float[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = Math.abs(data[i]);
        }
        return result;
    }

    /**
     * convert all the elements of the data series to their absolute value
     *
     * @param data
     * @return
     */
    public static double[] abs(double[] data) {
        double[] result = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = Math.abs(data[i]);
        }
        return result;
    }

    public static float[] arrayMultiply(float[] array1, float[] array2) {
        if (array1.length != array2.length) {
            throw new IllegalArgumentException("Input arrays must be same length!");
        }
        float[] result = new float[array1.length];
        for (int j = 0; j < array1.length; ++j) {
            result[j] = array1[j] * array2[j];
        }
        return result;
    }

    /**
     * Calculate the autocorrelation of a data series
     *
     * @param x
     *            - the data series
     * @return The autocorrelation of the input sequence.
     */
    public static double[] autocorrelate(float[] x) {
        return crosscorrelate(x, x);
    }

    /**
     * Calculate the autocorrelation at a specific offset
     *
     * @param x
     *            - the data series
     * @param shift
     *            - the specific point desired
     *
     *            Note autocorrelate(data,0) gives the autocorrelation value at
     *            0 shift also known as the "energy" of the trace
     * @return The value of the autocorrelation at shift.
     */
    public static double autocorrelate(float[] x, int shift) {
        return crosscorrelate(x, x, shift);
    }

    /**
     * Calculate the autocorrelation of a data series
     *
     * @param x
     *            - the data series
     */
    public static double[] autocorrelate(double[] x) {
        return crosscorrelate(x, x);
    }

    /**
     * Calculate the autocorrelation at a specific offset
     *
     * @param x
     *            - the data series
     * @param shift
     *            - the specific point desired Note autocorrelate(data,0) gives
     *            the autocorrelation value at 0 shift also known as the
     *            "energy" of the trace
     * @return autocorrelation.
     */
    public static double autocorrelate(double[] x, int shift) {
        return crosscorrelate(x, x, shift);
    }

    public static int countGlitches(float[] data, int windowLength, double threshold) {
        return getGlitches(data, windowLength, threshold).size();
    }

    /**
     * Get the cross-correlation series of two data sets
     *
     * for shift = 0:size result[shift] = sum( x(t) * y(t+shift))
     *
     * @param x
     *            the first data series
     * @param y
     *            the second data series
     * @return the cross-correlation series
     */
    public static double[] crosscorrelate(float[] x, float[] y) {
        int shift = Math.max(x.length, y.length) - 1;
        double[] result = new double[2 * shift + 1];

        for (int i = -shift; i <= shift; i++) {
            result[i + shift] = crosscorrelate(x, y, i);
        }

        return result;
    }

    /**
     * Get the value of the cross-correlation series at a specific offset
     *
     * result[shift] = sum( x(t) * y(t+shift) )
     *
     * @param x
     *            the first data series
     * @param y
     *            the second data series
     * @param shift
     *            - the offset between the data series
     * @return the cross correlation at a specific point
     *
     *         Note the energy of a trace is the autocorrelation at zero shift
     */
    public static double crosscorrelate(float[] x, float[] y, int shift) {
        double result = 0;

        for (int ii = 0; ii < x.length; ii++) {
            int yindex = shift + ii;

            if ((yindex >= 0) && (yindex < y.length)) {
                result += x[ii] * y[yindex];
            }
        }

        return result;
    }

    /**
     * Get the cross-correlation series of two data sets for shift = 0:size
     * result[shift] = sum( x(t) * y(t+shift))
     *
     * @param x
     *            the first data series
     * @param y
     *            the second data series
     * @return the cross-correlation series
     */
    public static double[] crosscorrelate(double[] x, double[] y) {
        int shift = Math.max(x.length, y.length) - 1;
        double[] result = new double[2 * shift + 1];

        for (int ii = -shift; ii <= shift; ii++) {
            result[ii + shift] = crosscorrelate(x, y, ii);
        }

        return result;
    }

    /**
     * Get the value of the cross-correlation series at a specific offset
     * result[shift] = sum( x(t) * y(t+shift) )
     *
     * @param x
     *            the first data series
     * @param y
     *            the second data series
     * @param shift
     *            - the offset between the data series
     * @return the cross correlation at a specific point Note the energy of a
     *         trace is the autocorrelation at zero shift
     */
    public static double crosscorrelate(double[] x, double[] y, int shift) {
        double result = 0;

        for (int ii = 0; ii < x.length; ii++) {
            int yindex = shift + ii;

            if ((yindex >= 0) && (yindex < y.length)) {
                result += x[ii] * y[yindex];
            }
        }

        return result;
    }
    // ---------------------------------------------------------------

    /**
     * Produce a cut version of the input series. The cut is made such that no
     * value in the series is less than minVal and no value in the series is
     * greater than maxVal. Assumes that the input series is sorted from least
     * to greatest. Note that if the minVal and maxVal are such that all values
     * in the input series are retained, then the input is simply passed out as
     * the result.
     *
     * @param data
     *            The data to be cut.
     * @param minVal
     *            The value bounding the cut series on the low side.
     * @param maxVal
     *            The value bounding the series on the high side.
     * @return The cut series.
     */
    public static double[] cut(double[] data, double minVal, double maxVal) {
        if (data[0] >= minVal && data[data.length - 1] <= maxVal) {
            return data;
        } else {
            int minIdx = 0;
            int maxIdx = data.length - 1;
            int i = 0;
            while (data[i++] < minVal) {
                ++minIdx;
            }
            i = maxIdx;
            while (data[i--] > maxVal) {
                --maxIdx;
            }
            int length = maxIdx - minIdx + 1;
            double[] result = new double[length];
            int idx = 0;
            for (i = minIdx; i <= maxIdx; ++i) {
                result[idx++] = data[i];
            }
            return result;
        }
    }

    /**
     * From the SeriesMath method given a data series it is decimated so that
     * only every Nth point is retained This should be equivalent to the more
     * versatile interpolate methods
     *
     * where N is the decimationfactor
     *
     * @param data
     *            the original data series array
     * @param decimationfactor
     *            should be an integer greater than 2
     * @return the decimated series
     */
    public static float[] decimate(float[] data, int decimationfactor) {
        if (decimationfactor < 2) {
            return data; // decimationfactor = 1
        }
        int dlen = data.length / decimationfactor;
        if (dlen * decimationfactor < data.length) {
            dlen++;
        }

        float[] result = new float[dlen];
        for (int i = 0; i < dlen; i++) {
            result[i] = data[i * decimationfactor];
        }
        return result;
    }

    /**
     * A double value version of the decimation method
     *
     * @param data
     * @param decimationfactor
     * @return the decimated series
     */
    public static double[] decimate(double[] data, int decimationfactor) {
        int dlen = data.length / decimationfactor;
        if (dlen * decimationfactor < data.length) {
            dlen++;
        }

        double[] result = new double[dlen];
        for (int i = 0; i < dlen; i++) {
            result[i] = data[i * decimationfactor];
        }
        return result;
    }

    /**
     * dot product of two equal length Series
     *
     * dot(A,B) = SUM(AiBi) = A1B1 + A2B2+...AnBn
     *
     * @param data1
     * @param data2
     * @return
     */
    public static Double dotProduct(double[] data1, double[] data2) {
        return getSum(elementMultiplication(data1, data2));
    }

    /**
     * dot product of two equal length Series
     *
     * dot(A,B) = SUM(AiBi) = A1B1 + A2B2+...AnBn
     *
     * @param data1
     * @param data2
     * @return
     */
    public static Double dotProduct(float[] data1, float[] data2) {
        return getSum(elementMultiplication(data1, data2));
    }

    /**
     * adds data1's values to data2's
     *
     * @param data1
     * @param data2
     * @return
     */
    public static float[] elementAddition(float[] data1, float[] data2) {

        if (data1.length != data2.length) {
            return null;
        }

        float[] result = new float[data1.length];

        for (int i = 0; i < data1.length; i++) {
            result[i] = data1[i] + data2[i];
        }

        return result;
    }

    /**
     * divide the individual elements of one data series by those of another
     *
     * @param data1
     *            - the first data series
     * @param data2
     *            - the denominator data series must have the same length as the
     *            first
     * @return a new double[] series in which result[ii] = data1[ii]/data2[ii]
     *
     *         Akin to array left division in Matlab A.\B
     */
    public static float[] elementDivision(float[] data1, float[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        float[] result = new float[data1.length];

        for (int i = 0; i < data1.length; i++) {
            result[i] = data1[i] / data2[i];
        }

        return result;
    }

    /**
     * divide the individual elements of one data series by those of another
     *
     * @param data1
     *            - the first data series
     * @param data2
     *            - the denominator data series must have the same length as the
     *            first
     * @return a new double[] series in which result[ii] = data1[ii]/data2[ii]
     *
     *         Akin to array left division in Matlab A.\B
     */
    public static double[] elementDivision(double[] data1, double[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        double[] result = new double[data1.length];

        for (int i = 0; i < data1.length; i++) {
            result[i] = data1[i] / data2[i];
        }

        return result;
    }
    // ---------------------------------------------------------------

    /**
     * Element-by-element multiplication, C = A.*B
     *
     * @param data1
     * @param data2
     * @return result[ii] = data1[ii] * data2[ii]
     */
    public static float[] elementMultiplication(float[] data1, float[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        float[] result = new float[data1.length];

        for (int i = 0; i < data1.length; i++) {
            result[i] = data1[i] * data2[i];
        }

        return result;
    }

    /**
     * Element-by-element multiplication, C = A.*B
     *
     * @param data1
     * @param data2
     * @return result[ii] = data1[ii] * data2[ii]
     */
    public static double[] elementMultiplication(double[] data1, double[] data2) {
        if (data1.length != data2.length) {
            return null;
        }

        double[] result = new double[data1.length];

        for (int i = 0; i < data1.length; i++) {
            result[i] = data1[i] * data2[i];
        }

        return result;
    }

    /**
     * Subtracts from data1 data2's values
     *
     * @param data1
     * @param data2
     * @return
     */
    public static float[] elementSubtraction(float[] data1, float[] data2) {

        if (data1.length != data2.length) {
            return null;
        }

        float[] result = new float[data1.length];

        for (int i = 0; i < data1.length; i++) {
            result[i] = data1[i] - data2[i];
        }

        return result;
    }

    /**
     * Calculate the envelope of real valued data using a Hilbert transform
     *
     * E(t) = sqrt( data^2 + Hilbert(data) ^2 )
     *
     * @param data
     *            : the data series
     * @return the envelope function for the data
     */
    public static float[] envelope(float[] data) {
        float[] envelope = new float[data.length];

        float[] Hilbert = hilbert(data);

        for (int ii = 0; ii < data.length; ii++) {
            double H2 = Math.pow(Hilbert[ii], 2.); // H2 = hilbert^2 should be real valued
            double d2 = data[ii] * data[ii];

            envelope[ii] = (float) Math.sqrt(d2 + H2);
        }
        return envelope;
    }

    //------------------------ Steve Myers' FIND utilties--------------------------//
    /**
     * Finds the index of values meeting specified conditions. Each element of
     * array is tested against value using specified logical ,condition
     * (==,LTEQ,GTEQ,LT,GT) are used in conjuntion with value
     *
     * usage e.g. find( longarray, "greater than equal to", 7);
     */
    public static int[] find(long[] array, String condition, long value) {
        int[] indexes;
        Integer temp;
        List<Integer> indexV = new ArrayList<>();

        if (condition.equals("==")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == value) {
                    indexV.add(i);
                }
            }
        } else if (condition.equals("<=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] <= value) {
                    indexV.add(i);
                }
            }
        } else if (condition.equals(">=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] >= value) {
                    indexV.add(i);
                }
            }
        } else if (condition.equals("<")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] < value) {
                    indexV.add(i);
                }
            }
        } else if (condition.equals(">")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] > value) {
                    indexV.add(i);
                }
            }
        }
        indexes = new int[indexV.size()];
        for (int i = 0; i < indexV.size(); i++) {
            temp = indexV.get(i);
            indexes[i] = temp;
        }
        return indexes;
    }

    /**
     * Finds the index of values meeting specified conditions. Each element of
     * array is tested against value using specified logical ,condition
     * (==,LTEQ,GTEQ,LT,GT) are used in conjuntion with value
     */
    public static int[] find(int[] array, String condition, int value) {
        int[] indexes;
        Integer temp;
        ArrayList<Integer> indexV = new ArrayList<>();

        if (condition.equals("==")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] <= value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] >= value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] < value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] > value) {
                    indexV.add(i);
                }
            }
        }
        indexes = new int[indexV.size()];
        for (int i = 0; i < indexV.size(); i++) {
            temp = indexV.get(i);
            indexes[i] = temp;
        }
        return indexes;
    }

    /**
     * Finds the index of values meeting specified conditions. Each element of
     * array is tested against value using specified logical ,condition
     * (==,LTEQ,GTEQ,LT,GT) are used in conjuntion with value
     */
    public static int[] find(float[] array, String condition, float value) {
        int[] indexes;
        Integer temp;
        ArrayList<Integer> indexV = new ArrayList<>();

        if (condition.equals("==")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] <= value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] >= value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] < value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] > value) {
                    indexV.add(i);
                }
            }
        }
        indexes = new int[indexV.size()];
        for (int i = 0; i < indexV.size(); i++) {
            temp = indexV.get(i);
            indexes[i] = temp;
        }
        return indexes;
    }

    /**
     * Finds the index of values meeting specified conditions. Each element of
     * array is tested against value using specified logical ,condition
     * (==,LTEQ,GTEQ,LT,GT) are used in conjuntion with value
     *
     * @param array
     * @param condition
     * @param value
     * @return
     */
    public static int[] find(double[] array, String condition, double value) {
        int[] indexes;
        Integer temp;
        ArrayList<Integer> indexV = new ArrayList<>();

        if (condition.equals("==")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] <= value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">=")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] >= value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] < value) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">")) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] > value) {
                    indexV.add(i);
                }
            }
        }
        indexes = new int[indexV.size()];
        for (int i = 0; i < indexV.size(); i++) {
            temp = indexV.get(i);
            indexes[i] = temp;
        }
        return indexes;
    }

    /**
     * For 2 arrays of equal length find the elements of array 1 that are ("GT",
     * "LT", "==" ...) the elements of array 2
     *
     * @param array1
     *            an array of doubles
     * @param condition
     * @param array2
     *            a second array of doubles
     * @return an array containing the indices for each element that passes the
     *         conditional test
     *
     *         e.g. array1 = { 1.1, 2.2, 3.3}, array2 = {4.4, 2.2, 3.3}
     *         find(array1, "==" array2) will return result = {1, 2}
     */
    public static int[] find(double[] array1, String condition, double[] array2) {
        int[] indexes;
        Integer temp;
        ArrayList<Integer> indexV = new ArrayList<>();

        if (condition.equals("==")) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] == array2[i]) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<=")) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] <= array2[i]) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">=")) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] >= array2[i]) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals("<")) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] < array2[i]) {
                    indexV.add(i);
                }
            }
        }
        if (condition.equals(">")) {
            for (int i = 0; i < array1.length; i++) {
                if (array1[i] > array2[i]) {
                    indexV.add(i);
                }
            }
        }
        indexes = new int[indexV.size()];
        for (int i = 0; i < indexV.size(); i++) {
            temp = indexV.get(i);
            indexes[i] = temp;
        }
        return indexes;
    }

    public static DiscontinuityCollection findDiscontinuities(float[] data, double sampRate, int winLength, double factor) {
        return DiscontinuityFinder.findDiscontinuities(data, sampRate, winLength, factor);
    }

    /**
     * Find the indices in the data series that match a specific value
     *
     * @param data
     *            - an array of doubles
     * @param value
     *            - the specific double value you are looking for
     * @return - a Vector of all the indices which match that value
     */
    public static List<Integer> findIndicesOf(double[] data, double value) {
        List<Integer> result = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            if (data[i] == value) {
                result.add(i);
            }
        }
        return result;
    }
    // -------------------------------------------------------------

    /**
     * Compute definite integral using extended trapezoidal rule.
     *
     * @param data
     *            The evenly-spaced function to integrate.
     * @param delta
     *            The sample interval.
     * @param idx1
     *            The starting index over which to integrate.
     * @param idx2
     *            The ending index over which to integrate.
     * @return The value of the definite integral.
     */
    public static double getDefiniteIntegral(float[] data, double delta, int idx1, int idx2) {
        double result = (data[idx1] + data[idx2]) / 2;
        for (int j = idx1 + 1; j < idx2; ++j) {
            result += data[j];
        }
        result *= delta;
        return result;
    }

    public static double getDefiniteIntegral(SeriesMath.Function f, double delta, int idx1, int idx2) {
        double result = (f.eval(idx1) + f.eval(idx2)) / 2;
        for (int j = idx1 + 1; j < idx2; ++j) {
            result += f.eval(j);
        }
        result *= delta;
        return result;
    }

    public static double getEnergy(float[] x) {
        return crosscorrelate(x, x, 0);
    }

    /**
     * @param data
     * @return the largest absolute value in the series
     */
    public static float getExtremum(float[] data) {
        float smax = 0.0f;
        float sabs;
        for (int i = 0; i < data.length; i++) {
            sabs = Math.abs(data[i]);
            if (sabs > smax) {
                smax = sabs;
            }
        }
        return smax;
    }

    public static float[] getFirstDifference(float[] data) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Invalid input array for first difference!");
        }
        float[] result = new float[data.length];
        Arrays.fill(result, 0);
        for (int j = 0; j < result.length - 1; ++j) {
            result[j] = data[j + 1] - data[j];
        }
        return result;
    }

    public static Collection<Glitch> getGlitches(float[] data, int windowLength, double threshold) {
        Collection<Glitch> glitches = new ArrayList<>();

        RollingStats window1 = null;
        RollingStats window2 = null;
        for (int i = windowLength; i < data.length - windowLength; i++) {
            if (window1 == null) {
                window1 = new RollingStats(Arrays.copyOfRange(data, 0, windowLength - 1));
                window2 = new RollingStats(Arrays.copyOfRange(data, i + 1, i + windowLength));
            } else {
                window1.replace(data[i - windowLength - 1], data[i - 1]);
                window2.replace(data[i], data[i + windowLength]);
            }

            double value = data[i];
            double mean = (window1.getMean() + window2.getMean()) / 2;
            double deviation = Math.abs(value - mean);

            double currentStd = (window1.getStandardDeviation() + window2.getStandardDeviation()) / 2;
            if (deviation > threshold * currentStd) {

                glitches.add(new Glitch(i, (float) mean));
            }
        }
        return glitches;
    }

    public static double getKurtosis(float[] data) {
        return new SampleStatistics(data, SampleStatistics.Order.FOURTH).getRMS();

    }

    public static float getMax(float[] data, int idx1, int idx2) {
        if (idx1 < 0) {
            throw new IllegalArgumentException("Starting index is < 0!");
        }
        if (idx2 >= data.length) {
            throw new IllegalArgumentException("Ending index is past end of array!");
        }
        if (idx1 > idx2) {
            throw new IllegalArgumentException("First index is > second index!");
        }
        float max = -Float.MAX_VALUE;
        for (int i = idx1; i <= idx2; ++i) {
            if (data[i] > max) {
                max = data[i];
            }
        }

        return max;
    }

    /**
     * Gets the maximum value of the series.
     *
     * @param data
     *            Array containing the series values
     * @return The max value of the series
     */
    public static float getMax(float[] data) {
        return getMax(data, 0, data.length - 1);
    }

    /**
     * Gets the maximum value of the series.
     *
     * @param data
     *            Array containing the series values
     * @return The max value of the series
     */
    public static double getMax(double[] data) {
        int Nsamps = data.length;
        if (Nsamps < 1) {
            return 0.0F;
        }
        double max = -Double.MAX_VALUE;
        for (int i = 0; i < Nsamps; i++) {
            if (data[i] > max) {
                max = data[i];
            }
        }
        return max;
    }

    public static double getMax(Collection<Double> data) {
        return new SampleStatistics(data, SampleStatistics.Order.FIRST).getMax();

    }

    /**
     * Gets the maximum value of the series and the index it occurs at.
     *
     * @param data
     *            Array containing the series values
     * @return The (index, max value) of the series
     */
    public static PairT<Integer, Float> getMaxIndex(float[] data) {
        int Nsamps = data.length;
        if (Nsamps < 1) {
            throw new IllegalStateException(EMPTY_ARRAY);
        }
        float max = -Float.MAX_VALUE;
        int imax = 0;
        for (int i = 0; i < Nsamps; i++) {
            if (data[i] > max) {
                imax = i;
                max = data[i];
            }
        }

        return new PairT<>(imax, max);
    }

    /**
     * Gets the maximum value of the series and the index it occurs at.
     *
     * @param data
     *            Array containing the series values
     * @return The (index, max value) of the series
     */
    public static PairT<Integer, Double> getMaxIndex(double[] data) {
        int Nsamps = data.length;
        if (Nsamps < 1) {
            throw new IllegalStateException(EMPTY_ARRAY);
        }
        double max = -Double.MAX_VALUE;
        int imax = 0;
        for (int i = 0; i < Nsamps; i++) {
            if (data[i] > max) {
                imax = i;
                max = data[i];
            }
        }

        return new PairT<>(imax, max);
    }

    /**
     * Gets the mean value of the series.
     *
     * @param data
     *            Array containing the series values
     * @return The mean value of the series
     */
    public static double getMean(float[] data) {
        if (data.length < 1) {
            return (0.0);
        }
        return getSum(data) / data.length;
    }

    public static double getMean(Collection<? extends Number> c) {
        if (c.isEmpty()) {
            return 0;
        } else {
            return getSum(c) / c.size();
        }
    }

    /**
     * Gets the mean value of the series.
     *
     * @param data
     *            Array containing the series values
     * @return The mean value of the series
     */
    public static double getMean(double[] data) {
        if (data.length < 1) {
            return (0.0);
        }
        return getSum(data) / data.length;
    }

    /**
     * Gets the median value of the time series.
     *
     * @param data
     *            Array containing the series values
     * @return The median value of the series
     */
    public static double getMedian(float[] data) {
        return new SampleStatistics(data, SampleStatistics.Order.FIRST).getMedian();
    }

    public static double getMedian(double[] values) {
        Median median = new Median();
        return median.evaluate(values);
    }

    public static double getMedian(List<Double> data) {
        int Nsamps = data.size();
        double median = 0.0;
        if (Nsamps < 1) {
            return median;
        }

        List<Double> tmp = new ArrayList<>(data);
        Collections.sort(tmp);
        int nhalf = Nsamps / 2;
        if (2 * nhalf == Nsamps) {
            median = 0.5 * (tmp.get(nhalf) + tmp.get(nhalf - 1));
        } else {
            median = tmp.get(nhalf);
        }
        return median;

    }

    public static double getMin(Collection<Double> data) {
        return new SampleStatistics(data, SampleStatistics.Order.FIRST).getMin();
    }

    public static float getMin(float[] data, int idx1, int idx2) {
        if (idx1 < 0) {
            throw new IllegalArgumentException("Starting index is < 0!");
        }
        if (idx2 >= data.length) {
            throw new IllegalArgumentException("Ending index is past end of array!");
        }
        if (idx1 > idx2) {
            throw new IllegalArgumentException("First index is > second index!");
        }
        float min = Float.MAX_VALUE;
        for (int i = idx1; i <= idx2; ++i) {
            if (data[i] < min) {
                min = data[i];
            }
        }

        return min;
    }

    /**
     * Gets the minimum value of the series.
     *
     * @param data
     *            Array containing the series values
     * @return The min value of the series
     */
    public static float getMin(float[] data) {
        return getMin(data, 0, data.length - 1);
    }

    /**
     * Gets the minimum value of the series.
     *
     * @param data
     *            Array containing the series values
     * @return The min value of the series
     */
    public static double getMin(double[] data) {
        int Nsamps = data.length;
        if (Nsamps < 1) {
            return 0.0F;
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < Nsamps; ++i) {
            if (data[i] < min) {
                min = data[i];
            }
        }
        return min;
    }

    /**
     * Gets the maximum value of the series and the index it occurs at.
     *
     * @param data
     *            Array containing the series values
     * @return The (index, min value) of the series
     */
    public static PairT<Integer, Double> getMinIndex(double[] data) {
        int Nsamps = data.length;
        if (Nsamps < 1) {
            throw new IllegalStateException(EMPTY_ARRAY);
        }
        double min = Double.MAX_VALUE;
        int imin = 0;
        for (int i = 0; i < Nsamps; i++) {
            if (data[i] < min) {
                imin = i;
                min = data[i];
            }
        }

        return new PairT<>(imin, min);
    }

    /**
     * Gets the minimum value of the series and the index it occurs at.
     *
     * @param data
     *            Array containing the series values
     * @return The (index, max value) of the series
     */
    public static PairT<Integer, Float> getMinIndex(float[] data) {
        int Nsamps = data.length;
        if (Nsamps < 1) {
            throw new IllegalStateException(EMPTY_ARRAY);
        }
        float min = Float.MAX_VALUE;
        int imin = 0;
        for (int i = 0; i < Nsamps; i++) {
            if (data[i] < min) {
                imin = i;
                min = data[i];
            }
        }

        return new PairT<>(imin, min);
    }
    // -------------------------------------------------------------

    /**
     * Treat the series as a vector and calculate the norm (aka the direction or
     * the normal) of the vector
     *
     * @param data
     * @return
     */
    public static double getNorm(double[] data) {
        double sumsquares = 0.;
        for (int ii = 0; ii < data.length; ii++) {
            sumsquares = sumsquares + data[ii] * data[ii];
        }

        return Math.sqrt(sumsquares);
    }

    public static double getPeakToPeakAmplitude(float[] data) {
        if (data.length < 1) {
            return 0;
        }
        double min = Double.MAX_VALUE;
        double max = -min;
        for (float v : data) {
            if (v > max) {
                max = v;
            }
            if (v < min) {
                min = v;
            }
        }
        return max - min;
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

    /**
     * Gets the RMS value of the data in the input float array.
     *
     * @param data
     *            The input float array.
     * @return The RMS value
     */
    public static double getRMS(float[] data) {
        return new SampleStatistics(data, SampleStatistics.Order.FIRST).getRMS();
    }

    /**
     * Gets the RMS value of the data in the input float array.
     *
     * @param data
     *            The input float array.
     * @return The RMS value
     */
    public static double getRMS(double[] data) {
        return new SampleStatistics(data, SampleStatistics.Order.FIRST).getRMS();
    }

    public static double getRMS(Collection<? extends Number> c) {
        return new SampleStatistics(c, SampleStatistics.Order.FIRST).getRMS();
    }

    public static double getRange(float[] u) {
        return new SampleStatistics(u, SampleStatistics.Order.FIRST).getRange();

    }

    public static double getSnr(float[] data, double sampleRate, double dataStartTime, double pickEpochTime, double prePickWindowLength, double postPickWindowLength) {
        if (sampleRate <= 0) {
            throw new IllegalArgumentException("Sample rate must be > 0!");
        }

        if (pickEpochTime <= dataStartTime) {
            throw new IllegalArgumentException("Pick time (" + pickEpochTime + ") must be > data start time (" + dataStartTime + ")!");
        }

        double dataEndTime = dataStartTime + (data.length - 1) / sampleRate;

        if (pickEpochTime >= dataEndTime) {
            throw new IllegalArgumentException("Pick time must be < data end time!");
        }

        double preWinStartTime = pickEpochTime - prePickWindowLength;
        if (preWinStartTime < dataStartTime) {
            preWinStartTime = dataStartTime;
        }

        double postWinEndTime = pickEpochTime + postPickWindowLength;
        if (postWinEndTime > dataEndTime) {
            postWinEndTime = dataEndTime;
        }

        int pickIndex = (int) Math.round((pickEpochTime - dataStartTime) * sampleRate);
        int preWinStartIndex = (int) Math.round((preWinStartTime - dataStartTime) * sampleRate);
        int postWinEndIndex = (int) Math.round((postWinEndTime - dataStartTime) * sampleRate);

        return getSnr(data, pickIndex, preWinStartIndex, postWinEndIndex);
    }

    public static double getSnr(float[] data, int pickIndex, int preWinStartIndex, int postWinEndIndex) {
        int preWinSamples = pickIndex - preWinStartIndex + 1;

        if (preWinSamples < 2) {
            throw new IllegalArgumentException("Not enough samples in pre-pick window!");
        }
        int postWinSamples = postWinEndIndex - pickIndex + 1;
        if (postWinSamples < 2) {
            throw new IllegalArgumentException("Not enough samples in post-pick window!");
        }
        double postP2P = getWindowPeakToPeak(data, pickIndex, postWinSamples);
        double preP2P = getWindowPeakToPeak(data, preWinStartIndex, preWinSamples);
        if (preP2P > 0) {
            return postP2P / preP2P;
        } else {
            return postP2P > 0 ? 1 : 0;
        }
    }

    /**
     * Gets the standard deviation of the time series.
     *
     * @param data
     *            Array containing the series values
     * @return The standard deviation value
     */
    public static double getStDev(float[] data) {
        return Math.sqrt(getVariance(data));
    }

    /**
     * Gets the standard deviation of the time series.
     *
     * @param data
     *            Array containing the series values
     * @return The standard deviation value
     */
    public static double getStDev(double[] data) {
        return Math.sqrt(getVariance(data));
    }

    public static double getStandardDeviation(Collection<? extends Number> c) {
        return Math.sqrt(getVariance(c));
    }

    public static double getStd(double[] vec, int idx1, int idx2) {

        double mean = 0.0;
        double std = 0.0;
        int N = idx2 - idx1 + 1;
        for (int j = idx1; j <= idx2; ++j) {
            mean += vec[j];
        }
        mean /= N;
        for (int j = idx1; j <= idx2; ++j) {
            double v = vec[j] - mean;
            std += v * v;
        }
        std /= (N - 1);
        return Math.sqrt(std);
    }

    public static double[] getSubSection(double[] v, int minIdx, int maxIdx) {

        int N = v.length;
        if (minIdx == 0 && maxIdx == N - 1) {
            return v;
        } else {
            int M = maxIdx - minIdx + 1;
            double[] result = new double[M];
            System.arraycopy(v, minIdx, result, 0, M);
            return result;
        }
    }

    public static float[] getSubSection(float[] v, int minIdx, int maxIdx) {

        int N = v.length;
        if (minIdx == 0 && maxIdx == N - 1) {
            return v;
        } else {
            int M = maxIdx - minIdx + 1;
            float[] result = new float[M];
            System.arraycopy(v, minIdx, result, 0, M);
            return result;
        }
    }

    public static int[] getSubSection(int[] v, int minIdx, int maxIdx) {

        int N = v.length;
        if (minIdx == 0 && maxIdx == N - 1) {
            return v;
        } else {
            int M = maxIdx - minIdx + 1;
            int[] result = new int[M];
            System.arraycopy(v, minIdx, result, 0, M);
            return result;
        }
    }

    /**
     * Gets the sum of the values in the input array
     *
     * @param data
     *            Array containing the series values
     * @return The sum of the values
     */
    public static double getSum(float[] data) {
        double result = 0.0;
        for (float aData : data) {
            result += aData;
        }
        return result;
    }

    public static double getSum(Collection<? extends Number> c) {
        double result = 0;
        for (Number v : c) {
            result += v.doubleValue();
        }
        return result;
    }

    // ---------------------------------------------------------------
    /**
     * Gets the sum of the values in the input array
     *
     * @param data
     *            Array containing the series values
     * @return The sum of the values
     */
    public static double getSum(double[] data) {
        double result = 0.0;
        for (double aData : data) {
            result += aData;
        }
        return result;
    }

    public static double getSumOfSquares(float[] data) {
        double result = 0.0;
        for (float aData : data) {
            result += aData * aData;
        }
        return result;
    }

    /**
     * Treat the series as a vector and calculate the unit vector (aka the
     * direction vector or the normalized vector) based on the norm
     *
     * unit_vector[ii] = data[ii]/norm;
     *
     * @param data
     *            is an Array containing the series values
     */
    public static double[] getUnitVector(double[] data) {
        double norm = getNorm(data);

        if (norm == 0.) // The original data are a zero vector. Return the original zero vector as the unit vector
        {
            return data;
        }

        double[] result = new double[data.length];

        for (int ii = 0; ii < data.length; ii++) {
            result[ii] = data[ii] / norm;
        }

        return result;
    }

    /**
     * Gets the variance of the time series.
     *
     * @param data
     *            Array containing the series values
     * @return The variance value
     */
    public static double getVariance(float[] data) {
        int Nsamps = data.length;
        if (Nsamps < 2) {
            return (0.0);
        }
        double mean = getMean(data);
        double dot = 0.0;
        for (int j = 0; j < Nsamps; j++) {
            double deviation = data[j] - mean;
            dot += deviation * deviation;
        }
        return dot / (Nsamps - 1);
    }

    public static double getVariance(Collection<? extends Number> c) {
        if (c.size() < 2) {
            return 0;
        }

        double mean = getMean(c);
        double dot = 0;
        for (Number n : c) {
            double deviation = n.doubleValue() - mean;
            dot += deviation * deviation;
        }
        return dot / (c.size() - 1);
    }

    // --------------------------------------------------------------------
    /**
     * Gets the variance of the time series.
     *
     * @param data
     *            Array containing the series values
     * @return The variance value
     */
    public static double getVariance(double[] data) {
        int Nsamps = data.length;
        if (Nsamps < 2) {
            return (0.0);
        }
        double mean = getMean(data);
        double dot = 0.0;
        for (int j = 0; j < Nsamps; j++) {
            double deviation = data[j] - mean;
            dot += deviation * deviation;
        }
        return dot / (Nsamps - 1);
    }

    // ---------------------------------------------------------------------------
    public static double getWindowPeakToPeak(float[] data, int idx, int Nsamps) {
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

    public static boolean hasVariance(float[] data, int start, int end) {
        boolean constant = true;
        if (end - start > 0) {
            double value = data[start];
            for (int i = start + 1; i <= end && constant; i++) {
                constant = data[i] == value;
            }
        }
        return !constant;
    }

    /**
     * Calculate the Discrete Hilbert Transform of real valued data
     *
     * H(f=0) == data(f=0) H(f=length/2) == data(f=length/2)
     *
     * H(f) = ( 2 * data(f) ) : t = 1 : length/2 - 1 H(f) = 0.0 : t = length/2
     * +1 : length
     *
     * H(t) = ifft(H(f))
     *
     * @param data
     *            : the data series
     * @return the complex valued Hilbert transformation (H(t))
     */
    public static float[] hilbert(float[] data) {
        int initialLength = data.length;
        initialLength = toPowerOfTwo(initialLength);
        double[][] seq = new double[2][initialLength];
        for (int i = 0; i < data.length; i++) {
            seq[R][i] = data[i];
        }

        FastFourierTransformer.transformInPlace(seq, DftNormalization.STANDARD, TransformType.FORWARD);

        for (int ii = 1; ii < seq[R].length / 2; ii++) {
            seq[R][ii] *= 2.0;
            seq[I][ii] *= 2.0;
        }

        for (int ii = seq[R].length / 2 + 1; ii < seq[R].length; ii++) {
            seq[R][ii] = 0.0;
            seq[I][ii] = 0.0;
        }

        FastFourierTransformer.transformInPlace(seq, DftNormalization.STANDARD, TransformType.INVERSE);

        float[] hilbert = new float[seq[I].length];
        for (int ii = 0; ii < seq[I].length; ii++) {
            // The Hilbert Transform is the imaginary part of the fft
            hilbert[ii] = (float) seq[I][ii];
        }

        return hilbert;
    }

    private static int toPowerOfTwo(int initialLength) {
        if (ArithmeticUtils.isPowerOfTwo(initialLength)) {
            return initialLength;
        }
        //There are faster bit-twiddling tricks you can do for this but until this becomes a speed problem I'd like to avoid the fragility those tend to cause long term.
        int n = 1;
        while (n < initialLength) {
            n = n << 1;
        }
        return n;
    }

    /**
     * Interpolate a single y-value given x- and y-arrays.
     *
     * @param x
     *            The array of x-values, assumed to be monitonically increasing
     *            but not-necessarily evenly-spaced..
     * @param y
     *            The array of y-values corresponding to the samples in the
     *            x-array.
     * @param xinterp
     *            The x-value at which an interpolated y-value is to be
     *            computed. xinterp is assumed to be in the range spanned by x.
     * @return The interpolated y-value.
     */
    public static double interpolate(double[] x, double[] y, double xinterp) {
        return wigint(x, y, 0.0, EPS, xinterp);
    }

    /**
     * Interpolate a single y-value given x- and y-arrays.
     *
     * @param x
     *            The array of x-values, assumed to be monitonically increasing
     *            but not-necessarily evenly-spaced..
     * @param y
     *            The array of y-values corresponding to the samples in the
     *            x-array.
     * @param xinterp
     *            The x-value at which an interpolated y-value is to be
     *            computed. xinterp is assumed to be in the range spanned by x.
     * @return The interpolated y-value.
     */
    public static float interpolate(float[] x, float[] y, float xinterp) {
        int N = x.length;
        if (y.length != N) {
            throw new IllegalArgumentException(LENGTH_MISMATCH);
        }
        if (xinterp < x[0] || xinterp > x[N - 1]) {
            throw new IllegalArgumentException("xinterp value is out of the range of x.");
        }
        double[] X = new double[N];
        double[] Y = new double[N];
        for (int j = 0; j < N; ++j) {
            X[j] = x[j];
            Y[j] = y[j];
        }
        return (float) wigint(X, Y, 0.0, EPS, xinterp);
    }

    /**
     * Interpolate a single y-value
     *
     * @param xStart
     *            The x-value corresponding to the first y-value.
     * @param dx
     *            The sample interval of the data.
     * @param y
     *            The array of y-values.
     * @param xinterp
     *            The x-value at which an interpolated y-value is to be
     *            computed. xinterp is assumed to be in the range spanned by x,
     *            e.g. xStart LTEQ xinterp LTEQ (y.length-1) * dx. @return The
     *            interpolated y-value.
     */
    public static double interpolate(double xStart, double dx, double[] y, double xinterp) {
        if (xinterp < xStart || xinterp > (y.length - 1) * dx) {
            throw new IllegalArgumentException("The xinterp value is out of range.");
        }
        double[] x = new double[1];
        x[0] = xStart;
        return wigint(x, y, dx, EPS, xinterp);
    }

    /**
     * Produce a vector of interpolated values.
     *
     * @param x
     *            The array of x-values, assumed to be monotonically increasing
     *            but not-necessarily evenly-spaced..
     * @param y
     *            The array of y-values corresponding to the samples in the
     *            x-array.
     * @param xinterp
     *            The array of x-values for which interpolated y-values are to
     *            be computed. All values in xinterp are assumed to be within
     *            the range spanned by x.
     * @return The array of interpolated y-values.
     */
    public static double[] interpolate(double[] x, double[] y, double[] xinterp) {
        int N = x.length;
        if (y.length != N) {
            throw new IllegalArgumentException(LENGTH_MISMATCH);
        }
        double[] result = new double[xinterp.length];
        for (int j = 0; j < xinterp.length; ++j) {
            result[j] = wigint(x, y, 0.0, EPS, xinterp[j]);
        }
        return result;
    }

    /**
     * Produce a vector of interpolated values.
     *
     * @param x
     *            The array of x-values, assumed to be monotonically increasing
     *            but not-necessarily evenly-spaced..
     * @param y
     *            The array of y-values corresponding to the samples in the
     *            x-array.
     * @param xinterp
     *            The array of x-values for which interpolated y-values are to
     *            be computed. All values in xinterp are assumed to be within
     *            the range spanned by x.
     * @return The array of interpolated y-values.
     */
    public static float[] interpolate(float[] x, float[] y, float[] xinterp) {
        int N = x.length;
        if (y.length != N) {
            throw new IllegalArgumentException(LENGTH_MISMATCH);
        }
        double[] X = new double[N];
        double[] Y = new double[N];
        for (int j = 0; j < x.length; ++j) {
            X[j] = x[j];
        }
        for (int j = 0; j < y.length; ++j) {
            Y[j] = y[j];
        }

        float[] result = new float[xinterp.length];
        for (int j = 0; j < xinterp.length; ++j) {
            result[j] = (float) wigint(X, Y, 0.0, EPS, xinterp[j]);
        }
        return result;
    }

    /**
     * Interpolate an array of y-values
     *
     * @param xStart
     *            The x-value corresponding to the first y-value.
     * @param dx
     *            The sample interval of the data.
     * @param y
     *            The array of y-values.
     * @param xinterp
     *            An array of x-values for which y-values are to be
     *            interpolated. All values in xinterp must be in the range
     *            defined by xStart, dx, and y.length.
     * @return The interpolated y-value.
     */
    public static double[] interpolate(double xStart, double dx, double[] y, double[] xinterp) {
        double[] x = new double[y.length];
        for (int j = 0; j < y.length; ++j) {
            x[j] = xStart + j * dx;
        }
        double[] result = new double[xinterp.length];
        for (int j = 0; j < xinterp.length; ++j) {
            result[j] = wigint(x, y, 0.0, EPS, xinterp[j]);
        }
        return result;
    }

    /**
     * Interpolate an array of y-values
     *
     * @param xStart
     *            The x-value corresponding to the first y-value.
     * @param dx
     *            The sample interval of the data.
     * @param y
     *            The array of y-values.
     * @param xinterp
     *            An array of x-values for which y-values are to be
     *            interpolated. All values in xinterp must be in the range
     *            defined by xStart, dx, and y.length.
     * @return The interpolated y-value.
     */
    public static float[] interpolate(float xStart, float dx, float[] y, float[] xinterp) {
        double[] x = new double[1];
        x[0] = xStart;
        double[] Y = new double[y.length];
        for (int j = 0; j < y.length; ++j) {
            Y[j] = y[j];
        }

        float[] result = new float[xinterp.length];
        for (int j = 0; j < xinterp.length; ++j) {
            result[j] = (float) wigint(x, Y, dx, EPS, xinterp[j]);
        }
        return result;
    }

    public static float[] interpolate(double xStart, double oldDx, float[] y, double newDx) {
        double timeSpan = (y.length - 1) * oldDx;
        int numberInterpolatedPoints = (int) (timeSpan / newDx);
        double[] xinterp = new double[numberInterpolatedPoints];
        for (int j = 0; j < numberInterpolatedPoints; ++j) {
            xinterp[j] = (xStart + j * newDx);
        }

        double[] x = new double[1];
        x[0] = xStart;
        double[] Y = new double[y.length];
        for (int j = 0; j < y.length; ++j) {
            Y[j] = y[j];
        }

        float[] result = new float[xinterp.length];
        for (int j = 0; j < xinterp.length; ++j) {
            result[j] = (float) wigint(x, Y, oldDx, EPS, xinterp[j]);
        }
        return result;

    }

    /**
     * Interpolate a single y-value
     *
     * @param xStart
     *            The x-value corresponding to the first y-value.
     * @param dx
     *            The sample interval of the data.
     * @param y
     *            The array of y-values.
     * @param xinterp
     *            The x-value at which an interpolated y-value is to be
     *            computed. xinterp is assumed to be in the range spanned by x,
     *            e.g. xStart LTEQ xinterp LTEQ (y.length-1) * dx. @return The
     *            interpolated y-value.
     */
    public static float interpolateValue(double xStart, double dx, float[] y, double xinterp) {
        if (xinterp < xStart || xinterp > (y.length - 1) * dx) {
            throw new IllegalArgumentException("The xinterp value is out of range.");
        }
        int N = y.length;
        double[] Y = new double[N];
        for (int j = 0; j < N; ++j) {
            Y[j] = y[j];
        }
        double[] x = new double[1];
        x[0] = xStart;
        return (float) wigint(x, Y, dx, EPS, xinterp);
    }

    public static boolean isConstant(float[] data) {
        if (data == null || data.length < 2) {
            return true;
        }
        float v1 = data[0];
        for (float v : data) {
            if (v != v1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create an array for which each point is the log10 value of every point in
     * the original input array
     *
     * Note values less than 0. are replaced by 0.
     *
     * @param data
     * @return the log10 array
     */
    public static float[] log10(float[] data) {
        float[] result = new float[data.length];
        for (int ii = 0; ii < data.length; ii++) {
            if (data[ii] < 0.f) {
                result[ii] = 0.f;
            } else {
                result[ii] = (float) Math.log10(data[ii]);
            }
        }
        return result;
    }

    public static float[] matlabDiff(float[] data) {
        if (data == null || data.length < 2) {
            throw new IllegalArgumentException("Invalid input array for first difference!");
        }
        float[] result = new float[data.length - 1];
        for (int j = 0; j < data.length - 1; ++j) {
            result[j] = data[j + 1] - data[j];
        }
        return result;
    }
    // -------------------------------------------------------------

    /**
     * A method of padding your array with some elements
     *
     * @param <T>
     * @param original
     * @param padElement
     *            what to fill the padded elements with
     * @param newLength
     *            the length to pad to.
     * @param javaSucks
     *            because it throws away the generic type information forcing us
     *            to pass it this kludgy way!
     * @return
     */
    public static <T extends Number> T[] padRight(T[] original, T padElement, int newLength, Class<T> javaSucks) {
        if (newLength <= 0) {
            throw new IllegalArgumentException("newLength must be > 0");
        }

        final T[] padded = (T[]) Array.newInstance(javaSucks, newLength);
        Arrays.fill(padded, padElement);
        if (original == null) {

            return padded;
        }
        if (newLength < original.length) {
            throw new IllegalArgumentException("newLength must be > original.length");
        }

        System.arraycopy(original, 0, padded, 0, original.length);
        return padded;
    }

    public static double quickMedian(float[] values) {
        return quickMedian(new FloatList(values));
    }

    public static double quickMedian(double[] values) {
        return quickMedian(new DoubleList(values));
    }

    public static double quickMedian(NumericalList values) {
        return quickMedian(values, 2);
    }

    public static int removeGlitches(float[] data, int windowLength, double threshold) {
        Collection<Glitch> glitches = getGlitches(data, windowLength, threshold);
        for (Glitch glitch : glitches) {
            data[glitch.getIndex()] = glitch.getCorrection();
        }
        return glitches.size();
    }

    public static int removeGlitches(float[] data, int windowLength, double MinGlitchAmp, double Thresh2) {
        int nglitch = 0;

        if (windowLength < 2) {
            return 0;
        }

        if (data.length / windowLength < 2) {
            return 0;
        }

        removeMedian(data);

        // Taper ends of trace to remove spikes at end.
        double taperPercent = 2.0;
        taper(data, taperPercent);

        // Initialize mean and variance.
        float[] buffer = new float[windowLength];
        System.arraycopy(data, data.length - windowLength, buffer, 0, windowLength);
        RollingStats stats = new RollingStats(buffer);
        double dataMean = stats.getMean();
        double dataStd = stats.getStandardDeviation();

        int j = data.length - windowLength - 1;
        while (j > 0) {

            double deviation = Math.abs((data[j] - dataMean));
            if ((deviation > Thresh2 * dataStd) && (deviation > MinGlitchAmp)) {
                data[j] = (float) dataMean;
                nglitch++;
            }
            int lst = j + windowLength;
            stats.replace(data[lst], data[j]);
            dataMean = stats.getMean();
            dataStd = stats.getStandardDeviation();
            j--;
        }

        // Large amplitude glitches may have biased the original mean, so that deglitched
        // trace has a non-zero-mean. Remove that.
        removeMean(data);
        taper(data, taperPercent);

        return nglitch;
    }

    /**
     * A simple glitch removal method if the (data[j] - median) falls above a
     * defined threshhold replace data[j] with the median value
     *
     * @param data
     *            The data array to be deglitched.
     * @param threshhold
     *            The threshold in terms of signal variance.
     */
    public static void removeGlitches(float[] data, double threshhold) {
        double median = getMedian(data);
        double variance = getVariance(data);
        double reject = threshhold * Math.sqrt(variance);
        for (int jj = 0; jj < data.length; jj++) {
            double value = Math.abs((data[jj] - median));

            if (value > reject) {
                data[jj] = (float) median;
            }
        }
    }

    /**
     * Check whether the data series has flat segments - where every element of
     * the segment is identical
     *
     * @param minsegmentlength
     *            the shortest number of datapoints that must be identical
     *            before it qualifies as "flat"
     * @param data
     * @param segmentlength
     *            an integer number of elements defining the shortest segment
     * @return true if there are any single-valued segments
     */
    public static boolean hasFlatSegments(float[] data, int segmentlength) {
        float[] dx = new float[segmentlength];

        //TODO: currently hardwiring the number of elements that must be flat - replace with windowlength
        for (int jj = 0; jj < (data.length - 9); jj++) {
            for (int subset = 1; subset < 10; subset++)// currently - 10 points in a row are flagged
            {
                dx[subset - 1] = data[jj] - data[jj + subset];
            }

            boolean isconstant = isConstant(dx);
            if (isconstant) {
                return isconstant; // "True" The series has flat segments
            }
        }

        return false; // if you get to here - there are no segments with 10 equal values
    }

    // ---------------------------------------------------------------
    public static void removeMedian(float[] data) {
        int NPTS = data.length;
        if (NPTS < 1) {
            return;
        }
        double median = getMedian(data);
        for (int j = 0; j < NPTS; ++j) {
            data[j] -= median;
        }
    }

    public static void replaceValue(float[] data, float value, float replacement) {
        for (int j = 0; j < data.length; ++j) {
            if (data[j] == value) {
                data[j] = replacement;
            }
        }
    }

    /**
     * Perform a clockwise rotation of a pair of signals (Based on the SAC C
     * code rotate.c) This method replaces the original signals with their
     * rotated counterparts.
     *
     * @param signal1
     *            - first input signal
     * @param signal2
     *            - second input signal
     * @param angle
     *            - angle of rotation (clockwise from direction of signal1) in
     *            degrees
     * @param npinput
     *            - TRUE if the input signals have "normal" polarity
     * @param npoutput
     *            - TRUE if the output signals have "normal" polarity
     *
     *            "normal" polarity is such that the second component leads the
     *            first component by 90 degrees in a clockwise rotation.
     */
    public static void rotate(float[] signal1, float[] signal2, double angle, boolean npinput, boolean npoutput) {
        double con11;
        double con12;
        double con21;
        double con22;
        double cosa;
        double sina;
        double radians = Math.toRadians(angle);
        cosa = Math.cos(radians);
        sina = Math.sin(radians);
        con11 = cosa;
        con12 = sina;
        con21 = -sina;
        con22 = cosa;

        if (!npinput) {
            con12 = -con12;
            con22 = -con22;
        }
        if (!npoutput) {
            con21 = -con21;
            con22 = -con22;
        }

        int ns = Math.min(signal1.length, signal2.length); // note: this assumes that the signals both start at the same time, but may be of different lengths

        float[] result = new float[2];

        for (int js = 0; js < ns; js++) {
            result[0] = (float) (con11 * signal1[js] + con12 * signal2[js]);
            result[1] = (float) (con21 * signal1[js] + con22 * signal2[js]);

            signal1[js] = result[0];// note this replaces the original signal1
            signal2[js] = result[1];// note this replaces the original signal2
        }
    }

    public static void setMaximumRange(float[] data, double maxRange) {
        if (maxRange <= 0) {
            throw new IllegalStateException("The specified max range muyst be greater than 0!");
        }
        double min = SeriesMath.getMin(data);
        double max = SeriesMath.getMax(data);
        double range = max - min;
        if (range > maxRange) {
            double scale = maxRange / range;
            SeriesMath.multiplyScalar(data, scale);
        }
    }

    public static void setPtoPAmplitude(float[] data, double newRange) {
        double min = Double.MAX_VALUE;
        double max = -min;
        double mean = 0.0;
        int npts = data.length;
        for (float v : data) {
            max = Math.max(v, max);
            min = Math.min(v, min);
            mean += v;
        }
        mean /= npts;
        double range = max - min;
        if (range <= 0) {
            range = 1;
        }
        double scale = newRange / range;
        for (int j = 0; j < data.length; ++j) {
            data[j] = (float) ((data[j] - mean) * scale);
        }

    }

    public static int[] sign(float[] x) {
        int[] result = new int[x.length];
        for (int j = 0; j < x.length; ++j) {
            result[j] = (int) Math.signum(x[j]);
        }
        return result;
    }

    /**
     * Create an array for which each point is the signed sqrt value of every
     * point in the original input array
     *
     * Note values less than 0. are replaced by -1* sqrt(abs(data))
     *
     * @param data
     * @return the signed sqrt array
     */
    public static float[] signedSqrt(float[] data) {
        float[] result = new float[data.length];
        for (int ii = 0; ii < data.length; ii++) {
            if (data[ii] <= 0.f) {
                result[ii] = -1.f * (float) Math.sqrt(Math.abs(data[ii]));
            } else {
                result[ii] = (float) Math.sqrt(data[ii]);
            }
        }
        return result;
    }

    /**
     * Create an array for which each point is the signed square value of every
     * point in the original input array
     *
     * Note values less than 0. are replaced by -1*data*data
     *
     * @param data
     * @return the signed squared value array
     */
    public static float[] signedSquare(float[] data) {
        float[] result = new float[data.length];
        for (int ii = 0; ii < data.length; ii++) {
            if (data[ii] <= 0.f) {
                result[ii] = -1.f * data[ii] * data[ii];
            } else {
                result[ii] = data[ii] * data[ii];
            }
        }
        return result;

    }

    public static void signum(float[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.signum(data[i]);
        }
    }

    public static void signum(double[] data) {
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.signum(data[i]);
        }
    }

    /**
     * Create an array for which each point is the sqrt value of every point in
     * the original input array
     *
     * Note values less than 0. are replaced by 0.
     *
     * @param data
     * @return the sqrt array
     */
    public static float[] sqrt(float[] data) {
        float[] result = new float[data.length];
        for (int ii = 0; ii < data.length; ii++) {
            if (data[ii] <= 0.f) {
                result[ii] = 0.f;
            } else {
                result[ii] = (float) Math.sqrt(data[ii]);
            }
        }
        return result;
    }

    /**
     * Create an array for which each point is the squared value of every point
     * in the original input array
     *
     * @param data
     * @return the squared value array
     */
    public static float[] square(float[] data) {
        float[] result = new float[data.length];

        for (int ii = 0; ii < data.length; ii++) {
            result[ii] = data[ii] * data[ii];
        }
        return result;
    }

    /**
     * Create an array for which each point is the squared value of every point
     * in the original input array
     *
     * @param data
     * @return the squared value array
     */
    public static double[] square(double[] data) {
        double[] result = new double[data.length];

        for (int ii = 0; ii < data.length; ii++) {
            result[ii] = data[ii] * data[ii];
        }
        return result;
    }

    public static float[] testdebugSmooth(float[] data, int halfWidth) {
        double[] Ddata = floatToDouble(data);
        Ddata = testdebugSmooth(Ddata, halfWidth);
        return doubleToFloat(Ddata);
    }

    /**
     *
     * @param data
     *            The data to be smoothed.
     * @param halfWidth
     *            The half-width of the smoothing window.
     * @return The smoothed data series.
     */
    public static double[] testdebugSmooth(double[] data, int halfWidth) {
        double fullrms = getRMS(data);
        double epsilon = fullrms / 1.e12; // todo arbitrary value - make more general
        if (epsilon <= 0.) {
            return data;
        }

        int N = data.length;
        double[] result = new double[N];
        if (halfWidth > N) {
            throw new IllegalArgumentException("The halfWidth is > than the array length.");
        }

        int fullWidth = 2 * halfWidth + 1;
        double[] datasubset = new double[fullWidth];

        for (int index = 0; index < halfWidth; index++) {
            result[index] = (data[index] / fullrms);
        }

        for (int index = halfWidth; index < N - halfWidth - 1; index++) {
            System.arraycopy(data, index - halfWidth, datasubset, 0, fullWidth);

            double rmsvalue = getRMS(datasubset);
            if (rmsvalue < epsilon) {
                rmsvalue = epsilon;
            }
            result[index] = data[index] / rmsvalue;
        }

        for (int index = N - halfWidth; index < N; index++) {
            result[index] = data[index] / fullrms;
        }
        return result;
    }

    public static void triangleTaper(float[] data, double taperPercent) {
        int nsamps = data.length;
        int minTaperPoints = 5;
        if (nsamps < 2 * minTaperPoints) {
            return;
        }
        int taperPoints = (int) (taperPercent / 100 * nsamps);
        if (taperPoints > nsamps / 2) {
            taperPoints = nsamps / 2;
        }
        if (taperPoints < minTaperPoints) {
            taperPoints = minTaperPoints;
        }
        double factor = 1.0 / (taperPoints - 1);
        for (int j = 0; j < taperPoints; ++j) {
            double value = j * factor;
            data[j] *= value;
            data[nsamps - j - 1] *= value;
        }
    }

    /**
     * a whitening filter
     *
     * y(t) = x(t) + SUM(k = 1:p){a(k)*x(t-k)}
     *
     * @param x
     *            the initial data set
     * @param a
     *            a vector of whitening coefficients
     * @return the whitened data
     */
    public static float[] whiten(float[] x, double[] a) {
        float[] y = new float[x.length];

        for (int tt = 0; tt < x.length; tt++) {
            float AX = 0.f;

            for (int kk = 0; kk < a.length; kk++) {
                if (tt - kk > 0) {
                    AX += (float) a[kk] * x[tt - (kk + 1)];
                }
            }

            y[tt] = x[tt] + AX;
        }

        return y;
    }

    /**
     * =====================================================================
     * PURPOSE: Interpolates evenly or unevenly spaced data.
     * =====================================================================
     * INPUT ARGUMENTS:
     *
     * @param x:
     *            X array if unevenly spaced, first x if evenly spaced.
     * @param y:
     *            Y array.
     * @param dx:
     *            Set to 0.0 if unevenly spaced, to sampling interval if evenly
     *            spaced.
     * @param eps:
     *            Interpolation factor.
     * @param t:
     *            Time value to interpolateValue to.
     *            =====================================================================
     * @return The interpolated value.
     */
    private static double wigint(double[] x, double[] y, double dx, double eps, double t) {
        int j;
        int n1;
        double a;
        double am;
        double amd;
        double amu;
        double dxd;
        double dxj;
        double dxj1;
        double dxj1s;
        double dxjs;
        double dxu;
        double dy;
        double dyd;
        double dyu;
        double epsi;
        double h;
        double hc;
        double hs;
        double sp;
        double sp1;
        double t1;
        double t2;
        double t3;
        double t4;
        double w;
        double wd;
        double wu;

        int npts = y.length;
        epsi = .0001;
        if (eps > 0.) {
            epsi = eps;
        }
        if (dx == 0.) {
            if (x.length != npts) {
                throw new IllegalArgumentException("For unevenly-spaced data, the x and y arrays must be the same length.");
            }
            for (j = 0; j < npts; ++j) {
                a = x[j] - t;
                if (a > 0.) {
                    break;
                }
            }
            --j;
            dxj = t - x[j];
            if (dxj == 0.) {
                return y[j];
            }
            h = x[j + 1] - x[j];
            dxj1 = t - x[j + 1];
        } else {
            j = (int) ((t - x[0]) / dx);
            dxj = t - x[0] - (j) * dx;
            if (dxj == 0.) {
                return y[j];
            }
            h = dx;
            dxj1 = dxj - h;
        }
        hs = h * h;
        hc = hs * h;
        dxjs = dxj * dxj;
        dxj1s = dxj1 * dxj1;
        dy = y[j + 1] - y[j];
        am = dy / h;
        amd = am;
        amu = am;
        if (j != 0) {
            if (dx == 0.) {
                dxd = x[j] - x[j - 1];
            } else {
                dxd = dx;
            }
            dyd = y[j] - y[j - 1];
            amd = dyd / dxd;
        }
        n1 = j + 1;
        if (n1 != npts - 1) {
            if (dx == 0.) {
                dxu = x[j + 2] - x[j + 1];
            } else {
                dxu = dx;
            }
            dyu = y[j + 2] - y[j + 1];
            amu = dyu / dxu;
        }
        wd = 1. / Math.max(Math.abs(amd), epsi);
        w = 1. / Math.max(Math.abs(am), epsi);
        wu = 1. / Math.max(Math.abs(amu), epsi);
        sp = (wd * amd + w * am) / (wd + w);
        sp1 = (w * am + wu * amu) / (w + wu);
        t1 = y[j] * (dxj1s / hs + 2. * dxj * dxj1s / hc);
        t2 = y[j + 1] * (dxjs / hs - 2. * dxj1 * dxjs / hc);
        t3 = sp * dxj * dxj1s / hs;
        t4 = sp1 * dxjs * dxj1 / hs;
        return t1 + t2 + t3 + t4;
    }

    /*
     * Source:
     * http://www.i-programmer.info/babbages-bag/505-quick-median.html?start=1
     */
    private static double quickMedian(NumericalList a, int k) {
        int L = 0;
        int R = a.size() - 1;
        k = a.size() / k;
        int[] indices = new int[2];
        while (L < R) {
            double x = a.get(k);
            indices[0] = L;
            indices[1] = R;
            split(a, x, indices);
            if (indices[1] < k) {
                L = indices[0];
            }
            if (k < indices[0]) {
                R = indices[1];
            }
        }

        double median = a.get(k);
        if (a.size() % 2 == 0) {
            double m2 = a.get(0);
            for (int i = 1; i < k; i++) {
                m2 = Math.max(m2, a.get(i));
            }
            median = (median + m2) / 2.0;
        }

        return median;
    }

    private static void split(NumericalList a, double x, int[] indices) {
        // Do the left and right scan until the pointers cross
        do {
            // Scan from the left then scan from the right
            while (a.get(indices[0]) < x) {
                indices[0]++;
            }
            while (x < a.get(indices[1])) {
                indices[1]--;
            }
            // Now swap values if they are in the wrong part:
            if (indices[0] <= indices[1]) {
                double t = a.get(indices[0]);
                a.set(indices[0], a.get(indices[1]));
                a.set(indices[1], t);
                indices[0]++;
                indices[1]--;
            }
            // And continue the scan until the pointers cross:
        } while (indices[0] <= indices[1]);
    }

    /**
     * Calculates the covariance of two equally sized vectors.
     *
     * cov(v1,v2) = 1/(n-1)*sum(xi-xavg)*sum(yi-yavg)
     *
     * @param vec1
     * @param vec2
     * @return the covariance between the provided vectors. Value will be in the
     *         range [-1,1]
     */
    public double covariance(double[] vec1, double[] vec2) {
        if (vec1 == null || vec2 == null) {
            throw new IllegalArgumentException("Vectors must be non-null");
        }

        // it is not the job of this method to pad vectors
        if (vec1.length != vec2.length) {
            throw new IllegalArgumentException("Vectors must be equal length");
        }

        // Two empty vectors - covariance undefined?
        if (vec1.length == 0) {
            return 0;
        }

        int n = vec1.length;

        double xAverage = getMean(vec1);
        double yAverage = getMean(vec2);

        double sumDifferencesX = 0;
        double sumDifferencesY = 0;
        for (int indexVec = 0; indexVec < n; indexVec++) {
            sumDifferencesX += vec1[indexVec] - xAverage;
            sumDifferencesY += vec2[indexVec] - yAverage;
        }

        return (1.0 / (n - 1.0)) * sumDifferencesX * sumDifferencesY;

    }

    public interface Function {

        public double eval(int x);
    }

}
