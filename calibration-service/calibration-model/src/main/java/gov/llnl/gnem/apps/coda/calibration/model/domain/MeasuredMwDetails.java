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

import java.time.format.DateTimeFormatter;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;

public class MeasuredMwDetails {

    private String eventId;

    private Double logM0Nm;

    private Double mw;

    private Double mw1Max;

    private Double mw1Min;

    private Double mw2Max;

    private Double mw2Min;

    private Double me;

    private Double me1Min;

    private Double me1Max;

    private Double me2Min;

    private Double me2Max;

    private Double obsEnergy;

    private Double logTotalEnergy;

    private Double logTotalEnergy1Min;

    private Double logTotalEnergy1Max;

    private Double logTotalEnergy2Min;

    private Double logTotalEnergy2Max;

    private Double logTotalEnergyMDAC;

    private Double logTotalEnergyMDAC1Min;

    private Double logTotalEnergyMDAC1Max;

    private Double logTotalEnergyMDAC2Min;

    private Double logTotalEnergyMDAC2Max;

    private Double energyRatio;

    private Double obsAppStress;

    private Double obsAppStress1Min;

    private Double obsAppStress1Max;

    private Double obsAppStress2Min;

    private Double obsAppStress2Max;

    private Double refMw;

    private Double valMw;

    private Double apparentStressInMpa;

    private Double apparentStress1Max;

    private Double apparentStress1Min;

    private Double apparentStress2Max;

    private Double apparentStress2Min;

    private Double refApparentStressInMpa;

    private Double valApparentStressInMpa;

    private Double cornerFreq;

    private Double cornerFreq1Max;

    private Double cornerFreq1Min;

    private Double cornerFreq2Max;

    private Double cornerFreq2Min;

    private Integer dataCount;

    private Integer stationCount;

    private Double latitude;

    private Double longitude;

    private Double depth;

    private String datetime;

    private Integer iterations;

    private Double misfit;

    private Double misfitMean;

    private Double misfitSd;

    private Double bandCoverage;

    private Boolean likelyPoorlyConstrained;

    public MeasuredMwDetails(MeasuredMwParameters meas, ReferenceMwParameters ref, ValidationMwParameters val, Event event) {
        if (meas != null) {
            this.eventId = meas.getEventId();
            this.logM0Nm = meas.getLogM0Nm();
            this.mw = meas.getMw();
            this.me = meas.getMe();
            this.obsEnergy = meas.getObsEnergy();
            this.logTotalEnergy = meas.getLogTotalEnergy();
            this.logTotalEnergyMDAC = meas.getLogTotalEnergyMDAC();

            this.obsAppStress1Min = meas.getObsAppStress1Min();
            this.obsAppStress1Max = meas.getObsAppStress1Max();
            this.obsAppStress2Min = meas.getObsAppStress2Min();
            this.obsAppStress2Max = meas.getObsAppStress2Max();
            this.logTotalEnergy1Min = meas.getLogTotalEnergy1Min();
            this.logTotalEnergy1Max = meas.getLogTotalEnergy1Max();
            this.logTotalEnergy2Min = meas.getLogTotalEnergy2Min();
            this.logTotalEnergy2Max = meas.getLogTotalEnergy2Max();
            this.logTotalEnergyMDAC1Min = meas.getLogTotalEnergyMDAC1Min();
            this.logTotalEnergyMDAC1Max = meas.getLogTotalEnergyMDAC1Max();
            this.logTotalEnergyMDAC2Min = meas.getLogTotalEnergyMDAC2Min();
            this.logTotalEnergyMDAC2Max = meas.getLogTotalEnergyMDAC2Max();

            this.energyRatio = meas.getEnergyRatio();
            this.obsAppStress = meas.getObsAppStress();
            this.mw1Max = meas.getMw1Max();
            this.mw1Min = meas.getMw1Min();
            this.mw2Max = meas.getMw2Max();
            this.mw2Min = meas.getMw2Min();
            this.apparentStressInMpa = meas.getApparentStressInMpa();
            this.apparentStress1Max = meas.getApparentStress1Max();
            this.apparentStress1Min = meas.getApparentStress1Min();
            this.apparentStress2Max = meas.getApparentStress2Max();
            this.apparentStress2Min = meas.getApparentStress2Min();
            this.me1Min = meas.getMe1Min();
            this.me1Max = meas.getMe1Max();
            this.me2Min = meas.getMe2Min();
            this.me2Max = meas.getMe2Max();
            this.cornerFreq = meas.getCornerFrequency();
            this.cornerFreq1Max = meas.getCornerFrequencyUq1Max();
            this.cornerFreq1Min = meas.getCornerFrequencyUq1Min();
            this.cornerFreq2Max = meas.getCornerFrequencyUq2Max();
            this.cornerFreq2Min = meas.getCornerFrequencyUq2Min();
            this.dataCount = meas.getDataCount();
            this.stationCount = meas.getStationCount();
            this.iterations = meas.getIterations();
            this.misfit = meas.getMisfit();
            this.misfitMean = meas.getMeanMisfit();
            this.misfitSd = meas.getMisfitSd();
            this.bandCoverage = meas.getBandCoverage();
            this.likelyPoorlyConstrained = meas.isLikelyPoorlyConstrained();
        }
        if (event != null && event.getEventId() != null) {
            this.eventId = event.getEventId();
            this.latitude = event.getLatitude();
            this.longitude = event.getLongitude();
            this.depth = event.getDepth();
            if (event.getOriginTime() != null) {
                this.datetime = DateTimeFormatter.ISO_INSTANT.format(event.getOriginTime().toInstant());
            }
        }
        if (ref != null) {
            if (eventId == null) {
                this.eventId = ref.getEventId();
            }
            this.refApparentStressInMpa = ref.getRefApparentStressInMpa();
            this.refMw = ref.getRefMw();
        }

        if (val != null) {
            if (eventId == null) {
                this.eventId = val.getEventId();
            }
            this.valApparentStressInMpa = val.getApparentStressInMpa();
            this.valMw = val.getMw();
        }
    }

