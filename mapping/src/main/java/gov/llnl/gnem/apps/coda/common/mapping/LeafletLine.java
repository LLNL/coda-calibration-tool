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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.util.UUID;

import gov.llnl.gnem.apps.coda.common.mapping.api.Line;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;

public class LeafletLine implements Line {

    private final String id;
    private Location startLocation;
    private Location endLocation;

    public LeafletLine(Location startLocation, Location endLocation) {
        this(UUID.randomUUID().toString(), startLocation, endLocation);
    }

    public LeafletLine(String id, Location startLocation, Location endLocation) {
        super();
        this.id = id;
        setStartLocation(startLocation);
        setEndLocation(endLocation);
    }

    @Override
    public Location getStartLocation() {
        return startLocation;
    }

    @Override
    public LeafletLine setStartLocation(Location startLocation) {
        if (startLocation == null) {
            this.startLocation = new Location(0d, 0d);
        } else {
            this.startLocation = startLocation;
        }
        return this;
    }

    @Override
    public Location getEndLocation() {
        return endLocation;
    }

    @Override
    public LeafletLine setEndLocation(Location endLocation) {
        if (endLocation == null) {
            this.endLocation = new Location(0d, 0d);
        } else {
            this.endLocation = endLocation;
        }
        return this;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((endLocation == null) ? 0 : endLocation.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((startLocation == null) ? 0 : startLocation.hashCode());
        return result;
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
        LeafletLine other = (LeafletLine) obj;
        if (endLocation == null) {
            if (other.endLocation != null) {
                return false;
            }
        } else if (!endLocation.equals(other.endLocation)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (startLocation == null) {
            if (other.startLocation != null) {
                return false;
            }
        } else if (!startLocation.equals(other.startLocation)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(id).append("\", \"").append(startLocation).append("\", \"").append(endLocation).append('\"');
        return builder.toString();
    }
}
