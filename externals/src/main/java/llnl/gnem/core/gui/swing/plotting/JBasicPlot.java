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
package llnl.gnem.core.gui.swing.plotting;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Stack;

import llnl.gnem.core.gui.swing.plotting.jgeographicplot.AzimuthalZoomState;
import llnl.gnem.core.gui.swing.plotting.plotobjects.Line;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;

/**
 * This is the base class for all all plots that can be hosted by a
 * JPlotContainer. or a JMultiAxisPlot container
 */
public abstract class JBasicPlot {

    private boolean visible;
    private boolean canDisplay;
    protected CoordinateTransform coordTransform;
    protected DrawingUnits unitsMgr;
    protected DrawingRegion PlotRegion;
    protected JPlotContainer owner;
    protected Stack<AzimuthalZoomState> zoomStack;
    protected boolean generateLineSelectionRegion = true;
    private final ZorderManager zorderManager;

    public JBasicPlot(JPlotContainer owner) {
        canDisplay = true;
        visible = true;
        coordTransform = owner.getCoordinateTransform();
        this.owner = owner;
        unitsMgr = owner.getUnitsMgr();

        setPlotRegion(new DrawingRegion());

        zoomStack = new Stack<AzimuthalZoomState>();
        zorderManager = new ZorderManager();
    }

    protected final void setPlotRegion(DrawingRegion region) {
        PlotRegion = region;
        PlotRegion.setFillRegion(false);
        PlotRegion.setBackgroundColor(owner.getPlotRegion().getBackgroundColor());
        PlotRegion.setDrawBox(false);
        PlotRegion.setLineColor(Color.black);
    }

    /**
     * Changes the CoordinateTransform used by this plot.
     *
     * @param ct
     *            The new CoordinateTransform to use.
     */
    public void setCoordinateTransform(CoordinateTransform ct) {
        coordTransform = ct;
    }

    public void Render(Graphics g, int top, int height) {
    }

    /**
     * Zoom in (change the axis limits). The old limits are stored so that later
     * the plot can be unzoomed. There is no practical limit to the number of
     * zoom levels available.
     *
     * @param state
     *            An object holding the new axis limits to be used
     */
    public abstract void ZoomIn(ZoomState state);

    /**
     * Unzoom one level. If the plot has not been zoomed,no action is taken.
     */
    public abstract void ZoomOut();

    /**
     * Unzoom to the original axis limits. This undoes an arbitrary number of
     * zooms with one operation.
     */
    public abstract void UnzoomAll();

    /**
     * Gets the JPlotContainer that owns this JBasicPlot object
     *
     * @return The owner value
     */
    public JPlotContainer getOwner() {
        return owner;
    }

    /**
     * Gets the plotRegion attribute of the JSubplot object
     *
     * @return The plotRegion value
     */
    public DrawingRegion getPlotRegion() {
        return PlotRegion;
    }

    /**
     * Gets the visible attribute of the JBasicPlot object
     *
     * @return The visible value
     */
    public boolean getVisible() {
        return visible;
    }

    /**
     * Sets the visible attribute of the JBasicPlot object
     *
     * @param v
     *            The new visible value
     */
    public void setVisible(boolean v) {
        visible = v;
    }

    /**
     * Currently used only by the JSubplot descendents. When a subplot must be
     * hidden because of a zoom operation, this should be set to false.
     *
     * @param v
     */
    public void setCanDisplay(boolean v) {
        canDisplay = v;
    }

    /**
     * Currently used only by the JSubplot descendents. If a subplot has been
     * hidden because of a zoom operation, this returns false.
     *
     * @return true if the plot is within the displayable part of the
     *         JMultiAxisPlot
     */
    public boolean getCanDisplay() {
        return canDisplay;
    }

    /**
     * Removes all PlotObjects from the plot
     */
    protected synchronized void DeleteAxisObjects() {
        //  getObjects().clear();
        zorderManager.clear();
    }

    public CoordinateTransform getCoordinateTransform() {
        return coordTransform;
    }

    public DrawingUnits getUnitsMgr() {
        return unitsMgr;
    }

    /**
     * Gets the offset of the left edge of the plot interior in pixels relative
     * to the left edge of the graphics context.
     *
     * @return The plotLeft value
     */
    public int getPlotLeft() {
        return (int) PlotRegion.getRect().getX();
    }

    /**
     * Gets the offset from the top of the graphics context to the top of the
     * plot interior in pixels.
     *
     * @return The plotTop value
     */
    public int getPlotTop() {
        Rectangle rect = PlotRegion.getRect();
        if (rect != null) {
            return (int) rect.getY();
        } else {
            return 0;
        }
    }

