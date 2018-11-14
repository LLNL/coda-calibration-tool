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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.text.NumberFormat;

import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;

/**
 * Created by dodge1 Date: Feb 7, 2008
 */
public class PositionMarker extends PlotObject {

    private double xCenter;
    private double yCenter;
    private double innerSize;
    private double outerSize;
    private Color color;

    public PositionMarker() {
        xCenter = 0.5;
        yCenter = 0.5;
        innerSize = 5.0;
        outerSize = 24.0;
        setColor(Color.red);
    }

    /**
     * Constructor for the Symbol object
     * 
     * @param X
     *            The X-center of the symbol in real-world coordinates
     * @param Y
     *            The Y-center of the symbol in real-world coordinates
     * @param size
     *            The innerSize of the Symbol in mm
     */
    public PositionMarker(double X, double Y, double size, double outerSize) {
        xCenter = X;
        yCenter = Y;
        this.innerSize = size;
        this.outerSize = outerSize;
        setColor(Color.red);
    }

    public PositionMarker(double X, double Y, double innerSize, double outerSize, Color color) {
        xCenter = X;
        yCenter = Y;
        this.innerSize = innerSize;
        this.outerSize = outerSize;
        setColor(color);
    }

    @Override
    public void setSelected(boolean selected, Graphics g) {
    }

    /**
     * render this Symbol to the supplied graphics context
     *
     * @param g
     *            The graphics context
     * @param owner
     *            The JBasicPlot that owns this symbol
     */
    @Override
    public void render(Graphics g, JBasicPlot owner) {
        if (g == null || !visible || owner == null || !owner.getCanDisplay()) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;

        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate(0.0, 0.0, xCenter, yCenter);
        ct.WorldToPlot(coord);

        int xcenter = (int) coord.getX();
        int ycenter = (int) coord.getY();

        int inner = (int) (owner.getUnitsMgr().getPixelsPerUnit() * innerSize);
        int outer = (int) (owner.getUnitsMgr().getPixelsPerUnit() * outerSize);
        g2d.clip(owner.getPlotRegion().getRect());

        paintIt(g, xcenter, ycenter, inner, outer);
    }

    public void paintIt(Graphics g, int x, int y, int inner, int outer) {
        int width = 3;
        Graphics2D g2d = (Graphics2D) g;
        g2d.setColor(color);
        g2d.setStroke(new BasicStroke(width));
        g2d.drawLine(x, y - inner, x, y - outer);
        g2d.drawLine(x + inner, y, x + outer, y);
        g2d.drawLine(x, y + inner, x, y + outer);
        g2d.drawLine(x - inner, y, x - outer, y);
    }

    /**
     * Gets the xcenter attribute of the Symbol object
     *
     * @return The xcenter value
     */
    public double getXcenter() {
        return xCenter;
    }

    /**
     * Gets the ycenter attribute of the Symbol object
     *
     * @return The ycenter value
     */
    public double getYcenter() {
        return yCenter;
    }

    /**
     * Gets the symbolSize attribute of the Symbol object
     *
     * @return The symbolSize value
     */
    public double getSymbolSize() {
        return innerSize;
    }

    /**
     * Gets the color attribute of the Symbol object
     *
     * @return The color value
     */
    public Color getColor() {
        return color;
    }

    /**
     * Sets the xcenter attribute of the Symbol object
     *
     * @param v
     *            The new xcenter value
     */
    public void setXcenter(double v) {
        xCenter = v;
    }

    /**
     * Sets the ycenter attribute of the Symbol object
     *
     * @param v
     *            The new ycenter value
     */
    public void setYcenter(double v) {
        yCenter = v;
    }

    /**
     * Sets the symbolSize attribute of the Symbol object
     *
     * @param v
     *            The new symbolSize value
     */
    public void setSymbolSize(double v) {
        innerSize = v;
    }

    /**
     * Sets the color attribute of the Symbol object
     *
     * @param v
     *            The new color value
     */
    public void setColor(Color v) {
        color = v;
    }

    /**
     * Move this Symbol to a different place in the subplot
     *
     * @param owner
     *            The JBasicPlot that owns this symbol
     * @param graphics
     * @param dx
     *            The amount to move in the X-direction in real-world
     *            coordinates
     * @param dy
     *            The amount to move in the Y-direction in real-world
     *            coordinates
     */
    @Override
    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {
        if (graphics == null) {
            graphics = owner.getOwner().getGraphics();
        }
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.clip(owner.getPlotRegion().getRect());
        render(graphics, owner);
        if (canDragX) {
            xCenter += dx;
        }
        if (canDragY) {
            yCenter += dy;
        }
        render(graphics, owner);
    }

    /**
     * Gets a String description of this Symbol object
     *
     * @return The String description
     */
    @Override
    public String toString() {
        StringBuffer s = new StringBuffer(" Symbol at (");
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(5);
        s.append(f.format(xCenter));
        s.append(", ");
        s.append(f.format(yCenter));
        s.append(")");
        return s.toString();
    }

    public double getOuterSize() {
        return outerSize;
    }

    public void setOuterSize(double outerSize) {
        this.outerSize = outerSize;
    }
}
