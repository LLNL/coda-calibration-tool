package gov.llnl.gnem.apps.coda.common.service.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;

public class MetadataUtils {

    public static Map<FrequencyBand, SharedFrequencyBandParameters> mapSharedParamsToFrequencyBands(List<SharedFrequencyBandParameters> sharedBands) {
        return sharedBands.stream().collect(Collectors.toMap(fbp -> new FrequencyBand(fbp.getLowFrequency(), fbp.getHighFrequency()), fbp -> fbp));
    }

    public static Map<FrequencyBand, Map<Station, SiteFrequencyBandParameters>> mapSiteParamsToFrequencyBands(List<SiteFrequencyBandParameters> params) {
        return params.stream()
                     .collect(
                             Collectors.groupingBy(
                                     site -> new FrequencyBand(site.getLowFrequency(), site.getHighFrequency()),
                                         Collectors.toMap(SiteFrequencyBandParameters::getStation, Function.identity())));
    }

}
