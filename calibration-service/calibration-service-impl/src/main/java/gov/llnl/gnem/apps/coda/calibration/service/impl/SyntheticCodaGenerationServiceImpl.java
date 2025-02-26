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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticCodaGenerationService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SyntheticCodaModel;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
public class SyntheticCodaGenerationServiceImpl implements SyntheticCodaGenerationService {

    private static final Logger log = LoggerFactory.getLogger(SyntheticCodaGenerationServiceImpl.class);

    private WaveformToTimeSeriesConverter converter;
    private SyntheticCodaModel syntheticCodaModel;
    private SharedFrequencyBandParametersService sfbService;

    private ConfigurationService configService;

    @Autowired
    public SyntheticCodaGenerationServiceImpl(WaveformToTimeSeriesConverter converter, SyntheticCodaModel syntheticCodaModel, SharedFrequencyBandParametersService sfbService,
            ConfigurationService configService) {
        this.converter = converter;
        this.syntheticCodaModel = syntheticCodaModel;
        this.sfbService = sfbService;
        this.configService = configService;
    }

    @Override
    public List<SyntheticCoda> generateSynthetics(List<Waveform> waveforms, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap) {
        return waveforms.parallelStream()
                        .map(wave -> createSyntheticFromWaveform(wave, frequencyBandParameterMap.get(new FrequencyBand(wave.getLowFrequency(), wave.getHighFrequency()))))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
    }

    private SyntheticCoda createSyntheticFromWaveform(Waveform sourceWaveform, SharedFrequencyBandParameters model) {
        if (WaveformUtils.isValidWaveform(sourceWaveform) && model != null) {
            SyntheticCoda synth = new SyntheticCoda();

            Station station = sourceWaveform.getStream().getStation();
            Event event = sourceWaveform.getEvent();

            TimeSeries seis = converter.convert(sourceWaveform);

            double distance = configService.getDistanceFunc().apply(configService.getEventCoord(event), configService.getStationCoord(station));

            double br = syntheticCodaModel.getDistanceFunction(model.getBeta0(), model.getBeta1(), model.getBeta2(), distance);
            double vr = syntheticCodaModel.getDistanceFunction(model.getVelocity0(), model.getVelocity1(), model.getVelocity2(), distance);
            double gr = syntheticCodaModel.getDistanceFunction(model.getGamma0(), model.getGamma1(), model.getGamma2(), distance);

            TimeT eventTime = new TimeT(event.getOriginTime());
            TimeT codastart;
            TimeT offsetCodaStart = null;

            codastart = eventTime;
            if (vr != 0.0) {
                codastart = codastart.add(distance / vr);
            } else if (sourceWaveform.getMaxVelTime() != null) {
                codastart = new TimeT(sourceWaveform.getMaxVelTime());
            }

            if (sourceWaveform.getUserStartTime() != null) {
                offsetCodaStart = new TimeT(sourceWaveform.getUserStartTime());
            }

            TimeT endTime = new TimeT(sourceWaveform.getEndTime());

            try {
                seis.cut(codastart, endTime);

                int startOffset = 0;
                if (offsetCodaStart != null) {
                    startOffset = (int) (offsetCodaStart.subtract(codastart).getEpochTime());
                }
                if (startOffset < 0) {
                    startOffset = 0;
                }

                if (seis.getSamprate() > 1.0) {
                    seis.interpolate(1.0);
                }

                int npts = seis.getNsamp();

                double[] Ac = new double[npts - startOffset];

                for (int ii = 0; ii < Ac.length; ii++) {
                    // t is relative to the phase start time - note t=0 is a
                    // singularity point - start at t = dt
                    double t = (ii + 1.0 + startOffset);
                    Ac[ii] = syntheticCodaModel.getSyntheticPointAtTime(gr, br, t);
                }

                synth.setSegment(Ac);
                if (offsetCodaStart != null) {
                    synth.setBeginTime(seis.getTime().add(offsetCodaStart.subtract(codastart).getEpochTime()).getDate());
                } else {
                    synth.setBeginTime(codastart.getDate());
                }

                synth.setEndTime(seis.getEndtime().getDate());
                synth.setSampleRate(seis.getSamprate());
                synth.setSourceWaveform(sourceWaveform);
                synth.setSourceModel(model);
                synth.setMeasuredV(vr);
                synth.setMeasuredB(br);
                synth.setMeasuredG(gr);

                return synth;
            } catch (IllegalArgumentException e) {
                log.warn("Error attempting to cut seismogram for Synthetic generation {}; {}", sourceWaveform, e.getMessage());
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public SyntheticCoda generateSynthetic(Double distance, Double lowFreq, Double highFreq, Double lengthSeconds) {
        SharedFrequencyBandParameters model = sfbService.findByFrequencyBand(new FrequencyBand(lowFreq, highFreq));
        if (model != null) {
            SyntheticCoda synth = new SyntheticCoda();

            double br = syntheticCodaModel.getDistanceFunction(model.getBeta0(), model.getBeta1(), model.getBeta2(), distance);
            double vr = syntheticCodaModel.getDistanceFunction(model.getVelocity0(), model.getVelocity1(), model.getVelocity2(), distance);
            double gr = syntheticCodaModel.getDistanceFunction(model.getGamma0(), model.getGamma1(), model.getGamma2(), distance);

            int npts = (int) (lengthSeconds + 0.5);

            double[] Ac = new double[npts];

            for (int ii = 0; ii < Ac.length; ii++) {
                // t is relative to the phase start time - note t=0 is a
                // singularity point - start at t = dt
                double t = (ii + 1.0);
                Ac[ii] = syntheticCodaModel.getSyntheticPointAtTime(gr, br, t);
            }

            synth.setSegment(Ac);
            synth.setBeginTime(new Date());

            Date endDate = new Date();
            endDate.setTime(npts);
            synth.setEndTime(endDate);

            synth.setSampleRate(1d);
            synth.setSourceModel(model);
            synth.setMeasuredV(vr);
            synth.setMeasuredB(br);
            synth.setMeasuredG(gr);

            return synth;
        }
        return null;
    }
}
