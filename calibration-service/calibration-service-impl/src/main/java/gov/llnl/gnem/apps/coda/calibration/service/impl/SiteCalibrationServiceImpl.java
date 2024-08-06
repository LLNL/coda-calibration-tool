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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.calibration.repository.SharedFrequencyBandParametersRepository;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MdacCalculatorService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MwMeasurementInputData;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.common.service.util.WaveformUtils;

@Service
public class SiteCalibrationServiceImpl implements SiteCalibrationService {

    private static final Logger log = LoggerFactory.getLogger(SiteCalibrationServiceImpl.class);

    private static final double DYNE_LOG10_ADJUSTMENT = 7d;

    private MdacCalculatorService mdac;
    private SiteFrequencyBandParametersService siteParamsService;
    private SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository;
    private MeasuredMwsService measuredMwsService;
    private SpectraCalculator spectraCalc;
    private ServiceConfig serviceConfig;

    @Autowired
    public SiteCalibrationServiceImpl(ServiceConfig serviceConfig, MdacCalculatorService mdac, SiteFrequencyBandParametersService siteParamsService, MeasuredMwsService measuredMwsService,
            SpectraCalculator spectraCalc, SharedFrequencyBandParametersRepository sharedFrequencyBandParametersRepository) {
        this.mdac = mdac;
        this.siteParamsService = siteParamsService;
        this.measuredMwsService = measuredMwsService;
        this.spectraCalc = spectraCalc;
        this.serviceConfig = serviceConfig;
        this.sharedFrequencyBandParametersRepository = sharedFrequencyBandParametersRepository;
    }

    @Override
    public List<MeasuredMwParameters> fitMws(final Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand, final MdacParametersFI mdacFI, final Map<PICK_TYPES, MdacParametersPS> mdacPS,
            final Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> stationFrequencyBandParameters, final PICK_TYPES selectedPhase) {
        final MdacParametersPS psRows = mdacPS.get(selectedPhase);
        final Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqBandStaMeasurementsMap = mapToEventAndStation(dataByFreqBand);
        final Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> weightFunctionMapByEvent = new HashMap<>();
        final Map<Event, Map<FrequencyBand, SummaryStatistics>> averageMapByEvent = new HashMap<>();

        final int totalFreqBands = sharedFrequencyBandParametersRepository.findDistinctFrequencyBands().size();
        Map<String, Integer> stationCount = new HashMap<>();
        Map<String, Double> bandCoverage = new HashMap<>();

        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            Set<String> stations = new HashSet<>();
            Double totalFreqsMeasured = 0.0;

            for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                FrequencyBand freqBand = freqStaMap.getKey();
                boolean freqWasMeasured = false;
                for (Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                    double amp = staMwEntry.getValue().getPathAndSiteCorrected();

                    if (amp != 0.0) {
                        if (!averageMapByEvent.containsKey(evid)) {
                            averageMapByEvent.put(evid, new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!averageMapByEvent.get(evid).containsKey(freqBand)) {
                            averageMapByEvent.get(evid).put(freqBand, new SummaryStatistics());
                        }
                        averageMapByEvent.get(evid).get(freqBand).addValue(amp);
                        freqWasMeasured = true;

                        Station sta = staMwEntry.getValue().getWaveform().getStream().getStation();
                        stations.add(sta.getNetworkName() + sta.getStationName());
                    }
                }

                if (freqWasMeasured) {
                    totalFreqsMeasured += 1;
                }
            }
            stationCount.put(evid.getEventId(), stations.size());

            if (totalFreqBands > 0) {
                bandCoverage.put(evid.getEventId(), totalFreqsMeasured / totalFreqBands);
            } else {
                bandCoverage.put(evid.getEventId(), 0.0);
            }

            weightFunctionMapByEvent.putIfAbsent(evid, createDataWeightMapFunction(averageMapByEvent.get(evid)));
        }
        MwMeasurementInputData inputData = new MwMeasurementInputData(averageMapByEvent, weightFunctionMapByEvent, psRows, stationCount, bandCoverage);
        List<MeasuredMwParameters> measuredMws = spectraCalc.measureMws(inputData, selectedPhase, mdacFI);
        measuredMws.forEach(measuredMw -> {
            measuredMw.setStationCount(stationCount.get(measuredMw.getEventId()));
        });

