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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Event;
import gov.llnl.gnem.apps.coda.calibration.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Station;
import gov.llnl.gnem.apps.coda.calibration.model.domain.util.PICK_TYPES;
import gov.llnl.gnem.apps.coda.calibration.service.api.MeasuredMwsService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteCalibrationService;
import gov.llnl.gnem.apps.coda.calibration.service.api.SiteFrequencyBandParametersService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.MdacCalculatorService;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.SpectraCalculator;
import gov.llnl.gnem.apps.coda.calibration.service.impl.processing.WaveformUtils;

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

        //Step 1
        Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapMdac = new HashMap<>();
        //Step 2
        Map<Event, Map<FrequencyBand, SummaryStatistics>> averageMapByEvent = new HashMap<>();
        //Step 3
        Map<Station, Map<FrequencyBand, SummaryStatistics>> staFreqBandSiteCorrectionMapAverage = new HashMap<>();
        //Result
        Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> siteCorrections = new HashMap<>();

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
                        MdacParametersFI mdacFiEntry = mdacFI;
                        if (refMw.getStressDropInMpa() != null) {
                            // If we know a MPA stress drop for this reference
                            // event we want to use stress instead of apparent
                            // stress
                            // so we set Psi == 0.0 to use Sigma as stress drop
                            mdacFiEntry.setSigma(refMw.getStressDropInMpa());
                            mdacFiEntry.setPsi(0.0);
                        }
                        double[] refSpectra = mdac.calculateMdacSourceSpectra(psRows, mdacFiEntry, centerFreq, mw, UNUSED_DISTANCE);

                        for (Entry<Station, SpectraMeasurement> staMwEntry : staMwMap.getValue().entrySet()) {

                            double amp = staMwEntry.getValue().getPathCorrected();

                            if (!staFreqBandSiteCorrectionMapMdac.containsKey(staMwEntry.getKey())) {
                                staFreqBandSiteCorrectionMapMdac.put(staMwEntry.getKey(), new HashMap<FrequencyBand, SummaryStatistics>());
                            }
                            if (!staFreqBandSiteCorrectionMapMdac.get(staMwEntry.getKey()).containsKey(freqBand)) {
                                staFreqBandSiteCorrectionMapMdac.get(staMwEntry.getKey()).put(freqBand, new SummaryStatistics());
                            }

                            // Output should be Dyne-cm
                            double refAmp = Math.log10(refSpectra[1]) + DYNE_LOG10_ADJUSTMENT;
                            double ampDiff = refAmp - amp;

                            staFreqBandSiteCorrectionMapMdac.get(staMwEntry.getKey()).get(freqBand).addValue(ampDiff);
                        }
                    }
                }
            }
        }

        //2) For every station with a site correction measured apply it to every other event and get average site term for every frequency band
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();

            for (Entry<Event, Map<Station, SpectraMeasurement>> staMwMap : evidStaMap.getValue().entrySet()) {
                Event evid = staMwMap.getKey();
                for (Entry<Station, SpectraMeasurement> staMwEntry : staMwMap.getValue().entrySet()) {
                    if (staFreqBandSiteCorrectionMapMdac.containsKey(staMwEntry.getKey()) && staFreqBandSiteCorrectionMapMdac.get(staMwEntry.getKey()).containsKey(freqBand)) {

                        double amp = staMwEntry.getValue().getPathCorrected();
                        if (!averageMapByEvent.containsKey(evid)) {
                            averageMapByEvent.put(evid, new HashMap<FrequencyBand, SummaryStatistics>());
                        }
                        if (!averageMapByEvent.get(evid).containsKey(freqBand)) {
                            averageMapByEvent.get(evid).put(freqBand, new SummaryStatistics());
                        }

                        double refAmp = amp + staFreqBandSiteCorrectionMapMdac.get(staMwEntry.getKey()).get(freqBand).getMean();

                        averageMapByEvent.get(evid).get(freqBand).addValue(refAmp);
                    }
                }
            }
        }

        //3) Offset the original measurements by the average site term for each station/frequency band to get the final site terms
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();

            for (Entry<Event, Map<Station, SpectraMeasurement>> staMwMap : evidStaMap.getValue().entrySet()) {
                Event evid = staMwMap.getKey();
                if (averageMapByEvent.containsKey(evid) && averageMapByEvent.get(evid).containsKey(freqBand)) {
                    for (Entry<Station, SpectraMeasurement> staMwEntry : staMwMap.getValue().entrySet()) {
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
        for (Entry<FrequencyBand, Map<Event, Map<Station, SpectraMeasurement>>> evidStaMap : freqBandEvidStaMeasurementsMap.entrySet()) {
            FrequencyBand freqBand = evidStaMap.getKey();

            for (Entry<Event, Map<Station, SpectraMeasurement>> staMwMap : evidStaMap.getValue().entrySet()) {
                Event evid = staMwMap.getKey();
                for (Entry<Station, SpectraMeasurement> staMwEntry : staMwMap.getValue().entrySet()) {
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

        // 5) Measure the MW values per event
        List<MeasuredMwParameters> measuredMws = spectraCalc.measureMws(averageMapByEvent, PICK_TYPES.LG);

        //Return results as a set of Site corrections
        for (Entry<Station, Map<FrequencyBand, SummaryStatistics>> freqBandCorrectionMap : staFreqBandSiteCorrectionMapAverage.entrySet()) {
            Station station = freqBandCorrectionMap.getKey();

            for (Entry<FrequencyBand, SummaryStatistics> freqBandEntry : freqBandCorrectionMap.getValue().entrySet()) {
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
