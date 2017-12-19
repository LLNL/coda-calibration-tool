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
package llnl.gnem.core.util;

import java.io.IOException;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: dodge1
 * Date: Feb 18, 2004
 * Time: 1:02:20 PM
 */
public class Utility {

    /**
     * Given a Vector of Strings, return a Single String which is a comma-separated, list of single-qouted Strings
     * one for each element in the Vector. For example. If the input Vector contains the Strings
     * "Hello" and "World", this method will return the String "'Hello', 'World'"
     *
     * @param str A vector containing Strings that are to be returned in a single-quoted list
     * @return The String containing a comma-separated, single-quoted list of Strings
     */
    public static String getQuotedList(List<String> str) {
        StringBuilder sb = new StringBuilder();
        int N = str.size();
        for (int j = 0; j < N; ++j) {
            sb.append('\'').append(str.get(j)).append('\'');
            if (j < N - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static String BuildCommaDelimitedValueString(String[] intStrings) {

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < intStrings.length; i++) {
            sb.append(intStrings[i]);
            if (i < intStrings.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    public static void testInternetAccess(boolean quitOnFailure) throws IOException {
    }

    public static class CheckInternetConnectionTask extends TimerTask {

        private final boolean quitOnFailure;

        public CheckInternetConnectionTask() {
            quitOnFailure = true;
        }

        public CheckInternetConnectionTask(boolean quitOnFailure) {
            this.quitOnFailure = quitOnFailure;
        }

        @Override
        public void run() {
            try {
                testInternetAccess(quitOnFailure);
            } catch (IOException ex) {
                Logger.getLogger(Utility.class.getName()).log(Level.SEVERE, null, ex);
            }

        }
    }

    public static double log10(double x) {
        return Math.log10(x);
    }

    /**
     * Method to round a double value to the given precision
     * @param val The value to be rounded.
     * @param precision The number of decimal places to round to.
     * Will not inspect for: MagicNumber
     * @return  The rounded value.
     */
    public static double round(double val, int precision) {

        // Multiply by 10 to the power of precision and add 0.5 for rounding up
        // Take the nearest integer smaller than this value
        double aval = Math.floor(val * Math.pow(10, precision) + 0.5);

        // Divide it by 10**precision to get the rounded value
        return aval / Math.pow(10, precision);
    }

    /**
     * Returns the coefficients of a straight line fit to the data in input arrays
     * x and y between the points idx1 through idx2 inclusive.
     * @param x A double array containing the x-values.
     * @param y A double array containing the y-values.
     * @param idx1  The index of the first element in the section to be fitted.
     * @param idx2 The index of the last element to include in the fit.
     * @return A Pair object whose first object is the intercept as a Double and whose second object
     * is the slope as a Double. If the data being fit define a vertical line, the
     * slope object will have the value Double.MAX_VALUE.
     */
    public static Pair getLineCoefficients(double[] x, double[] y, int idx1, int idx2) {
        int n = x.length;
        if (y.length != n) {
            throw new IllegalArgumentException("Input arrays are not equal in length!");
        }
        if (idx1 < 0 || idx1 >= n) {
            throw new IllegalArgumentException("Index 1 is out of bounds!");
        }
        if (idx2 < 0 || idx2 >= n) {
            throw new IllegalArgumentException("Index 2 is out of bounds!");
        }
        if (idx2 < idx1 + 1) {
            throw new IllegalArgumentException("Index 2 must be at least one greater than index 1!");
        }




        // First determine the slope and intercept of the best fitting line...
        int nSamps = idx2 - idx1 + 1;
        double xbar = 0.0;
        for (int j = idx1; j <= idx2; ++j) {
            xbar += x[j];
        }
        xbar /= nSamps;

        double ybar = 0.0;
        for (int j = idx1; j <= idx2; ++j) {
            ybar += y[j];
        }
        ybar /= nSamps;

        double SSX = 0.0;
        double SSXY = 0.0;
        for (int j = idx1; j <= idx2; ++j) {
            double tmp1 = x[j] - xbar;
            double tmp2 = y[j] - ybar;
            SSX += tmp1 * tmp1;
            SSXY += tmp1 * tmp2;
        }
        double B1 = Double.MAX_VALUE;
        if (SSX > Double.MIN_VALUE) {
            B1 = SSXY / SSX;
        }

        // The slope

        double B0 = ybar - B1 * xbar;
        return new Pair(B0, B1);

    }
}
