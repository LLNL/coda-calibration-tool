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

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

/**
 * <p>
 * <b>If possible you should call MdacCalculatorService instead of calling this
 * class directly. It will handle the setup and care and feeding needed to
 * ensure you get a correct (or any) answer.</b>
 * </p>
 *
 * <p>
 * Revised Moment and Distance Amplitude Corrections (MDAC2) Based on the MDAC2
 * report by Walter and Taylor (2002). Original program was mdacsac.f based on
 * paper by Taylor et al. (2002) PAGEOPH, but modified by Walter to use known
 * moments from coda spectra and fit or correct all spectra simultaneously.
 * </p>
 *
 * <p>
 * This version is based on the Java version written by E. Matzel, which is in
 * turn based on the 2003 version of MDAC2SAC.java
 * </p>
 */
public class MdacCalculator {
    public static final double LOG10_OF_E = Math.log10(Math.E);
    public static final double DYNE_CM_TO_NEWTON_M = 1e-7;
    public static final double MPA_TO_PA = 1e6;
    public static final int ANGULAR_CORNER_FREQ_IDX = 3;

    // Event specific variables
    double sigmaA; // Apparent stress - derived from Sigma and M0
    double[][] wcvels;

    // phase specific variables
    double F;
    double logS0;
    double wc;
    double radiationpattern;
    double velocityR;
    double velocityS5;
    private double zeta;
    private double alphaS;
    private double betaS;
    private double radpatP;
    private double radpatS;
    private double m0_ref;
    private double K;

    public MdacCalculator(double sigma, double M0ref, double Psi, double Zeta, double AlphaS, double BetaS, double RadpatP, double RadpatS, double M0) {
        this.zeta = Zeta;
        this.alphaS = AlphaS;
        this.betaS = BetaS;
        this.radpatP = RadpatP;
        this.radpatS = RadpatS;
        this.m0_ref = M0ref;
        this.K = calculateK(zeta, alphaS, betaS, radpatP, radpatS);
        sigmaA = getApparentStress(MPA_TO_PA * sigma, M0, M0ref, Psi);
        wcvels = getCornerFrequencies(Zeta, AlphaS, BetaS, RadpatP, RadpatS, sigmaA, M0);
    }

    /**
     * Calculate the Mdac Source Spectra (No Q, site or geometrical spreading
     * terms)
     *
     * @param freq
     *            the frequency(Hz)
     * @param M0
     *            the Moment
     * @return logAmp, M0/wwc, w*M0/wwc
     */
    public double[] calculateMdacSourceSpectra(double freq, double M0) {
        // angular frequency omega
        double w = (2.0 * Math.PI * freq);

        double wwc = 1.0 + Math.pow((w / wc), 2.0);
        // log10 (1 + (w/wc)^2)
        double logwwc = Math.log10(wwc);

        double logAmp = logS0 - logwwc;
        double data = M0 / wwc;
        double veld = data * w;

        return new double[] { logAmp, data, veld, wc };
    }

    /**
     * Moment rate spectra using MDAC for comparison with the Coda Spectra
     *
     * NOTE this version uses the already computed value of the angular corner
     * frequency, which is dependent on the moment - so they may not match
     *
     * @param frequency
     *            the desired frequency in Hz
     * @param m0
     *            the seismic Moment in (?)
     * @param sigma
     * @param psi
     * @param phase
     *            {@link PICK_TYPES} phase to calculate the spectra for
     * @return the Moment rate spectra at frequency f, M0(f)
     */
    public double calculateMomentRateSpectra(double frequency, double m0, double sigma, double psi, PICK_TYPES phase) {
        // Note the default is for shear velocity "Sn" or "Lg"
        double cornerFrequency = calculateAngularCornerFrequency(K, MPA_TO_PA * sigma, m0, m0_ref, psi);
        if (PICK_TYPES.PN.equals(phase) || PICK_TYPES.PG.equals(phase)) {
            cornerFrequency = zeta * cornerFrequency;
        }
        return calculateMomentRateSpectra(frequency, cornerFrequency, m0);
    }

