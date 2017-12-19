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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.PowellOptimizer;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariateOptimizer;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.apache.commons.math3.stat.StatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.PathCalibrationMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.PathCostFunctionResult;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.WaveformUtils;
import llnl.gnem.core.util.Geometry.EModel;

@Service
@Transactional
public class Joint1DPathCorrection implements PathCalibrationService {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private SpectraCalculator spectraCalc;

    // How many different randomized starting parameter guesses to use when
    // trying to optimize
    private static final int STARTING_POINTS = 5;

    // Optimization terms common to a frequency band fit: p1,p2,q,xcross,xtrans
    private static final int NUM_TERMS = 4;
    private static final int P1_IDX = 0;
    private static final int Q_IDX = 1;
    private static final int XCROSS_IDX = 2;
    private static final int XTRANS_IDX = 3;
    private static final double TOLERANCE = 0.001;
    private static final Double EPS_DEFAULT = 5.0;

    private static final double XTRANS_MAX = 0.04;
    private static final double XTRANS_MIN = -10.0;
    private static final double XCROSS_MAX = 3.0;
    private static final double XCROSS_MIN = 0.0;
    private static final double Q_MAX = 3.0;
    private static final double Q_MIN = 0.0;
    private static final double P1_MIN = -10.0;
    private static final double P1_MAX = -0.001;
    private static final double SITE_MAX = 10.0;
    private static final double SITE_MIN = -10.0;

    // Evids actually potentially need meta-data about p1,p2,q,xt,xc,etc per
    // evid in the future but for now they are common values for 1D.
    private double p1 = Math.log10(0.0001);
    private double p2 = 1.0;
    private double q = Math.log10(500.0);
    private double xtrans = Math.log10(Math.log10(2.0));
    private double xcross = Math.log10(500.0);
    private double vphase = 3.5;

    private Map<FrequencyBand, Double> epsMap = new HashMap<>();

    private final double efact = Math.log10(Math.E);
    private PathCalibrationMeasurementService pathCalibrationMeasurementService;

    @Autowired
    public Joint1DPathCorrection(SpectraCalculator spectraCalc, PathCalibrationMeasurementService pathCalibrationMeasurementService) {
        this.spectraCalc = spectraCalc;
        this.pathCalibrationMeasurementService = pathCalibrationMeasurementService;

        // FIXME: Accept this as input
        this.epsMap.put(new FrequencyBand(0.02, 0.03), 8.0);
        this.epsMap.put(new FrequencyBand(0.03, 0.05), 8.0);
        this.epsMap.put(new FrequencyBand(0.05, 0.1), 5.0);
        this.epsMap.put(new FrequencyBand(0.1, 0.2), 5.0);
        this.epsMap.put(new FrequencyBand(0.2, 0.3), 5.0);
        this.epsMap.put(new FrequencyBand(0.3, 0.5), 5.0);
        this.epsMap.put(new FrequencyBand(0.5, 0.7), 5.5);
        this.epsMap.put(new FrequencyBand(0.7, 1.0), 5.8);
        this.epsMap.put(new FrequencyBand(1.0, 1.5), 5.6);
        this.epsMap.put(new FrequencyBand(1.5, 2.0), 5.4);
        this.epsMap.put(new FrequencyBand(2.0, 3.0), 5.5);
        this.epsMap.put(new FrequencyBand(3.0, 4.0), 5.3);
        this.epsMap.put(new FrequencyBand(4.0, 6.0), 6.3);
        this.epsMap.put(new FrequencyBand(6.0, 8.0), 6.3);
        this.epsMap.put(new FrequencyBand(8.0, 10.0), 6.0);
        this.epsMap.put(new FrequencyBand(10.0, 15.0), 2.0);
    }

