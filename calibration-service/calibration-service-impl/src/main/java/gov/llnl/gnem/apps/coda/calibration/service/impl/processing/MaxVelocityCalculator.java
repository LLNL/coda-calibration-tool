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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.base.Functions;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformPickService;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class MaxVelocityCalculator {

    private static final Logger log = LoggerFactory.getLogger(MaxVelocityCalculator.class);
    private WaveformToTimeSeriesConverter converter;
    private VelocityConfiguration velConf;
    private SharedFrequencyBandParametersService sfbService;
    private WaveformPickService pickService;
    private ConfigurationService configService;

    @Autowired
    public MaxVelocityCalculator(VelocityConfiguration velConf, WaveformToTimeSeriesConverter converter, SharedFrequencyBandParametersService sfbService, WaveformPickService pickService,
            ConfigurationService configService) {
        this.converter = converter;
        this.velConf = velConf;
        this.sfbService = sfbService;
        this.pickService = pickService;
        this.configService = configService;
    }

    public List<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms, VelocityConfiguration velocityConfiguration, boolean persistResults) {
        return computeMaximumVelocity(
                waveforms,
                    getFrequencyBandMap(),
                    velocityConfiguration.getGroupVelocity1InKmsGtDistance(),
                    velocityConfiguration.getGroupVelocity2InKmsGtDistance(),
                    velocityConfiguration.getGroupVelocity1InKmsLtDistance(),
                    velocityConfiguration.getGroupVelocity2InKmsLtDistance(),
                    velocityConfiguration.getDistanceThresholdInKm(),
                    persistResults);
    }

    public List<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms) {
        return computeMaximumVelocity(
                waveforms,
                    getFrequencyBandMap(),
                    velConf.getGroupVelocity1InKmsGtDistance(),
                    velConf.getGroupVelocity2InKmsGtDistance(),
                    velConf.getGroupVelocity1InKmsLtDistance(),
                    velConf.getGroupVelocity2InKmsLtDistance(),
                    velConf.getDistanceThresholdInKm(),
                    true);
    }

    public List<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms, VelocityConfiguration velocityConfiguration) {
        if (velocityConfiguration != null) {
            return computeMaximumVelocity(
                    waveforms,
                        getFrequencyBandMap(),
                        velocityConfiguration.getGroupVelocity1InKmsGtDistance(),
                        velocityConfiguration.getGroupVelocity2InKmsGtDistance(),
                        velocityConfiguration.getGroupVelocity1InKmsLtDistance(),
                        velocityConfiguration.getGroupVelocity2InKmsLtDistance(),
                        velocityConfiguration.getDistanceThresholdInKm(),
                        true);
        } else {
            return computeMaximumVelocity(waveforms);
        }
    }

    private Map<FrequencyBand, SharedFrequencyBandParameters> getFrequencyBandMap() {
        return sfbService.findAll().stream().collect(Collectors.toMap(sfb -> new FrequencyBand(sfb.getLowFrequency(), sfb.getHighFrequency()), Functions.identity(), (arg0, arg1) -> arg0));
    }

    private List<PeakVelocityMeasurement> computeMaximumVelocity(List<Waveform> waveforms, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBands, double gv1GtDistanceThreshold,
            double gv2GtDistanceThreshold, double gv1LtDistanceThreshold, double gv2LtDistanceThreshold, double thresholdInKm, boolean savePicks) {
        return waveforms.stream().parallel().map(rawWaveform -> {
            if (Thread.currentThread().isInterrupted()) {
                return null;
            }

            SharedFrequencyBandParameters band = frequencyBands.get(new FrequencyBand(rawWaveform.getLowFrequency(), rawWaveform.getHighFrequency()));
            double codaOffset = 0d;
            if (band != null) {
                codaOffset = band.getCodaStartOffset();
            }

            TimeSeries waveform = converter.convert(rawWaveform);

            double distance = configService.getDistanceFunc().apply(configService.getEventCoord(rawWaveform.getEvent()), configService.getStationCoord(rawWaveform.getStream().getStation()));
            TimeT origintime = new TimeT(rawWaveform.getEvent().getOriginTime());
            TimeT starttime;
            TimeT endtime;

            if (distance >= thresholdInKm) {
                starttime = origintime.add(distance / gv1GtDistanceThreshold);
                endtime = origintime.add(distance / gv2GtDistanceThreshold);
            } else {
                starttime = origintime.add(distance / gv1LtDistanceThreshold);
                endtime = origintime.add(distance / gv2LtDistanceThreshold);
            }

            // The envelope is in log10.
            try {
                // cut the coda window portion of the seismograms
                waveform.cut(starttime, endtime);

                // peakS[0] time in seconds for
                // reference peakS[1] max amplitude
                double[] peakS = WaveformUtils.getMaxTime(waveform, origintime);

                // +1 to avoid divide by zero slamming this to +Infinity when
                // the first sample is the largest
                if (peakS[0] == 0.0) {
                    peakS[0] = 1.0;
                }

                double velocity = distance / peakS[0];

                double noise = WaveformUtils.getNoiseFloor(rawWaveform.getSegment());

                // the envelope noise is in log10 units.
                double snrPeak = peakS[1] - noise;

                //TODO: Check for user set max time instead if that exists
                //Attach the measurement to the waveform for later
                rawWaveform.setMaxVelTime(origintime.add(peakS[0]).getDate());
                rawWaveform.setCodaStartTime(origintime.add(peakS[0] + codaOffset).getDate());

                boolean hasUserStartPick = false;
                if (rawWaveform.getAssociatedPicks() != null) {
                    for (WaveformPick pick : rawWaveform.getAssociatedPicks()) {
                        if (PICK_TYPES.UCS.getPhase().equals(pick.getPickName())) {
                            hasUserStartPick = true;
                            break;
                        }
                    }
                    //Drop old CS picks if there are any
                    rawWaveform.setAssociatedPicks(rawWaveform.getAssociatedPicks().stream().filter(p -> !PICK_TYPES.CS.getPhase().equals(p.getPickName())).collect(Collectors.toList()));

                    if (savePicks && !hasUserStartPick) {
                        WaveformPick startPick = new WaveformPick().setPickType(PICK_TYPES.CS.name())
                                                                   .setPickName(PICK_TYPES.CS.getPhase())
                                                                   .setWaveform(rawWaveform)
                                                                   .setPickTimeSecFromOrigin(peakS[0] + codaOffset);
                        startPick = pickService.save(startPick);
                        rawWaveform.getAssociatedPicks().add(startPick);
                    }
                }

                return new PeakVelocityMeasurement().setWaveform(rawWaveform)
                                                    .setNoiseStartSecondsFromOrigin(0d)
                                                    .setNoiseEndSecondsFromOrigin(20d)
                                                    .setNoiseLevel(noise)
                                                    .setSnr(snrPeak)
                                                    .setVelocity(velocity)
                                                    .setDistance(distance)
                                                    .setTime(peakS[0])
                                                    .setAmplitude(peakS[1]);
            } catch (IllegalArgumentException ill) {
                log.info("Unable to compute maximum velocity, this stack will be skipped. {} {}.", ill.getMessage(), rawWaveform);
                return new PeakVelocityMeasurement();
            }
        }).filter(measurement -> measurement != null && measurement.getWaveform() != null).collect(Collectors.toList());
    }
}
