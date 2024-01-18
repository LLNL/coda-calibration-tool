/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.MeasurementStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent.Status;
import gov.llnl.gnem.apps.coda.calibration.repository.SpectraRatioJointInversionSampleRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.SpectraRatioPairDetailsRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.SpectraRatioPairInversionSampleRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.AutopickingService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PeakVelocityMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraRatioPairDetailsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticCodaGenerationService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MdacCalculatorService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraRatioInversionCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import gov.llnl.gnem.apps.coda.common.service.util.MetadataUtils;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResult;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResultJoint;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatiosReport;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.SpectraRatioPairChangeEvent;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatioPairOperator;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
@Transactional
public class SpectraRatioServiceImpl implements SpectraRatioPairDetailsService {

    @Value(value = "${ratio-inversion.moment-error-range:0.001}")
    private double momentErrorRange;

    private Logger log = LoggerFactory.getLogger(SpectraRatioServiceImpl.class);

    private SpectraRatioPairDetailsRepository spectraRatioPairDetailsRepository;
    private SpectraRatioPairInversionSampleRepository spectraRatioPairInversionSampleRepository;
    private SpectraRatioJointInversionSampleRepository spectraRatioJontInversionSampleRepository;

    private NotificationService notificationService;

    private final WaveformService waveformService;
    private final PeakVelocityMeasurementService peakVelocityMeasurementsService;
    private final SharedFrequencyBandParametersService sharedParametersService;
    private final SiteFrequencyBandParametersService siteParamsService;
    private final ShapeCalibrationService shapeCalibrationService;
    private final SpectraCalculator spectraCalc;
    private final SyntheticCodaGenerationService syntheticGenerationService;
    private final ConfigurationService configService;
    private final MeasuredMwsService fitMwService;
    private final ReferenceMwParametersService refMwService;
    private final AutopickingService picker;

    private final MdacCalculatorService mdacService;
    private final MdacParametersFiService mdacFiService;
    private final MdacParametersPsService mdacPsService;

    private static final AtomicLong atomicLong = new AtomicLong(0l);

    private final ExecutorService measureService;

    @Autowired
    public SpectraRatioServiceImpl(SpectraRatioPairDetailsRepository spectraRatioRepository, SpectraRatioPairInversionSampleRepository spectraRatioPairInversionSampleRepository,
            SpectraRatioJointInversionSampleRepository spectraRatioJontInversionSampleRepository, NotificationService notificationService, WaveformService waveformService,
            PeakVelocityMeasurementService peakVelocityMeasurementsService, SharedFrequencyBandParametersService sharedParametersService, SiteFrequencyBandParametersService siteParamsService,
            ShapeCalibrationService shapeCalibrationService, SpectraCalculator spectraCalc, SyntheticCodaGenerationService syntheticGenerationService, ConfigurationService configService,
            AutopickingService picker, @Qualifier("MeasurementExecutorService") ExecutorService measureService, final MdacCalculatorService mdacService, final MdacParametersFiService mdacFiService,
            final MdacParametersPsService mdacPsService, MeasuredMwsService fitMwService, ReferenceMwParametersService refMwService) {
        this.spectraRatioPairDetailsRepository = spectraRatioRepository;
        this.spectraRatioPairInversionSampleRepository = spectraRatioPairInversionSampleRepository;
        this.spectraRatioJontInversionSampleRepository = spectraRatioJontInversionSampleRepository;
        this.notificationService = notificationService;
        this.waveformService = waveformService;
        this.peakVelocityMeasurementsService = peakVelocityMeasurementsService;
        this.sharedParametersService = sharedParametersService;
        this.siteParamsService = siteParamsService;
        this.shapeCalibrationService = shapeCalibrationService;
        this.spectraCalc = spectraCalc;
        this.syntheticGenerationService = syntheticGenerationService;
        this.configService = configService;
        this.picker = picker;
        this.measureService = measureService;
        this.mdacService = mdacService;
        this.mdacFiService = mdacFiService;
        this.mdacPsService = mdacPsService;
        this.fitMwService = fitMwService;
        this.refMwService = refMwService;
    }

