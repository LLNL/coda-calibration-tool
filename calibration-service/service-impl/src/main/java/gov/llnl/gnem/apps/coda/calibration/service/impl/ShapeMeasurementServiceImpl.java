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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.repository.ShapeMeasurementRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.ShapeMeasurementService;

@Service
public class ShapeMeasurementServiceImpl implements ShapeMeasurementService {

    private ShapeMeasurementRepository shapeMeasurementRepository;

    @Autowired
    public ShapeMeasurementServiceImpl(ShapeMeasurementRepository shapeMeasurementRepository) {
        this.shapeMeasurementRepository = shapeMeasurementRepository;
    }

    @Override
    public void delete(ShapeMeasurement value) {
        shapeMeasurementRepository.delete(value);
    }

    @Override
    public List<ShapeMeasurement> save(Iterable<ShapeMeasurement> entities) {
        return shapeMeasurementRepository.saveAll(entities);
    }

    @Override
    public void delete(Iterable<Long> ids) {
        shapeMeasurementRepository.deleteAll(findAll(ids));
    }

    @Override
    public ShapeMeasurement save(ShapeMeasurement entity) {
        return shapeMeasurementRepository.save(entity);
    }

    @Override
    public ShapeMeasurement findOne(Long id) {
        return shapeMeasurementRepository.findOneDetached(id);
    }

    @Override
    public ShapeMeasurement findOneForUpdate(Long id) {
        return shapeMeasurementRepository.findOneDetached(id);
    }

    @Override
    public List<ShapeMeasurement> findAll(Iterable<Long> ids) {
        return shapeMeasurementRepository.findAllById(ids);
    }

    @Override
    public List<ShapeMeasurement> findAll() {
        return shapeMeasurementRepository.findAll();
    }

    @Override
    public long count() {
        return shapeMeasurementRepository.count();
    }

    @Override
    public void deleteAll() {
        shapeMeasurementRepository.deleteAllInBatch();
    }
}
