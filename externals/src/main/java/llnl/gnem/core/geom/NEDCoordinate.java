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
package llnl.gnem.core.geom;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.BlockRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

/**
 * Represents a point in the North-East-Down local frame.
 *
 * @author maganazook1
 */
public class NEDCoordinate extends CartesianCoordinate {

    public NEDCoordinate(double x, double y) {
        super(x, y);
    }

    public NEDCoordinate(double x, double y, double z) {
        super(x, y, z);
    }

    public NEDCoordinate(GeographicCoordinate position, GeographicCoordinate origin) {
        // Set dummy values while we do the conversion
        super(0, 0, 0);

        NEDCoordinate nedCoordinate = NEDCoordinate.convertFromGeographicCoordinate(position, origin);
        this.setPoint(nedCoordinate.getPoint());
    }

    public static NEDCoordinate convertFromGeographicCoordinate(GeographicCoordinate position, GeographicCoordinate origin) {

        ECEFCoordinate ecefPosition = new ECEFCoordinate(position);
        ECEFCoordinate ecefOrigin = new ECEFCoordinate(origin);

        RealMatrix ecefToNEDRotationMatrix = getECEFToNEDRotationMatrix(origin);

        RealVector ecefPositionVector = new ArrayRealVector(new double[]{ecefPosition.getX(), ecefPosition.getY(), ecefPosition.getZ()});
        RealVector ecefOriginVector = new ArrayRealVector(new double[]{ecefOrigin.getX(), ecefOrigin.getY(), ecefOrigin.getZ()});

        RealVector ecefDifferenceVector = ecefPositionVector.subtract(ecefOriginVector);
        double[] nedPositionCoordinate = ecefToNEDRotationMatrix.operate(ecefDifferenceVector).toArray();

        return new NEDCoordinate(nedPositionCoordinate[0], nedPositionCoordinate[1], nedPositionCoordinate[2]);
    }

    /**
     *
     * @param origin the reference point for the local NED frame
     * @return the Direction Cosine Matrix for converting
     * Earth-Centered-Earth-Fixed (ECEF) to North-East-Down (NED)
     */
    private static RealMatrix getECEFToNEDRotationMatrix(GeographicCoordinate origin) {
        RealMatrix rotationMatrix = new BlockRealMatrix(3, 3);
        double lat = origin.getLat();
        double lon = origin.getLon();

        double sin_lon = Math.sin(lon);
        double cos_lon = Math.cos(lon);
        double sin_lat = Math.sin(lat);
        double cos_lat = Math.cos(lat);

        rotationMatrix.setEntry(0, 0, -sin_lat * cos_lon);
        rotationMatrix.setEntry(0, 1, -sin_lat * sin_lon);
        rotationMatrix.setEntry(0, 2, cos_lat);
        rotationMatrix.setEntry(1, 0, -sin_lon);
        rotationMatrix.setEntry(1, 1, cos_lon);
        rotationMatrix.setEntry(1, 2, 0.0);
        rotationMatrix.setEntry(2, 0, -cos_lat * cos_lon);
        rotationMatrix.setEntry(2, 1, -cos_lat * sin_lon);
        rotationMatrix.setEntry(2, 2, -sin_lat);

        return rotationMatrix;
    }

}