    /**
     * Calculate the shear wave angular corner frequency (wcs in Walter and
     * Taylor, Moment Rate Spectra V2 2006)
     *
     * @param K
     *            a variable defined by Walter and Taylor including the P and S
     *            radiation patterns and velocities
     * @param Sigma
     *            - Walter and Taylor sigma (can be stress drop or apparent
     *            stress)
     * @param m0
     *            - the seismic Moment (in ?)
     * @param m0_ref
     *            - reference Moment that helps define the corner frequency
     * @param psi
     *            - exponential term (note for constant apparent stress -> psi =
     *            0 and the reference moment is not needed
     * @return the angular corner frequency (wcs)
     */
    private double calculateAngularCornerFrequency(double K, double Sigma, double m0, double m0_ref, double psi) {
        double m0psi = Math.pow(m0_ref, psi); // Moref ^ psi
        double C = Math.pow(((K * Sigma) / m0psi), (1.0 / 3.0));
        return C * Math.pow(m0, (psi - 1) / 3.0);
    }

    /**
     * Calculate variable K from Walter and Taylor. K is used to calculate
     * corner frequencies and Moment rate spectra
     *
     * @param zeta
     * @param alphaS
     * @param betaS
     * @param radpatP
     * @param radpatS
     *
     * @return K
     */
    public static double calculateK(double zeta, double alphaS, double betaS, double radpatP, double radpatS) {
        double z3 = Math.pow(zeta, 3);
        double a5 = Math.pow(alphaS, 5);
        double b5 = Math.pow(betaS, 5);
        double b2 = Math.pow(betaS, 2);

        double tmpp = (radpatP * radpatP * z3) / a5;
        double tmps = (radpatS * radpatS) / b5;

        return (16. * Math.PI) / (b2 * (tmpp + tmps));
    }

    /**
     * Moment rate spectra using MDAC for comparison with the Coda Spectra
     *
     * @param frequency
     *            the desired frequency in Hz
     * @param wcorner
     *            a user specified angular corner frequency
     * @param M0
     *            the seismic Moment in (?)
     * @return the Moment rate spectra at frequency f, M0(f)
     */
    private double calculateMomentRateSpectra(double frequency, double wcorner, double M0) {
        double w = 2 * Math.PI * frequency;
        double wwc = w / wcorner;
        double wwc2 = wwc * wwc;

        return M0 / (1 + wwc2);
    }

    /**
     * Sets the phase-specific parameters: radiation pattern, velocity terms and
     * corner frequency then calculates the moment corner frequency scaling
     * parameter (F) and the zero-frequency spectral level source term (logS0)
     * radiation pattern - radpatP or radpatS depending on phase type. velocityR
     * - the receiver region velocity, AlphaR or BetaR. velocityS5 - 5th power
     * of the source region velocity, AlphaS or BetaS from getCornerFrequencies.
     * wc - the corner frequency wcp or wcs (see getCornerFrequencies).
     */
    public void initializePhaseSpecificVariables(MdacParametersPS mdacPs, MdacParametersFI mdacFi, double M0) {
        String phase = mdacPs.getPhase();

        if (PICK_TYPES.PN.getPhase().equals(phase) || PICK_TYPES.PG.getPhase().equals(phase)) {
            radiationpattern = mdacFi.getRadPatP();
            wc = wcvels[0][0];
            velocityS5 = wcvels[0][1];
            velocityR = mdacFi.getAlphaR();
        } else if (PICK_TYPES.SN.getPhase().equals(phase) || PICK_TYPES.LG.getPhase().equals(phase)) {
            radiationpattern = mdacFi.getRadPatS();
            wc = wcvels[1][0];
            velocityS5 = wcvels[1][1];
            velocityR = mdacFi.getBetaR();
        } else {
            return;
        }
        F = getF(radiationpattern, mdacFi.getRhos(), mdacFi.getRhor(), velocityS5, velocityR);
        logS0 = getLogS0(F, M0);
    }

