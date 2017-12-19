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

import llnl.gnem.core.gui.plotting.TickDir;

/**
 *
 * @author dodge1
 */
public class TickPrefs implements Serializable{
    private boolean visible = true;
    private TickDir direction = TickDir.IN;
    private String fontName = "Arial";
    private int fontSize = 10;
    private int fontStyle = Font.PLAIN;
    private Color color = Color.black;
    static final long serialVersionUID = -6254132982942263566L;


    /**
     * @return the direction
     */
    public TickDir getDirection() {
        return direction;
    }

    /**
     * @param direction the direction to set
     */
    public void setDirection(TickDir direction) {
        this.direction = direction;
    }

    /**
     *
     * @return the font to set
     */
    public Font getFont() {
        return new Font(fontName, fontStyle, fontSize);
    }

    /**
     *
     * @param font to set
     */
    public void setFont(Font font) {
        fontName = font.getName();
        fontSize = font.getSize();
        fontStyle = font.getStyle();
    }

    public Color getFontColor() {
        return color;
    }

    /**
     * @param color the color to set
     */
    public void setFontColor(Color color) {
        this.color = color;
    }

//    /**
//     * @return the fontName
//     */
//    public String getFontName() {
//        return fontName;
//    }

//    /**
//     * @param fontName the fontName to set
//     */
//    public void setFontName(String fontName) {
//        this.fontName = fontName;
//    }

//    /**
//     * @return the fontSize
//     */
//    public int getFontSize() {
//        return fontSize;
//    }

//    /**
//     * @param fontSize the fontSize to set
//     */
//    public void setFontSize(int fontSize) {
//        this.fontSize = fontSize;
//    }

    /**
     * @return the visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible the visible to set
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
