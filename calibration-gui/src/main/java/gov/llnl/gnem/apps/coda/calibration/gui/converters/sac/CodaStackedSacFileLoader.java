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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.sac;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.StackInfo;
import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.CodaFilenameParser;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToEnvelopeConverter;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacLoader;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class CodaStackedSacFileLoader implements FileToEnvelopeConverter {

    private static final String DEFAULT_VEL_UNITS = "nm/s";
    private SacLoader sacLoader;
    private CodaFilenameParser filenameParser;
    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*stack.*\\.env");

    @Autowired
    public CodaStackedSacFileLoader(SacLoader sacLoader, CodaFilenameParser filenameParser) {
        super();
        this.sacLoader = sacLoader;
        this.filenameParser = filenameParser;
    }

    @Override
    public Mono<Result<Waveform>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
            return Mono.fromSupplier(() -> sacLoader.convertSacFileToWaveform(file)).map(result -> {
                if (result.getResultPayload().isPresent()) {
                    Waveform waveform = result.getResultPayload().get();
                    Result<StackInfo> res = filenameParser.parse(file.getName().toUpperCase(Locale.ENGLISH));
                    if (res.isSuccess()) {
                        waveform.getStream().setChannelName("STACK");
                        waveform.setSegmentType(res.getResultPayload().get().getDataType());
                        waveform.setSegmentUnits(DEFAULT_VEL_UNITS);
                        waveform.setLowFrequency(res.getResultPayload().get().getLowFrequency());
                        waveform.setHighFrequency(res.getResultPayload().get().getHighFrequency());
                    } else {
                        result.getErrors().addAll(res.getErrors());
                        result.setSuccess(false);
                    }
                }
                return result;
            });
        }
        return Mono.empty();
    }

    @Override
    public Flux<Result<Waveform>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return filter;
    }
}
