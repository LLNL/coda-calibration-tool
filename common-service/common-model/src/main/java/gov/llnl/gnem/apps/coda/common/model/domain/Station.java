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

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.NumberFormat;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

@Embeddable
@JsonIdentityInfo(generator = ObjectIdGenerators.UUIDGenerator.class)
public class Station implements Serializable {

    private static final Logger log = LoggerFactory.getLogger(Station.class);
    private static final long serialVersionUID = 1L;
    private static final String UNK = "UNK";

    @Column(name = "network_name")
    private String networkName = UNK;

    @NotNull
    @Column(name = "stationLatitude")
    @NumberFormat
    private double latitude;

    @NotNull
    @Column(name = "stationLongitude")
    @NumberFormat
    private double longitude;

    @NotNull
    @Column(name = "station_name")
    private String stationName;

    public String getNetworkName() {
        return this.networkName;
    }

    public Station setNetworkName(String networkName) {
        this.networkName = networkName;
        return this;
    }

    public double getLatitude() {
        return this.latitude;
    }

    public Station setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return this.longitude;
    }

    public Station setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public String getStationName() {
        return this.stationName;
    }

    public Station setStationName(String stationName) {
        this.stationName = stationName;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((stationName == null) ? 0 : stationName.hashCode());
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
        Station other = (Station) obj;
        if (networkName == null) {
            if (other.networkName != null) {
                log.trace("Mismatched network names {} and {}", networkName, other.networkName);
                return false;
            }
        } else if (!networkName.equals(UNK) && !other.networkName.equals(UNK) && !networkName.equalsIgnoreCase(other.networkName)) {
            log.trace("Mismatched network names {} and {}", networkName, other.networkName);
            return false;
        }
        if (stationName == null) {
            if (other.stationName != null) {
                return false;
            }
        } else if (!stationName.equalsIgnoreCase(other.stationName)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "Station [networkName=" + networkName + ", latitude=" + latitude + ", longitude=" + longitude + ", stationName=" + stationName + "]";
    }

}
