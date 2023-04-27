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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.MeasuredMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ParamTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ReferenceMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.SpectraTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ValidationMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.model.domain.EventSpectraReport;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.gui.util.CommonGuiUtils;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import reactor.core.scheduler.Schedulers;

@Component
public class ParamExporter {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private ParameterClient paramClient;
    private List<ParamTempFileWriter> paramWriters;

    private EventClient eventClient;
    private List<MeasuredMwTempFileWriter> mwWriters;
    private List<ReferenceMwTempFileWriter> referenceMwWriters;
    private List<ValidationMwTempFileWriter> validationMwWriters;
    private List<SpectraTempFileWriter> spectraWriters;

    @Autowired
    public ParamExporter(ParameterClient paramClient, EventClient eventClient, List<ParamTempFileWriter> paramWriters, List<MeasuredMwTempFileWriter> mwWriters,
            List<ReferenceMwTempFileWriter> referenceMwWriters, List<ValidationMwTempFileWriter> validationMwWriters, List<SpectraTempFileWriter> spectraWriters) {
        this.paramClient = paramClient;
        this.paramWriters = paramWriters;
        this.eventClient = eventClient;
        this.mwWriters = mwWriters;
        this.referenceMwWriters = referenceMwWriters;
        this.validationMwWriters = validationMwWriters;
        this.spectraWriters = spectraWriters;
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

            List<MdacParametersFI> fi = paramClient.getFiParameters().toStream().filter(Objects::nonNull).collect(Collectors.toList());
            List<MdacParametersPS> ps = paramClient.getPsParameters().toStream().filter(Objects::nonNull).collect(Collectors.toList());
            VelocityConfiguration velocity = paramClient.getVelocityConfiguration().subscribeOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(5l));
            ShapeFitterConstraints shapeConstraints = paramClient.getShapeFitterConstraints().subscribeOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(5l));
            String polygonGeoJSON = paramClient.getMapPolygon().subscribeOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(5l));

            for (ParamTempFileWriter writer : paramWriters) {
                writer.writeParams(tmpFolder, sharedParametersByFreqBand, siteParameters, fi, ps, velocity, shapeConstraints, polygonGeoJSON);
            }
        }

        if (mwWriters != null) {
            List<MeasuredMwDetails> measuredMwsDetails = new ArrayList<>(eventClient.getMeasuredEventDetails()
                                                                                    .filter(Objects::nonNull)
                                                                                    .filter(MeasuredMwDetails::isValid)
                                                                                    .toStream()
                                                                                    .collect(Collectors.toList()));
            for (MeasuredMwTempFileWriter writer : mwWriters) {
                writer.writeMeasuredMws(tmpFolder, measuredMwsDetails);
            }

            if (referenceMwWriters != null) {
                List<ReferenceMwParameters> referenceMws = new ArrayList<>(eventClient.getReferenceEvents().filter(Objects::nonNull).toStream().collect(Collectors.toList()));
                for (ReferenceMwTempFileWriter writer : referenceMwWriters) {
                    writer.writeReferenceMwParams(tmpFolder, referenceMws);
                }
            }

            if (validationMwWriters != null) {
                List<ValidationMwParameters> validationMws = new ArrayList<>(eventClient.getValidationEvents().filter(Objects::nonNull).toStream().collect(Collectors.toList()));
                for (ValidationMwTempFileWriter writer : validationMwWriters) {
                    writer.writeValidationMws(tmpFolder, validationMws);
                }
            }
        }

        return CommonGuiUtils.zipDirectory(tmpFolder);
    }

    public void writeMeasuredMws(Path path, String filename, List<MeasuredMwDetails> mws) {
        for (MeasuredMwTempFileWriter writer : mwWriters) {
            writer.writeMeasuredMws(path, filename, mws);
        }
    }

    public void writeSpectra(Path path, String filename, List<EventSpectraReport> formattedExportValues) {
        for (SpectraTempFileWriter writer : spectraWriters) {
            writer.writeSpectraValues(path, filename, formattedExportValues);
        }
    }
}
