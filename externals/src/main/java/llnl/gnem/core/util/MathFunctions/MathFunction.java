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
package llnl.gnem.core.util.MathFunctions;

import java.security.SecureRandom;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is for general math functions not available in java.Math
 *
 * User: matzel Date: Aug 2, 2006 Time: 4:05:28 PM
 */
public class MathFunction {

    private static final Logger log = LoggerFactory.getLogger(MathFunction.class);

    private static final SecureRandom rng = new SecureRandom();

    /**
     * The Heavyside step function H(x) = 0.0 when x &lt; 0 = 0.5 when x == 0 =
     * 1.0 when x &gt; 0
     *
     * @param value
     *            the original value
     * @return Heavyside(value)
     */
    public static double Heaviside(double value) {
        if (value < 0) {
            return 0;
        } else if (value == 0) {
            return 0.5;
        } else {
            return 1;
        }
    }

    /**
     * Find the closest power of 2 to the input number
     *
     * e.g. 5 ==&gt; 4, 100 ==&gt; 128, 2==&gt;2 etc.
     *
     * @param num
     *            a long valued variable
     * @return the nearest power of 2 greater than num
     */
    public static long closestPowerOf2(long num) {
        if (num < 0) {
            return 0;
        } else {
            String bits = Long.toBinaryString(num);
            int exp = bits.length() - bits.indexOf('1');
            long larger = (long) Math.pow(2, exp); // next power of 2 greater than num
            long lower = (long) Math.pow(2, exp - 1); // previous power of 2 less than num

            return (larger - num) > (num - lower) ? lower : larger;
        }
    }

    /**
     * Find the next power of 2 greater than the input number
     *
     * e.g. 5 ==&gt; 8, 100 ==&gt; 128, 2==&gt;2 etc.
     *
     * @param num
     *            a long valued variable
     * @return the nearest power of 2 greater than num
     */
    public static long nextPowerOf2(long num) {
        if (num < 0) {
            return 0;
        } else {
            String bits = Long.toBinaryString(num);
            int exp = bits.length() - bits.indexOf('1');
            return (long) Math.pow(2, exp); // next power of 2 greater than num
        }
    }

    /**
     * Round up an integer to the next power of 2
     *
     * e.g. 5 ==&gt; 8, 100 ==&gt; 128, 2==&gt;2 etc.
     *
     * @param num
     *            an integer
     * @return the smallest power of 2 greater than num
     */
    public static int nextPowerOf2(int num) {
        return (int) nextPowerOf2((long) num);
    }

    /**
     * Find the closest power of 2 to the input number
     *
     * e.g. 5 ==&gt; 4, 100 ==&gt; 128, 2==&gt;2 etc.
     *
     * @param num
     *            an integer
     * @return the closest power of 2
     */
    public static int closestPowerOf2(int num) {
        return (int) closestPowerOf2((long) num);
    }

    /**
     * Bessel function of the first kind
     *
     * Jnu(z) = Sumk [ (-1)^k * (z/2)^(2k+nu) / Gamma(k+nu+1)*k! sum k from 0 to
     * infinity J0(0) = 1
     *
     * public static Complex Bessel(int n, double z) { } public static Complex
     * Bessel2(int n, double z) { } public static Complex Hankel1(int n, double
     * z) { Complex Hnz = Jnz + imag*Ynz; return Hnz; }
     *
     * public static Complex Hankel2(int n, double z) { Complex Hnz = Jnz -
     * imag*Ynz; return Hnz; }
     */
    /**
     * The Euler Gamma function (ref: functions.wolfram.com)
     *
     * Gamma(z) = Integral(0-inf) [ t^(z-1) * e^(-t) * dt ]
     *
     * below is an approximate expansion (to order 10)
     *
     * |Arg(z)| &lt; pi /\ (|z| --&gt; inf.)
     */
    public static double Gamma(double z) {
        double sqrt2PI = Math.sqrt(2 * Math.PI);
        double zz2 = Math.pow(z, z - 0.5); // z^(z-0.5)
        double ez = Math.exp(-z); // e^(-z)

        double z2 = z * z;
        double z3 = z2 * z;
        double z4 = z3 * z;
        double z5 = z4 * z;
        double z6 = z5 * z;
        double z7 = z6 * z;
        double z8 = z7 * z;
        double z9 = z8 * z;
        double z10 = z9 * z;

        //
        double o1 = 1d / 12d * z;
        double o2 = 1d / 288d * z2;
        double o3 = -139d / 51840d * z3;
        double o4 = -571d / 2488320d * z4;
        double o5 = 163879d / 209018880d * z5;
        double o6 = 5246819d / 75246796800d * z6;
        double o7 = -534703531d / 902961561600d * z7;
        double o8 = -4483131259d / 86684309913600d * z8;
        double o9 = 432261921612371d / 514904800886784000d * z9;
        double o10 = 1d / z10;// inexact term

        double terms = o1 + o2 + o3 + o4 + o5 + o6 + o7 + o8 + o9 + o10;
        double gamma = sqrt2PI * zz2 * ez * terms;

        return gamma;
    }

