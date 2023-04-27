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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.ReferenceMwParametersService;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.spectra.model.domain.MomentCornerEstimate;
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

    private final double DEFAULT_MOMENT_ERROR = 1.0;
    private final double DEFAULT_LOW_MOMENT = 1.0;
    private final double DEFAULT_HIGH_MOMENT = 25.0;
    private final double testMomentIncrement = 0.25;

    private final double lowTestAppStressMpa = 0.001;
    private final double highTestAppStressMpa = 100.0;
    private final double testAppStressIncrement = 0.1;

    private MeasuredMwsService fitMwService;
    private ReferenceMwParametersService refMwService;
    private MdacCalculator mdacCalculator;

    public SpectraRatioInversionCalculator(MdacCalculatorService mdacService, MdacParametersFI mdacFiEntry, MdacParametersPS psRows, MeasuredMwsService fitMwService,
            ReferenceMwParametersService refMwService) {
        this.fitMwService = fitMwService;
        this.refMwService = refMwService;
        //We just want the K constant for the given MDAC model so no need for a real moment here
        this.mdacCalculator = mdacService.getMdacCalculator(psRows, mdacFiEntry, DEFAULT_HIGH_MOMENT);
    }

    public Map<EventPair, List<MomentCornerEstimate>> gridSearchPerPair(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData) {

        Map<EventPair, List<MomentCornerEstimate>> estimatedMomentCorners = new HashMap<>();
        ratioData.entrySet().stream().forEach(eventPairEntry -> {
            EventPair eventPair = eventPairEntry.getKey();
            Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> stationData = eventPairEntry.getValue();

            double curCost = 0.0;

            //Check for fit or reference entries for both events and, if present, use those
            // as priors on the inversion to help constrain it.
            Double lowTestMomentEventA = null;
            Double highTestMomentEventA = null;
            Double lowTestMomentEventB = null;
            Double highTestMomentEventB = null;

            MeasuredMwParameters fitMoment = fitMwService.findByEventId(eventPair.getY().getEventId());
            ReferenceMwParameters refMoment = refMwService.findByEventId(eventPair.getY().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw());
                highTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw());
            } else if (refMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw());
                highTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw());
            }

            if (lowTestMomentEventA == null) {
                lowTestMomentEventA = DEFAULT_LOW_MOMENT;
                highTestMomentEventA = DEFAULT_HIGH_MOMENT;
            }

            fitMoment = fitMwService.findByEventId(eventPair.getX().getEventId());
            refMoment = refMwService.findByEventId(eventPair.getX().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw());
                highTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw());
            } else if (refMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw());
                highTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw());
            }

            if (lowTestMomentEventB == null) {
                lowTestMomentEventB = DEFAULT_LOW_MOMENT;
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

            List<Pair<Double, double[]>> costs = new ArrayList<>();
            double[] values = new double[4];
            for (double testMoment = lowTestMomentEventA; testMoment <= highTestMomentEventA; testMoment += testMomentIncrement) {
                values[0] = testMoment;
                for (double testAppStress = lowTestAppStressMpa; testAppStress <= lowTestAppStressMpa; testAppStress += testAppStressIncrement) {
                    values[1] = testAppStress;
                    for (double testMoment_2 = lowTestMomentEventB; testMoment_2 <= highTestMomentEventB; testMoment_2 += testMomentIncrement) {
                        values[2] = testMoment_2;
                        for (double testCornerFreq_2 = lowTestAppStressMpa; testCornerFreq_2 <= lowTestAppStressMpa; testCornerFreq_2 += testAppStressIncrement) {
                            values[3] = testCornerFreq_2;
                            curCost = costFunc.value(values);
                            costs.add(new Pair<>(curCost, values.clone()));
                        }
                    }
                }
            }

            List<MomentCornerEstimate> cornerEstimates = new ArrayList<>();
            cornerEstimates.add(new MomentCornerEstimate(null, costs.get(0).getY()[0], costs.get(0).getY()[1]));
            cornerEstimates.add(new MomentCornerEstimate(null, costs.get(0).getY()[2], costs.get(0).getY()[3]));
            estimatedMomentCorners.put(eventPair, cornerEstimates);
        });

        return estimatedMomentCorners;
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
                lowTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) + DEFAULT_MOMENT_ERROR;
            } else if (refMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + DEFAULT_MOMENT_ERROR;
            }

            if (lowTestMomentEventA == null) {
                lowTestMomentEventA = DEFAULT_LOW_MOMENT;
                highTestMomentEventA = DEFAULT_HIGH_MOMENT;
            }

            fitMoment = fitMwService.findByEventId(eventPair.getX().getEventId());
            refMoment = refMwService.findByEventId(eventPair.getX().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) + DEFAULT_MOMENT_ERROR;
            } else if (refMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + DEFAULT_MOMENT_ERROR;
            }

            if (lowTestMomentEventB == null) {
                lowTestMomentEventB = DEFAULT_LOW_MOMENT;
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

            SpectraRatioPairInversionResult estimate = new SpectraRatioPairInversionResult();
            estimate.setEventIdA(eventPair.getY().getEventId())
                    .setEventIdB(eventPair.getX().getEventId())
                    .setMomentEstimateA((float) best.getPoint()[0])
                    .setCornerEstimateA((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[0]), best.getPoint()[1]))
                    .setMomentEstimateB((float) best.getPoint()[2])
                    .setCornerEstimateB((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[2]), best.getPoint()[3]))
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
                lowTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventA = MdacCalculator.mwToLogM0(fitMoment.getMw()) + DEFAULT_MOMENT_ERROR;
            } else if (refMoment != null) {
                lowTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventA = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + DEFAULT_MOMENT_ERROR;
            }

            if (lowTestMomentEventA == null) {
                lowTestMomentEventA = DEFAULT_LOW_MOMENT;
                highTestMomentEventA = DEFAULT_HIGH_MOMENT;
            }

            fitMoment = fitMwService.findByEventId(eventPair.getX().getEventId());
            refMoment = refMwService.findByEventId(eventPair.getX().getEventId());

            if (fitMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventB = MdacCalculator.mwToLogM0(fitMoment.getMw()) + DEFAULT_MOMENT_ERROR;
            } else if (refMoment != null) {
                lowTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) - DEFAULT_MOMENT_ERROR;
                highTestMomentEventB = MdacCalculator.mwToLogM0(refMoment.getRefMw()) + DEFAULT_MOMENT_ERROR;
            }

            if (lowTestMomentEventB == null) {
                lowTestMomentEventB = DEFAULT_LOW_MOMENT;
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
                                                                                   highTestAppStressMpa);

        CMAESOptimizer optimizer = new CMAESOptimizer(5000, STOP_FITNESS, true, 0, 0, new MersenneTwister(), false, new SimplePointChecker<>(0.001, 0.001, 1000000));

        PointValuePair best = optimizer.optimize(
                new MaxEval(10000000),
                    new ObjectiveFunction(costFunc),
                    GoalType.MINIMIZE,
                    new SimpleBounds(inversionLowBounds, inversionHighBounds),
                    new InitialGuess(startingPoints),
                    new CMAESOptimizer.Sigma(sigmaValues),
                    new CMAESOptimizer.PopulationSize(100));

        //We split these back out to "per-pair" measurements to report them
        //It wastes some amount of space and makes N-d plots very hard
        // but it fits into our existing plots and ways of looking at the results
        // so for now we will leave it.
        Map<EventPair, Pair<EventInversionMap, EventInversionMap>> rawEstimates = costFunc.getSamplePoints();

        Map<EventPair, SpectraRatioPairInversionResultJoint> estimatedMomentCorners = new HashMap<>();
        for (Entry<EventPair, Pair<EventInversionMap, EventInversionMap>> rawEstimate : rawEstimates.entrySet()) {
            Pair<EventInversionMap, EventInversionMap> costs = rawEstimate.getValue();
            EventPair eventPair = rawEstimate.getKey();
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
                    .setMomentEstimateB((float) best.getPoint()[eventBidx])
                    .setCornerEstimateB((float) mdacCalculator.cornerFreqFromApparentStressM0(Math.pow(10, best.getPoint()[eventBidx]), best.getPoint()[eventBidx + 1]))
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

            return sum;
        }

        public Pair<EventInversionMap, EventInversionMap> getSamplePoints() {
            return costs;
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

        public SpectraRatioCostFunctionJoint(Map<EventPair, Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>>> ratioData, Map<String, Integer> eventIndexMap,
                BiFunction<Double, Double, Double> stressFunc, double m0minX, double m0maxX, double m0minY, double m0maxY, double appStressMinX, double appStressMaxX, double appStressMinY,
                double appStressMaxY) {
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
        }

        @Override
        public double value(double[] point) {
            float sum = 0f;

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
