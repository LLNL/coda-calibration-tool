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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import llnl.gnem.core.gui.plotting.AxisScale;
import llnl.gnem.core.gui.plotting.DrawingRegion;
import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.Limits;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.StripedDrawingRegion;
import llnl.gnem.core.gui.plotting.TickMetrics;
import llnl.gnem.core.gui.plotting.ZoomState;
import llnl.gnem.core.gui.plotting.epochTimePlot.EpochTimeXAxis;
import llnl.gnem.core.gui.plotting.plotobject.AbstractLine;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.LineBounds;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.transforms.CartesianTransform;
import llnl.gnem.core.util.TimeT;

/**
 * A class that manages individual subplots drawn within a JMultiAxisPlot.
 * Subplots can contain multiple lines, symbols, text and legends. They have X-
 * and Y-axes that can be displayed or hidden and can have their interior
 * regions brushed and stroked. All subplots within a JMultiAxisPlot share
 * common X-axis registration, although each subplot can have independent
 * limits.
 *
 * @author Doug Dodge
 */
public class JSubplot extends JBasicPlot {

    public JSubplot(JMultiAxisPlot owner, JMultiAxisPlot.XAxisType xAxisType) {
        this(owner, false, JMultiAxisPlot.XAxisType.Standard);
    }

    public JSubplot(JMultiAxisPlot owner, boolean striped, JMultiAxisPlot.XAxisType xAxisType) {
        super(owner);
        this.owner = owner;
        switch (xAxisType) {
            case Standard: {
                xaxis = new XAxis(owner);
                xaxis.setMin(0.0);
                xaxis.setMax(1.0);
                break;
            }
            case EpochTime: {
                xaxis = new EpochTimeXAxis(owner);
                xaxis.setMin(0.0);
                xaxis.setMax(new TimeT().getEpochTime());
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown X-axis type: " + xAxisType);
        }

        yaxis = new YAxis(this);
        xaxis.setMin(owner.getXaxis().getMin());
        xaxis.setMax(owner.getXaxis().getMax());
        xaxis.setVisible(false);
        yaxis.setMin(0.0);
        yaxis.setMax(1.0);
        setPlotRegion(striped ? new StripedDrawingRegion(yaxis) : new DrawingRegion());
        lastPlotRect = new Rectangle(0, 0, 1, 1);
        lastVisibleRect = new Rectangle(0, 0, 1, 1);
        lastXLimits = new Limits(0.0, 0.0);
        lastYLimits = new Limits(0.0, 0.0);
        showALL = false;
        this.getPlotRegion().setHighlighted(false);
    }

    @Override
    public void UnzoomAll() {
    }

    @Override
    public void ZoomOut() {
    }

    @Override
    public void ZoomIn(ZoomState state) {
    }

    /**
     * Sets the xmin attribute of the JSubplot object. If this new Xmin value is
     * less than the global Xmin, then the global Xmin will be updated and the
     * JMultiAxisPlot X-axis updated accordingly.
     *
     * @param v The new xmin value
     */
    public void setXmin(double v) {
        xaxis.setMin(v);
        JMultiAxisPlot thisOwner = (JMultiAxisPlot) owner;
        double globalXmin = thisOwner.getSubplotManager().getGlobalXmin();
        thisOwner.getXaxis().setMin(globalXmin);
    }

    /**
     * Sets the xmax attribute of the JSubplot object. If this new Xmax value is
     * greater than the global Xmax, then the global Xmax will be updated and
     * the JMultiAxisPlot X-axis updated accordingly.
     *
     * @param v The new xmax value
     */
    public void setXmax(double v) {
        JMultiAxisPlot thisOwner = (JMultiAxisPlot) owner;
        xaxis.setMax(v);
        double globalXmax = thisOwner.getSubplotManager().getGlobalXmax();
        thisOwner.getXaxis().setMax(globalXmax);
    }

    /**
     * Gets the X-axis of the JSubplot object
     *
     * @return The xaxis value
     */
    public XAxis getXaxis() {
        return xaxis;
    }

    /**
     * Gets the Y-axis of the JSubplot object
     *
     * @return The yaxis value
     */
    public YAxis getYaxis() {
        return yaxis;
    }

    public void SetAxisLimits() {
        // First set up the X-axis dimensions...
        double xmin = Double.MAX_VALUE;
        double xmax = -xmin;

        // Now set up the Y-axis dimensions...
        double ymin = xmin;
        double ymax = xmax;

        Line[] lines = getLines();
        if (lines == null) {
            throw new IllegalStateException("Attempt to set axis limits with no contents");
        }
        for (Line l : lines) {
            LineBounds bounds = l.getLineBounds();
            if (bounds.xmin < xmin) {
                xmin = bounds.xmin;
            }
            if (bounds.xmax > xmax) {
                xmax = bounds.xmax;
            }
            if (bounds.ymin < ymin) {
                ymin = bounds.ymin;
            }
            if (bounds.ymax > ymax) {
                ymax = bounds.ymax;
            }

        }

        SetAxisLimits(xmin, xmax, ymin, ymax);
    }

    public void setLogLogAxisLimits(double xmin, double xmax, double ymin, double ymax) {
        xmin = (int) (Math.log10(xmin));
        if (xmin <= 0) {
            --xmin;
        }
        xmin = Math.pow(10.0, xmin);

        ymin = (int) (Math.log10(ymin));
        if (ymin <= 0) {
            --ymin;
        }
        ymin = Math.pow(10.0, ymin);

        xmax = Math.pow(10.0, ((int) (Math.log10(xmax)) + 1));
        ymax = Math.pow(10.0, ((int) (Math.log10(ymax)) + 1));
        this.SetAxisLimits(xmin, xmax, ymin, ymax);
        this.setXmin(xmin);
        this.getYaxis().setMin(ymin);

    }

    public void SetAxisLimits(double xmin, double xmax, double ymin, double ymax) {
        TickMetrics ticks = PlotAxis.defineAxis(xmin, xmax);
        setXmax(ticks.getMax());
        setXmin(ticks.getMin());

        // Now set up the Y-axis dimensions...
        ticks = PlotAxis.defineAxis(ymin, ymax);
        yaxis.setMin(ticks.getMin());
        yaxis.setMax(ticks.getMax());
    }

    public void setXlimits(double xmin, double xmax) {
        TickMetrics ticks = PlotAxis.defineAxis(xmin, xmax);
        setXmax(ticks.getMax());
        setXmin(ticks.getMin());
    }

    public void setYlimits(double ymin, double ymax) {
        TickMetrics ticks = PlotAxis.defineAxis(ymin, ymax);
        yaxis.setMin(ticks.getMin());
        yaxis.setMax(ticks.getMax());
    }

    /**
     * Scale the subplot in the Y-dimension.
     *
     * @param scale Amount to scale by. Values greater (in absolute value) than
     * 1 magnify the plot. Non-positive values are not allowed.
     */
    public void Scale(double scale, boolean centerOnZero) {
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale value must not be zero.");
        }
        if (centerOnZero) {
            double range = yaxis.getMax() - yaxis.getMin();
            double half = range / 2 / scale;
            yaxis.setMin(-half);
            yaxis.setMax(half);
        } else {
            double center = (yaxis.getMin() + yaxis.getMax()) / 2;
            double half = yaxis.getMax() - center;
            double newHalf = half / scale;
            yaxis.setMin(center - newHalf);
            yaxis.setMax(center + newHalf);
        }
        Rectangle bounds = PlotRegion.getRect();
        if (bounds != null && owner != null) {
            Render(owner.getActiveGraphics(), (int) bounds.getX(), (int) bounds.getHeight());
        }
    }