    /**
     * Gets the width of the plot interior in pixels
     *
     * @return The plotWidth value
     */
    public int getPlotWidth() {
        return (int) PlotRegion.getRect().getWidth();
    }

    /**
     * Gets the height of the plot interior in pixels.
     *
     * @return The plotHeight value
     */
    public int getPlotHeight() {
        Rectangle rect = PlotRegion.getRect();
        if (rect != null) {
            return (int) rect.getHeight();
        } else {
            return 0;
        }
    }

    /**
     * Remove all objects from the axis, and redraw an empty axis on the Canvas.
     */
    public void Clear() {
        DeleteAxisObjects();
        if (!getVisible()) {
            return;
        }

        if (PlotRegion == null) {
            return;
        }
        Rectangle bounds = PlotRegion.getRect();
        if (bounds == null) {
            return;
        }

        Render(owner.getActiveGraphics(), (int) bounds.getX(), (int) bounds.getHeight());
    }

    /**
     * Adds a new plot object (Line, Text, Symbol, Legend, etc) to this subplot.
     *
     * @param obj
     *            The reference to the object being added.
     * @return A reference to the newly-added object.
     */
    public synchronized PlotObject AddPlotObject(PlotObject obj) {
        return AddPlotObject(obj, 0);
    }

    /**
     * Adds a PlotObject to the plot at the requested zlevel.
     *
     * @param obj
     *            The reference to the object being added.
     * @param level
     *            the zlevel at which to place the object.
     * @return A reference to the newly-added object.
     */
    public synchronized PlotObject AddPlotObject(PlotObject obj, int level) {
        obj.setOwner(this);
        if (obj instanceof Line) {
            Line line = (Line) obj;
            line.setGenerateSelectionRegion(this.generateLineSelectionRegion);
        }
        zorderManager.add(obj, level);
        return obj;
    }

    public synchronized void setLevelSymbolAlpha(int alpha, int level) {
        zorderManager.setLevelSymbolAlpha(alpha, level);
    }

    /**
     * Remove a PlotObject from the plot.
     *
     * @param po
     *            The PlotObject to be removed. If the object is not found, no
     *            action is taken.
     * @return true if the object was found and removed
     */
    public synchronized boolean DeletePlotObject(PlotObject po) {
        return zorderManager.remove(po);
    }

    /**
     * Gets a plot object whose Shape selection object intersects the x-Y input
     * value. Returns null if no object was selected.
     *
     * @param x
     *            The x-pixel value.
     * @param y
     *            The Y-pixel value
     * @return the selected object (or null if nothing selected).
     */
    public synchronized PlotObject getHotObject(int x, int y) {
        return zorderManager.getHotObject(x, y);
    }

    public synchronized PlotObject getHotObject(int x, int y, int level) {
        return zorderManager.getHotObject(x, y, level);
    }

    /**
     * Gets a Vector of all line objects in this plot. This was written to
     * support the Legend object, but may have some end-user value.
     *
     * @return The Lines Vector
     */
    public synchronized Line[] getLines() {
        ArrayList<Line> lines = zorderManager.getLines();
        return lines.toArray(new Line[0]);
    }

    public synchronized int getLineCount() {
        return zorderManager.getLineCount();
    }

    /**
     * Sets the polyLineUsage attribute of the subplot object. When set to true
     * All lines will be rendered using the polyline method, and each line have
     * a selection shape object computed. This is appropriate for rendering to
     * the screen, but gives low-resolution printer plots. To render to the
     * printer, set this parameter to false.
     *
     * @param value
     *            The new polyLineUsage value
     */
    public synchronized void setPolyLineUsage(boolean value) {
        zorderManager.setPolyLineUsage(value);
    }

    public void setGenerateLineSelectionRegion(boolean generateLineSelectionRegion) {
        this.generateLineSelectionRegion = generateLineSelectionRegion;
    }

    public void initializeCoordinateTransform() {
    }

    public void clearSelectionRegions() {
        zorderManager.clearSelectionRegions();
    }

    protected void renderVisiblePlotObjects(Graphics g) {
        zorderManager.renderVisiblePlotObjects(g, this);
    }

    public void clearText() {
        zorderManager.clearText();
    }

    public ArrayList<PlotObject> getVisiblePlotObjects() {
        return zorderManager.getVisiblePlotObjects();
    }

    public void setLevelVisible(boolean visible, int level) {
        zorderManager.setLevelVisible(visible, level);
    }

    public boolean isLevelVisible(int level) {
        return zorderManager.isLevelVisible(level);
    }

    public boolean contains(PlotObject po) {
        return zorderManager.contains(po);
    }
}
