/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318
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
package gov.llnl.gnem.apps.coda.common.service.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.calibration.model.domain.PeakVelocityMeasurement;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformPick;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;

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

    public static List<Waveform> filterToEndPicked(List<Waveform> stacks) {
        return stacks.parallelStream().filter(wave -> wave.getAssociatedPicks() != null).map(wave -> {
            Optional<WaveformPick> pick = wave.getAssociatedPicks().stream().filter(p -> p.getPickType() != null && PICK_TYPES.F.name().equalsIgnoreCase(p.getPickType().trim())).findFirst();
            if (pick.isPresent() && pick.get().getPickTimeSecFromOrigin() > 0) {
                return wave;
            } else {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public static List<PeakVelocityMeasurement> filterVelocityBySnr(final Map<FrequencyBand, SharedFrequencyBandParameters> snrFilterMap, List<PeakVelocityMeasurement> velocityMeasurements) {
        return velocityMeasurements.stream().parallel().filter(vel -> {
            boolean valid = false;
            if (vel.getWaveform() != null) {
                FrequencyBand fb = new FrequencyBand(vel.getWaveform().getLowFrequency(), vel.getWaveform().getHighFrequency());
                SharedFrequencyBandParameters params = snrFilterMap.get(fb);
                valid = params != null && vel.getSnr() >= params.getMinSnr();
            }
            return valid;
        }).collect(Collectors.toList());
    }
}
