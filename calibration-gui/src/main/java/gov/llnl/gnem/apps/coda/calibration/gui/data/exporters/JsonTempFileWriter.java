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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.MeasuredMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ParamTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ReferenceMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.ReferenceMwParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SharedFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SiteFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

@Component
public class JsonTempFileWriter implements ParamTempFileWriter, MeasuredMwTempFileWriter, ReferenceMwTempFileWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonTempFileWriter.class);

    private static final String CALIBRATION_JSON_NAME = "Calibration_Parameters.json";
    private static final String MW_JSON_NAME = "Measured_Events.json";

    private ObjectMapper mapper;

    public JsonTempFileWriter() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.addMixIn(SharedFrequencyBandParameters.class, SharedFrequencyBandParametersFileMixin.class);
        mapper.addMixIn(SiteFrequencyBandParameters.class, SiteFrequencyBandParametersFileMixin.class);
        mapper.addMixIn(ReferenceMwParameters.class, ReferenceMwParametersFileMixin.class);
        mapper.addMixIn(VelocityConfiguration.class, VelocityConfigurationFileMixin.class);
    }

    @Override
    public void writeParams(Path folder, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand, Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters,
            VelocityConfiguration velocityConfig) {
        try {
            JsonNode document = createOrGetDocument(folder, CALIBRATION_JSON_NAME);
            writeParams(createOrGetFile(folder, CALIBRATION_JSON_NAME), document, sharedParametersByFreqBand, siteParameters, velocityConfig);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void writeReferenceMwParams(Path folder, List<ReferenceMwParameters> mws) {
        try {
            JsonNode document = createOrGetDocument(folder, CALIBRATION_JSON_NAME);
            List<ReferenceMwParameters> refMws = mws.stream().filter(mw -> mw.getRefMw() != 0.0).collect(Collectors.toList());
            writeReferenceEvents(createOrGetFile(folder, CALIBRATION_JSON_NAME), document, refMws);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void writeMeasuredMws(Path folder, List<MeasuredMwDetails> measuredMwsDetails) {
        writeMeasuredMws(folder, MW_JSON_NAME, measuredMwsDetails);
    }

    @Override
    public void writeMeasuredMws(Path folder, String filename, List<MeasuredMwDetails> measuredMwsDetails) {
        try {
            JsonNode document = createOrGetDocument(folder, filename);
            writeMeasuredEvents(createOrGetFile(folder, filename), document, measuredMwsDetails);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void writeParams(File file, JsonNode document, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand,
            Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters, VelocityConfiguration velocityConfig) throws IOException {
        writeArrayNodeToFile(file, document, sharedParametersByFreqBand.values(), CalibrationJsonConstants.BAND_FIELD);
        if (siteParameters != null && !siteParameters.isEmpty()) {
            Map<String, Map<String, List<SiteFrequencyBandParameters>>> siteBands = siteParameters.entrySet()
                                                                                                  .stream()
                                                                                                  .collect(
                                                                                                          Collectors.groupingBy(
                                                                                                                  e -> e.getKey().getNetworkName(),
                                                                                                                      Collectors.toMap(
                                                                                                                              e -> e.getKey().getStationName(),
                                                                                                                                  e -> e.getValue().values().stream().collect(Collectors.toList()))));

            writeArrayNodeToFile(file, document, siteBands.entrySet(), CalibrationJsonConstants.SITE_CORRECTION_FIELD);
        }
        if (velocityConfig != null) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.VELOCITY_CONFIGURATION, mapper.valueToTree(velocityConfig));
        }
    }

    private void writeReferenceEvents(File file, JsonNode document, List<ReferenceMwParameters> referenceMwsDetails) throws IOException {
        writeArrayNodeToFile(file, document, referenceMwsDetails, CalibrationJsonConstants.REFERENCE_EVENTS_FIELD);
    }

    private void writeMeasuredEvents(File file, JsonNode document, List<MeasuredMwDetails> measuredMwsDetails) throws IOException {
        writeArrayNodeToFile(file, document, measuredMwsDetails, CalibrationJsonConstants.MEASURED_EVENTS_FIELD);
    }

    private <T> void writeArrayNodeToFile(File file, JsonNode document, Collection<T> values, String field) throws IllegalArgumentException, IOException {
        ArrayNode arrayNode = mapper.createArrayNode();
        for (T value : values) {
            arrayNode.add(mapper.valueToTree(value));
        }
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.SCHEMA_FIELD, mapper.valueToTree(CalibrationJsonConstants.SCHEMA_VALUE));
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.TYPE_FIELD, mapper.valueToTree(CalibrationJsonConstants.TYPE_VALUE));
        if (arrayNode.size() > 0) {
            writeFieldNodeToFile(file, document, field, arrayNode);
        }
    }

    private void writeFieldNodeToFile(File file, JsonNode document, String field, JsonNode node) throws IOException {
        if (node != null) {
            Files.deleteIfExists(file.toPath());
            document = addOrReplaceNode(document, field, node);
            try (BufferedWriter outstream = Files.newBufferedWriter(file.toPath())) {
                mapper.writerWithDefaultPrettyPrinter().writeValue(outstream, document);
            }
        }
    }

    private JsonNode addOrReplaceNode(JsonNode document, String field, JsonNode node) {
        if (document instanceof ObjectNode) {
            if (node != null) {
                document = deleteNodesIfExists(document, field);
                ((ObjectNode) document).set(field, node);
            }
            return document;
        } else {
            throw new IllegalStateException("Invalid document configuration encountered while creating document: " + document);
        }
    }

    private JsonNode deleteNodesIfExists(JsonNode document, String field) {
        for (JsonNode node : document) {
            if (node instanceof ObjectNode) {
                ObjectNode object = (ObjectNode) node;
                object.remove(field);
            }
        }
        return document;
    }

    private JsonNode createOrGetDocument(Path folder, String filename) throws JsonProcessingException, IOException {
        Path filePath = folder.resolve(filename);
        JsonNode rootNode;
        if (Files.exists(filePath) && Files.isRegularFile(filePath)) {
            rootNode = new ObjectMapper().readTree(filePath.toFile());
        } else {
            rootNode = mapper.createObjectNode();
        }

        if (rootNode == null) {
            rootNode = mapper.createObjectNode();
        }
        return rootNode;
    }

    private File createOrGetFile(Path folder, String filename) throws IOException {
        Files.deleteIfExists(folder.resolve(filename));
        File file = Files.createFile(folder.resolve(filename)).toFile();
        file.deleteOnExit();
        return file;
    }
}