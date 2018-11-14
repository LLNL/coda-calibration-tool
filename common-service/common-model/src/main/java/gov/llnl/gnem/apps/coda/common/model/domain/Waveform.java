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
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonManagedReference;

@Entity
@Table(name = "Waveform", indexes = { @Index(columnList = "beginTime", name = "btime_index"), @Index(columnList = "endTime", name = "etime_index"),
        @Index(columnList = "event_id", name = "w_event_id_index"), @Index(columnList = "station_name", name = "w_station_name_index"),
        @Index(columnList = "network_name", name = "w_network_name_index"), @Index(columnList = "lowFrequency", name = "lowFreq_index"), @Index(columnList = "highFrequency", name = "highFreq_index"),
        @Index(columnList = "segmentType", name = "type_index"), @Index(columnList = "segmentUnits", name = "units_index"), @Index(columnList = "sampleRate", name = "rate_index"),
        @Index(columnList = "channelName", name = "channel_name_index") })
public class Waveform {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

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
    private Double[] segment = new Double[0];

    @OneToMany(cascade = { CascadeType.ALL }, orphanRemoval = true, mappedBy = "waveform", targetEntity = WaveformPick.class, fetch = FetchType.EAGER)
    @JsonManagedReference(value = "waveform-picks")
    private List<WaveformPick> associatedPicks = new ArrayList<>();

    public Waveform() {
        //NOP
    }

    public Waveform(WaveformMetadata waveform) {
        this.setId(waveform.getId());
        this.setVersion(waveform.getVersion());
        this.setEvent(waveform.getEvent());
        this.setStream(waveform.getStream());
        this.setBeginTime(waveform.getBeginTime());
        this.setEndTime(waveform.getEndTime());
        this.setSegmentType(waveform.getSegmentType());
        this.setSegmentUnits(waveform.getSegmentUnits());
        this.setSampleRate(waveform.getSampleRate());
        this.setLowFrequency(waveform.getLowFrequency());
        this.setHighFrequency(waveform.getHighFrequency());
        this.setAssociatedPicks(waveform.getAssociatedPicks());
    }

    public Long getId() {
        return id;
    }

    public Waveform setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getVersion() {
        return this.version;
    }

    public Waveform setVersion(Long version) {
        this.version = version;
        return this;
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

    public Double[] getSegment() {
        return segment;
    }

    public Waveform setSegment(Double[] segment) {
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
            this.associatedPicks.clear();
            ;
            this.associatedPicks.addAll(associatedPicks);
            this.associatedPicks.forEach(pick -> pick.setWaveform(this));
        } else {
            this.associatedPicks.clear();
        }
        return this;
    }

    public static Waveform mergeNonNullOrEmptyFields(Waveform waveformOverlay, Waveform baseWaveform) {
        return baseWaveform.mergeNonNullOrEmptyFields(waveformOverlay);
    }

    public Waveform mergeNonNullOrEmptyFields(Waveform waveformOverlay) {
        if (waveformOverlay.getStream() != null) {
            this.setStream(waveformOverlay.getStream());
        }
        if (waveformOverlay.getEvent() != null) {
            this.setEvent(waveformOverlay.getEvent());
        }

        if (!StringUtils.isEmpty(waveformOverlay.getSegmentType())) {
            this.setSegmentType(waveformOverlay.getSegmentType());
        }
        if (!StringUtils.isEmpty(waveformOverlay.getSegmentUnits())) {
            this.setSegmentUnits(waveformOverlay.getSegmentUnits());
        }
        if (waveformOverlay.getSegment() != null) {
            this.setSegment(waveformOverlay.getSegment());
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
        if (waveformOverlay.getAssociatedPicks() != null) {
            this.setAssociatedPicks(waveformOverlay.getAssociatedPicks());
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((beginTime == null) ? 0 : beginTime.hashCode());
        result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
        result = prime * result + ((event == null) ? 0 : event.hashCode());
        result = prime * result + ((highFrequency == null) ? 0 : highFrequency.hashCode());
        result = prime * result + ((lowFrequency == null) ? 0 : lowFrequency.hashCode());
        result = prime * result + ((sampleRate == null) ? 0 : sampleRate.hashCode());
        result = prime * result + Arrays.hashCode(segment);
        result = prime * result + ((segmentType == null) ? 0 : segmentType.hashCode());
        result = prime * result + ((segmentUnits == null) ? 0 : segmentUnits.hashCode());
        result = prime * result + ((stream == null) ? 0 : stream.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Waveform other = (Waveform) obj;
        if (beginTime == null) {
            if (other.beginTime != null) {
                return false;
            }
        } else if (!beginTime.equals(other.beginTime)) {
            return false;
        }
        if (endTime == null) {
            if (other.endTime != null) {
                return false;
            }
        } else if (!endTime.equals(other.endTime)) {
            return false;
        }
        if (event == null) {
            if (other.event != null) {
                return false;
            }
        } else if (!event.equals(other.event)) {
            return false;
        }
        if (highFrequency == null) {
            if (other.highFrequency != null) {
                return false;
            }
        } else if (!highFrequency.equals(other.highFrequency)) {
            return false;
        }
        if (lowFrequency == null) {
            if (other.lowFrequency != null) {
                return false;
            }
        } else if (!lowFrequency.equals(other.lowFrequency)) {
            return false;
        }
        if (sampleRate == null) {
            if (other.sampleRate != null) {
                return false;
            }
        } else if (!sampleRate.equals(other.sampleRate)) {
            return false;
        }
        if (!Arrays.equals(segment, other.segment)) {
            return false;
        }
        if (segmentType == null) {
            if (other.segmentType != null) {
                return false;
            }
        } else if (!segmentType.equals(other.segmentType)) {
            return false;
        }
        if (segmentUnits == null) {
            if (other.segmentUnits != null) {
                return false;
            }
        } else if (!segmentUnits.equals(other.segmentUnits)) {
            return false;
        }
        if (stream == null) {
            if (other.stream != null) {
                return false;
            }
        } else if (!stream.equals(other.stream)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Waveform [id="
                + id
                + ", version="
                + version
                + ", event="
                + event
                + ", stream="
                + stream
                + ", beginTime="
                + beginTime
                + ", endTime="
                + endTime
                + ", segmentType="
                + segmentType
                + ", segmentUnits="
                + segmentUnits
                + ", lowFrequency="
                + lowFrequency
                + ", highFrequency="
                + highFrequency
                + ", sampleRate="
                + sampleRate
                + "]";
    }
}
