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
package gov.llnl.gnem.apps.coda.calibration.gui.data.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;

@Component
public class SwftStyleParamExporter {

    private NumberFormat dfmt6 = NumberFormatFactory.sixDecimalOneLeadingZero();

    private static final double DEFAULT_SITE_TERM = 0.0;

    private ParameterClient paramClient;

    private static final char SEP = ' ';

    @Autowired
    public SwftStyleParamExporter(ParameterClient paramClient) {
        this.paramClient = paramClient;
    }

    public File createExportArchive() throws IOException {
        Path tmpFolder = Files.createTempDirectory(Long.toString(System.currentTimeMillis()));
        tmpFolder.toFile().deleteOnExit();

        List<File> files = new ArrayList<>();

        Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand = paramClient.getSharedFrequencyBandParameters()
                                                                                                  .toStream()
                                                                                                  .filter(Objects::nonNull)
                                                                                                  .collect(Collectors.toMap(s -> new FrequencyBand(s.getLowFrequency(), s.getHighFrequency()),
                                                                                                                            Function.identity(),
                                                                                                                            (a, b) -> b,
                                                                                                                            TreeMap::new));

        Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters = paramClient.getSiteSpecificFrequencyBandParameters()
                                                                                                  .toStream()
                                                                                                  .filter(Objects::nonNull)
                                                                                                  .filter(s -> s.getStation() != null)
                                                                                                  .collect(Collectors.groupingBy(SiteFrequencyBandParameters::getStation,
                                                                                                                                 Collectors.toMap(s -> new FrequencyBand(s.getLowFrequency(),
                                                                                                                                                                         s.getHighFrequency()),
                                                                                                                                                  Function.identity(),
                                                                                                                                                  (a, b) -> b,
                                                                                                                                                  TreeMap::new)));

        File sharedParamFile = new File(tmpFolder.toFile(), "Shared.param");
        sharedParamFile.deleteOnExit();

        writeParams(sharedParamFile, sharedParametersByFreqBand);
        files.add(sharedParamFile);

        for (Entry<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> stationEntry : siteParameters.entrySet()) {
            File stationParamFile = new File(tmpFolder.toFile(), stationEntry.getKey().getStationName() + ".param");
            writeParams(stationParamFile, sharedParametersByFreqBand, stationEntry.getValue());
            stationParamFile.deleteOnExit();
            files.add(stationParamFile);
        }

        File zipDir = File.createTempFile("zip-dir", "tmp");
        zipDir.deleteOnExit();

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

        return zipDir;
    }

    private void writeParams(File paramFile, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand, Map<FrequencyBand, SiteFrequencyBandParameters> stationParametersByFreqBand)
            throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(paramFile.toPath(), Charset.defaultCharset())) {
            StringBuilder sb = new StringBuilder();
            String header = "#fl fh vel0 vel1 vel2 min max time b0 b1 b2 g0 g1 g2 p1 p2 xc xt q site\n";

            writer.write(header);
            for (Entry<FrequencyBand, SharedFrequencyBandParameters> entry : sharedParametersByFreqBand.entrySet()) {
                FrequencyBand fb = entry.getKey();
                SharedFrequencyBandParameters shared = entry.getValue();
                sb.append(dfmt6.format(shared.getLowFrequency())).append(SEP);
                sb.append(dfmt6.format(shared.getHighFrequency())).append(SEP);
                sb.append(dfmt6.format(shared.getVelocity0())).append(SEP);
                sb.append(dfmt6.format(shared.getVelocity1())).append(SEP);
                sb.append(dfmt6.format(shared.getVelocity2())).append(SEP);
                sb.append(dfmt6.format(shared.getMinSnr())).append(SEP);
                sb.append(dfmt6.format(shared.getMaxLength())).append(SEP);
                sb.append(dfmt6.format(shared.getMeasurementTime())).append(SEP);
                sb.append(dfmt6.format(shared.getBeta0())).append(SEP);
                sb.append(dfmt6.format(shared.getBeta1())).append(SEP);
                sb.append(dfmt6.format(shared.getBeta2())).append(SEP);
                sb.append(dfmt6.format(shared.getGamma0())).append(SEP);
                sb.append(dfmt6.format(shared.getGamma1())).append(SEP);
                sb.append(dfmt6.format(shared.getGamma2())).append(SEP);
                sb.append(dfmt6.format(shared.getS1())).append(SEP);
                sb.append(dfmt6.format(shared.getS2())).append(SEP);
                sb.append(dfmt6.format(shared.getXc())).append(SEP);
                sb.append(dfmt6.format(shared.getXt())).append(SEP);
                sb.append(dfmt6.format(shared.getQ())).append(SEP);

                if (stationParametersByFreqBand != null && stationParametersByFreqBand.containsKey(fb)) {
                    sb.append(dfmt6.format(stationParametersByFreqBand.get(fb).getSiteTerm())).append("\n");
                } else {
                    sb.append(DEFAULT_SITE_TERM).append("\n");
                }
            }
            writer.write(sb.toString());
        }
    }

    private void writeParams(File paramFile, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand) throws IOException {
        writeParams(paramFile, sharedParametersByFreqBand, null);
    }
}
