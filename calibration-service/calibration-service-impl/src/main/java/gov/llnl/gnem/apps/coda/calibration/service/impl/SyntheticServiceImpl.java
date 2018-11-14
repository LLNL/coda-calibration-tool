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

import gov.llnl.gnem.apps.coda.calibration.repository.SyntheticRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SyntheticService;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;

@Service
@Transactional
public class SyntheticServiceImpl implements SyntheticService {

    private SyntheticRepository repository;

    @Autowired
    public SyntheticServiceImpl(SyntheticRepository repository) {
        this.repository = repository;
    }

    @Override
    public void delete(SyntheticCoda value) {
        repository.delete(value);
    }

    @Override
    public List<SyntheticCoda> save(Iterable<SyntheticCoda> entities) {
        return repository.saveAll(entities);
    }

    @Override
    public void delete(Iterable<Long> ids) {
        ids.forEach(id -> repository.deleteById(id));
    }

    @Override
    public SyntheticCoda save(SyntheticCoda entity) {
        return repository.save(entity);
    }

    @Override
    public SyntheticCoda findOne(Long id) {
        return repository.findById(id).orElse(null);
    }

    @Override
    public SyntheticCoda findOneForUpdate(Long id) {
        return repository.findById(id).orElse(null);
    }
    

    @Override
    public SyntheticCoda findOneByWaveformId(Long id) {
       return repository.findByWaveformId(id);
    }

    @Override
    public List<SyntheticCoda> findAll(Iterable<Long> ids) {
        return repository.findAllById(ids);
    }

    @Override
    public List<SyntheticCoda> findAll() {
        return repository.findAll();
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public SyntheticCoda update(SyntheticCoda payload) {
        SyntheticCoda merged;
        if (payload.getId() != null) {
            merged = repository.findById(payload.getId()).get();
            merged = merged.mergeNonNullOrEmptyFields(payload);
        } else {
            merged = payload;
        }
        return repository.saveAndFlush(merged);
    }

    @Override
    public Collection<SyntheticCoda> update(Collection<SyntheticCoda> values) {
        List<SyntheticCoda> vals = new ArrayList<>(values.size());
        for (SyntheticCoda payload : values) {
            SyntheticCoda merged;
            if (payload.getId() != null) {
                merged = repository.findById(payload.getId()).get();
                merged = merged.mergeNonNullOrEmptyFields(payload);
            } else {
                merged = payload;
            }
            vals.add(merged);
        }
        repository.saveAll(vals);
        repository.flush();
        return vals;
    }

    @Override
    public void deleteAll() {
        repository.deleteAllInBatch();
    }
}
