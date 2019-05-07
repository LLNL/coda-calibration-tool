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
package gov.llnl.gnem.apps.coda.common.gui.converters.sac;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import llnl.gnem.core.io.SAC.SACFileReader;
import llnl.gnem.core.io.SAC.SACHeader;
import llnl.gnem.core.metadata.Channel;
import llnl.gnem.core.signalprocessing.Sequence;
import llnl.gnem.core.util.TimeT;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class SacLoader implements FileToWaveformConverter {
    private static final String SAC_HEADER_NOT_SET = "Error parsing (%s): SAC header variable (%s) is not set";
    private static final String UNKNOWN_VAL = "UNK";
    private static final String NM_PER_S_S = "nm/s/s";
    private static final String NM_PER_S = "nm/s";
    private static final String NM = "nm";
    private static final String ACCELERATION = "acceleration";
    private static final String DISPLACEMENT = "displacement";
    private static final String VELOCITY = "velocity";

    private static final Logger log = LoggerFactory.getLogger(SacLoader.class);
    //FIXME: This is just silly, I wish we could reliably get SAC files with .sac extensions but even still we need a better way than this...
    private final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*(\\.env|\\.synenv|\\.out|\\.param[s]?|\\.zip\\.tar|\\.gz|\\.tgz|\\.7z|\\.dat|\\.txt|\\.[a-z]*sh)$");
    private final PathMatcher acceptFilter = new PathMatcher() {
        @Override
        public boolean matches(Path path) {
            if (path != null && path.toFile().exists() && path.toFile().isFile() && !path.toFile().isHidden() && !filter.matches(path)) {
                return true;
            }
            return false;
        }
    };

    @Override
    public Mono<Result<Waveform>> convertFile(File file) {
        if (file != null && acceptFilter.matches(file.toPath())) {
            return Mono.fromSupplier(() -> convertSacFileToWaveform(file));
        }

        return Mono.empty();
    }

    @Override
    public Flux<Result<Waveform>> convertFiles(List<File> files) {
        return Flux.fromIterable(files).flatMap(this::convertFile);
    }

    public Result<Waveform> convertSacFileToWaveform(File file) {
        if (file == null) {
            return exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable. %s", "NULL", "File reference is null")));
        }

        String fileName = file.getPath().toString();
        log.trace("Reading {} ", fileName);
        SACFileReader reader = null;
        try {
            reader = new SACFileReader(file.getAbsolutePath());
            SACHeader header = reader.header;

            Result<String> headerResult = validateHeaderDefined(fileName, "KNETWK", header.knetwk);
            String networkName = headerResult.isSuccess() ? headerResult.getResultPayload().orElse(UNKNOWN_VAL) : UNKNOWN_VAL;

            headerResult = validateHeaderDefined(fileName, "KSTNM", header.kstnm);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            String stationName = headerResult.getResultPayload().orElse(UNKNOWN_VAL);

            TimeT originTime = header.getOriginTime();
            if (originTime == null) {
                originTime = header.getReferenceTime();
            }
            if (originTime == null) {
                return exceptionalResult(new LightweightIllegalStateException("Both reference time and origin time may not be null!"));
            }

            String evid = null;
            headerResult = validateHeaderDefined(fileName, "KEVNM", header.kevnm);
            if (headerResult.isSuccess() && !headerResult.getResultPayload().orElse("").trim().isEmpty() && headerResult.getResultPayload().orElse("").trim().matches("[0-9]*")) {
                evid = headerResult.getResultPayload().orElse("");
            } else {
                headerResult = validateHeaderDefined(fileName, "NEVID", header.nevid);
                if (headerResult.isSuccess() && !headerResult.getResultPayload().orElse("0").equals("0")) {
                    evid = headerResult.getResultPayload().get();
                } else {
                    evid = getOrCreateEvid(header);
                }
            }

            headerResult = validateHeaderDefined(fileName, "KCMPNM", header.kcmpnm);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            String chan = headerResult.getResultPayload().orElse(UNKNOWN_VAL);

            Channel channel = null;
            try {
                channel = new Channel(chan);
            } catch (Exception ex) {
                return exceptionalResult(
                        new LightweightIllegalStateException(String.format("Error parsing (%s): SAC header variable KCMPNM (%s) does not meet FDSN requirements!", fileName, chan), ex));
            }

            Date beginTime = Optional.ofNullable(header.getBeginTime().getMilliseconds()).map(time -> Date.from(Instant.ofEpochMilli(time))).orElse(null);
            Date endTime = Optional.ofNullable(header.getEndTime().getMilliseconds()).map(time -> Date.from(Instant.ofEpochMilli(time))).orElse(null);

            double minVal = -90;
            double maxVal = 90;

            headerResult = validateDoubleDefinedAndInRange(fileName, "EVLA", header.evla, minVal, maxVal);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            double eventLatitude = header.evla;

            headerResult = validateDoubleDefinedAndInRange(fileName, "STLA", header.stla, minVal, maxVal);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            double stationLatitude = header.stla;

            minVal = -180;
            maxVal = 180;

            headerResult = validateDoubleDefinedAndInRange(fileName, "EVLO", header.evlo, minVal, maxVal);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            double eventLongitude = header.evlo;

            headerResult = validateDoubleDefinedAndInRange(fileName, "STLO", header.stlo, minVal, maxVal);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            double stationLongitude = header.stlo;

            String dataType = StringUtils.trimToEmpty((String) header.getVariableMap().get("depvariabletype")).toLowerCase(Locale.ENGLISH);
            if (dataType.startsWith("acc")) {
                dataType = ACCELERATION;
            } else if (dataType.startsWith("vel")) {
                dataType = VELOCITY;
            } else if (dataType.startsWith("dis")) {
                dataType = DISPLACEMENT;
            } else {
                dataType = StringUtils.trimToEmpty(header.kuser0);
                if (dataType.isEmpty()) {
                    dataType = UNKNOWN_VAL;
                }
            }

            String dataUnits = "";
            if (dataType.equalsIgnoreCase(ACCELERATION)) {
                dataUnits = NM_PER_S_S;
            } else if (dataType.equalsIgnoreCase(VELOCITY)) {
                dataUnits = NM_PER_S;
            } else if (dataType.equalsIgnoreCase(DISPLACEMENT)) {
                dataUnits = NM;
            } else {
                dataUnits = StringUtils.trimToEmpty(header.kuser1);
                if (dataUnits.isEmpty() || SACHeader.isDefault(dataUnits)) {
                    dataUnits = UNKNOWN_VAL;
                }
            }

            double sampleRate = header.delta > 0 ? 1.0 / header.delta : 1.0;
            Sequence sequence = reader.readSequence(header.npts);
            float[] rawVals = sequence.getArray();
            double[] segment = new double[rawVals.length];
            try {
                IntStream.range(0, rawVals.length).forEach(index -> {
                    segment[index] = Double.valueOf(rawVals[index]);
                    if (!Double.isFinite(segment[index])) {
                        throw new LightweightIllegalStateException("Invalid data in segment for file: " + fileName);
                    }
                });
            } catch (LightweightIllegalStateException e) {
                return exceptionalResult(e);
            }

            return new Result<>(true,
                                new Waveform().setBeginTime(beginTime)
                                              .setEndTime(endTime)
                                              .setStream(
                                                      new Stream().setChannelName(chan)
                                                                  .setBandName(channel.getBandCode().name())
                                                                  .setOrientation(channel.getOrientationCode().name())
                                                                  .setInstrument(channel.getInstrumentCode().name())
                                                                  .setStation(
                                                                          new Station().setStationName(stationName)
                                                                                       .setLatitude(stationLatitude)
                                                                                       .setLongitude(stationLongitude)
                                                                                       .setNetworkName(networkName)))
                                              .setEvent(
                                                      new Event().setEventId(evid)
                                                                 .setLatitude(eventLatitude)
                                                                 .setLongitude(eventLongitude)
                                                                 .setOriginTime(Date.from(Instant.ofEpochMilli(originTime.getMilliseconds()))))
                                              .setSegment(segment)
                                              .setSegmentType(dataType)
                                              .setSegmentUnits(dataUnits)
                                              .setSampleRate(sampleRate)
                                              .setAssociatedPicks(getPicksFromHeader(header)));
        } catch (NegativeArraySizeException | IllegalStateException | IOException e) {
            return exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): file does not exist or is unreadable. %s", fileName, e.getMessage()), e));
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    private List<WaveformPick> getPicksFromHeader(SACHeader header) {
        List<WaveformPick> associatedPicks = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            if (!StringUtils.isBlank(header.kt[i]) && !SACHeader.isDefault(header.kt[i]) && header.t[i] != SACHeader.FLOATDEFAULT) {
                associatedPicks.add(new WaveformPick().setPickName(header.kt[i]).setPickType(header.kt[i]).setPickTimeSecFromOrigin(header.t[i]));
            }
        }

        if (header.a != SACHeader.FLOATDEFAULT) {
            associatedPicks.add(new WaveformPick().setPickName("a").setPickType("a").setPickTimeSecFromOrigin(header.a));
        }
        if (header.f != SACHeader.FLOATDEFAULT) {
            associatedPicks.add(new WaveformPick().setPickName("f").setPickType("f").setPickTimeSecFromOrigin(header.f));
        }

        return associatedPicks;
    }

    private Result<String> validateDoubleDefinedAndInRange(String fileName, String headerName, float value, double minVal, double maxVal) {

        Result<String> validation = new Result<>(false, "");
        if (SACHeader.isDefault(value)) {
            validation.getErrors().add(new LightweightIllegalStateException(String.format(SAC_HEADER_NOT_SET, fileName, headerName)));
        }

        if (value < minVal || value > maxVal) {
            validation.getErrors()
                      .add(
                              new LightweightIllegalStateException(String.format(
                                      "Error parsing (%s): SAC header variable %s must be between %f and %f! Actual value is %f",
                                          fileName,
                                          headerName,
                                          minVal,
                                          maxVal,
                                          value)));
        }

        if (validation.getErrors().isEmpty()) {
            validation.setSuccess(true);
        }

        return validation;
    }

    private Result<String> validateHeaderDefined(String fileName, String headerName, int headerValue) {
        String validHeader = null;
        Result<String> validation = new Result<>(false, validHeader);

        if (SACHeader.isDefault(headerValue)) {
            validation.getErrors().add(new LightweightIllegalStateException(String.format(SAC_HEADER_NOT_SET, fileName, headerName)));
        } else {
            validHeader = Integer.toString(headerValue);
            validation.setResultPayload(Optional.of(validHeader));
        }

        if (validation.getErrors().isEmpty()) {
            validation.setSuccess(true);
        }

        return validation;
    }

    private Result<String> validateHeaderDefined(String fileName, String headerName, String headerValue) {

        String validHeader = StringUtils.trimToEmpty(headerValue);
        Result<String> validation = new Result<>(false, validHeader);

        if (SACHeader.isDefault(validHeader)) {
            validation.getErrors().add(new LightweightIllegalStateException(String.format(SAC_HEADER_NOT_SET, fileName, headerName)));
        }

        if (validation.getErrors().isEmpty()) {
            validation.setSuccess(true);
        }

        return validation;
    }

    private Result<Waveform> exceptionalResult(Exception error) {
        List<Exception> exceptions = new ArrayList<>();
        exceptions.add(error);
        return exceptionalResult(exceptions);
    }

    private Result<Waveform> exceptionalResult(List<Exception> errors) {
        return new Result<>(false, errors, null);
    }

    @Override
    public PathMatcher getMatchingPattern() {
        return acceptFilter;
    }

    public String getOrCreateEvid(Waveform waveform) {
        int evid = 0;
        Double time = 0d;
        if (waveform.getEvent() != null && waveform.getEvent().getOriginTime() != null) {
            time = new TimeT(waveform.getEvent().getOriginTime()).getEpochTime();
        }
        evid = createJDateMinuteResolutionFromEpoch(time);
        return Integer.toString(evid);
    }

    public String getOrCreateEvid(SACHeader header) {
        int evid = 0;
        Double time = 0d;
        TimeT relTime = header.getOriginTime();
        if (relTime != null) {
            time = relTime.getEpochTime();
        } else {
            relTime = header.getReferenceTime();
            if (relTime != null) {
                time = relTime.getEpochTime();
            }
        }
        evid = createJDateMinuteResolutionFromEpoch(time);
        return Integer.toString(evid);
    }

    private int createJDateMinuteResolutionFromEpoch(Double instant) {
        TimeT time = new TimeT(instant);
        int jDate = time.getYear() % 100;
        jDate = jDate * 1000 + time.getJDay();
        jDate = jDate * 100 + time.getHour();
        jDate = jDate * 100 + time.getMinute();
        return jDate;
    }
}
