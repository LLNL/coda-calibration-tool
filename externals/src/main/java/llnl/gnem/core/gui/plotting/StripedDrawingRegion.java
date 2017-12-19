/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.gui.plotting;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

import llnl.gnem.core.gui.plotting.jmultiaxisplot.YAxis;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;

/**
 *
 * @author addair1
 */
public class StripedDrawingRegion extends DrawingRegion {

    private final YAxis axis;

    public StripedDrawingRegion(YAxis axis) {
        this.axis = axis;
    }

    @Override
    public void render(Graphics g) {
        super.render(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(getOffsetColor(7));

        CoordinateTransform ct = axis.getCoordinateTransform();

        Rectangle box = getRect();
        TickMetrics ticks = axis.getTickMetrics(box.height);
        boolean skip = true;
        int last = 0;
        boolean first = true;
        while (ticks.hasNext()) {
            double value = ticks.getNext();
            Coordinate c = new Coordinate(0.0, 0.0, 0.0, value);
            ct.WorldToPlot(c);
            int y = (int) c.getY();

            if (first) {
                first = false;
            } else {
                if (skip) {
                    last = y;
                } else {
                    g2d.fillRect(box.x, y, box.width, last - y);
                }
                skip = !skip;
            }
        }

        drawBox(g);
    }
}
