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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeMeasurement;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class ShapeMeasurementWebClient implements ShapeMeasurementClient {

    private WebClient client;

    @Autowired
    public ShapeMeasurementWebClient(WebClient client) {
        this.client = client;
    }

    @Override
    public Flux<ShapeMeasurement> getMeasuredShapes() {
        return client.get()
                     .uri("/shape-measurements/all/")
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMapMany(response -> response.bodyToFlux(ShapeMeasurement.class))
                     .onErrorReturn(new ShapeMeasurement());
    }

    @Override
    public Mono<ShapeMeasurement> getMeasuredShape(Long id) {
        return client.get()
                     .uri("/shape-measurements/byWaveformId/" + id)
                     .accept(MediaType.APPLICATION_JSON)
                     .exchange()
                     .flatMap(response -> response.bodyToMono(ShapeMeasurement.class))
                     .onErrorReturn(new ShapeMeasurement());
    }
}
