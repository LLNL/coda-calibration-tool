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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.MeasuredMwsRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;

@Service
@Transactional
public class MeasuredMwsServiceImpl implements MeasuredMwsService {

    private MeasuredMwsRepository measuredMwsRepository;

    public MeasuredMwsRepository getMeasuredMwsRepository() {
        return measuredMwsRepository;
    }

    public void setMeasuredMwsRepository(MeasuredMwsRepository measuredMwsRepository) {
        this.measuredMwsRepository = measuredMwsRepository;
    }

    @Autowired
    public MeasuredMwsServiceImpl(MeasuredMwsRepository measuredMwsRepository) {
        setMeasuredMwsRepository(measuredMwsRepository);
    }

    @Transactional
    public void delete(MeasuredMwParameters MeasuredMwParameters) {
        getMeasuredMwsRepository().delete(MeasuredMwParameters);
    }

    @Transactional
    public List<MeasuredMwParameters> save(Iterable<MeasuredMwParameters> entities) {
        return getMeasuredMwsRepository().saveAll(entities);
    }

    @Transactional
    public void delete(Iterable<Long> ids) {
        List<MeasuredMwParameters> toDelete = getMeasuredMwsRepository().findAllById(ids);
        getMeasuredMwsRepository().deleteInBatch(toDelete);
    }

    @Transactional
    public MeasuredMwParameters save(MeasuredMwParameters entity) {
        return getMeasuredMwsRepository().save(entity);
    }

    public MeasuredMwParameters findOne(Long id) {
        return getMeasuredMwsRepository().findOneDetached(id);
    }

    public MeasuredMwParameters findOneForUpdate(Long id) {
        return getMeasuredMwsRepository().findOneDetached(id);
    }

    public List<MeasuredMwParameters> findAll(Iterable<Long> ids) {
        return getMeasuredMwsRepository().findAllById(ids);
    }

    public List<MeasuredMwParameters> findAll() {
        return getMeasuredMwsRepository().findAll();
    }

    public long count() {
        return getMeasuredMwsRepository().count();
    }

    public Class<MeasuredMwParameters> getEntityType() {
        return MeasuredMwParameters.class;
    }

    @Override
    public void deleteAll() {
        getMeasuredMwsRepository().deleteAllInBatch();
    }

    public Class<Long> getIdType() {
        return Long.class;
    }
}
