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

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;

@Entity
@Table(name = "Path_Measurements")
public class PathCalibrationMeasurement implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @NumberFormat
    private Double initialResidual;

    @NumberFormat
    private Double finalResidual;

    @Embedded
    private FrequencyBand frequencyBand;

    public Long getId() {
        return id;
    }

    public PathCalibrationMeasurement setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public Double getInitialResidual() {
        return initialResidual;
    }

    public PathCalibrationMeasurement setInitialResidual(Double initialResidual) {
        this.initialResidual = initialResidual;
        return this;
    }

    public Double getFinalResidual() {
        return finalResidual;
    }

    public PathCalibrationMeasurement setFinalResidual(Double finalResidual) {
        this.finalResidual = finalResidual;
        return this;
    }

    public FrequencyBand getFrequencyBand() {
        return frequencyBand;
    }

    public PathCalibrationMeasurement setFrequencyBand(FrequencyBand frequencyBand) {
        this.frequencyBand = frequencyBand;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((finalResidual == null) ? 0 : finalResidual.hashCode());
        result = prime * result + ((frequencyBand == null) ? 0 : frequencyBand.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((initialResidual == null) ? 0 : initialResidual.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        PathCalibrationMeasurement other = (PathCalibrationMeasurement) obj;
        if (finalResidual == null) {
            if (other.finalResidual != null) {
                return false;
            }
        } else if (!finalResidual.equals(other.finalResidual)) {
            return false;
        }
        if (frequencyBand == null) {
            if (other.frequencyBand != null) {
                return false;
            }
        } else if (!frequencyBand.equals(other.frequencyBand)) {
            return false;
        }
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (initialResidual == null) {
            if (other.initialResidual != null) {
                return false;
            }
        } else if (!initialResidual.equals(other.initialResidual)) {
            return false;
        }
        if (version == null) {
            if (other.version != null) {
                return false;
            }
        } else if (!version.equals(other.version)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "PathCalibrationMeasurement [id=" + id + ", version=" + version + ", initialResidual=" + initialResidual + ", finalResidual=" + finalResidual + ", frequencyBand=" + frequencyBand + "]";
    }

}
