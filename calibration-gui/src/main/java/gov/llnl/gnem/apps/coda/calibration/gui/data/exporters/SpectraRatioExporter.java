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
package gov.llnl.gnem.apps.coda.calibration.gui.data.exporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.SpectraRatioTempFileWriter;
import gov.llnl.gnem.apps.coda.common.gui.util.CommonGuiUtils;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;

@Component
public class SpectraRatioExporter {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private List<SpectraRatioTempFileWriter> spectraRatioWriters;

    @Autowired
    public SpectraRatioExporter(List<SpectraRatioTempFileWriter> spectraRatioWriters) {
        this.spectraRatioWriters = spectraRatioWriters;
    }

    public File createExportArchive(SpectraRatiosReportByEventPair spectraRatioReport, EventPair eventPair, Path directory) throws IOException {
        if (spectraRatioWriters != null) {
            for (SpectraRatioTempFileWriter writer : spectraRatioWriters) {
                writer.writeSpectraRatiosReport(directory, spectraRatioReport, eventPair);
            }
        }

        return CommonGuiUtils.zipDirectory(directory);
    }

    public void writeSpectraRatioPairDetails(BufferedWriter fileWriter, SpectraRatioPairDetailsMetadata ratio) {
        for (SpectraRatioTempFileWriter writer : spectraRatioWriters) {
            writer.writeSpectraRatioDetails(fileWriter, ratio);
        }
    }
}
