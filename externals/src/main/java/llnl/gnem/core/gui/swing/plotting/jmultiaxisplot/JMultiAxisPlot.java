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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Observer;
import java.util.Stack;

import javax.swing.ToolTipManager;

import llnl.gnem.core.gui.swing.plotting.DrawingRegion;
import llnl.gnem.core.gui.swing.plotting.JPlotContainer;
import llnl.gnem.core.gui.swing.plotting.MouseMode;
import llnl.gnem.core.gui.swing.plotting.PaintMode;
import llnl.gnem.core.gui.swing.plotting.PanStyle;
import llnl.gnem.core.gui.swing.plotting.PenStyle;
import llnl.gnem.core.gui.swing.plotting.PickCreationInfo;
import llnl.gnem.core.gui.swing.plotting.ZoomLimits;
import llnl.gnem.core.gui.swing.plotting.ZoomType;
import llnl.gnem.core.gui.swing.plotting.epochTimePlot.EpochTimeXAxis;
import llnl.gnem.core.gui.swing.plotting.keymapper.ControlKeyMapper;
import llnl.gnem.core.gui.swing.plotting.plotobjects.Line;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;
import llnl.gnem.core.gui.swing.plotting.transforms.CartesianTransform;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;
import llnl.gnem.core.gui.swing.waveform.plotPrefs.AxisPrefs;
import llnl.gnem.core.gui.swing.waveform.plotPrefs.DrawingRegionPrefs;
import llnl.gnem.core.gui.swing.waveform.plotPrefs.PlotPreferenceModel;
import llnl.gnem.core.gui.swing.waveform.plotPrefs.PlotPresentationPrefs;
import llnl.gnem.core.util.PairT;
import llnl.gnem.core.util.TimeT;

/**
 * A class that manages a collection of one to many subplots. Each subplot can
 * display its own set of axes. Y-axes are independent of one-another, but the
 * X-axes are synchronized with each other and with a common X-axis displayed at
 * the bottom of the JMultiAxisPlot. Although individual X-axes can have
 * different limits, a vertical line drawn through all the subplots has the same
 * X-value in all subplots.
 *
 * @author Doug Dodge
 */
public class JMultiAxisPlot extends JPlotContainer {

    private static final Color SELECTION_COLOR = new Color(0xADD8E6);
    private final XAxis xaxis;
    private final SubplotManager subplots;
    private final JPlotMouseListener plotMouseListener;
    private final Stack<MouseMode> mouseModes;
    private final Selection selection;
    private final Cursor defaultCursor;
    private final Cursor handCursor;
    private final Cursor crossCursor;
    private final Cursor moveCursor;
    private MouseMode defaultMode;
    private MouseMode mouseMode;
    private PanStyle panStyle;
    private JSubplot activePlot;
    protected PlotPresentationPrefs prefs;

    public void maybeHandleTabKey(KeyEvent e) {
    }

    public enum XAxisType {

        Standard, EpochTime
    }

    /**
     * Constructor for the JMultiAxisPlot object
     */
    public JMultiAxisPlot() {
        this(MouseMode.SELECT_ZOOM, XAxisType.Standard);
    }

    public JMultiAxisPlot(XAxisType axisType) {
        this(MouseMode.SELECT_ZOOM, axisType);
    }

    public JMultiAxisPlot(MouseMode defaultMode, XAxisType xAxisType) {
        panStyle = PanStyle.ChangeAxisLimits;
        PlotPreferenceModel.getInstance().registerPlot(this);
        prefs = PlotPreferenceModel.getInstance().getPrefs();

        this.defaultMode = defaultMode;
        mouseMode = defaultMode;
        defaultCursor = new Cursor(Cursor.DEFAULT_CURSOR);
        handCursor = new Cursor(Cursor.HAND_CURSOR);
        crossCursor = new Cursor(Cursor.CROSSHAIR_CURSOR);
        moveCursor = new Cursor(Cursor.MOVE_CURSOR);

        coordTransform = new CartesianTransform();
        switch (xAxisType) {
        case Standard: {
            xaxis = new XAxis(this);
            xaxis.setMin(0.0);
            xaxis.setMax(1.0);
            break;
        }
        case EpochTime: {
            xaxis = new EpochTimeXAxis(this);
            xaxis.setMin(0.0);
            xaxis.setMax(new TimeT().getEpochTime());
            break;
        }
        default:
            throw new IllegalArgumentException("Unknown X-axis type: " + xAxisType);
        }
        subplots = new SubplotManager(this);
        plotMouseListener = new JPlotMouseListener(this);
        mouseModes = new Stack<>();
        selection = new Selection();

        setFocusTraversalKeysEnabled(false);
        addMouseListener(plotMouseListener);
        addMouseMotionListener(plotMouseListener);
        addKeyListener(plotMouseListener);
        this.addMouseWheelListener(plotMouseListener);
        //   this.setDoubleBuffered( false );

        setCursorForMouseModeChange();
        configurePlotFromPrefs();
    }

