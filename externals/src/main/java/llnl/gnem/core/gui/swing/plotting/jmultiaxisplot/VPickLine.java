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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.ArrayList;

import llnl.gnem.core.gui.swing.plotting.JBasicPlot;
import llnl.gnem.core.gui.swing.plotting.PenStyle;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObjectRenderer;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;

/**
 * A class that provides a graphical, interactive representation of a data value
 * pick. VPickLines can be moved interactively, and can be queried for their
 * value. VPickLine also has associated error bars and an associated window. The
 * error bars and the window can be turned on and off as required.
 *
 * @author Doug Dodge
 */
public class VPickLine extends PlotObject {
    /**
     * Gets the data value (real-world value) currently represented by this
     * VPickLine object.
     *
     * @return The data value
     */
    public double getXval() {
        return xval;
    }

    /**
     * Gets the error value associated with the VPickLine object. This is the
     * absolute value of the difference between the VPickLine data value and the
     * data value of one of the associated error bars.
     *
     * @return The error value
     */
    public double getStd() {
        return leftStd.getStd();
    }

    /**
     * Gets the draggable attribute of the VPickLine object. This attribute
     * controls whether the user can move the pick by dragging with the mouse.
     *
     * @return The draggable value
     */
    public boolean getDraggable() {
        return canDragX;
    }

    /**
     * Gets the color of the line rendered by this VPickLine object
     *
     * @return The color value
     */
    public Color getColor() {
        return color;
    }

    /**
     * Gets the width of the line rendered by this VPickLine object
     *
     * @return The width value in pixels
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the windowHandle attribute of the VPickLine object. This is the
     * object that allows interaction with the right side of the window
     * associated with this pick.
     *
     * @return The windowHandle value
     */
    public JWindowHandle getWindowHandle() {
        return windowHandle;
    }

    /**
     * Gets the text associated with the VPickLine object
     *
     * @return The text value
     */
    public String getText() {
        return text;
    }

    /**
     * Gets a reference to the associated JWindowRegion. This can be used to set
     * color values etc.
     *
     * @return The JWindowRegion reference
     */
    public JWindowRegion getWindow() {
        return window;
    }

    /**
     * Gets the dataBridge associated with the VPickLine object. If no
     * PickDataBridge is associated, returns null.
     *
     * @return The dataBridge value
     */
    public PickDataBridge getDataBridge() {
        return dataBridge;
    }

    /**
     * Sets the X-position of the VPickLine in real-world coordinates. Does not
     * cause a re-render of the object.
     *
     * @param v
     *            The new x-value in real-world coordinates.
     */
    public void setXval(double v) {
        xval = v;
        if (forceInBounds) {
            if (xval < minBound)
                xval = minBound;
        }
        if (windowHandle.getForceInBounds()) {
            double duration = window.getDuration();
            double maxBound = windowHandle.getUpperBound();
            if (xval + duration > maxBound)
                xval = maxBound - duration;
        }
    }

    /**
     * Sets the y-position of the center of the VPickLine. This method only has
     * an effect if the VPickLine was instantiated using a constructor that
     * specifies both x and y values. Changes made using this method do not
     * cause re-rendering of the object.
     *
     * @param v
     *            The new y-value in real-world coordinates.
     */
    public void setYval(double v) {
        yval = v;
    }

    /**
     * Sets the height of the VPickLine object in real-world coordinates. This
     * method only has an effect if the VPickLine was instantiated using a
     * constructor that specifies both x and y values and pick height. Changes
     * made using this method do not cause re-rendering of the object.
     *
     * @param v
     *            The new height value
     */
    public void setHeight(double v) {
        height = v;
    }

    /**
     * Sets the text displayed with the VPickLine object
     *
     * @param v
     *            The new text value
     */
    public void setText(String v) {
        text = v;
    }

