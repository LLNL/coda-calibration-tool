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
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.text.NumberFormat;

import llnl.gnem.core.gui.swing.plotting.JBasicPlot;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;

/**
 * A graphical representation of a window associated with a pick. The window and
 * its associated pick can be selected and dragged. This class is responsible
 * for rendering the region, controlling its selection status, and making that
 * information available to end-users. Adjustment of window duration is
 * accomplished with a JWindowHandle class.
 *
 * @author Doug Dodge
 */
public class JWindowRegion extends PlotObject {

    /**
     * @return the windowText
     */
    public String getWindowText() {
        return windowText;
    }

    /**
     * @param windowText
     *            the windowText to set
     */
    public void setWindowText(String windowText) {
        this.windowText = windowText;
    }

    /**
     * @return the showWindowText
     */
    public boolean isShowWindowText() {
        return showWindowText;
    }

    /**
     * @param showWindowText
     *            the showWindowText to set
     */
    public void setShowWindowText(boolean showWindowText) {
        this.showWindowText = showWindowText;
    }

    private double rightHandleFractionalWidth;
    private int handleX;
    private int handleBottom;
    private int handleTop;
    private boolean showReferenceLine;
    private double referenceLineYValue;
    private double duration;
    private final VPickLine associatedPick;
    private Color color;
    private Color selectedColor;
    private boolean selected;
    private final Color outlineColor;
    private boolean renderInterior = true;
    private double minDuration;
    private boolean forceInBounds;
    private double maxBound;
    private double minBound;
    private String windowText;
    private boolean showWindowText;

    /**
     * Gets the duration in seconds of the window represented by the
     * JWindowRegion object
     *
     * @return The duration value
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Sets the duration in seconds of the window represented by the
     * JWindowRegion object. The window is not immediately re-rendered through
     * this call. A separate call to render must be made.
     *
     * @param v
     *            The new duration value (seconds)
     */
    public void setDuration(double v) {
        setDurationNoNotify(v);
        PickDataBridge pdb = associatedPick.getDataBridge();
        if (pdb != null) {
            pdb.setDurationNoNotify(duration);
        }
    }

    public void setDurationNoNotify(double v) {
        duration = v >= minDuration ? v : minDuration;
    }

    /**
     * Gets the associatedPick of the JWindowRegion object
     *
     * @return The associatedPick value
     */
    public VPickLine getAssociatedPick() {
        return associatedPick;
    }

    /**
     * Sets the color of the JWindowRegion object. This is the color displayed
     * when the window and associated pick are not in a selected state.
     *
     * @param c
     *            The new color value
     */
    public void setColor(Color c) {
        color = c;
    }

    /**
     * Sets the selectedColor attribute of the JWindowRegion object. This is the
     * color displayed when the window and associated pick are in a selected
     * state.
     *
     * @param c
     *            The new selectedColor value
     */
    public void setSelectedColor(Color c) {
        selectedColor = c;
    }

    /**
     * Sets the visible attribute of the JWindowRegion object
     *
     * @param v
     *            The new visible value
     */
    @Override
    public void setVisible(boolean v) {
        super.setVisible(v);
        associatedPick.getWindowHandle().setVisible(v);
    }

    public void setMinDuration(double v) {
        minDuration = v > 0 ? v : 0;
    }

    public double getMinDuration() {
        return minDuration;
    }

    /**
     * Description of the Method
     *
     * @param pickPosition
     *            Description of the Parameter
     */
    public void MoveToAndRender(double pickPosition) {
        double oldPos = associatedPick.getXval();
        if (pickPosition != oldPos) {
            ChangePositionNoNotify(owner, pickPosition - oldPos, 0.0, null);
        }
    }

    public void ChangePosition(double dx) {
        ChangePosition(owner, null, dx, 0.0);
    }

    public void ChangePositionNoNotify(double dx) {
        ChangePositionNoNotify(owner, dx, 0.0, null);
    }

