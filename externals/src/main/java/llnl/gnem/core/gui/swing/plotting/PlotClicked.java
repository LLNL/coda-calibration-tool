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

import java.awt.event.MouseEvent;

import llnl.gnem.core.gui.swing.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.swing.plotting.transforms.Coordinate;

/**
 * Created by dodge1 Date: Feb 7, 2008
 */
public class PlotClicked {
    private final MouseEvent me;
    private final Coordinate coordinate;
    private final JSubplot subplot;

    public PlotClicked(MouseEvent me, Coordinate c, JSubplot subplot) {
        this.me = me;
        this.coordinate = c;
        this.subplot = subplot;
    }

    public MouseEvent getMe() {
        return me;
    }

    public Coordinate getCoordinate() {
        return coordinate;
    }

    public JSubplot getSubplot() {
        return subplot;
    }
}