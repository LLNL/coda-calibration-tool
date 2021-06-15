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
package llnl.gnem.core.gui.swing.waveform.plotPrefs;

import java.awt.Color;
import java.io.Serializable;

import llnl.gnem.core.gui.swing.plotting.PenStyle;
import llnl.gnem.core.gui.swing.plotting.jmultiaxisplot.PickTextPosition;

/**
 *
 * @author dodge1
 */
public class PickPrefs implements Serializable {

    private double height;
    private double heightInMillimeters; // Used for picks in record-section views.
    private Color color;
    private int width;
    private int textSize;
    private PickTextPosition textPosition;
    private PenStyle penStyle;
    static final long serialVersionUID = -7088607112685498455L;

    public PickPrefs() {
        height = 0.8;
        heightInMillimeters = 5.0;
        color = Color.black;
        width = 2;
        textSize = 10;
        textPosition = PickTextPosition.BOTTOM;
        penStyle = PenStyle.SOLID;
    }

    public PickPrefs(double pickHeight, double pickHeightInMillimeters, Color pickColor, int pickWidth, int pickTextSize, PickTextPosition position, PenStyle penStyle) {
        this.height = pickHeight;
        this.heightInMillimeters = pickHeightInMillimeters;
        this.color = pickColor;
        this.width = pickWidth;
        this.textSize = pickTextSize;
        this.textPosition = position;
        this.penStyle = penStyle;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

    public PickTextPosition getTextPosition() {
        return textPosition;
    }

    public void setTextPosition(PickTextPosition textPosition) {
        this.textPosition = textPosition;
    }

    public PenStyle getPenStyle() {
        return penStyle;
    }

    public void setPenStyle(PenStyle penStyle) {
        this.penStyle = penStyle;
    }

    public double getHeightInMillimeters() {
        return heightInMillimeters;
    }

    public void setHeightInMillimeters(double heightInMillimeters) {
        this.heightInMillimeters = heightInMillimeters;
    }
}