    /**
     * Plot the input X-Y data after first erasing any pre-existing objects in
     * the subplot.
     *
     * @param X X-data values
     * @param Y Y-data values
     * @param c The color of the line
     * @param m The PaintMode of the line
     * @param s The PenStyle of the line
     * @param w The width of the line
     * @return the line object created by Plot.
     */
    public PlotObject Plot(float[] X, float[] Y, Color c, PaintMode m, PenStyle s, int w) {
        SetupPlot(X, Y);
        return AddPlotObject(new Line(X, Y, c, m, s, w));
    }

    /**
     * Plot the input X-Y data after first erasing any pre-existing objects in
     * the subplot.
     *
     * @param X X-data values
     * @param Y Y-data values
     * @return the line object created by Plot.
     */
    public PlotObject Plot(float[] X, float[] Y) {
        SetupPlot(X, Y);
        return AddPlotObject(new Line(X, Y));
    }

    public PlotObject Plot(double x0, double dx, float[] Y) {
        return Plot(x0, dx, 1, Y);
    }

    public PlotObject Plot(double x0, double dx, int lineWidth, float[] Y) {
        Line line = new Line(x0, dx, Y, lineWidth);
        SetupPlot(x0, dx, line);
        return AddPlotObject(line);
    }

    public PlotObject Plot(AbstractLine line) {
        double x0 = line.getStart();
        double dx = line.getIncrement();

        SetupPlot(x0, dx, line);
        return AddPlotObject(line);
    }

