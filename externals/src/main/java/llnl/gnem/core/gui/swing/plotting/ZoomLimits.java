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
package llnl.gnem.core.gui.swing.plotting;

/**
 * Class containing the axis limits for a particular stage of zooming for a
 * single JSubplot.
 *
 * @author Doug Dodge
 */
public class ZoomLimits {
    /**
     * Constructor for the ZoomLimits object
     *
     * @param xmin
     *            Minimum limit for the X-axis
     * @param xmax
     *            Maximum limit for the X-axis
     * @param ymin
     *            Minimum limit for the Y-axis
     * @param ymax
     *            Maximum limit for the Y-axis
     */
    public ZoomLimits(double xmin, double xmax, double ymin, double ymax) {
        this.xmin = xmin;
        this.xmax = xmax;
        this.ymin = ymin;
        this.ymax = ymax;
    }

    public ZoomLimits(Limits xlimits, Limits ylimits) {
        xmin = xlimits.getMin();
        xmax = xlimits.getMax();
        ymin = ylimits.getMin();
        ymax = ylimits.getMax();
    }

    @Override
    public String toString() {
        return "ZoomLimits{" + "xmin=" + xmin + ", xmax=" + xmax + ", ymin=" + ymin + ", ymax=" + ymax + '}';
    }

    /**
     * Copy Constructor for the ZoomLimits object
     *
     * @param orig
     *            ZoomLimits object to copy
     */
    public ZoomLimits(ZoomLimits orig) {
        this.xmin = orig.xmin;
        this.xmax = orig.xmax;
        this.ymin = orig.ymin;
        this.ymax = orig.ymax;
    }

    /**
     * Minimum limit for the X-axis
     */
    public double xmin;
    /**
     * Maximum limit for the X-axis
     */
    public double xmax;
    /**
     * Minimum limit for the Y-axis
     */
    public double ymin;
    /**
     * Maximum limit for the Y-axis
     */
    public double ymax;
}
