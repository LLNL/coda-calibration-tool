/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.Sigma;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.util.FastMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;

/**
 * Calculate the coda decay parameters (Mayeda, 2003)
 */
public class CalibrationCurveFitter {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    // 6.0
    protected static final double MAX_V_P1 = 600;
    // .5
    protected static final double MIN_V_P1 = 50.0;
    protected static final double V0_REG = 100;
    protected static final double MAX_V_P2 = 5000;
    protected static final double MIN_V_P2 = 1;
    protected static final double MAX_V_P3 = 5000;
    protected static final double MIN_V_P3 = 1;

    // -0.001
    protected static final double MAX_B_P1 = -10;
    // -0.5
    protected static final double MIN_B_P1 = -500;
    protected static final double B0_REG = 10000;

    protected static final double MAX_B_P2 = 5;
    protected static final double MIN_B_P2 = 0.0001;
    protected static final double MAX_B_P3 = 1500;
    protected static final double MIN_B_P3 = 0.0001;

    // 3
    protected static final double MAX_G_P1 = 300;
    // -0.5
    protected static final double MIN_G_P1 = 5;
    protected static final double G0_REG = 100;

    protected static final double MAX_G_P2 = 1000;
    protected static final double MIN_G_P2 = 1;
    protected static final double G1_REG = -1;

    protected static final double MAX_G_P3 = 1000;
    protected static final double MIN_G_P3 = 1;

    protected static final double YVV_MIN = 0.5;
    protected static final double YVV_MAX = 6.01;
    protected static final double V_DIST_MAX = 1600;

    protected static final double YBB_MIN = -3.0E-2;
    protected static final double YBB_MAX = 0.0005;
    protected static final double B_DIST_MAX = 1550;

    protected static final double YGG_MIN = 0.1;
    protected static final double YGG_MAX = 100.0;
    protected static final double G_DIST_MAX = 600;

    private static final int ITER_COUNT = 10;
    private static final int DATA_POINT_CUTOFF = 100;

    public double[] fitCodaStraightLine(final float[] segment) {

        Double bestFit = null;
        double[] curve = new double[4];
        for (int i = 1; i <= 30; i++) {
            final double gamma = ((double) i) * 0.1;

            SimpleRegression regression = new SimpleRegression();

            for (int j = (int) (segment.length * .3); j < segment.length; j++) {
                final double t = j + 1.0;
                double amp = segment[j] + (Math.log10(t) * gamma);
                regression.addData(t, amp);
            }

            double sum = 0.0;
            double intercept = regression.getIntercept();
            double beta = regression.getSlope();

            for (int j = (int) (segment.length * .3); j < segment.length; j++) {
                double t = j + 1.0;
                double actual = segment[j] + (Math.log10(t) * gamma);
                double predicted = intercept + ((1 / Math.log10(Math.E)) * beta * t);
                sum = sum + FastMath.sqrt((predicted - actual) * (predicted - actual));
            }

            double error = 100 * (sum / (segment.length));

            if (bestFit == null || sum < bestFit) {
                bestFit = sum;
                curve[0] = gamma;
                curve[1] = (1 / Math.log10(Math.E)) * beta;
                curve[2] = intercept;
                curve[3] = error;
            }
        }
        return curve;
    }

    public double[] gridSearchCodaVApacheCMAES(List<Entry<Double, Double>> velocityDistancePairs) {
        return gridSearchCodaVApacheCMAES(velocityDistancePairs, YVV_MIN, YVV_MAX, 0, V_DIST_MAX);
    }

