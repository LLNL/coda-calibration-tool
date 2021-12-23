/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwReportByEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurementMetadata;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurementMetadataImpl;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.CalibrationStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.MeasurementStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.service.api.AutopickingService;
import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.DatabaseCleaningService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PeakVelocityMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticCodaGenerationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ValidationMwParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.EnergyInfo;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;

@Service
@Transactional
//FIXME: Kind of becoming a god-class, maybe see if we can break this up.
public class CalibrationServiceImpl implements CalibrationService {

    private static final Logger log = LoggerFactory.getLogger(CalibrationServiceImpl.class);

    private WaveformService waveformService;
    private PeakVelocityMeasurementService peakVelocityMeasurementsService;
    private SharedFrequencyBandParametersService sharedParametersService;
    private SiteFrequencyBandParametersService siteParamsService;
    private ShapeCalibrationService shapeCalibrationService;
    private SpectraMeasurementService spectraMeasurementService;
    private SpectraCalculator spectraCalc;
    private SyntheticCodaGenerationService syntheticGenerationService;
    private PathCalibrationService pathCalibrationService;
    private MdacParametersFiService mdacFiService;
    private MdacParametersPsService mdacPsService;
    private ReferenceMwParametersService referenceMwService;
    private ValidationMwParametersService validationMwService;
    private SiteCalibrationService siteCalibrationService;
    private SyntheticService syntheticService;
    private NotificationService notificationService;
    private DatabaseCleaningService cleaningService;
    private ConfigurationService configService;
    private AutopickingService picker;

    private static final AtomicLong atomicLong = new AtomicLong(0l);

