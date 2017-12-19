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
package gov.llnl.gnem.apps.coda.calibration.repository;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.repository.support.JpaEntityInformation;

import io.springlets.data.jpa.repository.support.DetachableJpaRepositoryImpl;

/**
 * <p>
 * This is a quick hack to the existing DetachableJpaRepositoryImpl to correctly
 * implement the findOneDetached with the new findById API changes in Spring
 * JPA. It should be removed ASAP when the API has settled.
 * </p>
 * 
 * @see DetachableJpaRepositoryImpl
 */
public class DetachableJpaRepoImpl<T, ID extends Serializable> extends DetachableJpaRepositoryImpl<T, ID> {
    private EntityManager entityManager;

    public DetachableJpaRepoImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
    }

    public DetachableJpaRepoImpl(Class<T> domainClass, EntityManager em) {
        super(domainClass, em);
        this.entityManager = em;
    }

    @Override
    public T findOneDetached(ID id) {
        T entity = findById(id).orElse(null);
        if (entity != null) {
            entityManager.detach(entity);
        }
        return entity;
    }

}
