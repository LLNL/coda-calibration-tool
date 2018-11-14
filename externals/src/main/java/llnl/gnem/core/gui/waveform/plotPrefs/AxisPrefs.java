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
package llnl.gnem.core.gui.waveform.plotPrefs;

import java.awt.Color;
import java.io.Serializable;

/**
 *
 * @author dodge1
 */
public class AxisPrefs implements Serializable {
    private LabelPrefs labelPrefs;
    private TickPrefs tickPrefs;
    private Color color;
    private int penWidth;
    private boolean visible;
    static final long serialVersionUID = 7471394890458400754L;

    public AxisPrefs() {
        labelPrefs = new LabelPrefs();
        tickPrefs = new TickPrefs();
        color = Color.black;
        penWidth = 1;
        visible = true;
    }

    /**
     * @return the color
     */
    public Color getColor() {
        return color;
    }

    /**
     * @param color
     *            the color to set
     */
    public void setColor(Color color) {
        this.color = color;
    }

    /**
     * @return the labelPrefs
     */
    public LabelPrefs getLabelPrefs() {
        return labelPrefs;
    }

    /**
     * @return the penWidth
     */
    public int getPenWidth() {
        return penWidth;
    }

    /**
     * @param penWidth
     *            the penWidth to set
     */
    public void setPenWidth(int penWidth) {
        this.penWidth = penWidth;
    }

    /**
     * @return the tickPrefs
     */
    public TickPrefs getTickPrefs() {
        return tickPrefs;
    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible
     *            the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
