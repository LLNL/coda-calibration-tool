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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.model.domain.EnvelopeFit;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;

/**
 * Calculate the coda decay parameters (Mayeda, 2003)
 */
public class CalibrationCurveFitter {

    private static final Logger log = LoggerFactory.getLogger(CalibrationCurveFitter.class);

    // 6.0
    protected static final double MAX_V_P1 = 600;
    // .5
    protected static final double MIN_V_P1 = 50.0;
    protected static final double V0_REG = 100;
    protected static final double MAX_V_P2 = 5000;
    protected static final double MIN_V_P2 = 1;
    protected static final double MAX_V_P3 = 5000;
    protected static final double MIN_V_P3 = 1;

    // .1
    protected static final double MAX_B_P1 = 1000;
    // -0.05
    protected static final double MIN_B_P1 = -500;
    protected static final double B0_REG = 10000;

    protected static final double MAX_B_P2 = 20;
    protected static final double MIN_B_P2 = 0.1;
    protected static final double MAX_B_P3 = 1500;
    protected static final double MIN_B_P3 = 0.0001;

    // 1
    protected static final double MAX_G_P1 = 100;
    // -0.1
    protected static final double MIN_G_P1 = 0;
    protected static final double G0_REG = 100;

    protected static final double MAX_G_P2 = 101;
    protected static final double MIN_G_P2 = 0;
    protected static final double G1_REG = -1;

    protected static final double MAX_G_P3 = 101;
    protected static final double MIN_G_P3 = 1;

    protected static final double YVV_MIN = 0.5;
    protected static final double YVV_MAX = 6.01;
    protected static final double V_DIST_MAX = 1600;

    protected static final double YBB_MIN = -12.0E-2;
    protected static final double YBB_MAX = 0.0005;
    protected static final double B_DIST_MAX = 1550;

    protected static final double YGG_MIN = 0.01;
    protected static final double YGG_MAX = 100.0;
    protected static final double G_DIST_MAX = 600;

    private static final int ITER_COUNT = 10;

