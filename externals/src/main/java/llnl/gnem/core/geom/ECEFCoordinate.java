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
package llnl.gnem.core.geom;

import llnl.gnem.core.util.Geometry.EModel;

/**
 * Represents a point in the Earth-Centered Earth-Fixed Coordinate System
 *
 * "ECEF ("Earth-Centered, Earth-Fixed"), also known as ECR ("Earth Centered
 * Rotational"), is a geographic coordinate system and Cartesian coordinate
 * system, and is sometimes known as a "conventional terrestrial" system.[1] It
 * represents positions as an X, Y, and Z coordinate. The point (0,0,0) is
 * defined as the center of mass of the Earth,[2] hence the name Earth-Centered.
 * Its axes are aligned with the International Reference Pole (IRP) and
 * International Reference Meridian (IRM) that are fixed with respect to the
 * surface of the Earth,[3][4] hence the name Earth-Fixed. This term can cause
 * confusion since the Earth does not rotate about the z-axis (unlike an
 * inertial system such as ECI), and is therefore alternatively called ECR.
 *
 * The z-axis is pointing towards the north but it does not coincide exactly
 * with the instantaneous Earth rotational axis.[3] The slight "wobbling" of the
 * rotational axis is known as polar motion.[5] The x-axis intersects the sphere
 * of the Earth at 0° latitude (Equator) and 0° longitude (Greenwich). This
 * means that ECEF rotates with the earth and therefore, coordinates of a point
 * fixed on the surface of the earth do not change. Conversion from a WGS84
 * Datum to ECEF can be used as an intermediate step in converting velocities to
 * the North East Down coordinate system." https://en.wikipedia.org/wiki/ECEF
 *
 * @see
 * http://www.springer.com/cda/content/document/cda_downloaddocument/9780857296344-c2.pdf?SGWID=0-0-45-1143141-p174116371
 * @see https://en.wikipedia.org/wiki/ECEF
 * @see https://en.wikipedia.org/wiki/Geographic_coordinate_conversion
 *
 * @author maganazook1
 */
public class ECEFCoordinate extends CartesianCoordinate {

    public static final int SEMI_MAJOR_AXIS_METERS = 6378137;
    public static final int SEMI_MINOR_AXIS_METERS = 6356752;
    public static final double FLATTENING_FACTOR = 1 / 298.257223563;
    public static final double FIRST_ECCENTRICITY = 0.08181919;

    public ECEFCoordinate(double x, double y) {
        super(x, y);
    }

    public ECEFCoordinate(double x, double y, double z) {
        super(x, y, z);
    }

    /**
     * Converts a lat-lon-height coordinate to an ECEF one.
     *
     * @param geographicCoordinate
     */
    public ECEFCoordinate(GeographicCoordinate geographicCoordinate) {
        //dummy values before we do the conversion
        super(0, 0, 0);

        ECEFCoordinate coordinate = ECEFCoordinate.convertGeographicToECEF(geographicCoordinate);

        this.setPoint(coordinate.getPoint());
    }

    /**
     * Converts a geographic coordinate to the ECEF coordinate system
     *
     * @param geographicCoordinate
     * @return
     */
    public static ECEFCoordinate convertGeographicToECEF(GeographicCoordinate geographicCoordinate) {
        final double longitudeDegrees = geographicCoordinate.getLon();
        final double latitudeDegrees = geographicCoordinate.getLat();
        final double height = geographicCoordinate.getElevation();

        double cosineLongitude = Math.cos(longitudeDegrees * EModel.DEGREES_TO_RADIANS);
        double sineLongitude = Math.sin(longitudeDegrees * EModel.DEGREES_TO_RADIANS);

        double cosineLatitude = Math.cos(latitudeDegrees * EModel.DEGREES_TO_RADIANS);
        double sineLatitude = Math.sin(latitudeDegrees * EModel.DEGREES_TO_RADIANS);

        double primeVerticalRadiusOfCurvature = calculatePrimeVerticalRadiusOfCurvature(latitudeDegrees);

        double x = (primeVerticalRadiusOfCurvature + height) * cosineLatitude * cosineLongitude;
        double y = (primeVerticalRadiusOfCurvature + height) * cosineLatitude * sineLongitude;
        double z = (primeVerticalRadiusOfCurvature * (1 - Math.pow(FIRST_ECCENTRICITY, 2)) + height) * sineLatitude;

        return new ECEFCoordinate(x, y, z);
    }

    /**
     * Calculates the Prime Vertical Radius of Curvature given a latitude in
     * degrees
     *
     * @param latitudeDegrees
     * @return
     */
    public static double calculatePrimeVerticalRadiusOfCurvature(double latitudeDegrees) {
        final double sineLatitudeSqaured = Math.pow(Math.sin(latitudeDegrees * EModel.DEGREES_TO_RADIANS), 2);
        final double firstEccentricitySquared = Math.pow(FIRST_ECCENTRICITY, 2);

        return SEMI_MAJOR_AXIS_METERS / Math.sqrt(1 - (firstEccentricitySquared * sineLatitudeSqaured));
    }
}
