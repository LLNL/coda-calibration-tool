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

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@Embeddable
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class)
public class Event implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    @Column(name = "event_id")
    private String eventId;

    @NotNull
    @Column(name = "originTime")
    @Temporal(TemporalType.TIMESTAMP)
    @DateTimeFormat(style = "M-")
    private Date originTime;

    @NotNull
    @Column(name = "eventLatitude")
    @NumberFormat
    private double latitude;

    @NotNull
    @Column(name = "eventLongitude")
    @NumberFormat
    private double longitude;

    public String getEventId() {
        return this.eventId;
    }

    public Event setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public Date getOriginTime() {
        return this.originTime;
    }

    public Event setOriginTime(Date originTime) {
        this.originTime = originTime;
        if (StringUtils.isEmpty(this.eventId)) {
            this.eventId = Long.toString(originTime.getTime());
        }
        return this;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public Event setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public Event setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
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
        Event other = (Event) obj;
        if (eventId == null) {
            if (other.eventId != null) {
                return false;
            }
        } else if (!eventId.equals(other.eventId)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Event [eventId=" + eventId + ", originTime=" + originTime + ", latitude=" + latitude + ", longitude=" + longitude + "]";
    }

}
