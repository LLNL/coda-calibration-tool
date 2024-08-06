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
package llnl.gnem.core.gui.plotting.api;

import java.io.Serializable;

public interface Axis extends Serializable {

    public enum Type {
        X, Y, Z, Y_RIGHT, LOG_X, LOG_Y, X_TOP
    }

    public enum TickFormat {
        POW_10("pow10"), LOG10_DYNE_CM_TO_MW("log10_dyne_cm_to_mw");

        private String format;

        TickFormat(String format) {
            this.format = format;
        }

        public String getFormat() {
            return format;
        }
    }

    public void setText(String label);

    public String getText();

    public Type getAxisType();

    public void setAxisType(Type axisType);

    public double getMax();

    public double getMin();

    public void setMin(double min);

    public void setMax(double max);

    public TickFormat getTickFormat();

    public void setTickFormat(TickFormat tickFormat);

    public String getTickFormatString();

    public void setTickFormatString(String tickFormat);
}
