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
package gov.llnl.gnem.apps.coda.common.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.PassFailEvent;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.messaging.WaveformChangeEvent;
import gov.llnl.gnem.apps.coda.common.repository.WaveformRepository;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;

@Service
public class WaveformServiceImpl implements WaveformService {

    private WaveformRepository waveformRepository;
    private NotificationService notificationService;
    private ExampleMatcher ignoreStandardFieldsMatcher = ExampleMatcher.matching().withIgnoreNullValues().withIgnoreCase().withIgnorePaths("id", "version", "associatedPicks", "segment");

    @Autowired
    public WaveformServiceImpl(WaveformRepository waveformRepository, NotificationService notificationService) {
        this.waveformRepository = waveformRepository;
        this.notificationService = notificationService;
    }

    public WaveformRepository getWaveformRepository() {
        return waveformRepository;
    }

    public void setWaveformRepository(WaveformRepository waveformRepository) {
        this.waveformRepository = waveformRepository;
    }

    @Transactional
    @Override
    public void delete(Waveform waveform) {
        getWaveformRepository().delete(waveform);
        notificationService.post(new WaveformChangeEvent(getIds(waveform)).setDelete(true));
    }

    @Transactional
    @Override
    public List<Waveform> save(Iterable<Waveform> entities) {
        List<Waveform> saved = new LinkedList<>();
        for (Waveform entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Transactional
    @Override
    public void delete(Iterable<Long> ids) {
        List<Waveform> toDelete = getWaveformRepository().findAllById(ids);
        getWaveformRepository().deleteInBatch(toDelete);
        notificationService.post(new WaveformChangeEvent(getIds(toDelete)).setDelete(true));
    }

    @Transactional
    @Override
    public Waveform save(Waveform entity) {
        Waveform wave;
        if (entity.getId() != null) {
            wave = waveformRepository.save(entity);
        } else {
            wave = update(entity);
        }
        return wave;
    }

    @Override
    public Waveform findOne(Long id) {
        return getWaveformRepository().findOneDetached(id);
    }

    @Override
    public Waveform findOneForUpdate(Long id) {
        return getWaveformRepository().findOneDetached(id);
    }

    @Override
    public List<Waveform> findAll(Iterable<Long> ids) {
        return getWaveformRepository().findAllById(ids);
    }

    @Override
    public List<Waveform> findAllMetadata(List<Long> ids) {
        return getWaveformRepository().findAllMetadataByIds(ids);
    }

    @Override
    public List<Waveform> findAll() {
        return getWaveformRepository().findAll();
    }

    @Override
    public long count() {
        return getWaveformRepository().count();
    }

    public Class<Waveform> getEntityType() {
        return Waveform.class;
    }

    public Class<Integer> getIdType() {
        return Integer.class;
    }

    @Transactional
    @Override
    public Waveform update(Waveform entry) {
        Waveform mergedEntry = waveformRepository.saveAndFlush(attachIfAvailableInRepository(entry));
        notificationService.post(new WaveformChangeEvent(getIds(mergedEntry)).setAddOrUpdate(true));
        return mergedEntry;
    }

    @Transactional
    @Override
    public List<Waveform> update(Long sessionId, List<Waveform> values) {
        List<Waveform> vals = new ArrayList<>(values.size());
        for (Waveform entry : values) {
            Waveform mergedEntry = attachIfAvailableInRepository(entry);
            vals.add(mergedEntry);
        }
        CompletableFuture.runAsync(() -> {
            List<Waveform> saved = waveformRepository.saveAll(vals);
            waveformRepository.flush();
            if (sessionId != null) {
                notificationService.post(new PassFailEvent(sessionId, UUID.randomUUID().toString(), new Result<Object>(true, Boolean.TRUE)));
                notificationService.post(new WaveformChangeEvent(getIds(saved)).setAddOrUpdate(true));
            }
        });
        return vals;
    }

    private List<Long> getIds(Waveform waveform) {
        return getIds(Collections.singletonList(waveform));
    }

    private List<Long> getIds(List<Waveform> vals) {
        return vals.stream().map(Waveform::getId).collect(Collectors.toList());
    }

    private Waveform attachIfAvailableInRepository(Waveform entry) {
        Waveform mergedEntry = null;
        if (entry != null) {
            if (entry.getId() != null) {
                mergedEntry = waveformRepository.findById(entry.getId()).orElse(null);
            } else if (entry.getEvent() != null && entry.getStream() != null && entry.getStream().getStation() != null && entry.getLowFrequency() != null && entry.getHighFrequency() != null) {
                mergedEntry = waveformRepository.findByUniqueFields(
                        entry.getEvent().getEventId(),
                            entry.getStream().getStation().getNetworkName(),
                            entry.getStream().getStation().getStationName(),
                            entry.getLowFrequency(),
                            entry.getHighFrequency());
            }
            if (mergedEntry != null) {
                mergedEntry = mergedEntry.mergeNonNullOrEmptyFields(entry);
            } else {
                mergedEntry = entry;
            }
        }
        return mergedEntry;
    }

    @Override
    public List<Waveform> getAllActiveStacks() {
        return getByExampleAllDistinctMatching(new Waveform().setActive(Boolean.TRUE).setStream(new Stream().setChannelName(Stream.TYPE_STACK)));
    }

    @Override
    public List<Waveform> getAllStacks() {
        return getByExampleAllDistinctMatching(new Waveform().setStream(new Stream().setChannelName(Stream.TYPE_STACK)));
    }

    @Override
    public List<Waveform> findAllActiveStacksByEventIdAndStationNames(String eventId, List<String> stationNames) {
        return waveformRepository.findAllActiveStacksByEventIdAndStationNames(eventId, stationNames);
    }

    @Override
    public List<Waveform> getAllActiveStacksInStationNames(List<String> stationNames) {
        return waveformRepository.findAllActiveStacksByStationNames(stationNames);
    }

    @Override
    public List<Waveform> getActiveSharedEventStationStacksById(Long id) {
        return waveformRepository.findAllActiveSharedEventStationStacksById(id);
    }

    @Override
    public List<Waveform> getByExampleAllDistinctMatching(Waveform waveform) {
        return Optional.ofNullable(waveformRepository.findAll(Example.of(waveform, ignoreStandardFieldsMatcher))).orElseGet(ArrayList::new).stream().distinct().collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public List<Waveform> getUniqueEventStationStacks() {
        return waveformRepository.getWaveformMetadata().stream().filter(md -> md.getId() != null).collect(Collectors.toList());
    }

    @Override
    public Event findEventById(String eventId) {
        List<Event> results = waveformRepository.findEventById(eventId, PageRequest.of(0, 1));
        if (results != null && !results.isEmpty()) {
            return results.get(0);
        } else {
            return new Event();
        }
    }

    @Override
    public List<Long> setActiveFlagForIds(List<Long> selectedWaveforms, boolean active) {
        List<Waveform> waveforms = waveformRepository.findAllById(selectedWaveforms);
        waveforms.forEach(w -> w.setActive(active));
        waveformRepository.saveAll(waveforms);
        notificationService.post(new WaveformChangeEvent(selectedWaveforms).setAddOrUpdate(true));
        return selectedWaveforms;
    }

    @Override
    public List<Long> setActiveFlagByEventId(String eventId, boolean active) {
        List<Long> ids = new ArrayList<>();
        if (waveformRepository.setActiveByEventId(eventId, active) > 0) {
            ids.addAll(waveformRepository.findAllIdsByEventId(eventId));
            notificationService.post(new WaveformChangeEvent(ids).setAddOrUpdate(true));
        }
        return ids;
    }

    @Override
    public List<Long> setActiveFlagByStationName(String stationName, boolean active) {
        List<Long> ids = new ArrayList<>();
        if (waveformRepository.setActiveByStationName(stationName, active) > 0) {
            ids.addAll(waveformRepository.findAllIdsByStationName(stationName));
            notificationService.post(new WaveformChangeEvent(ids).setAddOrUpdate(true));
        }
        return ids;
    }

}