    public PlotObject Plot(double x0, double dx, float[] Y, int level) {
        Line line = new Line(x0, dx, Y);
        SetupPlot(x0, dx, line);
        return AddPlotObject(line, level);
    }

    void ScaleTraces(double xmin, double xmax, boolean autoRescaleY) {
        Line[] lines = getLines();
        double Ymax = Double.NEGATIVE_INFINITY;
        double Ymin = Double.POSITIVE_INFINITY;
        for (Line line : lines) {
            Point2D limits = line.getYMinMax(xmin, xmax);
            Ymin = Math.min(Ymin, limits.getX());
            Ymax = Math.max(Ymax, limits.getY());
        }
        if (autoRescaleY) {
            TickMetrics tm = PlotAxis.defineAxis(Ymin, Ymax);
            yaxis.setMin(tm.getMin());
            yaxis.setMax(tm.getMax());
        }
    }

    /**
     * render this subplot object
     *
     * @param g Graphics context on which the rendering will take place
     * @param top Top of the subplot in pixels
     * @param height Height of the subplot in pixels
     */
    void Render(Graphics g, int top, int height) {
        if (!getVisible()) {
            return;
        }
        // Instead of rendering everything, only render lines if they intersect the dirty rect.
        Rectangle dirtyRect = getDirtyRect(g);
        int LeftMargin = owner.getPlotLeft();
        int BoxWidth = owner.getPlotWidth();
        PlotRegion.setRect(LeftMargin, top, height, BoxWidth);

        // This is the rectangle that encloses the interior of the plot (inside the axes box).
        Rectangle plotRect = PlotRegion.getRect();

        Rectangle visibleRect = owner.getVisibleRect();

        // showALL check here enables rendering of all ticks and plots past this point.
        if (!showALL && !plotRect.intersects(dirtyRect.intersection(owner.getVisibleRect()))) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;

        Limits currentXLimits = new Limits(xaxis.getMin(), xaxis.getMax());
        Limits currentYLimits = new Limits(yaxis.getMin(), yaxis.getMax());
        if (owner.isForceFullRender() || dirtyRect.contains(plotRect) || // Dirty region is entire plot
                !lastPlotRect.equals(plotRect) || // Plot was resized
                !lastVisibleRect.equals(visibleRect) || // scrollable plot had scroolbar change
                !currentXLimits.equals(lastXLimits) || //Plot has been zoomed/unzoomed
                !currentYLimits.equals(lastYLimits)
                || !generateLineSelectionRegion) {
            renderPlotFromScratch(g2d, LeftMargin, top, height, BoxWidth, plotRect);
        } else {
            reRenderDirtyRegion(dirtyRect, g2d, LeftMargin, top, height, BoxWidth, plotRect);
        }
        lastPlotRect = plotRect;
        lastVisibleRect = visibleRect;
        lastXLimits = currentXLimits;
        lastYLimits = currentYLimits;
        g.setClip(dirtyRect);
    }

    private void reRenderDirtyRegion(Rectangle dirtyRect, Graphics2D g2d, int leftMargin, int top, int height, int boxWidth, Rectangle plotRect) {
        g2d.clip(owner.getPlotBorder().getRect());

        // render the plotting region and box. Have to use the larger box here so axis box does not
        // get clipped.
        PlotRegion.render(g2d);

        initializeCoordinateTransform();

        // Now set clipping region so only objects within plot and dirty rectangle get rendered.
        Rectangle innerClip = plotRect.intersection(dirtyRect);
        g2d.clip(innerClip);

        renderPlotObjectsInDirtyRegion(dirtyRect, g2d);

        // Axes are drawn partially outside the plot interior, so use the larger clip region.
        g2d.setClip(dirtyRect);
        g2d.clip(owner.getPlotBorder().getRect());
        renderAxes(g2d, leftMargin, top, height, boxWidth);
    }

