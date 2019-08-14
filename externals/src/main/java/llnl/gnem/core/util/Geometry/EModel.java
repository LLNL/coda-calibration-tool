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
package llnl.gnem.core.util.Geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import llnl.gnem.core.geom.GeographicCoordinate;
import llnl.gnem.core.polygon.Vertex;
import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import net.sf.geographiclib.GeodesicLine;
import net.sf.geographiclib.GeodesicMask;

/**
 * A class that provides static methods for calculating distances and azimuths
 * on a spherical Earth or a WGS84 ellipsoidal Earth.
 *
 * @author Doug Dodge
 */
public class EModel {

    private static final double HALF_CIRCLE = 180.0;
    public static final double DEGREES_TO_RADIANS = Math.PI / HALF_CIRCLE;
    public static final double KM_PER_NAUTICAL_MILE = 1.852;
    public static final int MINUTES_PER_DEGREE = 60;
    public static final double RADIANS_TO_DEGREES = HALF_CIRCLE / Math.PI;
    private static final double EPSILON = 1.7453e-008;
    private static final double IASPEIRadius = 6371000.0;
    private static final double KILOMETERS_PER_DEGREE = (Math.PI / HALF_CIRCLE) * (IASPEIRadius / 1000);
    private static final double LOW_PREC_PI = 3.14159265359;
    private static final double WGS84Radius = 6378137.0;
    private static final double flat = 1. / 2.98257223563e02;
    private static final double log10v = Math.log(10.0);
    private static final double polarRadius = 6356752.314;
    private static final double twopi = 2.0 * Math.PI;
    private static final double a = WGS84Radius;
    private static final double f = flat;
    private static final double b = a * (1 - f);
    private static final double e2 = 1 - b * b / a / a;
    private static final double e = Math.sqrt(e2);
    private static final double ePrime2 = f * (2 - f) / (1 - f) / (1 - f);
    private static final double ePrime = Math.sqrt(ePrime2);

    public static GeodeticCoordinate enu2Geodetic(GeodeticCoordinate refg, ENUCoordinate pos) {
        ECEFCoordinate tmp = enu2ecef(pos, refg);
        return ecef2geodetic(tmp);
    }

    private static ENUCoordinate nez2enu(NEZCoordinate other) {
        return new ENUCoordinate(other.getyEastKm() * 1000, other.getxNorthKm() * 1000, -other.getzDownKm() * 1000);
    }

    static double getSeparationMeters(GeodeticCoordinate c1, GeodeticCoordinate c2) {
        ECEFCoordinate ec1 = geodetic2ecef(c1);
        ECEFCoordinate ec2 = geodetic2ecef(c2);
        return ec1.getSeparationMeters(ec2);
    }

    static double getDistance(GeodeticCoordinate pt1, GeodeticCoordinate pt2) {
        return Geodesic.WGS84.Inverse(pt1.getLat(), pt1.getLon(), pt2.getLat(), pt2.getLon()).s12 / 1000;
    }

    private EModel() {
    }

    /**
     * Gets the angle in degrees between two vectors represented as azimuths in
     * degrees clockwise from North.
     *
     * @param azimuth1
     *            Azimuth of the first vector in degrees clockwise from North.
     * @param azimuth2
     *            Azimuth of the second vector in degrees clockwise from North.
     * @return The angle between the two azimuths in degrees.
     */
    public static double getAngleBetweenAzimuths(double azimuth1, double azimuth2) {
        double theta1 = (90.0 - azimuth1) * DEGREES_TO_RADIANS;
        double theta2 = (90.0 - azimuth2) * DEGREES_TO_RADIANS;
        double dotProduct = getDotProduct(Math.cos(theta1), Math.sin(theta1), Math.cos(theta2), Math.sin(theta2));
        return Math.acos(dotProduct) * RADIANS_TO_DEGREES;
    }

