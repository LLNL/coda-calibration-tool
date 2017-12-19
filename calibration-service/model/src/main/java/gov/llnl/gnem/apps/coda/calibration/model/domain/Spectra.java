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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.util.ArrayList;
import java.util.List;

import gov.llnl.gnem.apps.coda.calibration.model.domain.util.SPECTRA_TYPES;

public class Spectra {

    private List<java.awt.geom.Point2D.Double> xyVals;
    private double stressDrop = -1;
    private double mw = -1;
    private SPECTRA_TYPES type;

    /**
     * @param xyVals
     *            List of java.awt.geom.Point2D.Double entries representing X, Y
     *            points
     * @param mw
     * @param stressDrop
     */
    public Spectra(SPECTRA_TYPES type, List<java.awt.geom.Point2D.Double> xyVals, Double mw, Double stressDrop) {
        this.type = type;
        this.xyVals = xyVals;
        if (mw != null) {
            this.mw = mw;
        }
        if (stressDrop != null) {
            this.stressDrop = stressDrop;
        }
    }

    public Spectra() {
        this.type = SPECTRA_TYPES.UNK;
        xyVals = new ArrayList<>();
    }

    public List<java.awt.geom.Point2D.Double> getSpectraXY() {
        return xyVals;
    }

    public double getStressDrop() {
        return stressDrop;
    }

    public double getMw() {
        return mw;
    }

    public SPECTRA_TYPES getType() {
        return type;
    }
}
