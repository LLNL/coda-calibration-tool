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
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.controllers.AbstractSeismogramSaveLoadController;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.CodaFilenameParser;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.StackInfo;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacExporter;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacLoader;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
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
        this.setMaxBatching(50);
    }

    private List<File> getSacFiles(Path path) {
        List<File> files = new ArrayList<>();
        if (path != null) {
            try (Stream<Path> fs = Files.walk(path)) {
                files = fs.map(p -> p.toFile()).filter(f -> f.isFile() && f.getName().toLowerCase().endsWith(".env")).collect(Collectors.toList());
            } catch (IOException e) {
                log.warn(e.getMessage(), e);
            }
        }
        return files;
    }

    @Override
    public void loadFiles(List<File> inputFiles) {
        try {
            Files.createDirectories(getExportPath());
            super.loadFiles(inputFiles);
        } catch (IOException ex) {
            // TODO: bus.post(new DisplayableExceptionEvent("Unable to create directory for envelopes.", ex));
            log.error(ex.getMessage(), ex);
        }
    }

    public TreeMap<String, List<File>> createEnvelopeMapping(List<File> files) {
        TreeMap<String, List<File>> evidStaFreqMap = new TreeMap<String, List<File>>();
        for (int ii = 0; ii < files.size(); ii++) {
            try {
                File file = files.get(ii);
                Result<StackInfo> res = filenameParser.parse(file.getName().toUpperCase(Locale.ENGLISH));
                if (res.isSuccess() && res.getResultPayload().isPresent()) {
                    StackInfo info = res.getResultPayload().get();
                    try (SACHeader header = new SACHeader(file)) {
                        if (isValidStationName(header)) {
                            String evid = null;
                            if (header.kevnm != null && !header.kevnm.trim().isEmpty() && header.kevnm.trim().matches("[0-9]*")) {
                                evid = header.kevnm.trim();
                            } else {
                                if (header.nevid != 0) {
                                    evid = Integer.toString(header.nevid).trim();
                                } else {
                                    evid = sacLoader.getOrCreateEvid(header).trim();
                                }
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

    public void stackEnvelopes(TreeMap<String, List<File>> evidStaFreqMap) {
        evidStaFreqMap.entrySet().parallelStream().forEach(entry -> {
            if (entry.getValue().size() > 1) {
                List<File> files = entry.getValue();
                TimeSeries stackedSeries = null;
                Waveform stackedWaveform = null;
                int nseismograms = 0;

                for (int i = 0; i < files.size(); i++) {
                    Result<Waveform> result = sacLoader.convertSacFileToWaveform(files.get(i));
                    if (result.isSuccess() && result.getResultPayload().isPresent()) {
                        Waveform rawWaveform = result.getResultPayload().get();
                        if (rawWaveform != null && rawWaveform.getSegment() != null && rawWaveform.getSegment().length > 0) {
                            float[] fData = new float[rawWaveform.getSegment().length];
                            for (int j = 0; j < fData.length; ++j) {
                                fData[j] = (float) rawWaveform.getSegment()[j];
                            }
                            TimeSeries seis = new TimeSeries(fData, rawWaveform.getSampleRate(), new TimeT(rawWaveform.getBeginTime()));
                            if (stackedSeries == null) {
                                stackedSeries = seis;
                                stackedWaveform = rawWaveform;
                                nseismograms = 1;
                            } else {
                                try {
                                    if (seis.getTime().gt(stackedSeries.getTime())) {
                                        stackedSeries.cutBefore(seis.getTime());
                                    } else {
                                        seis.cutBefore(stackedSeries.getTime());
                                    }

                                    if (seis.getEndtime().lt(stackedSeries.getEndtime())) {
                                        stackedSeries.cutAfter(seis.getEndtime());
                                    } else {
                                        seis.cutAfter(stackedSeries.getEndtime());
                                    }

                                    if (stackedSeries.AddSeismogram(seis)) {
                                        nseismograms++;
                                    } else {
                                        log.warn("{}. Unabled to stack series {} and {}; sample rate or timing mismatch.", entry.getKey(), stackedSeries, seis);
                                    }
                                } catch (IllegalArgumentException e) {
                                    log.warn("{}. Unabled to stack series {} and {}; {}", entry.getKey(), stackedSeries, seis, e.getMessage());
                                }
                            }
                            if (stackedWaveform != null && (stackedWaveform.getLowFrequency() == null || stackedWaveform.getHighFrequency() == null)) {
                                Result<StackInfo> res = filenameParser.parse(files.get(i).getName().toUpperCase(Locale.ENGLISH));
                                if (res.isSuccess() && res.getResultPayload().isPresent()) {
                                    StackInfo info = res.getResultPayload().get();
                                    stackedWaveform.setLowFrequency(info.getLowFrequency());
                                    stackedWaveform.setHighFrequency(info.getHighFrequency());
                                }
                            }
                        }
                    } else {
                        log.warn("Unabled to read envelope file {}. {}", files.get(i), result.getErrors());
                    }

                    // TODO: Implement array processing
                    // TODO: Profile to improve perf
                    // TODO: Progress bars for stacking
                    // TODO: Export envelopes and stacks to separate dirs
                    // TODO: Create folder hierarchy for output (Year->Month->Evid->Sta)

                    // 1. For each element envelope, find record begin and end of each
                    // trace ( b and e may have changed because of time shifting)
                    // 2. Keep the largest begin and smallest end times of all elements
                    // 3. Form an average envelope by summing each trace and then
                    // dividing by the total number of envelopes used
                    // 4. TODO ? Decimate the averaged envelopes using interp d 0.5
                    // where d is the inverse of the number of samples per second TODO
                    // == didn't we already do this?
                    // 5. Change station header location lat and lon to the reference
                    // (array center?) and re-compute hypocentral distance
                    // 6. Store the envelope for coda amplitude measurement
                }

                if (nseismograms > 1) {
                    stackedSeries.MultiplyScalar(1 / ((double) nseismograms));
                    float[] stackedData = stackedSeries.getData();
                    double[] data = new double[stackedData.length];
                    for (int i = 0; i < data.length; ++i) {
                        data[i] = stackedData[i];
                    }
                    stackedWaveform.setSegment(data);
                    stackedWaveform.getStream().setChannelName("STACK");

                    File stackFolder = getExportPath(stackedWaveform).toFile();
                    sacExporter.writeWaveformToDirectory(stackFolder, stackedWaveform);
                } else {
                    log.warn("No valid seismograms available to stack for inputs: {}", files);
                }

            }
        });
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
            throw new IllegalStateException("Unable to export waveform,waveform was null");
        }

        String station = Optional.ofNullable(w.getStream()).map(stream -> stream.getStation()).map(sta -> sta.getStationName()).orElse("");
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
