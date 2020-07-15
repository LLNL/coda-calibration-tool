/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.awt.geom.Point2D;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;

public class SpectraPlotController {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private SpectralPlot spectraPlot = new SpectralPlot();;
    private Map<Point2D.Double, SpectraMeasurement> spectraSymbolMap = new ConcurrentHashMap<>();
    private boolean isYaxisResizble = false;
    private Function<SpectraMeasurement, Double> dataFunction;

    public SpectraPlotController(Function<SpectraMeasurement, Double> dataFunction) {
        this.dataFunction = dataFunction;
    }

    public SpectralPlot getSpectralPlot() {
        return spectraPlot;
    }

    public Map<Point2D.Double, SpectraMeasurement> getSymbolMap() {
        return spectraSymbolMap;
    }

    public void setYAxisResize(boolean shouldYAxisShrink, double minY, double maxY) {
        if (isYaxisResizble) {
            spectraPlot.setAutoCalculateYaxisRange(shouldYAxisShrink);
            if (shouldYAxisShrink) {
                spectraPlot.setAllYlimits(minY, maxY);
            } else {
                spectraPlot.setAllYlimits();
            }
        }
    }

    public void setYAxisResizable(boolean isYaxisResizble) {
        this.isYaxisResizble = isYaxisResizble;
    }

    public Function<SpectraMeasurement, Double> getDataFunc() {
        return dataFunction;
    }
}
