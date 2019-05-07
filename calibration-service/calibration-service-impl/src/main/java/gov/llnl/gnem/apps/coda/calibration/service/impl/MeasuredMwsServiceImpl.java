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
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.MeasuredMwsRepository;
import gov.llnl.gnem.apps.coda.calibration.repository.ReferenceMwParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.common.service.api.WaveformService;

@Service
@Transactional
public class MeasuredMwsServiceImpl implements MeasuredMwsService {

    private MeasuredMwsRepository measuredMwsRepository;
    private ReferenceMwParametersRepository referenceMwsRepository;
    private WaveformService eventRepository;

    @Autowired
    public MeasuredMwsServiceImpl(MeasuredMwsRepository measuredMwsRepository, ReferenceMwParametersRepository referenceMwsRepository, WaveformService eventRepository) {
        this.measuredMwsRepository = measuredMwsRepository;
        this.referenceMwsRepository = referenceMwsRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public void delete(MeasuredMwParameters MeasuredMwParameters) {
        measuredMwsRepository.delete(MeasuredMwParameters);
    }

    @Transactional
    public List<MeasuredMwParameters> save(Iterable<MeasuredMwParameters> entities) {
        return measuredMwsRepository.saveAll(entities);
    }

    @Transactional
    public void delete(Iterable<Long> ids) {
        List<MeasuredMwParameters> toDelete = measuredMwsRepository.findAllById(ids);
        measuredMwsRepository.deleteInBatch(toDelete);
    }

    @Transactional
    public MeasuredMwParameters save(MeasuredMwParameters entity) {
        return measuredMwsRepository.save(entity);
    }

    public MeasuredMwParameters findOne(Long id) {
        return measuredMwsRepository.findOneDetached(id);
    }

    public MeasuredMwParameters findOneForUpdate(Long id) {
        return measuredMwsRepository.findOneDetached(id);
    }

    public List<MeasuredMwParameters> findAll(Iterable<Long> ids) {
        return measuredMwsRepository.findAllById(ids);
    }

    public List<MeasuredMwParameters> findAll() {
        return measuredMwsRepository.findAll();
    }

    public long count() {
        return measuredMwsRepository.count();
    }

    public Class<MeasuredMwParameters> getEntityType() {
        return MeasuredMwParameters.class;
    }

    @Override
    public void deleteAll() {
        measuredMwsRepository.deleteAllInBatch();
    }

    public Class<Long> getIdType() {
        return Long.class;
    }

    @Override
    public List<MeasuredMwDetails> findAllDetails() {
        List<MeasuredMwParameters> measured = measuredMwsRepository.findAll();
        List<ReferenceMwParameters> reference = referenceMwsRepository.findAll();
        List<MeasuredMwDetails> details = measured.stream().map(meas -> {
            ReferenceMwParameters rmw = reference.stream().filter(ref -> ref.getEventId().equals(meas.getEventId())).findAny().orElseGet(() -> null);
            return new MeasuredMwDetails(meas, rmw, eventRepository.findEventById(meas.getEventId()));
        }).collect(Collectors.toList());

        details.addAll(reference.stream()
                                .filter(ref -> measured.stream().noneMatch(meas -> ref.getEventId().equals(meas.getEventId())))
                                .map(ref -> new MeasuredMwDetails(null, ref, eventRepository.findEventById(ref.getEventId())))
                                .collect(Collectors.toList()));

        return details;
    }
}
