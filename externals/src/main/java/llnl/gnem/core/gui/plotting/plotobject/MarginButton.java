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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.GeneralPath;

import javax.swing.ImageIcon;

import llnl.gnem.core.gui.plotting.JBasicPlot;

/**
 * Created by dodge1
 * Date: Mar 31, 2010
 */
public class MarginButton extends PlotObject {
    private final MarginButtonIconGroup icons;
    private int state;
    private boolean enabled;
    private int vOffset;
    private int hOffset;

    public MarginButton(MarginButtonIconGroup icons) {
        this.icons = icons;
        state = 0;
        vOffset = 1;
        hOffset = 1;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public void render(Graphics g, JBasicPlot owner) {
        region.clear();
        Graphics2D g2d = (Graphics2D) g;
        ImageIcon icon = icons.getIcon(state);
        if (!enabled)
            icon = icons.getIcon(3);
        Image image = icon.getImage();
        int top = owner.getPlotTop();
        int height = owner.getPlotHeight();
        int vpos = top + height - icon.getIconHeight() - vOffset;
        int xleft = owner.getPlotLeft() + owner.getPlotWidth() - icon.getIconWidth() - hOffset;
        g2d.drawImage(image, xleft, vpos, null);
        GeneralPath box = new GeneralPath();
        box.moveTo(xleft, vpos);
        box.lineTo(xleft + icon.getIconWidth(), vpos);
        box.lineTo(xleft + icon.getIconWidth(), vpos + icon.getIconHeight());
        box.lineTo(xleft, vpos + icon.getIconHeight());
        box.lineTo(xleft, vpos);
        addToRegion(box);
    }

    public void setToRollOverState(Graphics g) {
        owner = this.getOwner();
        if (enabled && state != 1) {
            state = 1;
            render(g, owner);
        }
    }

    public void setToNormalState(Graphics g) {
        if (enabled && state != 0) {
            state = 0;

            owner = this.getOwner();
            render(g, owner);
        }
    }

    public void setToDepressedState(Graphics g) {
        if (enabled && state != 2) {
            state = 2;
            owner = this.getOwner();
            render(g, owner);
        }
    }

    @Override
    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {
        // Not implemented in this subclass.
    }

    public void setVOffset(int vOffset) {
        this.vOffset = vOffset;
    }

    public void setHOffset(int hOffset) {
        this.hOffset = hOffset;
    }
}
