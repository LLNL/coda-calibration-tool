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
package gov.llnl.gnem.apps.coda.spectra.model.domain;

public class SpectraEvent {
	
	private String eventID;
	private Fraction fraction;
	
	public SpectraEvent(String eventID) {
		this.eventID = eventID;
		this.fraction = Fraction.NONE;
	}
	
	public void setFraction(Fraction fraction) {
		this.fraction = fraction;
	}
	
	public Fraction getFraction() {
		return this.fraction;
	}

	/**
	 * Usually larger events
	 * @return True if event is a numerator.
	 */
	public boolean isNumerator() {
		return this.fraction == Fraction.NUMERATOR;
	}

	public void setNumerator(boolean isNumerator) {
		if (isNumerator) {
			this.fraction = Fraction.NUMERATOR;
		} else {
			this.fraction = Fraction.NONE;
		}
	}

	/**
	 * Usually smaller events.
	 * @return True if event is a denominator.
	 */
	public boolean isDenominator() {
		return this.fraction == Fraction.DENOMINATOR;
	}

	public void setDenominator(boolean isDenominator) {
		if (isDenominator) {
			this.fraction = Fraction.DENOMINATOR;
		} else {
			this.fraction = Fraction.NONE;
		}
	}

	public String getEventID() {
		return this.eventID;
	}
	
	
	public enum Fraction {
		NONE,
		// Usually larger events
		NUMERATOR,
		// Usually smaller events
		DENOMINATOR
	}
}
