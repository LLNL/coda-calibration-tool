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
package llnl.gnem.core.polygon;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import llnl.gnem.core.util.FileInputArrayLoader;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.util.Geometry.GeodeticCoordinate;
import llnl.gnem.core.util.Geometry.NEZCoordinate;

/**
 * Encapsulates a Vector of Vertices describing a polygon and provides
 * operations to rapidly determine whether a point is inside the polygon.
 *
 * @author Doug Dodge
 */
public class BasePolygon implements Serializable {

    private static final int DEGREES_IN_CIRCLE = 360;
    private Vertex[] vertices;
    private CartesianPolygon cartesianPolygon;
    private GeodeticCoordinate coordOrigin;
    private boolean isSpansDateLine = false;
    // Need this serialVersionUId in order to be compatible with serialized
    // Polygons stored on server for map display.
    static final long serialVersionUID = 3801579673383872402L;
    
    public void scale(double factor) {
        cartesianPolygon.scale(factor);
        ArrayList<Vector3D> scaled = cartesianPolygon.getVertices();
        if( scaled.size() != vertices.length){
            throw new IllegalStateException("Size of vertex collection changed after scaling!");
        }
        for ( int j = 0; j < vertices.length; ++j ){
            NEZCoordinate tmp = new NEZCoordinate(scaled.get(j));
            GeodeticCoordinate v = EModel.getGeodeticCoords(coordOrigin, tmp);
            Vertex vv = new Vertex(v.getLat(), v.getLon());
            vertices[j] = vv;
        }
    }

    /**
     * Constructor for the Polygon object
     *
     * @param P A Vector of Vertex objects describing the polygon's perimeter.
     */
    public BasePolygon(Vertex[] P) {
        vertices = P.clone();
        setLocalCoords();
    }
    
    public BasePolygon(Vertex[] P, boolean isSpansDateline) {
        vertices = P.clone();
        this.isSpansDateLine = isSpansDateline;
        setLocalCoords();
    }
    
    public BasePolygon(final String polygonFileName) throws IOException {
        String[] lines = FileInputArrayLoader.fillStrings(polygonFileName);
        if (lines.length < 3) {
            throw new IllegalArgumentException("File: " + polygonFileName + " has too few points!");
        }
        
        vertices = new Vertex[lines.length];
        for (int j = 0; j < lines.length; ++j) {
            StringTokenizer st = new StringTokenizer(lines[j]);
            int num = st.countTokens();
            if (num != 2) {
                throw new IllegalStateException("Line should contain one lat and one lon. Instead line is: " + lines[j]);
            }
            String latstr = st.nextToken();
            String lonstr = st.nextToken();
            vertices[j] = new Vertex(Double.parseDouble(latstr), Double.parseDouble(lonstr));
        }
        setLocalCoords();
        
    }

    /**
     * Determine whether a point (given as a Vertex object) is inside the
 BasePolygon. Uses a method by Sedgewick, "Algorithms in C"
     *
     * @param t The point to be tested
     * @return true if the point is inside the BasePolygon
     */
    public boolean contains(Vertex t) {
        double lon = isSpansDateLine && t.getLon() < 0 ? DEGREES_IN_CIRCLE + t.getLon() : t.getLon();
        NEZCoordinate tLocal = EModel.getLocalCoords(coordOrigin, new GeodeticCoordinate(t.getLat(), lon, 0.0));
        return cartesianPolygon.contains(tLocal.toVector3D());
    }

    /**
     * Determine whether a point (given as a lat and a lon is inside the
 BasePolygon. Uses a method by Sedgewick, "Algorithms in C"
     *
     * @param x The latitude of the point to be tested
     * @param y The longitude of the point to be tested
     * @return true if the point is inside the BasePolygon
     */
    public boolean contains(double x, double y) {
        NEZCoordinate tLocal = EModel.getLocalCoords(coordOrigin, new GeodeticCoordinate(x, y, 0.0));
        return cartesianPolygon.contains(tLocal.toVector3D());
    }

