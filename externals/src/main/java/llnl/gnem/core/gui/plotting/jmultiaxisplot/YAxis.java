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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import llnl.gnem.core.gui.plotting.AxisScale;
/*
 *  COPYRIGHT NOTICE
 *  GnemUtils Version 1.0
 *  Copyright (C) 2002 Lawrence Livermore National Laboratory.
 */
import llnl.gnem.core.gui.plotting.DrawingUnits;
import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.TickDir;
import llnl.gnem.core.gui.plotting.TickLabel;
import llnl.gnem.core.gui.plotting.TickMetrics;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;

/**
 * A class that manages Y-axes of subplots in a JMultiAxisPlot
 *
 * @author Doug Dodge
 */
public class YAxis extends PlotAxis {

    /**
     * Constructor for the YAxis object
     *
     * @param subplot
     *            The subplot that owns this Y-axis
     */
    public YAxis(JSubplot subplot) {
        this.subplot = subplot;
        xpos = 0.0;
        setLabelOffset(15.0);
    }

    //
    /**
     * Gets the X-position of the YAxis. A YAxis is always aligned vertically so
     * that it extends from the top to the bottom of the plotting area. However,
     * it can be shifted left and right. The XPosition is the horizontal
     * position in Drawing units e.g. mm.
     *
     * @return The X-offset in mm from the left end of the X-axis.
     */
    public double XPosition() {
        return xpos;
    }

    /**
     * Sets the X-position of the YAxis. A YAxis is always aligned vertically so
     * that it extends from the top to the bottom of the plotting area. However,
     * it can be shifted left and right. The XPosition is the horizontal
     * position in Drawing units e.g. mm.
     *
     * @param v
     *            The X-offset in mm from the left end of the X-axis.
     */
    public void XPosition(double v) {
        xpos = v;
    }

    /**
     * Gets the minimum value of the YAxis
     *
     * @return The min value
     */
    public double getMin() {
        return ymin;
    }

    /**
     * Sets the minimum value of the YAxis
     *
     * @param v
     *            The new min value
     */
    public void setMin(double v) {
        ymin = v;
    }

    /**
     * Gets the maximum value of the YAxis
     *
     * @return The max value
     */
    public double getMax() {
        return ymax;
    }

    /**
     * Sets the maximum value of the YAxis
     *
     * @param v
     *            The new max value
     */
    public void setMax(double v) {
        ymax = v;
    }

    public CoordinateTransform getCoordinateTransform() {
        return subplot.getCoordinateTransform();
    }

    public TickMetrics getTickMetrics(double height) {
        AxisScale scale = subplot.getCoordinateTransform().getYScale();
        if (scale == AxisScale.LOG) {
            return defineLogAxis(ymin, ymax, fullyDecorate(height));
        } else {
            return defineAxis(ymin, ymax, fullyDecorate(height));
        }
    }

    public boolean fullyDecorate(double height) {
        DrawingUnits du = subplot.getUnitsMgr();
        return du.getVertUnitsToPixels(minFullyDecoratedAxisLength) <= height;
    }

