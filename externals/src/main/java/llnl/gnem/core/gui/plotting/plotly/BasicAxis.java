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
package llnl.gnem.core.gui.plotting.plotly;

import llnl.gnem.core.gui.plotting.api.Axis;

public class BasicAxis implements Axis {

    private static final long serialVersionUID = 1L;
    private Type axisType;
    private String text;
    private double min;
    private double max;
    private TickFormat tickFormat;

    public BasicAxis(final Type axisType, final String label) {
        this.axisType = axisType;
        this.text = label;
    }

    @Override
    public void setText(final String label) {
        this.text = label;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public Type getAxisType() {
        return axisType;
    }

    @Override
    public void setAxisType(final Type axisType) {
        this.axisType = axisType;
    }

    @Override
    public double getMax() {
        return max;
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public void setMin(final double min) {
        this.min = min;
    }

    @Override
    public void setMax(final double max) {
        this.max = max;
    }

    @Override
    public TickFormat getTickFormat() {
        return tickFormat;
    }

    @Override
    public void setTickFormat(TickFormat tickFormat) {
        this.tickFormat = tickFormat;
    }

}
