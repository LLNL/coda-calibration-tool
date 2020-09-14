/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.sac.CodaStackedSacFileLoader;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.CodaSNREndTimePicker;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.CodaFilenameParserImpl;
import gov.llnl.gnem.apps.coda.common.gui.converters.sac.SacLoader;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

public class AssessAutopicker {
    private final static Logger log = LoggerFactory.getLogger(AssessAutopicker.class);
    private static CodaSNREndTimePicker picker = new CodaSNREndTimePicker();
    private static CodaStackedSacFileLoader loader = new CodaStackedSacFileLoader(new SacLoader(), new CodaFilenameParserImpl());
    private static WaveformToTimeSeriesConverter converter = new WaveformToTimeSeriesConverter();

    public static void main(String[] args) {
        List<File> files = null;
        try (Stream<Path> stream = Files.walk(Paths.get(args[0]))) {
            files = stream.filter(path -> path.getFileName().toString().toLowerCase(Locale.ENGLISH).endsWith(".env")).map(path -> path.toFile()).filter(o -> o != null).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (files == null) {
            return;
        }

        List<Waveform> waveforms = loader.convertFiles(files)
                                         .doOnError(error -> Assertions.fail(error.getMessage()))
                                         .filter(Objects::nonNull)
                                         .filter(r -> r.getResultPayload() != null && r.getResultPayload().isPresent())
                                         .map(r -> r.getResultPayload().get())
                                         .filter(w -> w.getAssociatedPicks() != null
                                                 && w.getAssociatedPicks().stream().filter(pick -> PICK_TYPES.F.getPhase().equalsIgnoreCase(pick.getPickType())).findAny().isPresent())
                                         .collectList()
                                         .block(Duration.ofSeconds(10l));

        DescriptiveStatistics stats = new DescriptiveStatistics();
        waveforms.forEach(w -> {
            TimeSeries series = converter.convert(w);
            TimeSeries halfSeries = converter.convert(w);
            halfSeries.cut(0, (halfSeries.getNsamp() - 1) / 2);
            Double maxTime = halfSeries.getMaxTime()[0];

            double startTime = series.getTime().getEpochTime() + maxTime;
            double stopTime = picker.getEndTime(series.getData(),
                                                series.getSamprate(),
                                                startTime,
                                                series.getIndexForTime(startTime),
                                                0,
                                                1200,
                                                0.0,
                                                WaveformUtils.getNoiseFloor(WaveformUtils.floatsToDoubles(series.getData())));
            if (new TimeT(stopTime).gt(new TimeT(startTime))) {
                stopTime = stopTime + series.getTime().subtractD(new TimeT(w.getEvent().getOriginTime()));
            }
            double autoPickedTime = new TimeT(stopTime).subtractD(new TimeT(startTime));

            double humanPickedTime = w.getAssociatedPicks()
                                      .stream()
                                      .filter(pick -> PICK_TYPES.F.getPhase().equalsIgnoreCase(pick.getPickType()))
                                      .findAny()
                                      .map(pick -> (double) pick.getPickTimeSecFromOrigin())
                                      .orElseGet(() -> Double.valueOf(-100));

            if (autoPickedTime > 0) {
                stats.addValue(humanPickedTime - autoPickedTime);
            }
        });
        log.info(stats.toString());
    }
}
