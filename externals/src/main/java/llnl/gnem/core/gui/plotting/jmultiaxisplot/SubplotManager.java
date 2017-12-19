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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Stack;

import llnl.gnem.core.gui.plotting.DrawingRegion;
import llnl.gnem.core.gui.plotting.ZoomLimits;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;

/**
 * A class that manages the JSubplots in a JMultiAxisPlot
 *
 * @author Doug Dodge
 */
public class SubplotManager {
    /**
     * Constructor for the SubplotManager object
     *
     * @param owner Reference to the JMultiAxisPlot that owns this subplot.
     */
    public SubplotManager(JMultiAxisPlot owner) {
        this.owner = owner;
        subplots = new ArrayList<>();
        plotSpacing = 5.0;
    }

    public Iterator<SubplotZoomData> iterator() {
        return subplots.iterator();
    }

    /**
     * Gets the spacing in mm between adjacent subplots
     *
     * @return The spacing in mm
     */
    public double getplotSpacing() {
        return plotSpacing;
    }

    /**
     * Sets the spacing in mm between adjacent subplots
     *
     * @param v The new spacing value
     */
    public void setplotSpacing(double v) {
        plotSpacing = v >= 0 ? v : 0.0;
    }

    public void setYaxisVisibility(boolean visible) {
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            YAxis ya = p.getYaxis();
            ya.setVisible(visible);
        }
    }

    public void setSubplotBoxVisibility(boolean visible) {
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            p.getPlotRegion().setDrawBox(visible);
            p.getPlotRegion().setFillRegion(visible);
        }
    }

    public void clearText() {
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            p.clearText();
        }
    }

    /**
     * Gets the JMultiAxisPlot that owns the SubplotManager object
     *
     * @return The JMultiAxisPlot reference
     */
    public JMultiAxisPlot getOwner() {
        return owner;
    }

    /**
     * Remove all subplots from the JMultiAxisPlot
     */
    public void clear() {
        for (SubplotZoomData szd: subplots) {
            JSubplot sp = szd.getSubplot();
            sp.Clear();
        }
        subplots.clear();
    }

    /**
     * Adds a new subplot to the SubplotManager object
     *
     * @param p The subplot to be added to the SubplotManager
     * @return A reference to the newly-added subplot
     */
    public JSubplot addSubplot(JSubplot p) {
        subplots.add(new SubplotZoomData(p));
        return p;
    }

    /**
     * Adds a new subplot to the SubplotManager object at a specific position
     *
     * @param p The subplot to be added to the SubplotManager
     * @param position The vertical position of the newly-added subplot (0-N
     * with 0 at the top)
     * @return A reference to the newly-added subplot
     */
    public JSubplot addSubplot(JSubplot p, int position) {
        int N = subplots.size();
        if (position >= N) {
            subplots.add(new SubplotZoomData(p));
        } else if (position < 0) {
            subplots.add(0, new SubplotZoomData(p));
        } else {
            subplots.add(position, new SubplotZoomData(p));
        }
        return p;
    }

    /**
     * Move the input subplot up by one position in the JMultiAxisPlot.
     *
     * @param p A reference to the subplot to be moved
     */
    public void MovePlotUp(JSubplot p) {
        int idx = getIndexOf(p);
        if (idx < 0) {
            throw new IllegalArgumentException("Subplot not found in this SubplotManager!");
        }
        SubplotZoomData v = subplots.remove(idx--);
        addSubplot(v, idx);
    }

    /**
     * Move the input subplot down by one position in the JMultiAxisPlot.
     *
     * @param p A reference to the subplot to be moved
     */
    public void MovePlotDown(JSubplot p) {
        int idx = getIndexOf(p);
        if (idx < 0) {
            throw new IllegalArgumentException("Subplot not found in this SubplotManager!");
        }
        Object v = subplots.remove(idx++);
        addSubplot((SubplotZoomData) v, idx);
    }

    /**
     * Move the input subplot to a new position in the JMultiAxisPlot.
     *
     * @param p A reference to the subplot to be moved
     * @param position The vertical position (0-N) to move to
     */
    public void MovePlot(JSubplot p, int position) {
        int idx = getIndexOf(p);
        if (idx < 0) {
            throw new IllegalArgumentException("Subplot not found in this SubplotManager!");
        }
        Object v = subplots.remove(idx);
        addSubplot((SubplotZoomData) v, position);
    }

    /**
     * remove the input subplot from the JMultiAxisPlot.
     *
     * @param p A reference to the subplot to be removed
     */
    public void RemovePlot(JSubplot p) {
        int idx = getIndexOf(p);
        if (idx < 0) {
            throw new IllegalArgumentException("Subplot not found in this SubplotManager!");
        }
        subplots.remove(idx);
    }

    /**
     * Sets the polyLineUsage of the contained subplots
     *
     * @param value The new polyLineUsage value
     */
    public void setPolyLineUsage(boolean value) {
        int N = subplots.size();
        for (int j = 0; j < N; ++j) {
            JSubplot p = subplots.get(j).getSubplot();
            p.setPolyLineUsage(value);
        }
    }

    public ArrayList<JSubplot> getSubplots() {
        ArrayList<JSubplot> result = new ArrayList<>();
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            result.add(p);
        }

        return result;
    }

    /**
     * render all the contained subplots to the supplied graphics context
     *
     * @param g The graphics context to use
     * @param topMargin The position (in pixels) of the top of the plotting area
     * in the JMultiAxisPlot object.
     * @param height The height of the plotting area in pixels.
     */
    public void Render(Graphics g, int topMargin, int height) {
        int N = getNumVisibleSubplots();
        if (N < 1) {
            return;
        }
        int ps = owner.getUnitsMgr().getVertUnitsToPixels(plotSpacing);
        if (N == 1) {
            JSubplot p = getFirstDisplayableSubplot();
            XAxis xaxis = p.getXaxis();
            owner.getXaxis().setMin(xaxis.getMin());
            owner.getXaxis().setMax(xaxis.getMax());

            // If there is only one plot, then render its axis even if its visibility is
            // set to false as long as the JMultiAxisPlot X-axis visibility is true.
            boolean XAxisVisible = owner.getXaxis().getVisible();
            boolean plotXaxisVisible = xaxis.getVisible();
            if (XAxisVisible) {
                xaxis.setVisible(true);
            }
            p.Render(g, topMargin, height);
            xaxis.setVisible(plotXaxisVisible);
        } else {
            int subplotHeight = (height - (N + 1) * ps) / N;
            int idx = 0;
            for (SubplotZoomData szd : subplots) {
                if (szd.isVisible()) {
                    int top = subplotHeight * idx + (idx + 1) * ps + topMargin;
                    JSubplot p = szd.getSubplot();
                    if (showALL) {
                        p.setShowALL(true);
                    }
                    p.Render(g, top, subplotHeight);
                    if (showALL) {
                        p.setShowALL(false);
                    }
                    ++idx;
                }
            }
        }
    }

    /**
     * Gets the global Xmin of all the subplots in the SubplotManager object
     *
     * @return The global Xmin value
     */
    double getGlobalXmin() {
        int N = subplots.size();
        if (N < 1) {
            return 0.0;
        }
        JSubplot p1 = (subplots.get(0)).getSubplot();
        double v = p1.getXaxis().getMin();
        for (int j = 1; j < N; ++j) {
            JSubplot p = (subplots.get(j)).getSubplot();
            v = Math.min(v, p.getXaxis().getMin());
        }
        return v;
    }

    /**
     * Gets the global Xmax of all the subplots in the SubplotManager object
     *
     * @return The global Xmax value
     */
    double getGlobalXmax() {
        int N = subplots.size();
        if (N < 1) {
            return 0.0;
        }
        JSubplot p1 = (subplots.get(0)).getSubplot();
        double v = p1.getXaxis().getMax();
        for (int j = 1; j < N; ++j) {
            JSubplot p = (subplots.get(j)).getSubplot();
            v = Math.max(v, p.getXaxis().getMax());
        }
        return v;
    }

    /**
     * Sets the allXlimits attribute of the SubplotManager object
     *
     * @param xmin The new allXlimits value
     * @param xmax The new allXlimits value
     */
    void setAllXlimits(double xmin, double xmax) {
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            p.getXaxis().setMin(xmin);
            p.getXaxis().setMax(xmax);
        }
    }

    /**
     * Description of the Method
     *
     * @param xmin Description of the Parameter
     * @param xmax Description of the Parameter
     */
    void ScaleAllTraces(double xmin, double xmax, boolean autoScaleY) {
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            p.ScaleTraces(xmin, xmax, autoScaleY);
        }
    }

    public void YScaleAllTraces(double scale, boolean centerOnZero) {
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            p.Scale(scale, centerOnZero);
        }
    }
    
    public Stack<ZoomLimits> getZoomLimits(JSubplot p) {
        for (SubplotZoomData szd : subplots) {
            if(szd.getSubplot() == p ){
                return szd.getZoomLimits();
            }
        }
        return new Stack<>();
    }

    /**
     * Description of the Method
     *
     * @param zoomRect Description of the Parameter
     */
    public void zoomToBox(Rectangle zoomRect) {
        Area zoomArea = new Area(zoomRect);
        double globalXmax = 1;
        double globalXmin = 0;
        for (SubplotZoomData szd : subplots) {
            if (szd.isDisplayable()) {
                JSubplot p = szd.getSubplot();
                //Area subplotbox = new Area(p.getPlotRegion().getRect());
                Shape s1;
                Area subplotbox;
                DrawingRegion dr1;
                if (p==null) 
                    continue;
                dr1=p.getPlotRegion();
                if (dr1==null)
                    continue;
                s1=dr1.getRect();
                if (s1==null)
                    continue;
                subplotbox=new Area(s1);
                if (subplotbox==null)
                    continue;
                
                
                subplotbox.intersect(zoomArea);
                if (subplotbox.isEmpty()) {
                    XAxis ax = p.getXaxis();
                    YAxis ay = p.getYaxis();
                    ZoomLimits newLimits = new ZoomLimits(ax.getMin(), ax.getMax(), ay.getMin(), ay.getMax());
                    szd.zoomIn(false, newLimits);
                } else {
                    ZoomLimits newLimits = getSubplotZoomLimits(p, subplotbox);
                    globalXmin = newLimits.xmin;
                    globalXmax = newLimits.xmax;
                    szd.zoomIn(true, newLimits);

                }
            } else {
                JSubplot p = szd.getSubplot();
                XAxis ax = p.getXaxis();
                YAxis ay = p.getYaxis();
                ZoomLimits newLimits = new ZoomLimits(ax.getMin(), ax.getMax(), ay.getMin(), ay.getMax());
                szd.zoomIn(false, newLimits);
            }
        }
        owner.getXaxis().setMin(globalXmin);
        owner.getXaxis().setMax(globalXmax);
    }

    public void zoomToNewLimits(ZoomLimits newLimits) {
        double globalXmax = 1;
        double globalXmin = 0;
        for (SubplotZoomData szd : subplots) {
            globalXmin = newLimits.xmin;
            globalXmax = newLimits.xmax;
            szd.zoomIn(true, newLimits);
        }
        owner.getXaxis().setMin(globalXmin);
        owner.getXaxis().setMax(globalXmax);

    }

    public void initXLimits(double xmin, double xmax) {
        for (SubplotZoomData szd : subplots) {
            JSubplot subplot = szd.getSubplot();
            YAxis axis = subplot.getYaxis();
            ZoomLimits limits = new ZoomLimits(xmin, xmax, axis.getMin(), axis.getMax());
            szd.initLimits(limits);
        }
        owner.getXaxis().setMin(xmin);
        owner.getXaxis().setMax(xmax);
    }

    public void zoomToNewXLimits(double xmin, double xmax) {
        for (SubplotZoomData szd : subplots) {
            JSubplot subplot = szd.getSubplot();
            YAxis axis = subplot.getYaxis();
            ZoomLimits limits = new ZoomLimits(xmin, xmax, axis.getMin(), axis.getMax());
            szd.zoomIn(true, limits);
        }
        owner.getXaxis().setMin(xmin);
        owner.getXaxis().setMax(xmax);
    }

    public void zoomToNewYlimits(double ymin, double ymax) {
        double xmin = owner.getXaxis().getMin();
        double xmax = owner.getXaxis().getMax();
        for (SubplotZoomData szd : subplots) {
            ZoomLimits limits = new ZoomLimits(xmin, xmax, ymin, ymax);
            szd.zoomIn(true, limits);
        }
    }

    public void zoomPlotToNewYlimits(JSubplot plot, double ymin, double ymax) {
        double xmin = owner.getXaxis().getMin();
        double xmax = owner.getXaxis().getMax();
        for (SubplotZoomData szd : subplots) {
            if (szd.getSubplot() == plot) {
                ZoomLimits limits = new ZoomLimits(xmin, xmax, ymin, ymax);
                szd.zoomIn(true, limits);
                break;
            }
        }

    }

    public void scaleAllSubplotsToMatchSelected(JSubplot aPlot) {
        double ymin = aPlot.getYaxis().getMin();
        double ymax = aPlot.getYaxis().getMax();
        zoomToNewYlimits(ymin, ymax);
    }

    public void setSubplotDisplayable(JSubplot p, boolean v) {
        for (SubplotZoomData szd : subplots) {
            if (szd.getSubplot() == p) {
                szd.setDisplayable(v);
                return;
            }
        }
    }

    boolean ZoomOut() {
        double globalXmin = 0;
        double globalXmax = 1;
        boolean zoomedOut = false;
        for (SubplotZoomData szd : subplots) {
            if (szd.zoomOut()) {
                zoomedOut = true;
                if (szd.isVisible()) {
                    JSubplot p = szd.getSubplot();
                    XAxis ax = p.getXaxis();
                    globalXmin = ax.getMin();
                    globalXmax = ax.getMax();
                }
            }
        }
        if (zoomedOut) {
            owner.getXaxis().setMin(globalXmin);
            owner.getXaxis().setMax(globalXmax);
        }
        return zoomedOut;
    }

    public void UnzoomAll() {
        double globalXmin = 0;
        double globalXmax = 1;
        for (SubplotZoomData szd : subplots) {
            szd.unzoomAll();
            if (szd.isVisible()) {
                JSubplot p = szd.getSubplot();
                XAxis ax = p.getXaxis();
                globalXmin = ax.getMin();
                globalXmax = ax.getMax();
            }
        }
        owner.getXaxis().setMin(globalXmin);
        owner.getXaxis().setMax(globalXmax);
        owner.repaint();
    }

    ArrayList<SubplotSelectionRegion> getSelectedRegionList(Rectangle zoomRect) {
        ArrayList<SubplotSelectionRegion> result = new ArrayList<>();
        Area zoomArea = new Area(zoomRect);
        for (SubplotZoomData szd : subplots) {
            if (szd.isVisible()) {
                JSubplot p = szd.getSubplot();
                Area subplotbox = new Area(p.getPlotRegion().getRect());
                subplotbox.intersect(zoomArea);
                if (!subplotbox.isEmpty()) {
                    ZoomLimits zl = getSubplotZoomLimits(p, subplotbox);
                    result.add(new SubplotSelectionRegion(p, zl));
                }
            }
        }
        return result;
    }

    private static ZoomLimits getSubplotZoomLimits(JSubplot p, Area subplotbox) {
        Rectangle r = subplotbox.getBounds();
        CoordinateTransform ct = p.getCoordinateTransform();
        Coordinate coord = new Coordinate(r.getX(), r.getY());
        reInitializeTransform(p, ct);

        ct.PlotToWorld(coord);
        double Xmin = coord.getWorldC1();
        double ymin = coord.getWorldC2();

        coord.setX(r.getX() + r.getWidth());
        coord.setY(r.getY() + r.getHeight());
        ct.PlotToWorld(coord);
        double Xmax = coord.getWorldC1();
        double ymax = coord.getWorldC2();

        if (Xmax < Xmin) {
            double tmp = Xmin;
            Xmin = Xmax;
            Xmax = tmp;
        }
        if (ymax < ymin) {
            double tmp = ymin;
            ymin = ymax;
            ymax = tmp;
        }
        return new ZoomLimits(Xmin, Xmax, ymin, ymax);
    }

    private static void reInitializeTransform(JSubplot p, CoordinateTransform ct) {
        double xmin = p.getXaxis().getMin();
        double xmax = p.getXaxis().getMax();

        int LeftMargin = p.getPlotLeft();
        int BoxWidth = p.getPlotWidth();
        double ymin = p.getYaxis().getMin();
        double ymax = p.getYaxis().getMax();
        int top = p.getPlotTop();
        int height = p.getPlotHeight();

        ct.initialize(xmin, xmax, LeftMargin, BoxWidth,
                ymin, ymax, top, height);
    }

    /**
     * Gets the clickedObject attribute of the SubplotManager object
     *
     * @param X Description of the Parameter
     * @param Y Description of the Parameter
     * @return The clickedObject value
     */
    PlotObject getClickedObject(int X, int Y) {
        int N = subplots.size();
        if (N < 1) {
            return null;
        }
        for (SubplotZoomData szd : subplots) {
            JSubplot p = szd.getSubplot();
            PlotObject po = p.getHotObject(X, Y);
            if (po != null) {
                return po;
            }
        }
        return null;
    }

    /**
     * Gets the clickedSubplot attribute of the SubplotManager object
     *
     * @param X Description of the Parameter
     * @param Y Description of the Parameter
     * @return The clickedSubplot value
     */
    public JSubplot getCurrentSubplot(int X, int Y) {
        int N = subplots.size();
        if (N < 1) {
            return null;
        }
        for (int j = 0; j < N; ++j) {
            SubplotZoomData szd = subplots.get(j);
            if (!szd.isVisible()) {
                continue;
            }
            JSubplot p = szd.getSubplot();
            if ((p == null) || (p.getPlotRegion() == null) || (p.getPlotRegion().getRect() == null)) {
                return null;
            }
            if (p.getVisible() && p.getPlotRegion().getRect().contains(X, Y)) {
                return p;
            }
        }
        return null;
    }

    public int getNumVisibleSubplots() {
        int N = subplots.size();
        if (N < 1) {
            return 0;
        }
        int count = 0;
        for (int j = 0; j < N; ++j) {
            SubplotZoomData szd = subplots.get(j);
            if (szd.isVisible()) {
                ++count;
            }
        }
        return count;
    }

    private JSubplot getFirstDisplayableSubplot() {
        int N = subplots.size();
        if (N < 1) {
            return null;
        }
        for (int j = 0; j < N; ++j) {
            SubplotZoomData szd = subplots.get(j);
            if (szd.isVisible()) {
                return szd.getSubplot();
            }
        }
        return null;
    }

    private void addSubplot(SubplotZoomData p, int position) {
        int N = subplots.size();
        if (position >= N) {
            subplots.add(p);
        } else if (position < 0) {
            subplots.add(0, p);
        } else {
            subplots.add(position, p);
        }
    }

    private int getIndexOf(JSubplot pin) {
        int N = subplots.size();
        if (N < 1) {
            return -1;
        }
        for (int j = 0; j < N; ++j) {
            JSubplot p = (subplots.get(j)).getSubplot();
            if (p == pin) {
                return j;
            }
        }
        return -1;
    }
    private final JMultiAxisPlot owner;
    private final ArrayList<SubplotZoomData> subplots;
    private double plotSpacing;

    public boolean isShowALL() {
        return showALL;
    }

    public void setShowALL(boolean showALL) {
        this.showALL = showALL;
    }
    private boolean showALL;
}