    @Transactional
    @Override
    public void delete(SpectraRatioPairDetails value) {
        spectraRatioPairDetailsRepository.delete(value);
        notificationService.post(new SpectraRatioPairChangeEvent(getIds(value)).setDelete(true));
    }

    @Transactional
    @Override
    public List<SpectraRatioPairDetails> save(Iterable<SpectraRatioPairDetails> entities) {
        List<SpectraRatioPairDetails> saved = new LinkedList<>();
        for (SpectraRatioPairDetails entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Transactional
    @Override
    public void delete(Iterable<Long> ids) {
        List<SpectraRatioPairDetails> toDelete = spectraRatioPairDetailsRepository.findAllById(ids);
        spectraRatioPairDetailsRepository.deleteAllInBatch(toDelete);
        notificationService.post(new SpectraRatioPairChangeEvent(getIds(toDelete)).setDelete(true));
    }

    @Transactional
    @Override
    public SpectraRatioPairDetails save(SpectraRatioPairDetails entity) {
        SpectraRatioPairDetails value = spectraRatioPairDetailsRepository.save(entity);
        return value;
    }

    @Transactional
    @Override
    public SpectraRatioPairDetails update(SpectraRatioPairDetails entity) {
        SpectraRatioPairDetails update = null;
        if (entity.getId() != null) {
            update = findOneForUpdate(entity.getId());
        } else {
            update = spectraRatioPairDetailsRepository.findByWaveformIds(entity.getNumerWaveform().getId(), entity.getDenomWaveform().getId());
        }
        if (update != null) {
            update.mergeNonNullFields(entity);
            entity = update;
        }

        SpectraRatioPairOperator op = new SpectraRatioPairOperator(entity);
        op.updateDiffSegment();
        entity = save(entity);

        return entity;
    }

    @Override
    public SpectraRatioPairDetails findOne(Long id) {
        return spectraRatioPairDetailsRepository.findOneDetached(id);
    }

    @Override
    public SpectraRatioPairDetails findOneForUpdate(Long id) {
        return spectraRatioPairDetailsRepository.findById(id).orElse(new SpectraRatioPairDetails());
    }

    @Override
    public List<SpectraRatioPairDetails> findAll(Iterable<Long> ids) {
        return spectraRatioPairDetailsRepository.findAllById(ids);
    }

    @Override
    public List<SpectraRatioPairDetails> findAll() {
        return spectraRatioPairDetailsRepository.findAll();
    }

    @Override
    public long count() {
        return spectraRatioPairDetailsRepository.count();
    }

    public Class<SpectraRatioPairDetails> getEntityType() {
        return SpectraRatioPairDetails.class;
    }

    public Class<Integer> getIdType() {
        return Integer.class;
    }

    private List<Long> getIds(SpectraRatioPairDetails value) {
        return getIds(Collections.singletonList(value));
    }

    private List<Long> getIds(List<SpectraRatioPairDetails> vals) {
        return vals.stream().map(SpectraRatioPairDetails::getId).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Future<Result<SpectraRatiosReport>> makeSpectraRatioMeasurementsFromRatioData(Set<String> smallEventIds, Set<String> largeEventIds, List<RatioEventData> ratioEventData) {
        return makeSpectraRatioMeasurementsBase(false, smallEventIds, largeEventIds, eventIds -> getSpectraListFromRatioEventData(eventIds, ratioEventData), this::calcFreqRatioEmptyWaveform);
    }

    @Override
    @Transactional
    public Future<Result<SpectraRatiosReport>> makeSpectraRatioMeasurementsFromWaveforms(Boolean autoPickingEnabled, Boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds) {
        return makeSpectraRatioMeasurementsBase(
                persistResults,
                    smallEventIds,
                    largeEventIds,
                    eventIDs -> getSpectraListFromWaveforms(eventIDs, autoPickingEnabled, persistResults),
                    this::calcFreqRatio);
    }

    private Future<Result<SpectraRatiosReport>> makeSpectraRatioMeasurementsBase(Boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds,
            Function<Set<String>, List<SpectraMeasurement>> spectraListFunc, BiFunction<SpectraMeasurement, SpectraMeasurement, Result<SpectraRatioPairDetails>> calcRatioFunc) {
        log.debug("Starting spectra ratio calculation at {}", LocalDateTime.now());
        final Long id = atomicLong.getAndIncrement();

        Supplier<SpectraRatiosReport> ratioCalcFunc = () -> {

            Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> spectraSmallEventData = getSpectraMeasurementsMap(smallEventIds, spectraListFunc);
            Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> spectraLargeEventData = getSpectraMeasurementsMap(largeEventIds, spectraListFunc);

            Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData = new HashMap<>();

            List<SpectraRatioPairDetails> ratioDataList = new ArrayList<>();

            List<SpectraRatioPairDetails> userEdited = spectraRatioPairDetailsRepository.findByUserEditedTrue();
            Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> userEditedRatios = mapRatioDataToEvents(userEdited);

            // Loop through Events
            for (Entry<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> eventEntry : spectraSmallEventData.entrySet()) {
                Event curEvent = eventEntry.getKey();

                // Loop through Stations
                for (Entry<Station, Map<FrequencyBand, SpectraMeasurement>> stationEntry : eventEntry.getValue().entrySet()) {
                    Station curStation = stationEntry.getKey();

                    Map<FrequencyBand, SpectraMeasurement> dataSortedByFreq = new TreeMap<FrequencyBand, SpectraMeasurement>(stationEntry.getValue());

                    // Loop through frequencies
                    for (FrequencyBand freqBand : dataSortedByFreq.keySet()) {
                        if (spectraSmallEventData.containsKey(curEvent)) {
                            Map<FrequencyBand, SpectraMeasurement> smallDataAtStation = spectraSmallEventData.get(curEvent).get(curStation);

                            // Check the other event set to see if it has the same station available
                            for (Entry<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> secondEventEntry : spectraLargeEventData.entrySet()) {

                                // Don't expect to see the same event in both lists but need to rule it out
                                if (!secondEventEntry.getKey().getEventId().equalsIgnoreCase(curEvent.getEventId())) {

                                    Map<FrequencyBand, SpectraMeasurement> largeDataAtStation = secondEventEntry.getValue().get(curStation);
                                    // Make sure both events have data from the same station

                                    if (smallDataAtStation != null && largeDataAtStation != null) {
                                        SpectraMeasurement smallDataAtFreq = smallDataAtStation.get(freqBand);
                                        SpectraMeasurement largeDataAtFreq = largeDataAtStation.get(freqBand);
                                        if (smallDataAtFreq != null && largeDataAtFreq != null) {
                                            SpectraMeasurement smallSpectra = smallDataAtFreq;
                                            SpectraMeasurement largeSpectra = largeDataAtFreq;

                                            EventPair eventPair = new EventPair();
                                            eventPair.setX(curEvent);
                                            eventPair.setY(largeDataAtFreq.getWaveform().getEvent());

                                            SpectraRatioPairDetails userRatio = null;
                                            if (userEditedRatios != null && userEditedRatios.containsKey(eventPair) && userEditedRatios.get(eventPair).containsKey(curStation)) {
                                                userRatio = userEditedRatios.get(eventPair).get(curStation).get(freqBand);
                                            }

                                            if (userRatio == null) {
                                                Result<SpectraRatioPairDetails> ratioDetails = calcRatioFunc.apply(smallSpectra, largeSpectra);
                                                if (ratioDetails.isSuccess()) {
                                                    ratioDataList.add(ratioDetails.getResultPayload().get());

                                                    if (!ratioData.containsKey(eventPair)) {
                                                        ratioData.put(eventPair, new HashMap<>());
                                                    }

                                                    Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> ratiosForPair = ratioData.get(eventPair);
                                                    if (!ratiosForPair.containsKey(curStation)) {
                                                        ratiosForPair.put(curStation, new HashMap<>());
                                                    }
                                                    ratiosForPair.get(curStation).put(freqBand, ratioDetails.getResultPayload().get());
                                                    ratioData.put(eventPair, ratiosForPair);
                                                } else {
                                                    log.info("Unable to ratio spectra {}", ratioDetails.getErrors());
                                                }
                                            } else {
                                                Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> ratiosForPair = ratioData.get(eventPair);
                                                if (!ratiosForPair.containsKey(curStation)) {
                                                    ratiosForPair.put(curStation, new HashMap<>());
                                                }
                                                ratiosForPair.get(curStation).put(freqBand, userRatio);
                                                ratioData.put(eventPair, ratiosForPair);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (ratioDataList.isEmpty()) {
                return new SpectraRatiosReportByEventPair().getReport();
            }

            Map<EventPair, SpectraRatioPairInversionResult> inversionEstimates = invertEventRatioPairs(ratioData);
            Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionEstimates = invertEventRatios(ratioData);

            if (persistResults.booleanValue()) {
                if (userEdited != null && !userEdited.isEmpty()) {
                    spectraRatioPairDetailsRepository.deleteAllNotInIdsList(userEdited.stream().map(SpectraRatioPairDetails::getId).collect(Collectors.toList()));
                } else {
                    spectraRatioPairDetailsRepository.deleteAll();
                }
                ratioDataList = spectraRatioPairDetailsRepository.saveAll(ratioDataList);
                spectraRatioPairInversionSampleRepository.deleteAll();
                spectraRatioJontInversionSampleRepository.deleteAll();
                spectraRatioPairInversionSampleRepository.saveAll(inversionEstimates.values());
                spectraRatioJontInversionSampleRepository.saveAll(jointInversionEstimates.values());
            }
            SpectraRatiosReport finalReport = new SpectraRatiosReportByEventPair().setRatiosReportByEventPair(ratioData)
                                                                                  .setInversionResults(inversionEstimates)
                                                                                  .setJointInversionResults(jointInversionEstimates)
                                                                                  .getReport();
            if (ratioDataList.size() > 0 && ratioDataList.get(0).isLoadedFromJson()) {
                finalReport.setLoadedFromJson(true);
            }

            return finalReport;
        };

        return getRatioCalcFuture(id, ratioCalcFunc);
    }

    private Map<EventPair, SpectraRatioPairInversionResult> invertEventRatioPairs(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData) {
        final MdacParametersFI mdacFiEntry = new MdacParametersFI(mdacFiService.findFirst());
        final MdacParametersPS psRows = mdacPsService.findMatchingPhase(PICK_TYPES.LG.getPhase());
        SpectraRatioInversionCalculator inversion = new SpectraRatioInversionCalculator(mdacService, mdacFiEntry, psRows, fitMwService, refMwService, momentErrorRange);
        Map<EventPair, SpectraRatioPairInversionResult> inversionResults = inversion.cmaesRegressionPerPair(ratioData);
        return inversionResults;
    }

    private Map<EventPair, SpectraRatioPairInversionResultJoint> invertEventRatios(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData) {
        final MdacParametersFI mdacFiEntry = new MdacParametersFI(mdacFiService.findFirst());
        final MdacParametersPS psRows = mdacPsService.findMatchingPhase(PICK_TYPES.LG.getPhase());
        SpectraRatioInversionCalculator inversion = new SpectraRatioInversionCalculator(mdacService, mdacFiEntry, psRows, fitMwService, refMwService, momentErrorRange);
        Map<EventPair, SpectraRatioPairInversionResultJoint> inversionResults = inversion.cmaesRegressionJoint(ratioData);
        return inversionResults;
    }

    protected double centerFreq(final Double lowFrequency, final Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    private List<SpectraMeasurement> getSpectraListFromRatioEventData(Set<String> eventIds, List<RatioEventData> ratioEventData) {
        List<SpectraMeasurement> spectraMeasurements = new ArrayList<>();

        List<RatioEventData> eventDataToUse = new ArrayList<>();

        eventIds.forEach(eventId -> {
            Optional<RatioEventData> ratioDataOption = ratioEventData.stream().filter(ratioEvent -> ratioEvent.getEventId().equals(eventId)).findFirst();
            if (ratioDataOption.isPresent()) {
                eventDataToUse.add(ratioDataOption.get());
            }
        });

        eventDataToUse.forEach(ratioData -> {
            Event event = new Event();
            event.setEventId(ratioData.getEventId());
            event.setOriginTime(ratioData.getDate());

            ratioData.getStationData().forEach(stationData -> {
                Station station = new Station();
                station.setStationName(stationData.getStationName());
                for (int idx = 0; idx < stationData.getFrequencyData().size(); idx++) {
                    double freq = stationData.getFrequencyData().get(idx);
                    double amp = stationData.getAmplitudeData().get(idx);
                    double[] dataSegment = { amp };

                    Waveform wave = new Waveform();
                    Stream waveformStream = new Stream();
                    waveformStream.setStation(station);
                    wave.setEvent(event);
                    wave.setLowFrequency(freq);
                    wave.setHighFrequency(freq);
                    wave.setStream(waveformStream);
                    wave.setSegment(dataSegment);
                    SpectraMeasurement spectra = new SpectraMeasurement();
                    spectra.setWaveform(wave);
                    spectraMeasurements.add(spectra);
                }
            });
        });

        return spectraMeasurements;
    }

    private List<SpectraMeasurement> getSpectraListFromWaveforms(Set<String> eventIDs, boolean autoPickingEnabled, boolean persistResults) {
        List<Waveform> stacks = filterWaveforms(waveformService.getAllActiveStacks(), eventIDs);
        if (stacks != null) {
            VelocityConfiguration velocityConfig = configService.getVelocityConfiguration();
            Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> stationFrequencyBandMap = MetadataUtils.mapSiteParamsToFrequencyBands(siteParamsService.findAll());
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = MetadataUtils.mapSharedParamsToFrequencyBands(sharedParametersService.findAll());

            List<Waveform> measStacks = stacks;
            List<PeakVelocityMeasurement> velocityMeasured = Optional.ofNullable(peakVelocityMeasurementsService.measureVelocities(measStacks, velocityConfig)).orElseGet(ArrayList::new);
            if (autoPickingEnabled) {
                velocityMeasured = picker.autoPickVelocityMeasuredWaveforms(velocityMeasured, frequencyBandParameterMap);
            }

            final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap = new HashMap<>(frequencyBandParameterMap);
            velocityMeasured = MetadataUtils.filterVelocityBySnr(snrFilterMap, velocityMeasured);

            measStacks = velocityMeasured.stream().map(PeakVelocityMeasurement::getWaveform).filter(Objects::nonNull).collect(Collectors.toList());
            measStacks = MetadataUtils.filterToEndPicked(measStacks);

            List<SyntheticCoda> synthetics = syntheticGenerationService.generateSynthetics(measStacks, frequencyBandParameterMap);

            if (autoPickingEnabled) {
                ShapeFitterConstraints constraints = configService.getCalibrationShapeFitterConstraints();
                velocityMeasured = shapeCalibrationService.adjustEndPicksBasedOnSynthetics(velocityMeasured, synthetics, constraints);

                if (persistResults) {
                    List<Waveform> picks = velocityMeasured.stream().map(PeakVelocityMeasurement::getWaveform).filter(Objects::nonNull).collect(Collectors.toList());
                    waveformService.save(picks);
                }

                measStacks = velocityMeasured.stream().map(PeakVelocityMeasurement::getWaveform).collect(Collectors.toList());
                measStacks = MetadataUtils.filterToEndPicked(measStacks);

                synthetics = syntheticGenerationService.generateSynthetics(measStacks, frequencyBandParameterMap);
            }

            return spectraCalc.measureAmplitudes(synthetics, frequencyBandParameterMap, velocityConfig, stationFrequencyBandMap);
        } else {
            log.info("Unable to measure Spectra Ratios, no waveforms were found.");
        }
        return new ArrayList<>();
    }

    public Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> getSpectraMeasurementsMap(Set<String> eventIDs, Function<Set<String>, List<SpectraMeasurement>> spectraListFunc) {

        Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> spectraDataMap = new HashMap<>();
        final Long id = atomicLong.getAndIncrement();

        List<SpectraMeasurement> spectra = spectraListFunc.apply(eventIDs);

        if (spectra.isEmpty()) {
            log.info("Unable to measure Spectra Ratios, no waveforms were provided.");
            notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.COMPLETE));
            return spectraDataMap;
        }

        log.trace("Getting spectra measurement for {}", eventIDs.toArray().toString());
        spectraDataMap = mapToEventAndStation(spectraByStation(spectra));
        log.trace("Spectra Data Map created...");

        return spectraDataMap;
    }

    private Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> mapRatioDataToEvents(List<SpectraRatioPairDetails> ratios) {
        Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioEventMap = new HashMap<>();
        if (ratios != null) {
            ratios.forEach(ratio -> {
                Waveform largeEvent = ratio.getNumerWaveform();
                Waveform smallEvent = ratio.getDenomWaveform();
                Station station = ratio.getNumerWaveform().getStream().getStation();

                FrequencyBand band = new FrequencyBand(ratio.getNumerWaveform().getLowFrequency(), ratio.getNumerWaveform().getHighFrequency());

                EventPair eventPair = new EventPair();
                eventPair.setX(smallEvent.getEvent());
                eventPair.setY(largeEvent.getEvent());
                ratioEventMap.computeIfAbsent(eventPair, v -> new HashMap<Station, Map<FrequencyBand, SpectraRatioPairDetails>>()).computeIfAbsent(station, v -> new HashMap<>()).put(band, ratio);
            });
        }
        return ratioEventMap;
    }

    private Result<SpectraRatioPairDetails> setUncutSegments(final SpectraMeasurement numeratorSpectra, final SpectraMeasurement denominatorSpectra) {
        if (denominatorSpectra == null || numeratorSpectra == null) {
            return new Result<>(false, null);
        }

        Waveform numerWaveform = numeratorSpectra.getWaveform();
        Waveform denomWaveform = denominatorSpectra.getWaveform();

        if (denomWaveform == null || numerWaveform == null) {
            return new Result<>(false, null);
        }

        SpectraRatioPairDetails ratioDetails = new SpectraRatioPairDetails(numerWaveform, denomWaveform);
        SpectraRatioPairOperator ratio = new SpectraRatioPairOperator(ratioDetails);
        TimeT numerOriginTime = new TimeT(ratio.getNumeratorEventOriginTime());
        TimeT denomOriginTime = new TimeT(ratio.getDenominatorEventOriginTime());
        ratio.setPeakAndFMarkerCutTimes(numeratorSpectra.getStartCutSec(), denominatorSpectra.getStartCutSec(), numeratorSpectra.getEndCutSec(), denominatorSpectra.getEndCutSec());

        // If f-marker comes before peak, f-marker may not be good. Return a false ratio
        if (ratioDetails.getNumerEndCutSec() < ratioDetails.getNumerStartCutSec() || ratioDetails.getDenomEndCutSec() < ratioDetails.getDenomStartCutSec()) {
            return new Result<>(false, ratioDetails);
        }

        WaveformToTimeSeriesConverter converter = new WaveformToTimeSeriesConverter();
        TimeSeries denomSegment = converter.convert(denomWaveform);
        TimeSeries numerSegment = converter.convert(numerWaveform);

        // Interpolate the sample rates to match the  higher one
        double maxRate = Math.max(numerWaveform.getSampleRate(), denomWaveform.getSampleRate());
        if (denomWaveform.getSampleRate() != maxRate) {
            denomSegment.interpolate(maxRate);
        } else if (numerWaveform.getSampleRate() != maxRate) {
            numerSegment.interpolate(maxRate);
        }

        // Get index of start and end cuts using peak value and f-marker
        int numerStartIdx = numerSegment.getIndexForTime(new TimeT(ratioDetails.getNumerStartCutSec()).add(numerOriginTime).getEpochTime());
        int denomStartIdx = denomSegment.getIndexForTime(new TimeT(ratioDetails.getDenomStartCutSec()).add(denomOriginTime).getEpochTime());
        int numerEndIdx = numerSegment.getIndexForTime(new TimeT(ratioDetails.getNumerEndCutSec()).add(numerOriginTime).getEpochTime());
        int denomEndIdx = denomSegment.getIndexForTime(new TimeT(ratioDetails.getDenomEndCutSec()).add(denomOriginTime).getEpochTime());

        //Calculate the ratio based on the new segments
        ratio.updateCutTimesAndRecalculateDiff(numerStartIdx, denomStartIdx, numerEndIdx, denomEndIdx);
        return new Result<>(true, ratioDetails);
    }

    private Result<SpectraRatioPairDetails> calcFreqRatio(SpectraMeasurement smallSpectra, SpectraMeasurement largeSpectra) {

        Result<SpectraRatioPairDetails> ratioDetailsResult = setUncutSegments(largeSpectra, smallSpectra);

        if (ratioDetailsResult.isSuccess() && ratioDetailsResult.getResultPayload().isPresent()) {
            SpectraRatioPairDetails ratioDetails = ratioDetailsResult.getResultPayload().get();
            return new Result<>(true, ratioDetails);
        }

        return new Result<>(false, null);
    }

    private Result<SpectraRatioPairDetails> calcFreqRatioEmptyWaveform(SpectraMeasurement smallSpectra, SpectraMeasurement largeSpectra) {

        if (smallSpectra == null || largeSpectra == null) {
            return new Result<>(false, null);
        }

        SpectraRatioPairDetails ratioDetails = new SpectraRatioPairDetails();
        ratioDetails.setLoadedFromJson(true);
        double largeAmp = largeSpectra.getWaveform().getData().getFirst();
        double smallAmp = smallSpectra.getWaveform().getData().getFirst();
        ratioDetails.setNumerWaveform(largeSpectra.getWaveform());
        ratioDetails.setDenomWaveform(smallSpectra.getWaveform());
        ratioDetails.setNumerAvg(largeAmp);
        ratioDetails.setDenomAvg(smallAmp);
        ratioDetails.setDiffAvg(largeAmp - smallAmp);
        return new Result<>(true, ratioDetails);
    }

    private Map<Station, List<SpectraMeasurement>> spectraByStation(List<SpectraMeasurement> spectra) {
        return spectra.stream().filter(Objects::nonNull).filter(s -> s.getWaveform() != null).collect(Collectors.groupingBy(s -> s.getWaveform().getStream().getStation()));
    }

    private List<Waveform> filterWaveforms(List<Waveform> waveforms, Set<String> eventIDs) {
        Predicate<Waveform> getEventWaveformsPredicate = w -> eventIDs.stream().anyMatch(e -> w.getEvent().getEventId().equalsIgnoreCase(e));

        List<Waveform> filteredWaveforms = waveforms.stream().filter(getEventWaveformsPredicate).collect(Collectors.toList());

        log.trace("Filtered Events Waveforms count: {}", filteredWaveforms.size());
        return filteredWaveforms;
    }

    private Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> mapToEventAndStation(Map<Station, List<SpectraMeasurement>> dataByStation) {
        Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> data = new HashMap<>();
        if (dataByStation != null) {

            for (Entry<Station, List<SpectraMeasurement>> stationMeasurements : dataByStation.entrySet()) {
                if (stationMeasurements.getValue() != null && !stationMeasurements.getValue().isEmpty()) {
                    for (SpectraMeasurement spectraMeas : stationMeasurements.getValue()) {
                        Event event = spectraMeas.getWaveform().getEvent();
                        Station station = spectraMeas.getWaveform().getStream().getStation();
                        if (!data.containsKey(event)) {
                            data.put(event, new HashMap<>());
                        }
                        Map<Station, Map<FrequencyBand, SpectraMeasurement>> stationFrequencyMap = data.get(event);
                        if (!stationFrequencyMap.containsKey(station)) {
                            stationFrequencyMap.put(station, new HashMap<>());
                        }
                        Map<FrequencyBand, SpectraMeasurement> frequencyMap = stationFrequencyMap.get(station);

                        FrequencyBand frequencyBand = new FrequencyBand(spectraMeas.getWaveform().getLowFrequency(), spectraMeas.getWaveform().getHighFrequency());
                        frequencyMap.put(frequencyBand, spectraMeas);
                    }
                }
            }
        }
        return data;

    }

    private Future<Result<SpectraRatiosReport>> getRatioCalcFuture(final Long id, Supplier<SpectraRatiosReport> ratioCalcFunc) {
        Future<Result<SpectraRatiosReport>> future;
        try {
            notificationService.post(new RatioStatusEvent(id, Status.STARTING));
            future = measureService.submit(() -> {
                try {
                    SpectraRatiosReport ratioByStationReport = ratioCalcFunc.get();
                    if (ratioByStationReport.getData().isEmpty()) {
                        log.info("There was no data from the report.");
                        return new Result<>(false, null);
                    }
                    return new Result<>(true, ratioByStationReport);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    notificationService.post(new RatioStatusEvent(id, RatioStatusEvent.Status.ERROR));
                    throw ex;
                }
            });
            notificationService.post(new RatioStatusEvent(id, RatioStatusEvent.Status.PROCESSING));
        } catch (RejectedExecutionException e) {
            notificationService.post(new RatioStatusEvent(id, RatioStatusEvent.Status.ERROR));
            future = CompletableFuture.completedFuture(new Result<>(false, Collections.singletonList(e), new SpectraRatiosReport()));
        }
        return future;
    }

    @Override
    public List<SpectraRatioPairDetailsMetadata> findAllMetadataOnly() {
        return spectraRatioPairDetailsRepository.findAllMetdataOnly();
    }

    @Override
    public List<String> loadRatioMetadata(List<SpectraRatioPairDetailsMetadata> ratios) {
        List<String> errors = new ArrayList<>();
        for (SpectraRatioPairDetailsMetadata ratio : ratios) {
            //Relies on one ratio per distinct event pair/station/freq tuple
            //Look for wave/freq tuple
            WaveformMetadata numerWaveMeta = ratio.getNumerWaveform();
            WaveformMetadata denomWaveMeta = ratio.getDenomWaveform();
            Waveform numerWaveform = null;
            Waveform denomWaveform = null;

            Waveform matches = waveformService.getByMatchingKeys(
                    numerWaveMeta.getStream().getStation(),
                        numerWaveMeta.getEvent().getEventId(),
                        numerWaveMeta.getLowFrequency(),
                        numerWaveMeta.getHighFrequency());
            if (matches != null) {
                numerWaveform = matches;
            }

            matches = waveformService.getByMatchingKeys(
                    denomWaveMeta.getStream().getStation(),
                        denomWaveMeta.getEvent().getEventId(),
                        denomWaveMeta.getLowFrequency(),
                        denomWaveMeta.getHighFrequency());
            if (matches != null) {
                denomWaveform = matches;
            }

            if (numerWaveform != null && denomWaveform != null) {
                //Check if we can attach to an existing SpectraRatioPairDetails
                SpectraRatioPairDetails spectraRatio = spectraRatioPairDetailsRepository.findByWaveformIds(numerWaveform.getId(), denomWaveform.getId());
                if (spectraRatio == null) {
                    spectraRatio = new SpectraRatioPairDetails(numerWaveform, denomWaveform);
                }
                spectraRatio.mergeNonNullFields(ratio);
                SpectraRatioPairOperator op = new SpectraRatioPairOperator(spectraRatio);
                op.updateDiffSegment();
                spectraRatioPairDetailsRepository.save(spectraRatio);
            } else {
                errors.add(
                        "Unable to find matching waveforms for ratio: \n"
                                + "Event Id A "
                                + denomWaveMeta.getEvent().getEventId()
                                + "\n"
                                + "Event Id B "
                                + numerWaveMeta.getEvent().getEventId()
                                + "\n");
            }
        }
        return errors;
    }
}
