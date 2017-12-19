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

/**
 * Translate between real-world coordinates (mm) and pixels for placement of
 * axis components. (Not used for translation of plot objects( lines, points,
 * etc ). That translation is done by the ValueMapper class. This class will
 * get constructed using the Graphics context of the device on which the axis
 * will be rendered. However, Any specific rendering of a plot could be on an
 * arbitrary device. Therefore, the setCanvas method must be called by JSubPlot at
 * the start of each rendering.
 *
 * @author Doug Dodge
 */
public class DrawingUnits {

    /**
     * Gets the horizontal pixel equivalent of a value in millimeters for this
     * graphics context
     *
     * @param u Input value in millimeters
     * @return The equivalent value in pixels
     */
    public int getHorizUnitsToPixels( double u )
    {
        return (int) ( u * PixelsPerHorizUnit );
    }

    /**
     * Gets the vertical pixel equivalent of a value in millimeters for this
     * graphics context
     *
     * @param u Input value in millimeters
     * @return The equivalent value in pixels
     */
    public int getVertUnitsToPixels( double u )
    {
        return (int) ( u * PixelsPerVertUnit );
    }

    /**
     * Gets the horizontal millimeters equivalent of a value in pixelsfor this
     * graphics context
     *
     * @param u Input value in pixels
     * @return The equivalent value in millimeters
     */
    public double getHorizPixelsToUnits( int u )
    {
        return u / PixelsPerHorizUnit;
    }

    /**
     * Gets the vertical millimeters equivalent of a value in pixelsfor this
     * graphics context
     *
     * @param u Input value in pixels
     * @return The equivalent value in millimeters
     */
    public double getVertPixelsToUnits( int u )
    {
        return u / PixelsPerVertUnit;
    }

    /**
     * Gets the average pixel equivalent of one millimeter for this graphics
     * context
     *
     * @return The equivalent value in pixels
     */
    public double getPixelsPerUnit()
    {
        return ( PixelsPerHorizUnit + PixelsPerVertUnit ) / 2;
    }

    private final double PixelsPerHorizUnit = 72.0 / 25.4;
    // Units are assumed to be millimeters
    private final double PixelsPerVertUnit = 72.0 / 25.4;
    // Units are assumed to be millimeters
}

