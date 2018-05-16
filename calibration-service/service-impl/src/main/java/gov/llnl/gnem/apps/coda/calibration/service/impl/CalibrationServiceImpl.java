/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.CalibrationStatusEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.messaging.Result;
import gov.llnl.gnem.apps.coda.calibration.model.domain.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.calibration.service.api.CalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.DatabaseCleaningService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PeakVelocityMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SpectraMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticCodaGenerationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.calibration.service.api.WaveformService;

@Service
@Transactional
public class CalibrationServiceImpl implements CalibrationService {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private WaveformService waveformService;
	private PeakVelocityMeasurementService peakVelocityMeasurementsService;
	private SharedFrequencyBandParametersService sharedParametersService;
	private ShapeCalibrationService shapeCalibrationService;
	private SpectraMeasurementService spectraMeasurementService;
	private SyntheticCodaGenerationService syntheticGenerationService;
	private PathCalibrationService pathCalibrationService;
	private MdacParametersFiService mdacFiService;
	private MdacParametersPsService mdacPsService;
	private ReferenceMwParametersService referenceMwService;
	private SiteCalibrationService siteCalibrationService;
	private SyntheticService syntheticService;
	private NotificationService notificationService;
	private DatabaseCleaningService cleaningService;

	private static final AtomicLong atomicLong = new AtomicLong(0l);

	private static final ExecutorService service = new ThreadPoolExecutor(	1, 1, 0, TimeUnit.SECONDS,
																			new ArrayBlockingQueue<>(1), r -> {
																				Thread thread = new Thread(r);
																				thread.setDaemon(true);
																				return thread;
																			});

	@Autowired
	public CalibrationServiceImpl(WaveformService waveformService,
			PeakVelocityMeasurementService peakVelocityMeasurementsService,
			SharedFrequencyBandParametersService sharedParametersService,
			ShapeCalibrationService shapeCalibrationService, SpectraMeasurementService spectraMeasurementService,
			SyntheticCodaGenerationService syntheticGenerationService, PathCalibrationService pathCalibrationService,
			MdacParametersFiService mdacFiService, MdacParametersPsService mdacPsService,
			ReferenceMwParametersService referenceMwService, SiteCalibrationService siteCalibrationService,
			SyntheticService syntheticService, NotificationService notificationService,
			DatabaseCleaningService cleaningService) {
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
	}

