/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
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

import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.GvDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.repository.VelocityConfigurationRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.ConfigurationService;
import gov.llnl.gnem.apps.coda.common.service.api.NotificationService;

@Service
@Transactional
public class ConfigurationServiceImpl implements ConfigurationService {

    private EntityManager em;
    private VelocityConfigurationRepository repository;
    private NotificationService notificationService;

    @Autowired
    public ConfigurationServiceImpl(EntityManager em, VelocityConfigurationRepository repository, NotificationService notificationService) {
        this.em = em;
        this.repository = repository;
        this.notificationService = notificationService;        
    }

    @Override
    public VelocityConfiguration update(VelocityConfiguration entry) {
        VelocityConfiguration mergedEntry;
        if (entry.getId() != null) {
            mergedEntry = repository.findById(entry.getId()).get();
        } else {
            mergedEntry = repository.findFirstByOrderById();
        }
        if (mergedEntry != null) {
            mergedEntry = mergedEntry.merge(entry);
        } else {
            mergedEntry = entry;
        }
        notificationService.post(new GvDataChangeEvent());
        return repository.saveAndFlush(mergedEntry);
    }

    @Override
    public VelocityConfiguration getVelocityConfiguration() {
        VelocityConfiguration res = repository.findFirstByOrderById();
        em.detach(res);
        return res;
    }
}
