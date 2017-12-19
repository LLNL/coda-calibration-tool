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

public class SphericalDirectionCosines
{
    /** An object containing the direction cosines between the center of a sphere and
        a point on its surface */
    public SphericalDirectionCosines()
    {
     /** An empty constructor */
    }

    /** A constructor used to create a DirectionCosine object
        for a point at a given colatitude and longitude (in radians) */
    public SphericalDirectionCosines(double colatitude, double longitude)
    {
        getDirectionCosines(colatitude, longitude);
    }

    void getDirectionCosines(double colatitude, double longitude)
    {
        /** Get direction cosines for a line joining a center point
            to a surface point at (colatitude, longitude) */
            A = Math.sin(colatitude)*Math.cos(longitude);
            B = Math.sin(colatitude)*Math.sin(longitude);
            C = Math.cos(colatitude);

        // direction cosines for the parallel of latitude through the point
            D = Math.sin(longitude);
            E = -1.0*Math.cos(longitude);

        // direction cosines for the meridian through the point
            G = Math.cos(colatitude)*Math.cos(longitude);
            H = Math.cos(colatitude)*Math.sin(longitude);
            K = -1.0 * Math.sin(colatitude);
    }

    double A, B, C, D, E, G, H, K;
}



