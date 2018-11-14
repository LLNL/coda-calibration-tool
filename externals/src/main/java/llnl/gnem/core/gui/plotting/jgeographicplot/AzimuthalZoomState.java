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
package llnl.gnem.core.gui.plotting.jgeographicplot;

import llnl.gnem.core.gui.plotting.ZoomState;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;

/**
 * This class is part of the implementation of a "Zoom Stack" for azimuthal
 * geographic plots. It stores the origin of the zoom state and the radius of
 * the zoom and allows access to those values.
 */
public class AzimuthalZoomState implements ZoomState {

    private double centerLat;
    private double centerLon;
    private double degreeRadius;

    /**
     * Constructor for the AzimuthalZoomState that takes the origin as a lat-lon
     * pair.
     *
     * @param centerLat
     *            The latitude of the origin
     * @param centerLon
     *            The longitude of the origin
     * @param degreeRadius
     *            The radius of the zoom state in degrees
     */
    public AzimuthalZoomState(double centerLat, double centerLon, double degreeRadius) {
        this.centerLat = centerLat;
        this.centerLon = centerLon;
        this.degreeRadius = degreeRadius;
    }

    /**
     * Constructor for the AzimuthalZoomState that takes the origin as a
     * Coordinate object
     *
     * @param center
     *            The origin of the zoom state
     * @param degreeRadius
     *            The radius of the zoom state in degrees
     */
    public AzimuthalZoomState(Coordinate center, double degreeRadius) {
        centerLat = center.getWorldC1();
        centerLon = center.getWorldC2();
        if (centerLon < -180)
            centerLon += 360;
        this.degreeRadius = degreeRadius;
    }

    /**
     * Gets the latitude of the zoom state origin
     *
     * @return The latitude value
     */
    public double getCenterLat() {
        return centerLat;
    }

    /**
     * Gets the longitude of the zoom state origin
     *
     * @return The longitude value
     */
    public double getCenterLon() {
        return centerLon;
    }

    /**
     * Gets the radius of the zoom state in degrees
     *
     * @return The radius value
     */
    public double getDegreeRadius() {
        return degreeRadius;
    }

    /**
     * Gets the origin of the zoom state as a Coordinate object
     *
     * @return The zoom state origin
     */
    public Coordinate getCenterCoordinate() {
        Coordinate c = new Coordinate(0, 0);
        c.setWorldC1(centerLat);
        c.setWorldC2(centerLon);
        return c;
    }
}
