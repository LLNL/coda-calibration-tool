/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.api;

import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.util.GeodeticCoordinate;
import gov.llnl.gnem.apps.coda.common.model.util.WGS84DistanceCalcFunction;

public interface ConfigurationService {
    public VelocityConfiguration getVelocityConfiguration();

    public VelocityConfiguration update(VelocityConfiguration entry);

    public CalibrationSettings getCalibrationSettings();

    public CalibrationSettings update(CalibrationSettings entry);

    public ShapeFitterConstraints getCalibrationShapeFitterConstraints();

    public ShapeFitterConstraints update(ShapeFitterConstraints entry);

    public String updatePolygon(String rawGeoJSON);

    public String getPolygonGeoJSON();

    public WGS84DistanceCalcFunction getDistanceFunc();

    public Double getEpicentralDistance(GeodeticCoordinate coordA, GeodeticCoordinate coordB);

    public Double getHypocentralDistance(GeodeticCoordinate coordA, GeodeticCoordinate coordB);

    public GeodeticCoordinate getEventCoord(Event event);

    public GeodeticCoordinate getStationCoord(Station station);
}
