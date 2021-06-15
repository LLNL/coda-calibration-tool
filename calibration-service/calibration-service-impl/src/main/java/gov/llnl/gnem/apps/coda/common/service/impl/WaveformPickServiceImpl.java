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
package gov.llnl.gnem.apps.coda.common.service.impl;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.repository.WaveformPickRepository;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformPickService;

@Service
@Transactional
public class WaveformPickServiceImpl implements WaveformPickService {

    /**
     *
     */
    private WaveformPickRepository waveformPickRepository;

    /**
     *
     * @param waveformPickRepository
     */
    @Autowired
    public WaveformPickServiceImpl(WaveformPickRepository waveformPickRepository) {
        setWaveformPickRepository(waveformPickRepository);
    }

    /**
     *
     * @return WaveformPickRepository
     */
    public WaveformPickRepository getWaveformPickRepository() {
        return waveformPickRepository;
    }

    /**
     *
     * @param waveformPickRepository
     */
    public void setWaveformPickRepository(WaveformPickRepository waveformPickRepository) {
        this.waveformPickRepository = waveformPickRepository;
    }

    /**
     *
     * @param waveformPick
     */
    @Transactional
    @Override
    public void delete(WaveformPick waveformPick) {
        getWaveformPickRepository().delete(waveformPick);
    }

    /**
     *
     * @param entities
     * @return List
     */
    @Transactional
    @Override
    public List<WaveformPick> save(Iterable<WaveformPick> entities) {
        List<WaveformPick> saved = new LinkedList<>();
        for (WaveformPick entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    /**
     *
     * @param ids
     */
    @Transactional
    @Override
    public void delete(Iterable<Long> ids) {
        List<WaveformPick> toDelete = getWaveformPickRepository().findAllById(ids);
        getWaveformPickRepository().deleteInBatch(toDelete);
    }

    /**
     *
     * @param entity
     * @return WaveformPick
     */
    @Transactional
    @Override
    public WaveformPick save(WaveformPick entity) {
        return waveformPickRepository.saveAndFlush(entity);
    }

    /**
     *
     * @param id
     * @return WaveformPick
     */
    @Override
    public WaveformPick findOne(Long id) {
        return getWaveformPickRepository().findOneDetached(id);
    }

    /**
     *
     * @param id
     * @return WaveformPick
     */
    @Override
    public WaveformPick findOneForUpdate(Long id) {
        return getWaveformPickRepository().findOneDetached(id);
    }

    /**
     *
     * @param ids
     * @return List
     */
    @Override
    public List<WaveformPick> findAll(Iterable<Long> ids) {
        return getWaveformPickRepository().findAllById(ids);
    }

    /**
     *
     * @return List
     */
    @Override
    public List<WaveformPick> findAll() {
        return getWaveformPickRepository().findAll();
    }

    /**
     *
     * @return Long
     */
    @Override
    public long count() {
        return getWaveformPickRepository().count();
    }

    /**
     *
     * @return Class
     */
    public Class<WaveformPick> getEntityType() {
        return WaveformPick.class;
    }

    /**
     *
     * @return Class
     */
    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public void clearAutopicks() {
        waveformPickRepository.deleteAllByPickType(PICK_TYPES.AP.name());
    }
}
