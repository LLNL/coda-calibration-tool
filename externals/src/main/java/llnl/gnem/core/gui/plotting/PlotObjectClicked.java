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
package llnl.gnem.core.gui.plotting;


import java.awt.event.MouseEvent;

import llnl.gnem.core.gui.plotting.plotobject.PlotObject;

/**
 * A class that holds a MouseEvent and a PlotObject. Used by PlotObjectObservable
 * to pass information about mouse button events involving PlotObjects to registered
 * listeners.
 *
 * @author Doug Dodge
 * @see llnl.gnem.plotting.PlotObjectObservable
 */
public class PlotObjectClicked {
    /**
     * The packaged MouseEvent
     */
    public MouseEvent me;
    /**
     * The packaged PlotObject
     */
    public PlotObject po;
    /**
     * The MouseMode in effect when this selection was made
     */
    public MouseMode mode;

    /**
     * Constructor for the PlotObjectSelectInfo object
     *
     * @param me MouseEvent to be packaged
     * @param po PlotObject to be packaged
     */
    public PlotObjectClicked( MouseEvent me, PlotObject po, MouseMode mode )
    {
        this.me = me;
        this.po = po;
        this.mode = mode;
    }
}