    public static double factorial(double z) {
        return Gamma(z + 1);
    }

    /**
     * the factorial of an integer
     */
    public static double factorial(int n) throws Exception {
        if (n < 0) {
            throw new Exception("result is infinity");
        }

        if (n == 0) {
            return 1;
        }

        double result = 1.;

        for (int ii = 1; ii <= n; ii++) {
            result = result * ii;
        }

        return result;
    }

    /**
     * Binomial(n,k) == n! / k! * (n - k )!
     *
     * //todo verify the conditionals
     */
    public static double binomial(int n, int k) throws Exception {
        if (k < 0) {
            return 0;
        }
        if (k > n) {
            return 0;
        }

        if (k == 0) {
            return 1;
        }

        double result = factorial(n) / (factorial(k) * factorial(n - k));
        return result;
    }

    /**
     * Generalized power series for the error function erf(z)
     *
     * erf(z) = 2/sqrt(PI) * SUM(k = 0:inf) [ -1^k * z^(2k+1) / (k!*(2k+1)) ]
     */
    public static double erf(double z) {
        int kmax = 10;//todo decide on a reasonable cutoff point

        if (z == 0) {
            return 0;
        }

        double sqrtPI2 = 2. / Math.sqrt(Math.PI);
        double powerseries = 0;

        for (int k = 0; k < kmax; k++) {
            try {
                double numerator = Math.pow(-1, k) * Math.pow(z, 2d * k + 1d);
                double denominator = factorial(k) * (2d * k + 1d);
                powerseries = powerseries + numerator / denominator;
            } catch (Exception e) {
                log.warn("you should never reach this exception: erf(z)");
            }
        }

        return sqrtPI2 * powerseries;

    }

    /**
     * Routine to reproduce the results of the SAC codes linrng_v function
     * (linrng.c)
     *
     * @param value
     *            - the actual value
     * @param min
     *            - the minimum value
     * @param max
     *            - the maximum value
     * @return - true if the value falls between the minimum and the maximum
     */
    public static boolean isBetween(double value, double min, double max) {
        return (value >= min && value <= max);
    }

    /**
     * create a random value between two values
     *
     * @param low
     * @param high
     * @return
     */
    public static double randomBetween(double low, double high) {
        double random = rng.nextDouble();

        double scale = high - low;

        random = low + random * scale;

        return random;

    }

    //------------ Functions on Vectors --------------------------------------
    /**
     * Given 2 vectors, A and B, get the cosine of the angle between the two.
     *
     * A.B = |A||B| cos(theta)
     *
     * @param vectorA
     *            : the first N-dimensional vector
     * @param vectorB
     *            : the second N-dimensional vector
     * @return cos(theta)
     *
     */
    public Double getCosineTheta(List<Double> vectorA, List<Double> vectorB) {
        Double AdotB = dotprod(vectorA, vectorB);
        Double normA = L2norm(vectorA);
        Double normB = L2norm(vectorB);

        Double costheta = AdotB / (normA * normB);
        return costheta;
    }

    public Double dotprod(List<Double> vectorA, List<Double> vectorB) {
        Double result = 0.;

        if (vectorA.size() == vectorB.size()) {
            for (int index = 0; index < vectorA.size(); index++) {
                Double elementproduct = vectorA.get(index) * vectorB.get(index);
                result = result + elementproduct;
            }
        } else {
            return null;
        }

        return result;
    }

    /**
     * Calculate the L2 norm: Sqrt(Sum of the Squares of an N-dimensional
     * vector)
     *
     * @param ndimensionalvector
     * @return
     */
    public static double L2norm(List<Double> ndimensionalvector) {
        double sumofsquares = 0;
        for (Double element : ndimensionalvector) {
            double squarevalue = element * element;
            sumofsquares = sumofsquares + squarevalue;
        }
        double result = Math.sqrt(sumofsquares);
        return result;
    }

}