    public MeasuredMwDetails() {
        // nop
    }

    public String getEventId() {
        return eventId;
    }

    public MeasuredMwDetails setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public Double getLogM0Nm() {
        return logM0Nm;
    }

    public Double getMw() {
        return mw;
    }

    public Double getMw1Max() {
        return mw1Max;
    }

    public MeasuredMwDetails setMw1Max(Double mw1Max) {
        this.mw1Max = mw1Max;
        return this;
    }

    public Double getMw1Min() {
        return mw1Min;
    }

    public MeasuredMwDetails setMw1Min(Double mw1Min) {
        this.mw1Min = mw1Min;
        return this;
    }

    public Double getMw2Max() {
        return mw2Max;
    }

    public MeasuredMwDetails setMw2Max(Double mw2Max) {
        this.mw2Max = mw2Max;
        return this;
    }

    public Double getMw2Min() {
        return mw2Min;
    }

    public MeasuredMwDetails setMw2Min(Double mw2Min) {
        this.mw2Min = mw2Min;
        return this;
    }

    public Double getApparentStressInMpa() {
        return apparentStressInMpa;
    }

    public MeasuredMwDetails setApparentStressInMpa(Double apparentStressInMpa) {
        this.apparentStressInMpa = apparentStressInMpa;
        return this;
    }

    public Double getApparentStress1Max() {
        return apparentStress1Max;
    }

    public MeasuredMwDetails setApparentStress1Max(Double apparentStress1Max) {
        this.apparentStress1Max = apparentStress1Max;
        return this;
    }

    public Double getApparentStress1Min() {
        return apparentStress1Min;
    }

    public MeasuredMwDetails setApparentStress1Min(Double apparentStress1Min) {
        this.apparentStress1Min = apparentStress1Min;
        return this;
    }

    public Double getApparentStress2Max() {
        return apparentStress2Max;
    }

    public MeasuredMwDetails setApparentStress2Max(Double apparentStress2Max) {
        this.apparentStress2Max = apparentStress2Max;
        return this;
    }

    public Double getApparentStress2Min() {
        return apparentStress2Min;
    }

    public MeasuredMwDetails setApparentStress2Min(Double apparentStress2Min) {
        this.apparentStress2Min = apparentStress2Min;
        return this;
    }

    public Double getCornerFreq() {
        return cornerFreq;
    }

