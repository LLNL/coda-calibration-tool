/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.io.Serializable;
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
@Table(name = "Shape_Fit_Constraints")
public class ShapeFitterConstraints implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @NumberFormat
    private double maxVP1;
    @NumberFormat
    private double minVP1;
    @NumberFormat
    private double v0reg;
    @NumberFormat
    private double maxVP2;
    @NumberFormat
    private double minVP2;
    @NumberFormat
    private double maxVP3;
    @NumberFormat
    private double minVP3;
    @NumberFormat
    private double maxBP1;
    @NumberFormat
    private double minBP1;
    @NumberFormat
    private double b0reg;
    @NumberFormat
    private double maxBP2;
    @NumberFormat
    private double minBP2;
    @NumberFormat
    private double maxBP3;
    @NumberFormat
    private double minBP3;
    @NumberFormat
    private double maxGP1;
    @NumberFormat
    private double minGP1;
    @NumberFormat
    private double g0reg;
    @NumberFormat
    private double maxGP2;
    @NumberFormat
    private double minGP2;
    @NumberFormat
    private double g1reg;
    @NumberFormat
    private double maxGP3;
    @NumberFormat
    private double minGP3;
    @NumberFormat
    private double yvvMin;
    @NumberFormat
    private double yvvMax;
    @NumberFormat
    private double vDistMax;
    @NumberFormat
    private double vDistMin;
    @NumberFormat
    private double ybbMin;
    @NumberFormat
    private double ybbMax;
    @NumberFormat
    private double bDistMax;
    @NumberFormat
    private double bDistMin;
    @NumberFormat
    private double yggMin;
    @NumberFormat
    private double yggMax;
    @NumberFormat
    private double gDistMin;
    @NumberFormat
    private double gDistMax;
    @NumberFormat
    private double minIntercept;
    @NumberFormat
    private double maxIntercept;
    @NumberFormat
    private double minBeta;
    @NumberFormat
    private double maxBeta;
    @NumberFormat
    private double minGamma;
    @NumberFormat
    private double maxGamma;
    @NumberFormat
    private int iterations;
    @NumberFormat
    private int fittingPointCount;
    @NumberFormat
    private double lengthWeight;

    public Long getId() {
        return id;
    }

    public ShapeFitterConstraints setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public ShapeFitterConstraints setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public double getMaxVP1() {
        return maxVP1;
    }

    public ShapeFitterConstraints setMaxVP1(double maxVP1) {
        this.maxVP1 = maxVP1;
        return this;
    }

    public double getMinVP1() {
        return minVP1;
    }

    public ShapeFitterConstraints setMinVP1(double minVP1) {
        this.minVP1 = minVP1;
        return this;
    }

    public double getV0reg() {
        return v0reg;
    }

    public ShapeFitterConstraints setV0reg(double v0reg) {
        this.v0reg = v0reg;
        return this;
    }

    public double getMaxVP2() {
        return maxVP2;
    }

    public ShapeFitterConstraints setMaxVP2(double maxVP2) {
        this.maxVP2 = maxVP2;
        return this;
    }

    public double getMinVP2() {
        return minVP2;
    }

    public ShapeFitterConstraints setMinVP2(double minVP2) {
        this.minVP2 = minVP2;
        return this;
    }

    public double getMaxVP3() {
        return maxVP3;
    }

    public ShapeFitterConstraints setMaxVP3(double maxVP3) {
        this.maxVP3 = maxVP3;
        return this;
    }

    public double getMinVP3() {
        return minVP3;
    }

    public ShapeFitterConstraints setMinVP3(double minVP3) {
        this.minVP3 = minVP3;
        return this;
    }

    public double getMaxBP1() {
        return maxBP1;
    }

    public ShapeFitterConstraints setMaxBP1(double maxBP1) {
        this.maxBP1 = maxBP1;
        return this;
    }

    public double getMinBP1() {
        return minBP1;
    }

    public ShapeFitterConstraints setMinBP1(double minBP1) {
        this.minBP1 = minBP1;
        return this;
    }

    public double getB0reg() {
        return b0reg;
    }

    public ShapeFitterConstraints setB0reg(double b0reg) {
        this.b0reg = b0reg;
        return this;
    }

    public double getMaxBP2() {
        return maxBP2;
    }

    public ShapeFitterConstraints setMaxBP2(double maxBP2) {
        this.maxBP2 = maxBP2;
        return this;
    }

    public double getMinBP2() {
        return minBP2;
    }

    public ShapeFitterConstraints setMinBP2(double minBP2) {
        this.minBP2 = minBP2;
        return this;
    }

    public double getMaxBP3() {
        return maxBP3;
    }

    public ShapeFitterConstraints setMaxBP3(double maxBP3) {
        this.maxBP3 = maxBP3;
        return this;
    }

    public double getMinBP3() {
        return minBP3;
    }

    public ShapeFitterConstraints setMinBP3(double minBP3) {
        this.minBP3 = minBP3;
        return this;
    }

    public double getMaxGP1() {
        return maxGP1;
    }

    public ShapeFitterConstraints setMaxGP1(double maxGP1) {
        this.maxGP1 = maxGP1;
        return this;
    }

    public double getMinGP1() {
        return minGP1;
    }

    public ShapeFitterConstraints setMinGP1(double minGP1) {
        this.minGP1 = minGP1;
        return this;
    }

    public double getG0reg() {
        return g0reg;
    }

    public ShapeFitterConstraints setG0reg(double g0reg) {
        this.g0reg = g0reg;
        return this;
    }

    public double getMaxGP2() {
        return maxGP2;
    }

    public ShapeFitterConstraints setMaxGP2(double maxGP2) {
        this.maxGP2 = maxGP2;
        return this;
    }

    public double getMinGP2() {
        return minGP2;
    }

    public ShapeFitterConstraints setMinGP2(double minGP2) {
        this.minGP2 = minGP2;
        return this;
    }

    public double getG1reg() {
        return g1reg;
    }

    public ShapeFitterConstraints setG1reg(double g1reg) {
        this.g1reg = g1reg;
        return this;
    }

    public double getMaxGP3() {
        return maxGP3;
    }

    public ShapeFitterConstraints setMaxGP3(double maxGP3) {
        this.maxGP3 = maxGP3;
        return this;
    }

    public double getMinGP3() {
        return minGP3;
    }

    public ShapeFitterConstraints setMinGP3(double minGP3) {
        this.minGP3 = minGP3;
        return this;
    }

    public double getYvvMin() {
        return yvvMin;
    }

    public ShapeFitterConstraints setYvvMin(double yvvMin) {
        this.yvvMin = yvvMin;
        return this;
    }

    public double getYvvMax() {
        return yvvMax;
    }

    public ShapeFitterConstraints setYvvMax(double yvvMax) {
        this.yvvMax = yvvMax;
        return this;
    }

    public double getvDistMax() {
        return vDistMax;
    }

    public ShapeFitterConstraints setvDistMax(double vDistMax) {
        this.vDistMax = vDistMax;
        return this;
    }

    public double getvDistMin() {
        return vDistMin;
    }

    public ShapeFitterConstraints setvDistMin(double vDistMin) {
        this.vDistMin = vDistMin;
        return this;
    }

    public double getYbbMin() {
        return ybbMin;
    }

    public ShapeFitterConstraints setYbbMin(double ybbMin) {
        this.ybbMin = ybbMin;
        return this;
    }

    public double getYbbMax() {
        return ybbMax;
    }

    public ShapeFitterConstraints setYbbMax(double ybbMax) {
        this.ybbMax = ybbMax;
        return this;
    }

    public double getbDistMax() {
        return bDistMax;
    }

    public ShapeFitterConstraints setbDistMax(double bDistMax) {
        this.bDistMax = bDistMax;
        return this;
    }

    public double getbDistMin() {
        return bDistMin;
    }

    public ShapeFitterConstraints setbDistMin(double bDistMin) {
        this.bDistMin = bDistMin;
        return this;
    }

    public double getYggMin() {
        return yggMin;
    }

    public ShapeFitterConstraints setYggMin(double yggMin) {
        this.yggMin = yggMin;
        return this;
    }

    public double getYggMax() {
        return yggMax;
    }

    public ShapeFitterConstraints setYggMax(double yggMax) {
        this.yggMax = yggMax;
        return this;
    }

    public double getgDistMin() {
        return gDistMin;
    }

    public ShapeFitterConstraints setgDistMin(double gDistMin) {
        this.gDistMin = gDistMin;
        return this;
    }

    public double getgDistMax() {
        return gDistMax;
    }

    public ShapeFitterConstraints setgDistMax(double gDistMax) {
        this.gDistMax = gDistMax;
        return this;
    }

    public double getMinIntercept() {
        return minIntercept;
    }

    public ShapeFitterConstraints setMinIntercept(double minIntercept) {
        this.minIntercept = minIntercept;
        return this;
    }

    public double getMaxIntercept() {
        return maxIntercept;
    }

    public ShapeFitterConstraints setMaxIntercept(double maxIntercept) {
        this.maxIntercept = maxIntercept;
        return this;
    }

    public double getMinBeta() {
        return minBeta;
    }

    public ShapeFitterConstraints setMinBeta(double minBeta) {
        this.minBeta = minBeta;
        return this;
    }

    public double getMaxBeta() {
        return maxBeta;
    }

    public ShapeFitterConstraints setMaxBeta(double maxBeta) {
        this.maxBeta = maxBeta;
        return this;
    }

    public double getMinGamma() {
        return minGamma;
    }

    public ShapeFitterConstraints setMinGamma(double minGamma) {
        this.minGamma = minGamma;
        return this;
    }

    public double getMaxGamma() {
        return maxGamma;
    }

    public ShapeFitterConstraints setMaxGamma(double maxGamma) {
        this.maxGamma = maxGamma;
        return this;
    }

    public int getIterations() {
        return iterations;
    }

    public ShapeFitterConstraints setIterations(int iterations) {
        this.iterations = iterations;
        return this;
    }

    public int getFittingPointCount() {
        return fittingPointCount;
    }

    public ShapeFitterConstraints setFittingPointCount(int fittingPointCount) {
        this.fittingPointCount = fittingPointCount;
        return this;
    }

    public double getLengthWeight() {
        return lengthWeight;
    }

    public ShapeFitterConstraints setLengthWeight(double lengthWeight) {
        this.lengthWeight = lengthWeight;
        return this;
    }

    public ShapeFitterConstraints merge(ShapeFitterConstraints other) {
        if (other.getId() != null) {
            id = other.getId();
        }
        if (other.getVersion() != null) {
            version = other.getVersion();
        }
        maxVP1 = other.getMaxVP1();
        minVP1 = other.getMinVP1();
        v0reg = other.getV0reg();
        maxVP2 = other.getMaxVP2();
        minVP2 = other.getMinVP2();
        maxVP3 = other.getMaxVP3();
        minVP3 = other.getMinVP3();
        maxBP1 = other.getMaxBP1();
        minBP1 = other.getMinBP1();
        b0reg = other.getB0reg();
        maxBP2 = other.getMaxBP2();
        minBP2 = other.getMinBP2();
        maxBP3 = other.getMaxBP3();
        minBP3 = other.getMinBP3();
        maxGP1 = other.getMaxGP1();
        minGP1 = other.getMinGP1();
        g0reg = other.getG0reg();
        maxGP2 = other.getMaxGP2();
        minGP2 = other.getMinGP2();
        g1reg = other.getG1reg();
        maxGP3 = other.getMaxGP3();
        minGP3 = other.getMinGP3();
        yvvMin = other.getYvvMin();
        yvvMax = other.getYvvMax();
        ybbMin = other.getYbbMin();
        ybbMax = other.getYbbMax();
        yggMin = other.getYggMin();
        yggMax = other.getYggMax();
        minIntercept = other.getMinIntercept();
        maxIntercept = other.getMaxIntercept();
        minBeta = other.getMinBeta();
        maxBeta = other.getMaxBeta();
        minGamma = other.getMinGamma();
        maxGamma = other.getMaxGamma();
        vDistMax = other.getvDistMax();
        vDistMin = other.getvDistMin();
        bDistMax = other.getbDistMax();
        bDistMin = other.getbDistMin();
        iterations = other.getIterations();
        gDistMin = other.getgDistMin();
        gDistMax = other.getgDistMax();
        if (other.getFittingPointCount() != 0.0) {
            fittingPointCount = other.getFittingPointCount();
        }
        if (other.getLengthWeight() != 0.0) {
            lengthWeight = other.getLengthWeight();
        }
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                b0reg,
                    bDistMax,
                    bDistMin,
                    fittingPointCount,
                    g0reg,
                    g1reg,
                    gDistMax,
                    gDistMin,
                    id,
                    iterations,
                    maxBP1,
                    maxBP2,
                    maxBP3,
                    maxBeta,
                    maxGP1,
                    maxGP2,
                    maxGP3,
                    maxGamma,
                    maxIntercept,
                    maxVP1,
                    maxVP2,
                    maxVP3,
                    minBP1,
                    minBP2,
                    minBP3,
                    minBeta,
                    minGP1,
                    minGP2,
                    minGP3,
                    minGamma,
                    minIntercept,
                    minVP1,
                    minVP2,
                    minVP3,
                    v0reg,
                    vDistMax,
                    vDistMin,
                    version,
                    ybbMax,
                    ybbMin,
                    yggMax,
                    yggMin,
                    yvvMax,
                    yvvMin,
                    lengthWeight);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ShapeFitterConstraints)) {
            return false;
        }
        ShapeFitterConstraints other = (ShapeFitterConstraints) obj;
        return Double.doubleToLongBits(b0reg) == Double.doubleToLongBits(other.b0reg)
                && Double.doubleToLongBits(bDistMax) == Double.doubleToLongBits(other.bDistMax)
                && Double.doubleToLongBits(bDistMin) == Double.doubleToLongBits(other.bDistMin)
                && fittingPointCount == other.fittingPointCount
                && Double.doubleToLongBits(g0reg) == Double.doubleToLongBits(other.g0reg)
                && Double.doubleToLongBits(g1reg) == Double.doubleToLongBits(other.g1reg)
                && Double.doubleToLongBits(gDistMax) == Double.doubleToLongBits(other.gDistMax)
                && Double.doubleToLongBits(gDistMin) == Double.doubleToLongBits(other.gDistMin)
                && Objects.equals(id, other.id)
                && iterations == other.iterations
                && Double.doubleToLongBits(maxBP1) == Double.doubleToLongBits(other.maxBP1)
                && Double.doubleToLongBits(maxBP2) == Double.doubleToLongBits(other.maxBP2)
                && Double.doubleToLongBits(maxBP3) == Double.doubleToLongBits(other.maxBP3)
                && Double.doubleToLongBits(maxBeta) == Double.doubleToLongBits(other.maxBeta)
                && Double.doubleToLongBits(maxGP1) == Double.doubleToLongBits(other.maxGP1)
                && Double.doubleToLongBits(maxGP2) == Double.doubleToLongBits(other.maxGP2)
                && Double.doubleToLongBits(maxGP3) == Double.doubleToLongBits(other.maxGP3)
                && Double.doubleToLongBits(maxGamma) == Double.doubleToLongBits(other.maxGamma)
                && Double.doubleToLongBits(maxIntercept) == Double.doubleToLongBits(other.maxIntercept)
                && Double.doubleToLongBits(maxVP1) == Double.doubleToLongBits(other.maxVP1)
                && Double.doubleToLongBits(maxVP2) == Double.doubleToLongBits(other.maxVP2)
                && Double.doubleToLongBits(maxVP3) == Double.doubleToLongBits(other.maxVP3)
                && Double.doubleToLongBits(minBP1) == Double.doubleToLongBits(other.minBP1)
                && Double.doubleToLongBits(minBP2) == Double.doubleToLongBits(other.minBP2)
                && Double.doubleToLongBits(minBP3) == Double.doubleToLongBits(other.minBP3)
                && Double.doubleToLongBits(minBeta) == Double.doubleToLongBits(other.minBeta)
                && Double.doubleToLongBits(minGP1) == Double.doubleToLongBits(other.minGP1)
                && Double.doubleToLongBits(minGP2) == Double.doubleToLongBits(other.minGP2)
                && Double.doubleToLongBits(minGP3) == Double.doubleToLongBits(other.minGP3)
                && Double.doubleToLongBits(minGamma) == Double.doubleToLongBits(other.minGamma)
                && Double.doubleToLongBits(minIntercept) == Double.doubleToLongBits(other.minIntercept)
                && Double.doubleToLongBits(minVP1) == Double.doubleToLongBits(other.minVP1)
                && Double.doubleToLongBits(minVP2) == Double.doubleToLongBits(other.minVP2)
                && Double.doubleToLongBits(minVP3) == Double.doubleToLongBits(other.minVP3)
                && Double.doubleToLongBits(v0reg) == Double.doubleToLongBits(other.v0reg)
                && Double.doubleToLongBits(vDistMax) == Double.doubleToLongBits(other.vDistMax)
                && Double.doubleToLongBits(vDistMin) == Double.doubleToLongBits(other.vDistMin)
                && Objects.equals(version, other.version)
                && Double.doubleToLongBits(ybbMax) == Double.doubleToLongBits(other.ybbMax)
                && Double.doubleToLongBits(ybbMin) == Double.doubleToLongBits(other.ybbMin)
                && Double.doubleToLongBits(yggMax) == Double.doubleToLongBits(other.yggMax)
                && Double.doubleToLongBits(yggMin) == Double.doubleToLongBits(other.yggMin)
                && Double.doubleToLongBits(yvvMax) == Double.doubleToLongBits(other.yvvMax)
                && Double.doubleToLongBits(yvvMin) == Double.doubleToLongBits(other.yvvMin)
                && Double.doubleToLongBits(lengthWeight) == Double.doubleToLongBits(other.lengthWeight);
    }

    @Override
    public String toString() {
        return "ShapeFitterConstraints [id="
                + id
                + ", version="
                + version
                + ", maxVP1="
                + maxVP1
                + ", minVP1="
                + minVP1
                + ", v0reg="
                + v0reg
                + ", maxVP2="
                + maxVP2
                + ", minVP2="
                + minVP2
                + ", maxVP3="
                + maxVP3
                + ", minVP3="
                + minVP3
                + ", maxBP1="
                + maxBP1
                + ", minBP1="
                + minBP1
                + ", b0reg="
                + b0reg
                + ", maxBP2="
                + maxBP2
                + ", minBP2="
                + minBP2
                + ", maxBP3="
                + maxBP3
                + ", minBP3="
                + minBP3
                + ", maxGP1="
                + maxGP1
                + ", minGP1="
                + minGP1
                + ", g0reg="
                + g0reg
                + ", maxGP2="
                + maxGP2
                + ", minGP2="
                + minGP2
                + ", g1reg="
                + g1reg
                + ", maxGP3="
                + maxGP3
                + ", minGP3="
                + minGP3
                + ", yvvMin="
                + yvvMin
                + ", yvvMax="
                + yvvMax
                + ", vDistMax="
                + vDistMax
                + ", vDistMin="
                + vDistMin
                + ", ybbMin="
                + ybbMin
                + ", ybbMax="
                + ybbMax
                + ", bDistMax="
                + bDistMax
                + ", bDistMin="
                + bDistMin
                + ", yggMin="
                + yggMin
                + ", yggMax="
                + yggMax
                + ", gDistMin="
                + gDistMin
                + ", gDistMax="
                + gDistMax
                + ", minIntercept="
                + minIntercept
                + ", maxIntercept="
                + maxIntercept
                + ", minBeta="
                + minBeta
                + ", maxBeta="
                + maxBeta
                + ", minGamma="
                + minGamma
                + ", maxGamma="
                + maxGamma
                + ", iterations="
                + iterations
                + ", fittingPointCount="
                + fittingPointCount
                + ", lengthWeight="
                + lengthWeight
                + "]";
    }
}