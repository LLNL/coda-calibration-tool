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
package llnl.gnem.core.gui.swing.plotting;

/**
 * A class that maps X-values back and forth between real-world values and pixel
 * values. Used for data point conversions, not for positioning elements of the
 * axis.
 *
 * @author Doug Dodge
 */
public class XValueMapper {
    /**
     * Constructor for the XValueMapper object
     */
    public XValueMapper() {
        theXaxisDir = XaxisDir.RIGHT;
        XAxisScale = AxisScale.LINEAR;
    }

    /**
     * Gets the X-axis direction
     *
     * @return The X-axis direction
     */
    public XaxisDir getXAxisDir() {
        return theXaxisDir;
    }

    /**
     * Sets the X-axis direction
     *
     * @param d
     *            The X-axis direction
     */
    public void setXAxisDir(final XaxisDir d) {
        theXaxisDir = d;
    }

    /**
     * Gets the scaling ( LINEAR or LOG )
     *
     * @return The Scale value
     */
    public AxisScale getXScale() {
        return XAxisScale;
    }

    /**
     * Sets the scaling ( LINEAR or LOG )
     *
     * @param s
     *            The Scale value
     */
    public void setXScale(final AxisScale s) {
        XAxisScale = s;
    }

    /**
     * Method called just prior to rendering that is used to set the current
     * scaling between data values and pixel values.
     *
     * @param xmin
     *            Minimum data value of axis
     * @param xmax
     *            Maximum data value of axis
     * @param left
     *            Left edge of axis in pixels
     * @param width
     *            Width of axis in pixels
     */
    public void Initialize(double xmin, final double xmax, final int left, final int width) {
        if (XAxisScale == AxisScale.LOG) {
            if (xmax <= 0) {
                throw new IllegalArgumentException("Xmax is <= 0 and XAxisScale is logarithmic!");
            }
            if (xmin <= 0) {
                xmin = xmax / 100;
            }
        }
        Xmin = XAxisScale == AxisScale.LINEAR ? xmin : Math.log10(xmin);
        Xmax = XAxisScale == AxisScale.LINEAR ? xmax : Math.log10(xmax);
        Left = left;
        Width = width;
        final double xrange = Xmax - Xmin;
        XpixelsPerDataUnit = xrange != 0 ? Width / xrange : MaxPixel;
        if (theXaxisDir == XaxisDir.RIGHT) {
            factor1 = Left - Xmin * XpixelsPerDataUnit;
            factor2 = Xmin - Left / XpixelsPerDataUnit;
        } else {
            factor1 = Left + Width + Xmin * XpixelsPerDataUnit;
            factor2 = Xmin + (Width + Left) / XpixelsPerDataUnit;
            XpixelsPerDataUnit *= -1;
        }
    }
    //---------------------------------------------------------------------------

    /**
     * Gets the X-value in user-space (pixels) for the given real-world X-value
     *
     * @param Xval
     *            Real-world X-value
     * @return The x pixel value
     */
    public double getXpixel(double Xval) {
        if (XAxisScale == AxisScale.LOG) {
            if (Xval > 0) {
                Xval = Math.log10(Xval);
            } else {
                Xval = Xmin;
            }
        }
        return factor1 + Xval * XpixelsPerDataUnit;
    }
    //---------------------------------------------------------------------------

    /**
     * Gets the real-world X-value corresponding to the supplied user-space
     * (pixel) value.
     *
     * @param Xpixel
     *            User-space value
     * @return Real-world X-value
     */
    public double getXvalue(final double Xpixel) {
        final double result = factor2 + Xpixel / XpixelsPerDataUnit;
        return XAxisScale == AxisScale.LINEAR ? result : Math.pow(10.0, result);
    }
    //---------------------------------------------------------------------------

    public AxisScale getAxisScale() {
        return XAxisScale;
    }

    public int getWidth() {
        return Width;
    }

    private double Xmin;
    private double Xmax;
    private double XpixelsPerDataUnit;
    private XaxisDir theXaxisDir;
    private AxisScale XAxisScale;
    private double Left;
    private int Width;
    private double factor1;
    private double factor2;
    private final static int MaxPixel = 32766;
}
