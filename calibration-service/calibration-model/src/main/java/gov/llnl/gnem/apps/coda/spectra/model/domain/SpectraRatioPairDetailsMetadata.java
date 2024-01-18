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
package gov.llnl.gnem.apps.coda.spectra.model.domain;

import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;

public interface SpectraRatioPairDetailsMetadata {
    public Long getId();

    public Integer getVersion();

    public SpectraRatioPairDetailsMetadata setId(Long id);

    public SpectraRatioPairDetailsMetadata setVersion(Integer version);

    public Double getDiffAvg();

    public Double getNumerAvg();

    public Double getDenomAvg();

    public WaveformMetadata getNumerWaveform();

    public WaveformMetadata getDenomWaveform();

    public int getCutSegmentLength();

    public double getCutTimeLength();

    public Double getNumerWaveStartSec();

    public Double getDenomWaveStartSec();

    public Double getNumerWaveEndSec();

    public Double getDenomWaveEndSec();

    public Double getNumerPeakSec();

    public Double getDenomPeakSec();

    public Double getNumerFMarkerSec();

    public Double getDenomFMarkerSec();

    public Double getNumerStartCutSec();

    public Double getDenomStartCutSec();

    public Double getNumerEndCutSec();

    public Double getDenomEndCutSec();

    public int getNumerStartCutIdx();

    public int getDenomStartCutIdx();

    public int getNumerEndCutIdx();

    public int getDenomEndCutIdx();

    public boolean getUserEdited();
}
