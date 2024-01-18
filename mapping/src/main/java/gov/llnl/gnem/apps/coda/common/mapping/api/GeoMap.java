/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import gov.llnl.gnem.apps.coda.common.mapping.MapCallbackEvent;
import gov.llnl.gnem.apps.coda.common.mapping.WMSLayerDescriptor;

public interface GeoMap {

    public void clearIcons();

    public Set<Icon> getIcons();

    public boolean addIcon(Icon icon);

    public boolean removeIcon(Icon icon);

    public void addIcons(Collection<Icon> icons);

    public void removeIcons(Collection<Icon> icons);

    public long getIconCount();

    public void addLayer(WMSLayerDescriptor layer);

    public void addShape(GeoShape shape);

    public void removeShape(GeoShape shape);

    public void fitViewToActiveShapes();

    public void registerEventCallback(Consumer<MapCallbackEvent> callback);

    public void removeEventCallback(Consumer<MapCallbackEvent> callback);

    public void show();

    public String getPolygonGeoJSON();

    public void setPolygonGeoJSON(String geoJSON);
}
