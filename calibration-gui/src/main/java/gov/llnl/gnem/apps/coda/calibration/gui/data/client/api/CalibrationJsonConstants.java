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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client.api;

public class CalibrationJsonConstants {
    public static final String TYPE_FIELD = "type";
    public static final String TYPE_VALUE = "llnl/coda-calibration-tool";
    public static final String SCHEMA_FIELD = "schemaVersion";
    public static final String SCHEMA_VALUE = "1";
    public static final String BAND_FIELD = "bands";
    public static final String SITE_CORRECTION_FIELD = "site-corrections";
    public static final String MDAC_PS_FIELD = "mdac-ps";
    public static final String MDAC_FI_FIELD = "mdac-fi";
    public static final String REFERENCE_EVENTS_FIELD = "reference-events";
    public static final String MEASURED_EVENTS_FIELD = "measured-events";
    public static final String VELOCITY_CONFIGURATION = "velocity-configuration";
}
