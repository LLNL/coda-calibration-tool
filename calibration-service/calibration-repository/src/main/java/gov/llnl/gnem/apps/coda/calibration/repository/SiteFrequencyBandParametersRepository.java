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

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import io.springlets.data.jpa.repository.DetachableJpaRepository;

@Transactional
public interface SiteFrequencyBandParametersRepository extends DetachableJpaRepository<SiteFrequencyBandParameters, Long> {

    @Query("select distinct sfb.station.stationName from SiteFrequencyBandParameters sfb")
    public List<String> findDistinctStationNames();

    @Query("select sfb from SiteFrequencyBandParameters sfb where sfb.station.networkName = :networkName and sfb.station.stationName = :stationName and sfb.siteTerm = :siteTerm and sfb.lowFrequency = :lowFrequency and sfb.highFrequency = :highFrequency")
    public SiteFrequencyBandParameters findByUniqueFields(@Param("networkName") String networkName, @Param("stationName") String stationName, @Param("siteTerm") double siteTerm,
            @Param("lowFrequency") double lowFrequency, @Param("highFrequency") double highFrequency);
}