    /**
     * Sets the (unselected) color of the VPickLine object
     *
     * @param v
     *            The new color value
     */
    public void setColor(Color v) {
        color = v;
        renderColor = color;
        leftStd.setColor(v);
        rightStd.setColor(v);
    }

    /**
     * Sets the (selected) Color of the VPickLine object
     *
     * @param v
     *            The new selected Color value
     */
    public void setSelectedColor(Color v) {
        selectedColor = v;
    }

    /**
     * Sets the width in pixels of the VPickLine object
     *
     * @param v
     *            The new width value
     */
    public void setWidth(int v) {
        width = v;
    }

    /**
     * Controls whether the VPickLine can be adjusted by dragging with the
     * mouse.
     *
     * @param v
     *            The new draggable value
     */
    public VPickLine setDraggable(boolean v) {
        canDragX = v;
        window.setCanDragX(canDragX);
        return this;
    }

    /**
     * Sets the size of the text (in points) displayed with the VPickLine object
     *
     * @param v
     *            The new text Size in points
     */
    public void setTextSize(int v) {
        textSize = v;
    }

    /**
     * Sets the text Position of the VPickLine object. Text can be displayed
     * either above the pick line or below the line.
     *
     * @param v
     *            The new text Position value
     */
    public void setTextPosition(PickTextPosition v) {
        textPosition = v;
    }

    /**
     * Sets the value of the error for the associated VPickLineErrorBar objects.
     * This method does not cause the error bars to be re-rendered.
     *
     * @param v
     *            The new error value in real-world coordinates
     */
    public void setStd(double v) {
        leftStd.setStd(v);
        rightStd.setStd(v);
    }

    /**
     * Sets the bracket Width of the associated VPickLineErrorBar objects. Each
     * error bar has an inward-facing bracket at top and bottom. The width of
     * this bracket is specified in millimeters.
     *
     * @param v
     *            The new bracket Width value in millimeters
     */
    public void setBracketWidth(double v) {
        leftStd.setBracketWidth(v);
        rightStd.setBracketWidth(v);
    }

    /**
     * Sets the handle Width of the associated VPickLineErrorBar objects. Each
     * error bar has an optional square handle that may make it easier to select
     * with the mouse. This method sets the size of a side of the squares in
     * millimeters.
     *
     * @param v
     *            The new handle Width value in millimeters.
     */
    public void setHandleWidth(double v) {
        leftStd.setHandleWidth(v);
        rightStd.setHandleWidth(v);
    }

    /**
     * Controls whether the error-bar handles are displayed. The error bars have
     * optional box-like handles that make it easier to grab them, particularly
     * when the std is zero or near zero.
     *
     * @param v
     *            The new showHandles value
     */
    public void setErrorBarShowHandles(boolean v) {
        leftStd.setShowHandles(v);
        rightStd.setShowHandles(v);
    }

    public boolean isShowWindowHandle() {
        return showWindowHandle;
    }

    public void setShowWindowHandle(boolean showWindowHandle) {
        this.showWindowHandle = showWindowHandle;
        windowHandle.setVisible(visible && showWindowHandle);
    }

    /**
     * Sets the showErrorBars attribute of the VPickLine object. When this
     * attribute is true, a pair of adjustable error bars will be displayed
     * along with the pick line.
     *
     * @param v
     *            The new showErrorBars value
     */
    public void setShowErrorBars(boolean v) {
        showErrorBars = v;
        leftStd.setVisible(v && visible);
        rightStd.setVisible(v && visible);
    }

    @Override
    public void setVisible(boolean v) {
        visible = v;
        leftStd.setVisible(v && showErrorBars);
        rightStd.setVisible(v && showErrorBars);
        window.setVisible(v && showWindow);
        windowHandle.setVisible(v && showWindowHandle);
        getResidualBar().setVisible(v && showResidualBar);
    }

    public void setShowResidualBar(boolean v) {
        showResidualBar = v;
        getResidualBar().setVisible(v && visible);
    }

    public void setPenStyle(PenStyle penStyle) {
        this.penStyle = penStyle;
    }

