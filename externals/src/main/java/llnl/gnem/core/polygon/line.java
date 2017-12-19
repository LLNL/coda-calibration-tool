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
package llnl.gnem.core.polygon;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 * User: dodge1
 * Date: Jan 22, 2004
 * Time: 1:43:24 PM
 */
public class line {
    public Vector3D p1;
    public Vector3D p2;

    public line()
    {
        p1 = new Vector3D( 0.0, 0.0, 0.0 );
        p2 = new Vector3D( 0.0, 0.0, 0.0 );
    }

    public line( Vector3D pp1, Vector3D pp2 )
    {
        p1 = new Vector3D( pp1.getX(), pp1.getY(), pp1.getZ() );
        p2 = new Vector3D( pp2.getX(), pp2.getY(), pp2.getZ() );
    }

}


