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
package gov.llnl.gnem.apps.coda.envelope.model.domain;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;

public class EnvelopeBandParameters extends FrequencyBand {
    private static final long serialVersionUID = 1L;
    private Integer smoothing;
    private Integer interpolation;
    private static final int DEFAULT_SMOOTHING = 2;
    private static final int DEFAULT_INTERPOLATION = 4;

    public EnvelopeBandParameters() {
        super();
    }

    public EnvelopeBandParameters(double lowFrequency, double highFrequency) {
        this(lowFrequency, highFrequency, DEFAULT_SMOOTHING, DEFAULT_INTERPOLATION);
    }

    public EnvelopeBandParameters(double lowFrequency, double highFrequency, int smoothing) {
        this(lowFrequency, highFrequency, DEFAULT_SMOOTHING, DEFAULT_INTERPOLATION);
    }

    public EnvelopeBandParameters(double lowFrequency, double highFrequency, int smoothing, int interpolation) {
        super();
        setLowFrequency(lowFrequency);
        setHighFrequency(highFrequency);
        this.smoothing = smoothing;
        this.interpolation = interpolation;
    }

    public Integer getSmoothing() {
        return smoothing;
    }

    public EnvelopeBandParameters setSmoothing(Integer smoothing) {
        this.smoothing = smoothing;
        return this;
    }

    public Integer getInterpolation() {
        return interpolation;
    }

    public EnvelopeBandParameters setInterpolation(Integer interpolation) {
        this.interpolation = interpolation;
        return this;
    }

}
