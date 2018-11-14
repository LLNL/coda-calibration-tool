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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.Map;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

public class PathCostFunctionResult {

    private Map<Event, Map<Station, Double>> residuals;
    private double cost;
    private double mean;
    private Map<Event, Map<Station, Double>> siteCorrections;

    public PathCostFunctionResult(Map<Event, Map<Station, Double>> residuals, double cost, double mean, Map<Event, Map<Station, Double>> siteCorrections) {
        this.residuals = residuals;
        this.cost = cost;
        this.mean = mean;
        this.siteCorrections = siteCorrections;
    }

    public Map<Event, Map<Station, Double>> getResiduals() {
        return residuals;
    }

    public void setResiduals(Map<Event, Map<Station, Double>> residuals) {
        this.residuals = residuals;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public Map<Event, Map<Station, Double>> getSiteCorrections() {
        return siteCorrections;
    }

    public void setSiteCorrections(Map<Event, Map<Station, Double>> siteCorrections) {
        this.siteCorrections = siteCorrections;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(cost);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mean);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((residuals == null) ? 0 : residuals.hashCode());
        result = prime * result + ((siteCorrections == null) ? 0 : siteCorrections.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PathCostFunctionResult other = (PathCostFunctionResult) obj;
        if (Double.doubleToLongBits(cost) != Double.doubleToLongBits(other.cost))
            return false;
        if (Double.doubleToLongBits(mean) != Double.doubleToLongBits(other.mean))
            return false;
        if (residuals == null) {
            if (other.residuals != null)
                return false;
        } else if (!residuals.equals(other.residuals))
            return false;
        if (siteCorrections == null) {
            if (other.siteCorrections != null)
                return false;
        } else if (!siteCorrections.equals(other.siteCorrections))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "PathCostFunctionResult [residuals=" + residuals + ", cost=" + cost + ", mean=" + mean + ", siteCorrections=" + siteCorrections + "]";
    }

}