    /**
     * Move this window horizontally. This is done in concert with the VPickLine
     * and its error bars. This method should only be called by the mouse motion
     * listener associated with the JMultiAxisPlot containing this object. The
     * method notifies any observers registered with its PickDataBridge, so if
     * called by an observer could result in inifinite recursion.
     *
     * @param axis
     *            The JBasicPlot that owns this window
     * @param graphics
     * @param dx
     *            The horizontal shift in data units
     * @param dy
     *            The vertical shift in data units ( not used ).
     */
    @Override
    public void ChangePosition(JBasicPlot axis, Graphics graphics, double dx, double dy) {
        ChangePositionNoNotify(axis, dx, dy, graphics);
        associatedPick.updateResidualBar(dx);
        PickDataBridge wd = associatedPick.getDataBridge();
        if (wd != null) {
            wd.setTime(associatedPick.getXval(), this);
            wd.setDurationNoNotify(duration);
        }
    }

    /**
     * Produce a String representation of this window.
     *
     * @return The String representation.
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("JWindowRegion: Duration = ");
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(5);
        s.append(f.format(duration));
        return s.toString();
    }

    /**
     * Constructor for the JWindowRegion object
     *
     * @param associatedPick
     *            The pick that is attached to this JWindowRegion
     * @param duration
     *            The duration of the window in seconds.
     * @param visible
     *            Description of the Parameter
     */
    public JWindowRegion(VPickLine associatedPick, double duration, boolean visible) {
        this.duration = duration;
        this.associatedPick = associatedPick;
        canDragY = false;
        canDragX = associatedPick.getDraggable();
        color = Color.lightGray;
        selectedColor = new Color(255, 200, 200);
        outlineColor = Color.black;
        selected = false;
        this.visible = visible;
        minDuration = 0;
        forceInBounds = false;
        maxBound = Double.MAX_VALUE;
        minBound = -maxBound;
        rightHandleFractionalWidth = .9;
        showReferenceLine = false;
        referenceLineYValue = 0;
        windowText = "";
        showWindowText = false;
    }

    public void setForceInBounds(boolean v) {
        forceInBounds = v;
    }

    public void setUpperBound(double v) {
        maxBound = v;
    }

    public void setLowerBound(double v) {
        minBound = v;
    }

    public boolean getForceInBounds() {
        return forceInBounds;
    }

    public double getUpperBound() {
        return maxBound;
    }

    public double getLowerBound() {
        return minBound;
    }

    public void setSelected(boolean selected) {
        Graphics g = this.owner.getOwner().getActiveGraphics();
        setSelected(selected, g);
    }

    /**
     * Sets the newSelectionStatus state of the JWindowRegion object. This
     * involves changing the color to and from the "newSelectionStatus" color to
     * the "normal" color.
     *
     * @param newSelectionStatus
     *            true or false
     * @param g
     *            The graphics context on which to render this window.
     */
    @Override
    public void setSelected(boolean newSelectionStatus, Graphics g) {
        if (this.selected == newSelectionStatus) {
            return;
        }
        setRenderInterior(false);
        selected = newSelectionStatus;
        setRenderInterior(true);
        this.owner.getOwner().repaint();
    }

    boolean getSelected() {
        return selected;
    }

