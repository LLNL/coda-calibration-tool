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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.calibration.gui.converters.sac.SacExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.calibration.gui.util.PassFailEventProgressListener;
import gov.llnl.gnem.apps.coda.calibration.gui.util.ProgressEventProgressListener;
import gov.llnl.gnem.apps.coda.calibration.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Progress;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.ProgressEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;

//TODO: This class needs a GUI to display a list of files it's attempting to load and process + pass/fail indicators
@Component
@ConfigurationProperties("waveform.client")
public class WaveformLoadingController {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WaveformClient client;

    private int maxBatching = 100;

    private List<FileToWaveformConverter> fileConverters;

    private EventBus bus;

    private AtomicLong idCounter = new AtomicLong();

    private SacExporter sacExporter;

    @Autowired
    public WaveformLoadingController(List<FileToWaveformConverter> fileConverters, WaveformClient client, EventBus bus, SacExporter sacExporter) {
        super();
        this.fileConverters = fileConverters;
        this.client = client;
        this.bus = bus;
        this.sacExporter = sacExporter;
    }

    public void saveToDirectory(File exportDirectory) {
        CompletableFuture.runAsync(() -> {
            try {
                if (exportDirectory.isDirectory() && exportDirectory.canWrite()) {
                    Progress fileProcessingProgress = new Progress(0l, 0l);
                    ProgressEvent processingProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileProcessingProgress);
                    ProgressMonitor processingMonitor = new ProgressMonitor("Exporting", new ProgressEventProgressListener(bus, processingProgressEvent));

                    try {
                        ProgressGui progressGui = new ProgressGui();
                        progressGui.show();
                        progressGui.addProgressMonitor(processingMonitor);
                        fileProcessingProgress.setTotal(1l);
                        bus.post(processingProgressEvent);

                        client.getAllStacks().doOnComplete(() -> {
                            fileProcessingProgress.setCurrent(1l);
                            bus.post(processingProgressEvent);
                        }).filter(w -> w != null).filter(w -> w.getId() != null).subscribe(w -> {
                            sacExporter.writeWaveformToDirectory(exportDirectory, w);
                        });
                    } catch (RuntimeException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (IllegalStateException e) {
                log.error("Unable to instantiate loading display {}", e.getMessage(), e);
            }
        });
    }

    public void loadFiles(List<File> inputFiles) {

        CompletableFuture.runAsync(() -> {

            try (Stream<File> fileStream = inputFiles.stream()) {
                List<File> files = new ArrayList<>();
                fileStream.forEach(input -> {
                    try (Stream<Path> walkStream = Files.walk(input.toPath(), 10)) {
                        files.addAll(walkStream.filter(p -> p.toFile().isFile() && validPath(p)).map(Path::toFile).collect(Collectors.toList()));
                    } catch (IOException e) {
                        log.trace(e.getMessage(), e);
                    }
                });

                if (!files.isEmpty()) {
                    Progress fileProcessingProgress = new Progress(0l, 0l);
                    ProgressEvent processingProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileProcessingProgress);
                    ProgressMonitor processingMonitor = new ProgressMonitor("File Processing", new ProgressEventProgressListener(bus, processingProgressEvent));

                    Progress fileUploadProgress = new Progress(0l, 0l);
                    ProgressEvent fileUploadProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileUploadProgress);
                    ProgressMonitor uploadMonitor = new ProgressMonitor("Saving Data", new PassFailEventProgressListener(bus, fileUploadProgressEvent));

                    try {
                        ProgressGui progressGui = new ProgressGui();
                        progressGui.show();
                        progressGui.addProgressMonitor(processingMonitor);
                        progressGui.addProgressMonitor(uploadMonitor);

                        fileUploadProgress.setTotal((long) (files.size() / maxBatching) + 1);
                        bus.post(fileUploadProgressEvent);

                        fileProcessingProgress.setTotal(Integer.toUnsignedLong(files.size()));
                        bus.post(processingProgressEvent);

                        fileConverters.stream().forEach(fileConverter -> fileConverter.convertFiles(files).buffer(maxBatching, ArrayList::new).subscribe(results -> {
                            // TODO: Feedback to the user about failure causes!
                            try {
                                List<Result<Waveform>> successfulResults = results.stream().filter(Result::isSuccess).collect(Collectors.toList());
                                client.postWaveforms(fileUploadProgressEvent.getId(), successfulResults.stream().map(result -> result.getResultPayload().get()).collect(Collectors.toList()))
                                      .retry(3)
                                      .subscribe();
                                fileProcessingProgress.setCurrent(fileProcessingProgress.getCurrent() + successfulResults.size());
                                bus.post(processingProgressEvent);
                            } catch (JsonProcessingException ex) {
                                log.trace(ex.getMessage(), ex);
                            }
                        }));
                    } catch (RuntimeException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (IllegalStateException e) {
                log.error("Unable to instantiate loading display {}", e.getMessage(), e);
            }

        });

    }

    private boolean validPath(Path p) {
        Optional<FileToWaveformConverter> match = fileConverters.stream().filter(fc -> fc.getMatchingPattern().matches(p)).findAny();
        return match.isPresent();
    }

    public int getMaxBatching() {
        return maxBatching;
    }

    public void setMaxBatching(int maxBatching) {
        this.maxBatching = maxBatching;
    }
}
