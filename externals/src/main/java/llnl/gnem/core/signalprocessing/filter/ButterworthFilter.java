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
package llnl.gnem.core.signalprocessing.filter;

import llnl.gnem.core.util.Passband;

/***
 * @author Dave Harris
 ***/
public class ButterworthFilter extends IIRFilter {
    /**
     * The Constructor for the ButterworthFilter that fully specifies the
     * filter.
     *
     * @param order
     *            The order of the filter
     * @param cutoff1
     *            The first cutoff frequency. For lowpass and highpass filters,
     *            this is the only cutoff values that matters and the other
     *            should be set to 0.0. For bandpass and bandrejhect filters
     *            this is the low frequency corner of the filter.
     * @param cutoff2
     *            For bandpass and bandreject filters, this is the high
     *            frequency corner. For other filters, this value should be 0.0.
     * @param T
     *            The sample interval in seconds.
     * @param passband
     *            The passband of the filter, e.g. LOW_PASS, HIGH_PASS, etc.
     */
    public ButterworthFilter(int order, Passband passband, double cutoff1, double cutoff2, double T) {
        super(new ButterworthAnalogFilter(order), passband, cutoff1, cutoff2, T);
    }
}