    /**
     * Constructor for the VPickLine object that specifies a fixed height and
     * specific x and y values for the VPickLine object and which leaves other
     * values to defaults
     *
     * @param xval
     *            The x-value at which to place the VPickLine object
     * @param yval
     *            The y-value of the center of the VPickLine object
     * @param height
     *            The height of the VPickLine object in millimeters.
     * @param text
     *            The text to associate with the VPickLine object.
     */
    public VPickLine(double xval, double yval, double height, String text) {
        initialize(xval, yval, height, text, Color.black, 1, 10, PickTextPosition.BOTTOM, 0, false, false, 1.0, 0.0, null);
    }

    /**
     * Constructor for the VPickLine object that specifies a fixed height and
     * specific x and y values for the VPickLine object and which allows other
     * parameters to be set.
     *
     * @param xval
     *            The x-value at which to place the VPickLine object
     * @param yval
     *            The y-value of the center of the VPickLine object
     * @param height
     *            The height of the VPickLine object in millimeters.
     * @param text
     *            The text to associate with the VPickLine object.
     * @param color
     *            The color of the VPickLine object
     * @param width
     *            The line thickness of the VPickLine object
     * @param draggable
     *            The draggability of the VPickLine object
     * @param textSize
     *            The size of the associated text in points
     * @param textPos
     *            The position of the text (top or bottom)
     */
    public VPickLine(double xval, double yval, double height, String text, Color color, int width, boolean draggable, int textSize, PickTextPosition textPos) {
        initialize(xval, yval, height, text, color, width, textSize, textPos, 0, false, draggable, 1.0, 0.0, null);
    }

    /**
     * Constructor for the VPickLine object that creates a VPickLine which
     * adaptively sizes itself to the current plot dimensions and which leaves
     * other parameters at default values.
     *
     * @param xval
     *            The x-value at which to place the VPickLine object
     * @param yAxisFraction
     *            The fraction of the JSubplot height that should be occupied by
     *            the VPickLine object
     * @param text
     *            The text to associate with the VPickLine object.
     */
    public VPickLine(double xval, double yAxisFraction, String text) {
        initialize(xval, 0, 0, text, Color.black, 1, 10, PickTextPosition.BOTTOM, yAxisFraction, true, false, 1.0, 0.0, null);
    }

    /**
     * Constructor for the VPickLine object that creates a VPickLine that
     * adaptively sizes itself to the current plot dimensions and that allows
     * other parameters to be set.
     *
     * @param xval
     *            The x-value at which to place the VPickLine object
     * @param yAxisFraction
     *            The fraction of the JSubplot height that should be occupied by
     *            the VPickLine object
     * @param text
     *            The text to associate with the VPickLine object.
     * @param color
     *            The color of the VPickLine object
     * @param width
     *            The line thickness of the VPickLine object
     * @param draggable
     *            The draggability of the VPickLine object
     * @param textSize
     *            The size of the associated text in points
     * @param textPos
     *            The position of the text (top or bottom)
     */
    public VPickLine(double xval, double yAxisFraction, String text, Color color, int width, boolean draggable, int textSize, PickTextPosition textPos) {
        initialize(xval, 0, 0, text, color, width, textSize, textPos, yAxisFraction, true, draggable, 1.0, 0.0, null);
    }

    /**
     * Constructor for the VPickLine object that installs a PickDataBridge
     * observer into the VPickLine and that adaptively sizes itself to the
     * current plot dimensions and that allows other parameters to be set.
     *
     * @param dataBridge
     *            Description of the Parameter
     * @param yAxisFraction
     *            The fraction of the JSubplot height that should be occupied by
     *            the VPickLine object
     * @param text
     *            The text to associate with the VPickLine object.
     * @param color
     *            The color of the VPickLine object
     * @param width
     *            The line thickness of the VPickLine object
     * @param draggable
     *            The draggability of the VPickLine object
     * @param textSize
     *            The size of the associated text in points
     * @param textPos
     *            The position of the text (top or bottom)
     */
    public VPickLine(PickDataBridge dataBridge, double yAxisFraction, String text, Color color, int width, boolean draggable, int textSize, PickTextPosition textPos) {
        double deltim = dataBridge.getDeltim() >= 0 ? dataBridge.getDeltim() : 0;
        double duration = dataBridge.getDuration();
        initialize(dataBridge.getRelativeTime(), 0, 0, text, color, width, textSize, textPos, yAxisFraction, true, draggable, deltim, duration, dataBridge);
    }

