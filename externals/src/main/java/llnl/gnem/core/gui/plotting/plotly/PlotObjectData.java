/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
package llnl.gnem.core.gui.plotting.plotly;

import java.util.ArrayList;
import java.util.List;

import llnl.gnem.core.gui.plotting.api.FillModes;
import llnl.gnem.core.gui.plotting.api.HoverModes;

public class PlotObjectData {
    private final List<Double> xData;
    private final List<Double> yData;
    private final List<Double[]> zData;
    private final List<Double> cData;
    private final List<Double> errorData;
    private final List<Double> errorDataMinus;
    private boolean useHorizontalErrorBars;
    private HoverModes hoverMode;
    private FillModes fillMode;
    private String hoverTemplate;

    private final List<String> textData;
    private PlotTrace traceStyle;

    public PlotObjectData() {
        this(null);
    }

    public PlotObjectData(final PlotTrace traceStyle) {
        xData = new ArrayList<>(0);
        yData = new ArrayList<>(0);
        zData = new ArrayList<>(0);
        cData = new ArrayList<>(0);
        errorData = new ArrayList<>(0);
        errorDataMinus = new ArrayList<>(0);
        useHorizontalErrorBars = false;
        textData = new ArrayList<>(0);
        this.traceStyle = traceStyle;
    }

    public List<Double> getXdata() {
        return xData;
    }

    public List<Double> getYdata() {
        return yData;
    }

    public List<Double[]> getZdata() {
        return zData;
    }

    public List<Double> getColorData() {
        return cData;
    }

    public List<Double> getErrorData() {
        return errorData;
    }

    public List<Double> getErrorDataMinus() {
        return errorDataMinus;
    }

    public boolean useHorizontalErrorBars() {
        return useHorizontalErrorBars;
    }

    public void setErrorBarsHorizontal(final boolean useHorizontalErrorBars) {
        this.useHorizontalErrorBars = useHorizontalErrorBars;
    }

    public List<String> getTextData() {
        return textData;
    }

    public HoverModes getHoverMode() {
        return hoverMode;
    }

    public void setHoverMode(HoverModes hoverMode) {
        this.hoverMode = hoverMode;
    }

    public FillModes getFillMode() {
        return fillMode;
    }

    public void setFillMode(FillModes fillMode) {
        this.fillMode = fillMode;
    }

    public String getHoverTemplate() {
        return hoverTemplate;
    }

    public void setHoverTemplate(String hoverTemplate) {
        this.hoverTemplate = hoverTemplate;
    }

    public PlotTrace getTraceStyle() {
        return traceStyle;
    }

    public void setTraceStyle(final PlotTrace traceStyle) {
        this.traceStyle = traceStyle;
    }

    public void clear() {
        xData.clear();
        yData.clear();
        zData.clear();
        cData.clear();
        errorData.clear();
        errorDataMinus.clear();
        textData.clear();
    }
}
