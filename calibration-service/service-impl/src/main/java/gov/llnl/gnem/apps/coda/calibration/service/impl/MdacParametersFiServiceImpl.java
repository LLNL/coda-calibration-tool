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

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.repository.MdacParametersFiRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;

@Service
@Transactional
public class MdacParametersFiServiceImpl implements MdacParametersFiService {

    private MdacParametersFiRepository mdacParametersRepository;
    private EntityManager em;

    @Autowired
    public MdacParametersFiServiceImpl(EntityManager em, MdacParametersFiRepository mdacParametersRepository) {
        this.em = em;
        setMdacParametersRepository(mdacParametersRepository);
    }

    public MdacParametersFiRepository getMdacParametersRepository() {
        return mdacParametersRepository;
    }

    public void setMdacParametersRepository(MdacParametersFiRepository mdacParametersRepository) {
        this.mdacParametersRepository = mdacParametersRepository;
    }

    @Transactional
    public void delete(MdacParametersFI mdacParameters) {
        getMdacParametersRepository().delete(mdacParameters);
    }

    @Transactional
    public List<MdacParametersFI> save(Iterable<MdacParametersFI> entities) {
        return getMdacParametersRepository().saveAll(entities);
    }

    @Transactional
    public void delete(Iterable<Long> ids) {
        List<MdacParametersFI> toDelete = getMdacParametersRepository().findAllById(ids);
        getMdacParametersRepository().deleteInBatch(toDelete);
    }

    @Transactional
    public MdacParametersFI save(MdacParametersFI entity) {
        return getMdacParametersRepository().save(entity);
    }

    public MdacParametersFI findOne(Long id) {
        return getMdacParametersRepository().findOneDetached(id);
    }

    public MdacParametersFI findOneForUpdate(Long id) {
        return getMdacParametersRepository().findOneDetached(id);
    }

    public List<MdacParametersFI> findAll(Iterable<Long> ids) {
        return getMdacParametersRepository().findAllById(ids);
    }

    public List<MdacParametersFI> findAll() {
        return getMdacParametersRepository().findAll();
    }

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
            mergedEntry = mdacParametersRepository.findById(entry.getId()).get();
        } else {
            mergedEntry = mdacParametersRepository.findFirstByOrderById();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.mergeNonNullOrEmptyFields(entry);
        } else {
            mergedEntry = entry;
        }
        return mdacParametersRepository.saveAndFlush(mergedEntry);
    }
}
