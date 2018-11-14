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
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.metamodel.EntityType;
import javax.persistence.metamodel.Type.PersistenceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.service.api.DatabaseCleaningService;
import gov.llnl.gnem.apps.coda.common.model.util.Durable;

@Service
public class DatabaseCleaningServiceImpl implements DatabaseCleaningService {

    private EntityManager entityManager;

    @Autowired
    public DatabaseCleaningServiceImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public boolean clearAll() {
        try {
            List<String> tableNames = new ArrayList<>();
            for (EntityType<?> entity : entityManager.getMetamodel().getEntities()) {
                if (entity.getPersistenceType() == PersistenceType.ENTITY) {
                    Durable durableAnnotation = entity.getJavaType().getAnnotation(Durable.class);
                    Table tableAnnotation = entity.getJavaType().getAnnotation(Table.class);
                    if (tableAnnotation != null && durableAnnotation == null) {
                        tableNames.add(tableAnnotation.name());
                    }
                }
            }

            entityManager.flush();
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
            tableNames.forEach(tableName -> entityManager.createNativeQuery("TRUNCATE TABLE " + tableName).executeUpdate());
            entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
