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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Observer;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.event.MouseInputAdapter;

import llnl.gnem.core.gui.plotting.DrawingRegion;
import llnl.gnem.core.gui.plotting.Limits;
import llnl.gnem.core.gui.plotting.MouseMode;
import llnl.gnem.core.gui.plotting.PanStyle;
import llnl.gnem.core.gui.plotting.PickCreationInfo;
import llnl.gnem.core.gui.plotting.PlotObjectObservable;
import llnl.gnem.core.gui.plotting.ZoomType;
import llnl.gnem.core.gui.plotting.keymapper.ControlKeyMapper;
import llnl.gnem.core.gui.plotting.keymapper.DefaultControlKeyMapper;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.LineWindow;
import llnl.gnem.core.gui.plotting.plotobject.LineWindowHandle;
import llnl.gnem.core.gui.plotting.plotobject.MarginButton;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;

/**
 * This class is used by JMultiAxisPlot to manage mouse interactions. There are
 * no public methods.
 *
 * @author Doug Dodge
 */
public class JPlotMouseListener extends MouseInputAdapter implements KeyListener {
    private int keyCode;
    private Coordinate mouseZoomRefPoint;
    private MarginButton marginButton;
    private final JMultiAxisPlot plot;
    private int anchorX;
    private int anchorY;
    private int lastX;
    private int lastY;
    private double DragLastX;
    private double DragLastY;
    private boolean zooming;
    private boolean dragging;
    private PlotObject currentObject;
    private JSubplot currentDragSubplot;
    private JSubplot selectedSubplot;
    private ZoomType zoomType;
    private Rectangle zoomRect;
    private boolean modeChangeOccurred;
    private boolean handPanning;
    private int panStartX;
    private double anchorXmin;
    private double anchorXmax;
    private VPickLine mouseOverPick;
    private VPickLineErrorBar mouseOverErrorBar;
    private JWindowRegion mouseOverWindow;
    private LineWindow mouseOverLineWindow;
    private LineWindowHandle mouseOverLineWindowHandle;
    private JWindowHandle mouseOverWindowHandle;
    private Line mouseOverLine;
    private Symbol mouseOverSymbol;
    private boolean showPickTooltips;
    private final PlotObjectObservable objectObservable;
    private PickMovedState pickMovedState;
    private WindowDurationChangedState windowDurationChangedState;
    private PickErrorChangeState pickErrorChangeState;
    private ControlKeyMapper controlKeyMapper;
    private boolean keyIsDown;
    private Coordinate currentCoord;
    private PickCreationInfo currentPickCreationInfo;
    private PanMagnifierManager panMagnifierManager;
    private Graphics2D myGraphics;
    private PanStyle panStyle;
    private MouseEvent currentEvent;
    private boolean handPanning2;
    private int panStartY;
    private double anchorYmin;
    private double anchorYmax;
    private final Color zoomColor = new Color(230, 230, 255);

