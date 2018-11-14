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

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.SiteFrequencyBandParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;

@Service
@Transactional
public class SiteFrequencyBandParametersServiceImpl implements SiteFrequencyBandParametersService {

    private SiteFrequencyBandParametersRepository siteFrequencyBandParametersRepository;

    @Autowired
    public SiteFrequencyBandParametersServiceImpl(SiteFrequencyBandParametersRepository siteFrequencyBandParametersRepository) {
        setSiteFrequencyBandParametersRepository(siteFrequencyBandParametersRepository);
    }

    public SiteFrequencyBandParametersRepository getSiteFrequencyBandParametersRepository() {
        return siteFrequencyBandParametersRepository;
    }

    public void setSiteFrequencyBandParametersRepository(SiteFrequencyBandParametersRepository siteFrequencyBandParametersRepository) {
        this.siteFrequencyBandParametersRepository = siteFrequencyBandParametersRepository;
    }

    @Transactional
    public void delete(SiteFrequencyBandParameters siteFrequencyBandParameters) {
        getSiteFrequencyBandParametersRepository().delete(siteFrequencyBandParameters);
    }

    @Transactional
    public List<SiteFrequencyBandParameters> save(Iterable<SiteFrequencyBandParameters> entities) {
        return getSiteFrequencyBandParametersRepository().saveAll(entities);
    }

    @Transactional
    public void delete(Iterable<Long> ids) {
        List<SiteFrequencyBandParameters> toDelete = getSiteFrequencyBandParametersRepository().findAllById(ids);
        getSiteFrequencyBandParametersRepository().deleteInBatch(toDelete);
    }

    @Transactional
    public SiteFrequencyBandParameters save(SiteFrequencyBandParameters entity) {
        return getSiteFrequencyBandParametersRepository().save(entity);
    }

    public SiteFrequencyBandParameters findOne(Long id) {
        return getSiteFrequencyBandParametersRepository().findOneDetached(id);
    }

    public SiteFrequencyBandParameters findOneForUpdate(Long id) {
        return getSiteFrequencyBandParametersRepository().findOneDetached(id);
    }

    public List<SiteFrequencyBandParameters> findAll(Iterable<Long> ids) {
        return getSiteFrequencyBandParametersRepository().findAllById(ids);
    }

    public List<SiteFrequencyBandParameters> findAll() {
        return getSiteFrequencyBandParametersRepository().findAll();
    }

    public long count() {
        return getSiteFrequencyBandParametersRepository().count();
    }

    public Class<SiteFrequencyBandParameters> getEntityType() {
        return SiteFrequencyBandParameters.class;
    }

    @Override
    public void deleteAll() {
        siteFrequencyBandParametersRepository.deleteAllInBatch();
    }

    public Class<Long> getIdType() {
        return Long.class;
    }
}
