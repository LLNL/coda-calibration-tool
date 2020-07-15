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
package gov.llnl.gnem.apps.coda.calibration.repository;

import java.util.Collection;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.repository.DetachableJpaRepository;

@Transactional
public interface SyntheticRepository extends DetachableJpaRepository<SyntheticCoda, Long> {

    @Query("select synth from SyntheticCoda synth where synth.sourceWaveform.id = :id")
    public SyntheticCoda findByWaveformId(@Param("id") Long id);

    @Query("select synth from SyntheticCoda synth where synth.sourceWaveform.id in :ids")
    public Collection<SyntheticCoda> findByWaveformIds(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("delete from SyntheticCoda synth where synth.sourceModel.id = :id")
    public void deleteBySharedFrequencyBandParametersId(@Param("id") Long id);

    @Modifying
    @Query("delete from SyntheticCoda synth where synth.sourceModel.id in :ids")
    public void deleteInBatchBySharedFrequencyBandParametersIds(@Param("ids") Collection<Long> ids);
}