    public void clear() {
        plotMouseListener.clear();
        subplots.clear();
        this.repaint();
    }

    /**
     * Sets the mouseMode attribute of the JMultiAxisPlot object
     *
     * @param newMode
     *            The new mouseMode value
     */
    public void setMouseMode(MouseMode newMode) {
        mouseModes.push(mouseMode);
        mouseMode = newMode;
        setCursorForMouseModeChange();

        plotMouseListener.notifyObserversMouseModeChange(newMode);
    }

    public void nullifyDeletedObject(PlotObject po) {
        plotMouseListener.nullifyDeletedObject(po);
        if (po instanceof VPickLine) {
            VPickLine vpl = (VPickLine) po;
            for (PlotObject p : vpl.getContainedObjects()) {
                nullifyDeletedObject(p);
            }

        }
    }

    private void setCursorForMouseModeChange() {
        if (null != mouseMode) {
            switch (mouseMode) {
            case PAN:
                setCursor(handCursor);
                break;
            case PAN2:
                setCursor(moveCursor);
                break;
            case SELECT_REGION:
                setCursor(crossCursor);
                break;
            case CREATE_PICK:
                setCursor(crossCursor);
                break;
            default:
                setCursor(defaultCursor);
                break;
            }
        }
    }

    public void restorePreviousMouseMode() {
        if (mouseModes.empty()) {
        } else {
            mouseMode = mouseModes.pop();
            setCursorForMouseModeChange();
            plotMouseListener.notifyObserversMouseModeChange(mouseMode);
        }
    }

    void revertToDefaultMouseMode() {
        mouseModes.clear();
        mouseMode = defaultMode;
        setCursorForMouseModeChange();
        plotMouseListener.notifyObserversMouseModeChange(mouseMode);
    }

    public MouseMode getMouseMode() {
        return mouseMode;
    }

    public void HideSubplot(JSubplot p) {
        if (p != null) {
            subplots.setSubplotDisplayable(p, false);
        }
    }

    public void ShowSubplot(JSubplot p) {
        if (p != null) {
            subplots.setSubplotDisplayable(p, true);
        }
    }

    public MouseEvent getCurrentMouseEvent() {
        return plotMouseListener.getCurrentEvent();
    }

    /**
     * Gets a reference to the X-axis object used by this JMultiAxisPlot. This
     * is the common X-axis for all subplots. By default it is displayed in
     * addition to any X-axes that may be displayed by subplots.
     *
     * @return The X-axis reference
     */
    public XAxis getXaxis() {
        return xaxis;
    }

    public double getplotSpacing() {
        return subplots.getplotSpacing();
    }

    /**
     * Sets the spacing in mm between adjacent subplots
     *
     * @param v
     *            The new spacing value
     */
    public void setplotSpacing(double v) {
        subplots.setplotSpacing(v);
    }

    public void setSubplotBoxVisibility(boolean visible) {
        subplots.setSubplotBoxVisibility(visible);
    }

    public void setYaxisVisibility(boolean visible) {
        subplots.setYaxisVisibility(visible);
    }

    /**
     * Sets the zoomType of the JMultiAxisPlot object. There are three ways in
     * which the mouse can be used to control zooming. It can be used to draw a
     * box that includes part of one or more contained subplots. It can be used
     * to simultaneously select a common x-range for all subplots which will be
     * zoomed to with y-scaling unchanged. Or, it can be used to select a common
     * x-range which will be zoomed to with adaptive y-scaling. This method
     * selects which of these zoom types will be used.
     *
     * @param zoomType
     *            The new zoomType value
     */
    public void setZoomType(ZoomType zoomType) {
        plotMouseListener.setZoomType(zoomType);
    }

