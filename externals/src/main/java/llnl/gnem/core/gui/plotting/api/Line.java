/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
package llnl.gnem.core.gui.plotting.api;

public interface Line extends PlotObject {

    public double[] getX();

    public PlotObject setX(double[] xVals);

    public double[] getY();

    public PlotObject setY(double[] yVals);

    public double[] getErrorData();

    public PlotObject setErrorData(double[] errorData);

    public double[] getErrorDataMinus();

    public PlotObject setErrorDataMinus(double[] errorDataMinus);

    public boolean getUseHorizontalErrorBars();

    public PlotObject setUseHorizontalErrorBars(boolean useHorizontalErrorBars);

    public String getHoverTemplate();

    public PlotObject setHoverTemplate(String hoverTemplate);

    public double[] getColor();

    public PlotObject setColor(double[] colorVals);

    public LineStyles getStyle();

    public PlotObject setStyle(LineStyles style);

    public HoverModes getHoverMode();

    public PlotObject setHoverMode(HoverModes hoverMode);

    public FillModes getFillMode();

    public PlotObject setFillMode(FillModes fillMode);

    public int getPxThickness();

    public PlotObject setPxThickness(int pxThickness);

    public String getColorMap();

    public PlotObject setColorMap(String colorMap);
}
