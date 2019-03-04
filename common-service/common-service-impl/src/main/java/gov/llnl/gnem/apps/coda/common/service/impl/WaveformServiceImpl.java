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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
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
import gov.llnl.gnem.apps.coda.common.repository.WaveformRepository;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;

@Service
public class WaveformServiceImpl implements WaveformService {

    private WaveformRepository waveformRepository;
    private NotificationService notificationService;

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
    }

    @Transactional
    @Override
    public Waveform save(Waveform entity) {
        return update(entity);
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
        Waveform mergedEntry = attachIfAvailableInRepository(entry);
        return waveformRepository.saveAndFlush(mergedEntry);
    }

    @Transactional
    @Override
    public List<Waveform> update(Long sessionId, Collection<Waveform> values) {
        List<Waveform> vals = new ArrayList<>(values.size());
        for (Waveform entry : values) {
            Waveform mergedEntry = attachIfAvailableInRepository(entry);
            vals.add(mergedEntry);
        }
        CompletableFuture.runAsync(() -> waveformRepository.saveAll(vals)).thenRun(() -> {
            if (sessionId != null) {
                notificationService.post(new PassFailEvent(sessionId, UUID.randomUUID().toString(), new Result<Object>(true, Boolean.TRUE)));
            }
        });
        return vals;
    }

    private Waveform attachIfAvailableInRepository(Waveform entry) {
        Waveform mergedEntry = null;
        if (entry.getId() != null) {
            mergedEntry = waveformRepository.findById(entry.getId()).get();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.mergeNonNullOrEmptyFields(entry);
        } else {
            mergedEntry = entry;
        }
        return mergedEntry;
    }

    @Override
    public List<Waveform> getAllActiveStacks() {
        ExampleMatcher matcher = ExampleMatcher.matchingAll().withIgnoreNullValues().withIgnoreCase().withIgnorePaths("id", "version", "associatedPicks", "segment");
        return waveformRepository.findAll(Example.of(new Waveform().setStream(new Stream().setChannelName(Stream.TYPE_STACK)).setActive(Boolean.TRUE), matcher));
    }

    @Override
    public List<Waveform> getByExampleAllMatching(Waveform waveform) {
        ExampleMatcher matcher = ExampleMatcher.matching().withIgnoreNullValues().withIgnoreCase().withIgnorePaths("id", "version", "associatedPicks", "segment");
        return waveformRepository.findAll(Example.of(waveform, matcher));
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
}