    public double[] gridSearchCodaVApacheCMAES(final List<Entry<Double, Double>> velocityDistancePairs, final double yvvMin, final double yvvMax, final double distMin, final double distMax) {
        double maxP1 = MAX_V_P1;
        double minP1 = MIN_V_P1;
        double maxP2 = MAX_V_P2;
        double minP2 = MIN_V_P2;
        double maxP3 = MAX_V_P3;
        double minP3 = MIN_V_P3;

        MultivariateFunction prediction = point -> {
            double v0 = point[0] / V0_REG;
            double v1 = point[1];
            double v2 = point[2];

            double vDistMin = v0 - v1 / (distMin + v2);
            double vDistMax = v0 - v1 / (distMax + v2);
            if (vDistMin < yvvMin || vDistMax > yvvMax) {
                return Double.MAX_VALUE;
            }

            double sum = 0.0;
            for (Entry<Double, Double> velDistPair : velocityDistancePairs) {
                final Double velocity = velDistPair.getKey();
                final Double distance = velDistPair.getValue();
                double predictedVel = v0 - (v1 / (distance + v2));

                sum = sum + Math.sqrt((velocity - predictedVel) * (velocity - predictedVel));
            }
            return sum;
        };

        PointValuePair bestResult = optimizeCMAES(prediction,
                                                  new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                                                          ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                                                  new CMAESOptimizer.Sigma(new double[] { 1, 75, 100 }),
                                                  new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
        for (int i = 1; i < ITER_COUNT; i++) {
            PointValuePair result = optimizeCMAES(prediction,
                                                  new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                                                          ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                                                  new CMAESOptimizer.Sigma(new double[] { 1, 75, 100 }),
                                                  new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
            if (result.getValue() < bestResult.getValue()) {
                bestResult = result;
            }
        }

        double[] curve = new double[4];
        curve[0] = bestResult.getPoint()[0] / V0_REG;
        curve[1] = bestResult.getPoint()[1];
        curve[2] = bestResult.getPoint()[2];
        curve[3] = bestResult.getValue();

        if (curve[3] == Double.MAX_VALUE) {
            // Couldn't fit, don't trust us.
            curve[3] = -1.0;
        }
        return curve;
    }

    public double[] gridSearchCodaBApacheCMAES(List<Entry<Double, Double>> betaDistancePairs) {
        return gridSearchCodaBApacheCMAES(betaDistancePairs, YBB_MIN, YBB_MAX, 0, B_DIST_MAX);
    }

    public double[] gridSearchCodaBApacheCMAES(final List<Entry<Double, Double>> betaDistancePairs, final double ybbMin, final double ybbMax, final double distMin, final double distMax) {
        double maxP1 = MAX_B_P1;
        double minP1 = MIN_B_P1;
        double maxP2 = MAX_B_P2;
        double minP2 = MIN_B_P2;
        double maxP3 = MAX_B_P3;
        double minP3 = MIN_B_P3;

        MultivariateFunction prediction = point -> {
            double b0 = point[0] / B0_REG;
            double b1 = point[1];
            double b2 = point[2];

            double ybbDistMin = b0 - b1 / (distMin + b2);
            double ybbDistMax = b0 - b1 / (distMax + b2);
            if (ybbDistMin < ybbMin || ybbDistMax > ybbMax) {
                return Double.MAX_VALUE;
            }

            double sum = 0.0;
            for (Entry<Double, Double> bDistPair : betaDistancePairs) {
                final Double beta = bDistPair.getKey();
                final Double distance = bDistPair.getValue();
                double predictedBeta = b0 - (b1 / (distance + b2));

                sum = sum + FastMath.sqrt((beta - predictedBeta) * (beta - predictedBeta));
            }
            return sum;
        };

        PointValuePair bestResult = optimizeCMAES(prediction,
                                                  new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                                                          ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                                                  new CMAESOptimizer.Sigma(new double[] { .05, 0.5, 750 }),
                                                  50,
                                                  new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
        for (int i = 1; i < ITER_COUNT; i++) {
            PointValuePair result = optimizeCMAES(prediction,
                                                  new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                                                          ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                                                  new CMAESOptimizer.Sigma(new double[] { .05, 0.5, 750 }),
                                                  50,
                                                  new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
            if (result.getValue() < bestResult.getValue()) {
                bestResult = result;
            }
        }

        double[] curve = new double[4];
        curve[0] = bestResult.getPoint()[0] / B0_REG;
        curve[1] = bestResult.getPoint()[1];
        curve[2] = bestResult.getPoint()[2];
        curve[3] = bestResult.getValue();

        if (curve[0] == 0.0) {
            curve[0] = 0.0001;
        }
        if (curve[1] == 0.0) {
            curve[1] = 0.0001;
        }
        if (curve[2] == 0.0) {
            curve[2] = 0.0001;
        }
        if (curve[3] == Double.MAX_VALUE) {
            // Couldn't fit, don't trust us.
            curve[3] = -1.0;
        }
        return curve;
    }

    public double[] gridSearchCodaGApacheCMAES(List<Entry<Double, Double>> gammaDistancePairs) {
        return gridSearchCodaGApacheCMAES(gammaDistancePairs, YGG_MIN, YGG_MAX, 0, G_DIST_MAX);
    }

    public double[] gridSearchCodaGApacheCMAES(final List<Entry<Double, Double>> gammaDistancePairs, final double gMin, final double gMax, final double distMin, final double distMax) {
        double maxP1 = MAX_G_P1;
        double minP1 = MIN_G_P1;
        double maxP2 = MAX_G_P2;
        double minP2 = MIN_G_P2;
        double maxP3 = MAX_G_P3;
        double minP3 = MIN_G_P3;

        MultivariateFunction prediction = point -> {
            double g0 = point[0] / G0_REG;
            double g1 = point[1] * G1_REG;
            double g2 = point[2];

            double gDistMin = g0 - g1 / (distMin + g2);
            double gDistMinNext = g0 - g1 / ((distMin + 1.0) + g2);
            double gDistMax = g0 - g1 / (distMax + g2);
            if (gDistMin < gMin || gDistMax > gMax || gDistMin < gDistMinNext) {
                return Double.MAX_VALUE;
            }

            double sum = 0.0;
            for (Entry<Double, Double> gDistPair : gammaDistancePairs) {
                final Double gamma = gDistPair.getKey();
                final Double distance = gDistPair.getValue();
                double predictedGamma = g0 - (g1 / (distance + g2));

                sum = sum + FastMath.sqrt((gamma - predictedGamma) * (gamma - predictedGamma));
            }
            return sum;
        };

        ConvergenceChecker<PointValuePair> convergenceChecker = new SimplePointChecker<>(1.0, 1.0, 100000);

        PointValuePair bestResult = optimizeCMAES(prediction,
                                                  new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), minP2, minP3 }),
                                                  new CMAESOptimizer.Sigma(new double[] { 1, 250, 500 }),
                                                  convergenceChecker,
                                                  50,
                                                  new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
        for (int i = 1; i < ITER_COUNT; i++) {

            PointValuePair result = optimizeCMAES(prediction,
                                                  new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                                                          ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                                                  new CMAESOptimizer.Sigma(new double[] { 1, 250, 500 }),
                                                  convergenceChecker,
                                                  50,
                                                  new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
            if (result.getValue() < bestResult.getValue()) {
                bestResult = result;
            }
        }

        double[] curve = new double[4];
        curve[0] = bestResult.getPoint()[0] / G0_REG;
        curve[1] = bestResult.getPoint()[1] * G1_REG;
        curve[2] = bestResult.getPoint()[2];
        curve[3] = bestResult.getValue();
        if (curve[3] == Double.MAX_VALUE) {
            // Couldn't fit, don't trust us.
            curve[3] = -1.0;
        }
        return curve;
    }

    private PointValuePair optimizeCMAES(MultivariateFunction prediction, InitialGuess initialGuess, Sigma sigma, int popSize, SimpleBounds bounds) {
        return optimizeCMAES(prediction, initialGuess, sigma, null, popSize, bounds);
    }

    private PointValuePair optimizeCMAES(MultivariateFunction prediction, InitialGuess initialGuess, Sigma sigma, SimpleBounds bounds) {
        return optimizeCMAES(prediction, initialGuess, sigma, null, 100, bounds);
    }

    private PointValuePair optimizeCMAES(MultivariateFunction prediction, InitialGuess initialGuess, CMAESOptimizer.Sigma stepSize, ConvergenceChecker<PointValuePair> convergenceChecker,
            Integer popSize, SimpleBounds bounds) {
        CMAESOptimizer optimizer = new CMAESOptimizer(1000000, 0, true, 0, 10, new MersenneTwister(), true, convergenceChecker);
        return optimizer.optimize(new MaxEval(1000000), new ObjectiveFunction(prediction), GoalType.MINIMIZE, bounds, initialGuess, stepSize, new CMAESOptimizer.PopulationSize(popSize));
    }

    public double[] gridSearchCodaV(List<Entry<Double, Double>> velocityDistancePairs) {
        return gridSearchCodaV(velocityDistancePairs, YVV_MIN, YVV_MAX, 0, V_DIST_MAX);
    }

    public double[] gridSearchCodaV(final List<Entry<Double, Double>> velocityDistancePairs, final double yvvMin, final double yvvMax, final double distMin, final double distMax) {

        final Double[] baseResult = new Double[] { 0.0, 0.0, 0.0, 9E29 };
        return ArrayUtils.toPrimitive(IntStream.rangeClosed(0, 45).parallel().mapToObj(iv0 -> {
            double v0 = 2.5 + (iv0) * 0.05;
            return IntStream.rangeClosed(0, 200).parallel().mapToObj(iv1 -> {
                double v1 = 0.0 + (iv1) * 2.0;

                return IntStream.rangeClosed(0, 200).parallel().mapToObj(iv2 -> {
                    double v2 = 1.0 + (iv2) * 1.0;

                    // avoid "unphysical" situations
                    double yvvDistMin = v0 - v1 / (distMin + v2);
                    double yvvDistMax = v0 - v1 / (distMax + v2);
                    if (yvvDistMin < yvvMin || yvvDistMax > yvvMax) {
                        return baseResult;
                    }

                    double sum = 0.0;
                    for (Entry<Double, Double> velDistPair : velocityDistancePairs) {
                        final Double velocity = velDistPair.getKey();
                        final Double distance = velDistPair.getValue();
                        double predictedVel = v0 - (v1 / (distance + v2));
                        sum = sum + Math.sqrt((velocity - predictedVel) * (velocity - predictedVel));
                    }
                    return new Double[] { v0, v1, v2, sum };
                }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult);
            }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult);
        }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult));
    }

    public double[] gridSearchCodaB(List<Entry<Double, Double>> betaDistancePairs) {
        return gridSearchCodaB(betaDistancePairs, YBB_MIN, YBB_MAX, 0, B_DIST_MAX);
    }

    public double[] gridSearchCodaB(final List<Entry<Double, Double>> betaDistancePairs, final double ybbMin, final double ybbMax, final double distMin, final double distMax) {
        final Double[] baseResult = new Double[] { 0.0, 0.0, 0.0, 9E29 };
        return ArrayUtils.toPrimitive(IntStream.rangeClosed(0, 200).parallel().mapToObj(ib0 -> {
            double b0 = 0.0 - ib0 * 0.0005;
            return IntStream.rangeClosed(0, 200).parallel().mapToObj(ib1 -> {
                double b1 = 0.0 + ib1 * 0.02;

                return IntStream.rangeClosed(0, 500).parallel().mapToObj(ib2 -> {
                    double b2 = 0.0001 + ib2 * 1.0;

                    // avoid "unphysical" situations
                    double ybbDistMin = b0 - b1 / (distMin + b2);
                    double ybbDistMax = b0 - b1 / (distMax + b2);
                    if (ybbDistMin < ybbMin || ybbDistMax > ybbMax) {
                        return baseResult;
                    }

                    double sum = 0.0;
                    for (Entry<Double, Double> fistPair : betaDistancePairs) {
                        final Double beta = fistPair.getKey();
                        final Double distance = fistPair.getValue();
                        double bb = b0 - (b1 / (distance + b2));

                        sum = sum + Math.sqrt((beta - bb) * (beta - bb));
                    }
                    return new Double[] { b0, b1, b2, sum };
                }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult);
            }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult);
        }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult));
    }

    public double[] gridSearchCodaG(List<Entry<Double, Double>> gammaDistancePairs) {
        return gridSearchCodaG(gammaDistancePairs, YGG_MIN, YGG_MAX, 0, G_DIST_MAX);
    }

    public double[] gridSearchCodaG(final List<Entry<Double, Double>> gammaDistancePairs, final double yggMin, final double yggMax, final double distMin, final double distMax) {
        final Double[] baseResult = new Double[] { 0.0, 0.0, 0.0, 9E29 };
        return ArrayUtils.toPrimitive(IntStream.rangeClosed(0, 20).parallel().mapToObj(ig0 -> {
            double g0 = 2.0 - (ig0) * 0.1;

            return IntStream.rangeClosed(0, 100).parallel().mapToObj(ig1 -> {

                double g1 = -((double) ig1) * 1.0;
                return IntStream.rangeClosed(0, 100).parallel().mapToObj(ig2 -> {
                    double g2 = 1.0 + (ig2) * 1.0;

                    // avoid "unphysical" situations

                    double yggDistMin = g0 - g1 / (distMin + g2);
                    double yggDistMinNext = g0 - g1 / ((distMin + 1.0) + g2);
                    double yggDistMax = g0 - g1 / (distMax + g2);
                    if (yggDistMin < yggMin || yggDistMax > yggMax || yggDistMin < yggDistMinNext) {
                        return baseResult;
                    }

                    double sum = 0.0;
                    for (Entry<Double, Double> gammaDistPair : gammaDistancePairs) {
                        final Double gamma = gammaDistPair.getKey();
                        final Double distance = gammaDistPair.getValue();
                        double predictedGamma = g0 - (g1 / (distance + g2));

                        sum = sum + Math.sqrt((gamma - predictedGamma) * (gamma - predictedGamma));
                    }

                    return new Double[] { g0, g1, g2, sum };
                }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult);
            }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult);
        }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElse(baseResult));
    }

    /**
     * Attempts to perform a non-linear optimization over a set of
     * [Velocity,Distance] values derived from velocityDistancePairsFreqMap.
     * This method will skip over input Frequency Bands that have no matching
     * entry in freqBandMap as the output Velocity0-2 values are updated in
     * place on the SharedFrequencyBandParameters object.
     * 
     * @param velocityDistancePairsFreqMap
     * @param freqBandMap
     * @return Map<FrequencyBand, SharedFrequencyBandParameters> with updated
     *         Velocity 0-2 values given the best available model fit for the
     *         input velocityDistancePairsFreqMap
     */
    public Map<FrequencyBand, SharedFrequencyBandParameters> fitAllVelocity(Map<FrequencyBand, List<PeakVelocityMeasurement>> velocityDistancePairsFreqMap,
            Map<FrequencyBand, SharedFrequencyBandParameters> freqBandMap) {
        for (Entry<FrequencyBand, List<PeakVelocityMeasurement>> velDistPairs : velocityDistancePairsFreqMap.entrySet()) {
            if (freqBandMap.get(velDistPairs.getKey()) != null) {
                double[] curve = gridSearch(velDistPairs.getValue().stream().map(v -> new AbstractMap.SimpleEntry<Double, Double>(v.getVelocity(), v.getDistance())).collect(Collectors.toList()),
                                            DATA_POINT_CUTOFF,
                                            new ApacheGridSearchV(),
                                            new BasicGridSearchV());

                freqBandMap.put(velDistPairs.getKey(), freqBandMap.get(velDistPairs.getKey()).setVelocity0(curve[0]).setVelocity1(curve[1]).setVelocity2(curve[2]));
            }
        }

        return freqBandMap;
    }

    /**
     * Attempts to perform a non-linear optimization over a set of
     * [Beta,Distance] values derived from betaDistancePairsFreqMap. This method
     * will skip over input Frequency Bands that have no matching entry in
     * freqBandMap as the output Beta0-2 values are updated in place on the
     * SharedFrequencyBandParameters object.
     * 
     * @param betaDistancePairsFreqMap
     * @param freqBandMap
     * @return Map<FrequencyBand, SharedFrequencyBandParameters> with updated
     *         Beta 0-2 values given the best available model fit for the input
     *         betaDistancePairsFreqMap
     */
    public Map<FrequencyBand, SharedFrequencyBandParameters> fitAllBeta(Map<FrequencyBand, List<ShapeMeasurement>> betaDistancePairsFreqMap,
            Map<FrequencyBand, SharedFrequencyBandParameters> freqBandMap) {

        for (Entry<FrequencyBand, List<ShapeMeasurement>> betaDistPairs : betaDistancePairsFreqMap.entrySet()) {
            if (freqBandMap.get(betaDistPairs.getKey()) != null) {
                double[] curve = gridSearch(betaDistPairs.getValue().stream().map(v -> new AbstractMap.SimpleEntry<Double, Double>(v.getMeasuredBeta(), v.getDistance())).collect(Collectors.toList()),
                                            DATA_POINT_CUTOFF,
                                            new ApacheGridSearchB(),
                                            new BasicGridSearchB());
                // Artificially lower the intercept value, b0 to 95%
                // to account for possible noise contamination causing
                // too shallow decay
                freqBandMap.put(betaDistPairs.getKey(), freqBandMap.get(betaDistPairs.getKey()).setBeta0(curve[0] * 1.05).setBeta1(curve[1]).setBeta2(curve[2]));
            }
        }

        return freqBandMap;
    }

    /**
     * Attempts to perform a non-linear optimization over a set of
     * [Gamma,Distance] values derived from gammaDistancePairsFreqMap. This
     * method will skip over input Frequency Bands that have no matching entry
     * in freqBandMap as the output Gamma0-2 values are updated in place on the
     * SharedFrequencyBandParameters object.
     * 
     * @param gammaDistancePairsFreqMap
     * @param freqBandMap
     * @return Map<FrequencyBand, SharedFrequencyBandParameters> with updated
     *         Gamma 0-2 values given the best available model fit for the input
     *         gammaDistancePairsFreqMap
     */
    public Map<FrequencyBand, SharedFrequencyBandParameters> fitAllGamma(Map<FrequencyBand, List<ShapeMeasurement>> gammaDistancePairsFreqMap,
            Map<FrequencyBand, SharedFrequencyBandParameters> freqBandMap) {

        for (Entry<FrequencyBand, List<ShapeMeasurement>> gammaDistPairs : gammaDistancePairsFreqMap.entrySet()) {
            if (freqBandMap.get(gammaDistPairs.getKey()) != null) {
                double[] curve = gridSearch(gammaDistPairs.getValue()
                                                          .stream()
                                                          .map(v -> new AbstractMap.SimpleEntry<Double, Double>(v.getMeasuredGamma(), v.getDistance()))
                                                          .collect(Collectors.toList()),
                                            DATA_POINT_CUTOFF,
                                            new ApacheGridSearchG(),
                                            new BasicGridSearchG());
                freqBandMap.put(gammaDistPairs.getKey(), freqBandMap.get(gammaDistPairs.getKey()).setGamma0(curve[0]).setGamma1(curve[1]).setGamma2(curve[2]));
            }
        }

        return freqBandMap;
    }

    private double[] gridSearch(List<Entry<Double, Double>> value, final int dataCutoff, GridFitter main, GridFitter fallback) {
        double[] fit;

        if (value.size() < dataCutoff) {
            fit = fallback.fitGrid(value);
        } else {
            try {
                fit = main.fitGrid(value);
                // TODO: Evaluation metrics to decide if its a 'good' fit.
                if (fit[3] == -1) {
                    log.trace("Failed to fit using main method, using fallback.");
                    fit = fallback.fitGrid(value);
                }
            } catch (MaxCountExceededException e) {
                log.trace("Failed to fit using main method, using fallback. {}", e.getMessage(), e);
                fit = fallback.fitGrid(value);
            }
        }

        return fit;
    }

    public interface GridFitter {
        public double[] fitGrid(List<Entry<Double, Double>> value);
    };

    private class ApacheGridSearchV implements GridFitter {
        @Override
        public double[] fitGrid(List<Entry<Double, Double>> value) {
            return gridSearchCodaVApacheCMAES(value);
        }
    }

    private class BasicGridSearchV implements GridFitter {
        @Override
        public double[] fitGrid(List<Entry<Double, Double>> value) {
            return gridSearchCodaV(value);
        }
    }

    private class ApacheGridSearchB implements GridFitter {
        @Override
        public double[] fitGrid(List<Entry<Double, Double>> value) {
            return gridSearchCodaBApacheCMAES(value);
        }
    }

    private class BasicGridSearchB implements GridFitter {
        @Override
        public double[] fitGrid(List<Entry<Double, Double>> value) {
            return gridSearchCodaB(value);
        }
    }

    private class ApacheGridSearchG implements GridFitter {
        @Override
        public double[] fitGrid(List<Entry<Double, Double>> value) {
            return gridSearchCodaGApacheCMAES(value);
        }
    }

    private class BasicGridSearchG implements GridFitter {
        @Override
        public double[] fitGrid(List<Entry<Double, Double>> value) {
            return gridSearchCodaG(value);
        }
    }
}
