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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToReferenceEventConverter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ReferenceEventClient;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

//TODO: This class needs a GUI to display a list of files it's attempting to load and process + pass/fail indicators
@Component
public class ReferenceEventLoadingController {

    private static final Logger log = LoggerFactory.getLogger(ReferenceEventLoadingController.class);

    private ReferenceEventClient client;

    private int maxBatching = 1000;

    private List<FileToReferenceEventConverter> fileConverters;

    @Autowired
    public ReferenceEventLoadingController(List<FileToReferenceEventConverter> fileConverters, ReferenceEventClient client) throws IOException {
        super();
        this.fileConverters = fileConverters;
        this.client = client;
    }

    public void loadFiles(List<File> files) {
        List<File> validFiles = files.stream().filter(f -> validPath(f.toPath())).collect(Collectors.toList());
        fileConverters.stream().forEach(fileConverter -> fileConverter.convertFiles(validFiles).buffer(maxBatching, ArrayList::new).subscribe(results -> {
            try {
                //TODO: Feedback to the user about pass/fail!
                client.postReferenceEvents(results.stream().filter(Result::isSuccess).map(result -> result.getResultPayload().get()).collect(Collectors.toList())).retry(3).subscribe();
            } catch (JsonProcessingException ex) {
                log.trace(ex.getMessage(), ex);
            }
            results.stream().filter(result -> !result.isSuccess()).flatMap(result -> result.getErrors().stream()).forEach(ex -> log.warn(ex.getMessage(), ex));
        }));
    }

    private boolean validPath(Path p) {
        Optional<FileToReferenceEventConverter> match = fileConverters.stream().filter(fc -> fc.getMatchingPattern().matches(p)).findAny();
        return match.isPresent();
    }

    public int getMaxBatching() {
        return maxBatching;
    }

    public void setMaxBatching(int maxBatching) {
        this.maxBatching = maxBatching;
    }
}
