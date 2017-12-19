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

import javax.persistence.Column;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

@Entity
@Table(name = "Site_FB_Params")
public class SiteFrequencyBandParameters {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

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

    public Long getVersion() {
        return this.version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public double getLowFrequency() {
        return this.lowFrequency;
    }

    public void setLowFrequency(double lowFrequency) {
        this.lowFrequency = lowFrequency;
    }

    public double getHighFrequency() {
        return this.highFrequency;
    }

    public void setHighFrequency(double highFrequency) {
        this.highFrequency = highFrequency;
    }

    public double getSiteTerm() {
        return this.siteTerm;
    }

    public void setSiteTerm(double siteTerm) {
        this.siteTerm = siteTerm;
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
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(highFrequency);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        temp = Double.doubleToLongBits(lowFrequency);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(siteTerm);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((station == null) ? 0 : station.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
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
        SiteFrequencyBandParameters other = (SiteFrequencyBandParameters) obj;
        if (Double.doubleToLongBits(highFrequency) != Double.doubleToLongBits(other.highFrequency))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (Double.doubleToLongBits(lowFrequency) != Double.doubleToLongBits(other.lowFrequency))
            return false;
        if (Double.doubleToLongBits(siteTerm) != Double.doubleToLongBits(other.siteTerm))
            return false;
        if (station == null) {
            if (other.station != null)
                return false;
        } else if (!station.equals(other.station))
            return false;
        if (version == null) {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "SiteFrequencyBandParameters [id=" + id + ", version=" + version + ", lowFrequency=" + lowFrequency + ", highFrequency=" + highFrequency + ", station=" + station + ", siteTerm="
                + siteTerm + "]";
    }

}
