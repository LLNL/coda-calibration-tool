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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;

@Component
public class SyntheticCodaModel {

    /**
     * log10(Ac(t)) = log10(A0) -gamma(r)*log10(t) + b(r) * t * log10(e)
     *
     * @param gr
     *            - the gamma(r) term: g(r) = g0 - g1/(g2+dist)
     * @param br
     *            - the b(r) term: b0 - b1/(b2+dist)
     * @param t
     *            - time in seconds from the phase start time
     * @return log10(Amplitude)
     *
     *         Note this method returns a point on the synthetic envelope to
     *         recover the full envelope use: for (ii = 1; ii < npts; ii++){ t =
     *         tstart + ii*dt; log10Ac(ii) = getSyntheticPointAtTime(gr,br,t); }
     */
    public Double getSyntheticPointAtTime(double gr, double br, double t) {
        if (t <= 0) {
            return -10.; // a default value for undefined and singularity points
        }
        // NOTE Kevin sets the log10(A0) term to 1.0 instead of 0.0
        double log10A0 = 1.0;
        // NOTE both Scott and Kevin wrap the constant log10(e) into the b(r)
        // variable
        return log10A0 - gr * Math.log10(t) + br * t;
    }

    public double getDistanceFunction(double value0, double value1, double value2, double distance) {
        return value0 - value1 / (value2 + distance);
    }

    /**
     * Get the value of the Empirical Synthetic Envelope at a specific distance
     * and measurement time
     *
     * @param params
     *            - a SharedFrequencyBandParameters containing the required
     *            v0,v1,v2, g0,g1,g2 and b0,b1,b2 values
     * @param distance
     *            - the source-receiver distance in kilometers
     * @param measurementtime
     *            - the time in seconds after the start of the coda
     * @return the amplitude value at that point
     */
    public double getPointAtTimeAndDistance(SharedFrequencyBandParameters params, double measurementtime, double distance) {
        double br = getDistanceFunction(params.getBeta0(), params.getBeta1(), params.getBeta2(), distance);
        double gr = getDistanceFunction(params.getGamma0(), params.getGamma1(), params.getGamma2(), distance);
        return getSyntheticPointAtTime(gr, br, measurementtime);
    }
}
