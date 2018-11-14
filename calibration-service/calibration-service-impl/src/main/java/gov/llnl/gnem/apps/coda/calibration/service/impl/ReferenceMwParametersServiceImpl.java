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
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.ReferenceMwParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;

@Service
@Transactional
public class ReferenceMwParametersServiceImpl implements ReferenceMwParametersService {

    private ReferenceMwParametersRepository referenceMwParametersRepository;

    @Autowired
    public ReferenceMwParametersServiceImpl(ReferenceMwParametersRepository referenceMwParametersRepository) {
        setReferenceMwParametersRepository(referenceMwParametersRepository);
    }

    public ReferenceMwParametersRepository getReferenceMwParametersRepository() {
        return referenceMwParametersRepository;
    }

    public void setReferenceMwParametersRepository(ReferenceMwParametersRepository referenceMwParametersRepository) {
        this.referenceMwParametersRepository = referenceMwParametersRepository;
    }

    @Transactional
    public void delete(ReferenceMwParameters referenceMwParameters) {
        getReferenceMwParametersRepository().delete(referenceMwParameters);
    }

    @Transactional
    public List<ReferenceMwParameters> save(Iterable<ReferenceMwParameters> entities) {
        List<ReferenceMwParameters> results = new ArrayList<>();
        for (ReferenceMwParameters ref : entities) {
            results.add(save(ref));
        }
        return results;
    }

    @Transactional
    public void delete(Iterable<Long> ids) {
        List<ReferenceMwParameters> toDelete = getReferenceMwParametersRepository().findAllById(ids);
        getReferenceMwParametersRepository().deleteInBatch(toDelete);
    }

    @Transactional
    public ReferenceMwParameters save(ReferenceMwParameters entity) {
        ReferenceMwParameters persistentRef = referenceMwParametersRepository.findOneByEventId(entity.getEventId());
        if (persistentRef == null) {
            persistentRef = entity;
        } else {
            persistentRef.merge(entity);
        }
        return getReferenceMwParametersRepository().save(persistentRef);
    }

    public ReferenceMwParameters findOne(Long id) {
        return getReferenceMwParametersRepository().findOneDetached(id);
    }

    public ReferenceMwParameters findOneForUpdate(Long id) {
        return getReferenceMwParametersRepository().findOneDetached(id);
    }

    public List<ReferenceMwParameters> findAll(Iterable<Long> ids) {
        return getReferenceMwParametersRepository().findAllById(ids);
    }

    public List<ReferenceMwParameters> findAll() {
        return getReferenceMwParametersRepository().findAll();
    }

    public long count() {
        return getReferenceMwParametersRepository().count();
    }

    public Class<ReferenceMwParameters> getEntityType() {
        return ReferenceMwParameters.class;
    }

    public Class<Long> getIdType() {
        return Long.class;
    }
}
