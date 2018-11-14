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
package llnl.gnem.core.gui.plotting.plotobject;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import llnl.gnem.core.gui.plotting.AxisScale;
import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.JPlotContainer;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;
import llnl.gnem.core.gui.waveform.plotPrefs.PlotPreferenceModel;

/**
 *
 * @author addair1
 */
public abstract class AbstractLine extends PlotObject {
    private float[] sigmaY;
    private double start;
    private double increment;
    private Color color;
    private Color selectedColor;
    private Color renderColor;
    private PaintMode Mode;
    private PenStyle penStyle;
    private SymbolStyle symbolStyle = SymbolStyle.NONE;
    private int _Width;
    private int _NumRegionSegments = 50;
    private boolean _UsePolyline;
    protected CoordinateTransform coordTransform;
    private boolean generateSelectionRegion = true;
    private boolean selected;
    private double SymbolSize = 2.0;
    private Color SymbolFillColor = Color.black;
    private Color SymbolEdgeColor = Color.black;
    private static final int MAX_POINTS_TO_PLOT = 60000;
    private static final int MAX_POLYLINE_POINTS = 8000;
    private LineBounds bounds = null;
    private List<Point> pixelPoints;
    private RegionGenerationStyle regionGenerationStyle = RegionGenerationStyle.RECTANGULAR_BLOCK;
    private int maxSymbolsToPlot = 200;
    private boolean limitPlottedSymbols = true;
    private boolean plotLineSymbols = false;

    public AbstractLine() {
        this(Color.blue, PaintMode.COPY, PenStyle.SOLID, 1);
    }

    public AbstractLine(double start, double increment) {
        this(start, increment, Color.blue, PaintMode.COPY, PenStyle.SOLID, 1);
    }

    /**
     * Constructor for the Line object
     *
     * @param c
     *            The color of the Line
     * @param m
     *            The PaintMode of the Line
     * @param s
     *            The PenStyle of the Line
     * @param w
     *            The width of the Line
     */
    public AbstractLine(Color c, PaintMode m, PenStyle s, int w) {
        this(0.0, 0.0, c, m, s, w);
    }

    public AbstractLine(double start, double increment, Color c, PaintMode m, PenStyle s, int w) {
        this(start, increment, c, m, s, w, SymbolStyle.NONE, null);
    }

    public AbstractLine(double start, double increment, Color c, PaintMode m, PenStyle s, int w, SymbolStyle symbol, float[] sigmaY) {
        this.start = start;
        if (increment < 0) {
            throw new IllegalArgumentException("Increment must be non-negative!");
        }
        this.increment = increment;
        color = c;
        setSelectedColor();
        renderColor = color;
        Mode = m;
        penStyle = s;
        _Width = w;
        symbolStyle = symbol;
        this.sigmaY = sigmaY;

        _UsePolyline = true;
        pixelPoints = new ArrayList<>();
        maxSymbolsToPlot = PlotPreferenceModel.getInstance().getPrefs().getMaxSymbolsToPlot();
        limitPlottedSymbols = PlotPreferenceModel.getInstance().getPrefs().isLimitPlottedSymbols();
        plotLineSymbols = PlotPreferenceModel.getInstance().getPrefs().isPlotLineSymbols();
    }

    /**
     * Gets the color of the Line
     *
     * @return The color value
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color of this line.
     *
     * @param c
     *            The color to use.
     */
    public void setColor(final Color c) {
        color = c;
        renderColor = color;
        setSelectedColor();
    }

    public void setColor(final Color c, boolean renderNow, boolean retainSelectionRegion) {
        setColor(c);
        if (renderNow && owner != null) {
            EventQueue.invokeLater(new LineRenderer(retainSelectionRegion));
        }
    }

    public void setRegionGenerationStyle(RegionGenerationStyle regionGenerationStyle) {
        this.regionGenerationStyle = regionGenerationStyle;
    }

    /**
     * @return the maxSymbolsToPlot
     */
    public int getMaxSymbolsToPlot() {
        return maxSymbolsToPlot;
    }

    /**
     * @param maxSymbolsToPlot
     *            the maxSymbolsToPlot to set
     */
    public void setMaxSymbolsToPlot(int maxSymbolsToPlot) {
        this.maxSymbolsToPlot = maxSymbolsToPlot;
    }

    /**
     * @return the limitPlottedSymbols
     */
    public boolean isLimitPlottedSymbols() {
        return limitPlottedSymbols;
    }

    /**
     * @param limitPlottedSymbols
     *            the limitPlottedSymbols to set
     */
    public void setLimitPlottedSymbols(boolean limitPlottedSymbols) {
        this.limitPlottedSymbols = limitPlottedSymbols;
    }

