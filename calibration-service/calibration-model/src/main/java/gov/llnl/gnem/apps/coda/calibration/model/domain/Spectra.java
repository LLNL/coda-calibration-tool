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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;

public class Spectra {

	private List<java.awt.geom.Point2D.Double> xyVals;
	private double apparentStress = -1;
	private double mw = -1;
	private Double cornerFrequency = null;
	private double obsEnergy;
	private double logTotalEnergy = 0;
	private double logTotalEnergyMDAC = 0;
	private double obsAppStress = 0;
	private SPECTRA_TYPES type;

	/**
	 * @param xyVals List of java.awt.geom.Point2D.Double entries representing X, Y
	 *               points
	 * @param mw
	 * @param stress
	 */
	public Spectra(SPECTRA_TYPES type, List<java.awt.geom.Point2D.Double> xyVals, Double mw, Double stress,
			Double cornerFrequency, Double obsEnergy, Double logTotalEnergy, Double logTotalEnergyMDAC,
			Double obsAppStress) {
		this.type = type;
		this.xyVals = xyVals;
		if (mw != null) {
			this.mw = mw;
		}
		if (stress != null) {
			this.apparentStress = stress;
		}
		if (cornerFrequency != null) {
			this.cornerFrequency = cornerFrequency;
		}
		if (obsEnergy != null) {
			this.obsEnergy = obsEnergy;
		}
		if (logTotalEnergy != null) {
			this.logTotalEnergy = logTotalEnergy;
		}
		if (logTotalEnergyMDAC != null) {
			this.logTotalEnergyMDAC = logTotalEnergyMDAC;
		}
		if (obsAppStress != null) {
			this.obsAppStress = obsAppStress;
		}
	}

	public Spectra() {
		this.type = SPECTRA_TYPES.UNK;
		xyVals = new ArrayList<>();
	}

	public List<java.awt.geom.Point2D.Double> getSpectraXY() {
		return xyVals;
	}

	public double getApparentStress() {
		return apparentStress;
	}

	public double getObsAppStress() {
		return obsAppStress;
	}

	public double getMw() {
		return mw;
	}

	public SPECTRA_TYPES getType() {
		return type;
	}

	public Double getCornerFrequency() {
		return cornerFrequency;
	}

	public Double getObsEnergy() {
		return obsEnergy;
	}

	public Double getLogTotalEnergy() {
		return logTotalEnergy;
	}

	public Double getLogTotalEnergyMDAC() {
		return logTotalEnergyMDAC;
	}

	@Override
	public int hashCode() {
		return Objects.hash(apparentStress, cornerFrequency, mw, obsEnergy, logTotalEnergy, logTotalEnergyMDAC, type,
				xyVals);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Spectra)) {
			return false;
		}
		Spectra other = (Spectra) obj;
		return Double.doubleToLongBits(apparentStress) == Double.doubleToLongBits(other.apparentStress)
				&& Objects.equals(cornerFrequency, other.cornerFrequency)
				&& Double.doubleToLongBits(mw) == Double.doubleToLongBits(other.mw)
				&& Objects.equals(obsEnergy, other.obsEnergy) && Objects.equals(logTotalEnergy, other.logTotalEnergy)
				&& Objects.equals(logTotalEnergyMDAC, other.logTotalEnergyMDAC) && type == other.type
				&& Objects.equals(xyVals, other.xyVals);
	}

	@Override
	public String toString() {
		final int maxLen = 10;
		StringBuilder builder = new StringBuilder();
		builder.append("Spectra [xyVals=")
				.append(xyVals != null ? xyVals.subList(0, Math.min(xyVals.size(), maxLen)) : null)
				.append(", apparentStress=").append(apparentStress).append(", mw=").append(mw)
				.append(", cornerFrequency=").append(cornerFrequency).append(", obsEnergy=").append(obsEnergy)
				.append(", logTotalEnergy=").append(logTotalEnergy).append(", logTotalEnergyMDAC=")
				.append(logTotalEnergyMDAC).append(", obsAppStress=").append(obsAppStress).append("]").append(", type=")
				.append(type).append("]");
		return builder.toString();
	}
}
