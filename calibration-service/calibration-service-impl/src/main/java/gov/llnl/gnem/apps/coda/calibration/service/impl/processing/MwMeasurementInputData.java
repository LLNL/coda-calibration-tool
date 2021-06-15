package gov.llnl.gnem.apps.coda.calibration.service.impl.processing;

import java.util.Map;
import java.util.SortedMap;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.common.model.domain.Event;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;

public class MwMeasurementInputData {
    private Map<Event, Map<FrequencyBand, SummaryStatistics>> evidMap;
    private Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights;
    private MdacParametersPS mdacPs;
    private Map<String, Integer> stationCount;
    private Map<String, Double> bandCoverageMetric;

    public MwMeasurementInputData(Map<Event, Map<FrequencyBand, SummaryStatistics>> evidMap, Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights, MdacParametersPS mdacPs,
            Map<String, Integer> stationCount, Map<String, Double> bandCoverageMetric) {
        this.evidMap = evidMap;
        this.eventWeights = eventWeights;
        this.mdacPs = mdacPs;
        this.stationCount = stationCount;
        this.bandCoverageMetric = bandCoverageMetric;
    }

    public Map<Event, Map<FrequencyBand, SummaryStatistics>> getEvidMap() {
        return evidMap;
    }

    public Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> getEventWeights() {
        return eventWeights;
    }

    public void setEventWeights(Map<Event, Function<Map<Double, Double>, SortedMap<Double, Double>>> eventWeights) {
        this.eventWeights = eventWeights;
    }

    public MdacParametersPS getMdacPs() {
        return mdacPs;
    }

    public void setMdacPs(MdacParametersPS mdacPs) {
        this.mdacPs = mdacPs;
    }

    public Map<String, Integer> getStationCount() {
        return stationCount;
    }

    public void setStationCount(Map<String, Integer> stationCount) {
        this.stationCount = stationCount;
    }

    public Map<String, Double> getBandCoverageMetric() {
        return bandCoverageMetric;
    }

    public void setBandCoverageMetric(Map<String, Double> bandCoverageMetric) {
        this.bandCoverageMetric = bandCoverageMetric;
    }
}