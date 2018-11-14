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

import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.llnl.gnem.apps.coda.common.model.domain.Event;

public class MeasuredMwDetails {

    private String eventId;

    private Double mw;

    private Double stressDropInMpa;

    private Double latitude;

    private Double longitude;

    private String datetime;

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_INSTANT;

    public MeasuredMwDetails(MeasuredMwParameters meas, Event event) {
        if (meas != null && event != null) {
            setEventId(meas.getEventId());
            setMw(meas.getMw());
            setStressDropInMpa(meas.getStressDropInMpa());
            setLatitude(event.getLatitude());
            setLongitude(event.getLongitude());
            if (event.getOriginTime() != null) {
                setDatetime(dateFormatter.format(event.getOriginTime().toInstant()));
            }
        }
    }

    public String getEventId() {
        return eventId;
    }

    public MeasuredMwDetails setEventId(String eventId) {
        this.eventId = eventId;
        return this;
    }

    public Double getMw() {
        return mw;
    }

    public MeasuredMwDetails setMw(double mw) {
        this.mw = mw;
        return this;
    }

    public Double getStressDropInMpa() {
        return stressDropInMpa;
    }

    public MeasuredMwDetails setStressDropInMpa(Double stressDropInMpa) {
        this.stressDropInMpa = stressDropInMpa;
        return this;
    }

    public Double getLatitude() {
        return latitude;
    }

    public MeasuredMwDetails setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public Double getLongitude() {
        return longitude;
    }

    public MeasuredMwDetails setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public String getDatetime() {
        return datetime;
    }

    public MeasuredMwDetails setDatetime(String datetime) {
        this.datetime = datetime;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((datetime == null) ? 0 : datetime.hashCode());
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        long temp;
        temp = Double.doubleToLongBits(latitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(longitude);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(mw);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((stressDropInMpa == null) ? 0 : stressDropInMpa.hashCode());
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
        MeasuredMwDetails other = (MeasuredMwDetails) obj;
        if (datetime == null) {
            if (other.datetime != null) {
                return false;
            }
        } else if (!datetime.equals(other.datetime)) {
            return false;
        }
        if (eventId == null) {
            if (other.eventId != null) {
                return false;
            }
        } else if (!eventId.equals(other.eventId)) {
            return false;
        }
        if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude)) {
            return false;
        }
        if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude)) {
            return false;
        }
        if (Double.doubleToLongBits(mw) != Double.doubleToLongBits(other.mw)) {
            return false;
        }
        if (stressDropInMpa == null) {
            if (other.stressDropInMpa != null) {
                return false;
            }
        } else if (!stressDropInMpa.equals(other.stressDropInMpa)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"")
               .append(eventId)
               .append("\", \"")
               .append(mw)
               .append("\", \"")
               .append(stressDropInMpa)
               .append("\", \"")
               .append(latitude)
               .append("\", \"")
               .append(longitude)
               .append("\", \"")
               .append(datetime)
               .append("\"");
        return builder.toString();
    }

    @JsonIgnore
    public boolean isValid() {
        return eventId != null && mw != null && stressDropInMpa != null && latitude != null && longitude != null && datetime != null;
    }
}