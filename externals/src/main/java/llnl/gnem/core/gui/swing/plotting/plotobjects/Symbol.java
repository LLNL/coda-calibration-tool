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

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.text.NumberFormat;
import java.util.Vector;

import javax.swing.ImageIcon;

import llnl.gnem.core.gui.swing.plotting.JBasicPlot;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.swing.plotting.transforms.CoordinateTransform;

/**
 * The base class for all Symbols that can be plotted in a JSubPlot. Symbols
 * consist of a geometric shape centered on user-supplied coordinates and
 * optional text centered under the shape.
 *
 * @author Doug Dodge Will not inspect for: MagicNumber
 */
public abstract class Symbol extends PlotObject {
    public Symbol() {
        _Xcenter = 0.5;
        _Ycenter = 0.5;
        _SymbolSize = 8.0;
        setFillColor(Color.black);
        _EdgeColor = Color.black;
        _TextColor = Color.black;
        _Text = "";
        _Visible = true;
        _TextVisible = false;
        _FontSize = 10;
        edgeRenderColor = _EdgeColor;
    }

    /**
     * Constructor for the Symbol object
     *
     * @param X
     *            The X-center of the symbol in real-world coordinates
     * @param Y
     *            The Y-center of the symbol in real-world coordinates
     * @param size
     *            The size of the Symbol in mm
     */
    public Symbol(double X, double Y, double size) {
        _Xcenter = X;
        _Ycenter = Y;
        _SymbolSize = size;
        setFillColor(Color.black);
        _EdgeColor = Color.black;
        _TextColor = Color.black;
        _Text = "";
        _Visible = true;
        _TextVisible = true;
        _FontSize = 10;
        edgeRenderColor = _EdgeColor;
    }

    /**
     * Constructor for the Symbol object
     *
     * @param X
     *            The X-center of the symbol in real-world coordinates
     * @param Y
     *            The Y-center of the symbol in real-world coordinates
     * @param size
     *            The size of the Symbol in mm
     * @param fillC
     *            The fill color for the symbol
     * @param edgeC
     *            The edge color for the symbol
     * @param textC
     *            The color of the text associated with the symbol
     * @param text
     *            The text string to be plotted with the symbol
     * @param visible
     *            The visibility of the symbol
     * @param textVis
     *            The visibility of the associated text
     * @param fontsize
     *            The font size of the associated text.
     */
    public Symbol(double X, double Y, double size, Color fillC, Color edgeC, Color textC, String text, boolean visible, boolean textVis, double fontsize) {
        _Xcenter = X;
        _Ycenter = Y;
        _SymbolSize = size;
        setFillColor(fillC);
        _EdgeColor = edgeC;
        _TextColor = textC;
        _Text = text;
        _Visible = visible;
        _TextVisible = textVis;
        _FontSize = fontsize;
        edgeRenderColor = _EdgeColor;
    }

    @Override
    public void setSelected(boolean selected, Graphics g) {
        if (selected) {
            edgeRenderColor = new Color(255 - _EdgeColor.getRed(), 255 - _EdgeColor.getGreen(), 255 - _EdgeColor.getBlue(), alpha);
        } else {
            edgeRenderColor = _EdgeColor;
        }
        render(g, getOwner());
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
        if (g == null || !_Visible || owner == null || !owner.getCanDisplay()) {
            return;
        }
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaintMode(); // Make sure that we are not in XOR mode.

        // Remove any pre-existing regions before creating new...
        region.clear();

        CoordinateTransform ct = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate(0.0, 0.0, _Xcenter, _Ycenter);
        ct.WorldToPlot(coord);

        int xcenter = (int) coord.getX();
        int ycenter = (int) coord.getY();

        if (_PinXcoord) {
            xcenter = _XpinCenter;
        }

        if (_PinYcoord) {
            ycenter = _YpinCenter;
        }

        int symbolsize = (int) (owner.getUnitsMgr().getPixelsPerUnit() * _SymbolSize);
        g2d.clip(owner.getPlotRegion().getRect());

        PaintSymbol(g, xcenter, ycenter, symbolsize);
        if (_Text != null && _Text.length() > 0 && _TextVisible) {
            PaintSymbolTextOnCanvas(g2d, xcenter, ycenter, symbolsize);
        }

        for (SymbolTag tag : getTags()) {
            tag.render(xcenter, ycenter, g);
        }
    }

