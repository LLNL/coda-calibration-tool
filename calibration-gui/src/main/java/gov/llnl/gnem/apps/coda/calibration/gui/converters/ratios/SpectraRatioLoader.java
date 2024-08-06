/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.ratios;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformMetadataImpl;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadataImpl;

@Component
public class SpectraRatioLoader {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*json");

    private ObjectMapper streamedMapper = new ObjectMapper();

    public SpectraRatioLoader() {
        SimpleModule module = new SimpleModule("SpectraRatioMetadataMapper", Version.unknownVersion());
        SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
        resolver.addMapping(SpectraRatioPairDetailsMetadata.class, SpectraRatioPairDetailsMetadataImpl.class);
        resolver.addMapping(WaveformMetadata.class, WaveformMetadataImpl.class);
        module.setAbstractTypes(resolver);
        streamedMapper.registerModule(module);
        //Support for Optional
        streamedMapper.registerModule(new Jdk8Module());
    }

    public List<Result<SpectraRatioPairDetailsMetadata>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
            List<Result<SpectraRatioPairDetailsMetadata>> results = new ArrayList<>();
            try (MappingIterator<SpectraRatioPairDetailsMetadata> it = streamedMapper.readerFor(SpectraRatioPairDetailsMetadata.class).readValues(file)) {
                while (it.hasNextValue()) {
                    SpectraRatioPairDetailsMetadata ratio = it.nextValue();
                    results.add(new Result<>(true, ratio));
                }
            } catch (IOException e) {
                Result<SpectraRatioPairDetailsMetadata> result = new Result<>(false, null);
                result.getErrors().add(e);
                results.add(result);
            }
            return results;
        }
        return Collections.singletonList(new Result<>(false, Collections.singletonList(new LightweightIllegalStateException("File " + file + " does not exist or is not a file.")), null));
    }
}
