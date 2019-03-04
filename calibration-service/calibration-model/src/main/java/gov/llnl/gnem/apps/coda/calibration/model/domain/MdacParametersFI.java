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
@Table(name = "MdacFI")
public class MdacParametersFI {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @NumberFormat
    private double sigma;

    @NumberFormat
    private double delSigma;

    @NumberFormat
    private double psi;

    @NumberFormat
    private double delPsi;

    @NumberFormat
    private double zeta;

    @NumberFormat
    private double m0ref;

    @NumberFormat
    private double alphas;

    @NumberFormat
    private double betas;

    @NumberFormat
    private double rhos;

    @NumberFormat
    private double radPatP;

    @NumberFormat
    private double radPatS;

    @NumberFormat
    private double alphaR;

    @NumberFormat
    private double betaR;

    @NumberFormat
    private double rhor;

    public MdacParametersFI() {
        super();
    }

    public MdacParametersFI(MdacParametersFI mdacFI) {
        super();
        this.id = new Long(mdacFI.getId());
        this.version = new Long(mdacFI.getVersion());
        this.sigma = mdacFI.getSigma();
        this.delSigma = mdacFI.getDelSigma();
        this.psi = mdacFI.getPsi();
        this.delPsi = mdacFI.getDelPsi();
        this.zeta = mdacFI.getZeta();
        this.m0ref = mdacFI.getM0ref();
        this.alphas = mdacFI.getAlphas();
        this.betas = mdacFI.getBetas();
        this.rhos = mdacFI.getRhos();
        this.radPatP = mdacFI.getRadPatP();
        this.radPatS = mdacFI.getRadPatS();
        this.alphaR = mdacFI.getAlphaR();
        this.betaR = mdacFI.getBetaR();
        this.rhor = mdacFI.getRhor();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        // instanceof is false if the instance is null
        if (!(obj instanceof MdacParametersFI)) {
            return false;
        }
        return getId() != null && Objects.equals(getId(), ((MdacParametersFI) obj).getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }

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

    public double getSigma() {
        return this.sigma;
    }

    public void setSigma(double sigma) {
        this.sigma = sigma;
    }

    public double getDelSigma() {
        return this.delSigma;
    }

    public void setDelSigma(double delSigma) {
        this.delSigma = delSigma;
    }

    public double getPsi() {
        return this.psi;
    }

    public void setPsi(double psi) {
        this.psi = psi;
    }

    public double getDelPsi() {
        return this.delPsi;
    }

    public void setDelPsi(double delPsi) {
        this.delPsi = delPsi;
    }

    public double getZeta() {
        return this.zeta;
    }

    public void setZeta(double zeta) {
        this.zeta = zeta;
    }

    public double getM0ref() {
        return this.m0ref;
    }

    public void setM0ref(double m0ref) {
        this.m0ref = m0ref;
    }

    public double getAlphas() {
        return this.alphas;
    }

    public void setAlphas(double alphas) {
        this.alphas = alphas;
    }

    public double getBetas() {
        return this.betas;
    }

    public void setBetas(double betas) {
        this.betas = betas;
    }

    public double getRhos() {
        return this.rhos;
    }

    public void setRhos(double rhos) {
        this.rhos = rhos;
    }

    public double getRadPatP() {
        return this.radPatP;
    }

    public void setRadPatP(double radPatP) {
        this.radPatP = radPatP;
    }

    public double getRadPatS() {
        return this.radPatS;
    }

    public void setRadPatS(double radPatS) {
        this.radPatS = radPatS;
    }

    public double getAlphaR() {
        return this.alphaR;
    }

    public void setAlphaR(double alphaR) {
        this.alphaR = alphaR;
    }

    public double getBetaR() {
        return betaR;
    }

    public void setBetaR(double betaR) {
        this.betaR = betaR;
    }

    public double getRhor() {
        return this.rhor;
    }

    public void setRhor(double rhor) {
        this.rhor = rhor;
    }

    @Override
    public String toString() {
        return "MdacParametersFI [id="
                + id
                + ", version="
                + version
                + ", sigma="
                + sigma
                + ", delSigma="
                + delSigma
                + ", psi="
                + psi
                + ", delPsi="
                + delPsi
                + ", zeta="
                + zeta
                + ", m0ref="
                + m0ref
                + ", alphas="
                + alphas
                + ", betas="
                + betas
                + ", rhos="
                + rhos
                + ", radPatP="
                + radPatP
                + ", radPatS="
                + radPatS
                + ", alphaR="
                + alphaR
                + ", betaR="
                + betaR
                + ", rhor="
                + rhor
                + "]";
    }

    public MdacParametersFI mergeNonNullOrEmptyFields(MdacParametersFI overlay) {

        this.sigma = overlay.sigma;
        this.delSigma = overlay.delSigma;
        this.psi = overlay.psi;
        this.delPsi = overlay.delPsi;
        this.zeta = overlay.zeta;
        this.m0ref = overlay.m0ref;
        this.alphas = overlay.alphas;
        this.betas = overlay.betas;
        this.rhos = overlay.rhos;
        this.radPatP = overlay.radPatP;
        this.radPatS = overlay.radPatS;
        this.alphaR = overlay.alphaR;
        this.betaR = overlay.betaR;
        this.rhor = overlay.rhor;

        return this;
    }
}