    public MeasuredMwDetails setCornerFreq(Double cornerFreq) {
        this.cornerFreq = cornerFreq;
        return this;
    }

    public Double getCornerFreq1Max() {
        return cornerFreq1Max;
    }

    public void setCornerFreq1Max(Double cornerFreq1Max) {
        this.cornerFreq1Max = cornerFreq1Max;
    }

    public Double getCornerFreq1Min() {
        return cornerFreq1Min;
    }

    public void setCornerFreq1Min(Double cornerFreq1Min) {
        this.cornerFreq1Min = cornerFreq1Min;
    }

    public Double getCornerFreq2Max() {
        return cornerFreq2Max;
    }

    public void setCornerFreq2Max(Double cornerFreq2Max) {
        this.cornerFreq2Max = cornerFreq2Max;
    }

    public Double getCornerFreq2Min() {
        return cornerFreq2Min;
    }

    public void setCornerFreq2Min(Double cornerFreq2Min) {
        this.cornerFreq2Min = cornerFreq2Min;
    }

    public Double getMe() {
        return me;
    }

    public Double getMe1Min() {
        return me1Min;
    }

    public MeasuredMwDetails setMe1Min(Double me1Min) {
        this.me1Min = me1Min;
        return this;
    }

    public Double getMe1Max() {
        return me1Max;
    }

    public MeasuredMwDetails setMe1Max(Double me1Max) {
        this.me1Max = me1Max;
        return this;
    }

    public Double getMe2Min() {
        return me2Min;
    }

    public MeasuredMwDetails setMe2Min(Double me2Min) {
        this.me2Min = me2Min;
        return this;
    }

    public Double getMe2Max() {
        return me2Max;
    }

    public MeasuredMwDetails setMe2Max(Double me2Max) {
        this.me2Max = me2Max;
        return this;
    }

    public Double getObsEnergy() {
        return obsEnergy;
    }

    public MeasuredMwDetails setObsEnergy(Double obsEnergy) {
        this.obsEnergy = obsEnergy;
        return this;
    }

    public Double getObsAppStress1Min() {
        return obsAppStress1Min;
    }

    public Double getObsAppStress1Max() {
        return obsAppStress1Max;
    }

    public Double getObsAppStress2Min() {
        return obsAppStress2Min;
    }

    public Double getObsAppStress2Max() {
        return obsAppStress2Max;
    }

    public Double getTotalEnergy() {
        return logTotalEnergy;
    }

    public MeasuredMwDetails setTotalEnergy(Double totalEnergy) {
        this.logTotalEnergy = totalEnergy;
        return this;
    }

    public Double getLogTotalEnergy1Min() {
        return logTotalEnergy1Min;
    }

    public Double getLogTotalEnergy1Max() {
        return logTotalEnergy1Max;
    }

    public Double getLogTotalEnergy2Min() {
        return logTotalEnergy2Min;
    }

    public Double getLogTotalEnergy2Max() {
        return logTotalEnergy2Max;
    }

    public Double getTotalEnergyMDAC() {
        return logTotalEnergyMDAC;
    }

    public MeasuredMwDetails setTotalEnergyMDAC(Double totalEnergyMDAC) {
        this.logTotalEnergyMDAC = totalEnergyMDAC;
        return this;
    }

    public Double getLogTotalEnergyMDAC1Min() {
        return logTotalEnergyMDAC1Min;
    }

    public Double getLogTotalEnergyMDAC1Max() {
        return logTotalEnergyMDAC1Max;
    }

    public Double getLogTotalEnergyMDAC2Min() {
        return logTotalEnergyMDAC2Min;
    }

    public Double getLogTotalEnergyMDAC2Max() {
        return logTotalEnergyMDAC2Max;
    }

    public Double getEnergyRatio() {
        return energyRatio;
    }

    public MeasuredMwDetails setEnergyRatio(Double energyRatio) {
        this.energyRatio = energyRatio;
        return this;
    }

    public Double getEnergyStress() {
        return obsAppStress;
    }

    public MeasuredMwDetails setEnergyStress(Double energyStress) {
        this.obsAppStress = energyStress;
        return this;
    }

    public MeasuredMwDetails setMw(double mw) {
        this.mw = mw;
        return this;
    }

