/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.Stream;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;

public class WaveformMetadataImpl implements WaveformMetadata {
    private Long id;
    private Integer version = 0;
    private Event event;
    private Stream stream;
    private Date beginTime;
    private Date endTime;
    private Date maxVelTime;
    private Date codaStartTime;
    private Date userStartTime;
    private String segmentType;
    private String segmentUnits;
    private Double lowFrequency;
    private Double highFrequency;
    private Double sampleRate;
    private List<WaveformPick> associatedPicks = new ArrayList<>();
    private Boolean active = Boolean.TRUE;

    public WaveformMetadataImpl() {
    }

    public WaveformMetadataImpl(Waveform waveform) {
        this.id = waveform.getId();
        this.version = waveform.getVersion();
        this.event = waveform.getEvent();
        this.stream = waveform.getStream();
        this.beginTime = waveform.getBeginTime();
        this.endTime = waveform.getEndTime();
        this.codaStartTime = waveform.getCodaStartTime();
        this.userStartTime = waveform.getUserStartTime();
        this.segmentType = waveform.getSegmentType();
        this.segmentUnits = waveform.getSegmentUnits();
        this.lowFrequency = waveform.getLowFrequency();
        this.highFrequency = waveform.getHighFrequency();
        this.sampleRate = waveform.getSampleRate();
        if (waveform.getAssociatedPicks() != null) {
            this.associatedPicks = waveform.getAssociatedPicks().stream().map(wp -> wp.setWaveform(null)).collect(Collectors.toList());
        }
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public Event getEvent() {
        return event;
    }

    @Override
    public Stream getStream() {
        return stream;
    }

    @Override
    public Date getBeginTime() {
        return beginTime;
    }

    @Override
    public Date getEndTime() {
        return endTime;
    }

    @Override
    public Date getMaxVelTime() {
        return maxVelTime;
    }

    @Override
    public Date getCodaStartTime() {
        return codaStartTime;
    }

    @Override
    public Date getUserStartTime() {
        return userStartTime;
    }

    @Override
    public String getSegmentType() {
        return segmentType;
    }

    @Override
    public String getSegmentUnits() {
        return segmentUnits;
    }

    @Override
    public Double getLowFrequency() {
        return lowFrequency;
    }

    @Override
    public Double getHighFrequency() {
        return highFrequency;
    }

    @Override
    public Double getSampleRate() {
        return sampleRate;
    }

    @Override
    public List<WaveformPick> getAssociatedPicks() {
        return associatedPicks;
    }

    @Override
    public Boolean getActive() {
        return active;
    }

    public WaveformMetadataImpl setId(Long id) {
        this.id = id;
        return this;
    }

    public WaveformMetadataImpl setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public WaveformMetadataImpl setEvent(Event event) {
        this.event = event;
        return this;
    }

    public WaveformMetadataImpl setStream(Stream stream) {
        this.stream = stream;
        return this;
    }

    public WaveformMetadataImpl setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
        return this;
    }

    public WaveformMetadataImpl setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public WaveformMetadataImpl setUserStartTime(Date userStartTime) {
        this.userStartTime = userStartTime;
        return this;
    }

    public WaveformMetadataImpl setSegmentType(String segmentType) {
        this.segmentType = segmentType;
        return this;
    }

    public WaveformMetadataImpl setSegmentUnits(String segmentUnits) {
        this.segmentUnits = segmentUnits;
        return this;
    }

    public WaveformMetadataImpl setLowFrequency(Double lowFrequency) {
        this.lowFrequency = lowFrequency;
        return this;
    }

    public WaveformMetadataImpl setHighFrequency(Double highFrequency) {
        this.highFrequency = highFrequency;
        return this;
    }

    public WaveformMetadataImpl setSampleRate(Double sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public WaveformMetadataImpl setAssociatedPicks(List<WaveformPick> associatedPicks) {
        this.associatedPicks = associatedPicks;
        return this;
    }

    public WaveformMetadataImpl setActive(Boolean active) {
        this.active = active;
        return this;
    }

}
