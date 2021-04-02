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

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.MdacDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.repository.MdacParametersFiRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Service
@Transactional
public class MdacParametersFiServiceImpl implements MdacParametersFiService {

    private MdacParametersFiRepository mdacParametersRepository;
    private EntityManager em;
    private NotificationService notificationService;

    @Autowired
    public MdacParametersFiServiceImpl(EntityManager em, MdacParametersFiRepository mdacParametersRepository, NotificationService notificationService) {
        this.em = em;
        setMdacParametersRepository(mdacParametersRepository);
        this.notificationService = notificationService;
    }

    public MdacParametersFiRepository getMdacParametersRepository() {
        return mdacParametersRepository;
    }

    public void setMdacParametersRepository(MdacParametersFiRepository mdacParametersRepository) {
        this.mdacParametersRepository = mdacParametersRepository;
    }

    @Override
    @Transactional
    public void delete(MdacParametersFI mdacParameters) {
        getMdacParametersRepository().delete(mdacParameters);
        notificationService.post(new MdacDataChangeEvent());
    }

    @Override
    @Transactional
    public List<MdacParametersFI> save(Iterable<MdacParametersFI> entities) {
        notificationService.post(new MdacDataChangeEvent());
        return getMdacParametersRepository().saveAll(entities);
    }

    @Override
    @Transactional
    public void delete(Iterable<Long> ids) {
        List<MdacParametersFI> toDelete = getMdacParametersRepository().findAllById(ids);
        getMdacParametersRepository().deleteInBatch(toDelete);
        notificationService.post(new MdacDataChangeEvent());
    }

    @Override
    @Transactional
    public MdacParametersFI save(MdacParametersFI entity) {
        notificationService.post(new MdacDataChangeEvent());
        return getMdacParametersRepository().save(entity);
    }

    @Override
    public MdacParametersFI findOne(Long id) {
        return getMdacParametersRepository().findOneDetached(id);
    }

    @Override
    public MdacParametersFI findOneForUpdate(Long id) {
        return getMdacParametersRepository().findOneDetached(id);
    }

    @Override
    public List<MdacParametersFI> findAll(Iterable<Long> ids) {
        return getMdacParametersRepository().findAllById(ids);
    }

    @Override
    public List<MdacParametersFI> findAll() {
        return getMdacParametersRepository().findAll();
    }

    @Override
    public long count() {
        return getMdacParametersRepository().count();
    }

    public Class<MdacParametersFI> getEntityType() {
        return MdacParametersFI.class;
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public MdacParametersFI findFirst() {
        MdacParametersFI res = mdacParametersRepository.findFirstByOrderById();
        em.detach(res);
        return res;
    }

    @Override
    public MdacParametersFI update(MdacParametersFI entry) {
        MdacParametersFI mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = mdacParametersRepository.findById(entry.getId()).orElse(null);
        } else {
            mergedEntry = mdacParametersRepository.findFirstByOrderById();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.merge(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new MdacDataChangeEvent());
        return mdacParametersRepository.saveAndFlush(mergedEntry);
    }
}
