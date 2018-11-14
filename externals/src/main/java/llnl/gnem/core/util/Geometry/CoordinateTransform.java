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

import java.io.File;
import java.util.ArrayList;
import java.util.StringTokenizer;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import llnl.gnem.core.util.FileUtil.FileManager;
import llnl.gnem.core.util.MathFunctions.MathFunction;

/**
 * User: Eric Matzel Date: Mar 23, 2010
 */
public class CoordinateTransform {

    private static final Logger log = LoggerFactory.getLogger(CoordinateTransform.class);
    private final static double HALF_CIRCLE = 180.0;
    public final static double RADIANS_TO_DEGREES = HALF_CIRCLE / Math.PI;
    public final static double DEGREES_TO_RADIANS = Math.PI / HALF_CIRCLE;

    /**
     * Spherical Coordinates to Cartesian Coordinates
     *
     * @param theta
     *            angle x to y (a.k.a. longitude in RADIANS)
     * @param phi
     *            angle from x-y plane to z (a.k.a. latitude in RADIANS)
     * @param radius
     *            length of the vector from the origin at (0,0,0)
     * @return a Point3d object in Cartesian coordinates
     */
    public static Vector3D SphericalToCartesian(double theta, double phi, double radius) {
        double x = radius * Math.cos(phi) * Math.cos(theta);
        double y = radius * Math.cos(phi) * Math.sin(theta);
        double z = radius * Math.sin(phi);

        Vector3D result = new Vector3D(x, y, z);

        return result;
    }

    /**
     * Spherical Coordinates to Cartesian Coordinates
     *
     * @param point
     *            - a Point3d object in format point.x = theta, point.y = phi,
     *            point.z = radius
     *
     *            Note theta and phi need to be in RADIANS
     * @return a Point3d object in cartesian x,y,z coordinates
     */
    public static Vector3D SphericalToCartesian(Vector3D point) {
        double theta = point.getX();
        double phi = point.getY();
        double radius = point.getZ();

        return SphericalToCartesian(theta, phi, radius);
    }

