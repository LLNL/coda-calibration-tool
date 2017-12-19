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

public class Polygon extends llnl.gnem.core.polygon.BasePolygon {

    private static final long serialVersionUID = -8925155950211896078L;
    private int polyid;
    final String polyName;
    double minlat, maxlat, minlon, maxlon;

    public Polygon(int id, String polyName, Vertex[] verts) {
        super(verts);
        this.polyid = id;
        this.polyName = polyName;
        minlat = maxlat = verts[0].getLat();
        minlon = maxlon = verts[0].getLon();
        for (Vertex vert : verts) {
            if (vert.getLat() > maxlat) {
                maxlat = vert.getLat();
            } else if (vert.getLat() < minlat) {
                minlat = vert.getLat();
            }
            if (vert.getLon() > maxlon) {
                maxlon = vert.getLon();
            } else if (vert.getLon() < minlon) {
                minlon = vert.getLon();
            }
        }
    }

    public Polygon(int id, String polyName, Vertex[] verts, double minLat, double maxLat, double minLon, double maxLon) {
        super(verts);
        this.polyid = id;
        this.polyName = polyName;
        this.minlat = minLat;
        this.maxlat = maxLat;
        this.minlon = minLon;
        this.maxlon = maxLon;
    }

    public int getPolyId() {
        return polyid;
    }

    public String getName() {
        return polyName;
    }

    @Override
    public double getMinLat() {
        return minlat;
    }

    @Override
    public double getMaxLat() {
        return maxlat;
    }

    @Override
    public double getMinLon() {
        return minlon;
    }

    @Override
    public double getMaxLon() {
        return maxlon;
    }

    @Override
    public boolean contains(double x, double y) {
        if (x < minlat || x > maxlat || y < minlon || y > maxlon) {
            return false;
        }
        return super.contains(x, y);
    }

    @Override
    public boolean contains(Vertex v) {
        if (v.getLat() < minlat || v.getLat() > maxlat || v.getLon() < minlon || v.getLon() > maxlon) {
            return false;
        }
        return super.contains(v);
    }

    /**
     * @param polyid the polyid to set
     */
    public void setPolyid(int polyid) {
        this.polyid = polyid;
    }
}