    public EnvelopeFit fitCodaCMAES(final float[] segment) {

        double minInt = 0.01;
        double maxInt = 10.0;
        double minGamma = 0.01;
        double maxGamma = 1.00;
        double minBeta = -1.0;
        double maxBeta = -0.001;

        EnvelopeFit fit = new EnvelopeFit();

        SimpleRegression regression = new SimpleRegression();
        for (int j = 0; j < segment.length; j++) {
            regression.addData(j + 1, segment[j]);
        }
        double startIntercept = regression.getIntercept();
        double startBeta = regression.getSlope();

        MultivariateFunction prediction = point -> {
            double intercept = point[0];
            double gamma = point[1];
            double beta = point[2];
            double sum = 0.0;
            for (int j = 0; j < segment.length; j++) {
                double t = j + 1.0;
                double actual = segment[j];
                double predicted = intercept - (gamma * Math.log10(t)) + (beta * t);
                sum = lossFunction(sum, predicted, actual);
            }
            return sum;
        };

        ConvergenceChecker<PointValuePair> convergenceChecker = new SimplePointChecker<>(0.0005, -1.0, 100000);

        if (Double.isNaN(startIntercept)) {
            startIntercept = ThreadLocalRandom.current().nextDouble(minInt, maxInt);
            startBeta = minBeta;
        } else if (startBeta > maxBeta || startBeta < minBeta) {
            startBeta = minBeta;
        }

        PointValuePair bestResult = optimizeCMAES(
                prediction,
                    new InitialGuess(new double[] { startIntercept, minGamma, startBeta }),
                    new CMAESOptimizer.Sigma(new double[] { 0.5, 0.05, 0.05 }),
                    convergenceChecker,
                    50,
                    new SimpleBounds(new double[] { -Double.MAX_VALUE, minGamma, minBeta }, new double[] { Double.MAX_VALUE, maxGamma, maxBeta }));

        double[] curve = bestResult.getKey();
        fit.setIntercept(curve[0]);
        fit.setGamma(curve[1]);
        fit.setBeta(curve[2]);
        fit.setError(bestResult.getValue());

        return fit;
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
                sum = lossFunction(sum, velocity, predictedVel);
            }
            return sum;
        };

        PointValuePair bestResult = optimizeCMAES(
                prediction,
                    new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                            ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                    new CMAESOptimizer.Sigma(new double[] { 1, 75, 100 }),
                    new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
        for (int i = 1; i < ITER_COUNT; i++) {
            PointValuePair result = optimizeCMAES(
                    prediction,
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
                sum = lossFunction(sum, beta, predictedBeta);
            }
            return sum;
        };

        PointValuePair bestResult = optimizeCMAES(
                prediction,
                    new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                            ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                    new CMAESOptimizer.Sigma(new double[] { .05, 0.5, 750 }),
                    50,
                    new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
        for (int i = 1; i < ITER_COUNT; i++) {
            PointValuePair result = optimizeCMAES(
                    prediction,
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

    private double lossFunction(double sum, final Double X, double Y) {
        return sum + Math.pow(.5d, 2.0) + (Math.sqrt(1d + Math.pow(Math.abs(X - Y) / .5d, 2.0)) - 1d);
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
                sum = lossFunction(sum, gamma, predictedGamma);
            }
            return sum;
        };

        ConvergenceChecker<PointValuePair> convergenceChecker = new SimplePointChecker<>(0.005, 0.005, 100000);

        PointValuePair bestResult = optimizeCMAES(
                prediction,
                    new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), minP2, minP3 }),
                    new CMAESOptimizer.Sigma(new double[] { 1, 50, 50 }),
                    convergenceChecker,
                    50,
                    new SimpleBounds(new double[] { minP1, minP2, minP3 }, new double[] { maxP1, maxP2, maxP3 }));
        for (int i = 1; i < ITER_COUNT; i++) {

            PointValuePair result = optimizeCMAES(
                    prediction,
                        new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minP1, maxP1), ThreadLocalRandom.current().nextDouble(minP2, maxP2),
                                ThreadLocalRandom.current().nextDouble(minP3, maxP3) }),
                        new CMAESOptimizer.Sigma(new double[] { 1, 50, 50 }),
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
                        sum = lossFunction(sum, velocity, predictedVel);
                    }
                    return new Double[] { v0, v1, v2, sum };
                }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult);
            }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult);
        }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult));
    }

    public double[] gridSearchCodaB(List<Entry<Double, Double>> betaDistancePairs) {
        return gridSearchCodaB(betaDistancePairs, YBB_MIN, YBB_MAX, 0, B_DIST_MAX);
    }

    public double[] gridSearchCodaB(final List<Entry<Double, Double>> betaDistancePairs, final double ybbMin, final double ybbMax, final double distMin, final double distMax) {
        final Double[] baseResult = new Double[] { 0.0, 0.0, 0.0, 9E29 };
        return ArrayUtils.toPrimitive(IntStream.rangeClosed(-50, 201).parallel().mapToObj(ib0 -> {
            double b0 = -(ib0 - 1.0) * 0.001;
            return IntStream.rangeClosed(-10, 201).parallel().mapToObj(ib1 -> {
                double b1 = (ib1 - 1.0) * 0.01;
                return IntStream.rangeClosed(-10, 800).parallel().mapToObj(ib2 -> {
                    double b2 = ib2 * 1.50;

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
                        sum = lossFunction(sum, beta, bb);
                    }
                    return new Double[] { b0, b1, b2, sum };
                }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult);
            }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult);
        }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult));
    }

    public double[] gridSearchCodaG(List<Entry<Double, Double>> gammaDistancePairs) {
        return gridSearchCodaG(gammaDistancePairs, YGG_MIN, YGG_MAX, 0, G_DIST_MAX);
    }

    public double[] gridSearchCodaG(final List<Entry<Double, Double>> gammaDistancePairs, final double yggMin, final double yggMax, final double distMin, final double distMax) {
        final Double[] baseResult = new Double[] { 0.0, 0.0, 0.0, 9E29 };
        return ArrayUtils.toPrimitive(IntStream.rangeClosed(1, 21).parallel().mapToObj(ig0 -> {
            double g0 = 2.001 - (ig0 - 1.0) * 0.1;

            return IntStream.rangeClosed(1, 101).parallel().mapToObj(ig1 -> {

                double g1 = -(ig1 - 1.0);
                return IntStream.rangeClosed(1, 101).parallel().mapToObj(ig2 -> {
                    double g2 = 1.0 + (ig2 - 1.0);

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
                        sum = lossFunction(sum, gamma, predictedGamma);
                    }

                    return new Double[] { g0, g1, g2, sum };
                }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult);
            }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult);
        }).min((o1, o2) -> o1[3] > o2[3] ? 1 : -1).orElseGet(() -> baseResult));
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

        velocityDistancePairsFreqMap.entrySet().parallelStream().filter(velDistPairs -> freqBandMap.get(velDistPairs.getKey()) != null).forEach(velDistPairs -> {
            double[] curve = gridSearch(
                    velDistPairs.getValue().stream().map(v -> new AbstractMap.SimpleEntry<Double, Double>(v.getVelocity(), v.getDistance())).collect(Collectors.toList()),
                        new ApacheGridSearchV(),
                        new BasicGridSearchV());

            freqBandMap.put(velDistPairs.getKey(), freqBandMap.get(velDistPairs.getKey()).setVelocity0(curve[0]).setVelocity1(curve[1]).setVelocity2(curve[2]));
        });
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

        betaDistancePairsFreqMap.entrySet().parallelStream().filter(betaDistPairs -> freqBandMap.get(betaDistPairs.getKey()) != null).forEach(betaDistPairs -> {
            double[] curve = gridSearch(
                    betaDistPairs.getValue().stream().map(v -> new AbstractMap.SimpleEntry<Double, Double>(v.getMeasuredBeta(), v.getDistance())).collect(Collectors.toList()),
                        new ApacheGridSearchB(),
                        new BasicGridSearchB());
            // Artificially lower the intercept value, b0 to 95%
            // to account for possible noise contamination causing
            // too shallow decay
            freqBandMap.put(betaDistPairs.getKey(), freqBandMap.get(betaDistPairs.getKey()).setBeta0(curve[0] * 1.05).setBeta1(curve[1]).setBeta2(curve[2]));
        });

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

        gammaDistancePairsFreqMap.entrySet().parallelStream().filter(gammaDistPairs -> freqBandMap.get(gammaDistPairs.getKey()) != null).forEach(gammaDistPairs -> {
            double[] curve = gridSearch(
                    gammaDistPairs.getValue().stream().map(v -> new AbstractMap.SimpleEntry<Double, Double>(v.getMeasuredGamma(), v.getDistance())).collect(Collectors.toList()),
                        new ApacheGridSearchG(),
                        new BasicGridSearchG());
            freqBandMap.put(gammaDistPairs.getKey(), freqBandMap.get(gammaDistPairs.getKey()).setGamma0(curve[0]).setGamma1(curve[1]).setGamma2(curve[2]));
        });

        return freqBandMap;
    }

    private double[] gridSearch(List<Entry<Double, Double>> value, GridFitter main, GridFitter fallback) {
        double[] fit;
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
