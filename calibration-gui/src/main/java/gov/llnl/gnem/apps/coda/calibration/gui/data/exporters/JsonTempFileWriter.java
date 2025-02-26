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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationJsonConstants;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.MeasuredMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ParamTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ReferenceMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.SpectraRatioTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.SpectraTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.api.ValidationMwTempFileWriter;
import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings;
import gov.llnl.gnem.apps.coda.calibration.model.domain.EventSpectraReport;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.MdacFiFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.MdacPsFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.ReferenceMwParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.ShapeFitterConstraintsFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SharedFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SiteFrequencyBandParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.SpectraRatioPairDetailsMetadataMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.ValidationMwParametersFileMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.WaveformMetadataMixin;
import gov.llnl.gnem.apps.coda.calibration.model.domain.mixins.WaveformPickMixin;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadataImpl;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResult;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResultJoint;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;

@Component
public class JsonTempFileWriter implements SpectraTempFileWriter, ParamTempFileWriter, MeasuredMwTempFileWriter, ReferenceMwTempFileWriter, ValidationMwTempFileWriter, SpectraRatioTempFileWriter {

    private static final Logger log = LoggerFactory.getLogger(JsonTempFileWriter.class);

    private static final String CALIBRATION_JSON_NAME = "Calibration_Parameters.json";
    private static final String MW_JSON_NAME = "Measured_Events.json";
    private static final String RATIO_JSON_NAME = "Spectra_Ratio_Event_Pair_Details.json";

    private ObjectMapper mapper;

    private ObjectMapper streamedMapper;

    public JsonTempFileWriter() {
        mapper = new ObjectMapper();
        mapper.setSerializationInclusion(Include.NON_NULL);
        mapper.setSerializationInclusion(Include.NON_EMPTY);
        mapper.addMixIn(SharedFrequencyBandParameters.class, SharedFrequencyBandParametersFileMixin.class);
        mapper.addMixIn(SiteFrequencyBandParameters.class, SiteFrequencyBandParametersFileMixin.class);
        mapper.addMixIn(ReferenceMwParameters.class, ReferenceMwParametersFileMixin.class);
        mapper.addMixIn(CalibrationSettings.class, CalibrationSettingsFileMixin.class);
        mapper.addMixIn(VelocityConfiguration.class, VelocityConfigurationFileMixin.class);
        mapper.addMixIn(ShapeFitterConstraints.class, ShapeFitterConstraintsFileMixin.class);
        mapper.addMixIn(MdacParametersFI.class, MdacFiFileMixin.class);
        mapper.addMixIn(MdacParametersPS.class, MdacPsFileMixin.class);
        mapper.addMixIn(ValidationMwParameters.class, ValidationMwParametersFileMixin.class);
        mapper.addMixIn(SpectraRatioPairDetailsMetadata.class, SpectraRatioPairDetailsMetadataMixin.class);
        mapper.addMixIn(WaveformMetadata.class, WaveformMetadataMixin.class);
        mapper.addMixIn(WaveformPick.class, WaveformPickMixin.class);

        //For writing to an output stream we don't control so
        //Jackson doesn't attempt to close it when we go out of scope
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);

