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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.Legend;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.SymbolLegend;
import llnl.gnem.core.gui.plotting.SymbolLegend.SymbolTextPair;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PlotProperties;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.TickScaleFunc;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.SymbolDef;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;
import llnl.gnem.core.gui.plotting.plotobject.SymbolStyle;

/**
 * @author E. Matzel
 * @since Oct 22, 2007
 */
public class SpectralPlot extends JMultiAxisPlot {

    private static final long serialVersionUID = 1L;

    private static final String AVG_MW_LEGEND_LABEL = "Avg";

    private static final double EPSILON = 0.00000000000001;

    private transient JSubplot jsubplot;

    private double xmin = Double.MAX_VALUE;
    private double xmax = -xmin;
    private double ymin = xmin;
    private double ymax = xmax;

    private transient PlotProperties properties = new PlotProperties();

    private double defaultXMin = -1.;
    private double defaultXMax = 1.;
    private double defaultYMin = 19.0;
    private double defaultYMax = 27.0;
    private int symbol = 0;

    public SpectralPlot() {
        super();
        setupPlot();
    }

    public SpectralPlot(List<Point2D.Double> datavector, Color color, SymbolStyle symbolstyle) {
        super();
        setupPlot();
        plotXYdata(datavector, color, symbolstyle);
    }

    private void setupPlot() {

        this.getXaxis().setLabelText("X-axis label");
        this.getXaxis().setTickScaleFunction(new TickScaleFunc() {

            private final NumberFormat dfmt2 = new DecimalFormat("#0.0#");

            @Override
            public String func(String tick) {
                if (tick == null || tick.isEmpty()) {
                    return tick;
                }
                try {
                    Double val = Math.pow(10, Double.parseDouble(tick));
                    return dfmt2.format(val);
                } catch (NumberFormatException e) {
                    return tick;
                }
            }
        });

        this.getPlotBorder().setBackgroundColor(new Color(200, 200, 200));
        this.setHorizontalOffset(15);
        this.setVerticalOffset(15);
        this.setBorderWidth(15);
        this.getPlotBorder().setDrawBox(true);
        this.getPlotBorder().setFillRegion(true);
        this.getPlotRegion().setDrawBox(false);
        this.getPlotRegion().setFillRegion(true);
        this.getPlotRegion().setBackgroundColor(Color.white);
        this.getTitle().setText("Title String");
        this.getTitle().setFontSize(14);

        jsubplot = this.addSubplot();
        jsubplot.getPlotRegion().setDrawBox(true);

        jsubplot.getPlotRegion().setFillRegion(true);
        jsubplot.getPlotRegion().setDrawBox(true);
        jsubplot.getPlotRegion().setBackgroundColor(new Color(0.96F, 0.96F, 0.96F));
        jsubplot.getYaxis().setLabelText("Y-axis label");
    }

    public void clearPlot() {
        xmin = defaultXMin;
        xmax = defaultXMax;
        ymin = defaultYMin;
        ymax = defaultYMax;
        jsubplot.Clear();
    }

    /**
     * @param plots
     *            <p>
     *            Map of [Legend Key, {X, Y}] values. Presently this is used
     *            mostly as [Station Name, { log10(centerFrequency),
     *            log10(amplitiude) }]
     *            </p>
     * @param showLegend
     */
    public void plotXYdata(final Map<String, List<Point2D.Double>> plots, Boolean showLegend) {
        plotXYdata(plots, showLegend, null, null);
    }

