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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.sac;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.PathMatcher;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.calibration.gui.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Stream;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;
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

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    final PathMatcher filter = FileSystems.getDefault().getPathMatcher("regex:(?i).*\\.sac");

    @Override
    public Mono<Result<Waveform>> convertFile(File file) {
        if (file != null && file.exists() && file.isFile() && filter.matches(file.toPath())) {
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

        String fileName = file.getName();
        log.trace("Reading {} ", fileName);
        SACFileReader reader = null;
        try {
            reader = new SACFileReader(file.getAbsolutePath());
            SACHeader header = reader.header;
            Sequence sequence = reader.readSequence(header.npts);

            Result<String> headerResult = validateHeaderDefined(fileName, "KNETWK", header.knetwk);
            String networkName = headerResult.isSuccess() ? headerResult.getResultPayload().get() : UNKNOWN_VAL;

            headerResult = validateHeaderDefined(fileName, "KSTNM", header.kstnm);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            String stationName = headerResult.getResultPayload().get();

            String evid = null;
            headerResult = validateHeaderDefined(fileName, "NEVID", header.nevid);
            if (headerResult.isSuccess() && !headerResult.getResultPayload().get().equals("0")) {
                evid = headerResult.getResultPayload().get();
            }
            else {
                headerResult = validateHeaderDefined(fileName, "KEVNM", header.kevnm);
                if (headerResult.isSuccess() && headerResult.getResultPayload().isPresent() && !headerResult.getResultPayload().get().isEmpty()) {
                    evid = headerResult.getResultPayload().get();
                } 
            }

            headerResult = validateHeaderDefined(fileName, "KCMPNM", header.kcmpnm);
            if (!headerResult.isSuccess()) {
                return exceptionalResult(headerResult.getErrors());
            }
            String chan = headerResult.getResultPayload().get();

            Channel channel = null;
            try {
                channel = new Channel(chan);
            } catch (Exception ex) {
                return exceptionalResult(new LightweightIllegalStateException(String.format("Error parsing (%s): SAC header variable KCMPNM (%s) does not meet FDSN requirements!", fileName, chan),
                                                                              ex));
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

            String dataType = StringUtils.trimToEmpty((String) header.getVariableMap().get("depvariabletype"));
            if (dataType.startsWith("acc")) {
                dataType = ACCELERATION;
            } else if (dataType.startsWith("dis")) {
                dataType = DISPLACEMENT;
            } else {
                dataType = StringUtils.trimToEmpty(header.kuser0);
                if (dataType.isEmpty()) {
                    dataType = UNKNOWN_VAL;
                }
            }

            String dataUnits = "";
            if (dataType.equals(ACCELERATION)) {
                dataUnits = NM_PER_S_S;
            } else if (dataType.equals(VELOCITY)) {
                dataUnits = NM_PER_S;
            } else if (dataType.equals(DISPLACEMENT)) {
                dataUnits = NM;
            } else {
                dataType = StringUtils.trimToEmpty(header.kuser1);
                if (dataType.isEmpty()) {
                    dataType = UNKNOWN_VAL;
                }
            }

            double sampleRate = header.delta > 0 ? 1.0 / header.delta : 1.0;
            float[] rawVals = sequence.getArray();
            Double[] segment = new Double[rawVals.length];
            IntStream.range(0, rawVals.length).forEach(index -> segment[index] = Double.valueOf(rawVals[index]));

            TimeT originTime = header.getOriginTime() != null ? header.getOriginTime() : header.getReferenceTime();
            if (originTime == null) {
                return exceptionalResult(new IllegalArgumentException("Both reference time and origin time may not be null!"));
            }

            return new Result<>(true,
                                new Waveform().setBeginTime(beginTime)
                                              .setEndTime(endTime)
                                              .setStream(new Stream().setChannelName(chan)
                                                                     .setBandName(channel.getBandCode().name())
                                                                     .setOrientation(channel.getOrientationCode().name())
                                                                     .setInstrument(channel.getInstrumentCode().name())
                                                                     .setStation(new Station().setStationName(stationName)
                                                                                              .setLatitude(stationLatitude)
                                                                                              .setLongitude(stationLongitude)
                                                                                              .setNetworkName(networkName)))
                                              .setEvent(new Event().setEventId(evid)
                                                                   .setLatitude(eventLatitude)
                                                                   .setLongitude(eventLongitude)
                                                                   .setOriginTime(Date.from(Instant.ofEpochMilli(originTime.getMilliseconds()))))
                                              .setSegment(segment)
                                              .setSegmentType(dataType)
                                              .setSegmentUnits(dataUnits)
                                              .setSampleRate(sampleRate)
                                              .setAssociatedPicks(getPicksFromHeader(header)));
        } catch (FileNotFoundException e) {
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
        List<Exception> exceptions = new ArrayList<>();

        if (SACHeader.isDefault(value)) {
            exceptions.add(new LightweightIllegalStateException(String.format(SAC_HEADER_NOT_SET, fileName, headerName)));
        }

        if (value < minVal || value > maxVal) {
            exceptions.add(new LightweightIllegalStateException(String.format("Error parsing (%s): SAC header variable %s must be between %f and %f! Actual value is %f",
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
        List<Exception> exceptions = new ArrayList<>();

        if (SACHeader.isDefault(headerValue)) {
            exceptions.add(new LightweightIllegalStateException(String.format(SAC_HEADER_NOT_SET, fileName, headerName)));
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
        List<Exception> exceptions = new ArrayList<>();

        if (SACHeader.isDefault(validHeader)) {
            exceptions.add(new LightweightIllegalStateException(String.format(SAC_HEADER_NOT_SET, fileName, headerName)));
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
        return filter;
    }
}
