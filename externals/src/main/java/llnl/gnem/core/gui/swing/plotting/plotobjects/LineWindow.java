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
package llnl.gnem.core.gui.swing.plotting.plotobjects;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import llnl.gnem.core.gui.swing.plotting.JBasicPlot;
import llnl.gnem.core.gui.swing.plotting.JPlotContainer;
import llnl.gnem.core.gui.swing.plotting.PaintMode;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;
import llnl.gnem.core.polygon.BinarySearch;
import llnl.gnem.core.util.Pair;

/**
 * User: dodge1 Date: Jul 21, 2005 Time: 12:57:55 PM
 */
public class LineWindow extends PlotObject {

    private Line owningLine;
    protected float[] xArray;
    protected float beginTimeAtDragStart = 0;
    private float[] yArray;
    private final Color color = new Color(200, 255, 200);
    private final Color disabledColor = Color.LIGHT_GRAY;
    private final Color selectedColor = new Color(255, 200, 200);
    private Color renderColor;
    protected LineWindowHandle leftHandle;
    protected LineWindowHandle rightHandle;
    private final PaintMode Mode = PaintMode.XOR;
    private final int numRegionSegments = 100;
    private CoordinateTransform coordTransform;
    private boolean selected;
    private Area currentArea;
    private static final int VERY_BIG_WINDOW_DIMENSION = 10000;
    private static final int TOLERANCE = 6;
    private static final int MIN_POINTS_MIN_LIMIT = 10;
    private int minPointsInWindow = MIN_POINTS_MIN_LIMIT;
    private double minWindowLength;
    private LineWindowMigrationManager migrationManager = null;
    private boolean canMigrate = true;
    private boolean usable = true;

    public LineWindow(Line owner, double startTime, double endTime) {
        initialize(owner, endTime, startTime);

    }

    Pair validateTimeBounds(double endTime, double startTime) {
        LineBounds bounds = owningLine.getLineBounds();
        int npts = owningLine.length();
        if (npts < minPointsInWindow) {
            throw new IllegalStateException("Line is too short to host a LineWindow!");
        }

        double lineDuration = bounds.xmax - bounds.xmin;
        double averageDt = lineDuration / npts;
        double minDuration = minPointsInWindow * averageDt;
        if (endTime <= startTime + minDuration) {
            endTime = startTime + minDuration;
        }
        if (startTime < bounds.xmin) {
            double increment = bounds.xmin - startTime;
            startTime += increment;
            endTime += increment;
        }
        if (endTime > bounds.xmax) {
            double decrement = bounds.xmax - endTime;
            endTime += decrement;
            startTime += decrement;
        }
        if (startTime < bounds.xmin) {
            startTime = bounds.xmin - startTime;
        }
        return new Pair(startTime, endTime);
    }

    private void initialize(Line owner, double endTime, double startTime) {
        owningLine = owner;

        Pair bounds = validateTimeBounds(endTime, startTime);
        startTime = (Double) bounds.getFirst();
        endTime = (Double) bounds.getSecond();
        Pair windowArrays = owner.getSubsectionData(startTime, endTime, minPointsInWindow);
        xArray = (float[]) windowArrays.getFirst();
        yArray = (float[]) windowArrays.getSecond();
        double dx = xArray[1] - xArray[0];
        minWindowLength = (minPointsInWindow - 1) * dx;
        renderColor = color;
        canDragX = true;

        leftHandle = new LineWindowHandle(this, startTime);
        rightHandle = new LineWindowHandle(this, endTime);
    }

    public LineWindow(Line owner, double startTime, double endTime, LineWindowMigrationManager manager) {
        initialize(owner, endTime, startTime);
        migrationManager = manager;
    }

    public double getMinimumLimit() {
        LineBounds bounds = owningLine.getLineBounds();
        return bounds.xmin;
    }

    public double getMaximumLimit() {
        LineBounds bounds = owningLine.getLineBounds();
        return bounds.xmax;
    }

    @Override
    public void setSelected(boolean selected, Graphics g) {
        if (this.selected == selected) {
            return;
        }
        reRender(g, owner);
        Color tmp = usable ? color : disabledColor;
        renderColor = selected ? selectedColor : tmp;
        reRender(g, owner);
        this.selected = selected;
    }

