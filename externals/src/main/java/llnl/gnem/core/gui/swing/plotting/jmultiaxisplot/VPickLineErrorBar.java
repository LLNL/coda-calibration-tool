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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;

import llnl.gnem.core.gui.swing.plotting.JBasicPlot;
import llnl.gnem.core.gui.swing.plotting.XaxisDir;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;
import llnl.gnem.core.gui.swing.plotting.transforms.CartesianTransform;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;

/**
 * A class that collaborates with VPickLine to display interactive error bars
 * that are associated with a single pick.
 *
 * @author Doug Dodge
 */
public class VPickLineErrorBar extends PlotObject {
    /**
     * Gets the error value associated with the VPickLineErrorBar object. This
     * is the absolute value of the difference between the VPickLine data value
     * and the data value of one of the associated error bars.
     *
     * @return The error value
     */
    public double getStd() {
        return std;
    }

    /**
     * Sets the value of the error for the VPickLineErrorBar object. This method
     * does not cause the error bar to be re-rendered.
     * 
     * @param v
     *            The new error value in real-world coordinates
     */
    public void setStd(double v) {
        std = v >= 0 ? v : 0;
        PickDataBridge wd = associatedPick.getDataBridge();
        if (wd != null) {
            wd.setDeltim(std, this);
        }
    }

    /**
     * Sets the bracket Width of the VPickLineErrorBar object. Each error bar
     * has an inward-facing bracket at top and bottom. The width of this bracket
     * is specified in millimeters.
     *
     * @param v
     *            The new bracket Width value in millimeters
     */
    public void setBracketWidth(double v) {
        BracketWidth = Math.min(v, 0.0);
    }

    /**
     * Sets the handle Width of the VPickLineErrorBar object. Each error bar has
     * an optional square handle that may make it easier to select with the
     * mouse. This method sets the size of a side of the squares in millimeters.
     *
     * @param v
     *            The new handle Width value in millimeters.
     */
    public void setHandleWidth(double v) {
        handleWidth = Math.min(v, 0.0);
    }

    /**
     * Controls whether the error-bar handle is displayed.
     *
     * @param v
     *            The new showHandles value
     */
    public void setShowHandles(boolean v) {
        showHandles = v;
    }

    /**
     * Constructor for the VPickLineErrorBar object
     *
     * @param associatedPick
     *            A reference to the VPickLine object associated with this error
     *            bar
     * @param std
     *            The separation distance in real-world coordinates of this
     *            error bar from the VPickLine.
     * @param dir
     *            The specification of which side of the VPickLine to place this
     *            error bar.
     */
    public VPickLineErrorBar(VPickLine associatedPick, double std, PickErrorDir dir) {
        this.std = std;
        this.associatedPick = associatedPick;
        canDragY = false;
        canDragX = associatedPick.getDraggable();
        BracketWidth = 3.0;
        handleWidth = 2.0;
        color = associatedPick.getColor();
        selectedColor = Color.red;
        selected = false;
        renderColor = color;
        showHandles = true;
        visible = false;
        this.dir = dir;
    }

    /**
     * Sets the color of the VPickLineErrorBar object
     *
     * @param c
     *            The new color value
     */
    public void setColor(Color c) {
        color = c;
        renderColor = c;
    }

    /**
     * Change the position of this error bar. This method should only be called
     * by the mouse motion listener associated with the JMultiAxisPlot
     * containing this object. The method notifies any observers registered with
     * its PickDataBridge, so if called by an observer could result in inifinite
     * recursion.
     *
     * @param axis
     *            The JBasicPlot that owns this error bar
     * @param graphics
     * @param dx
     *            The x-shift in real-world coordinates
     * @param dy
     *            The y-shift in real-world coordinates
     */
    @Override
    public void ChangePosition(JBasicPlot axis, Graphics graphics, double dx, double dy) {
        Graphics g = axis.getOwner().getGraphics();
        render(g, axis);
        CartesianTransform ct = (CartesianTransform) axis.getCoordinateTransform();
        if (dir == PickErrorDir.RIGHT) {
            if (ct.getXAxisDir() == XaxisDir.RIGHT) {
                std += dx;
            } else {
                std -= dx;
            }
        } else if (ct.getXAxisDir() == XaxisDir.RIGHT) {
            std -= dx;
        } else {
            std += dx;
        }
        if (std < 0) {
            std = 0;
        }
        associatedPick.UpdateOther(this, axis, g);
        PickDataBridge wd = associatedPick.getDataBridge();
        if (wd != null) {
            wd.setDeltim(std, this);
        }
        render(g, axis);
    }

