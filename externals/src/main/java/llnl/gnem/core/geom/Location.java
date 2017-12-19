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

/**
 *
 * @author addair1
 */
public class Location<T extends Coordinate> implements Comparable<Location> {
    private final T coord;
    private final String name;
    
    public Location(T coord) {
        this(coord, coord.toString());
    }
    
    public Location(T coord, String name) {
        this.coord = coord;
        this.name = name;
    }
    
    public T getCoordinate() {
        return coord;
    }
    
    public String getName() {
        return name;
    }
    
    public double getDistance(Location<T> other) {
        return coord.getDistance(other.coord);
    }

    @Override
    public int compareTo(Location other) {
        return name.compareTo(other.name);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Location other = (Location) obj;
        if (this.coord != other.coord && (this.coord == null || !this.coord.equals(other.coord))) {
            return false;
        }
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 73 * hash + (this.coord != null ? this.coord.hashCode() : 0);
        hash = 73 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
}
