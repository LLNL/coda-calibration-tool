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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.TooManyEvaluationsException;
import org.apache.commons.math3.exception.TooManyIterationsException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.apache.commons.math3.stat.descriptive.SynchronizedMultivariateSummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.VelocityConfiguration;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersFiService;
import gov.llnl.gnem.apps.coda.calibration.service.api.MdacParametersPsService;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformToTimeSeriesConverter;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;
import llnl.gnem.core.util.SeriesMath;
import llnl.gnem.core.util.TimeT;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.util.MathFunctions.FitnessCriteria;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

@Component
public class SpectraCalculator {
    private double PHASE_VELOCITY_KM_S;

    private static final Logger log = LoggerFactory.getLogger(SpectraCalculator.class);

    private WaveformToTimeSeriesConverter converter;
    private SyntheticCodaModel syntheticCodaModel;
    private MdacCalculatorService mdacService;
    private MdacParametersFiService mdacFiService;
    private MdacParametersPsService mdacPsService;

    @Value("${spectra-calc.iteration-cutoff:50}")
    private int iterationCutoff = 50;
    @Value("${spectra-calc.min-mw:0.01}")
    private double minMW = 0.01;
    @Value("${spectra-calc.max-mw:8.0}")
    private double maxMW = 8.0;
    @Value("${spectra-calc.min-stress-mpa:0.00001}")
    private double minMPA = 0.00001;
    @Value("${spectra-calc.max-stress-mpa:10.00}")
    private double maxMPA = 10.00;

    private static final int LOG10_M0 = 0;
    private static final int MW_FIT = 1;
    private static final int DATA_COUNT = 2;
    private static final int RMS_FIT = 3;
    private static final int STRESS = 4;
    private static final int MW_MEAN = 5;
    private static final int MPA_MEAN = 6;
    private static final int FIT_MEAN = 7;
    private static final int MW_SD = 8;
    private static final int MPA_SD = 9;
    private static final int FIT_SD = 10;
    private static final int MW = 0;
    private static final int MPA = 1;
    private static final int FIT = 2;

    @Autowired
    public SpectraCalculator(WaveformToTimeSeriesConverter converter, SyntheticCodaModel syntheticCodaModel, MdacCalculatorService mdacService, MdacParametersFiService mdacFiService,
            MdacParametersPsService mdacPsService, VelocityConfiguration velConf) {
        this.converter = converter;
        this.syntheticCodaModel = syntheticCodaModel;
        this.mdacService = mdacService;
        this.mdacFiService = mdacFiService;
        this.mdacPsService = mdacPsService;
        this.PHASE_VELOCITY_KM_S = velConf.getPhaseVelocityInKms();
    }

    public List<SpectraMeasurement> measureAmplitudes(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            VelocityConfiguration velocityConfig) {
        return measureAmplitudes(generatedSynthetics, frequencyBandParameterMap, velocityConfig, null);
    }

    public List<SpectraMeasurement> measureAmplitudes(List<SyntheticCoda> generatedSynthetics, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            VelocityConfiguration velocityConfig, Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap) {
        return generatedSynthetics.parallelStream()
                                  .map(synth -> measureAmplitudeForSynthetic(synth, frequencyBandParameterMap, frequencyBandSiteParameterMap, velocityConfig))
                                  .filter(Objects::nonNull)
                                  .collect(Collectors.toList());
    }

    private SpectraMeasurement measureAmplitudeForSynthetic(SyntheticCoda synth, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap, VelocityConfiguration velocityConfig) {

        FrequencyBand frequencyBand = new FrequencyBand(synth.getSourceWaveform().getLowFrequency(), synth.getSourceWaveform().getHighFrequency());
        SharedFrequencyBandParameters params = frequencyBandParameterMap.get(frequencyBand);
        if (params != null) {
            TimeSeries envSeis = converter.convert(synth.getSourceWaveform());
            TimeSeries synthSeis = new TimeSeries(WaveformUtils.doublesToFloats(synth.getSegment()), synth.getSampleRate(), new TimeT(synth.getBeginTime()));

            envSeis.interpolate(synthSeis.getSamprate());

            Station station = synth.getSourceWaveform().getStream().getStation();
            Event event = synth.getSourceWaveform().getEvent();

            double distance = EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude());
            double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + distance);
            if (vr == 0.0) {
                vr = 1.0;
            }
            TimeT originTime = new TimeT(event.getOriginTime());
            final TimeT startTime;
            TimeT tempTime;
            TimeT trimTime = originTime.add(distance / vr);
            TimeSeries trimmedWaveform = new TimeSeries(envSeis);
            try {
                trimmedWaveform.cutBefore(trimTime);
                trimmedWaveform.cutAfter(trimTime.add(30.0));

                tempTime = new TimeT(trimTime.getEpochTime() + trimmedWaveform.getMaxTime()[0]);
            } catch (IllegalArgumentException e) {
                tempTime = trimTime;
            }
            startTime = tempTime;