	@Override
	public boolean start(Boolean autoPickingEnabled) {
		// FIXME: These *All methods should be *AllByProjectID instead!
		final Long id = atomicLong.getAndIncrement();
		try {
			service.submit(() -> {
				try {
					notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.STARTING));
					log.info("Starting calibration at {}", LocalDateTime.now());

					// TODO: Look at removing auto picking code from the methods
					// below and centralizing it to here instead.
					Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.findAll());
					final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap = new HashMap<>(frequencyBandParameterMap);

					List<Waveform> stacks = waveformService.getAllStacks();
					// In general each step produces output that the next step
					// consumes

					// 1) Compute the peak velocity, amplitude, and SNR values
					// for
					// the given coda stacks using theoretical group velocities
					// to cut
					// the windows for noise and SN/LG arrival
					Collection<PeakVelocityMeasurement> velocityMeasurements = peakVelocityMeasurementsService.measureVelocities(stacks);

					// First step is to clean up all the intermediary results if
					// they exist. This is wildly not-thread-safe as you might
					// imagine.
					peakVelocityMeasurementsService.deleteAll();
					syntheticService.deleteAll();

					// We want to filter out the ones that don't pass the user's
					// SNR threshold
					List<PeakVelocityMeasurement> snrFilteredVelocity = velocityMeasurements.stream().filter(vel -> {
						boolean valid = false;
						if (vel.getWaveform() != null) {
							FrequencyBand fb = new FrequencyBand(	vel.getWaveform().getLowFrequency(),
																	vel.getWaveform().getHighFrequency());
							SharedFrequencyBandParameters params = snrFilterMap.get(fb);
							valid = params != null && vel.getSnr() >= params.getMinSnr();
						}
						return valid;
					}).collect(Collectors.toList());

					// Now save the new ones we just calculated
					peakVelocityMeasurementsService.save(snrFilteredVelocity);

					// 2) Compute the shape parameters describing each stack
					// (Velocity V0-2, Beta B0-2, Gamma G0-2) and then fit
					// models to each of
					// those parameters for each frequency band that can be used
					// to generate
					// synthetic coda at any given distance and frequency band
					// combination
					frequencyBandParameterMap = shapeCalibrationService.measureShapes(	snrFilteredVelocity,
																						frequencyBandParameterMap,
																						autoPickingEnabled);

					frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.save(frequencyBandParameterMap.values()));

					// 3) Now we need to generate some basic synthetics for the
					// measurement code to use to determine where to measure the
					// raw
					// amplitudes. Then feed the synthetics to the measurement
					// service and get raw at start and raw at measurement time
					// values back
					stacks = stacks.parallelStream().filter(wave -> wave.getAssociatedPicks() != null).map(wave -> {

						Optional<WaveformPick> pick = wave	.getAssociatedPicks().stream()
															.filter(p -> PICK_TYPES.F	.name()
																						.equalsIgnoreCase(p.getPickType()))
															.findFirst();
						if (pick.isPresent() && pick.get().getPickTimeSecFromOrigin() > 0) {
							return wave;
						} else {
							return null;
						}
					}).filter(Objects::nonNull).collect(Collectors.toList());
					
					List<SpectraMeasurement> spectra = spectraMeasurementService.measureSpectra(syntheticGenerationService.generateSynthetics(	stacks,
																																				frequencyBandParameterMap),
																								frequencyBandParameterMap,
																								autoPickingEnabled);

					// 4) For each event in the data set find all stations that
					// recorded the event, then compute what the estimated path
					// effect
					// correction needs to be for each frequency band
					frequencyBandParameterMap = pathCalibrationService.measurePathCorrections(	spectraByFrequencyBand(spectra),
																								frequencyBandParameterMap);

					frequencyBandParameterMap = mapParamsToFrequencyBands(sharedParametersService.save(frequencyBandParameterMap.values()));

					// 5) Measure the amplitudes again but this time we can
					// compute
					// ESH path corrected values
					spectra = spectraMeasurementService.measureSpectra(	syntheticGenerationService.generateSynthetics(	stacks,
																														frequencyBandParameterMap),
																		frequencyBandParameterMap, autoPickingEnabled);

					// 6) Now using those path correction values plus a list of
					// trusted Mw/spectra measurements for some subset of events
					// in the data
					// set we can compute what the offset is at each station
					// from the
					// expected source spectra for that MW value. This value is
					// recorded as
					// the site specific offset for measured values at each
					// frequency band
					Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap = siteCalibrationService.measureSiteCorrections(spectraByFrequencyBand(spectra),
																																								mdacFiService.findFirst(),
																																								collectByFrequencyBand(mdacPsService.findAll()),
																																								collectByEvid(referenceMwService.findAll()),
																																								frequencyBandParameterMap,
																																								PICK_TYPES.LG);
					// 7) Measure the amplitudes one last time to fill out the
					// Path+Site corrected amplitude values
					spectra = spectraMeasurementService.measureSpectra(	syntheticService.save(syntheticGenerationService.generateSynthetics(stacks,
																																			frequencyBandParameterMap)),
																		frequencyBandParameterMap, autoPickingEnabled,
																		frequencyBandSiteParameterMap);

					log.info("Calibration complete at {}", LocalDateTime.now());
					notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.COMPLETE));
				} catch (Exception ex) {
					log.error(ex.getMessage(), ex);
					notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.ERROR,
																		new Result<Exception>(false, ex)));
					throw ex;
				}
				return new CompletableFuture<>();
			});
		} catch (RejectedExecutionException e) {
			notificationService.post(new CalibrationStatusEvent(id, CalibrationStatusEvent.Status.ERROR,
																new Result<Exception>(false, e)));
			return false;
		}
		return true;
	}

	private Map<String, List<ReferenceMwParameters>> collectByEvid(List<ReferenceMwParameters> refMws) {
		return refMws.stream().collect(Collectors.groupingBy(ReferenceMwParameters::getEventId));
	}

	private Map<PICK_TYPES, MdacParametersPS> collectByFrequencyBand(List<MdacParametersPS> mdacPs) {
		return mdacPs	.stream().filter(ps -> PICK_TYPES.isKnownPhase(ps.getPhase()))
						.collect(Collectors.toMap(	ps -> (PICK_TYPES) PICK_TYPES.valueOf(ps.getPhase().toUpperCase()),
													Function.identity()));
	}

	private Map<FrequencyBand, List<SpectraMeasurement>> spectraByFrequencyBand(List<SpectraMeasurement> spectra) {
		return spectra	.stream().filter(Objects::nonNull).filter(s -> s.getWaveform() != null)
						.collect(Collectors.groupingBy(s -> new FrequencyBand(	s.getWaveform().getLowFrequency(),
																				s.getWaveform().getHighFrequency())));
	}

	private Map<FrequencyBand, SharedFrequencyBandParameters> mapParamsToFrequencyBands(
			Collection<SharedFrequencyBandParameters> params) {
		return params.stream().collect(Collectors.toMap(
														fbp -> new FrequencyBand(	fbp.getLowFrequency(),
																					fbp.getHighFrequency()),
														fbp -> fbp));
	}

	@PreDestroy
	private void stop() {
		service.shutdownNow();
	}

	@Override
	public boolean clearData() {
		if (cleaningService != null) {
			return cleaningService.clearAll();
		}

		return false;
	}

}
