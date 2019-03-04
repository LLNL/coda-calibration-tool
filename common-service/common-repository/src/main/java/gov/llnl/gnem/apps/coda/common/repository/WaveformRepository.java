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
package gov.llnl.gnem.apps.coda.common.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import io.springlets.data.jpa.repository.DetachableJpaRepository;

@Transactional
public interface WaveformRepository extends DetachableJpaRepository<Waveform, Long> {

    @Query("select w from Waveform w where w.beginTime = :beginTime and w.endTime = :endTime and w.event = :event and w.stream = :stream and w.segmentType = :segmentType and w.segmentUnits = :segmentUnits and w.lowFrequency = :lowFrequency and w.highFrequency = :highFrequency")
    public Waveform findOneByAllFields(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime, @Param("event") Event event, @Param("stream") Stream stream,
            @Param("segmentType") String segmentType, @Param("segmentUnits") String segmentUnits, @Param("lowFrequency") Double lowFrequency, @Param("highFrequency") Double highFrequency);

    @Query("select new Waveform(w.id, w.version, w.event, w.stream, w.beginTime, w.endTime, w.segmentType, w.segmentUnits, w.lowFrequency, w.highFrequency, w.sampleRate, w.active) from Waveform w")
    public List<Waveform> getWaveformMetadata();

    @Query("select p from WaveformPick p where p.waveform.id = :id")
    public List<WaveformPick> findPicksByWaveformId(@Param("id") Long id);

    @Query("select w.event from Waveform w where w.event.eventId = :eventId order by w.id desc")
    public List<Event> findEventById(@Param("eventId") String eventId, Pageable pageable);
}
