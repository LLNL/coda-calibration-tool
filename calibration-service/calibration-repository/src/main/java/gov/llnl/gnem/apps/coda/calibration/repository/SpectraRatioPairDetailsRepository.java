/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.common.repository.DetachableJpaRepository;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;

@Transactional
public interface SpectraRatioPairDetailsRepository extends DetachableJpaRepository<SpectraRatioPairDetails, Long> {

    public List<SpectraRatioPairDetails> findByUserEditedTrue();

    @Modifying
    @Query("delete from SpectraRatioPairDetails ratio where ratio.id NOT IN :ids")
    public void deleteAllNotInIdsList(@Param("ids") List<Long> ids);

    @Query("select ratio from SpectraRatioPairDetails ratio")
    public List<SpectraRatioPairDetailsMetadata> findAllMetdataOnly();

    @Query("select ratio from SpectraRatioPairDetails ratio where ratio.numerWaveform.id = :numerId and ratio.denomWaveform.id = :denomId")
    public SpectraRatioPairDetails findByWaveformIds(@Param("numerId") Long numerId, @Param("denomId") Long denomId);
}
