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

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

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

    @NumberFormat
    private double s1;

    @NumberFormat
    private double s2;

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
    private double measurementTime = 100d;

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

    public double getS1() {
        return this.s1;
    }

    public SharedFrequencyBandParameters setS1(double s1) {
        this.s1 = s1;
        return this;
    }

    public double getS2() {
        return this.s2;
    }

    public SharedFrequencyBandParameters setS2(double s2) {
        this.s2 = s2;
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

    public double getMeasurementTime() {
        return measurementTime;
    }

    public SharedFrequencyBandParameters setMeasurementTime(double measurementTime) {
        this.measurementTime = measurementTime;
        return this;
    }

    @Override
    public String toString() {
        return "SharedFrequencyBandParameters [id="
                + id
                + ", version="
                + version
                + ", lowFrequency="
                + getLowFrequency()
                + ", highFrequency="
                + getHighFrequency()
                + ", velocity0="
                + velocity0
                + ", velocity1="
                + velocity1
                + ", velocity2="
                + velocity2
                + ", beta0="
                + beta0
                + ", beta1="
                + beta1
                + ", beta2="
                + beta2
                + ", gamma0="
                + gamma0
                + ", gamma1="
                + gamma1
                + ", gamma2="
                + gamma2
                + ", minSnr="
                + minSnr
                + ", s1="
                + s1
                + ", s2="
                + s2
                + ", xc="
                + xc
                + ", xt="
                + xt
                + ", q="
                + q
                + ", minLength="
                + minLength
                + ", maxLength="
                + maxLength
                + ", measurementTime="
                + measurementTime
                + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(beta0);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(beta1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(beta2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((frequencyBand == null) ? 0 : frequencyBand.hashCode());
        temp = Double.doubleToLongBits(gamma0);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(gamma1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(gamma2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxLength);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(measurementTime);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minLength);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minSnr);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(q);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(s1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(s2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(velocity0);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(velocity1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(velocity2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(xc);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(xt);
        result = prime * result + (int) (temp ^ (temp >>> 32));
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
        SharedFrequencyBandParameters other = (SharedFrequencyBandParameters) obj;
        if (Double.doubleToLongBits(beta0) != Double.doubleToLongBits(other.beta0)) {
            return false;
        }
        if (Double.doubleToLongBits(beta1) != Double.doubleToLongBits(other.beta1)) {
            return false;
        }
        if (Double.doubleToLongBits(beta2) != Double.doubleToLongBits(other.beta2)) {
            return false;
        }
        if (frequencyBand == null) {
            if (other.frequencyBand != null) {
                return false;
            }
        } else if (!frequencyBand.equals(other.frequencyBand)) {
            return false;
        }
        if (Double.doubleToLongBits(gamma0) != Double.doubleToLongBits(other.gamma0)) {
            return false;
        }
        if (Double.doubleToLongBits(gamma1) != Double.doubleToLongBits(other.gamma1)) {
            return false;
        }
        if (Double.doubleToLongBits(gamma2) != Double.doubleToLongBits(other.gamma2)) {
            return false;
        }
        if (Double.doubleToLongBits(maxLength) != Double.doubleToLongBits(other.maxLength)) {
            return false;
        }
        if (Double.doubleToLongBits(measurementTime) != Double.doubleToLongBits(other.measurementTime)) {
            return false;
        }
        if (Double.doubleToLongBits(minLength) != Double.doubleToLongBits(other.minLength)) {
            return false;
        }
        if (Double.doubleToLongBits(minSnr) != Double.doubleToLongBits(other.minSnr)) {
            return false;
        }
        if (Double.doubleToLongBits(q) != Double.doubleToLongBits(other.q)) {
            return false;
        }
        if (Double.doubleToLongBits(s1) != Double.doubleToLongBits(other.s1)) {
            return false;
        }
        if (Double.doubleToLongBits(s2) != Double.doubleToLongBits(other.s2)) {
            return false;
        }
        if (Double.doubleToLongBits(velocity0) != Double.doubleToLongBits(other.velocity0)) {
            return false;
        }
        if (Double.doubleToLongBits(velocity1) != Double.doubleToLongBits(other.velocity1)) {
            return false;
        }
        if (Double.doubleToLongBits(velocity2) != Double.doubleToLongBits(other.velocity2)) {
            return false;
        }
        if (Double.doubleToLongBits(xc) != Double.doubleToLongBits(other.xc)) {
            return false;
        }
        if (Double.doubleToLongBits(xt) != Double.doubleToLongBits(other.xt)) {
            return false;
        }
        return true;
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
        this.s1 = overlay.s1;
        this.s2 = overlay.s2;
        this.xc = overlay.xc;
        this.xt = overlay.xt;
        this.q = overlay.q;
        this.minLength = overlay.minLength;
        this.maxLength = overlay.maxLength;
        this.measurementTime = overlay.measurementTime;

        return this;
    }

}
