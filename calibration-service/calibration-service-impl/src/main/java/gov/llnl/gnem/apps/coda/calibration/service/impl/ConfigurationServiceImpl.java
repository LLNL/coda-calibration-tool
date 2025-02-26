/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings;
import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings.DistanceCalcMethod;
import gov.llnl.gnem.apps.coda.calibration.model.domain.GeoJsonPolygon;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationSettingsChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.GvDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.ShapeConstraintsChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.repository.CalibrationSettingsRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.CalibrationShapeFitterConstraintsRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.PolygonRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.VelocityConfigurationRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.util.GeodeticCoordinate;
import gov.llnl.gnem.apps.coda.common.model.util.WGS84DistanceCalcFunction;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManager;
import llnl.gnem.core.util.Geometry.EModel;

@Service
@Transactional
public class ConfigurationServiceImpl implements ConfigurationService {

    private EntityManager em;
    private VelocityConfigurationRepository velConfRepository;
    private CalibrationSettingsRepository calSettingsRepository;
    private CalibrationShapeFitterConstraintsRepository shapeConstraintsRepository;
    private PolygonRepository polygonRepository;
    private CalibrationSettings defaultCalSettings;
    private VelocityConfiguration defaultVelConf;
    private ShapeFitterConstraints defaultShapeFitterConstraint;
    private NotificationService notificationService;

    @Autowired
    public ConfigurationServiceImpl(EntityManager em, CalibrationSettingsRepository calSettingsRepository, VelocityConfigurationRepository velConfRepository,
            CalibrationShapeFitterConstraintsRepository shapeConstraintsRepository, CalibrationSettings defaultCalSettings, VelocityConfiguration defaultVelConf,
            ShapeFitterConstraints defaultShapeFitterConstraint, PolygonRepository polygonRepository, NotificationService notificationService) {
        this.em = em;
        this.defaultCalSettings = defaultCalSettings;
        this.calSettingsRepository = calSettingsRepository;
        this.velConfRepository = velConfRepository;
        this.shapeConstraintsRepository = shapeConstraintsRepository;
        this.defaultVelConf = defaultVelConf;
        this.defaultShapeFitterConstraint = defaultShapeFitterConstraint;
        this.polygonRepository = polygonRepository;
        this.notificationService = notificationService;
    }

    @PostConstruct
    private void setup() {
        update(defaultCalSettings);
        update(defaultVelConf);
        update(defaultShapeFitterConstraint);
    }

    @Override
    public CalibrationSettings update(CalibrationSettings entry) {
        CalibrationSettings mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = calSettingsRepository.findById(entry.getId()).orElse(null);
        } else {
            mergedEntry = calSettingsRepository.findFirstByOrderById();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.merge(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new CalibrationSettingsChangeEvent());
        return calSettingsRepository.saveAndFlush(mergedEntry);
    }

    @Override
    public CalibrationSettings getCalibrationSettings() {
        CalibrationSettings res = calSettingsRepository.findFirstByOrderById();
        em.detach(res);
        return res;
    }

    @Override
    public VelocityConfiguration update(VelocityConfiguration entry) {
        VelocityConfiguration mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = velConfRepository.findById(entry.getId()).orElse(null);
        } else {
            mergedEntry = velConfRepository.findFirstByOrderById();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.merge(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new GvDataChangeEvent());
        return velConfRepository.saveAndFlush(mergedEntry);
    }

    @Override
    public VelocityConfiguration getVelocityConfiguration() {
        VelocityConfiguration res = velConfRepository.findFirstByOrderById();
        em.detach(res);
        return res;
    }

    @Override
    public ShapeFitterConstraints update(ShapeFitterConstraints entry) {
        ShapeFitterConstraints mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = shapeConstraintsRepository.findById(entry.getId()).orElse(null);
        } else {
            mergedEntry = shapeConstraintsRepository.findFirstByOrderById();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.merge(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new ShapeConstraintsChangeEvent());
        return shapeConstraintsRepository.saveAndFlush(mergedEntry);
    }

    @Override
    public ShapeFitterConstraints getCalibrationShapeFitterConstraints() {
        ShapeFitterConstraints res = shapeConstraintsRepository.findFirstByOrderById();
        em.detach(res);
        return res;
    }

    @Override
    public String updatePolygon(String rawGeoJSON) {
        //TODO: Support multiples/additive updates
        polygonRepository.deleteAll();
        return polygonRepository.save(new GeoJsonPolygon().setRawGeoJson(rawGeoJSON)).getRawGeoJson();
    };

    @Override
    public String getPolygonGeoJSON() {
        return polygonRepository.findAll().stream().findAny().orElseGet(GeoJsonPolygon::new).getRawGeoJson();
    }

    @Override
    public Double getEpicentralDistance(GeodeticCoordinate coordA, GeodeticCoordinate coordB) {
        return EModel.getDistanceWGS84(coordA.getLat(), coordA.getLon(), coordB.getLat(), coordB.getLon());
    }

    @Override
    public Double getHypocentralDistance(GeodeticCoordinate coordA, GeodeticCoordinate coordB) {

        llnl.gnem.core.util.Geometry.GeodeticCoordinate convertedCoordA = new llnl.gnem.core.util.Geometry.GeodeticCoordinate(coordA.getLat(), coordA.getLon(), Math.copySign(coordA.getDepthKm(), -1d));
        llnl.gnem.core.util.Geometry.GeodeticCoordinate convertedCoordB = new llnl.gnem.core.util.Geometry.GeodeticCoordinate(coordB.getLat(), coordB.getLon(), coordB.getElevationKm());

        return EModel.getSeparationMeters(convertedCoordA, convertedCoordB) / 1000.0;
    }

    @Override
    public GeodeticCoordinate getEventCoord(Event event) {
        // Note we are assuming event depth is in meters, and converting to km for distance calculations in km
        return new GeodeticCoordinate(event.getLatitude(), event.getLongitude(), event.getDepth() / 1000.0);
    }

    @Override
    public GeodeticCoordinate getStationCoord(Station station) {
        // Note we are assuming station elevation is in meters, and converting to km for distance calculations in km
        return new GeodeticCoordinate(station.getLatitude(), station.getLongitude(), station.getElevation() / 1000.0);
    }

    @Override
    public WGS84DistanceCalcFunction getDistanceFunc() {

        String method = calSettingsRepository.findAll().stream().findAny().get().getDistanceCalcMethod();

        if (DistanceCalcMethod.HYPOCENTRAL.getValue().equalsIgnoreCase(method)) {
            return this::getHypocentralDistance;
        }

        return this::getEpicentralDistance;
    }
}
