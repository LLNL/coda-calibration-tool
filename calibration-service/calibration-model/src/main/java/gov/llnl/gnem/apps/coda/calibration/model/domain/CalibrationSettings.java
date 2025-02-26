/*
* Copyright (c) 2024, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import gov.llnl.gnem.apps.coda.common.model.util.Durable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Durable
@Entity
@Table(name = "Calibration_Settings")
public class CalibrationSettings implements Serializable {

    public enum DistanceCalcMethod {
        EPICENTRAL("EPICENTRAL"), HYPOCENTRAL("HYPOCENTRAL");

        private String value;

        DistanceCalcMethod(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @Column(name = "distanceCalcMethod")
    private String distanceCalcMethod = DistanceCalcMethod.EPICENTRAL.getValue();

    public Long getId() {
        return this.id;
    }

    public CalibrationSettings setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return this.version;
    }

    public String getDistanceCalcMethod() {
        return distanceCalcMethod.replaceAll("'", "");
    }

    public CalibrationSettings setDistanceCalcMethod(String distanceCalcMethod) {
        this.distanceCalcMethod = distanceCalcMethod;
        return this;
    }

    public CalibrationSettings merge(CalibrationSettings overlay) {
        if (overlay.distanceCalcMethod != null && overlay.distanceCalcMethod != this.distanceCalcMethod) {
            this.distanceCalcMethod = overlay.distanceCalcMethod;
        }

        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(distanceCalcMethod, id, version);
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
        CalibrationSettings other = (CalibrationSettings) obj;
        return distanceCalcMethod == other.distanceCalcMethod && Objects.equals(id, other.id) && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        return "CalibrationSettings [id=" + id + ", version=" + version + ", distanceCalcMethod=" + distanceCalcMethod + "]";
    }

}
