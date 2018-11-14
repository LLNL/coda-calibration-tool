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

import java.util.Optional;
import java.util.UUID;

import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;

public class LeafletIcon implements Icon {

    private static final Double DEFAULT = 0d;

    private final String id;
    private Location location;
    private String friendlyName;
    private IconTypes iconType;
    private IconStyles iconStyle;

    public LeafletIcon(Location location, String friendlyName, IconTypes iconType) {
        this(UUID.randomUUID().toString(), location, friendlyName, iconType);
    }

    public LeafletIcon(Location location, String friendlyName, IconTypes iconType, IconStyles style) {
        this(UUID.randomUUID().toString(), location, friendlyName, iconType, style);
    }

    public LeafletIcon(String id, Location location, String friendlyName, IconTypes iconType) {
        this.id = id;
        setLocation(location);
        this.friendlyName = friendlyName;
        this.iconType = iconType;
    }

    public LeafletIcon(String id, Location location, String friendlyName, IconTypes iconType, IconStyles style) {
        this.id = id;
        setLocation(location);
        this.friendlyName = friendlyName;
        this.iconType = iconType;
        this.iconStyle = style;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public Location setLocation(Location location) {
        if (location == null) {
            this.location = new Location(DEFAULT, DEFAULT);
        } else {
            this.location = new Location(location.getLatitude(), location.getLongitude());
        }
        return this.location;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(id).append("\", \"").append(location).append("\", \"").append(friendlyName).append("\", \"").append(iconType).append("\"");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((friendlyName == null) ? 0 : friendlyName.hashCode());
        result = prime * result + ((iconType == null) ? 0 : iconType.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((location == null) ? 0 : location.hashCode());
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
        LeafletIcon other = (LeafletIcon) obj;
        if (friendlyName == null) {
            if (other.friendlyName != null) {
                return false;
            }
        } else if (!friendlyName.equals(other.friendlyName)) {
            return false;
        }
        if (iconType != other.iconType) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (location == null) {
            if (other.location != null) {
                return false;
            }
        } else if (!location.equals(other.location)) {
            return false;
        }
        return true;
    }

    @Override
    public String getFriendlyName() {
        return Optional.ofNullable(friendlyName).orElse("");
    }

    @Override
    public IconTypes getType() {
        return Optional.ofNullable(iconType).orElse(IconTypes.DEFAULT);
    }

    @Override
    public IconStyles getStyle() {
        return Optional.ofNullable(iconStyle).orElse(IconStyles.DEFAULT);
    }

    @Override
    public String getId() {
        return id;
    }
}