    public Double getRefMw() {
        return refMw;
    }

    public MeasuredMwDetails setRefMw(Double refMw) {
        this.refMw = refMw;
        return this;
    }

    public Double getValMw() {
        return valMw;
    }

    public MeasuredMwDetails setValMw(Double mw) {
        this.valMw = mw;
        return this;
    }

    public Double getValApparentStressInMpa() {
        return valApparentStressInMpa;
    }

    public MeasuredMwDetails setValApparentStressInMpa(Double stressMpa) {
        this.valApparentStressInMpa = stressMpa;
        return this;
    }

    public Double getRefApparentStressInMpa() {
        return refApparentStressInMpa;
    }

    public MeasuredMwDetails setRefApparentStressInMpa(Double refApparentStressInMpa) {
        this.refApparentStressInMpa = refApparentStressInMpa;
        return this;
    }

    public Integer getDataCount() {
        return dataCount;
    }

    public MeasuredMwDetails setDataCount(Integer dataCount) {
        this.dataCount = dataCount;
        return this;
    }

    public Integer getStationCount() {
        return stationCount;
    }

    public MeasuredMwDetails setStationCount(Integer stationCount) {
        this.stationCount = stationCount;
        return this;
    }

    public Double getLatitude() {
        return latitude;
    }

    public MeasuredMwDetails setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public Double getLongitude() {
        return longitude;
    }

    public MeasuredMwDetails setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public Double getDepth() {
        return depth;
    }

    public MeasuredMwDetails setDepth(double depth) {
        this.depth = depth;
        return this;
    }

    public String getDatetime() {
        return datetime;
    }

    public MeasuredMwDetails setDatetime(String datetime) {
        this.datetime = datetime;
        return this;
    }

    public Integer getIterations() {
        return iterations;
    }

    public MeasuredMwDetails setIterations(Integer iterations) {
        this.iterations = iterations;
        return this;
    }

    public Double getMisfit() {
        return misfit;
    }

    public MeasuredMwDetails setMisfit(Double misfit) {
        this.misfit = misfit;
        return this;
    }

    public Double getMisfitMean() {
        return misfitMean;
    }

    public MeasuredMwDetails setMisfitMean(Double misfitMean) {
        this.misfitMean = misfitMean;
        return this;
    }

    public Double getMisfitSd() {
        return misfitSd;
    }

    public MeasuredMwDetails setMisfitSd(Double misfitSd) {
        this.misfitSd = misfitSd;
        return this;
    }

    public Double getBandCoverage() {
        return bandCoverage;
    }

    public MeasuredMwDetails setBandCoverage(Double bandCoverage) {
        this.bandCoverage = bandCoverage;
        return this;
    }

    public Boolean isLikelyPoorlyConstrained() {
        return likelyPoorlyConstrained;
    }

    public void setLikelyPoorlyConstrained(Boolean likelyPoorlyConstrained) {
        this.likelyPoorlyConstrained = likelyPoorlyConstrained;
    }