    /**
     * Returns the value of the source moment-corner frequency scaling parameter
     * (F) given the radiation pattern coefficient appropriate to the phase
     * desired along with the source and receiver densities (rhos, rhor) the 5th
     * power of the source region velocity (cs5) and the receiver region
     * velocity (cr)
     */
    private double getF(double radiationpattern, double rhos, double rhor, double cs5, double cr) {
        // from Aki and Richards (1980)
        // use this with getS0 --> S0 = F * M0
        return radiationpattern / (4.0 * Math.PI * Math.pow(rhos * rhor * cs5 * cr, 0.5));
    }

    /**
     * Returns the log10 of the zero-frequency spectral level source term S0
     * given the Moment (M0) and the source moment - corner frequency scaling
     * parameter (F) obtained from the getF() routine.
     */
    private double getLogS0(double F, double M0) {
        double S0 = F * M0;
        return Math.log10(S0);
    }

    /**
     * Returns the log10 of the Geometrical Spreading coefficient given the
     * source-receiver distance and the critical distance for the specific phase
     * note units of distance, distcrit are meters.
     */
    private double getLogGeometricalSpreading(double distance, double distcrit, double eta) {
        double Gr;
        if (distance < distcrit) {
            Gr = 1. / distance;
        } else {
            Gr = 1. / distcrit * Math.pow((distcrit / distance), eta);
        }
        return Math.log10(Gr);
    }

    /**
     * Returns the apparent stress given the reference stress (sigma) and
     * Moment(M0) at reference moment (M0ref) and with scaling exponent Psi
     */
    private double getApparentStress(double Sigma, double M0, double M0ref, double Psi) {
        double ratio = M0 / M0ref;
        return Sigma * Math.pow(ratio, Psi);
    }

    /**
     * Returns the log10 of the frequency independent attenuation term (Qfi)
     * given the s-r distance, attenuation at 1-Hz (Q0) and phase velocity (U0)
     * this is used to obtain a Q(f) term in the MDAC calculation
     */
    private double getLogQfi(double distance, double Q0, double U0) {
        // See equation A6 (Rodgers and Walter, 2002)
        // Q(f) = exp( -pi* f^(1-gamma) * distance / U0 *Q0)
        return (distance * Math.PI * LOG10_OF_E) / (Q0 * U0);
    }

    /**
     * Calculates the corner frequencies for P and S wave types (wcp, wcs)
     * returns 4-element array containing the result {{wcp, alphas^5},{wcs,
     * betas^5}}
     */
    private double[][] getCornerFrequencies(double zeta, double alphas, double betas, double radpatp, double radpats, double sigmaA, double M0) {
        double a5 = Math.pow(alphas, 5);
        double b5 = Math.pow(betas, 5);
        double wcs = Math.pow(((K * sigmaA) / M0), (1.0 / 3.0));
        double wcp = zeta * wcs;

        return new double[][] { { wcp, a5 }, { wcs, b5 } };
    }

    public double calculateEnergy(double M0, double appStressMpa, final MdacParametersPS mdacPs, final MdacParametersFI mdacFi) {
        return (M0 * appStressMpa * MPA_TO_PA) / (mdacFi.getRhos() * Math.pow(mdacFi.getBetas(), 2.0));
    }

    public static double mwToM0(double Mw) {
        return Math.pow(10.0, mwToLogM0(Mw));
    }

    public static double mwToLogM0(double Mw) {
        return 1.5 * Mw + 9.10;
    }

    public static double logM0ToMw(double logM0) {
        return (logM0 - 9.1) / 1.5;
    }

    public double getK() {
        return K;
    }

    public double apparentStressFromMwFc(Double mw, Double fc) {
        double M0 = mwToM0(mw);
        return apparentStressFromM0Fc(M0, fc);
    }

    public double apparentStressFromM0Fc(Double M0, Double fc) {
        double wfc3 = Math.pow(Math.PI * 2.0 * fc, 3.0);
        double appStress = ((wfc3 * M0) / K) / MPA_TO_PA;
        return appStress;
    }

    public double cornerFreqFromApparentStressM0(Double M0, Double appStress) {
        double sigma = appStress * MPA_TO_PA;
        double corner = Math.pow((sigma * K) / M0, 1.0 / 3.0) / (Math.PI * 2.0);
        return corner;
    }
}
