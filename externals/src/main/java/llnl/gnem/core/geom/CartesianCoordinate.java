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
package llnl.gnem.core.geom;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author addair1
 */
public class CartesianCoordinate implements Coordinate<CartesianCoordinate> {

    private Vector3D point;

    /**
     * Limited visibility setter specifically added for the ECEFCoordinate class
     * which must first perform a conversion before setting the point member
     *
     * @param point
     */
    protected void setPoint(Vector3D point) {
        this.point = point;
    }

    public CartesianCoordinate(double x, double y) {
        this(x, y, 0.0);
    }

    public CartesianCoordinate(double x, double y, double z) {
        point = new Vector3D(x, y, z);
    }

    @Override
    public double getDistance(CartesianCoordinate other) {
        return point.distance(other.point);
    }

    @Override
    public double getElevation() {
        return getZ();
    }

    /**
     * Returns the coordinate as an array
     *
     * @return a double array of [x,y,z]
     */
    public double[] getArray() {
        if (point == null) {
            return null;
        }
        return point.toArray();
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f)", getX(), getY(), getZ());
    }

    public Vector3D getPoint() {
        return point;
    }

    public double getX() {
        return point.getX();
    }

    public double getY() {
        return point.getY();
    }

    public double getZ() {
        return point.getZ();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CartesianCoordinate other = (CartesianCoordinate) obj;
        if (this.point != other.point && (this.point == null || !this.point.equals(other.point))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 47 * hash + (this.point != null ? this.point.hashCode() : 0);
        return hash;
    }
}
