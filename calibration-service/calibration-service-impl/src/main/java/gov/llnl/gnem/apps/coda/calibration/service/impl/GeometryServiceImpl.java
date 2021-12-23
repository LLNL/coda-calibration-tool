/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.util.GeometryCombiner;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.wololo.geojson.Feature;
import org.wololo.geojson.FeatureCollection;
import org.wololo.geojson.GeoJSONFactory;
import org.wololo.jts2geojson.GeoJSONReader;

import gov.llnl.gnem.apps.coda.calibration.model.domain.GeoJsonPolygon;
import gov.llnl.gnem.apps.coda.calibration.repository.PolygonRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.GeometryService;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import gov.llnl.gnem.apps.coda.common.repository.WaveformRepository;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Service
public class GeometryServiceImpl implements GeometryService {

    private static final Logger log = LoggerFactory.getLogger(GeometryServiceImpl.class);

    private WaveformRepository waveformRepository;
    private PolygonRepository polygonRepository;
    private NotificationService notificationService;
    private final GeometryFactory geomFactory;

    @Autowired
    public GeometryServiceImpl(WaveformRepository waveformRepository, PolygonRepository polygonRepository, NotificationService notificationService) {
        this.waveformRepository = waveformRepository;
        this.polygonRepository = polygonRepository;
        this.notificationService = notificationService;
        this.geomFactory = new GeometryFactory();
    }

    @Override
    public List<Long> setActiveFlagOutsidePolygon(boolean active) {
        return toggleActiveByPolygon(false, active);
    }

    @Override
    public List<Long> setActiveFlagInsidePolygon(boolean active) {
        return toggleActiveByPolygon(true, active);
    }

    private List<Long> toggleActiveByPolygon(boolean inside, boolean active) {
        List<Long> ids = new ArrayList<>();

        List<GeoJsonPolygon> geoJSON = polygonRepository.findAll();
        List<Long> selectedWaveformIds = findIdsByPolygonAndActiveStatus(geoJSON, inside, !active);

        if (!selectedWaveformIds.isEmpty()) {
            int updatedRows = waveformRepository.setActiveIn(active, selectedWaveformIds);
            if (updatedRows > 0) {
                ids.addAll(selectedWaveformIds);
            }
        }

        if (!ids.isEmpty()) {
            notificationService.post(new WaveformChangeEvent(ids).setAddOrUpdate(true));
        }
        return ids;
    }

    private List<Long> findIdsByPolygonAndActiveStatus(List<GeoJsonPolygon> geoJSON, boolean inside, boolean active) {
        Set<Long> selectedWaveformIds = new HashSet<>();
        if (geoJSON != null && !geoJSON.isEmpty()) {
            try {
                FeatureCollection featureCollection = (FeatureCollection) GeoJSONFactory.create(geoJSON.get(0).getRawGeoJson());

                GeoJSONReader reader = new GeoJSONReader();

                //Combine the geometries
                List<Geometry> geoms = new ArrayList<>();
                for (Feature feature : featureCollection.getFeatures()) {
                    geoms.add(reader.read(feature.getGeometry()));
                }

                //BufferOp here helps us repair the any self intersections, important!
                PreparedGeometry geo = PreparedGeometryFactory.prepare(BufferOp.bufferOp(GeometryCombiner.combine(geoms), 0.0));

                Geometry bounds = geo.getGeometry().getEnvelope();
                //minx miny, maxx miny, maxx maxy, minx maxy, minx miny
                if (bounds.getNumPoints() == 5) {
                    Coordinate[] coords = bounds.getCoordinates();

                    //Test for polygon intersection and drop any that fail
                    Set<Waveform> possibleWaveforms;
                    BiFunction<Boolean, Boolean, Boolean> test;
                    if (inside) {
                        possibleWaveforms = new HashSet<>(waveformRepository.getMetadataInsideBounds(
                                active,
                                    Math.min(coords[0].getY(), coords[2].getY()),
                                    Math.min(coords[0].getX(), coords[2].getX()),
                                    Math.max(coords[0].getY(), coords[2].getY()),
                                    Math.max(coords[0].getX(), coords[2].getX())));

                        Collection<Waveform> stationWaveforms = waveformRepository.findAllMetadataByIds(
                                possibleWaveforms.parallelStream()
                                                 .map(w -> w.getStream().getStation().getStationName())
                                                 .distinct()
                                                 .flatMap(stationName -> waveformRepository.findAllIdsByStationName(stationName).stream())
                                                 .distinct()
                                                 .collect(Collectors.toList()));
                        possibleWaveforms.addAll(stationWaveforms);

                        test = (resEv, resSta) -> resSta == true || resEv == true;
                    } else {
                        possibleWaveforms = new HashSet<>(waveformRepository.getWaveformMetadataByActive(active));
                        test = (resEv, resSta) -> resSta == false || resEv == false;
                    }

                    selectedWaveformIds.addAll(possibleWaveforms.stream().filter(w -> {
                        boolean resEv = geo.covers(geomFactory.createPoint(new Coordinate(w.getEvent().getLongitude(), w.getEvent().getLatitude())));
                        boolean resSta = geo.covers(geomFactory.createPoint(new Coordinate(w.getStream().getStation().getLongitude(), w.getStream().getStation().getLatitude())));
                        return test.apply(resEv, resSta);
                    }).map(Waveform::getId).collect(Collectors.toList()));

                }
            } catch (RuntimeException ex) {
                log.error(ex.getLocalizedMessage());
            }
        }
        return new ArrayList<>(selectedWaveformIds);
    }
}
