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
package llnl.gnem.core.gui.plotting.transforms;


import llnl.gnem.core.gui.plotting.AxisScale;
import llnl.gnem.core.gui.plotting.XValueMapper;
import llnl.gnem.core.gui.plotting.XaxisDir;
import llnl.gnem.core.gui.plotting.YValueMapper;
import llnl.gnem.core.gui.plotting.YaxisDir;


public class CartesianTransform implements CoordinateTransform {

    public CartesianTransform()
    {
        xmapper = new XValueMapper();
        ymapper = new YValueMapper();
    }


    /**
     * Initialize the CoordinateTransform object given the current dimensions of the axis
     * on which the plot is being rendered.  This must be done at the start of rendering when
     * the plot has resized.
     *
     * @param WorldC1Min  The minimum value of the first world coordinate
     * @param WorldC1Max  The maximum value of the first world coordinate
     * @param axisXmin    The minimum (horizontal) pixel of the plot region
     *                    assuming the x-axis direction is not reversed.
     * @param axisXwidth  The width of the plot region
     * @param WorldC2Min  The minimum value of the second world coordinate
     * @param WorldC2Max  The maximum value of the second world coordinate
     * @param axisYmin    The minimum (vertical) pixel of the plot region
     *                    assuming the y-axis direction is not reversed.
     * @param axisYheight The height of the plot region
     */
    @Override
    public void initialize( double WorldC1Min, double WorldC1Max, int axisXmin, int axisXwidth,
                            double WorldC2Min, double WorldC2Max, int axisYmin, int axisYheight )
    {
        xmapper.Initialize( WorldC1Min, WorldC1Max, axisXmin, axisXwidth );
        ymapper.Initialize( WorldC2Min, WorldC2Max, axisYmin, axisYheight );

    }


    /**
     * Populates the plot part of the Coordinate object by applying the transform
     * from World to plot. Assumes the World part of the Coordinate object has been set
     * and the CartesionTransform object has been initialized.
     *
     * @param v The Coordinate object with its World values set. After returning, the
     *          Plot values will be set. The World values are not modified.
     */
    @Override
    public void WorldToPlot( Coordinate v )
    {
        v.setX( xmapper.getXpixel( v.getWorldC1() ) );
        v.setY( ymapper.getYpixel( v.getWorldC2() ) );

    }

    /**
     * Populates the world part of the Coordinate object by applying the transform
     * from plot to world. Assumes the plot part of the Coordinate object has been set
     * and the CartesionTransform object has been initialized.
     *
     * @param v The Coordinate object with its plot values set. After returning, the
     *          world values will be set. The plot values are not modified.
     */
    @Override
    public void PlotToWorld( Coordinate v )
    {
        v.setWorldC1( xmapper.getXvalue( v.getX() ) );
        v.setWorldC2( ymapper.getYvalue( v.getY() ) );

    }

    @Override
    public void setXScale( AxisScale scale )
    {
        xmapper.setXScale( scale );
    }

    @Override
    public void setYScale( AxisScale scale )
    {
        ymapper.setYScale( scale );
    }

    @Override
    public AxisScale getXScale()
    {
        return xmapper.getXScale();
    }

    @Override
    public AxisScale getYScale()
    {
        return ymapper.getYScale();
    }

    /**
     * Gets the X-axis direction
     *
     * @return The X-axis direction
     */
    public XaxisDir getXAxisDir()
    {
        return xmapper.getXAxisDir();
    }

    /**
     * Sets the X-axis direction
     *
     * @param d The X-axis direction
     */
    public void setXAxisDir( XaxisDir d )
    {
        xmapper.setXAxisDir( d );
    }


    /**
     * Gets the direction of this YAxis (UP or DOWN)
     *
     * @return The axis direction value
     */
    public YaxisDir getYAxisDir()
    {
        return ymapper.getYAxisDir();
    }

    /**
     * Sets the direction of this YAxis (UP or DOWN)
     *
     * @param d The new axis direction value
     */
    public void setYAxisDir( YaxisDir d )
    {
        ymapper.setYAxisDir( d );
    }

    @Override
    public double getWorldDistance( Coordinate c1, Coordinate c2 )
    {
        double X1 = c1.getWorldC1();
        double Y1 = c1.getWorldC2();
        double X2 = c2.getWorldC1();
        double Y2 = c2.getWorldC2();
        double dx = X2 - X1;
        double dy = Y2 - Y1;
        return Math.sqrt( dx * dx + dy * dy );
    }

    /**
     * Determine whether a point is in range for the purpose of plotting
     *
     * @param v A Coordinate object containing the point to be tested.
     * @return Returns true if the supplied coordinate has negative
     *         values and the scale type is LOG
     */
    @Override
    public boolean isOutOfBounds( Coordinate v )
    {
        double X = v.getWorldC1();
        double Y = v.getWorldC2();

        if( X <= 0 && getXScale() == AxisScale.LOG )
            return true;
        if( Y <= 0 && getYScale() == AxisScale.LOG )
            return true;
        return false;
    }
    
    @Override
    public int getWidthPixels() {
        return xmapper.getWidth();
    }


    private XValueMapper xmapper;
    private YValueMapper ymapper;


}
