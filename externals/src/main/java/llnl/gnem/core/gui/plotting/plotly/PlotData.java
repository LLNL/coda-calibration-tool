/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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

package llnl.gnem.core.gui.plotting.plotly;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Title;

public class PlotData implements Serializable {
    private static final long serialVersionUID = 1L;
    private AtomicBoolean plotReady;
    private PlotTrace defaultTraceStyle;
    private List<Axis> axes;
    private Map<String, PlotObjectData> defaultTypePlots;
    private Color backgroundColor;
    private Title plotTitle;
    private ObjectMapper mapper;
    private boolean showLegend;
    private boolean showGroupVelocity;
    private boolean showWindowLine;
    private boolean showCodaStartLine;
    private boolean showHorizontalErrorBars;
    private boolean useHorizontalBottomLegend;
    private String dragmode;

    public PlotData(final PlotTrace defaultTraceStyle, final Color backgroundColor, final Title plotTitle) {
        this.defaultTraceStyle = defaultTraceStyle;
        this.backgroundColor = backgroundColor;
        this.plotTitle = plotTitle;
    }

    public AtomicBoolean getPlotReady() {
        return plotReady;
    }

    public void setPlotReady(final AtomicBoolean plotReady) {
        this.plotReady = plotReady;
    }

    public PlotTrace getDefaultTraceStyle() {
        return defaultTraceStyle;
    }

    public void setDefaultTraceStyle(final PlotTrace defaultTraceStyle) {
        this.defaultTraceStyle = defaultTraceStyle;
    }

    public List<Axis> getAxes() {
        return axes;
    }

    public void setAxes(final List<Axis> axes) {
        this.axes = axes;
    }

    public Map<String, PlotObjectData> getDefaultTypePlots() {
        return defaultTypePlots;
    }

    public void setDefaultTypePlots(final Map<String, PlotObjectData> defaultTypePlots) {
        this.defaultTypePlots = defaultTypePlots;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(final Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public Title getPlotTitle() {
        return plotTitle;
    }

    public void setPlotTitle(final Title plotTitle) {
        this.plotTitle = plotTitle;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public boolean shouldShowLegend() {
        return showLegend;
    }

    public void setShowLegend(final boolean showLegend) {
        this.showLegend = showLegend;
    }

    public boolean shouldShowGroupVelocity() {
        return showGroupVelocity;
    }

    public void setShowGroupVelocity(final boolean showGroupVelocity) {
        this.showGroupVelocity = showGroupVelocity;
    }

    public boolean shouldShowCodaStartLine() {
        return showCodaStartLine;
    }

    public void setShowCodaStartLine(boolean showCodaStartLine) {
        this.showCodaStartLine = showCodaStartLine;
    }

    public boolean getShowHorizontalErrorBars() {
        return showHorizontalErrorBars;
    }

    public void setShowHorizontalErrorBars(final boolean showHorizontalErrorBars) {
        this.showHorizontalErrorBars = showHorizontalErrorBars;
    }

    public boolean shouldShowWindowLine() {
        return showWindowLine;
    }

    public void setShowWindowLines(final boolean showWindowLine) {
        this.showWindowLine = showWindowLine;
    }

    public void setUseHorizontalBottomLegend(boolean useHorizontalBottomLegend) {
        this.useHorizontalBottomLegend = useHorizontalBottomLegend;
    }

    public boolean useHorizontalBottomLegend() {
        return useHorizontalBottomLegend;
    }

    public String getDragmode() {
        return dragmode;
    }

    public void setDragmode(String dragmode) {
        this.dragmode = dragmode;
    }
}