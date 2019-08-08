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
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PathCalibrationMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationMeasurementService;
import gov.llnl.gnem.apps.coda.calibration.service.api.PathCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.Geometry.EModel;

@Service
@Transactional
public class Joint1DPathCorrection implements PathCalibrationService {

    private static final Logger log = LoggerFactory.getLogger(Joint1DPathCorrection.class);

    private SpectraCalculator spectraCalc;

    // Optimization terms common to a frequency band fit: p1,p2,q,xcross,xtrans
    private static final int NUM_TERMS = 4;
    private static final int P1_IDX = 0;
    private static final int Q_IDX = 1;
    private static final int XCROSS_IDX = 2;
    private static final int XTRANS_IDX = 3;
    private static final double TOLERANCE = 1E-10;

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

    private static final int POP_SIZE = 20;

    // Evids actually potentially need meta-data about p1,p2,q,xt,xc,etc per
    // evid in the future but for now they are common values for 1D.
    private double p1 = Math.log10(0.0001);
    private double p2 = 1.0;
    private double q = Math.log10(500.0);
    private double xtrans = Math.log10(Math.log10(2.0));
    private double xcross = Math.log10(500.0);

    @Value("#{'${path.phase-speed-kms:${phase.phase-speed-kms:${phase-speed-kms:3.5}}}'}")
    private double vphase;

    private static final double efact = Math.log10(Math.E);
    private PathCalibrationMeasurementService pathCalibrationMeasurementService;
    private List<double[]> paramPoints;
    @Value(value = "${path.use-aggressive-opt:true}")
    private boolean agressiveOptimization;

    @Autowired
    public Joint1DPathCorrection(SpectraCalculator spectraCalc, PathCalibrationMeasurementService pathCalibrationMeasurementService) {
        this.spectraCalc = spectraCalc;
        this.pathCalibrationMeasurementService = pathCalibrationMeasurementService;
    }

