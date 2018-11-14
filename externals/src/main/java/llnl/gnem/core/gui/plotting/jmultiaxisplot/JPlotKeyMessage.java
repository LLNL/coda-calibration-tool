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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import java.awt.event.KeyEvent;

import llnl.gnem.core.gui.plotting.keymapper.ControlKeyMapper;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;

/**
 * A class containing information about selected plot objects and the keyboard
 * state. Instances of this class are sent to observers when the user selects a
 * plot object and enters a key code while the object is selected. Interested
 * observers can act on this information as desired.
 *
 * @author Doug Dodge
 */
public class JPlotKeyMessage {
    /**
     * Constructor for the JPlotKeyMessage object
     *
     * @param e
     *            The KeyEvent that triggered this message to be sent.
     * @param p
     *            The currently-selected subplot.
     * @param o
     *            The selected PlotObject. (Could be null if the user clicked
     *            inside the axis boundaries while entering the key
     *            combination.)
     * @param controlKeyMapper
     *            provides platform-specific key mappings
     */
    public JPlotKeyMessage(KeyEvent e, JSubplot p, PlotObject o, ControlKeyMapper controlKeyMapper) {
        this.keyEvent = e;
        this.subplot = p;
        this.plotObject = o;
        this.controlKeyMapper = controlKeyMapper;
    }

    /**
     * Gets the JSubplot
     *
     * @return The contained JSubplot
     */
    public JSubplot getSubplot() {
        return subplot;
    }

    /**
     * Gets the plotObject attribute of the JPlotKeyMessage object
     *
     * @return The plotObject value
     */
    public PlotObject getPlotObject() {
        return plotObject;
    }

    /**
     * Gets the keyEvent attribute of the JPlotKeyMessage object
     *
     * @return The keyEvent value
     */
    public KeyEvent getKeyEvent() {
        return keyEvent;
    }

    public ControlKeyMapper getControlKeyMapper() {
        return controlKeyMapper;
    }

    private final JSubplot subplot;
    private final PlotObject plotObject;
    private final KeyEvent keyEvent;
    private final ControlKeyMapper controlKeyMapper;
}
