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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;

public interface SpectraMeasurementMetadata {

    public Long getId();

    public Integer getVersion();

    public double getEndCutSec();

    public double getPathAndSiteCorrected();

    public double getPathCorrected();

    public double getRawAtMeasurementTime();

    public double getRawAtStart();

    public double getRmsFit();

    public double getStartCutSec();

    public WaveformMetadata getWaveform();

    public SpectraMeasurementMetadata setId(Long id);

    public SpectraMeasurementMetadata setPathAndSiteCorrected(double pathAndSiteCorrected);

    public SpectraMeasurementMetadata setPathCorrected(double pathCorrected);

    public SpectraMeasurementMetadata setEndCutSec(double endCutSec);

    public SpectraMeasurementMetadata setWaveform(WaveformMetadata waveform);

    public SpectraMeasurementMetadata setRmsFit(double rmsFit);

    public SpectraMeasurementMetadata setStartCutSec(double startCutSec);

    public SpectraMeasurementMetadata setRawAtMeasurementTime(double rawAtMeasurementTime);

    public SpectraMeasurementMetadata setRawAtStart(double rawAtStart);

    public SpectraMeasurementMetadata setVersion(Integer version);
}
