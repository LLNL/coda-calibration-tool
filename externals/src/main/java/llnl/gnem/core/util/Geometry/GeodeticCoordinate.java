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
package llnl.gnem.core.util.Geometry;

import java.util.Collection;


public class GeodeticCoordinate {

    private final double lat;
    private final double lon;
    private final double depthKm;
    private final double elevationKm;

    public static GeodeticCoordinate getCentroid(Collection<GeodeticCoordinate> values) {
        if (values.isEmpty()) {
            return null;
        }
        double latc = 0;
        double lonc = 0;
        double depthc = 0;
        for (GeodeticCoordinate gc : values) {
            latc += gc.lat;
            lonc += gc.lon;
            depthc = gc.depthKm;
        }
        latc /= values.size();
        lonc /= values.size();
        depthc /= values.size();
        return new GeodeticCoordinate(latc, lonc, depthc);
    }

    public GeodeticCoordinate() {
        lat=0;
        lon=0;
        depthKm=0;
        elevationKm=0;
    }
/**
 * Constructs a GeodeticCoordinate from lat, lon, depth.
 * @param lat
 * @param lon
 * @param depth Depth is in km with positive down. Depth = 0 is on the ellipsoid.
 */
    public GeodeticCoordinate(double lat, double lon, double depth) {
        this.lat=lat;
        this.lon=lon;
        this.depthKm=depth;
        elevationKm=0;
    }
    
    public GeodeticCoordinate(double lat, double lon, double depth, double elevKm) {
        this.lat=lat;
        this.lon=lon;
        this.depthKm=depth;
        elevationKm=elevKm;
    }

    public GeodeticCoordinate(GeodeticCoordinate old) {
        lat = old.getLat();
        lon = old.getLon();
        depthKm = old.getDepthKm();
        this.elevationKm = old.elevationKm;
    }



    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getDepthKm() {
        return depthKm;
    }


    public double getElevationKm() {
        return elevationKm;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.lat) ^ (Double.doubleToLongBits(this.lat) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.lon) ^ (Double.doubleToLongBits(this.lon) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.depthKm) ^ (Double.doubleToLongBits(this.depthKm) >>> 32));
        hash = 29 * hash + (int) (Double.doubleToLongBits(this.elevationKm) ^ (Double.doubleToLongBits(this.elevationKm) >>> 32));
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final GeodeticCoordinate other = (GeodeticCoordinate) obj;
        if (Double.doubleToLongBits(this.lat) != Double.doubleToLongBits(other.lat)) {
            return false;
        }
        if (Double.doubleToLongBits(this.lon) != Double.doubleToLongBits(other.lon)) {
            return false;
        }
        if (Double.doubleToLongBits(this.depthKm) != Double.doubleToLongBits(other.depthKm)) {
            return false;
        }
        if (Double.doubleToLongBits(this.elevationKm) != Double.doubleToLongBits(other.elevationKm)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "GeodeticCoordinate{" + "lat=" + lat + ", lon=" + lon + ", depthKm=" + depthKm + ", elevationKm=" + elevationKm + '}';
    }
    
    
}
