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

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticCodaGenerationService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SyntheticCodaModel;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.WaveformUtils;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
public class SyntheticCodaGenerationServiceImpl implements SyntheticCodaGenerationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WaveformToTimeSeriesConverter converter;
    private SyntheticCodaModel syntheticCodaModel;

    @Autowired
    public SyntheticCodaGenerationServiceImpl(WaveformToTimeSeriesConverter converter, SyntheticCodaModel syntheticCodaModel) {
        this.converter = converter;
        this.syntheticCodaModel = syntheticCodaModel;
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

            double distance = EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude());
            double br = syntheticCodaModel.getDistanceFunction(model.getBeta0(), model.getBeta1(), model.getBeta2(), distance);
            double vr = syntheticCodaModel.getDistanceFunction(model.getVelocity0(), model.getVelocity1(), model.getVelocity2(), distance);
            double gr = syntheticCodaModel.getDistanceFunction(model.getGamma0(), model.getGamma1(), model.getGamma2(), distance);

            // setting dt = 1.0 - not the SACfile's header.delta
            double dt = 1.;

            // note distance/vr is a singularity point - start at t = dt
            TimeT eventTime = new TimeT(event.getOriginTime());
            TimeT codastart = eventTime;
            if (vr != 0.0) {
                codastart = codastart.add(distance / vr);
            }
            double maxTime = seis.getMaxTime()[0];
            double timediff = maxTime - codastart.subtractD(eventTime);
            if (Math.abs(timediff) < 5.0) {
                codastart.add(timediff);
            }

            TimeT endTime = new TimeT(sourceWaveform.getEndTime());

            try {
                seis.cut(codastart, endTime);
                seis.interpolate(1 / dt);

                int npts = seis.getNsamp();

                //TODO: Set synthetic end time to max length of measurement (+1?) for FB if it's set and > 0.0
                Double[] Ac = new Double[npts];

                for (int ii = 0; ii < Ac.length; ii++) {
                    // t is relative to the phase start time - note t=0 is a
                    // singularity point - start at t = dt
                    double t = (ii + 1) * dt;
                    Ac[ii] = syntheticCodaModel.getSyntheticPointAtTime(gr, br, t);
                }

                synth.setSegment(Ac);
                synth.setBeginTime(seis.getTime().getDate());
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
}
