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
package llnl.gnem.core.metadata;

/**
 *
 * @author dodge1
 */
public enum InstrumentType {

    SEISMOMETER, TILT_METER, CREEP_METER, CALIBRATION_INPUT, 
    PRESSURE, TEST_POINT, MAGNETOMETER, HUMIDITY, ROTATIONAL_SENSOR, 
    TEMPERATURE, WATER_CURRENT, GEOPHONE, ELECTRIC_POTENTIAL, RAINFALL, 
    LINEAR_STRAIN, TIDE, BOLOMETER, VOLUMETRIC_STRAIN, WIND, DERIVED, 
    NON_SPECIFIC, SYNTHESIZED_BEAM
}
