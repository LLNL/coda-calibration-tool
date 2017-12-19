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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import llnl.gnem.core.gui.plotting.Limits;

/**
 * Created by: dodge1
 * Date: Jan 27, 2005
 */
public class PanMagnifierManager {
    private static double LOG_MAX_MAGNIFICATION = 2.6;
    private static double LOG_SCALE_FACTOR = 2.0;
    private int windowTop;
    private int startY;
    private int startOffset;
    private double yStart;

    private double logMagnificationFactor;
    private double dataRange;


    public PanMagnifierManager( int windowTop,
                                int windowHeight,
                                int startY,
                                double startYMin,
                                double startYMax,
                                double yStart )
    {
        this.windowTop = windowTop;
        this.startY = startY;
        this.startOffset = startY - windowTop;
        this.yStart = yStart;

        logMagnificationFactor = LOG_MAX_MAGNIFICATION / windowHeight;
        dataRange = startYMax - startYMin;
    }

    public Limits getCurrentYLimits( int currentYPixelValue )
    {
        int currentOffset = currentYPixelValue - windowTop;
        int deviation = startOffset - currentOffset;
        double magnification = Math.pow( 10.0, logMagnificationFactor * deviation );
        double newRange = dataRange / magnification;
        double yMin = yStart - newRange / 2;
        double yMax = yStart + newRange / 2;
        return new Limits( yMin, yMax );
    }

    public double getMagnification( int currentYPixelValue )
    {
        double logMagnification = 1.0;
        double denominator = ( startY - windowTop );
        if( denominator != 0 )
            logMagnification = LOG_SCALE_FACTOR * ( startY - currentYPixelValue ) / denominator;
        if( logMagnification < -LOG_SCALE_FACTOR )
            logMagnification = -LOG_SCALE_FACTOR;
        return Math.pow( 10.0, logMagnification );
    }
}