    /**
     * Constructor for the VPickLine object that installs a PickDataBridge
     * observer into the VPickLine and that creates a pick with a specific
     * y-value and height in mm.
     *
     * @param dataBridge
     *            Description of the Parameter
     * @param text
     *            The text to associate with the VPickLine object.
     * @param color
     *            The color of the VPickLine object
     * @param width
     *            The line thickness of the VPickLine object
     * @param draggable
     *            The draggability of the VPickLine object
     * @param textSize
     *            The size of the associated text in points
     * @param textPos
     *            The position of the text (top or bottom)
     */

    /**
     * Constructor for the VPickLine object that installs a PickDataBridge
     * observer into the VPickLine and that creates a pick with a specific
     * y-value and height in mm.
     *
     * @param dataBridge
     *            Description of the Parameter
     * @param ycenter
     *            The Y-value of the pick center.
     * @param pickHeight
     *            The Pick height in mm.
     * @param text
     *            The text to associate with the VPickLine object.
     * @param color
     *            The color of the VPickLine object
     * @param width
     *            The line thickness of the VPickLine object
     * @param draggable
     *            The draggability of the VPickLine object
     * @param textSize
     *            The size of the associated text in points
     * @param textPos
     *            The position of the text (top or bottom)
     */
    public VPickLine(PickDataBridge dataBridge, double ycenter, double pickHeight, String text, Color color, int width, boolean draggable, int textSize, PickTextPosition textPos) {
        double deltim = dataBridge.getDeltim() >= 0 ? dataBridge.getDeltim() : 0;
        double duration = dataBridge.getDuration();
        initialize(dataBridge.getRelativeTime(), ycenter, pickHeight, text, color, width, textSize, textPos, -1, false, draggable, deltim, duration, dataBridge);
    }

    private void initialize(double xval, double yval, double height, String text, Color color, int width, int textSize, PickTextPosition textPos, double yAxisFraction, boolean value,
            boolean draggable, double deltim, double duration, PickDataBridge dataBridge) {
        this.xval = xval;
        this.yval = yval;
        this.height = height;
        this.text = text;
        this.color = color;
        selectedColor = Color.red;
        renderColor = color;
        this.width = width;
        this.textSize = textSize;
        textPosition = textPos;
        this.yAxisFraction = yAxisFraction;
        pickHeightIsWindowFrac = value;
        canDragY = false;
        canDragX = draggable;
        leftStd = new VPickLineErrorBar(this, deltim, PickErrorDir.LEFT);
        rightStd = new VPickLineErrorBar(this, deltim, PickErrorDir.RIGHT);
        window = new JWindowRegion(this, duration, false);
        windowHandle = new JWindowHandle(this);
        selected = false;
        this.dataBridge = dataBridge;
        forceInBounds = false;
        minBound = -Double.MAX_VALUE;
        penStyle = PenStyle.SOLID;
        showErrorBars = false;
        residualBar = new ResidualBar(this, 0.0, false);
        String os = System.getProperty("os.name");
        canRenderTextWhileDragging = os != null && !os.equals("Mac OS X") ? true : false;
    }

    public void setResidual(double residual) {
        residualBar.setResidual(residual);
    }

