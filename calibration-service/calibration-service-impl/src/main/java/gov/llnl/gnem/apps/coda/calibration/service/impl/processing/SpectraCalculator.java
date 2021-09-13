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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.ConvergenceChecker;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.MultiStartMultivariateOptimizer;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.random.SobolSequenceGenerator;
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
    private final double PHASE_VELOCITY_KM_S;

    private static final Logger log = LoggerFactory.getLogger(SpectraCalculator.class);

    private final WaveformToTimeSeriesConverter converter;
    private final SyntheticCodaModel syntheticCodaModel;
    private final MdacCalculatorService mdacService;
    private final MdacParametersFiService mdacFiService;
    private final MdacParametersPsService mdacPsService;

    @Value("${spectra-calc.iteration-cutoff:50}")
    private int iterationCutoff = 50;
    @Value("${spectra-calc.min-mw:0.01}")
    private double minMW = 0.01;
    @Value("${spectra-calc.max-mw:10.0}")
    private double maxMW = 10.0;
    @Value("${spectra-calc.min-apparent-stress-mpa:0.0001}")
    private double minApparentStress = 0.0001;
    @Value("${spectra-calc.max-apparent-stress-mpa:1000.00}")
    private double maxApparentStress = 1000.00;
    @Value("${report-stress-bounds-in-uq:false}")
    private boolean reportStressBoundsInUQ = false;

    private static final int LOG10_M0 = 0;
    private static final int MW_FIT = 1;
    private static final int DATA_COUNT = 2;
    private static final int RMS_FIT = 3;
    private static final int APP_STRESS = 4;
    private static final int MW_MEAN = 5;
    private static final int MW_1_MIN = 6;
    private static final int MW_1_MAX = 7;
    private static final int MW_2_MIN = 8;
    private static final int MW_2_MAX = 9;
    private static final int APP_STRESS_MEAN = 10;
    private static final int FIT_MEAN = 11;
    private static final int MW_SD = 12;
    private static final int APP_STRESS_SD = 13;
    private static final int FIT_SD = 14;
    private static final int APP_1_MIN = 15;
    private static final int APP_1_MAX = 16;
    private static final int APP_2_MIN = 17;
    private static final int APP_2_MAX = 18;
    private static final int CORNER_FREQ = 19;
    private static final int CORNER_FREQ_SD = 20;
    private static final int ITR_COUNT = 21;
    private static final int MDAC_ENERGY = 22;
    private static final int PARAM_COUNT = 23;

    private static final int MW = 0;
    private static final int MPA = 1;
    private static final int FIT = 2;
    private static final int CORNER = 3;

    private final Comparator<Triple<Double, Double, Double>> mwFittingComparator = (o1, o2) -> {
        int compare = Double.compare(o1.getLeft(), o2.getLeft());
        if (compare == 0) {
            compare = Double.compare(o1.getMiddle(), o2.getMiddle());
            if (compare == 0) {
                compare = Double.compare(o2.getRight(), o1.getRight());
            }
        }
        return compare;
    };

    @Autowired
    public SpectraCalculator(final WaveformToTimeSeriesConverter converter, final SyntheticCodaModel syntheticCodaModel, final MdacCalculatorService mdacService,
            final MdacParametersFiService mdacFiService, final MdacParametersPsService mdacPsService, final VelocityConfiguration velConf) {
        this.converter = converter;
        this.syntheticCodaModel = syntheticCodaModel;
        this.mdacService = mdacService;
        this.mdacFiService = mdacFiService;
        this.mdacPsService = mdacPsService;
        this.PHASE_VELOCITY_KM_S = velConf.getPhaseVelocityInKms();
    }

    public List<SpectraMeasurement> measureAmplitudes(final List<SyntheticCoda> generatedSynthetics, final Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            final VelocityConfiguration velocityConfig) {
        return measureAmplitudes(generatedSynthetics, frequencyBandParameterMap, velocityConfig, null);
    }

    public List<SpectraMeasurement> measureAmplitudes(final List<SyntheticCoda> generatedSynthetics, final Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            final VelocityConfiguration velocityConfig, final Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap) {
        return generatedSynthetics.parallelStream()
                                  .map(synth -> measureAmplitudeForSynthetic(synth, frequencyBandParameterMap, frequencyBandSiteParameterMap, velocityConfig))
                                  .filter(Objects::nonNull)
                                  .collect(Collectors.toList());
    }

    private SpectraMeasurement measureAmplitudeForSynthetic(final SyntheticCoda synth, final Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameterMap,
            final Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> frequencyBandSiteParameterMap, final VelocityConfiguration velocityConfig) {

        final FrequencyBand frequencyBand = new FrequencyBand(synth.getSourceWaveform().getLowFrequency(), synth.getSourceWaveform().getHighFrequency());
        final SharedFrequencyBandParameters params = frequencyBandParameterMap.get(frequencyBand);
        if (params != null) {
            final TimeSeries envSeis = converter.convert(synth.getSourceWaveform());
            final TimeSeries synthSeis = new TimeSeries(WaveformUtils.doublesToFloats(synth.getSegment()), synth.getSampleRate(), new TimeT(synth.getBeginTime()));

            envSeis.interpolate(synthSeis.getSamprate());

            final Station station = synth.getSourceWaveform().getStream().getStation();
            final Event event = synth.getSourceWaveform().getEvent();

            final double distance = EModel.getDistanceWGS84(event.getLatitude(), event.getLongitude(), station.getLatitude(), station.getLongitude());
            double vr = params.getVelocity0() - params.getVelocity1() / (params.getVelocity2() + distance);
            if (vr == 0.0) {
                vr = 1.0;
            }

            final TimeT originTime = new TimeT(event.getOriginTime());

            TimeT codaStart;
            if (synth.getSourceWaveform().getMaxVelTime() != null) {
                codaStart = new TimeT(synth.getSourceWaveform().getMaxVelTime());
            } else {
                codaStart = originTime;
                codaStart = codaStart.add(distance / vr);
            }
            final TimeT startTime = codaStart;

            double siteCorrection = 0.0;

            if (frequencyBandSiteParameterMap != null && frequencyBandSiteParameterMap.containsKey(frequencyBand) && frequencyBandSiteParameterMap.get(frequencyBand).get(station) != null) {
                siteCorrection = frequencyBandSiteParameterMap.get(frequencyBand).get(station).getSiteTerm();
            }

            final double eshCorrection = log10ESHcorrection(
                    synth.getSourceWaveform().getLowFrequency(),
                        synth.getSourceWaveform().getHighFrequency(),
                        params.getP1(),
                        params.getS2(),
                        params.getXc(),
                        params.getXt(),
                        params.getQ(),
                        distance,
                        velocityConfig);

            final double minlength = params.getMinLength();
            final double maxlength = params.getMaxLength();

            TimeT endTime = null;

            if (synth.getSourceWaveform().getAssociatedPicks() != null) {
                endTime = synth.getSourceWaveform()
                               .getAssociatedPicks()
                               .stream()
                               .filter(p -> p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim()))
                               .findFirst()
                               .map(pick -> {
                                   final double startpick = startTime.subtractD(originTime);
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

            boolean cutSucceeded = false;
            try {
                // Note this mutates envSeis and synthSeis!
                cutSucceeded = WaveformUtils.cutSeismograms(envSeis, synthSeis, startTime, endTime);
            } catch (final IllegalArgumentException e) {
                log.warn("Error attempting to cut seismograms during amplitude measurement; {}", e.getMessage());
            }

            if (cutSucceeded) {
                final float[] envdata = envSeis.getData();
                final float[] synthdata = synthSeis.getData();

                // envelope minus synthetic
                final double rawAmp = new TimeSeries(SeriesMath.subtract(envdata, synthdata), synthSeis.getSamprate(), synthSeis.getTime()).getMedian();

                // NOTE - this is what Rengin refers to as the RAW Amplitude;
                // calculated below
                final double rawAtMeasurementTime;

                final float[] synthscaled = SeriesMath.add(synthdata, rawAmp);
                final Double fit = FitnessCriteria.CVRMSD(envdata, synthscaled);

                // path corrected using Scott's Extended Street and Herrman
                // method and site correction
                double pathCorrectedAmp = rawAmp + eshCorrection;

                // user defined coda measurement time
                final double measurementtime = params.getMeasurementTime();
                if (measurementtime > 0.) {
                    final double shiftedvalue = syntheticCodaModel.getPointAtTimeAndDistance(params, measurementtime, distance);
                    rawAtMeasurementTime = rawAmp + shiftedvalue;
                } else {
                    rawAtMeasurementTime = rawAmp;
                }
                pathCorrectedAmp = rawAtMeasurementTime + eshCorrection;

                double correctedAmp = pathCorrectedAmp + siteCorrection;

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
    public double log10ESHcorrection(final double lowfreq, final double highfreq, final double alpha1, final double alpha2, final double xc, final double xt, final double q, final double distance,
            final VelocityConfiguration velocityConfig) {
        final double log10esh = log10ESHcorrection(alpha1, alpha2, xc, xt, distance);

        if ((log10esh == 0.) || (q == 0.)) {
            return 0.; // no ESH correction
        }

        final double f0 = Math.sqrt(lowfreq * highfreq);
        final double efact = Math.log10(Math.E);
        double vphase;
        if (velocityConfig != null && velocityConfig.getPhaseVelocityInKms() != null && velocityConfig.getPhaseVelocityInKms() != 0.0) {
            vphase = velocityConfig.getPhaseVelocityInKms();
        } else {
            vphase = PHASE_VELOCITY_KM_S;
        }
        final double distQ = distance * Math.PI * f0 * efact / (q * vphase);
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
    public double log10ESHcorrection(final double s1, final double s2, final double xcross, final double xtrans, final double distance) {
        double eshmod;
        final double xstart = xcross / xtrans;
        final double xend = xcross * xtrans;

        if (distance <= xstart) {
            eshmod = -1.0 * s1 * Math.log10(distance);
        } else if (distance >= xend) {
            final double ds = s2 - s1;
            eshmod = -1.0 * s1 * Math.log10(xstart) - (s1 + ds / 2.) * Math.log10(xend / xstart) - s2 * Math.log10(distance / xend);
        } else {
            // singular if xtrans=1, but should not get here
            final double s = (s2 - s1) / Math.log10(xend / xstart);
            final double ds = s * Math.log10(distance / xstart);
            eshmod = -1.0 * s1 * Math.log10(xstart) - (s1 + ds / 2.) * Math.log10(distance / xstart);
        }

        return eshmod;
    }

    public Spectra computeReferenceSpectra(final ReferenceMwParameters refEvent, final List<FrequencyBand> bands, final PICK_TYPES selectedPhase) {
        return computeSpecificSpectra(refEvent.getRefMw(), refEvent.getRefApparentStressInMpa(), null, bands, selectedPhase, SPECTRA_TYPES.REF);
    }

    public Spectra computeFitSpectra(final MeasuredMwParameters event, final Collection<FrequencyBand> bands, final PICK_TYPES selectedPhase) {
        EnergyInfo eInfo = new EnergyInfo(event.getObsEnergy(), event.getLogTotalEnergy(), event.getLogTotalEnergyMDAC(), event.getEnergyRatio(), event.getObsAppStress());
        return computeSpecificSpectra(event.getMw(), event.getApparentStressInMpa(), eInfo, bands, selectedPhase, SPECTRA_TYPES.FIT);
    }

    public Spectra computeSpecificSpectra(final Double mw, final Double apparentStress, final EnergyInfo energyInfo, final Collection<FrequencyBand> bands, final PICK_TYPES selectedPhase,
            final SPECTRA_TYPES type) {

        final MdacParametersFI mdacFiEntry = new MdacParametersFI(mdacFiService.findFirst());
        final MdacParametersPS psRows = mdacPsService.findMatchingPhase(selectedPhase.getPhase());

        final List<Point2D.Double> xyPoints = new ArrayList<>();

        if (apparentStress != null && apparentStress > 0.0) {
            mdacFiEntry.setSigma(apparentStress);
            mdacFiEntry.setPsi(0.0);
        }

        final DoubleUnaryOperator mdacFunction = mdacService.getCalculateMdacAmplitudeForMwFunction(psRows, mdacFiEntry, mw, selectedPhase);
        Double cornerFrequency = null;

        if (type != null && type.equals(SPECTRA_TYPES.FIT)) {
            cornerFrequency = mdacService.getCornerFrequency(psRows, mdacFiEntry.setPsi(0.0).setSigma(apparentStress), mw);
        }
        for (final FrequencyBand band : bands) {
            final double centerFreq = band.getLowFrequency() + (band.getHighFrequency() - band.getLowFrequency()) / 2.;
            final double logFreq = Math.log10(centerFreq);

            final double amplitude = mdacFunction.applyAsDouble(centerFreq);

            if (amplitude > 0) {
                final Point2D.Double point = new Point2D.Double(logFreq, amplitude);
                xyPoints.add(point);
            }
        }
        Collections.sort(xyPoints, (p1, p2) -> Double.compare(p1.getX(), p2.getX()));
        if (energyInfo == null) {
            return new Spectra(type, xyPoints, mw, apparentStress, cornerFrequency, null, null, null, null);
        }
        return new Spectra(type,
                           xyPoints,
                           mw,
                           apparentStress,
                           cornerFrequency,
                           energyInfo.getObsEnergy(),
                           energyInfo.getLogTotalEnergy(),
                           energyInfo.getLogEnergyMDAC(),
                           energyInfo.getObsApparentStress());
    }

    /**
     * An estimate of the Mw and corner frequency based on Mdac spectra Based on
     * the MDAC2 spectra calculations published by Walter and Taylor, 2001
     * UCRL-ID-146882
     *
     */
    public List<MeasuredMwParameters> measureMws(MwMeasurementInputData inputData, final PICK_TYPES selectedPhase, MdacParametersFI mdacFi) {
        return inputData.getEvidMap().entrySet().parallelStream().map(entry -> {
            SortedMap<FrequencyBand, SummaryStatistics> measurements = new TreeMap<>(entry.getValue());
            double[] MoMw = fitMw(entry.getKey(), measurements, selectedPhase, mdacFi, inputData.getMdacPs(), inputData.getEventWeights().get(entry.getKey()));
            if (MoMw == null) {
                log.warn("MoMw calculation returned null value");
                return null;
            }

            EnergyInfo info = calcTotalEnergyInfo(measurements, MoMw[MW_FIT], MoMw[APP_STRESS], mdacFi);

            return new MeasuredMwParameters().setEventId(entry.getKey().getEventId())
                                             .setDataCount((int) MoMw[DATA_COUNT])
                                             .setStationCount(inputData.getStationCount().get(entry.getKey().getEventId()))
                                             .setBandCoverage(inputData.getBandCoverageMetric().get(entry.getKey().getEventId()))
                                             .setObsEnergy(info.getObsEnergy())
                                             .setLogTotalEnergy(info.getLogTotalEnergy())
                                             .setLogTotalEnergyMDAC(info.getLogEnergyMDAC())
                                             .setEnergyRatio(info.getEnergyRatio())
                                             .setObsAppStress(info.getObsApparentStress())
                                             .setMw(MoMw[MW_FIT])
                                             .setMeanMw(MoMw[MW_MEAN])
                                             .setMwSd(MoMw[MW_SD])
                                             .setMw1Min(MoMw[MW_1_MIN])
                                             .setMw1Max(MoMw[MW_1_MAX])
                                             .setMw2Min(MoMw[MW_2_MIN])
                                             .setMw2Max(MoMw[MW_2_MAX])
                                             .setApparentStressInMpa(MoMw[APP_STRESS])
                                             .setMeanApparentStressInMpa(MoMw[APP_STRESS_MEAN])
                                             .setApparentStressSd(MoMw[APP_STRESS_SD])
                                             .setApparentStress1Min(MoMw[APP_1_MIN])
                                             .setApparentStress1Max(MoMw[APP_1_MAX])
                                             .setApparentStress2Min(MoMw[APP_2_MIN])
                                             .setApparentStress2Max(MoMw[APP_2_MAX])
                                             .setMisfit(MoMw[RMS_FIT])
                                             .setMeanMisfit(MoMw[FIT_MEAN])
                                             .setMisfitSd(MoMw[FIT_SD])
                                             .setCornerFrequency(MoMw[CORNER_FREQ])
                                             .setCornerFrequencySd(MoMw[CORNER_FREQ_SD])
                                             .setIterations((int) MoMw[ITR_COUNT]);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * Now compute the total energy by summing the squared product of the
     * discrete path-corrected moment rate spectra and omega. Then divide by
     * 4*pi*pi*rho*(vel**5)/(Radiation) Radiation=(2/5)
     *
     * rho=2.7 gm/cm**3 vel=3.5E5 cm/s
     *
     * Then finally multiply by delta omega at the front.
     *
     * Mayeda, K., and Walter, W. R. (1996), Moment, energy, stress drop, and
     * source spectra of western United States earthquakes from regional coda
     * envelopes, J. Geophys. Res., 101( B5), 11195– 11208,
     * doi:10.1029/96JB00112.
     */
    public EnergyInfo calcTotalEnergyInfo(final SortedMap<FrequencyBand, SummaryStatistics> measurements, double mwMDAC, double apparentStress, MdacParametersFI mdacFI) {

        final int measCount = measurements.entrySet().size();

        double[] logAmplitudes = new double[measCount];
        double[] lowFreq = new double[measCount];
        double[] highFreq = new double[measCount];

        // Initialize data for calculations below
        int idx = 0;
        for (final Entry<FrequencyBand, SummaryStatistics> meas : measurements.entrySet()) {
            // Unit conversion from Dyne cm to MKS
            logAmplitudes[idx] = meas.getValue().getMean() - 7.0;
            lowFreq[idx] = meas.getKey().getLowFrequency();
            highFreq[idx] = meas.getKey().getHighFrequency();
            idx++;
        }

        // double k = Math.sqrt(0.4 / (4 * Math.PI * Math.PI * mdacFI.getRhos() * Math.pow(mdacFI.getBetas(), 5)));
        double k = Math.sqrt(6.48E-24); // Numerically derived k constant
        double sumEnergy = 0.0;
        double end;
        double start;
        double amp;
        double curEnergy;
        for (int i = 1; i < measCount - 1; i++) {
            end = 2.0 * Math.PI * highFreq[i];
            start = 2.0 * Math.PI * lowFreq[i];

            amp = Math.pow(10.0, logAmplitudes[i]) * k;
            curEnergy = (Math.pow(amp, 2.0) / 3.0) * (Math.pow(end, 3.0) - Math.pow(start, 3.0));
            sumEnergy = sumEnergy + curEnergy;
        }

        /**
         * Now for the last point, assume an omega square fall-off and integrate
         * from wN to infinity.
         */
        final double wN = Math.PI * (highFreq[measCount - 1] + lowFreq[measCount - 1]);
        final double AN = (Math.pow(10, logAmplitudes[measCount - 1])) * k;
        final double delta_wN = (Math.PI * (highFreq[measCount - 1] + lowFreq[measCount - 1]) / 2.0);
        final double EN = ((Math.pow(AN, 2.0)) / 3.0) * (Math.pow(wN, 3.0) - Math.pow(wN - delta_wN, 3.0));
        final double eTotal_obs = sumEnergy + EN;

        /**
         * mw : Compute moment, from the low frequency level of the 2 lowest
         * values
         */
        double logMoment = 0.0;
        double mw = 0.0;

        /**
         * A moment magnitude scale ISSN: 0148-0227 , 2156-2202; DOI:
         * 10.1029/JB084iB05p02348 Journal of geophysical research. , 1979,
         * Vol.84(B5), p.2348-2350 energyContMKS = 9.1 / 1.5
         */
        final double energyConstMKS = 9.1 / 1.5;
        for (int i = 0; i < measCount - 1; i++) {
            if (logAmplitudes[i] > 0.0 && logAmplitudes[i + 1] > 0.0) {
                logMoment = (logAmplitudes[i] + logAmplitudes[i + 1]) / 2.0;
                mw = ((2.0 / 3.0) * logMoment - energyConstMKS);
                break;
            }
        }

        double logMomentMDAC = 1.5 * (mwMDAC + energyConstMKS); // Log10(moment)

        // Extrapolated low frequency energy
        final double lowAmp = (Math.pow(10, logMoment)) * k;

        final double wF = 2.0 * Math.PI * lowFreq[0];
        final double eTotal_low = Math.pow(lowAmp, 2) * Math.pow(wF, 3.0) / 3.0;

        // We do the same thing for the high frequency extrapolation.
        final double ahi = Math.pow(10.0, logAmplitudes[measCount - 1]) * k;
        final double eTotal_hi = Math.pow(ahi, 2) * Math.pow(wN, 3.0);

        // This is the total energy of low, observed and high
        double energyS = eTotal_low + eTotal_obs + eTotal_hi;

        // Calculate ratio of the P to S wave spectral energy
        double pContribution = 1.0
                + (Math.pow(mdacFI.getRadPatP(), 2) / Math.pow(mdacFI.getRadPatS(), 2)) * (Math.pow(mdacFI.getBetas(), 5) / Math.pow(mdacFI.getAlphas(), 5)) * Math.pow(mdacFI.getZeta(), 3);

        double energy = energyS * pContribution;

        double obsEnergy = (Math.log10(eTotal_obs));
        double logTotalEnergy = (Math.log10(energy));
        double mu = mdacFI.getRhos() * mdacFI.getBetas() * mdacFI.getBetas();
        double stressTotal = (mu * energy / (Math.pow(10, logMoment))) / 1E6;
        double logEnergyMDAC = Math.log10(((MdacCalculator.MPA_TO_PA * apparentStress * Math.pow(10, logMomentMDAC)) / mu));

        return new EnergyInfo(obsEnergy, logTotalEnergy, logEnergyMDAC, eTotal_obs / energy, stressTotal);
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
    public double[] fitMw(final Event event, final Map<FrequencyBand, SummaryStatistics> measurements, final PICK_TYPES phase, final MdacParametersFI mdacFi, final MdacParametersPS mdacPs,
            final Function<Map<Double, Double>, SortedMap<Double, Double>> weightFunction) {
        final double[] result = new double[PARAM_COUNT];

        final SortedMap<Double, Double> frequencyBands = new TreeMap<>();
        final SortedSet<Triple<Double, Double, Double>> optimizerMeasurements = Collections.synchronizedSortedSet(new TreeSet<>(mwFittingComparator));
        long dataCount = 0l;

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

        final Map<Double, Double> weightMap = weightFunction.apply(frequencyBands);
        final SynchronizedMultivariateSummaryStatistics stats = new SynchronizedMultivariateSummaryStatistics(4, false);
        final MultivariateFunction mdacFunction = point -> {
            final double testMw = point[0];
            final double testSigma = point[1];

            final Map<Object, double[]> dataMap = new HashMap<>();

            final DoubleUnaryOperator mdacFunc = mdacService.getCalculateMdacAmplitudeForMwFunction(mdacPs, mdacFi, testMw, phase, testSigma);

            for (final Entry<FrequencyBand, SummaryStatistics> meas : measurements.entrySet()) {
                final double logAmplitude = meas.getValue().getMean();
                final double lowFreq = meas.getKey().getLowFrequency();
                final double highFreq = meas.getKey().getHighFrequency();
                final double centerFreq = (highFreq + lowFreq) / 2.0;

                // Note this is in dyne-cm to match Kevin
                final double log10mdacM0 = mdacFunc.applyAsDouble(centerFreq);

                if (logAmplitude > 0.) {
                    final double[] coda_vs_mdac = new double[] { logAmplitude, log10mdacM0 };
                    dataMap.put(centerFreq, coda_vs_mdac);
                }
            }

            final double fit = WCVRMSD(weightMap, dataMap);
            final double corner = mdacService.getCornerFrequency(mdacService.getCalculateMdacSourceSpectraFunction(mdacPs, new MdacParametersFI(mdacFi).setPsi(0.0).setSigma(testSigma), testMw));
            stats.addValue(new double[] { testMw, testSigma, fit, corner });
            optimizerMeasurements.add(new ImmutableTriple<>(fit, testMw, testSigma));
            return fit;
        };

        final ConvergenceChecker<PointValuePair> convergenceChecker = new SimplePointChecker<>(0.00001, 0.00001, 100000);
        final CMAESOptimizer cmaes = new CMAESOptimizer(1000000, 0, true, 0, 10, new MersenneTwister(), false, convergenceChecker);
        final MultiStartMultivariateOptimizer optimizer = new MultiStartMultivariateOptimizer(cmaes, 10, new SobolSequenceGenerator(2));
        int iterations = iterationCutoff;
        try {
            final PointValuePair optimizerResult = runOptimizer(mdacFunction, optimizer);

            // converted back into dyne-cm to match Kevin's format
            final double testMw = optimizerResult.getPoint()[MW];
            // log10M0
            result[LOG10_M0] = Math.log10(mdacService.getMwInDyne(testMw));
            result[MW_FIT] = testMw; // best Mw fit
            // this is the number of elements that have a signal measurement
            result[DATA_COUNT] = dataCount;
            // this is the rmsfit measurement
            result[RMS_FIT] = optimizerResult.getValue();
            // this is the stress
            result[APP_STRESS] = optimizerResult.getPoint()[MPA];
            iterations = cmaes.getIterations();
        } catch (MaxCountExceededException e) {
            log.warn("Failed to converge while attempting to fit an Mw to this event {}, falling back to a grid search.", event);
        }

        if (iterations >= iterationCutoff) {
            double best = result[RMS_FIT] != 0.0 ? result[RMS_FIT] : Double.MAX_VALUE;
            for (double mw = minMW; mw < maxMW; mw = mw + ((maxMW - minMW) / 100.)) {
                for (double stress = minApparentStress; stress < maxApparentStress; stress = stress + ((maxApparentStress - minApparentStress) / 100.)) {
                    final double res = mdacFunction.value(new double[] { mw, stress });
                    final double corner = mdacService.getCornerFrequency(mdacPs, mdacFi.setPsi(0.0).setSigma(stress), mw);
                    stats.addValue(new double[] { mw, stress, res, corner });
                    optimizerMeasurements.add(new ImmutableTriple<>(res, mw, stress));
                    if (res < best) {
                        best = res;
                        result[MW_FIT] = mw;
                        result[APP_STRESS] = stress;
                    }
                    iterations++;
                }
            }

            result[LOG10_M0] = Math.log10(mdacService.getMwInDyne(result[MW_FIT]));
            result[RMS_FIT] = best;
        }

        final RealMatrix C = stats.getCovariance();

        final double[] mean = stats.getMean();
        result[MW_MEAN] = mean[MW];
        result[MW_SD] = Math.sqrt(C.getEntry(MW, MW));
        result[APP_STRESS_MEAN] = mean[MPA];
        result[APP_STRESS_SD] = Math.sqrt(C.getEntry(MPA, MPA));
        result[FIT_MEAN] = mean[FIT];
        result[FIT_SD] = Math.sqrt(C.getEntry(FIT, FIT));
        result[CORNER_FREQ_SD] = Math.sqrt(C.getEntry(CORNER, CORNER));
        result[MDAC_ENERGY] = mdacService.getEnergy(result[MW_FIT], result[APP_STRESS], mdacPs, mdacFi);

        // This is kinda wonky mathmatically but at least it roughly scales with N so
        // until I can get a stats person to eyeball this it'll have to do.
        final double SE = Math.sqrt(C.getEntry(FIT, FIT) / (stats.getN() - 2.0));
        final double f1 = result[RMS_FIT] + SE;
        final double f2 = f1 + (2.0 * SE);

        double mw1min = Double.POSITIVE_INFINITY;
        double mw1max = Double.NEGATIVE_INFINITY;
        double mw2min = Double.POSITIVE_INFINITY;
        double mw2max = Double.NEGATIVE_INFINITY;

        double as1min = Double.POSITIVE_INFINITY;
        double as1max = Double.NEGATIVE_INFINITY;
        double as2min = Double.POSITIVE_INFINITY;
        double as2max = Double.NEGATIVE_INFINITY;

        for (final Triple<Double, Double, Double> meas : optimizerMeasurements) {
            final Double fit = meas.getLeft();
            final Double mw = meas.getMiddle();
            final Double apparentStress = meas.getRight();

            if (fit < f1) {
                if (mw < mw1min) {
                    mw1min = mw;
                    as1min = apparentStress;
                }
                if (mw > mw1max) {
                    mw1max = mw;
                    as1max = apparentStress;
                }
            }
            if (fit < f2) {
                if (mw < mw2min) {
                    mw2min = mw;
                    as2min = apparentStress;
                }
                if (mw > mw2max) {
                    mw2max = mw;
                    as2max = apparentStress;
                }
            } else {
                break;
            }
        }

        result[MW_1_MIN] = mw1min;
        result[MW_1_MAX] = mw1max;
        result[MW_2_MIN] = mw2min;
        result[MW_2_MAX] = mw2max;
        if (reportStressBoundsInUQ) {
            result[APP_1_MIN] = as1min;
            result[APP_1_MAX] = as1max;
            result[APP_2_MIN] = as2min;
            result[APP_2_MAX] = as2max;
        } else {
            result[APP_1_MIN] = result[APP_STRESS];
            result[APP_1_MAX] = result[APP_STRESS];
            result[APP_2_MIN] = result[APP_STRESS];
            result[APP_2_MAX] = result[APP_STRESS];
        }
        result[CORNER_FREQ] = mdacService.getCornerFrequency(
                mdacService.getCalculateMdacSourceSpectraFunction(mdacPs, new MdacParametersFI(mdacFi).setPsi(0.0).setSigma(result[APP_STRESS]), result[MW_FIT]));
        result[ITR_COUNT] = iterations;

        return result;
    }

    private PointValuePair runOptimizer(final MultivariateFunction mdacFunction, final MultiStartMultivariateOptimizer optimizer) {
        return optimizer.optimize(
                new MaxEval(1000000),
                    new ObjectiveFunction(mdacFunction),
                    GoalType.MINIMIZE,
                    new SimpleBounds(new double[] { minMW, minApparentStress }, new double[] { maxMW, maxApparentStress }),
                    new InitialGuess(new double[] { ThreadLocalRandom.current().nextDouble(minMW, maxMW), ThreadLocalRandom.current().nextDouble(minApparentStress, maxApparentStress) }),
                    new CMAESOptimizer.Sigma(new double[] { 0.5, 1.0 }),
                    new CMAESOptimizer.PopulationSize(50));
    }

    /**
     * @param weightMap
     * @param dataMap
     * @return
     */
    protected static double WCVRMSD(final Map<Double, Double> weightMap, final Map<Object, double[]> dataMap) {
        for (final Entry<Object, double[]> entry : dataMap.entrySet()) {
            if (weightMap.containsKey(entry.getKey())) {
                final double[] val = entry.getValue();
                for (int i = 0; i < val.length; i++) {
                    val[i] *= weightMap.get(entry.getKey());
                }
                dataMap.put(entry.getKey(), val);
            }
        }
        return FitnessCriteria.CVRMSD(dataMap);
    }
}