    /**
     * @return the plotLineSymbols
     */
    public boolean isPlotLineSymbols() {
        return plotLineSymbols;
    }

    /**
     * @param plotLineSymbols
     *            the plotLineSymbols to set
     */
    public void setPlotLineSymbols(boolean plotLineSymbols) {
        this.plotLineSymbols = plotLineSymbols;
    }

    private class LineRenderer implements Runnable {
        private final boolean retainSelectionRegion;

        private LineRenderer(boolean retainSelectionRegion) {
            this.retainSelectionRegion = retainSelectionRegion;
        }

        @Override
        public void run() {
            Graphics g = owner.getOwner().getGraphics();
            if (g != null) {
                renderLine(g, owner, retainSelectionRegion);
            }

        }
    }

    /**
     * Gets the paintMode of the Line
     *
     * @return The paintMode value
     */
    public PaintMode getPaintMode() {
        return Mode;
    }

    public void setPaintMode(PaintMode mode) {
        Mode = mode;
    }

    /**
     * Gets the penStyle of the Line
     *
     * @return The penStyle value
     */
    public PenStyle getPenStyle() {
        return penStyle;
    }

    /**
     * Gets the width of the Line
     *
     * @return The width value
     */
    public int getWidth() {
        return _Width;
    }

    /**
     * Sets the number of regions used in creating a Shape object for Line
     * selection. Higher numbers provide better resolution in picking, but
     * increase the rendering time.
     *
     * @param nseg
     *            The new value
     */
    public void setNumRegionSegments(int nseg) {
        _NumRegionSegments = nseg;
        if (_NumRegionSegments < 1) {
            _NumRegionSegments = 1;
        }
        if (_NumRegionSegments > 300) {
            _NumRegionSegments = 300;
        }
    }

    /**
     * Sets the way in which the Line is rendered. If PolylineUsage is true,
     * then the data values defining the line are decimated to distinct pixel
     * values and then rendering is done using the polyline method. This method
     * should be used for on-screen graphics as it also creates the selection
     * Shape object. However, for printing, PolylineUsage should be set to
     * false. Otherwise, the printed line will be interpolated from the
     * low-density pixels of the screen to the high-density of the printer, and
     * the resolution will be poor.
     *
     * @param v
     *            The new value
     */
    public void setPolylineUsage(boolean v) {
        _UsePolyline = v;
    }

    /**
     * render the line to the supplied graphics context
     *
     * @param g
     *            The graphics context
     * @param owner
     *            The JSubplot that owns this Line
     */
    @Override
    public synchronized void render(Graphics g, JBasicPlot owner) {
        renderLine(g, owner, false);
    }

    public synchronized void renderLine(Graphics g, JBasicPlot owner, boolean retainOldSelectionRegion) {

        if (!isVisible() || g == null || owner == null) {
            return;
        }

        pixelPoints.clear();
        Graphics2D g2d = (Graphics2D) g;
        Rectangle rect = owner.getPlotRegion().getRect();
        g2d.clip(rect);

        if (penStyle != PenStyle.NONE) {

            // Remove any pre-existing regions before creating new...
            if (!retainOldSelectionRegion) {
                region.clear();
            }

            if (increment == 0 && getXSize() == 0) {
                return;
            }
            updateCoordinateTransform(owner.getCoordinateTransform());
            g2d.setColor(renderColor);
            Mode.setGraphicsPaintMode(g2d);
            g2d.setStroke(penStyle.getStroke(_Width));
            if (!_UsePolyline) {
                SimpleRender(g2d);
            } else {
                RegionRender(g2d);
            }
        }

        if (symbolStyle != SymbolStyle.NONE) {
            RenderSymbols(g2d);
        }

        if (generateSelectionRegion) {
            createSelectionRegion();
        }
    }

    protected void updateCoordinateTransform(CoordinateTransform coordTransform) {
        this.coordTransform = coordTransform;
    }

    public Point2D getYMinMax(double xmin, double xmax) {
        double Ymax = Double.NEGATIVE_INFINITY;
        double Ymin = Double.POSITIVE_INFINITY;
        int Npts = getYSize();
        for (int j = 0; j < Npts; ++j) {
            double xval = getXValue(j);
            if (xval >= xmin && xval <= xmax) {
                Ymin = Math.min(Ymin, getYValue(j));
                Ymax = Math.max(Ymax, getYValue(j));
            }
            if (xval >= xmax) {
                break;
            }
        }
        return new Point2D.Double(Ymin, Ymax);
    }

