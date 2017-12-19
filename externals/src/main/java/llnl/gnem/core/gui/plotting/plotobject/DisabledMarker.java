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
package llnl.gnem.core.gui.plotting.plotobject;


import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import llnl.gnem.core.gui.plotting.JBasicPlot;


public class DisabledMarker extends PlotObject {

    /**
     * render this Symbol to the supplied graphics context
     *
     * @param g     The graphics context
     * @param owner The JBasicPlot that owns this symbol
     */
    @Override
    public void render( Graphics g, JBasicPlot owner )
    {
        if( g == null || !visible || owner == null || !owner.getCanDisplay() )
            return;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaintMode(); // Make sure that we are not in XOR mode.

        int top = owner.getPlotTop();
        int height = owner.getPlotHeight();
        int left = owner.getPlotLeft();
        int width = owner.getPlotWidth();
        g2d.setPaint(Color.red);
        g2d.drawLine(left,top,left + width, top + height);
        g2d.drawLine(left,top+height,left+width,top);
    }



    @Override
    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {
        // This object is not allowed to change position.
    }


}