    public Rectangle getBoundingRect() {
        if (currentArea != null) {
            return currentArea.getBounds();
        } else {
            return null;
        }
    }

    public double getStartTime() {
        return leftHandle.getXval();
    }

    public double getEndTime() {
        return rightHandle.getXval();
    }

    public double getDuration() {
        return getEndTime() - getStartTime();
    }

    public void reRender(Graphics g, JBasicPlot owningPlot) {
        if (region.isEmpty()) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        Rectangle rect = owner.getPlotRegion().getRect();
        g2d.clip(rect);
        g2d.setColor(renderColor);
        Mode.setGraphicsPaintMode(g2d);
        g2d.fill(currentArea);
    }

    @Override
    public void render(Graphics g, JBasicPlot owningPlot) {

        if (!isVisible() || g == null || owningPlot == null) {
            return;
        }
        currentArea = getArea(xArray, yArray, 0, xArray.length, owningPlot);
        Graphics2D g2d = (Graphics2D) g;
        Rectangle rect = owner.getPlotRegion().getRect();
        g2d.clip(rect);
        region.clear();
        Color tmp = usable ? color : disabledColor;
        renderColor = selected ? selectedColor : tmp;

        g2d.setColor(renderColor);
        Mode.setGraphicsPaintMode(g2d);
        coordTransform = owner.getCoordinateTransform();

        if (currentArea != null) {
            removeAreaOutsideHandles();

            addToRegion(currentArea);
            g2d.fill(currentArea);
        }
        leftHandle.render(g, owningPlot);
        rightHandle.render(g, owningPlot);

    }

    private void removeAreaOutsideHandles() {
        Coordinate coord = new Coordinate(0.0, 0.0);
        coord.setWorldC1(leftHandle.getXval());
        coord.setWorldC2(0.0);
        coordTransform.WorldToPlot(coord);
        Area removeArea = createRemoveArea(0, (int) coord.getX());
        if (removeArea != null) {
            currentArea.subtract(removeArea);
        }

        coord.setWorldC1(rightHandle.getXval());
        coordTransform.WorldToPlot(coord);
        removeArea = createRemoveArea((int) coord.getX(), VERY_BIG_WINDOW_DIMENSION);
        if (removeArea != null) {
            currentArea.subtract(removeArea);
        }
    }

    @Override
    public ArrayList<LineWindowHandle> getContainedObjects() {
        ArrayList<LineWindowHandle> v = new ArrayList<>();
        v.add(leftHandle);
        v.add(rightHandle);
        return v;
    }

    @Override
    public boolean hasContainedObjects() {
        return true;
    }

    public void changeWindowStart(double dx) {
        double leftLimit = getMinimumLimit();
        double oldValue = leftHandle.getXval();
        double newValue = oldValue + dx;
        if (newValue < leftLimit) {
            newValue = leftLimit;
        }
        Pair arrays = getWindowDataForLeftSideChange(newValue);
        xArray = (float[]) arrays.getFirst();
        yArray = (float[]) arrays.getSecond();
        leftHandle.setXval(newValue);
    }

    public void changeWindowEnd(double dx) {
        double rightLimit = getMaximumLimit();
        double oldValue = rightHandle.getXval();
        double newValue = oldValue + dx;
        if (newValue > rightLimit) {
            newValue = rightLimit;
        }
        Pair arrays = this.getWindowDataForRightSideChange(newValue);
        xArray = (float[]) arrays.getFirst();
        yArray = (float[]) arrays.getSecond();
        rightHandle.setXval(newValue);
    }

    public void moveWindow(double dx) {
        dx = keepWindowInTraceLimits(dx);
        double newStartTime = leftHandle.getXval() + dx;
        leftHandle.setXval(newStartTime);
        double newEndTime = rightHandle.getXval() + dx;
        rightHandle.setXval(newEndTime);
        Pair arrays = getNewWindowData(newStartTime, newEndTime);
        xArray = (float[]) arrays.getFirst();
        yArray = (float[]) arrays.getSecond();

    }