    public abstract int getXSize();

    public abstract int getYSize();

    public abstract double getXDataMin();

    public abstract double getXDataMax();

    public abstract double getYDataMin();

    public abstract double getYDataMax();

    /**
     * Gets the minimum value from the input float array
     *
     * @param V
     *            The array to be measured
     * @return The minimum value from the array.
     */
    public static double getDataMin(float[] V) {
        double result = V[0];
        int N = V.length;
        for (int j = 0; j < N; ++j) {
            result = Math.min(result, V[j]);
        }
        return result;
    }

    /**
     * Gets the maximum value from the input float array
     *
     * @param V
     *            The array to be measured
     * @return The maximum value from the array.
     */
    public static double getDataMax(float[] V) {
        double result = V[0];
        int N = V.length;
        for (int j = 0; j < N; ++j) {
            result = Math.max(result, V[j]);
        }
        return result;
    }

    /**
     * Produce a String description of this Line
     *
     * @return The String Description
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Line with attributes: Color = " + color);
        s.append(", PaintMode = ").append(Mode).append(", PenStyle = ").append(penStyle).append(", Width = ").append(_Width);
        return s.toString();
    }

    /**
     * Shift this line to a different place in the JBasicPlot
     *
     * @param owner
     *            The JBasicPlot that owns this Line
     * @param graphics
     * @param dx
     *            The amount to shift in the X-direction (real-world)
     * @param dy
     *            The amount to shift in the Y-direction (real-world)
     */
    @Override
    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {
        if (graphics == null) {
            graphics = owner.getOwner().getGraphics();
            graphics.setXORMode(Color.white);
        }
        Graphics2D g2d = (Graphics2D) graphics;
        updateCoordinateTransform(owner.getCoordinateTransform());
        g2d.setColor(color);
        g2d.setStroke(penStyle.getStroke(_Width));
        g2d.clip(owner.getPlotRegion().getRect());

        SimpleRender(g2d);
        if (canDragX) {
            updateXValues(dx);
        }
        if (canDragY) {
            updateYValues((float) dy);
        }

        resetBounds(); // They will get re-computed on next full render.
        SimpleRender(g2d);

    }

    public abstract void updateXValues(double dx);

    protected abstract void updateYValues(float dy);

    public void renderNow() {
        JBasicPlot theOwner = this.getOwner();
        if (theOwner != null) {
            JPlotContainer container = theOwner.getOwner();
            if (container != null) {
                Graphics g = container.getGraphics();
                Graphics2D g2d = (Graphics2D) g;
                g.setColor(color);
                g2d.setStroke(penStyle.getStroke(_Width));
                g2d.clip(theOwner.getPlotRegion().getRect());
                render(g, theOwner);
            }
        }
    }

    protected abstract int getMinIndex();

    protected abstract int getMaxIndex();

    private void SimpleRender(Graphics2D g2d) {
        Coordinate coord = new Coordinate(0.0, 0.0);
        GeneralPath p = new GeneralPath();
        int minIdx = getMinIndex();
        int maxIdx = getMaxIndex();
        int Npts = maxIdx - minIdx + 1;
        if (Npts < 2) {
            return;
        }

        int inc = Math.max(Npts / MAX_POINTS_TO_PLOT, 1);
        int pointsInPath = 0;
        for (int j = minIdx; j <= maxIdx; j += inc) {
            double xval = getXValue(j);
            pointsInPath++;
            coord.setWorldC1(xval);
            coord.setWorldC2(getYValue(j));
            if (coordTransform.isOutOfBounds(coord)) {
                if (p.getCurrentPoint() != null) {
                    g2d.draw(p);
                    p.reset();
                }
            } else if (pointsInPath > 1000) {
                g2d.draw(p);
                p.reset();
                pointsInPath = 0;
            } else if (p.getCurrentPoint() == null) {
                coord.setWorldC1(xval);
                coord.setWorldC2(getYValue(j));
                coordTransform.WorldToPlot(coord);

                p.moveTo((float) coord.getX(), (float) coord.getY());
            } else {
                coordTransform.WorldToPlot(coord);
                p.lineTo((float) coord.getX(), (float) coord.getY());
            }

        }
        g2d.draw(p);
    }

    private void RegionRender(Graphics2D g2d) {
        if (coordTransform.getXScale() == AxisScale.LOG || coordTransform.getYScale() == AxisScale.LOG) {
            SafeRegionRender(g2d);
        } else {
            FastRegionRender(g2d);
        }
    }

