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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.param;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToParameterConverter;
import gov.llnl.gnem.apps.coda.calibration.gui.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;
import reactor.core.publisher.Flux;

@Service
public class CodaParamFileLoader implements FileToParameterConverter<SharedFrequencyBandParameters> {

    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.param");

    @Override
    public Flux<Result<SharedFrequencyBandParameters>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
            return Flux.fromIterable(convertParamFileToSharedBandParameters(file));
        }
        return Flux.empty();
    }

    @Override
    public Flux<Result<SharedFrequencyBandParameters>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    public List<Result<SharedFrequencyBandParameters>> convertParamFileToSharedBandParameters(File file) {
        if (file == null) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable. %s",
                                                                                                                  "NULL",
                                                                                                                  "File reference is null"))));
        }

        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.skip(1l).parallel().filter(l -> !l.isEmpty()).map(line -> {
                SharedFrequencyBandParameters sfb = new SharedFrequencyBandParameters();

                StringTokenizer tokenizer = new StringTokenizer(line, " _\t\r\n\f");
                // the first two entries are low and high ends of the pass band (e.g. formatted either like 0.01 0.05  or 0.01_0.05)
                sfb.setLowFrequency(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setHighFrequency(Double.parseDouble(tokenizer.nextToken().trim()));

                sfb.setVelocity0(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setVelocity1(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setVelocity2(Double.parseDouble(tokenizer.nextToken().trim()));

                sfb.setMinSnr(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setMaxLength(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setMeasurementTime(Double.parseDouble(tokenizer.nextToken().trim()));

                sfb.setBeta0(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setBeta1(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setBeta2(Double.parseDouble(tokenizer.nextToken().trim()));

                sfb.setGamma0(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setGamma1(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setGamma2(Double.parseDouble(tokenizer.nextToken().trim()));

                sfb.setS1(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setS2(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setXc(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setXt(Double.parseDouble(tokenizer.nextToken().trim()));
                sfb.setQ(Double.parseDouble(tokenizer.nextToken().trim()));

                return new Result<>(true, sfb);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): %s", file.getName(), e.getMessage()), e)));
        }
    }

    private Result<SharedFrequencyBandParameters> exceptionalResult(Exception error) {
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        return exceptionalResult(exceptions);
    }

    private Result<SharedFrequencyBandParameters> exceptionalResult(List<Exception> errors) {
        return new Result<>(false, errors, null);
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return filter;
    }
}
