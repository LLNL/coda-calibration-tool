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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

@Entity
@Table(name = "Measured_Mws", indexes = { @Index(columnList = "eventId", name = "event_id_index") })
public class MeasuredMwParameters implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @Column(name = "eventId")
    private String eventId;

    @NumberFormat
    private double mw;

    @NumberFormat
    @Column(nullable = true)
    private Double meanMw;

    @NumberFormat
    @Column(nullable = true)
    private Double mwSd;

    @NumberFormat
    @Column(nullable = true)
    private Double mw1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double mw1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double mw2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double mw2Max;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStressInMpa;

    @NumberFormat
    @Column(nullable = true)
    private Double meanApparentStressInMpa;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStressSd;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStress1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStress1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStress2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStress2Max;

    @NumberFormat
    @Column(nullable = true)
    private Double misfit;

    @NumberFormat
    @Column(nullable = true)
    private Double meanMisfit;

    @NumberFormat
    @Column(nullable = true)
    private Double misfitSd;

    @NumberFormat
    @Column(nullable = true)
    private Double cornerFrequency;

    @NumberFormat
    @Column(nullable = true)
    private Double cornerFrequencySd;

    private int dataCount;

    private int iterations;

    public Long getId() {
        return id;
    }

    public MeasuredMwParameters setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public String getEventId() {
        return eventId;
    }

    public MeasuredMwParameters setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public double getMw() {
        return mw;
    }

    public MeasuredMwParameters setMw(double mw) {
        this.mw = mw;
        return this;
    }

    public Double getMwSd() {
        return mwSd;
    }

    public MeasuredMwParameters setMwSd(Double mwSd) {
        this.mwSd = mwSd;
        return this;
    }

    public Double getMw1Min() {
        return mw1Min;
    }

    public MeasuredMwParameters setMw1Min(Double mw1Min) {
        this.mw1Min = mw1Min;
        return this;
    }

    public Double getMw1Max() {
        return mw1Max;
    }

    public MeasuredMwParameters setMw1Max(Double mw1Max) {
        this.mw1Max = mw1Max;
        return this;
    }

    public Double getMw2Min() {
        return mw2Min;
    }

    public MeasuredMwParameters setMw2Min(Double mw2Min) {
        this.mw2Min = mw2Min;
        return this;
    }

    public Double getMw2Max() {
        return mw2Max;
    }

    public MeasuredMwParameters setMw2Max(Double mw2Max) {
        this.mw2Max = mw2Max;
        return this;
    }

    public Double getApparentStressInMpa() {
        return apparentStressInMpa;
    }

    public MeasuredMwParameters setApparentStressInMpa(Double apparentStressInMpa) {
        this.apparentStressInMpa = apparentStressInMpa;
        return this;
    }

    public Double getApparentStressSd() {
        return apparentStressSd;
    }

    public MeasuredMwParameters setApparentStressSd(Double apparentStressSd) {
        this.apparentStressSd = apparentStressSd;
        return this;
    }

    public Double getApparentStress1Min() {
        return apparentStress1Min;
    }

    public MeasuredMwParameters setApparentStress1Min(Double apparentStress1Min) {
        this.apparentStress1Min = apparentStress1Min;
        return this;
    }

    public Double getApparentStress1Max() {
        return apparentStress1Max;
    }

    public MeasuredMwParameters setApparentStress1Max(Double apparentStress1Max) {
        this.apparentStress1Max = apparentStress1Max;
        return this;
    }

    public Double getApparentStress2Min() {
        return apparentStress2Min;
    }

    public MeasuredMwParameters setApparentStress2Min(Double apparentStress2Min) {
        this.apparentStress2Min = apparentStress2Min;
        return this;
    }

    public Double getApparentStress2Max() {
        return apparentStress2Max;
    }

    public MeasuredMwParameters setApparentStress2Max(Double apparentStress2Max) {
        this.apparentStress2Max = apparentStress2Max;
        return this;
    }

    public Double getMisfit() {
        return misfit;
    }

    public MeasuredMwParameters setMisfit(Double misfit) {
        this.misfit = misfit;
        return this;
    }

    public Double getMisfitSd() {
        return misfitSd;
    }

    public MeasuredMwParameters setMisfitSd(Double misfitSd) {
        this.misfitSd = misfitSd;
        return this;
    }

    public Integer getDataCount() {
        return dataCount;
    }

    public MeasuredMwParameters setDataCount(Integer dataCount) {
        this.dataCount = dataCount;
        return this;
    }

    public Double getMeanMw() {
        return meanMw;
    }

    public MeasuredMwParameters setMeanMw(Double meanMw) {
        this.meanMw = meanMw;
        return this;
    }

    public Double getMeanApparentStressInMpa() {
        return meanApparentStressInMpa;
    }

    public MeasuredMwParameters setMeanApparentStressInMpa(Double meanApparentStressInMpa) {
        this.meanApparentStressInMpa = meanApparentStressInMpa;
        return this;
    }

    public Double getMeanMisfit() {
        return meanMisfit;
    }

    public MeasuredMwParameters setMeanMisfit(Double meanMisfit) {
        this.meanMisfit = meanMisfit;
        return this;
    }

    public Double getCornerFrequency() {
        return cornerFrequency;
    }

    public MeasuredMwParameters setCornerFrequency(Double cornerFrequency) {
        this.cornerFrequency = cornerFrequency;
        return this;
    }

    public Double getCornerFrequencySd() {
        return cornerFrequencySd;
    }

    public MeasuredMwParameters setCornerFrequencySd(Double cornerFrequencySd) {
        this.cornerFrequencySd = cornerFrequencySd;
        return this;
    }

    public int getIterations() {
        return iterations;
    }

    public MeasuredMwParameters setIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    public MeasuredMwParameters merge(MeasuredMwParameters overlay) {
        this.mw = overlay.getMw();
        this.meanMw = overlay.getMeanMw();
        this.mwSd = overlay.getMwSd();
        this.mw1Min = overlay.getMw1Min();
        this.mw1Max = overlay.getMw1Max();
        this.mw2Min = overlay.getMw2Min();
        this.mw2Max = overlay.getMw2Max();
        this.apparentStressInMpa = overlay.getApparentStressInMpa();
        this.meanApparentStressInMpa = overlay.getMeanApparentStressInMpa();
        this.apparentStressSd = overlay.getApparentStressSd();
        this.apparentStress1Min = overlay.getApparentStress1Min();
        this.apparentStress1Max = overlay.getApparentStress1Max();
        this.apparentStress2Min = overlay.getApparentStress2Min();
        this.apparentStress2Max = overlay.getApparentStress2Max();
        this.misfit = overlay.getMisfit();
        this.meanMisfit = overlay.getMeanMisfit();
        this.misfitSd = overlay.getMisfitSd();
        this.dataCount = overlay.getDataCount();
        this.iterations = overlay.getIterations();
        this.cornerFrequency = overlay.getCornerFrequency();
        this.cornerFrequencySd = overlay.getCornerFrequencySd();
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                apparentStress1Max,
                    apparentStress1Min,
                    apparentStress2Max,
                    apparentStress2Min,
                    apparentStressInMpa,
                    apparentStressSd,
                    cornerFrequency,
                    cornerFrequencySd,
                    dataCount,
                    eventId,
                    id,
                    iterations,
                    meanApparentStressInMpa,
                    meanMisfit,
                    meanMw,
                    misfit,
                    misfitSd,
                    mw,
                    mw1Max,
                    mw1Min,
                    mw2Max,
                    mw2Min,
                    mwSd,
                    version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MeasuredMwParameters)) {
            return false;
        }
        MeasuredMwParameters other = (MeasuredMwParameters) obj;
        return Objects.equals(apparentStress1Max, other.apparentStress1Max)
                && Objects.equals(apparentStress1Min, other.apparentStress1Min)
                && Objects.equals(apparentStress2Max, other.apparentStress2Max)
                && Objects.equals(apparentStress2Min, other.apparentStress2Min)
                && Objects.equals(apparentStressInMpa, other.apparentStressInMpa)
                && Objects.equals(apparentStressSd, other.apparentStressSd)
                && Objects.equals(cornerFrequency, other.cornerFrequency)
                && Objects.equals(cornerFrequencySd, other.cornerFrequencySd)
                && dataCount == other.dataCount
                && Objects.equals(eventId, other.eventId)
                && Objects.equals(id, other.id)
                && iterations == other.iterations
                && Objects.equals(meanApparentStressInMpa, other.meanApparentStressInMpa)
                && Objects.equals(meanMisfit, other.meanMisfit)
                && Objects.equals(meanMw, other.meanMw)
                && Objects.equals(misfit, other.misfit)
                && Objects.equals(misfitSd, other.misfitSd)
                && Double.doubleToLongBits(mw) == Double.doubleToLongBits(other.mw)
                && Objects.equals(mw1Max, other.mw1Max)
                && Objects.equals(mw1Min, other.mw1Min)
                && Objects.equals(mw2Max, other.mw2Max)
                && Objects.equals(mw2Min, other.mw2Min)
                && Objects.equals(mwSd, other.mwSd)
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MeasuredMwParameters [id=")
               .append(id)
               .append(", version=")
               .append(version)
               .append(", eventId=")
               .append(eventId)
               .append(", mw=")
               .append(mw)
               .append(", meanMw=")
               .append(meanMw)
               .append(", mwSd=")
               .append(mwSd)
               .append(", mw1Min=")
               .append(mw1Min)
               .append(", mw1Max=")
               .append(mw1Max)
               .append(", mw2Min=")
               .append(mw2Min)
               .append(", mw2Max=")
               .append(mw2Max)
               .append(", apparentStressInMpa=")
               .append(apparentStressInMpa)
               .append(", meanApparentStressInMpa=")
               .append(meanApparentStressInMpa)
               .append(", apparentStressSd=")
               .append(apparentStressSd)
               .append(", apparentStress1Min=")
               .append(apparentStress1Min)
               .append(", apparentStress1Max=")
               .append(apparentStress1Max)
               .append(", apparentStress2Min=")
               .append(apparentStress2Min)
               .append(", apparentStress2Max=")
               .append(apparentStress2Max)
               .append(", misfit=")
               .append(misfit)
               .append(", meanMisfit=")
               .append(meanMisfit)
               .append(", misfitSd=")
               .append(misfitSd)
               .append(", cornerFrequency=")
               .append(cornerFrequency)
               .append(", cornerFrequencySd=")
               .append(cornerFrequencySd)
               .append(", dataCount=")
               .append(dataCount)
               .append(", iterations=")
               .append(iterations)
               .append("]");
        return builder.toString();
    }

}