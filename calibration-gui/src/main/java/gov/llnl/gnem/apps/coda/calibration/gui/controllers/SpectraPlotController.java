/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import javafx.geometry.Point2D;
import llnl.gnem.core.gui.plotting.api.Symbol;

public class SpectraPlotController {
    private final SpectralPlot spectraPlot = new SpectralPlot();
    private boolean isYaxisResizble = false;
    private final Function<SpectraMeasurement, Double> dataFunction;
    private boolean shouldShowFits = false;
    private final Map<Point2D, SpectraMeasurement> spectraMeasurementMap = new HashMap<>();

    public SpectraPlotController(final Function<SpectraMeasurement, Double> dataFunction) {
        this.dataFunction = dataFunction;
    }

    public SpectralPlot getSpectralPlot() {
        return spectraPlot;
    }

    public Map<Point2D, SpectraMeasurement> getSpectraMeasurementMap() {
        return spectraMeasurementMap;
    }

    public Map<Point2D, List<Symbol>> getSymbolMap() {
        return spectraPlot.getSymbolMap();
    }

    public void setYAxisResize(final boolean shouldYAxisShrink, final double minY, final double maxY) {
        if (isYaxisResizble) {
            spectraPlot.setAutoCalculateYaxisRange(shouldYAxisShrink);
            if (shouldYAxisShrink) {
                spectraPlot.setAllYlimits(minY, maxY);
            } else {
                spectraPlot.setAllYlimits();
            }
        }
    }

    public void setYAxisResizable(final boolean isYaxisResizble) {
        this.isYaxisResizble = isYaxisResizble;
    }

    public Function<SpectraMeasurement, Double> getDataFunc() {
        return dataFunction;
    }

    public void setShowCornerFrequencies(final boolean showCornerFrequencies) {
        this.spectraPlot.showCornerFrequency(showCornerFrequencies);
    }

    public boolean shouldShowFits() {
        return shouldShowFits;
    }

    public void setShouldShowFits(final boolean shouldShowFits) {
        this.shouldShowFits = shouldShowFits;
    }

    public void showConstraintWarningBanner(boolean visible) {
        spectraPlot.showConstraintWarningBanner(visible);
    }
}
