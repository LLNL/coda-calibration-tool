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
package gov.llnl.gnem.apps.coda.common.gui.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToSeismogramConverter;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacExporter;
import gov.llnl.gnem.apps.coda.common.gui.events.ShowFailureReportEvent;
import gov.llnl.gnem.apps.coda.common.gui.events.EnvelopeLoadStartingEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressEventProgressListener;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.PassFailEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.Progress;
import gov.llnl.gnem.apps.coda.common.model.messaging.ProgressEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import javafx.application.Platform;
import javafx.scene.input.MouseEvent;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public abstract class AbstractSeismogramSaveLoadController<FC extends FileToSeismogramConverter, R> {

    protected static final Long LOCAL_FAIL_EVENT = -1l;

    protected Supplier<Flux<Waveform>> saveClient;

    protected BiFunction<Long, List<Waveform>, Flux<R>> loadClient;

    protected int maxBatching = 20;

    protected List<FC> fileConverters;

    protected EventBus bus;

    protected AtomicLong idCounter = new AtomicLong();

    protected SacExporter sacExporter;

    private Logger log;

    private Runnable completionCallback = null;

    public AbstractSeismogramSaveLoadController(List<FC> fileConverters, EventBus bus, Logger log, SacExporter sacExporter, Supplier<Flux<Waveform>> saveClient,
            BiFunction<Long, List<Waveform>, Flux<R>> loadClient) {
        super();
        this.fileConverters = fileConverters;
        this.saveClient = saveClient;
        this.loadClient = loadClient;
        this.bus = bus;
        this.log = log;
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

                        saveClient.get().doOnComplete(() -> {
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
                log.error("Unable to instantiate saving progress display {}", e.getMessage(), e);
            }
        });
    }

    public void loadFiles(List<File> inputFiles) {
        loadFiles(inputFiles, this.getCompletionCallback());
    }

    public void loadFiles(List<File> inputFiles, Runnable completionCallback) {
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
                    bus.post(new EnvelopeLoadStartingEvent());
                    // TODO: Condense these bars into a composite pass/fail progress bar
                    Progress fileProcessingProgress = new Progress(-1l, 0l);
                    Progress fileFailedProgress = new Progress(-1l, 0l);
                    ProgressEvent processingProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileProcessingProgress);
                    ProgressEvent processingFailedProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileFailedProgress);
                    ProgressMonitor processingMonitor = new ProgressMonitor("File Processing", new ProgressEventProgressListener(bus, processingProgressEvent));
                    ProgressMonitor processingFailedMonitor = new ProgressMonitor("Processing Failures", new ProgressEventProgressListener(bus, processingFailedProgressEvent));
                    processingFailedMonitor.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> bus.post(new ShowFailureReportEvent()));
                    Platform.runLater(() -> processingFailedMonitor.getProgressBar().getStyleClass().add("red-bar"));

                    try {
                        ProgressGui progressGui = new ProgressGui();
                        progressGui.show();
                        progressGui.addProgressMonitor(processingMonitor);
                        progressGui.addProgressMonitor(processingFailedMonitor);

                        fileProcessingProgress.setTotal(Integer.toUnsignedLong(files.size()));
                        bus.post(processingProgressEvent);

                        fileFailedProgress.setTotal(0l);
                        bus.post(processingFailedProgressEvent);

                        fileConverters.stream().forEach(fileConverter -> fileConverter.convertFiles(files).buffer(maxBatching, ArrayList::new).subscribe(results -> {
                            try {

                                List<Waveform> successfulResults = results.parallelStream().filter(Result::isSuccess).map(result -> result.getResultPayload().get()).collect(Collectors.toList());
                                List<Result<Waveform>> failedResults = results.parallelStream().filter(r -> !r.isSuccess()).collect(Collectors.toList());

                                loadClient.apply(idCounter.getAndIncrement(), successfulResults).retry(3).parallel().runOn(Schedulers.parallel()).subscribe();

                                fileProcessingProgress.setCurrent(fileProcessingProgress.getCurrent() + successfulResults.size());

                                bus.post(processingProgressEvent);
                                if (failedResults.size() > 0) {
                                    fileProcessingProgress.setTotal(fileProcessingProgress.getTotal() - failedResults.size());
                                    fileFailedProgress.setTotal(fileFailedProgress.getTotal() + failedResults.size());
                                    fileFailedProgress.setCurrent(fileFailedProgress.getCurrent() + failedResults.size());
                                    bus.post(processingFailedProgressEvent);
                                    failedResults.forEach(r -> bus.post(new PassFailEvent(LOCAL_FAIL_EVENT, "", r)));
                                }
                            } catch (RuntimeException ex) {
                                log.trace(ex.getMessage(), ex);
                            }
                        }));

                        if (completionCallback != null) {
                            completionCallback.run();
                        }
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
        Optional<FC> match = fileConverters.stream().filter(fc -> fc.getMatchingPattern().matches(p)).findAny();
        return match.isPresent();
    }

    public int getMaxBatching() {
        return maxBatching;
    }

    public void setMaxBatching(int maxBatching) {
        this.maxBatching = maxBatching;
    }

    public Runnable getCompletionCallback() {
        return completionCallback;
    }

    public AbstractSeismogramSaveLoadController<FC, R> setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
        return this;
    }
}
