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
package llnl.gnem.core.util.seriesMathHelpers;

import java.util.Map;
import java.util.TreeMap;

/**
 *
 * @author dodge1
 */
public class DiscontinuityCollection {

    Map<Integer, Discontinuity> items;
    private int numItems;
    private double averageDeviation;
    private double maxDeviation;
    private double averageKurtosis;
    private double maxKurtosis;
    private boolean statsCalculated = false;

    public DiscontinuityCollection() {
        items = new TreeMap<Integer, Discontinuity>();
    }

    public DiscontinuityCollection(Discontinuity item) {
        items = new TreeMap<Integer, Discontinuity>();
        items.put(item.getIndex(), item);
    }

    public void add(Discontinuity item) {
        items.put(item.getIndex(), item);
    }

    public int size() {
        return items.size();
    }

    @Override
    public String toString() {
        if (!statsCalculated) {
            computeStatistics();
        }
        return String.format("%d discontinuities with average deviation of %f", numItems, averageDeviation);
    }

    public void computeStatistics() {
        numItems = items.size();
        averageDeviation = 0;
        maxDeviation = 0;
        averageKurtosis = 0;
        maxKurtosis = 0;
        if (numItems > 0) {
            for (Discontinuity d : items.values()) {
                averageDeviation += d.getRelativeDeviation();
                if (d.getRelativeDeviation() > maxDeviation) {
                    maxDeviation = d.getRelativeDeviation();
                }
                averageKurtosis += d.getKurtosis();
                if (d.getKurtosis() > maxKurtosis) {
                    maxKurtosis = d.getKurtosis();
                }
            }
            averageDeviation /= numItems;
            averageKurtosis /= numItems;
        }
    }

    /**
     * @return the numItems
     */
    public int getNumItems() {
        if (!statsCalculated) {
            computeStatistics();
        }
        return numItems;
    }

    /**
     * @return the maxDeviation
     */
    public double getMaxDeviation() {
        if (!statsCalculated) {
            computeStatistics();
        }
        return maxDeviation;
    }

    /**
     * @return the averageKurtosis
     */
    public double getAverageKurtosis() {
        if (!statsCalculated) {
            computeStatistics();
        }
        return averageKurtosis;
    }

    /**
     * @return the maxKurtosis
     */
    public double getMaxKurtosis() {
        if (!statsCalculated) {
            computeStatistics();
        }
        return maxKurtosis;
    }

    /**
     * @return the averageDeviation
     */
    public double getAverageDeviation() {
        if (!statsCalculated) {
            computeStatistics();
        }
        return averageDeviation;
    }
}
