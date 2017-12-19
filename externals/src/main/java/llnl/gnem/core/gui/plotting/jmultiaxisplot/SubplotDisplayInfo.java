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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import llnl.gnem.core.gui.plotting.ZoomLimits;

/**
 * Class containing the information controlling display and limits for a single
 * JSubplot.
 *
 * @author Doug Dodge
 */
class SubplotDisplayInfo {
    /**
     * Constructor for the SubplotDisplayInfo object
     *
     * @param displayable Whether this JSubplot should be displayed
     * @param current     The current axis limits for this JSubplot
     */
    public SubplotDisplayInfo( boolean displayable, ZoomLimits current )
    {
        this.displayable = displayable;
        this.Limits = new ZoomLimits( current );
    }

    boolean displayable;

    @Override
    public String toString() {
        return "SubplotDisplayInfo{" + "displayable=" + displayable + ", Limits=" + Limits + '}';
    }
    ZoomLimits Limits;
}


