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
package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.repository.SharedFrequencyBandParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.ServiceConfig;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.test.annotations.IntTest;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;

@IntTest
public class SpectraCalculatorTest {

    private static final Logger log = LoggerFactory.getLogger(SpectraCalculatorTest.class);

    @Mock
    private MeasuredMwsService measuredMwsService;

    @Mock
    private SiteFrequencyBandParametersService siteParamsService;

    private WaveformToTimeSeriesConverter converter = new WaveformToTimeSeriesConverter();

    @Mock
    private SyntheticCodaModel syntheticCodaModel;

    @Autowired
    private MdacParametersFiService mdacFiService;

    @Autowired
    private MdacParametersPsService mdacPsService;

    @InjectMocks
    private VelocityConfiguration velConf;

    @Mock
    private ServiceConfig svcConf;

    @Mock
    private SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository;

    @Autowired
    private SpectraCalculator spectraCalc;

    @Autowired
    private MdacCalculatorService mdacService;

    private MdacParametersPS mdacPs;

    private MdacParametersFI mdacFi;

    private PICK_TYPES phase = PICK_TYPES.LG;

    private double RHO = 2700.0;
    private double BETA = 3500.0;

    @BeforeEach
    protected void setUp() throws Exception {
        velConf = new VelocityConfiguration();
        velConf.setPhaseVelocityInKms(0.0);

        mdacPs = new MdacParametersPS(); // Populate using sql value const
        mdacPs.setId(0L);
        mdacPs.setDelEta(0.0);
        mdacPs.setDelGamma0(0.0);
        mdacPs.setDelQ0(0.0);
        mdacPs.setDistCrit(0.001);
        mdacPs.setEta(1.1);
        mdacPs.setGamma0(0.65);
        mdacPs.setPhase("Lg");
        mdacPs.setQ0(210.0);
        mdacPs.setSnr(2.0);
        mdacPs.setU0(7900.0);

        mdacFi = new MdacParametersFI(); // Use for mdacFunc params;
        mdacFi.setId(0L);
        mdacFi.setAlphaR(6000.0);
        mdacFi.setAlphas(6000.0);
        mdacFi.setBetas(BETA);
        mdacFi.setBetaR(BETA);
        mdacFi.setDelPsi(0.0);
        mdacFi.setDelSigma(0.0);
        mdacFi.setM0ref(1000000000);
        mdacFi.setPsi(0.25);
        mdacFi.setRadPatP(0.44);
        mdacFi.setRadPatS(0.6);
        mdacFi.setRhor(RHO);
        mdacFi.setRhos(RHO);
        mdacFi.setSigma(0.3);
        mdacFi.setZeta(1);

        mdacService = new MdacCalculatorService();
        spectraCalc = new SpectraCalculator(converter, syntheticCodaModel, mdacService, mdacFiService, mdacPsService, velConf);
    }

    class TestInput {
        SortedMap<FrequencyBand, SummaryStatistics> testMeasurements;
        double apparentStress;
        double mwMDAC;
        double energy;
        int sampleCount;

        /**
         * Generate test input object using synthetic data generated from
         * parameters.
         *
         * @param mwValue
         * @param sigmaValue
         * @param startFreq
         * @param endFreq
         * @param freqCount
         * @param appStress
         */
        TestInput(double mwValue, double appStress, double startFreq, double endFreq, int freqCount) {
            // Takes center freq and gives amp
            final DoubleUnaryOperator mdacFunc = mdacService.getCalculateMdacAmplitudeForMwFunction(mdacPs, mdacFi, mwValue, phase, appStress);

            final double expectedEnergy = mdacService.getEnergy(mwValue, appStress, mdacPs, mdacFi);

            // Loop through frequencies to generate amplitudes
            final double stepSize = ((Math.log10(endFreq) - Math.log10(startFreq)) / (freqCount - 1));
            final double logStart = Math.log10(startFreq);

            double[][] data = new double[freqCount][2];

            for (int i = 0; i < freqCount; i++) {
                // Get the centerFreq
                data[i][0] = Math.pow(10, logStart + (i * stepSize));
                // Get the amplitude
                data[i][1] = mdacFunc.applyAsDouble(data[i][0]);
            }

            this.testMeasurements = createMeasurementsFromData(data, startFreq, endFreq);
            this.sampleCount = freqCount;
            this.apparentStress = appStress;
            this.mwMDAC = mwValue;
            this.energy = Math.log10(expectedEnergy);
        }

