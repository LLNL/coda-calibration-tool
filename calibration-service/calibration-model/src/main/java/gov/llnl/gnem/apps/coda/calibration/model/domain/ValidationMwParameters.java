/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

@Entity
@Table(name = "Validation_Mws", indexes = { @Index(columnList = "eventId", name = "val_mw_event_id_index") })
public class ValidationMwParameters implements Serializable {

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
    private Double apparentStressInMpa;

    public Long getId() {
        return id;
    }

    public ValidationMwParameters setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public String getEventId() {
        return eventId;
    }

    public ValidationMwParameters setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public double getMw() {
        return mw;
    }

    public ValidationMwParameters setMw(double mw) {
        this.mw = mw;
        return this;
    }

    public Double getApparentStressInMpa() {
        return apparentStressInMpa;
    }

    public ValidationMwParameters setApparentStressInMpa(Double apparentStressInMpa) {
        this.apparentStressInMpa = apparentStressInMpa;
        return this;
    }

    @Override
    public String toString() {
        return "ValidationMwParams [id=" + id + ", version=" + version + ", eventId=" + eventId + ", Mw=" + mw + ", ApparentStressInMpa=" + apparentStressInMpa + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        long temp;
        temp = Double.doubleToLongBits(mw);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((apparentStressInMpa == null) ? 0 : apparentStressInMpa.hashCode());
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
        ValidationMwParameters other = (ValidationMwParameters) obj;
        if (eventId == null) {
            if (other.eventId != null) {
                return false;
            }
        } else if (!eventId.equals(other.eventId)) {
            return false;
        }
        if (Double.doubleToLongBits(mw) != Double.doubleToLongBits(other.getMw())) {
            return false;
        }
        if (apparentStressInMpa == null) {
            if (other.apparentStressInMpa != null) {
                return false;
            }
        } else if (!apparentStressInMpa.equals(other.apparentStressInMpa)) {
            return false;
        }
        return true;
    }

    public ValidationMwParameters merge(ValidationMwParameters overlay) {
        this.mw = overlay.getMw();
        this.apparentStressInMpa = overlay.getApparentStressInMpa();
        return this;
    }

}