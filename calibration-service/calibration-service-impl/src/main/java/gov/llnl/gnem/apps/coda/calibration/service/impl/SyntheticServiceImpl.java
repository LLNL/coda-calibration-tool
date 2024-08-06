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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.repository.SyntheticRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Service
@Transactional
public class SyntheticServiceImpl implements SyntheticService {

    private static final Logger log = LoggerFactory.getLogger(SyntheticServiceImpl.class);

    private SyntheticRepository repository;

    private EntityManager em;

    @Autowired
    public SyntheticServiceImpl(SyntheticRepository repository, EntityManager em) {
        this.repository = repository;
        this.em = em;
    }

    @Override
    public void delete(SyntheticCoda value) {
        repository.delete(value);
    }

    @Override
    public List<SyntheticCoda> save(Iterable<SyntheticCoda> entities) {
        return repository.saveAll(entities);
    }

    @Override
    public void delete(Iterable<Long> ids) {
        ids.forEach(id -> repository.deleteById(id));
    }

    @Override
    public SyntheticCoda save(SyntheticCoda entity) {
        return repository.save(entity);
    }

    @Override
    public SyntheticCoda findOne(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public SyntheticCoda findOneForUpdate(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public SyntheticCoda findOneByWaveformId(Long id) {
        return repository.findByWaveformId(id);
    }

    @Override
    public Collection<SyntheticCoda> findAllByWaveformId(Collection<Long> ids) {
        return repository.findByWaveformIds(ids);
    }

    @Override
    public List<SyntheticCoda> findAll(Iterable<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public List<SyntheticCoda> findAll() {
        return repository.findAll();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public SyntheticCoda update(SyntheticCoda payload) {
        SyntheticCoda merged;
        Optional<SyntheticCoda> existing = repository.findById(payload.getId());
        if (payload.getId() != null && existing.isPresent()) {
            merged = existing.get();
            merged = merged.mergeNonNullOrEmptyFields(payload);
        } else {
            merged = payload;
        }
        return repository.saveAndFlush(merged);
    }

    @Override
    public Collection<SyntheticCoda> update(Collection<SyntheticCoda> values) {
        List<SyntheticCoda> vals = new ArrayList<>(values.size());
        for (SyntheticCoda payload : values) {
            SyntheticCoda merged;
            Optional<SyntheticCoda> existing = repository.findById(payload.getId());
            if (payload.getId() != null && existing.isPresent()) {
                merged = existing.get();
                merged = merged.mergeNonNullOrEmptyFields(payload);
            } else {
                merged = payload;
            }
            vals.add(merged);
        }
        repository.saveAll(vals);
        repository.flush();
        return vals;
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }

    @Override
    public List<SyntheticCoda> amplitudeCorrectedSyntheticsQuery(final String eventId, final String stationName, final Double lowFreq, final Double highFreq, final Double sampleRate) {
        List<SyntheticCoda> synthetics = repository.findMatchingSelection(eventId, stationName, lowFreq, highFreq);
        //This ensures the results are detached so we don't accidently overwrite data in the database
        synthetics.forEach(em::detach);
        if (sampleRate != null) {
            synthetics.parallelStream().filter(s -> s.getSegment() != null).forEach(synth -> {
                if (synth.getSampleRate() != sampleRate && sampleRate != null) {
                    TimeSeries series = new TimeSeries();
                    series.setData(WaveformUtils.doublesToFloats(synth.getSegment()));
                    series.setSamprate(synth.getSampleRate());
                    series.interpolate(sampleRate);
                    synth.setSegment(WaveformUtils.floatsToDoubles(series.getData()));
                    synth.setSampleRate(sampleRate);
                }
            });
        }

        //Adjust amplitudes by median value of source waveform cut
        synthetics.parallelStream().filter(s -> s.getSegment() != null).forEach(synth -> {

            WaveformPick endPick = null;
            Waveform sourceWaveform = synth.getSourceWaveform();
            Event event = sourceWaveform.getEvent();

            TimeT originTime;
            if (event != null) {
                originTime = new TimeT(event.getOriginTime());
            } else {
                originTime = new TimeT(synth.getBeginTime());
            }

            for (final WaveformPick p : sourceWaveform.getAssociatedPicks()) {
                if (p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim())) {
                    endPick = p;
                    break;
                }
            }
            TimeT endTime;

            if (endPick != null && event != null) {
                endTime = new TimeT(originTime).add(endPick.getPickTimeSecFromOrigin());
            } else {
                endTime = new TimeT(synth.getEndTime());
            }

            TimeT startTime = new TimeT(synth.getBeginTime());
            if (startTime.lt(endTime)) {
                TimeSeries source = new WaveformToTimeSeriesConverter().convert(sourceWaveform);
                source.cut(startTime, endTime);
                source.interpolate(synth.getSampleRate());

                TimeSeries synthSeries = new TimeSeries(WaveformUtils.doublesToFloats(synth.getSegment()), synth.getSampleRate(), new TimeT(synth.getBeginTime()));
                final TimeSeries diffSeis = source.subtract(synthSeries);
                //If check is just accounting for very low resampling rates basically dropping the signal to a
                // single point. It's a degenerate case so we should log an error and just not alter the segment
                if (synthSeries.getData().length >= 2 && source.getData().length >= 2) {
                    synthSeries.AddScalar(diffSeis.getMedian());
                    synth.setSegment(WaveformUtils.floatsToDoubles(synthSeries.getData()));
                } else {
                    log.warn(
                            "Attempted to create an amplitude adjusted synthetic with too few data points. Synthetic pts {}, Waveform pts {}, sampling rate {}",
                                synthSeries.getData().length,
                                source.getData().length,
                                synth.getSampleRate());
                }
            }

        });
        return synthetics;
    }

}
