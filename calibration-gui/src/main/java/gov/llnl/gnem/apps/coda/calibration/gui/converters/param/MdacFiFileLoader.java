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
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import reactor.core.publisher.Flux;

@Service
public class MdacFiFileLoader implements FileToParameterConverter<MdacParametersFI> {

    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*fi.*\\.(txt|dat)");

    @Override
    public Flux<Result<MdacParametersFI>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
            return Flux.fromIterable(convert(file));
        }
        return Flux.empty();
    }

    @Override
    public Flux<Result<MdacParametersFI>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    public List<Result<MdacParametersFI>> convert(File file) {
        if (file == null) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable. %s",
                                                                                                                  "NULL",
                                                                                                                  "File reference is null"))));
        }

        try (Stream<String> lines = Files.lines(file.toPath())) {
            return lines.skip(1l).parallel().filter(l -> !l.isEmpty()).map(line -> {
                MdacParametersFI param = new MdacParametersFI();

                StringTokenizer tokenizer = new StringTokenizer(line);
                param.setSigma(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setDelSigma(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setPsi(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setDelPsi(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setZeta(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setM0ref(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setAlphas(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setBetas(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setRhos(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setRadPatP(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setRadPatS(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setAlphaR(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setBetaR(Double.parseDouble(tokenizer.nextToken().trim()));
                param.setRhor(Double.parseDouble(tokenizer.nextToken().trim()));

                return new Result<>(true, param);
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.singletonList(exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): %s", file.getName(), e.getMessage()), e)));
        }
    }

    private Result<MdacParametersFI> exceptionalResult(Exception error) {
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        return exceptionalResult(exceptions);
    }

    private Result<MdacParametersFI> exceptionalResult(List<Exception> errors) {
        return new Result<>(false, errors, null);
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return filter;
    }
}
