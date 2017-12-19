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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum InstrumentCode {

    
    
    H(InstrumentType.SEISMOMETER, "High Gain Seismometer", OrientationCode.getSeismometerCodes()),
    L(InstrumentType.SEISMOMETER, "Low Gain Seismometer", OrientationCode.getSeismometerCodes()),
    G(InstrumentType.SEISMOMETER, "Gravimeter", OrientationCode.getSeismometerCodes()),
    M(InstrumentType.SEISMOMETER, "Mass Position Seismometer", OrientationCode.getSeismometerCodes()),
    N(InstrumentType.SEISMOMETER, "Accelerometer", OrientationCode.getSeismometerCodes()),
    A(InstrumentType.TILT_METER, "Tilt Meter", OrientationCode.getTiltCodes()),
    B(InstrumentType.CREEP_METER, "Creep Meter", OrientationCode.getUnknownCode()),
    C(InstrumentType.CALIBRATION_INPUT, "Calibration Input", OrientationCode.getCalibrationCodes()),
    D(InstrumentType.PRESSURE, "Pressure", OrientationCode.getPressureCodes()),
    E(InstrumentType.TEST_POINT, "Electronic Test Point", Arrays.asList(OrientationCode.values())),
    F(InstrumentType.MAGNETOMETER, "Magnetometer", OrientationCode.getMagnetometerCodes()),
    I(InstrumentType.HUMIDITY, "Humidity", OrientationCode.getHumidityCodes()),
    J(InstrumentType.ROTATIONAL_SENSOR, "Rotational Sensor", OrientationCode.getSeismometerCodes()),
    K(InstrumentType.TEMPERATURE, "Temperature", OrientationCode.getHumidityCodes()),
    O(InstrumentType.WATER_CURRENT, "Water Current", OrientationCode.getUnknownCode()),
    P(InstrumentType.GEOPHONE, "Geophone", OrientationCode.getMagnetometerCodes()),
    Q(InstrumentType.ELECTRIC_POTENTIAL, "Electric Potential", OrientationCode.getUnknownCode()),
    R(InstrumentType.RAINFALL, "Rainfall", OrientationCode.getUnknownCode()),
    S(InstrumentType.LINEAR_STRAIN, "Linear Strain", OrientationCode.getMagnetometerCodes()),
    T(InstrumentType.TIDE, "Tide", OrientationCode.getTideCode()),
    U(InstrumentType.BOLOMETER, "Bolometer", OrientationCode.getUnknownCode()),
    V(InstrumentType.VOLUMETRIC_STRAIN, "Volumetric Strain", OrientationCode.getUnknownCode()),
    W(InstrumentType.WIND, "Wind", OrientationCode.getWindCodes()),
    X(InstrumentType.DERIVED, "Derived", OrientationCode.getUnknownCode()),
    Y(InstrumentType.NON_SPECIFIC, "Non-Specific", OrientationCode.getUnknownCode()),
    Z(InstrumentType.SYNTHESIZED_BEAM, "Synthesized Beam", OrientationCode.getBeamCodes());
    private final InstrumentType insType;
    private final String descrip;
    private final ArrayList<OrientationCode> allowedOrientations;

    private InstrumentCode(InstrumentType type, String descrip, List<OrientationCode> allowed) {
        insType = type;
        this.descrip = descrip;
        this.allowedOrientations = new ArrayList<>(allowed);
    }

    /**
     * @return the insType
     */
    public InstrumentType getInsType() {
        return insType;
    }

    /**
     * @return the descrip
     */
    public String getDescrip() {
        return descrip;
    }
    
    public List<OrientationCode> getAllowedOrientations()
    {
        return new ArrayList<>(allowedOrientations);
    }
}
