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
package gov.llnl.gnem.apps.coda.common.gui.converters.sac;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.MethodSource;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import llnl.gnem.core.io.SAC.SACHeader;

public class SacExporterTest {

    private SacExporter exporter;

    private static File testDir;

    private static Collection<Object[]> testParamSet() throws Exception {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] { null, "", waveformNotValidAssertions() });
        params.add(new Object[] { new Waveform(), "", waveformNotValidAssertions() });
        params.add(new Object[] { new Waveform().setEvent(new Event()), "", waveformNotValidAssertions() });
        params.add(new Object[] { new Waveform().setStream(new Stream()), "", waveformNotValidAssertions() });
        params.add(new Object[] { new Waveform().setStream(new Stream()).setEvent(new Event()), "", waveformNotValidAssertions() });
        params.add(new Object[] { new Waveform().setStream(new Stream().setStation(new Station())).setEvent(new Event()), "", waveformNotValidAssertions() });
        params.add(
                new Object[] { new Waveform().setLowFrequency(.0).setHighFrequency(.0).setSegmentType("vel").setStream(new Stream().setStation(new Station())).setEvent(new Event()), "",
                        waveformNotValidAssertions() });

        params.add(new Object[] { createValidWaveform(), "RNG_STACK_12345_0.00_0.00_VEL_.ENV", waveformValidAssertions() });
        return params;
    }

    public static Waveform createValidWaveform() {
        return new Waveform().setLowFrequency(.0)
                             .setHighFrequency(.0)
                             .setSegmentType("vel")
                             .setSampleRate(.0)
                             .setBeginTime(Date.from(Instant.now()))
                             .setEndTime(Date.from(Instant.now()))
                             .setStream(new Stream().setChannelName(Stream.TYPE_STACK).setStation(new Station().setStationName("RNG")))
                             .setEvent(new Event().setEventId("12345").setDepth(100.0).setOriginTime(Date.from(Instant.now())));
    }

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        testDir = Files.createDirectory(Paths.get(".sac_exporter_test_" + Instant.now().toEpochMilli())).toFile();
        testDir.deleteOnExit();
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        Files.deleteIfExists(testDir.toPath());
    }

    @BeforeEach
    public void setUp() throws Exception {
        exporter = new SacExporter();
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @ParameterizedTest
    @MethodSource("testParamSet")
    public void testWriteWaveformToDirectory(Waveform input, ArgumentsAccessor arguments) throws Exception {
        @SuppressWarnings("unchecked")
        Consumer<Result<String>> assertions = arguments.get(2, Consumer.class);
        assertions.accept(exporter.writeWaveformToDirectory(testDir, input));
    }

    @ParameterizedTest
    @MethodSource("testParamSet")
    public void testGetFileName(Waveform input, String expectedFilename) throws Exception {
        String actual = exporter.getFileName(input);
        Assertions.assertEquals(expectedFilename, actual);
    }

    @Test
    public void testEventDepthPopulated() throws Exception {
        //GMPAPPS-1947 Envelope tool was dropping event depth info during import/export.
        Waveform waveform = createValidWaveform();
        SACHeader header = exporter.sacHeaderFromWaveform(waveform);
        Assertions.assertTrue(header.evdp != 0 && !SACHeader.isDefault(header.evdp), "Expect that the waveform should have a populated event depth if the original file had it.");
    }

    private static Consumer<Result<String>> waveformNotValidAssertions() throws Exception {
        return res -> {
            Assertions.assertFalse(res.isSuccess(), "Result on an invalid waveform should be false");
            try {
                java.util.stream.Stream<Path> stream = Files.list(testDir.toPath());
                Assertions.assertFalse(stream.findFirst().isPresent(), "Files should never be created for an invalid waveform");
                stream.close();
            } catch (IOException e) {
                Assertions.fail("Unable to list the contents of the test directory: " + testDir.getAbsolutePath() + " : " + e.toString());
            }
        };
    }

    private static Consumer<Result<String>> waveformValidAssertions() {
        return res -> {
            Assertions.assertTrue(res.isSuccess(), "Result on an valid waveform should be true: " + res.getResultPayload().get());
            Path filePath = testDir.toPath().resolve(res.getResultPayload().get());
            Assertions.assertTrue(Files.exists(filePath), "File should be created for valid waveforms");
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                Assertions.fail("Temporary files should never exist after a test is complete but an error occured during deletion: " + e.toString());
            }
        };
    }
}
