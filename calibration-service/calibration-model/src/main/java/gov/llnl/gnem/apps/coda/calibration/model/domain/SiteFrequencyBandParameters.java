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

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

import gov.llnl.gnem.apps.coda.common.model.domain.Station;

@Entity
@Table(name = "Site_FB_Params")
public class SiteFrequencyBandParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @NumberFormat
    private double lowFrequency;

    @NumberFormat
    private double highFrequency;

    @Embedded
    private Station station;

    @NumberFormat
    private double siteTerm;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return this.version;
    }

    public double getLowFrequency() {
        return this.lowFrequency;
    }

    public SiteFrequencyBandParameters setLowFrequency(double lowFrequency) {
        this.lowFrequency = lowFrequency;
        return this;
    }

    public double getHighFrequency() {
        return this.highFrequency;
    }

    public SiteFrequencyBandParameters setHighFrequency(double highFrequency) {
        this.highFrequency = highFrequency;
        return this;
    }

    public double getSiteTerm() {
        return this.siteTerm;
    }

    public SiteFrequencyBandParameters setSiteTerm(double siteTerm) {
        this.siteTerm = siteTerm;
        return this;
    }

    public Station getStation() {
        return station;
    }

    public SiteFrequencyBandParameters setStation(Station station) {
        this.station = station;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(highFrequency, lowFrequency, station);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SiteFrequencyBandParameters)) {
            return false;
        }
        SiteFrequencyBandParameters other = (SiteFrequencyBandParameters) obj;
        if (Double.doubleToLongBits(highFrequency) != Double.doubleToLongBits(other.highFrequency)) {
            return false;
        }
        if (Double.doubleToLongBits(lowFrequency) != Double.doubleToLongBits(other.lowFrequency)) {
            return false;
        }
        if (!Objects.equals(station, other.station)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "SiteFrequencyBandParameters [id="
                + id
                + ", version="
                + version
                + ", lowFrequency="
                + lowFrequency
                + ", highFrequency="
                + highFrequency
                + ", station="
                + station
                + ", siteTerm="
                + siteTerm
                + "]";
    }

    public SiteFrequencyBandParameters mergeNonNullOrEmptyFields(SiteFrequencyBandParameters overlay) {
        if (overlay != null) {
            if (overlay.getId() != null) {
                id = overlay.getId();
            }
            if (overlay.getVersion() != null) {
                version = overlay.getVersion();
            }
            if (overlay.getStation() != null) {
                if (station == null) {
                    station = new Station();
                }
                if ((overlay.getStation().getNetworkName() != null) && (!overlay.getStation().getNetworkName().equals("UNK") || station.getNetworkName() == null)) {
                    station.setNetworkName(overlay.getStation().getNetworkName());
                }
                if (overlay.getStation().getStationName() != null) {
                    station.setStationName(overlay.getStation().getStationName());
                }
                if (overlay.getStation().getLatitude() != 0.0) {
                    station.setLatitude(overlay.getStation().getLatitude());
                }
                if (overlay.getStation().getLongitude() != 0.0) {
                    station.setLongitude(overlay.getStation().getLongitude());
                }
                if (overlay.getStation().getElevation() != 0.0) {
                    station.setElevation(overlay.getStation().getElevation());
                }
            }
            if (overlay.getSiteTerm() != 0.0) {
                siteTerm = overlay.getSiteTerm();
            }
            if (overlay.getLowFrequency() != 0.0) {
                lowFrequency = overlay.getLowFrequency();
            }
            if (overlay.getHighFrequency() != 0.0) {
                highFrequency = overlay.getHighFrequency();
            }
        }
        return this;
    }

}
