/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

import com.fasterxml.jackson.annotation.JsonAlias;

import gov.llnl.gnem.apps.coda.common.model.util.Durable;

@Durable
@Entity
@Table(name = "Velocity_Configuration")
public class VelocityConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @JsonAlias("phaseSpeedInKms")
    @NumberFormat
    private Double phaseVelocityInKms;

    @NumberFormat
    private Double groupVelocity1InKmsGtDistance;

    @NumberFormat
    private Double groupVelocity2InKmsGtDistance;

    @NumberFormat
    private Double groupVelocity1InKmsLtDistance;

    @NumberFormat
    private Double groupVelocity2InKmsLtDistance;

    @NumberFormat
    private Double distanceThresholdInKm;

    public Long getId() {
        return id;
    }

    public Integer getVersion() {
        return version;
    }

    public Double getPhaseVelocityInKms() {
        return phaseVelocityInKms;
    }

    public VelocityConfiguration setPhaseVelocityInKms(Double phaseVelocityInKms) {
        this.phaseVelocityInKms = phaseVelocityInKms;
        return this;
    }

    public Double getGroupVelocity1InKmsGtDistance() {
        return groupVelocity1InKmsGtDistance;
    }

    public VelocityConfiguration setGroupVelocity1InKmsGtDistance(Double groupVelocity1InKmsGtDistance) {
        this.groupVelocity1InKmsGtDistance = groupVelocity1InKmsGtDistance;
        return this;
    }

    public Double getGroupVelocity2InKmsGtDistance() {
        return groupVelocity2InKmsGtDistance;
    }

    public VelocityConfiguration setGroupVelocity2InKmsGtDistance(Double groupVelocity2InKmsGtDistance) {
        this.groupVelocity2InKmsGtDistance = groupVelocity2InKmsGtDistance;
        return this;
    }

    public Double getGroupVelocity1InKmsLtDistance() {
        return groupVelocity1InKmsLtDistance;
    }

    public VelocityConfiguration setGroupVelocity1InKmsLtDistance(Double groupVelocity1InKmsLtDistance) {
        this.groupVelocity1InKmsLtDistance = groupVelocity1InKmsLtDistance;
        return this;
    }

    public Double getGroupVelocity2InKmsLtDistance() {
        return groupVelocity2InKmsLtDistance;
    }

    public VelocityConfiguration setGroupVelocity2InKmsLtDistance(Double groupVelocity2InKmsLtDistance) {
        this.groupVelocity2InKmsLtDistance = groupVelocity2InKmsLtDistance;
        return this;
    }

    public Double getDistanceThresholdInKm() {
        return distanceThresholdInKm;
    }

    public VelocityConfiguration setDistanceThresholdInKm(Double distanceThresholdInKm) {
        this.distanceThresholdInKm = distanceThresholdInKm;
        return this;
    }

    public VelocityConfiguration merge(VelocityConfiguration overlay) {
        if (overlay.phaseVelocityInKms != null && overlay.phaseVelocityInKms != 0.0) {
            this.phaseVelocityInKms = overlay.phaseVelocityInKms;
        }
        if (overlay.groupVelocity1InKmsGtDistance != null && overlay.groupVelocity1InKmsGtDistance != 0.0) {
            this.groupVelocity1InKmsGtDistance = overlay.groupVelocity1InKmsGtDistance;
        }
        if (overlay.groupVelocity2InKmsGtDistance != null && overlay.groupVelocity2InKmsGtDistance != 0.0) {
            this.groupVelocity2InKmsGtDistance = overlay.groupVelocity2InKmsGtDistance;
        }
        if (overlay.groupVelocity1InKmsLtDistance != null && overlay.groupVelocity1InKmsLtDistance != 0.0) {
            this.groupVelocity1InKmsLtDistance = overlay.groupVelocity1InKmsLtDistance;
        }
        if (overlay.groupVelocity2InKmsLtDistance != null && overlay.groupVelocity2InKmsLtDistance != 0.0) {
            this.groupVelocity2InKmsLtDistance = overlay.groupVelocity2InKmsLtDistance;
        }
        if (overlay.distanceThresholdInKm != null && overlay.distanceThresholdInKm != 0.0) {
            this.distanceThresholdInKm = overlay.distanceThresholdInKm;
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((distanceThresholdInKm == null) ? 0 : distanceThresholdInKm.hashCode());
        result = prime * result + ((groupVelocity1InKmsGtDistance == null) ? 0 : groupVelocity1InKmsGtDistance.hashCode());
        result = prime * result + ((groupVelocity1InKmsLtDistance == null) ? 0 : groupVelocity1InKmsLtDistance.hashCode());
        result = prime * result + ((groupVelocity2InKmsGtDistance == null) ? 0 : groupVelocity2InKmsGtDistance.hashCode());
        result = prime * result + ((groupVelocity2InKmsLtDistance == null) ? 0 : groupVelocity2InKmsLtDistance.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((phaseVelocityInKms == null) ? 0 : phaseVelocityInKms.hashCode());
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
        VelocityConfiguration other = (VelocityConfiguration) obj;
        if (distanceThresholdInKm == null) {
            if (other.distanceThresholdInKm != null) {
                return false;
            }
        } else if (!distanceThresholdInKm.equals(other.distanceThresholdInKm)) {
            return false;
        }
        if (groupVelocity1InKmsGtDistance == null) {
            if (other.groupVelocity1InKmsGtDistance != null) {
                return false;
            }
        } else if (!groupVelocity1InKmsGtDistance.equals(other.groupVelocity1InKmsGtDistance)) {
            return false;
        }
        if (groupVelocity1InKmsLtDistance == null) {
            if (other.groupVelocity1InKmsLtDistance != null) {
                return false;
            }
        } else if (!groupVelocity1InKmsLtDistance.equals(other.groupVelocity1InKmsLtDistance)) {
            return false;
        }
        if (groupVelocity2InKmsGtDistance == null) {
            if (other.groupVelocity2InKmsGtDistance != null) {
                return false;
            }
        } else if (!groupVelocity2InKmsGtDistance.equals(other.groupVelocity2InKmsGtDistance)) {
            return false;
        }
        if (groupVelocity2InKmsLtDistance == null) {
            if (other.groupVelocity2InKmsLtDistance != null) {
                return false;
            }
        } else if (!groupVelocity2InKmsLtDistance.equals(other.groupVelocity2InKmsLtDistance)) {
            return false;
        }
        if (phaseVelocityInKms == null) {
            if (other.phaseVelocityInKms != null) {
                return false;
            }
        } else if (!phaseVelocityInKms.equals(other.phaseVelocityInKms)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("VelocityConfiguration [id=")
               .append(id)
               .append(", version=")
               .append(version)
               .append(", phaseVelocityInKms=")
               .append(phaseVelocityInKms)
               .append(", groupVelocity1InKmsGtDistance=")
               .append(groupVelocity1InKmsGtDistance)
               .append(", groupVelocity2InKmsGtDistance=")
               .append(groupVelocity2InKmsGtDistance)
               .append(", groupVelocity1InKmsLtDistance=")
               .append(groupVelocity1InKmsLtDistance)
               .append(", groupVelocity2InKmsLtDistance=")
               .append(groupVelocity2InKmsLtDistance)
               .append(", distanceThresholdInKm=")
               .append(distanceThresholdInKm)
               .append("]");
        return builder.toString();
    }
}
