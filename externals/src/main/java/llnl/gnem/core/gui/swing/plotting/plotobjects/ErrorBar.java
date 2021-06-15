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
package llnl.gnem.core.gui.swing.plotting.plotobjects;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;

import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;

/**
 * A class that draws a centered circle superimposed on an "I-beam" whose height
 * is equal to the standard error associated with the measurement being
 * represented by the centered symbol. The symbol may have text associated with
 * it and can have its internal color set independently of its edge color. The
 * symbol edge color is the same color as the I-Beam representing the
 * uncertainty. The positive uncertainty can be set independently of the
 * negative uncertainty.
 */
public class ErrorBar extends Symbol {

    public enum Orientation {
        Vertical, Horizontal
    };

    /**
     * Constructor for ErrorBar that allows specification of both a plus and a
     * minus value for the standard error as well as setting all properties for
     * the centered symbol.
     * 
     * @param sigmaPlus
     *            The positive standard error in data units
     * @param sigmaMinus
     *            The negative standard error in data units
     * @param halfWidth
     *            The half-width of the error bar "I-Beam" in mm
     * @param X
     *            X-coordinate of the center of the symbol
     * @param Y
     *            Y-coordinate of the center of the symbol
     * @param size
     *            Size of the symbol in millimeters
     * @param fillC
     *            Fill color of the symbol
     * @param edgeC
     *            Edge color of the symbol edge
     * @param textC
     *            Color of the text
     * @param text
     *            Optional text associated with the symbol.
     * @param visible
     *            Controls whether the symbol is visible.
     * @param textVis
     *            Controls whether the text associated with the symbol is
     *            visible.
     * @param fontsize
     *            The fontsize of the associated text.
     */
    public ErrorBar(double X, double Y, double size, double sigmaPlus, double sigmaMinus, double halfWidth, Color fillC, Color edgeC, Color textC, String text, boolean visible, boolean textVis,
            double fontsize) {
        super(X, Y, size, fillC, edgeC, textC, text, visible, textVis, fontsize);
        Math.abs(sigmaMinus);
        this.sigmaValue = Math.abs(sigmaPlus);
        this.halfWidth = halfWidth;
    }

    /**
     * Constructor for ErrorBar that allows specification of a single standard
     * error value for the standard error as well as setting all properties for
     * the centered symbol.
     *
     * @param X
     *            X-coordinate of the center of the symbol
     * @param Y
     *            Y-coordinate of the center of the symbol
     * @param size
     *            Size of the symbol in millimeters
     * @param sigma
     *            The standard error in data units
     * @param halfWidth
     *            The half-width of the error bar "I-Beam" in mm
     * @param fillC
     *            Fill color of the symbol
     * @param edgeC
     *            Edge color of the symbol edge
     * @param textC
     *            Color of the text
     * @param text
     *            Optional text associated with the symbol.
     * @param visible
     *            Controls whether the symbol is visible.
     * @param textVis
     *            Controls whether the text associated with the symbol is
     *            visible.
     * @param fontsize
     *            The fontsize of the associated text.
     */
    public ErrorBar(double X, double Y, double size, double sigma, double halfWidth, Color fillC, Color edgeC, Color textC, String text, boolean visible, boolean textVis, double fontsize) {
        super(X, Y, size, fillC, edgeC, textC, text, visible, textVis, fontsize);
        Math.abs(sigma);
        this.sigmaValue = Math.abs(sigma);
        this.halfWidth = halfWidth;
    }

    /**
     * Constructor that allows setting independent Y-standard errors but uses
     * default values for the centered symbol
     *
     * @param sigmaPlus
     *            The positive standard error in data units
     * @param sigmaMinus
     *            The negative standard error in data units
     * @param halfWidth
     *            The half-width of the error bar "I-Beam" in mm
     * @param X
     *            X-coordinate of the center of the symbol
     * @param Y
     *            Y-coordinate of the center of the symbol
     * @param size
     *            Size of the symbol in millimeters
     */
    public ErrorBar(double X, double Y, double size, double sigmaPlus, double sigmaMinus, double halfWidth) {
        super(X, Y, size);
        Math.abs(sigmaMinus);
        this.sigmaValue = Math.abs(sigmaPlus);
        this.halfWidth = halfWidth;
    }

