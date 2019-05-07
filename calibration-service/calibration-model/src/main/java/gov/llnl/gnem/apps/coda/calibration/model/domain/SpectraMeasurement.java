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
@Table(name = "Spectra_Measurements")
public class SpectraMeasurement implements Serializable {

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

    @NumberFormat
    private double rawAtStart;

    @NumberFormat
    private double rawAtMeasurementTime;

    @NumberFormat
    private double pathCorrected;

    @NumberFormat
    private double pathAndSiteCorrected;

    @NumberFormat
    private double startCutSec;

    @NumberFormat
    private double endCutSec;

    @NumberFormat
    private double rmsFit;

    public SpectraMeasurement() {
        //nop
    }

    public SpectraMeasurement(SpectraMeasurementMetadata md) {
        this.id = md.getId();
        this.version = md.getVersion();
        this.waveform = new Waveform(md.getWaveform());
        this.rawAtStart = md.getRawAtStart();
        this.rawAtMeasurementTime = md.getRawAtMeasurementTime();
        this.pathCorrected = md.getPathCorrected();
        this.pathAndSiteCorrected = md.getPathAndSiteCorrected();
        this.startCutSec = md.getStartCutSec();
        this.endCutSec = md.getEndCutSec();
        this.rmsFit = md.getRmsFit();
    }

    public Integer getVersion() {
        return version;
    }

    public Waveform getWaveform() {
        return waveform;
    }

    public SpectraMeasurement setWaveform(Waveform waveform) {
        this.waveform = waveform;
        return this;
    }

    public Long getId() {
        return id;
    }

    public SpectraMeasurement setId(Long id) {
        this.id = id;
        return this;
    }

    public double getRawAtStart() {
        return rawAtStart;
    }

    public SpectraMeasurement setRawAtStart(double rawAtStart) {
        this.rawAtStart = rawAtStart;
        return this;
    }

    public double getRawAtMeasurementTime() {
        return rawAtMeasurementTime;
    }

    public SpectraMeasurement setRawAtMeasurementTime(double rawAtMeasurementTime) {
        this.rawAtMeasurementTime = rawAtMeasurementTime;
        return this;
    }

    public double getPathCorrected() {
        return pathCorrected;
    }

    public SpectraMeasurement setPathCorrected(double pathCorrected) {
        this.pathCorrected = pathCorrected;
        return this;
    }

    public double getPathAndSiteCorrected() {
        return pathAndSiteCorrected;
    }

    public SpectraMeasurement setPathAndSiteCorrected(double pathAndSiteCorrected) {
        this.pathAndSiteCorrected = pathAndSiteCorrected;
        return this;
    }

    public double getRmsFit() {
        return rmsFit;
    }

    public SpectraMeasurement setRmsFit(double rmsFit) {
        this.rmsFit = rmsFit;
        return this;
    }

    public static long getSerialversionuid() {
        return serialVersionUID;
    }

    public double getStartCutSec() {
        return startCutSec;
    }

    public SpectraMeasurement setStartCutSec(double startCutSec) {
        this.startCutSec = startCutSec;
        return this;
    }

    public double getEndCutSec() {
        return endCutSec;
    }

    public SpectraMeasurement setEndCutSec(double endCutSec) {
        this.endCutSec = endCutSec;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(endCutSec);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        temp = Double.doubleToLongBits(pathAndSiteCorrected);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(pathCorrected);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(rawAtMeasurementTime);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(rawAtStart);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(rmsFit);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(startCutSec);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        result = prime * result + ((waveform == null) ? 0 : waveform.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        SpectraMeasurement other = (SpectraMeasurement) obj;
        if (Double.doubleToLongBits(endCutSec) != Double.doubleToLongBits(other.endCutSec)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (Double.doubleToLongBits(pathAndSiteCorrected) != Double.doubleToLongBits(other.pathAndSiteCorrected)) {
            return false;
        }
        if (Double.doubleToLongBits(pathCorrected) != Double.doubleToLongBits(other.pathCorrected)) {
            return false;
        }
        if (Double.doubleToLongBits(rawAtMeasurementTime) != Double.doubleToLongBits(other.rawAtMeasurementTime)) {
            return false;
        }
        if (Double.doubleToLongBits(rawAtStart) != Double.doubleToLongBits(other.rawAtStart)) {
            return false;
        }
        if (Double.doubleToLongBits(rmsFit) != Double.doubleToLongBits(other.rmsFit)) {
            return false;
        }
        if (Double.doubleToLongBits(startCutSec) != Double.doubleToLongBits(other.startCutSec)) {
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

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SpectraMeasurement [id="
                + id
                + ", version="
                + version
                + ", waveform="
                + waveform
                + ", rawAtStart="
                + rawAtStart
                + ", rawAtMeasurementTime="
                + rawAtMeasurementTime
                + ", pathCorrected="
                + pathCorrected
                + ", pathAndSiteCorrected="
                + pathAndSiteCorrected
                + ", startCutSec="
                + startCutSec
                + ", endCutSec="
                + endCutSec
                + ", rmsFit="
                + rmsFit
                + "]";
    }

}
