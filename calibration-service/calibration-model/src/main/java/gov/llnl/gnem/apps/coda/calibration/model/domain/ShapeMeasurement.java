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
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Version;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

@Entity
@Table(name = "Shape_Measurements")
public class ShapeMeasurement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Waveform waveform;

    @NumberFormat
    private double v0;

    @NumberFormat
    private double v1;

    @NumberFormat
    private double v2;

    @NumberFormat
    private double measuredBeta;

    @NumberFormat
    private double measuredGamma;

    @NumberFormat
    private double measuredIntercept;

    @NumberFormat
    private double measuredError;

    @Column(name = "measuredTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date measuredTime;

    @NumberFormat
    private double distance;

    @NumberFormat
    private double timeDifference;

    public Long getId() {
        return id;
    }

    public ShapeMeasurement setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public ShapeMeasurement setVersion(Long version) {
        this.version = version;
        return this;
    }

    public Waveform getWaveform() {
        return waveform;
    }

    public ShapeMeasurement setWaveform(Waveform waveform) {
        this.waveform = waveform;
        return this;
    }

    public double getV0() {
        return v0;
    }

    public ShapeMeasurement setV0(double v0) {
        this.v0 = v0;
        return this;
    }

    public double getV1() {
        return v1;
    }

    public ShapeMeasurement setV1(double v1) {
        this.v1 = v1;
        return this;
    }

    public double getV2() {
        return v2;
    }

    public ShapeMeasurement setV2(double v2) {
        this.v2 = v2;
        return this;
    }

    public double getMeasuredBeta() {
        return measuredBeta;
    }

    public ShapeMeasurement setMeasuredBeta(double measuredBeta) {
        this.measuredBeta = measuredBeta;
        return this;
    }

    public double getMeasuredGamma() {
        return measuredGamma;
    }

    public ShapeMeasurement setMeasuredGamma(double measuredGamma) {
        this.measuredGamma = measuredGamma;
        return this;
    }

    public double getMeasuredIntercept() {
        return measuredIntercept;
    }

    public ShapeMeasurement setMeasuredIntercept(double measuredIntercept) {
        this.measuredIntercept = measuredIntercept;
        return this;
    }

    public double getMeasuredError() {
        return measuredError;
    }

    public ShapeMeasurement setMeasuredError(double measuredError) {
        this.measuredError = measuredError;
        return this;
    }

    public Date getMeasuredTime() {
        return this.measuredTime;
    }

    public ShapeMeasurement setMeasuredTime(Date measuredTime) {
        this.measuredTime = measuredTime;
        return this;
    }

    public double getTimeDifference() {
        return timeDifference;
    }

    public ShapeMeasurement setTimeDifference(double timeDifference) {
        this.timeDifference = timeDifference;
        return this;
    }

    public double getDistance() {
        return distance;
    }

    public ShapeMeasurement setDistance(double distance) {
        this.distance = distance;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(distance);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        temp = Double.doubleToLongBits(measuredBeta);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(measuredError);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(measuredGamma);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(measuredIntercept);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(timeDifference);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v0);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v1);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(v2);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((waveform == null) ? 0 : waveform.hashCode());
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
        ShapeMeasurement other = (ShapeMeasurement) obj;
        if (Double.doubleToLongBits(distance) != Double.doubleToLongBits(other.distance)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (Double.doubleToLongBits(measuredBeta) != Double.doubleToLongBits(other.measuredBeta)) {
            return false;
        }
        if (Double.doubleToLongBits(measuredError) != Double.doubleToLongBits(other.measuredError)) {
            return false;
        }
        if (Double.doubleToLongBits(measuredGamma) != Double.doubleToLongBits(other.measuredGamma)) {
            return false;
        }
        if (Double.doubleToLongBits(measuredIntercept) != Double.doubleToLongBits(other.measuredIntercept)) {
            return false;
        }
        if (Double.doubleToLongBits(timeDifference) != Double.doubleToLongBits(other.timeDifference)) {
            return false;
        }
        if (Double.doubleToLongBits(v0) != Double.doubleToLongBits(other.v0)) {
            return false;
        }
        if (Double.doubleToLongBits(v1) != Double.doubleToLongBits(other.v1)) {
            return false;
        }
        if (Double.doubleToLongBits(v2) != Double.doubleToLongBits(other.v2)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        if (waveform == null) {
            if (other.waveform != null) {
                return false;
            }
        } else if (!waveform.equals(other.waveform)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "ShapeMeasurement [id="
                + id
                + ", version="
                + version
                + ", waveform="
                + waveform
                + ", v0="
                + v0
                + ", v1="
                + v1
                + ", v2="
                + v2
                + ", measuredBeta="
                + measuredBeta
                + ", measuredGamma="
                + measuredGamma
                + ", measuredIntercept="
                + measuredIntercept
                + ", measuredError="
                + measuredError
                + ", distance="
                + distance
                + ", timeDifference="
                + timeDifference
                + "]";
    }

}