    /**
     * Cartesian coordinates x,y,z to Spherical coordinates theta, phi, r
     *
     * @param point
     *            a Point3d object x,y,z
     * @return a Point3d object in format point.x = theta, point.y = phi,
     *         point.z = radius (theta and phi in RADIANS)
     */
    public static Vector3D CartesianToSpherical(Vector3D point) {
        return CartesianToSpherical(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Cartesian coordinates x,y,z to Spherical coordinates theta, phi, r
     *
     * @param x
     * @param y
     * @param z
     * @return a Point3d object in format point.x = theta, point.y = phi,
     *         point.z = r (theta and phi in RADIANS)
     */
    public static Vector3D CartesianToSpherical(double x, double y, double z) {
        double theta = Math.atan2(y, x);
        double phi = Math.atan2(z, Math.sqrt(x * x + y * y));
        double radius = Math.sqrt(x * x + y * y + z * z);

        Vector3D result = new Vector3D(theta, phi, radius);

        return result;
    }

    /**
     * Cartesian coordinates x,y,z to Polar coordinates theta, rho, z
     *
     * @param point
     *            a Point3d object x,y,z
     * @return a Point3d object in format point.x = theta, point.y = rho,
     *         point.z = z
     */
    public static Vector3D CartesianToPolar(Vector3D point) {
        return CartesianToPolar(point.getX(), point.getY(), point.getZ());
    }

    /**
     * Cartesian coordinates x,y,z to Polar coordinates theta, rho, z
     *
     * @param x
     * @param y
     * @param z
     * @return a Point3d object in format point.x = theta, point.y = rho,
     *         point.z = z
     */
    public static Vector3D CartesianToPolar(double x, double y, double z) {
        double theta = Math.atan2(y, x);
        double rho = Math.sqrt(x * x + y * y);

        Vector3D result = new Vector3D(theta, rho, z);
        return result;
    }

    /**
     * Cartesian coordinates x,y to Polar coordinates theta, phi, z = 0
     *
     * @param x
     * @param y
     * @return a Point3d object in format point.x = theta, point.y = rho,
     *         point.z = z
     */
    public static Vector3D CartesianToPolar(double x, double y) {
        return CartesianToPolar(x, y, 0);
    }

    /**
     * Polar coordinates theta, rho, z to Cartesian coordinates x,y,z
     *
     * @param theta
     *            angle from x to y
     * @param rho
     *            distance from origin (0,0,0)
     * @param z
     *            distance along the z-axis
     * @return a Point3d object (x,y,z)
     */
    public static Vector3D PolarToCartesian(double theta, double rho, double z) {
        double x = rho * Math.cos(theta);
        double y = rho * Math.sin(theta);
        // z = z

        Vector3D result = new Vector3D(x, y, z);
        return result;
    }

    /**
     * Polar coordinates theta, rho, (z = 0) to Cartesian coordinates x, y, (z =
     * 0)
     *
     * @param theta
     *            angle from x to y
     * @param rho
     *            distance from origin (0,0)
     * @return a Point3d object (x,y,0)
     */
    public static Vector3D PolarToCartesian(double theta, double rho) {
        return PolarToCartesian(theta, rho, 0);
    }

    /**
     * Polar coordinates theta, rho, (z = 0) to Cartesian coordinates x, y, z
     *
     * @param point
     *            a Point3d object in format point.x = theta, point.y = rho,
     *            point.z = z
     * @return a Point3d object (x,y,z)
     */
    public static Vector3D PolarToCartesian(Vector3D point) {
        double theta = point.getX();
        double rho = point.getY();
        double z = point.getZ();

        return PolarToCartesian(theta, rho, z);
    }

    /**
     * A convenience method to allow SphericalToCartesian conversion from
     * longitude, latitude (DEGREES) instead of theta,phi (RADIANS)
     *
     * @param longitude
     *            - the longitude in DEGREES
     * @param latitude
     *            - the latitude in DEGREES
     * @param radius
     *            - the radius to the point in space NOTE: units of radius (e.g.
     *            meters, km, miles) determine the units of XYZ
     * @return a Point3d object (x,y,z)
     */
    public static Vector3D LonLatRadiusToXYZ(double longitude, double latitude, double radius) {
        double theta = longitude * DEGREES_TO_RADIANS;
        double phi = latitude * DEGREES_TO_RADIANS;

        return SphericalToCartesian(theta, phi, radius);
    }

    /**
     * A convenience method to allow SphericalToCartesian conversion from
     * longitude, latitude (DEGREES) instead of theta,phi (RADIANS) NOTE: units
     * of radius (e.g. meters, km, miles) determine the units of XYZ
     *
     * @param lonlatradpoint
     *            = a Point3d object (longitude, latitude, radius)
     * @return a Point3d object (x,y,z)
     */
    public static Vector3D LonLatRadiusToXYZ(Vector3D lonlatradpoint) {
        double longitude = lonlatradpoint.getX();
        double latitude = lonlatradpoint.getY();
        double radius = lonlatradpoint.getZ();

        return LonLatRadiusToXYZ(longitude, latitude, radius);
    }

    /**
     * A convenience method to allow CartesianToSpherical conversion from X,Y,Z
     * to longitude, latitude (DEGREES) and radius NOTE: units of xyz (e.g.
     * meters, km, miles) determine the units of radius
     *
     * @param x
     * @param y
     * @param z
     * @return a Point3d object (longitude, latitude, radius)
     */
    public static Vector3D XYZToLonLatRadius(double x, double y, double z) {
        Vector3D result = CartesianToSpherical(x, y, z);

        return new Vector3D(result.getX() * RADIANS_TO_DEGREES, result.getY() * RADIANS_TO_DEGREES, result.getZ());
    }

    /**
     * A convenience method to allow CartesianToSpherical conversion from X,Y,Z
     * to longitude, latitude (DEGREES) and radius
     *
     * @param xyzpoint
     *            - a Point3d object (X, Y, Z)
     * @return a Point3d object (longitude, latitude, radius)
     */
    public static Vector3D XYZToLonLatRadius(Vector3D xyzpoint) {
        return XYZToLonLatRadius(xyzpoint.getX(), xyzpoint.getY(), xyzpoint.getZ());
    }

    public static double GeodeticToGeocentricLatitude(double geodeticlatitude) {
        double DegreesToRadians = Math.PI / 180.;
        double ecc = 0.081819190842621; // WGS84 eccentricity
        double phi = geodeticlatitude * DegreesToRadians;

        double a = (1 - (ecc * ecc)) * Math.sin(phi);
        double b = Math.cos(phi);
        double geocentriclatitude = Math.atan2(a, b) / DegreesToRadians;
        return geocentriclatitude;
    }

    public static double GeocentricToGeodeticLatitude(double geocentriclatitude) {
        double DegreesToRadians = Math.PI / 180.;
        double ecc = 0.081819190842621; // WGS84 eccentricity
        double phi = geocentriclatitude * DegreesToRadians;

        double a = (1 - (ecc * ecc)) * Math.cos(phi);
        double b = Math.sin(phi);
        double geodeticlatitude = Math.atan2(b, a) / DegreesToRadians;
        return geodeticlatitude;
    }

    //-----TODO move the following methods to more appropriate classes-------------------------------------------------------------
    static void createHemisphereSources(double centerlat, double centerlon, double radius, double hemisphereradius, int maxpoints) {
        //int maxpoints = 10000; // create 10,000 points
        //double centerlat = 40;
        //double centerlon = -121;
        //double radius = 6371; radius to center of the earth

        Vector3D xyzresult = LonLatRadiusToXYZ(centerlon, centerlat, radius);

        double x0 = xyzresult.getX();
        double y0 = xyzresult.getY();
        double z0 = xyzresult.getZ();

        double r = hemisphereradius;

        log.warn(x0 + "\t" + y0 + "\t" + z0);

        int index = 0;
        while (index < maxpoints) {
            double dx = MathFunction.randomBetween(-r, r); // creates a random

            double dymax = Math.sqrt(r * r - dx * dx); // y^2 = r^2 - x^2
            double dy = MathFunction.randomBetween(-dymax, dymax);

            double dz = Math.sqrt(dymax * dymax - dy * dy); //z^2 = r^2 - x^2 - y^2  Note this only uses the positive half of the result space.

            double x = x0 + dx;
            double y = y0 + dy;
            double z1 = z0 + dz; // Note minus sign means that radius will be smaller. depth is greater
            double z2 = z0 - dz;

            Vector3D lonlatradius1 = XYZToLonLatRadius(x, y, z1);
            Vector3D lonlatradius2 = XYZToLonLatRadius(x, y, z2);

            // check whether hemisphere's radius is above the original radius
            // save only the portion of the sphere that falls below the reference radius
            if (lonlatradius1.getZ() < radius) // forces the hemisphere to fall below the original radius point
            {
                writeSW4source(lonlatradius1.getX(), lonlatradius1.getY(), 1000 * (radius - lonlatradius1.getZ()));
                index = index + 1;
            }
            if (lonlatradius2.getZ() < radius) // forces the hemisphere to fall below the original radius point
            {
                writeSW4source(lonlatradius2.getX(), lonlatradius2.getY(), 1000 * (radius - lonlatradius2.getZ())); // note conversion from radius in km to depth in m
                index = index + 1;
            }

        }
    }

    /**
     * Write SW4/WPP style source output
     */
    static void writeSW4source(double longitude, double latitude, double depth) {
        String dfmt4 = "%-10.4f";// Note -10.4f aligns to the left +10.4.f aligns to right

        double t0 = 0.100;
        double M0 = 1e10;
        double mxx = 1. / 3.;
        double myy = 1. / 3.;
        double mzz = 1. / 3.;
        double mxy = 0.;
        double mxz = 0.;
        double myz = 0.;

        depth = depth - 1500; //TODO replace this is a special case for Newberry - reference radius MSL versus ~min surface radius

        String sourceline = "source lon="
                + String.format(dfmt4, longitude)
                + "  lat="
                + String.format(dfmt4, latitude)
                + "  z="
                + String.format(dfmt4, depth)
                + "  type=Dirac t0="
                + t0
                + "  m0="
                + M0
                + "  mxx="
                + String.format(dfmt4, mxx)
                + "  mxy="
                + String.format(dfmt4, mxy)
                + "  myy="
                + String.format(dfmt4, myy)
                + "  mxz="
                + String.format(dfmt4, mxz)
                + "  myz="
                + String.format(dfmt4, myz)
                + "  mzz="
                + String.format(dfmt4, mzz);

        log.warn(sourceline);
    }

}

/**
 * Conversion to/from Swiss projection to Latitude Longitude
 *
 * Based on the equations in "Formulas and constants for the calculation of the
 * Swiss conformal cylindrical projection and for the transformation between
 * coordinate systems" published by swisstopo, May 2008
 *
 * @author matzel1
 */
class SwissProjection {

    /**
     * 4 Approximate solution for the transformation CH1903 ⇔ WGS84
     */
    /**
     * Approximate method (from section 4.1), should be accurate to ~ 1 meter
     *
     * formulas for the direct transformation of: ellipsoidal ETRS89 or WGS84
     * coordinates to Swiss projection coordinates
     *
     * @param longitude
     *            in decimal degrees
     * @param latitude
     *            in decimal degrees
     * @return Easting, Northing in meters
     *
     */
    public static double[] LonLatToSwissProjection(double longitude, double latitude) {

        //1. The latitudes phi and longitudes lambda have to be converted into arc seconds ["] 
        double latsec = latitude * 3600;
        double lonsec = longitude * 3600;

        //2. The following auxiliary values have to be calculated (differences of latitude and longitude relative to the projection centre in Bern in the unit [10000"]): 
        double phiprime = (latsec - 169028.66) / 10000;
        double lambdaprime = (lonsec - 26782.5) / 10000;
        double lambdaprime2 = lambdaprime * lambdaprime;
        double phiprime2 = phiprime * phiprime;
        double lambdaprime3 = lambdaprime2 * lambdaprime;
        double phiprime3 = phiprime2 * phiprime;

        double Easting = 600072.37 + 211455.93 * lambdaprime - 10938.51 * lambdaprime * phiprime - 0.36 * lambdaprime * phiprime2 - 44.54 * lambdaprime3;// in meters
        double Northing = 200147.07 + 308807.95 * phiprime + 3745.25 * lambdaprime2 + 76.63 * phiprime2 - 194.56 * lambdaprime2 * phiprime + 119.79 * phiprime3;// in meters

        double[] result = { Easting, Northing };
        return result;

    }

    /**
     * Approximate method (from section 4.2). Should be accurate to
     * approximately 0.12" longitude and 0.08" in latitude
     *
     * formulas for the direct transformation of: Swiss projection coordinates
     * to ellipsoidal ETRS89 or WGS84 coordinates
     *
     * @param Easting
     *            : in meters
     * @param Northing:
     *            in meters
     * @return latitude, longitude in decimal degrees
     *
     */
    public static double[] SwissProjectionToLonLat(double Easting, double Northing) {
        //1. The projection coordinates y (easting) and x (northing) have to be converted into the civilian system (Bern = 0 / 0) and have to expressed in the unit [1000 km] : 
        double yprime = (Easting - 600000) / 1000000; // units from meters to 1000 km;
        double xprime = (Northing - 200000) / 1000000; // units from meters to 1000 km;
        double yprime2 = yprime * yprime;
        double xprime2 = xprime * xprime;
        double yprime3 = yprime2 * yprime;
        double xprime3 = xprime2 * xprime;

        //2. The longitude and latitude have to be calculated in the unit [10000"] 
        double lambdaprime = 2.6779094 + 4.728982 * yprime + 0.791484 * yprime * xprime + 0.1306 * yprime * xprime2 - 0.0436 * yprime3;
        double phiprime = 16.9023892 + 3.238272 * xprime - 0.270978 * yprime2 - 0.002528 * xprime2 - 0.0447 * yprime2 * xprime - 0.0140 * xprime3;

        //3. Longitude and latitude have to be converted to the unit [°] 
        double longitude = lambdaprime * 100 / 36;
        double latitude = phiprime * 100 / 36;

        double[] result = { longitude, latitude };

        return result;
    }

    /**
     * A utility test method to read an East,North catalog and return the
     * equivalent longitudes and latitudes.
     */
    public static void BaselCatalog() {
        File file = FileManager.openFile("Open a catalog file", null, null)[0];

        ArrayList<String> textrowlist = FileManager.createTextRowCollection(file);
        for (String row : textrowlist) {
            try {
                StringTokenizer tokenizer = new StringTokenizer(row);
                long evid = Long.parseLong(tokenizer.nextToken());
                double Easting = Double.parseDouble(tokenizer.nextToken()); // a number in meters ~ 600,000
                double Northing = Double.parseDouble(tokenizer.nextToken());// a number in meters ~ 200,000 
                double Zum_elevation = Double.parseDouble(tokenizer.nextToken()); // Basel catelog is m below sea level. z positive downward

                // calculate the latitude and longitude here
                double[] lonlat = SwissProjectionToLonLat(Easting, Northing);
            } catch (Exception e) {
            }
        }

    }
}