    /**
     * Produce a String representation of this object
     *
     * @return The String representation
     */
    @Override
    public String toString() {
        StringBuffer s = new StringBuffer("Pick Error Bar: Std = ");
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(5);
        s.append(f.format(std));
        return s.toString();
    }

    /**
     * render this error bar to the supplied graphics context.
     *
     * @param g
     *            The graphics context on which to render the error bar
     * @param axis
     *            The JBasicPlot that owns this error bar.
     */
    @Override
    public void render(Graphics g, JBasicPlot axis) {
        if (!isVisible() || !owner.getCanDisplay()) {
            return;
        }
        if (JMultiAxisPlot.getAllowXor()) {
            g.setXORMode(Color.white);
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.clip(owner.getPlotRegion().getRect());

        // Remove any pre-existing regions before creating new...
        region.clear();
        g2d.setColor(renderColor);
        g2d.setStroke(new BasicStroke(associatedPick.getWidth()));

        // Get the X-position of this bracket
        CartesianTransform ct = (CartesianTransform) axis.getCoordinateTransform();
        double Xpos;
        if (dir == PickErrorDir.RIGHT) {
            Xpos = associatedPick.getXval() + std;
            if (ct.getXAxisDir() == XaxisDir.LEFT) {
                Xpos -= 2 * std;
            }
        } else {
            Xpos = associatedPick.getXval() - std;
            if (ct.getXAxisDir() == XaxisDir.LEFT) {
                Xpos += 2 * std;
            }
        }
        Coordinate coord = new Coordinate(0.0, 0.0, Xpos, 0.0);
        ct.WorldToPlot(coord);
        int xpos = (int) coord.getX();

        // Now get the separation in pixels of the bracket from the pick. Use that
        // to make sure that the bracket legs do not extend past pick line
        coord.setWorldC1(associatedPick.getXval());
        ct.WorldToPlot(coord);
        int Xcenter = (int) coord.getX();
        int separation = Math.abs(xpos - Xcenter);
        int bwInt = axis.getUnitsMgr().getHorizUnitsToPixels(BracketWidth);
        bwInt = Math.min(bwInt, separation);

        // Draw the bracket as long as there is some separation from the pick line.
        int bot = associatedPick.getLineBottom();
        int top = associatedPick.getLineTop();
        GeneralPath p = new GeneralPath();
        int offset;
        if (dir == PickErrorDir.RIGHT) {
            offset = -bwInt;
        } else {
            offset = bwInt;
        }
        p.moveTo(xpos + offset, bot);
        p.lineTo(xpos, bot);
        p.lineTo(xpos, top);
        p.lineTo(xpos + offset, top);
        if (separation > 0) {
            // Don't want to XOR out the pick line
            g2d.draw(p);
        }

        // Add to the region vector
        int tol = 3;
        addToRegion(new Rectangle2D.Double(xpos - tol, top, 2 * tol, bot - top));

        // Add a little handle for use when std = 0
        if (showHandles) {
            int hw = axis.getUnitsMgr().getHorizUnitsToPixels(handleWidth);
            int center = (top + bot) / 2;
            int rectLeft;
            if (dir == PickErrorDir.RIGHT) {
                rectLeft = xpos + 1;
            } else {
                rectLeft = xpos - hw - 1;
            }
            Rectangle rect = new Rectangle(rectLeft, center - hw / 2, hw, hw);
            g2d.fill(rect);
            addToRegion(rect);
        }
    }

    /**
     * Sets the selected state of the VPickLineErrorBar object. Basically, this
     * amounts to changing the color in which the object is rendered. This
     * method will cause a re-render of the object.
     *
     * @param selected
     *            The new selected value
     * @param g
     *            The graphics context on which this object is rendered.
     */
    @Override
    public void setSelected(boolean selected, Graphics g) {
        if (this.selected == selected) {
            return;
        }
        VPickLineErrorBar other = associatedPick.getOther(this);
        render(g, owner);
        other.render(g, owner);
        renderColor = selected ? selectedColor : color;
        other.renderColor = renderColor;
        render(g, owner);
        other.render(g, owner);
        this.selected = selected;
        other.selected = selected;
    }

    public VPickLine getAssociatedPick() {
        return associatedPick;
    }

    boolean getSelected() {
        return selected;
    }

    private double std;
    private VPickLine associatedPick;
    private double BracketWidth;
    private double handleWidth;
    private PickErrorDir dir;
    private Color color;
    private Color selectedColor;
    private Color renderColor;
    private boolean showHandles;
    private boolean selected;
}
