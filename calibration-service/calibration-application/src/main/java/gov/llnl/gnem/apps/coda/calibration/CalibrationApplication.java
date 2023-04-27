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
package gov.llnl.gnem.apps.coda.calibration;

import java.util.Locale;
import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import gov.llnl.gnem.apps.coda.common.repository.DetachableJpaRepoImpl;

@SpringBootApplication
@EnableAutoConfiguration
@ComponentScan("gov.llnl.gnem.apps.coda.common.application")
@ComponentScan("gov.llnl.gnem.apps.coda.common.service")
@ComponentScan("gov.llnl.gnem.apps.coda.envelope")
@ComponentScan("gov.llnl.gnem.apps.coda.calibration")
@EntityScan(basePackages = { "gov.llnl.gnem.apps.coda.calibration", "gov.llnl.gnem.apps.coda.envelope", "gov.llnl.gnem.apps.coda.spectra", "gov.llnl.gnem.apps.coda.common" })
@EnableJpaRepositories(basePackages = { "gov.llnl.gnem.apps.coda.calibration", "gov.llnl.gnem.apps.coda.envelope", "gov.llnl.gnem.apps.coda.spectra", "gov.llnl.gnem.apps.coda.common" }, repositoryBaseClass = DetachableJpaRepoImpl.class)
public class CalibrationApplication {
    @PostConstruct
    void started() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CalibrationApplication.class, args);
    }
}