    public ImageIcon generateIcon() {
        BufferedImage image = new BufferedImage(17, 17, BufferedImage.TRANSLUCENT);
        ImageIcon ic = new ImageIcon(image);
        Graphics g = image.getGraphics();
        PaintSymbol(g, 8, 8, 16);
        g.dispose();
        return ic;
    }

    public BufferedImage getBufferedImage(int size) {
        BufferedImage buffImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics g = buffImage.getGraphics();
        PaintSymbol(g, size / 2 + 1, size / 2 + 1, size);
        return buffImage;
    }

    /**
     * render the symbol (but not its text) to the graphics context
     *
     * @param g
     *            The graphics context
     * @param xcenter
     *            The X-center of the symbol in user-space coordinates
     * @param ycenter
     *            The Y-center of the symbol in user-space coordinates
     * @param symbolsize
     *            The size of the symbol in user-space (pixels)
     */
    public abstract void PaintSymbol(Graphics g, int xcenter, int ycenter, int symbolsize);

    /**
     * Gets the xcenter attribute of the Symbol object
     *
     * @return The xcenter value
     */
    public double getXcenter() {
        return _Xcenter;
    }

    /**
     * Gets the ycenter attribute of the Symbol object
     *
     * @return The ycenter value
     */
    public double getYcenter() {
        return _Ycenter;
    }

    /**
     * Gets the symbolSize attribute of the Symbol object
     *
     * @return The symbolSize value
     */
    public double getSymbolSize() {
        return _SymbolSize;
    }

    /**
     * Gets the fontSize attribute of the Symbol object
     *
     * @return The fontSize value
     */
    public double getFontSize() {
        return _FontSize;
    }

    /**
     * Gets the fillColor attribute of the Symbol object
     *
     * @return The fillColor value
     */
    public Color getFillColor() {
        return fillColor;
    }

    /**
     * Gets the edgeColor attribute of the Symbol object
     *
     * @return The edgeColor value
     */
    public Color getEdgeColor() {
        return _EdgeColor;
    }

    /**
     * Gets the textColor attribute of the Symbol object
     *
     * @return The textColor value
     */
    public Color getTextColor() {
        return _TextColor;
    }

    /**
     * Gets the text attribute of the Symbol object
     *
     * @return The text value
     */
    public String getText() {
        return _Text;
    }

    /**
     * Gets the visible attribute of the Symbol object
     *
     * @return The visible value
     */
    @Override
    public boolean isVisible() {
        return _Visible;
    }

    /**
     * Gets the textVisible attribute of the Symbol object
     *
     * @return The textVisible value
     */
    public boolean getTextVisible() {
        return _TextVisible;
    }

    /**
     * Sets the xcenter attribute of the Symbol object
     *
     * @param v
     *            The new xcenter value
     */
    public void setXcenter(double v) {
        _Xcenter = v;
    }

    /**
     * Sets the ycenter attribute of the Symbol object
     *
     * @param v
     *            The new ycenter value
     */
    public void setYcenter(double v) {
        _Ycenter = v;
    }

    /**
     * Sets the symbolSize attribute of the Symbol object
     *
     * @param v
     *            The new symbolSize value
     */
    public void setSymbolSize(double v) {
        _SymbolSize = v;
    }

    /**
     * Sets the fontSize attribute of the Symbol object
     *
     * @param v
     *            The new fontSize value
     */
    public void setFontSize(double v) {
        _FontSize = v;
    }

    /**
     * Sets the fillColor attribute of the Symbol object
     *
     * @param v
     *            The new fillColor value
     */
    public void setFillColor(Color v) {
        fillColor = new Color(v.getRed(), v.getGreen(), v.getBlue(), alpha);
    }

    /**
     * Sets the edgeColor attribute of the Symbol object
     *
     * @param v
     *            The new edgeColor value
     */
    public void setEdgeColor(Color v) {
        _EdgeColor = v;
    }

    /**
     * Sets the textColor attribute of the Symbol object
     *
     * @param v
     *            The new textColor value
     */
    public void setTextColor(Color v) {
        _TextColor = v;
    }

    /**
     * Sets the text attribute of the Symbol object
     *
     * @param v
     *            The new text value
     */
    public void setText(String v) {
        _Text = v;
    }

