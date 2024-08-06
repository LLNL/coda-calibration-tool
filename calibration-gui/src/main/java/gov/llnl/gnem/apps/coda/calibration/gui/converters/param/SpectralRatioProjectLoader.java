/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
* All rights reserved.
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
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;

public class SpectralRatioProjectLoader {

    private final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.json");

    private final ObjectMapper mapper;

    public SpectralRatioProjectLoader() {
        mapper = new ObjectMapper();
        //Support for Optional
        mapper.registerModule(new Jdk8Module());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    public Result<List<SpectraRatioPairDetailsMetadata>> convertFile(File file) {
        return convertFileToSpectraRatioPairDetailsMetadata(file);
    }

    private Result<List<SpectraRatioPairDetailsMetadata>> convertFileToSpectraRatioPairDetailsMetadata(File file) {
        Result<List<SpectraRatioPairDetailsMetadata>> res = new Result<>(false, null);
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            List<SpectraRatioPairDetailsMetadata> data = new ArrayList<>();

            for (String line : lines) {
                try {
                    data.add(mapper.readValue(line, SpectraRatioPairDetailsMetadata.class));
                } catch (JsonProcessingException e) {
                    return exceptionalResult(e);
                }
            }
            res.setResultPayload(Optional.of(data));
        } catch (IOException e) {
            return exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): %s", file.getName(), e.getMessage()), e));
        }
        return res;
    }

    private Result<List<SpectraRatioPairDetailsMetadata>> exceptionalResult(Exception error) {
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        return new Result<List<SpectraRatioPairDetailsMetadata>>(false, exceptions, null);
    }
}
