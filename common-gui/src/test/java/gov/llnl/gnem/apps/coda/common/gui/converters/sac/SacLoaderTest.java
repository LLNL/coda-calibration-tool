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
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

public class SacLoaderTest {

    private SacLoader loader;

    @BeforeAll
    public static void setupBeforeClass() {
    }

    @BeforeEach
    public void setup() {
        loader = new SacLoader();
    }

    @AfterEach
    public void teardown() {
        loader = null;
    }

    @ParameterizedTest
    @MethodSource("singleFile")
    public void testConvertFile(File inputFile) throws Exception {
        Result<Waveform> res = loader.convertFile(inputFile).doOnError(error -> Assert.fail(error.getMessage())).block(Duration.ofSeconds(10l));
        Assert.assertTrue("Expect that waveform results should all complete successfully", res.isSuccess());
        Assert.assertTrue("Expect that waveform results should have no error or warning messages", res.getErrors().isEmpty());
        Assert.assertTrue("Expect that waveform results should have a Waveform payload", res.getResultPayload().isPresent());
    }

    public static Collection<Arguments> singleFile() throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get("src/test/resources/sac/"))) {
            return stream.filter(path -> path.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".sac")).map(path -> {
                return Arguments.of(path.toFile());
            }).collect(Collectors.toList());
        }
    }

    @Test
    public void testConvertFiles() throws Exception {
        List<List<File>> multipleFiles = IntStream.range(0, 3).mapToObj(i -> {
            try (Stream<Path> stream = Files.walk(Paths.get("src/test/resources/sac/"))) {
                return stream.filter(path -> path.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".sac")).map(path -> path.toFile()).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(o -> o != null).collect(Collectors.toList());
        List<Result<Waveform>> results = loader.convertFiles(multipleFiles.get(0)).doOnError(error -> Assert.fail(error.getMessage())).collectList().block(Duration.ofSeconds(10l));
        for (Result<Waveform> res : results) {
            Assert.assertTrue("Expect that waveform results should all complete successfully", res.isSuccess());
            Assert.assertTrue("Expect that waveform results should have no error or warning messages", res.getErrors().isEmpty());
            Assert.assertTrue("Expect that waveform results should have a Waveform payload", res.getResultPayload().isPresent());
        }
    }

    @Test
    public void testEventDepthPopulated() throws Exception {
        //GMPAPPS-1947 Envelope tool was dropping event depth info.
        List<List<File>> multipleFiles = IntStream.range(0, 3).mapToObj(i -> {
            try (Stream<Path> stream = Files.walk(Paths.get("src/test/resources/sac/"))) {
                return stream.filter(path -> path.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".sac")).map(path -> path.toFile()).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(o -> o != null).collect(Collectors.toList());
        List<Result<Waveform>> results = loader.convertFiles(multipleFiles.get(0)).doOnError(error -> Assert.fail(error.getMessage())).collectList().block(Duration.ofSeconds(10l));
        for (Result<Waveform> res : results) {
            Assert.assertTrue("Expect that the waveform should have a populated event depth if the original file had it.", res.getResultPayload().get().getEvent().getDepth() != 0);
        }
    }
}