    /**
     * Sets the visible attribute of the Symbol object
     *
     * @param v
     *            The new visible value
     */
    @Override
    public void setVisible(boolean v) {
        _Visible = v;
    }

    /**
     * Sets the textVisible attribute of the Symbol object
     *
     * @param v
     *            The new textVisible value
     */
    public void setTextVisible(boolean v) {
        _TextVisible = v;
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
            graphics.setXORMode(Color.white);
        }
        Graphics2D g2d = (Graphics2D) graphics;
        g2d.clip(owner.getPlotRegion().getRect());
        render(graphics, owner);
        if (canDragX) {
            _Xcenter += dx;
        }
        if (canDragY) {
            _Ycenter += dy;
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
        StringBuilder s = new StringBuilder(" Symbol at (");
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(5);
        s.append(f.format(_Xcenter));
        s.append(", ");
        s.append(f.format(_Ycenter));
        s.append(')');
        if (_Text != null && _Text.length() > 0) {
            s.append(" Text is: ").append(_Text);
        }
        return s.toString();
    }

    /**
     * render the Symbol's text to the supplied graphics context
     *
     * @param g
     *            The graphics context
     * @param x
     *            The symbol center X-coordinate in pixels
     * @param y
     *            The symbol center Y-coordinate in pixels
     * @param size
     *            The size of the symbol in pixels
     */
    protected void PaintSymbolTextOnCanvas(Graphics g, int x, int y, int size) {
        Graphics2D g2d = (Graphics2D) g;
        // Save old color
        Color oldColor = g2d.getColor();

        // Create new font and color
        g2d.setColor(_TextColor);

        // Layout and render text
        TextLayout textTl = new TextLayout(_Text, new Font("Arial", Font.PLAIN, (int) _FontSize), new FontRenderContext(null, false, false));
        float advance = textTl.getAdvance() - 3;
        // Don't know why, but I need this correction factor of 3 for centering
        //float ascent = textTl.getAscent();
        double height = textTl.getBounds().getHeight();
        float xpos = (x - advance / 2);
        float ypos = (float) (y + size / 2 + height + 2.0F);
        // move top two points down from bottom of symbol
        textTl.draw(g2d, xpos, ypos);
        AffineTransform textAt = new AffineTransform();
        textAt.translate(xpos, ypos);
        Shape s = textTl.getOutline(textAt);
        addToRegion(s.getBounds2D());

        // restore old color
        g2d.setColor(oldColor);
    }

    public void setXcoordPinned(boolean v) {
        _PinXcoord = v;
    }

    public void setYcoordPinned(boolean v) {
        _PinYcoord = v;
    }

    public void setXcoordIntValue(int v) {
        _XpinCenter = v;
    }

    public void setYcoordIntValue(int v) {
        _YpinCenter = v;
    }

    /**
     * The X-coordinate of the Symbol center in real-world coordinates
     */
    protected double _Xcenter;
    /**
     * The Y-coordinate of the Symbol center in real-world coordinates
     */
    protected double _Ycenter;
    /**
     * The size of the symbol in millimeters
     */
    protected double _SymbolSize;
    /**
     * The fill color of the symbol
     */
    private Color fillColor;
    /**
     * The edge color of the symbol
     */
    protected Color _EdgeColor;
    /**
     * The color of the symbol text
     */
    protected Color _TextColor;
    /**
     * The text to display with the symbol
     */
    protected String _Text;
    /**
     * The visibility of the symbol
     */
    protected boolean _Visible;
    /**
     * The visibility of the associated text
     */
    protected boolean _TextVisible;
    /**
     * The font size of the associated text
     */
    protected double _FontSize;
    protected boolean _PinXcoord = false;
    protected boolean _PinYcoord = false;
    protected int _XpinCenter = 0;
    protected int _YpinCenter = 0;
    protected Color edgeRenderColor;
    private int alpha = 255;
    private Vector<SymbolTag> tags = new Vector<SymbolTag>();

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
        _EdgeColor = new Color(_EdgeColor.getRed(), _EdgeColor.getGreen(), _EdgeColor.getBlue(), alpha);
        setFillColor(getFillColor()); // Sets the alpha

    }

    public void addSymbolTag(SymbolTag tag) {
        getTags().add(tag);
    }

    public Vector<SymbolTag> getTags() {
        return tags;
    }
}
