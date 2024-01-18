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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.ratios.SpectraRatioLoader;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraRatioClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.SpectraRatioExporter;
import gov.llnl.gnem.apps.coda.common.gui.controllers.ProgressGui;
import gov.llnl.gnem.apps.coda.common.gui.events.EnvelopeLoadStartingEvent;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressEventProgressListener;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.model.messaging.PassFailEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.Progress;
import gov.llnl.gnem.apps.coda.common.model.messaging.ProgressEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;

@Component
public class SpectraRatioLoadingController {

    private static final Logger log = LoggerFactory.getLogger(SpectraRatioLoadingController.class);
    private SpectraRatioClient client;
    private EventBus bus;

    protected static final Long LOCAL_FAIL_EVENT = -1l;
    protected int maxBatching = 20;

    protected AtomicLong idCounter = new AtomicLong();

    private PathMatcher fileMatcher = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.(json)");
    private SpectraRatioLoader ratioLoader;
    private SpectraRatioExporter ratioExporter;

    @Autowired
    public SpectraRatioLoadingController(SpectraRatioLoader ratioLoader, SpectraRatioExporter ratioExporter, SpectraRatioClient client, EventBus bus) {
        this.ratioLoader = ratioLoader;
        this.ratioExporter = ratioExporter;
        this.client = client;
        this.bus = bus;
    }

    public void saveToDirectory(File exportDirectory) {
        CompletableFuture.runAsync(() -> {
            try {
                if (exportDirectory.isDirectory() && exportDirectory.canWrite()) {
                    Progress fileProcessingProgress = new Progress(0l, 0l);
                    ProgressEvent processingProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileProcessingProgress);
                    ProgressMonitor processingMonitor = new ProgressMonitor("Exporting Ratio Project", new ProgressEventProgressListener(bus, processingProgressEvent));

                    try (final BufferedWriter fileWriter = new BufferedWriter(new FileWriter(exportDirectory.toPath().resolve("CERT-Project-" + Instant.now().toEpochMilli() + ".json").toFile()))) {
                        ProgressGui progressGui = ProgressGui.getInstance();
                        progressGui.show();
                        progressGui.addProgressMonitor(processingMonitor);
                        fileProcessingProgress.setTotal(1l);
                        bus.post(processingProgressEvent);
                        client.getRatiosMetadata().doOnComplete(() -> {
                            fileProcessingProgress.setCurrent(1l);
                            bus.post(processingProgressEvent);
                        }).filter(x -> x != null).filter(x -> x.getId() != null).subscribe(x -> {
                            ratioExporter.writeSpectraRatioPairDetails(fileWriter, x);
                        });
                    } catch (RuntimeException | IOException e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } catch (IllegalStateException e) {
                log.error("Unable to instantiate saving progress display {}", e.getMessage(), e);
            }
        });
    }

    public void loadFiles(List<File> inputFiles) {
        Progress progress = new Progress(-1l, 0l);
        ProgressEvent progressEvent = new ProgressEvent(idCounter.getAndIncrement(), progress);
        ProgressMonitor progressMonitor = new ProgressMonitor("Loading Ratio Project", new ProgressEventProgressListener(bus, progressEvent));
        loadFiles(inputFiles, () -> {
            progress.setTotal(1l);
            progress.setCurrent(1l);
            bus.post(progressEvent);
        }, progressMonitor);
    }

    public void loadFiles(List<File> inputFiles, Runnable completionCallback, ProgressMonitor... additionalBars) {
        CompletableFuture.runAsync(() -> {
            try (Stream<File> fileStream = inputFiles.stream()) {
                List<File> files = new ArrayList<>();
                fileStream.forEach(input -> {
                    try (Stream<Path> walkStream = Files.walk(input.toPath(), 10)) {
                        files.addAll(walkStream.filter(p -> p.toFile().isFile() && fileMatcher.matches(p)).map(Path::toFile).collect(Collectors.toList()));
                    } catch (IOException e) {
                        log.trace(e.getMessage(), e);
                    }
                });

                if (!files.isEmpty()) {
                    bus.post(new EnvelopeLoadStartingEvent());
                    Progress fileProcessingProgress = new Progress(-1l, 0l);
                    Progress fileFailedProgress = new Progress(-1l, 0l);
                    ProgressEvent processingProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileProcessingProgress);
                    ProgressEvent processingFailedProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileFailedProgress);
                    ProgressMonitor processingMonitor = new ProgressMonitor("Ratio Project File Loading", new ProgressEventProgressListener(bus, processingProgressEvent));
                    ProgressMonitor processingFailedMonitor = new ProgressMonitor("Ratio Project Processing Failures", new ProgressEventProgressListener(bus, processingFailedProgressEvent));
                    processingFailedMonitor.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> bus.post(new ShowFailureReportEvent()));
                    Platform.runLater(() -> processingFailedMonitor.getProgressBar().getStyleClass().add("red-bar"));

                    try {
                        ProgressGui progressGui = ProgressGui.getInstance();
                        progressGui.show();
                        progressGui.addProgressMonitor(processingMonitor);
                        progressGui.addProgressMonitor(processingFailedMonitor);

                        if (additionalBars != null) {
                            for (ProgressMonitor bar : additionalBars) {
                                progressGui.addProgressMonitor(bar);
                            }
                        }

                        fileProcessingProgress.setTotal(0l);
                        bus.post(processingProgressEvent);

                        fileFailedProgress.setTotal(0l);
                        bus.post(processingFailedProgressEvent);

                        for (File file : files) {
                            List<Result<SpectraRatioPairDetailsMetadata>> results = ratioLoader.convertFile(file);
                            try {
                                List<SpectraRatioPairDetailsMetadata> successfulResults = results.parallelStream()
                                                                                                 .filter(Result::isSuccess)
                                                                                                 .map(result -> result.getResultPayload().get())
                                                                                                 .collect(Collectors.toList());

                                List<String> loadFailures = new ArrayList<>();
                                client.loadRatioMetadata(idCounter.getAndIncrement(), successfulResults).doOnNext(ret -> loadFailures.add(ret)).retry(3).blockLast(Duration.ofHours(1l));

                                if (loadFailures.size() > 0) {
                                    fileFailedProgress.setTotal(fileFailedProgress.getTotal() + loadFailures.size());
                                    fileFailedProgress.setCurrent(fileFailedProgress.getCurrent() + loadFailures.size());
                                    bus.post(processingFailedProgressEvent);
                                    loadFailures.forEach(
                                            r -> bus.post(new PassFailEvent(LOCAL_FAIL_EVENT, "", new Result<>(false, Collections.singletonList(new LightweightIllegalStateException(r)), null))));
                                }

                                fileProcessingProgress.setTotal(fileProcessingProgress.getTotal() + successfulResults.size());
                                fileProcessingProgress.setCurrent(fileProcessingProgress.getCurrent() + successfulResults.size() - loadFailures.size());
                                bus.post(processingProgressEvent);
                            } catch (RuntimeException ex) {
                                log.trace(ex.getMessage(), ex);
                            }
                        }
                    } catch (RuntimeException e) {
                        log.error(e.getMessage(), e);
                    }

                }
            } catch (IllegalStateException e) {
                log.error("Unable to instantiate loading display {}", e.getMessage(), e);
            }
            if (completionCallback != null) {
                completionCallback.run();
            }
        });
    }
}
