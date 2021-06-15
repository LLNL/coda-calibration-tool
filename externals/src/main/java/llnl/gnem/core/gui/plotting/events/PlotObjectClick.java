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

import java.util.List;

import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;

/**
 * A class that holds a MouseEvent and a 2D coordinate in plot space. Used by to
 * pass information about mouse button events involving PlotObjects to
 * listeners.
 */
public class PlotObjectClick {
    private MouseEvent me;
    private List<Point2D> points;

    public PlotObjectClick(MouseEvent me, List<Point2D> points) {
        this.setMouseEvent(me);
        this.points = points;
    }

    public MouseEvent getMouseEvent() {
        return me;
    }

    public PlotObjectClick setMouseEvent(MouseEvent me) {
        this.me = me;
        return this;
    }

    public List<Point2D> getPlotPoints() {
        return points;
    }

    public PlotObjectClick setPlotPoint(List<Point2D> dataPoints) {
        this.points = dataPoints;
        return this;
    }
}
