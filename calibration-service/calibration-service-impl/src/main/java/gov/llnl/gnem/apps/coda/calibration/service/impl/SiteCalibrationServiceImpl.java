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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
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
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MdacCalculatorService;
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
    private MeasuredMwsService measuredMwsService;
    private SpectraCalculator spectraCalc;
    private ServiceConfig serviceConfig;

    @Autowired
    public SiteCalibrationServiceImpl(ServiceConfig serviceConfig, MdacCalculatorService mdac, SiteFrequencyBandParametersService siteParamsService, MeasuredMwsService measuredMwsService,
            SpectraCalculator spectraCalc) {
        this.mdac = mdac;
        this.siteParamsService = siteParamsService;
        this.measuredMwsService = measuredMwsService;
        this.spectraCalc = spectraCalc;
        this.serviceConfig = serviceConfig;
    }

    @Override
    public List<MeasuredMwParameters> fitMws(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand, MdacParametersFI mdacFI, Map<PICK_TYPES, MdacParametersPS> mdacPS,
            Map<String, List<ReferenceMwParameters>> refMws, Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> stationFrequencyBandParameters, PICK_TYPES selectedPhase) {
        MdacParametersPS psRows = mdacPS.get(selectedPhase);
        Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqBandStaMeasurementsMap = mapToEventAndStation(dataByFreqBand);
        Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> weightFunctionMapByEvent = new HashMap<>();
        Map<Event, Map<FrequencyBand, SummaryStatistics>> averageMapByEvent = new HashMap<>();

        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                FrequencyBand freqBand = freqStaMap.getKey();
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
                    }
                }
            }
            weightFunctionMapByEvent.putIfAbsent(evid, createDataWeightMapFunction(averageMapByEvent.get(evid)));
        }
        return spectraCalc.measureMws(averageMapByEvent, weightFunctionMapByEvent, selectedPhase, psRows, mdacFI);
    }

    @Override
    public Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> measureSiteCorrections(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand, MdacParametersFI mdacFI,
            Map<PICK_TYPES, MdacParametersPS> mdacPS, Map<String, List<ReferenceMwParameters>> refMws, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters,
            PICK_TYPES selectedPhase) {
        MdacParametersPS psRows = mdacPS.get(selectedPhase);

        //Input
        Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqBandStaMeasurementsMap = mapToEventAndStation(dataByFreqBand);

        Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> weightFunctionMapByEvent = new HashMap<>();

        //Step 1
        Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapReferenceEvents = new HashMap<>();
        //Step 2
        Map<Event, Map<FrequencyBand, SummaryStatistics>> averageMapByEvent = new HashMap<>();
        //Step 3
        Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapAverage = new HashMap<>();
        //Result
        Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> siteCorrections = new HashMap<>();

        //0) Determine if we have a RefMw in the dataset with a GT stress
        boolean hasGtSpectra = refMws.values().stream().flatMap(refs -> refs.stream()).filter(ref -> ref.getRefApparentStressInMpa() != null).anyMatch(ref -> ref.getRefApparentStressInMpa() > 0.0);
        //0-1A) If yes then omit everything above the corner frequency for MDAC model only events
        //0-1B) Add it to the GT spectra event map
        //0-1C) Weight the Mw fit for that event 1.0 at all bands for that event
        //0-2A) If no then weight based on the standard error of the bands + 1

        //1) Generate spectra for reference events and get the site correction for each station that saw it.
        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            if (refMws != null && refMws.containsKey(evid.getEventId())) {
                List<ReferenceMwParameters> refMwsParams = refMws.get(evid.getEventId());
                ReferenceMwParameters refMw = refMwsParams.stream().findFirst().orElse(null);
                if (refMw != null) {
                    double mw = refMw.getRefMw();
                    boolean evidHasSpectra = false;
                    MdacParametersFI mdacFiEntry = new MdacParametersFI(mdacFI);
                    if (refMw.getRefApparentStressInMpa() != null && refMw.getRefApparentStressInMpa() != 0.0) {
                        mdacFiEntry.setSigma(refMw.getRefApparentStressInMpa());
                        mdacFiEntry.setPsi(0.0);
                        weightFunctionMapByEvent.put(evid, this::evenWeights);
                        evidHasSpectra = true;
                    } else if (serviceConfig.isSpectraTruncationEnabled() && hasGtSpectra) {
                        weightFunctionMapByEvent.put(evid, this::evenWeights);
                    }

                    Function<Double, double[]> mdacFunc = mdac.getCalculateMdacSourceSpectraFunction(psRows, mdacFiEntry, mw);
                    double cornerFreq = 0.0;
                    if (hasGtSpectra && !evidHasSpectra) {
                        cornerFreq = mdac.getCornerFrequency(mdacFunc);
                    }

                    for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                        FrequencyBand freqBand = freqStaMap.getKey();
                        Double lowFreq = freqBand.getLowFrequency();
                        Double highFreq = freqBand.getHighFrequency();

                        if (serviceConfig.isSpectraTruncationEnabled() && cornerFreq > 0.0 && highFreq > cornerFreq) {
                            continue;
                        }
                        double centerFreq = (highFreq + lowFreq) / 2.0;

                        double[] refSpectra = mdacFunc.apply(centerFreq);

                        for (Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {

                            double amp = staMwEntry.getValue().getPathCorrected();
                            if (!staFreqBandSiteCorrectionMapReferenceEvents.containsKey(staMwEntry.getKey())) {
                                staFreqBandSiteCorrectionMapReferenceEvents.put(staMwEntry.getKey(), new HashMap<FrequencyBand, SummaryStatistics>());
                            }
                            if (!staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).containsKey(freqBand)) {
                                staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).put(freqBand, new SummaryStatistics());
                            }

                            // Output should be Dyne-cm
                            double refAmp = Math.log10(refSpectra[1]) + DYNE_LOG10_ADJUSTMENT;
                            double ampDiff = refAmp - amp;

                            staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).get(freqBand).addValue(ampDiff);
                        }
                    }
                }
            }
        }

        //2) For every station with a site correction measured apply it to every other event and get average site term for every frequency band
        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                FrequencyBand freqBand = freqStaMap.getKey();
                for (Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                    if (staFreqBandSiteCorrectionMapReferenceEvents.containsKey(staMwEntry.getKey()) && staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).containsKey(freqBand)) {
                        double amp = staMwEntry.getValue().getPathCorrected();
                        if (!averageMapByEvent.containsKey(evid)) {
                            averageMapByEvent.put(evid, new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!averageMapByEvent.get(evid).containsKey(freqBand)) {
                            averageMapByEvent.get(evid).put(freqBand, new SummaryStatistics());
                        }

                        double refAmp = amp + staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).get(freqBand).getMean();
                        averageMapByEvent.get(evid).get(freqBand).addValue(refAmp);
                    }
                }
            }
        }

        //3) For all measurements offset by the average site term for each station/frequency band to get the final site terms
        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                FrequencyBand freqBand = freqStaMap.getKey();
                if (averageMapByEvent.containsKey(evid) && averageMapByEvent.get(evid).containsKey(freqBand)) {
                    for (Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                        double amp = staMwEntry.getValue().getPathCorrected();
                        if (!staFreqBandSiteCorrectionMapAverage.containsKey(staMwEntry.getKey())) {
                            staFreqBandSiteCorrectionMapAverage.put(staMwEntry.getKey(), new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).containsKey(freqBand)) {
                            staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).put(freqBand, new SummaryStatistics());
                        }
                        double refAmp = averageMapByEvent.get(evid).get(freqBand).getMean();
                        double ampDiff = refAmp - amp;
                        staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).get(freqBand).addValue(ampDiff);
                    }
                }
            }
        }

        //4) Re-average the events using the new site corrections
        averageMapByEvent.clear();
        for (Entry<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> evidFreqMap : evidFreqBandStaMeasurementsMap.entrySet()) {
            Event evid = evidFreqMap.getKey();
            for (Entry<FrequencyBand, Map<Station, SpectraMeasurement>> freqStaMap : evidFreqMap.getValue().entrySet()) {
                FrequencyBand freqBand = freqStaMap.getKey();
                for (Entry<Station, SpectraMeasurement> staMwEntry : freqStaMap.getValue().entrySet()) {
                    if (staFreqBandSiteCorrectionMapAverage.containsKey(staMwEntry.getKey()) && staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).containsKey(freqBand)) {
                        double amp = staMwEntry.getValue().getPathCorrected();
                        if (!averageMapByEvent.containsKey(evid)) {
                            averageMapByEvent.put(evid, new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!averageMapByEvent.get(evid).containsKey(freqBand)) {
                            averageMapByEvent.get(evid).put(freqBand, new SummaryStatistics());
                        }
                        double refAmp = amp + staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).get(freqBand).getMean();
                        averageMapByEvent.get(evid).get(freqBand).addValue(refAmp);
                    }
                }
            }
            weightFunctionMapByEvent.putIfAbsent(evid, createDataWeightMapFunction(averageMapByEvent.get(evid)));
        }

        // 5) Convert average map into a set of Site correction objects
        for (Entry<Station, Map<FrequencyBand, SummaryStatistics>> freqBandCorrectionMap : staFreqBandSiteCorrectionMapAverage.entrySet()) {
            Station station = freqBandCorrectionMap.getKey();
            Set<Entry<FrequencyBand, SummaryStatistics>> bandEntries = freqBandCorrectionMap.getValue().entrySet();

            for (Entry<FrequencyBand, SummaryStatistics> freqBandEntry : bandEntries) {
                SiteFrequencyBandParameters siteParam = new SiteFrequencyBandParameters();
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
        List<MeasuredMwParameters> measuredMws = spectraCalc.measureMws(averageMapByEvent, weightFunctionMapByEvent, selectedPhase, psRows, mdacFI);

        overwriteMeasuredMws(measuredMws);
        return siteCorrections;
    }

    private void overwriteMeasuredMws(List<MeasuredMwParameters> measuredMws) {
        measuredMwsService.deleteAll();
        measuredMwsService.save(measuredMws);
    }

    private void overwriteSiteParams(Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> siteCorrections) {
        siteParamsService.deleteAll();
        siteParamsService.save(siteCorrections.values().parallelStream().flatMap(staMap -> staMap.values().stream()).collect(Collectors.toList()));
    }

    private SortedMap<Double, Double> evenWeights(Map<Double, Double> frequencies) {
        SortedMap<Double, Double> weightMap = new TreeMap<>();
        for (Double frequency : frequencies.keySet()) {
            weightMap.put(frequency, Double.valueOf(1d));
        }
        return weightMap;
    }

    private Function<Map<Double, Double>, SortedMap<Double, Double>> createDataWeightMapFunction(Map<FrequencyBand, SummaryStatistics> data) {
        return (Map<Double, Double> frequencies) -> {
            SortedMap<Double, Double> weightMap = new TreeMap<>();
            Map<Double, SummaryStatistics> rawData = new HashMap<>();
            if (data != null) {
                data.entrySet().forEach(entry -> rawData.put((entry.getKey().getHighFrequency() + entry.getKey().getLowFrequency()) / 2.0, entry.getValue()));
            }
            for (Double frequency : frequencies.keySet()) {
                SummaryStatistics stats = rawData.getOrDefault(frequency, new SummaryStatistics());
                
                Double weight;
                if (stats.getN() > 1 && Double.isFinite(stats.getStandardDeviation())) {
                    weight = 1d + 1.0/(stats.getStandardDeviation()/Math.sqrt(stats.getN()));
                }
                else {
                    weight = Double.valueOf(1d);
                }
                if (!Double.isFinite(weight)) {
                    weight = Double.valueOf(1d);
                }
                weightMap.put(frequency, weight);
            }
            return weightMap;
        };
    }

    public static Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> mapToEventAndStation(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand) {
        Map<Event, Map<FrequencyBand, Map<Station, SpectraMeasurement>>> data = new HashMap<>();
        if (dataByFreqBand != null) {
            for (Entry<FrequencyBand, List<SpectraMeasurement>> entries : dataByFreqBand.entrySet()) {
                for (SpectraMeasurement entry : entries.getValue()) {
                    if (WaveformUtils.isValidWaveform(entry.getWaveform())) {
                        Event event = entry.getWaveform().getEvent();
                        Station station = entry.getWaveform().getStream().getStation();
                        if (!data.containsKey(event)) {
                            data.put(event, new HashMap<>());
                        }
                        Map<FrequencyBand, Map<Station, SpectraMeasurement>> bandMap = data.get(event);
                        if (!bandMap.containsKey(entries.getKey())) {
                            bandMap.put(entries.getKey(), new HashMap<>());
                        }
                        Map<Station, SpectraMeasurement> stationMap = bandMap.get(entries.getKey());
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

    public SiteCalibrationServiceImpl setMdac(MdacCalculatorService mdac) {
        this.mdac = mdac;
        return this;
    }

    public SiteFrequencyBandParametersService getSiteParamsService() {
        return siteParamsService;
    }

    public SiteCalibrationServiceImpl setSiteParamsService(SiteFrequencyBandParametersService siteParamsService) {
        this.siteParamsService = siteParamsService;
        return this;
    }

    public MeasuredMwsService getMeasuredMwsService() {
        return measuredMwsService;
    }

    public SiteCalibrationServiceImpl setMeasuredMwsService(MeasuredMwsService measuredMwsService) {
        this.measuredMwsService = measuredMwsService;
        return this;
    }

    public SpectraCalculator getSpectraCalc() {
        return spectraCalc;
    }

    public SiteCalibrationServiceImpl setSpectraCalc(SpectraCalculator spectraCalc) {
        this.spectraCalc = spectraCalc;
        return this;
    }

    public ServiceConfig getServiceConfig() {
        return serviceConfig;
    }

    public SiteCalibrationServiceImpl setServiceConfig(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        return this;
    }
}
