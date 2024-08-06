/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

/***
 * This plotting event is used to signal when frequency level mode has been
 * changed, when user is setting frequency level
 */

public class PlotFreqLevelChange {
    private static final long serialVersionUID = 1L;

    private boolean lflMode;
    private double x;
    private double xx;
    private double y;
    private double yy;

    public PlotFreqLevelChange(boolean lflMode, double x, double y, double xx, double yy) {
        this.lflMode = lflMode;
        this.x = x;
        this.y = y;
        this.xx = xx;
        this.yy = yy;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public double getX() {
        return x;
    }

    public double getXx() {
        return xx;
    }

    public double getY() {
        return y;
    }

    public double getYy() {
        return yy;
    }

    public void setLflMode(boolean lflMode) {
        this.lflMode = lflMode;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setXx(double xx) {
        this.xx = xx;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setYy(double yy) {
        this.yy = yy;
    }

    public boolean isLflMode() {
        return lflMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lflMode, x, xx, y, yy);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlotFreqLevelChange)) {
            return false;
        }
        PlotFreqLevelChange other = (PlotFreqLevelChange) obj;
        return lflMode == other.lflMode
                && Double.doubleToLongBits(x) == Double.doubleToLongBits(other.x)
                && Double.doubleToLongBits(xx) == Double.doubleToLongBits(other.xx)
                && Double.doubleToLongBits(y) == Double.doubleToLongBits(other.y)
                && Double.doubleToLongBits(yy) == Double.doubleToLongBits(other.yy);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PlotFreqLevelChange [lflMode=").append(lflMode).append(", x=").append(x).append(", xx=").append(xx).append(", y=").append(y).append(", yy=").append(yy).append("]");
        return builder.toString();
    }
}
