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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.FileToParameterConverter;
import gov.llnl.gnem.apps.coda.calibration.gui.converters.param.RawGeoJSON;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.events.ParametersLoadedEvent;
import gov.llnl.gnem.apps.coda.calibration.gui.events.UpdateMapPolygonEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.CalibrationSettings;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteCorrections;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.common.gui.converters.api.FileToWaveformConverter;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import reactor.core.publisher.Mono;

@Component
public class CodaParamLoadingController {

    private static final Logger log = LoggerFactory.getLogger(CodaParamLoadingController.class);

    private ParameterClient paramsClient;

    private EventClient eventClient;

    /** The max batching size for posting data to the data layer. */
    private int maxBatching = 100;

    private List<FileToParameterConverter<?>> fileConverters;

    private EventBus bus;

    /**
     * Instantiates a new coda param loading controller to handle loading files
     * and converting them to domain objects.
     *
     * @param fileConverters
     *            the file converters available for use in converting a file to
     *            one or more domain objects
     * @param paramsClient
     *            the params client to use for saving and loading calibration
     *            parameter objects, e.g. {@link SharedFrequencyBandParameters}
     * @param eventClient
     *            the event client to use for saving and loading {@link Event}
     *            and related domain objects
     * @param bus
     *            the event bus to register for sending and receiving loading
     *            related events
     */
    @Autowired
    public CodaParamLoadingController(List<FileToParameterConverter<?>> fileConverters, ParameterClient paramsClient, EventClient eventClient, EventBus bus) {
        this.fileConverters = fileConverters;
        this.paramsClient = paramsClient;
        this.eventClient = eventClient;
        this.bus = bus;
        if (paramsClient == null) {
            throw new IllegalStateException("Unable to find implementation of Coda Param loding client");
        }

        if (eventClient == null) {
            throw new IllegalStateException("Unable to find implementation of Reference Event loding client");
        }
    }

    /**
     * Attempt to load a list of files based on whatever
     * {@link FileToWaveformConverter} instances are available in the runtime
     * container
     *
     * @param files
     *            the files to attempt to load
     */
    public void loadFiles(List<File> files) {
        CompletableFuture.runAsync(() -> {
            if (fileConverters != null && !fileConverters.isEmpty()) {
                List<File> validFiles = files.stream().filter(f -> validPath(f.toPath())).collect(Collectors.toList());
                convertFiles(validFiles);
                bus.post(new ParametersLoadedEvent());
            }
        });
    }

