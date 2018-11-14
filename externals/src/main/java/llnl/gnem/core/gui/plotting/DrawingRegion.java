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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 * A DrawingRegion is a rectangular region on the canvas on which objects are
 * rendered. A DrawingRegion has a brushable interior and a surrounding line
 * Various properties can be changed for both the interior and the line.
 *
 * @author Doug Dodge
 */
public class DrawingRegion {

    private Rectangle box;
    private boolean drawBox;
    private boolean fillRegion;
    private Color backgroundColor;
    private Color lineColor;
    private int boxLineWidth;
    private Color backgroundHighlightColor;
    private Color backgroundUnselectedColor;
    private int highlightDifferential = 10;

    /**
     * Constructor for the DrawingRegion object
     */
    public DrawingRegion() {
        drawBox = true;
        backgroundHighlightColor = Color.white;
        backgroundUnselectedColor = new Color(255 - highlightDifferential, 255 - highlightDifferential, 255 - highlightDifferential);
        backgroundColor = backgroundHighlightColor;
        lineColor = Color.black;
        boxLineWidth = 1;
        fillRegion = true;
    }

    public void setHighlighted(boolean value) {
        if (value)
            backgroundColor = backgroundHighlightColor;
        else
            backgroundColor = backgroundUnselectedColor;
    }

    /**
     * Gets the Rectangle enclosing this DrawingRegion
     *
     * @return The Rectangle
     */
    public Rectangle getRect() {
        return box;
    }

    /**
     * Returns true if the region is set to draw a box around its exterior.
     *
     * @return true if the region will be rendered with a surrounding box.
     */
    public boolean getDrawBox() {
        return drawBox;
    }

    /**
     * Controls whether a box is drawn around the region
     *
     * @param v
     *            true if a box should be rendered.
     */
    public void setDrawBox(boolean v) {
        drawBox = v;
    }

    /**
     * Returns true if the region will be rendered with its interior brushed. If
     * the interior is not brushed, then the underlying component will show
     * through.
     *
     * @return true if the interior should be brushed.
     */
    public boolean getFillRegion() {
        return fillRegion;
    }

    /**
     * Control whether the interior of the region is brushed.
     *
     * @param v
     *            true if the interior is to be brushed.
     */
    public void setFillRegion(boolean v) {
        fillRegion = v;
    }

    /**
     * Gets the background Color of the DrawingRegion object
     *
     * @return The background Color
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getUnselectedColor() {
        return backgroundUnselectedColor;
    }

    /**
     * Sets the background Color of the DrawingRegion object
     *
     * @param color
     *            The new background Color value
     */
    public void setBackgroundColor(Color color) {
        int red = color.getRed();
        int highlightRed = red;
        int green = color.getGreen();
        int highlightGreen = green;
        int blue = color.getBlue();
        int highlightBlue = blue;
        if (red + highlightDifferential < 256) {
            highlightRed = red + highlightDifferential;
        } else
            red -= highlightDifferential;
        if (green + highlightDifferential < 256) {
            highlightGreen = green + highlightDifferential;
        } else
            green -= highlightDifferential;

        if (blue + highlightDifferential < 256) {
            highlightBlue = blue + highlightDifferential;
        } else
            blue -= highlightDifferential;
        backgroundColor = new Color(red, green, blue);
        backgroundUnselectedColor = backgroundColor;
        backgroundHighlightColor = new Color(highlightRed, highlightGreen, highlightBlue);

    }

    /**
     * Gets the line Color of the DrawingRegion object
     *
     * @return The line Color value
     */
    public Color getLineColor() {
        return lineColor;
    }

    /**
     * Sets the line Color of the DrawingRegion object
     *
     * @param C
     *            The new line Color value
     */
    public void setLineColor(Color C) {
        lineColor = C;
    }

    /**
     * Gets the boxLine Width of the DrawingRegion object
     *
     * @return The boxLine Width value
     */
    public int getBoxLineWidth() {
        return boxLineWidth;
    }

    /**
     * Sets the boxLine Width of the DrawingRegion object
     *
     * @param v
     *            The new boxLine Width value
     */
    public void setBoxLineWidth(int v) {
        boxLineWidth = v;
    }

    /**
     * @param LeftMargin
     *            The left margin of the drawing region in pixels
     * @param TopMargin
     *            The top margin of the drawing region in pixels
     * @param BoxHeight
     *            The box height in pixels
     * @param BoxWidth
     *            The box width in pixels
     */
    public void setRect(int LeftMargin, int TopMargin, int BoxHeight, int BoxWidth) {
        box = new Rectangle(LeftMargin, TopMargin, BoxWidth, BoxHeight);
    }

    /**
     * render this drawing region to the supplied graphics context
     *
     * @param gin
     *            The graphics context on which to render the drawing region
     */
    public void render(Graphics gin) {
        if (!drawBox && !fillRegion)
            return;

        Graphics2D g = (Graphics2D) gin;
        g.setPaintMode();
        if (fillRegion) {
            g.setColor(backgroundColor);
            g.fill(box);
        }
        drawBox(gin);
    }

    protected void drawBox(Graphics gin) {
        Graphics2D g = (Graphics2D) gin;
        g.setPaintMode();

        if (drawBox) {
            g.setColor(lineColor);
            g.setStroke(new BasicStroke(boxLineWidth));
            g.draw(box);
        }
    }

    protected Color getOffsetColor(int offset) {
        Color bg = getBackgroundColor();
        Color current = getUnselectedColor();

        int red = offset(bg.getRed(), current.getRed(), offset);
        int green = offset(bg.getGreen(), current.getGreen(), offset);
        int blue = offset(bg.getBlue(), current.getBlue(), offset);

        return new Color(red, green, blue);
    }

    private int offset(int component, int current, int offset) {
        return (component - offset > 0) ? component - offset : current;
    }

    /**
     * @return currently specified highlight differential
     */
    public int getHighlightDifferential() {
        return highlightDifferential;
    }

    /**
     * Allows the application to specify a different highlight differential than
     * the default. Expected usage is to setHighlightDifferential and then call
     * setBackgroundColor(Color color) to update the highlighted and unselected
     * background colors.
     * 
     * @param highlightDifferential
     */
    public void setHighlightDifferential(int highlightDifferential) {
        this.highlightDifferential = highlightDifferential;
    }

}
