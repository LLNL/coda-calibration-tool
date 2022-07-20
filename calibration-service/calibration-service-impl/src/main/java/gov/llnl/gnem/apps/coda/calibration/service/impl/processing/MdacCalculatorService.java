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

import java.util.function.DoubleFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.UnaryOperator;

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
     * @param Mw
     *            Magnitude of expected event
     * @return Function that computes logAmp, M0/wwc, w*M0/wwc given a frequency
     */
    public DoubleFunction<double[]> getCalculateMdacSourceSpectraFunction(final MdacParametersPS psEntry, final MdacParametersFI fiEntry, final double Mw) {
        final Double M0 = getM0(Mw);
        final MdacCalculator mdc = new MdacCalculator(fiEntry.getSigma(),
                                                      fiEntry.getM0ref(),
                                                      fiEntry.getPsi(),
                                                      fiEntry.getZeta(),
                                                      fiEntry.getAlphas(),
                                                      fiEntry.getBetas(),
                                                      fiEntry.getRadPatP(),
                                                      fiEntry.getRadPatS(),
                                                      M0);
        mdc.initializePhaseSpecificVariables(psEntry, fiEntry, M0);
        return frequency -> mdc.calculateMdacSourceSpectra(frequency, M0);
    }

    /**
     * @param psEntry
     *            Phase specific parameters for the MDAC2 model
     * @param fiEntry
     *            Independent parameters for the MDAC2 model
     * @param Mw
     *            Magnitude of expected event
     * @param phase
     *            The phase to use for the MDAC calculation. Should be one of
     *            the phases in {@link PICK_TYPES}.
     * @param sigma
     *            Apparent stress in MPa to use. May be null to use the MDAC
     *            parameters instead. Otherwise Psi=0 and Sigma=stress
     * @return Function that computes logAmp In Dyne-CM given a frequency
     */
    public DoubleUnaryOperator getCalculateMdacAmplitudeForMwFunction(final MdacParametersPS psEntry, final MdacParametersFI fiEntry, final double Mw, final PICK_TYPES phase, final Double sigma) {
        // M0 in N-m units
        final double M0 = MdacCalculator.DYNE_CM_TO_NEWTON_M * Math.pow(10, 1.5 * (Mw + 10.73));
        UnaryOperator<Double> mdacFunction;

        final MdacCalculator mdc = new MdacCalculator(fiEntry.getSigma(),
                                                      fiEntry.getM0ref(),
                                                      fiEntry.getPsi(),
                                                      fiEntry.getZeta(),
                                                      fiEntry.getAlphas(),
                                                      fiEntry.getBetas(),
                                                      fiEntry.getRadPatP(),
                                                      fiEntry.getRadPatS(),
                                                      M0);
        mdc.initializePhaseSpecificVariables(psEntry, fiEntry, M0);

        if (sigma != null) {
            mdacFunction = frequency -> mdc.calculateMomentRateSpectra(frequency, M0, sigma, 0.0, phase);
        } else {
            mdacFunction = frequency -> mdc.calculateMomentRateSpectra(frequency, M0, fiEntry.getSigma(), fiEntry.getPsi(), phase);
        }

        return frequency -> Math.log10(mdacFunction.apply(frequency)) + 7;
    }

    /**
     * @param psRows
     *            Phase specific parameters for the MDAC2 model
     * @param mdacFiEntry
     *            Independent parameters for the MDAC2 model
     * @param refMw
     *            Magnitude of expected event
     * @param phase
     *            The phase to use for the MDAC calculation. Should be one of
     *            the phases in {@link PICK_TYPES}.
     * @return Function that computes logAmp In Dyne-CM given a frequency
     */
    public DoubleUnaryOperator getCalculateMdacAmplitudeForMwFunction(final MdacParametersPS psRows, final MdacParametersFI mdacFiEntry, final double refMw, final PICK_TYPES phase) {
        return getCalculateMdacAmplitudeForMwFunction(psRows, mdacFiEntry, refMw, phase, null);
    }

    /**
     * Returns the Moment given the source Magnitude Mw
     *
     * @param Mw
     *            The event magnitude Mw
     * @return the Seismic Moment M0
     */
    public double getM0(final double Mw) {
        return MdacCalculator.mwToM0(Mw);
    }

    public double getCornerFrequency(final DoubleFunction<double[]> mdacFunc) {
        return mdacFunc.apply(1.0)[MdacCalculator.ANGULAR_CORNER_FREQ_IDX] / (Math.PI * 2.0);
    }

    public double getCornerFrequency(final MdacParametersPS mdacPs, final MdacParametersFI stress, final double mw) {
        return getCornerFrequency(getCalculateMdacSourceSpectraFunction(mdacPs, stress, mw));
    }

    public double getEnergy(double Mw, double appStressMpa, final MdacParametersPS psEntry, final MdacParametersFI fiEntry) {
        // M0 in N-m units
        final double M0 = MdacCalculator.mwToM0(Mw);
        final MdacCalculator mdc = new MdacCalculator(fiEntry.getSigma(),
                                                      fiEntry.getM0ref(),
                                                      fiEntry.getPsi(),
                                                      fiEntry.getZeta(),
                                                      fiEntry.getAlphas(),
                                                      fiEntry.getBetas(),
                                                      fiEntry.getRadPatP(),
                                                      fiEntry.getRadPatS(),
                                                      M0);
        mdc.initializePhaseSpecificVariables(psEntry, fiEntry, M0);

        return mdc.calculateEnergy(M0, appStressMpa, psEntry, fiEntry);
    }
}