    /**
     * Adds a Subplot to the JMultiAxisPlot object. If other Subplots already
     * exist, the new Subplot will be placed as the bottom Subplot.
     *
     * @return A reference to the Subplot just added
     */
    public JSubplot addSubplot() {
        return addSubplot(false);
    }

    public JSubplot addSubplot(boolean striped) {
        JSubplot plot = subplots.addSubplot(new JSubplot(this, striped, JMultiAxisPlot.XAxisType.Standard));
        setPlotProperties(plot);
        return plot;
    }

    /**
     * Adds a Subplot to the JMultiAxisPlot object at a user-specified position.
     *
     * @param position
     *            The vertical placement of the Subplot being added. If position
     *            is LTEQ 0 then the new Subplot will be placed at the top. If
     *            position is GTEQ the number of Subplots already in the
     *            JMultiAxisPlot, then the new Subplot will be placed at the
     *            bottom.
     * @return A reference to the Subplot just added
     */
    public JSubplot addSubplot(int position) {
        JSubplot plot = subplots.addSubplot(new JSubplot(this, JMultiAxisPlot.XAxisType.Standard), position);
        setPlotProperties(plot);
        return plot;
    }

    /**
     * Gets the SubplotManager of the JMultiAxisPlot object. The SubplotManager
     * controls the position and rendering of Subplots contained in this object.
     * It also allows access to individual Subplot references.
     *
     * @return The SubplotManager value
     */
    public SubplotManager getSubplotManager() {
        return subplots;
    }

    /**
     * render this component to the supplied graphics context. To render on the
     * screen, call the render() method. To render on some other Graphics, call
     * the render method that takes a Graphics reference and the 4 position
     * values. The position values are all in Drawing Units, e.g. mm.
     *
     * @param g
     *            The graphics context on which to render this object
     */
    @Override
    public void Render(Graphics g) {
        ActiveGraphics = g;
        BoxHeight = unitsMgr.getVertPixelsToUnits(getHeight()) - 2 * VerticalOffset;
        BoxWidth = unitsMgr.getHorizPixelsToUnits(getWidth()) - 2 * HorizontalOffset;
        Render(ActiveGraphics, HorizontalOffset, VerticalOffset, BoxWidth, BoxHeight);
    }

    /**
     * render to some graphics context, supplying positioning parameters
     * appropriate for the device on which rendering will occur.
     *
     * @param g
     *            The graphics context
     * @param HOffset
     *            The horizontal offset in mm
     * @param VertOffset
     *            The vertical offset in mm
     * @param boxWidth
     *            The width of the plot region in mm
     * @param boxHeight
     *            The height of the plot region in mm
     */
    @Override
    public void Render(Graphics g, double HOffset, double VertOffset, double boxWidth, double boxHeight) {
        if (!getVisible()) {
            return;
        }
        left = unitsMgr.getHorizUnitsToPixels(HOffset);
        top = unitsMgr.getVertUnitsToPixels(VertOffset);
        plotWidth = unitsMgr.getHorizUnitsToPixels(boxWidth);
        plotHeight = unitsMgr.getVertUnitsToPixels(boxHeight);
        int bWidth = unitsMgr.getHorizUnitsToPixels(borderWidth);
        PlotBorder.setRect(left - bWidth, top - bWidth, plotHeight + 2 * bWidth, plotWidth + 2 * bWidth);

        // Color the border region and box
        if (showBorder) {
            PlotBorder.render(g);
            // render the plot title
            title.Render(g, left, top, plotWidth, unitsMgr);
        }
        PlotRegion.setRect(left, top, plotHeight, plotWidth);

        // Color the plotting region and box
        PlotRegion.render(g);

        // Don't render interior if it cannot show up.
        if (plotHeight < 2 || plotWidth < 2) {
            return;
        }

        // Initialize the data xmapper for the current plot size and data values
        // Can only initialize the x-part with meaningful values since there are no y-axes available.
        coordTransform.initialize(xaxis.getMin(), xaxis.getMax(), left, plotWidth, 0.0, 1.0, 0, 1);
        // render subplots
        subplots.Render(g, top, plotHeight);
        xaxis.Render(g, left, top, plotHeight, plotWidth);

        selection.draw(g);
    }

