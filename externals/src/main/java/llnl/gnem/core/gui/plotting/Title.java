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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;

/**
 * Class that is responsible for managing a horizontally-centered title string
 * that can be drawn on the top of a plot.
 *
 * @author Doug Dodge
 */
public class Title {
    private String text = "";
    private String fontName = "Arial";
    private int fontSize = 10;
    private int fontStyle = Font.PLAIN;
    private Color color = Color.black;
    private boolean visible = true;
    private double offset = 2.0;
    // Offset of label from top of plot in physical units, e.g. mm

    /**
     * Default Constructor for the Title object
     */
    public Title() {
    }

    /**
     * Constructor for the Title object
     *
     * @param text
     *            The title text
     * @param fontname
     *            The name of the font in which the title will be rendered
     * @param c
     *            The color with which to render the title
     * @param size
     *            The font size to use when rendering the title
     * @param v
     *            Controls whether the title is visible
     * @param off
     *            The vertical offset in mm of the title from the top of the
     *            plot.
     */
    public Title(String text, String fontname, Color c, int size, boolean v, double off) {
        this.text = text;
        this.fontName = fontname;
        this.color = c;
        this.fontSize = size;
        this.visible = v;
        this.offset = off;
    }

    /**
     * Gets the color attribute of the Title object
     *
     * @return The color value
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the color attribute of the Title object
     *
     * @param v
     *            The new color value
     */
    public void setColor(Color v) {
        color = v;
    }

    /**
     *
     * @return the font to set
     */
    public Font getFont() {
        return new Font(fontName, fontStyle, fontSize);
    }

    /**
     *
     * @param font
     *            to set
     */
    public void setFont(Font font) {
        fontName = font.getName();
        fontSize = font.getSize();
        fontStyle = font.getStyle();
    }

    /**
     * Gets the fontName attribute of the Title object
     *
     * @return The fontName value
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * Sets the fontName attribute of the Title object
     *
     * @param v
     *            The new fontName value
     */
    public void setFontName(String v) {
        fontName = v;
    }

    /**
     * Gets the fontSize attribute of the Title object
     *
     * @return The fontSize value
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Sets the fontSize attribute of the Title object
     *
     * @param v
     *            The new fontSize value
     */
    public void setFontSize(int v) {
        fontSize = v >= 1 ? v : 1;
    }

    /**
     * Gets the offset attribute of the Title object
     *
     * @return The offset value
     */
    public double getOffset() {
        return offset;
    }

    /**
     * Sets the offset attribute of the Title object
     *
     * @param v
     *            The new offset value
     */
    public void setOffset(double v) {
        offset = v;
    }

    /**
     * Gets the text attribute of the Title object
     *
     * @return The text value
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text attribute of the Title object
     *
     * @param v
     *            The new text value
     */
    public void setText(String v) {
        text = v;
    }

    /**
     * Gets the visible attribute of the Title object
     *
     * @return The visible value
     */
    public boolean getVisible() {
        return visible;
    }

    /**
     * Sets the visible attribute of the Title object
     *
     * @param v
     *            The new visible value
     */
    public void setVisible(boolean v) {
        visible = v;
    }

    /**
     * render the title on the supplied graphics context
     *
     * @param g
     *            The graphics context on which to render the title
     * @param LeftMargin
     *            The left margin of the plot in pixels
     * @param TopMargin
     *            The top margin of the plot in pixels
     * @param BoxWidth
     *            The plot width in pixels
     * @param unitsMgr
     *            The Units Manager of the containing axis that will be used for
     *            computing offsets.
     */
    public void Render(Graphics g, int LeftMargin, int TopMargin, int BoxWidth, DrawingUnits unitsMgr) {
        if (!visible || text == null || text.length() < 1)
            return;
        int xpos = LeftMargin + BoxWidth / 2;
        int ypos = TopMargin - unitsMgr.getVertUnitsToPixels(offset);
        Graphics2D g2d = (Graphics2D) g;
        // Save old font and color
        Font oldFont = g2d.getFont();
        Color oldColor = g2d.getColor();

        // Create new font and color
        g2d.setColor(color);
        g2d.setFont(new Font(fontName, Font.PLAIN, fontSize));

        // Layout and render text
        FontMetrics fm = g2d.getFontMetrics();
        int advance = fm.stringWidth(text);
        g2d.drawString(text, xpos - advance / 2, ypos - fm.getMaxAscent());

        // restore old values
        g2d.setColor(oldColor);
        g2d.setFont(oldFont);
    }

}
