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

import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.ValidationMwParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.ValidationMwParametersService;

@Service
@Transactional
public class ValidationMwParametersServiceImpl implements ValidationMwParametersService {

    private ValidationMwParametersRepository validationMwParametersRepository;

    @Autowired
    public ValidationMwParametersServiceImpl(ValidationMwParametersRepository validationMwParametersRepository) {
        this.validationMwParametersRepository = validationMwParametersRepository;
    }

    public ValidationMwParametersRepository getValidationMwParametersRepository() {
        return validationMwParametersRepository;
    }

    public void setValidationMwParametersRepository(ValidationMwParametersRepository validationMwParametersRepository) {
        this.validationMwParametersRepository = validationMwParametersRepository;
    }

    @Override
    @Transactional
    public void delete(ValidationMwParameters validationMwParameters) {
        validationMwParametersRepository.delete(validationMwParameters);
    }

    @Override
    @Transactional
    public List<ValidationMwParameters> save(Iterable<ValidationMwParameters> entities) {
        List<ValidationMwParameters> results = new ArrayList<>();
        for (ValidationMwParameters ref : entities) {
            results.add(save(ref));
        }
        return results;
    }

    @Override
    @Transactional
    public void delete(Iterable<Long> ids) {
        List<ValidationMwParameters> toDelete = validationMwParametersRepository.findAllById(ids);
        validationMwParametersRepository.deleteAllInBatch(toDelete);
        validationMwParametersRepository.flush();
    }

    @Override
    @Transactional
    public void deleteAllByEventIds(Collection<String> eventIds) {
        List<ValidationMwParameters> toDelete = validationMwParametersRepository.findAllByEventIds(eventIds);
        validationMwParametersRepository.deleteAllInBatch(toDelete);
        validationMwParametersRepository.flush();
    }

    @Override
    @Transactional
    public ValidationMwParameters save(ValidationMwParameters entity) {
        ValidationMwParameters persistentRef = validationMwParametersRepository.findOneByEventId(entity.getEventId());
        if (persistentRef == null) {
            persistentRef = entity;
        } else {
            persistentRef.merge(entity);
        }
        return validationMwParametersRepository.save(persistentRef);
    }

    @Override
    public ValidationMwParameters findOne(Long id) {
        return validationMwParametersRepository.findOneDetached(id);
    }

    @Override
    public ValidationMwParameters findOneForUpdate(Long id) {
        return validationMwParametersRepository.findOneDetached(id);
    }

    @Override
    public List<ValidationMwParameters> findAll(Iterable<Long> ids) {
        return validationMwParametersRepository.findAllById(ids);
    }

    @Override
    public List<ValidationMwParameters> findAll() {
        return validationMwParametersRepository.findAll();
    }

    @Override
    public long count() {
        return validationMwParametersRepository.count();
    }

    public Class<ValidationMwParameters> getEntityType() {
        return ValidationMwParameters.class;
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public List<ValidationMwParameters> findAllByEventIds(Collection<String> eventIds) {
        return validationMwParametersRepository.findAllByEventIds(eventIds);
    }

    @Override
    public ValidationMwParameters findByEventId(String evid) {
        return validationMwParametersRepository.findOneByEventId(evid);
    }
}