    private void renderPlotObjectsInDirtyRegion(Rectangle dirtyRect, Graphics2D g2d) {
        ArrayList<PlotObject> plotObjects = this.getVisiblePlotObjects();
        for (PlotObject plotObject : plotObjects) {
            Rectangle bounds = plotObject.getBounds();
            if (bounds != null && bounds.intersects(dirtyRect)) {
                renderObject(plotObject, g2d);
            }
        }
    }

    private void renderObject(PlotObject obj, Graphics2D g2d) {
        if (obj instanceof Line) {
            Line line = (Line) obj;
            reRenderLine(line, g2d);
        } else {
            obj.render(g2d, this);
        }
    }

    /**
     * Re-render a line that has been previously drawn and that consequently has
     * a selection region.
     *
     * @param line
     * @param g2d
     */
    private void reRenderLine(Line line, Graphics2D g2d) {
        LineBounds lineBounds = line.getLineBounds();
        if (isBoundsIntersectsPlotRegion(lineBounds)) {
            line.setGenerateSelectionRegion(false);
            boolean retainOldSelectionRegion = true;
            line.Render(g2d, this, retainOldSelectionRegion);
        }
    }

    /**
     * render this plot from scratch. This must be done when plot has been
     * re-sized or rescaled, or the dirty region is the entire plot, or the plot
     * is being rendered on a new graphics context.
     *
     * @param g2d
     * @param leftMargin
     * @param top
     * @param height
     * @param boxWidth
     * @param plotRect
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private void renderPlotFromScratch(Graphics2D g2d, int leftMargin, int top, int height, int boxWidth, Rectangle plotRect) {
        clearSelectionRegions();

        Rectangle userClip = g2d.getClipBounds();

        // this allows all plots to be rendered
        if (!showALL) {
            g2d.clip(owner.getPlotBorder().getRect().intersection(owner.getVisibleRect()));
        }
        // render the plotting region and box

        initializeCoordinateTransform();
        PlotRegion.render(g2d);
        ArrayList<Line> renderedLines = renderPlotObjects(g2d);

        g2d.setClip(userClip);
        g2d.clip(owner.getPlotBorder().getRect());
        renderAxes(g2d, leftMargin, top, height, boxWidth);

        if (generateLineSelectionRegion) {
            createSelectionRegions(renderedLines);
        }
    }

    private Rectangle getDirtyRect(Graphics g) {
        Rectangle dirtyRect = owner.getPlotBorder().getRect();
        if (!owner.isForceFullRender()) {
            Shape s = g.getClip();
            if (s != null) {
                dirtyRect = s.getBounds();
            }
        }
        return dirtyRect;
    }

    private void renderAxes(Graphics2D g2d, int leftMargin, int top, int height, int boxWidth) {
        // render the y-axis  ( x-axis gets rendered by JPlotContainer )
        yaxis.Render(g2d, leftMargin, top, height, boxWidth);
    }

    private synchronized void createSelectionRegions(ArrayList<Line> renderedLines) {
        for (Line line : renderedLines) {
            line.createSelectionRegion();
        }
    }

    private ArrayList<Line> renderPlotObjects(Graphics g) {
        ArrayList<PlotObject> plotObjects = this.getVisiblePlotObjects();
        ArrayList<Line> renderedLines = new ArrayList<>();

        for (PlotObject obj : plotObjects) {
            if (obj instanceof Line) {
                Line line = (Line) obj;
                LineBounds bounds = line.getLineBounds();
                if (isBoundsIntersectsPlotRegion(bounds)) {
                    line.render(g, this);
                    renderedLines.add(line);
                }
            } else {
                obj.render(g, this);
            }
        }

        return renderedLines;
    }

    private boolean isBoundsIntersectsPlotRegion(LineBounds bounds) {
        return !(bounds.xmax < xaxis.getMin() || bounds.xmin > xaxis.getMax()) && !(bounds.ymin > yaxis.getMax() || bounds.ymax < yaxis.getMin());
    }

    @Override
    public void initializeCoordinateTransform() {
        Rectangle rect = PlotRegion.getRect();
        double xmin = xaxis.getMin();
        double xmax = xaxis.getMax();
        double ymin = yaxis.getMin();
        double ymax = yaxis.getMax();

        coordTransform.initialize(xmin, xmax, rect.x, rect.width,
                ymin, ymax, rect.y, rect.height);

    }

    private void SetupPlot(float[] X, float[] Y) {
        setVisible(true);
        DeleteAxisObjects();

        CartesianTransform ct = (CartesianTransform) coordTransform;
        ct.setXScale(AxisScale.LINEAR);
        ct.setYScale(AxisScale.LINEAR);

        setAxisLimits(X, Y);

    }

    public void setAxisLimits(float[] X, float[] Y) {
        // First set up the X-axis dimensions...
        double xmin = Line.getDataMin(X);
        double xmax = Line.getDataMax(X);

        // Now set up the Y-axis dimensions...
        double ymin = Line.getDataMin(Y);
        double ymax = Line.getDataMax(Y);
        TickMetrics ticks = PlotAxis.defineAxis(xmin, xmax);
        setXmax(ticks.getMax());
        setXmin(ticks.getMin());

        // Now set up the Y-axis dimensions...
        ticks = PlotAxis.defineAxis(ymin, ymax);
        yaxis.setMin(ticks.getMin());
        yaxis.setMax(ticks.getMax());
    }

    public void setUpAxesForLines() {
        Line[] lines = this.getLines();

        CartesianTransform ct = (CartesianTransform) coordTransform;
        ct.setXScale(AxisScale.LINEAR);
        ct.setYScale(AxisScale.LINEAR);

        double xmin = Double.MAX_VALUE;
        double xmax = -xmin;

        // Now set up the Y-axis dimensions...
        double ymin = xmin;
        double ymax = xmax;
        for (Line line : lines) {
            LineBounds bounds = line.getLineBounds();
            xmin = Math.min(xmin, bounds.xmin);
            xmax = Math.max(xmax, bounds.xmax);
            ymin = Math.min(ymin, bounds.ymin);
            ymax = Math.max(ymax, bounds.ymax);
        }
        TickMetrics ticks = PlotAxis.defineAxis(xmin, xmax);
        setXmax(ticks.getMax());
        setXmin(ticks.getMin());

        // Now set up the Y-axis dimensions...
        ticks = PlotAxis.defineAxis(ymin, ymax);
        yaxis.setMin(ticks.getMin());
        yaxis.setMax(ticks.getMax());

    }

    private void SetupPlot(double x0, double dx, AbstractLine line) {
        setVisible(true);
        DeleteAxisObjects();

        CartesianTransform ct = (CartesianTransform) coordTransform;
        ct.setXScale(AxisScale.LINEAR);
        ct.setYScale(AxisScale.LINEAR);

        // First set up the X-axis dimensions...
        double xmax = x0 + (line.getYSize() - 1) * dx;

        // Now set up the Y-axis dimensions...
        double ymin = line.getYDataMin();
        double ymax = line.getYDataMax();
        TickMetrics ticks = PlotAxis.defineAxis(x0, xmax);
        setXmax(ticks.getMax());
        setXmin(ticks.getMin());

        // Now set up the Y-axis dimensions...
        ticks = PlotAxis.defineAxis(ymin, ymax);
        yaxis.setMin(ticks.getMin());
        yaxis.setMax(ticks.getMax());
    }
    private final XAxis xaxis;
    private final YAxis yaxis;
    private Rectangle lastPlotRect;
    private Rectangle lastVisibleRect;
    private Limits lastXLimits;
    private Limits lastYLimits;

    public boolean isShowALL() {
        return showALL;
    }

    public void setShowALL(boolean showALL) {
        this.showALL = showALL;
    }
    private boolean showALL;

    public Limits getLastXLimits() {
        return lastXLimits;
    }

    public Limits getLastYLimits() {
        return lastYLimits;
    }
}
