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

/**
 *
 * @author dodge1
 */
public class Discontinuity {

    private final int index;
    private final double time;
    private final double relativeDeviation;
    private final SampleStatistics statistics;

    public Discontinuity(int index, double time, double relativeDeviation, SampleStatistics statistics) {
        this.index = index;
        this.time = time;
        this.relativeDeviation = relativeDeviation;
        this.statistics = statistics;
    }

    /**
     * @return the index
     */
    public int getIndex() {
        return index;
    }

    /**
     * @return the time
     */
    public double getTime() {
        return time;
    }

    /**
     * @return the relativeDeviation
     */
    public double getRelativeDeviation() {
        return relativeDeviation;
    }
    
    public double getKurtosis()
    {
        return statistics.getKurtosis();
    }

    @Override
    public String toString() {
        return String.format("discontinuity at %d (%f) with relative deviation = %f, Kurtosis = %f",
                index, time, relativeDeviation, statistics.getKurtosis());
    }
}