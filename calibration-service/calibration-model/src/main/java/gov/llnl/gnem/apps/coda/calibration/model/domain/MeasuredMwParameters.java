/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Version;

import org.springframework.format.annotation.NumberFormat;

@Entity
@Table(name = "Measured_Mws", indexes = { @Index(columnList = "eventId", name = "event_id_index") })
public class MeasuredMwParameters implements Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "ID")
	private Long id;

	@Version
	private Integer version = 0;

	@Column(name = "eventId")
	private String eventId;

	@NumberFormat
	private double mw;

	@NumberFormat
	@Column(nullable = true)
	private Double meanMw;

	@NumberFormat
	@Column(nullable = true)
	private Double mwSd;

	@NumberFormat
	@Column(nullable = true)
	private Double mw1Min;

	@NumberFormat
	@Column(nullable = true)
	private Double mw1Max;

	@NumberFormat
	@Column(nullable = true)
	private Double mw2Min;

	@NumberFormat
	@Column(nullable = true)
	private Double mw2Max;

	@NumberFormat
	@Column(nullable = true)
	private Double apparentStressInMpa;

	@NumberFormat
	@Column(nullable = true)
	private Double meanApparentStressInMpa;

	@NumberFormat
	@Column(nullable = true)
	private Double apparentStressSd;

	@NumberFormat
	@Column(nullable = true)
	private Double apparentStress1Min;

	@NumberFormat
	@Column(nullable = true)
	private Double apparentStress1Max;

	@NumberFormat
	@Column(nullable = true)
	private Double apparentStress2Min;

	@NumberFormat
	@Column(nullable = true)
	private Double apparentStress2Max;

	@NumberFormat
	@Column(nullable = true)
	private Double misfit;

	@NumberFormat
	@Column(nullable = true)
	private Double meanMisfit;

	@NumberFormat
	@Column(nullable = true)
	private Double misfitSd;

	@NumberFormat
	@Column(nullable = true)
	private Double cornerFrequency;

	@NumberFormat
	@Column(nullable = true)
	private Double cornerFrequencySd;

	@NumberFormat
	@Column(nullable = true)
	private Double bandCoverage;

	@NumberFormat
	@Column(nullable = true)
	private Double obsEnergy;

	@NumberFormat
	@Column(nullable = true)
	private Double logTotalEnergy;

	@NumberFormat
	@Column(nullable = true)
	private Double logTotalEnergyMDAC;

	@NumberFormat
	@Column(nullable = true)
	private Double energyRatio;

	@NumberFormat
	@Column(nullable = true)
	private Double obsAppStress;

	private int dataCount;

	private int stationCount;

	private int iterations;

	public Long getId() {
		return id;
	}

	public MeasuredMwParameters setId(Long id) {
		this.id = id;
		return this;
	}

	public Integer getVersion() {
		return version;
	}

	public String getEventId() {
		return eventId;
	}

	public MeasuredMwParameters setEventId(String eventId) {
		this.eventId = eventId;
		return this;
	}

	public double getMw() {
		return mw;
	}

	public MeasuredMwParameters setMw(double mw) {
		this.mw = mw;
		return this;
	}

	public Double getMwSd() {
		return mwSd;
	}

	public MeasuredMwParameters setMwSd(Double mwSd) {
		this.mwSd = mwSd;
		return this;
	}

	public Double getMw1Min() {
		return mw1Min;
	}

	public MeasuredMwParameters setMw1Min(Double mw1Min) {
		this.mw1Min = mw1Min;
		return this;
	}

	public Double getMw1Max() {
		return mw1Max;
	}

	public MeasuredMwParameters setMw1Max(Double mw1Max) {
		this.mw1Max = mw1Max;
		return this;
	}

	public Double getMw2Min() {
		return mw2Min;
	}

	public MeasuredMwParameters setMw2Min(Double mw2Min) {
		this.mw2Min = mw2Min;
		return this;
	}

	public Double getMw2Max() {
		return mw2Max;
	}

	public MeasuredMwParameters setMw2Max(Double mw2Max) {
		this.mw2Max = mw2Max;
		return this;
	}

	public Double getApparentStressInMpa() {
		return apparentStressInMpa;
	}

	public MeasuredMwParameters setApparentStressInMpa(Double apparentStressInMpa) {
		this.apparentStressInMpa = apparentStressInMpa;
		return this;
	}

	public Double getApparentStressSd() {
		return apparentStressSd;
	}

	public MeasuredMwParameters setApparentStressSd(Double apparentStressSd) {
		this.apparentStressSd = apparentStressSd;
		return this;
	}

	public Double getApparentStress1Min() {
		return apparentStress1Min;
	}

	public MeasuredMwParameters setApparentStress1Min(Double apparentStress1Min) {
		this.apparentStress1Min = apparentStress1Min;
		return this;
	}

	public Double getApparentStress1Max() {
		return apparentStress1Max;
	}

	public MeasuredMwParameters setApparentStress1Max(Double apparentStress1Max) {
		this.apparentStress1Max = apparentStress1Max;
		return this;
	}

	public Double getApparentStress2Min() {
		return apparentStress2Min;
	}

	public MeasuredMwParameters setApparentStress2Min(Double apparentStress2Min) {
		this.apparentStress2Min = apparentStress2Min;
		return this;
	}

	public Double getApparentStress2Max() {
		return apparentStress2Max;
	}

	public MeasuredMwParameters setApparentStress2Max(Double apparentStress2Max) {
		this.apparentStress2Max = apparentStress2Max;
		return this;
	}

	public Double getMisfit() {
		return misfit;
	}

	public MeasuredMwParameters setMisfit(Double misfit) {
		this.misfit = misfit;
		return this;
	}

	public Double getMisfitSd() {
		return misfitSd;
	}

	public MeasuredMwParameters setMisfitSd(Double misfitSd) {
		this.misfitSd = misfitSd;
		return this;
	}

	public Integer getDataCount() {
		return dataCount;
	}

	public MeasuredMwParameters setDataCount(Integer dataCount) {
		this.dataCount = dataCount;
		return this;
	}

	public Integer getStationCount() {
		return stationCount;
	}

	public MeasuredMwParameters setStationCount(Integer stationCount) {
		this.stationCount = stationCount;
		return this;
	}

	public Double getObsEnergy() {
		return obsEnergy;
	}

	public MeasuredMwParameters setObsEnergy(Double obsEnergy) {
		this.obsEnergy = obsEnergy;
		return this;
	}

	public Double getLogTotalEnergy() {
		return logTotalEnergy;
	}

	public MeasuredMwParameters setLogTotalEnergy(Double logTotalEnergy) {
		this.logTotalEnergy = logTotalEnergy;
		return this;
	}

	public Double getLogTotalEnergyMDAC() {
		return logTotalEnergyMDAC;
	}

	public MeasuredMwParameters setLogTotalEnergyMDAC(Double totalEnergyMDAC) {
		this.logTotalEnergyMDAC = totalEnergyMDAC;
		return this;
	}

	public Double getEnergyRatio() {
		return energyRatio;
	}

	public MeasuredMwParameters setEnergyRatio(Double energyRatio) {
		this.energyRatio = energyRatio;
		return this;
	}

	public Double getObsAppStress() {
		return obsAppStress;
	}

	public MeasuredMwParameters setObsAppStress(Double obsAppStress) {
		this.obsAppStress = obsAppStress;
		return this;
	}

	public Double getMeanMw() {
		return meanMw;
	}

	public MeasuredMwParameters setMeanMw(Double meanMw) {
		this.meanMw = meanMw;
		return this;
	}

	public Double getMeanApparentStressInMpa() {
		return meanApparentStressInMpa;
	}

	public MeasuredMwParameters setMeanApparentStressInMpa(Double meanApparentStressInMpa) {
		this.meanApparentStressInMpa = meanApparentStressInMpa;
		return this;
	}

	public Double getMeanMisfit() {
		return meanMisfit;
	}

	public MeasuredMwParameters setMeanMisfit(Double meanMisfit) {
		this.meanMisfit = meanMisfit;
		return this;
	}

	public Double getCornerFrequency() {
		return cornerFrequency;
	}

	public MeasuredMwParameters setCornerFrequency(Double cornerFrequency) {
		this.cornerFrequency = cornerFrequency;
		return this;
	}

	public Double getCornerFrequencySd() {
		return cornerFrequencySd;
	}

	public MeasuredMwParameters setCornerFrequencySd(Double cornerFrequencySd) {
		this.cornerFrequencySd = cornerFrequencySd;
		return this;
	}

	public int getIterations() {
		return iterations;
	}

	public MeasuredMwParameters setIterations(int iterations) {
		this.iterations = iterations;
		return this;
	}

	public Double getBandCoverage() {
		return bandCoverage;
	}

	public MeasuredMwParameters setBandCoverage(Double bandCoverage) {
		this.bandCoverage = bandCoverage;
		return this;
	}

	public MeasuredMwParameters merge(MeasuredMwParameters overlay) {
		this.mw = overlay.getMw();
		this.meanMw = overlay.getMeanMw();
		this.mwSd = overlay.getMwSd();
		this.mw1Min = overlay.getMw1Min();
		this.mw1Max = overlay.getMw1Max();
		this.mw2Min = overlay.getMw2Min();
		this.mw2Max = overlay.getMw2Max();
		this.apparentStressInMpa = overlay.getApparentStressInMpa();
		this.meanApparentStressInMpa = overlay.getMeanApparentStressInMpa();
		this.apparentStressSd = overlay.getApparentStressSd();
		this.apparentStress1Min = overlay.getApparentStress1Min();
		this.apparentStress1Max = overlay.getApparentStress1Max();
		this.apparentStress2Min = overlay.getApparentStress2Min();
		this.apparentStress2Max = overlay.getApparentStress2Max();
		this.misfit = overlay.getMisfit();
		this.meanMisfit = overlay.getMeanMisfit();
		this.misfitSd = overlay.getMisfitSd();
		this.dataCount = overlay.getDataCount();
		this.stationCount = overlay.getStationCount();
		this.bandCoverage = overlay.getBandCoverage();
		this.iterations = overlay.getIterations();
		this.cornerFrequency = overlay.getCornerFrequency();
		this.cornerFrequencySd = overlay.getCornerFrequencySd();
		this.obsEnergy = overlay.getObsEnergy();
		this.logTotalEnergy = overlay.getLogTotalEnergy();
		this.logTotalEnergyMDAC = overlay.getLogTotalEnergyMDAC();
		this.energyRatio = overlay.getEnergyRatio();
		this.obsAppStress = overlay.getObsAppStress();

		return this;
	}

	@Override
	public int hashCode() {
		return Objects.hash(apparentStress1Max, apparentStress1Min, apparentStress2Max, apparentStress2Min,
				apparentStressInMpa, apparentStressSd, bandCoverage, cornerFrequency, cornerFrequencySd, dataCount,
				energyRatio, obsAppStress, eventId, id, iterations, meanApparentStressInMpa, meanMisfit, meanMw, misfit,
				misfitSd, mw, mw1Max, mw1Min, mw2Max, mw2Min, mwSd, stationCount, obsEnergy, logTotalEnergy,
				logTotalEnergyMDAC, version);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MeasuredMwParameters)) {
			return false;
		}
		MeasuredMwParameters other = (MeasuredMwParameters) obj;
		return Objects.equals(apparentStress1Max, other.apparentStress1Max)
				&& Objects.equals(apparentStress1Min, other.apparentStress1Min)
				&& Objects.equals(apparentStress2Max, other.apparentStress2Max)
				&& Objects.equals(apparentStress2Min, other.apparentStress2Min)
				&& Objects.equals(apparentStressInMpa, other.apparentStressInMpa)
				&& Objects.equals(apparentStressSd, other.apparentStressSd)
				&& Objects.equals(bandCoverage, other.bandCoverage)
				&& Objects.equals(cornerFrequency, other.cornerFrequency)
				&& Objects.equals(cornerFrequencySd, other.cornerFrequencySd) && dataCount == other.dataCount
				&& Objects.equals(energyRatio, other.energyRatio) && Objects.equals(obsAppStress, other.obsAppStress)
				&& Objects.equals(eventId, other.eventId) && Objects.equals(id, other.id)
				&& iterations == other.iterations
				&& Objects.equals(meanApparentStressInMpa, other.meanApparentStressInMpa)
				&& Objects.equals(meanMisfit, other.meanMisfit) && Objects.equals(meanMw, other.meanMw)
				&& Objects.equals(misfit, other.misfit) && Objects.equals(misfitSd, other.misfitSd)
				&& Double.doubleToLongBits(mw) == Double.doubleToLongBits(other.mw)
				&& Objects.equals(mw1Max, other.mw1Max) && Objects.equals(mw1Min, other.mw1Min)
				&& Objects.equals(mw2Max, other.mw2Max) && Objects.equals(mw2Min, other.mw2Min)
				&& Objects.equals(mwSd, other.mwSd) && stationCount == other.stationCount
				&& Objects.equals(obsEnergy, other.obsEnergy) && Objects.equals(logTotalEnergy, other.logTotalEnergy)
				&& Objects.equals(logTotalEnergyMDAC, other.logTotalEnergyMDAC)
				&& Objects.equals(version, other.version);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MeasuredMwParameters [id=");
		builder.append(id);
		builder.append(", version=");
		builder.append(version);
		builder.append(", eventId=");
		builder.append(eventId);
		builder.append(", mw=");
		builder.append(mw);
		builder.append(", meanMw=");
		builder.append(meanMw);
		builder.append(", mwSd=");
		builder.append(mwSd);
		builder.append(", mw1Min=");
		builder.append(mw1Min);
		builder.append(", mw1Max=");
		builder.append(mw1Max);
		builder.append(", mw2Min=");
		builder.append(mw2Min);
		builder.append(", mw2Max=");
		builder.append(mw2Max);
		builder.append(", apparentStressInMpa=");
		builder.append(apparentStressInMpa);
		builder.append(", meanApparentStressInMpa=");
		builder.append(meanApparentStressInMpa);
		builder.append(", apparentStressSd=");
		builder.append(apparentStressSd);
		builder.append(", apparentStress1Min=");
		builder.append(apparentStress1Min);
		builder.append(", apparentStress1Max=");
		builder.append(apparentStress1Max);
		builder.append(", apparentStress2Min=");
		builder.append(apparentStress2Min);
		builder.append(", apparentStress2Max=");
		builder.append(apparentStress2Max);
		builder.append(", misfit=");
		builder.append(misfit);
		builder.append(", meanMisfit=");
		builder.append(meanMisfit);
		builder.append(", misfitSd=");
		builder.append(misfitSd);
		builder.append(", cornerFrequency=");
		builder.append(cornerFrequency);
		builder.append(", cornerFrequencySd=");
		builder.append(cornerFrequencySd);
		builder.append(", bandCoverage=");
		builder.append(bandCoverage);
		builder.append(", obsEnergy=");
		builder.append(obsEnergy);
		builder.append(", logTotalEnergy=");
		builder.append(logTotalEnergy);
		builder.append(", logTotalEnergyMDAC=");
		builder.append(logTotalEnergyMDAC);
		builder.append(", energyRatio=");
		builder.append(energyRatio);
		builder.append(", obsAppStress=");
		builder.append(obsAppStress);
		builder.append(", dataCount=");
		builder.append(dataCount);
		builder.append(", stationCount=");
		builder.append(stationCount);
		builder.append(", iterations=");
		builder.append(iterations);
		builder.append("]");
		return builder.toString();
	}
}