        return measuredMws;
    }

    @Override
    public Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> measureSiteCorrections(final Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand, final MdacParametersFI mdacFI,
            final Map<PICK_TYPES, MdacParametersPS> mdacPS, final Map<String, List<ReferenceMwParameters>> refMws, Map<String, List<ValidationMwParameters>> valMws,
            final Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters, final PICK_TYPES selectedPhase) {
        final MdacParametersPS psRows = mdacPS.get(selectedPhase);

        //Input
        final Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqBandStaMeasurementsMap = mapToEventAndStation(dataByFreqBand);

        final Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> weightFunctionMapByEvent = new HashMap<>();

        //Step 1
        final Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapReferenceEvents = new HashMap<>();
        //Step 2
        final Map<Event, Map<FrequencyBand, SummaryStatistics>> averageMapByEvent = new HashMap<>();
        //Step 3
        final Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapAverage = new HashMap<>();
        //Result
        final Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> siteCorrections = new HashMap<>();

        // Get total frequency count for bandCoverage calculation
        final int totalFreqBands = sharedFrequencyBandParametersRepository.findDistinctFrequencyBands().size();
        Map<String, Integer> stationCount = new HashMap<>();
        Map<String, Double> bandCoverage = new HashMap<>();

        //0) Determine if we have a RefMw in the dataset with a GT stress
        boolean hasGtSpectra = refMws.values().stream().flatMap(List::stream).filter(ref -> ref.getRefApparentStressInMpa() != null).anyMatch(ref -> ref.getRefApparentStressInMpa() > 0.0);
        //0-1A) If yes then omit everything above the corner frequency for MDAC model only events
        //0-1B) Add it to the GT spectra event map
        //0-1C) Weight the Mw fit for that event 1.0 at all bands for that event
        //0-2A) If no then weight based on the standard error of the bands + 1

        //1) Generate spectra for reference events and get the site correction for each station that saw it.
        for (final Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            final Event evid = evidFreqMap.getKey();
            if (refMws != null && refMws.containsKey(evid.getEventId())) {
                final List<ReferenceMwParameters> refMwsParams = refMws.get(evid.getEventId());
                final ReferenceMwParameters refMw = refMwsParams.stream().findFirst().orElse(null);
                if (refMw != null) {
                    final double mw = refMw.getRefMw();
                    boolean evidHasSpectra = false;
                    final MdacParametersFI mdacFiEntry = new MdacParametersFI(mdacFI);
                    if (refMw.getRefApparentStressInMpa() != null && refMw.getRefApparentStressInMpa() != 0.0) {
                        mdacFiEntry.setSigma(refMw.getRefApparentStressInMpa());
                        mdacFiEntry.setPsi(0.0);
                        weightFunctionMapByEvent.put(evid, this::evenWeights);
                        evidHasSpectra = true;
                    } else if (serviceConfig.isSpectraTruncationEnabled() && hasGtSpectra) {
                        weightFunctionMapByEvent.put(evid, this::evenWeights);
                    }

                    DoubleFunction<double[]> mdacFunc = mdac.getCalculateMdacSourceSpectraFunction(psRows, mdacFiEntry, mw);
                    double cornerFreq = 0.0;
                    if (hasGtSpectra && !evidHasSpectra) {
                        cornerFreq = mdac.getCornerFrequency(mdacFunc);
                    }

                    for (final Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                        final FrequencyBand freqBand = freqStaMap.getKey();
                        final Double lowFreq = freqBand.getLowFrequency();
                        final Double highFreq = freqBand.getHighFrequency();

                        if (serviceConfig.isSpectraTruncationEnabled() && cornerFreq > 0.0 && highFreq > cornerFreq) {
                            continue;
                        }
                        final double centerFreq = (highFreq + lowFreq) / 2.0;

                        final double[] refSpectra = mdacFunc.apply(centerFreq);

                        for (final Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {

                            final double amp = staMwEntry.getValue().getPathCorrected();
                            if (!staFreqBandSiteCorrectionMapReferenceEvents.containsKey(staMwEntry.getKey())) {
                                staFreqBandSiteCorrectionMapReferenceEvents.put(staMwEntry.getKey(), new HashMap<FrequencyBand, SummaryStatistics>());
                            }
                            if (!staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).containsKey(freqBand)) {
                                staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).put(freqBand, new SummaryStatistics());
                            }

                            // Output should be Dyne-cm
                            final double refAmp = Math.log10(refSpectra[1]) + DYNE_LOG10_ADJUSTMENT;
                            final double ampDiff = refAmp - amp;

                            staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).get(freqBand).addValue(ampDiff);
                        }
                    }
                }
            }
        }

        //2) For every station with a site correction measured apply it to every other event and get average site term for every frequency band
        for (final Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            final Event evid = evidFreqMap.getKey();
            if ((refMws != null && refMws.containsKey(evid.getEventId())) || (valMws == null || valMws.isEmpty() || !valMws.containsKey(evid.getEventId()))) {
                for (final Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                    final FrequencyBand freqBand = freqStaMap.getKey();
                    for (final Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                        if (staFreqBandSiteCorrectionMapReferenceEvents.containsKey(staMwEntry.getKey())
                                && staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).containsKey(freqBand)) {
                            final double amp = staMwEntry.getValue().getPathCorrected();
                            if (!averageMapByEvent.containsKey(evid)) {
                                averageMapByEvent.put(evid, new HashMap<FrequencyBand, SummaryStatistics>());
                            }
                            if (!averageMapByEvent.get(evid).containsKey(freqBand)) {
                                averageMapByEvent.get(evid).put(freqBand, new SummaryStatistics());
                            }

                            final double refAmp = amp + staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).get(freqBand).getMean();
                            averageMapByEvent.get(evid).get(freqBand).addValue(refAmp);
                        }
                    }
                }
            }
        }

        //3) For all measurements offset by the average site term for each station/frequency band to get the final site terms
        for (final Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            final Event evid = evidFreqMap.getKey();
            if ((refMws != null && refMws.containsKey(evid.getEventId())) || (valMws == null || valMws.isEmpty() || !valMws.containsKey(evid.getEventId()))) {
                for (final Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                    final FrequencyBand freqBand = freqStaMap.getKey();
                    if (averageMapByEvent.containsKey(evid) && averageMapByEvent.get(evid).containsKey(freqBand)) {
                        for (final Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                            final double amp = staMwEntry.getValue().getPathCorrected();
                            if (!staFreqBandSiteCorrectionMapAverage.containsKey(staMwEntry.getKey())) {
                                staFreqBandSiteCorrectionMapAverage.put(staMwEntry.getKey(), new HashMap<FrequencyBand, SummaryStatistics>());
                            }
                            if (!staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).containsKey(freqBand)) {
                                staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).put(freqBand, new SummaryStatistics());
                            }
                            final double refAmp = averageMapByEvent.get(evid).get(freqBand).getMean();
                            final double ampDiff = refAmp - amp;
                            staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).get(freqBand).addValue(ampDiff);
                        }
                    }
                }
            }
        }

        //4) Re-average the events using the new site corrections
        averageMapByEvent.clear();

        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            Set<String> stations = new HashSet<>();
            Double totalFreqsMeasured = 0.0;

            for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                FrequencyBand freqBand = freqStaMap.getKey();
                boolean freqWasMeasured = false;

                for (Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                    if (staFreqBandSiteCorrectionMapAverage.containsKey(staMwEntry.getKey()) && staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).containsKey(freqBand)) {
                        final double amp = staMwEntry.getValue().getPathCorrected();
                        if (!averageMapByEvent.containsKey(evid)) {
                            averageMapByEvent.put(evid, new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!averageMapByEvent.get(evid).containsKey(freqBand)) {
                            averageMapByEvent.get(evid).put(freqBand, new SummaryStatistics());
                        }
                        final double refAmp = amp + staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).get(freqBand).getMean();
                        averageMapByEvent.get(evid).get(freqBand).addValue(refAmp);

                        if (amp != 0.0) {
                            freqWasMeasured = true;

                            Station sta = staMwEntry.getValue().getWaveform().getStream().getStation();
                            stations.add(sta.getNetworkName() + sta.getStationName());
                        }
                    }
                }

                if (freqWasMeasured) {
                    totalFreqsMeasured += 1;
                }
            }
            stationCount.put(evid.getEventId(), stations.size());

            if (totalFreqBands > 0) {
                bandCoverage.put(evid.getEventId(), totalFreqsMeasured / totalFreqBands);
            } else {
                bandCoverage.put(evid.getEventId(), 0.0);
            }
            weightFunctionMapByEvent.putIfAbsent(evid, createDataWeightMapFunction(averageMapByEvent.get(evid)));
        }

        // 5) Convert average map into a set of Site correction objects
        for (final Entry<Station, Map<FrequencyBand, SummaryStatistics>> freqBandCorrectionMap : staFreqBandSiteCorrectionMapAverage.entrySet()) {
            final Station station = freqBandCorrectionMap.getKey();
            final Set<Entry<FrequencyBand, SummaryStatistics>> bandEntries = freqBandCorrectionMap.getValue().entrySet();

            for (final Entry<FrequencyBand, SummaryStatistics> freqBandEntry : bandEntries) {
                final SiteFrequencyBandParameters siteParam = new SiteFrequencyBandParameters();
                siteParam.setStation(station);
                siteParam.setHighFrequency(freqBandEntry.getKey().getHighFrequency());
                siteParam.setLowFrequency(freqBandEntry.getKey().getLowFrequency());
                siteParam.setSiteTerm(freqBandEntry.getValue().getMean());
                if (!siteCorrections.containsKey(freqBandEntry.getKey())) {
                    siteCorrections.put(freqBandEntry.getKey(), new HashMap<>());
                }
                siteCorrections.get(freqBandEntry.getKey()).put(station, siteParam);
            }
        }
        overwriteSiteParams(siteCorrections);

        // 6) Measure the MW values per event
        MwMeasurementInputData inputData = new MwMeasurementInputData(averageMapByEvent, weightFunctionMapByEvent, psRows, stationCount, bandCoverage);
        List<MeasuredMwParameters> measuredMws = spectraCalc.measureMws(inputData, selectedPhase, mdacFI);

        overwriteMeasuredMws(measuredMws);
        return siteCorrections;
    }

    private void overwriteMeasuredMws(final List<MeasuredMwParameters> measuredMws) {
        measuredMwsService.deleteAll();
        measuredMwsService.save(measuredMws);
    }

    private void overwriteSiteParams(final Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> siteCorrections) {
        siteParamsService.deleteAll();
        siteParamsService.save(siteCorrections.values().parallelStream().flatMap(staMap -> staMap.values().stream()).collect(Collectors.toList()));
    }

    private SortedMap<Double, Double> evenWeights(final Map<Double, Double> frequencies) {
        final SortedMap<Double, Double> weightMap = new TreeMap<>();
        for (final Double frequency : frequencies.keySet()) {
            weightMap.put(frequency, Double.valueOf(1d));
        }
        return weightMap;
    }

    private Function<Map<Double, Double>, SortedMap<Double, Double>> createDataWeightMapFunction(final Map<FrequencyBand, SummaryStatistics> data) {
        return (final Map<Double, Double> frequencies) -> {
            final SortedMap<Double, Double> weightMap = new TreeMap<>();
            final Map<Double, SummaryStatistics> rawData = new HashMap<>();
            if (data != null) {
                data.entrySet().forEach(entry -> rawData.put((entry.getKey().getHighFrequency() + entry.getKey().getLowFrequency()) / 2.0, entry.getValue()));
            }
            double maxWeight = 2.0;
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
                    weight = maxWeight;
                }
                weightMap.put(frequency, weight);
            }
            return weightMap;
        };
    }

    public static Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> mapToEventAndStation(final Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand) {
        final Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> data = new HashMap<>();
        if (dataByFreqBand != null) {
            for (final Entry<FrequencyBand, List<SpectraMeasurement>> entries : dataByFreqBand.entrySet()) {
                for (final SpectraMeasurement entry : entries.getValue()) {
                    if (WaveformUtils.isValidWaveform(entry.getWaveform())) {
                        final Event event = entry.getWaveform().getEvent();
                        final Station station = entry.getWaveform().getStream().getStation();
                        if (!data.containsKey(event)) {
                            data.put(event, new HashMap<>());
                        }
                        final Map<FrequencyBand, Map<Station, SpectraMeasurement>> bandMap = data.get(event);
                        if (!bandMap.containsKey(entries.getKey())) {
                            bandMap.put(entries.getKey(), new HashMap<>());
                        }
                        final Map<Station, SpectraMeasurement> stationMap = bandMap.get(entries.getKey());
                        stationMap.put(station, entry);
                    } else {
                        log.debug("No valid waveform for {}", entry);
                    }
                }
            }
        }
        return data;
    }

    public MdacCalculatorService getMdac() {
        return mdac;
    }

    public SiteCalibrationServiceImpl setMdac(final MdacCalculatorService mdac) {
        this.mdac = mdac;
        return this;
    }

    public SiteFrequencyBandParametersService getSiteParamsService() {
        return siteParamsService;
    }

    public SiteCalibrationServiceImpl setSiteParamsService(final SiteFrequencyBandParametersService siteParamsService) {
        this.siteParamsService = siteParamsService;
        return this;
    }

    public MeasuredMwsService getMeasuredMwsService() {
        return measuredMwsService;
    }

    public SiteCalibrationServiceImpl setMeasuredMwsService(final MeasuredMwsService measuredMwsService) {
        this.measuredMwsService = measuredMwsService;
        return this;
    }

    public SpectraCalculator getSpectraCalc() {
        return spectraCalc;
    }

    public SiteCalibrationServiceImpl setSpectraCalc(final SpectraCalculator spectraCalc) {
        this.spectraCalc = spectraCalc;
        return this;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public SiteCalibrationServiceImpl setServiceConfig(final ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        return this;
    }
}
