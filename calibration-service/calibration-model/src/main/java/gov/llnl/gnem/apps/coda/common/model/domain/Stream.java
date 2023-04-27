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
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@Embeddable
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class)
public class Stream implements Serializable {

    public static final String TYPE_STACK = "STACK";

    private static final long serialVersionUID = 1L;

    @Embedded
    private Station station;

    @Column(name = "channelName")
    private String channelName;

    @Column(name = "bandName")
    private String bandName;

    @Column(name = "orientation")
    private String orientation;

    @Column(name = "instrument")
    private String instrument;

    public Station getStation() {
        return station;
    }

    public Stream setStation(Station station) {
        this.station = station;
        return this;
    }

    public String getChannelName() {
        return channelName;
    }

    public Stream setChannelName(String channelName) {
        this.channelName = channelName;
        return this;
    }

    public String getBandName() {
        return bandName;
    }

    public Stream setBandName(String bandName) {
        this.bandName = bandName;
        return this;
    }

    public String getOrientation() {
        return orientation;
    }

    public Stream setOrientation(String orientation) {
        this.orientation = orientation;
        return this;
    }

    public String getInstrument() {
        return instrument;
    }

    public Stream setInstrument(String instrument) {
        this.instrument = instrument;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(channelName, station);
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
        Stream other = (Stream) obj;
        if (!Objects.equals(channelName, other.channelName)) {
            return false;
        }
        if (!Objects.equals(station, other.station)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Stream [station=" + station + ", channelName=" + channelName + ", bandName=" + bandName + ", orientation=" + orientation + ", instrument=" + instrument + "]";
    }

}
