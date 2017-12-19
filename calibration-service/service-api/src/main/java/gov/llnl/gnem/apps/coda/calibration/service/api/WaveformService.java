/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.api;

import java.util.Collection;
import java.util.List;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Waveform;

public interface WaveformService extends BaseService<Waveform, Long> {

    public List<Waveform> getByExampleAllMatching(Waveform waveform);

    public List<Waveform> getAllStacks();

    public Collection<Waveform> update(Long sessionId, Collection<Waveform> values);

    public Waveform update(Waveform waveformPayload);

    public List<Object[]> getUniqueEventStationStacks();
}
