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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ParamTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

@Component
public class SwftStyleParamFileWriter implements ParamTempFileWriter {

    private static final char NEWLINE = '\n';

    private NumberFormat dfmt6 = NumberFormatFactory.sixDecimalOneLeadingZero();

    private static final double DEFAULT_SITE_TERM = 0.0;
    private static final char SEP = ' ';

    private static final Logger log = LoggerFactory.getLogger(SwftStyleParamFileWriter.class);

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
                sb.append(dfmt6.format(shared.getP1())).append(SEP);
                sb.append(dfmt6.format(shared.getS2())).append(SEP);
                sb.append(dfmt6.format(shared.getXc())).append(SEP);
                sb.append(dfmt6.format(shared.getXt())).append(SEP);
                sb.append(dfmt6.format(shared.getQ())).append(SEP);

                if (stationParametersByFreqBand != null && stationParametersByFreqBand.containsKey(fb)) {
                    sb.append(dfmt6.format(stationParametersByFreqBand.get(fb).getSiteTerm())).append(NEWLINE);
                } else {
                    sb.append(DEFAULT_SITE_TERM).append(NEWLINE);
                }
            }
            writer.write(sb.toString());
        }
    }

    @Override
    public void writeParams(Path folder, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand, Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters,
            List<MdacParametersFI> fi, List<MdacParametersPS> ps, VelocityConfiguration velocity, ShapeFitterConstraints shapeConstraints) {
        File sharedParamFile = new File(folder.toFile(), "Shared.param");
        sharedParamFile.deleteOnExit();
        try {
            writeParams(sharedParamFile, sharedParametersByFreqBand, null);

            for (Entry<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> stationEntry : siteParameters.entrySet()) {
                File stationParamFile = new File(folder.toFile(), stationEntry.getKey().getStationName() + ".param");
                writeParams(stationParamFile, sharedParametersByFreqBand, stationEntry.getValue());
                stationParamFile.deleteOnExit();
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }

    }

}
