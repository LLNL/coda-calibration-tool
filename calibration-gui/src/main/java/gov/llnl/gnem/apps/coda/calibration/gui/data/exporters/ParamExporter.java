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
package gov.llnl.gnem.apps.coda.calibration.gui.data.exporters;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ReferenceEventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.MeasuredMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ParamTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

@Component
public class ParamExporter {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private ParameterClient paramClient;
    private List<ParamTempFileWriter> paramWriters;

    private ReferenceEventClient eventClient;
    private List<MeasuredMwTempFileWriter> mwWriters;

    @Autowired
    public ParamExporter(ParameterClient paramClient, ReferenceEventClient eventClient, List<ParamTempFileWriter> paramWriters, List<MeasuredMwTempFileWriter> mwWriters) {
        this.paramClient = paramClient;
        this.paramWriters = paramWriters;

        this.eventClient = eventClient;
        this.mwWriters = mwWriters;
    }

    public File createExportArchive() throws IOException {
        Path tmpFolder = Files.createTempDirectory(Long.toString(System.currentTimeMillis()));
        tmpFolder.toFile().deleteOnExit();

        if (paramWriters != null) {
            Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand = paramClient.getSharedFrequencyBandParameters()
                                                                                                      .toStream()
                                                                                                      .filter(Objects::nonNull)
                                                                                                      .collect(
                                                                                                              Collectors.toMap(
                                                                                                                      s -> new FrequencyBand(s.getLowFrequency(), s.getHighFrequency()),
                                                                                                                          Function.identity(),
                                                                                                                          (a, b) -> b,
                                                                                                                          TreeMap::new));

            Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters = paramClient.getSiteSpecificFrequencyBandParameters()
                                                                                                      .toStream()
                                                                                                      .filter(Objects::nonNull)
                                                                                                      .filter(s -> s.getStation() != null)
                                                                                                      .collect(
                                                                                                              Collectors.groupingBy(
                                                                                                                      SiteFrequencyBandParameters::getStation,
                                                                                                                          Collectors.toMap(
                                                                                                                                  s -> new FrequencyBand(s.getLowFrequency(), s.getHighFrequency()),
                                                                                                                                      Function.identity(),
                                                                                                                                      (a, b) -> b,
                                                                                                                                      TreeMap::new)));

            for (ParamTempFileWriter writer : paramWriters) {
                writer.writeParams(tmpFolder, sharedParametersByFreqBand, siteParameters);
            }
        }

        if (mwWriters != null) {

            List<MeasuredMwDetails> measuredMwsDetails = new ArrayList<>();
            //Get corresponding Event details
            eventClient.getMeasuredEvents().filter(Objects::nonNull).filter(mw -> mw.getEventId() != null && !mw.getEventId().trim().isEmpty()).subscribe(mw -> {
                Event event = eventClient.getEvent(mw.getEventId()).block();
                MeasuredMwDetails details = new MeasuredMwDetails(mw, event);
                if (details.isValid()) {
                    measuredMwsDetails.add(details);
                }
            });
            for (MeasuredMwTempFileWriter writer : mwWriters) {
                writer.writeParams(tmpFolder, measuredMwsDetails);
            }
        }

        File zipDir = File.createTempFile("zip-dir", "tmp");
        zipDir.deleteOnExit();

        try (Stream<Path> fileStream = Files.walk(tmpFolder, 5)) {
            List<File> files = fileStream.map(p -> p.toFile()).filter(f -> f.isFile()).collect(Collectors.toList());
            try (ArchiveOutputStream os = new ArchiveStreamFactory().createArchiveOutputStream("zip", new FileOutputStream(zipDir))) {
                for (File file : files) {
                    os.putArchiveEntry(new ZipArchiveEntry(file, file.getName()));
                    IOUtils.copy(new FileInputStream(file), os);
                    os.closeArchiveEntry();
                }
                os.flush();
                os.close();
            } catch (ArchiveException e) {
                throw new IOException(e);
            }
            try (Stream<Path> tmpFileStream = Files.walk(tmpFolder)) {
                tmpFileStream.sorted(Comparator.reverseOrder()).forEach(t -> {
                    try {
                        Files.deleteIfExists(t);
                    } catch (IOException e) {
                        log.trace("Unable to delete temporary file {}", e.getMessage(), e);
                    }
                });
            }
        }
        return zipDir;
    }
}