    /**
     * Sets the polyLineUsage attribute of the JMultiAxisPlot object. When set
     * to true All lines will be rendered using the polyline method, and each
     * line will have a selection shape object computed. This is appropriate for
     * rendering to the screen, but gives low- resolution printer plots. To
     * render to the printer, set this parameter to false.
     *
     * @param value
     *            The new polyLineUsage value
     */
    @Override
    public void setPolyLineUsage(boolean value) {
        subplots.setPolyLineUsage(value);
    }

    /**
     * Create a single X-Y plot in this object. Any previous subplots will be
     * removed and their references invalidated.
     *
     * @param X
     *            The array of X-values
     * @param Y
     *            The array of Y-values
     * @param c
     *            The color of the line
     * @param m
     *            The PaintMode of the line
     * @param s
     *            The PenStyle of the line
     * @param w
     *            The width of the line
     * @return A line object
     */
    public PlotObject Plot(float[] X, float[] Y, Color c, PaintMode m, PenStyle s, int w) {
        subplots.clear();
        JSubplot p = subplots.addSubplot(new JSubplot(this, JMultiAxisPlot.XAxisType.Standard));
        return p.Plot(X, Y, c, m, s, w);
    }

    /**
     * Create a single X-Y plot in this object. Any previous subplots will be
     * removed and their references invalidated.
     *
     * @param X
     *            The array of X-values
     * @param Y
     *            The array of Y-values
     * @return A line object
     */
    public PlotObject Plot(float[] X, float[] Y) {
        subplots.clear();
        JSubplot p = subplots.addSubplot(new JSubplot(this, JMultiAxisPlot.XAxisType.Standard));
        return p.Plot(X, Y);
    }

    public void removeSubplot(JSubplot plot) {
        subplots.RemovePlot(plot);
    }

    /**
     * Sets the x-limits of all contained subplots to the input values.
     *
     * @param xmin
     *            The minimum value for all x-axes
     * @param xmax
     *            The maximum value for all x-axes
     */
    public void setAllXlimits(double xmin, double xmax) {
        xaxis.setMin(xmin);
        xaxis.setMax(xmax);
        subplots.setAllXlimits(xmin, xmax);
    }

    public void setAllXlimits() {
        double xmin = subplots.getGlobalXmin();
        double xmax = subplots.getGlobalXmax();
        xaxis.setMin(xmin);
        xaxis.setMax(xmax);
        subplots.setAllXlimits(xmin, xmax);
    }

    /**
     * Auto-scale the y-axes of all the subplots for the data within the range
     * of the x-values specified as input arguments.
     *
     * @param xmin
     *            The minimum x-value to be used in computing new y-limits.
     * @param xmax
     *            The maximum x-value to be used in computing new x-limits.
     * @param resetYlimits
     */
    public void scaleAllTraces(double xmin, double xmax, boolean resetYlimits) {
        subplots.ScaleAllTraces(xmin, xmax, resetYlimits);
    }

    public void scaleAllTraces(boolean resetYlimits) {
        scaleAllTraces(getXaxis().getMin(), getXaxis().getMax(), resetYlimits);
    }

    /**
     * Change the y-limits on all the subplots. After rescaling, each new
     * y-range will be equal to the old range divided by scale. Thus values of
     * scale GT 1 will magnify the data and values LT 1 will shrink the data.
     * Scale values GTEQ 0 are not allowed.
     *
     * @param scale
     *            The scale factor to use in scaling all the subplots.
     * @param centerOnZero
     *            When true scaling is around the zero point.
     */
    public void YScaleAllTraces(double scale, boolean centerOnZero) {
        subplots.YScaleAllTraces(scale, centerOnZero);
    }

    public void setToolTipDismissDelay(int seconds) {
        ToolTipManager.sharedInstance().setDismissDelay(1000000 * seconds);
    }

    public void setShowPickTooltips(boolean v) {
        plotMouseListener.setShowPickTooltips(v);
    }

    public void addPlotObjectObserver(Observer o) {
        plotMouseListener.addPlotObjectObserver(o);
    }

    ArrayList<SubplotSelectionRegion> getSelectedRegionList(Rectangle zoomRect) {
        return subplots.getSelectedRegionList(zoomRect);
    }

