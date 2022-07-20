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

import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.util.PairT;

/***
 * This plotting event is used to signal when a plot's zoom level has changed.
 * It can be created when the user does a rectangle select of a smaller portion
 * of a waveform plot to zoom in. The axes are changed and the change is passed
 * as this event.
 */

public class PlotAxisChange {
    private static final long serialVersionUID = 1L;

    private final boolean reset;
    private final PairT<AxisLimits, AxisLimits> axisRange;

    public PlotAxisChange(final boolean reset, final double xMin, final double xMax, final double yMin, final double yMax) {
        this.reset = reset;
        this.axisRange = new PairT<>(new AxisLimits(Axis.Type.X, xMin, xMax), new AxisLimits(Axis.Type.Y, yMin, yMax));
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    /***
     *
     * @return True if the plot zoom level was restored to default, otherwise
     *         false if the zoom was a specific region in the plot
     */
    public boolean isReset() {
        return reset;
    }

    public PairT<AxisLimits, AxisLimits> getAxisLimits() {
        return axisRange;
    }

    @Override
    public int hashCode() {
        return Objects.hash(axisRange, reset);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PlotAxisChange)) {
            return false;
        }
        PlotAxisChange other = (PlotAxisChange) obj;
        return Objects.equals(axisRange, other.axisRange) && reset == other.reset;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PlotAxisChange [reset=");
        builder.append(reset);
        builder.append(", axisRange=");
        builder.append(axisRange);
        builder.append("]");
        return builder.toString();
    }

}
