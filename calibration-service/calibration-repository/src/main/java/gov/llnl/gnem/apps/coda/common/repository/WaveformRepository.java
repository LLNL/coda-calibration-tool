/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.util.Set;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;

@Transactional
public interface WaveformRepository extends DetachableJpaRepository<Waveform, Long> {

    @Query("select w from Waveform w where w.event.eventId = :eventId and w.stream.station.networkName = :networkName and w.stream.station.stationName = :stationName and w.lowFrequency = :lowFrequency and w.highFrequency = :highFrequency order by w.id desc")
    public Waveform findByUniqueFields(@Param("eventId") String eventId, @Param("networkName") String networkName, @Param("stationName") String stationName, @Param("lowFrequency") Double lowFrequency,
            @Param("highFrequency") Double highFrequency);

    @Query("select w from Waveform w where w.beginTime = :beginTime and w.endTime = :endTime and w.event = :event and w.stream = :stream and w.segmentType = :segmentType and w.segmentUnits = :segmentUnits and w.lowFrequency = :lowFrequency and w.highFrequency = :highFrequency order by w.id desc")
    public Waveform findOneByAllFields(@Param("beginTime") Date beginTime, @Param("endTime") Date endTime, @Param("event") Event event, @Param("stream") Stream stream,
            @Param("segmentType") String segmentType, @Param("segmentUnits") String segmentUnits, @Param("lowFrequency") Double lowFrequency, @Param("highFrequency") Double highFrequency);

    @Query("select new Waveform(w.id, w.version, w.event, w.stream, w.beginTime, w.endTime, w.maxVelTime, w.segmentType, w.segmentUnits, w.lowFrequency, w.highFrequency, w.sampleRate, w.active) from Waveform w order by w.id desc")
    public Set<Waveform> getWaveformMetadata();

    @Query("select new Waveform(w.id, w.version, w.event, w.stream, w.beginTime, w.endTime, w.maxVelTime, w.segmentType, w.segmentUnits, w.lowFrequency, w.highFrequency, w.sampleRate, w.active) from Waveform w where w.active = :active order by w.id desc")
    public List<Waveform> getWaveformMetadataByActive(@Param("active") boolean active);

    @Query("select new Waveform(w.id, w.version, w.event, w.stream, w.beginTime, w.endTime, w.maxVelTime, w.segmentType, w.segmentUnits, w.lowFrequency, w.highFrequency, w.sampleRate, w.active) from Waveform w where w.id in :ids order by w.id desc")
    public List<Waveform> findAllMetadataByIds(@Param("ids") List<Long> ids);

    @Query("select new Waveform(w.id, w.version, w.event, w.stream, w.beginTime, w.endTime, w.maxVelTime, w.segmentType, w.segmentUnits, w.lowFrequency, w.highFrequency, w.sampleRate, w.active) from Waveform w where w.id = :id order by w.id desc")
    public Waveform findWaveformMetadataById(@Param("id") Long id);

    @Query("select p from WaveformPick p where p.waveform.id = :id")
    public List<WaveformPick> findPicksByWaveformId(@Param("id") Long id);

    @Query("select w.event from Waveform w where w.event.eventId = :eventId order by w.id desc")
    public List<Event> findEventById(@Param("eventId") String eventId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Waveform w SET w.active = :active where w.event.eventId = :eventId")
    public int setActiveByEventId(@Param("eventId") String eventId, @Param("active") boolean active);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Waveform w SET w.active = :active where w.stream.station.stationName = :stationName")
    public int setActiveByStationName(@Param("stationName") String stationName, @Param("active") boolean active);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Waveform w SET w.active = :active")
    public void setAllActive(boolean active);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Waveform w SET w.active = :active where w.id in (:ids)")
    public int setActiveIn(@Param("active") boolean active, @Param("ids") List<Long> waveformIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Waveform w SET w.active = :active where w.id not in (:ids)")
    public int setActiveNotIn(@Param("active") boolean active, @Param("ids") List<Long> waveformIds);

    @Query("select new Waveform(w.id, w.version, w.event, w.stream, w.beginTime, w.endTime, w.maxVelTime, w.segmentType, w.segmentUnits, w.lowFrequency, w.highFrequency, w.sampleRate, w.active) from Waveform w "
            + "where w.active = :active and"
            + "(w.stream.station.latitude between :minX and :maxX "
            + "and w.stream.station.longitude between :minY and :maxY) "
            + "or (w.event.latitude between :minX and :maxX "
            + "and w.event.longitude between :minY and :maxY)")
    public List<Waveform> getMetadataInsideBounds(@Param("active") boolean active, @Param("minX") Double minX, @Param("minY") Double minY, @Param("maxX") Double maxX, @Param("maxY") Double maxY);

    @Query("select w.id from Waveform w where w.event.eventId = :eventId")
    public List<Long> findAllIdsByEventId(@Param("eventId") String eventId);

    @Query("select w.id from Waveform w where w.stream.station.stationName = :stationName")
    public List<Long> findAllIdsByStationName(@Param("stationName") String stationName);

    @Query("select w from Waveform w where w.active = true and w.stream.channelName = 'STACK' and w.stream.station.stationName in :stationNames")
    public List<Waveform> findAllActiveStacksByStationNames(@Param("stationNames") List<String> stationNames);

    @Query("select w from Waveform w where w.active = true and w.event.eventId = :eventId and w.stream.channelName = 'STACK' and w.stream.station.stationName in :stationNames")
    public List<Waveform> findAllActiveStacksByEventIdAndStationNames(@Param("eventId") String eventId, @Param("stationNames") List<String> stationNames);

    @Query("select w.id from Waveform w where w.active = false")
    public List<Long> findAllInactiveIds();

    @Query("select w.id from Waveform w where w.id not in (:ids)")
    public List<Long> findIdsNotIn(@Param("ids") List<Long> ids);

    @Query("select w from Waveform w where w.active = true and w.stream.channelName = 'STACK' "
            + "and w.event.eventId in (select ev.event.eventId from Waveform ev where ev.id = :id) "
            + "and w.stream.station.stationName in (select sta.stream.station.stationName from Waveform sta where sta.id = :id)")
    public List<Waveform> findAllActiveSharedEventStationStacksById(@Param("id") Long id);
}