    /**
     * Sets the forceInBounds attribute of the VPickLine object. When this
     * attribute is true, the VPickLine can only have values greater than
     * lowerBound. This capability supports applications that need to control
     * the adjustment of picks.
     *
     * @param v
     *            The new forceInBounds value
     */
    public void setForceInBounds(boolean v) {
        forceInBounds = v;
    }

    /**
     * Sets the lowerBound attribute of the VPickLine object. When forceInBounds
     * is true, the VPickLine can only have values greater than lowerBound. This
     * capability supports applications that need to control the adjustment of
     * picks.
     *
     * @param v
     *            The new lowerBound value
     */
    public void setLowerBound(double v) {
        minBound = v;
    }

    public double getLowerBound() {
        return minBound;
    }

    public void updateResidualBar(double dx) {
        if (getResidualBar() != null)
            getResidualBar().setResidual(getResidualBar().getResidual() + dx);
    }

    /**
     * Change the position of this object within its owning subplot by an amount
     * specified by the input values.
     *
     * @param axisIn
     *            The JBasicPlot that owns this VPickLine
     * @param graphics
     * @param dx
     *            The change in x-value (real-world coordinates)
     * @param dy
     *            The change in y-value (real-world coordinates)
     */
    @Override
    public void ChangePosition(JBasicPlot axisIn, Graphics graphics, double dx, double dy) {
        if (!canDragX)
            return;
        presetTextRenderCapability();

        JSubplot axis = (JSubplot) axisIn;
        if (getResidualBar() != null)
            getResidualBar().setResidual(getResidualBar().getResidual() - dx);

        double minDuration = window.getMinDuration();
        double duration = window.getDuration();
        if (duration - dx < minDuration)
            dx = duration - minDuration;

        double oldXval = xval;
        xval += dx;
        if (forceInBounds) {
            if (xval < minBound) {
                dx = minBound - oldXval;
                xval = minBound;
            }
        }
        if (dataBridge != null)
            dataBridge.setTime(xval, this);
        duration -= dx;
        if (dataBridge != null)
            dataBridge.setDuration(duration, this);
        window.setDuration(duration);
        postSetTextRenderCapability();
        axisIn.getOwner().repaint();
    }

    public void postSetTextRenderCapability() {
        doRenderText = true;
    }

    public void presetTextRenderCapability() {
        doRenderText = canRenderTextWhileDragging;
    }

    private void renderSubComponents(Graphics g, JSubplot axis) {
        if (leftStd != null)
            leftStd.render(g, axis);
        if (rightStd != null)
            rightStd.render(g, axis);
        if (window != null)
            window.render(g, axis);
        if (windowHandle != null)
            windowHandle.render(g, axis);
        if (getResidualBar() != null)
            getResidualBar().render(g, axis);
    }

    /**
     * Produce a String representation of this object
     *
     * @return The String representation
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder(" Pick at X = ");
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(5);
        s.append(f.format(xval));
        if (text != null && text.length() > 0) {
            s.append(" Text is: ");
            s.append(text);
        }
        return s.toString();
    }

    /**
     * Gets the left-hand side VPickLineErrorBar of the VPickLine object
     *
     * @return The leftStd value
     */
    VPickLineErrorBar getLeftStd() {
        return leftStd;
    }

    public void setAllDraggable(boolean draggable) {
        setDraggable(draggable);
        leftStd.setCanDragX(draggable);
        leftStd.setCanDragY(draggable);
        rightStd.setCanDragX(draggable);
        rightStd.setCanDragY(draggable);
    }

    /**
     * Gets the right-hand side VPickLineErrorBar of the VPickLine object
     *
     * @return The rightStd value
     */
    VPickLineErrorBar getRightStd() {
        return rightStd;
    }

    /**
     * Sets the selected state of the VPickLine object. This causes a
     * re-rendering to either the default color or to the selected color
     * depending on the value of the selected parameter.
     *
     * @param selected
     *            The new selected value
     * @param g
     *            The graphics context on which the VPickLine is being rendered.
     */
    @Override
    public void setSelected(boolean selected, Graphics g) {
        if (this.selected != selected) {
            doRenderSubComponents = false;
            renderColor = selected ? selectedColor : color;
            this.selected = selected;
            doRenderSubComponents = true;
            this.owner.getOwner().repaint();
        }
    }

