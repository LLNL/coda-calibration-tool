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
package llnl.gnem.core.gui.waveform;

import java.awt.Color;

import llnl.gnem.core.gui.plotting.plotobject.AbstractLine;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.waveform.seismogram.TimeSeries;

/**
 *
 * @author addair1
 */
public class SeriesPlot extends SeismogramPlot<TimeSeries> {

    public SeriesPlot(String xLabel, TimeSeries... seismograms) {
        super(xLabel, seismograms);
    }

    @Override
    protected AbstractLine getLine(TimeSeries seismogram, Color lineColor) {
        AbstractLine line = (AbstractLine) getSubplot(seismogram).Plot(0.0, seismogram.getDelta(), 1, seismogram.getData());
        line.setColor(getCorrectedColor(lineColor));
        return line;
    }

    @Override
    public AbstractLine addLine(TimeSeries seismogram, Color lineColor) {
        Line line = new Line(0.0, seismogram.getDelta(), seismogram.getData(), 1);
        line.setColor(lineColor);
        getSubplot(seismogram).AddPlotObject(line);
        return line;
    }

    @Override
    protected void addSeismogram(TimeSeries seismogram) {
        seismogram.addListener(this);
        super.addSeismogram(seismogram);
    }

    public void addSeismogramPublic(TimeSeries seismogram) {
        addSeismogram(seismogram);
    }

    @Override
    public void clear() {
        super.clear();
        for (TimeSeries seismogram : getSeismograms()) {
            seismogram.removeListener(this);
        }
    }
}
