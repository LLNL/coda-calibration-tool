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

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.MethodSource;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

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

        params.add(
                new Object[] { new Waveform().setLowFrequency(.0)
                                             .setHighFrequency(.0)
                                             .setSegmentType("vel")
                                             .setSampleRate(.0)
                                             .setBeginTime(Date.from(Instant.now()))
                                             .setEndTime(Date.from(Instant.now()))
                                             .setStream(new Stream().setChannelName("STACK").setStation(new Station().setStationName("RNG")))
                                             .setEvent(new Event().setEventId("12345").setOriginTime(Date.from(Instant.now()))),
                        "RNG_STACK_12345_0.0_0.0_VEL_.ENV", waveformValidAssertions() });
        return params;
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
        Assert.assertEquals(expectedFilename, actual);
    }

    private static Consumer<Result<String>> waveformNotValidAssertions() throws Exception {
        return res -> {
            Assert.assertFalse("Result on an invalid waveform should be false", res.isSuccess());
            try {
                java.util.stream.Stream<Path> stream = Files.list(testDir.toPath());
                Assert.assertFalse("Files should never be created for an invalid waveform", stream.findFirst().isPresent());
                stream.close();
            } catch (IOException e) {
                Assert.fail("Unable to list the contents of the test directory: " + testDir.getAbsolutePath() + " : " + e.toString());
            }
        };
    }

    private static Consumer<Result<String>> waveformValidAssertions() {
        return res -> {
            Assert.assertTrue("Result on an valid waveform should be true: " + res.getResultPayload().get(), res.isSuccess());
            Path filePath = testDir.toPath().resolve(res.getResultPayload().get());
            Assert.assertTrue("File should be created for valid waveforms", Files.exists(filePath));
            try {
                Files.delete(filePath);
            } catch (IOException e) {
                Assert.fail("Temporary files should never exist after a test is complete but an error occured during deletion: " + e.toString());
            }
        };
    }
}
