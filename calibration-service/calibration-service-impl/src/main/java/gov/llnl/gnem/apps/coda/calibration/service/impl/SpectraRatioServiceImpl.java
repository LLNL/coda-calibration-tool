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
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import gov.llnl.gnem.apps.coda.common.service.util.MetadataUtils;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
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

    @Value(value = "${ratio-inversion.moment-error-range:0.1}")
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

    @Override
    public SpectraRatioPairDetails findOne(Long id) {
        return spectraRatioPairDetailsRepository.findOneDetached(id);
    }

    @Override
    public SpectraRatioPairDetails findOneForUpdate(Long id) {
        return spectraRatioPairDetailsRepository.findOneDetached(id);
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
    public Future<Result<SpectraRatiosReport>> makeSpectraRatioMeasurements(boolean autoPickingEnabled, boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds) {
        log.debug("Starting spectra ratio calculation at {}", LocalDateTime.now());
        final Long id = atomicLong.getAndIncrement();

        Supplier<SpectraRatiosReport> ratioCalcFunc = () -> {
            Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> spectraSmallEventData = getSpectraMeasurementForEvent(smallEventIds, autoPickingEnabled, persistResults);
            Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> spectraLargeEventData = getSpectraMeasurementForEvent(largeEventIds, autoPickingEnabled, persistResults);

            Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData = new HashMap<>();

            List<SpectraRatioPairDetails> ratioDataList = new ArrayList<>();

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

                                            Result<SpectraRatioPairDetails> ratioDetails = calcFreqRatio(smallSpectra, largeSpectra);
                                            if (ratioDetails.isSuccess()) {
                                                ratioDataList.add(ratioDetails.getResultPayload().get());

                                                EventPair eventPair = new EventPair();
                                                eventPair.setX(curEvent);
                                                eventPair.setY(largeDataAtFreq.getWaveform().getEvent());
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Map<EventPair, SpectraRatioPairInversionResult> inversionEstimates = invertEventRatioPairs(ratioData);
            Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionEstimates = invertEventRatios(ratioData);

            if (persistResults) {
                spectraRatioPairDetailsRepository.saveAll(ratioDataList);
                spectraRatioPairInversionSampleRepository.deleteAll();
                spectraRatioJontInversionSampleRepository.deleteAll();
                spectraRatioPairInversionSampleRepository.saveAll(inversionEstimates.values());
                spectraRatioJontInversionSampleRepository.saveAll(jointInversionEstimates.values());
            }
            return new SpectraRatiosReportByEventPair().setRatiosReportByEventPair(ratioData).setInversionResults(inversionEstimates).setJointInversionResults(jointInversionEstimates).getReport();
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

    private Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> getSpectraMeasurementForEvent(Set<String> eventIDs, boolean autoPickingEnabled, boolean persistResults) {
        log.trace("Getting spectra measurement for {}", eventIDs.toArray().toString());
        final Long id = atomicLong.getAndIncrement();

        List<Waveform> stacks = filterWaveforms(waveformService.getAllActiveStacks(), eventIDs);
        Map<Event, Map<Station, Map<FrequencyBand, SpectraMeasurement>>> spectraDataMap = new HashMap<>();
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

            List<SpectraMeasurement> spectra = spectraCalc.measureAmplitudes(synthetics, frequencyBandParameterMap, velocityConfig, stationFrequencyBandMap);

            spectraDataMap = mapToEventAndStation(spectraByStation(spectra));
            log.trace("Spectra Data Map created...");

            return spectraDataMap;
        } else {
            log.info("Unable to measure Spectra Ratios, no waveforms were provided.");
        }
        notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.COMPLETE));
        log.info("Spectra Ratio Measurement complete at {}", LocalDateTime.now());
        return spectraDataMap;
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
                        if (WaveformUtils.isValidWaveform(spectraMeas.getWaveform())) {
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
                    Result<SpectraRatiosReport> finalReport = new Result<>(true, ratioByStationReport);
                    return finalReport;
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
}
