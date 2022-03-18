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

    private Double mw;

    private Double obsEnergy;

    private Double logTotalEnergy;

    private Double logTotalEnergyMDAC;

    private Double energyRatio;

    private Double obsAppStress;

    private Double mwSd;

    private Double mw1Max;

    private Double mw1Min;

    private Double mw2Max;

    private Double mw2Min;

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

    private Double cornerFreqSd;

    private Integer dataCount;

    private Integer stationCount;

    private Double latitude;

    private Double longitude;

    private Double depth;

    private String datetime;

    private Integer iterations;

    private Double misfit;

    private Double bandCoverage;

    private Boolean likelyPoorlyConstrained;

    public MeasuredMwDetails(MeasuredMwParameters meas, ReferenceMwParameters ref, ValidationMwParameters val, Event event) {
        if (meas != null) {
            this.eventId = meas.getEventId();
            this.mw = meas.getMw();
            this.obsEnergy = meas.getObsEnergy();
            this.logTotalEnergy = meas.getLogTotalEnergy();
            this.logTotalEnergyMDAC = meas.getLogTotalEnergyMDAC();
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
            this.cornerFreq = meas.getCornerFrequency();
            this.dataCount = meas.getDataCount();
            this.stationCount = meas.getStationCount();
            this.iterations = meas.getIterations();
            this.misfit = meas.getMisfit();
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

    public Double getMw() {
        return mw;
    }

    public Double getObsEnergy() {
        return obsEnergy;
    }

    public MeasuredMwDetails setObsEnergy(Double obsEnergy) {
        this.obsEnergy = obsEnergy;
        return this;
    }

    public Double getTotalEnergy() {
        return logTotalEnergy;
    }

    public MeasuredMwDetails setTotalEnergy(Double totalEnergy) {
        this.logTotalEnergy = totalEnergy;
        return this;
    }

    public Double getTotalEnergyMDAC() {
        return logTotalEnergyMDAC;
    }

    public MeasuredMwDetails setTotalEnergyMDAC(Double totalEnergyMDAC) {
        this.logTotalEnergyMDAC = totalEnergyMDAC;
        return this;
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

    public Double getMwSd() {
        return mwSd;
    }

    public MeasuredMwDetails setMwSd(Double mwSd) {
        this.mwSd = mwSd;
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

    public Double getApparentStressInMpa() {
        return apparentStressInMpa;
    }

    public MeasuredMwDetails setApparentStressInMpa(Double apparentStressInMpa) {
        this.apparentStressInMpa = apparentStressInMpa;
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

    public Double getCornerFreqSd() {
        return cornerFreqSd;
    }

    public MeasuredMwDetails setCornerFreqSd(Double cornerFreqSd) {
        this.cornerFreqSd = cornerFreqSd;
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

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("MeasuredMwDetails [eventId=")
               .append(eventId)
               .append(", mw=")
               .append(mw)
               .append(", obsEnergy=")
               .append(obsEnergy)
               .append(", logTotalEnergy=")
               .append(logTotalEnergy)
               .append(", logTotalEnergyMDAC=")
               .append(logTotalEnergyMDAC)
               .append(", energyRatio=")
               .append(energyRatio)
               .append(", obsAppStress=")
               .append(obsAppStress)
               .append(", mwSd=")
               .append(mwSd)
               .append(", mw1Max=")
               .append(mw1Max)
               .append(", mw1Min=")
               .append(mw1Min)
               .append(", mw2Max=")
               .append(mw2Max)
               .append(", mw2Min=")
               .append(mw2Min)
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
               .append(", refApparentStressInMpa=")
               .append(refApparentStressInMpa)
               .append(", valApparentStressInMpa=")
               .append(valApparentStressInMpa)
               .append(", cornerFreq=")
               .append(cornerFreq)
               .append(", cornerFreqSd=")
               .append(cornerFreqSd)
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
               .append(", bandCoverage=")
               .append(bandCoverage)
               .append(", likelyPoorlyConstrained=")
               .append(likelyPoorlyConstrained)
               .append("]");
        return builder.toString();
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
                    cornerFreqSd,
                    dataCount,
                    datetime,
                    depth,
                    energyRatio,
                    eventId,
                    iterations,
                    latitude,
                    likelyPoorlyConstrained,
                    logTotalEnergy,
                    logTotalEnergyMDAC,
                    longitude,
                    misfit,
                    mw,
                    mw1Max,
                    mw1Min,
                    mw2Max,
                    mw2Min,
                    mwSd,
                    obsAppStress,
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
                && Objects.equals(cornerFreqSd, other.cornerFreqSd)
                && Objects.equals(dataCount, other.dataCount)
                && Objects.equals(datetime, other.datetime)
                && Objects.equals(depth, other.depth)
                && Objects.equals(energyRatio, other.energyRatio)
                && Objects.equals(eventId, other.eventId)
                && Objects.equals(iterations, other.iterations)
                && Objects.equals(latitude, other.latitude)
                && Objects.equals(likelyPoorlyConstrained, other.likelyPoorlyConstrained)
                && Objects.equals(logTotalEnergy, other.logTotalEnergy)
                && Objects.equals(logTotalEnergyMDAC, other.logTotalEnergyMDAC)
                && Objects.equals(longitude, other.longitude)
                && Objects.equals(misfit, other.misfit)
                && Objects.equals(mw, other.mw)
                && Objects.equals(mw1Max, other.mw1Max)
                && Objects.equals(mw1Min, other.mw1Min)
                && Objects.equals(mw2Max, other.mw2Max)
                && Objects.equals(mw2Min, other.mw2Min)
                && Objects.equals(mwSd, other.mwSd)
                && Objects.equals(obsAppStress, other.obsAppStress)
                && Objects.equals(obsEnergy, other.obsEnergy)
                && Objects.equals(refApparentStressInMpa, other.refApparentStressInMpa)
                && Objects.equals(refMw, other.refMw)
                && Objects.equals(stationCount, other.stationCount)
                && Objects.equals(valApparentStressInMpa, other.valApparentStressInMpa)
                && Objects.equals(valMw, other.valMw);
    }

    @JsonIgnore
    public boolean isValid() {
        return eventId != null;
    }
}