    private Pair getWindowDataForHandleChange(LineWindowHandle lwh) {
        double newValue = lwh.getXval();
        if (lwh == leftHandle) {
            return getWindowDataForLeftSideChange(newValue);
        } else {
            return getWindowDataForRightSideChange(newValue);
        }

    }

    public void startingDragOperation() {
        beginTimeAtDragStart = xArray[0];
    }

    protected void windowHandleWasMoved(LineWindowHandle lwh, JBasicPlot owningPlot) {
        ensureWindowMeetsMinLengthCriterion(lwh);

        double newValue = lwh.getXval();
        Pair windowArrays = getWindowDataForHandleChange(lwh);
        float[] x = (float[]) windowArrays.getFirst();
        float[] y = (float[]) windowArrays.getSecond();
        coordTransform = owner.getCoordinateTransform();

        JPlotContainer container = owningPlot.getOwner();
        if (container != null) {
            Graphics g = container.getGraphics();
            if (g != null) {
                if (lwh == leftHandle) {

                    double dx = newValue - xArray[0];

                    if (dx < 0) {
                        int offset = getArrayOffset(xArray, x);
                        Area addArea = getArea(x, y, 0, offset, owningPlot);
                        if (addArea != null) {
                            ensureNoOverlapWithCurrentArea(addArea);
                            currentArea.add(addArea);
                        }
                        rePaintCurrentArea(g, addArea, null);
                    } else {
                        Area removeArea = createLeftSideRemoveArea(x[0], y[0]);
                        modifyCurrentArea(removeArea, null);
                        rePaintCurrentArea(g, null, removeArea);
                    }

                } else {

                    double dx = newValue - xArray[xArray.length - 1];
                    if (Math.abs(dx) < xArray[1] - xArray[0]) {
                        return;
                    }
                    if (dx < 0) {
                        int npts = x.length;
                        if (npts < xArray.length) {
                            Area removeArea = createRightSideRemoveArea(xArray[npts - 1], yArray[npts - 1]);
                            modifyCurrentArea(removeArea, null);
                            rePaintCurrentArea(g, null, removeArea);
                        }
                    } else {
                        int offset = getLeftArrayOffset(xArray, x);
                        int npts = x.length;
                        Area addArea = getArea(x, y, npts - offset, npts, owningPlot);
                        if (addArea != null) {
                            ensureNoOverlapWithCurrentArea(addArea);
                            currentArea.add(addArea);
                        }
                        rePaintCurrentArea(g, addArea, null);

                    }

                }

                xArray = x;
                yArray = y;
            }
        }
    }

    private void ensureWindowMeetsMinLengthCriterion(LineWindowHandle lwh) {
        if (lwh == rightHandle) {
            if (lwh.getXval() < leftHandle.getXval() + minWindowLength) {
                lwh.setXval(leftHandle.getXval() + minWindowLength);
            }
        } else {
            if (lwh.getXval() > rightHandle.getXval() - minWindowLength) {
                lwh.setXval(rightHandle.getXval() - minWindowLength);
            }
        }
    }

    double limitWindowCrush(LineWindowHandle lwh, double dx) {
        double proposed = lwh.getXval() + dx;
        if (lwh == leftHandle && dx > 0) {
            int maxIndex = xArray.length - minPointsInWindow;
            if (proposed >= xArray[maxIndex]) {
                return xArray[maxIndex] - lwh.getXval();
            }
            int[] range = BinarySearch.bounds(xArray, (float) proposed);
            if (range[0] < 0 || range[0] > maxIndex) {
                return xArray[maxIndex] - lwh.getXval();
            } else {
                return dx;
            }
        } else if (lwh == rightHandle && dx < 0) {
            int minIndex = minPointsInWindow - 1;
            double restrictedDx = xArray[minIndex] - lwh.getXval();
            if (proposed < xArray[0]) {
                return restrictedDx;
            }
            int[] range = BinarySearch.bounds(xArray, (float) proposed);
            if (range[0] < 0 || range[1] < minIndex) {
                return restrictedDx;
            } else {
                return dx;
            }
        } else {
            return dx;
        }
    }

