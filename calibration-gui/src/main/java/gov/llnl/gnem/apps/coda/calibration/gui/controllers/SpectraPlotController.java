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

import java.util.function.Function;

import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;

public class SpectraPlotController extends BaseSpectraPlotController<SpectraMeasurement> {

    public SpectraPlotController(final Function<SpectraMeasurement, Double> dataFunction) {
        super(dataFunction);
    }

    public void showConstraintWarningBanner(boolean visible) {
        this.getSpectralPlot().showConstraintWarningBanner(visible);
    }
}
