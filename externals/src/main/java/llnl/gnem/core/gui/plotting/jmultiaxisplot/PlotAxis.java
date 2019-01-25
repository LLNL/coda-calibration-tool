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
import java.awt.Graphics;

import llnl.gnem.core.gui.plotting.AxisScale;
import llnl.gnem.core.gui.plotting.HorizAlignment;
import llnl.gnem.core.gui.plotting.TickDir;
import llnl.gnem.core.gui.plotting.TickLabel;
import llnl.gnem.core.gui.plotting.TickMetrics.LinearTickMetrics;
import llnl.gnem.core.gui.plotting.TickMetrics.LogTickMetrics;

/**
 * Base class for axes contained in a JSubPlot.
 *
 * @author Doug Dodge
 */
public abstract class PlotAxis {

    /**
     * Constructor for the PlotAxis object
     */
    public PlotAxis() {
        ticks = new TickData();
        label = new LabelData();

        axisColor = Color.black;
        axisPenWidth = 1;
        visible = true;
        fullyDecorateAxis = false;
        minFullyDecoratedAxisLength = 25.0;
    }
    //---------------------------------------------------------------------------

    /**
     * Gets the number of minor ticks that will be rendered by this axis
     *
     * @return The number of minor ticks
     */
    public int getNumMinorTicks() {
        return ticks.getNumMinor();
    }

    /**
     * Sets the numMinorTicks attribute of the PlotAxis object
     *
     * @param v
     *            The new numMinorTicks value
     */
    public void setNumMinorTicks(int v) {
        int val = v >= 0 ? v : 0;
        ticks.setNumMinor(val);
    }

    /**
     * Gets the number of major ticks that will be rendered by this axis
     *
     * @return The number of major ticks
     */
    public double getMajorTickLen() {
        return ticks.getMajorLen();
    }

    /**
     * Sets the majorTickLen attribute of the PlotAxis object
     *
     * @param v
     *            The new majorTickLen value
     */
    public void setMajorTickLen(double v) {
        double val = v >= 0 ? v : 0;
        ticks.setMajorLen(val);
    }

    /**
     * Gets the length in millimeters of the minor ticks in this axis
     *
     * @return The minor tick length (mm)
     */
    public double getMinorTickLen() {
        return ticks.getMinorLen();
    }

    /**
     * Sets the minorTickLen attribute of the PlotAxis object
     *
     * @param v
     *            The new minorTickLen value
     */
    public void setMinorTickLen(double v) {
        double val = v >= 0 ? v : 0;
        ticks.setMinorLen(val);
    }

    /**
     * Gets the direction (in/out) of ticks drawn on this axis.
     *
     * @return The tick Direction value
     */
    public TickDir getTickDirection() {
        return ticks.getDir();
    }

    /**
     * Sets the tickDirection attribute of the PlotAxis object
     *
     * @param v
     *            The new tickDirection value
     */
    public void setTickDirection(TickDir v) {
        ticks.setDir(v);
    }

    /**
     * Gets the visibility of ticks in this axis
     *
     * @return The visibility value
     */
    public boolean getTicksVisible() {
        return ticks.isVisible();
    }

    /**
     * Sets the ticksVisible attribute of the PlotAxis object
     *
     * @param v
     *            The new ticksVisible value
     */
    public void setTicksVisible(boolean v) {
        ticks.setVisible(v);
    }

    /**
     *
     * @return The label Font
     */
    public Font getTickFont() {
        return ticks.getFont();
    }

    /**
     *
     * @param font
     */
    public void setTickFont(Font font) {
        ticks.setFont(font);
    }

    /**
     * Gets the tickFontName attribute of the PlotAxis object
     *
     * @return The tickFontName value
     */
    public String getTickFontName() {
        return ticks.getFontName();
    }

    /**
     * Sets the tickFontName attribute of the PlotAxis object
     *
     * @param v
     *            The new tickFontName value
     */
    public void setTickFontName(String v) {
        ticks.setFontName(v);
    }

    /**
     * Gets the tickFontColor attribute of the PlotAxis object
     *
     * @return The tickFontColor value
     */
    public Color getTickFontColor() {
        return ticks.getFontColor();
    }