    public static double getAngleInRadians(double v1x, double v1y, double v2x, double v2y) {
        double dotProduct = getDotProduct(v1x, v1y, v2x, v2y);
        double normV1 = getL2Norm(v1x, v1y);
        if (normV1 == 0) {
            throw new IllegalArgumentException("Cannot compute angle. v1 has 0 length.");
        }
        double normV2 = getL2Norm(v2x, v2y);
        if (normV2 == 0) {
            throw new IllegalArgumentException("Cannot compute angle. v2 has 0 length.");
        }
        return Math.acos(dotProduct / normV1 / normV2);
    }

    /**
     * Gets the azimuthal gap of an origin solution given a Vector of
     * event-station azimuths.
     *
     * @param az
     *            The Vector of event-station azimuths
     * @return The azgap value
     */
    public static double getAzGapFromAzVector(List<Double> az) {
        Collections.sort(az);
        double maxGap = 0.0;
        double dif;
        int nsta = az.size();
        for (int i = 1; i < nsta; ++i) {
            if ((dif = az.get(i) - az.get(i - 1)) > maxGap) {
                maxGap = dif;
            }
        }
        if ((dif = az.get(0) + 360 - az.get(nsta - 1)) > maxGap) {
            maxGap = dif;
        }
        return maxGap;
    }

    /**
     * Gets the azimuthal gap of an origin solution given the epicenter location
     * and the locations of the stations used in the origin solution.
     *
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @param stla
     *            A Vector of station latitudes
     * @param stlo
     *            A Vector of station longitudes
     * @return The azgap value
     */
    public static double getAzgap(double evla, double evlo, List<Double> stla, List<Double> stlo) {
        int nsta = stla.size();
        if (nsta < 1 || nsta != stlo.size()) {
            return -1.0;
        }
        List<Double> az = new ArrayList<>();

        //Calculate event-station azimuths for all stations...
        for (int j = 0; j < nsta; ++j) {
            double Stla = stla.get(j);
            double Stlo = stlo.get(j);
            GeodesicData g = Geodesic.WGS84.Inverse(Stla, Stlo, evla, evlo);
            az.add(g.azi2);
        }
        return getAzGapFromAzVector(az);
    }

    /**
     * Gets the azimuth of an event relative to a station on a spherical Earth.
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The azimuth of the event relative to the station
     */
    public static double getAzimuth(double stla, double stlo, double evla, double evlo) {
        GeodesicData g = Geodesic.WGS84.Inverse(stla, stlo, evla, evlo);
        return g.azi1 >= 0 ? g.azi1 : 360 + g.azi1;
    }

