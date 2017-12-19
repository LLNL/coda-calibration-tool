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

import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.transforms.Coordinate;

/**
 * Created by: dodge1
 * Date: Jan 13, 2005
 */
public class PickCreationInfo {
    public PlotObject getSelectedObject()
    {
        if( clickedObject != null && clickedObject instanceof Line){
            return clickedObject;
        }
        else{
            if( owningPlot != null && owningPlot.getLineCount() == 1 ){
                Line[] lines = owningPlot.getLines();
                return lines[0];
            }
            else
                return null;
        }
    }

    public JSubplot getOwningPlot()
    {
        return owningPlot;
    }

    public Coordinate getCoordinate()
    {
        return coordinate;
    }

    public MouseEvent getMouseEvent()
    {
        return mouseEvent;
    }

    public void setClickedObject( PlotObject clickedObject )
    {
        this.clickedObject = clickedObject;
    }

    private PlotObject clickedObject;
    private final JSubplot   owningPlot;
    private final Coordinate coordinate;
    private final MouseEvent mouseEvent;

    public PickCreationInfo( PlotObject clickedObject, JSubplot   owningPlot,
                             Coordinate coordinate, MouseEvent mouseEvent )
    {
        this.clickedObject = clickedObject;
        this.owningPlot = owningPlot;
        this.coordinate = coordinate;
        this.mouseEvent = mouseEvent;
    }
}
