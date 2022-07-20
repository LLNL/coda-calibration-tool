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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.repository.SharedFrequencyBandParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MdacCalculatorService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SyntheticCodaModel;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;

@ExtendWith(MockitoExtension.class)
public class SiteCalibrationServiceImplTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Mock
    private MdacCalculatorService mdac;

    @Mock
    private MeasuredMwsService measuredMwsService;

    @Mock
    private SiteFrequencyBandParametersService siteParamsService;

    private WaveformToTimeSeriesConverter converter = new WaveformToTimeSeriesConverter();

    @Mock
    private SyntheticCodaModel syntheticCodaModel;

    @Mock
    private MdacParametersFiService mdacFiService;

    @Mock
    private MdacParametersPsService mdacPsService;

    @Mock
    private VelocityConfiguration velConf;

    @Mock
    private ServiceConfig svcConf;

    @Mock
    private SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository;

    @InjectMocks
    private SiteCalibrationServiceImpl siteCalibrationServiceImpl;

    @BeforeEach
    protected void setUp() throws Exception {
        Mockito.when(mdac.getCalculateMdacSourceSpectraFunction(Mockito.any(), Mockito.any(), Mockito.anyDouble())).thenReturn(f -> new double[] { 1.0, 1.0, 1.0, 1.0 });
        Mockito.when(mdac.getCalculateMdacAmplitudeForMwFunction(Mockito.any(), Mockito.any(), Mockito.anyDouble(), Mockito.any(), Mockito.anyDouble())).thenReturn(f -> Double.valueOf(1.0d));
        Mockito.when(mdac.getCalculateMdacAmplitudeForMwFunction(Mockito.any(), Mockito.any(), Mockito.anyDouble(), Mockito.any())).thenReturn((DoubleUnaryOperator) operand -> 0);
        Mockito.when(sharedFrequencyBandParametersRepository.findDistinctFrequencyBands()).thenReturn(new ArrayList<FrequencyBand>());
        Mockito.when(mdacFiService.findFirst()).thenReturn(new MdacParametersFI());
        Mockito.when(mdacPsService.findMatchingPhase(Mockito.any())).thenReturn(new MdacParametersPS());

        SpectraCalculator spectraCalc = new SpectraCalculator(converter, syntheticCodaModel, mdac, mdacFiService, mdacPsService, velConf);
        siteCalibrationServiceImpl.setSpectraCalc(spectraCalc);
        siteCalibrationServiceImpl.setServiceConfig(new ServiceConfig());
    }

    @AfterEach
    protected void tearDown() throws Exception {
    }

    @Test
    public void testSiteCorrectionsOneStationEvent() throws Exception {

        //Testing a fix for a NPE caused when the following three statements are true.
        //1) You have a station that saw no calibration events ("InvalidEventStation")
        //2) That station shares a non-calibration event with a station that DID see a calibration event ("TEST" in this case)
        //3) The station with no calibration event ALSO has an event that no other station saw ("3456").

        Waveform w1 = createWaveform();

        Waveform w2 = createWaveform();
        w2.getEvent().setEventId("2345");

        Waveform w3 = createWaveform();
        w3.getEvent().setEventId("2345");
        w3.getStream().getStation().setStationName("InvalidEventStation");

        Waveform w4 = createWaveform();
        w4.getEvent().setEventId("3456");
        w4.getStream().getStation().setStationName("InvalidEventStation");

        SpectraMeasurement sm = new SpectraMeasurement();
        sm.setWaveform(w1);
        sm.setPathCorrected(1d);
        sm.setRawAtMeasurementTime(1d);
        sm.setRawAtStart(1d);

        SpectraMeasurement sm2 = new SpectraMeasurement();
        sm2.setWaveform(w2);
        sm2.setPathCorrected(1d);
        sm2.setRawAtMeasurementTime(1d);
        sm2.setRawAtStart(1d);

        SpectraMeasurement sm3 = new SpectraMeasurement();
        sm3.setWaveform(w3);
        sm3.setPathCorrected(1d);
        sm3.setRawAtMeasurementTime(1d);
        sm3.setRawAtStart(1d);

        SpectraMeasurement sm4 = new SpectraMeasurement();
        sm4.setWaveform(w4);
        sm4.setPathCorrected(1d);
        sm4.setRawAtMeasurementTime(1d);
        sm4.setRawAtStart(1d);

        List<SpectraMeasurement> spectra = new ArrayList<>(1);
        spectra.add(sm);
        spectra.add(sm2);
        spectra.add(sm3);
        spectra.add(sm4);

        Map<FrequencyBand, List<SpectraMeasurement>> spectraMap = new HashMap<>();
        spectraMap.put(new FrequencyBand(0d, 1d), spectra);

        Map<PICK_TYPES, MdacParametersPS> mdacPsMap = new HashMap<>();
        mdacPsMap.put(PICK_TYPES.LG, new MdacParametersPS());

        Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = new HashMap<>();
        frequencyBandParameterMap.put(new FrequencyBand(0d, 1d), new SharedFrequencyBandParameters());

        Map<String, List<ReferenceMwParameters>> refEventMap = new HashMap<>();
        refEventMap.put("1234", Collections.singletonList(new ReferenceMwParameters().setRefMw(5d).setEventId("1234")));

        Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> results = siteCalibrationServiceImpl.measureSiteCorrections(
                spectraMap,
                    new MdacParametersFI(),
                    mdacPsMap,
                    refEventMap,
                    frequencyBandParameterMap,
                    PICK_TYPES.LG);

        assertEquals(1, results.size(), "Should have one frequency band");
        Map<Station, SiteFrequencyBandParameters> stations = results.values().stream().findFirst().get();
        assertEquals(2, stations.size(), "Should have two stations");
        assertTrue(stations.values().stream().allMatch(sfb -> sfb.getSiteTerm() == 6.0), "Both stations should have the same site term");
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
