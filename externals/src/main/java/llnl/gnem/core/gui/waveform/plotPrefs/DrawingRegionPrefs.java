/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
import java.awt.Font;
import java.io.Serializable;

/**
 *
 * @author dodge1
 */
public class DrawingRegionPrefs implements Serializable{
    boolean drawBox = true;
    Color lineColor = Color.black;
    int lineWidth = 1;
    boolean fillRegion = true;
    Color backgroundColor = Color.white;
    static final long serialVersionUID = 4967515221780333339L;

    private Font font = null;
    private Color fontColor = null;

    /**
     * @return the drawBox
     */
    public boolean isDrawBox() {
        return drawBox;
    }

    /**
     * @param drawBox the drawBox to set
     */
    public void setDrawBox(boolean drawBox) {
        this.drawBox = drawBox;
    }

    /**
     * @return the lineColor
     */
    public Color getLineColor() {
        return lineColor;
    }

    /**
     * @param lineColor the lineColor to set
     */
    public void setLineColor(Color lineColor) {
        this.lineColor = lineColor;
    }

    /**
     * @return the lineWidth
     */
    public int getLineWidth() {
        return lineWidth;
    }

    /**
     * @param lineWidth the lineWidth to set
     */
    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
    }

    /**
     * @return the fillRegion
     */
    public boolean isFillRegion() {
        return fillRegion;
    }

    /**
     * @param fillRegion the fillRegion to set
     */
    public void setFillRegion(boolean fillRegion) {
        this.fillRegion = fillRegion;
    }

    /**
     * @return the backgroundColor
     */
    public Color getBackgroundColor() {
        return backgroundColor;
    }

    /**
     * @param backgroundColor the backgroundColor to set
     */
    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }



    /**
     * @return the color
     */
    public Color getFontColor() {
        if (fontColor == null) {
            fontColor = Color.black;
        }
        return fontColor;
    }

    /**
     * @param color the color to set
     */
    public void setFontColor(Color color) {
        this.fontColor = color;
    }

    /**
     *
     * @return the font to set
     */
    public Font getFont() {
        if (font == null) {
            font = new Font("Arial", Font.PLAIN, 14);
        }
        return font;
    }

    /**
     *
     * @param font to set
     */
    public void setFont(Font font) {
        this.font = font;
    }

}
