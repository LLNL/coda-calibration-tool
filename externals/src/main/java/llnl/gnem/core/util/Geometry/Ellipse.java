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


import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import llnl.gnem.core.polygon.Vertex;

/**
 *
 * @author matzel1
 */
public class Ellipse
{
    double semimajoraxis;
    double semiminoraxis;
    double eccentricity;
    double focaldistance;
    
    Vector3D focus1; // focus1 in cartesian coordinates
    Vector3D focus2; // focus2 in cartesian coordinates
 
    /**
     * A flat ellipse in Cartesian coordinates
     * 
     * @param focus1
     * @param focus2
     * @param point 
     */
    public Ellipse(Vector3D focus1, Vector3D focus2, Vector3D point)
    {
        this.focus1 = focus1;
        this.focus2 = focus2;
        
        double distance12 = focus1.distance(focus2);
        double distance1p = focus1.distance(point);
        double distance2p = focus2.distance(point);
                
        semimajoraxis = (distance1p + distance2p)/2;//half the distance across the ellipse along the principal axis
        semiminoraxis = Math.sqrt(semimajoraxis * semimajoraxis - (distance12 * distance12)/4);
        focaldistance = distance12/2;
        eccentricity = distance12/semimajoraxis;  
    }
    
    /**
     * The projection of an ellipse onto the Earth
     * 
     * Note all distances, including focal distance, semimajor and semiminor axes, refer to the path along the Earth's surface
     * @param focus1 (lat, lon) of focus 1
     * @param focus2 (lat, lon) of focus 2
     * @param point  (lat, lon) of a point on the ellipse surrounding focus1 and focus2
     */
    public Ellipse(Vertex focus1, Vertex focus2, Vertex point)
    {
        double distance12 = EModel.getDistance(focus1, focus2);
        double distance1p = EModel.getDistance(focus1, point);
        double distance2p = EModel.getDistance(focus2, point);
        
        double radius1 = EModel.getEarthRadius(focus1.getLat());
        double radius2 = EModel.getEarthRadius(focus2.getLat());
        
        // convert the foci into x,y,z coordinates for consistency with the Ellipse class
        this.focus1 = CoordinateTransform.LonLatRadiusToXYZ(focus1.getLon(), focus1.getLat(), radius1);  
        this.focus2 = CoordinateTransform.LonLatRadiusToXYZ(focus2.getLon(), focus2.getLat(), radius2);     
        
        semimajoraxis = (distance1p + distance2p)/2;//half the distance across the ellipse along the principal axis
        semiminoraxis = Math.sqrt(semimajoraxis * semimajoraxis - (distance12 * distance12)/4);
        focaldistance = distance12/2;
        eccentricity = distance12/semimajoraxis;     
    }
    

    public double getSemiMajorAxis()
    {
        return semimajoraxis;
    }
      
    public double getSemiMinorAxis()
    {
        return semiminoraxis;
    }
    
    public double getEccentricity()
    {
        return eccentricity;
    }
    
    public double getArea()
    {
        return Math.PI * (semimajoraxis * semiminoraxis)/4.;
    }
    /**
     * @return the distance from the focus to the center of the ellipse
     */
    public double getFocalDistance()
    {
        return focaldistance;
        
        //return Math.sqrt(semimajoraxis*semimajoraxis - semiminoraxis*semiminoraxis);
    }
    
    double calculateFocalDistance()
    {
        focaldistance = Math.sqrt(semimajoraxis*semimajoraxis - semiminoraxis*semiminoraxis);
        return focaldistance;
    }
    
}
