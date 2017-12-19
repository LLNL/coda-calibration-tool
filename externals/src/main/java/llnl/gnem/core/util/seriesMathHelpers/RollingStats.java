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

import llnl.gnem.core.util.SeriesMath;

/**
 * http://en.wikipedia.org/wiki/Standard_deviation#Rapid_calculation_methods
 * @author dodge1
 */
public class RollingStats {

    private final int n;
    private double mean;
    private double s1;
    private double s2;

    public RollingStats(float[] data) {
        n = data.length;
        mean = SeriesMath.getMean(data);
        s1 = SeriesMath.getSum(data);
        s2 = SeriesMath.getSumOfSquares(data);
    }

    public void replace(float oldDatum, float newDatum) {
        mean += (newDatum / n) - (oldDatum / n);
        s1 += newDatum - oldDatum;
        s2 += (newDatum * newDatum) - (oldDatum * oldDatum);
    }

    public double getMean() {
        return mean;
    }

    public double getStandardDeviation() {
        return Math.sqrt((n * s2 - s1 * s1) / (n * (n-1)));
    }
    
    @Override
    public String toString()
    {
        return String.format("Mean = %f, std = %f", getMean(), getStandardDeviation());
    }
}