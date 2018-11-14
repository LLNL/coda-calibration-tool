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
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.VertAlignment;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;

/**
 * Text that is pinned in the horizontal direction a fixed amount from one of
 * the plot's edges. In the vertical direction, the text is fixed to a data
 * value. The effect is that upon zooming, the text stays the same distance from
 * the edge (horizontally) but floats to the new position of the Y-value.
 *
 * @author Doug Dodge
 */
public class XPinnedText extends BasicText {
    /**
     * Constructor for the XPinnedText object
     *
     * @param x
     *            Horizontal offset from the plot edge in mm
     * @param y
     *            Y-value (real-world) of the text
     * @param text
     *            The text string
     * @param hp
     *            The plot edge to pin to ( LEFT, RIGHT )
     * @param fontName
     *            The name of the font used to render the text
     * @param fontSize
     *            The size of the font used to render the text
     * @param textC
     *            The color of the text
     * @param hAlign
     *            The horizontal alignment of the text relative to the pin
     *            point.
     * @param vAlign
     *            The vertical alignment of the text relative to the pin point.
     */
    public XPinnedText(double x, double y, String text, HorizPinEdge hp, String fontName, double fontSize, Color textC, HorizAlignment hAlign, VertAlignment vAlign) {
        super(text, fontName, fontSize, textC, hAlign, vAlign);
        xValueMM = x;
        yValueRealWorld = y;
        H_Pin = hp;
    }

    /**
     * Constructor for the XPinnedText object
     *
     * @param x
     *            Horizontal offset from the plot edge in mm
     * @param y
     *            Y-value (real-world) of the text
     * @param text
     *            The text string
     */
    public XPinnedText(double x, double y, String text) {
        super(text);
        xValueMM = x;
        yValueRealWorld = y;
        H_Pin = HorizPinEdge.LEFT;
    }

    /**
     * render this text string
     *
     * @param g
     *            The graphics context on which to render the text
     * @param owner
     *            The JBasicPlot that owns this text
     */
    public void render(Graphics g, JBasicPlot owner) {
        if (!visible || _Text.length() < 1 || !owner.getCanDisplay())
            return;

        // Remove any pre-existing regions before creating new...
        region.clear();
        int xOffset = owner.getUnitsMgr().getHorizUnitsToPixels(xValueMM);
        int xval = H_Pin == HorizPinEdge.LEFT ? owner.getPlotLeft() + xOffset : owner.getPlotLeft() + owner.getPlotWidth() - xOffset;
        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate c = new Coordinate(0.0, 0.0, 0.0, yValueRealWorld);
        ct.WorldToPlot(c);
        int yval = (int) c.getY(); //owner.getYMapper().getYpixel(_Y);
        Graphics2D g2d = (Graphics2D) g;
        // Save old color
        Color oldColor = g2d.getColor();

        // Create new font and color
        g2d.setColor(_Color);

        // Layout and render text
        TextLayout textTl = new TextLayout(_Text, new Font(_FontName, Font.PLAIN, (int) _FontSize), new FontRenderContext(null, false, false));
        float xshift = getHorizontalAlignmentOffset(textTl);
        float yshift = getVerticalAlignmentOffset(textTl);
        textTl.draw(g2d, xval + xshift, yval + yshift);
        AffineTransform textAt = new AffineTransform();
        textAt.translate(xval + xshift, yval + yshift);
        Shape s = textTl.getOutline(textAt);
        addToRegion(s.getBounds2D());

        // restore old color
        g2d.setColor(oldColor);
    }

    private double xValueMM;
    private double yValueRealWorld;
    private HorizPinEdge H_Pin;

    public void setYValue(double newValue) {
        yValueRealWorld = newValue;
    }
}
