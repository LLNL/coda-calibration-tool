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
import llnl.gnem.core.gui.plotting.VertPinEdge;

/**
 * Description of the Class
 *
 * @author Doug Dodge
 */
public class PinnedText extends BasicText {

    /**
     * Constructor for the PinnedText object
     *
     * @param xPos
     *            distance in mm from the x-pin edge
     * @param yPos
     *            distance in mm from the y-pin edge
     * @param text
     *            The text to render
     * @param hp
     *            The horizontal edge to pin to
     * @param vp
     *            The vertical edge to pin to
     * @param fontName
     *            The name of the font used to render the text
     * @param fontSize
     *            The fontSize
     * @param textC
     *            The color of the text
     * @param hAlign
     *            The horizontal alignment type
     * @param vAlign
     *            The vertical alignment type
     */
    public PinnedText(double xPos, double yPos, String text, HorizPinEdge hp, VertPinEdge vp, String fontName, double fontSize, Color textC, HorizAlignment hAlign, VertAlignment vAlign) {
        super(text, fontName, fontSize, textC, hAlign, vAlign);
        xposition = xPos;
        yPosition = yPos;
        horizontalPinEdge = hp;
        verticalPinEdge = vp;
    }

    /**
     * Constructor for the PinnedText object
     *
     * @param x
     *            distance in mm from the x-pin edge
     * @param y
     *            distance in mm from the y-pin edge
     * @param text
     *            The text to render
     */
    public PinnedText(double x, double y, String text) {
        super(text);
        xposition = x;
        yPosition = y;
        horizontalPinEdge = HorizPinEdge.LEFT;
        verticalPinEdge = VertPinEdge.TOP;
    }

    /**
     * Description of the Method
     *
     * @param g
     *            Description of the Parameter
     * @param owner
     *            Description of the Parameter
     */
    @Override
    public void render(Graphics g, JBasicPlot owner) {
        if (!visible || _Text.length() < 1 || !owner.getCanDisplay()) {
            return;
        }

        // Remove any pre-existing regions before creating new...
        region.clear();
        int xOffset = owner.getUnitsMgr().getHorizUnitsToPixels(xposition);
        int yOffset = owner.getUnitsMgr().getVertUnitsToPixels(yPosition);
        int xval = horizontalPinEdge == HorizPinEdge.LEFT ? owner.getPlotLeft() + xOffset : owner.getPlotLeft() + owner.getPlotWidth() - xOffset;
        int yval = verticalPinEdge == VertPinEdge.TOP ? owner.getPlotTop() + yOffset : owner.getPlotTop() + owner.getPlotHeight() - yOffset;
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

    private double xposition;
    private double yPosition;
    private HorizPinEdge horizontalPinEdge;
    private VertPinEdge verticalPinEdge;
}
