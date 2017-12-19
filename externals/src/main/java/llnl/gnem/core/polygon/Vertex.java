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

import java.io.Serializable;


/**
 * A class that encapsulates a 2-D point on the surface of the Earth described by a latitude and a longitude
 *
 * @author Doug Dodge
 */
public class Vertex implements Serializable {
    /**
     * Creates a new Vertex from a latitude and a longitude value
     *
     * @param Lat The latitude of the point
     * @param Lon The longitude of the point
     */
    public Vertex( double Lat, double Lon )
    {
        lat = ValidateLat( Lat );
        lon = ValidateLon( Lon );
    }

    /**
     * Copy Constructor for the Vertex object
     *
     * @param v The Vertex to be copied
     */
    public Vertex( Vertex v )
    {
        lat = v.lat;
        lon = v.lon;
    }

    /**
     * Gets the lat attribute of the Vertex object
     *
     * @return The lat value
     */
    public double getLat()
    {
        return lat;
    }

    /**
     * Gets the longitude of the Vertex
     *
     * @return The lon value
     */
    public double getLon()
    {
        return lon;
    }

    /**
     * Sets the latitude of the Vertex
     *
     * @param Lat The new lat value
     */
    public void setLat( double Lat )
    {
        lat = ValidateLat( Lat );
    }

    /**
     * Sets the longitude of the Vertex
     *
     * @param Lon The new lon value
     */
    public void setLon( double Lon )
    {
        lon = ValidateLon( Lon );
    }

    /**
     * Determine whether two Vertex objects describe the same point.
     *
     * @param o The Vertex to be tested
     * @return true if the two Vertex objects describe the same point.
     */
    public boolean equals( Object o )
    {
        if( o == this )
            return true;
        if( o instanceof Vertex ) {
            // No need to check for null because instanceof handles that check

            Vertex tmp = (Vertex) o;
            return tmp.lat == lat && tmp.lon == lon;
        }
        else
            return false;
    }

    /**
     * Return a unique hash code based on the latitude and longitude values
     *
     * @return The hash code
     */
    public int hashCode()
    {
        return new Double( lat ).hashCode() ^ new Double( lon ).hashCode();
    }

    /**
     * Returns a String description of the Vertex.
     *
     * @return The String description.
     */
    public String toString()
    {
        return "Lat = " + lat + ", Lon = " + lon;
    }

    private double ValidateLat( double Lat )
    {
        if( Lat > 90 )
            Lat = 90;
        if( Lat < -90 )
            Lat = -90;
        return Lat;
    }

    private double ValidateLon( double Lon )
    {
//        while( Lon > 180 ) Lon -= 360;

//        while( Lon < -180 ) Lon += 360;

        return Lon;
    }

    private double lat;
    private double lon;
}



