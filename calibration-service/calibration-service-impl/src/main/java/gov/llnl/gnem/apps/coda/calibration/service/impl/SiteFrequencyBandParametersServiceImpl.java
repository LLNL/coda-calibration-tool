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

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.SiteFrequencyBandParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;

@Service
@Transactional
public class SiteFrequencyBandParametersServiceImpl implements SiteFrequencyBandParametersService {

    private static final Logger log = LoggerFactory.getLogger(SiteFrequencyBandParametersServiceImpl.class);

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

    @Override
    @Transactional
    public void delete(SiteFrequencyBandParameters siteFrequencyBandParameters) {
        siteFrequencyBandParametersRepository.delete(siteFrequencyBandParameters);
    }

    @Override
    @Transactional
    public List<SiteFrequencyBandParameters> save(Iterable<SiteFrequencyBandParameters> entities) {
        List<SiteFrequencyBandParameters> saved = new LinkedList<>();
        for (SiteFrequencyBandParameters entity : entities) {
            saved.add(save(entity));
        }
        return saved;
    }

    @Override
    @Transactional
    public SiteFrequencyBandParameters save(SiteFrequencyBandParameters entity) {
        SiteFrequencyBandParameters entry;
        if (entity.getId() != null) {
            entry = siteFrequencyBandParametersRepository.save(entity);
        } else {
            entry = update(entity);
        }
        return entry;
    }

    @Transactional
    @Override
    public SiteFrequencyBandParameters update(SiteFrequencyBandParameters entry) {
        SiteFrequencyBandParameters attachedEntry = attachIfAvailableInRepository(entry);
        if (attachedEntry != null) {
            attachedEntry = siteFrequencyBandParametersRepository.saveAndFlush(attachedEntry);
        }
        return attachedEntry;
    }

    private SiteFrequencyBandParameters attachIfAvailableInRepository(SiteFrequencyBandParameters entry) {
        SiteFrequencyBandParameters mergedEntry = null;
        if (entry != null) {
            if (entry.getId() != null) {
                mergedEntry = siteFrequencyBandParametersRepository.findById(entry.getId()).orElse(null);
            } else if (entry.getSiteTerm() != 0.0 && entry.getStation() != null && entry.getStation().getStationName() != null && entry.getLowFrequency() != 0.0 && entry.getHighFrequency() != 0.0) {
                if (entry.getStation().getNetworkName() == null || entry.getStation().getNetworkName().equals("UNK")) {
                    List<SiteFrequencyBandParameters> values = siteFrequencyBandParametersRepository.findByUniqueFields(
                            entry.getStation().getStationName(),
                                entry.getLowFrequency(),
                                entry.getHighFrequency());
                    SiteFrequencyBandParameters val = null;
                    if (values != null) {
                        long hits = values.size();
                        if (hits > 1) {
                            log.warn("Could not unambiguously match entry {} given possible matches {}. This entry will not be saved.", entry, values);
                            return null;
                        } else if (hits == 1) {
                            val = values.get(0);
                        }
                    }
                    mergedEntry = val;
                } else {
                    mergedEntry = siteFrequencyBandParametersRepository.findByUniqueFields(
                            entry.getStation().getNetworkName(),
                                entry.getStation().getStationName(),
                                entry.getLowFrequency(),
                                entry.getHighFrequency());
                }
            }
            if (mergedEntry != null) {
                mergedEntry = mergedEntry.mergeNonNullOrEmptyFields(entry);
            } else {
                mergedEntry = entry;
            }
        }
        return mergedEntry;
    }

    @Override
    @Transactional
    public void delete(Iterable<Long> ids) {
        List<SiteFrequencyBandParameters> toDelete = siteFrequencyBandParametersRepository.findAllById(ids);
        siteFrequencyBandParametersRepository.deleteInBatch(toDelete);
    }

    @Override
    public SiteFrequencyBandParameters findOne(Long id) {
        return siteFrequencyBandParametersRepository.findOneDetached(id);
    }

    @Override
    public SiteFrequencyBandParameters findOneForUpdate(Long id) {
        return siteFrequencyBandParametersRepository.findOneDetached(id);
    }

    @Override
    public List<SiteFrequencyBandParameters> findAll(Iterable<Long> ids) {
        return siteFrequencyBandParametersRepository.findAllById(ids);
    }

    @Override
    public List<SiteFrequencyBandParameters> findAll() {
        return siteFrequencyBandParametersRepository.findAll();
    }

    @Override
    public List<String> findDistinctStationNames() {
        return siteFrequencyBandParametersRepository.findDistinctStationNames();
    }

    @Override
    public long count() {
        return siteFrequencyBandParametersRepository.count();
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
