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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
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

    private static final double UNUSED_DISTANCE = -1d;
    private static final double DYNE_LOG10_ADJUSTMENT = 7d;
    private MdacCalculatorService mdac;
    private SiteFrequencyBandParametersService siteParamsService;
    private MeasuredMwsService measuredMwsService;
    private SpectraCalculator spectraCalc;

    @Autowired
    public SiteCalibrationServiceImpl(MdacCalculatorService mdac, SiteFrequencyBandParametersService siteParamsService, MeasuredMwsService measuredMwsService, SpectraCalculator spectraCalc) {
        this.mdac = mdac;
        this.siteParamsService = siteParamsService;
        this.measuredMwsService = measuredMwsService;
        this.spectraCalc = spectraCalc;
    }

    @Override
    public Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> measureSiteCorrections(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand, MdacParametersFI mdacFI,
            Map<PICK_TYPES, MdacParametersPS> mdacPS, Map<String, List<ReferenceMwParameters>> refMws, Map<FrequencyBand, SharedFrequencyBandParameters> frequencyBandParameters,
            PICK_TYPES selectedPhase) {

        // TODO: Validate all the input exists and is sufficient to compute a
        // useful site correction.

        MdacParametersPS psRows = mdacPS.get(selectedPhase);

        //Input
        Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> freqBandEvidStaMeasurementsMap = mapToEventAndStation(dataByFreqBand);

        Map<Event, Function<Map<Double, Double>, Map<Double, Double>>> weightFunctionMapByEvent = new HashMap<>();

        //Step 1
        Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapReferenceEvents = new HashMap<>();
        //Step 2
        Map<Event, Map<FrequencyBand, SummaryStatistics>> averageMapByEvent = new HashMap<>();
        //Step 3
        Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapAverage = new HashMap<>();
        //Result
        Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> siteCorrections = new HashMap<>();

        //0) Determine if we have a RefMW in the dataset with a GT stress drop
        //0-1A) If yes then omit everything above the corner frequency for MDAC model only events
        //0-1B) Add it to the GT spectra event map
        //0-1C) Weight the Mw fit for that event 1.0 at all bands
        //0-2A) If no then weight the corner frequency highly, the low middle, and the high low

        //1) Generate spectra for reference events and get the site correction for each station that saw it.
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();
            Double lowFreq = freqBand.getLowFrequency();
            Double highFreq = freqBand.getHighFrequency();
            double centerFreq = (highFreq + lowFreq) / 2;
            for (Entry<Event, Map<Station, SpectraMeasurement>> staMwMap : evidStaMap.getValue().entrySet()) {
                Event evid = staMwMap.getKey();
                if (refMws.containsKey(evid.getEventId())) {
                    List<ReferenceMwParameters> refMwsParams = refMws.get(evid.getEventId());
                    ReferenceMwParameters refMw = refMwsParams.stream().findFirst().orElse(null);
                    if (refMw != null) {
                        double mw = refMw.getRefMw();
                        MdacParametersFI mdacFiEntry = new MdacParametersFI(mdacFI);
                        if (refMw.getStressDropInMpa() != null && refMw.getStressDropInMpa() != 0.0) {
                            // If we know a MPA stress drop for this reference
                            // event we want to use stress instead of apparent
                            // stress so we set Psi == 0.0 to use Sigma as stress drop
                            mdacFiEntry.setSigma(refMw.getStressDropInMpa());
                            mdacFiEntry.setPsi(0.0);
                            weightFunctionMapByEvent.put(evid, this::noWeights);
                        }
                        double[] refSpectra = mdac.calculateMdacSourceSpectra(psRows, mdacFiEntry, centerFreq, mw, UNUSED_DISTANCE);

                        for (Entry<Station, SpectraMeasurement> staMwEntry : staMwMap.getValue().entrySet()) {

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
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();
            for (Entry<Event, Map<Station, SpectraMeasurement>> evidStaEntry : evidStaMap.getValue().entrySet()) {
                Event evid = evidStaEntry.getKey();
                for (Entry<Station, SpectraMeasurement> staMwEntry : evidStaEntry.getValue().entrySet()) {
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
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();
            for (Entry<Event, Map<Station, SpectraMeasurement>> evidStaEntry : evidStaMap.getValue().entrySet()) {
                Event evid = evidStaEntry.getKey();
                if (averageMapByEvent.containsKey(evid) && averageMapByEvent.get(evid).containsKey(freqBand)) {
                    for (Entry<Station, SpectraMeasurement> staMwEntry : evidStaEntry.getValue().entrySet()) {
                        if (!weightFunctionMapByEvent.containsKey(evid)) {
                            weightFunctionMapByEvent.put(evid, this::lowerFreqHigherWeights);
                        }
                        double amp = staMwEntry.getValue().getPathCorrected();
                        if (!staFreqBandSiteCorrectionMapAverage.containsKey(staMwEntry.getKey())) {
                            staFreqBandSiteCorrectionMapAverage.put(staMwEntry.getKey(), new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).containsKey(freqBand)) {
                            staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).put(freqBand, new SummaryStatistics());
                        }
                        //We want to keep the reference events as-is
                        if (staFreqBandSiteCorrectionMapReferenceEvents.containsKey(staMwEntry.getKey())
                                && staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).containsKey(freqBand)) {
                            staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).put(freqBand, staFreqBandSiteCorrectionMapReferenceEvents.get(staMwEntry.getKey()).get(freqBand));
                        } else {
                            double refAmp = averageMapByEvent.get(evid).get(freqBand).getMean();
                            double ampDiff = refAmp - amp;
                            staFreqBandSiteCorrectionMapAverage.get(staMwEntry.getKey()).get(freqBand).addValue(ampDiff);
                        }
                    }
                }
            }
        }

        //4) Re-average the events using the new site corrections 
        averageMapByEvent.clear();
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();
            for (Entry<Event, Map<Station, SpectraMeasurement>> evidStaEntry : evidStaMap.getValue().entrySet()) {
                Event evid = evidStaEntry.getKey();
                for (Entry<Station, SpectraMeasurement> staMwEntry : evidStaEntry.getValue().entrySet()) {
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
        }

        //TODO: Weight bands by number of points and std-dev instead

        // 5) Measure the MW values per event
        List<MeasuredMwParameters> measuredMws = spectraCalc.measureMws(averageMapByEvent, weightFunctionMapByEvent, PICK_TYPES.LG);

        //Return results as a set of Site corrections
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
        siteParamsService.deleteAll();
        siteParamsService.save(siteCorrections.values().parallelStream().flatMap(staMap -> staMap.values().stream()).collect(Collectors.toList()));

        measuredMwsService.deleteAll();
        measuredMwsService.save(measuredMws);

        return siteCorrections;
    }

    private Map<Double, Double> noWeights(Map<Double, Double> frequencies) {
        Map<Double, Double> weightMap = new TreeMap<>();
        for (Double frequency : frequencies.keySet()) {
            weightMap.put(frequency, 1d);
        }
        return weightMap;
    }

    private Map<Double, Double> lowerFreqHigherWeights(Map<Double, Double> frequencies) {
        Map<Double, Double> weightMap = new TreeMap<>(frequencies);
        int count = 0;
        for (Entry<Double, Double> entry : weightMap.entrySet()) {
            entry.setValue(count == 0 || count == 2 ? 0.5 : count == 1 ? 1.0 : count == 3 ? 0.25 : 0.1);
            count++;
        }
        return weightMap;
    }

    public static Map<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> mapToEventAndStation(Map<FrequencyBand, List<SpectraMeasurement>> dataByFreqBand) {

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
}