    private void FastRegionRender(Graphics2D g2d) {

        Coordinate coord = new Coordinate(0.0, 0.0);

        int minIdx = getMinIndex();
        int maxIdx = getMaxIndex();
        int Npts = maxIdx - minIdx + 1;
        if (Npts < 1) {
            return;
        }

        int[] xPoints = new int[Npts];
        int[] yPoints = new int[Npts];

        int k = 0;
        int lastX = -1;
        int lastY = -1;
        for (int j = minIdx; j <= maxIdx; ++j) {
            double xval = getXValue(j);
            coord.setWorldC1(xval);
            coord.setWorldC2(getYValue(j));
            if (coordTransform.isOutOfBounds(coord)) {
                if (k > 1) {
                    g2d.drawPolyline(xPoints, yPoints, k);
                }
                Npts -= (k + 1);
                k = 0;
            } else {
                coordTransform.WorldToPlot(coord);
                int x = (int) coord.getX();
                int y = (int) coord.getY();
                if (x != lastX || y != lastY) {
                    lastX = x;
                    lastY = y;
                    xPoints[k] = x;
                    yPoints[k++] = y;
                    pixelPoints.add(new Point(x, y));
                    if (k == MAX_POLYLINE_POINTS) {
                        g2d.drawPolyline(xPoints, yPoints, MAX_POLYLINE_POINTS);
                        k = 0;
                        Npts -= MAX_POLYLINE_POINTS;
                    }
                }
            }
        }
        if (k > 1) {
            g2d.drawPolyline(xPoints, yPoints, k);
        }
    }

    public void createSelectionRegion() {
        if (pixelPoints.size() < 2) {
            return;
        }
        if (!this.isSelectable()) {
            return;
        }

        JBasicPlot jbPlot = getOwner();
        if (jbPlot == null) {
            return;
        }

        RegionGenerator rg = null;
        Rectangle visibleRect = jbPlot.getOwner().getVisibleRect();

        Rectangle plotRect = jbPlot.getPlotRegion().getRect();
        if (visibleRect != null && plotRect != null) {
            // add the plot Region to the list to create a selected region
            rg = new RegionGenerator(plotRect.intersection(visibleRect), _NumRegionSegments, regionGenerationStyle);
            Area a = rg.generateRegion(pixelPoints);
            if (a != null) {
                addToRegion(a);
            }
        }

        pixelPoints.clear();
    }

    private void RenderSymbols(Graphics2D g2d) {

        int minIdx = getMinIndex();
        int maxIdx = getMaxIndex();
        int Npts = maxIdx - minIdx + 1;
        if (Npts < 2) {
            return;
        }

        if (!plotLineSymbols)
            return;

        if (limitPlottedSymbols && Npts > maxSymbolsToPlot) {
            return;
        }
        int inc = Math.max(Npts / MAX_POINTS_TO_PLOT, 1);

        for (int j = minIdx; j <= maxIdx; j += inc) {
            double xval = getXValue(j);

            Symbol s;
            if (symbolStyle == SymbolStyle.ERROR_BAR) {
                s = new ErrorBar(xval, getYValue(j), SymbolSize, sigmaY[j], 2.0);
                s.setOwner(owner);
            } else {
                s = SymbolFactory.createSymbol(symbolStyle, xval, getYValue(j), SymbolSize, SymbolFillColor, SymbolEdgeColor, Color.black, "", true, false, 10.0);
            }

            if (s != null) {
                s.render(g2d, owner);
            }

        }

    }

    protected abstract double getXValue(int i);

    protected abstract double getYValue(int i);