    /**
     * Given a set of parsed file handlers attempt to convert them into domain
     * objects and route them through the data access layer to the appropriate
     * endpoints
     *
     * @param validFiles
     *            the files a {@link FileToWaveformConverter} indicated it was
     *            capable of processing
     */
    protected void convertFiles(List<File> validFiles) {
        fileConverters.stream().forEach(fileConverter -> fileConverter.convertFiles(validFiles).subscribe(result -> {
            // TODO: Feedback to the user about failure causes!
            if (result.isSuccess()) {
                Optional<?> res = result.getResultPayload();
                if (res.isPresent()) {
                    if (res.get() instanceof SharedFrequencyBandParameters) {
                        SharedFrequencyBandParameters sfb = (SharedFrequencyBandParameters) res.get();
                        try {
                            Mono<String> request = paramsClient.setSharedFrequencyBandParameter(sfb);
                            if (request != null) {
                                request.retry(3).subscribe();
                            } else {
                                log.error("Returned a null request from the parameter client while posting SharedFrequencyBandParameters {}", sfb);
                            }
                        } catch (JsonProcessingException ex) {
                            log.trace(ex.getMessage(), ex);
                        }
                    } else if (res.get() instanceof SiteCorrections) {
                        try {
                            Mono<String> request = paramsClient.setSiteSpecificFrequencyBandParameter(new ArrayList<>(((SiteCorrections) res.get()).getSiteCorrections()));
                            if (request != null) {
                                request.retry(3).subscribe();
                            } else {
                                log.error("Returned a null request from the parameter client while posting SiteFrequencyBandParameters {}", (res.get()));
                            }
                        } catch (JsonProcessingException ex) {
                            log.trace(ex.getMessage(), ex);
                        }
                    } else if (res.get() instanceof MdacParametersPS) {
                        MdacParametersPS entry = (MdacParametersPS) res.get();
                        try {
                            Mono<String> request = paramsClient.setPsParameter(entry);
                            if (request != null) {
                                request.retry(3).subscribe();
                            } else {
                                log.error("Returned a null request from the parameter client while posting MdacParametersPS {}", entry);
                            }
                        } catch (JsonProcessingException ex) {
                            log.trace(ex.getMessage(), ex);
                        }
                    } else if (res.get() instanceof MdacParametersFI) {
                        MdacParametersFI entry = (MdacParametersFI) res.get();
                        try {
                            Mono<String> request = paramsClient.setFiParameter(entry);
                            if (request != null) {
                                request.retry(3).subscribe();
                            } else {
                                log.error("Returned a null request from the parameter client while posting MdacParametersFI {}", entry);
                            }
                        } catch (JsonProcessingException ex) {
                            log.trace(ex.getMessage(), ex);
                        }
                    } else if (res.get() instanceof ValidationMwParameters) {
                        ValidationMwParameters entry = (ValidationMwParameters) res.get();
                        try {
                            Mono<String> request = eventClient.postValidationEvents(Collections.singletonList(entry));
                            if (request != null) {
                                request.retry(3).subscribe();
                            } else {
                                log.error("Returned a null request from the parameter client while posting ReferenceMwParameters {}", entry);
                            }
                        } catch (JsonProcessingException ex) {
                            log.trace(ex.getMessage(), ex);
                        }
                    } else if (res.get() instanceof ReferenceMwParameters) {
                        ReferenceMwParameters entry = (ReferenceMwParameters) res.get();
                        try {
                            Mono<String> request = eventClient.postReferenceEvents(Collections.singletonList(entry));
                            if (request != null) {
                                request.retry(3).subscribe();
                            } else {
                                log.error("Returned a null request from the parameter client while posting ReferenceMwParameters {}", entry);
                            }
                        } catch (JsonProcessingException ex) {
                            log.trace(ex.getMessage(), ex);
                        }
                    } else if (res.get() instanceof CalibrationSettings) {
                        CalibrationSettings entry = (CalibrationSettings) res.get();
                        Mono<String> request = paramsClient.updateCalibrationSettings(entry);
                        if (request != null) {
                            request.retry(3).subscribe();
                        } else {
                            log.error("Returned a null request from the parameter client while posting CalibrationSettings {}", entry);
                        }
                    } else if (res.get() instanceof VelocityConfiguration) {
                        VelocityConfiguration entry = (VelocityConfiguration) res.get();
                        Mono<String> request = paramsClient.updateVelocityConfiguration(entry);
                        if (request != null) {
                            request.retry(3).subscribe();
                        } else {
                            log.error("Returned a null request from the parameter client while posting VelocityConfiguration {}", entry);
                        }
                    } else if (res.get() instanceof ShapeFitterConstraints) {
                        ShapeFitterConstraints entry = (ShapeFitterConstraints) res.get();
                        Mono<String> request = paramsClient.updateShapeFitterConstraints(entry);
                        if (request != null) {
                            request.retry(3).subscribe();
                        } else {
                            log.error("Returned a null request from the parameter client while posting ShapeFitterConstraints {}", entry);
                        }
                    } else if (res.get() instanceof RawGeoJSON) {
                        RawGeoJSON entry = (RawGeoJSON) res.get();
                        bus.post(new UpdateMapPolygonEvent(entry.getRawGeoJSON()));
                    }
                }
            }
        }));

    }

    private boolean validPath(Path p) {
        Optional<FileToParameterConverter<?>> match = Optional.ofNullable(fileConverters).orElseGet(Collections::emptyList).stream().filter(fc -> fc.getMatchingPattern().matches(p)).findAny();
        return match.isPresent();
    }

    public int getMaxBatching() {
        return maxBatching;
    }

    public void setMaxBatching(int maxBatching) {
        this.maxBatching = maxBatching;
    }
}
