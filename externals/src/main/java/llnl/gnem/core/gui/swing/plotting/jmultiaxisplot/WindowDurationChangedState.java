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

/**
 * Created by: dodge1 Date: Dec 3, 2004
 */
public class WindowDurationChangedState {
    private final JSubplot sp;
    private final JWindowHandle handle;
    private final double startDuration;
    private double deltaD;

    public WindowDurationChangedState(JWindowHandle handle, JSubplot sp, double start) {
        this.sp = sp;
        this.handle = handle;
        this.startDuration = start;
        this.deltaD = 0.0;
    }

    public JWindowHandle getWindowHandle() {
        return handle;
    }

    public JSubplot getSubplot() {
        return sp;
    }

    public void finishChange() {
        deltaD = handle.getAssociatedPick().getWindow().getDuration() - startDuration;
    }

    public double getDeltaD() {
        return deltaD;
    }
}
