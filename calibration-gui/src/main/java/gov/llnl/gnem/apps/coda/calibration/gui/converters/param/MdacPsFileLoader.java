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
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;
import reactor.core.publisher.Flux;

@Service
public class MdacPsFileLoader implements FileToParameterConverter<MdacParametersPS> {

    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*ps.*\\.(txt|dat)");

    @Override
    public Flux<Result<MdacParametersPS>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
            return Flux.fromIterable(convert(file));
        }
        return Flux.empty();
    }

    @Override
    public Flux<Result<MdacParametersPS>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    public List<Result<MdacParametersPS>> convert(File file) {
        if (file == null) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable. %s",
                                                                                                                  "NULL",
                                                                                                                  "File reference is null"))));
        }

        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.parallel().filter(l -> !l.isEmpty()).map(line -> {
                MdacParametersPS param = new MdacParametersPS();

                StringTokenizer tokenizer = new StringTokenizer(line);
                param.setPhase(tokenizer.nextToken().trim());
                param.setQ0(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setDelQ0(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setGamma0(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setDelGamma0(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setU0(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setEta(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setDelEta(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setDistCrit(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setSnr(Double.parseDouble(tokenizer.nextToken().trim()));

                return new Result<>(true, param);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): %s", file.getName(), e.getMessage()), e)));
        }
    }

    private Result<MdacParametersPS> exceptionalResult(Exception error) {
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        return exceptionalResult(exceptions);
    }

    private Result<MdacParametersPS> exceptionalResult(List<Exception> errors) {
        return new Result<>(false, errors, null);
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return filter;
    }
}