    /**
     * Constructor for the JPlotMouseListener object
     *
     * @param plot
     *            The JMultiAxisPlot that will be using this listener.
     */
    JPlotMouseListener(JMultiAxisPlot plot) {
        this.plot = plot;
        myGraphics = (Graphics2D) plot.getGraphics();
        objectObservable = new PlotObjectObservable();
        controlKeyMapper = new DefaultControlKeyMapper();
        currentPickCreationInfo = null;
        panMagnifierManager = null;
        zoomType = ZoomType.ZOOM_BOX;
        setPanStyle(plot.getPanStyle());
        clear();
        plot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK, false), "SET_PAN");
        plot.getActionMap().put("SET_PAN", new SetPanAction());
        plot.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK, true), "UNSET_PAN");
        plot.getActionMap().put("UNSET_PAN", new UnsetPanAction());
    }

    class SetPanAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            MouseMode mouseMode = MouseMode.PAN2;
            if (!modeChangeOccurred || plot.getMouseMode() == MouseMode.PAN) {
                modeChangeOccurred = true;
                plot.setMouseMode(mouseMode);
            }

        }
    }

    class UnsetPanAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            plot.revertToDefaultMouseMode();
            modeChangeOccurred = false;
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (e.getWheelRotation() > 0) {
            plot.zoomOut();
            plot.repaint();
        } else if (keyIsDown && keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_RIGHT) {
            maybeZoomSideways(e);
        } else if (keyIsDown && keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
            maybeZoomVertical(e);
        } else {
            objectObservable.MouseWheelAction(e);
        }
    }

    private void maybeZoomVertical(MouseWheelEvent e) {
        plot.maybePerformVerticalMouseWheelZoom(e, mouseZoomRefPoint);
    }

    private void maybeZoomSideways(MouseWheelEvent mwe) {
        plot.performHorizontalMouseWheelZoom(mwe, mouseZoomRefPoint.getWorldC1());
    }

    public final void clear() {
        zooming = false;
        dragging = false;
        handPanning = false;
        currentDragSubplot = null;
        mouseOverPick = null;
        mouseOverErrorBar = null;
        mouseOverWindow = null;
        mouseOverWindowHandle = null;
        mouseOverLine = null;
        mouseOverSymbol = null;
        mouseOverLineWindow = null;
        mouseOverLineWindowHandle = null;
        showPickTooltips = true;
        keyIsDown = false;
        marginButton = null;
        currentPickCreationInfo = null;
    }

    public void nullifyDeletedObject(PlotObject po) {
        if (po == currentObject) {
            currentObject = null;
        }

        if (po == mouseOverPick) {
            mouseOverPick = null;
        } else if (po == mouseOverErrorBar) {
            mouseOverErrorBar = null;
        } else if (po == mouseOverWindow) {
            mouseOverWindow = null;
        } else if (po == mouseOverWindowHandle) {
            mouseOverWindowHandle = null;
        } else if (po == mouseOverLine) {
            mouseOverLine = null;
        } else if (po == mouseOverSymbol) {
            mouseOverSymbol = null;
        } else if (po == mouseOverLineWindow) {
            mouseOverLineWindow = null;
        } else if (po == mouseOverLineWindowHandle) {
            mouseOverLineWindowHandle = null;
        }
    }

    public ControlKeyMapper getControlKeyMapper() {
        return controlKeyMapper;
    }

    public void setcontrolKeyMapper(ControlKeyMapper controlKeyMapper) {
        this.controlKeyMapper = controlKeyMapper;
    }

    void addPlotObjectObserver(Observer o) {
        objectObservable.addObserver(o);
    }

    void setShowPickTooltips(boolean v) {
        showPickTooltips = v;
    }

    /**
     * Controls whether the mouse is being used to do selects / zooms or whether
     * it is being used to do panning.
     *
     * @param newMode
     *            The new mouseMode value
     */
    void notifyObserversMouseModeChange(MouseMode newMode) {
        objectObservable.notifyMouseModeChange(newMode);
    }

    /**
     * Controls the type of zoom rectangle that can be drawn
     *
     * @param zoomType
     *            The new zoomType value
     */
    public void setZoomType(ZoomType zoomType) {
        this.zoomType = zoomType;
    }

    /**
     * This method is called when the mouse is pressed inside a JMultiAxisPlot.
     * Currently, mouse clicks can signal the start of zoom, select, drag, or
     * pan operations.
     *
     * @param me
     *            The MouseEvent containing information on the current mouse
     *            position.
     */
    @Override
    public void mousePressed(MouseEvent me) {
        currentEvent = me;
        if (plot == null) {
            return;
        }

        plot.requestFocus();
        // Checkheck to make sure the pointer is inside the plotting region
        int X = me.getX();
        int Y = me.getY();
        DrawingRegion dr = plot.getPlotRegion();
        if (dr == null) {
            return;
        }

        Rectangle rect = dr.getRect();
        if (rect == null) {
            return;
        }
        if (!rect.contains(X, Y)) {
            return;
        }
        CoordinateTransform ct = plot.getCoordinateTransform();
        currentCoord = new Coordinate(X, Y);
        ct.PlotToWorld(getCurrentCoord());
        // Check to see if any plot objects were selected
        JSubplot sp = plot.getCurrentSubplot(X, Y);
        selectedSubplot = sp;
        if (sp == null) {
            return;
        }
        MouseMode mouseMode = plot.getMouseMode();
        PlotObject po = sp.getHotObject(X, Y);
        if (po == null) {
            currentObject = null;
            if (me.getClickCount() == 1) {
                objectObservable.sendPlotClickedMessage(me, currentCoord, selectedSubplot);
            }
        }

        if (po instanceof MarginButton) {
            marginButton = (MarginButton) po;
            marginButton.setToDepressedState(myGraphics);
        }

        // Send message that a PlotObject was clicked
        if (po != null && mouseMode != MouseMode.SELECT_REGION && mouseMode != MouseMode.PAN) {
            objectObservable.MouseButtonAction(me, po, mouseMode);
        }

        // Send a message that user is attempting to creat a pick by clicking on object in PickMode
        if (me.getButton() == 1 && mouseMode == MouseMode.CREATE_PICK) {
            currentPickCreationInfo = new PickCreationInfo(po, sp, getCurrentCoord(), me);
            objectObservable.createNewPick(currentPickCreationInfo);
            return;
        }

        // Zoom the plot out one level.
        if (me.getButton() == MouseEvent.BUTTON3 && po == null && mouseMode != MouseMode.SELECT_REGION) {
            sendZoomOutMessage();
            return;
        }

        // Maybe drag a plot object
        if (mouseMode == MouseMode.SELECT_ZOOM || mouseMode == MouseMode.CONTROL_SELECT) {
            if (po != null && me.getButton() == 1) { // Only allow drag with left button
                plot.unselect();
                maybeDragObject(po, X, Y, sp);
                return;
            }
        }

        // Start a Zoom operation
        if (mouseMode == MouseMode.SELECT_ZOOM || mouseMode == MouseMode.ZOOM_ONLY || mouseMode == MouseMode.SELECT_REGION) {
            if (zoomType == ZoomType.ZOOM_OFF) {
                return;
            }
            plot.unselect();
            startZoom(X, Y);
            return;
        }

        if (mouseMode == MouseMode.PAN) {
            startPan(X, Y, getCurrentCoord(), sp);
        }
        if (mouseMode == MouseMode.PAN2) {
            startPan2(X, Y, getCurrentCoord(), sp);
        }
    }

    private void startPan(int x, int y, Coordinate ct, JSubplot sp) {
        Rectangle newBounds = plot.getPlotRegion().getRect();
        plot.zoomToBox(newBounds);
        YAxis axis = sp.getYaxis();
        panMagnifierManager = new PanMagnifierManager(sp.getPlotTop(), sp.getPlotHeight(), y, axis.getMin(), axis.getMax(), ct.getWorldC2());

        panStartX = x;
        XAxis ax = plot.getXaxis();
        anchorXmin = ax.getMin();
        anchorXmax = ax.getMax();
        plot.setPolyLineUsage(false);
        handPanning = true;
        objectObservable.sendPanStartMessage(panStyle);
    }

    private void startPan2(int x, int y, Coordinate ct, JSubplot sp) {
        Rectangle newBounds = plot.getPlotRegion().getRect();
        plot.zoomToBox(newBounds);
        panStartX = x;
        XAxis xaxis = plot.getXaxis();
        anchorXmin = xaxis.getMin();
        anchorXmax = xaxis.getMax();
        plot.setPolyLineUsage(false);
        handPanning2 = true;
        panStartY = y;
        YAxis yaxis = sp.getYaxis();
        anchorYmin = yaxis.getMin();
        anchorYmax = yaxis.getMax();

    }

    private void startZoom(int x, int y) {
        anchorX = x;
        anchorY = y;
        lastX = anchorX;
        lastY = anchorY;

        myGraphics = (Graphics2D) plot.getGraphics();
        if (myGraphics == null) {
            myGraphics = (Graphics2D) plot.getGraphics();
        }
        myGraphics.setXORMode(Color.white);
        myGraphics.setColor(zoomColor);
        myGraphics.setStroke(new BasicStroke(1));
        //     Rectangle rect = plot.getPlotRegion().getRect();
        //     myGraphics.clip(rect);

        zooming = true;

    }

    private void maybeDragObject(PlotObject po, int x, int y, JSubplot sp) {
        if (JMultiAxisPlot.getAllowXor()) {
            myGraphics = (Graphics2D) plot.getGraphics();
            myGraphics.setXORMode(Color.white);
        }

        if (po instanceof VPickLine) {
            VPickLine vpl = (VPickLine) po;
            po = vpl.getWindow();
        }
        currentObject = po;
        if (po.getCanDragX() || po.getCanDragY()) {
            CoordinateTransform ct = plot.getCoordinateTransform();
            currentCoord = new Coordinate(x, y);
            ct.PlotToWorld(getCurrentCoord());
            DragLastX = getCurrentCoord().getWorldC1();
            DragLastY = getCurrentCoord().getWorldC2();
            currentDragSubplot = sp;
            dragging = true;
            if (isDraggingPickOrWindow(po)) {
                VPickLine vpl = getPickLine(po);
                pickMovedState = new PickMovedState(vpl, sp, vpl.getXval());
            } else if (po instanceof JWindowHandle) {
                JWindowHandle handle = (JWindowHandle) po;
                double duration = handle.getAssociatedPick().getWindow().getDuration();
                windowDurationChangedState = new WindowDurationChangedState(handle, sp, duration);
            } else if (po instanceof VPickLineErrorBar) {
                VPickLineErrorBar errorbar = (VPickLineErrorBar) po;
                double std = errorbar.getStd();
                pickErrorChangeState = new PickErrorChangeState(errorbar.getAssociatedPick(), sp, std);
            } else if (po instanceof LineWindowHandle) {
                LineWindowHandle tmp = (LineWindowHandle) po;
                tmp.startingDragOperation();
            } else if (currentObject instanceof LineWindow) {
                LineWindow tmp = (LineWindow) currentObject;
                tmp.startingDragOperation();
            }

        }
    }

    private static VPickLine getPickLine(PlotObject po) {
        if (po instanceof VPickLine) {
            return (VPickLine) po;
        } else if (po instanceof JWindowRegion) {
            JWindowRegion jwr = (JWindowRegion) po;
            return jwr.getAssociatedPick();
        } else {
            throw new IllegalArgumentException("PlotObject has no connection to a VPickLine.");
        }
    }

    private static boolean isDraggingPickOrWindow(PlotObject po) {
        return po instanceof VPickLine || po instanceof JWindowRegion;
    }

    /**
     * Method called when the mouse is released wihin a JMultiAxisPlot.
     * Currently this is used to manage the end of zoom, PlotObject drag, and
     * pan operations.
     *
     * @param me
     *            The MouseEvent containing information on the current mouse
     *            position.
     */
    @Override
    public void mouseReleased(MouseEvent me) {
        if (me.getClickCount() == 2) {
            objectObservable.sendPlotDoubleClickedMessage(me, currentCoord);
        }

        currentEvent = me;
        selectedSubplot = null;
        if (handPanning) {
            plot.setPolyLineUsage(true);
            plot.repaint();
            handPanning = false;
            objectObservable.sendPanCompleteMessage(panStyle);
            return;
        }

        if (handPanning2) {
            plot.setPolyLineUsage(true);
            plot.repaint();
            handPanning2 = false;
            return;
        }
        MouseMode mouseMode = plot.getMouseMode();

        if (marginButton != null) {
            marginButton.setToRollOverState(myGraphics);
        }
        if (currentObject != null) {
            objectObservable.MouseButtonAction(me, currentObject, mouseMode);
        }
        if (dragging && !zooming && mouseMode != MouseMode.SELECT_REGION) {
            finishObjectDrag();
            return;
        }
        if (!zooming) {
            return;
        }

        zooming = false;
        myGraphics = (Graphics2D) plot.getGraphics();

        if (zoomType == ZoomType.ZOOM_ALL || zoomType == ZoomType.ZOOM_ALL_RESCALE) {
            if (Math.abs(anchorX - lastX) < 2) {
                return;
            }
            if (mouseMode == MouseMode.SELECT_REGION) {
                SendRegionSelectMessage();
                return;
            }
            drawAllBox(myGraphics);
            sendZoomInMessage(zoomRect);
        } else if (zoomType == ZoomType.ZOOM_BOX) {
            drawbox(myGraphics);
            if (Math.abs(anchorX - lastX) < 2 || Math.abs(anchorY - lastY) < 2) {
                return;
            }
            if (mouseMode == MouseMode.SELECT_REGION) {
                SendRegionSelectMessage();
            } else {
                sendZoomInMessage(zoomRect);
            }
        }
    }

    private void finishObjectDrag() {
        myGraphics.setPaintMode();
        dragging = false;
        if (!(currentObject instanceof VPickLine) && !(currentObject instanceof VPickLineErrorBar)) {
            plot.repaint();
        }

        if (pickMovedState != null) {
            objectObservable.FinishDraggingPick(pickMovedState);
        }

        if (windowDurationChangedState != null) {
            objectObservable.finishChangingWindowDuration(windowDurationChangedState);
        }

        if (pickErrorChangeState != null) {
            objectObservable.finishChangingPickError(pickErrorChangeState);
        }

        if (currentObject instanceof LineWindowHandle) {
            LineWindowHandle tmp = (LineWindowHandle) currentObject;
            tmp.finishedDragOperation();
        } else if (currentObject instanceof LineWindow) {
            LineWindow tmp = (LineWindow) currentObject;
            tmp.finishedDragOperation();
        }

        pickErrorChangeState = null;
        windowDurationChangedState = null;
        pickMovedState = null;
        currentObject = null;
        currentDragSubplot = null;
    }

    @Override
    public void mouseClicked(MouseEvent me) {
        currentEvent = me;
        MouseMode mouseMode = plot.getMouseMode();
        if (currentObject != null) {
            objectObservable.MouseButtonAction(me, currentObject, mouseMode);
        }

        if (marginButton != null && marginButton.isEnabled()) {
            objectObservable.marginButtonClicked(marginButton);
            marginButton.setToNormalState(myGraphics);
        }
    }

    /**
     * The method called when the mouse is being dragged within the
     * JMultiAxisPlot. Currently, this is used to help manage plot zooming,
     * PlotObject dragging, and panning operations.
     *
     * @param me
     *            The MouseEvent containing information on the current mouse
     *            position.
     */
    @Override
    public void mouseDragged(MouseEvent me) {
        currentEvent = me;
        int x = me.getX();
        int y = me.getY();

        if (plot.getPlotRegion().getRect() == null || !plot.getPlotRegion().getRect().contains(x, y)) {
            return;
        }
        if (dragging && plot.getCurrentSubplot(x, y) != currentDragSubplot) {
            return;
        }
        if (dragging && currentObject != null) {
            dragCurrentObject(x, y);
            return;
        }
        if (handPanning) {
            doPanOperation(x, y);
            return;
        }
        if (handPanning2) {
            doPan2Operation(x, y);
            return;
        }
        if (!zooming) {
            return;
        }

        continueZoomOperation(x, y);
    }

    private void continueZoomOperation(int x, int y) {
        if (zoomType == ZoomType.ZOOM_ALL || zoomType == ZoomType.ZOOM_ALL_RESCALE) {
            drawAllBox(myGraphics);
        } else if (zoomType == ZoomType.ZOOM_BOX) {
            drawbox(myGraphics);
        }
        lastX = x;
        lastY = y;
        if (zoomType == ZoomType.ZOOM_ALL || zoomType == ZoomType.ZOOM_ALL_RESCALE) {
            drawAllBox(myGraphics);
        } else if (zoomType == ZoomType.ZOOM_BOX) {
            drawbox(myGraphics);
        }
    }

    private void doPanOperation(int x, int y) {
        if (panStyle == PanStyle.ChangeAxisLimits) {
            CoordinateTransform ct = plot.getCoordinateTransform();
            currentCoord = new Coordinate(x, y);
            ct.PlotToWorld(getCurrentCoord());
            double WorldX = getCurrentCoord().getWorldC1();
            getCurrentCoord().setX(panStartX);
            ct.PlotToWorld(getCurrentCoord());
            double dx = WorldX - getCurrentCoord().getWorldC1();
            plot.setAllXlimits(anchorXmin - dx, anchorXmax - dx);

            plot.scaleAllTraces(plot.getXaxis().getMin(), plot.getXaxis().getMax(), true);
            Limits limits = panMagnifierManager.getCurrentYLimits(y);
            YAxis axis = selectedSubplot.getYaxis();
            axis.setMin(limits.getMin());
            axis.setMax(limits.getMax());

            plot.repaint();
        } else if (panStyle == PanStyle.AdjustScaleFactor) {
            double magnification = panMagnifierManager.getMagnification(y);
            objectObservable.sendNewScaleFactor(magnification);
        } else {
            throw new IllegalArgumentException("Unsupported Pan Style!");
        }
    }

    private void doPan2Operation(int x, int y) {
        CoordinateTransform ct = plot.getCoordinateTransform();
        currentCoord = new Coordinate(x, y);
        ct.PlotToWorld(currentCoord);
        double WorldX = currentCoord.getWorldC1();
        double WorldY = currentCoord.getWorldC2();
        currentCoord.setX(panStartX);
        currentCoord.setY(panStartY);
        ct.PlotToWorld(currentCoord);
        double dx = WorldX - currentCoord.getWorldC1();
        double dy = WorldY - currentCoord.getWorldC2();
        plot.setAllXlimits(anchorXmin - dx, anchorXmax - dx);

        YAxis axis = selectedSubplot.getYaxis();
        axis.setMin(anchorYmin - dy);
        axis.setMax(anchorYmax - dy);
        plot.repaint();
    }

    private void dragCurrentObject(int x, int y) {
        CoordinateTransform ct = currentDragSubplot.getCoordinateTransform();
        currentCoord = new Coordinate(x, y);
        ct.PlotToWorld(getCurrentCoord());
        double X = getCurrentCoord().getWorldC1();
        double Y = getCurrentCoord().getWorldC2();
        currentObject.ChangePosition(currentDragSubplot, myGraphics, X - DragLastX, Y - DragLastY);
        DragLastX = X;
        DragLastY = Y;
    }

    /**
     * This method is called when the mouse first enters the JMultiAxisPlot.
     * Currently, it is used to set the mouse pointer style and to set the focus
     * to the JMultiAxisPlot.
     *
     * @param me
     *            The MouseEvent containing information on the current mouse
     *            position.
     */
    @Override
    public void mouseEntered(MouseEvent me) {
        //   The plot must get the focus when the mouse enters. Otherwise if a PickingStateManager
        // has been registered on this plot, keyboard events from within the plot may not be seen
        // as having come from plot, and unpredictable behavior will result.
        plot.requestFocusInWindow();
    }

    /**
     * The handler for mouse motion events. Currently, this is used just for
     * giving visual feedback on which objects are currently under the mouse
     * pointer.
     *
     * @param me
     *            The MouseEvent containing information on the current mouse
     *            position.
     */
    @Override
    public void mouseMoved(MouseEvent me) {
        currentEvent = me;
        if (plot == null || plot.getPlotRegion() == null || plot.getPlotRegion().getRect() == null) {
            selectedSubplot = null;
            currentObject = null;
            return;
        }
        int x = me.getX();
        int y = me.getY();

        CoordinateTransform ct = plot.getCoordinateTransform();
        currentCoord = new Coordinate(x, y);
        ct.PlotToWorld(getCurrentCoord());

        objectObservable.MouseMove(getCurrentCoord());

        if (!plot.getPlotRegion().getRect().contains(x, y)) {
            selectedSubplot = null;
            currentObject = null;
            currentPickCreationInfo = new PickCreationInfo(currentObject, selectedSubplot, getCurrentCoord(), me);
            return;
        }

        myGraphics = (Graphics2D) plot.getGraphics();
        JSubplot sp = plot.getCurrentSubplot(x, y);
        plot.setActiveSubplot(sp);
        if (sp == null) {
            plot.setToolTipText(null);
            selectedSubplot = null;
            currentObject = null;
            currentPickCreationInfo = new PickCreationInfo(currentObject, selectedSubplot, getCurrentCoord(), me);
            return;
        }
        selectedSubplot = sp;

        PlotObject po = sp.getHotObject(x, y);
        if (po == null) {
            if (marginButton != null) {
                marginButton.setToNormalState(myGraphics);
            }
            UnselectAll(myGraphics);
            currentObject = null;
            plot.setToolTipText(null);
            currentPickCreationInfo = new PickCreationInfo(currentObject, selectedSubplot, getCurrentCoord(), me);
            return;
        }
        currentObject = po;
        if (po instanceof MarginButton) {
            marginButton = (MarginButton) po;
            marginButton.setToRollOverState(myGraphics);
        }
        currentPickCreationInfo = new PickCreationInfo(currentObject, selectedSubplot, getCurrentCoord(), me);

        // Current object does not belong to this subplot, so skip it.
        if (po.getOwner() != sp) {
            return;
        }

        objectObservable.MouseOverAction(po);
        if (ObjectAlreadyHot(po)) {
            return;
        }
        // Hot object has changed
        resetHotObject(myGraphics, po);
    }

    @Override
    public void mouseExited(MouseEvent me) {
        currentEvent = me;
        plot.setToolTipText(null);
        plot.setActiveSubplot(null);
        if (marginButton != null) {
            marginButton.setToNormalState(myGraphics);
        }
        UnselectAll(myGraphics);
        currentPickCreationInfo = null;
    }

    private void resetHotObject(Graphics g, PlotObject po) {
        UnselectAll(g);
        if (po instanceof VPickLine) {
            mouseOverPick = (VPickLine) po;
            mouseOverPick.setSelected(true, g);
            objectObservable.sendPickSelectionStateChangeMessage(mouseOverPick, true);
            if (showPickTooltips) {
                PickDataBridge pdb = mouseOverPick.getDataBridge();
                if (pdb != null) {
                    plot.setToolTipText(pdb.getInfoString());
                }
            }
        } else if (po instanceof LineWindow) {
            mouseOverLineWindow = (LineWindow) po;
            mouseOverLineWindow.setSelected(true, g);
        } else if (po instanceof LineWindowHandle) {
            mouseOverLineWindowHandle = (LineWindowHandle) po;
            mouseOverLineWindowHandle.setSelected(true, g);
        } else if (po instanceof JWindowRegion) {
            mouseOverWindow = (JWindowRegion) po;
            mouseOverWindow.setSelected(true, g);
            if (showPickTooltips) {
                PickDataBridge pdb = mouseOverWindow.getAssociatedPick().getDataBridge();
                if (pdb != null) {
                    plot.setToolTipText(pdb.getInfoString());
                }
            }
        } else if (po instanceof VPickLineErrorBar) {
            mouseOverErrorBar = (VPickLineErrorBar) po;
            mouseOverErrorBar.setSelected(true, g);
        } else if (po instanceof JWindowHandle) {
            mouseOverWindowHandle = (JWindowHandle) po;
            mouseOverWindowHandle.setSelected(true, g);
        } else if (po instanceof Line) {
            mouseOverLine = (Line) po;
            mouseOverLine.setSelected(true, g);
        } else if (po instanceof Symbol) {
            mouseOverSymbol = (Symbol) po;
            mouseOverSymbol.setSelected(true, g);
        } else if (po instanceof MarginButton) {
            marginButton = (MarginButton) po;
        }
    }

    private void drawbox(Graphics g) {
        int xl = Math.min(anchorX, lastX);
        int xr = Math.max(anchorX, lastX);
        int yt = Math.min(anchorY, lastY);
        int yb = Math.max(anchorY, lastY);
        zoomRect = new Rectangle(xl, yt, xr - xl, yb - yt);
        ((Graphics2D) g).draw(zoomRect);
        ((Graphics2D) g).fill(zoomRect);
    }

    private void drawAllBox(Graphics g) {
        Rectangle rect = plot.getPlotRegion().getRect();
        int xl = Math.min(anchorX, lastX);
        int xr = Math.max(anchorX, lastX);
        zoomRect = new Rectangle(xl, (int) rect.getY(), xr - xl, (int) rect.getHeight());

        Graphics2D g2 = (Graphics2D) g;
        if (plot.getMouseMode() == MouseMode.SELECT_REGION) {
            plot.select(xl, xr);
        } else {
            g2.draw(zoomRect);
            g2.fill(zoomRect);
        }
    }

    private void UnselectAll(Graphics g) {
        if (mouseOverPick != null) {
            mouseOverPick.setSelected(false, g);
            objectObservable.sendPickSelectionStateChangeMessage(mouseOverPick, false);
        }
        if (mouseOverErrorBar != null) {
            mouseOverErrorBar.setSelected(false, g);
        }
        if (mouseOverWindow != null) {
            mouseOverWindow.setSelected(false, g);
        }
        if (mouseOverWindowHandle != null) {
            mouseOverWindowHandle.setSelected(false, g);
        }
        if (mouseOverLine != null) {
            mouseOverLine.setSelected(false, g);
        }
        if (mouseOverLineWindow != null) {
            mouseOverLineWindow.setSelected(false, g);
        }
        if (mouseOverLineWindowHandle != null) {
            mouseOverLineWindowHandle.setSelected(false, g);
        }
        if (mouseOverSymbol != null) {
            mouseOverSymbol.setSelected(false, g);
        }

        mouseOverPick = null;
        mouseOverErrorBar = null;
        mouseOverWindow = null;
        mouseOverWindowHandle = null;
        mouseOverLine = null;
        mouseOverLineWindow = null;
        mouseOverLineWindowHandle = null;
        mouseOverSymbol = null;
        marginButton = null;
        currentPickCreationInfo = null;
    }

    private boolean ObjectAlreadyHot(PlotObject po) {
        return po == mouseOverPick
                || po == mouseOverErrorBar
                || po == mouseOverWindow
                || po == mouseOverWindowHandle
                || po == mouseOverLine
                || po == mouseOverLineWindow
                || po == mouseOverLineWindowHandle
                || po == mouseOverSymbol;
    }

    private void SendRegionSelectMessage() {
        if (zoomRect == null) {
            return;
        }

        ArrayList<SubplotSelectionRegion> regions = plot.getSelectedRegionList(zoomRect);
        if (!regions.isEmpty()) {
            objectObservable.RegionSelectionAction(regions);
        }

        plot.repaint();
        plot.restorePreviousMouseMode();
    }

    JSubplot getCurrentSubplot() {
        return selectedSubplot;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        objectObservable.MouseKeyboardSelectAction(new JPlotKeyMessage(e, selectedSubplot, currentObject, controlKeyMapper));
        // Keymatic action causes this event to be fired repeatedly as long as key is down.
        // Only process for first event.
        if (!keyIsDown) {
            mouseZoomRefPoint = currentCoord;
            keyIsDown = true;
            keyCode = e.getKeyCode();
            MouseMode mouseMode = controlKeyMapper.getMouseMode(keyCode);
            if (mouseMode != null && currentObject == null) {
                modeChangeOccurred = true;
                plot.setMouseMode(mouseMode);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (modeChangeOccurred) {
            plot.revertToDefaultMouseMode();
            modeChangeOccurred = false;
        }
        keyIsDown = false;
        objectObservable.KeyReleasedAction(e);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public PickCreationInfo getPickCreationInfo() {
        return currentPickCreationInfo;
    }

    public final void setPanStyle(PanStyle panStyle) {
        this.panStyle = panStyle;
    }

    public MouseEvent getCurrentEvent() {
        return currentEvent;
    }

    public Coordinate getCurrentCoord() {
        return currentCoord;
    }

    private void sendZoomOutMessage() {
        plot.handleZoomOut();
        objectObservable.sendZoomOutMessage();
    }

    private void sendZoomInMessage(Rectangle zoomRect) {
        int xleft = zoomRect.x;
        int yleft = zoomRect.y;
        Coordinate cLeft = new Coordinate(xleft, yleft);
        CoordinateTransform ct = plot.getCoordinateTransform();
        ct.PlotToWorld(cLeft);

        int xRight = zoomRect.x + zoomRect.width;
        int yRight = zoomRect.y + zoomRect.height;
        Coordinate cRight = new Coordinate(xRight, yRight);
        ct.PlotToWorld(cRight);

        double xmin = cLeft.getWorldC1();
        double xmax = cRight.getWorldC1();
        ZoomInStateChange zisc = new ZoomInStateChange(zoomRect, xmin, xmax, plot);
        plot.handleZoomIn(zisc);
        objectObservable.sendZoomInMessage(zisc);
    }
}
