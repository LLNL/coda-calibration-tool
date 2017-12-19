/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
    @Column(name = "version")
    private Long version;

    @Column(name = "eventId")
    private String eventId;

    @NumberFormat
    private double mw;

    @NumberFormat
    @Column(nullable = true)
    private Double stressDropInMpa;

    public Long getId() {
        return id;
    }

    public MeasuredMwParameters setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getVersion() {
        return version;
    }

    public MeasuredMwParameters setVersion(Long version) {
        this.version = version;
        return this;
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

    public Double getStressDropInMpa() {
        return stressDropInMpa;
    }

    public MeasuredMwParameters setStressDropInMpa(Double stressDropInMpa) {
        this.stressDropInMpa = stressDropInMpa;
        return this;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MeasuredMwParameters [id=" + id + ", version=" + version + ", eventId=" + eventId + ", mw=" + mw + ", stressDropInMpa=" + stressDropInMpa + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        long temp;
        temp = Double.doubleToLongBits(mw);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((stressDropInMpa == null) ? 0 : stressDropInMpa.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        MeasuredMwParameters other = (MeasuredMwParameters) obj;
        if (eventId == null) {
            if (other.eventId != null)
                return false;
        } else if (!eventId.equals(other.eventId))
            return false;
        if (Double.doubleToLongBits(mw) != Double.doubleToLongBits(other.mw))
            return false;
        if (stressDropInMpa == null) {
            if (other.stressDropInMpa != null)
                return false;
        } else if (!stressDropInMpa.equals(other.stressDropInMpa))
            return false;
        return true;
    }

    public MeasuredMwParameters merge(MeasuredMwParameters overlay) {
        this.mw = overlay.getMw();
        this.stressDropInMpa = overlay.getStressDropInMpa();
        return this;
    }

}