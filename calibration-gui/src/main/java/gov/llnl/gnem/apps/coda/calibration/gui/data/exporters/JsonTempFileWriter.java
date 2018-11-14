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
import java.util.List;
import java.util.Map;

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
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SharedFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

@Component
public class JsonTempFileWriter implements ParamTempFileWriter, MeasuredMwTempFileWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonTempFileWriter.class);

    private static final String CALIBRATION_JSON_NAME = "Calibration_Parameters.json";
    private static final String MW_JSON_NAME = "Measured_Events.json";

    private ObjectMapper mapper;

    public JsonTempFileWriter() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.addMixIn(SharedFrequencyBandParameters.class, SharedFrequencyBandParametersFileMixin.class);
    }

    @Override
    public void writeParams(Path folder, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand, Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters) {
        try {
            JsonNode document = createOrGetDocument(folder, CALIBRATION_JSON_NAME);
            writeParams(createOrGetFile(folder, CALIBRATION_JSON_NAME), document, sharedParametersByFreqBand, siteParameters);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void writeParams(Path folder, List<MeasuredMwDetails> measuredMwsDetails) {
        try {
            JsonNode document = createOrGetDocument(folder, MW_JSON_NAME);
            writeMeasuredEvents(createOrGetFile(folder, MW_JSON_NAME), document, measuredMwsDetails);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void writeParams(File file, JsonNode document, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand,
            Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters) throws IOException {
        ArrayNode arrayNode = mapper.createArrayNode();
        for (SharedFrequencyBandParameters sfb : sharedParametersByFreqBand.values()) {
            //TODO: Validation logic            
            //if sfb is valid
            arrayNode.add(mapper.valueToTree(sfb));
            //else log error
        }
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.SCHEMA_FIELD, mapper.valueToTree(CalibrationJsonConstants.SCHEMA_VALUE));
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.TYPE_FIELD, mapper.valueToTree(CalibrationJsonConstants.TYPE_VALUE));
        if (arrayNode.size() > 0) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.BAND_FIELD, arrayNode);
        }
    }

    private void writeMeasuredEvents(File file, JsonNode document, List<MeasuredMwDetails> measuredMwsDetails) throws IOException {
        ArrayNode arrayNode = mapper.createArrayNode();
        for (MeasuredMwDetails mw : measuredMwsDetails) {
            //TODO: Validation logic            
            //if mw is valid
            arrayNode.add(mapper.valueToTree(mw));
            //else log error
        }
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.SCHEMA_FIELD, mapper.valueToTree(CalibrationJsonConstants.SCHEMA_VALUE));
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.TYPE_FIELD, mapper.valueToTree(CalibrationJsonConstants.TYPE_VALUE));
        if (arrayNode.size() > 0) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.MEASURED_EVENTS_FIELD, arrayNode);
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
        if (document != null && document instanceof ObjectNode) {
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
        return rootNode;
    }

    private File createOrGetFile(Path folder, String filename) throws IOException {
        File file = Files.createFile(folder.resolve(filename)).toFile();
        file.deleteOnExit();
        return file;
    }
}
