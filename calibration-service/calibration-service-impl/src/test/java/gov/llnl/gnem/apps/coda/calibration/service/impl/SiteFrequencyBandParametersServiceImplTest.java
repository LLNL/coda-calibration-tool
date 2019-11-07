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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.test.annotations.IntTest;

@IntTest
@DataJpaTest(showSql = false)
@ExtendWith(MockitoExtension.class)
@ContextConfiguration(classes = CalibrationServiceTestContext.class)
public class SiteFrequencyBandParametersServiceImplTest {

    @Autowired
    private SiteFrequencyBandParametersServiceImpl service;

    @Test
    public void testSaveDuplicate() throws Exception {
        SiteFrequencyBandParameters initial = genSite();
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        assertThat(service.findAll()).size().isEqualTo(1).describedAs("Should have saved only one site entry");
    }

    @Test
    public void testSaveDuplicateOneUnknownNetwork() throws Exception {
        SiteFrequencyBandParameters initial = genSite();
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        initial.getStation().setNetworkName("UNK");
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        assertThat(service.findAll()).size().isEqualTo(1).describedAs("Should have saved only one site entry");
    }

    @Test
    public void testSaveAmbiguousNetwork() throws Exception {
        SiteFrequencyBandParameters initial = genSite();
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        initial.getStation().setNetworkName("B");
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        initial.getStation().setNetworkName("UNK");
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        assertThat(service.findAll()).size().isEqualTo(2).describedAs("Should have saved only one site entry");
    }

    @Test
    public void testSaveExistingIdNoManualVersionBump() throws Exception {
        SiteFrequencyBandParameters initial = genSite();
        initial = service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        initial.getStation().setNetworkName("B");
        service.save(new SiteFrequencyBandParameters().mergeNonNullOrEmptyFields(initial));
        assertThat(service.findAll()).size().isEqualTo(1).describedAs("Should have saved only one site entry");
        assertThat(service.findAll().get(0).getStation().getNetworkName()).isEqualTo("B").describedAs("Should have the modified network name");
    }

    private SiteFrequencyBandParameters genSite() {
        SiteFrequencyBandParameters x = new SiteFrequencyBandParameters().setStation(new Station().setLatitude(1d).setLongitude(1d).setNetworkName("A").setStationName("STA"))
                                                                         .setLowFrequency(1d)
                                                                         .setHighFrequency(2d)
                                                                         .setSiteTerm(10d);
        return x;
    }
}