        streamedMapper = new ObjectMapper(jsonFactory);
        streamedMapper.addMixIn(SpectraRatioPairDetailsMetadata.class, SpectraRatioPairDetailsMetadataMixin.class);
        streamedMapper.addMixIn(WaveformMetadata.class, WaveformMetadataMixin.class);
        streamedMapper.addMixIn(WaveformPick.class, WaveformPickMixin.class);
    }

    @Override
    public void writeParams(Path folder, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand, Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters,
            List<MdacParametersFI> fi, List<MdacParametersPS> ps, CalibrationSettings calibrationSettings, VelocityConfiguration velocityConfig, ShapeFitterConstraints shapeConstraints,
            String polygonGeoJSON) {
        try {
            JsonNode document = createOrGetDocument(folder, CALIBRATION_JSON_NAME);
            writeParams(
                    createOrGetFile(folder, CALIBRATION_JSON_NAME),
                        document,
                        sharedParametersByFreqBand,
                        siteParameters,
                        fi,
                        ps,
                        calibrationSettings,
                        velocityConfig,
                        shapeConstraints,
                        polygonGeoJSON);
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
    public void writeValidationMws(Path folder, List<ValidationMwParameters> validationMws) {
        writeValidationMws(folder, CALIBRATION_JSON_NAME, validationMws);
    }

    @Override
    public void writeValidationMws(Path folder, String filename, List<ValidationMwParameters> validationMws) {
        try {
            JsonNode document = createOrGetDocument(folder, filename);
            writeValidationEvents(createOrGetFile(folder, filename), document, validationMws);
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

    @Override
    public void writeSpectraValues(Path folder, String filename, List<EventSpectraReport> measurements) {
        try {
            JsonNode document = createOrGetDocument(folder, filename);
            writeEventSpectraReports(createOrGetFile(folder, filename), document, measurements);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void writeSpectraRatioDetails(BufferedWriter fileWriter, SpectraRatioPairDetailsMetadata spectraRatioPairDetails) {
        try {
            //Writes out JSON objects as one-per-line
            //Useful mostly for when objects are large and we don't
            //want to deserialize them all at once (memory or time)
            ObjectWriter writer = streamedMapper.writer();
            writer.writeValue(fileWriter, spectraRatioPairDetails);
            fileWriter.append('\n');
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    @Override
    public void writeSpectraRatiosReport(Path folder, SpectraRatiosReportByEventPair ratioReport, EventPair eventPair) {
        try {
            JsonNode document = createOrGetDocument(folder, RATIO_JSON_NAME);
            List<SpectraRatioPairDetailsMetadata> ratioDetails = new ArrayList<>(ratioReport.getRatiosList(eventPair)
                                                                                            .stream()
                                                                                            .map(SpectraRatioPairDetailsMetadataImpl::new)
                                                                                            .collect(Collectors.toList()));
            SpectraRatioPairInversionResult pairInversion = ratioReport.getReport().getInversionEstimates().get(eventPair);
            SpectraRatioPairInversionResultJoint jointInversion = ratioReport.getReport().getJointInversionEstimates().get(eventPair);

            writeSpectraRatioEvents(createOrGetFile(folder, RATIO_JSON_NAME), document, ratioDetails);
            writeJsonNodeToFile(createOrGetFile(folder, RATIO_JSON_NAME), document, pairInversion, "pair_inversion");
            writeJsonNodeToFile(createOrGetFile(folder, RATIO_JSON_NAME), document, jointInversion, "joint_inversion");

            Pair<Double, Double> userSetFreqHighLow = ratioReport.getReport().getUserAdjustedLowAndHighFreqLevels().get(eventPair);
            if (userSetFreqHighLow != null) {
                ObjectNode userSetFrequencies = mapper.createObjectNode();
                userSetFrequencies.put("low_frequency_level", userSetFreqHighLow.getX());
                userSetFrequencies.put("high_frequency_level", userSetFreqHighLow.getY());

                Double appStress = ratioReport.getReport().getUserSetStressResult();
                if (appStress != null) {
                    userSetFrequencies.put("user_set_lfl_hfl_stress", ratioReport.getReport().getUserSetStressResult());
                }

                writeJsonNodeToFile(createOrGetFile(folder, RATIO_JSON_NAME), document, userSetFrequencies, "user_saved_frequency_levels");
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void writeEventSpectraReports(File file, JsonNode document, List<EventSpectraReport> measurements) throws IOException {
        writeArrayNodeToFile(file, document, measurements, CalibrationJsonConstants.SPECTRA);
    }

    private void writeParams(File file, JsonNode document, Map<FrequencyBand, SharedFrequencyBandParameters> sharedParametersByFreqBand,
            Map<Station, Map<FrequencyBand, SiteFrequencyBandParameters>> siteParameters, List<MdacParametersFI> fi, List<MdacParametersPS> ps, CalibrationSettings calibrationSettings,
            VelocityConfiguration velocityConfig, ShapeFitterConstraints shapeConstraints, String polygonGeoJSON) throws IOException {
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
        if (fi != null && !fi.isEmpty()) {
            writeArrayNodeToFile(file, document, fi, CalibrationJsonConstants.MDAC_FI_FIELD);
        }
        if (ps != null && !ps.isEmpty()) {
            writeArrayNodeToFile(file, document, ps, CalibrationJsonConstants.MDAC_PS_FIELD);
        }
        if (calibrationSettings != null) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.CALIBRATION_SETTINGS, mapper.valueToTree(calibrationSettings));
        }
        if (velocityConfig != null) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.VELOCITY_CONFIGURATION, mapper.valueToTree(velocityConfig));
        }
        if (shapeConstraints != null) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.SHAPE_CONSTRAINTS, mapper.valueToTree(shapeConstraints));
        }
        if (polygonGeoJSON != null && !polygonGeoJSON.isEmpty()) {
            writeFieldNodeToFile(file, document, CalibrationJsonConstants.POLYGON_FIELD, mapper.readTree(polygonGeoJSON));
        }
    }

    private void writeReferenceEvents(File file, JsonNode document, List<ReferenceMwParameters> referenceMwsDetails) throws IOException {
        writeArrayNodeToFile(file, document, referenceMwsDetails, CalibrationJsonConstants.REFERENCE_EVENTS_FIELD);
    }

    private void writeValidationEvents(File file, JsonNode document, List<ValidationMwParameters> validationMws) throws IOException {
        writeArrayNodeToFile(file, document, validationMws, CalibrationJsonConstants.VALIDATION_EVENTS_FIELD);
    }

    private void writeMeasuredEvents(File file, JsonNode document, List<MeasuredMwDetails> measuredMwsDetails) throws IOException {
        writeArrayNodeToFile(file, document, measuredMwsDetails, CalibrationJsonConstants.MEASURED_EVENTS_FIELD);
    }

    private void writeSpectraRatioEvents(File file, JsonNode document, List<SpectraRatioPairDetailsMetadata> spectraRatioPairDetails) throws IOException {
        writeArrayNodeToFile(file, document, spectraRatioPairDetails, "spectra-ratio-pair-details");
    }

    private <T> void writeJsonNodeToFile(File file, JsonNode document, T objectNode, String field) throws IllegalArgumentException, IOException {
        JsonNode jsonNode = mapper.valueToTree(objectNode);

        writeMetadataHeaders(file, document);
        writeFieldNodeToFile(file, document, field, jsonNode);
    }

    private <T> void writeArrayNodeToFile(File file, JsonNode document, Collection<T> values, String field) throws IllegalArgumentException, IOException {
        ArrayNode arrayNode = mapper.createArrayNode();
        for (T value : values) {
            arrayNode.add(mapper.valueToTree(value));
        }
        writeMetadataHeaders(file, document);
        if (arrayNode.size() > 0) {
            writeFieldNodeToFile(file, document, field, arrayNode);
        }
    }

    private void writeMetadataHeaders(File file, JsonNode document) throws IOException {
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.SCHEMA_FIELD, mapper.valueToTree(CalibrationJsonConstants.SCHEMA_VALUE));
        writeFieldNodeToFile(file, document, CalibrationJsonConstants.TYPE_FIELD, mapper.valueToTree(CalibrationJsonConstants.TYPE_VALUE));
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

        if (rootNode == null || rootNode.getNodeType().equals(JsonNodeType.MISSING)) {
            rootNode = mapper.createObjectNode();
        }
        return rootNode;
    }

    private File createOrGetFile(Path folder, String filename) throws IOException {
        Files.deleteIfExists(folder.resolve(filename));
        File file = Files.createFile(folder.resolve(filename)).toFile();
        return file;
    }

}
