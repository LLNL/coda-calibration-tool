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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.List;

/**
 * User: dodge1 Date: Jul 21, 2005 Time: 3:06:56 PM
 */
public class RegionGenerator {
    private Rectangle bounds;
    private int numSubRegions;
    private List<Point> points;
    private int tolerance = 4;
    private RegionGenerationStyle generationStyle;
    private static final int MAX_POINTS_PER_SEGMENTED_REGION = 20;
    private static final int MIN_POINTS_PER_REGION = 2;

    public RegionGenerator(Rectangle bounds, int numSubRegions, RegionGenerationStyle generationStyle) {
        this.bounds = bounds;
        this.numSubRegions = numSubRegions;
        this.generationStyle = generationStyle;
        points = new ArrayList<Point>();
    }

    public void addPoint(int x, int y) {
        if (bounds == null || !bounds.contains(x, y)) {

        } else
            points.add(new Point(x, y));
    }

    public void setToleranceInPixels(int tolerance) {
        if (tolerance >= 2)
            this.tolerance = tolerance;
    }

    public Area generateRegion(List<Point> linePoints) {
        if (linePoints.size() < 2) {
            return null;
        }

        int minIdx = getMinIdx(linePoints);
        int maxIdx = linePoints.size() - 1;
        if (minIdx >= 0) {
            maxIdx = getMaxIdx(linePoints, minIdx);
        } else
            minIdx = 0;

        fillPointsList(minIdx, maxIdx, linePoints);
        return generateRegion();
    }

    private int getMaxIdx(List<Point> linePoints, int minIdx) {
        int maxIdx = linePoints.size() - 1;
        if (bounds != null && minIdx >= 0)
            maxIdx = getMaxIndex(linePoints, minIdx);
        return maxIdx;
    }

    private int getMinIdx(List<Point> linePoints) {
        int minIdx = 0;
        if (bounds != null) {
            minIdx = getMinIndex(linePoints);
            if (minIdx < 0) { // See if a rectangle including first-last points intersects bounds
                minIdx = adjustBasedOnBoundsText(linePoints, minIdx);
            }
        }
        return minIdx;
    }

    private int adjustBasedOnBoundsText(List<Point> linePoints, int minIdx) {
        Point firstPoint = linePoints.get(0);
        Point lastPoint = linePoints.get(linePoints.size() - 1);
        Rectangle rect = new Rectangle(Math.min(firstPoint.x, lastPoint.x), Math.min(firstPoint.y, lastPoint.y), Math.abs(lastPoint.x - firstPoint.x), Math.abs(firstPoint.y - lastPoint.y));
        if (rect.contains(bounds)) {
            minIdx = 0;
        }
        return minIdx;
    }

    private void fillPointsList(int minIdx, int maxIdx, List<Point> linePoints) {
        points.clear();
        for (int j = minIdx; j <= maxIdx; ++j)
            points.add(linePoints.get(j));
    }

    private int getMaxIndex(List<Point> linePoints, int minIdx) {
        int maxIdx = linePoints.size() - 1;
        for (int j = maxIdx; j > minIdx; --j) {
            if (bounds.contains(linePoints.get(j))) {
                maxIdx = j + 1;
                break;
            }
        }
        maxIdx = Math.min(linePoints.size() - 1, maxIdx);
        return maxIdx;
    }

    private int getMinIndex(List<Point> linePoints) {
        int minIdx = -1;
        for (int j = 0; j < linePoints.size(); ++j) {
            if (bounds.contains(linePoints.get(j))) {
                minIdx = Math.max(0, j - 1);
                break;
            }
        }
        return minIdx;
    }

    public Area generateRegion() {
        int N = points.size();
        if (N < 1)
            return null;

        int pointsPerRegion = getPointsPerSubRegion(N);

        if ((pointsPerRegion == MIN_POINTS_PER_REGION && N <= MAX_POINTS_PER_SEGMENTED_REGION) || generationStyle == RegionGenerationStyle.LINE_SEGMENT) {
            return createMergedLineSegmentsShape(N);
        } else {
            return createMergedRectanglesShape(N, pointsPerRegion);
        }
    }

    private Area createMergedLineSegmentsShape(int n) {
        Area result = new Area();
        int MAX_PTS_PER_SEGMENT = 32;
        int segStart = 0;
        int N = points.size();
        while (true) {
            int pointsPerSegment = MAX_PTS_PER_SEGMENT;
            int segEnd = segStart + pointsPerSegment - 1;
            if (segEnd >= N)
                break;
            while (!segmentContainsIntermediatePoints(segStart, segEnd)) {
                pointsPerSegment /= 2;
                segEnd = segStart + pointsPerSegment - 1;
            }
            Point p1 = points.get(segStart);
            Point p2 = points.get(segEnd - 1);
            addNewSegmentToShape(p2, p1, result);
            segStart = segEnd;
        }

        int i = segStart;
        int N2 = n - 1;
        while (i < N2) {
            Point p1 = points.get(i++);
            Point p2 = points.get(i);
            addNewSegmentToShape(p2, p1, result);
        }
        return result;
    }