    @Override
    public Map<FrequencyBand, SharedFrequencyBandParameters> measurePathCorrections(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand,
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters) {

        List<PathCalibrationMeasurement> measurements = new ArrayList<>();
        Map<FrequencyBand, SharedFrequencyBandParameters> pathCorrectedFrequencyBandParameters = new HashMap<>();

        Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> dataMappedToEventAndStation = removeSingleStationOrFewerEntries(mapToEventAndStation(dataByFreqBand));

        for (Entry<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParams : frequencyBandParameters.entrySet()) {

            SharedFrequencyBandParameters pathCorrectedParams = frequencyBandParams.getValue();

            Map<Station, Integer> eventCountByStation = new HashMap<>();
            Set<Station> stations = new HashSet<>();
            Map<Station, Integer> stationIdxMap = new HashMap<>();
            Map<Event, Map<Station, Double>> dataMap = new HashMap<>();
            Map<Event, Map<Station, Double>> distanceMap = new HashMap<>();
            FrequencyBand frequencyBand = frequencyBandParams.getKey();

            Map<Event, Map<Station, SpectraMeasurement>> freqBandData = dataMappedToEventAndStation.get(frequencyBand);

            if (freqBandData != null) {
                // Avoiding divide by zero as this is only ever used for
                // residual calculations
                double totalDataCount = 1.0;
                for (Entry<Event, Map<Station, SpectraMeasurement>> eventEntry : freqBandData.entrySet()) {
                    Event event = eventEntry.getKey();
                    for (Entry<Station, SpectraMeasurement> stationEntry : eventEntry.getValue().entrySet()) {
                        SpectraMeasurement spectra = stationEntry.getValue();
                        Station station = stationEntry.getKey();

                        if (!dataMap.containsKey(event)) {
                            dataMap.put(event, new HashMap<Station, Double>());
                            distanceMap.put(event, new HashMap<Station, Double>());
                        }
                        if (dataMap.get(event).containsKey(station)) {
                            log.info("Saw same evid/sta pair more than once {} {}", event.getEventId(), station.getStationName());
                        }

                        if (!eventCountByStation.containsKey(station)) {
                            eventCountByStation.put(station, 0);
                        }
                        dataMap.get(event).put(station, spectra.getRawAtMeasurementTime());
                        distanceMap.get(event).put(station, EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude()));

                        stations.add(station);
                        eventCountByStation.put(station, eventCountByStation.get(station) + 1);
                        totalDataCount++;
                    }
                }

                // Common terms + one site term for each station
                double[] optimizationParams = new double[NUM_TERMS + stations.size()];
                double[] optimizationLowBounds = new double[NUM_TERMS + stations.size()];
                double[] optimizationHighBounds = new double[NUM_TERMS + stations.size()];

                int optIdx = NUM_TERMS;
                for (Station stationName : stations) {
                    stationIdxMap.put(stationName, optIdx);
                    optIdx++;
                }

                // FIXME: These shouldn't be hardcoded
                optimizationParams[P1_IDX] = p1;
                optimizationLowBounds[P1_IDX] = P1_MIN;
                optimizationHighBounds[P1_IDX] = P1_MAX;

                optimizationParams[Q_IDX] = q;
                optimizationLowBounds[Q_IDX] = Q_MIN;
                optimizationHighBounds[Q_IDX] = Q_MAX;

                optimizationParams[XCROSS_IDX] = xcross;
                optimizationLowBounds[XCROSS_IDX] = XCROSS_MIN;
                optimizationHighBounds[XCROSS_IDX] = XCROSS_MAX;

                optimizationParams[XTRANS_IDX] = xtrans;
                optimizationLowBounds[XTRANS_IDX] = XTRANS_MIN;
                optimizationHighBounds[XTRANS_IDX] = XTRANS_MAX;

                for (int i = 4; i < optimizationParams.length; i++) {
                    optimizationParams[i] = 0.0;
                    optimizationLowBounds[i] = SITE_MIN;
                    optimizationHighBounds[i] = SITE_MAX;
                }

                // starting L1.2 residual
                Double initialResidual = Math.pow(costFunction(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand, optimizationParams) / totalDataCount, (1.0 / 1.2));

                PointValuePair optimizedResult = IntStream.range(0, STARTING_POINTS).parallel().mapToObj(i -> {
                    ConvergenceChecker<PointValuePair> convergenceChecker = new SimpleValueChecker(TOLERANCE, TOLERANCE);
                    PowellOptimizer optimizer = new PowellOptimizer(TOLERANCE, TOLERANCE, convergenceChecker);
                    MultivariateFunction prediction = new ESHPathMultivariate(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand);
                    PointValuePair opt = null;
                    try {
                        opt = optimizer.optimize(new MaxEval(2000000),
                                                 new ObjectiveFunction(prediction),
                                                 GoalType.MINIMIZE,
                                                 new InitialGuess(perturbParams(optimizationLowBounds, optimizationHighBounds)));
                    } catch (TooManyEvaluationsException e) {
                    }
                    return opt;
                }).filter(Objects::nonNull).reduce((a, b) -> Double.compare(a.getValue(), b.getValue()) <= 0 ? a : b).orElse(null);

                if (optimizedResult == null) {
                    //FIXME: Return an error to the client and stop the calibration
                    log.error("Unable to converge while optimizing for {}", frequencyBand);
                } else {
                    optimizationParams = optimizedResult.getPoint();
                }

                // final L1.2 residual
                PathCostFunctionResult finalResults = costFunctionFull(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand, optimizationParams);
                Double finalResidual = Math.pow(finalResults.getCost() / totalDataCount, (1.0 / 1.2));

                PathCalibrationMeasurement measurement = new PathCalibrationMeasurement();
                measurement.setInitialResidual(initialResidual);
                measurement.setFinalResidual(finalResidual);
                measurement.setFrequencyBand(frequencyBand);
                measurements.add(measurement);

                pathCorrectedParams.setS1(Math.pow(10.0, optimizationParams[P1_IDX]));
                pathCorrectedParams.setS2(p2);
                pathCorrectedParams.setQ(Math.pow(10.0, optimizationParams[Q_IDX]));
                pathCorrectedParams.setXc(Math.pow(10.0, optimizationParams[XCROSS_IDX]));
                pathCorrectedParams.setXt(Math.pow(10.0, Math.pow(10.0, optimizationParams[XTRANS_IDX])));
                pathCorrectedFrequencyBandParameters.put(frequencyBand, pathCorrectedParams);
            }
        }