    /**
     * Sets the tickFontColor attribute of the PlotAxis object
     *
     * @param v
     *            The new tickFontColor value
     */
    public void setTickFontColor(Color v) {
        ticks.setFontColor(v);
    }

    /**
     * Gets the tickFontSize attribute of the PlotAxis object
     *
     * @return The tickFontSize value
     */
    public int getTickFontSize() {
        return ticks.getFontSize();
    }

    /**
     * Sets the tickFontSize attribute of the PlotAxis object
     *
     * @param v
     *            The new tickFontSize value
     */
    public void setTickFontSize(int v) {
        ticks.setFontSize(v);
    }

    /**
     * Gets the labelColor attribute of the PlotAxis object
     *
     * @return The labelColor value
     */
    public Color getLabelColor() {
        return label.getColor();
    }

    /**
     * Sets the labelColor attribute of the PlotAxis object
     *
     * @param v
     *            The new labelColor value
     */
    public void setLabelColor(Color v) {
        label.setColor(v);
    }

    /**
     *
     * @return The label Font
     */
    public Font getLabelFont() {
        return label.getFont();
    }

    /**
     *
     * @param font
     */
    public void setLabelFont(Font font) {
        label.setFont(font);
    }

    /**
     * 
     * @param size
     */
    public void setLabelFontSize(int size) {
        label.setSize(size);
    }

    /**
     * Gets the labelOffset attribute of the PlotAxis object
     *
     * @return The labelOffset value
     */
    public double getLabelOffset() {
        return label.getOffset();
    }

    /**
     * Sets the labelOffset attribute of the PlotAxis object
     *
     * @param v
     *            The new labelOffset value
     */
    public void setLabelOffset(double v) {
        label.setOffset(v);
    }

    /**
     * Gets the labelText attribute of the PlotAxis object
     *
     * @return The labelText value
     */
    public String getLabelText() {
        return label.getText();
    }

    /**
     * Sets the labelText attribute of the PlotAxis object
     *
     * @param v
     *            The new labelText value
     */
    public void setLabelText(String v) {
        label.setText(v);
    }

    /**
     * Gets the labelVisible attribute of the PlotAxis object
     *
     * @return The labelVisible value
     */
    public boolean getLabelVisible() {
        return label.isVisible();
    }

    /**
     * Sets the labelVisible attribute of the PlotAxis object
     *
     * @param v
     *            The new labelVisible value
     */
    public void setLabelVisible(boolean v) {
        label.setVisible(v);
    }

    /**
     * Gets the axisColor attribute of the PlotAxis object
     *
     * @return The axisColor value
     */
    public Color getAxisColor() {
        return axisColor;
    }

    /**
     * Sets the axisColor attribute of the PlotAxis object
     *
     * @param v
     *            The new axisColor value
     */
    public void setAxisColor(Color v) {
        axisColor = v;
    }

    /**
     * Gets the axisPenWidth attribute of the PlotAxis object
     *
     * @return The axisPenWidth value
     */
    public int getAxisPenWidth() {
        return axisPenWidth;
    }

    /**
     * Sets the axisPenWidth attribute of the PlotAxis object
     *
     * @param v
     *            The new axisPenWidth value
     */
    public void setAxisPenWidth(int v) {
        axisPenWidth = v > 0 ? v : 1;
    }

    /**
     * Gets the visible attribute of the PlotAxis object
     *
     * @return The visible value
     */
    public boolean getVisible() {
        return visible;
    }

    /**
     * Sets the visible attribute of the PlotAxis object
     *
     * @param v
     *            The new visible value
     */
    public void setVisible(boolean v) {
        visible = v;
    }

