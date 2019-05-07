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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

@Entity
@Table(name = "Peak_Velocity_Measurements")
public class PeakVelocityMeasurement implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @OneToOne(optional = false)
    @JoinColumn(unique = true)
    private Waveform waveform;

    @Column(name = "timeSec")
    @NumberFormat
    private double timeSecFromOrigin;

    @NumberFormat
    private double snr;

    @NumberFormat
    private double velocity;

    @NumberFormat
    private double amplitude;

    @NumberFormat
    private double distance;

    @NumberFormat
    private double noiseStartSecondsFromOrigin;

    @NumberFormat
    private double noiseEndSecondsFromOrigin;

    @NumberFormat
    private double noiseLevel;

    public PeakVelocityMeasurement() {
        //nop
    }

    public PeakVelocityMeasurement(PeakVelocityMeasurementMetadata metadata) {
        this.id = metadata.getId();
        this.version = metadata.getVersion();
        this.timeSecFromOrigin = metadata.getTimeSecFromOrigin();
        this.snr = metadata.getSnr();
        this.velocity = metadata.getVelocity();
        this.amplitude = metadata.getAmplitude();
        this.distance = metadata.getDistance();
        this.noiseStartSecondsFromOrigin = metadata.getNoiseStartSecondsFromOrigin();
        this.noiseEndSecondsFromOrigin = metadata.getNoiseEndSecondsFromOrigin();
        this.noiseLevel = metadata.getNoiseLevel();
        this.waveform = new Waveform(metadata.getWaveform());
    }

    public Integer getVersion() {
        return version;
    }

    public Waveform getWaveform() {
        return waveform;
    }

    public PeakVelocityMeasurement setWaveform(Waveform waveform) {
        this.waveform = waveform;
        return this;
    }

    public double getTime() {
        return timeSecFromOrigin;
    }

    public PeakVelocityMeasurement setTime(double timeMsFromOrigin) {
        this.timeSecFromOrigin = timeMsFromOrigin;
        return this;
    }

    public double getSnr() {
        return snr;
    }

    public PeakVelocityMeasurement setSnr(double snr) {
        this.snr = snr;
        return this;
    }

    public double getVelocity() {
        return velocity;
    }

    public PeakVelocityMeasurement setVelocity(double velocity) {
        this.velocity = velocity;
        return this;
    }

    public double getAmplitude() {
        return amplitude;
    }

    public PeakVelocityMeasurement setAmplitude(double amplitude) {
        this.amplitude = amplitude;
        return this;
    }

    public double getDistance() {
        return distance;
    }

    public PeakVelocityMeasurement setDistance(double distance) {
        this.distance = distance;
        return this;
    }

    public Long getId() {
        return id;
    }

    public PeakVelocityMeasurement setId(Long id) {
        this.id = id;
        return this;
    }

    public double getTimeSecFromOrigin() {
        return timeSecFromOrigin;
    }

    public PeakVelocityMeasurement setTimeSecFromOrigin(double timeSecFromOrigin) {
        this.timeSecFromOrigin = timeSecFromOrigin;
        return this;
    }

    public double getNoiseStartSecondsFromOrigin() {
        return noiseStartSecondsFromOrigin;
    }

    public PeakVelocityMeasurement setNoiseStartSecondsFromOrigin(double noiseStartSecondsFromOrigin) {
        this.noiseStartSecondsFromOrigin = noiseStartSecondsFromOrigin;
        return this;
    }

    public double getNoiseEndSecondsFromOrigin() {
        return noiseEndSecondsFromOrigin;
    }

    public PeakVelocityMeasurement setNoiseEndSecondsFromOrigin(double noiseEndSecondsFromOrigin) {
        this.noiseEndSecondsFromOrigin = noiseEndSecondsFromOrigin;
        return this;
    }

    public double getNoiseLevel() {
        return noiseLevel;
    }

    public PeakVelocityMeasurement setNoiseLevel(double noiseLevel) {
        this.noiseLevel = noiseLevel;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(amplitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(distance);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        temp = Double.doubleToLongBits(noiseEndSecondsFromOrigin);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(noiseLevel);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(noiseStartSecondsFromOrigin);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(snr);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(timeSecFromOrigin);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(velocity);
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
        PeakVelocityMeasurement other = (PeakVelocityMeasurement) obj;
        if (Double.doubleToLongBits(amplitude) != Double.doubleToLongBits(other.amplitude)) {
            return false;
        }
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
        if (Double.doubleToLongBits(noiseEndSecondsFromOrigin) != Double.doubleToLongBits(other.noiseEndSecondsFromOrigin)) {
            return false;
        }
        if (Double.doubleToLongBits(noiseLevel) != Double.doubleToLongBits(other.noiseLevel)) {
            return false;
        }
        if (Double.doubleToLongBits(noiseStartSecondsFromOrigin) != Double.doubleToLongBits(other.noiseStartSecondsFromOrigin)) {
            return false;
        }
        if (Double.doubleToLongBits(snr) != Double.doubleToLongBits(other.snr)) {
            return false;
        }
        if (Double.doubleToLongBits(timeSecFromOrigin) != Double.doubleToLongBits(other.timeSecFromOrigin)) {
            return false;
        }
        if (Double.doubleToLongBits(velocity) != Double.doubleToLongBits(other.velocity)) {
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
        StringBuilder builder = new StringBuilder();
        builder.append("\"")
               .append(id)
               .append("\", \"")
               .append(version)
               .append("\", \"")
               .append(waveform)
               .append("\", \"")
               .append(timeSecFromOrigin)
               .append("\", \"")
               .append(snr)
               .append("\", \"")
               .append(velocity)
               .append("\", \"")
               .append(amplitude)
               .append("\", \"")
               .append(distance)
               .append("\", \"")
               .append(noiseStartSecondsFromOrigin)
               .append("\", \"")
               .append(noiseEndSecondsFromOrigin)
               .append("\", \"")
               .append(noiseLevel)
               .append('\"');
        return builder.toString();
    }

}