        pathCalibrationMeasurementService.deleteAll();
        pathCalibrationMeasurementService.save(measurements);

        return pathCorrectedFrequencyBandParameters;
    }

    private Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> mapToEventAndStation(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand) {
        Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> data = new HashMap<>();
        if (dataByFreqBand != null) {

            for (Entry<FrequencyBand, List<SpectraMeasurement>> entries : dataByFreqBand.entrySet()) {
                if (!data.containsKey(entries.getKey())) {
                    data.put(entries.getKey(), new HashMap<>());
                }
                Map<Event, Map<Station, SpectraMeasurement>> eventMap = data.get(entries.getKey());
                for (SpectraMeasurement entry : entries.getValue()) {
                    if (WaveformUtils.isValidWaveform(entry.getWaveform())) {
                        Event event = entry.getWaveform().getEvent();
                        Station station = entry.getWaveform().getStream().getStation();
                        if (!eventMap.containsKey(event)) {
                            eventMap.put(event, new HashMap<>());
                        }
                        Map<Station, SpectraMeasurement> stationMap = eventMap.get(event);
                        stationMap.put(station, entry);
                    }
                }
            }
        }
        return data;
    }

    private class ESHPathMultivariate implements MultivariateFunction {
        private Map<Event, Map<Station, SpectraMeasurement>> freqBandData;
        private Map<Event, Map<Station, Double>> dataMap;
        private Map<Event, Map<Station, Double>> distanceMap;
        private FrequencyBand frequencyBand;
        private Map<Station, Integer> stationIdxMap;

        public ESHPathMultivariate(Map<Event, Map<Station, SpectraMeasurement>> freqBandData, Map<Event, Map<Station, Double>> dataMap, Map<Event, Map<Station, Double>> distanceMap,
                Map<Station, Integer> stationIdxMap, FrequencyBand frequencyBand) {
            this.freqBandData = freqBandData;
            this.dataMap = dataMap;
            this.distanceMap = distanceMap;
            this.frequencyBand = frequencyBand;
            this.stationIdxMap = stationIdxMap;
        }

        @Override
        public double value(double[] point) {
            return costFunction(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand, point);
        }
    }

    private Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> removeSingleStationOrFewerEntries(Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> freqBandData) {
        Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> localData = new HashMap<>();
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> frequencyEntry : freqBandData.entrySet()) {
            FrequencyBand frequency = frequencyEntry.getKey();
            for (Event evid : frequencyEntry.getValue().keySet()) {
                if (freqBandData.get(frequency).get(evid).size() > 1) {
                    if (!localData.containsKey(frequency)) {
                        localData.put(frequency, new HashMap<Event, Map<Station, SpectraMeasurement>>());
                    }
                    localData.get(frequency).put(evid, freqBandData.get(frequency).get(evid));
                }
            }
        }
        return localData;
    }

    /**
     * L1.2 cost function for use in optimization code. Extended Street-Herrmann
     * spreading model, no Q.
     */
    public double costFunction(Map<Event, Map<Station, SpectraMeasurement>> evidStaData, Map<Event, Map<Station, Double>> dataMap, Map<Event, Map<Station, Double>> distanceMap,
            Map<Station, Integer> stationIdxMap, FrequencyBand frequencyBand, double[] optimizationParams) {
        return costFunctionFull(evidStaData, dataMap, distanceMap, stationIdxMap, frequencyBand, optimizationParams).getCost();
    }

    public PathCostFunctionResult costFunctionFull(Map<Event, Map<Station, SpectraMeasurement>> evidStaData, Map<Event, Map<Station, Double>> dataMap, Map<Event, Map<Station, Double>> distanceMap,
            Map<Station, Integer> stationIdxMap, FrequencyBand frequencyBand, double[] optimizationParams) {

        Map<Event, Map<Station, Double>> localDataMap = new HashMap<>();
        Map<Event, Map<Station, Double>> residuals = new HashMap<>();
        Map<Event, Map<Station, Double>> siteCorrections = new HashMap<>();

        double rsum = 0.0;
        double f0 = Math.sqrt(frequencyBand.getLowFrequency() * frequencyBand.getHighFrequency());

        for (Entry<Event, Map<Station, SpectraMeasurement>> evidEntry : evidStaData.entrySet()) {
            Event evid = evidEntry.getKey();
            Map<Station, SpectraMeasurement> stationMapData = evidEntry.getValue();

            List<Double> dataVec = new ArrayList<>(optimizationParams.length);
            for (Entry<Station, SpectraMeasurement> entry : stationMapData.entrySet()) {
                double del = distanceMap.get(evid).get(entry.getKey());
                double site = optimizationParams[stationIdxMap.get(entry.getKey())];
                double p1 = Math.pow(10.0, optimizationParams[P1_IDX]);
                double q = Math.pow(10.0, optimizationParams[Q_IDX]);
                double xcross = Math.pow(10.0, optimizationParams[XCROSS_IDX]);
                double xtrans = Math.pow(10.0, Math.pow(10.0, optimizationParams[XTRANS_IDX]));
                double pdat = site + spectraCalc.log10ESHcorrection(p1, p2, xcross, xtrans, del) - del * Math.PI * f0 * efact / (q * vphase);
                double adjustedVal = dataMap.get(evid).get(entry.getKey()) - pdat;
                dataVec.add(adjustedVal);
                if (!localDataMap.containsKey(evid)) {
                    localDataMap.put(evid, new HashMap<Station, Double>());
                }
                localDataMap.get(evid).put(entry.getKey(), adjustedVal);

                if (!siteCorrections.containsKey(evid)) {
                    siteCorrections.put(evid, new HashMap<Station, Double>());
                }
                siteCorrections.get(evid).put(entry.getKey(), site);
            }

            if (dataVec.size() > 1) {
                double dmed = lpMean(ArrayUtils.toPrimitive(dataVec.toArray(new Double[0])));
                for (Entry<Station, SpectraMeasurement> entry : stationMapData.entrySet()) {
                    rsum = rsum + Math.pow(Math.abs(localDataMap.get(evid).get(entry.getKey()) - dmed), 1.2);
                    if (!residuals.containsKey(evid)) {
                        residuals.put(evid, new HashMap<Station, Double>());
                    }
                    residuals.get(evid).put(entry.getKey(), localDataMap.get(evid).get(entry.getKey()) - dmed);
                }
            }
        }

        List<Double> dataVec = new ArrayList<>(optimizationParams.length);
        for (int i = 0; i < optimizationParams.length - NUM_TERMS; i++) {
            dataVec.add(optimizationParams[i + NUM_TERMS]);
        }

        double smed = lpMean(ArrayUtils.toPrimitive(dataVec.toArray(new Double[0])));

        Double eps = epsMap.get(frequencyBand);
        if (eps == null) {
            eps = EPS_DEFAULT;
        }

        rsum = rsum + smed * smed * eps * ((double) dataVec.size()) * 10.0;

        for (int i = 0; i < optimizationParams.length - NUM_TERMS; i++) {
            // range penalties
            double xlog = optimizationParams[P1_IDX];
            if (xlog > P1_MAX) {
                rsum = rsum + 1000.0 * Math.pow(xlog - P1_MAX, 2);
            } else if (xlog < P1_MIN) {
                rsum = rsum + 1000.0 * Math.pow(P1_MIN - xlog, 2);
            }

            xlog = optimizationParams[Q_IDX];
            if (xlog > Q_MAX) {
                rsum = rsum + 1000.0 * Math.pow(xlog - Q_MAX, 2);
            } else if (xlog < Q_MIN) {
                rsum = rsum + 1000.0 * Math.pow(-Q_MIN - xlog, 2);
            }

            xlog = optimizationParams[XCROSS_IDX];
            if (xlog > XCROSS_MAX) {
                rsum = rsum + 1000.0 * Math.pow(xlog - XCROSS_MAX, 2);
            } else if (xlog < XCROSS_MIN) {
                rsum = rsum + 1000.0 * Math.pow(XCROSS_MIN - xlog, 2);
            }

            xlog = optimizationParams[XTRANS_IDX];
            if (xlog > XTRANS_MAX) {
                rsum = rsum + 1000.0 * Math.pow(xlog - XTRANS_MAX, 2);
            } else if (xlog < XTRANS_MIN) {
                rsum = rsum + 1000.0 * Math.pow(XTRANS_MIN - xlog, 2);
            }

            // site range penalties -10 to 10
            double site = optimizationParams[i];
            if (site > SITE_MAX) {
                rsum = rsum + 1000.0 * Math.pow(site - SITE_MAX, 2);
            } else if (site < SITE_MIN) {
                rsum = rsum + 1000.0 * Math.pow(SITE_MIN - site, 2);
            }
        }

        return new PathCostFunctionResult(residuals, rsum, smed, siteCorrections);
    }

    private double lpMean(final double[] values) {
        double mean = 0.0;
        double xmin = StatUtils.min(values);
        double xmax = StatUtils.max(values);
        double xstart = (xmin + xmax) / 2.0;
        if (values.length < 2) {
            mean = xmin;
        } else if (values.length == 2 || xmin == xmax) {
            mean = xstart;
        } else {
            UnivariateOptimizer optimizer = new BrentOptimizer(1e-10, 1e-14);
            UnivariatePointValuePair result = optimizer.optimize(new UnivariateObjectiveFunction(x -> xlpSum(values, x)),
                                                                 new MaxEval(1000000),
                                                                 GoalType.MINIMIZE,
                                                                 new SearchInterval(xmin, xmax, xstart));
            mean = result.getPoint();
        }
        return mean;
    }

    private double xlpSum(double[] x, double x0) {
        double sum = 0.0;
        for (int i = 0; i < x.length; i++) {
            sum = sum + Math.pow(Math.abs(x[i] - x0), 1.2);
        }
        return sum;
    }

    private double[] perturbParams(double[] optimizationLowBounds, double[] optimizationHighBounds) {
        double[] newParams = new double[optimizationLowBounds.length];

        for (int i = 0; i < newParams.length; i++) {
            if (optimizationLowBounds[i] < 0.0) {
                newParams[i] = RandomUtils.nextDouble(0.0, optimizationHighBounds[i] + Math.abs(optimizationLowBounds[i])) + optimizationLowBounds[i];
            } else {
                newParams[i] = RandomUtils.nextDouble(optimizationLowBounds[i], optimizationHighBounds[i]);
            }
        }

        return newParams;
    }

}