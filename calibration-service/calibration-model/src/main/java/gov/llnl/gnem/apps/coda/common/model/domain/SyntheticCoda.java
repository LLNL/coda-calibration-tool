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

import java.io.Serializable;
import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "Synthetic_Coda", indexes = { @Index(columnList = "source_waveform_id", name = "source_waveform_id_index") })
public class SyntheticCoda implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @ManyToOne(optional = false)
    private Waveform sourceWaveform;

    @Column(name = "sampleRate")
    @NumberFormat
    private Double sampleRate;

    @Column(name = "segment")
    @NotNull
    @Lob
    @Basic(fetch = FetchType.LAZY)
    private DoubleArrayList segment = new DoubleArrayList(0);

    @Column(name = "beginTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date beginTime;

    @Column(name = "endTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date endTime;

    @ManyToOne(optional = false, cascade = { CascadeType.MERGE })
    private SharedFrequencyBandParameters sourceModel;

    private Double measuredV;

    private Double measuredB;

    private Double measuredG;

    public Long getId() {
        return id;
    }

    public SyntheticCoda setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Waveform getSourceWaveform() {
        return sourceWaveform;
    }

    public SyntheticCoda setSourceWaveform(Waveform sourceWaveform) {
        this.sourceWaveform = sourceWaveform;
        return this;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public Double getSampleRate() {
        return sampleRate;
    }

    public SyntheticCoda setSampleRate(double sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    public double[] getSegment() {
        return segment.toArray();
    }

    public SyntheticCoda setSegment(double[] segment) {
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
    public SyntheticCoda setData(DoubleArrayList segment) {
        this.segment = segment;
        return this;
    }

    public Date getBeginTime() {
        return beginTime;
    }

    public SyntheticCoda setBeginTime(Date beginTime) {
        this.beginTime = beginTime;
        return this;
    }

    public Date getEndTime() {
        return endTime;
    }

    public SyntheticCoda setEndTime(Date endTime) {
        this.endTime = endTime;
        return this;
    }

    public SharedFrequencyBandParameters getSourceModel() {
        return sourceModel;
    }

    public SyntheticCoda setSourceModel(SharedFrequencyBandParameters sourceModel) {
        this.sourceModel = sourceModel;
        return this;
    }

    public Double getMeasuredV() {
        return measuredV;
    }

    public SyntheticCoda setMeasuredV(Double measuredV) {
        this.measuredV = measuredV;
        return this;
    }

    public Double getMeasuredB() {
        return measuredB;
    }

    public SyntheticCoda setMeasuredB(Double measuredB) {
        this.measuredB = measuredB;
        return this;
    }

    public Double getMeasuredG() {
        return measuredG;
    }

    public SyntheticCoda setMeasuredG(Double measuredG) {
        this.measuredG = measuredG;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(beginTime, endTime, id, measuredB, measuredG, measuredV, sampleRate, segment, sourceModel, sourceWaveform, version);
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
        SyntheticCoda other = (SyntheticCoda) obj;
        if (!Objects.equals(beginTime, other.beginTime)) {
            return false;
        }
        if (!Objects.equals(endTime, other.endTime)) {
            return false;
        }
        if (!Objects.equals(id, other.id)) {
            return false;
        }
        if (!Objects.equals(measuredB, other.measuredB)) {
            return false;
        }
        if (!Objects.equals(measuredG, other.measuredG)) {
            return false;
        }
        if (!Objects.equals(measuredV, other.measuredV)) {
            return false;
        }
        if (!Objects.equals(sampleRate, other.sampleRate)) {
            return false;
        }
        if (!Objects.equals(segment, other.segment)) {
            return false;
        }
        if (!Objects.equals(sourceModel, other.sourceModel)) {
            return false;
        }
        if (!Objects.equals(sourceWaveform, other.sourceWaveform)) {
            return false;
        }
        if (!Objects.equals(version, other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SyntheticCoda [id="
                + id
                + ", version="
                + version
                + ", sourceWaveform="
                + sourceWaveform
                + ", sampleRate="
                + sampleRate
                + ", beginTime="
                + beginTime
                + ", endTime="
                + endTime
                + ", sourceModel="
                + sourceModel
                + ", measuredV="
                + measuredV
                + ", measuredB="
                + measuredB
                + ", measuredG="
                + measuredG
                + "]";
    }

    public SyntheticCoda mergeNonNullOrEmptyFields(SyntheticCoda overlay) {

        if (overlay.getBeginTime() != null) {
            this.setBeginTime(overlay.getBeginTime());
        }
        if (overlay.getEndTime() != null) {
            this.setEndTime(overlay.getEndTime());
        }

        if (overlay.hasData()) {
            this.setData(overlay.getData());
        }

        if (overlay.getSampleRate() != null) {
            this.setSampleRate(overlay.getSampleRate());
        }

        if (overlay.getSourceWaveform() != null) {
            this.setSourceWaveform(overlay.getSourceWaveform());
        }

        if (overlay.getMeasuredV() != null) {
            this.setMeasuredV(overlay.getMeasuredV());
        }

        if (overlay.getMeasuredB() != null) {
            this.setMeasuredB(overlay.getMeasuredB());
        }

        if (overlay.getMeasuredG() != null) {
            this.setMeasuredG(overlay.getMeasuredG());
        }

        return this;
    }

}
