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
    private Double apparentStressInMpa;

    @NumberFormat
    @Column(nullable = true)
    private Double meanApparentStressInMpa;

    @NumberFormat
    @Column(nullable = true)
    private Double stressSd;

    @NumberFormat
    @Column(nullable = true)
    private Double misfit;

    @NumberFormat
    @Column(nullable = true)
    private Double meanMisfit;

    @NumberFormat
    @Column(nullable = true)
    private Double misfitSd;

    private int dataCount;

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

    public Double getApparentStressInMpa() {
        return apparentStressInMpa;
    }

    public MeasuredMwParameters setApparentStressInMpa(Double apparentStressInMpa) {
        this.apparentStressInMpa = apparentStressInMpa;
        return this;
    }

    public Double getStressSd() {
        return stressSd;
    }

    public MeasuredMwParameters setStressSd(Double stressSd) {
        this.stressSd = stressSd;
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

    public MeasuredMwParameters merge(MeasuredMwParameters overlay) {
        this.mw = overlay.getMw();
        this.meanMw = overlay.getMeanMw();
        this.mwSd = overlay.getMwSd();
        this.apparentStressInMpa = overlay.getApparentStressInMpa();
        this.meanApparentStressInMpa = overlay.getMeanApparentStressInMpa();
        this.stressSd = overlay.getStressSd();
        this.misfit = overlay.getMisfit();
        this.meanMisfit = overlay.getMeanMisfit();
        this.misfitSd = overlay.getMisfitSd();
        this.dataCount = overlay.getDataCount();
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(apparentStressInMpa, dataCount, eventId, id, meanApparentStressInMpa, meanMisfit, meanMw, misfit, misfitSd, mw, mwSd, stressSd, version);
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
        return Objects.equals(apparentStressInMpa, other.apparentStressInMpa)
                && dataCount == other.dataCount
                && Objects.equals(eventId, other.eventId)
                && Objects.equals(id, other.id)
                && Objects.equals(meanApparentStressInMpa, other.meanApparentStressInMpa)
                && Objects.equals(meanMisfit, other.meanMisfit)
                && Objects.equals(meanMw, other.meanMw)
                && Objects.equals(misfit, other.misfit)
                && Objects.equals(misfitSd, other.misfitSd)
                && Double.doubleToLongBits(mw) == Double.doubleToLongBits(other.mw)
                && Objects.equals(mwSd, other.mwSd)
                && Objects.equals(stressSd, other.stressSd)
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
               .append(", apparentStressInMpa=")
               .append(apparentStressInMpa)
               .append(", meanApparentStressInMpa=")
               .append(meanApparentStressInMpa)
               .append(", stressSd=")
               .append(stressSd)
               .append(", misfit=")
               .append(misfit)
               .append(", meanMisfit=")
               .append(meanMisfit)
               .append(", misfitSd=")
               .append(misfitSd)
               .append(", dataCount=")
               .append(dataCount)
               .append("]");
        return builder.toString();
    }

}