/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.sac;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;

@RunWith(Parameterized.class)
public class SacLoaderTestSingle {

    private SacLoader loader;

    @Parameter
    public File singleFile;

    @BeforeClass
    public static void setupBeforeClass() {
    }

    @Before
    public void setup() {
        loader = new SacLoader();
    }

    @After
    public void teardown() {
        loader = null;
    }

    @Test
    public void testConvertFile() throws Exception {
        Result<Waveform> res = loader.convertFile(singleFile).doOnError(error -> Assert.fail(error.getMessage())).block(Duration.ofSeconds(10l));
        Assert.assertTrue("Expect that waveform result should all complete successfully", res.isSuccess());
        Assert.assertTrue("Expect that waveform result should have no error or warning messages", res.getErrors().isEmpty());
        Assert.assertTrue("Expect that waveform result should have a Waveform payload", res.getResultPayload().isPresent());
    }

    @Test
    public void testConvertFiles() throws Exception {
        List<List<File>> multipleFiles = IntStream.range(0, 3).mapToObj(i -> {
            try (Stream<Path> stream = Files.walk(Paths.get("src/test/resources/sac/"))) {
                return stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".sac")).map(path -> path.toFile()).collect(Collectors.toList());
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(o -> o != null).collect(Collectors.toList());
        List<Result<Waveform>> results = loader.convertFiles(multipleFiles.get(0)).doOnError(error -> Assert.fail(error.getMessage())).collectList().block(Duration.ofSeconds(10l));
        for (Result<Waveform> res : results) {
            Assert.assertTrue("Expect that waveform results should all complete successfully", res.isSuccess());
            Assert.assertTrue("Expect that waveform result should have no error or warning messages", res.getErrors().isEmpty());
            Assert.assertTrue("Expect that waveform result should have a Waveform payload", res.getResultPayload().isPresent());
        }
    }

    @Parameters(name = "singleFile")
    public static Collection<Object[]> singleFile() throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get("src/test/resources/sac/"))) {
            return stream.filter(path -> path.getFileName().toString().toLowerCase().endsWith(".sac")).map(path -> {
                return new Object[] { path.toFile() };
            }).collect(Collectors.toList());
        }
    }

}
