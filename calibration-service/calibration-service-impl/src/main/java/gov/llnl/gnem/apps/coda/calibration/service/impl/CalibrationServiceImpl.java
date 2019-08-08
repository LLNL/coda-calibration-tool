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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.time.LocalDateTime;
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
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
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
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.LightweightIllegalStateException;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;

@Service
@Transactional
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

    @Autowired
    public CalibrationServiceImpl(WaveformService waveformService, PeakVelocityMeasurementService peakVelocityMeasurementsService, SharedFrequencyBandParametersService sharedParametersService,
            ShapeCalibrationService shapeCalibrationService, SpectraMeasurementService spectraMeasurementService, SyntheticCodaGenerationService syntheticGenerationService,
            PathCalibrationService pathCalibrationService, MdacParametersFiService mdacFiService, MdacParametersPsService mdacPsService, ReferenceMwParametersService referenceMwService,
            SiteCalibrationService siteCalibrationService, SyntheticService syntheticService, NotificationService notificationService, DatabaseCleaningService cleaningService,
            ConfigurationService configService, SiteFrequencyBandParametersService siteParamsService, SpectraCalculator spectraCalc, AutopickingService picker,
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
    public Future<Result<List<MeasuredMwDetails>>> makeMwMeasurements(Boolean autoPickingEnabled) {
        final Long id = atomicLong.getAndIncrement();
        Future<Result<List<MeasuredMwDetails>>> future = CompletableFuture.completedFuture(new Result<>(false, Collections.emptyList()));
        Supplier<List<MeasuredMwDetails>> measurementFunc = () -> {
            List<String> stationNames = siteParamsService.findDistinctStationNames();
            List<Waveform> stacks = waveformService.getAllActiveStacksInStationNames(stationNames);
            return makeMwMeasurements(id, autoPickingEnabled, stacks);
        };

        future = getMeasurementFuture(id, measurementFunc);
        return future;
    }

    @Override
    public Future<Result<List<MeasuredMwDetails>>> makeMwMeasurements(Boolean autoPickingEnabled, Set<String> eventIds) {
        final Long id = atomicLong.getAndIncrement();
        Supplier<List<MeasuredMwDetails>> measurementFunc = () -> {
            List<MeasuredMwDetails> measuredMws = new ArrayList<>(0);
            List<String> stationNames = siteParamsService.findDistinctStationNames();
            List<Waveform> stacks = eventIds.stream().flatMap(eventId -> waveformService.findAllActiveStacksByEventIdAndStationNames(eventId, stationNames).stream()).collect(Collectors.toList());
            if (stacks != null && !stacks.isEmpty()) {
                measuredMws = makeMwMeasurements(id, autoPickingEnabled, stacks);
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
    public Future<Result<List<MeasuredMwDetails>>> makeMwMeasurements(Boolean autoPickingEnabled, List<Waveform> waveforms) {
        final Long id = atomicLong.getAndIncrement();
        Supplier<List<MeasuredMwDetails>> measurementFunc = () -> {
            List<MeasuredMwDetails> measuredMws = new ArrayList<>(0);
            List<String> stationNames = siteParamsService.findDistinctStationNames();
            if (waveforms != null) {
                List<Waveform> stacks = waveforms.stream()
                                                 .filter(WaveformUtils::isValidWaveform)
                                                 .filter(w -> stationNames.contains(w.getStream().getStation().getStationName()))
                                                 .collect(Collectors.toList());

                if (stacks != null && !stacks.isEmpty()) {
                    measuredMws = makeMwMeasurements(id, autoPickingEnabled, stacks);
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

    private Future<Result<List<MeasuredMwDetails>>> getMeasurementFuture(final Long id, Supplier<List<MeasuredMwDetails>> measurementFunc) {
        Future<Result<List<MeasuredMwDetails>>> future;
        try {
            future = measureService.submit(() -> {
                try {
                    notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.STARTING));
                    List<MeasuredMwDetails> measurements = measurementFunc.get();
                    return new Result<>(true, measurements);
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.ERROR, new Result<>(false, ex)));
                    throw ex;
                }
            });
        } catch (RejectedExecutionException e) {
            notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.ERROR, new Result<Exception>(false, e)));
            future = CompletableFuture.completedFuture(new Result<>(false, Collections.singletonList(e), Collections.emptyList()));
        }
        return future;
    }

    private List<MeasuredMwDetails> makeMwMeasurements(Long id, Boolean autoPickingEnabled, List<Waveform> stacks) {
        log.info("Starting measurement at {}", LocalDateTime.now());
        List<MeasuredMwDetails> measuredMws = Collections.emptyList();

        if (stacks != null) {
            List<Event> eventsInStacks = stacks.stream().map(w -> w.getEvent()).filter(Objects::nonNull).distinct().collect(Collectors.toList());
            VelocityConfiguration velocityConfig = configService.getVelocityConfiguration();
            Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> stationFrequencyBandMap = mapParamsToFrequencyBands(siteParamsService.findAll());
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.findAll());

            List<Waveform> measStacks = stacks;
            Collection<PeakVelocityMeasurement> velocityMeasured = Optional.ofNullable(peakVelocityMeasurementsService.measureVelocities(measStacks, velocityConfig))
                                                                           .orElseGet(() -> Stream.empty())
                                                                           .collect(Collectors.toList());
            if (autoPickingEnabled) {
                velocityMeasured = picker.autoPickVelocityMeasuredWaveforms(velocityMeasured, frequencyBandParameterMap);
            }

            log.trace("velocity measurements {}", velocityMeasured);

            final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap = new HashMap<>(frequencyBandParameterMap);
            velocityMeasured = filterVelocityBySnr(snrFilterMap, velocityMeasured.stream());
            measStacks = velocityMeasured.stream().map(vel -> vel.getWaveform()).collect(Collectors.toList());
            measStacks = filterToEndPicked(measStacks);

            log.trace("filtered stacks {}", measStacks);

            List<SpectraMeasurement> spectra = spectraCalc.measureAmplitudes(
                    syntheticGenerationService.generateSynthetics(measStacks, frequencyBandParameterMap),
                        frequencyBandParameterMap,
                        velocityConfig,
                        stationFrequencyBandMap);

            log.trace("spectra {}", spectra);

            List<MeasuredMwParameters> measuredMwsParams = siteCalibrationService.fitMws(
                    spectraByFrequencyBand(spectra),
                        mdacFiService.findFirst(),
                        collectByFrequencyBand(mdacPsService.findAll()),
                        collectByEvid(referenceMwService.findAll()),
                        stationFrequencyBandMap,
                        PICK_TYPES.LG);

            log.trace("measured mw params {}", measuredMwsParams);

            measuredMws = Optional.ofNullable(measuredMwsParams)
                                  .orElseGet(ArrayList::new)
                                  .stream()
                                  .map(
                                          mwp -> new MeasuredMwDetails(mwp,
                                                                       null,
                                                                       Optional.ofNullable(waveformService.findEventById(mwp.getEventId()))
                                                                               .filter(Objects::nonNull)
                                                                               .filter(e -> e.getEventId() != null)
                                                                               .orElseGet(
                                                                                       () -> eventsInStacks.stream()
                                                                                                           .filter(e -> e.getEventId().equalsIgnoreCase(mwp.getEventId()))
                                                                                                           .findAny()
                                                                                                           .orElseGet(null))))
                                  .collect(Collectors.toList());

            log.trace("mw details {}", measuredMws);

            notificationService.post(new MeasurementStatusEvent(id, MeasurementStatusEvent.Status.COMPLETE));

            log.info("Measurement complete at {}", LocalDateTime.now());
        } else {
            log.info("Unable to measure Mws, no waveforms were provided.");
        }
        return measuredMws;
    }

    @Override
    public boolean startCalibration(Boolean autoPickingEnabled) {
        // FIXME: These *All methods should be *AllByProjectID instead!
        final Long id = atomicLong.getAndIncrement();
        try {
            calService.submit(() -> {
                try {
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.STARTING));
                    log.info("Starting calibration at {}", LocalDateTime.now());

                    // TODO: Look at removing auto picking code from the methods
                    // below and centralizing it to here instead.
                    Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.findAll());
                    final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap = new HashMap<>(frequencyBandParameterMap);

                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.PEAK_STARTING));
                    List<Waveform> stacks = waveformService.getAllActiveStacks();
                    // In general each step produces output that the next step
                    // consumes

                    VelocityConfiguration velocityConfig = configService.getVelocityConfiguration();

                    // 1) Compute the peak velocity, amplitude, and SNR values
                    // for the given coda stacks using theoretical group velocities
                    // to cut the windows for noise and SN/LG arrival
                    Stream<PeakVelocityMeasurement> velocityMeasurements = peakVelocityMeasurementsService.measureVelocities(stacks, velocityConfig);

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

                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.SHAPE_STARTING));

                    // 2) Compute the shape parameters describing each stack
                    // (Velocity V0-2, Beta B0-2, Gamma G0-2) and then fit
                    // models to each of those parameters for each frequency band that can be used
                    // to generate synthetic coda at any given distance and frequency band
                    // combination
                    frequencyBandParameterMap = shapeCalibrationService.measureShapes(snrFilteredVelocity, frequencyBandParameterMap, autoPickingEnabled);

                    frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.save(frequencyBandParameterMap.values()));

                    // 3) Now we need to generate some basic synthetics for the
                    // measurement code to use to determine where to measure the
                    // raw amplitudes. Then feed the synthetics to the measurement
                    // service and get raw at start and raw at measurement time
                    // values back
                    stacks = filterToEndPicked(stacks);

                    List<SpectraMeasurement> spectra = spectraMeasurementService.measureSpectra(
                            syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap),
                                frequencyBandParameterMap,
                                velocityConfig);

                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.PATH_STARTING));

                    // 4) For each event in the data set find all stations that
                    // recorded the event, then compute what the estimated path
                    // effect correction needs to be for each frequency band
                    frequencyBandParameterMap = pathCalibrationService.measurePathCorrections(spectraByFrequencyBand(spectra), frequencyBandParameterMap, velocityConfig);

                    frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.save(frequencyBandParameterMap.values()));

                    // 5) Measure the amplitudes again but this time we can
                    // compute ESH path corrected values
                    spectra = spectraMeasurementService.measureSpectra(syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap), frequencyBandParameterMap, velocityConfig);

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

                    // 7) Measure the amplitudes one last time to fill out the
                    // Path+Site corrected amplitude values
                    spectra = spectraMeasurementService.measureSpectra(
                            syntheticService.save(syntheticGenerationService.generateSynthetics(stacks, frequencyBandParameterMap)),
                                frequencyBandParameterMap,
                                velocityConfig,
                                frequencyBandSiteParameterMap);

                    log.info("Calibration complete at {}", LocalDateTime.now());
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.COMPLETE));
                } catch (Exception ex) {
                    log.error(ex.getMessage(), ex);
                    notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.ERROR, new Result<>(false, ex)));
                    throw ex;
                }
                return new CompletableFuture<>();
            });
        } catch (RejectedExecutionException e) {
            notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.ERROR, new Result<Exception>(false, e)));
            return false;
        }
        return true;
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
                     .collect(Collectors.groupingBy(site -> new FrequencyBand(site.getLowFrequency(), site.getHighFrequency()), Collectors.toMap(site -> site.getStation(), Function.identity())));
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
}
