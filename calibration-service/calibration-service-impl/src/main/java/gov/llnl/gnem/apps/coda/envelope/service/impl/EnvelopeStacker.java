/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.envelope.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import gov.llnl.gnem.apps.coda.envelope.service.api.WaveformStacker;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class EnvelopeStacker implements WaveformStacker {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private WaveformToTimeSeriesConverter converter;

    @Autowired
    public EnvelopeStacker(WaveformToTimeSeriesConverter converter) {
        this.converter = converter;
    }

    @Override
    public Waveform stackEnvelopes(List<Waveform> waves) {
        // TODO: Implement array processing
        // TODO: Profile to improve perf
        // TODO: Progress notification for stacking

        //TODO: Thoughts on array stacks
        // 1. Compare all the elements in the array to look for outliers in measurements (bad channels).
        // Needs some kind of hypothesis testing probably to try and figure out 'bad' in this context (majority rules?).
        // 2. For each element envelope, find record begin and end of each
        // trace ( b and e may have changed because of time shifting)
        // 3. Keep the largest begin and smallest end times of all elements
        // 4. Form an average envelope by summing each trace and then
        // dividing by the total number of envelopes used
        // 5. Change station header location lat and lon to the reference
        // (array element closest to centroid of hull maybe?) and re-compute hypocentral distance
        // 6. Store the envelope for coda amplitude measurement

        Waveform base = null;
        if (waves != null && !waves.isEmpty()) {
            try {
                base = waves.get(0);
                TimeSeries seis = converter.convert(base);
                for (int i = 1; i < waves.size(); i++) {
                    TimeSeries seis2 = converter.convert(waves.get(i));
                    seis = seis.add(seis2);
                }
                seis.MultiplyScalar(1d / (waves.size()));
                base.setSegment(WaveformUtils.floatsToDoubles(seis.getData()));

                if (!base.hasData() || base.getSegmentLength() == 0) {
                    return null;
                }

                base.setSampleRate(seis.getSamprate());
                base.setBeginTime(seis.getTime().getDate());
                base.setEndTime(seis.getEndtime().getDate());
                if (base.getStream() != null) {
                    base.getStream().setChannelName(Stream.TYPE_STACK);
                }
            } catch (Exception e) {
                log.info(e.getMessage(), e);
            }
        } else {
            log.info("Empty list provided for creating envelopes, skipping");
        }
        return base;
    }
}
