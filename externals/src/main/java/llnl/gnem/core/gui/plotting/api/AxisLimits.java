/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package llnl.gnem.core.gui.plotting.api;

import java.util.Objects;

import llnl.gnem.core.gui.plotting.api.Axis.Type;

public class AxisLimits {

    private Type axisType;
    private double min;
    private double max;

    public AxisLimits(final Type axisType, final double min, final double max) {
        this.axisType = axisType;
        this.min = min;
        this.max = max;
    }

    public Type getAxis() {
        return axisType;
    }

    public void setAxis(final Type axis) {
        this.axisType = axis;
    }

    public double getMin() {
        return min;
    }

    public void setMin(final double min) {
        this.min = min;
    }

    public double getMax() {
        return max;
    }

    public void setMax(final double max) {
        this.max = max;
    }

    @Override
    public int hashCode() {
        return Objects.hash(axisType, max, min);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AxisLimits)) {
            return false;
        }
        final AxisLimits other = (AxisLimits) obj;
        return axisType == other.axisType && Double.doubleToLongBits(max) == Double.doubleToLongBits(other.max) && Double.doubleToLongBits(min) == Double.doubleToLongBits(other.min);
    }

    @Override
    public String toString() {
        return "AxisLimits [axis=" + axisType + ", min=" + min + ", max=" + max + "]";
    }

}
