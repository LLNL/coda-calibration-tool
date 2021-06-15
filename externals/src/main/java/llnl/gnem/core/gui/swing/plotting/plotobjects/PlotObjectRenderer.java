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
package llnl.gnem.core.gui.swing.plotting.plotobjects;

import java.awt.Graphics;

import llnl.gnem.core.gui.swing.plotting.JBasicPlot;
import llnl.gnem.core.gui.swing.plotting.JPlotContainer;

/**
 * User: dodge1 Date: Jun 8, 2005 Time: 1:26:22 PM
 */
public class PlotObjectRenderer implements Runnable {
    private PlotObject obj;

    public PlotObjectRenderer(PlotObject obj) {
        this.obj = obj;
    }

    public void run() {
        JBasicPlot plot = obj.getOwner();
        if (plot != null) {
            JPlotContainer container = plot.getOwner();
            if (container != null) {
                Graphics g = container.getGraphics();
                if (g != null)
                    obj.render(g, plot);
            }
        }
    }
}
