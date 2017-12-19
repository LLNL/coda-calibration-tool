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
package llnl.gnem.core.gui.waveform;

import java.awt.event.KeyEvent;
import java.util.Observable;
import java.util.Observer;

import llnl.gnem.core.gui.plotting.MouseMode;
import llnl.gnem.core.gui.plotting.PickCreationInfo;
import llnl.gnem.core.gui.plotting.PlotObjectClicked;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JPlotKeyMessage;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickErrorChangeState;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PickMovedState;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.WindowDurationChangedState;

/**
 *
 * @author dodge1
 */
public class WaveformPlot extends JMultiAxisPlot implements Observer {
    private boolean ctrlKeyIsDown;
   
    public WaveformPlot() {
        this(MouseMode.SELECT_ZOOM, "");
        
    }
    
    public WaveformPlot(MouseMode defaultMode, String xAxisLabel) {
        super(defaultMode, XAxisType.Standard);
        ctrlKeyIsDown = false;
        getXaxis().setLabelText(xAxisLabel);
        addPlotObjectObserver(this);
    }

    @Override
    public void update(Observable observable, Object obj) {
        // TODO redo this so we're not having to do so many instanceof calls.
        if (obj instanceof JPlotKeyMessage) {
            handleKeyMessage(obj);
        } else if (obj instanceof KeyEvent) {
            KeyEvent keyEvent = (KeyEvent) obj;
            if (!keyEvent.isControlDown()) {
                ctrlKeyIsDown = false;
            }
        } else if (obj instanceof PlotObjectClicked) {
            handlePlotObjectClicked(obj);
        } else if (obj instanceof PickCreationInfo) {
            handlePickCreationInfo(obj);
        } else if (obj instanceof PickMovedState) {
            handlePickMovedState(obj);
        } else if (obj instanceof PickErrorChangeState) {
            handlePickErrorChangeState(obj);
        } else if (obj instanceof WindowDurationChangedState && !ctrlKeyIsDown) {
            resizeSingleWindow(obj);
        } else if (obj instanceof WindowDurationChangedState && ctrlKeyIsDown) {
            resizeAllWindows(obj);
        }
    }

    public void magnify() {
        scale(2.0);
    }

    public void reduce() {
        scale(0.5);
    }

    protected void handleKeyMessage(Object obj) {
        JPlotKeyMessage msg = (JPlotKeyMessage) obj;
        KeyEvent keyEvent = msg.getKeyEvent();
        if (keyEvent.isControlDown()) {
            ctrlKeyIsDown = true;
        }
    }

    protected void handlePlotObjectClicked(Object obj) {
    }

    protected void handlePickCreationInfo(Object obj) {
    }

    protected void handlePickMovedState(Object obj) {
    }

    protected void handlePickErrorChangeState(Object obj) {
    }

    protected void resizeSingleWindow(Object obj) {
    }

    protected void resizeAllWindows(Object obj) {
    }


}