    /**
     * @param plots
     *            <p>
     *            Map of [Legend Key, {X, Y}] values. Presently this is used
     *            mostly as [Station Name, { log10(centerFrequency),
     *            log10(amplitiude) }]
     *            </p>
     * @param showLegend
     * @param referenceSpectra
     *            <p>
     *            A source spectra containing the amp/freq/mw information for a
     *            reference event used in the calibration
     *            </p>
     * @param theoreticalSpectra
     *            <p>
     *            A source spectra containing the amp/freq/mw information for a
     *            theoretical best fit spectra
     *            </p>
     */
    public void plotXYdata(final Map<String, List<Point2D.Double>> plots, Boolean showLegend, Spectra... spectra) {
        List<Point2D.Double> allPoints = new ArrayList<>();
        ArrayList<SymbolTextPair> symbolsForLegend = new ArrayList<>();

        // line based legends for trending
        Legend legend = new Legend(getTitle().getFontName(), getTitle().getFontSize(), HorizPinEdge.RIGHT, VertPinEdge.TOP, 5, 5);

        Map<String, SymbolStyle> symbols = new HashMap<>();

        // plot each set of points - do NOT connect the points
        int i = 0;
        int max = plots.size();
        for (Entry<String, List<Point2D.Double>> entry : plots.entrySet()) {
            allPoints.addAll(entry.getValue());
        }

        if (plots.size() > 1) {
            // get the average for each freq and plot it - connect the points
            List<Point2D.Double> allOrderedPoints = sortPointsByX(allPoints);
            List<Point2D.Double> averages = createAveragePlot(allOrderedPoints);

            float[] x = new float[averages.size()];
            float[] y = new float[averages.size()];
            for (i = 0; i < averages.size(); i++) {
                Point2D.Double point = averages.get(i);
                x[i] = (float) point.getX();
                y[i] = (float) point.getY();
            }

            Line line = new Line(x, y, Color.BLACK, PaintMode.COPY, PenStyle.SOLID, 2);
            jsubplot.AddPlotObject(line);
            legend.addLabeledLine(AVG_MW_LEGEND_LABEL, line);
        }

        for (int j = 0; j < spectra.length; j++) {
            plotSpectraObject(jsubplot, spectra[j], legend);
        }

        for (Entry<String, List<Point2D.Double>> entry : plots.entrySet()) {
            String key = entry.getKey();
            Color color = getSpacedOutColour(i++, max);
            SymbolStyle symbolStyle;
            if (!symbols.containsKey(key)) {
                symbolStyle = nextSymbol();
                symbols.put(key, symbolStyle);
            } else {
                symbolStyle = symbols.get(key);
            }
            plotXYdata(entry.getValue(), color, symbolStyle);
            SymbolDef symbolDef = new SymbolDef(symbolStyle, properties.getSymbolSize(), color, color);
            SymbolTextPair pair = new SymbolTextPair(key, symbolDef);
            symbolsForLegend.add(pair);
        }

        refreshPlotAxes();

        Collections.sort(symbolsForLegend, (s1, s2) -> StringUtils.compare(s1.getText(), s2.getText()));

        // symbol based legend for Stations
        SymbolLegend symbolLegend = new SymbolLegend(symbolsForLegend, getTitle().getFontName(), getTitle().getFontSize(), HorizPinEdge.LEFT, VertPinEdge.BOTTOM, 5, 5);

        if (showLegend) {
            jsubplot.AddPlotObject(symbolLegend);
            jsubplot.AddPlotObject(legend);
        }

        repaint();
    }

    private void plotSpectraObject(JSubplot jsubplot, Spectra spectra, Legend legend) {
        int i;
        float[] x;
        float[] y;
        if (spectra != null && !spectra.getSpectraXY().isEmpty()) {
            List<Point2D.Double> netMwValues = spectra.getSpectraXY();
            x = new float[netMwValues.size()];
            y = new float[netMwValues.size()];
            for (i = 0; i < netMwValues.size(); i++) {
                Point2D.Double point = netMwValues.get(i);
                x[i] = (float) point.getX();
                y[i] = (float) point.getY();
            }

            Line line;
            switch (spectra.getType()) {
            case REF:
                line = new Line(x, y, Color.BLACK, PaintMode.COPY, PenStyle.DASH, 2);
                break;
            default:
                line = new Line(x, y, Color.RED, PaintMode.COPY, PenStyle.DASH, 2);
                break;
            }
            jsubplot.AddPlotObject(line);
            DecimalFormat df = new DecimalFormat("#.0#");
            if (spectra.getStressDrop() > 0.0) {
                DecimalFormat df2 = new DecimalFormat("#0.0#");
                legend.addLabeledLine(spectra.getType().name() + ' ' + df.format(spectra.getMw()) + " @ " + df2.format(spectra.getStressDrop()) + "MPa", line);
            } else {
                legend.addLabeledLine(spectra.getType().name() + ' ' + df.format(spectra.getMw()), line);
            }
        }
    }

    private SymbolStyle nextSymbol() {
        SymbolStyle nextStyle;
        symbol = ++symbol % 8;
        switch (symbol) {
        case 0:
            nextStyle = SymbolStyle.CIRCLE;
            break;
        case 1:
            nextStyle = SymbolStyle.SQUARE;
            break;
        case 2:
            nextStyle = SymbolStyle.DIAMOND;
            break;
        case 3:
            nextStyle = SymbolStyle.TRIANGLEUP;
            break;
        case 4:
            nextStyle = SymbolStyle.TRIANGLEDN;
            break;
        case 5:
            nextStyle = SymbolStyle.PLUS;
            break;
        case 6:
            nextStyle = SymbolStyle.CROSS;
            break;
        case 7:
            nextStyle = SymbolStyle.STAR5;
            break;
        case 8:
            nextStyle = SymbolStyle.HEXAGON;
            break;
        default:
            nextStyle = SymbolStyle.ERROR_BAR;
        }

        return nextStyle;
    }

    private List<Point2D.Double> createAveragePlot(List<Point2D.Double> allOrderedPoints) {
        List<Point2D.Double> xyvector = new ArrayList<>();
        List<Double> amplitudes = new ArrayList<>();
        double xTest = allOrderedPoints.get(0).getX();

        for (Point2D.Double point : allOrderedPoints) {
            if (Math.abs(Math.abs(point.getX()) - Math.abs(xTest)) < EPSILON) {
                amplitudes.add(point.getY());
            } else {
                Double sum = 0d;
                for (Double vals : amplitudes) {
                    sum += vals;
                }
                Double amplitude = sum / amplitudes.size();
                Point2D.Double xypoint = new Point2D.Double(xTest, amplitude);
                xyvector.add(xypoint);

                xTest = point.getX();
                amplitudes.clear();
                amplitudes.add(point.getY());
            }
        }
        // don't forget to add the last point
        if (!amplitudes.isEmpty()) {
            Double sum = 0d;
            for (Double vals : amplitudes) {
                sum += vals;
            }
            Double amplitude = sum / amplitudes.size();
            Point2D.Double xypoint = new Point2D.Double(xTest, amplitude);
            xyvector.add(xypoint);
        }

        return xyvector;
    }