    private void SafeRegionRender(Graphics2D g2d) {
        // Build a vector of vectors of Points such that:
        // Adjacent elements of vectors are distinct (This is a usually a decimation of input data.)
        // No vector contains points whose X-values are outside axis limits
        // If X-scale is log, no x-values are non-positive
        // If Y-scale is log, no y-values are non-positive

        List<List<Point2D>> segments = new ArrayList<List<Point2D>>();
        List<Point2D> thisSeg = new ArrayList<Point2D>();
        int minIdx = getMinIndex();
        int maxIdx = getMaxIndex();
        Coordinate coord = new Coordinate(0.0, 0.0);

        for (int j = minIdx; j <= maxIdx; ++j) {
            double xval = getXValue(j);
            coord.setWorldC1(xval);
            coord.setWorldC2(getYValue(j));
            if (coordTransform.isOutOfBounds(coord)) {
                //Need to terminate the current segment and prepare a new one for use.
                if (thisSeg.size() > 1) {
                    segments.add(thisSeg);
                    thisSeg = new ArrayList<Point2D>();
                }
            } else {
                coordTransform.WorldToPlot(coord);
                Point2D P = new Point2D.Double(coord.getX(), coord.getY());
                pixelPoints.add(new Point((int) P.getX(), (int) P.getY()));
                if (thisSeg.size() > 0) {
                    Point2D Plast = thisSeg.get(thisSeg.size() - 1);
                    if (!P.equals(Plast)) {
                        thisSeg.add(P);
                    }
                } else {
                    thisSeg.add(P);
                }
            }
        }
        if (thisSeg.size() > 1) {
            segments.add(thisSeg);
        }
        if (segments.size() < 1) // Nothing to plot
        {
            return;
        }

        // Plot all the line segments
        for (List<Point2D> Seg : segments) {
            int M = Seg.size();
            if (M > 1) {
                int[] xPoints = new int[M];
                int[] yPoints = new int[M];
                for (int k = 0; k < M; ++k) {
                    Point2D p = Seg.get(k);
                    xPoints[k] = (int) p.getX();
                    yPoints[k] = (int) p.getY();
                }
                g2d.drawPolyline(xPoints, yPoints, M);
            }
        }
    }

    public SymbolStyle getSymbolStyle() {
        return symbolStyle;
    }

    public void setSymbolStyle(SymbolStyle symbolStyle) {
        this.symbolStyle = symbolStyle;
    }

    public double getSymbolSize() {
        return SymbolSize;
    }

    public void setSymbolSize(double symbolSize) {
        this.SymbolSize = symbolSize;
    }

    public Color getSymbolFillColor() {
        return SymbolFillColor;
    }

    public void setSymbolFillColor(Color symbolFillColor) {
        SymbolFillColor = symbolFillColor;
    }

    public Color getSymbolEdgeColor() {
        return SymbolEdgeColor;
    }

    public void setSymbolEdgeColor(Color symbolEdgeColor) {
        SymbolEdgeColor = symbolEdgeColor;
    }

    public void setPenStyle(PenStyle penStyle) {
        this.penStyle = penStyle;
    }

    public void setWidth(int width) {
        this._Width = width;
    }

    public LineBounds getLineBounds() {
        if (bounds != null) {
            return bounds;
        } else {
            bounds = new LineBounds();
            if (getYSize() > 0) {
                if (getXSize() == 0) {
                    bounds.xmin = start;
                    bounds.xmax = (getYSize() - 1) * increment + start;
                } else {
                    bounds.xmin = getXDataMin();
                    bounds.xmax = getXDataMax();
                }

                bounds.ymin = getYDataMin();
                bounds.ymax = getYDataMax();
            }
            return bounds;
        }
    }

    public void setGenerateSelectionRegion(boolean generateSelectionRegion) {
        this.generateSelectionRegion = generateSelectionRegion;
    }

    @Override
    public void setSelected(boolean selected, Graphics g) {
        if (this.selected != selected) {
            Rectangle rect = owner.getPlotRegion().getRect();
            Graphics2D g2d = (Graphics2D) g;
            g2d.clip(rect);
            owner.initializeCoordinateTransform();

            renderColor = selected ? selectedColor : color;
            renderLine(g, owner, true);
            this.selected = selected;
        }
    }

    private void setSelectedColor() {
        int intensityIncrement = 75;
        int maxValue = 255;
        int highThresh = maxValue - intensityIncrement;

        int red = color.getRed();
        int green = color.getGreen();
        int blue = color.getBlue();

        if (blue == maxValue) {
            if (red <= highThresh && green <= highThresh) {
                red += intensityIncrement;
                green += intensityIncrement;
            }
        } else if (green == maxValue) {
            if (red <= highThresh && blue <= highThresh) {
                red += intensityIncrement;
                blue += intensityIncrement;
            }
        } else if (red == maxValue) {
            if (green <= highThresh && blue <= highThresh) {
                green += intensityIncrement;
                blue += intensityIncrement;
            }
        } else if (blue <= highThresh) {
            blue += intensityIncrement;
        } else if (green <= highThresh) {
            green += intensityIncrement;
        } else if (red <= highThresh) {
            red += intensityIncrement;
        } else {
            red -= intensityIncrement;
            green -= intensityIncrement;
            blue -= intensityIncrement;
        }
        selectedColor = new Color(red, green, blue);
    }

    public double getStart() {
        return start;
    }

    public double getIncrement() {
        return increment;
    }

    public void setStart(double newStart) {
        start = newStart;
    }

    public void setIncrement(double inc) {
        increment = inc;
    }

    protected void resetBounds() {
        bounds = null;
    }
}
