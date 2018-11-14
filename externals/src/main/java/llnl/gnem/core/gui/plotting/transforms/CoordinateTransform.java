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
package llnl.gnem.core.gui.plotting.transforms;

import llnl.gnem.core.gui.plotting.AxisScale;

public interface CoordinateTransform {

    /**
     * Initialize the CoordinateTransform object given the current dimensions of
     * the axis on which the plot is being rendered. This must be done at the
     * start of rendering when the plot has resized.
     *
     * @param WorldC1Min
     *            The minimum value of the first world coordinate
     * @param WorldC1Max
     *            The maximum value of the first world coordinate
     * @param axisXmin
     *            The minimum (horizontal) pixel of the plot region assuming the
     *            x-axis direction is not reversed.
     * @param axisXwidth
     *            The width of the plot region
     * @param WorldC2Min
     *            The minimum value of the second world coordinate
     * @param WorldC2Max
     *            The maximum value of the second world coordinate
     * @param axisYmin
     *            The minimum (vertical) pixel of the plot region assuming the
     *            y-axis direction is not reversed.
     * @param axisYheight
     *            The height of the plot region
     */
    public void initialize(double WorldC1Min, double WorldC1Max, int axisXmin, int axisXwidth, double WorldC2Min, double WorldC2Max, int axisYmin, int axisYheight);

    /**
     * Populates the plot part of the Coordinate object by applying the
     * transform from World to plot. Assumes the World part of the Coordinate
     * object has been set and the CoordinateTransform object has been
     * initialized.
     *
     * @param v
     *            The Coordinate object with its World values set. After
     *            returning, the Plot values will be set. The World values are
     *            not modified.
     */
    public void WorldToPlot(Coordinate v);

    /**
     * Populates the world part of the Coordinate object by applying the
     * transform from plot to world. Assumes the plot part of the Coordinate
     * object has been set and the CoordinateTransform object has been
     * initialized.
     *
     * @param v
     *            The Coordinate object with its plot values set. After
     *            returning, the world values will be set. The plot values are
     *            not modified.
     */
    public void PlotToWorld(Coordinate v);

    /**
     * determines whether a specific coordinate value is within the current
     * Transform bounds Not all transforms have bounds,and implementations in
     * which bounds are not important should just return false.
     *
     * @param v
     *            The coordinate to be tested
     * @return true if the coordinate is out of bounds.
     */
    public boolean isOutOfBounds(Coordinate v);

    /**
     * Gets the distance in world coordinates between two points
     *
     * @param c1
     *            The first coordinate
     * @param c2
     *            The second coordinate
     * @return The distance
     */
    public double getWorldDistance(Coordinate c1, Coordinate c2);

    public void setXScale(AxisScale scale);

    public void setYScale(AxisScale scale);

    public AxisScale getXScale();

    public AxisScale getYScale();

    public int getWidthPixels();
}
