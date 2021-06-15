/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.gui.plotting.events;

import java.util.Objects;

public class PlotShapeMove {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final double x0;
    private final double x1;
    private final double y0;
    private final double y1;

    public PlotShapeMove(final String name, final double x0, final double x1, final double y0, final double y1) {
        this.name = name;
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public String getName() {
        return name;
    }

    public double getX0() {
        return x0;
    }

    public double getX1() {
        return x1;
    }

    public double getY0() {
        return y0;
    }

    public double getY1() {
        return y1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x0, x1, y0, y1);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlotShapeMove)) {
            return false;
        }
        PlotShapeMove other = (PlotShapeMove) obj;
        return Objects.equals(name, other.name)
                && Double.doubleToLongBits(x0) == Double.doubleToLongBits(other.x0)
                && Double.doubleToLongBits(x1) == Double.doubleToLongBits(other.x1)
                && Double.doubleToLongBits(y0) == Double.doubleToLongBits(other.y0)
                && Double.doubleToLongBits(y1) == Double.doubleToLongBits(other.y1);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PlotShapeMove [name=").append(name).append(", x0=").append(x0).append(", x1=").append(x1).append(", y0=").append(y0).append(", y1=").append(y1).append("]");
        return builder.toString();
    }

}
