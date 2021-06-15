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
package llnl.gnem.core.gui.swing.plotting;

/**
 * A type-safe enum class for interpretation of mouse actions
 *
 * @author Doug Dodge
 */
public enum MouseMode {
    SELECT_ZOOM("select_zoom"), // Left button down marks a selection attempt or start of a zoom
    PAN("pan"), // Left button down marks start of panning
    PAN2("pan2"), // two-D pan
    ZOOM_ONLY("zoomOnly"), // Left button down marks start of zoom only
    CONTROL_SELECT("ControlSelect"), // Indicates that the Control Key was depressed while this mouse action was made.
    SELECT_REGION("RegionSelect"),
    /**
     * Indicates that the mouse can only be used for selecting a region. Region
     * selection causes no change to the plot. but any registered observers will
     * be notified of the action, and given the specifications of the selected
     * region.
     */
    CREATE_PICK("CreatePick"),
    /**
     * In this mode, the mouse listener will interpret left-mouse clicks as an
     * attempt to create a new pick.
     */
    CREATE_POLYGON("CreatePolygon");

    private final String name;

    MouseMode(String name) {
        this.name = name;
    }

    /**
     * Return a String description of this type.
     *
     * @return The String description
     */
    @Override
    public String toString() {
        return name;
    }

}
