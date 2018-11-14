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
package gov.llnl.gnem.apps.coda.envelope.model.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Default14BandEnvelopeJobConfiguration {
    @Bean
    public static EnvelopeJobConfiguration getConfiguration() {
        EnvelopeJobConfiguration conf = new EnvelopeJobConfiguration();
        List<EnvelopeBandParameters> frequencyBands = new ArrayList<>();
        EnvelopeBandParameters bandParams;

        bandParams = new EnvelopeBandParameters(0.02, 0.03, 5);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.03, 0.05, 5);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.05, 0.10, 5);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.10, 0.20, 5);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.20, 0.30, 5);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.30, 0.50, 5);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.50, 0.70, 4);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(0.70, 1.00, 4);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(1.00, 1.50, 1);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(1.50, 2.00, 1);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(2.00, 3.00, 1);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(3.00, 4.00, 1);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(4.00, 6.00, 1);
        frequencyBands.add(bandParams);
        bandParams = new EnvelopeBandParameters(6.00, 8.00, 1);
        frequencyBands.add(bandParams);

        conf.setFrequencyBandConfiguration(frequencyBands);
        return conf;
    }
}
