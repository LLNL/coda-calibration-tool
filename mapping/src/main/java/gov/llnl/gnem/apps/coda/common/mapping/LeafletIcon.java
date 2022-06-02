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
import java.util.function.BiConsumer;

import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;

public class LeafletIcon implements Icon {

    private static final Double DEFAULT = 0d;

    private final String id;
    private Location location;
    private String friendlyName;
    private IconTypes iconType;
    private IconStyles iconStyle;
    private Double iconSize;
    private BiConsumer<Boolean, String> selectedCallback;
    private boolean shouldBeAnnotated = false;

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
    public String getFriendlyName() {
        return Optional.ofNullable(friendlyName).orElse(id);
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

    @Override
    public Icon setIconSelectionCallback(BiConsumer<Boolean, String> callback) {
        this.selectedCallback = callback;
        return this;
    }

    @Override
    public BiConsumer<Boolean, String> getIconSelectionCallback() {
        return selectedCallback;
    }

    @Override
    public Double getIconSize() {
        return iconSize;
    }

    @Override
    public Icon setIconSize(Double iconSize) {
        this.iconSize = iconSize;
        return this;
    }

    @Override
    public boolean shouldBeAnnotated() {
        return shouldBeAnnotated;
    }

    @Override
    public Icon setShouldBeAnnotated(boolean shouldBeAnnotated) {
        this.shouldBeAnnotated = shouldBeAnnotated;
        return this;
    }
}