    @Override
    public Map<FrequencyBand, SharedFrequencyBandParameters> measurePathCorrections(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand,
            Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters, VelocityConfiguration velConf) {

        if (velConf != null) {
            Double phase = velConf.getPhaseSpeedInKms();
            if (phase != null && phase != 0.0) {
                vphase = phase;
            }
        }

        List<PathCalibrationMeasurement> measurements = new ArrayList<>();
        Map<FrequencyBand, SharedFrequencyBandParameters> pathCorrectedFrequencyBandParameters = new HashMap<>();

        Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> dataMappedToEventAndStation = removeSingleStationOrFewerEntries(mapToEventAndStation(dataByFreqBand));

        frequencyBandParameters.entrySet().parallelStream().forEach(frequencyBandParams -> {

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
                final double totalDataCount;
                long dataCount = 0l;
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
                        dataCount++;
                    }
                }
                dataCount = dataCount > 0 ? dataCount : 1;
                totalDataCount = dataCount;

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

                double[] sigmaArray = new double[optimizationParams.length];
                for (int i = 0; i < sigmaArray.length; i++) {
                    sigmaArray[i] = 0.5;
                }

                // starting residual
                Double initialResidual = Math.pow(costFunction(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand, optimizationParams) / totalDataCount, 2.0);
                log.debug("Band {} initial cost: {}", frequencyBand.getLowFrequency(), initialResidual);

                List<double[]> paramPoints = makeParamPoints(NUM_TERMS, agressiveOptimization, optimizationLowBounds, optimizationHighBounds);
                PointValuePair optimizedResult = IntStream.range(0, paramPoints.size()).parallel().mapToObj(i -> {
                    ConvergenceChecker<PointValuePair> convergenceChecker = new SimpleValueChecker(TOLERANCE, TOLERANCE);
                    CMAESOptimizer optimizer = new CMAESOptimizer(1000000, TOLERANCE, true, 0, 10, new MersenneTwister(), true, convergenceChecker);

                    MultivariateFunction prediction = new ESHPathMultivariate(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand);
                    PointValuePair opt = null;
                    try {
                        opt = optimizer.optimize(
                                new MaxEval(1000000),
                                    new ObjectiveFunction(prediction),
                                    GoalType.MINIMIZE,
                                    new SimpleBounds(optimizationLowBounds, optimizationHighBounds),
                                    new InitialGuess(paramPoints.get(i)),
                                    new CMAESOptimizer.PopulationSize(POP_SIZE),
                                    new CMAESOptimizer.Sigma(sigmaArray));
                    } catch (TooManyEvaluationsException e) {
                    }
                    log.debug("frequency: {}, iteration: {}, residual: {}", frequencyBand.getLowFrequency(), i, opt.getValue());
                    return opt;
                }).filter(Objects::nonNull).reduce((a, b) -> Double.compare(a.getValue(), b.getValue()) <= 0 ? a : b).orElse(null);

                if (optimizedResult == null) {
                    //FIXME: Return an error to the client and stop the calibration
                    log.error("Unable to converge while optimizing for {}", frequencyBand);
                } else {
                    optimizationParams = optimizedResult.getPoint();
                }

                // final residual
                Double finalResults = costFunction(freqBandData, dataMap, distanceMap, stationIdxMap, frequencyBand, optimizationParams);
                Double finalResidual = Math.pow(finalResults / totalDataCount, 2.0);
                log.debug("Band {} final cost: {}", frequencyBand.getLowFrequency(), finalResidual);

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
        });

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
     * cost function for use in optimization code. Extended Street-Herrmann
     * spreading model, no Q.
     */
    public double costFunction(Map<Event, Map<Station, SpectraMeasurement>> evidStaData, Map<Event, Map<Station, Double>> dataMap, Map<Event, Map<Station, Double>> distanceMap,
            Map<Station, Integer> stationIdxMap, FrequencyBand frequencyBand, double[] optimizationParams) {
        Map<Event, Map<Station, Double>> localDataMap = new HashMap<>();

        double cost = 0.0;
        double freq0 = Math.sqrt(frequencyBand.getLowFrequency() * frequencyBand.getHighFrequency());

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
                double pdat = site + spectraCalc.log10ESHcorrection(p1, p2, xcross, xtrans, del) - del * Math.PI * freq0 * efact / (q * vphase);
                double adjustedVal = dataMap.get(evid).get(entry.getKey()) - pdat;
                dataVec.add(adjustedVal);
                if (!localDataMap.containsKey(evid)) {
                    localDataMap.put(evid, new HashMap<Station, Double>());
                }
                localDataMap.get(evid).put(entry.getKey(), adjustedVal);
            }

            if (dataVec.size() > 1) {
                double huberDel = .5d;
                double median = new DescriptiveStatistics(ArrayUtils.toPrimitive(dataVec.toArray(new Double[0]))).getPercentile(50d);
                for (Entry<Station, SpectraMeasurement> entry1 : stationMapData.entrySet()) {
                    double diff = Math.abs(localDataMap.get(evid).get(entry1.getKey()) - median);
                    cost = cost + (Math.pow(huberDel, 2.0) + (Math.sqrt(1d + Math.pow(diff / huberDel, 2.0)) - 1d));
                }
            }
        }
        return cost;

    }

    /**
     * @param numberOfTerms
     * @param optimizationBounds
     *            I'm making the pretty big assumption these are ordered such
     *            that bound0 is the min and boundN is the max.
     * @return
     * @throws IllegalStateException
     *             if optimizationBounds.length < 2
     */
    private List<double[]> makeParamPoints(int numberOfTerms, boolean agressiveOptimization, double[]... optimizationBounds) {
        if (paramPoints != null && !paramPoints.isEmpty() && paramPoints.get(0).length == numberOfTerms) {
            return paramPoints;
        }
        if (optimizationBounds.length < 2) {
            throw new IllegalStateException("Optmization bounds needs at least two entries for the low and high boundary conditions. Got: " + optimizationBounds.length);
        }

        int terms = (int) (Math.pow(numberOfTerms, optimizationBounds.length)) + 1;
        List<double[]> params = new ArrayList<>(terms);
        int[] selectionIndex = new int[numberOfTerms];
        double[] param = optimizationBounds[0].clone();

        for (int j = 0; j < numberOfTerms; j++) {
            param[j] = optimizationBounds[0][j] + (optimizationBounds[optimizationBounds.length - 1][j] - optimizationBounds[0][j]) / 2;
        }

        params.add(param.clone());
        param = optimizationBounds[0].clone();

        if (agressiveOptimization) {
            for (int i = 1; i < terms; i++) {
                for (int j = 0; j < selectionIndex.length; j++) {
                    param[j] = optimizationBounds[selectionIndex[j]][j];
                }
                params.add(param.clone());
                param = optimizationBounds[0].clone();
                selectionIndex = nextIndex(selectionIndex, optimizationBounds.length);
            }
        } else {
            params.add(param);
            param = optimizationBounds[optimizationBounds.length - 1].clone();
            params.add(param);
        }

        paramPoints = params;
        return paramPoints;
    }

    private int[] nextIndex(int[] selectionIndex, int base) {
        int[] index = selectionIndex.clone();
        for (int i = 0; i < index.length; i++) {
            index[i]++;
            if (index[i] % base == 0) {
                index[i] = 0;
            } else {
                break;
            }
        }
        return index;
    }
}