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

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

import com.fasterxml.jackson.annotation.JsonAlias;

import gov.llnl.gnem.apps.coda.common.model.util.Durable;

@Durable
@Entity
@Table(name = "Shared_FB_Params")
public class SharedFrequencyBandParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @Embedded
    private FrequencyBand frequencyBand = new FrequencyBand();

    @NumberFormat
    private double velocity0;

    @NumberFormat
    private double velocity1;

    @NumberFormat
    private double velocity2;

    @NumberFormat
    private double beta0;

    @NumberFormat
    private double beta1;

    @NumberFormat
    private double beta2;

    @NumberFormat
    private double gamma0;

    @NumberFormat
    private double gamma1;

    @NumberFormat
    private double gamma2;

    @NumberFormat
    private double minSnr;

    @JsonAlias("s1")
    @NumberFormat
    private double p1;

    @JsonAlias("s2")
    @NumberFormat
    private double p2;

    @NumberFormat
    private double xc;

    @NumberFormat
    private double xt;

    @NumberFormat
    private double q;

    @NumberFormat
    private double minLength;

    @NumberFormat
    private double maxLength;

    @Column(name = "MEASURE_TIME")
    @NumberFormat
    private double measurementTime = 0d;

    @NumberFormat
    private double codaStartOffset = 0d;

    public Long getId() {
        return this.id;
    }

    public SharedFrequencyBandParameters setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return this.version;
    }

    public double getLowFrequency() {
        return this.frequencyBand.getLowFrequency();
    }

    public SharedFrequencyBandParameters setLowFrequency(double lowFrequency) {
        this.frequencyBand.setLowFrequency(lowFrequency);
        return this;
    }

    public double getHighFrequency() {
        return this.frequencyBand.getHighFrequency();
    }

    public SharedFrequencyBandParameters setHighFrequency(double highFrequency) {
        this.frequencyBand.setHighFrequency(highFrequency);
        return this;
    }

    public double getVelocity0() {
        return this.velocity0;
    }

    public SharedFrequencyBandParameters setVelocity0(double velocity0) {
        this.velocity0 = velocity0;
        return this;
    }

    public double getVelocity1() {
        return this.velocity1;
    }

    public SharedFrequencyBandParameters setVelocity1(double velocity1) {
        this.velocity1 = velocity1;
        return this;
    }

    public double getVelocity2() {
        return this.velocity2;
    }

    public SharedFrequencyBandParameters setVelocity2(double velocity2) {
        this.velocity2 = velocity2;
        return this;
    }

    public double getBeta0() {
        return this.beta0;
    }

    public SharedFrequencyBandParameters setBeta0(double beta0) {
        this.beta0 = beta0;
        return this;
    }

    public double getBeta1() {
        return this.beta1;
    }

    public SharedFrequencyBandParameters setBeta1(double beta1) {
        this.beta1 = beta1;
        return this;
    }

    public double getBeta2() {
        return this.beta2;
    }

    public SharedFrequencyBandParameters setBeta2(double beta2) {
        this.beta2 = beta2;
        return this;
    }

    public double getGamma0() {
        return this.gamma0;
    }

    public SharedFrequencyBandParameters setGamma0(double gamma0) {
        this.gamma0 = gamma0;
        return this;
    }

    public double getGamma1() {
        return this.gamma1;
    }

    public SharedFrequencyBandParameters setGamma1(double gamma1) {
        this.gamma1 = gamma1;
        return this;
    }

    public double getGamma2() {
        return this.gamma2;
    }

    public SharedFrequencyBandParameters setGamma2(double gamma2) {
        this.gamma2 = gamma2;
        return this;
    }

    public double getMinSnr() {
        return this.minSnr;
    }

    public SharedFrequencyBandParameters setMinSnr(double minSnr) {
        this.minSnr = minSnr;
        return this;
    }

    public double getP1() {
        return this.p1;
    }

    public SharedFrequencyBandParameters setP1(double p1) {
        this.p1 = p1;
        return this;
    }

    public double getS2() {
        return this.p2;
    }

    public SharedFrequencyBandParameters setS2(double p2) {
        this.p2 = p2;
        return this;
    }

    public double getXc() {
        return this.xc;
    }

    public SharedFrequencyBandParameters setXc(double xc) {
        this.xc = xc;
        return this;
    }

    public double getXt() {
        return this.xt;
    }

    public SharedFrequencyBandParameters setXt(double xt) {
        this.xt = xt;
        return this;
    }

    public double getQ() {
        return this.q;
    }

    public SharedFrequencyBandParameters setQ(double q) {
        this.q = q;
        return this;
    }

    public double getMinLength() {
        return minLength;
    }

    public SharedFrequencyBandParameters setMinLength(double minLength) {
        this.minLength = minLength;
        return this;
    }

    public double getMaxLength() {
        return maxLength;
    }

    public SharedFrequencyBandParameters setMaxLength(double maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public double getCodaStartOffset() {
        return codaStartOffset;
    }

    public SharedFrequencyBandParameters setCodaStartOffset(double codaStartOffset) {
        this.codaStartOffset = codaStartOffset;
        return this;
    }

    public double getMeasurementTime() {
        return measurementTime;
    }

    public SharedFrequencyBandParameters setMeasurementTime(double measurementTime) {
        this.measurementTime = measurementTime;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SharedFrequencyBandParameters [id=")
               .append(id)
               .append(", version=")
               .append(version)
               .append(", frequencyBand=")
               .append(frequencyBand)
               .append(", velocity0=")
               .append(velocity0)
               .append(", velocity1=")
               .append(velocity1)
               .append(", velocity2=")
               .append(velocity2)
               .append(", beta0=")
               .append(beta0)
               .append(", beta1=")
               .append(beta1)
               .append(", beta2=")
               .append(beta2)
               .append(", gamma0=")
               .append(gamma0)
               .append(", gamma1=")
               .append(gamma1)
               .append(", gamma2=")
               .append(gamma2)
               .append(", minSnr=")
               .append(minSnr)
               .append(", p1=")
               .append(p1)
               .append(", p2=")
               .append(p2)
               .append(", xc=")
               .append(xc)
               .append(", xt=")
               .append(xt)
               .append(", q=")
               .append(q)
               .append(", minLength=")
               .append(minLength)
               .append(", maxLength=")
               .append(maxLength)
               .append(", measurementTime=")
               .append(measurementTime)
               .append(", codaStartOffset=")
               .append(codaStartOffset)
               .append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                beta0,
                    beta1,
                    beta2,
                    codaStartOffset,
                    frequencyBand,
                    gamma0,
                    gamma1,
                    gamma2,
                    maxLength,
                    measurementTime,
                    minLength,
                    minSnr,
                    p1,
                    p2,
                    q,
                    velocity0,
                    velocity1,
                    velocity2,
                    xc,
                    xt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SharedFrequencyBandParameters)) {
            return false;
        }
        SharedFrequencyBandParameters other = (SharedFrequencyBandParameters) obj;
        return Double.doubleToLongBits(beta0) == Double.doubleToLongBits(other.beta0)
                && Double.doubleToLongBits(beta1) == Double.doubleToLongBits(other.beta1)
                && Double.doubleToLongBits(beta2) == Double.doubleToLongBits(other.beta2)
                && Double.doubleToLongBits(codaStartOffset) == Double.doubleToLongBits(other.codaStartOffset)
                && Objects.equals(frequencyBand, other.frequencyBand)
                && Double.doubleToLongBits(gamma0) == Double.doubleToLongBits(other.gamma0)
                && Double.doubleToLongBits(gamma1) == Double.doubleToLongBits(other.gamma1)
                && Double.doubleToLongBits(gamma2) == Double.doubleToLongBits(other.gamma2)
                && Double.doubleToLongBits(maxLength) == Double.doubleToLongBits(other.maxLength)
                && Double.doubleToLongBits(measurementTime) == Double.doubleToLongBits(other.measurementTime)
                && Double.doubleToLongBits(minLength) == Double.doubleToLongBits(other.minLength)
                && Double.doubleToLongBits(minSnr) == Double.doubleToLongBits(other.minSnr)
                && Double.doubleToLongBits(p1) == Double.doubleToLongBits(other.p1)
                && Double.doubleToLongBits(p2) == Double.doubleToLongBits(other.p2)
                && Double.doubleToLongBits(q) == Double.doubleToLongBits(other.q)
                && Double.doubleToLongBits(velocity0) == Double.doubleToLongBits(other.velocity0)
                && Double.doubleToLongBits(velocity1) == Double.doubleToLongBits(other.velocity1)
                && Double.doubleToLongBits(velocity2) == Double.doubleToLongBits(other.velocity2)
                && Double.doubleToLongBits(xc) == Double.doubleToLongBits(other.xc)
                && Double.doubleToLongBits(xt) == Double.doubleToLongBits(other.xt);
    }

    public SharedFrequencyBandParameters mergeNonNullOrEmptyFields(SharedFrequencyBandParameters overlay) {
        if (overlay.frequencyBand != null && overlay.getLowFrequency() != 0.0 && overlay.getHighFrequency() != 0.0) {
            this.frequencyBand = overlay.frequencyBand;
        }
        this.velocity0 = overlay.velocity0;
        this.velocity1 = overlay.velocity1;
        this.velocity2 = overlay.velocity2;
        this.beta0 = overlay.beta0;
        this.beta1 = overlay.beta1;
        this.beta2 = overlay.beta2;
        this.gamma0 = overlay.gamma0;
        this.gamma1 = overlay.gamma1;
        this.gamma2 = overlay.gamma2;
        this.minSnr = overlay.minSnr;
        this.p1 = overlay.p1;
        this.p2 = overlay.p2;
        this.xc = overlay.xc;
        this.xt = overlay.xt;
        this.q = overlay.q;
        this.minLength = overlay.minLength;
        this.maxLength = overlay.maxLength;
        this.codaStartOffset = overlay.getCodaStartOffset();
        this.measurementTime = overlay.measurementTime;

        return this;
    }

}
