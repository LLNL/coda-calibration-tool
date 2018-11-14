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
package llnl.gnem.core.polygon;

import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import llnl.gnem.core.util.Geometry.NEZCoordinate;

public class CartesianPolygon {
    private final ArrayList<Vector3D> vertices;

    public CartesianPolygon(ArrayList<NEZCoordinate> v) {
        vertices = new ArrayList<>();
        for (NEZCoordinate coord : v) {
            vertices.add(coord.toVector3D());
        }
    }

    public void scale(double factor) {
        for (Vector3D v3d : vertices) {
            v3d.scalarMultiply(factor);
        }
    }

    public boolean contains(Vector3D tLocal) {
        // build the test line ...
        line testline = new line(tLocal, new Vector3D(Double.MAX_VALUE, tLocal.getY(), tLocal.getZ()));
        int N = vertices.size();
        int i = 0;
        int count = 0;
        double yval = tLocal.getY();
        line edge;
        while (i < N) {
            int next = i < N - 1 ? i + 1 : 0;
            Vector3D vi = vertices.get(i);
            Vector3D vNext = vertices.get(next);
            if (vi.equals(tLocal)) {
                return true;
            }
            if (vi.getX() < vNext.getX()) {
                edge = new line(vi, vNext);
            } else {
                edge = new line(vNext, vi);
            }
            if (intersect(edge, testline)) {
                ++count;
                if (edge.p2.getY() == yval || edge.p1.getY() == yval) // If the intersection is on a vertex don't
                {
                    ++i;
                }
                // process next edge or intersection gets counted twice.
            }
            ++i;
        }
        return (count % 2 == 1);
    }

    // ---------------------------------------------------------------------------
    public boolean contains(double x, double y) {
        return contains(new Vector3D(x, y, 0));
    }

    /*
     *  Given 3 Vertices p0, p1, p2 traveling from the 1st to the 2nd to the 3rd, are
     *  we going counter clockwise or clockwise?
     *  1  => ccw
     *  -1 => cw
     *  0  => Vertices colinear and p2 between p0 and p1
     */
    private int ccw(Vector3D p0, Vector3D p1, Vector3D p2) {
        double dx1;
        double dx2;
        double dy1;
        double dy2;
        dx1 = p1.getX() - p0.getX();
        dy1 = p1.getY() - p0.getY();
        dx2 = p2.getX() - p0.getX();
        dy2 = p2.getY() - p0.getY();
        if (dx1 * dy2 > dy1 * dx2) {
            return 1;
        }
        if (dx1 * dy2 < dy1 * dx2) {
            return -1;
        }
        if ((dx1 * dy2 < 0) || (dy1 * dy2 < 0)) {
            return -1;
        }
        if ((dx1 * dx1 + dy1 * dy1) < (dx2 * dx2 + dy2 * dy2)) {
            return 1;
        }
        return 0;
    }

    private boolean intersect(line l1, line l2) {
        double ccw11 = ccw(l1.p1, l1.p2, l2.p1);
        double ccw12 = ccw(l1.p1, l1.p2, l2.p2);
        double ccw21 = ccw(l2.p1, l2.p2, l1.p1);
        double ccw22 = ccw(l2.p1, l2.p2, l1.p2);
        return (ccw11 * ccw12 <= 0 && ccw21 * ccw22 <= 0);
    }

    public ArrayList<Vector3D> getVertices() {
        return new ArrayList<Vector3D>(vertices);
    }
}
