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
package llnl.gnem.core.gui.swing.plotting.jmultiaxisplot;

import llnl.gnem.core.gui.swing.plotting.ZoomLimits;

/**
 * Class containing a JSubplot and a subregion within the JSubplot. Used for
 * passing selection messages from the JMultiAxisPlot MouseListener to
 * interested observers.
 *
 * @author Doug Dodge
 */
public class SubplotSelectionRegion {
    /**
     * Constructor for the SubplotSelectionRegion object
     *
     * @param p
     *            The JSubplot
     * @param region
     *            The selected region within this subplot
     */
    public SubplotSelectionRegion(JSubplot p, ZoomLimits region) {
        this.p = p;
        this.region = region;
    }

    /**
     * Gets the JSubplot
     *
     * @return The subplot value
     */
    public JSubplot getSubplot() {
        return p;
    }

    /**
     * Gets the selectedRegion for the contained JSubplot
     *
     * @return The selected Region
     */
    public ZoomLimits getSelectedRegion() {
        return region;
    }

    private JSubplot p;
    private ZoomLimits region;
}