        /**
         * Creates test input object using test data provided as 2D array of
         * size N. Expected data is in the form: [[center frequency 0, amplitude
         * 0],[center frequency 1, amplitude 1],...,frequency N, amplitude N]]
         *
         * @param testData
         * @param logMoment
         * @param appStress
         * @param energy
         */
        TestInput(double[][] testData, double logMoment, double appStress, double energy) {
            this.testMeasurements = createMeasurementsFromData(testData, testData[0][0], testData[testData.length - 1][0]);
            this.apparentStress = appStress;
            /**
             * logMoment is calculated in calculateTotalEnergyInfo using mwMDAC
             * like this: final double energyConstMKS = 9.1 / 1.5; double
             * logMomentMDAC = 1.5 * (mwMDAC + energyConstMKS); // Log10(moment)
             * Since we have logMoment to start with as input, we solve for
             * mwMDAC so it can be passed to our function: for testing: mwMDAC =
             * logMoment / 1.5 - 9.1 / 1.5 -> (logMoment - 9.1) / 1.5
             */
            this.sampleCount = this.testMeasurements.size();
            this.mwMDAC = (logMoment - 9.1) / 1.5;
            this.energy = energy;
        }

        public SortedMap<FrequencyBand, SummaryStatistics> getTestMeasurements() {
            return testMeasurements;
        }

        public double getApparentStress() {
            return apparentStress;
        }

        public double getMwMDAC() {
            return mwMDAC;
        }

        public double getEnergy() {
            return energy;
        }

        public int getSamplesTaken() {
            return sampleCount;
        }

        private final SortedMap<FrequencyBand, SummaryStatistics> createMeasurementsFromData(double[][] testData, double startFreq, double endFreq) {
            SortedMap<FrequencyBand, SummaryStatistics> measurements = new TreeMap<>();

            for (int i = 1; i < testData.length; i++) {
                FrequencyBand freqBand = new FrequencyBand();
                SummaryStatistics stats = new SummaryStatistics();
                double delta = (testData[i][0] - testData[i - 1][0]) / 2.0;
                freqBand.setLowFrequency(Math.max(testData[i][0] - delta, startFreq));
                freqBand.setHighFrequency(Math.min(testData[i][0] + delta, endFreq));
                stats.addValue(testData[i][1]);

                measurements.put(freqBand, stats);
            }

            FrequencyBand freqBand = new FrequencyBand();
            SummaryStatistics stats = new SummaryStatistics();
            double delta = (testData[1][0] - testData[0][0]) / 2.0;
            freqBand.setLowFrequency(Math.max(testData[0][0] - delta, startFreq));
            freqBand.setHighFrequency(Math.min(testData[0][0] + delta, endFreq));
            stats.addValue(testData[0][1]);
            measurements.put(freqBand, stats);

            return measurements;
        }
    }

