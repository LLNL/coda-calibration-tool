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
package llnl.gnem.core.util.Geometry;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class NEZCoordinate {

    private final double xNorthKm;
    private final double yEastKm;
    private final double zDownKm;

    public NEZCoordinate(double xNorthKm, double yEastKm, double zDownKm) {
        this.xNorthKm = xNorthKm;
        this.yEastKm = yEastKm;
        this.zDownKm = zDownKm;
    }

    public NEZCoordinate(Vector3D nezKmVector) {
        xNorthKm = nezKmVector.getX();
        yEastKm = nezKmVector.getY();
        zDownKm = nezKmVector.getZ();
    }

    @Override
    public String toString() {
        return "NEZCoordinate{" + "xNorthKm=" + getxNorthKm() + ", yEastKm=" + getyEastKm() + ", zDownKm=" + getzDownKm() + '}';
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.getxNorthKm()) ^ (Double.doubleToLongBits(this.getxNorthKm()) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.getyEastKm()) ^ (Double.doubleToLongBits(this.getyEastKm()) >>> 32));
        hash = 59 * hash + (int) (Double.doubleToLongBits(this.getzDownKm()) ^ (Double.doubleToLongBits(this.getzDownKm()) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NEZCoordinate other = (NEZCoordinate) obj;
        if (Double.doubleToLongBits(this.getxNorthKm()) != Double.doubleToLongBits(other.getxNorthKm())) {
            return false;
        }
        if (Double.doubleToLongBits(this.getyEastKm()) != Double.doubleToLongBits(other.getyEastKm())) {
            return false;
        }
        if (Double.doubleToLongBits(this.getzDownKm()) != Double.doubleToLongBits(other.getzDownKm())) {
            return false;
        }
        return true;
    }

    /**
     * @return the xNorthKm
     */
    public double getxNorthKm() {
        return xNorthKm;
    }

    /**
     * @return the yEastKm
     */
    public double getyEastKm() {
        return yEastKm;
    }

    /**
     * @return the zDownKm
     */
    public double getzDownKm() {
        return zDownKm;
    }

    public NEZCoordinate subtract(NEZCoordinate other) {
        return new NEZCoordinate(xNorthKm - other.xNorthKm, yEastKm - other.yEastKm, zDownKm - other.zDownKm);
    }

    public double getNorm() {
        return Math.sqrt(xNorthKm * xNorthKm + yEastKm * yEastKm + zDownKm * zDownKm);
    }

    public Vector3D toVector3D() {
        return new Vector3D(xNorthKm, yEastKm, zDownKm);
    }

}
