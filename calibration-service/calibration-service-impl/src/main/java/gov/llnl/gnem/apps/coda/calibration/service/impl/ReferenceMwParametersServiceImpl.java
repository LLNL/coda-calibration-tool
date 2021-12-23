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
        this.referenceMwParametersRepository = referenceMwParametersRepository;
    }

    public ReferenceMwParametersRepository getReferenceMwParametersRepository() {
        return referenceMwParametersRepository;
    }

    public void setReferenceMwParametersRepository(ReferenceMwParametersRepository referenceMwParametersRepository) {
        this.referenceMwParametersRepository = referenceMwParametersRepository;
    }

    @Override
    @Transactional
    public void delete(ReferenceMwParameters referenceMwParameters) {
        referenceMwParametersRepository.delete(referenceMwParameters);
    }

    @Override
    @Transactional
    public List<ReferenceMwParameters> save(Iterable<ReferenceMwParameters> entities) {
        List<ReferenceMwParameters> results = new ArrayList<>();
        for (ReferenceMwParameters ref : entities) {
            results.add(save(ref));
        }
        return results;
    }

    @Override
    @Transactional
    public void delete(Iterable<Long> ids) {
        List<ReferenceMwParameters> toDelete = referenceMwParametersRepository.findAllById(ids);
        referenceMwParametersRepository.deleteAllInBatch(toDelete);
        referenceMwParametersRepository.flush();
    }

    @Override
    @Transactional
    public void deleteAllByEventIds(Collection<String> eventIds) {
        List<ReferenceMwParameters> toDelete = referenceMwParametersRepository.findAllByEventIds(eventIds);
        referenceMwParametersRepository.deleteAllInBatch(toDelete);
        referenceMwParametersRepository.flush();
    }

    @Override
    @Transactional
    public ReferenceMwParameters save(ReferenceMwParameters entity) {
        ReferenceMwParameters persistentRef = referenceMwParametersRepository.findOneByEventId(entity.getEventId());
        if (persistentRef == null) {
            persistentRef = entity;
        } else {
            persistentRef.merge(entity);
        }
        return referenceMwParametersRepository.save(persistentRef);
    }

    @Override
    public ReferenceMwParameters findOne(Long id) {
        return referenceMwParametersRepository.findOneDetached(id);
    }

    @Override
    public ReferenceMwParameters findOneForUpdate(Long id) {
        return referenceMwParametersRepository.findOneDetached(id);
    }

    @Override
    public List<ReferenceMwParameters> findAll(Iterable<Long> ids) {
        return referenceMwParametersRepository.findAllById(ids);
    }

    @Override
    public List<ReferenceMwParameters> findAll() {
        return referenceMwParametersRepository.findAll();
    }

    @Override
    public long count() {
        return referenceMwParametersRepository.count();
    }

    public Class<ReferenceMwParameters> getEntityType() {
        return ReferenceMwParameters.class;
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public List<ReferenceMwParameters> findAllByEventIds(Collection<String> eventIds) {
        return referenceMwParametersRepository.findAllByEventIds(eventIds);
    }

    @Override
    public ReferenceMwParameters findByEventId(String evid) {
        return referenceMwParametersRepository.findOneByEventId(evid);
    }
}