    private void addNewSegmentToShape(Point p2, Point p1, Area result) {
        if (p2.x == p1.x) { // create a vertical rectangle
            Rectangle rect = new Rectangle(p1.x - tolerance, Math.min(p1.y, p2.y), 2 * tolerance, Math.abs(p2.y - p1.y));
            result.add(new Area(rect));
            return;
        }

        if (p2.x < p1.x) {
            Point tmp = p1;
            p1 = p2;
            p2 = tmp;
        }
        Shape s = createThickLineSegment(p2, p1);

        result.add(new Area(s));
    }

    boolean segmentContainsIntermediatePoints(int idx1, int idx2) {
        if (idx2 < idx1 + 2)
            return true;
        Point p1 = points.get(idx1);
        Point p2 = points.get(idx2);
        double diffx2x1 = p2.x - p1.x;
        double diffy2y1 = p2.y - p1.y;
        double denominator = Math.sqrt(diffx2x1 * diffx2x1 + diffy2y1 * diffy2y1);
        for (int j = idx1 + 1; j < idx2; ++j) {
            if (distanceToLine(p1, p2, points.get(j), denominator) > tolerance)
                return false;
        }
        return true;
    }

    /**
     * Gets the distance of Point p0 from the line defined by p1 and p2.
     *
     * @param p1
     *            The start point of the line
     * @param p2
     *            The end point of the line
     * @param p0
     *            The point whose distance from the line is to be computed.
     * @return The distance of p0 from the line (p1, p2).
     */
    double distanceToLine(Point p1, Point p2, Point p0, double denominator) {
        double numerator = (p2.x - p1.x) * (p1.y - p0.y) - (p1.x - p0.x) * (p2.y - p1.y);
        return numerator / denominator;
    }

    private Area createMergedRectanglesShape(int n, int pointsPerRegion) {
        Area result = new Area();
        int i = 0;
        Rectangle thisRegion = new Rectangle(points.get(i++));
        int pointsAdded = 0;
        while (i < n) {
            thisRegion.add(points.get(i++));
            pointsAdded++;
            if (pointsAdded == pointsPerRegion) {
                addToResult(thisRegion, result);
                pointsAdded = 0;
                --i;
                thisRegion = new Rectangle(points.get(i));

            }
        }
        addToResult(thisRegion, result);
        return result;
    }

    private Shape createThickLineSegment(Point p2, Point p1) {
        double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
        double segmentLength = getSegmentLength(angle, p2, p1);
        double halfLength = segmentLength / 2;
        int xCenter = (p2.x + p1.x) / 2;
        int yCenter = (p2.y + p1.y) / 2;
        return getRotatedRectangle(xCenter, halfLength, yCenter, segmentLength, angle);
    }

    private Shape getRotatedRectangle(int xCenter, double halfLength, int yCenter, double segmentLength, double angle) {
        Rectangle rect = new Rectangle((int) (xCenter - halfLength), yCenter - tolerance, (int) segmentLength, 2 * tolerance);
        AffineTransform rotation = AffineTransform.getRotateInstance(angle, xCenter, yCenter);
        return rotation.createTransformedShape(rect);
    }

    private double getSegmentLength(double angle, Point p2, Point p1) {
        double cosAngle = Math.abs(Math.cos(angle));
        double dx = p2.x - p1.x;
        double dy = Math.abs(p2.y - p1.y);
        if (cosAngle > 0)
            dy = dx / cosAngle;
        dy += tolerance;
        return dy;
    }

    private int getPointsPerSubRegion(int n) {
        if (numSubRegions > n)
            numSubRegions = n;

        int pointsPerRegion = n / numSubRegions;
        if (pointsPerRegion < 2) {
            pointsPerRegion = 2;
            numSubRegions = n / pointsPerRegion;
        }
        return pointsPerRegion;
    }

    private void addToResult(Rectangle thisRegion, Area result) {

        int TOL2 = 2 * tolerance;
        if (thisRegion.width < tolerance || thisRegion.height < tolerance) {
            int left = (int) thisRegion.getX();
            int top = (int) thisRegion.getY();
            thisRegion.setBounds(left - tolerance, top - tolerance, thisRegion.width + TOL2, thisRegion.height + TOL2);
        }
        result.add(new Area(thisRegion));
    }
}
