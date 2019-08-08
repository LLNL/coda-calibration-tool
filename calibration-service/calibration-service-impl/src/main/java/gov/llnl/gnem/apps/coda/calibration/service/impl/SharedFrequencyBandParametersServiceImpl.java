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

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.messaging.BandParametersDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.repository.SharedFrequencyBandParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.SyntheticRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SharedFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Service
@Transactional
public class SharedFrequencyBandParametersServiceImpl implements SharedFrequencyBandParametersService {

    private static final Logger log = LoggerFactory.getLogger(SharedFrequencyBandParametersServiceImpl.class);

    private SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository;
    private SyntheticRepository syntheticsRepository;
    private NotificationService notificationService;

    @Autowired
    public SharedFrequencyBandParametersServiceImpl(SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository, SyntheticRepository syntheticsRepository,
            NotificationService notificationService) {
        this.sharedFrequencyBandParametersRepository = sharedFrequencyBandParametersRepository;
        this.syntheticsRepository = syntheticsRepository;
        this.notificationService = notificationService;
    }

    public SharedFrequencyBandParametersRepository getSharedFrequencyBandParametersRepository() {
        return sharedFrequencyBandParametersRepository;
    }

    public void setSharedFrequencyBandParametersRepository(SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository) {
        this.sharedFrequencyBandParametersRepository = sharedFrequencyBandParametersRepository;
    }

    @Override
    public void delete(SharedFrequencyBandParameters sharedFrequencyBandParameters) {
        syntheticsRepository.deleteBySharedFrequencyBandParametersId(sharedFrequencyBandParameters.getId());
        getSharedFrequencyBandParametersRepository().delete(sharedFrequencyBandParameters);
        notificationService.post(new BandParametersDataChangeEvent());
    }

    @Override
    public List<SharedFrequencyBandParameters> save(Iterable<SharedFrequencyBandParameters> entities) {
        notificationService.post(new BandParametersDataChangeEvent());
        return getSharedFrequencyBandParametersRepository().saveAll(entities);
    }

    @Override
    public void delete(Iterable<Long> ids) {
        List<SharedFrequencyBandParameters> toDelete = getSharedFrequencyBandParametersRepository().findAllById(ids);
        syntheticsRepository.deleteInBatchBySharedFrequencyBandParametersIds(toDelete.stream().map(sfb -> sfb.getId()).collect(Collectors.toList()));
        getSharedFrequencyBandParametersRepository().deleteInBatch(toDelete);
        notificationService.post(new BandParametersDataChangeEvent());
    }

    @Override
    public SharedFrequencyBandParameters save(SharedFrequencyBandParameters entity) {
        notificationService.post(new BandParametersDataChangeEvent());
        return getSharedFrequencyBandParametersRepository().save(entity);
    }

    @Override
    public SharedFrequencyBandParameters findOne(Long id) {
        return getSharedFrequencyBandParametersRepository().findOneDetached(id);
    }

    @Override
    public SharedFrequencyBandParameters findOneForUpdate(Long id) {
        return getSharedFrequencyBandParametersRepository().findOneDetached(id);
    }

    @Override
    public List<SharedFrequencyBandParameters> findAll(Iterable<Long> ids) {
        return getSharedFrequencyBandParametersRepository().findAllById(ids);
    }

    @Override
    public List<SharedFrequencyBandParameters> findAll() {
        return getSharedFrequencyBandParametersRepository().findAll();
    }

    @Override
    public long count() {
        return getSharedFrequencyBandParametersRepository().count();
    }

    public Class<SharedFrequencyBandParameters> getEntityType() {
        return SharedFrequencyBandParameters.class;
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public List<FrequencyBand> getFrequencyBands() {
        return sharedFrequencyBandParametersRepository.findDistinctFrequencyBands();
    }

    @Override
    public SharedFrequencyBandParameters update(SharedFrequencyBandParameters entry) {
        SharedFrequencyBandParameters mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = sharedFrequencyBandParametersRepository.findById(entry.getId()).get();
        } else {
            mergedEntry = sharedFrequencyBandParametersRepository.findByLowFrequencyAndHighFrequency(entry.getLowFrequency(), entry.getHighFrequency());
        }
        if (mergedEntry != null) {
            log.trace("Merging shared band entry: new {} old {}", entry, mergedEntry);
            mergedEntry = mergedEntry.mergeNonNullOrEmptyFields(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new BandParametersDataChangeEvent());
        return sharedFrequencyBandParametersRepository.saveAndFlush(mergedEntry);
    }

    @Override
    public SharedFrequencyBandParameters findByFrequencyBand(FrequencyBand band) {
        return sharedFrequencyBandParametersRepository.findByLowFrequencyAndHighFrequency(band.getLowFrequency(), band.getHighFrequency());
    }
}