    private Area getArea(float[] x, float[] y, int idxStart, int idxEnd, JBasicPlot owningPlot) {
        RegionGenerator rg = new RegionGenerator(owningPlot.getPlotRegion().getRect(), numRegionSegments, RegionGenerationStyle.RECTANGULAR_BLOCK);

        rg.setToleranceInPixels(TOLERANCE);
        coordTransform = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate(0.0, 0.0);

        for (int j = idxStart; j < idxEnd; ++j) {
            double xval = x[j];
            coord.setWorldC1(xval);
            coord.setWorldC2(y[j]);
            coordTransform.WorldToPlot(coord);
            Point2D P = new Point2D.Double(coord.getX(), coord.getY());
            Point p = new Point((int) P.getX(), (int) P.getY());
            rg.addPoint(p.x, p.y);
        }
        return rg.generateRegion();
    }

    public void updateXYData(JBasicPlot owningPlot) {
        ChangePosition(owningPlot, null, 0.0, 0.0);
    }

    @Override
    public void ChangePosition(JBasicPlot owningPlot, Graphics graphics, double dx, double dy) {
        dx = keepWindowInTraceLimits(dx);

        if (graphics != null) {
            // erase both window handles first
            leftHandle.render(graphics, owningPlot);
            rightHandle.render(graphics, owningPlot);
            // change their values
            double newStartTime = leftHandle.getXval() + dx;
            leftHandle.setXval(newStartTime);
            double newEndTime = rightHandle.getXval() + dx;
            rightHandle.setXval(newEndTime);
            Pair windowArrays = getNewWindowData(newStartTime, newEndTime);
            updateAndPaintNewWindowRegion(windowArrays, dx, owningPlot, graphics);
            // redraw the window handles
            leftHandle.render(graphics, owningPlot);
            rightHandle.render(graphics, owningPlot);
        }

    }

    private double keepWindowInTraceLimits(double dx) {
        double minLimit = getMinimumLimit();
        double tmpPos = xArray[0] + dx;
        if (tmpPos < minLimit) {
            dx = xArray[0] - minLimit;
        }

        tmpPos = xArray[xArray.length - 1] + dx;
        double maxLimit = getMaximumLimit();
        if (tmpPos > maxLimit) {
            dx = maxLimit - xArray[xArray.length - 1];
        }
        return dx;
    }

    private void updateAndPaintNewWindowRegion(Pair windowArrays, double dx, JBasicPlot owningPlot, Graphics g) {
        float[] x = (float[]) windowArrays.getFirst();
        float[] y = (float[]) windowArrays.getSecond();
        Area removeArea = null;
        Area addArea = null;
        coordTransform = owner.getCoordinateTransform();
        if (dx > 0) {
            int npts = x.length;
            int offset = getArrayOffset(x, xArray);
            if (offset != 0) {
                removeArea = createLeftSideRemoveArea(x[0], y[0]);
                int start = npts - offset;
                addArea = getArea(x, y, start, npts, owningPlot);
            }
        } else if (dx < 0) {
            int npts = x.length;
            int offset = getArrayOffset(xArray, x);
            if (offset != 0) {
                removeArea = createRightSideRemoveArea(x[npts - 1], x[npts - 1]);
                addArea = getArea(x, y, 0, offset, owningPlot);
            }
        }

        modifyCurrentArea(removeArea, addArea);
        rePaintCurrentArea(g, addArea, removeArea);

        xArray = x;
        yArray = y;
    }

    private Pair getNewWindowData(double newStartTime, double newEndTime) {
        Pair bounds = validateTimeBounds(newEndTime, newStartTime);
        newStartTime = (Double) bounds.getFirst();
        newEndTime = (Double) bounds.getSecond();
        return owningLine.getSubsectionData(newStartTime, newEndTime, minPointsInWindow);
    }

    private Pair getWindowDataForLeftSideChange(double newTime) {
        double endTime = xArray[xArray.length - 1];
        Pair bounds = validateTimeBounds(endTime, newTime);
        newTime = (Double) bounds.getFirst();
        endTime = (Double) bounds.getSecond();
        return owningLine.getSubsectionData(newTime, endTime, minPointsInWindow);
    }