    private static final ExecutorService calService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), r -> {
        Thread thread = new Thread(r);
        thread.setName("Calibration");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService measureService;

    private Map<Long, Future<?>> runningJobs = new ConcurrentHashMap<>(2);

    @Autowired
    public CalibrationServiceImpl(WaveformService waveformService, PeakVelocityMeasurementService peakVelocityMeasurementsService, SharedFrequencyBandParametersService sharedParametersService,
            ShapeCalibrationService shapeCalibrationService, SpectraMeasurementService spectraMeasurementService, SyntheticCodaGenerationService syntheticGenerationService,
            PathCalibrationService pathCalibrationService, MdacParametersFiService mdacFiService, MdacParametersPsService mdacPsService, ReferenceMwParametersService referenceMwService,
            ValidationMwParametersService validationMwService, SiteCalibrationService siteCalibrationService, SyntheticService syntheticService, NotificationService notificationService,
            DatabaseCleaningService cleaningService, ConfigurationService configService, SiteFrequencyBandParametersService siteParamsService, SpectraCalculator spectraCalc, AutopickingService picker,
            @Qualifier("MeasurementExecutorService") ExecutorService measureService) {
        this.waveformService = waveformService;
        this.peakVelocityMeasurementsService = peakVelocityMeasurementsService;
        this.sharedParametersService = sharedParametersService;
        this.shapeCalibrationService = shapeCalibrationService;
        this.spectraMeasurementService = spectraMeasurementService;
        this.syntheticGenerationService = syntheticGenerationService;
        this.pathCalibrationService = pathCalibrationService;
        this.mdacFiService = mdacFiService;
        this.mdacPsService = mdacPsService;
        this.referenceMwService = referenceMwService;
        this.validationMwService = validationMwService;
        this.siteCalibrationService = siteCalibrationService;
        this.syntheticService = syntheticService;
        this.notificationService = notificationService;
        this.cleaningService = cleaningService;
        this.configService = configService;
        this.siteParamsService = siteParamsService;
        this.spectraCalc = spectraCalc;
        this.picker = picker;
        this.measureService = measureService;
    }

    @Override
    public Future<Result<MeasuredMwReportByEvent>> makeMwMeasurements(boolean autoPickingEnabled, boolean persistResults) {
        final Long id = atomicLong.getAndIncrement();
        Future<Result<MeasuredMwReportByEvent>> future = CompletableFuture.completedFuture(new Result<>(false, new MeasuredMwReportByEvent()));
        Supplier<MeasuredMwReportByEvent> measurementFunc = () -> {
            List<String> stationNames = siteParamsService.findDistinctStationNames();
            List<Waveform> stacks = waveformService.getAllActiveStacksInStationNames(stationNames);
            return makeMwMeasurements(id, autoPickingEnabled, persistResults, stacks);
        };

        future = getMeasurementFuture(id, measurementFunc);
        return future;
    }

    @Override
    public Future<Result<MeasuredMwReportByEvent>> makeMwMeasurements(boolean autoPickingEnabled, boolean persistResults, Set<String> eventIds) {
        final Long id = atomicLong.getAndIncrement();
        Supplier<MeasuredMwReportByEvent> measurementFunc = () -> {
            MeasuredMwReportByEvent measuredMws = new MeasuredMwReportByEvent();
            List<String> stationNames = siteParamsService.findDistinctStationNames();
            List<Waveform> stacks = eventIds.stream().flatMap(eventId -> waveformService.findAllActiveStacksByEventIdAndStationNames(eventId, stationNames).stream()).collect(Collectors.toList());
            if (stacks != null && !stacks.isEmpty()) {
                measuredMws = makeMwMeasurements(id, autoPickingEnabled, persistResults, stacks);
            } else {
                notificationService.post(
                        new MeasurementStatusEvent(id,
                                                   MeasurementStatusEvent.Status.ERROR,
                                                   new Result<Exception>(false,
                                                                         new LightweightIllegalStateException("No matching waveforms found for event ids: " + eventIds != null
                                                                                 ? eventIds.toString()
                                                                                 : "{null}"))));
            }

            return measuredMws;
        };
        return getMeasurementFuture(id, measurementFunc);
    }

    @Override
    public Future<Result<MeasuredMwReportByEvent>> makeMwMeasurements(boolean autoPickingEnabled, boolean persistResults, List<Waveform> waveforms) {
        final Long id = atomicLong.getAndIncrement();
        Supplier<MeasuredMwReportByEvent> measurementFunc = () -> {
            MeasuredMwReportByEvent measuredMws = new MeasuredMwReportByEvent();
            List<String> stationNames = siteParamsService.findDistinctStationNames();
            if (waveforms != null) {
                List<Waveform> stacks = waveforms.stream()
                                                 .filter(WaveformUtils::isValidWaveform)
                                                 .filter(w -> stationNames.contains(w.getStream().getStation().getStationName()))
                                                 .collect(Collectors.toList());

                if (stacks != null && !stacks.isEmpty()) {
                    measuredMws = makeMwMeasurements(id, autoPickingEnabled, persistResults, stacks);
                } else {
                    String msg = "No valid waveforms provided. Waveforms must have event and station information and must match a station for which a site calibration exists.";
                    log.trace(msg);
                    notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.ERROR, new Result<Exception>(false, new LightweightIllegalStateException(msg))));
                }
            }
            return measuredMws;
        };
        return getMeasurementFuture(id, measurementFunc);
    }

    private Future<Result<MeasuredMwReportByEvent>> getMeasurementFuture(final Long id, Supplier<MeasuredMwReportByEvent> measurementFunc) {
        Future<Result<MeasuredMwReportByEvent>> future;
        try {
            future = measureService.submit(() -> {
                try {
                    notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.STARTING));
                    MeasuredMwReportByEvent measurements = measurementFunc.get();
                    return new Result<>(true, measurements);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.ERROR, new Result<>(false, ex)));
                    throw ex;
                }
            });
        } catch (RejectedExecutionException e) {
            notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.ERROR, new Result<Exception>(false, e)));
            future = CompletableFuture.completedFuture(new Result<>(false, Collections.singletonList(e), new MeasuredMwReportByEvent()));
        }
        return future;
    }

    private MeasuredMwReportByEvent makeMwMeasurements(Long id, boolean autoPickingEnabled, boolean persistResults, List<Waveform> stacks) {
        log.info("Starting measurement at {}", LocalDateTime.now());
        MeasuredMwReportByEvent details = new MeasuredMwReportByEvent();
        if (stacks != null) {
            List<Event> eventsInStacks = stacks.stream().map(Waveform::getEvent).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            VelocityConfiguration velocityConfig = configService.getVelocityConfiguration();
            Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> stationFrequencyBandMap = mapParamsToFrequencyBands(siteParamsService.findAll());
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.findAll());

            List<Waveform> measStacks = stacks;
            List<PeakVelocityMeasurement> velocityMeasured = Optional.ofNullable(peakVelocityMeasurementsService.measureVelocities(measStacks, velocityConfig))
                                                                     .orElseGet(Stream::empty)
                                                                     .collect(Collectors.toList());
            if (autoPickingEnabled) {
                velocityMeasured = picker.autoPickVelocityMeasuredWaveforms(velocityMeasured, frequencyBandParameterMap);
            }

            final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap = new HashMap<>(frequencyBandParameterMap);
            velocityMeasured = filterVelocityBySnr(snrFilterMap, velocityMeasured.stream());

            measStacks = velocityMeasured.stream().map(PeakVelocityMeasurement::getWaveform).filter(Objects::nonNull).collect(Collectors.toList());
            measStacks = filterToEndPicked(measStacks);

            List<SyntheticCoda> synthetics = syntheticGenerationService.generateSynthetics(measStacks, frequencyBandParameterMap);

            if (autoPickingEnabled) {
                ShapeFitterConstraints constraints = configService.getCalibrationShapeFitterConstraints();
                velocityMeasured = shapeCalibrationService.adjustEndPicksBasedOnSynthetics(velocityMeasured, synthetics, constraints);
                try {
                    shapeCalibrationService.measureShapes(velocityMeasured, frequencyBandParameterMap, constraints, autoPickingEnabled, false);
                } catch (InterruptedException e) {
                    return details;
                }
                if (persistResults) {
                    List<Waveform> picks = velocityMeasured.stream().map(PeakVelocityMeasurement::getWaveform).filter(Objects::nonNull).collect(Collectors.toList());
                    waveformService.save(picks);
                }

                measStacks = velocityMeasured.stream().map(PeakVelocityMeasurement::getWaveform).collect(Collectors.toList());
                measStacks = filterToEndPicked(measStacks);

                synthetics = syntheticGenerationService.generateSynthetics(measStacks, frequencyBandParameterMap);
            }

            List<SpectraMeasurement> spectra = spectraCalc.measureAmplitudes(synthetics, frequencyBandParameterMap, velocityConfig, stationFrequencyBandMap);

            List<MeasuredMwParameters> measuredMwsParams = siteCalibrationService.fitMws(
                    spectraByFrequencyBand(spectra),
                        mdacFiService.findFirst(),
                        collectByFrequencyBand(mdacPsService.findAll()),
                        collectByEvid(referenceMwService.findAll()),
                        stationFrequencyBandMap,
                        PICK_TYPES.LG);

            Map<Event, MeasuredMwParameters> measuredMwsMap = Optional.ofNullable(measuredMwsParams).orElseGet(ArrayList::new).stream().map(mwp -> {
                Event event = getEventForId(mwp.getEventId(), eventsInStacks);
                if (event != null) {
                    return new AbstractMap.SimpleEntry<>(event, mwp);
                } else {
                    return null;
                }
            }).collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue));

            final Map<FrequencyBand, SharedFrequencyBandParameters> freqParamMap = frequencyBandParameterMap;
            Map<String, List<Spectra>> fitSpectra = measuredMwsMap.entrySet()
                                                                  .parallelStream()
                                                                  .map(
                                                                          mw -> new AbstractMap.SimpleEntry<>(mw.getKey().getEventId(),
                                                                                                              computeFitSpectra(mw.getValue(), freqParamMap.keySet(), PICK_TYPES.LG)))
                                                                  .collect(Collectors.toConcurrentMap(SimpleEntry::getKey, SimpleEntry::getValue));

            if (persistResults) {
                peakVelocityMeasurementsService.deleteAll();
                syntheticService.deleteAll();
                peakVelocityMeasurementsService.save(velocityMeasured);
                syntheticService.save(synthetics);
            }

            details.setFitSpectra(fitSpectra);

            details.setMeasuredMwDetails(
                    measuredMwsMap.entrySet()
                                  .parallelStream()
                                  .collect(Collectors.toConcurrentMap(kv -> kv.getKey().getEventId(), kv -> new MeasuredMwDetails(kv.getValue(), null, null, kv.getKey()))));

            details.setSpectraMeasurements(
                    spectra.parallelStream()
                           .map(s -> new AbstractMap.SimpleEntry<String, SpectraMeasurementMetadata>(s.getWaveform().getEvent().getEventId(), new SpectraMeasurementMetadataImpl(s)))
                           .collect(Collectors.groupingByConcurrent(SimpleEntry::getKey, Collectors.mapping(SimpleEntry::getValue, Collectors.toList()))));
        } else {
            log.info("Unable to measure Mws, no waveforms were provided.");
        }
        notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.COMPLETE));
        log.info("Measurement complete at {}", LocalDateTime.now());
        return details;
    }

    private List<Spectra> computeFitSpectra(MeasuredMwParameters event, Set<FrequencyBand> frequencyBands, PICK_TYPES selectedPhase) {
        List<Spectra> spectra = new ArrayList<>();
        if (event != null) {
            EnergyInfo eInfo = new EnergyInfo(event.getObsEnergy(), event.getLogTotalEnergy(), event.getLogTotalEnergyMDAC(), event.getEnergyRatio(), event.getObsAppStress());
            spectra.add(spectraCalc.computeFitSpectra(event, frequencyBands, selectedPhase));
            spectra.add(spectraCalc.computeSpecificSpectra(event.getMw1Max(), event.getApparentStress1Max(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ1));
            spectra.add(spectraCalc.computeSpecificSpectra(event.getMw1Min(), event.getApparentStress1Min(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ1));
            spectra.add(spectraCalc.computeSpecificSpectra(event.getMw2Max(), event.getApparentStress2Max(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ2));
            spectra.add(spectraCalc.computeSpecificSpectra(event.getMw2Min(), event.getApparentStress2Min(), eInfo, frequencyBands, selectedPhase, SPECTRA_TYPES.UQ2));
        }
        return spectra;

    }

    private Event getEventForId(String eventId, List<Event> eventsInStacks) {
        return Optional.ofNullable(waveformService.findEventById(eventId))
                       .filter(Objects::nonNull)
                       .filter(e -> e.getEventId() != null)
                       .orElseGet(() -> eventsInStacks.stream().filter(e -> e.getEventId().equalsIgnoreCase(eventId)).findAny().orElseGet(null));
    }

    @Override
    public boolean startCalibration(boolean autoPickingEnabled) {
        // FIXME: These *All methods should be *AllByProjectID instead!
        final Long id = atomicLong.getAndIncrement();
        try {
            runningJobs.put(id, calService.submit(() -> {
                try {
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.STARTING));
                    log.info("Starting calibration at {}", LocalDateTime.now());

                    Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.findAll());
                    final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap = new HashMap<>(frequencyBandParameterMap);

                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.PEAK_STARTING));
                    List<Waveform> stacks = waveformService.getAllActiveStacks();
                    // In general each step produces output that the next step
                    // consumes

                    VelocityConfiguration velocityConfig = configService.getVelocityConfiguration();
                    ShapeFitterConstraints constraints = configService.getCalibrationShapeFitterConstraints();

                    // 1) Compute the peak velocity, amplitude, and SNR values
                    // for the given coda stacks using theoretical group velocities
                    // to cut the windows for noise and SN/LG arrival
                    Stream<PeakVelocityMeasurement> velocityMeasurements = peakVelocityMeasurementsService.measureVelocities(stacks, velocityConfig);

                    //Save the max velocity information even if we aren't auto-picking
                    stacks = waveformService.save(stacks);

                    // First step is to clean up all the intermediary results if
                    // they exist. This is as wildly not-thread-safe as you might
                    // imagine.
                    peakVelocityMeasurementsService.deleteAll();
                    syntheticService.deleteAll();

                    // We want to filter out the ones that don't pass the user's
                    // SNR threshold
                    List<PeakVelocityMeasurement> snrFilteredVelocity = filterVelocityBySnr(snrFilterMap, velocityMeasurements);

                    // Now save the new ones we just calculated
                    peakVelocityMeasurementsService.save(snrFilteredVelocity);

                    ConcurrencyUtils.checkInterrupt();
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.SHAPE_STARTING));

                    // If auto-picking is enabled attempt to pick any envelopes that
                    // don't already have F-picks in this set
                    if (autoPickingEnabled) {
                        snrFilteredVelocity = picker.autoPickVelocityMeasuredWaveforms(snrFilteredVelocity, frequencyBandParameterMap);
                        snrFilteredVelocity = snrFilteredVelocity.parallelStream().map(v -> v.setWaveform(waveformService.save(v.getWaveform()))).collect(Collectors.toList());
                        ConcurrencyUtils.checkInterrupt();
                    }

                    // 2) Compute the shape parameters describing each stack
                    // (Velocity V0-2, Beta B0-2, Gamma G0-2) and then fit
                    // models to each of those parameters for each frequency band that can be used
                    // to generate synthetic coda at any given distance and frequency band
                    // combination
                    frequencyBandParameterMap = shapeCalibrationService.measureShapes(snrFilteredVelocity, frequencyBandParameterMap, constraints);
                    frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.save(frequencyBandParameterMap.values()));

                    // 3) Now we need to generate some basic synthetics for the
                    // measurement code to use to determine where to measure the
                    // raw amplitudes. Then feed the synthetics to the measurement
                    // service and get raw at start and raw at measurement time
                    // values back
                    stacks = snrFilteredVelocity.stream().map(PeakVelocityMeasurement::getWaveform).filter(Objects::nonNull).collect(Collectors.toList());
                    stacks = filterToEndPicked(stacks);

                    List<SyntheticCoda> synthetics = syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap);
                    List<SpectraMeasurement> spectra = spectraMeasurementService.measureSpectra(synthetics, frequencyBandParameterMap, velocityConfig);

                    if (autoPickingEnabled) {
                        // 1. Re-pick based on divergence from model
                        snrFilteredVelocity = shapeCalibrationService.adjustEndPicksBasedOnSynthetics(snrFilteredVelocity, synthetics, constraints);

                        // Now that we have re-picked based on the average model divergence we want to
                        // regenerate the synthetics and re-measure
                        // 2. Re-fit shapes to new pick
                        frequencyBandParameterMap = shapeCalibrationService.measureShapes(snrFilteredVelocity, frequencyBandParameterMap, constraints, autoPickingEnabled, true);

                        // Save any updated picks
                        List<Waveform> picks = snrFilteredVelocity.stream().map(PeakVelocityMeasurement::getWaveform).collect(Collectors.toList());
                        waveformService.save(picks);

                        // 3. Now that we have re-picked based on the average model divergence we want
                        // to regenerate the synthetics based on the new shape measurements
                        spectra = spectraMeasurementService.measureSpectra(syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap), frequencyBandParameterMap, velocityConfig);
                    }
                    synthetics = null;

                    ConcurrencyUtils.checkInterrupt();
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.PATH_STARTING));

                    // 4) For each event in the data set find all stations that
                    // recorded the event, then compute what the estimated path
                    // effect correction needs to be for each frequency band
                    frequencyBandParameterMap = pathCalibrationService.measurePathCorrections(spectraByFrequencyBand(spectra), frequencyBandParameterMap, velocityConfig);

                    frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.save(frequencyBandParameterMap.values()));
                    ConcurrencyUtils.checkInterrupt();

                    // 5) Measure the amplitudes again but this time we can
                    // compute ESH path corrected values
                    spectra = spectraMeasurementService.measureSpectra(syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap), frequencyBandParameterMap, velocityConfig);

                    ConcurrencyUtils.checkInterrupt();
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.SITE_STARTING));

                    // 6) Now using those path correction values plus a list of
                    // trusted Mw/spectra measurements for some subset of events
                    // in the data set we can compute what the offset is at each station
                    // from the expected source spectra for that MW value. This value is
                    // recorded as the site specific offset for measured values at each
                    // frequency band
                    Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap = siteCalibrationService.measureSiteCorrections(
                            spectraByFrequencyBand(spectra),
                                mdacFiService.findFirst(),
                                collectByFrequencyBand(mdacPsService.findAll()),
                                collectByEvid(referenceMwService.findAll()),
                                frequencyBandParameterMap,
                                PICK_TYPES.LG);

                    ConcurrencyUtils.checkInterrupt();
                    // 7) Measure the amplitudes one last time to fill out the
                    // Path+Site corrected amplitude values
                    spectra = spectraMeasurementService.measureSpectra(
                            syntheticService.save(syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap)),
                                frequencyBandParameterMap,
                                velocityConfig,
                                frequencyBandSiteParameterMap);

                    log.info("Calibration complete at {}", LocalDateTime.now());
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.COMPLETE));
                } catch (InterruptedException interrupted) {
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.COMPLETE, new Result<>(true, interrupted)));
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.ERROR, new Result<>(false, ex)));
                    throw ex;
                } finally {
                    runningJobs.remove(id);
                }
            }));
        } catch (RejectedExecutionException e) {
            notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.ERROR, new Result<Exception>(false, e)));
            return false;
        }
        return true;
    }

    @Override
    public boolean cancelCalibration(Long id) {
        boolean cancelled = false;
        Future<?> task = runningJobs.get(id);
        if (task != null) {
            task.cancel(true);
            cancelled = true;
        }
        return cancelled;
    }

    private List<Waveform> filterToEndPicked(List<Waveform> stacks) {
        return stacks.parallelStream().filter(wave -> wave.getAssociatedPicks() != null).map(wave -> {
            Optional<WaveformPick> pick = wave.getAssociatedPicks().stream().filter(p -> p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim())).findFirst();
            if (pick.isPresent() && pick.get().getPickTimeSecFromOrigin() > 0) {
                return wave;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private List<PeakVelocityMeasurement> filterVelocityBySnr(final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap, Stream<PeakVelocityMeasurement> velocityMeasurements) {
        return velocityMeasurements.parallel().filter(vel -> {
            boolean valid = false;
            if (vel.getWaveform() != null) {
                FrequencyBand fb = new FrequencyBand(vel.getWaveform().getLowFrequency(), vel.getWaveform().getHighFrequency());
                SharedFrequencyBandParameters params = snrFilterMap.get(fb);
                valid = params != null && vel.getSnr() >= params.getMinSnr();
            }
            return valid;
        }).collect(Collectors.toList());
    }

    private Map<String, List<ReferenceMwParameters>> collectByEvid(List<ReferenceMwParameters> refMws) {
        return refMws.stream().filter(Objects::nonNull).collect(Collectors.groupingBy(ReferenceMwParameters::getEventId));
    }

    private Map<PICK_TYPES, MdacParametersPS> collectByFrequencyBand(List<MdacParametersPS> mdacPs) {
        return mdacPs.stream()
                     .filter(ps -> ps != null && PICK_TYPES.isKnownPhase(ps.getPhase().trim()))
                     .collect(Collectors.toMap(ps -> PICK_TYPES.valueOf(ps.getPhase().toUpperCase(Locale.ENGLISH).trim()), Function.identity()));
    }

    private Map<FrequencyBand, List<SpectraMeasurement>> spectraByFrequencyBand(List<SpectraMeasurement> spectra) {
        return spectra.stream()
                      .filter(Objects::nonNull)
                      .filter(s -> s.getWaveform() != null)
                      .collect(Collectors.groupingBy(s -> new FrequencyBand(s.getWaveform().getLowFrequency(), s.getWaveform().getHighFrequency())));
    }

    private Map<FrequencyBand, SharedFrequencyBandParameters> mapParamsToFrequencyBands(Collection<SharedFrequencyBandParameters> params) {
        return params.stream().collect(Collectors.toMap(fbp -> new FrequencyBand(fbp.getLowFrequency(), fbp.getHighFrequency()), fbp -> fbp));
    }

    private Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> mapParamsToFrequencyBands(List<SiteFrequencyBandParameters> params) {
        return params.stream()
                     .collect(
                             Collectors.groupingBy(
                                     site -> new FrequencyBand(site.getLowFrequency(), site.getHighFrequency()),
                                         Collectors.toMap(SiteFrequencyBandParameters::getStation, Function.identity())));
    }

    @PreDestroy
    private void stop() {
        calService.shutdownNow();
        measureService.shutdownNow();
    }

    @Override
    public boolean clearData() {
        if (cleaningService != null) {
            return cleaningService.clearAll();
        }

        return false;
    }

    @Override
    public List<String> toggleAllByEventIds(List<String> eventIds) {
        List<String> infoMesssages = new ArrayList<>(0);
        for (String evid : eventIds) {
            ReferenceMwParameters ref = referenceMwService.findByEventId(evid);
            ValidationMwParameters val = validationMwService.findByEventId(evid);

            // Has ref but no validation (move Ref to Val)
            if (ref != null && ref.getRefMw() != 0.0 && (val == null || val.getMw() == 0.0)) {
                if (val == null) {
                    val = new ValidationMwParameters();
                }
                val.setEventId(ref.getEventId());
                val.setMw(ref.getRefMw());
                val.setApparentStressInMpa(ref.getRefApparentStressInMpa());
                referenceMwService.delete(ref);
                validationMwService.save(val);
                infoMesssages.add("Evid " + evid + " converted from reference event to validation.");
            }
            // Has val but no reference (move Val to Ref)
            else if (val != null && val.getMw() != 0.0 && (ref == null || ref.getRefMw() == 0.0)) {
                if (ref == null) {
                    ref = new ReferenceMwParameters();
                }
                ref.setEventId(val.getEventId());
                ref.setRefMw(val.getMw());
                ref.setRefApparentStressInMpa(val.getApparentStressInMpa());
                validationMwService.delete(val);
                referenceMwService.save(ref);
                infoMesssages.add("Evid " + evid + " converted from validation event to reference.");
            }
            // Has both/neither (info message?)
            else {
                infoMesssages.add("Ignoring request to convert evid " + evid + ", either the event has both or both are blank. Ref " + ref + "; Val " + val);
            }
        }
        return infoMesssages;
    }
}
