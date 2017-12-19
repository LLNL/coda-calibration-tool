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
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToReferenceEventConverter;
import gov.llnl.gnem.apps.coda.calibration.gui.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;
import reactor.core.publisher.Flux;

@Component
public class ReferenceEventLoader implements FileToReferenceEventConverter {

    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.(txt|dat)");

    @Override
    public Flux<Result<ReferenceMwParameters>> convertFile(File file) {
        return Flux.fromIterable(convertFileToReferenceMwParameters(file));
    }

    @Override
    public Flux<Result<ReferenceMwParameters>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    public List<Result<ReferenceMwParameters>> convertFileToReferenceMwParameters(File file) {

        ArrayList<Result<ReferenceMwParameters>> results = new ArrayList<>();

        if (file == null || !file.exists() || file.isDirectory()) {
            return exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable.", "NULL")));
        }
        if (!file.getName().toUpperCase().endsWith(".TXT") && !file.getName().toUpperCase().endsWith(".DAT")) {
            return exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file is not of a recognized format. The accepted file formats are .txt and .dat.",
                                                                                        file.getName())));
        }

        Pattern pattern = Pattern.compile("\\s+");
        try (Stream<String> lines = Files.readAllLines(file.toPath()).stream()) {
            results.addAll(lines.filter(l -> !l.isEmpty()).map(String::trim).map(pattern::split).map(this::extractReferenceEvent).collect(Collectors.toList()));
        } catch (IOException e) {
            return exceptionalResult(e);
        }

        return results;
    }

    private Result<ReferenceMwParameters> extractReferenceEvent(String[] tokens) {
        Result<ReferenceMwParameters> result = new Result<>(false, null);
        ReferenceMwParameters refEvent = new ReferenceMwParameters();
        try {
            if (tokens.length >= 2) {
                refEvent.setEventId(tokens[0]).setRefMw(Double.valueOf(tokens[1]));
                if (tokens.length == 3 && tokens[2].matches("^[-+]?\\d+(\\.\\d+)?$")) {
                    refEvent.setStressDropInMpa(Double.valueOf(tokens[2]));
                }
                result.setSuccess(true).setResultPayload(Optional.of(refEvent));
            } else {
                result.setErrors(Collections.singletonList(new LightweightIllegalStateException(String.format("Unexpected or invalid column count: got (%s) expected [>=2].", tokens.length))));
            }
        } catch (NumberFormatException nfe) {
            result.setErrors(Collections.singletonList(nfe));
        }
        return result;
    }

    private List<Result<ReferenceMwParameters>> exceptionalResult(Exception error) {
        ArrayList<Result<ReferenceMwParameters>> results = new ArrayList<>();
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        results.add(new Result<ReferenceMwParameters>(false, exceptions, null));
        return results;
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return filter;
    }
}
