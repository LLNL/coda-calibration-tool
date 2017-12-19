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

package llnl.gnem.core.util.randomNumbers;

/**
 *
 * @author dodge1
 */
public abstract class BaseRandomAlgorithm implements RandomAlgorithm{

    @Override
    public abstract double nextDouble();

    @Override
    public abstract int nextInt();

    @Override
    public abstract int nextInt(int n);

    @Override
    public abstract long nextLong();

    @Override
    public int getBoundedInt(int lower, int upper) {
        int outRange = upper - lower;
        long min = Integer.MIN_VALUE;
        long max = Integer.MAX_VALUE;
        long inRange = max - min;
        long value = this.nextInt();
        double numerator = (value - min) * outRange;
        long result = lower + Math.round(numerator / inRange);
        return (int) result;
    }

    @Override
    public double getBoundedDouble(double lower, double upper) {
        double outRange = upper - lower;
        long min = Integer.MIN_VALUE;
        long max = Integer.MAX_VALUE;
        long inRange = max - min;
        long value = this.nextInt();
        double numerator = (value - min) * outRange;
        return lower + numerator / inRange;
    }
    
        /**
     * Produce a normally-distributed deviate using Box-Muller transformation.
     * Adapted from Numerical Recipes P. 203
     *
     * @param mean
     * @param std
     * @return normal deviate
     */
    @Override
    public double nextGaussian(double mean, double std) {
        double r = Double.MAX_VALUE;
        double v1 = 0;
        while (r >= 1) {
            v1 = 2 * nextDouble() - 1;
            double v2 = 2 * nextDouble() - 1;
            r = v1 * v1 + v2 * v2;
        }
        double value = v1 * Math.sqrt(-2 * Math.log(r) / r);
        return value * std + mean;
    }

    
}
