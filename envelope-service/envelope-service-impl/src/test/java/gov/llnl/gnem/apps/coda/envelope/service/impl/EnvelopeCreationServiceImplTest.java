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
package gov.llnl.gnem.apps.coda.envelope.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.envelope.model.domain.Default14BandEnvelopeJobConfiguration;
import gov.llnl.gnem.apps.coda.envelope.service.api.EnvelopeParamsService;

@ExtendWith(MockitoExtension.class)
public class EnvelopeCreationServiceImplTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private EnvelopeParamsService params;

    private EnvelopeCreationServiceImpl envelopeCreationService;

    @BeforeAll
    protected static void setUpBeforeClass() throws Exception {
    }

    @AfterAll
    protected static void tearDownAfterClass() throws Exception {
    }

    @BeforeEach
    protected void setUp() throws Exception {
        WaveformToTimeSeriesConverter converter = new WaveformToTimeSeriesConverter();
        envelopeCreationService = new EnvelopeCreationServiceImpl(converter, params, new EnvelopeStacker(converter));
    }

    @AfterEach
    protected void tearDown() throws Exception {
    }

    @Test
    public void testCreateEnvelopesDefault14Bands() throws Exception {
        Mockito.when(params.getConfiguration()).thenReturn(Default14BandEnvelopeJobConfiguration.getConfiguration());
        List<Waveform> waveforms = new ArrayList<>();

        waveforms = generateWaveforms();

        Result<List<Waveform>> stacks = envelopeCreationService.createEnvelopes(1l, waveforms, null, false);
        assertTrue(stacks.isSuccess());
        assertTrue(stacks.getResultPayload().isPresent());
        long eventCount = waveforms.stream().map(w -> w.getEvent().getEventId()).distinct().count();
        long bandCount = params.getConfiguration().getFrequencyBandConfiguration().size();
        long channelCount = waveforms.stream().map(w -> w.getStream().getChannelName()).distinct().count();
        assertEquals(bandCount * eventCount * channelCount, stacks.getResultPayload().get().stream().distinct().count());
    }

    @Test
    public void testCreateStacksDefault14Bands() throws Exception {
        Mockito.when(params.getConfiguration()).thenReturn(Default14BandEnvelopeJobConfiguration.getConfiguration());
        List<Waveform> waveforms = new ArrayList<>();

        waveforms = generateWaveforms();

        Result<List<Waveform>> stacks = envelopeCreationService.createEnvelopes(1l, waveforms, null, true);
        assertTrue(stacks.isSuccess());
        assertTrue(stacks.getResultPayload().isPresent());

        long eventCount = waveforms.stream().map(w -> w.getEvent().getEventId()).distinct().count();
        long bandCount = params.getConfiguration().getFrequencyBandConfiguration().size();
        assertEquals(bandCount * eventCount, stacks.getResultPayload().get().stream().distinct().count());
    }

    private List<Waveform> generateWaveforms() {
        Date startTime = Date.from(Instant.now());
        Date endTime = Date.from(startTime.toInstant().plusSeconds(1l));

        Event event = new Event();
        event.setEventId("1234");
        event.setLatitude(1.0);
        event.setLongitude(1.0);

        Event event2 = new Event();
        event2.setEventId("2345");
        event2.setLatitude(1.0);
        event2.setLongitude(1.0);

        Station station = new Station();
        station.setLatitude(1.0);
        station.setLongitude(1.0);
        station.setStationName("TEST");
        station.setNetworkName(null);

        Stream s1 = new Stream();
        s1.setChannelName("BHE");
        s1.setStation(station);

        Stream s2 = new Stream();
        s2.setChannelName("BHN");
        s2.setStation(station);

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

        Arrays.fill(data, 0, 200, 1000.0);
        Arrays.fill(data, 2, 400, 2000.0);
        Waveform w2 = new Waveform();
        w2.setEvent(event);
        w2.setStream(s2);
        w2.setSegment(data);
        w2.setBeginTime(startTime);
        w2.setEndTime(endTime);
        w2.setSampleRate(16d);

        Waveform w3 = Waveform.mergeNonNullOrEmptyFields(w1, new Waveform());
        w3.setEvent(event2);
        Waveform w4 = Waveform.mergeNonNullOrEmptyFields(w2, new Waveform());
        w4.setEvent(event2);

        List<Waveform> waveforms = new ArrayList<>(2);
        waveforms.add(w1);
        waveforms.add(w2);

        //Toss some duplicates in just to see
        waveforms.add(w1);
        waveforms.add(w2);

        waveforms.add(w3);
        waveforms.add(w4);
        return waveforms;
    }
}
