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

/***
 * @author Dave Harris
 ***/
public class HilbertIIR {

    private float x1, x2, x3;
    private float y, y1, y2;
    private float p, p1, p2;
    private float q, q1, q2;
    private float w, w1, w2;

    private float out1, out2;

    public HilbertIIR() {
        clearstates();
    } // constructor

    // evaluation operators

    public void clearstates() {
        x1 = 0.0f;
        x2 = 0.0f;
        x3 = 0.0f;
        y = 0.0f;
        y1 = 0.0f;
        y2 = 0.0f;
        p = 0.0f;
        p1 = 0.0f;
        p2 = 0.0f;
        q = 0.0f;
        q1 = 0.0f;
        q2 = 0.0f;
        w = 0.0f;
        w1 = 0.0f;
        w2 = 0.0f;
    }

    public void singlestep(float x) {
        y = 0.94167f * (y2 - x1) + x3;
        w = 0.53239f * (w2 - y) + y2;
        w2 = w1;
        w1 = w;
        y2 = y1;
        y1 = y;

        //

        p = 0.186540f * (p2 - x) + x2;
        q = 0.7902015f * (q2 - p) + p2;
        p2 = p1;
        p1 = p;
        q2 = q1;
        q1 = q;

        //

        x3 = x2;
        x2 = x1;
        x1 = x;

        //

        out1 = w;
        out2 = q;
    }

    public float getInPhase() {
        return out1;
    }

    public float getQuadrature() {
        return out2;
    }

}
