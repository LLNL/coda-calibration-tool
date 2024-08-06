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
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

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
    private Double pickTimeSecFromOrigin;

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
            this.pickType = this.pickType.toUpperCase(Locale.ENGLISH).trim();
        }
        return this;
    }

    public Double getPickTimeSecFromOrigin() {
        return pickTimeSecFromOrigin;
    }

    public WaveformPick setPickTimeSecFromOrigin(Double pickTimeSecFromOrigin) {
        this.pickTimeSecFromOrigin = pickTimeSecFromOrigin;
        return this;
    }

    @Override
    public String toString() {
        return "WaveformPick [id=" + id + ", version=" + version + ", pickName=" + pickName + ", pickType=" + pickType + ", pickTimeSecFromOrigin=" + pickTimeSecFromOrigin + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(pickName, pickType, waveform);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof WaveformPick)) {
            return false;
        }
        WaveformPick other = (WaveformPick) obj;
        return Objects.equals(pickName, other.pickName) && Objects.equals(pickType, other.pickType);
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