    /**
     * Gets the azimuth of an event relative to a station using the WGS84 Earth
     * model.
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The azimuth of the event relative to the station
     */
    public static double getAzimuthWGS84(double stla, double stlo, double evla, double evlo) {
        double result = Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).azi1;
        return result >= 0 ? result : 360 + result;
    }

    /**
     * Gets the back azimuth in degrees (azimuth from event to station) on a
     * spherical Earth.
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The back azimuth value
     */
    public static double getBAZ(double stla, double stlo, double evla, double evlo) {
        double result = Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).azi1;
        return result >= 0 ? result : 360 + result;
    }

    /**
     * Gets the back azimuth in degrees (azimuth from event to station) using
     * the WGS84 Earth model
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The back azimuth value
     */
    public static double getBAZWGS84(double stla, double stlo, double evla, double evlo) {
        double result = Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).azi1;
        return result >= 0 ? result : 360 + result;
    }

    /**
     * Gets the change in longitude in degrees given a latitude and departure in
     * km
     *
     * @param lat
     *            The latitude at which to compute.
     * @param departureKm
     *            The departure in km.
     * @return the change in longitude.
     */
    public static double getDegreesFromDeparture(double lat, double departureKm) {
        return Math.abs(departureKm / (KM_PER_NAUTICAL_MILE * MINUTES_PER_DEGREE * Math.cos(Math.toRadians(lat))));
    }

    public static double getDegreesPerKilometer() {
        return 1 / KILOMETERS_PER_DEGREE;
    }

    /**
     * Computes the delay in passage of a plane wave from the reference element
     * of an array to an observation element of the array. The array is assumed
     * to be small enough in aperature that the plane-wave approximation is
     * valid and that errors introduced by assigning a local Cartesian
     * coordinate system are small.
     *
     * @param sourcePosition
     *            The position of the source of the plane wave (used to
     *            determine the back azimuth.)
     * @param referenceElementPosition
     *            The position of the reference element of the array.
     * @param observationElementPosition
     *            The position of the element for which the delay is to be
     *            computed.
     * @param apparentVelocity
     *            The apparent velocity of the plane wave in km/sec.
     * @return The delay in seconds between when the wavefront crosses the
     *         reference element and when it crosses the observation element.
     */
    public static double getDelay(GeodeticCoordinate sourcePosition, GeodeticCoordinate referenceElementPosition, GeodeticCoordinate observationElementPosition, double apparentVelocity) {
        NEZCoordinate localCoords = EModel.getLocalCoords(referenceElementPosition, observationElementPosition);
        double baz = EModel.getBAZWGS84(referenceElementPosition.getLat(), referenceElementPosition.getLon(), sourcePosition.getLat(), sourcePosition.getLon());
        double az = baz - 180;
        double sx = Math.cos(az * DEGREES_TO_RADIANS) / apparentVelocity;
        double sy = Math.sin(az * DEGREES_TO_RADIANS) / apparentVelocity;
        double dtx = sx * localCoords.getxNorthKm();
        double dty = sy * localCoords.getyEastKm();
        return dtx + dty;
    }

    /**
     * Gets the distance in degrees of an event from a station on a spherical
     * Earth.
     *
     * @param stla
     *            Station latitude.
     * @param stlo
     *            Station longitude.
     * @param evla
     *            Event latitude.
     * @param evlo
     *            Event longitude.
     * @return The distance in degrees.
     */
    public static double getDelta(double stla, double stlo, double evla, double evlo) {
        return Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).a12;
    }

    /**
     * Gets the distance in degrees of an event from a station on a spherical
     * Earth.
     *
     * @param sta
     *            Vertex object containing station coordinates.
     * @param event
     *            Vertex object containing the event epicentral coordinates.
     * @return The distance in degrees.
     */
    public static double getDelta(Vertex sta, Vertex event) {
        return getDelta(sta.getLat(), sta.getLon(), event.getLat(), event.getLon());
    }

    /**
     * Gets the distance in degrees of an event from a station using the WGS84
     * Earth model
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The distance in degrees
     */
    public static double getDeltaWGS84(double stla, double stlo, double evla, double evlo) {
        return Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).a12;
    }

    /**
     * Gets the distance in degrees of two points on the surface of the Earth
     * using the WGS84 Earth model.
     *
     * @param point1
     *            The first point.
     * @param point2
     *            The second point.
     * @return The distance in degrees.
     */
    public static double getDeltaWGS84(Vertex point1, Vertex point2) {
        return Geodesic.WGS84.Inverse(point1.getLat(), point1.getLon(), point2.getLat(), point2.getLon()).a12;
    }

    /**
     * Gets the distance in km along a line of constant latitude
     *
     * @param lat
     *            The latitude at which to compute.
     * @param delta
     *            The distance in degrees
     * @return the departure in km
     */
    public static double getDepartureKm(double lat, double delta) {
        return Math.abs(delta * KM_PER_NAUTICAL_MILE * MINUTES_PER_DEGREE * Math.cos(Math.toRadians(lat)));
    }

    /**
     * Gets the distance in km between two points on a spherical Earth.
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The distance in kilometers
     */
    public static double getDistance(double stla, double stlo, double evla, double evlo) {
        return Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).s12 / 1000;
    }

    /**
     * Gets the distance in km between two points on a spherical Earth model.
     *
     * @param pt1
     *            Vertex object representing first point.
     * @param pt2
     *            Vertex object representing second point.
     * @return The distance in km.
     */
    public static double getDistance(Vertex pt1, Vertex pt2) {
        return Geodesic.WGS84.Inverse(pt1.getLat(), pt1.getLon(), pt2.getLat(), pt2.getLon()).s12 / 1000;
    }

    /**
     * Gets the distance in km between two points using the WGS84 Earth model
     *
     * @param stla
     *            Station latitude
     * @param stlo
     *            Station longitude
     * @param evla
     *            Event latitude
     * @param evlo
     *            Event longitude
     * @return The distance in kilometers
     */
    public static double getDistanceWGS84(double stla, double stlo, double evla, double evlo) {
        return Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).s12 / 1000;
    }

    private static double getDotProduct(double v1x, double v1y, double v2x, double v2y) {
        return v1x * v2x + v1y * v2y;
    }

    /**
     * Gets the Earth Radius in km as a function of latitude in degrees
     *
     * @param latitude
     *            The latitude in degrees of the place where the radius is
     *            desired
     * @return The Earth Radius value in km
     */
    public static double getEarthRadius(double latitude) {
        double theta = (90 - latitude) * DEGREES_TO_RADIANS;
        double cosT = Math.cos(theta);
        double sinT = Math.sin(theta);
        double t1 = polarRadius * cosT;
        double t2 = WGS84Radius * sinT;
        return Math.sqrt(t1 * t1 + t2 * t2) / 1000;
    }

    /**
     * Gets the Event to Station azimuth on a spherical Earth model.
     *
     * @param event
     *            The epicentral Vertex.
     * @param sta
     *            The station Vertex.
     * @return The azimuth of the station relative to the event epicenter in
     *         degrees.
     */
    public static double getEsaz(Vertex event, Vertex sta) {
        double result = Geodesic.WGS84.Inverse(event.getLat(), event.getLon(), sta.getLat(), sta.getLon()).azi1;
        return result >= 0 ? result : 360 + result;
    }

    public static double getEsaz(double evla, double evlo, double stla, double stlo) {
        double result = Geodesic.WGS84.Inverse(evla, evlo, stla, stlo).azi1;
        return result >= 0 ? result : 360 + result;
    }

    /**
     * Gets the Event to Station azimuth using the WGS84 Earth model.
     *
     * @param event
     *            The epicentral Vertex.
     * @param sta
     *            The station Vertex.
     * @return The azimuth of the station relative to the event epicenter in
     *         degrees.
     */
    public static double getEsazWGS84(Vertex event, Vertex sta) {
        double result = Geodesic.WGS84.Inverse(event.getLat(), event.getLon(), sta.getLat(), sta.getLon()).azi1;
        return result >= 0 ? result : 360 + result;
    }

    public static GeodeticCoordinate getGeodeticCoords(GeodeticCoordinate coordOrigin, NEZCoordinate localCoords) {
        ENUCoordinate tmp = nez2enu(localCoords);
        return enu2Geodetic(coordOrigin, tmp);
    }

    /**
     * Returns a great-circle azimuth in degrees between two points specified by
     * their latitude and longitude in degrees.
     *
     * @param lat1
     *            Latitude of the first point in degrees.
     * @param lon1
     *            Longitude of the second point in degrees.
     * @param lat2
     *            Latitude of the second point in degrees.
     * @param lon2
     *            Longitude of the second point in degrees.
     * @return great-circle distance in degrees
     */
    public static double getGreatCircleAzimuth(double lat1, double lon1, double lat2, double lon2) {
        lat1 *= DEGREES_TO_RADIANS;
        lon1 *= DEGREES_TO_RADIANS;
        lat2 *= DEGREES_TO_RADIANS;
        lon2 *= DEGREES_TO_RADIANS;

        // Identify those cases where a pole is a starting
        // point or a destination, and those cases where it is not
        if (lat1 >= Math.PI / 2 - EPSILON) {
            return 180;
        } else if (lat1 <= EPSILON - Math.PI / 2) {
            return 0.0;
        } else if (lat2 >= Math.PI / 2 - EPSILON) {
            return 0.0;
        } else if (lat2 <= EPSILON - Math.PI / 2) {
            return 180;
        } else {
            double dlong = lon2 - lon1;
            double term1 = Math.sin(dlong) * Math.cos(lat2);
            double term2 = Math.cos(lat1) * Math.sin(lat2);
            double term3 = Math.sin(lat1) * Math.cos(lat2) * Math.cos(dlong);
            double result = Math.atan2(term1, term2 - term3) * RADIANS_TO_DEGREES;
            return result >= 0 ? result : result + 360;
        }
    }

    /**
     * Returns a great-circle distance in degrees between two points specified
     * by their latitude and longitude in degrees.
     *
     * @param lat1
     *            Latitude of the first point in degrees.
     * @param lon1
     *            Longitude of the second point in degrees.
     * @param lat2
     *            Latitude of the second point in degrees.
     * @param lon2
     *            Longitude of the second point in degrees.
     * @return great-circle distance in degrees
     */
    public static double getGreatCircleDelta(double lat1, double lon1, double lat2, double lon2) {
        double delta1 = lat1 * DEGREES_TO_RADIANS;
        double lambda1 = lon1 * DEGREES_TO_RADIANS;
        double delta2 = lat2 * DEGREES_TO_RADIANS;
        double lambda2 = lon2 * DEGREES_TO_RADIANS;
        double term1 = Math.cos(delta1) * Math.cos(delta2) * Math.cos(lambda1 - lambda2);
        double term2 = Math.sin(delta1) * Math.sin(delta2);
        return Math.acos(term1 + term2) * RADIANS_TO_DEGREES;
    }

    public static double getKilometersPerDegree() {
        return KILOMETERS_PER_DEGREE;
    }

    private static double getL2Norm(double vx, double vy) {
        return Math.sqrt(vx * vx + vy * vy);
    }

    public static ArrayList<NEZCoordinate> getLocalCoords(GeodeticCoordinate coordOrigin, Vertex[] vertices) {
        ArrayList<GeodeticCoordinate> vertexVector = new ArrayList<>();
        for (Vertex v : vertices) {
            GeodeticCoordinate gc = new GeodeticCoordinate(v.getLat(), v.getLon(), 0.0);
            vertexVector.add(gc);
        }
        return getLocalCoords(coordOrigin, vertexVector);
    }

    public static ArrayList<NEZCoordinate> getLocalCoords(GeodeticCoordinate coordOrigin, ArrayList<GeodeticCoordinate> vertices) {
        ArrayList<NEZCoordinate> resultVector = new ArrayList<>();
        for (GeodeticCoordinate position : vertices) {
            NEZCoordinate nez = getLocalCoords(coordOrigin, position);
            resultVector.add(nez);
        }
        return resultVector;
    }

    public static NEZCoordinate getLocalCoords(GeodeticCoordinate coordOrigin, GeodeticCoordinate position) {
        ENUCoordinate enu = geodetic2enu(coordOrigin, position);
        return enu2nez(enu);
    }

    public static NEZCoordinate enu2nez(ENUCoordinate enu) {
        double northKm = enu.getyNorthMeters() / 1000;
        double eastKm = enu.getxEastMeters() / 1000;
        double zDownKm = -enu.getzUpMeters() / 1000;
        return new NEZCoordinate(northKm, eastKm, zDownKm);
    }

    private static double getLocalRadius(double xlatrad, double xdepc) {
        double localRadius;
        double r1 = Math.sin(xlatrad) * WGS84Radius;
        double r2 = Math.cos(xlatrad) * polarRadius;
        localRadius = WGS84Radius * polarRadius / Math.sqrt(r1 * r1 + r2 * r2) / 1000.0 - xdepc;
        return localRadius;
    }

    /**
     * A convience method to calculate the Local Radius based on the latitude in
     * degrees
     *
     * @param latitude
     *            - in DEGREES
     * @param depth
     *            - in km
     * @return the local radius at the given latitude
     */
    public static double getLocalRadiusFromLatitude(double latitude, double depth) {
        return getLocalRadius(latitude * DEGREES_TO_RADIANS, depth);
    }

    public static double getSeaz(double evla, double evlo, double stla, double stlo) {
        double result = Geodesic.WGS84.Inverse(stla, stlo, evla, evlo).azi1;
        return result >= 0 ? result : 360 + result;
    }

    /**
     * Gets the station to event azimuth.
     *
     * @param event
     *            position of the event epicenter.
     * @param sta
     *            position of the station.
     * @return the station-to-event azimuth in degrees.
     */
    public static double getSeaz(Vertex event, Vertex sta) {
        double result = Geodesic.WGS84.Inverse(sta.getLat(), sta.getLon(), event.getLat(), event.getLon()).azi1;
        return result >= 0 ? result : 360 + result;
    }

    public static double getWGS84RadiusKm() {
        return WGS84Radius / 1000.0;
    }

    public static Vertex reckonWGS84(double latIn, double lonIn, double deltaIn, double azimuthIn) {
        GeodesicLine aLine = new GeodesicLine(Geodesic.WGS84, latIn, lonIn, azimuthIn);
        GeodesicData data = aLine.ArcPosition(deltaIn);
        return new Vertex(data.lat2, data.lon2);
    }

    /**
     * Calculate point at azimuth and distance from another point.
     * <p>
     * Returns a Vertex at great-circle distance delta and azimuth Azimuth.
     * <p>
     *
     * @param latIn
     *            latitude in degrees of start point
     * @param deltaIn
     *            great-circle distance between points in degrees
     * @param azimuthIn
     *            east of north (-180 LTEQ Azimuth LTEQ 180 )
     * @param lonIn
     *            Longitude of the start point
     * @return Vertex of point reckoned to
     */
    public static Vertex reckon(double latIn, double lonIn, double deltaIn, double azimuthIn) {
        double lat = latIn * DEGREES_TO_RADIANS;
        double lon = lonIn * DEGREES_TO_RADIANS;
        double azimuth = azimuthIn * DEGREES_TO_RADIANS;
        double delta = deltaIn * DEGREES_TO_RADIANS;
        double coslat = Math.cos(lat);
        double sinlat = Math.sin(lat);
        double cosAz = Math.cos(azimuth);
        double sinAz = Math.sin(azimuth);
        double sinc = Math.sin(delta);
        double cosc = Math.cos(delta);
        double tmpLat = RADIANS_TO_DEGREES * (Math.asin(sinlat * cosc + coslat * sinc * cosAz));
        double tmpLon = RADIANS_TO_DEGREES * (Math.atan2(sinc * sinAz, coslat * cosc - sinlat * sinc * cosAz) + lon);
        if (tmpLon > 180) {
            tmpLon -= 360;
        }
        if (tmpLon < -180) {
            tmpLon += 360;
        }

        return new Vertex(tmpLat, tmpLon);
    }

    /**
     * Returns an array of Vertex objects defining points on a small circle of
     * radius radius centered at the point center. The number of Vertex objects
     * returned is equal to npts. TODO note as written this returns (npts + 1)
     * Vertices
     *
     * @param center
     *            A Vertex object defining the center latitude-longitude of the
     *            small circle.
     * @param radius
     *            The radius of the small circle in degrees.
     * @param npts
     *            The number of points to create.
     * @return An array of Vertex objects defining the requested small circle.
     */
    public static Vertex[] smallCircle(Vertex center, double radius, int npts) {
        if (npts < 4) {
            npts = 4;
        }
        if (radius > 90) {
            radius = 90;
        }
        Vertex[] vertices = new Vertex[npts + 1];
        double dtheta = 360.0 / npts;
        double lat = center.getLat();
        double lon = center.getLon();
        for (int j = 0; j <= npts; ++j) // TODO note as written this returns (npts + 1) Vertices  the first and last being identical
        {
            double azimuth = j * dtheta;
            vertices[j] = EModel.reckon(lat, lon, radius, azimuth);
        }
        return vertices;
    }

    /**
     * Computes a vector of waypoints (including the start and end) for a
     * great-circle track defined by its start and end points.
     *
     * @param startLat
     *            The starting point latitude in degrees.
     * @param startLon
     *            The starting point longitude in degrees.
     * @param endLat
     *            The ending point latitude in degrees.
     * @param endLon
     *            The ending point longitude in degrees.
     * @param npts
     *            The number of desired points in the track.
     * @return The Vector of Vertex objects that define the track.
     */
    public static ArrayList<Vertex> track(double startLat, double startLon, double endLat, double endLon, int npts) {
        if (npts < 3) {
            npts = 3;
        }

        ArrayList<Vertex> result = new ArrayList<>();
        GeodesicLine aLine = Geodesic.WGS84.InverseLine(startLat, startLon, endLat, endLon);
        double arcDist = aLine.Distance();
        double ds = arcDist / (npts);
        for (int i = 0; i <= npts; ++i) {
            GeodesicData g = aLine.Position(i * ds, GeodesicMask.LATITUDE | GeodesicMask.LONGITUDE);
            result.add(new Vertex(g.lat2, g.lon2));
        }
        return result;
    }

    /*
     * function to compute a great circle arc on a sphere
     *
     * after the fortran routine by Dave Harris to do the same.
     *
     */
    public static void getGreatCircleArc(GeographicCoordinate coord1, GeographicCoordinate coord2, double az, double dist, double[] lats, double[] lons, int npts) {

        double earth_radius = WGS84Radius / 1000;
        double dl = dist / (npts - 1);

        double theta = Math.toRadians(coord1.getLat());
        double ct = Math.cos(theta);
        double st = Math.sin(theta);

        double phi = Math.toRadians(coord1.getLon());
        double cp = Math.cos(phi);
        double sp = Math.sin(phi);

        double bearing = Math.toRadians(az);
        double cb = Math.cos(bearing);
        double sb = Math.sin(bearing);

        lats[0] = coord1.getLat();
        lons[0] = coord1.getLon();
        int d = 0;
        for (int i = 1; i < npts - 1; i++) {
            d += dl;
            double delta = (d / earth_radius);
            double cd = Math.cos(delta);
            double sd = Math.sin(delta);
            double ez = cd * st + sd * cb * ct;
            double ey = cd * ct * cp + sd * (-cb * st * cp - sb * sp);
            double ex = cd * ct * sp + sd * (-cb * st * sp + sb * cp);

            lats[i] = (float) (Math.toDegrees(Math.atan2(ez, Math.sqrt(ex * ex + ey * ey))));
            lons[i] = (float) (Math.toDegrees(Math.atan2(ex, ey)));
        }
        lats[npts - 1] = coord2.getLat();
        lons[npts - 1] = coord2.getLon();
    }

    public static double geodeticLatToGeocentricLat(double lat, double heightMeters) {
        double phi = Math.toRadians(lat);
        double sinPhi = Math.sin(phi);
        double N = a / Math.sqrt(1 - e2 * sinPhi * sinPhi);
        double tanPhiP = N * (1 - f) * (1 - f) / (N + heightMeters) * Math.tan(phi);
        return Math.toDegrees(Math.atan(tanPhiP));
    }

    public static ECEFCoordinate geodetic2ecef(GeodeticCoordinate coord) {
        double lat = coord.getLat();
        double lon = coord.getLon();
        double h = -coord.getDepthKm() * 1000;
        double phi = Math.toRadians(lat);
        double lambda = Math.toRadians(lon);
        double sinPhi = Math.sin(phi);
        double cosphi = Math.cos(phi);
        double N = a / Math.sqrt(1 - e2 * sinPhi * sinPhi);
        double X = (N + h) * cosphi * Math.cos(lambda);
        double Y = (N + h) * cosphi * Math.sin(lambda);
        double Z = (N * (1 - e2) + h) * sinPhi;
        return new ECEFCoordinate(X, Y, Z);
    }

    public static GeodeticCoordinate ecef2geodetic(ECEFCoordinate coord) {
        return EModel.ecef2geodetic(coord.getX(), coord.getY(), coord.getZ());
    }

    public static GeodeticCoordinate ecef2geodetic(double X, double Y, double Z) {
        double r = Math.sqrt(X * X + Y * Y);
        double E2 = a * a - b * b;
        double F = 54 * b * b * Z * Z;
        double G = r * r + (1 - e2) * Z * Z - e2 * E2;
        double C = e2 * e2 * F * r * r / G / G / G;
        double S = Math.pow(1 + C + Math.sqrt(C * C + 2 * C), 1.0 / 3.0);
        double P = F / (3 * (S + 1 / S + 1) * (S + 1 / S + 1) * G * G);
        double Q = Math.sqrt(1 + 2 * e2 * e2 * P);
        double r01 = -P * e2 * r / (1 + Q);
        double r02 = a * a * (1 + 1 / Q) / 2;
        double r03 = P * (1 - e2) * Z * Z / Q / (1 + Q);
        double r04 = P * r * r / 2;
        double r0 = r01 + Math.sqrt(r02 - r03 - r04);
        double u1 = r - e2 * r0;
        double u12 = u1 * u1;
        double U = Math.sqrt(u12 + Z * Z);
        double V = Math.sqrt(u12 + (1 - e2) * Z * Z);
        double Z0 = b * b * Z / a / V;
        double h = U * (1 - b * b / a / V);
        double phi = Math.atan((Z + ePrime2 * Z0) / r);
        double lambda = Math.atan2(Y, X);
        double lat = Math.toDegrees(phi);
        double lon = Math.toDegrees(lambda);
        return new GeodeticCoordinate(lat, lon, -h / 1000); // takes depth in km.
    }

    public static ENUCoordinate ecef2enu(GeodeticCoordinate gref, ECEFCoordinate pos) {
        ECEFCoordinate ref = geodetic2ecef(gref);
        double lambdar = Math.toRadians(gref.getLon());
        double phir = Math.toRadians(gref.getLat());
        RealMatrix ro = new Array2DRowRealMatrix(3, 3);
        double sinlr = Math.sin(lambdar);
        double coslr = Math.cos(lambdar);
        double sinp = Math.sin(phir);
        double cosp = Math.cos(phir);
        ro.setEntry(0, 0, -sinlr);
        ro.setEntry(0, 1, coslr);
        ro.setEntry(0, 2, 0);
        ro.setEntry(1, 0, -sinp * coslr);
        ro.setEntry(1, 1, -sinp * sinlr);
        ro.setEntry(1, 2, cosp);

        ro.setEntry(2, 0, cosp * coslr);
        ro.setEntry(2, 1, cosp * sinlr);
        ro.setEntry(2, 2, sinp);

        RealMatrix dif = new Array2DRowRealMatrix(3, 1);
        dif.setEntry(0, 0, pos.getX() - ref.getX());
        dif.setEntry(1, 0, pos.getY() - ref.getY());
        dif.setEntry(2, 0, pos.getZ() - ref.getZ());
        RealMatrix result = ro.multiply(dif);
        return new ENUCoordinate(result.getEntry(0, 0), result.getEntry(1, 0), result.getEntry(2, 0));
    }

    public static ENUCoordinate geodetic2enu(GeodeticCoordinate ref, GeodeticCoordinate pos) {
        ECEFCoordinate pose = geodetic2ecef(pos);
        return ecef2enu(ref, pose);
    }

    public static ECEFCoordinate enu2ecef(ENUCoordinate data, GeodeticCoordinate ref) {

        double lambda = Math.toRadians(ref.getLon());
        double phi = Math.toRadians(ref.getLat());
        RealMatrix ro = new Array2DRowRealMatrix(3, 3);
        double sinl = Math.sin(lambda);
        double cosl = Math.cos(lambda);
        double sinp = Math.sin(phi);
        double cosp = Math.cos(phi);

        ro.setEntry(0, 0, -sinl);
        ro.setEntry(0, 1, -sinp * cosl);
        ro.setEntry(0, 2, cosp * cosl);
        ro.setEntry(1, 0, cosl);
        ro.setEntry(1, 1, -sinp * sinl);
        ro.setEntry(1, 2, cosp * sinl);
        ro.setEntry(2, 0, 0);
        ro.setEntry(2, 1, cosp);
        ro.setEntry(2, 2, sinp);

        RealMatrix dif = new Array2DRowRealMatrix(3, 1);
        dif.setEntry(0, 0, data.getxEastMeters());
        dif.setEntry(1, 0, data.getyNorthMeters());
        dif.setEntry(2, 0, data.getzUpMeters());
        RealMatrix result = ro.multiply(dif);
        ECEFCoordinate refecef = geodetic2ecef(ref);
        double X = result.getEntry(0, 0) + refecef.getX();
        double Y = result.getEntry(1, 0) + refecef.getY();
        double Z = result.getEntry(2, 0) + refecef.getZ();
        return new ECEFCoordinate(X, Y, Z);
    }
}
