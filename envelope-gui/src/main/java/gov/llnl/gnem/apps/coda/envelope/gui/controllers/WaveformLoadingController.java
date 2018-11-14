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
package gov.llnl.gnem.apps.coda.envelope.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.controllers.AbstractSeismogramSaveLoadController;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacExporter;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeClient;

@Component
@ConfigurationProperties("envelope-app.waveform.client")
public class WaveformLoadingController extends AbstractSeismogramSaveLoadController<FileToWaveformConverter, Waveform> {

    private static Path exportPath = Paths.get("./envelopes/");

    private static final Logger log = LoggerFactory.getLogger(WaveformLoadingController.class);

    @Autowired
    public WaveformLoadingController(List<FileToWaveformConverter> fileConverters, EnvelopeClient client, EnvelopeParamsController params, EventBus bus, SacExporter sacExporter) {
        super(fileConverters, bus, log, sacExporter, () -> client.getAllEnvelopes(), (id, waveforms) -> client.postEnvelopes(id, waveforms).doOnNext(w -> {
            sacExporter.writeWaveformToDirectory(exportPath.toFile(), w);
        }));
        this.setMaxBatching(1);
    }

    @Override
    public void loadFiles(List<File> inputFiles) {
        try {
            Files.createDirectories(exportPath);
            super.loadFiles(inputFiles);
        } catch (IOException ex) {
            // TODO: bus.post(new DisplayableExceptionEvent("Unable to create directory for envelopes.", ex));
            log.error(ex.getMessage(), ex);
        }
    }
}
