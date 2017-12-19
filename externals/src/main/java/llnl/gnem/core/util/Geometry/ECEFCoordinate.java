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
package llnl.gnem.core.util.Geometry;


public class ECEFCoordinate {
    private final double xMeters;
    private final double yMeters;
    private final double zMeters;

    public ECEFCoordinate(double x, double y, double z) {
        this.xMeters = x;
        this.yMeters = y;
        this.zMeters = z;
    }

    /**
     * @return the x
     */
    public double getX() {
        return xMeters;
    }

    /**
     * @return the y
     */
    public double getY() {
        return yMeters;
    }

    /**
     * @return the z
     */
    public double getZ() {
        return zMeters;
    }

    double getSeparationMeters(ECEFCoordinate other) {
       double dx = xMeters-other.xMeters;
       double dy = yMeters-other.yMeters;
       double dz = zMeters-other.zMeters;
       return Math.sqrt(dx*dx+dy*dy+dz*dz);
    }
    
}
