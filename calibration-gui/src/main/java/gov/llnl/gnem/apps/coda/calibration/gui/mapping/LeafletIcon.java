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
package gov.llnl.gnem.apps.coda.calibration.gui.mapping;

import java.util.Optional;

import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.calibration.gui.mapping.api.Location;

public class LeafletIcon implements Icon {

    private Location location;
    private String friendlyName;
    private IconTypes iconType;

    public LeafletIcon(Location location, String friendlyName, IconTypes iconType) {
        this.location = location;
        this.friendlyName = friendlyName;
        this.iconType = iconType;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public Location setLocation(Location location) {
        this.location = location;
        return location;
    }

    @Override
    public String toString() {
        return "LeafletIcon [location=" + location + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((location == null) ? 0 : location.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LeafletIcon other = (LeafletIcon) obj;
        if (location == null) {
            if (other.location != null)
                return false;
        } else if (!location.equals(other.location))
            return false;
        return true;
    }

    @Override
    public String getFriendlyName() {
        return Optional.of(friendlyName).orElse("");
    }

    @Override
    public IconTypes getType() {
        if (iconType != null) {
            return iconType;
        }
        return IconTypes.DEFAULT;
    }
}
