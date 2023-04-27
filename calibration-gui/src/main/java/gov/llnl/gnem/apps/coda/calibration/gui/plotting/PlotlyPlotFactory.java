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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import org.springframework.stereotype.Service;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.Type;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.Rectangle;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import llnl.gnem.core.gui.plotting.plotly.BasicLine;
import llnl.gnem.core.gui.plotting.plotly.BasicSymbol;
import llnl.gnem.core.gui.plotting.plotly.BasicTitle;
import llnl.gnem.core.gui.plotting.plotly.PlotlyPlot;
import llnl.gnem.core.gui.plotting.plotly.PlotData;
import llnl.gnem.core.gui.plotting.plotly.PlotTrace;

@Service
public class PlotlyPlotFactory implements PlotFactory {

    @Override
    public BasicPlot basicPlot() {
        return new PlotlyPlot();
    }

    @Override
    public BasicPlot lineAndMarkerScatterPlot() {
        return new PlotlyPlot(false, new PlotData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER_AND_LINE), Color.WHITE, new BasicTitle()));
    }

    @Override
    public Axis axis(final Type axisType, final String label) {
        return new BasicAxis(axisType, label);
    }

    @Override
    public Line line(final double[] xVals, final double[] yVals, final Color color, final LineStyles style, final int pxThickness) {
        return new BasicLine(xVals, yVals, color, style, pxThickness);
    }

    @Override
    public Line lineX(String label, double startingX, double xIncrement, float[] xData, Color color, LineStyles style, int pxThickness) {
        return new BasicLine(label, startingX, xIncrement, xData, color, style, pxThickness);
    }

    @Override
    public VerticalLine verticalLine(final double x, final double yRatio, final String label) {
        return new VerticalLine(x, yRatio, label);
    }

    @Override
    public Rectangle rectangle(final double x1, final double x2, final double yRatio, final String label, final Color color) {
        return new Rectangle(x1, x2, yRatio, label, color);
    }

    @Override
    public Symbol createSymbol(final SymbolStyles style, final String name, final double x, final double y, final Color color, final Color edgeColor, final Color textColor, final String text,
            final boolean textVisible) {
        return new BasicSymbol(style, name, x, y, color, edgeColor, textColor, text, textVisible);
    }

}
