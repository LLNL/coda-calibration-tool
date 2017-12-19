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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class PolygonSet {

    private final PolygonSetType type;
    private final String name;
    private final int id;
    private double minlat;
    private double maxlat;
    private double minlon;
    private double maxlon;
    private List<Polygon> polygons;

    public PolygonSet(PolygonSetType type, String name, int id,
            double minlat, double maxlat, double minlon, double maxlon) {
        this.type = type;
        this.name = name;
        this.id = id;
        this.minlat = minlat;
        this.maxlat = maxlat;
        this.minlon = minlon;
        this.maxlon = maxlon;
        polygons = null;
    }
    
    public PolygonSet(PolygonSetType type, String name, int id, Collection<Polygon> polygons )
    {
        this.type = type;
        this.name = name;
        this.id = id;
        this.polygons = new ArrayList<Polygon>(polygons);
        refreshSetBounds();
    }

    public void setLatBounds(double minlat, double maxlat) {
        this.minlat = minlat;
        this.maxlat = maxlat;
    }

    public void setLonBounds(double minlon, double maxlon) {
        this.minlon = minlon;
        this.maxlon = maxlon;
    }

    public final void refreshSetBounds() {
        Iterator<Polygon> polyIter = polygons.iterator();
         Double minLat = 90.0;
        Double maxLat = -90.0;
        Double minLon = 180.0;
        Double maxLon = -180.0;

        while (polyIter.hasNext()) {
            Polygon poly = polyIter.next();
            if (poly.getMinLat() < minLat) {
                minLat = poly.getMinLat();
            }
            if (poly.getMaxLat() > maxLat) {
                maxLat = poly.getMaxLat();
            }
            if (poly.getMinLon() < minLon) {
                minLon = poly.getMinLon();
            }
            if (poly.getMaxLon() > maxLon) {
                maxLon = poly.getMaxLon();
            }
        }

        // Update the PolygonSet values
        setLatBounds(minLat, maxLat);
        setLonBounds(minLon, maxLon);
    }

    public PolygonSetType getType() {
        return type;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getMinLat() {
        return minlat;
    }

    public double getMaxLat() {
        return maxlat;
    }

    public double getMinLon() {
        return minlon;
    }

    public double getMaxLon() {
        return maxlon;
    }

    @Override
    public String toString() {
        return getName();
    }

    public boolean contains(double lat, double lon) {
        if (lat > maxlat || lat < minlat) {
            return false;
        }
        if (lon > maxlon || lon < minlon) {
            return false;
        }

        for (Polygon poly : getPolygons()) {
            if (poly.contains(lat, lon)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPolygons() {
        return polygons != null;
    }

    public void removePolygon(Polygon poly) {
        polygons.remove(poly);
    }

    public void setPolygons(List<Polygon> polygons) {
        this.polygons = polygons;
    }

    public List<Polygon> getPolygons() {
        return polygons;
    }
}
