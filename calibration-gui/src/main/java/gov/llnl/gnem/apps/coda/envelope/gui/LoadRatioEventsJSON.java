/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.controllers.ProgressGui;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressEventProgressListener;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.model.messaging.Progress;
import gov.llnl.gnem.apps.coda.common.model.messaging.ProgressEvent;
import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

@Service
public class LoadRatioEventsJSON {
    private static final String JSON_EXT = ".json";
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    protected AtomicLong idCounter = new AtomicLong();
    private EventBus bus;

    private Runnable completionCallback = null;
    private ObservableList<RatioEventData> loadedData = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    @Autowired
    public LoadRatioEventsJSON(EventBus bus) {
        this.bus = bus;
    }

    public void loadFiles(List<File> inputFiles, Runnable callback) {
        Progress progress = new Progress(-1l, 0l);
        Progress fileFailedProgress = new Progress(-1l, 0l);
        ProgressEvent progressEvent = new ProgressEvent(idCounter.getAndIncrement(), progress);
        ProgressEvent processingFailedProgressEvent = new ProgressEvent(idCounter.getAndIncrement(), fileFailedProgress);
        ProgressMonitor progressMonitor = new ProgressMonitor("Data Processing", new ProgressEventProgressListener(bus, progressEvent));
        ProgressMonitor processingFailedMonitor = new ProgressMonitor("Processing Failures", new ProgressEventProgressListener(bus, processingFailedProgressEvent));
        Platform.runLater(() -> processingFailedMonitor.getProgressBar().getStyleClass().add("red-bar"));

        List<RatioEventData> ratioEventData = new ArrayList<>();

        CompletableFuture.runAsync(() -> {
            ProgressGui progressGui = ProgressGui.getInstance();
            progressGui.show();
            progressGui.addProgressMonitor(progressMonitor);
            progressGui.addProgressMonitor(processingFailedMonitor);

            List<File> jsonFiles = getJsonFiles(inputFiles);

            progress.setTotal(Integer.toUnsignedLong(jsonFiles.size()));
            bus.post(progressEvent);

            fileFailedProgress.setTotal(0l);
            bus.post(processingFailedProgressEvent);

            jsonFiles.forEach(file -> {
                ObjectMapper mapper = new ObjectMapper();
                List<RatioEventData> ratioData = null;
                TypeReference<List<RatioEventData>> mapType = new TypeReference<List<RatioEventData>>() {
                };
                try {
                    ratioData = mapper.readValue(file, mapType);
                    progress.setCurrent(progress.getCurrent() + 1);
                    progressEvent.setProgress(progress);
                    bus.post(progressEvent);
                    ratioEventData.addAll(ratioData);
                } catch (IOException e) {
                    progress.setTotal(progress.getTotal() - 1);
                    progressEvent.setProgress(progress);
                    fileFailedProgress.setTotal(fileFailedProgress.getTotal() + 1);
                    fileFailedProgress.setCurrent(fileFailedProgress.getCurrent() + 1);
                    processingFailedProgressEvent.setProgress(fileFailedProgress);
                    bus.post(progressEvent);
                    bus.post(processingFailedProgressEvent);
                    log.info("Unable to load spectra ratio data");
                }
            });

            if (!ratioEventData.isEmpty()) {
                loadedData.clear();
                loadedData.setAll(ratioEventData.stream().distinct().collect(Collectors.toList()));
                callback.run();
                if (completionCallback != null) {
                    completionCallback.run();
                }
            }
        });
    }

    public Runnable getCompletionCallback() {
        return completionCallback;
    }

    public LoadRatioEventsJSON setCompletionCallback(Runnable completionCallback) {
        this.completionCallback = completionCallback;
        return this;
    }

    public RatioEventData getRatioEventByEventId(String eventId) {
        return loadedData.filtered(ratioData -> ratioData.getEventId().equals(eventId)).get(0);
    }

    public List<RatioEventData> getLoadedRatioEventData() {
        return loadedData;
    }

    public void clearLoadedData() {
        loadedData.clear();
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
        }
    }

}
