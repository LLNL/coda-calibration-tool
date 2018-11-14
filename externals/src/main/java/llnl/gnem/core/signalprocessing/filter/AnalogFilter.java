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

/***
 * @author Dave Harris
 ***/
class AnalogFilter {
    protected Vector<AnalogSecOrdSect> sections;

    // default constructor

    public AnalogFilter() {
        sections = new Vector<AnalogSecOrdSect>();
    }

    // copy constructor

    public AnalogFilter(AnalogFilter F) {
        sections = new Vector<AnalogSecOrdSect>(F.sections.size());
        for (int i = 0; i < sections.size(); i++) {
            sections.addElement(new AnalogSecOrdSect(F.get(i)));
        }
    }

    // Insertion operator

    public void print(PrintStream ps) {
        ps.println("Analog Filter: ");
        ps.print("  Number of second order sections: " + nSections());
        for (int i = 0; i < nSections(); i++) {
            ps.println("    Section " + i + ": ");
            (get(i)).print(ps);
        }
    }

    // accessors:

    public int nSections() {
        return sections.size();
    }

    AnalogSecOrdSect get(int i) {
        return sections.elementAt(i);
    }

    // mutators

    public void addSection(AnalogSecOrdSect S) {
        sections.addElement(S);
    }

    // evaluation:

    Complex evaluateAt(Complex s) {
        Complex H = new Complex(1.0, 0.0);
        for (int i = 0; i < sections.size(); i++) {
            H = H.multiply(get(i).evaluateAt(s));
        }
        return H;
    }

    // transformations:

    AnalogFilter LPtoLP(double fh) {
        AnalogFilter result = new AnalogFilter();
        for (int i = 0; i < sections.size(); i++) {
            result.addSection(get(i).LPtoLP(fh));
        }
        return result;
    }

    AnalogFilter LPtoHP(double fl) {
        AnalogFilter result = new AnalogFilter();
        for (int i = 0; i < sections.size(); i++) {
            result.addSection(get(i).LPtoHP(fl));
        }
        return result;
    }

    AnalogFilter LPtoBP(double fl, double fh) {
        AnalogFilter result = new AnalogFilter();
        Vector<AnalogSecOrdSect> V;
        for (int i = 0; i < sections.size(); i++) {
            V = get(i).LPtoBP(fl, fh);
            for (int j = 0; j < V.size(); j++) {
                result.addSection(V.elementAt(j));
            }
        }
        return result;
    }

    AnalogFilter LPtoBR(double fl, double fh) {
        AnalogFilter result = new AnalogFilter();
        AnalogSecOrdSect S;
        Vector<AnalogSecOrdSect> V;
        for (int i = 0; i < sections.size(); i++) {
            S = get(i).LPtoHP(1. / (2.0 * Math.PI));
            V = S.LPtoBP(fl, fh);
            for (int j = 0; j < V.size(); j++) {
                result.addSection(V.elementAt(j));
            }
        }
        return result;
    }

    // class functions:

    static double warp(double f, double ts) {
        return Math.tan(Math.PI * f * ts) / (Math.PI * ts);
    }
}
