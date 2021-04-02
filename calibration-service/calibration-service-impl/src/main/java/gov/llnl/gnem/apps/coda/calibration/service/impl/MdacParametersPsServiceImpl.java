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

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.MdacDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.repository.MdacParametersPsRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Service
@Transactional
public class MdacParametersPsServiceImpl implements MdacParametersPsService {

    private MdacParametersPsRepository mdacParametersRepository;
    private EntityManager em;
    private NotificationService notificationService;

    @Autowired
    public MdacParametersPsServiceImpl(EntityManager em, MdacParametersPsRepository mdacParametersRepository, NotificationService notificationService) {
        this.em = em;
        setMdacParametersRepository(mdacParametersRepository);
        this.notificationService = notificationService;
    }

    public MdacParametersPsRepository getMdacParametersRepository() {
        return mdacParametersRepository;
    }

    public void setMdacParametersRepository(MdacParametersPsRepository mdacParametersRepository) {
        this.mdacParametersRepository = mdacParametersRepository;
    }

    @Override
    @Transactional
    public void delete(MdacParametersPS mdacParameters) {
        getMdacParametersRepository().delete(mdacParameters);
        notificationService.post(new MdacDataChangeEvent());
    }

    @Override
    @Transactional
    public List<MdacParametersPS> save(Iterable<MdacParametersPS> entities) {
        notificationService.post(new MdacDataChangeEvent());
        return getMdacParametersRepository().saveAll(entities);
    }

    @Override
    @Transactional
    public void delete(Iterable<Long> ids) {
        List<MdacParametersPS> toDelete = getMdacParametersRepository().findAllById(ids);
        getMdacParametersRepository().deleteInBatch(toDelete);
        notificationService.post(new MdacDataChangeEvent());
    }

    @Override
    @Transactional
    public MdacParametersPS save(MdacParametersPS entity) {
        notificationService.post(new MdacDataChangeEvent());
        return getMdacParametersRepository().save(entity);
    }

    @Override
    public MdacParametersPS findOne(Long id) {
        return getMdacParametersRepository().findOneDetached(id);
    }

    @Override
    public MdacParametersPS findOneForUpdate(Long id) {
        return getMdacParametersRepository().findOneDetached(id);
    }

    @Override
    public List<MdacParametersPS> findAll(Iterable<Long> ids) {
        return getMdacParametersRepository().findAllById(ids);
    }

    @Override
    public List<MdacParametersPS> findAll() {
        return getMdacParametersRepository().findAll();
    }

    @Override
    public long count() {
        return getMdacParametersRepository().count();
    }

    public Class<MdacParametersPS> getEntityType() {
        return MdacParametersPS.class;
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public MdacParametersPS findMatchingPhase(String phase) {
        MdacParametersPS res = mdacParametersRepository.findByPhase(phase);
        em.detach(res);
        return res;
    }

    @Override
    public MdacParametersPS update(MdacParametersPS entry) {
        MdacParametersPS mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = mdacParametersRepository.findById(entry.getId()).orElse(null);
        } else {
            mergedEntry = mdacParametersRepository.findByPhase(entry.getPhase());
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.mergeNonNullOrEmptyFields(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new MdacDataChangeEvent());
        return mdacParametersRepository.saveAndFlush(mergedEntry);
    }
}
