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
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Version;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "Waveform_Pick")
public class WaveformPick implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @JsonBackReference(value = "waveform-picks")
    @ManyToOne(optional = false, cascade = { CascadeType.PERSIST })
    private Waveform waveform;

    @Column(name = "pickName")
    private String pickName;

    @Column(name = "pickType")
    private String pickType;

    @Column(name = "pickTimeSecFromOrigin")
    private Float pickTimeSecFromOrigin;

    public Long getId() {
        return this.id;
    }

    public WaveformPick setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return this.version;
    }

    public Waveform getWaveform() {
        return waveform;
    }

    public WaveformPick setWaveform(Waveform waveform) {
        this.waveform = waveform;
        return this;
    }

    public String getPickName() {
        return this.pickName;
    }

    public WaveformPick setPickName(String pickName) {
        this.pickName = pickName;
        return this;
    }

    public String getPickType() {
        return pickType;
    }

    public WaveformPick setPickType(String pickType) {
        this.pickType = pickType;
        if (pickType != null) {
            this.pickType = this.pickType.toUpperCase(Locale.ENGLISH);
        }
        return this;
    }

    public Float getPickTimeSecFromOrigin() {
        return pickTimeSecFromOrigin;
    }

    public WaveformPick setPickTimeSecFromOrigin(Float pickTimeSecFromOrigin) {
        this.pickTimeSecFromOrigin = pickTimeSecFromOrigin;
        return this;
    }

    @Override
    public String toString() {
        return "WaveformPick [id=" + id + ", version=" + version + ", pickName=" + pickName + ", pickType=" + pickType + ", pickTimeSecFromOrigin=" + pickTimeSecFromOrigin + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((pickName == null) ? 0 : pickName.hashCode());
        result = prime * result + ((pickTimeSecFromOrigin == null) ? 0 : pickTimeSecFromOrigin.hashCode());
        result = prime * result + ((pickType == null) ? 0 : pickType.hashCode());
        result = prime * result + ((waveform == null) ? 0 : waveform.hashCode());
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
        WaveformPick other = (WaveformPick) obj;
        if (pickType == null) {
            if (other.pickType != null) {
                return false;
            }
        } else if (!pickType.equals(other.pickType)) {
            return false;
        }
        return true;
    }

    public WaveformPick mergeNonNullOrEmptyFields(WaveformPick pickOverlay) {
        if (pickOverlay.getPickName() != null) {
            this.setPickName(pickOverlay.getPickName());
        }
        if (pickOverlay.getPickTimeSecFromOrigin() != null) {
            this.setPickTimeSecFromOrigin(pickOverlay.getPickTimeSecFromOrigin());
        }

        if (pickOverlay.getPickType() != null) {
            this.setPickType(pickOverlay.getPickType());
        }
        return this;
    }
}
