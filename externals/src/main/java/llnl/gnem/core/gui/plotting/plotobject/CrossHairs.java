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
package llnl.gnem.core.gui.plotting.plotobject;

import java.awt.Color;
import java.awt.Graphics;

import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;

/**
 *
 * @author dodge1
 */
public class CrossHairs extends PlotObject {
    private final Line horizLine;
    private final Line vertLine;
    private static final int npts = 10;

    public CrossHairs(double xmin, double xmax, double ymin, double ymax, double xval, double yval, Color color, PenStyle penStyle) {
        horizLine = addHorizontalLine(xmin, xmax, yval, npts, color, penStyle);
        vertLine = addVerticalLine(xval, ymin, ymax, npts, color, penStyle);
    }

    @Override
    public void render(Graphics g, JBasicPlot owner) {
        horizLine.render(g, owner);
        vertLine.render(g, owner);
    }

    @Override
    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {
        horizLine.ChangePosition(owner, graphics, dx, dy);
        vertLine.ChangePosition(owner, graphics, dx, dy);
    }

    private static Line addHorizontalLine(double xmin, double xmax, double yval, int npts, Color c, PenStyle style) {
        float dx = (float) (xmax - xmin) / (npts - 1);
        float[] x = new float[npts];
        float[] y = new float[npts];
        for (int j = 0; j < npts; ++j) {
            x[j] = (float) xmin + dx * j;
            y[j] = (float) yval;
        }
        return new Line(x, y, c, PaintMode.COPY, style, 1);
    }

    private static Line addVerticalLine(double xval, double ymin, double ymax, int npts, Color c, PenStyle style) {
        float dy = (float) (ymax - ymin) / (npts - 1);
        float[] x = new float[npts];
        float[] y = new float[npts];
        for (int j = 0; j < npts; ++j) {
            x[j] = (float) xval;
            y[j] = (float) ymin + dy * j;
        }
        return new Line(x, y, c, PaintMode.COPY, style, 1);
    }

}