    /**
     * This will use custom test data to test the energy calculation results and
     * compare their accuracy with the actual expected output values.
     *
     * @param mw
     * @param appStress
     * @param startFreq
     * @param endFreq
     */
    public void testCalcTotalEnergyOutput(double[][] data, double logMoment, double appStress, double energy) {
        TestInput testInput = new TestInput(data, logMoment, appStress, energy);

        log.info(
                "CalcTotalEnergy Results (using custom data):\nMw: {}, apparent stress: {}, center frequency range: {} to {}, expected energy: ",
                    testInput.mwMDAC,
                    appStress,
                    data[0][0],
                    data[data.length - 1][0],
                    testInput.getEnergy());

        EnergyInfo testResult = spectraCalc.calcTotalEnergyInfo(
                new TreeMap<FrequencyBand, Double>(testInput.testMeasurements.entrySet()
                                                                             .stream()
                                                                             .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getMean()))
                                                                             .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))),
                    testInput.mwMDAC,
                    testInput.apparentStress,
                    mdacFi);
        double energyRatio = testResult.getLogTotalEnergy() / testInput.getEnergy();
        double errorPercent = Math.abs(energyRatio - 1) * 100;

        double diff = testResult.getLogTotalEnergy() - testInput.getEnergy();
        log.info(
                "Result ({} samples): {}, Diff: {}, % off: {}. App Stress={}",
                    testInput.getSamplesTaken(),
                    testResult.getLogTotalEnergy(),
                    String.format("%f", diff),
                    String.format("%f", errorPercent),
                    testResult.getObsApparentStress());

        double errorTolerance = 4.0;
        log.info("\nErr%: {}", errorPercent);
        log.info("{}", StringUtils.repeat("-", 100));
        assert (errorPercent < errorTolerance); // Make sure percent error is less that tolerance
    }

    /**
     * This will use synthetic data to test the energy calculation results and
     * compare their accuracy with the actual expected output values.
     *
     * @param mw
     * @param appStress
     * @param startFreq
     * @param endFreq
     */
    public void testCalcTotalEnergyOutput(double mw, double appStress, double startFreq, double endFreq) {
        List<TestInput> testInput = new ArrayList<>();

        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 10));
        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 100));
        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 1000));
        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 100000));

        log.info("CalcTotalEnergy Results:\nMw: {}, apparent stress: {}, frequency range: {} to {}, expected energy: {}", mw, appStress, startFreq, endFreq, testInput.get(0).getEnergy());

        int idx = 1;
        double minDiff = Double.POSITIVE_INFINITY;
        int minIdx = 0;
        double maxDiff = 0.0;

        for (TestInput input : testInput) {
            EnergyInfo testResult = spectraCalc.calcTotalEnergyInfo(
                    new TreeMap<FrequencyBand, Double>(input.testMeasurements.entrySet()
                                                                             .stream()
                                                                             .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().getMean()))
                                                                             .collect(Collectors.toMap(SimpleEntry::getKey, SimpleEntry::getValue))),
                        input.mwMDAC,
                        input.apparentStress,
                        mdacFi);
            double energyRatio = testResult.getLogTotalEnergy() / input.getEnergy();
            double errorPercent = Math.abs(energyRatio - 1) * 100;
            if (errorPercent > maxDiff) {
                maxDiff = errorPercent;
            }
            if (errorPercent < minDiff) {
                minDiff = errorPercent;
                minIdx = idx;
            }
            double diff = testResult.getLogTotalEnergy() - testInput.get(0).getEnergy();
            log.info(
                    "Result {} ({} samples): {}, Diff: {}, % off: {}. App Stress={}",
                        idx,
                        input.getSamplesTaken(),
                        testResult.getLogTotalEnergy(),
                        String.format("%f", diff),
                        String.format("%f", errorPercent),
                        testResult.getObsApparentStress());
            idx += 1;
        }

        double errorTolerance = 0.3;
        log.info("\nMin err%: {} (result {})", minDiff, minIdx);
        log.info("{}", StringUtils.repeat("-", 100));
        assert (minDiff < errorTolerance); // Make sure percent error is less that tolerance
    }

    /**
     * Borrowed from the SiteCalibrationServiceImpl.java to generate weightmap
     * function for the fitMw test.
     *
     * @param data
     * @return
     */
    private Function<Map<Double, Double>, SortedMap<Double, Double>> createDataWeightMapFunction(final Map<FrequencyBand, SummaryStatistics> data) {
        return (final Map<Double, Double> frequencies) -> {
            final SortedMap<Double, Double> weightMap = new TreeMap<>();
            final Map<Double, SummaryStatistics> rawData = new HashMap<>();
            if (data != null) {
                data.entrySet().forEach(entry -> rawData.put((entry.getKey().getHighFrequency() + entry.getKey().getLowFrequency()) / 2.0, entry.getValue()));
            }
            double maxWeight = 1.0;
            for (Double frequency : frequencies.keySet()) {
                SummaryStatistics stats = rawData.getOrDefault(frequency, new SummaryStatistics());

                Double weight;
                if (stats.getN() > 1 && Double.isFinite(stats.getStandardDeviation())) {
                    weight = 1d + 1.0 / (stats.getStandardDeviation() / Math.sqrt(stats.getN()));
                } else {
                    weight = Double.valueOf(1d);
                }
                if (!Double.isFinite(weight)) {
                    weight = Double.valueOf(1d);
                }
                if (weight > maxWeight) {
                    maxWeight = weight;
                }
                weightMap.put(frequency, weight);
            }
            int lowestFrequencies = 2;
            int i = 0;
            for (final Double frequency : frequencies.keySet()) {
                if (i >= lowestFrequencies) {
                    break;
                }
                weightMap.put(frequency, 2 * maxWeight);
                i++;
            }
            return weightMap;
        };
    }

    /**
     * This will run the fitMw function using synthetic data to test the
     * accuracy of the output results
     *
     * @param mw
     * @param appStress
     * @param startFreq
     * @param endFreq
     */
    public void testFitMwOutput(double mw, double appStress, double startFreq, double endFreq) {
        List<TestInput> testInput = new ArrayList<>();

        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 10));
        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 100));
        testInput.add(new TestInput(mw, appStress, startFreq, endFreq, 1000));

        log.info("FitMw Results:\nMw: {}, apparent stress: {}, frequency range: {} to {}, expected energy: {}", mw, appStress, startFreq, endFreq, testInput.get(0).getEnergy());

        for (final TestInput input : testInput) {
            Event event = new Event();
            event.setEventId("123");
            event.setLatitude(1.0);
            event.setLongitude(1.0);

            long dataCount = 0l;
            final SortedMap<Double, Double> frequencyBands = new TreeMap<>();

            final SortedMap<FrequencyBand, SummaryStatistics> measurements = input.getTestMeasurements();

            for (final Entry<FrequencyBand, SummaryStatistics> meas : measurements.entrySet()) {
                final double logAmplitude = meas.getValue().getMean();
                if (logAmplitude > 0.0) {
                    final double lowFreq = meas.getKey().getLowFrequency();
                    final double highFreq = meas.getKey().getHighFrequency();
                    final double centerFreq = (highFreq + lowFreq) / 2.0;
                    frequencyBands.put(centerFreq, logAmplitude);
                    dataCount = dataCount + meas.getValue().getN();
                }
            }

            final Function<Map<Double, Double>, SortedMap<Double, Double>> weightFunction = createDataWeightMapFunction(measurements);
            double[] MoMw = spectraCalc.fitMw(event, measurements, phase, mdacFi, mdacPs, weightFunction);
            log.info("MW_FIT: {}, DATA_COUNT: {}, APP_STRESS: {}, FIT_MEAN: {},\nCORNER_FREQ: {}, MDAC_ENERGY: {}", MoMw[1], MoMw[2], MoMw[4], MoMw[11], MoMw[19], Math.log10(MoMw[22]));
        }

        log.info("{}", StringUtils.repeat("-", 100));
    }

    double[][] testInputData = { { 0.040, 22.3146 }, { 0.075, 22.5454 }, { 0.400, 22.7264 }, { 0.600, 22.7431 }, { 0.850, 22.7141 }, { 1.250, 22.5399 }, { 1.750, 22.1075 }, { 2.500, 21.6355 },
            { 3.500, 21.3823 } };

    @Test
    public void testCalcTotalEnergyInfo() throws Exception {
        // testCalcTotalEnergyOutput(testInputData, 15.43, 2.279, 10.97);
        testCalcTotalEnergyOutput(2.0, 1.0, 0.01, 200.0);
        // testFitMwOutput(2.0, 1.0, 0.01, 200.0);
        testCalcTotalEnergyOutput(5.0, 1.0, 0.01, 200.0);
        // testFitMwOutput(5.0, 10.0, 0.01, 200.0);
        testCalcTotalEnergyOutput(7.0, 5.0, 0.01, 200.0);
        // testFitMwOutput(7.0, 1.0, 0.01, 200.0);
        testCalcTotalEnergyOutput(9.0, 10.0, 0.001, 200.0);
        // testFitMwOutput(9.0, 1.0, 0.01, 200.0);
    }
}