    public void setColorImmediate(Color v) {
        if (color == v)
            return;

        PlotObjectRenderer por = new PlotObjectRenderer(this);
        setColor(v);
        por.run();
    }

    public void setSelected(boolean selected) {
        setSelected(selected, this.getOwner().getOwner().getGraphics());
    }

    boolean getSelected() {
        return selected;
    }

    /**
     * render this VPickLine object and its contained PlotObjects to the
     * supplied graphics context.
     *
     * @param g
     *            The graphics context on which to render the VPickLine object
     * @param owner
     *            The JSubplot object that owns this VPickLine.
     */
    @Override
    public synchronized void render(Graphics g, JBasicPlot owner) {
        if (!isVisible() || g == null || owner == null || !owner.getCanDisplay())
            return;
        Graphics2D g2d = (Graphics2D) g;
        Rectangle rect = owner.getPlotRegion().getRect();
        g2d.clip(rect);
        // Remove any pre-existing regions before creating new...
        region.clear();
        g2d.setColor(renderColor);
        g2d.setStroke(penStyle.getStroke(width));
        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate(0.0, 0.0, xval, yval);
        ct.WorldToPlot(coord);
        int xpos = (int) coord.getX();
        int ycenter;
        int halfHeight;
        if (pickHeightIsWindowFrac) {
            ycenter = owner.getPlotTop() + owner.getPlotHeight() / 2;
            halfHeight = (int) (owner.getPlotHeight() * yAxisFraction / 2);
        } else {
            ycenter = (int) coord.getY();
            halfHeight = Math.max(owner.getUnitsMgr().getVertUnitsToPixels(height) / 4, 1);
        }
        bottom = ycenter + halfHeight;
        top = ycenter - halfHeight;
        g.drawLine(xpos, bottom, xpos, top);
        int tol = 3;
        addToRegion(new Rectangle2D.Double(xpos - tol, top, 2 * tol, bottom - top));
        if (text.length() > 0 && doRenderText) {
            if (textPosition == PickTextPosition.BOTTOM)
                AddLineText(g, xpos, bottom);
            else
                AddLineText(g, xpos, top);
        }
        if (doRenderSubComponents)
            renderSubComponents(g, (JSubplot) owner);

    }

    /**
     * This is an over-ride of the PlotObject::hasContainedObjects method.
     * During rendering, the object is queried to determine whether it contains
     * any sub-objects that must be rendered. If so, the PlotObject is asked to
     * produce a vector of its contained PlotObjects.
     *
     * @return This method always returns true for VPickLine since VPickLine
     *         contains pick error bars, a window and a window handle.
     */
    @Override
    public boolean hasContainedObjects() {
        return true;
    }

    /**
     * Gets a vector of contained PlotObjects. This is used during rendering of
     * the VPickLine, and allows its contained PlotObjects to be rendered.
     *
     * @return The Vector of contained PlotObjects.
     */
    @Override
    public ArrayList<? extends PlotObject> getContainedObjects() {
        ArrayList<PlotObject> v = new ArrayList<>();
        v.add(leftStd);
        v.add(rightStd);
        v.add(window);
        v.add(windowHandle);
        v.add(getResidualBar());
        return v;
    }

    /**
     * Gets the pixel value of the bottom of the pick line. This is used by
     * associated objects during their rendering. This value is reset each time
     * the VPickLine is rendered.
     *
     * @return The pixel value of the bottom of the VPickLine
     */
    int getLineBottom() {
        return bottom;
    }

    /**
     * Gets the pixel value of the top of the pick line. This is used by
     * associated objects during their rendering. This value is reset each time
     * the VPickLine is rendered.
     *
     * @return The pixel value of the top of the VPickLine
     */
    int getLineTop() {
        return top;
    }

