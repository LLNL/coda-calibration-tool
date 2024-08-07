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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.controllers.AbstractSeismogramSaveLoadController;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.CodaFilenameParser;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.StackInfo;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacExporter;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacLoader;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressEventProgressListener;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Progress;
import gov.llnl.gnem.apps.coda.common.model.messaging.ProgressEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeClient;
import llnl.gnem.core.io.SAC.SACHeader;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class WaveformLoadingController extends AbstractSeismogramSaveLoadController<FileToWaveformConverter, Waveform> {

    private static final Logger log = LoggerFactory.getLogger(WaveformLoadingController.class);

    private static final String SEP = "_";

    private Path exportPath = Paths.get("./envelopes/");

    private SacLoader sacLoader;

    private CodaFilenameParser filenameParser;

    private ProgressMonitor progressMonitor;

    private ProgressEvent progressEvent;

    private Progress progress;

    @Value(value = "${envelope-app.max-batching:50}")
    private int batchSize;

    @Autowired
    public WaveformLoadingController(List<FileToWaveformConverter> fileConverters, EnvelopeClient client, EnvelopeParamsController params, EventBus bus, SacExporter sacExporter, SacLoader sacLoader,
            CodaFilenameParser filenameParser) {
        super(fileConverters, bus, log, sacExporter, () -> client.getAllEnvelopes(), null);
        this.sacLoader = sacLoader;
        this.filenameParser = filenameParser;
        this.loadClient = (id, waveforms) -> client.postEnvelopes(id, waveforms).doOnNext(w -> {
            this.sacExporter.writeWaveformToDirectory(getExportPath(w).toFile(), w);
        });
        this.setCompletionCallback(() -> {
            stackEnvelopes(createEnvelopeMapping(getSacFiles(getExportPath())));
        });
    }

    @PostConstruct
    private void setup() {
        if (batchSize > 0) {
            this.setMaxBatching(batchSize);
        } else {
            log.warn("Invalid batch size {} defined. Defaulting to {} instead.", batchSize, maxBatching);
        }
    }

    private List<File> getSacFiles(Path path) {
        List<File> files = new ArrayList<>();
        if (path != null) {
            try (Stream<Path> fs = Files.walk(path)) {
                files = fs.map(Path::toFile).filter(f -> f.isFile() && f.getName().toLowerCase(Locale.ENGLISH).endsWith(".env")).collect(Collectors.toList());
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        }
        return files;
    }

    @Override
    public void loadFiles(List<File> inputFiles) {
        try {
            progress = new Progress(-1l, 0l);
            progressEvent = new ProgressEvent(idCounter.getAndIncrement(), progress);
            progressMonitor = new ProgressMonitor("Saving Event-Sta-Freq pairs", new ProgressEventProgressListener(bus, progressEvent));
            Files.createDirectories(getExportPath());
            super.loadFiles(inputFiles, this.getCompletionCallback(), progressMonitor);
        } catch (IOException ex) {
            // TODO: bus.post(new DisplayableExceptionEvent("Unable to create directory for envelopes.", ex));
            log.error(ex.getMessage(), ex);
        }
    }

    public SortedMap<String, List<File>> createEnvelopeMapping(List<File> files) {
        TreeMap<String, List<File>> evidStaFreqMap = new TreeMap<>();
        for (int ii = 0; ii < files.size(); ii++) {
            try {
                File file = files.get(ii);
                Result<StackInfo> res = filenameParser.parse(file.getName().toUpperCase(Locale.ENGLISH));
                Optional<StackInfo> payload = res.getResultPayload();
                if (res.isSuccess() && payload.isPresent()) {
                    StackInfo info = payload.get();
                    try (SACHeader header = new SACHeader(file)) {
                        if (isValidStationName(header)) {
                            String evid = null;
                            if (header.kevnm != null && !header.kevnm.trim().isEmpty() && header.kevnm.trim().matches("[0-9]*")) {
                                evid = header.kevnm.trim();
                            } else if (header.nevid != 0) {
                                evid = Integer.toString(header.nevid).trim();
                            } else {
                                evid = sacLoader.getOrCreateEvid(header).trim();
                            }
                            if (evid != null) {
                                String evidStaFreq = evid + SEP + header.kstnm.trim() + SEP + info.getLowFrequency() + SEP + info.getHighFrequency();
                                evidStaFreqMap.putIfAbsent(evidStaFreq, new ArrayList<>());
                                evidStaFreqMap.get(evidStaFreq).add(file);
                            } else {
                                log.warn("No valid evid for {}", file.getName());
                            }
                        } else {
                            log.warn("No valid station for {}", file.getName());
                        }
                    }
                }
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        }
        return evidStaFreqMap;
    }

    private boolean isValidStationName(SACHeader header) {
        return header.kstnm != null && !SACHeader.STRINGDEFAULT.equalsIgnoreCase(header.kstnm) && !"".equalsIgnoreCase(header.kstnm) && !"0".equalsIgnoreCase(header.kstnm);
    }

    public void stackEnvelopes(SortedMap<String, List<File>> evidStaFreqMap) {
        final AtomicLong count = new AtomicLong(0);
        progress.setTotal((long) evidStaFreqMap.size());
        progress.setCurrent(count.get());
        progressEvent.setProgress(progress);
        bus.post(progressEvent);

        evidStaFreqMap.entrySet().parallelStream().forEach(entry -> {
            if (!entry.getValue().isEmpty()) {
                List<File> files = entry.getValue();
                Map<String, List<Waveform>> waveformsByFreqAndSta = new HashMap<>();

                for (int i = 0; i < files.size(); i++) {
                    Result<StackInfo> res = filenameParser.parse(files.get(i).getName().toUpperCase(Locale.ENGLISH));
                    if (res != null && res.isSuccess() && res.getResultPayload().isPresent()) {
                        StackInfo stackInfo = res.getResultPayload().get();
                        Result<Waveform> result = sacLoader.convertSacFileToWaveform(files.get(i));

                        if (result.isSuccess() && result.getResultPayload().isPresent()) {
                            Waveform rawWaveform = result.getResultPayload().get();
                            rawWaveform.setLowFrequency(stackInfo.getLowFrequency());
                            rawWaveform.setHighFrequency(stackInfo.getHighFrequency());
                            if (rawWaveform != null
                                    && rawWaveform.hasData()
                                    && rawWaveform.getSegmentLength() > 0
                                    && rawWaveform.getStream() != null
                                    && rawWaveform.getStream().getStation() != null
                                    && rawWaveform.getStream().getChannelName() != null) {
                                if (!gov.llnl.gnem.apps.coda.common.model.domain.Stream.TYPE_STACK.equalsIgnoreCase(rawWaveform.getStream().getChannelName())) {
                                    waveformsByFreqAndSta.computeIfAbsent(entry.getKey() + " " + rawWaveform.getStream().getStation().hashCode(), k -> new ArrayList<>()).add(rawWaveform);
                                }
                            } else {
                                log.warn("No data or bad station specification for waveform {}.", rawWaveform);
                            }
                        } else {
                            log.warn("Unable to read envelope file {}. {}", files.get(i), result.getErrors());
                        }
                    } else {
                        log.warn("Unable to parse envelope filename for frequency band {}. {}", files.get(i), res.getErrors());
                    }
                }

                List<Waveform> stackedWaveforms = waveformsByFreqAndSta.entrySet().stream().map(e -> stackEnvelopes(e.getValue())).filter(Objects::nonNull).collect(Collectors.toList());

                // TODO: Export envelopes and stacks to separate dirs
                for (Waveform stackedWaveform : stackedWaveforms) {
                    File stackFolder = getExportPath(stackedWaveform).toFile();
                    sacExporter.writeWaveformToDirectory(stackFolder, stackedWaveform);
                }
            }

            long currentCount = count.getAndIncrement();
            if (currentCount % this.getMaxBatching() == 0) {
                progress.setCurrent(currentCount);
                progressEvent.setProgress(progress);
                bus.post(progressEvent);
            }
        });

        progress.setCurrent(progress.getTotal());
        progressEvent.setProgress(progress);
        bus.post(progressEvent);
    }

    private Waveform stackEnvelopes(List<Waveform> waves) {
        // FIXME: Duplicate of the one in service.
        //             Need a common-utils because this pulls in stuff from Externals for TimeSeries etc so I can't cheat and slam it into the common model.
        Waveform base = null;
        if (waves != null && !waves.isEmpty()) {
            try {
                base = waves.get(0);
                TimeSeries seis = convertToTimeSeries(base);

                for (int i = 1; i < waves.size(); i++) {
                    TimeSeries seis2 = convertToTimeSeries(waves.get(i));
                    seis = seis.add(seis2);
                }
                seis.MultiplyScalar(1d / waves.size());

                float[] seisData = seis.getData();
                double[] data = new double[seisData.length];
                for (int j = 0; j < data.length; ++j) {
                    data[j] = seisData[j];
                }
                base.setSegment(data);
                if (!base.hasData() || base.getSegmentLength() == 0) {
                    return null;
                }

                base.setSampleRate(seis.getSamprate());
                base.setBeginTime(seis.getTime().getDate());
                base.setEndTime(seis.getEndtime().getDate());
                if (base.getStream() != null) {
                    base.getStream().setChannelName(gov.llnl.gnem.apps.coda.common.model.domain.Stream.TYPE_STACK);
                }
            } catch (Exception e) {
                log.info(e.getMessage(), e);
            }
        } else {
            log.info("Empty list provided for creating envelopes, skipping");
        }
        return base;
    }

    private TimeSeries convertToTimeSeries(Waveform base) {
        double[] segment = base.getSegment();
        float[] fData = new float[segment.length];
        for (int j = 0; j < fData.length; ++j) {
            fData[j] = (float) segment[j];
        }
        TimeSeries seis = new TimeSeries(fData, base.getSampleRate(), new TimeT(base.getBeginTime()));
        return seis;
    }

    public Path getExportPath() {
        return exportPath;
    }

    public Path getExportPath(Waveform w) {

        Path path = exportPath;
        if (exportPath == null) {
            throw new IllegalStateException("Unable to export waveform, export path was null");
        }

        if (w == null) {
            throw new IllegalStateException("Unable to export waveform, waveform was null");
        }

        String station = Optional.ofNullable(w.getStream()).map(gov.llnl.gnem.apps.coda.common.model.domain.Stream::getStation).map(Station::getStationName).orElse("");
        TimeT time = w.getEvent() != null ? new TimeT(w.getEvent().getOriginTime()) : new TimeT(w.getBeginTime());
        if (time != null) {
            String evid = sacLoader.getOrCreateEvid(w);
            // evid
            int year = time.getYear();
            int month = time.getMonth();

            String newPath = exportPath.toAbsolutePath().toString() + File.separator + year + File.separator + String.format("%02d", month) + File.separator + evid + File.separator + station;

            File result = new File(newPath);
            if (!result.exists()) {
                boolean wasCreated = result.mkdirs();
                if (!wasCreated) {
                    throw new IllegalStateException("Could not create directory: " + newPath);
                }
            }
            path = result.toPath();
        }
        return path;
    }

    public void setExportPath(File exportDirectory) {
        if (exportDirectory != null) {
            Path path;
            if (exportDirectory.isFile() && exportDirectory.getParentFile() != null) {
                path = exportDirectory.getParentFile().toPath();
            } else {
                path = exportDirectory.toPath();
            }
            this.exportPath = path;
        }
    }
}
