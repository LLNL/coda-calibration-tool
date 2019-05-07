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

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;

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
        final int prime = 31;
        int result = 1;
        result = prime * result + ((beginTime == null) ? 0 : beginTime.hashCode());
        result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((measuredB == null) ? 0 : measuredB.hashCode());
        result = prime * result + ((measuredG == null) ? 0 : measuredG.hashCode());
        result = prime * result + ((measuredV == null) ? 0 : measuredV.hashCode());
        result = prime * result + ((sampleRate == null) ? 0 : sampleRate.hashCode());
        result = prime * result + ((segment == null) ? 0 : segment.hashCode());
        result = prime * result + ((sourceModel == null) ? 0 : sourceModel.hashCode());
        result = prime * result + ((sourceWaveform == null) ? 0 : sourceWaveform.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        SyntheticCoda other = (SyntheticCoda) obj;
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
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (measuredB == null) {
            if (other.measuredB != null) {
                return false;
            }
        } else if (!measuredB.equals(other.measuredB)) {
            return false;
        }
        if (measuredG == null) {
            if (other.measuredG != null) {
                return false;
            }
        } else if (!measuredG.equals(other.measuredG)) {
            return false;
        }
        if (measuredV == null) {
            if (other.measuredV != null) {
                return false;
            }
        } else if (!measuredV.equals(other.measuredV)) {
            return false;
        }
        if (sampleRate == null) {
            if (other.sampleRate != null) {
                return false;
            }
        } else if (!sampleRate.equals(other.sampleRate)) {
            return false;
        }
        if (segment == null) {
            if (other.segment != null) {
                return false;
            }
        } else if (!segment.equals(other.segment)) {
            return false;
        }
        if (sourceModel == null) {
            if (other.sourceModel != null) {
                return false;
            }
        } else if (!sourceModel.equals(other.sourceModel)) {
            return false;
        }
        if (sourceWaveform == null) {
            if (other.sourceWaveform != null) {
                return false;
            }
        } else if (!sourceWaveform.equals(other.sourceWaveform)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
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

        if (overlay.getSegment() != null) {
            this.setSegment(overlay.getSegment());
        }

        if (overlay.getSampleRate() != null) {
            this.setSampleRate(overlay.getSampleRate());
        }

        if (overlay.getSourceWaveform() != null) {
            this.setSourceWaveform(overlay.getSourceWaveform());
        }

        return this;
    }

}