    @JsonIgnore
    public boolean isValid() {
        return eventId != null;
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
                    cornerFreq,
                    cornerFreq1Max,
                    cornerFreq1Min,
                    cornerFreq2Max,
                    cornerFreq2Min,
                    dataCount,
                    datetime,
                    depth,
                    energyRatio,
                    eventId,
                    iterations,
                    latitude,
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
                    longitude,
                    logM0Nm,
                    me,
                    me1Max,
                    me1Min,
                    me2Max,
                    me2Min,
                    misfit,
                    misfitMean,
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
                    refApparentStressInMpa,
                    refMw,
                    stationCount,
                    valApparentStressInMpa,
                    valMw);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof MeasuredMwDetails)) {
            return false;
        }
        MeasuredMwDetails other = (MeasuredMwDetails) obj;
        return Objects.equals(apparentStress1Max, other.apparentStress1Max)
                && Objects.equals(apparentStress1Min, other.apparentStress1Min)
                && Objects.equals(apparentStress2Max, other.apparentStress2Max)
                && Objects.equals(apparentStress2Min, other.apparentStress2Min)
                && Objects.equals(apparentStressInMpa, other.apparentStressInMpa)
                && Objects.equals(bandCoverage, other.bandCoverage)
                && Objects.equals(cornerFreq, other.cornerFreq)
                && Objects.equals(cornerFreq1Max, other.cornerFreq1Max)
                && Objects.equals(cornerFreq1Min, other.cornerFreq1Min)
                && Objects.equals(cornerFreq2Max, other.cornerFreq2Max)
                && Objects.equals(cornerFreq2Min, other.cornerFreq2Min)
                && Objects.equals(dataCount, other.dataCount)
                && Objects.equals(datetime, other.datetime)
                && Objects.equals(depth, other.depth)
                && Objects.equals(energyRatio, other.energyRatio)
                && Objects.equals(eventId, other.eventId)
                && Objects.equals(iterations, other.iterations)
                && Objects.equals(latitude, other.latitude)
                && Objects.equals(likelyPoorlyConstrained, other.likelyPoorlyConstrained)
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
                && Objects.equals(longitude, other.longitude)
                && Objects.equals(logM0Nm, other.logM0Nm)
                && Objects.equals(me, other.me)
                && Objects.equals(me1Max, other.me1Max)
                && Objects.equals(me1Min, other.me1Min)
                && Objects.equals(me2Max, other.me2Max)
                && Objects.equals(me2Min, other.me2Min)
                && Objects.equals(misfit, other.misfit)
                && Objects.equals(misfitMean, other.misfitMean)
                && Objects.equals(misfitSd, other.misfitSd)
                && Objects.equals(mw, other.mw)
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
                && Objects.equals(refApparentStressInMpa, other.refApparentStressInMpa)
                && Objects.equals(refMw, other.refMw)
                && Objects.equals(stationCount, other.stationCount)
                && Objects.equals(valApparentStressInMpa, other.valApparentStressInMpa)
                && Objects.equals(valMw, other.valMw);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MeasuredMwDetails [eventId=")
               .append(eventId)
               .append(", m0Nm=")
               .append(logM0Nm)
               .append(", mw=")
               .append(mw)
               .append(", mw1Max=")
               .append(mw1Max)
               .append(", mw1Min=")
               .append(mw1Min)
               .append(", mw2Max=")
               .append(mw2Max)
               .append(", mw2Min=")
               .append(mw2Min)
               .append(", me=")
               .append(me)
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
               .append(", refMw=")
               .append(refMw)
               .append(", valMw=")
               .append(valMw)
               .append(", apparentStressInMpa=")
               .append(apparentStressInMpa)
               .append(", apparentStress1Max=")
               .append(apparentStress1Max)
               .append(", apparentStress1Min=")
               .append(apparentStress1Min)
               .append(", apparentStress2Max=")
               .append(apparentStress2Max)
               .append(", apparentStress2Min=")
               .append(apparentStress2Min)
               .append(", me1Min=")
               .append(me1Min)
               .append(", me1Max=")
               .append(me1Max)
               .append(", me2Min=")
               .append(me2Min)
               .append(", me2Max=")
               .append(me2Max)
               .append(", refApparentStressInMpa=")
               .append(refApparentStressInMpa)
               .append(", valApparentStressInMpa=")
               .append(valApparentStressInMpa)
               .append(", cornerFreq=")
               .append(cornerFreq)
               .append(", cornerFreq1Max=")
               .append(cornerFreq1Max)
               .append(", cornerFreq1Min=")
               .append(cornerFreq1Min)
               .append(", cornerFreq2Max=")
               .append(cornerFreq2Max)
               .append(", cornerFreq2Min=")
               .append(cornerFreq2Min)
               .append(", dataCount=")
               .append(dataCount)
               .append(", stationCount=")
               .append(stationCount)
               .append(", latitude=")
               .append(latitude)
               .append(", longitude=")
               .append(longitude)
               .append(", depth=")
               .append(depth)
               .append(", datetime=")
               .append(datetime)
               .append(", iterations=")
               .append(iterations)
               .append(", misfit=")
               .append(misfit)
               .append(", misfitMean=")
               .append(misfitMean)
               .append(", misfitSd=")
               .append(misfitSd)
               .append(", bandCoverage=")
               .append(bandCoverage)
               .append(", likelyPoorlyConstrained=")
               .append(likelyPoorlyConstrained)
               .append("]");
        return builder.toString();
    }

}