    /**
     * Return the first PlotObject within the plot that contains the input point
     * (specified in pixels). There is no Z-order control, so where two or more
     * PlotObjects contain the input point, the last one added will be returned.
     * If no PlotObject contains the input point, then null is returned.
     *
     * @param X
     *            The X-pixel value
     * @param Y
     *            The Y-pixel value
     * @return The PlotObject containing the point or null if no PlotObject
     *         contains the point.
     */
    public PlotObject getClickedObject(int X, int Y) {
        return subplots.getClickedObject(X, Y);
    }

    /**
     * Gets the subplot that contains the input point
     *
     * @param X
     *            The X-pixel value
     * @param Y
     *            The Y-pixel value
     * @return The JSubplot containing the point or null if no JSubplot contains
     *         the point.
     */
    public JSubplot getCurrentSubplot(int X, int Y) {
        return subplots.getCurrentSubplot(X, Y);
    }

    /**
     * Zoom all the contained JSubplots so that their total displayed area falls
     * within the input Rectangle. Any JSubplot whose intersection with the
     * input Rectangle is empty will not be displayed after zooming.
     *
     * @param zoomRect
     *            The Rectangle (specified in pixels) that defines the final
     *            displayed region.
     */
    public void zoomToBox(Rectangle zoomRect) {
        subplots.zoomToBox(zoomRect);
    }

    public void zoomToBox(ZoomInStateChange zisc) {
        this.handleZoomIn(zisc);
    }

    public void zoomToNewLimits(ZoomLimits newLimits) {
        subplots.zoomToNewLimits(newLimits);
    }

    public void initXLimits(double xmin, double xmax) {
        subplots.initXLimits(xmin, xmax);
    }

    public void zoomToNewXLimits(double xmin, double xmax) {
        subplots.zoomToNewXLimits(xmin, xmax);
    }

    public Stack<ZoomLimits> getZoomLimits(JSubplot p) {
        return subplots.getZoomLimits(p);
    }

    /*
     * Methods Added to be compatible with JScrollableMultiAxisPlot
     */
    public boolean zoomInProgress() {
        return false;
    }

    public void resizePlot(int height, int width) {
    }

    public Container getScrollPane() {
        return null;
    }

    public void initScrolling(Container parent) {
    }

    public void setNeedToReloadPlotImage(boolean ignore) {
    }

    public void invalidateSavedImage() {
    }

    public boolean isNeedToReloadPlotImage() {
        return false;
    }

    ////////////////////////////////////////////////////////////////
    public void zoomToCurrentBorder() {
        Rectangle rect = this.getPlotRegion().getRect();
        zoomToBox(rect);
    }

    /**
     * Return to the previous zoom state. If there is no previous zoom state,
     * then no action is taken.
     *
     * @return true if zoom out is successful.
     */
    public boolean zoomOut() {
        return subplots.ZoomOut();
    }

    public void unzoomAll() {
        subplots.UnzoomAll();
    }

    public void setcontrolKeyMapper(ControlKeyMapper controlKeyMapper) {
        plotMouseListener.setcontrolKeyMapper(controlKeyMapper);
    }

    public PickCreationInfo getPickCreationInfo() {
        return plotMouseListener.getPickCreationInfo();
    }

    Iterator getSubplotIterator() {
        return subplots.iterator();
    }

    public JPlotMouseListener getMouseListener() {
        return plotMouseListener;
    }

    @Override
    public void setCoordinateTransform(CoordinateTransform transform) {
        super.setCoordinateTransform(transform);
        Collection<JSubplot> plots = subplots.getSubplots();
        for (JSubplot plot : plots) {
            plot.setCoordinateTransform(transform);
        }
    }

    public JSubplot getOwningSubplot(PlotObject po) {

        if (po != null) {
            for (JSubplot plot : subplots.getSubplots()) {
                if (plot.contains(po)) {
                    return plot;
                }
            }
            return null;
        } else {
            return null;
        }
    }

    public void scaleAllSubplotsToMatchSelected(JSubplot aPlot) {
        subplots.scaleAllSubplotsToMatchSelected(aPlot);
    }

    public void scale(double factor) {

        for (JSubplot plot : getSubplotManager().getSubplots()) {
            plot.Scale(factor, true);
        }
        repaint();
    }

