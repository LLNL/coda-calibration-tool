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

import java.util.Stack;

import llnl.gnem.core.gui.plotting.ZoomLimits;

/**
 * Class containing a JSubplot and its zoomdata.
 *
 * @author Doug Dodge
 */
class SubplotZoomData {

    private final JSubplot p;
    private final Stack<SubplotDisplayInfo> stack;
    private SubplotDisplayInfo initialState;
    private boolean displayable;

    /**
     * Constructor for the SubplotZoomData object
     *
     * @param p The JSubplot
     */
    public SubplotZoomData(JSubplot p) {
        this.p = p;
        stack = new Stack<>();
        initialState = null;
        displayable = true;
    }

    public void initLimits(ZoomLimits limits) {
        unzoomAll();
        initialState = getCurrentState();
        zoomIn(p.getCanDisplay(), limits);
    }

    public Stack<ZoomLimits> getZoomLimits() {
        Stack<ZoomLimits> result = new Stack<>();
        for (SubplotDisplayInfo sdi : stack) {
            result.push(sdi.Limits);
        }
        return result;
    }

    /**
     * Zoom in to the state held in the input argument
     *
     * @param newState The state to zoom to
     */
    public void zoomIn(boolean visible, ZoomLimits limits) {
        saveCurrentState();
        SubplotDisplayInfo newState = new SubplotDisplayInfo(visible && p.getCanDisplay(), limits);
        setState(newState);
    }

    /**
     * Zoom out to the last state stored in the state vector
     *
     * @return true if zoom is successful.
     */
    public boolean zoomOut() {
        if (!stack.isEmpty()) {
            setState(stack.pop());
            return true;
        }
        return false;
    }

    /**
     * Zoom to the first state in the state vector
     */
    public void unzoomAll() {
        if (initialState != null) {
            setState(initialState);
        } else if (!stack.isEmpty()) {
            setState(stack.get(0));
        }
        stack.clear();
    }

    public void setDisplayable(boolean v) {
        displayable = v;
    }

    public boolean isDisplayable() {
        return displayable;
    }

    public boolean isVisible() {
        return isDisplayable() && p.getCanDisplay();
    }

    /**
     * Gets the JSubplot
     *
     * @return The subplot value
     */
    public JSubplot getSubplot() {
        return p;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SubplotZoomData{" + "subplot(" + Integer.toHexString(System.identityHashCode(p)) + "), state=:");
        for (SubplotDisplayInfo sdi : stack) {
            sb.append("\n\t\t");
            sb.append(sdi);
        }
        sb.append(", displayable=" + displayable);
        return sb.toString();
    }

    private void saveCurrentState() {
        stack.push(getCurrentState());
    }

    private SubplotDisplayInfo getCurrentState() {
        XAxis ax = p.getXaxis();
        YAxis ay = p.getYaxis();
        ZoomLimits currentLimits = new ZoomLimits(ax.getMin(), ax.getMax(), ay.getMin(), ay.getMax());
        return new SubplotDisplayInfo(p.getCanDisplay(), currentLimits);
    }

    private void setState(SubplotDisplayInfo state) {
        XAxis ax = p.getXaxis();
        YAxis ay = p.getYaxis();

        p.setCanDisplay(displayable && state.displayable);

        ax.setMin(state.Limits.xmin);
        ax.setMax(state.Limits.xmax);
        ay.setMin(state.Limits.ymin);
        ay.setMax(state.Limits.ymax);
    }
}