            double siteCorrection = 0.0;

            if (frequencyBandSiteParameterMap != null && frequencyBandSiteParameterMap.containsKey(frequencyBand) && frequencyBandSiteParameterMap.get(frequencyBand).get(station) != null) {
                siteCorrection = frequencyBandSiteParameterMap.get(frequencyBand).get(station).getSiteTerm();
            }

            double eshCorrection = log10ESHcorrection(
                    synth.getSourceWaveform().getLowFrequency(),
                        synth.getSourceWaveform().getHighFrequency(),
                        params.getP1(),
                        params.getS2(),
                        params.getXc(),
                        params.getXt(),
                        params.getQ(),
                        distance,
                        velocityConfig);

            double minlength = params.getMinLength();
            double maxlength = params.getMaxLength();

            TimeT endTime = null;

            if (synth.getSourceWaveform().getAssociatedPicks() != null) {
                endTime = synth.getSourceWaveform()
                               .getAssociatedPicks()
                               .stream()
                               .filter(p -> p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim()))
                               .findFirst()
                               .map(pick -> {
                                   double startpick = startTime.subtractD(originTime);
                                   TimeT result = originTime;
                                   result = pick.getPickTimeSecFromOrigin() > (startpick + maxlength) ? result.add(startpick + maxlength) : result.add(pick.getPickTimeSecFromOrigin());
                                   return result;
                               })
                               .orElse(null);
            }

            if (endTime == null) {
                log.trace("Unable to determine and time of Coda for waveform: {}", synth.getSourceWaveform());
                return null;
            }

            // cut the coda window portion of the seismograms
            if (startTime.ge(endTime)) {
                log.trace("Coda envelope start time is after the seismograms end time, start: {} end: {} for envelope: {}", startTime.getDate(), endTime.getDate(), synth.getSourceWaveform());
                return null;
            } else if (endTime.le(startTime)) {
                log.trace("Coda envelope end time is before the start of the seismogram, start: {} end: {} for envelope: {}", startTime.getDate(), endTime.getDate(), synth.getSourceWaveform());
                return null;
            }

            // Note this mutates envSeis and synthSeis!
            boolean cutSucceeded = cutSeismograms(envSeis, synthSeis, startTime, endTime);

            if (cutSucceeded) {
                float[] envdata = envSeis.getData();
                float[] synthdata = synthSeis.getData();

                // envelope minus synthetic
                double rawAmp = new TimeSeries(SeriesMath.subtract(envdata, synthdata), synthSeis.getSamprate(), synthSeis.getTime()).getMedian();

                // NOTE - this is what Rengin refers to as the RAW Amplitude;
                // calculated below
                double rawAtMeasurementTime = 0.;

                float[] synthscaled = SeriesMath.add(synthdata, rawAmp);
                Double fit = FitnessCriteria.CVRMSD(envdata, synthscaled);

                // path corrected using Scott's Extended Street and Herrman
                // method and site correction
                double pathCorrectedAmp = rawAmp + eshCorrection;
                double correctedAmp = pathCorrectedAmp + siteCorrection;

                // user defined coda measurement time
                double measurementtime = params.getMeasurementTime();
                if (measurementtime > 0.) {
                    double shiftedvalue = syntheticCodaModel.getPointAtTimeAndDistance(params, measurementtime, distance);
                    rawAtMeasurementTime = rawAmp + shiftedvalue;
                } else {
                    rawAtMeasurementTime = rawAmp;
                }
                pathCorrectedAmp = rawAtMeasurementTime + eshCorrection;
                correctedAmp = pathCorrectedAmp + siteCorrection;

                if (endTime.subtract(startTime).getEpochTime() < minlength) {
                    log.debug("Coda window length too short for envelope: {}", synth.getSourceWaveform());
                    return null;
                }

                if (siteCorrection <= 0.) {
                    correctedAmp = 0.;
                }

                return new SpectraMeasurement().setWaveform(synth.getSourceWaveform())
                                               .setRawAtStart(rawAmp)
                                               .setRawAtMeasurementTime(rawAtMeasurementTime)
                                               .setPathCorrected(pathCorrectedAmp)
                                               .setPathAndSiteCorrected(correctedAmp)
                                               .setStartCutSec(startTime.subtract(originTime).getEpochTime())
                                               .setEndCutSec(endTime.subtract(originTime).getEpochTime())
                                               .setRmsFit(fit);
            }
        }

        return null;

    }

    private boolean cutSeismograms(TimeSeries a, TimeSeries b, TimeT startTime, TimeT endTime) {
        boolean completedCut = false;
        try {
            a.cut(startTime, endTime);
            b.cut(startTime, endTime);

            // These might be off by some small number of samples due to
            // precision
            // errors during cut and SeriesMath will throw an ArrayBounds if
            // they
            // don't match exactly so we need to double check!
            if (a.getNsamp() != b.getNsamp()) {

                TimeT start = a.getTime();
                TimeT end = a.getEndtime();
                TimeT startA = a.getTime();
                TimeT startB = b.getTime();
                TimeT endA = a.getEndtime();
                TimeT endB = b.getEndtime();

                // choose the latest start time and the earliest end time for
                // the
                // cut window
                if (startA.lt(startB)) {
                    start = startB;
                }
                if (endA.gt(endB)) {
                    end = endB;
                }
                if (start.ge(end)) {
                    // don't continue if the seismograms don't overlap
                    return completedCut;
                }

                int begin_index_a = a.getIndexForTime(start.getEpochTime());
                int end_index_a = a.getIndexForTime(end.getEpochTime());

                int begin_index_b = b.getIndexForTime(start.getEpochTime());
                int end_index_b = b.getIndexForTime(end.getEpochTime());

                int nptsa = (end_index_a - begin_index_a) + 1;
                int nptsb = (end_index_b - begin_index_b) + 1;

                int npts = Math.min(nptsa, nptsb);
                // now cut the traces again
                a.cut(begin_index_a, (begin_index_a + npts - 1));
                b.cut(begin_index_b, (begin_index_b + npts - 1));
            }
            completedCut = true;
        } catch (IllegalArgumentException e) {
            log.warn("Error attempting to cut seismograms during amplitude measurement; {}", e.getMessage());
        }

        return completedCut;
    }

    /**
     * Scott Phillips Extended Street and Hermann path correction including
     * distance and Q
     *
     * @param lowfreq
     *            the low frequency cut
     * @param highfreq
     *            the high frequency cut
     * @param alpha1
     * @param alpha2
     * @param xc
     *            xcross
     * @param xt
     *            xtrans
     * @param distance
     *            source-receiver distance
     * @param q
     *            the attenuation term
     * @return -log10(esh) + distance*pi*f0*log10(e) / (vphase* q)
     */
    public double log10ESHcorrection(double lowfreq, double highfreq, double alpha1, double alpha2, double xc, double xt, double q, double distance, VelocityConfiguration velocityConfig) {
        double log10esh = log10ESHcorrection(alpha1, alpha2, xc, xt, distance);

        if ((log10esh == 0.) || (q == 0.)) {
            return 0.; // no ESH correction
        }

        double f0 = Math.sqrt(lowfreq * highfreq);
        double efact = Math.log10(Math.E);
        double vphase;
        if (velocityConfig != null && velocityConfig.getPhaseVelocityInKms() != null && velocityConfig.getPhaseVelocityInKms() != 0.0) {
            vphase = velocityConfig.getPhaseVelocityInKms();
        } else {
            vphase = PHASE_VELOCITY_KM_S;
        }
        double distQ = distance * Math.PI * f0 * efact / (q * vphase);
        // We want to return a positive number for path correction
        return -1 * log10esh + distQ;
    }

    /**
     * Extended Street and Herrmann path correction
     *
     * calculate log10 extended Street-Herrmann model for one distance no site,
     * no Q terms
     *
     * translated from Scott Phillips original fortran function eshmod
     */
    public double log10ESHcorrection(double s1, double s2, double xcross, double xtrans, double distance) {
        double eshmod;
        double xstart = xcross / xtrans;
        double xend = xcross * xtrans;

        if (distance <= xstart) {
            eshmod = -1.0 * s1 * Math.log10(distance);
        } else if (distance >= xend) {
            double ds = s2 - s1;
            eshmod = -1.0 * s1 * Math.log10(xstart) - (s1 + ds / 2.) * Math.log10(xend / xstart) - s2 * Math.log10(distance / xend);
        } else {
            // singular if xtrans=1, but should not get here
            double s = (s2 - s1) / Math.log10(xend / xstart);
            double ds = s * Math.log10(distance / xstart);
            eshmod = -1.0 * s1 * Math.log10(xstart) - (s1 + ds / 2.) * Math.log10(distance / xstart);
        }

        return eshmod;
    }

    public Spectra computeReferenceSpectra(ReferenceMwParameters refEvent, List<FrequencyBand> bands, PICK_TYPES selectedPhase) {

        MdacParametersFI mdacFiEntry = mdacFiService.findFirst();
        MdacParametersPS psRows = mdacPsService.findMatchingPhase(selectedPhase.getPhase());

        List<Point2D.Double> xyPoints = new ArrayList<>();
        for (FrequencyBand band : bands) {
            double centerFreq = band.getLowFrequency() + (band.getHighFrequency() - band.getLowFrequency()) / 2.;
            double logFreq = Math.log10(centerFreq);

            if (refEvent.getRefApparentStressInMpa() != null && refEvent.getRefApparentStressInMpa() > 0.0) {
                // If we know an apparent stress in MPA for this reference event we want
                // to use that stress so we set Psi == 0.0 to use Sigma
                mdacFiEntry.setSigma(refEvent.getRefApparentStressInMpa());
                mdacFiEntry.setPsi(0.0);
            }

            double amplitude = mdacService.calculateMdacAmplitudeForMw(psRows, mdacFiEntry, refEvent.getRefMw(), centerFreq, selectedPhase);

            if (amplitude > 0) {
                Point2D.Double point = new Point2D.Double(logFreq, amplitude);
                xyPoints.add(point);
            }
        }

        Collections.sort(xyPoints, (p1, p2) -> Double.compare(p1.getX(), p2.getX()));
        return new Spectra(SPECTRA_TYPES.REF, xyPoints, refEvent.getRefMw(), refEvent.getRefApparentStressInMpa());
    }

    public Spectra computeFitSpectra(MeasuredMwParameters event, Collection<FrequencyBand> bands, PICK_TYPES selectedPhase) {

        MdacParametersFI mdacFiEntry = mdacFiService.findFirst();
        MdacParametersPS psRows = mdacPsService.findMatchingPhase(selectedPhase.getPhase());

        List<Point2D.Double> xyPoints = new ArrayList<>();
        for (FrequencyBand band : bands) {
            double centerFreq = band.getLowFrequency() + (band.getHighFrequency() - band.getLowFrequency()) / 2.;
            double logFreq = Math.log10(centerFreq);

            if (event.getApparentStressInMpa() != null && event.getApparentStressInMpa() > 0.0) {
                mdacFiEntry.setSigma(event.getApparentStressInMpa());
                mdacFiEntry.setPsi(0.0);
            }

            double amplitude = mdacService.calculateMdacAmplitudeForMw(psRows, mdacFiEntry, event.getMw(), centerFreq, selectedPhase);

            if (amplitude > 0) {
                Point2D.Double point = new Point2D.Double(logFreq, amplitude);
                xyPoints.add(point);
            }
        }
        Collections.sort(xyPoints, (p1, p2) -> Double.compare(p1.getX(), p2.getX()));
        return new Spectra(SPECTRA_TYPES.FIT, xyPoints, event.getMw(), event.getApparentStressInMpa());
    }

    /**
     * An estimate of the Mw and corner frequency based on Mdac spectra Based on
     * the MDAC2 spectra calculations published by Walter and Taylor, 2001
     * UCRL-ID-146882
     *
     */
    public List<MeasuredMwParameters> measureMws(final Map<Event, Map<FrequencyBand, SummaryStatistics>> evidMap, Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights,
            final PICK_TYPES selectedPhase, MdacParametersPS mdacPs, MdacParametersFI mdacFi) {
        List<MeasuredMwParameters> measuredMws = new ArrayList<>(evidMap.entrySet().size());
        for (Entry<Event, Map<FrequencyBand, SummaryStatistics>> entry : evidMap.entrySet()) {
            Map<FrequencyBand, SummaryStatistics> measurements = entry.getValue();
            double[] MoMw = fitMw(entry.getKey(), measurements, selectedPhase, mdacFi, mdacPs, eventWeights.get(entry.getKey()));
            if (MoMw == null) {
                log.warn("MoMw calculation returned null value");
                continue;
            }
            measuredMws.add(
                    new MeasuredMwParameters().setEventId(entry.getKey().getEventId())
                                              .setDataCount((int) MoMw[DATA_COUNT])
                                              .setMw(MoMw[MW_FIT])
                                              .setMeanMw(MoMw[MW_MEAN])
                                              .setMwSd(MoMw[MW_SD])
                                              .setApparentStressInMpa(MoMw[STRESS])
                                              .setMeanApparentStressInMpa(MoMw[MPA_MEAN])
                                              .setStressSd(MoMw[MPA_SD])
                                              .setMisfit(MoMw[RMS_FIT])
                                              .setMeanMisfit(MoMw[FIT_MEAN])
                                              .setMisfitSd(MoMw[FIT_SD]));
        }
        return measuredMws;
    }

    /**
     * Grid search across a range of corner frequency and stress parameters
     * looking for the best fit theoretical source spectra and return the
     * resulting MW measurement.
     *
     * @param event
     *
     * @param measurements
     *            - the measured spectra values by frequency band
     * @param phase
     *            - the phase (e.g. "Lg") this should be present in the Mdac
     *            parameter set
     * @return double[] containing the final fit measurements
     */
    public double[] fitMw(Event event, final Map<FrequencyBand, SummaryStatistics> measurements, final PICK_TYPES phase, final MdacParametersFI mdacFi, final MdacParametersPS mdacPs,
            Function<Map<Double, Double>, SortedMap<Double, Double>> weightFunction) {
        double[] result = new double[11];

        final SortedMap<Double, Double> frequencyBands = new TreeMap<>();
        long dataCount = 0l;

        for (Entry<FrequencyBand, SummaryStatistics> meas : measurements.entrySet()) {
            double logAmplitude = meas.getValue().getMean();
            if (logAmplitude > 0.0) {
                double lowFreq = meas.getKey().getLowFrequency();
                double highFreq = meas.getKey().getHighFrequency();
                double centerFreq = (highFreq + lowFreq) / 2.0;
                frequencyBands.put(centerFreq, logAmplitude);
                dataCount = dataCount + meas.getValue().getN();
            }
        }

        final Map<Double, Double> weightMap = weightFunction.apply(frequencyBands);
        //TODO: Thinking about ways to flatten spectra fit for missing low frequencies.
        //TODO: Devise uncertainty quantification on site fit. Distribution of misfit vs mws evaluated maybe?
        //           Would need to add the extremes for the mw/mpa range to the misfit dist to get a good estimate of the range.
        //           Misfit is log normal probably; should check.

        final SynchronizedMultivariateSummaryStatistics stats = new SynchronizedMultivariateSummaryStatistics(3, false);
        MultivariateFunction mdacFunction = new MultivariateFunction() {

            @Override
            public double value(double[] point) {
                double testMw = point[0];
                double testSigma = point[1];

                HashMap<Object, double[]> dataMap = new HashMap<>();

                for (Entry<FrequencyBand, SummaryStatistics> meas : measurements.entrySet()) {
                    double logAmplitude = meas.getValue().getMean();
                    double lowFreq = meas.getKey().getLowFrequency();
                    double highFreq = meas.getKey().getHighFrequency();
                    double centerFreq = (highFreq + lowFreq) / 2.0;

                    if (mdacPs != null) {
                        // Note this is in dyne-cm to match Kevin
                        double log10mdacM0 = mdacService.calculateMdacAmplitudeForMw(mdacPs, mdacFi, testMw, centerFreq, phase, testSigma);

                        if (logAmplitude > 0.) {
                            double[] coda_vs_mdac = new double[] { logAmplitude, log10mdacM0 };
                            dataMap.put(centerFreq, coda_vs_mdac);
                        }
                    }
                }

                double fit = WCVRMSD(weightMap, dataMap);
                stats.addValue(new double[] { testMw, testSigma, fit });
                return fit;
            }
        };

        ConvergenceChecker<PointValuePair> convergenceChecker = new SimplePointChecker<>(0.00001, 0.00001, 100000);
        CMAESOptimizer optimizer = new CMAESOptimizer(1000000, 0, true, 0, 10, new MersenneTwister(), false, convergenceChecker);
        int iterations = iterationCutoff;
        try {
            PointValuePair optimizerResult = optimizer.optimize(
                    new MaxEval(1000000),
                        new ObjectiveFunction(mdacFunction),
                        GoalType.MINIMIZE,
                        new SimpleBounds(new double[] { minMW, minMPA }, new double[] { maxMW, maxMPA }),
                        new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minMW, maxMW), ThreadLocalRandom.current().nextDouble(minMPA, maxMPA) }),
                        new CMAESOptimizer.Sigma(new double[] { 0.5, 1.0 }),
                        new CMAESOptimizer.PopulationSize(50));

            // converted back into dyne-cm to match Kevin's format
            double testMw = optimizerResult.getPoint()[0];
            // log10M0
            result[LOG10_M0] = Math.log10(mdacService.getMwInDyne(testMw));
            result[MW_FIT] = testMw; // best Mw fit
            // this is the number of elements that have a signal measurement
            result[DATA_COUNT] = dataCount;
            // this is the rmsfit measurement
            result[RMS_FIT] = optimizerResult.getValue();
            // this is the stress
            result[STRESS] = optimizerResult.getPoint()[1];
            iterations = optimizer.getIterations();
        } catch (TooManyEvaluationsException | TooManyIterationsException e) {
            log.warn("Failed to converge while attempting to fit an Mw to this event {}, falling back to a grid search.", event);
        }

        if (iterations >= iterationCutoff) {
            double best = Double.MAX_VALUE;
            double mwVal = 0;
            double stressVal = 0;
            for (double mw = minMW; mw < maxMW; mw = mw + ((maxMW - minMW) / 100.)) {
                for (double stress = minMPA; stress < maxMPA; stress = stress + ((maxMPA - minMPA) / 100.)) {
                    double res = mdacFunction.value(new double[] { mw, stress });
                    stats.addValue(new double[] { mw, stress, res });
                    if (res < best) {
                        best = res;
                        mwVal = mw;
                        stressVal = stress;
                    }
                }
            }

            result[LOG10_M0] = Math.log10(mdacService.getMwInDyne(mwVal));
            result[MW_FIT] = mwVal;
            result[RMS_FIT] = best;
            result[STRESS] = stressVal;
        }

        RealMatrix C = stats.getCovariance();
        result[MW_MEAN] = stats.getMean()[MW];
        result[MW_SD] = Math.sqrt(C.getEntry(MW, MW));
        result[MPA_MEAN] = stats.getMean()[MPA];
        result[MPA_SD] = Math.sqrt(C.getEntry(MPA, MPA));
        result[FIT_MEAN] = stats.getMean()[FIT];
        result[FIT_SD] = Math.sqrt(C.getEntry(FIT, FIT));
        return result;
    }

    /**
     * @param weightMap
     * @param dataMap
     * @return
     */
    protected static double WCVRMSD(Map<Double, Double> weightMap, HashMap<Object, double[]> dataMap) {
        for (Entry<Object, double[]> entry : dataMap.entrySet()) {
            if (weightMap.containsKey(entry.getKey())) {
                double[] val = entry.getValue();
                for (int i = 0; i < val.length; i++) {
                    val[i] *= weightMap.get(entry.getKey());
                }
                dataMap.put(entry.getKey(), val);
            }
        }
        return FitnessCriteria.CVRMSD(dataMap);
    }
}
