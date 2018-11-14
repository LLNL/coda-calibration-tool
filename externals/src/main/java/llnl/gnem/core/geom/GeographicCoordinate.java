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
package llnl.gnem.core.geom;

import llnl.gnem.core.util.Geometry.EModel;

/**
 *
 * @author addair1
 */
public class GeographicCoordinate implements Coordinate<GeographicCoordinate> {
    public static final int CRUDE_DEG_TO_KM = 56;
    private final double lat;
    private final double lon;
    private final double height;

    public GeographicCoordinate(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        height = 0;
    }

    public GeographicCoordinate(double lat, double lon, double height) {
        this.lat = lat;
        this.lon = lon;
        this.height = height;
    }

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    @Override
    public double getElevation() {
        return height;
    }

    /*
     * Returns distance between two coordinates in kilometers
     */
    @Override
    public double getDistance(GeographicCoordinate other) {
        return EModel.getDistanceWGS84(lat, lon, other.lat, other.lon);
    }

    public double getDelta(GeographicCoordinate other) {
        return EModel.getDeltaWGS84(lat, lon, other.lat, other.lon);
    }

    public double getForwardAzimuth(GeographicCoordinate other) {
        double result = EModel.getAzimuthWGS84(lat, lon, other.lat, other.lon);
        return result >= 0 ? result : 360 + result;
    }

    public double getBackAzimuth(GeographicCoordinate other) {
        return other.getForwardAzimuth(this);
    }

    public double quickDistance(GeographicCoordinate other) {
        double dlat = other.getLat() - getLat();
        double dlon = other.getLon() - getLon();
        return (Math.abs(dlat) + Math.abs(dlon)) * CRUDE_DEG_TO_KM;
    }

    @Override
    public String toString() {
        return String.format("(%f, %f, %f)", getLat(), getLon(), getElevation());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 67 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
        hash = 67 * hash + (int) (Double.doubleToLongBits(this.lon) ^ (Double.doubleToLongBits(this.lon) >>> 32));
        hash = 67 * hash + (int) (Double.doubleToLongBits(this.height) ^ (Double.doubleToLongBits(this.height) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeographicCoordinate other = (GeographicCoordinate) obj;
        if (Double.doubleToLongBits(this.lat) != Double.doubleToLongBits(other.lat)) {
            return false;
        }
        if (Double.doubleToLongBits(this.lon) != Double.doubleToLongBits(other.lon)) {
            return false;
        }
        if (Double.doubleToLongBits(this.height) != Double.doubleToLongBits(other.height)) {
            return false;
        }
        return true;
    }

}
