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
package llnl.gnem.core.gui.plotting.jmultiaxisplot;

import java.awt.Color;
import java.awt.Font;

import llnl.gnem.core.gui.plotting.TickDir;

public class TickData {

    private int NumMinor = 4;
    private double MajorLen = 3; // In physical units, e.g. mm
    private double MinorLen = 2; // In physical units, e.g. mm
    private TickDir dir = TickDir.IN;
    private boolean visible = true;
    private String FontName = "Arial";
    private int FontSize = 10;
    private int FontStyle = Font.PLAIN;
    private Color FontColor = Color.black;

    public TickDir getDir() {
        return dir;
    }

    public void setDir(TickDir dir) {
        this.dir = dir;
    }

    public Font getFont() {
        return new Font(FontName, FontStyle, FontSize);
    }

    public void setFont(Font font) {
        FontName = font.getName();
        FontSize = font.getSize();
        FontStyle = font.getStyle();
    }

    public Color getFontColor() {
        return FontColor;
    }

    public void setFontColor(Color fontColor) {
        FontColor = fontColor;
    }

    public String getFontName() {
        return FontName;
    }

    public void setFontName(String fontName) {
        FontName = fontName;
    }

    public int getFontSize() {
        return FontSize;
    }

    public void setFontSize(int fontSize) {
        FontSize = fontSize;
    }

    public double getMajorLen() {
        return MajorLen;
    }

    public void setMajorLen(double majorLen) {
        MajorLen = majorLen;
    }

    public double getMinorLen() {
        return MinorLen;
    }

    public void setMinorLen(double minorLen) {
        MinorLen = minorLen;
    }

    public int getNumMinor() {
        return NumMinor;
    }

    public void setNumMinor(int numMinor) {
        NumMinor = numMinor;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

}
