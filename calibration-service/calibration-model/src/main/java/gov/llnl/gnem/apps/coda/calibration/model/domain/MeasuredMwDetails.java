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

    private Double refMw;

    private Double apparentStressInMpa;

    private Double refApparentStressInMpa;

    private Integer dataCount;

    private Double latitude;

    private Double longitude;

    private String datetime;

    public MeasuredMwDetails(MeasuredMwParameters meas, ReferenceMwParameters ref, Event event) {
        if (meas != null) {
            this.mw = meas.getMw();
            this.apparentStressInMpa = meas.getApparentStressInMpa();
            this.dataCount = meas.getDataCount();
        }
        if (event != null) {
            this.eventId = event.getEventId();
            this.latitude = event.getLatitude();
            this.longitude = event.getLongitude();
            if (event.getOriginTime() != null) {
                this.datetime = DateTimeFormatter.ISO_INSTANT.format(event.getOriginTime().toInstant());
            }
        }
        if (ref != null) {
            if (eventId == null) {
                this.eventId = ref.getEventId();
            }
            this.refApparentStressInMpa = ref.getRefApparentStressInMpa();
            this.refMw = ref.getRefMw();
        }
    }

    public MeasuredMwDetails() {
        //nop
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

    public Double getRefMw() {
        return refMw;
    }

    public MeasuredMwDetails setRefMw(Double refMw) {
        this.refMw = refMw;
        return this;
    }

    public Double getApparentStressInMpa() {
        return apparentStressInMpa;
    }

    public MeasuredMwDetails setApparentStressInMpa(Double apparentStressInMpa) {
        this.apparentStressInMpa = apparentStressInMpa;
        return this;
    }

    public Double getRefApparentStressInMpa() {
        return refApparentStressInMpa;
    }

    public MeasuredMwDetails setRefApparentStressInMpa(Double refApparentStressInMpa) {
        this.refApparentStressInMpa = refApparentStressInMpa;
        return this;
    }

    public Integer getDataCount() {
        return dataCount;
    }

    public MeasuredMwDetails setDataCount(Integer dataCount) {
        this.dataCount = dataCount;
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
        result = prime * result + ((apparentStressInMpa == null) ? 0 : apparentStressInMpa.hashCode());
        result = prime * result + ((datetime == null) ? 0 : datetime.hashCode());
        result = prime * result + ((eventId == null) ? 0 : eventId.hashCode());
        result = prime * result + ((latitude == null) ? 0 : latitude.hashCode());
        result = prime * result + ((longitude == null) ? 0 : longitude.hashCode());
        result = prime * result + ((mw == null) ? 0 : mw.hashCode());
        result = prime * result + ((refApparentStressInMpa == null) ? 0 : refApparentStressInMpa.hashCode());
        result = prime * result + ((refMw == null) ? 0 : refMw.hashCode());
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
        if (apparentStressInMpa == null) {
            if (other.apparentStressInMpa != null) {
                return false;
            }
        } else if (!apparentStressInMpa.equals(other.apparentStressInMpa)) {
            return false;
        }
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
        if (latitude == null) {
            if (other.latitude != null) {
                return false;
            }
        } else if (!latitude.equals(other.latitude)) {
            return false;
        }
        if (longitude == null) {
            if (other.longitude != null) {
                return false;
            }
        } else if (!longitude.equals(other.longitude)) {
            return false;
        }
        if (mw == null) {
            if (other.mw != null) {
                return false;
            }
        } else if (!mw.equals(other.mw)) {
            return false;
        }
        if (refApparentStressInMpa == null) {
            if (other.refApparentStressInMpa != null) {
                return false;
            }
        } else if (!refApparentStressInMpa.equals(other.refApparentStressInMpa)) {
            return false;
        }
        if (refMw == null) {
            if (other.refMw != null) {
                return false;
            }
        } else if (!refMw.equals(other.refMw)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "MeasuredMwDetails [eventId="
                + eventId
                + ", mw="
                + mw
                + ", refMw="
                + refMw
                + ", apparentStressInMpa="
                + apparentStressInMpa
                + ", refApparentStressInMpa="
                + refApparentStressInMpa
                + ", latitude="
                + latitude
                + ", longitude="
                + longitude
                + ", datetime="
                + datetime
                + "]";
    }

    @JsonIgnore
    public boolean isValid() {
        return eventId != null;
    }
}
