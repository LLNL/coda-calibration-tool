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
package gov.llnl.gnem.apps.coda.common.mapping.api;

import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconStyles;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;

public interface GeoShapeFactory {

    default public Icon newIcon(Location location, String friendlyName) {
        return newIcon(IconTypes.DEFAULT, location, friendlyName, null);
    }

    default public Icon newIcon(Icon.IconTypes iconType, Location location, String friendlyName) {
        return newIcon(iconType, location, friendlyName, null);
    }

    public Icon newIcon(Icon.IconTypes iconType, Location location, String friendlyName, IconStyles style);

    default public Icon newIcon(String id, IconTypes iconType, Location location, String friendlyName) {
        return newIcon(id, iconType, location, friendlyName, null);
    }

    public Icon newIcon(String id, IconTypes iconType, Location location, String friendlyName, IconStyles style);

    public Line newLine(Location start, Location end);

    public Line newLine(String id, Location start, Location end);
}
