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

public class PlotMouseEvent {
    private static final long serialVersionUID = 1L;
    private final double clientX;
    private final double clientY;
    private final double plotX;
    private final double plotY;

    public PlotMouseEvent(final double plotX, final double plotY, final double clientX, final double clientY) {
        this.plotX = plotX;
        this.plotY = plotY;
        this.clientX = clientX;
        this.clientY = clientY;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public double getPlotX() {
        return plotX;
    }

    public double getPlotY() {
        return plotY;
    }

    public double getClientX() {
        return clientX;
    }

    public double getClientY() {
        return clientY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientX, clientY, plotX, plotY);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlotMouseEvent)) {
            return false;
        }
        PlotMouseEvent other = (PlotMouseEvent) obj;
        return Double.doubleToLongBits(clientX) == Double.doubleToLongBits(other.clientX)
                && Double.doubleToLongBits(clientY) == Double.doubleToLongBits(other.clientY)
                && Double.doubleToLongBits(plotX) == Double.doubleToLongBits(other.plotX)
                && Double.doubleToLongBits(plotY) == Double.doubleToLongBits(other.plotY);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PlotMouseEvent [clientX=").append(clientX).append(", clientY=").append(clientY).append(", plotX=").append(plotX).append(", plotY=").append(plotY).append("]");
        return builder.toString();
    }
}
