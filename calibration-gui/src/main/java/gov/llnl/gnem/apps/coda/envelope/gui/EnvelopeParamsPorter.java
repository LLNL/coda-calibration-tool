/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.envelope.gui;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeParamsClient;
import gov.llnl.gnem.apps.coda.envelope.gui.events.EnvelopeJobConfigLoadCompleteEvent;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;

@Service
public class EnvelopeParamsPorter {
    private static final String JSON_EXT = ".json";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private EnvelopeParamsClient envParamsClient;
    private EventBus bus;

    @Autowired
    public EnvelopeParamsPorter(EnvelopeParamsClient envParamsClient, EventBus bus) {
        super();
        this.envParamsClient = envParamsClient;
        this.bus = bus;
    }

    public void loadParams(List<File> files) {
        List<File> jsonFiles = getJsonFiles(files);
        Optional<EnvelopeJobConfiguration> bandConfigOpt = jsonFiles.stream().map(json -> {
            ObjectMapper mapper = new ObjectMapper();
            EnvelopeJobConfiguration obj = null;
            try {
                obj = mapper.readValue(json, EnvelopeJobConfiguration.class);
            } catch (IOException e) {
                log.info("Unable to load envelope job configuration.", e);
            }
            return obj;
        }).filter(Objects::nonNull).findAny();

        if (bandConfigOpt.isPresent()) {
            if (envParamsClient != null) {
                envParamsClient.postEnvelopeJobConfiguration(bandConfigOpt.get()).doOnSuccess(sig -> bus.post(new EnvelopeJobConfigLoadCompleteEvent())).subscribe();
            }
        }
    }

    private List<File> getJsonFiles(List<File> inputFiles) {
        List<File> files = new ArrayList<>();
        if (files != null) {
            try (Stream<File> fs = inputFiles.stream()) {
                files = fs.filter(f -> f.isFile() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(JSON_EXT)).collect(Collectors.toList());
            }
        }
        return files;
    }

    public void saveParams(File file) {
        if (file != null) {
            if ((file.exists() && !file.isFile()) || (file.exists() && !file.canWrite())) {
                log.info("Unable to save envelope job configuration to {}. Does the file already exist and do you have write permissions?", file);
            }
            envParamsClient.getEnvelopeJobConfiguration().subscribe(jobConf -> {
                ObjectWriter writer = new ObjectMapper().writerWithDefaultPrettyPrinter();
                try {
                    if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(JSON_EXT)) {
                        writer.writeValue(new File(file.getAbsolutePath() + JSON_EXT), jobConf);
                    } else {
                        writer.writeValue(file, jobConf);
                    }
                } catch (IOException e) {
                    log.info("Unable to save envelope job configuration. {}", e.getMessage());
                }
            });
        }
    }

}
