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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//
//  DistanceAzimuth.java
//
//  Created by Eric Matzel on Mon Sep 22 2003.
//

public class DistanceAzimuth {

    private static final Logger log = LoggerFactory.getLogger(DistanceAzimuth.class);

    private DistanceAzimuth() {
        //NOP
    }

    /**
     * Determine Great Circle Paths, Distance, Azimuth and Back Azimuth for 2
     * points on a sphere input latitude and longitude in degrees
     */
    public DistanceAzimuth(double latitude1, double longitude1, double latitude2, double longitude2) {
        this.latitude1 = latitude1;
        this.latitude2 = latitude2;
        this.longitude1 = longitude1;
        this.longitude2 = longitude2;

        // from Lay and Wallace(1995) Box 4.4 page 135-136
        double A = longitude1 - longitude2;

        // convert to colatitude
        double b = 90. - latitude1;
        double c = 90. - latitude2;

        double a; // Epicentral distance between point 1 and point 2
        boolean pt1_to_east;
        double d12;

        double degreestoradians = Math.PI / 180.; // conversion for trigonometric calculations

        A = A * degreestoradians;
        b = b * degreestoradians;
        c = c * degreestoradians;

        // need to add code to determine if the station is located to the east or west of the source --
        // make sure to measure clockwise.
        d12 = longitude1 - longitude2;

        if (((d12 > 0.) && (d12 < 180.)) || (d12 > -360.) && (d12 < -180.)) {
            pt1_to_east = true;
        } else {
            pt1_to_east = false;
        }
        log.trace("pt1 to east?:" + pt1_to_east);
        a = Math.acos(Math.cos(b) * Math.cos(c) + Math.sin(b) * Math.sin(c) * Math.cos(A));
        azimuth = Math.acos((Math.cos(c) - Math.cos(a) * Math.cos(b)) / (Math.sin(a) * Math.sin(b)));
        backazimuth = Math.acos((Math.cos(b) - Math.cos(a) * Math.cos(c)) / (Math.sin(a) * Math.sin(c)));

        double EpicentralDistance = a / degreestoradians;
        azimuth = azimuth / degreestoradians;
        backazimuth = backazimuth / degreestoradians;

        if (pt1_to_east) {
            azimuth = 360. - azimuth;
        } else {
            backazimuth = 360. - backazimuth;
        }

        //!!!! refine the distance calculation
        distance = 111.195 * EpicentralDistance;

    }

    public DistanceAzimuth Spheroid(double latitude1, double longitude1, double latitude2, double longitude2) {
        /**
         * Geometry for source-receiver paths. Approximates the Earth as a
         * spheroid with a flattening, epsilon, of 0.0033528. convert from
         * geographic latitude to geocentric latitude. From Kennett (2000)
         * Appendix pgs 488-490
         */

        this.latitude1 = latitude1;
        this.latitude2 = latitude2;
        this.longitude1 = longitude1;
        this.longitude2 = longitude2;

        // convert from degrees to radians
        double degreestoradians = Math.PI / 180.; // conversion for trigonometric calculations
        double gglat1 = latitude1 * degreestoradians;
        double rlon1 = longitude1 * degreestoradians;
        double gglat2 = latitude2 * degreestoradians;
        double rlon2 = longitude2 * degreestoradians;

        //debugging            double gclat1 = Math.atan( (0.9933056)*Math.tan(gglat1));
        //debugging            double gclat2 = Math.atan( (0.9933056)*Math.tan(gglat2));
        double gclat1 = gglat1;
        double gclat2 = gglat2;
        // convert to colatitude
        double colat1 = Math.PI / 2. - gclat1;
        double colat2 = Math.PI / 2. - gclat2;
        log.trace("colatitudes:" + colat1 + "," + colat2);
        SphericalDirectionCosines dcs = new SphericalDirectionCosines(colat1, rlon1);
        SphericalDirectionCosines dcr = new SphericalDirectionCosines(colat2, rlon2);

        double azimuth = getAzimuth(dcs, dcr) / degreestoradians;
        double backazimuth = getAzimuth(dcr, dcs) / degreestoradians;
        double EpicentralDistance = getEpicentralDistance(dcs, dcr) / degreestoradians;
        //double GeodesicDistance = getGeodesicDistance(....);
        log.trace(" ");
        log.trace("Spheroid result:");
        log.trace("point 1: " + latitude1 + " " + longitude1);
        log.trace("point 2: " + latitude2 + " " + longitude2);
        log.trace("Distance: " + EpicentralDistance + " azimuth: " + azimuth + " backazimuth: " + backazimuth);
        log.trace("--------------------------------------------");

        this.azimuth = azimuth;
        this.backazimuth = backazimuth;
        this.distance = EpicentralDistance;

        return this;
    }

    double getEpicentralDistance(SphericalDirectionCosines dc1, SphericalDirectionCosines dc2) {
        double cosEpicentralDistance = (dc1.A * dc2.A + dc1.B * dc2.B + dc1.C * dc2.C);
        double EpicentralDistance = Math.acos(cosEpicentralDistance);
        return EpicentralDistance;
    }

    double getAzimuth(SphericalDirectionCosines dc1, SphericalDirectionCosines dc2) {
        /**
         * gets the azimuth from point 1 to point 2 based on direction cosines.
         */
        double sinAzimuth = -(dc1.D * dc2.A + dc1.E * dc2.B);
        double cosAzimuth = -(dc1.G * dc2.A + dc1.H * dc2.B + dc1.K * dc2.C);

        double Azimuth = Math.atan(sinAzimuth / cosAzimuth);

        log.trace("arcsin(azimuth): " + Math.asin(sinAzimuth) * 57.2958);
        log.trace("arccos(azimuth): " + Math.acos(cosAzimuth) * 57.2958);
        log.trace("arctan(azimuth): " + Azimuth * 57.2958);
        //!!! use an arctangent routine to get the azimuth from above
        return Azimuth;
    }

    public double getDistance() {
        return distance;
    }

    public double getAzimuth() {
        return azimuth;
    }

    public double getBackazimuth() {
        return backazimuth;
    }

    public double getLatitude1() {
        return latitude1;
    }

    public double getLatitude2() {
        return latitude2;
    }

    public double getLongitude1() {
        return longitude1;
    }

    public double getLongitude2() {
        return longitude2;
    }

    private double distance; // distance in kilometers
    private double azimuth; // azimuth measured clockwise from point 1 to point 2
    private double backazimuth; // backazimuth measured clockwise from point 2 to point 1
    private double latitude1; // the latitude of the first point
    private double longitude1; // the longitude of the first point
    private double latitude2; // the latitude of the second point
    private double longitude2; // the longitude of the second point
}
