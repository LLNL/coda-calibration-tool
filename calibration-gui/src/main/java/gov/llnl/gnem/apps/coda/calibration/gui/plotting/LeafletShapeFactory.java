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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.common.mapping.LeafletIcon;
import gov.llnl.gnem.apps.coda.common.mapping.LeafletLine;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoShapeFactory;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconStyles;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.api.Line;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;

@Service
public class LeafletShapeFactory implements GeoShapeFactory {

    @Override
    public Icon newIcon(IconTypes iconType, Location location, String friendlyName, IconStyles style) {
        if (style != null) {
            return new LeafletIcon(location, friendlyName, iconType, style);
        }
        return new LeafletIcon(location, friendlyName, iconType);
    }

    @Override
    public Icon newIcon(String id, IconTypes iconType, Location location, String friendlyName, IconStyles style) {
        if (style != null) {
            return new LeafletIcon(id, location, friendlyName, iconType, style);
        }
        return new LeafletIcon(id, location, friendlyName, iconType);
    }

    @Override
    public Line newLine(String id, Location start, Location end) {
        return new LeafletLine(id, start, end);
    }

    @Override
    public Line newLine(Location start, Location end) {
        return new LeafletLine(start, end);
    }
}