    /**
     * render the axis on the supplied graphics context
     *
     * @param g
     *            The graphics context
     * @param LeftMargin
     *            The plotting area left margin in pixels
     * @param TopMargin
     *            The plotting area top margin in pixels
     * @param BoxHeight
     *            The height of the plotting area in pixels
     * @param BoxWidth
     *            The width of the plotting area in pixels
     */
    public void Render(Graphics g, int LeftMargin, int TopMargin, int BoxHeight, int BoxWidth) {
        if (!visible) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        // Set to copy over pixels
        g2d.setPaintMode();
        g2d.setColor(axisColor);
        g2d.setStroke(new BasicStroke(axisPenWidth));

        // Draw the axis line...
        DrawingUnits du = subplot.getUnitsMgr();
        xPosPixel = LeftMargin + du.getHorizUnitsToPixels(xpos);
        g2d.drawLine(xPosPixel, TopMargin, xPosPixel, TopMargin + BoxHeight);
        if (ticks.getDir() == TickDir.IN) {
            MajorTickEndPixel = xPosPixel + du.getHorizUnitsToPixels(ticks.getMajorLen());
            MinorTickEndPixel = xPosPixel + du.getHorizUnitsToPixels(ticks.getMinorLen());
            tickLabelLeft = xPosPixel - du.getHorizUnitsToPixels(1.0);
        } else {
            MajorTickEndPixel = xPosPixel - du.getHorizUnitsToPixels(ticks.getMajorLen());
            MinorTickEndPixel = xPosPixel - du.getHorizUnitsToPixels(ticks.getMinorLen());
            tickLabelLeft = MajorTickEndPixel - du.getHorizUnitsToPixels(1.0);
        }

        // Save old font and color
        Font oldFont = g2d.getFont();
        Color oldColor = g2d.getColor();

        // Create new font and color
        g2d.setColor(getTickFontColor());
        g2d.setFont(new Font(getTickFontName(), Font.PLAIN, getTickFontSize()));

        // Only draw all tick information if axis is over a minimum length
        fullyDecorateAxis = du.getVertUnitsToPixels(minFullyDecoratedAxisLength) <= BoxHeight;
        CoordinateTransform ct = subplot.getCoordinateTransform();
        renderTicks(g2d, ymin, ymax, ct.getYScale());
        if (getLabelVisible() && fullyDecorateAxis) {
            DrawAxisLabel(g2d, TopMargin, BoxHeight);
        }

        // restore old values
        g2d.setColor(oldColor);
        g2d.setFont(oldFont);
    }

    /**
     * render a single tick
     *
     * @param g
     *            The graphics context to use
     * @param val
     *            The data value for this tick
     * @param label
     *            A String with the label value
     * @param isMajor
     *            true if this is a major tick
     */
    @Override
    protected void renderTick(Graphics g, double val, TickLabel label, boolean isMajor, HorizAlignment alignment) {
        Graphics2D g2d = (Graphics2D) g;
        CoordinateTransform ct = subplot.getCoordinateTransform();
        Coordinate c = new Coordinate(0.0, 0.0, 0.0, val);
        ct.WorldToPlot(c);
        int yPixelVal = (int) c.getY(); //subplot.getYMapper().getYpixel(val);
        if (isMajor) {
            g2d.drawLine(xPosPixel, yPixelVal, MajorTickEndPixel, yPixelVal);
            if (label.hasLabel1()) {
                FontMetrics fm = g2d.getFontMetrics();
                int advance = fm.stringWidth(label.getLabel1());
                g2d.drawString(label.getLabel1(), tickLabelLeft - advance, yPixelVal + fm.getAscent() / 2);
            }
        } else {
            g2d.drawLine(xPosPixel, yPixelVal, MinorTickEndPixel, yPixelVal);
        }
    }
    //---------------------------------------------------------------------------

    /**
     * Gets a tick label string given a value
     *
     * @param value
     *            The data value for this tick
     * @return The string to be printed for this tick
     */
    String getValueString(double value) {
        return String.format("%5.3g", value);
    }

    private void DrawAxisLabel(Graphics g, int TopMargin, int BoxHeight) {
        if (getLabelText().length() < 1) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        int ypos = TopMargin + BoxHeight / 2;
        int localXpos = xPosPixel - subplot.getUnitsMgr().getHorizUnitsToPixels(getLabelOffset());

        // Save old font and color
        Font oldFont = g2d.getFont();
        Color oldColor = g2d.getColor();

        //FontMetrics does not seem to work with rotated fonts, so first get metrics using an
        // unrotated font.
        Font testFont = getLabelFont();
        g2d.setFont(testFont);
        FontMetrics fm = g2d.getFontMetrics();
        int advance = fm.stringWidth(getLabelText());
        int ascent = fm.getAscent();

        // Create new font and color
        AffineTransform at = new AffineTransform();
        at.setToRotation(-Math.PI / 2.0);
        Font rotatedFont = getLabelFont().deriveFont(at);
        g2d.setColor(getLabelColor());
        g2d.setFont(rotatedFont);

        // Layout (rotate) and render text

        g2d.drawString(getLabelText(), localXpos + ascent, ypos + advance / 2);

        // restore old values
        g2d.setColor(oldColor);
        g2d.setFont(oldFont);
    }

    private double xpos;
    // Position in real-world units right of left edge of axis
    private int MajorTickEndPixel;
    private int MinorTickEndPixel;
    private int xPosPixel;
    private int tickLabelLeft;
    private final JSubplot subplot;
    private double ymin;
    private double ymax;
}
