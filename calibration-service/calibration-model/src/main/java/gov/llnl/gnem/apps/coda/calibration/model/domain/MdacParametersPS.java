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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

import gov.llnl.gnem.apps.coda.common.model.util.Durable;

@Durable
@Entity
@Table(name = "MdacPS")
public class MdacParametersPS {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @Column(unique = true)
    private String phase;

    @NumberFormat
    private double q0;

    @NumberFormat
    private double delQ0;

    @NumberFormat
    private double gamma0;

    @NumberFormat
    private double delGamma0;

    @NumberFormat
    private double u0;

    @NumberFormat
    private double eta;

    @NumberFormat
    private double delEta;

    @NumberFormat
    private double distCrit;

    @NumberFormat
    private double snr;

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

    public String getPhase() {
        return this.phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public double getQ0() {
        return this.q0;
    }

    public void setQ0(double q0) {
        this.q0 = q0;
    }

    public double getDelQ0() {
        return this.delQ0;
    }

    public void setDelQ0(double delQ0) {
        this.delQ0 = delQ0;
    }

    public double getGamma0() {
        return this.gamma0;
    }

    public void setGamma0(double gamma0) {
        this.gamma0 = gamma0;
    }

    public double getDelGamma0() {
        return this.delGamma0;
    }

    public void setDelGamma0(double delGamma0) {
        this.delGamma0 = delGamma0;
    }

    public double getU0() {
        return this.u0;
    }

    public void setU0(double u0) {
        this.u0 = u0;
    }

    public double getEta() {
        return this.eta;
    }

    public void setEta(double eta) {
        this.eta = eta;
    }

    public double getDelEta() {
        return this.delEta;
    }

    public void setDelEta(double delEta) {
        this.delEta = delEta;
    }

    public double getDistCrit() {
        return this.distCrit;
    }

    public void setDistCrit(double distCrit) {
        this.distCrit = distCrit;
    }

    public double getSnr() {
        return this.snr;
    }

    public void setSnr(double snr) {
        this.snr = snr;
    }

    @Override
    public String toString() {
        return "MdacParametersPS [id="
                + id
                + ", version="
                + version
                + ", phase="
                + phase
                + ", q0="
                + q0
                + ", delQ0="
                + delQ0
                + ", gamma0="
                + gamma0
                + ", delGamma0="
                + delGamma0
                + ", u0="
                + u0
                + ", eta="
                + eta
                + ", delEta="
                + delEta
                + ", distCrit="
                + distCrit
                + ", snr="
                + snr
                + "]";
    }

    public MdacParametersPS mergeNonNullOrEmptyFields(MdacParametersPS overlay) {
        if (overlay.getPhase() != null) {
            this.phase = overlay.phase;
        }
        this.q0 = overlay.q0;
        this.delQ0 = overlay.delQ0;
        this.gamma0 = overlay.gamma0;
        this.delGamma0 = overlay.delGamma0;
        this.u0 = overlay.u0;
        this.eta = overlay.eta;
        this.delEta = overlay.delEta;
        this.distCrit = overlay.distCrit;
        this.snr = overlay.snr;

        return this;
    }

}
