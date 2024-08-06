/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.spectra.model.domain;

import java.util.Objects;

import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.springframework.format.annotation.NumberFormat;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "Spectra_Ratio_Pair_Inversion_Sample_Joint")
public class SpectraRatioPairInversionResultJoint {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "ID")
    private Long id;

    @Version
    private Integer version = 0;

    @Column
    @NotNull
    private String eventIdA;

    @Column
    @NotNull
    private String eventIdB;

    @Column
    @NumberFormat
    @NotNull
    private float momentEstimateA;

    @Column
    @NumberFormat
    @NotNull
    private float cornerEstimateA;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateA1Min;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateA2Min;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateA1Max;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateA2Max;

    @Column
    @NumberFormat
    @NotNull
    private float momentEstimateB;

    @Column
    @NumberFormat
    @NotNull
    private float cornerEstimateB;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateB1Min;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateB2Min;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateB1Max;

    @Column
    @NumberFormat
    @NotNull
    private double cornerEstimateB2Max;

    @Column
    @NumberFormat
    @NotNull
    private float apparentStressEstimateA;

    @Column
    @NumberFormat
    @NotNull
    private float apparentStressEstimateB;

    @Column
    @NumberFormat
    @NotNull
    private float misfit;

    @Column
    @NotNull
    @Lob
    @Basic
    private FloatArrayList m0samples = new FloatArrayList(0);

    @Column
    @NotNull
    @Lob
    @Basic
    private IntArrayList m0XIndex = new IntArrayList(0);

    @Column
    @NotNull
    @Lob
    @Basic
    private IntArrayList m0YIndex = new IntArrayList(0);

    @Column
    @NotNull
    @Lob
    @Basic
    private FloatArrayList stressSamples = new FloatArrayList(0);

    @Column
    @NotNull
    @Lob
    @Basic
    private IntArrayList stressXIndex = new IntArrayList(0);

    @Column
    @NotNull
    @Lob
    @Basic
    private IntArrayList stressYIndex = new IntArrayList(0);

    @Column
    @NumberFormat
    @NotNull
    private float m0minX;

    @Column
    @NumberFormat
    @NotNull
    private float m0maxX;

    @Column
    @NumberFormat
    @NotNull
    private float m0minY;

    @Column
    @NumberFormat
    @NotNull
    private float m0maxY;

    @Column
    @NumberFormat
    @NotNull
    private int m0Xdim;

    @Column
    @NumberFormat
    @NotNull
    private int m0Ydim;

    @Column
    @NumberFormat
    @NotNull
    private float appStressMin;

    @Column
    @NumberFormat
    @NotNull
    private float appStressMax;

    @Column
    @NumberFormat
    @NotNull
    private int appStressXdim;

    @Column
    @NumberFormat
    @NotNull
    private int appStressYdim;

    @Column
    @NumberFormat
    @NotNull
    private double kConstant;

    public SpectraRatioPairInversionResultJoint() {
    }

    public Long getId() {
        return id;
    }

    public SpectraRatioPairInversionResultJoint setId(Long id) {
        this.id = id;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public SpectraRatioPairInversionResultJoint setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public String getEventIdA() {
        return eventIdA;
    }

    public SpectraRatioPairInversionResultJoint setEventIdA(String eventIdA) {
        this.eventIdA = eventIdA;
        return this;
    }

    public String getEventIdB() {
        return eventIdB;
    }

    public SpectraRatioPairInversionResultJoint setEventIdB(String eventIdB) {
        this.eventIdB = eventIdB;
        return this;
    }

    public float getMomentEstimateA() {
        return momentEstimateA;
    }

    public SpectraRatioPairInversionResultJoint setMomentEstimateA(float momentEstimate) {
        this.momentEstimateA = momentEstimate;
        return this;
    }

    public float getCornerEstimateA() {
        return cornerEstimateA;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateA(float cornerEstimate) {
        this.cornerEstimateA = cornerEstimate;
        return this;
    }

    public double getCornerEstimateA1Min() {
        return cornerEstimateA1Min;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateA1Min(double cornerEstimateA1Min) {
        this.cornerEstimateA1Min = cornerEstimateA1Min;
        return this;
    }

    public double getCornerEstimateA2Min() {
        return cornerEstimateA2Min;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateA2Min(double cornerEstimateA2Min) {
        this.cornerEstimateA2Min = cornerEstimateA2Min;
        return this;
    }

    public double getCornerEstimateA1Max() {
        return cornerEstimateA1Max;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateA1Max(double cornerEstimateA1Max) {
        this.cornerEstimateA1Max = cornerEstimateA1Max;
        return this;
    }

    public double getCornerEstimateA2Max() {
        return cornerEstimateA2Max;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateA2Max(double cornerEstimateA2Max) {
        this.cornerEstimateA2Max = cornerEstimateA2Max;
        return this;
    }

    public float getMomentEstimateB() {
        return momentEstimateB;
    }

    public SpectraRatioPairInversionResultJoint setMomentEstimateB(float momentEstimate) {
        this.momentEstimateB = momentEstimate;
        return this;
    }

    public float getCornerEstimateB() {
        return cornerEstimateB;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateB(float cornerEstimate) {
        this.cornerEstimateB = cornerEstimate;
        return this;
    }

    public double getCornerEstimateB1Min() {
        return cornerEstimateB1Min;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateB1Min(double cornerEstimateB1Min) {
        this.cornerEstimateB1Min = cornerEstimateB1Min;
        return this;
    }

    public double getCornerEstimateB2Min() {
        return cornerEstimateB2Min;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateB2Min(double cornerEstimateB2Min) {
        this.cornerEstimateB2Min = cornerEstimateB2Min;
        return this;
    }

    public double getCornerEstimateB1Max() {
        return cornerEstimateB1Max;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateB1Max(double cornerEstimateB1Max) {
        this.cornerEstimateB1Max = cornerEstimateB1Max;
        return this;
    }

    public double getCornerEstimateB2Max() {
        return cornerEstimateB2Max;
    }

    public SpectraRatioPairInversionResultJoint setCornerEstimateB2Max(double cornerEstimateB2Max) {
        this.cornerEstimateB2Max = cornerEstimateB2Max;
        return this;
    }

    public float getApparentStressEstimateA() {
        return apparentStressEstimateA;
    }

    public SpectraRatioPairInversionResultJoint setApparentStressEstimateA(float apparentStressEstimateA) {
        this.apparentStressEstimateA = apparentStressEstimateA;
        return this;
    }

    public float getApparentStressEstimateB() {
        return apparentStressEstimateB;
    }

    public SpectraRatioPairInversionResultJoint setApparentStressEstimateB(float apparentStressEstimateB) {
        this.apparentStressEstimateB = apparentStressEstimateB;
        return this;
    }

    public float getMisfit() {
        return misfit;
    }

    public SpectraRatioPairInversionResultJoint setMisfit(float misfit) {
        this.misfit = misfit;
        return this;
    }

    @JsonIgnore
    public FloatArrayList getM0data() {
        return m0samples;
    }

    @JsonIgnore
    public SpectraRatioPairInversionResultJoint setM0data(FloatArrayList m0samples) {
        this.m0samples = m0samples;
        return this;
    }

    public float[] getM0samples() {
        return m0samples.toArray();
    }

    public SpectraRatioPairInversionResultJoint setM0samples(float[] m0samples) {
        this.m0samples = new FloatArrayList(m0samples);
        return this;
    }

    @JsonIgnore
    public IntArrayList getM0XIdx() {
        return m0XIndex;
    }

    @JsonIgnore
    public SpectraRatioPairInversionResultJoint setM0XIdx(IntArrayList m0xIndex) {
        m0XIndex = m0xIndex;
        return this;
    }

    public int[] getM0XIndex() {
        return m0XIndex.toArray();
    }

    public SpectraRatioPairInversionResultJoint setM0XIndex(int[] m0xIndex) {
        m0XIndex = new IntArrayList(m0xIndex);
        return this;
    }

    @JsonIgnore
    public IntArrayList getM0YIdx() {
        return m0YIndex;
    }

    @JsonIgnore
    public SpectraRatioPairInversionResultJoint setM0YIdx(IntArrayList m0yIndex) {
        m0YIndex = m0yIndex;
        return this;
    }

    public int[] getM0YIndex() {
        return m0YIndex.toArray();
    }

    public SpectraRatioPairInversionResultJoint setM0YIndex(int[] m0yIndex) {
        m0YIndex = new IntArrayList(m0yIndex);
        return this;
    }

    @JsonIgnore
    public FloatArrayList getStressData() {
        return stressSamples;
    }

    @JsonIgnore
    public SpectraRatioPairInversionResultJoint setStressData(FloatArrayList stressSamples) {
        this.stressSamples = stressSamples;
        return this;
    }

    public float[] getStressSamples() {
        return stressSamples.toArray();
    }

    public SpectraRatioPairInversionResultJoint setStressSamples(float[] stressSamples) {
        this.stressSamples = new FloatArrayList(stressSamples);
        return this;
    }

    @JsonIgnore
    public IntArrayList getStressXIdx() {
        return stressXIndex;
    }

    @JsonIgnore
    public SpectraRatioPairInversionResultJoint setStressXIdx(IntArrayList stressXIndex) {
        this.stressXIndex = stressXIndex;
        return this;
    }

    public int[] getStressXIndex() {
        return stressXIndex.toArray();
    }

    public SpectraRatioPairInversionResultJoint setStressXIndex(int[] stressXIndex) {
        this.stressXIndex = new IntArrayList(stressXIndex);
        return this;
    }

    @JsonIgnore
    public IntArrayList getStressYIdx() {
        return stressYIndex;
    }

    @JsonIgnore
    public SpectraRatioPairInversionResultJoint setStressYIdx(IntArrayList stressYIndex) {
        this.stressYIndex = stressYIndex;
        return this;
    }

    public int[] getStressYIndex() {
        return stressYIndex.toArray();
    }

    public SpectraRatioPairInversionResultJoint setStressYIndex(int[] stressYIndex) {
        this.stressYIndex = new IntArrayList(stressYIndex);
        return this;
    }

    public float getM0minX() {
        return m0minX;
    }

    public SpectraRatioPairInversionResultJoint setM0minX(float m0minX) {
        this.m0minX = m0minX;
        return this;
    }

    public float getM0maxX() {
        return m0maxX;
    }

    public SpectraRatioPairInversionResultJoint setM0maxX(float m0maxX) {
        this.m0maxX = m0maxX;
        return this;
    }

    public float getM0minY() {
        return m0minY;
    }

    public SpectraRatioPairInversionResultJoint setM0minY(float m0minY) {
        this.m0minY = m0minY;
        return this;
    }

    public float getM0maxY() {
        return m0maxY;
    }

    public SpectraRatioPairInversionResultJoint setM0maxY(float m0maxY) {
        this.m0maxY = m0maxY;
        return this;
    }

    public float getAppStressMin() {
        return appStressMin;
    }

    public SpectraRatioPairInversionResultJoint setAppStressMin(float appStressMin) {
        this.appStressMin = appStressMin;
        return this;
    }

    public float getAppStressMax() {
        return appStressMax;
    }

    public SpectraRatioPairInversionResultJoint setAppStressMax(float appStressMax) {
        this.appStressMax = appStressMax;
        return this;
    }

    public int getM0Xdim() {
        return m0Xdim;
    }

    public SpectraRatioPairInversionResultJoint setM0Xdim(int m0Xdim) {
        this.m0Xdim = m0Xdim;
        return this;
    }

    public int getM0Ydim() {
        return m0Ydim;
    }

    public SpectraRatioPairInversionResultJoint setM0Ydim(int m0Ydim) {
        this.m0Ydim = m0Ydim;
        return this;
    }

    public int getAppStressXdim() {
        return appStressXdim;
    }

    public SpectraRatioPairInversionResultJoint setAppStressXdim(int appStressXdim) {
        this.appStressXdim = appStressXdim;
        return this;
    }

    public int getAppStressYdim() {
        return appStressYdim;
    }

    public SpectraRatioPairInversionResultJoint setAppStressYdim(int appStressYdim) {
        this.appStressYdim = appStressYdim;
        return this;
    }

    public double getkConstant() {
        return kConstant;
    }

    public SpectraRatioPairInversionResultJoint setkConstant(double kConstant) {
        this.kConstant = kConstant;
        return this;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                appStressMax,
                    appStressMin,
                    appStressXdim,
                    appStressYdim,
                    apparentStressEstimateA,
                    apparentStressEstimateB,
                    cornerEstimateA,
                    cornerEstimateB,
                    eventIdA,
                    eventIdB,
                    id,
                    kConstant,
                    m0XIndex,
                    m0Xdim,
                    m0YIndex,
                    m0Ydim,
                    m0maxX,
                    m0maxY,
                    m0minX,
                    m0minY,
                    m0samples,
                    misfit,
                    momentEstimateA,
                    momentEstimateB,
                    stressSamples,
                    stressXIndex,
                    stressYIndex,
                    version);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof SpectraRatioPairInversionResultJoint)) {
            return false;
        }
        SpectraRatioPairInversionResultJoint other = (SpectraRatioPairInversionResultJoint) obj;
        return Float.floatToIntBits(appStressMax) == Float.floatToIntBits(other.appStressMax)
                && Float.floatToIntBits(appStressMin) == Float.floatToIntBits(other.appStressMin)
                && appStressXdim == other.appStressXdim
                && appStressYdim == other.appStressYdim
                && Float.floatToIntBits(apparentStressEstimateA) == Float.floatToIntBits(other.apparentStressEstimateA)
                && Float.floatToIntBits(apparentStressEstimateB) == Float.floatToIntBits(other.apparentStressEstimateB)
                && Float.floatToIntBits(cornerEstimateA) == Float.floatToIntBits(other.cornerEstimateA)
                && Float.floatToIntBits(cornerEstimateB) == Float.floatToIntBits(other.cornerEstimateB)
                && Objects.equals(eventIdA, other.eventIdA)
                && Objects.equals(eventIdB, other.eventIdB)
                && Objects.equals(id, other.id)
                && Double.doubleToLongBits(kConstant) == Double.doubleToLongBits(other.kConstant)
                && Objects.equals(m0XIndex, other.m0XIndex)
                && m0Xdim == other.m0Xdim
                && Objects.equals(m0YIndex, other.m0YIndex)
                && m0Ydim == other.m0Ydim
                && Float.floatToIntBits(m0maxX) == Float.floatToIntBits(other.m0maxX)
                && Float.floatToIntBits(m0maxY) == Float.floatToIntBits(other.m0maxY)
                && Float.floatToIntBits(m0minX) == Float.floatToIntBits(other.m0minX)
                && Float.floatToIntBits(m0minY) == Float.floatToIntBits(other.m0minY)
                && Objects.equals(m0samples, other.m0samples)
                && Float.floatToIntBits(misfit) == Float.floatToIntBits(other.misfit)
                && Float.floatToIntBits(momentEstimateA) == Float.floatToIntBits(other.momentEstimateA)
                && Float.floatToIntBits(momentEstimateB) == Float.floatToIntBits(other.momentEstimateB)
                && Objects.equals(stressSamples, other.stressSamples)
                && Objects.equals(stressXIndex, other.stressXIndex)
                && Objects.equals(stressYIndex, other.stressYIndex)
                && Objects.equals(version, other.version);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectraRatioPairInversionResultJoint [id=")
               .append(id)
               .append(", version=")
               .append(version)
               .append(", eventIdA=")
               .append(eventIdA)
               .append(", eventIdB=")
               .append(eventIdB)
               .append(", momentEstimateA=")
               .append(momentEstimateA)
               .append(", cornerEstimateA=")
               .append(cornerEstimateA)
               .append(", momentEstimateB=")
               .append(momentEstimateB)
               .append(", cornerEstimateB=")
               .append(cornerEstimateB)
               .append(", apparentStressEstimateA=")
               .append(apparentStressEstimateA)
               .append(", apparentStressEstimateB=")
               .append(apparentStressEstimateB)
               .append(", misfit=")
               .append(misfit)
               .append(", m0samples=")
               .append(m0samples)
               .append(", m0XIndex=")
               .append(m0XIndex)
               .append(", m0YIndex=")
               .append(m0YIndex)
               .append(", stressSamples=")
               .append(stressSamples)
               .append(", stressXIndex=")
               .append(stressXIndex)
               .append(", stressYIndex=")
               .append(stressYIndex)
               .append(", m0minX=")
               .append(m0minX)
               .append(", m0maxX=")
               .append(m0maxX)
               .append(", m0minY=")
               .append(m0minY)
               .append(", m0maxY=")
               .append(m0maxY)
               .append(", m0Xdim=")
               .append(m0Xdim)
               .append(", m0Ydim=")
               .append(m0Ydim)
               .append(", appStressMin=")
               .append(appStressMin)
               .append(", appStressMax=")
               .append(appStressMax)
               .append(", appStressXdim=")
               .append(appStressXdim)
               .append(", appStressYdim=")
               .append(appStressYdim)
               .append(", kConstant=")
               .append(kConstant)
               .append("]");
        return builder.toString();
    }

}
