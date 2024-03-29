/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwReportByEvent;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

public interface CalibrationService {

    public boolean startCalibration(boolean autoPickingEnabled);

    public boolean cancelCalibration(Long id);

    public boolean clearData();

    public Future<Result<MeasuredMwReportByEvent>> makeMwMeasurements(boolean autoPickingEnabled, boolean persistResults);

    public Future<Result<MeasuredMwReportByEvent>> makeMwMeasurements(boolean autoPickingEnabled, boolean persistResults, Set<String> eventIds);

    public Future<Result<MeasuredMwReportByEvent>> makeMwMeasurements(boolean autoPickingEnabled, boolean persistResults, List<Waveform> stacks);

    public List<String> toggleAllByEventIds(List<String> eventIds);
}