    private List<Point2D.Double> sortPointsByX(List<Point2D.Double> allPoints) {
        int smallestIndex = getSmallestX(allPoints);
        List<Point2D.Double> orderedList = new ArrayList<>();
        orderedList.add(allPoints.remove(smallestIndex));
        while (!allPoints.isEmpty()) {
            // Find the index of the closest point (using another method)
            int nearestIndex = findNearestIndex(orderedList.get(orderedList.size() - 1), allPoints);
            // Remove from the unorderedList and add to the ordered one
            orderedList.add(allPoints.remove(nearestIndex));
        }

        return orderedList;
    }

    private int getSmallestX(List<Point2D.Double> allPoints) {
        int smallest = -1;
        double test = Double.MAX_VALUE;
        for (int i = 0; i < allPoints.size(); i++) {
            if (test > allPoints.get(i).getX()) {
                test = allPoints.get(i).getX();
                smallest = i;
            }
        }
        return smallest;
    }

    private int findNearestIndex(Point2D.Double thisPoint, List<Point2D.Double> listToSearch) {
        double nearestDistSquared = Double.POSITIVE_INFINITY;
        int nearestIndex = -1;
        for (int i = 0; i < listToSearch.size(); i++) {
            Point2D.Double point2 = listToSearch.get(i);
            double distsq = (thisPoint.x - point2.x) * (thisPoint.x - point2.x);
            if (distsq < nearestDistSquared) {
                nearestDistSquared = distsq;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    /**
     * This function splits the red-green-blue colour wheel into n equally
     * spaced divisions and returns the colour from a particular division.
     * 
     * @param index
     *            - The index of the colour to return, the range is 0 -
     *            (count-1)
     * @param count
     *            - The number of divisions to split the HSV colour wheel into
     * @return A java.awt.Color object containing the color.
     * @author HughesR
     */
    private Color getSpacedOutColour(int index, int count) {
        final float saturation = 0.95f; // Saturation
        final float brightness = 0.8f; // Brightness
        float hue = (float) index / (float) count;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    /**
     * Plot a series of double valued (x,y) points in the subplot
     *
     * @param datavector
     *            - a vector of Point2D.Double objects
     */
    public final void plotXYdata(List<Point2D.Double> data, Color color, SymbolStyle symbolstyle) {
        for (Point2D.Double point : data) {
            double x = point.getX();
            double y = point.getY();

            rescalePlot(x, y);

            Symbol symbol = SymbolFactory.createSymbol(symbolstyle, x, y, properties.getSymbolSize(), color, properties.getSymbolEdgeColor(), color, "", true, false, 10.0);
            jsubplot.AddPlotObject(symbol);
        }
    }

    /**
     * Ensure that the plot includes the minimum and maximum x and y points
     *
     * @param x
     * @param y
     */
    public void rescalePlot(double x, double y) {
        if (x < xmin) {
            xmin = x;
        }
        if (x > xmax) {
            xmax = x;
        }
        if (y < ymin) {
            ymin = y;
        }
        if (y > ymax) {
            ymax = y;
        }
    }

    public void refreshPlotAxes() {
        if (properties.getAutoCalculateYaxisRange()) {
            jsubplot.SetAxisLimits(xmin, xmax, ymin, ymax);
        } else {
            jsubplot.getYaxis().setMin(properties.getMinYAxisValue());
            jsubplot.getYaxis().setMax(properties.getMaxYAxisValue());
            jsubplot.getXaxis().setMax(xmax);
            jsubplot.getXaxis().setMin(xmin);
        }
    }

    /** Set the Title, X and Y axis labels simultaneously */
    public void setLabels(String title, String xlabel, String ylabel) {
        this.getTitle().setText(title);
        this.getXaxis().setLabelText(xlabel);
        jsubplot.getYaxis().setLabelText(ylabel);
    }

    public JSubplot getSubplot() {
        return jsubplot;
    }

    @Override
    public void grabFocus() {
        requestFocusInWindow();
    }

    public double getDefaultYMin() {
        return defaultYMin;
    }

    public void setDefaultYMin(double defaultYMin) {
        this.defaultYMin = defaultYMin;
    }

    public double getDefaultYMax() {
        return defaultYMax;
    }

    public void setDefaultYMax(double defaultYMax) {
        this.defaultYMax = defaultYMax;
    }

    public double getDefaultXMin() {
        return defaultXMin;
    }

    public void setDefaultXMin(double defaultXMin) {
        this.defaultXMin = defaultXMin;
    }

    public double getDefaultXMax() {
        return defaultXMax;
    }

    public void setDefaultXMax(double defaultXMax) {
        this.defaultXMax = defaultXMax;
    }
}
