/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.common.mapping.api;

import java.util.function.BiConsumer;

public interface Icon {

    public static final String FOCUS_TAG = "!!";

    public enum IconTypes {
        DEFAULT, CIRCLE, TRIANGLE_UP, POLYGON_OUT, POLYGON_IN
    }

    public enum IconStyles {
        DEFAULT, FOCUSED, BACKGROUND
    }

    public String getId();

    public Location getLocation();

    public Location setLocation(Location locations);

    public IconTypes getType();

    public IconStyles getStyle();

    public Double getIconSize();

    public Icon setIconSize(Double size);

    public String getFriendlyName();

    public Icon setIconSelectionCallback(BiConsumer<Boolean, String> callback);

    public BiConsumer<Boolean, String> getIconSelectionCallback();

    public boolean shouldBeAnnotated();

    public Icon setShouldBeAnnotated(boolean shouldBeAnnotated);
}
