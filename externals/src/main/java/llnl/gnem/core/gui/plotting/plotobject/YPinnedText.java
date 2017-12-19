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
package llnl.gnem.core.gui.plotting.plotobject;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.VertAlignment;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;


/**
 * Text that is pinned in the vertical direction a fixed amount from one of
 * the plot's edges. In the horizontal direction, the text is fixed to a data
 * value. The effect is that upon zooming, the text stays the same distance
 * from the edge (vertically) but floats to the new position of the X-value.
 *
 * @author Doug Dodge
 */
public class YPinnedText extends BasicText {
    /**
     * Constructor for the YPinnedText object
     *
     * @param x        The X-data value (real-world) of the text
     * @param y        The vertical offset in mm from the plot edge
     * @param text     The text string
     * @param vp       The plot edge to pin to ( TOP, BOTTOM )
     * @param fontName The name of the font used to render the text
     * @param fontSize The size of the font used to render the text
     * @param textC    The color of the text
     * @param hAlign   The horizontal alignment of the text relative to the pin
     *                 point.
     * @param vAlign   The vertical alignment of the text relative to the pin
     *                 point.
     */
    public YPinnedText( double x, double y, String text, VertPinEdge vp, String fontName, double fontSize, Color textC, HorizAlignment hAlign, VertAlignment vAlign )
    {
        super( text, fontName, fontSize, textC, hAlign, vAlign );
        _X = x;
        _Y = y;
        V_Pin = vp;
    }

    /**
     * Constructor for the YPinnedText object
     *
     * @param x    The X-data value (real-world) of the text
     * @param y    The vertical offset in mm from the plot edge
     * @param text The text string
     */
    public YPinnedText( double x, double y, String text )
    {
        super( text );
        _X = x;
        _Y = y;
        V_Pin = VertPinEdge.TOP;
    }

    /**
     * render this text string
     *
     * @param g     The graphics context on which to render the text
     * @param owner The JBasicPlot that owns this text
     */
    public void render( Graphics g, JBasicPlot owner )
    {
        if( !visible || _Text.length() < 1 || !owner.getCanDisplay() )
            return;

        // Remove any pre-existing regions before creating new...
        region.clear();
        int yOffset = owner.getUnitsMgr().getVertUnitsToPixels( _Y );
        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate( 0.0, 0.0, _X, 0.0 );
        ct.WorldToPlot( coord );
        int xval = (int) coord.getX();
        int yval = V_Pin == VertPinEdge.TOP ? owner.getPlotTop() + yOffset : owner.getPlotTop() + owner.getPlotHeight() - yOffset;
        Graphics2D g2d = (Graphics2D) g;
        // Save old color
        Color oldColor = g2d.getColor();

        // Create new font and color
        g2d.setColor( _Color );

        // Layout and render text
        TextLayout textTl = new TextLayout( _Text, new Font( _FontName, Font.PLAIN, (int) _FontSize ), new FontRenderContext( null, false, false ) );
        float xshift = getHorizontalAlignmentOffset( textTl );
        float yshift = getVerticalAlignmentOffset( textTl );
        textTl.draw( g2d, xval + xshift, yval + yshift );
        AffineTransform textAt = new AffineTransform();
        textAt.translate( xval + xshift, yval + yshift );
        Shape s = textTl.getOutline( textAt );
        addToRegion( s.getBounds2D() );

        // restore old color
        g2d.setColor( oldColor );
    }

    private double _X;
    private double _Y;
    private VertPinEdge V_Pin;
}