    /**
     * Gets the latitude of the most Southerly Vertex in the BasePolygon. For rapid
 selection of candidate points from the database, it is convenient to get
 the lat and lon extrema of the BasePolygon to form a query that returns all
 the points within the smallest box that encloses the BasePolygon.
     *
     * @return The minLat value
     */
    public double getMinLat() {
        double minLat = Double.MAX_VALUE;
        for (Vertex vertice : vertices) {
            double lat = vertice.getLat();
            if (lat < minLat) {
                minLat = lat;
            }
        }
        return minLat;
    }

    /**
     * Gets the latitude of the most Northerly Vertex in the BasePolygon. For rapid
 selection of candidate points from the database, it is convenient to get
 the lat and lon extrema of the BasePolygon to form a query that returns all
 the points within the smallest box that encloses the BasePolygon.
     *
     * @return The maxLat value
     */
    public double getMaxLat() {
        double maxLat = -Double.MAX_VALUE;
        for (Vertex vertice : vertices) {
            double lat = vertice.getLat();
            if (lat > maxLat) {
                maxLat = lat;
            }
        }
        return maxLat;
    }

    /**
     * Gets the latitude of the most Westerly Vertex in the BasePolygon. For rapid
 selection of candidate points from the database, it is convenient to get
 the lat and lon extrema of the BasePolygon to form a query that returns all
 the points within the smallest box that encloses the BasePolygon.
     *
     * @return The minLon value
     */
    public double getMinLon() {
        double minLon = Double.MAX_VALUE;
        for (Vertex vertice : vertices) {
            double lon = vertice.getLon();
            if (lon < minLon) {
                minLon = lon;
            }
        }
        return minLon;
    }

    /**
     * Gets the latitude of the most Easterly Vertex in the BasePolygon. For rapid
 selection of candidate points from the database, it is convenient to get
 the lat and lon extrema of the BasePolygon to form a query that returns all
 the points within the smallest box that encloses the BasePolygon.
     *
     * @return The maxLon value
     */
    public double getMaxLon() {
        double maxLon = -Double.MAX_VALUE;
        for (Vertex vertice : vertices) {
            double lon = vertice.getLon();
            if (lon > maxLon) {
                maxLon = lon;
            }
        }
        return maxLon;
    }

    /**
     * Make a BasePolygon that approximates a circle. The approximation will be good
     * for small radii near the Equator and poor near the poles.
     *
     * @param center The center of the BasePolygon
     * @param kmRadius The radius of the BasePolygon in km
     * @param npts The number of points in the polygon.
     * @return The generated BasePolygon
     */
    public static BasePolygon makeCirclePolygon(Vertex center, double kmRadius, int npts) {
        double lat = center.getLat();
        double EarthRadius = EModel.getEarthRadius(lat);
        double DegDistance = Math.toDegrees(kmRadius / EarthRadius);
        return makeCirclePolygon_Deg(center, DegDistance, npts);
    }
    
    public static BasePolygon makeCirclePolygon_Deg(Vertex center, double DegDistance, int npts) {
        Vertex[] vertices = EModel.smallCircle(center, DegDistance, npts);
        return new BasePolygon(vertices);
    }

    /**
     * Gets the vertices of the BasePolygon object
     *
     * @return The array of Vertex objects
     */
    public Vertex[] getVertices() {
        return vertices.clone();
    }
    
    private void setLocalCoords() {
        double latAvg = 0;
        double lonAvg = 0;
        for (Vertex v : vertices) {
            latAvg += v.getLat();
            if (isSpansDateLine && v.getLon() < 0) {
                lonAvg += (DEGREES_IN_CIRCLE + v.getLon());
            } else {
                lonAvg += v.getLon();
            }
            
        }
        coordOrigin = new GeodeticCoordinate(latAvg / vertices.length, lonAvg / vertices.length, 0.0);
        ArrayList<NEZCoordinate> vLocal = EModel.getLocalCoords(coordOrigin, vertices);
        cartesianPolygon = new CartesianPolygon(vLocal);
    }
}
