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
package llnl.gnem.core.gui.plotting;

import llnl.gnem.core.util.Utility;

/**
 * A class that maps Y-values back and forth between real-world values and pixel
 * values. Used for data point conversions, not for positioning elements of the
 * axis.
 *
 * @author Doug Dodge
 */
public class YValueMapper {
    /**
     * Constructor for the YValueMapper object
     */
    public YValueMapper() {
        theYaxisDir = YaxisDir.UP;
        YAxisScale = AxisScale.LINEAR;
    }

    /**
     * Gets the direction of this YAxis (UP or DOWN)
     *
     * @return The axis direction value
     */
    public YaxisDir getYAxisDir() {
        return theYaxisDir;
    }

    /**
     * Sets the direction of this YAxis (UP or DOWN)
     *
     * @param d
     *            The new axis direction value
     */
    public void setYAxisDir(YaxisDir d) {
        theYaxisDir = d;
    }

    /**
     * Gets the Scale type of this axis (LOG or LINEAR).
     *
     * @return The Scale value
     */
    public AxisScale getYScale() {
        return YAxisScale;
    }

    /**
     * Sets the Scale type of this axis (LOG or LINEAR).
     *
     * @param s
     *            The new Scale value
     */
    public void setYScale(AxisScale s) {
        YAxisScale = s;
    }

    /**
     * Method called before rendering of the subplot to set up-to-date scaling
     * information for conversion between real-world and user-space coordinates.
     *
     * @param ymin
     *            Real-world minimum of the axis
     * @param ymax
     *            Real-world maximum of the axis
     * @param top
     *            Top of the axis in pixels
     * @param height
     *            Height of the axis in pixels
     */
    public void Initialize(double ymin, double ymax, double top, double height) {
        if (YAxisScale == AxisScale.LOG) {
            if (ymax <= 0)
                throw new IllegalArgumentException("Ymax is <= 0 and YAxisScale is logarithmic!");
            if (ymin <= 0)
                ymin = ymax / 100000;
        }
        Ymin = YAxisScale == AxisScale.LINEAR ? ymin : Utility.log10(ymin);
        Ymax = YAxisScale == AxisScale.LINEAR ? ymax : Utility.log10(ymax);
        Top = top;
        Height = height;
        double yrange = Ymax - Ymin;
        YpixelsPerDataUnit = yrange != 0 ? Height / yrange : 32767.0;
        if (theYaxisDir == YaxisDir.DOWN) {
            factor1 = Top - Ymin * YpixelsPerDataUnit;
            factor2 = Ymin - Top / YpixelsPerDataUnit;
        } else {
            factor1 = Top + Height + Ymin * YpixelsPerDataUnit;
            factor2 = Ymin + (Height + Top) / YpixelsPerDataUnit;
            YpixelsPerDataUnit *= -1;
        }
    }

    /**
     * Gets the user-space (pixel) value corresponding to a real-world Y-value.
     *
     * @param Yval
     *            The real-world y-value
     * @return The y pixel value
     */
    public double getYpixel(double Yval) {
        if (YAxisScale == AxisScale.LOG) {
            if (Yval > 0)
                Yval = Utility.log10(Yval);
            else
                Yval = Ymin;
        }
        return factor1 + Yval * YpixelsPerDataUnit;
    }

    /**
     * Gets the real-world y-value corresponding to a user-space (pixel) value
     *
     * @param Ypixel
     *            The user-space y-value
     * @return The real-world y-value
     */
    public double getYvalue(double Ypixel) {
        double result = factor2 + Ypixel / YpixelsPerDataUnit;
        return YAxisScale == AxisScale.LINEAR ? result : Math.pow(10.0, result);
    }

    public AxisScale getAxisScale() {
        return YAxisScale;
    }

    private double Ymin;
    private double Ymax;
    private double YpixelsPerDataUnit;
    private YaxisDir theYaxisDir;
    private AxisScale YAxisScale;
    private double Top;
    private double Height;
    private double factor1;
    private double factor2;
}
