/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.Objects;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class DoubleValuedCoordinate {

    private Vector3D coordinate;
    private double value;

    public DoubleValuedCoordinate(double x, double y, double z) {
        this(new Vector3D(x, y, z), 0.0);
    }

    public DoubleValuedCoordinate(double x, double y, double z, double value) {
        this(new Vector3D(x, y, z), value);
    }

    public DoubleValuedCoordinate(Vector3D coordinate) {
        this(coordinate, 0.0);
    }

    public DoubleValuedCoordinate(Vector3D coordinate, Double value) {
        this.coordinate = coordinate;
        this.value = value;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Vector3D getCoordinate() {
        return coordinate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(coordinate, value);
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

        DoubleValuedCoordinate other = (DoubleValuedCoordinate) obj;
        if (!Objects.equals(coordinate, other.coordinate)) {
            return false;
        }

        if (Double.doubleToLongBits(value) != Double.doubleToLongBits(other.value)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DoubleValuedCoordinate [coordinate=").append(coordinate).append(", value=").append(value).append("]");
        return builder.toString();
    }
}