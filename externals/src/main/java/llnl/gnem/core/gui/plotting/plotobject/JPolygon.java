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
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import llnl.gnem.core.gui.plotting.JBasicPlot;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;
import llnl.gnem.core.gui.plotting.transforms.CoordinateTransform;
import llnl.gnem.core.polygon.BasePolygon;
import llnl.gnem.core.polygon.Vertex;

/**
 * Created by: dodge1
 * Date: Jan 6, 2005
 */
public class JPolygon extends PlotObject {
    private Point2D[] vertices;

    private Color FillColor = Color.blue;
    private Color EdgeColor = Color.black;
    private boolean edgeVisible = true;
    private boolean interiorFilled = true;
    private float width = 1;
    private PenStyle penStyle = PenStyle.SOLID;
    private String name = "";

    protected JPolygon() {
        vertices = new Point2D[0];
    }

    public JPolygon(BasePolygon poly) {
        Vertex[] vert = poly.getVertices();
        if (vert[0].equals(vert[vert.length - 1])) {
            vertices = new Point2D[vert.length];
            int j = 0;
            for (Vertex v : vert) {
                vertices[j++] = new Point2D.Double(v.getLat(), v.getLon());
            }
        } else {
            vertices = new Point2D[vert.length + 1];
            int j = 0;
            for (Vertex v : vert) {
                vertices[j++] = new Point2D.Double(v.getLat(), v.getLon());
            }
            vertices[j] = new Point2D.Double(vert[0].getLat(), vert[0].getLon());
        }
    }

    protected void setVertices(Point2D[] points) {
        vertices = points;
    }

    /**
     * The constructor for BasePolygon
     *
     * @param points An array of Vertex objects that collectively define the polygons
     *               vertices. This constructor creates a polygon with a blue interior and black edges.
     */
    public JPolygon(Point2D[] points) {
        vertices = points;
    }

    /**
     * Sets the color of the polygon interior.
     *
     * @param c The new interior color
     */
    public void setFillColor(Color c) {
        FillColor = c;
    }

    public void setEdgeColor(Color c) {
        EdgeColor = c;
    }

    /**
     * Renders this polygon with the current graphics context. As currently implemented,
     * if any part of the polygon is outside the axis limits, the polygon will not be rendered.
     * This behavior was chosen because for Azimuthal equal-area Coordinate Transforms, objects
     * at or near 180 degrees from the origin are distorted to fill the entire plot region.
     * If only the offending vertices are clipped, the polygon may become unrecognizable in shape.
     *
     * @param g     The graphics context on which to render this polygon.
     * @param owner The JBasicPlot on which this polygon is being rendered.
     */
    @Override
    public void render(Graphics g, JBasicPlot owner) {
        if (!isVisible())
            return;

        Rectangle rect = owner.getPlotRegion().getRect();
        Graphics2D g2d = (Graphics2D) g;
        g2d.clip(rect);
        region.clear();
        GeneralPath path = new GeneralPath();
        CoordinateTransform coordTransform = owner.getCoordinateTransform();
        Coordinate coord = new Coordinate(0, 0);
        coord.setWorldC1(vertices[0].getX());
        coord.setWorldC2(vertices[0].getY());
        coordTransform.WorldToPlot(coord);
        path.moveTo((float) coord.getX(), (float) coord.getY());
        for (int j = 1; j < getVertices().length; ++j) {
            coord.setWorldC1(vertices[j].getX());
            coord.setWorldC2(vertices[j].getY());
            coordTransform.WorldToPlot(coord);
            path.lineTo((float) coord.getX(), (float) coord.getY());

        }

        if (isInteriorFilled()) {
            g2d.setColor(FillColor);
            g2d.fill(path);
        }
        if (isEdgeVisible()) {
            g2d.setColor(EdgeColor);
            g2d.setStroke( getPenStyle().getStroke( width ) );
            g2d.draw(path);
        }
        addToRegion(path);

    }

    @Override
    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {

    }

    public boolean isEdgeVisible() {
        return edgeVisible;
    }

    public void setEdgeVisible(boolean edgeVisible) {
        this.edgeVisible = edgeVisible;
    }

    public boolean isInteriorFilled() {
        return interiorFilled;
    }

    public void setInteriorFilled(boolean interiorFilled) {
        this.interiorFilled = interiorFilled;
    }

    public void setStrokeWidth(float width) {
        this.width = width;
    }

    public Vertex[] getVertices()
    {
        Vertex[] result = new Vertex[vertices.length];
        for( int j = 0; j < vertices.length; ++j ){
            result[j] = new Vertex(vertices[j].getX(), vertices[j].getY() );
        }
        return result;
    }

    public PenStyle getPenStyle()
    {
        return penStyle;
    }

    public void setPenStyle(PenStyle penStyle)
    {
        this.penStyle = penStyle;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }
}
