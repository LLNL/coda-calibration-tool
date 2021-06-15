/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.gui.plotting.plotly;

public class PlotPoints {
    private double[] x;
    private double[] y;
    private double[] screenX;
    private double[] screenY;

    public PlotPoints() {
        //nop
    }

    public PlotPoints(double[] x, double[] y, double[] screenX, double[] screenY) {
        this.x = x;
        this.y = y;
        this.screenX = screenX;
        this.screenY = screenY;
    }

    public double[] getX() {
        return x;
    }

    public void setX(double[] x) {
        this.x = x;
    }

    public double[] getY() {
        return y;
    }

    public void setY(double[] y) {
        this.y = y;
    }

    public double[] getScreenX() {
        return screenX;
    }

    public void setScreenX(double[] screenX) {
        this.screenX = screenX;
    }

    public double[] getScreenY() {
        return screenY;
    }

    public void setScreenY(double[] screenY) {
        this.screenY = screenY;
    }

}
