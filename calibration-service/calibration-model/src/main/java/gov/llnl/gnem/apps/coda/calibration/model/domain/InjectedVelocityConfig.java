/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class InjectedVelocityConfig {
    @Value("#{'${path.phase-velocity-kms:${phase.phase-velocity-kms:${phase-velocity-kms:3.5}}}'}")
    private Double phaseVelocityInKms;

    @Value("#{'${velocity.calc.group-velocity1-gt-distance:${group-velocity1-gt-distance:4.7}}'}")
    private Double groupVelocity1InKmsGtDistance;

    @Value("#{'${velocity.calc.group-velocity2-gt-distance:${group-velocity2-gt-distance:2.3}}'}")
    private Double groupVelocity2InKmsGtDistance;

    @Value("#{'${velocity.calc.group-velocity1-lt-distance:${group-velocity1-lt-distance:3.9}}'}")
    private Double groupVelocity1InKmsLtDistance;

    @Value("#{'${velocity.calc.group-velocity2-lt-distance:${group-velocity2-lt-distance:1.9}}'}")
    private Double groupVelocity2InKmsLtDistance;

    @Value("#{'${velocity.calc.distance-threshold-km:${distance-threshold-km:300.0}}'}")
    private Double distanceThresholdInKm;

    @Bean
    public VelocityConfiguration toVelocityConfiguration() {
        return new VelocityConfiguration().setDistanceThresholdInKm(distanceThresholdInKm)
                                          .setGroupVelocity1InKmsGtDistance(groupVelocity1InKmsGtDistance)
                                          .setGroupVelocity1InKmsLtDistance(groupVelocity1InKmsLtDistance)
                                          .setGroupVelocity2InKmsGtDistance(groupVelocity2InKmsGtDistance)
                                          .setGroupVelocity2InKmsLtDistance(groupVelocity2InKmsLtDistance)
                                          .setPhaseVelocityInKms(phaseVelocityInKms);
    }
}
