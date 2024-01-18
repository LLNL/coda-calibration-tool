/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client.api;

import java.util.List;
import java.util.Set;

import gov.llnl.gnem.apps.coda.spectra.model.domain.RatioEventData;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetailsMetadata;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface SpectraRatioClient {

    public Mono<SpectraRatiosReportByEventPair> makeSpectraRatioMeasurementsFromWaveforms(Boolean autoPickingEnabled, Boolean persistResults, Set<String> smallEventIds, Set<String> largeEventIds);

    public Mono<SpectraRatiosReportByEventPair> makeSpectraRatioMeasurementsFromRatioData(Set<String> smallEventIds, Set<String> largeEventIds, List<RatioEventData> ratioEventData);

    public Mono<SpectraRatioPairDetails> updateRatio(SpectraRatioPairDetails ratio);

    public Flux<SpectraRatioPairDetails> getRatios();

    public Flux<SpectraRatioPairDetailsMetadata> getRatiosMetadata();

    public Flux<String> loadRatioMetadata(long andIncrement, List<SpectraRatioPairDetailsMetadata> ratios);
}