    /**
     * Constructor that allows specification of a single-standard error value
     * and that uses default values for all properties related to drawing the
     * centered symbol.
     *
     * @param X
     *            X-coordinate of the center of the symbol
     * @param Y
     *            Y-coordinate of the center of the symbol
     * @param size
     *            Size of the symbol in millimeters
     * @param sigma
     *            The standard error in data units
     * @param halfWidth
     *            The half-width of the error bar "I-Beam" in mm
     */
    public ErrorBar(double X, double Y, double size, double sigma, double halfWidth) {
        super(X, Y, size);
        Math.abs(sigma);
        this.sigmaValue = Math.abs(sigma);
        this.halfWidth = halfWidth;
    }

    public ErrorBar() {
        super();
        this.sigmaValue = 0.0;
        this.halfWidth = 1.0;
    }

    /**
     * render the symbol to the supplied graphics context. This method is called
     * by the base class render method. Text is rendered separately.
     *
     * @param g
     *            The graphics context
     * @param x
     *            The X-value ( in user space pixels )
     * @param y
     *            The Y-value ( in user space pixels )
     * @param h
     *            The height of the symbol in pixels
     */
    @Override
    public void PaintSymbol(Graphics g, int x, int y, int h) {
        int du = (int) (0.530 * h);
        Graphics2D g2d = (Graphics2D) g;

        // First draw the I-beam shaped error bars
        CoordinateTransform ct = owner.getCoordinateTransform();
        GeneralPath p = new GeneralPath();

        switch (ibeam) {
        case Vertical: {
            int half = owner.getUnitsMgr().getHorizUnitsToPixels(halfWidth);
            float leftEdge = (float) x - half;
            float rightEdge = leftEdge + 2 * half;
            Coordinate coord = new Coordinate(0.0, 0.0, 0.0, _Ycenter + sigmaValue);
            ct.WorldToPlot(coord);
            float top = (float) coord.getY();
            coord.setWorldC2(_Ycenter - sigmaValue);
            ct.WorldToPlot(coord);
            float bottom = (float) coord.getY();

            p.moveTo(leftEdge, top);
            p.lineTo(rightEdge, top);
            p.moveTo(x, top);
            p.lineTo(x, bottom);
            p.moveTo(leftEdge, bottom);
            p.lineTo(rightEdge, bottom);
        }
            break;

        case Horizontal: {
            int half = owner.getUnitsMgr().getVertUnitsToPixels(halfWidth);
            float topEdge = (float) y - half;
            float bottomEdge = topEdge + 2 * half;
            Coordinate coord = new Coordinate(0.0, 0.0, _Xcenter + sigmaValue, 0.0);
            ct.WorldToPlot(coord);
            float right = (float) coord.getX();
            coord.setWorldC1(_Xcenter - sigmaValue);
            ct.WorldToPlot(coord);
            float left = (float) coord.getX();

            p.moveTo(left, topEdge);
            p.lineTo(left, bottomEdge);
            p.moveTo(left, y);
            p.lineTo(right, y);
            p.moveTo(right, topEdge);
            p.lineTo(right, bottomEdge);
        }
            break;
        }

        g2d.setColor(_EdgeColor);
        g2d.draw(p);

        // Now draw the centered symbol
        Ellipse2D circle = new Ellipse2D.Double(x - du, y - du, 2 * du, 2 * du);
        g2d.setColor(getFillColor());
        g2d.fill(circle);
        g2d.setColor(_EdgeColor);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(circle);
        addToRegion(circle);
    }

    /**
     * Produce a String descriptor for this object
     *
     * @return The String descriptor
     */
    @Override
    public String toString() {
        return "ErrorBar" + super.toString();
    }

    public void setOrientation(Orientation value) {
        this.ibeam = value;
    }

    private double sigmaValue;
    private double halfWidth = 2.0; // width of error bars in mm.
    private Orientation ibeam = Orientation.Vertical;
}