    /**
     * Gets suitable axis minimum, maximum, and increment values based on the
     * input range. Converted from C++ version by Doug Dodge. Copyright (c)
     * 2000, Original version by Michael P.D. Bramley. Permission is granted to
     * use this code without restriction as long as this copyright notice
     * appears in all source files. Author: Michael P.D. Bramley Synopsis:
     * Function to define axis based on range of data using properties of
     * decimal place-value system and linearity of axis.
     *
     * @param minIn
     *            The minimum data value
     * @param maxIn
     *            The maximum data value.
     * @return A TickMetrics object holding the suggested axis minimum, maximum,
     *         and increment values.
     */
    public static LinearTickMetrics defineAxis(double minIn, double maxIn) {
        return defineAxis(minIn, maxIn, false);
    }

    public static LinearTickMetrics defineAxis(double minIn, double maxIn, boolean fullyDecorate) {
        // define local variables...
        double min = minIn;
        double max = maxIn;
        double inc;
        double testInc;
        double // candidate increment value
        Test_min;
        double // minimum scale value
        Test_max;
        double // maximum scale value
        Range = max - min;
        // range of data

        int i = 0;
        // counter

        // don't create problems -- solve them

        if (Range < 0) {
            return new LinearTickMetrics(0.0, 0.0, 0.0, fullyDecorate);
        } // handle special case of repeated values
        else if (Range == 0) {
            min = Math.ceil(max) - 1;
            max = min + 1;
            inc = 1;
            return new LinearTickMetrics(min, max, inc, fullyDecorate);
        }

        // compute candidate for increment

        testInc = Math.pow(10.0, Math.ceil(Math.log10(Range / 10)));

        // establish maximum scale value...

        Test_max = ((long) (max / testInc)) * testInc;
        if (Test_max < max) {
            Test_max += testInc;
        }

        // establish minimum scale value...

        Test_min = Test_max;
        double small = 1.0E-10;
        do {
            ++i;
            Test_min -= testInc;
            if (Math.abs(Test_min - min) < small) {
                break;
            }
        } while (Test_min >= min);

        // subtracting small values can screw up the scale limits,
        // eg: if DefineAxis is called with (min,max)=(0.01, 0.1),
        // then the calculated scale is 1.0408E17 TO 0.05 BY 0.01.
        // the following if statement corrects for this...

        if (Math.abs(Test_min) < small) {
            Test_min = 0;
        }

        // adjust for too few tick marks

        if (i < 6) {
            testInc /= 2;
            if ((Test_min + testInc) <= min) {
                Test_min += testInc;
            }
            if ((Test_max - testInc) >= max) {
                Test_max -= testInc;
            }
        }

        // pass back axis definition to caller

        min = Test_min;
        max = Test_max;
        inc = testInc;
        return new LinearTickMetrics(min, max, inc, fullyDecorate);
    }

    public static LogTickMetrics defineLogAxis(double minIn, double maxIn, boolean fullyDecorate) {
        double logMin = Math.log10(minIn);
        double logMax = Math.log10(maxIn);
        int displayMin = (int) logMin;
        int displayMax = (int) logMax;
        if (displayMin > logMin) {
            --displayMin;
        }
        if (displayMax < logMax) {
            ++displayMax;
        }

        return new LogTickMetrics(displayMin, displayMax, fullyDecorate);
    }

    /**
     * Information about the tick to be rendered on this axis.
     */
    protected TickData ticks;
    /**
     * Information about the label associated with this axis.
     */
    protected LabelData label;
    /**
     * The pen color of this axis.
     */
    protected Color axisColor;
    /**
     * The pen width used to render this axis.
     */
    protected int axisPenWidth;
    /**
     * Controls whether this axis will be rendered.
     */
    protected boolean visible;
    /**
     * Description of the Field
     */
    protected boolean fullyDecorateAxis;
    /**
     * Description of the Field
     */
    protected double minFullyDecoratedAxisLength;

    /**
     * render the ticks for this axis.
     *
     * @param g
     *            The graphics context.
     * @param min
     *            The minimum value of the axis.
     * @param max
     *            The maximum value of the axis.
     * @param Scale
     *            Controls whether the axis has a linear or log(10) scale.
     */
    protected void renderTicks(Graphics g, double min, double max, AxisScale Scale) {
        if (!ticks.isVisible()) {
            return;
        }
        if (Scale == AxisScale.LINEAR) {
            renderLinearTicks(g, min, max);
        } else {
            renderLogTicks(g, min, max);
        }
    }
    //---------------------------------------------------------------------------