    public PairT<Double, Double> pointToCoordinate(Point point) {
        CoordinateTransform ct = getCoordinateTransform();
        Coordinate currentCoord = new Coordinate(point.x, point.y);
        ct.PlotToWorld(currentCoord);
        double worldX = currentCoord.getWorldC1();
        double worldY = currentCoord.getWorldC2();
        return new PairT<>(worldX, worldY);
    }

    public PanStyle getPanStyle() {
        return panStyle;
    }

    public void setPanStyle(PanStyle panStyle) {
        this.panStyle = panStyle;
        if (plotMouseListener != null) {
            plotMouseListener.setPanStyle(panStyle);
        }
    }

    public void performHorizontalMouseWheelZoom(MouseWheelEvent mwe, double referenceValue) {
        if (mwe.getWheelRotation() < 0) {

            double xmin = getXaxis().getMin();
            double xmax = getXaxis().getMax();

            double range = xmax - xmin;
            double newRange = 0.9 * range;
            double halfRange = newRange / 2;

            double newXmin = referenceValue - halfRange;
            double newXmax = referenceValue + halfRange;
            if (newXmin < xmin) {
                newXmin = xmin;
                newXmax = referenceValue + referenceValue - newXmin;
            } else if (newXmax > xmax) {
                newXmax = xmax;
                newXmin = referenceValue - (xmax - referenceValue);
            }

            zoomToNewXLimits(newXmin, newXmax);
            repaint();

        } else {
            zoomOut();
            repaint();
        }
    }

    public void maybePerformVerticalMouseWheelZoom(MouseWheelEvent mwe, Coordinate coordinate) {
        double referenceValue = coordinate.getWorldC2();
        if (subplots.getNumVisibleSubplots() == 1) {
            JSubplot aplot = subplots.getCurrentSubplot((int) coordinate.getX(), (int) coordinate.getY());
            if (aplot != null) {
                YAxis axis = aplot.getYaxis();
                if (mwe.getWheelRotation() < 0) {
                    double xmin = axis.getMin();
                    double xmax = axis.getMax();

                    double range = xmax - xmin;
                    double newRange = 0.9 * range;
                    double halfRange = newRange / 2;

                    double newYmin = referenceValue - halfRange;
                    double newYmax = referenceValue + halfRange;
                    if (newYmin < xmin) {
                        newYmin = xmin;
                        newYmax = referenceValue + referenceValue - newYmin;
                    } else if (newYmax > xmax) {
                        newYmax = xmax;
                        newYmin = referenceValue - (xmax - referenceValue);
                    }
                    subplots.zoomPlotToNewYlimits(aplot, newYmin, newYmax);
                    repaint();
                }
            } else {
                zoomOut();
                repaint();

            }
        }
    }

    public void setActiveSubplot(JSubplot sp) {
        if (sp == activePlot) {
            return;
        }
        for (JSubplot subplot : subplots.getSubplots()) {
            subplot.getPlotRegion().setHighlighted(false);
        }
        if (sp != null) {
            sp.getPlotRegion().setHighlighted(true);
        }
        repaint();
        activePlot = sp;
    }

    public void handleZoomIn(ZoomInStateChange zisc) {
        zoomToBox(zisc.getZoomBounds());
        repaint();
    }

    public void handleZoomOut() {
        zoomOut();
        repaint();
    }

    public void unselect() {
        selection.setVisible(false);
        repaint();
    }

    public void select(int left, int right) {
        selection.setVisible(true);
        selection.setPosition(left, right);
        selection.draw(getGraphics());
    }

    private class Selection {

        private final Rectangle rect;
        private boolean visible;

        public Selection() {
            rect = new Rectangle();
            visible = false;
        }

        public void setVisible(boolean visible) {
            this.visible = visible;
        }

        public void setPosition(int left, int right) {
            rect.setBounds(left, (int) getPlotRegion().getRect().getY(), right - left, (int) getPlotRegion().getRect().getHeight());
        }

