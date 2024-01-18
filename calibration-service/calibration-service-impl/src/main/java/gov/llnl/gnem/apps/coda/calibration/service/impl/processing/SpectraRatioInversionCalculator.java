/*
* Copyright (c) 2022, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiFunction;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.SynchronizedMultivariateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedSummaryStatistics;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.RatioOptimizerMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResult;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResultJoint;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;

public class SpectraRatioInversionCalculator {

    private static final Logger log = LoggerFactory.getLogger(SpectraRatioInversionCalculator.class);

    private static final double STOP_FITNESS = 1e-10;
    private static final int XDIM = 32;
    private static final int YDIM = 32;

    private static final int PARAM_COUNT = 2;

    private static final int FIT = 0;

    private static final int JOINT_FIT = 0;
    private static final int JOINT_CF_A = 3;
    private static final int JOINT_CF_B = 6;

    private double momentErrorRange;
    private final double DEFAULT_LOW_MOMENT = 1.0;
    private final double DEFAULT_HIGH_MOMENT = 25.0;

    private final double lowTestAppStressMpa = 0.001;
    private final double highTestAppStressMpa = 100.0;

    private MeasuredMwsService fitMwService;
    private ReferenceMwParametersService refMwService;
    private MdacCalculator mdacCalculator;

    enum CORNER_FREQ_NAMES {
        A1_MIN, A1_MAX, B1_MIN, B1_MAX, A2_MIN, A2_MAX, B2_MIN, B2_MAX
    }

    private final Comparator<RatioOptimizerMeasurement> ratioOptimizerComparator = (o1, o2) -> {
        int compare = Double.compare(o1.getFit(), o2.getFit());
        if (compare == 0) {
            compare = Double.compare(o2.getCornerFreqA(), o1.getCornerFreqA());
            if (compare == 0) {
                compare = Double.compare(o2.getCornerFreqB(), o1.getCornerFreqB());
            }
        }
        return compare;
    };

    final Map<EventPair, Double[]> jointCornerMeasurements = new HashMap<>();
    Map<String, Double> eventMisfitA = new HashMap<>();
    Map<String, Double> eventMisfitB = new HashMap<>();

    // Create a map of eventpair to cornerFrequency min/max values for each event A and B in the pair
    final SynchronizedSummaryStatistics jointStats = new SynchronizedSummaryStatistics();

    public SpectraRatioInversionCalculator(MdacCalculatorService mdacService, MdacParametersFI mdacFiEntry, MdacParametersPS psRows, MeasuredMwsService fitMwService,
            ReferenceMwParametersService refMwService, double momentErrorRange) {
        this.fitMwService = fitMwService;
        this.refMwService = refMwService;
        //We just want the K constant for the given MDAC model so no need for a real moment here
        this.mdacCalculator = mdacService.getMdacCalculator(psRows, mdacFiEntry, DEFAULT_HIGH_MOMENT);
        this.momentErrorRange = momentErrorRange;
    }

    public Map<EventPair, SpectraRatioPairInversionResult> cmaesRegressionPerPair(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData) {
        Map<EventPair, SpectraRatioPairInversionResult> estimatedMomentCorners = new HashMap<>();
        ratioData.entrySet().stream().forEach(eventPairEntry -> {
            EventPair eventPair = eventPairEntry.getKey();
            Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> stationData = eventPairEntry.getValue();

            //Check for fit or reference entries for both events and, if present, use those
            // as priors on the inversion to help constrain it.
            Double lowTestMomentEventA = null;
            Double highTestMomentEventA = null;
            Double lowTestMomentEventB = null;
            Double highTestMomentEventB = null;

            MeasuredMwParameters fitMoment = fitMwService.findByEventId(eventPair.getY().getEventId());
            ReferenceMwParameters refMoment = refMwService.findByEventId(eventPair.getY().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) - momentErrorRange;
                highTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) + momentErrorRange;
            } else if (refMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - momentErrorRange;
                highTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + momentErrorRange;
            }

            if (lowTestMomentEventA == null) {
                lowTestMomentEventA = DEFAULT_LOW_MOMENT;
            }
            if (highTestMomentEventA == null) {
                highTestMomentEventA = DEFAULT_HIGH_MOMENT;
            }

            fitMoment = fitMwService.findByEventId(eventPair.getX().getEventId());
            refMoment = refMwService.findByEventId(eventPair.getX().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) - momentErrorRange;
                highTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) + momentErrorRange;
            } else if (refMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - momentErrorRange;
                highTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + momentErrorRange;
            }

            if (lowTestMomentEventB == null) {
                lowTestMomentEventB = DEFAULT_LOW_MOMENT;
            }
            if (highTestMomentEventB == null) {
                highTestMomentEventB = DEFAULT_HIGH_MOMENT;
            }

            SpectraRatioCostFunctionPerEventPair costFunc = new SpectraRatioCostFunctionPerEventPair(stationData,
                                                                                                     lowTestMomentEventB,
                                                                                                     highTestMomentEventB,
                                                                                                     lowTestMomentEventA,
                                                                                                     highTestMomentEventA,
                                                                                                     lowTestAppStressMpa,
                                                                                                     highTestAppStressMpa,
                                                                                                     lowTestAppStressMpa,
                                                                                                     highTestAppStressMpa);

            CMAESOptimizer optimizer = new CMAESOptimizer(500, STOP_FITNESS, true, 0, 0, new MersenneTwister(), false, new SimplePointChecker<>(0.001, 0.001, 100000));

            PointValuePair best = optimizer.optimize(
                    new MaxEval(1000000),
                        new ObjectiveFunction(costFunc),
                        GoalType.MINIMIZE,
                        new SimpleBounds(new double[] { lowTestMomentEventA, lowTestAppStressMpa, lowTestMomentEventB, lowTestAppStressMpa },
                                         new double[] { highTestMomentEventA, highTestAppStressMpa, highTestMomentEventB, highTestAppStressMpa }),
                        new InitialGuess(new double[] { lowTestMomentEventA + ((highTestMomentEventA - lowTestMomentEventA) / 2.0),
                                lowTestAppStressMpa + ((highTestAppStressMpa - lowTestAppStressMpa) / 2.0), lowTestMomentEventB + ((highTestMomentEventB - lowTestMomentEventB) / 2.0),
                                lowTestAppStressMpa + ((highTestAppStressMpa - lowTestAppStressMpa) / 2.0) }),
                        new CMAESOptimizer.Sigma(new double[] { (highTestMomentEventA - lowTestMomentEventA) / 2.0, (highTestAppStressMpa - lowTestAppStressMpa) / 2.0,
                                (highTestMomentEventB - lowTestMomentEventB) / 2.0, (highTestAppStressMpa - lowTestAppStressMpa) / 2.0 }),
                        new CMAESOptimizer.PopulationSize(100));

            //Technically we could save the second Z array copy here by storing these as a tensor rather than a matrix but
            //almost assuredly premature optimization at the moment
            Pair<EventInversionMap, EventInversionMap> costs = costFunc.getSamplePoints();
            EventInversionMap eventCost = costs.getX();
            IntArrayList m0XIdx = new IntArrayList(eventCost.size());
            IntArrayList m0YIdx = new IntArrayList(eventCost.size());
            FloatArrayList m0Samples = new FloatArrayList(eventCost.size());
            for (Entry<Pair<Integer, Integer>, Pair<Float, Integer>> cost : eventCost.entrySet()) {
                m0XIdx.add(cost.getKey().getX());
                m0YIdx.add(cost.getKey().getY());
                m0Samples.add(cost.getValue().getX());
            }

            eventCost = costs.getY();
            IntArrayList stressXIdx = new IntArrayList(eventCost.size());
            IntArrayList stressYIdx = new IntArrayList(eventCost.size());
            FloatArrayList stressSamples = new FloatArrayList(eventCost.size());
            for (Entry<Pair<Integer, Integer>, Pair<Float, Integer>> cost : eventCost.entrySet()) {
                stressXIdx.add(cost.getKey().getX());
                stressYIdx.add(cost.getKey().getY());
                stressSamples.add(cost.getValue().getX());
            }

            SynchronizedMultivariateSummaryStatistics stats = costFunc.getStats();
            // Calculate the corner freq min max x2
            final RealMatrix C = stats.getCovariance();

            final double SE = Math.sqrt(C.getEntry(FIT, FIT) / (stats.getN() - 4.0));
            final double f1 = best.getValue() + SE;
            final double f2 = best.getValue() + (SE * 2.0);

            double cornerFreqA1Min = Double.POSITIVE_INFINITY;
            double cornerFreqA2Min = Double.POSITIVE_INFINITY;
            double cornerFreqB1Min = Double.POSITIVE_INFINITY;
            double cornerFreqB2Min = Double.POSITIVE_INFINITY;
            double cornerFreqA1Max = Double.NEGATIVE_INFINITY;
            double cornerFreqA2Max = Double.NEGATIVE_INFINITY;
            double cornerFreqB1Max = Double.NEGATIVE_INFINITY;
            double cornerFreqB2Max = Double.NEGATIVE_INFINITY;

            for (RatioOptimizerMeasurement meas : costFunc.getOptimizerMeasurements()) {
                if (meas.getFit() < f1) {
                    if (meas.getCornerFreqA() < cornerFreqA1Min) {
                        cornerFreqA1Min = meas.getCornerFreqA();
                        cornerFreqA2Min = meas.getCornerFreqA();
                    }
                    if (meas.getCornerFreqA() > cornerFreqA1Max) {
                        cornerFreqA1Max = meas.getCornerFreqA();
                        cornerFreqA2Max = meas.getCornerFreqA();
                    }

                    if (meas.getCornerFreqB() < cornerFreqB1Min) {
                        cornerFreqB1Min = meas.getCornerFreqB();
                        cornerFreqB2Min = meas.getCornerFreqB();
                    }
                    if (meas.getCornerFreqB() > cornerFreqB1Max) {
                        cornerFreqB1Max = meas.getCornerFreqB();
                        cornerFreqB2Max = meas.getCornerFreqB();
                    }
                } else if (meas.getFit() < f2) {
                    if (meas.getCornerFreqA() < cornerFreqA2Min) {
                        cornerFreqA2Min = meas.getCornerFreqA();
                    }
                    if (meas.getCornerFreqA() > cornerFreqA2Max) {
                        cornerFreqA2Max = meas.getCornerFreqA();
                    }

                    if (meas.getCornerFreqB() < cornerFreqB2Min) {
                        cornerFreqB2Min = meas.getCornerFreqB();
                    }
                    if (meas.getCornerFreqB() > cornerFreqB2Max) {
                        cornerFreqB2Max = meas.getCornerFreqB();
                    }
                } else {
                    break;
                }
            }

            SpectraRatioPairInversionResult estimate = new SpectraRatioPairInversionResult();
            estimate.setEventIdA(eventPair.getY().getEventId())
                    .setEventIdB(eventPair.getX().getEventId())
                    .setMomentEstimateA((float) best.getPoint()[0])
                    .setCornerEstimateA((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[0]), best.getPoint()[1]))
                    .setCornerEstimateA1Min(cornerFreqA1Min)
                    .setCornerEstimateA1Max(cornerFreqA1Max)
                    .setCornerEstimateA2Min(cornerFreqA2Min)
                    .setCornerEstimateA2Max(cornerFreqA2Max)
                    .setMomentEstimateB((float) best.getPoint()[2])
                    .setCornerEstimateB((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[2]), best.getPoint()[3]))
                    .setCornerEstimateB1Min(cornerFreqB1Min)
                    .setCornerEstimateB1Max(cornerFreqB1Max)
                    .setCornerEstimateB2Min(cornerFreqB2Min)
                    .setCornerEstimateB2Max(cornerFreqB2Max)
                    .setApparentStressEstimateA((float) best.getPoint()[1])
                    .setApparentStressEstimateB((float) best.getPoint()[3])
                    .setMisfit(best.getValue().floatValue())
                    .setAppStressMin((float) lowTestAppStressMpa)
                    .setAppStressMax((float) highTestAppStressMpa)
                    .setM0minY(lowTestMomentEventA.floatValue())
                    .setM0maxY(highTestMomentEventA.floatValue())
                    .setM0minX(lowTestMomentEventB.floatValue())
                    .setM0maxX(highTestMomentEventB.floatValue())
                    .setM0XIdx(m0XIdx)
                    .setM0Xdim(XDIM)
                    .setM0YIdx(m0YIdx)
                    .setM0Ydim(YDIM)
                    .setM0data(m0Samples)
                    .setStressXIdx(stressXIdx)
                    .setAppStressXdim(XDIM)
                    .setStressYIdx(stressYIdx)
                    .setAppStressYdim(YDIM)
                    .setStressData(stressSamples)
                    .setkConstant(mdacCalculator.getK());

            estimatedMomentCorners.put(eventPair, estimate);
        });
        return estimatedMomentCorners;
    }

    public Map<EventPair, SpectraRatioPairInversionResultJoint> cmaesRegressionJoint(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData) {

        //The structure of this method is a little clunky because we are re-using an existing per-pair inversion.
        //So for now we have to do this song and dance where we re-package the data.
        //Should revisit at a future date if we drop the per-pair to make this a bit cleaner.

        Set<String> numerators = new HashSet<>();
        Set<String> denominators = new HashSet<>();
        for (EventPair eventPair : ratioData.keySet()) {
            numerators.add(eventPair.getY().getEventId());
            denominators.add(eventPair.getX().getEventId());
        }
        int uniquePossible = numerators.size() + denominators.size();
        double[] inversionLowBounds = new double[uniquePossible * PARAM_COUNT];
        double[] inversionHighBounds = new double[uniquePossible * PARAM_COUNT];
        double[] startingPoints = new double[uniquePossible * PARAM_COUNT];
        double[] sigmaValues = new double[uniquePossible * PARAM_COUNT];

        double highTestMomentA = 0;
        double lowTestMomentA = Double.MAX_VALUE;
        double highTestMomentB = 0;
        double lowTestMomentB = Double.MAX_VALUE;

        Map<String, Integer> eventIndexMap = new HashMap<>();
        Map<Pair<Integer, Integer>, List<Double[]>> eventPairData = new HashMap<>();

        int i = 0;
        for (EventPair eventPair : ratioData.keySet()) {
            int increment = 0;
            //We will need these indexes later to find specific events in the flattened vector
            Integer momentAIdx = eventIndexMap.get(eventPair.getY().getEventId());
            Integer momentBIdx = eventIndexMap.get(eventPair.getX().getEventId());

            if (momentAIdx == null) {
                eventIndexMap.put(eventPair.getY().getEventId(), i);
                momentAIdx = i;
                increment = increment + 2;
            }
            if (momentBIdx == null) {
                eventIndexMap.put(eventPair.getX().getEventId(), i + increment);
                momentBIdx = i + increment;
                increment = increment + 2;
            }
            int stressAIdx = momentAIdx + 1;
            int stressBIdx = momentBIdx + 1;

            Double lowTestMomentEventA = null;
            Double highTestMomentEventA = null;
            Double lowTestMomentEventB = null;
            Double highTestMomentEventB = null;

            //Check for fit or reference entries for both events and, if present, use those
            // as priors on the inversion to help constrain it.
            MeasuredMwParameters fitMoment = fitMwService.findByEventId(eventPair.getY().getEventId());
            ReferenceMwParameters refMoment = refMwService.findByEventId(eventPair.getY().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) - momentErrorRange;
                highTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) + momentErrorRange;
            } else if (refMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - momentErrorRange;
                highTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + momentErrorRange;
            }

            if (lowTestMomentEventA == null) {
                lowTestMomentEventA = DEFAULT_LOW_MOMENT;
            }
            if (highTestMomentEventA == null) {
                highTestMomentEventA = DEFAULT_HIGH_MOMENT;
            }

            fitMoment = fitMwService.findByEventId(eventPair.getX().getEventId());
            refMoment = refMwService.findByEventId(eventPair.getX().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) - momentErrorRange;
                highTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) + momentErrorRange;
            } else if (refMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - momentErrorRange;
                highTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + momentErrorRange;
            }

            if (lowTestMomentEventB == null) {
                lowTestMomentEventB = DEFAULT_LOW_MOMENT;
            }
            if (highTestMomentEventB == null) {
                highTestMomentEventB = DEFAULT_HIGH_MOMENT;
            }

            inversionLowBounds[momentAIdx] = lowTestMomentEventA;
            inversionLowBounds[stressAIdx] = lowTestAppStressMpa;
            inversionHighBounds[momentAIdx] = highTestMomentEventA;
            inversionHighBounds[stressAIdx] = highTestAppStressMpa;

            inversionLowBounds[momentBIdx] = lowTestMomentEventB;
            inversionLowBounds[stressBIdx] = lowTestAppStressMpa;
            inversionHighBounds[momentBIdx] = highTestMomentEventB;
            inversionHighBounds[stressBIdx] = highTestAppStressMpa;

            startingPoints[momentAIdx] = lowTestMomentEventA + ((highTestMomentEventA - lowTestMomentEventA) / 2.0);
            startingPoints[stressAIdx] = lowTestAppStressMpa + ((highTestAppStressMpa - lowTestAppStressMpa) / 2.0);
            startingPoints[momentBIdx] = lowTestMomentEventB + ((highTestMomentEventB - lowTestMomentEventB) / 2.0);
            startingPoints[stressBIdx] = startingPoints[stressAIdx];

            sigmaValues[momentAIdx] = (highTestMomentEventA - lowTestMomentEventA) / 2.0;
            sigmaValues[stressAIdx] = (highTestAppStressMpa - lowTestAppStressMpa) / 2.0;
            sigmaValues[momentBIdx] = (highTestMomentEventB - lowTestMomentEventB) / 2.0;
            sigmaValues[stressBIdx] = (highTestAppStressMpa - lowTestAppStressMpa) / 2.0;

            lowTestMomentA = Math.min(lowTestMomentA, lowTestMomentEventA);
            highTestMomentA = Math.max(highTestMomentA, highTestMomentEventA);
            lowTestMomentB = Math.min(lowTestMomentB, lowTestMomentEventB);
            highTestMomentB = Math.max(highTestMomentB, highTestMomentEventB);

            //Increment to the next block of values
            i = i + increment;
        }

        SpectraRatioCostFunctionJoint costFunc = new SpectraRatioCostFunctionJoint(ratioData,
                                                                                   eventIndexMap,
                                                                                   (mo, stress) -> mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, mo), stress),
                                                                                   lowTestMomentB,
                                                                                   highTestMomentB,
                                                                                   lowTestMomentA,
                                                                                   highTestMomentA,
                                                                                   lowTestAppStressMpa,
                                                                                   highTestAppStressMpa,
                                                                                   lowTestAppStressMpa,
                                                                                   highTestAppStressMpa,
                                                                                   eventPairData);

        CMAESOptimizer optimizer = new CMAESOptimizer(5000, STOP_FITNESS, true, 0, 0, new MersenneTwister(), false, new SimplePointChecker<>(0.001, 0.001, 1000000));

        PointValuePair best = optimizer.optimize(
                new MaxEval(10000000),
                    new ObjectiveFunction(costFunc),
                    GoalType.MINIMIZE,
                    new SimpleBounds(inversionLowBounds, inversionHighBounds),
                    new InitialGuess(startingPoints),
                    new CMAESOptimizer.Sigma(sigmaValues),
                    new CMAESOptimizer.PopulationSize(100));

        Map<CORNER_FREQ_NAMES, Map<Pair<Integer, Integer>, Double>> cornerFreqMap = new EnumMap<>(CORNER_FREQ_NAMES.class);

        cornerFreqMap.put(CORNER_FREQ_NAMES.A1_MIN, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.A1_MAX, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.B1_MIN, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.B1_MAX, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.A2_MIN, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.A2_MAX, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.B2_MIN, new HashMap<>());
        cornerFreqMap.put(CORNER_FREQ_NAMES.B2_MAX, new HashMap<>());

        for (Entry<Pair<Integer, Integer>, List<Double[]>> eventPairDataEntry : eventPairData.entrySet()) {

            Pair<Integer, Integer> idxPair = eventPairDataEntry.getKey();
            final double SE = jointStats.getStandardDeviation() / Math.sqrt(jointStats.getN() - (4.0 * eventPairData.size()));
            final double f1 = best.getValue().doubleValue() + SE;
            final double f2 = f1 + (SE * 2.0);

            double cornerFreqA1Min = Double.POSITIVE_INFINITY;
            double cornerFreqB1Min = Double.POSITIVE_INFINITY;
            double cornerFreqA2Min = Double.POSITIVE_INFINITY;
            double cornerFreqB2Min = Double.POSITIVE_INFINITY;
            double cornerFreqA1Max = Double.NEGATIVE_INFINITY;
            double cornerFreqB1Max = Double.NEGATIVE_INFINITY;
            double cornerFreqA2Max = Double.NEGATIVE_INFINITY;
            double cornerFreqB2Max = Double.NEGATIVE_INFINITY;

            for (Double[] values : eventPairDataEntry.getValue()) {
                Double eventFit = values[JOINT_FIT];
                Double cornerFreqA = values[JOINT_CF_A];
                Double cornerFreqB = values[JOINT_CF_B];
                if (eventFit < f1) {
                    if (cornerFreqA < cornerFreqA1Min) {
                        cornerFreqA1Min = cornerFreqA;
                        cornerFreqA2Min = cornerFreqA;
                    }
                    if (cornerFreqA > cornerFreqA1Max) {
                        cornerFreqA1Max = cornerFreqA;
                        cornerFreqA2Max = cornerFreqA;
                    }

                    if (cornerFreqB < cornerFreqB1Min) {
                        cornerFreqB1Min = cornerFreqB;
                        cornerFreqB2Min = cornerFreqB;
                    }
                    if (cornerFreqB > cornerFreqB1Max) {
                        cornerFreqB1Max = cornerFreqB;
                        cornerFreqB2Max = cornerFreqB;
                    }
                } else if (eventFit < f2) {
                    if (cornerFreqA < cornerFreqA2Min) {
                        cornerFreqA2Min = cornerFreqA;
                    }
                    if (cornerFreqA > cornerFreqA2Max) {
                        cornerFreqA2Max = cornerFreqA;
                    }

                    if (cornerFreqB < cornerFreqB2Min) {
                        cornerFreqB2Min = cornerFreqB;
                    }
                    if (cornerFreqB > cornerFreqB2Max) {
                        cornerFreqB2Max = cornerFreqB;
                    }
                }
            }

            cornerFreqMap.get(CORNER_FREQ_NAMES.A1_MIN).put(idxPair, cornerFreqA1Min);
            cornerFreqMap.get(CORNER_FREQ_NAMES.A1_MAX).put(idxPair, cornerFreqA1Max);
            cornerFreqMap.get(CORNER_FREQ_NAMES.B1_MIN).put(idxPair, cornerFreqB1Min);
            cornerFreqMap.get(CORNER_FREQ_NAMES.B1_MAX).put(idxPair, cornerFreqB1Max);
            cornerFreqMap.get(CORNER_FREQ_NAMES.A2_MIN).put(idxPair, cornerFreqA2Min);
            cornerFreqMap.get(CORNER_FREQ_NAMES.A2_MAX).put(idxPair, cornerFreqA2Max);
            cornerFreqMap.get(CORNER_FREQ_NAMES.B2_MIN).put(idxPair, cornerFreqB2Min);
            cornerFreqMap.get(CORNER_FREQ_NAMES.B2_MAX).put(idxPair, cornerFreqB2Max);
        }

        //We split these back out to "per-pair" measurements to report them
        //It wastes some amount of space and makes N-d plots very hard
        // but it fits into our existing plots and ways of looking at the results
        // so for now we will leave it.
        Map<EventPair, Pair<EventInversionMap, EventInversionMap>> rawEstimates = costFunc.getSamplePoints();

        Map<EventPair, SpectraRatioPairInversionResultJoint> estimatedMomentCorners = new HashMap<>();
        for (Entry<EventPair, Pair<EventInversionMap, EventInversionMap>> rawEstimate : rawEstimates.entrySet()) {
            Pair<EventInversionMap, EventInversionMap> costs = rawEstimate.getValue();
            EventPair eventPair = rawEstimate.getKey();
            Integer numerIdx = eventIndexMap.get(eventPair.getY().getEventId());
            Integer denomIdx = eventIndexMap.get(eventPair.getX().getEventId());
            Pair<Integer, Integer> idxPair = new Pair<>(numerIdx, denomIdx);
            Integer eventAidx = eventIndexMap.get(eventPair.getY().getEventId());
            Integer eventBidx = eventIndexMap.get(eventPair.getX().getEventId());

            EventInversionMap eventCost = costs.getX();
            IntArrayList m0XIdx = new IntArrayList(eventCost.size());
            IntArrayList m0YIdx = new IntArrayList(eventCost.size());
            FloatArrayList m0Samples = new FloatArrayList(eventCost.size());
            for (Entry<Pair<Integer, Integer>, Pair<Float, Integer>> cost : eventCost.entrySet()) {
                m0XIdx.add(cost.getKey().getX());
                m0YIdx.add(cost.getKey().getY());
                m0Samples.add(cost.getValue().getX());
            }

            eventCost = costs.getY();
            IntArrayList stressXIdx = new IntArrayList(eventCost.size());
            IntArrayList stressYIdx = new IntArrayList(eventCost.size());
            FloatArrayList stressSamples = new FloatArrayList(eventCost.size());
            for (Entry<Pair<Integer, Integer>, Pair<Float, Integer>> cost : eventCost.entrySet()) {
                stressXIdx.add(cost.getKey().getX());
                stressYIdx.add(cost.getKey().getY());
                stressSamples.add(cost.getValue().getX());
            }

            SpectraRatioPairInversionResultJoint estimate = new SpectraRatioPairInversionResultJoint();
            estimate.setEventIdA(eventPair.getY().getEventId())
                    .setEventIdB(eventPair.getX().getEventId())
                    .setMomentEstimateA((float) best.getPoint()[eventAidx])
                    .setCornerEstimateA((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[eventAidx]), best.getPoint()[eventAidx + 1]))
                    .setCornerEstimateA1Min(cornerFreqMap.get(CORNER_FREQ_NAMES.A1_MIN).get(idxPair))
                    .setCornerEstimateA1Max(cornerFreqMap.get(CORNER_FREQ_NAMES.A1_MAX).get(idxPair))
                    .setCornerEstimateA2Min(cornerFreqMap.get(CORNER_FREQ_NAMES.A2_MIN).get(idxPair))
                    .setCornerEstimateA2Max(cornerFreqMap.get(CORNER_FREQ_NAMES.A2_MAX).get(idxPair))
                    .setMomentEstimateB((float) best.getPoint()[eventBidx])
                    .setCornerEstimateB((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[eventBidx]), best.getPoint()[eventBidx + 1]))
                    .setCornerEstimateB1Min(cornerFreqMap.get(CORNER_FREQ_NAMES.B1_MIN).get(idxPair))
                    .setCornerEstimateB1Max(cornerFreqMap.get(CORNER_FREQ_NAMES.B1_MAX).get(idxPair))
                    .setCornerEstimateB2Min(cornerFreqMap.get(CORNER_FREQ_NAMES.B2_MIN).get(idxPair))
                    .setCornerEstimateB2Max(cornerFreqMap.get(CORNER_FREQ_NAMES.B2_MAX).get(idxPair))
                    .setApparentStressEstimateA((float) best.getPoint()[eventAidx + 1])
                    .setApparentStressEstimateB((float) best.getPoint()[eventBidx + 1])
                    .setMisfit(best.getValue().floatValue())
                    .setAppStressMin((float) lowTestAppStressMpa)
                    .setAppStressMax((float) highTestAppStressMpa)
                    .setM0minY((float) inversionLowBounds[eventAidx])
                    .setM0maxY((float) inversionHighBounds[eventAidx])
                    .setM0minX((float) inversionLowBounds[eventBidx])
                    .setM0maxX((float) inversionHighBounds[eventBidx])
                    .setM0XIdx(m0XIdx)
                    .setM0Xdim(XDIM)
                    .setM0YIdx(m0YIdx)
                    .setM0Ydim(YDIM)
                    .setM0data(m0Samples)
                    .setStressXIdx(stressXIdx)
                    .setAppStressXdim(XDIM)
                    .setStressYIdx(stressYIdx)
                    .setAppStressYdim(YDIM)
                    .setStressData(stressSamples)
                    .setkConstant(mdacCalculator.getK());
            estimatedMomentCorners.put(eventPair, estimate);
        }
        return estimatedMomentCorners;
    }

    private Pair<Float, Integer> accumulatePoint(float sum, Pair<Float, Integer> value) {
        if (value != null && value.getY() != null) {
            int count = value.getY();
            value.setY(count + 1);
            value.setX(((value.getX() * count) + sum) / (count + 1f));
        } else {
            value = new Pair<>(sum, 1);
        }
        return value;
    }

    private class SpectraRatioCostFunctionPerEventPair implements MultivariateFunction {

        private Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> stationData;
        private Pair<EventInversionMap, EventInversionMap> costs = new Pair<>(new EventInversionMap(), new EventInversionMap());
        final SortedSet<RatioOptimizerMeasurement> optimizerMeasurements = Collections.synchronizedSortedSet(new TreeSet<>(ratioOptimizerComparator));
        final SynchronizedMultivariateSummaryStatistics stats = new SynchronizedMultivariateSummaryStatistics(7, false);

        private double m0minX;
        private double m0maxX;
        private double m0minY;
        private double m0maxY;
        private double appStressMinX;
        private double appStressMaxX;
        private double appStressMinY;
        private double appStressMaxY;

        public SpectraRatioCostFunctionPerEventPair(Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> stationData, double m0minX, double m0maxX, double m0minY, double m0maxY,
                double appStressMinX, double appStressMaxX, double appStressMinY, double appStressMaxY) {
            this.stationData = stationData;
            this.m0minX = m0minX;
            this.m0maxX = m0maxX;
            this.m0minY = m0minY;
            this.m0maxY = m0maxY;
            this.appStressMinX = appStressMinX;
            this.appStressMaxX = appStressMaxX;
            this.appStressMinY = appStressMinY;
            this.appStressMaxY = appStressMaxY;
        }

        @Override
        public double value(double[] point) {
            float sum = 0f;
            double log10_M0 = point[0];
            double appStress = point[1];
            double cornerFreq = mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10.0, log10_M0), appStress);

            double log10_M0_2 = point[2];
            double appStress_2 = point[3];
            double cornerFreq_2 = mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10.0, log10_M0_2), appStress_2);

            for (Map<FrequencyBand, SpectraRatioPairDetails> recordings : stationData.values()) {
                for (Entry<FrequencyBand, SpectraRatioPairDetails> record : recordings.entrySet()) {
                    double centerFreq = (record.getKey().getLowFrequency() + record.getKey().getHighFrequency()) / 2.0;
                    double numer = log10_M0 - Math.log10(1.0 + Math.pow(centerFreq / cornerFreq, 2.0));
                    double denom = log10_M0_2 - Math.log10(1.0 + Math.pow(centerFreq / cornerFreq_2, 2.0));
                    double diff = numer - denom;
                    sum = (float) (sum + Math.abs(record.getValue().getDiffAvg() - diff));
                }
            }

            int y = (int) (((log10_M0 - m0minY) / (m0maxY - m0minY)) * (XDIM - 1));
            int x = (int) (((log10_M0_2 - m0minX) / (m0maxX - m0minX)) * (XDIM - 1));

            Pair<Integer, Integer> xyPoint = new Pair<>(x, y);

            Pair<Float, Integer> value = costs.getX().get(xyPoint);
            costs.getX().put(xyPoint, accumulatePoint(sum, value));

            int y2 = (int) (((Math.log10(appStress) - Math.log10(appStressMinY)) / (Math.log10(appStressMaxY) - Math.log10(appStressMinY))) * (YDIM - 1));
            int x2 = (int) (((Math.log10(appStress_2) - Math.log10(appStressMinX)) / (Math.log10(appStressMaxX) - Math.log10(appStressMinX))) * (YDIM - 1));

            xyPoint = new Pair<>(x2, y2);
            value = costs.getY().get(xyPoint);
            costs.getY().put(xyPoint, accumulatePoint(sum, value));

            stats.addValue(new double[] { sum, log10_M0, appStress, cornerFreq, log10_M0_2, appStress_2, cornerFreq_2 });
            optimizerMeasurements.add(new RatioOptimizerMeasurement(sum, log10_M0, appStress, cornerFreq, log10_M0_2, appStress_2, cornerFreq_2));

            return sum;
        }

        public Pair<EventInversionMap, EventInversionMap> getSamplePoints() {
            return costs;
        }

        public SortedSet<RatioOptimizerMeasurement> getOptimizerMeasurements() {
            return optimizerMeasurements;
        }

        public SynchronizedMultivariateSummaryStatistics getStats() {
            return stats;
        }

    }

    private class SpectraRatioCostFunctionJoint implements MultivariateFunction {

        private Map<String, Integer> eventIndexMap;
        private BiFunction<Double, Double, Double> cornerFreqFunc;
        private Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData;
        private double m0minX;
        private double m0maxX;
        private double m0minY;
        private double m0maxY;
        private double appStressMinX;
        private double appStressMaxX;
        private double appStressMinY;
        private double appStressMaxY;

        private Map<EventPair, Pair<EventInversionMap, EventInversionMap>> costs = new HashMap<>();
        private Map<Pair<Integer, Integer>, List<Double[]>> eventPairData;

        public SpectraRatioCostFunctionJoint(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData, Map<String, Integer> eventIndexMap,
                BiFunction<Double, Double, Double> stressFunc, double m0minX, double m0maxX, double m0minY, double m0maxY, double appStressMinX, double appStressMaxX, double appStressMinY,
                double appStressMaxY, Map<Pair<Integer, Integer>, List<Double[]>> eventPairData) {
            this.ratioData = ratioData;
            this.eventIndexMap = eventIndexMap;
            this.cornerFreqFunc = stressFunc;
            this.m0minX = m0minX;
            this.m0maxX = m0maxX;
            this.m0minY = m0minY;
            this.m0maxY = m0maxY;
            this.appStressMinX = appStressMinX;
            this.appStressMaxX = appStressMaxX;
            this.appStressMinY = appStressMinY;
            this.appStressMaxY = appStressMaxY;
            this.eventPairData = eventPairData;
        }

        @Override
        public double value(double[] point) {
            float sum = 0f;
            List<Double[]> eventPairInput = new ArrayList<>();

            for (Entry<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> eventPair : ratioData.entrySet()) {
                float eventPairSum = 0f;
                Integer numerIdx = eventIndexMap.get(eventPair.getKey().getY().getEventId());
                Integer denomIdx = eventIndexMap.get(eventPair.getKey().getX().getEventId());

                Pair<EventInversionMap, EventInversionMap> cost;
                if (costs.get(eventPair.getKey()) == null) {
                    cost = new Pair<EventInversionMap, EventInversionMap>(new EventInversionMap(), new EventInversionMap());
                    costs.put(eventPair.getKey(), cost);
                } else {
                    cost = costs.get(eventPair.getKey());
                }

                //We need to map event ids back to the big vector of double values the optimizer is using
                // where the idx is moment and +1 is apparentStress
                double log10_M0 = point[numerIdx];
                double log10_M0_2 = point[denomIdx];
                double appStress = point[numerIdx + 1];
                double appStress_2 = point[denomIdx + 1];
                double cornerFreq = cornerFreqFunc.apply(log10_M0, appStress);
                double cornerFreq_2 = cornerFreqFunc.apply(log10_M0_2, appStress_2);

                for (Map<FrequencyBand, SpectraRatioPairDetails> stationRecordings : eventPair.getValue().values()) {
                    for (Entry<FrequencyBand, SpectraRatioPairDetails> record : stationRecordings.entrySet()) {
                        final Double centerFreq = (record.getKey().getHighFrequency() + record.getKey().getLowFrequency()) / 2.0;

                        double numer = log10_M0 - Math.log10(1.0 + Math.pow(centerFreq / cornerFreq, 2.0));
                        double denom = log10_M0_2 - Math.log10(1.0 + Math.pow(centerFreq / cornerFreq_2, 2.0));
                        double diff = numer - denom;
                        double recordDiff = record.getValue().getDiffAvg();
                        eventPairSum = eventPairSum + (float) Math.abs(recordDiff - diff);
                    }
                }

                int y = (int) (((log10_M0 - m0minY) / (m0maxY - m0minY)) * (XDIM - 1));
                int x = (int) (((log10_M0_2 - m0minX) / (m0maxX - m0minX)) * (XDIM - 1));

                Pair<Integer, Integer> xyPoint = new Pair<>(x, y);
                Pair<Float, Integer> value = cost.getX().get(xyPoint);
                cost.getX().put(xyPoint, accumulatePoint(eventPairSum, value));

                int y2 = (int) (((Math.log10(appStress) - Math.log10(appStressMinY)) / (Math.log10(appStressMaxY) - Math.log10(appStressMinY))) * (YDIM - 1));
                int x2 = (int) (((Math.log10(appStress_2) - Math.log10(appStressMinX)) / (Math.log10(appStressMaxX) - Math.log10(appStressMinX))) * (YDIM - 1));

                xyPoint = new Pair<>(x2, y2);
                value = cost.getY().get(xyPoint);
                cost.getY().put(xyPoint, accumulatePoint(eventPairSum, value));

                sum = sum + eventPairSum;

                eventPairInput.add(new Double[] { numerIdx.doubleValue(), denomIdx.doubleValue(), cornerFreq, appStress, log10_M0, cornerFreq_2, appStress_2, log10_M0_2 });
            }

            jointStats.addValue(sum);

            for (Double[] eventPairValue : eventPairInput) {
                Integer numerIdx = eventPairValue[0].intValue();
                Integer denomIdx = eventPairValue[1].intValue();

                // Create a pair from the idx values
                Pair<Integer, Integer> idxPair = new Pair<>(numerIdx, denomIdx);

                // Create new array item that contains the sum as first value
                Double[] arr = new Double[eventPairValue.length + 1];
                arr[0] = (double) sum;
                for (int idx = 1; idx < arr.length; idx++) {
                    arr[idx] = eventPairValue[idx - 1];
                }

                // Updated event pair data by adding the arr
                if (!eventPairData.containsKey(idxPair)) {
                    List<Double[]> eventPairValues = new ArrayList<Double[]>();
                    eventPairValues.add(arr);
                    eventPairData.put(idxPair, eventPairValues);
                } else {
                    List<Double[]> eventPairValues = eventPairData.get(idxPair);
                    eventPairValues.add(arr);
                }
            }

            return sum;
        }

        public Map<EventPair, Pair<EventInversionMap, EventInversionMap>> getSamplePoints() {
            return costs;
        }

    }

    private class EventInversionMap extends TreeMap<Pair<Integer, Integer>, Pair<Float, Integer>> {
        public EventInversionMap() {
            super(new IntegerPointComparator());
        }

        private static final long serialVersionUID = 1L;
    }

    private class IntegerPointComparator implements Comparator<Pair<Integer, Integer>> {

        @Override
        public int compare(Pair<Integer, Integer> x, Pair<Integer, Integer> y) {
            int compare = Integer.compare(x.getX(), y.getX());
            if (compare == 0) {
                compare = Integer.compare(x.getY(), y.getY());
            }
            return compare;
        }
    };

}