    /**
     * render this window to the supplied graphics context.
     *
     * @param g
     *            The graphics context on which to render this window.
     * @param axisIn
     *            The JSubplot that owns this window.
     */
    @Override
    public void render(Graphics g, JBasicPlot axisIn) {
        JSubplot axis = (JSubplot) axisIn;
        if (!isVisible() || !owner.getCanDisplay()) {
            return;
        }
        double Pickpos = associatedPick.getXval();
        double Endpos = Pickpos + duration;
        if (Pickpos > axis.getXaxis().getMax() || Endpos < axis.getXaxis().getMin()) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.clip(owner.getPlotRegion().getRect());

        // Remove any pre-existing regions before creating new...
        region.clear();
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), 85);
        g2d.setColor(c);

        // Get the X-positions of the rectangle ends in pixels
        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate(0.0, 0.0, Pickpos, referenceLineYValue);
        ct.WorldToPlot(coord);

        int pickpos = (int) coord.getX();
        int lineYPos = (int) coord.getY();
        coord.setWorldC1(Endpos);
        ct.WorldToPlot(coord);
        int endpos = (int) coord.getX();
        if (endpos < pickpos) {
            int tmp = endpos;
            endpos = pickpos;
            pickpos = tmp;
        }
        pickpos += 1;
        // Dont want to XOR away the pick line

        // Draw the box.
        int bot = associatedPick.getLineBottom() - 2;
        int top = associatedPick.getLineTop() + 2;
        int height = bot - top;
        if (height >= 1) {
            Shape rect = getRegionShape(pickpos, top, endpos - pickpos, height);
            if (renderInterior) {
                g2d.fill(rect);
            }
            g2d.setColor(selected ? selectedColor : outlineColor);

            g2d.setStroke(new BasicStroke(1));
            g2d.draw(rect);
            addToRegion(rect);

            if (showReferenceLine) {
                g2d.drawLine(pickpos, lineYPos, endpos, lineYPos);
            }
            if (showWindowText) {
                addRegionText(g, pickpos, bot);
            }
        }
    }

    private void addRegionText(Graphics g, int xpixel, int ypixel) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(Color.black);
        g2d.drawString(windowText, xpixel + 1, ypixel - 1);
    }

    private GeneralPath getRegionShape(int left, int top, int width, int height) {
        GeneralPath p = new GeneralPath();
        int right = left + width;
        int bot = top + height;
        int h4 = height / 4;
        int h34 = (3 * height) / 4;
        handleTop = top + h4;
        handleBottom = top + h34;

        handleX = (int) (left + rightHandleFractionalWidth * width);
        p.moveTo(left, top);
        p.lineTo(right, top);
        p.lineTo(right, handleTop);
        p.lineTo(handleX, handleTop);
        p.lineTo(handleX, handleBottom);
        p.lineTo(right, handleBottom);
        p.lineTo(right, bot);
        p.lineTo(left, bot);
        p.lineTo(left, top);
        return p;
    }

    private void ChangePositionNoNotify(JBasicPlot axisIn, double dx, double dy, Graphics graphics) {
        JSubplot axis = (JSubplot) axisIn;
        if (graphics == null) {
            graphics = axis.getOwner().getGraphics();
            graphics.setXORMode(Color.white);
        }
        associatedPick.presetTextRenderCapability();
        associatedPick.postSetTextRenderCapability();
        double newXval = associatedPick.getXval() + dx;
        if (forceInBounds) {
            double windowEnd = newXval + duration;
            if (windowEnd > maxBound) {
                newXval = associatedPick.getXval();
            }
            if (newXval < minBound) {
                newXval = associatedPick.getXval();
            }
        }
        associatedPick.setXval(newXval);
        associatedPick.presetTextRenderCapability();
        associatedPick.postSetTextRenderCapability();
        axisIn.getOwner().repaint();
    }

    public void setRenderInterior(boolean renderInterior) {
        this.renderInterior = renderInterior;
    }

    public void setRightHandleFractionalWidth(double rightHandleFractionalWidth) {
        this.rightHandleFractionalWidth = Math.max(Math.min(rightHandleFractionalWidth, 1.0), 0.5);
    }

    public int getHandleX() {
        return handleX;
    }

    public int getHandleBottom() {
        return handleBottom;
    }

    public int getHandleTop() {
        return handleTop;
    }

    public boolean isShowReferenceLine() {
        return showReferenceLine;
    }

    public void setShowReferenceLine(boolean showReferenceLine) {
        this.showReferenceLine = showReferenceLine;
    }

    public double getReferenceLineYValue() {
        return referenceLineYValue;
    }

    public void setReferenceLineYValue(double referenceLineYValue) {
        this.referenceLineYValue = referenceLineYValue;
    }
}
