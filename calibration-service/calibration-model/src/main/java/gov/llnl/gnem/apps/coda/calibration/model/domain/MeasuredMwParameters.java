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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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
    private double logM0Nm;

    @NumberFormat
    private double mw;

    @NumberFormat
    private double me;

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
    private Double me1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double me1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double me2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double me2Max;

    @NumberFormat
    @Column(nullable = true)
    private Double apparentStressInMpa;

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
    private Double cornerFrequencyUq1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double cornerFrequencyUq1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double cornerFrequencyUq2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double cornerFrequencyUq2Max;

    @NumberFormat
    @Column(nullable = true)
    private Double bandCoverage;

    @NumberFormat
    @Column(nullable = true)
    private Double obsEnergy;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergy;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergy1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergy1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergy2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergy2Max;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergyMDAC;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergyMDAC1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergyMDAC1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergyMDAC2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double logTotalEnergyMDAC2Max;

    @NumberFormat
    @Column(nullable = true)
    private Double energyRatio;

    @NumberFormat
    @Column(nullable = true)
    private Double obsAppStress;

    @NumberFormat
    @Column(nullable = true)
    private Double obsAppStress1Min;

    @NumberFormat
    @Column(nullable = true)
    private Double obsAppStress1Max;

    @NumberFormat
    @Column(nullable = true)
    private Double obsAppStress2Min;

    @NumberFormat
    @Column(nullable = true)
    private Double obsAppStress2Max;

    private int dataCount;

    private int stationCount;

    private int iterations;

    private boolean likelyPoorlyConstrained = false;

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

    public double getLogM0Nm() {
        return logM0Nm;
    }

    public MeasuredMwParameters setLogM0Nm(double logM0Nm) {
        this.logM0Nm = logM0Nm;
        return this;
    }

    public double getMw() {
        return mw;
    }

    public MeasuredMwParameters setMw(double mw) {
        this.mw = mw;
        return this;
    }

    public double getMe() {
        return me;
    }

    public MeasuredMwParameters setMe(double me) {
        this.me = me;
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

    public Double getMe1Min() {
        return me1Min;
    }

    public MeasuredMwParameters setMe1Min(Double me1Min) {
        this.me1Min = me1Min;
        return this;
    }

    public Double getMe1Max() {
        return me1Max;
    }

    public MeasuredMwParameters setMe1Max(Double me1Max) {
        this.me1Max = me1Max;
        return this;
    }

    public Double getMe2Min() {
        return me2Min;
    }

    public MeasuredMwParameters setMe2Min(Double me2Min) {
        this.me2Min = me2Min;
        return this;
    }

    public Double getMe2Max() {
        return me2Max;
    }

    public MeasuredMwParameters setMe2Max(Double me2Max) {
        this.me2Max = me2Max;
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

    public Integer getStationCount() {
        return stationCount;
    }

    public MeasuredMwParameters setStationCount(Integer stationCount) {
        this.stationCount = stationCount;
        return this;
    }

    public Double getObsEnergy() {
        return obsEnergy;
    }

    public MeasuredMwParameters setObsEnergy(Double obsEnergy) {
        this.obsEnergy = obsEnergy;
        return this;
    }

    public Double getLogTotalEnergy() {
        return logTotalEnergy;
    }

    public MeasuredMwParameters setLogTotalEnergy(Double logTotalEnergy) {
        this.logTotalEnergy = logTotalEnergy;
        return this;
    }

    public Double getLogTotalEnergyMDAC() {
        return logTotalEnergyMDAC;
    }

    public MeasuredMwParameters setLogTotalEnergyMDAC(Double totalEnergyMDAC) {
        this.logTotalEnergyMDAC = totalEnergyMDAC;
        return this;
    }

    public Double getEnergyRatio() {
        return energyRatio;
    }

    public MeasuredMwParameters setEnergyRatio(Double energyRatio) {
        this.energyRatio = energyRatio;
        return this;
    }

    public Double getObsAppStress() {
        return obsAppStress;
    }

    public MeasuredMwParameters setObsAppStress(Double obsAppStress) {
        this.obsAppStress = obsAppStress;
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

    public Double getCornerFrequencyUq1Min() {
        return cornerFrequencyUq1Min;
    }

    public MeasuredMwParameters setCornerFrequencyUq1Min(Double cornerFrequencyUq1Min) {
        this.cornerFrequencyUq1Min = cornerFrequencyUq1Min;
        return this;
    }

    public Double getCornerFrequencyUq1Max() {
        return cornerFrequencyUq1Max;
    }

    public MeasuredMwParameters setCornerFrequencyUq1Max(Double cornerFrequencyUq1Max) {
        this.cornerFrequencyUq1Max = cornerFrequencyUq1Max;
        return this;
    }

    public Double getCornerFrequencyUq2Min() {
        return cornerFrequencyUq2Min;
    }

    public MeasuredMwParameters setCornerFrequencyUq2Min(Double cornerFrequencyUq2Min) {
        this.cornerFrequencyUq2Min = cornerFrequencyUq2Min;
        return this;
    }

    public Double getCornerFrequencyUq2Max() {
        return cornerFrequencyUq2Max;
    }

    public MeasuredMwParameters setCornerFrequencyUq2Max(Double cornerFrequencyUq2Max) {
        this.cornerFrequencyUq2Max = cornerFrequencyUq2Max;
        return this;
    }

    public int getIterations() {
        return iterations;
    }

    public MeasuredMwParameters setIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    public Double getBandCoverage() {
        return bandCoverage;
    }

    public MeasuredMwParameters setBandCoverage(Double bandCoverage) {
        this.bandCoverage = bandCoverage;
        return this;
    }

    public boolean isLikelyPoorlyConstrained() {
        return likelyPoorlyConstrained;
    }

    public MeasuredMwParameters setLikelyPoorlyConstrained(boolean likelyPoorlyConstrained) {
        this.likelyPoorlyConstrained = likelyPoorlyConstrained;
        return this;
    }

    public Double getLogTotalEnergy1Min() {
        return logTotalEnergy1Min;
    }

    public MeasuredMwParameters setLogTotalEnergy1Min(Double logTotalEnergy1Min) {
        this.logTotalEnergy1Min = logTotalEnergy1Min;
        return this;
    }

    public Double getLogTotalEnergy1Max() {
        return logTotalEnergy1Max;
    }

    public MeasuredMwParameters setLogTotalEnergy1Max(Double logTotalEnergy1Max) {
        this.logTotalEnergy1Max = logTotalEnergy1Max;
        return this;
    }

    public Double getLogTotalEnergy2Min() {
        return logTotalEnergy2Min;
    }

    public MeasuredMwParameters setLogTotalEnergy2Min(Double logTotalEnergy2Min) {
        this.logTotalEnergy2Min = logTotalEnergy2Min;
        return this;
    }

    public Double getLogTotalEnergy2Max() {
        return logTotalEnergy2Max;
    }

    public MeasuredMwParameters setLogTotalEnergy2Max(Double logTotalEnergy2Max) {
        this.logTotalEnergy2Max = logTotalEnergy2Max;
        return this;
    }

    public Double getLogTotalEnergyMDAC1Min() {
        return logTotalEnergyMDAC1Min;
    }

    public MeasuredMwParameters setLogTotalEnergyMDAC1Min(Double logTotalEnergyMDAC1Min) {
        this.logTotalEnergyMDAC1Min = logTotalEnergyMDAC1Min;
        return this;
    }

    public Double getLogTotalEnergyMDAC1Max() {
        return logTotalEnergyMDAC1Max;
    }

    public MeasuredMwParameters setLogTotalEnergyMDAC1Max(Double logTotalEnergyMDAC1Max) {
        this.logTotalEnergyMDAC1Max = logTotalEnergyMDAC1Max;
        return this;
    }

    public Double getLogTotalEnergyMDAC2Min() {
        return logTotalEnergyMDAC2Min;
    }

    public MeasuredMwParameters setLogTotalEnergyMDAC2Min(Double logTotalEnergyMDAC2Min) {
        this.logTotalEnergyMDAC2Min = logTotalEnergyMDAC2Min;
        return this;
    }

    public Double getLogTotalEnergyMDAC2Max() {
        return logTotalEnergyMDAC2Max;
    }

    public MeasuredMwParameters setLogTotalEnergyMDAC2Max(Double logTotalEnergyMDAC2Max) {
        this.logTotalEnergyMDAC2Max = logTotalEnergyMDAC2Max;
        return this;
    }

    public Double getObsAppStress1Min() {
        return obsAppStress1Min;
    }

    public MeasuredMwParameters setObsAppStress1Min(Double obsAppStress1Min) {
        this.obsAppStress1Min = obsAppStress1Min;
        return this;
    }

    public Double getObsAppStress1Max() {
        return obsAppStress1Max;
    }

    public MeasuredMwParameters setObsAppStress1Max(Double obsAppStress1Max) {
        this.obsAppStress1Max = obsAppStress1Max;
        return this;
    }

    public Double getObsAppStress2Min() {
        return obsAppStress2Min;
    }

    public MeasuredMwParameters setObsAppStress2Min(Double obsAppStress2Min) {
        this.obsAppStress2Min = obsAppStress2Min;
        return this;
    }

    public Double getObsAppStress2Max() {
        return obsAppStress2Max;
    }

    public MeasuredMwParameters setObsAppStress2Max(Double obsAppStress2Max) {
        this.obsAppStress2Max = obsAppStress2Max;
        return this;
    }

    public MeasuredMwParameters merge(MeasuredMwParameters overlay) {
        this.logM0Nm = overlay.getLogM0Nm();
        this.mw = overlay.getMw();
        this.me = overlay.getMe();
        this.mw1Min = overlay.getMw1Min();
        this.mw1Max = overlay.getMw1Max();
        this.mw2Min = overlay.getMw2Min();
        this.mw2Max = overlay.getMw2Max();
        this.apparentStressInMpa = overlay.getApparentStressInMpa();
        this.apparentStress1Min = overlay.getApparentStress1Min();
        this.apparentStress1Max = overlay.getApparentStress1Max();
        this.apparentStress2Min = overlay.getApparentStress2Min();
        this.apparentStress2Max = overlay.getApparentStress2Max();

        this.obsAppStress1Min = overlay.getObsAppStress1Min();
        this.obsAppStress1Max = overlay.getObsAppStress1Max();
        this.obsAppStress2Min = overlay.getObsAppStress2Min();
        this.obsAppStress2Max = overlay.getObsAppStress2Max();
        this.logTotalEnergy1Min = overlay.getLogTotalEnergy1Min();
        this.logTotalEnergy1Max = overlay.getLogTotalEnergy1Max();
        this.logTotalEnergy2Min = overlay.getLogTotalEnergy2Min();
        this.logTotalEnergy2Max = overlay.getLogTotalEnergy2Max();
        this.logTotalEnergyMDAC1Min = overlay.getLogTotalEnergyMDAC1Min();
        this.logTotalEnergyMDAC1Max = overlay.getLogTotalEnergyMDAC1Max();
        this.logTotalEnergyMDAC2Min = overlay.getLogTotalEnergyMDAC2Min();
        this.logTotalEnergyMDAC2Max = overlay.getLogTotalEnergyMDAC2Max();

        this.me1Min = overlay.getMe1Min();
        this.me1Max = overlay.getMe1Max();
        this.me2Min = overlay.getMe2Min();
        this.me2Max = overlay.getMe2Max();
        this.misfit = overlay.getMisfit();
        this.meanMisfit = overlay.getMeanMisfit();
        this.misfitSd = overlay.getMisfitSd();
        this.dataCount = overlay.getDataCount();
        this.stationCount = overlay.getStationCount();
        this.bandCoverage = overlay.getBandCoverage();
        this.iterations = overlay.getIterations();
        this.cornerFrequency = overlay.getCornerFrequency();
        this.cornerFrequencyUq1Min = overlay.getCornerFrequencyUq1Min();
        this.cornerFrequencyUq1Max = overlay.getCornerFrequencyUq1Max();
        this.cornerFrequencyUq2Min = overlay.getCornerFrequencyUq2Min();
        this.cornerFrequencyUq2Max = overlay.getCornerFrequencyUq2Max();
        this.obsEnergy = overlay.getObsEnergy();
        this.logTotalEnergy = overlay.getLogTotalEnergy();
        this.logTotalEnergyMDAC = overlay.getLogTotalEnergyMDAC();
        this.energyRatio = overlay.getEnergyRatio();
        this.obsAppStress = overlay.getObsAppStress();
        this.likelyPoorlyConstrained = overlay.isLikelyPoorlyConstrained();

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
                    bandCoverage,
                    cornerFrequency,
                    cornerFrequencyUq1Max,
                    cornerFrequencyUq1Min,
                    cornerFrequencyUq2Max,
                    cornerFrequencyUq2Min,
                    dataCount,
                    energyRatio,
                    eventId,
                    id,
                    iterations,
                    likelyPoorlyConstrained,
                    logTotalEnergy,
                    logTotalEnergy1Max,
                    logTotalEnergy1Min,
                    logTotalEnergy2Max,
                    logTotalEnergy2Min,
                    logTotalEnergyMDAC,
                    logTotalEnergyMDAC1Max,
                    logTotalEnergyMDAC1Min,
                    logTotalEnergyMDAC2Max,
                    logTotalEnergyMDAC2Min,
                    logM0Nm,
                    me,
                    me1Max,
                    me1Min,
                    me2Max,
                    me2Min,
                    meanMisfit,
                    misfit,
                    misfitSd,
                    mw,
                    mw1Max,
                    mw1Min,
                    mw2Max,
                    mw2Min,
                    obsAppStress,
                    obsAppStress1Max,
                    obsAppStress1Min,
                    obsAppStress2Max,
                    obsAppStress2Min,
                    obsEnergy,
                    stationCount,
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
                && Objects.equals(bandCoverage, other.bandCoverage)
                && Objects.equals(cornerFrequency, other.cornerFrequency)
                && Objects.equals(cornerFrequencyUq1Max, other.cornerFrequencyUq1Max)
                && Objects.equals(cornerFrequencyUq1Min, other.cornerFrequencyUq1Min)
                && Objects.equals(cornerFrequencyUq2Max, other.cornerFrequencyUq2Max)
                && Objects.equals(cornerFrequencyUq2Min, other.cornerFrequencyUq2Min)
                && dataCount == other.dataCount
                && Objects.equals(energyRatio, other.energyRatio)
                && Objects.equals(eventId, other.eventId)
                && Objects.equals(id, other.id)
                && iterations == other.iterations
                && likelyPoorlyConstrained == other.likelyPoorlyConstrained
                && Objects.equals(logTotalEnergy, other.logTotalEnergy)
                && Objects.equals(logTotalEnergy1Max, other.logTotalEnergy1Max)
                && Objects.equals(logTotalEnergy1Min, other.logTotalEnergy1Min)
                && Objects.equals(logTotalEnergy2Max, other.logTotalEnergy2Max)
                && Objects.equals(logTotalEnergy2Min, other.logTotalEnergy2Min)
                && Objects.equals(logTotalEnergyMDAC, other.logTotalEnergyMDAC)
                && Objects.equals(logTotalEnergyMDAC1Max, other.logTotalEnergyMDAC1Max)
                && Objects.equals(logTotalEnergyMDAC1Min, other.logTotalEnergyMDAC1Min)
                && Objects.equals(logTotalEnergyMDAC2Max, other.logTotalEnergyMDAC2Max)
                && Objects.equals(logTotalEnergyMDAC2Min, other.logTotalEnergyMDAC2Min)
                && Double.doubleToLongBits(logM0Nm) == Double.doubleToLongBits(other.logM0Nm)
                && Double.doubleToLongBits(me) == Double.doubleToLongBits(other.me)
                && Objects.equals(me1Max, other.me1Max)
                && Objects.equals(me1Min, other.me1Min)
                && Objects.equals(me2Max, other.me2Max)
                && Objects.equals(me2Min, other.me2Min)
                && Objects.equals(meanMisfit, other.meanMisfit)
                && Objects.equals(misfit, other.misfit)
                && Objects.equals(misfitSd, other.misfitSd)
                && Double.doubleToLongBits(mw) == Double.doubleToLongBits(other.mw)
                && Objects.equals(mw1Max, other.mw1Max)
                && Objects.equals(mw1Min, other.mw1Min)
                && Objects.equals(mw2Max, other.mw2Max)
                && Objects.equals(mw2Min, other.mw2Min)
                && Objects.equals(obsAppStress, other.obsAppStress)
                && Objects.equals(obsAppStress1Max, other.obsAppStress1Max)
                && Objects.equals(obsAppStress1Min, other.obsAppStress1Min)
                && Objects.equals(obsAppStress2Max, other.obsAppStress2Max)
                && Objects.equals(obsAppStress2Min, other.obsAppStress2Min)
                && Objects.equals(obsEnergy, other.obsEnergy)
                && stationCount == other.stationCount
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
               .append(", m0=")
               .append(logM0Nm)
               .append(", mw=")
               .append(mw)
               .append(", me=")
               .append(me)
               .append(", mw1Min=")
               .append(mw1Min)
               .append(", mw1Max=")
               .append(mw1Max)
               .append(", mw2Min=")
               .append(mw2Min)
               .append(", mw2Max=")
               .append(mw2Max)
               .append(", me1Min=")
               .append(me1Min)
               .append(", me1Max=")
               .append(me1Max)
               .append(", me2Min=")
               .append(me2Min)
               .append(", me2Max=")
               .append(me2Max)
               .append(", apparentStressInMpa=")
               .append(apparentStressInMpa)
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
               .append(", cornerFrequencyUq1Min=")
               .append(cornerFrequencyUq1Min)
               .append(", cornerFrequencyUq1Max=")
               .append(cornerFrequencyUq1Max)
               .append(", cornerFrequencyUq2Min=")
               .append(cornerFrequencyUq2Min)
               .append(", cornerFrequencyUq2Max=")
               .append(cornerFrequencyUq2Max)
               .append(", bandCoverage=")
               .append(bandCoverage)
               .append(", obsEnergy=")
               .append(obsEnergy)
               .append(", logTotalEnergy=")
               .append(logTotalEnergy)
               .append(", logTotalEnergy1Min=")
               .append(logTotalEnergy1Min)
               .append(", logTotalEnergy1Max=")
               .append(logTotalEnergy1Max)
               .append(", logTotalEnergy2Min=")
               .append(logTotalEnergy2Min)
               .append(", logTotalEnergy2Max=")
               .append(logTotalEnergy2Max)
               .append(", logTotalEnergyMDAC=")
               .append(logTotalEnergyMDAC)
               .append(", logTotalEnergyMDAC1Min=")
               .append(logTotalEnergyMDAC1Min)
               .append(", logTotalEnergyMDAC1Max=")
               .append(logTotalEnergyMDAC1Max)
               .append(", logTotalEnergyMDAC2Min=")
               .append(logTotalEnergyMDAC2Min)
               .append(", logTotalEnergyMDAC2Max=")
               .append(logTotalEnergyMDAC2Max)
               .append(", energyRatio=")
               .append(energyRatio)
               .append(", obsAppStress=")
               .append(obsAppStress)
               .append(", obsAppStress1Min=")
               .append(obsAppStress1Min)
               .append(", obsAppStress1Max=")
               .append(obsAppStress1Max)
               .append(", obsAppStress2Min=")
               .append(obsAppStress2Min)
               .append(", obsAppStress2Max=")
               .append(obsAppStress2Max)
               .append(", dataCount=")
               .append(dataCount)
               .append(", stationCount=")
               .append(stationCount)
               .append(", iterations=")
               .append(iterations)
               .append(", likelyPoorlyConstrained=")
               .append(likelyPoorlyConstrained)
               .append("]");
        return builder.toString();
    }

}