        public void draw(Graphics g) {
            if (visible) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setColor(SELECTION_COLOR);
                g2.setXORMode(Color.white);
                g2.fill(rect);
            }
        }
    }

    public void updateForChangedPrefs() {
        configurePlotFromPrefs();
    }

    protected void configurePlotFromPrefs() {
        prefs = PlotPreferenceModel.getInstance().getPrefs();
        DrawingRegionPrefs borderPrefs = prefs.getBorderPrefs();
        DrawingRegion border = getPlotBorder();
        border.setBackgroundColor(borderPrefs.getBackgroundColor());
        border.setBoxLineWidth(borderPrefs.getLineWidth());
        border.setDrawBox(borderPrefs.isDrawBox());
        border.setFillRegion(borderPrefs.isFillRegion());
        border.setLineColor(borderPrefs.getLineColor());

        DrawingRegionPrefs plotPrefs = prefs.getPlotRegionPrefs();
        DrawingRegion plotRegion = getPlotRegion();
        plotRegion.setBackgroundColor(plotPrefs.getBackgroundColor());
        plotRegion.setBoxLineWidth(plotPrefs.getLineWidth());
        plotRegion.setDrawBox(plotPrefs.isDrawBox());
        plotRegion.setFillRegion(plotPrefs.isFillRegion());
        plotRegion.setLineColor(plotPrefs.getLineColor());

        AxisPrefs xPrefs = prefs.getxAxisPrefs();
        PlotAxis xAxis = getXaxis();
        xAxis.setAxisColor(xPrefs.getColor());
        xAxis.setAxisPenWidth(xPrefs.getPenWidth());
        xAxis.setVisible(xPrefs.isVisible());

        xAxis.setLabelColor(xPrefs.getLabelPrefs().getFontColor());
        xAxis.setLabelFont(xPrefs.getLabelPrefs().getFont());
        xAxis.setLabelOffset(xPrefs.getLabelPrefs().getOffset());

        xAxis.setTickDirection(xPrefs.getTickPrefs().getDirection());
        xAxis.setTickFontColor(xPrefs.getTickPrefs().getFontColor());
        xAxis.setTickFont(xPrefs.getTickPrefs().getFont());
        xAxis.setTicksVisible(xPrefs.getTickPrefs().isVisible());

        getTitle().setColor(prefs.getTitleColor());
        //        getTitle().setFontSize(prefs.getTitleFontSize());
        Font titleFont = prefs.getTitleFont();
        getTitle().setFont(titleFont);

        SubplotManager mgr = this.getSubplotManager();
        for (JSubplot plot : mgr.getSubplots()) {
            setPlotProperties(plot);
        }

        repaint();
    }

    protected void setPlotProperties(JSubplot plot) {
        prefs = PlotPreferenceModel.getInstance().getPrefs();
        DrawingRegionPrefs plotPrefs = prefs.getPlotRegionPrefs();
        DrawingRegion plotRegion = plot.getPlotRegion();
        plotRegion.setBackgroundColor(plotPrefs.getBackgroundColor());
        plotRegion.setBoxLineWidth(plotPrefs.getLineWidth());
        plotRegion.setDrawBox(plotPrefs.isDrawBox());
        plotRegion.setFillRegion(plotPrefs.isFillRegion());
        plotRegion.setLineColor(plotPrefs.getLineColor());

        AxisPrefs yPrefs = prefs.getyAxisPrefs();
        PlotAxis yAxis = plot.getYaxis();
        yAxis.setAxisColor(yPrefs.getColor());
        yAxis.setAxisPenWidth(yPrefs.getPenWidth());
        yAxis.setVisible(yPrefs.isVisible());

        yAxis.setLabelColor(yPrefs.getLabelPrefs().getFontColor());
        yAxis.setLabelFont(yPrefs.getLabelPrefs().getFont());
        yAxis.setLabelOffset(yPrefs.getLabelPrefs().getOffset());

        yAxis.setTickDirection(yPrefs.getTickPrefs().getDirection());
        yAxis.setTickFontColor(yPrefs.getTickPrefs().getFontColor());
        yAxis.setTickFont(yPrefs.getTickPrefs().getFont());
        yAxis.setTicksVisible(yPrefs.getTickPrefs().isVisible());

        for (Line line : plot.getLines()) {
            line.setMaxSymbolsToPlot(prefs.getMaxSymbolsToPlot());
            line.setLimitPlottedSymbols(prefs.isLimitPlottedSymbols());
            line.setPlotLineSymbols(prefs.isPlotLineSymbols());
            line.setColor(prefs.getTraceColor());
        }

    }

}
