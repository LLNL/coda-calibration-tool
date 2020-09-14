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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.model.domain.GeoJsonPolygon;
import gov.llnl.gnem.apps.coda.calibration.repository.PolygonRepository;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.repository.WaveformRepository;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@ExtendWith(MockitoExtension.class)
public class GeometryServiceImplTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @InjectMocks
    private GeometryServiceImpl geometryService;

    @Mock
    private WaveformRepository waveformRepository;

    @Mock
    private PolygonRepository polygonRepository;

    @Mock
    private NotificationService notificationService;

    private List<Waveform> waveforms = new ArrayList<>();

    private List<GeoJsonPolygon> polygons = new ArrayList<>();

    @BeforeEach
    protected void setUp() throws Exception {
        Waveform w1 = createWaveform();
        w1.setId(0l);
        w1.setActive(false);
        w1.getEvent().setLatitude(-2.0);
        w1.getEvent().setLongitude(-2.0);
        w1.getStream().getStation().setLatitude(-1.0);
        w1.getStream().getStation().setLongitude(-1.0);
        waveforms.add(w1);

        Waveform w2 = createWaveform();
        w2.setId(1l);
        w2.setActive(true);
        w2.getEvent().setEventId("2345");
        w2.getEvent().setLatitude(-2.0);
        w2.getEvent().setLongitude(2.0);
        w2.getStream().getStation().setLatitude(-1.0);
        w2.getStream().getStation().setLongitude(1.0);
        waveforms.add(w2);

        Waveform w3 = createWaveform();
        w3.setId(2l);
        w3.setActive(true);
        w3.getStream().getStation().setStationName("TEST2");
        w3.getEvent().setLatitude(2.0);
        w3.getEvent().setLongitude(-2.0);
        w3.getStream().getStation().setLatitude(1.0);
        w3.getStream().getStation().setLongitude(-1.0);
        waveforms.add(w3);

        Waveform w4 = createWaveform();
        w4.setId(3l);
        w4.setActive(false);
        w4.getEvent().setEventId("2345");
        w4.getStream().getStation().setStationName("TEST2");
        w4.getEvent().setLatitude(2.0);
        w4.getEvent().setLongitude(2.0);
        w4.getStream().getStation().setLatitude(1.0);
        w4.getStream().getStation().setLongitude(1.0);
        waveforms.add(w4);

        polygons.add(new GeoJsonPolygon().setRawGeoJson("{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[-3.0,-3.0],[3.0,-3.0],[3.0,0.0],[-3.0,0.0],[-3.0,-3.0]]]}}]}"));
        Mockito.when(polygonRepository.findAll()).thenReturn(polygons);
        
        Mockito.when(waveformRepository.setActiveIn(Mockito.anyBoolean(), Mockito.anyList()))
               .thenAnswer(invocation -> {
                   boolean active = (boolean) invocation.getArgument(0);
                   List<Long> ids = (List<Long>) invocation.getArgument(1);
                   List<Waveform> selectedWaveforms = waveforms.stream().filter(w -> ids.contains(w.getId())).collect(Collectors.toList());
                   selectedWaveforms.forEach(w->w.setActive(active));
                   return selectedWaveforms.size();
               });
    }

    @AfterEach
    protected void tearDown() throws Exception {
        waveforms.clear();
    }

    @Test
    public void testExcludeInside() throws Exception {
        testInside(false);
    }

    @Test
    public void testIncludeInside() throws Exception {
        testInside(true);
    }

    private void testInside(boolean active) {        
        Mockito.when(waveformRepository.getMetadataInsideBounds(Mockito.anyBoolean(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble(), Mockito.anyDouble())).thenReturn(waveforms);
        List<Long> ids = geometryService.setActiveFlagInsidePolygon(active);
        List<Long> activeIds = waveforms.stream().filter(w -> w.getActive() == active).map(w -> w.getId()).collect(Collectors.toList());
        
        Assertions.assertArrayEquals(new long[] { 0l, 1l }, ArrayUtils.toPrimitive(ids.toArray(new Long[0])), "Expecting the first and second waveforms to toggle their active status.");
        Assertions.assertTrue(activeIds.containsAll(ids), "Expecting the included/excluded waveform list to contain the selected ids");
    }

    @Test
    public void testExcludeOutside() throws Exception {
        testOutside(false, 2l);
    }

    @Test
    public void testIncludeOutside() throws Exception {
        testOutside(true, 3l);
    }

    private void testOutside(boolean active, long... expectedIds) {
        Mockito.when(waveformRepository.getWaveformMetadataByActive(Mockito.anyBoolean()))
               .then(invocation -> waveforms.stream().filter(w -> w.getActive() == (boolean) invocation.getArgument(0)).collect(Collectors.toList()));
        
        List<Long> ids = geometryService.setActiveFlagOutsidePolygon(active);
        List<Long> activeIds = waveforms.stream().filter(w -> w.getActive() == active).map(w -> w.getId()).collect(Collectors.toList());
        Assertions.assertArrayEquals(expectedIds, ArrayUtils.toPrimitive(ids.toArray(new Long[0])));
        Assertions.assertTrue(activeIds.containsAll(ids), "Expecting the included/excluded waveform list to contain the selected ids");
    }

    private Waveform createWaveform() {
        Date startTime = Date.from(Instant.now());
        Date endTime = Date.from(startTime.toInstant().plusSeconds(1l));

        Event event = new Event();
        event.setEventId("1234");
        event.setLatitude(1.0);
        event.setLongitude(1.0);

        Station station = new Station();
        station.setLatitude(1.0);
        station.setLongitude(1.0);
        station.setStationName("TEST");
        station.setNetworkName(null);

        Stream s1 = new Stream();
        s1.setChannelName("BHE");
        s1.setStation(station);

        double[] data = new double[400];
        Arrays.fill(data, 0, 200, 1000.0);
        Arrays.fill(data, 2, 400, 2000.0);
        Waveform w1 = new Waveform();
        w1.setEvent(event);
        w1.setStream(s1);
        w1.setSegment(data);
        w1.setBeginTime(startTime);
        w1.setEndTime(endTime);
        w1.setSampleRate(16d);
        w1.setLowFrequency(0d);
        w1.setHighFrequency(1d);
        return w1;
    }

}
