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
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;


/**
 * Text displayed within a JSubplot that is tied to the real-world coordinates
 * shown on the X-, and Y-axes.
 *
 * @author Doug Dodge
 */
public class DataText extends BasicText {
    /**
     * Constructor for the DataText object
     *
     * @param x    The real-world X-coordinate
     * @param y    The real-world Y-coordinate
     * @param text The String to be displayed
     */
    public DataText( double x, double y, String text )
    {
        super( text );
        _X = x;
        _Y = y;
    }

    /**
     * Constructor for the DataText object
     *
     * @param x        The real-world X-coordinate
     * @param y        The real-world Y-coordinate
     * @param text     The String to be displayed
     * @param fontName The name of the font that will be used to render the text
     * @param fontSize The size in points of the font used to render the text
     * @param textC    The color of the text
     * @param hAlign   The horizontal alignment of the text. For example, left,
     *                 center, right
     * @param vAlign   The vertical alignment of the text. For example top,
     *                 center, bottom
     */
    public DataText( double x, double y, String text, String fontName, double fontSize, Color textC, HorizAlignment hAlign, VertAlignment vAlign )
    {
        super( text, fontName, fontSize, textC, hAlign, vAlign );
        _X = x;
        _Y = y;
    }

    /**
     * render the text to the supplied graphics context
     *
     * @param g     The graphics context on which to render the text
     * @param owner The JBasicPlot object to which the text belongs
     */
    public void render( Graphics g, JBasicPlot owner )
    {
        if( !visible || _Text.length() < 1 ) {
            return;
        }

        // Remove any pre-existing regions before creating new...
        region.clear();

        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate( 0.0, 0.0, _X, _Y );
        ct.WorldToPlot( coord );

        int xval = (int) coord.getX();
        int yval = (int) coord.getY();

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
}