    /**
     * Updates the position of a VPickLineErrorBar when the other one has been
     * changed. This allows the pair to move in sync.
     *
     * @param changedBar
     *            The VPickLineErrorBar which has had its position changed.
     * @param axis
     *            The JBasicPlot that owns this pick
     * @param g
     *            The graphics context being used to render these objects.
     */
    void UpdateOther(VPickLineErrorBar changedBar, JBasicPlot axis, Graphics g) {
        if (changedBar == leftStd) {
            rightStd.setStd(leftStd.getStd());
        } else {
            leftStd.setStd(rightStd.getStd());
        }
        axis.getOwner().repaint();
    }

    /**
     * Given a reference to a VPickLineErrorBar object, returns a reference to
     * the other VPickLineErrorBar associated with this pick. The input
     * reference must be for one of the two VPickLineErrorBar objects associated
     * with this pick.
     *
     * @param errorBar
     *            The VPickLineErrorBar whose match is sought
     * @return The other VPickLineErrorBar object
     */
    VPickLineErrorBar getOther(VPickLineErrorBar errorBar) {
        if (errorBar == leftStd)
            return rightStd;
        else
            return leftStd;
    }

    private void AddLineText(Graphics g, int xpixel, int ypixel) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(renderColor);

        // Layout and render text
        TextLayout textTl = new TextLayout(text, new Font("Arial", Font.PLAIN, textSize), new FontRenderContext(null, false, false));
        float advance = textTl.getAdvance() - 3;
        // Don't know why, but I need this correction factor of 3 for centering
        //    float ascent = textTl.getAscent();
        double thisHeight = textTl.getBounds().getHeight();
        float xpos = (xpixel - advance / 2);
        float textShift = (float) thisHeight + 2.0F;
        if (textPosition == PickTextPosition.TOP)
            textShift *= -1;
        float ypos = (ypixel + textSize / 2 + textShift);

        // move top two points down from bottom of symbol
        textTl.draw(g2d, xpos, ypos);
        AffineTransform textAt = new AffineTransform();
        textAt.translate(xpos, ypos);
        Shape s = textTl.getOutline(textAt);
        addToRegion(s.getBounds2D());
    }

    private double xval;
    private double yval;
    private double yAxisFraction;
    private double height;
    // in millimeters
    private boolean pickHeightIsWindowFrac;
    private String text;
    private Color color;
    private Color selectedColor;
    private Color renderColor;
    private int width;
    private int textSize;
    private PickTextPosition textPosition;
    private int bottom;
    private int top;
    private VPickLineErrorBar leftStd;
    private VPickLineErrorBar rightStd;
    private JWindowRegion window;
    private boolean selected;
    private JWindowHandle windowHandle;
    private PickDataBridge dataBridge;
    private boolean forceInBounds;
    private double minBound;
    private PenStyle penStyle;
    private boolean showErrorBars;
    private boolean showWindow = false;
    private boolean showWindowHandle = false;
    private boolean showErrorBarHandles = false;
    private ResidualBar residualBar;
    private boolean showResidualBar = false;
    private boolean doRenderSubComponents = true;
    private boolean canRenderTextWhileDragging = true;
    private boolean doRenderText = true;

    public boolean isShowWindow() {
        return showWindow;
    }

    public void setShowWindow(boolean showWindow) {
        this.showWindow = showWindow;
        window.setVisible(visible && showWindow);
    }

    public boolean isShowErrorBarHandles() {
        return showErrorBarHandles;
    }

    public void setShowErrorBarHandles(boolean showErrorBarHandles) {
        this.showErrorBarHandles = showErrorBarHandles;
        leftStd.setVisible(visible && showErrorBars);
        rightStd.setVisible(visible && showErrorBars);
    }

    public ResidualBar getResidualBar() {
        return residualBar;
    }

    public double getYval() {
        return yval;
    }
}
