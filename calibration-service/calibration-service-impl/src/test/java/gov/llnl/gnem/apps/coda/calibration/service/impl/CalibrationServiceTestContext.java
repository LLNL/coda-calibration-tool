/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import gov.llnl.gnem.apps.coda.common.repository.DetachableJpaRepoImpl;

@SpringBootApplication
@ComponentScan("gov.llnl.gnem.apps.coda.common.application")
@ComponentScan("gov.llnl.gnem.apps.coda.common.service")
@ComponentScan("gov.llnl.gnem.apps.coda.common.repository")
@ComponentScan("gov.llnl.gnem.apps.coda.common.model")
@ComponentScan("gov.llnl.gnem.apps.coda.calibration.application")
@ComponentScan("gov.llnl.gnem.apps.coda.calibration.service")
@ComponentScan("gov.llnl.gnem.apps.coda.calibration.repository")
@ComponentScan("gov.llnl.gnem.apps.coda.calibration.model")
@EntityScan(basePackages = { "gov.llnl.gnem.apps.coda.common", "gov.llnl.gnem.apps.coda.calibration" })
@EnableJpaRepositories(basePackages = { "gov.llnl.gnem.apps.coda.common", "gov.llnl.gnem.apps.coda.calibration" }, repositoryBaseClass = DetachableJpaRepoImpl.class)
public class CalibrationServiceTestContext {
}
