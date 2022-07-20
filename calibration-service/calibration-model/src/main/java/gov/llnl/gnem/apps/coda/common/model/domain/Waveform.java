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
package gov.llnl.gnem.apps.coda.common.model.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

@Entity
@Table(name = "Waveform", indexes = { @Index(columnList = "beginTime", name = "btime_index"), @Index(columnList = "endTime", name = "etime_index"),
        @Index(columnList = "codaStartTime", name = "maxVel_index"), @Index(columnList = "event_id", name = "w_event_id_index"), @Index(columnList = "station_name", name = "w_station_name_index"),
        @Index(columnList = "network_name", name = "w_network_name_index"), @Index(columnList = "lowFrequency", name = "lowFreq_index"), @Index(columnList = "highFrequency", name = "highFreq_index"),
        @Index(columnList = "segmentType", name = "type_index"), @Index(columnList = "segmentUnits", name = "units_index"), @Index(columnList = "sampleRate", name = "rate_index"),
        @Index(columnList = "channelName", name = "channel_name_index"), @Index(columnList = "active", name = "active") })
public class Waveform {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @Embedded
    private Event event;

    @Embedded
    private Stream stream;

    @Column(name = "beginTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date beginTime;

    @Column(name = "endTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date endTime;

    @Column(name = "maxVelTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date maxVelTime;

    @Column(name = "codaStartTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date codaStartTime;

    @Column(name = "userStartTime", nullable = true)
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date userStartTime;

    @Column(name = "segmentType")
    private String segmentType;

    @Column(name = "segmentUnits")
    private String segmentUnits;

    @Column(name = "lowFrequency")
    @NumberFormat
    private Double lowFrequency;

    @Column(name = "highFrequency")
    @NumberFormat
    private Double highFrequency;

    @Column(name = "sampleRate")
    @NumberFormat
    private Double sampleRate;

    @Column(name = "segment")
    @NotNull
    @Lob
    @Basic
    private DoubleArrayList segment = new DoubleArrayList(0);

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true, mappedBy = "waveform", targetEntity = WaveformPick.class, fetch = FetchType.EAGER)
    @JsonManagedReference(value = "waveform-picks")
    private List<WaveformPick> associatedPicks = new ArrayList<>();

    @Column(name = "active")
    private Boolean active = Boolean.TRUE;

    public Waveform() {
        //NOP
    }

    public Waveform(WaveformMetadata waveform) {
        this.setId(waveform.getId());
        this.version = waveform.getVersion();
        this.setEvent(waveform.getEvent());
        this.setStream(waveform.getStream());
        this.setBeginTime(waveform.getBeginTime());
        this.setEndTime(waveform.getEndTime());
        this.setMaxVelTime(waveform.getMaxVelTime());
        this.setCodaStartTime(waveform.getCodaStartTime());
        this.setUserStartTime(waveform.getUserStartTime());
        this.setSegmentType(waveform.getSegmentType());
        this.setSegmentUnits(waveform.getSegmentUnits());
        this.setSampleRate(waveform.getSampleRate());
        this.setLowFrequency(waveform.getLowFrequency());
        this.setHighFrequency(waveform.getHighFrequency());
        this.setAssociatedPicks(waveform.getAssociatedPicks());
        this.setActive(waveform.getActive());
    }

    public Waveform(Long id, Integer version, Event event, Stream stream, Date beginTime, Date endTime, Date maxVelTime, Date codaStartTime, Date userStartTime, String segmentType,
            String segmentUnits, Double lowFrequency, Double highFrequency, Double sampleRate, Boolean active) {
        this.id = id;
        this.version = version;
        this.event = event;
        this.stream = stream;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.maxVelTime = maxVelTime;
        this.codaStartTime = codaStartTime;
        this.userStartTime = userStartTime;
        this.segmentType = segmentType;
        this.segmentUnits = segmentUnits;
        this.lowFrequency = lowFrequency;
        this.highFrequency = highFrequency;
        this.sampleRate = sampleRate;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public Waveform setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Event getEvent() {
        return event;
    }

    public Waveform setEvent(Event event) {
        this.event = event;
        return this;
    }

    public Date getBeginTime() {
        return this.beginTime;
    }

    public Waveform setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
        return this;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    public Waveform setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public Date getMaxVelTime() {
        return maxVelTime;
    }

    public Waveform setMaxVelTime(Date maxVelTime) {
        this.maxVelTime = maxVelTime;
        return this;
    }

    public Date getCodaStartTime() {
        return codaStartTime;
    }

    public Waveform setCodaStartTime(Date codaStartTime) {
        this.codaStartTime = codaStartTime;
        return this;
    }

    public Date getUserStartTime() {
        return userStartTime;
    }

    public Waveform setUserStartTime(Date userStartTime) {
        this.userStartTime = userStartTime;
        return this;
    }

    public String getSegmentType() {
        return segmentType;
    }

    public Waveform setSegmentType(String segmentType) {
        this.segmentType = segmentType;
        return this;
    }

    public String getSegmentUnits() {
        return segmentUnits;
    }

    public Waveform setSegmentUnits(String segmentUnits) {
        this.segmentUnits = segmentUnits;
        return this;
    }

    public Double getLowFrequency() {
        return lowFrequency;
    }

    public Waveform setLowFrequency(Double lowFrequency) {
        this.lowFrequency = lowFrequency;
        return this;
    }

    public Double getHighFrequency() {
        return highFrequency;
    }

    public Waveform setHighFrequency(Double highFrequency) {
        this.highFrequency = highFrequency;
        return this;
    }

    public double[] getSegment() {
        return segment.toArray();
    }

    public Waveform setSegment(double[] segment) {
        this.segment = new DoubleArrayList(segment);
        return this;
    }

    @JsonIgnore
    public int getSegmentLength() {
        int len = 0;
        if (hasData()) {
            len = segment.size();
        }
        return len;
    }

    @JsonIgnore
    public boolean hasData() {
        return segment != null;
    }

    @JsonIgnore
    public DoubleArrayList getData() {
        return segment;
    }

    @JsonIgnore
    public Waveform setData(DoubleArrayList segment) {
        this.segment = segment;
        return this;
    }

    public Stream getStream() {
        return stream;
    }

    public Waveform setStream(Stream stream) {
        this.stream = stream;
        return this;
    }

    public Double getSampleRate() {
        return sampleRate;
    }

    public Waveform setSampleRate(Double sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public List<WaveformPick> getAssociatedPicks() {
        return associatedPicks;
    }

    public Waveform setAssociatedPicks(List<WaveformPick> associatedPicks) {
        if (associatedPicks != null) {
            associatedPicks.forEach(pick -> {
                int idx = this.associatedPicks.indexOf(pick);
                if (idx >= 0) {
                    this.associatedPicks.get(idx).mergeNonNullOrEmptyFields(pick);
                } else {
                    this.associatedPicks.add(pick);
                }
                pick.setWaveform(this);
            });

            //Remove picks not on the waveform anymore
            List<WaveformPick> shouldRemove = this.associatedPicks.stream().filter(pick -> associatedPicks.indexOf(pick) < 0).collect(Collectors.toList());
            this.associatedPicks.removeAll(shouldRemove);
        } else {
            this.associatedPicks.clear();
        }
        return this;
    }

    public Boolean isActive() {
        return active;
    }

    public Boolean getActive() {
        return active;
    }

    public Waveform setActive(Boolean active) {
        this.active = active;
        return this;
    }

    public static Waveform mergeNonNullOrEmptyFields(Waveform waveformOverlay, Waveform baseWaveform) {
        return baseWaveform.mergeNonNullOrEmptyFields(waveformOverlay);
    }

    public Waveform mergeNonNullOrEmptyFields(Waveform waveformOverlay) {
        if (waveformOverlay.getEvent() != null) {
            this.setEvent(waveformOverlay.getEvent());
        }
        if (waveformOverlay.getStream() != null) {
            this.setStream(waveformOverlay.getStream());
        }
        if (waveformOverlay.getLowFrequency() != null) {
            this.setLowFrequency(waveformOverlay.getLowFrequency());
        }
        if (waveformOverlay.getHighFrequency() != null) {
            this.setHighFrequency(waveformOverlay.getHighFrequency());
        }

        if (waveformOverlay.hasData()) {
            this.setData(waveformOverlay.getData());
        }
        if (StringUtils.hasText(waveformOverlay.getSegmentType())) {
            this.setSegmentType(waveformOverlay.getSegmentType());
        }
        if (StringUtils.hasText(waveformOverlay.getSegmentUnits())) {
            this.setSegmentUnits(waveformOverlay.getSegmentUnits());
        }
        if (waveformOverlay.getSampleRate() != null) {
            this.setSampleRate(waveformOverlay.getSampleRate());
        }
        if (waveformOverlay.getBeginTime() != null) {
            this.setBeginTime(waveformOverlay.getBeginTime());
        }
        if (waveformOverlay.getEndTime() != null) {
            this.setEndTime(waveformOverlay.getEndTime());
        }
        if (waveformOverlay.getMaxVelTime() != null) {
            this.setMaxVelTime(waveformOverlay.getMaxVelTime());
        }
        if (waveformOverlay.getCodaStartTime() != null) {
            this.setCodaStartTime(waveformOverlay.getCodaStartTime());
        }

        //Null is a valid value for UserStartTime so just set that directly
        this.setUserStartTime(waveformOverlay.getUserStartTime());

        if (waveformOverlay.getAssociatedPicks() != null && !waveformOverlay.getAssociatedPicks().isEmpty()) {
            //Merge picks
            Map<String, WaveformPick> picksByName = this.getAssociatedPicks().stream().collect(Collectors.toMap(WaveformPick::getPickName, Function.identity()));
            waveformOverlay.getAssociatedPicks().stream().forEach(p -> {
                WaveformPick managedPick = picksByName.get(p.getPickName());
                if (managedPick != null) {
                    managedPick.mergeNonNullOrEmptyFields(p);
                } else {
                    picksByName.put(p.getPickName(), p);
                }
            });
            if (picksByName.containsKey(PICK_TYPES.CS.getPhase()) && picksByName.containsKey(PICK_TYPES.UCS.getPhase())) {
                WaveformPick p = picksByName.remove(PICK_TYPES.CS.getPhase());
                p.setWaveform(null);
            }
            if (!waveformOverlay.getAssociatedPicks().stream().anyMatch(p -> PICK_TYPES.UCS.getPhase().equals(p.getPickName()))) {
                WaveformPick p = picksByName.remove(PICK_TYPES.UCS.getPhase());
                if (p != null) {
                    p.setWaveform(null);
                }
            }
            this.setAssociatedPicks(new ArrayList<>(picksByName.values()));
        }
        if (waveformOverlay.isActive() != null) {
            this.setActive(waveformOverlay.isActive());
        }
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                active,
                    beginTime,
                    codaStartTime,
                    endTime,
                    event,
                    highFrequency,
                    id,
                    lowFrequency,
                    maxVelTime,
                    sampleRate,
                    segment,
                    segmentType,
                    segmentUnits,
                    stream,
                    userStartTime,
                    version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Waveform)) {
            return false;
        }
        Waveform other = (Waveform) obj;
        return Objects.equals(active, other.active)
                && Objects.equals(beginTime, other.beginTime)
                && Objects.equals(codaStartTime, other.codaStartTime)
                && Objects.equals(endTime, other.endTime)
                && Objects.equals(event, other.event)
                && Objects.equals(highFrequency, other.highFrequency)
                && Objects.equals(id, other.id)
                && Objects.equals(lowFrequency, other.lowFrequency)
                && Objects.equals(maxVelTime, other.maxVelTime)
                && Objects.equals(sampleRate, other.sampleRate)
                && Objects.equals(segment, other.segment)
                && Objects.equals(segmentType, other.segmentType)
                && Objects.equals(segmentUnits, other.segmentUnits)
                && Objects.equals(stream, other.stream)
                && Objects.equals(userStartTime, other.userStartTime)
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        StringBuilder builder = new StringBuilder();
        builder.append("Waveform [id=")
               .append(id)
               .append(", version=")
               .append(version)
               .append(", event=")
               .append(event)
               .append(", stream=")
               .append(stream)
               .append(", beginTime=")
               .append(beginTime)
               .append(", endTime=")
               .append(endTime)
               .append(", maxVelTime=")
               .append(maxVelTime)
               .append(", codaStartTime=")
               .append(codaStartTime)
               .append(", userStartTime=")
               .append(userStartTime)
               .append(", segmentType=")
               .append(segmentType)
               .append(", segmentUnits=")
               .append(segmentUnits)
               .append(", lowFrequency=")
               .append(lowFrequency)
               .append(", highFrequency=")
               .append(highFrequency)
               .append(", sampleRate=")
               .append(sampleRate)
               .append(", segment=")
               .append(segment)
               .append(", associatedPicks=")
               .append(associatedPicks != null ? associatedPicks.subList(0, Math.min(associatedPicks.size(), maxLen)) : null)
               .append(", active=")
               .append(active)
               .append("]");
        return builder.toString();
    }

}
