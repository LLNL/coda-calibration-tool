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
package gov.llnl.gnem.apps.coda.spectra.model.domain;

import java.util.Date;
import java.util.Objects;

import jakarta.persistence.Basic;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

import org.eclipse.collections.impl.list.mutable.primitive.DoubleArrayList;
import org.springframework.format.annotation.NumberFormat;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

@Entity
@Table(name = "Spectra_Ratio_Pair_Details", indexes = { @Index(name = "denomWaveform.id_index", columnList = "id"), @Index(name = "numerWaveform.id_index", columnList = "id") })
public class SpectraRatioPairDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    // The ratio value
    @Column
    @NumberFormat
    private Double diffAvg;
    @Column
    @NumberFormat
    private Double numerAvg;
    @Column
    @NumberFormat
    private Double denomAvg;

    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    private Waveform numerWaveform;
    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    private Waveform denomWaveform;

    @Column
    private boolean loadedFromJson;

    @Column
    @NumberFormat
    private int cutSegmentLength;

    @Column
    @NumberFormat
    private double cutTimeLength;

    @Column
    @NotNull
    @Lob
    @Basic
    private DoubleArrayList diffSegment;

    @Column
    @NumberFormat
    private Double numerWaveStartSec;
    @Column
    @NumberFormat
    private Double denomWaveStartSec;
    @Column
    @NumberFormat
    private Double numerWaveEndSec;
    @Column
    @NumberFormat
    private Double denomWaveEndSec;
    @Column
    @NumberFormat
    private Double numerPeakSec;
    @Column
    @NumberFormat
    private Double denomPeakSec;
    @Column
    @NumberFormat
    private Double numerFMarkerSec;
    @Column
    @NumberFormat
    private Double denomFMarkerSec;
    @Column
    @NumberFormat
    private Double numerStartCutSec;
    @Column
    @NumberFormat
    private Double denomStartCutSec;
    @Column
    @NumberFormat
    private Double numerEndCutSec;
    @Column
    @NumberFormat
    private Double denomEndCutSec;
    @Column
    @NumberFormat
    private int numerStartCutIdx;
    @Column
    @NumberFormat
    private int denomStartCutIdx;
    @Column
    @NumberFormat
    private int numerEndCutIdx;
    @Column
    @NumberFormat
    private int denomEndCutIdx;
    @Column
    private boolean userEdited;

    public SpectraRatioPairDetails() {
        this.diffAvg = null;
        this.numerAvg = null;
        this.denomAvg = null;
        this.numerWaveform = null;
        this.denomWaveform = null;
        this.diffSegment = null;
        this.numerWaveStartSec = null;
        this.denomWaveStartSec = null;
        this.numerWaveEndSec = null;
        this.denomWaveEndSec = null;
        this.numerStartCutSec = null;
        this.denomStartCutSec = null;
        this.numerEndCutSec = null;
        this.denomEndCutSec = null;
        this.numerStartCutIdx = 0;
        this.denomStartCutIdx = 0;
        this.numerEndCutIdx = 0;
        this.denomEndCutIdx = 0;
        this.cutSegmentLength = 0;
        this.cutTimeLength = 0.0;
        this.loadedFromJson = false;
        this.userEdited = false;
    }

    public SpectraRatioPairDetails(SpectraRatioPairDetails ratioDetails) {
        this.id = ratioDetails.id;
        this.version = ratioDetails.version;
        this.diffAvg = ratioDetails.diffAvg;
        this.numerAvg = ratioDetails.numerAvg;
        this.denomAvg = ratioDetails.denomAvg;
        this.numerWaveform = ratioDetails.numerWaveform;
        this.denomWaveform = ratioDetails.denomWaveform;
        this.diffSegment = ratioDetails.diffSegment;
        this.numerWaveStartSec = ratioDetails.numerWaveStartSec;
        this.denomWaveStartSec = ratioDetails.denomWaveStartSec;
        this.numerWaveEndSec = ratioDetails.numerWaveEndSec;
        this.denomWaveEndSec = ratioDetails.denomWaveEndSec;
        this.numerStartCutSec = ratioDetails.numerStartCutSec;
        this.denomStartCutSec = ratioDetails.denomStartCutSec;
        this.numerEndCutSec = ratioDetails.numerEndCutSec;
        this.denomEndCutSec = ratioDetails.denomEndCutSec;
        this.numerStartCutIdx = ratioDetails.numerStartCutIdx;
        this.denomStartCutIdx = ratioDetails.denomStartCutIdx;
        this.numerEndCutIdx = ratioDetails.numerEndCutIdx;
        this.denomEndCutIdx = ratioDetails.denomEndCutIdx;
        this.cutSegmentLength = ratioDetails.cutSegmentLength;
        this.cutTimeLength = ratioDetails.cutTimeLength;
        this.loadedFromJson = ratioDetails.loadedFromJson;
        this.userEdited = ratioDetails.userEdited;
    }

    public SpectraRatioPairDetails(Waveform numeratorWaveform, Waveform denominatorWaveform) {
        this.diffAvg = null;
        this.denomAvg = null;
        this.numerAvg = null;
        this.numerWaveform = numeratorWaveform;
        this.denomWaveform = denominatorWaveform;
        this.diffSegment = null;
        this.numerStartCutSec = null;
        this.denomStartCutSec = null;
        this.numerEndCutSec = null;
        this.denomEndCutSec = null;
        this.numerStartCutIdx = 0;
        this.denomStartCutIdx = 0;
        this.numerEndCutIdx = 0;
        this.denomEndCutIdx = 0;
        this.cutSegmentLength = 0;
        this.cutTimeLength = 0.0;
        if (numeratorWaveform != null && denominatorWaveform != null) {
            this.numerWaveStartSec = getTimeMinusOriginSec(numeratorWaveform.getBeginTime(), true);
            this.denomWaveStartSec = getTimeMinusOriginSec(denominatorWaveform.getBeginTime(), false);
            this.numerWaveEndSec = getTimeMinusOriginSec(numeratorWaveform.getEndTime(), true);
            this.denomWaveEndSec = getTimeMinusOriginSec(denominatorWaveform.getEndTime(), false);
        } else {
            this.numerWaveStartSec = null;
            this.denomWaveStartSec = null;
            this.numerWaveEndSec = null;
            this.denomWaveEndSec = null;
        }
        this.loadedFromJson = false;
        this.userEdited = false;
    }

    private double getTimeMinusOriginSec(Date time, boolean isNumerator) {
        double newTime = 0.0;
        if (isNumerator && this.numerWaveform != null) {
            newTime = (time.getTime() - this.numerWaveform.getEvent().getOriginTime().getTime());
        } else if (!isNumerator && this.denomWaveform != null) {
            newTime = (time.getTime() - this.denomWaveform.getEvent().getOriginTime().getTime());
        }

        return newTime / 1000.0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Double getDiffAvg() {
        return diffAvg;
    }

    public void setDiffAvg(Double diffAvg) {
        this.diffAvg = diffAvg;
    }

    public boolean isLoadedFromJson() {
        return loadedFromJson;
    }

    public void setLoadedFromJson(boolean loadedFromJson) {
        this.loadedFromJson = loadedFromJson;
    }

    public Double getNumerAvg() {
        return numerAvg;
    }

    public void setNumerAvg(Double numerAvg) {
        this.numerAvg = numerAvg;
    }

    public Double getDenomAvg() {
        return denomAvg;
    }

    public void setDenomAvg(Double denomAvg) {
        this.denomAvg = denomAvg;
    }

    public Waveform getNumerWaveform() {
        return numerWaveform;
    }

    public void setNumerWaveform(Waveform numerWaveform) {
        this.numerWaveform = numerWaveform;
    }

    public Waveform getDenomWaveform() {
        return denomWaveform;
    }

    public void setDenomWaveform(Waveform denomWaveform) {
        this.denomWaveform = denomWaveform;
    }

    public int getCutSegmentLength() {
        return cutSegmentLength;
    }

    public void setCutSegmentLength(int cutSegmentLength) {
        this.cutSegmentLength = cutSegmentLength;
    }

    public double getCutTimeLength() {
        return cutTimeLength;
    }

    public void setCutTimeLength(double cutTimeLength) {
        this.cutTimeLength = cutTimeLength;
    }

    public double[] getDiffSegment() {
        if (diffSegment == null) {
            return null;
        }
        return diffSegment.toArray();
    }

    public void setDiffSegment(double[] diffSegment) {
        this.diffSegment = new DoubleArrayList(diffSegment);
    }

    public Double getNumerWaveStartSec() {
        return numerWaveStartSec;
    }

    public void setNumerWaveStartSec(Double numerWaveStartSec) {
        this.numerWaveStartSec = numerWaveStartSec;
    }

    public Double getDenomWaveStartSec() {
        return denomWaveStartSec;
    }

    public void setDenomWaveStartSec(Double denomWaveStartSec) {
        this.denomWaveStartSec = denomWaveStartSec;
    }

    public Double getNumerWaveEndSec() {
        return numerWaveEndSec;
    }

    public void setNumerWaveEndSec(Double numerWaveEndSec) {
        this.numerWaveEndSec = numerWaveEndSec;
    }

    public Double getDenomWaveEndSec() {
        return denomWaveEndSec;
    }

    public void setDenomWaveEndSec(Double denomWaveEndSec) {
        this.denomWaveEndSec = denomWaveEndSec;
    }

    public Double getNumerPeakSec() {
        return numerPeakSec;
    }

    public void setNumerPeakSec(Double numerPeakSec) {
        this.numerPeakSec = numerPeakSec;
    }

    public Double getDenomPeakSec() {
        return denomPeakSec;
    }

    public void setDenomPeakSec(Double denomPeakSec) {
        this.denomPeakSec = denomPeakSec;
    }

    public Double getNumerFMarkerSec() {
        return numerFMarkerSec;
    }

    public void setNumerFMarkerSec(Double numerFMarkerSec) {
        this.numerFMarkerSec = numerFMarkerSec;
    }

    public Double getDenomFMarkerSec() {
        return denomFMarkerSec;
    }

    public void setDenomFMarkerSec(Double denomFMarkerSec) {
        this.denomFMarkerSec = denomFMarkerSec;
    }

    public Double getNumerStartCutSec() {
        return numerStartCutSec;
    }

    public void setNumerStartCutSec(Double numerStartCutSec) {
        this.numerStartCutSec = numerStartCutSec;
    }

    public Double getDenomStartCutSec() {
        return denomStartCutSec;
    }

    public void setDenomStartCutSec(Double denomStartCutSec) {
        this.denomStartCutSec = denomStartCutSec;
    }

    public Double getNumerEndCutSec() {
        return numerEndCutSec;
    }

    public void setNumerEndCutSec(Double numerEndCutSec) {
        this.numerEndCutSec = numerEndCutSec;
    }

    public Double getDenomEndCutSec() {
        return denomEndCutSec;
    }

    public void setDenomEndCutSec(Double denomEndCutSec) {
        this.denomEndCutSec = denomEndCutSec;
    }

    public int getNumerStartCutIdx() {
        return numerStartCutIdx;
    }

    public void setNumerStartCutIdx(int numerStartCutIdx) {
        this.numerStartCutIdx = numerStartCutIdx;
    }

    public int getDenomStartCutIdx() {
        return denomStartCutIdx;
    }

    public void setDenomStartCutIdx(int denomStartCutIdx) {
        this.denomStartCutIdx = denomStartCutIdx;
    }

    public int getNumerEndCutIdx() {
        return numerEndCutIdx;
    }

    public void setNumerEndCutIdx(int numerEndCutIdx) {
        this.numerEndCutIdx = numerEndCutIdx;
    }

    public int getDenomEndCutIdx() {
        return denomEndCutIdx;
    }

    public void setDenomEndCutIdx(int denomEndCutIdx) {
        this.denomEndCutIdx = denomEndCutIdx;
    }

    public boolean isUserEdited() {
        return userEdited;
    }

    public void setUserEdited(boolean userEdited) {
        this.userEdited = userEdited;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                cutSegmentLength,
                    cutTimeLength,
                    denomAvg,
                    denomEndCutIdx,
                    denomEndCutSec,
                    denomFMarkerSec,
                    denomPeakSec,
                    denomStartCutIdx,
                    denomStartCutSec,
                    denomWaveEndSec,
                    denomWaveStartSec,
                    denomWaveform,
                    diffAvg,
                    diffSegment,
                    id,
                    loadedFromJson,
                    numerAvg,
                    numerEndCutIdx,
                    numerEndCutSec,
                    numerFMarkerSec,
                    numerPeakSec,
                    numerStartCutIdx,
                    numerStartCutSec,
                    numerWaveEndSec,
                    numerWaveStartSec,
                    numerWaveform,
                    userEdited,
                    version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpectraRatioPairDetails)) {
            return false;
        }
        SpectraRatioPairDetails other = (SpectraRatioPairDetails) obj;
        return cutSegmentLength == other.cutSegmentLength
                && Double.doubleToLongBits(cutTimeLength) == Double.doubleToLongBits(other.cutTimeLength)
                && Objects.equals(denomAvg, other.denomAvg)
                && denomEndCutIdx == other.denomEndCutIdx
                && Objects.equals(denomEndCutSec, other.denomEndCutSec)
                && Objects.equals(denomFMarkerSec, other.denomFMarkerSec)
                && Objects.equals(denomPeakSec, other.denomPeakSec)
                && denomStartCutIdx == other.denomStartCutIdx
                && Objects.equals(denomStartCutSec, other.denomStartCutSec)
                && Objects.equals(denomWaveEndSec, other.denomWaveEndSec)
                && Objects.equals(denomWaveStartSec, other.denomWaveStartSec)
                && Objects.equals(denomWaveform, other.denomWaveform)
                && Objects.equals(diffAvg, other.diffAvg)
                && Objects.equals(diffSegment, other.diffSegment)
                && Objects.equals(id, other.id)
                && loadedFromJson == other.loadedFromJson
                && Objects.equals(numerAvg, other.numerAvg)
                && numerEndCutIdx == other.numerEndCutIdx
                && Objects.equals(numerEndCutSec, other.numerEndCutSec)
                && Objects.equals(numerFMarkerSec, other.numerFMarkerSec)
                && Objects.equals(numerPeakSec, other.numerPeakSec)
                && numerStartCutIdx == other.numerStartCutIdx
                && Objects.equals(numerStartCutSec, other.numerStartCutSec)
                && Objects.equals(numerWaveEndSec, other.numerWaveEndSec)
                && Objects.equals(numerWaveStartSec, other.numerWaveStartSec)
                && Objects.equals(numerWaveform, other.numerWaveform)
                && userEdited == other.userEdited
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectraRatioPairDetails [id=")
               .append(id)
               .append(", version=")
               .append(version)
               .append(", diffAvg=")
               .append(diffAvg)
               .append(", numerAvg=")
               .append(numerAvg)
               .append(", denomAvg=")
               .append(denomAvg)
               .append(", numerWaveform=")
               .append(numerWaveform)
               .append(", denomWaveform=")
               .append(denomWaveform)
               .append(", loadedFromJson=")
               .append(loadedFromJson)
               .append(", cutSegmentLength=")
               .append(cutSegmentLength)
               .append(", cutTimeLength=")
               .append(cutTimeLength)
               .append(", diffSegment=")
               .append(diffSegment)
               .append(", numerWaveStartSec=")
               .append(numerWaveStartSec)
               .append(", denomWaveStartSec=")
               .append(denomWaveStartSec)
               .append(", numerWaveEndSec=")
               .append(numerWaveEndSec)
               .append(", denomWaveEndSec=")
               .append(denomWaveEndSec)
               .append(", numerPeakSec=")
               .append(numerPeakSec)
               .append(", denomPeakSec=")
               .append(denomPeakSec)
               .append(", numerFMarkerSec=")
               .append(numerFMarkerSec)
               .append(", denomFMarkerSec=")
               .append(denomFMarkerSec)
               .append(", numerStartCutSec=")
               .append(numerStartCutSec)
               .append(", denomStartCutSec=")
               .append(denomStartCutSec)
               .append(", numerEndCutSec=")
               .append(numerEndCutSec)
               .append(", denomEndCutSec=")
               .append(denomEndCutSec)
               .append(", numerStartCutIdx=")
               .append(numerStartCutIdx)
               .append(", denomStartCutIdx=")
               .append(denomStartCutIdx)
               .append(", numerEndCutIdx=")
               .append(numerEndCutIdx)
               .append(", denomEndCutIdx=")
               .append(denomEndCutIdx)
               .append(", userEdited=")
               .append(userEdited)
               .append("]");
        return builder.toString();
    }

    public void mergeNonNullFields(SpectraRatioPairDetails entity) {
        this.diffAvg = entity.getDiffAvg();
        this.numerAvg = entity.getNumerAvg();
        this.denomAvg = entity.getDenomAvg();
        if (entity.getNumerWaveform() != null) {
            if (this.numerWaveform == null) {
                this.numerWaveform = new Waveform();
            }
            this.numerWaveform.mergeNonNullOrEmptyFields(entity.getNumerWaveform());
        }
        if (entity.getDenomWaveform() != null) {
            if (this.denomWaveform == null) {
                this.denomWaveform = new Waveform();
            }
            this.denomWaveform.mergeNonNullOrEmptyFields(entity.getDenomWaveform());
        }
        this.loadedFromJson = entity.isLoadedFromJson();
        this.cutSegmentLength = entity.getCutSegmentLength();
        this.cutTimeLength = entity.getCutTimeLength();
        this.setDiffSegment(entity.getDiffSegment());
        this.numerWaveStartSec = entity.getNumerWaveStartSec();
        this.denomWaveStartSec = entity.getDenomWaveStartSec();
        this.numerWaveEndSec = entity.getNumerWaveEndSec();
        this.denomWaveEndSec = entity.getDenomWaveEndSec();
        this.numerPeakSec = entity.getNumerPeakSec();
        this.denomPeakSec = entity.getDenomPeakSec();
        this.numerFMarkerSec = entity.getNumerFMarkerSec();
        this.denomFMarkerSec = entity.getDenomFMarkerSec();
        this.numerStartCutSec = entity.getNumerStartCutSec();
        this.denomStartCutSec = entity.getDenomStartCutSec();
        this.numerEndCutSec = entity.getNumerEndCutSec();
        this.denomEndCutSec = entity.getDenomEndCutSec();
        this.numerStartCutIdx = entity.getNumerStartCutIdx();
        this.denomStartCutIdx = entity.getDenomStartCutIdx();
        this.numerEndCutIdx = entity.getNumerEndCutIdx();
        this.denomEndCutIdx = entity.getDenomEndCutIdx();
        this.userEdited = entity.isUserEdited();
    }

    public void mergeNonNullFields(SpectraRatioPairDetailsMetadata entity) {
        this.diffAvg = entity.getDiffAvg();
        this.denomAvg = entity.getDenomAvg();
        this.numerAvg = entity.getNumerAvg();
        this.cutSegmentLength = entity.getCutSegmentLength();
        this.cutTimeLength = entity.getCutTimeLength();
        this.numerWaveStartSec = entity.getNumerWaveStartSec();
        this.denomWaveStartSec = entity.getDenomWaveStartSec();
        this.numerWaveEndSec = entity.getNumerWaveEndSec();
        this.denomWaveEndSec = entity.getDenomWaveEndSec();
        this.numerPeakSec = entity.getNumerPeakSec();
        this.denomPeakSec = entity.getDenomPeakSec();
        this.numerFMarkerSec = entity.getNumerFMarkerSec();
        this.denomFMarkerSec = entity.getDenomFMarkerSec();
        this.numerStartCutSec = entity.getNumerStartCutSec();
        this.denomStartCutSec = entity.getDenomStartCutSec();
        this.numerEndCutSec = entity.getNumerEndCutSec();
        this.denomEndCutSec = entity.getDenomEndCutSec();
        this.numerStartCutIdx = entity.getNumerStartCutIdx();
        this.denomStartCutIdx = entity.getDenomStartCutIdx();
        this.numerEndCutIdx = entity.getNumerEndCutIdx();
        this.denomEndCutIdx = entity.getDenomEndCutIdx();
        this.userEdited = entity.getUserEdited();
    }

}
