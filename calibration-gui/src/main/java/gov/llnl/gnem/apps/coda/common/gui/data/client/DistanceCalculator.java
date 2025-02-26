/*
* Copyright (c) 2024, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.common.gui.data.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings.DistanceCalcMethod;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.util.GeodeticCoordinate;
import gov.llnl.gnem.apps.coda.common.model.util.WGS84DistanceCalcFunction;
import llnl.gnem.core.util.Geometry.EModel;

@Component
public class DistanceCalculator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private DistanceCalcMethod preferredMethod = DistanceCalcMethod.EPICENTRAL;

    private ParameterClient client;

    @Autowired
    public DistanceCalculator(ParameterClient client) {
        this.client = client;
        updateCalcMethod();
    }

    public void updateCalcMethod() {
        String method = client.getCalibrationSettings().block(Duration.ofMinutes(10l)).getDistanceCalcMethod();

        if (method.equals(DistanceCalcMethod.HYPOCENTRAL.getValue())) {
            preferredMethod = DistanceCalcMethod.HYPOCENTRAL;
        } else {
            preferredMethod = DistanceCalcMethod.EPICENTRAL;
        }
    }

    public WGS84DistanceCalcFunction getDistanceFunc() {
        if (preferredMethod == DistanceCalcMethod.HYPOCENTRAL) {
            return this::getHypocentralDistance;
        }

        return this::getEpicentralDistance;
    }

    public Double getEpicentralDistance(GeodeticCoordinate coordA, GeodeticCoordinate coordB) {
        return EModel.getDistanceWGS84(coordA.getLat(), coordA.getLon(), coordB.getLat(), coordB.getLon());
    }

    public Double getHypocentralDistance(GeodeticCoordinate coordA, GeodeticCoordinate coordB) {

        llnl.gnem.core.util.Geometry.GeodeticCoordinate convertedCoordA = new llnl.gnem.core.util.Geometry.GeodeticCoordinate(coordA.getLat(), coordA.getLon(), Math.copySign(coordA.getDepthKm(), -1d));
        llnl.gnem.core.util.Geometry.GeodeticCoordinate convertedCoordB = new llnl.gnem.core.util.Geometry.GeodeticCoordinate(coordB.getLat(), coordB.getLon(), coordB.getElevationKm());

        return EModel.getSeparationMeters(convertedCoordA, convertedCoordB) / 1000.0;
    }

    public static GeodeticCoordinate getEventCoord(Event event) {

        // Note we are assuming event depth is in meters, and converting to km for distance calculations in km
        return new GeodeticCoordinate(event.getLatitude(), event.getLongitude(), event.getDepth() / 1000.0);
    }

    public static GeodeticCoordinate getStationCoord(Station station) {

        // Here, we are assuming a station elevation is in meters
        return new GeodeticCoordinate(station.getLatitude(), station.getLongitude(), station.getElevation() / 1000.0);
    }
}
