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

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.calibration.repository.PeakVelocityMeasurementRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.PeakVelocityMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MaxVelocityCalculator;

@Service
@Transactional
public class PeakVelocityMeasurementServiceImpl implements PeakVelocityMeasurementService {

    private PeakVelocityMeasurementRepository repository;
    private MaxVelocityCalculator velocityCalc;

    @Autowired
    public PeakVelocityMeasurementServiceImpl(MaxVelocityCalculator velocityCalc, PeakVelocityMeasurementRepository repository) {
        super();
        this.repository = repository;
        this.velocityCalc = velocityCalc;
    }

    @Override
    public List<PeakVelocityMeasurement> save(Iterable<PeakVelocityMeasurement> entities) {
        return getRepository().saveAll(entities);
    }

    @Override
    public PeakVelocityMeasurement save(PeakVelocityMeasurement entity) {
        return getRepository().saveAndFlush(entity);
    }

    @Override
    public List<PeakVelocityMeasurement> findAll() {
        return getRepository().findAll();
    }

    @Override
    public long count() {
        return getRepository().count();
    }

    public PeakVelocityMeasurementRepository getRepository() {
        return repository;
    }

    public void setRepository(PeakVelocityMeasurementRepository repository) {
        this.repository = repository;
    }

    @Override
    public void delete(PeakVelocityMeasurement value) {
        getRepository().delete(value);
    }

    @Override
    public void delete(Iterable<Long> ids) {
        ids.spliterator().forEachRemaining(getRepository()::deleteById);
    }

    @Override
    public PeakVelocityMeasurement findOne(Long id) {
        return getRepository().findOneDetached(id);
    }

    @Override
    public PeakVelocityMeasurement findOneForUpdate(Long id) {
        return getRepository().findOneDetached(id);
    }

    @Override
    public List<PeakVelocityMeasurement> findAll(Iterable<Long> ids) {
        return getRepository().findAll();
    }

    @Override
    public Collection<PeakVelocityMeasurement> measureVelocities(List<Waveform> allStacks) {
        return velocityCalc.computeMaximumVelocity(allStacks);
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }

}
