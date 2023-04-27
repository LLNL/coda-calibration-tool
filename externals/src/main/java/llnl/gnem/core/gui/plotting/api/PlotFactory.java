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

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis.Type;

public interface PlotFactory {

    public BasicPlot basicPlot();

    public BasicPlot lineAndMarkerScatterPlot();

    public Axis axis(Type axisType, String label);

    public Line line(double[] xVals, double[] yVals, Color color, LineStyles style, int pxThickness);

    public Line lineX(String label, double startingX, double xIncrement, float[] xData, Color color, LineStyles style, int pxThickness);

    public VerticalLine verticalLine(double x, double yRatio, String label);

    public Rectangle rectangle(final double x1, final double x2, final double yRatio, final String label, final Color color);

    public Symbol createSymbol(SymbolStyles style, String name, double x, double y, Color color, Color edgeColor, Color textColor, String text, boolean textVisible);

}