    /**
     * render ticks for the linear scale case
     *
     * @param g
     *            The graphics context
     * @param minIn
     *            The minimum value of the axis
     * @param maxIn
     *            The maximum value of the axis
     */
    protected void renderLinearTicks(Graphics g, double minIn, double maxIn) {
        double min = minIn;
        double max = maxIn;
        double displayMin = min;
        double displayMax = max;
        LinearTickMetrics ticks = defineAxis(min, max, fullyDecorateAxis);
        while (ticks.hasNext()) {
            double val = ticks.getNext();
            if (fullyDecorateAxis) {
                double inc2 = ticks.getIncrement() / (getNumMinorTicks() + 1);
                for (int j = 1; j <= getNumMinorTicks(); ++j) {
                    double v = val + j * inc2;
                    if (v >= displayMin && v <= displayMax) {
                        renderTick(g, v, new TickLabel(), false, HorizAlignment.CENTER);
                    }
                    // render minor tick
                }
            }
            if (val >= displayMin && val <= displayMax) {
                String tmp = formatValue(val);
                renderTick(g, val, new TickLabel(tmp), true, HorizAlignment.CENTER);
                // render major tick
            }
        }
    }

    /**
     * render ticks for the log(10) case
     *
     * @param g
     *            The graphics context
     * @param minIn
     *            The minimum value of the axis
     * @param max
     *            The maximum value of the axis
     */
    protected void renderLogTicks(Graphics g, double minIn, double max) {
        if (max <= 0) {
            throw new IllegalStateException("Max is <= 0 and Xscale is logarithmic!");
        }

        double min = minIn;
        if (min <= 0) {
            min = max / 100;
        }
        LogTickMetrics ticks = defineLogAxis(min, max, fullyDecorateAxis);
        while (ticks.hasNext()) {
            double val = ticks.getNext();
            if (fullyDecorateAxis) {
                for (int j = 2; j <= 9; ++j) {
                    double v = val * j;
                    if (v >= min && v <= max) {
                        renderTick(g, v, new TickLabel(), false, HorizAlignment.CENTER);
                    }
                    // render minor tick
                }
            }
            if (val >= min && val <= max) {
                String tmp = formatValue(val);
                renderTick(g, val, new TickLabel(tmp), true, HorizAlignment.CENTER);
                // render major tick
            }
        }
    }
    //---------------------------------------------------------------------------

    private static String formatValue(double val) {
        String tmp = String.format("%.6G", val);
        int idxE = tmp.indexOf('E');
        if (idxE > 0) {
            String part2 = tmp.substring(idxE);
            int idxLastChr = idxE - 1;
            int idxDot = tmp.indexOf('.');
            if (idxDot > 0) {
                while (idxLastChr > idxDot) {
                    if (tmp.charAt(idxLastChr) != '0') {
                        break;
                    }
                    --idxLastChr;
                }
                if (tmp.charAt(idxLastChr) == '.') {
                    ++idxLastChr;
                }
                tmp = tmp.substring(0, idxLastChr + 1) + part2;
            }

        } else {
            int idxLastChr = tmp.length() - 1;
            int idxDot = tmp.indexOf('.');
            if (idxDot > 0) {
                while (idxLastChr > idxDot) {
                    if (tmp.charAt(idxLastChr) != '0') {
                        break;
                    }
                    --idxLastChr;
                }
                if (tmp.charAt(idxLastChr) == '.') {
                    --idxLastChr;
                }
                tmp = tmp.substring(0, idxLastChr + 1);
            }
        }
        return tmp;
    }
    //---------------------------------------------------------------------------

    /**
     * render a single tick. This action is performed by derived classes because
     * some of the specifics depend on the nature of the derived class
     *
     * @param g
     *            The graphics context
     * @param val
     *            The axis value at the position of the tick
     * @param label
     *            A label that may be associated with the tick
     * @param isMajor
     *            true if this is a major tick mark
     */
    protected abstract void renderTick(Graphics g, double val, TickLabel label, boolean isMajor, HorizAlignment alignment);

}
