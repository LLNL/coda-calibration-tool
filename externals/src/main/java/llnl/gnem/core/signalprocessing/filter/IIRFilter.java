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

import java.io.PrintStream;
import java.util.Vector;

import org.apache.commons.math3.complex.Complex;

import llnl.gnem.core.util.Passband;

/***
 * @author Dave Harris
 ***/
public class IIRFilter {
    protected Vector<SecOrdSection> sections;

    //  Constructor and Destructor:

    public IIRFilter() {
        sections = new Vector<SecOrdSection>();
    }

    //  Copy constructor and assignment

    public IIRFilter(IIRFilter F) {
        sections = new Vector<SecOrdSection>(F.sections.size());
        for (int i = 0; i < F.sections.size(); i++) {
            sections.addElement(F.get(i));
        }
    }

    //  Constructors:

    /**
     * Constructor for the IIRFilter object
     *
     * @param baseFilter
     *            Description of the Parameter
     * @param cutoff1
     *            The first cutoff frequency. For lowpass and highpass filters,
     *            this is the only cutoff values that matters and the other
     *            should be set to 0.0. For bandpass and bandrejhect filters
     *            this is the low frequency corner of the filter.
     * @param cutoff2
     *            For bandpass and bandreject filters, this is the high
     *            frequency corner. For other filters, this value should be 0.0.
     * @param T
     *            The sample interval in seconds of the data to be filtered
     * @param passband
     *            The passband of the filter, e.g. LOW_PASS, HIGH_PASS, etc.
     */
    public IIRFilter(AnalogFilter baseFilter, Passband passband, double cutoff1, double cutoff2, double T) {
        AnalogFilter prototype = null;
        if (passband == Passband.LOW_PASS) {
            prototype = baseFilter.LPtoLP(AnalogFilter.warp(cutoff1 * T / 2.0, 2.0));
        } else if (passband == Passband.HIGH_PASS) {
            prototype = baseFilter.LPtoHP(AnalogFilter.warp(cutoff1 * T / 2.0, 2.0));
        } else if (passband == Passband.BAND_PASS) {
            prototype = baseFilter.LPtoBP(AnalogFilter.warp(cutoff1 * T / 2.0, 2.0), AnalogFilter.warp(cutoff2 * T / 2.0, 2.0));
        } else if (passband == Passband.BAND_REJECT) {
            prototype = baseFilter.LPtoBR(AnalogFilter.warp(cutoff1 * T / 2.0, 2.0), AnalogFilter.warp(cutoff2 * T / 2.0, 2.0));
        }
        sections = new Vector<SecOrdSection>();
        if (prototype == null) {
            throw new IllegalStateException("Unable to initialize an AnalalogFilter while creating IIRFilter, possibly an unknown passband? Got band: "
                    + passband.toString()
                    + " and expected types are [LOW_PASS, HIGH_PASS, BAND_PASS, BAND_REJECT]");
        }
        for (int i = 0; i < prototype.nSections(); i++) {
            sections.addElement(new SecOrdSection(prototype.get(i), 2.0));
        }
    }

    //  Mutators

    public void addSection(SecOrdSection S) {
        sections.addElement(S);
    }

    //  Initializing sections:

    public void initialize() {
        for (int i = 0; i < sections.size(); i++) {
            get(i).clearstates();
        }
    }

    //  filtering:

    public float filter(float s) { // single step
        for (int i = 0; i < sections.size(); i++) {
            s = get(i).filter(s);
        }
        return s;
    }

    public void filter(float[] signal) {
        for (int i = 0; i < sections.size(); i++) {
            get(i).filter(signal);
        }
    }

    //  accessors:

    public int nSections() {
        return sections.size();
    }

    public SecOrdSection get(int i) {
        return sections.elementAt(i);
    }

    public Complex evaluateAt(double Omega) {
        Complex result = new Complex(1.0, 0.0);
        for (int i = 0; i < sections.size(); i++) {
            result = result.multiply(get(i).evaluateAt(Omega));
        }
        return result;
    }

    //  utilities:

    public void print(PrintStream ps) {
        ps.println("Digital filter: ");
        for (int i = 0; i < sections.size(); i++) {
            ps.println("\n  section " + (i + 1) + ":");
            get(i).print(ps);
        }
    }
}
