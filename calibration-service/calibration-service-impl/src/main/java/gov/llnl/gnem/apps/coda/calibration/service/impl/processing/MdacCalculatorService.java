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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

@Service
public class MdacCalculatorService {

    /**
     * Calculate the MDAC2 Source Spectra (with no Q, site, or geometrical
     * spreading terms)
     * 
     * @param psEntry
     *            Phase specific parameters for the MDAC2 model
     * @param fiEntry
     *            Independent parameters for the MDAC2 model
     * @param frequency
     *            Frequency to compute the MDAC2 source spectra for
     * @param Mw
     *            Magnitude of expected event
     * @param distance
     *            In kilometers. May be set to < 0, in which case it is not used
     *            in computing the source spectra
     * @return logAmp, M0/wwc, w*M0/wwc
     */
    public double[] calculateMdacSourceSpectra(MdacParametersPS psEntry, MdacParametersFI fiEntry, double frequency, double Mw, double distanceInKm) {
        Double M0 = getM0(Mw);
        MdacCalculator mdc = new MdacCalculator(fiEntry.getSigma(),
                                                fiEntry.getM0ref(),
                                                fiEntry.getPsi(),
                                                fiEntry.getZeta(),
                                                fiEntry.getAlphas(),
                                                fiEntry.getBetas(),
                                                fiEntry.getRadPatP(),
                                                fiEntry.getRadPatS(),
                                                M0);
        mdc.initializePhaseSpecificVariables(distanceInKm, psEntry, fiEntry, M0);
        return mdc.calculateMdacSourceSpectra(frequency, M0);
    }

    /**
     * @param psEntry
     *            Phase specific parameters for the MDAC2 model
     * @param fiEntry
     *            Independent parameters for the MDAC2 model
     * @param frequency
     *            Frequency to compute the MDAC2 source spectra for
     * @param Mw
     *            Magnitude of expected event
     * @param phase
     *            The phase to use for the MDAC calculation. Should be one of
     *            the phases in {@link PICK_TYPES}.
     * @param stress
     *            Apparent stress in MPA to use. May be null to use the MDAC
     *            parameters instead. Otherwise Psi=0 and Sigma=stress
     * @return logAmp In Dyne-CM
     */
    public double calculateMdacAmplitudeForMw(MdacParametersPS psEntry, MdacParametersFI fiEntry, double Mw, double frequency, PICK_TYPES phase, Double stress) {
        // M0 in N-m units
        double M0 = MdacCalculator.DYNE_CM_TO_NEWTON_M * Math.pow(10, 1.5 * (Mw + 10.73));

        double mdacM0 = Double.NEGATIVE_INFINITY;
        double distance = -1;

        MdacCalculator mdc = new MdacCalculator(fiEntry.getSigma(),
                                                fiEntry.getM0ref(),
                                                fiEntry.getPsi(),
                                                fiEntry.getZeta(),
                                                fiEntry.getAlphas(),
                                                fiEntry.getBetas(),
                                                fiEntry.getRadPatP(),
                                                fiEntry.getRadPatS(),
                                                M0);
        mdc.initializePhaseSpecificVariables(distance, psEntry, fiEntry, M0);

        //FIXME: Use the calibrations phase for this!
        if (stress != null) {
            mdacM0 = mdc.calculateMomentRateSpectra(frequency, M0, stress, 0.0, phase);
        } else {
            mdacM0 = mdc.calculateMomentRateSpectra(frequency, M0, fiEntry.getSigma(), fiEntry.getPsi(), phase);
        }

        return Math.log10(mdacM0) + 7;
    }

    /**
     * @param psEntry
     *            Phase specific parameters for the MDAC2 model
     * @param fiEntry
     *            Independent parameters for the MDAC2 model
     * @param frequency
     *            Frequency to compute the MDAC2 source spectra for
     * @param Mw
     *            Magnitude of expected event
     * @param phase
     *            The phase to use for the MDAC calculation. Should be one of
     *            the phases in {@link PICK_TYPES}.
     * @return logAmp In Dyne-CM
     */
    public double calculateMdacAmplitudeForMw(MdacParametersPS psRows, MdacParametersFI mdacFiEntry, double refMw, double centerFreq, PICK_TYPES phase) {
        return calculateMdacAmplitudeForMw(psRows, mdacFiEntry, refMw, centerFreq, phase, null);
    }

    /**
     * Returns the Moment given the source Magnitude Mw
     *
     * @param Mw
     *            The event magnitude Mw
     * @return the Seismic Moment M0
     */
    public double getM0(double Mw) {
        return MdacCalculator.mwToM0(Mw);
    }

    public double getMwInDyne(double testMw) {
        return MdacCalculator.mwInDyne(testMw);
    }
}