    private Pair getWindowDataForRightSideChange(double newTime) {
        double startTime = xArray[0];
        Pair bounds = validateTimeBounds(newTime, startTime);
        startTime = (Double) bounds.getFirst();
        newTime = (Double) bounds.getSecond();
        return owningLine.getSubsectionData(startTime, newTime, minPointsInWindow);
    }

    private void rePaintCurrentArea(Graphics g, Area addArea, Area removeArea) {
        Graphics2D g2d = (Graphics2D) g;
        Rectangle rect = owner.getPlotRegion().getRect();
        g2d.clip(rect);
        g2d.setColor(renderColor);
        Mode.setGraphicsPaintMode(g2d);

        if (addArea != null) {
            g2d.fill(addArea);
        }
        if (removeArea != null) {
            g2d.fill(removeArea);
        }
    }

    private void modifyCurrentArea(Area removeArea, Area addArea) {
        if (removeArea != null) {
            currentArea.subtract(removeArea);
        }
        if (addArea != null) {
            ensureNoOverlapWithCurrentArea(addArea);
            currentArea.add(addArea);
        }
    }

    private void ensureNoOverlapWithCurrentArea(Area addArea) {
        Area tmp = new Area(addArea);
        tmp.intersect(currentArea);
        addArea.subtract(tmp);
    }

    private Area createRightSideRemoveArea(float x, float y) {
        Coordinate coord = new Coordinate(0.0, 0.0);

        coord.setWorldC1(x);
        coord.setWorldC2(y);
        coordTransform.WorldToPlot(coord);
        return createRemoveArea((int) coord.getX(), VERY_BIG_WINDOW_DIMENSION);
    }

    private Area createLeftSideRemoveArea(float x, float y) {
        Coordinate coord = new Coordinate(0.0, 0.0);
        coord.setWorldC1(x);
        coord.setWorldC2(y);
        coordTransform.WorldToPlot(coord);
        return createRemoveArea(0, (int) coord.getX());
    }

    private Area createRemoveArea(int x, int width) {
        Area removeArea;
        Rectangle rect = new Rectangle(x, 0, width, VERY_BIG_WINDOW_DIMENSION);
        removeArea = new Area(currentArea);
        removeArea.intersect(new Area(rect));
        return removeArea;

    }

    private int getArrayOffset(float[] x, float[] otherX) {
        int offset = 0;
        float xStart = x[0];
        for (int j = 0; j < otherX.length; ++j) {
            if (otherX[j] == xStart) {
                offset = j;
                break;
            }
        }
        return offset;
    }

    private int getLeftArrayOffset(float[] x, float[] otherX) {
        int offset = 0;
        float xStart = x[x.length - 1];
        for (int j = otherX.length - 1; j >= 0; --j) {
            if (otherX[j] == xStart) {
                offset = j;
                break;
            }
        }
        return offset;

    }

    public int getMinPointsInWindow() {
        return minPointsInWindow;
    }

    public void setMinPointsInWindow(int minPointsInWindow) {
        if (minPointsInWindow < MIN_POINTS_MIN_LIMIT) {
            minPointsInWindow = MIN_POINTS_MIN_LIMIT;
        }

        this.minPointsInWindow = minPointsInWindow;
        double dx = xArray[1] - xArray[0];
        minWindowLength = (minPointsInWindow - 1) * dx;

    }

    protected void windowHandleMoveComplete(LineWindowHandle lwh, double totalChange) {
        if (migrationManager != null) {
            if (lwh == leftHandle) {
                migrationManager.startTimeChanged(this, totalChange);
            } else {
                migrationManager.endTimeChanged(this, totalChange);
            }
        }
    }

    public void finishedDragOperation() {
        if (migrationManager != null) {
            double dx = xArray[0] - beginTimeAtDragStart;
            migrationManager.windowWasMoved(this, dx);
        }
    }

    public boolean isMigratable() {
        return canMigrate;
    }

    public void setMigratable(boolean canMigrate) {
        this.canMigrate = canMigrate;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        if (this.usable == usable) {
            return;
        }
        this.usable = usable;
    }
}
