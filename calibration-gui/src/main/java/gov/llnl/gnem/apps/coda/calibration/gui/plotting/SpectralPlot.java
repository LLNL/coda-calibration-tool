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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.awt.Color;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import llnl.gnem.core.gui.plotting.HorizPinEdge;
import llnl.gnem.core.gui.plotting.Legend;
import llnl.gnem.core.gui.plotting.PaintMode;
import llnl.gnem.core.gui.plotting.PenStyle;
import llnl.gnem.core.gui.plotting.VertPinEdge;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.JSubplot;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.PlotProperties;
import llnl.gnem.core.gui.plotting.jmultiaxisplot.TickScaleFunc;
import llnl.gnem.core.gui.plotting.plotobject.Line;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;

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

    public SpectralPlot() {
        super();
        setupPlot();
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
     *            List of {X, Y, Symbol} values. Presently this is used mostly
     *            as { log10(centerFrequency), log10(amplitiude), Symbol }
     *            </p>
     * @param showLegend
     */
    public void plotXYdata(final List<PlotPoint> plots, Boolean showLegend) {
        plotXYdata(plots, showLegend, null, null);
    }

    /**
     * @param plots
     *            <p>
     *            List of {X, Y, Symbol} values. Presently this is used mostly
     *            as { log10(centerFrequency), log10(amplitiude), Symbol }
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
    public void plotXYdata(final List<PlotPoint> plots, Boolean showLegend, Spectra... spectra) {

        // line based legends for trending
        Legend legend = new Legend(getTitle().getFontName(), getTitle().getFontSize(), HorizPinEdge.RIGHT, VertPinEdge.TOP, 5, 5);

        if (plots.size() > 1) {
            // get the average for each freq and plot it - connect the points
            List<PlotPoint> averages = createAveragePlot(sortPointsByX(plots));

            float[] x = new float[averages.size()];
            float[] y = new float[averages.size()];
            for (int i = 0; i < averages.size(); i++) {
                PlotPoint point = averages.get(i);
                x[i] = point.getX().floatValue();
                y[i] = point.getY().floatValue();
            }

            Line line = new Line(x, y, Color.BLACK, PaintMode.COPY, PenStyle.SOLID, 2);
            jsubplot.AddPlotObject(line);
            legend.addLabeledLine(AVG_MW_LEGEND_LABEL, line);
        }

        for (int j = 0; j < spectra.length; j++) {
            plotSpectraObject(jsubplot, spectra[j], legend);
        }

        plotXYdata(plots);

        refreshPlotAxes();

        if (showLegend) {
            jsubplot.AddPlotObject(legend);
        }

        repaint();
    }

    private void plotSpectraObject(JSubplot jsubplot, Spectra spectra, Legend legend) {
        if (spectra != null && !spectra.getSpectraXY().isEmpty()) {
            List<PlotPoint> netMwValues = spectra.getSpectraXY().stream().map(d -> new PlotPoint(d.getX(), d.getY(), null, null)).collect(Collectors.toList());
            float[] x = new float[netMwValues.size()];
            float[] y = new float[netMwValues.size()];
            for (int i = 0; i < netMwValues.size(); i++) {
                PlotPoint point = netMwValues.get(i);
                x[i] = point.getX().floatValue();
                y[i] = point.getY().floatValue();
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

    private List<PlotPoint> createAveragePlot(List<PlotPoint> allOrderedPoints) {
        List<PlotPoint> xyvector = new ArrayList<>();
        List<Double> amplitudes = new ArrayList<>();
        double xTest = allOrderedPoints.get(0).getX();

        for (PlotPoint point : allOrderedPoints) {
            if (Math.abs(Math.abs(point.getX()) - Math.abs(xTest)) < EPSILON) {
                amplitudes.add(point.getY());
            } else {
                Double sum = 0d;
                for (Double vals : amplitudes) {
                    sum += vals;
                }
                Double amplitude = sum / amplitudes.size();
                PlotPoint xypoint = new PlotPoint(xTest, amplitude, null, null);
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
            PlotPoint xypoint = new PlotPoint(xTest, amplitude, null, null);
            xyvector.add(xypoint);
        }

        return xyvector;
    }

    private List<PlotPoint> sortPointsByX(List<PlotPoint> inPlots) {
        List<PlotPoint> plots = new ArrayList<>(inPlots);
        int smallestIndex = getSmallestX(plots);
        List<PlotPoint> orderedList = new ArrayList<>();
        orderedList.add(plots.remove(smallestIndex));
        while (!plots.isEmpty()) {
            // Find the index of the closest point (using another method)
            int nearestIndex = findNearestIndex(orderedList.get(orderedList.size() - 1), plots);
            // Remove from the unorderedList and add to the ordered one
            orderedList.add(plots.remove(nearestIndex));
        }

        return orderedList;
    }

    private int getSmallestX(List<PlotPoint> plots) {
        int smallest = -1;
        double test = Double.MAX_VALUE;
        for (int i = 0; i < plots.size(); i++) {
            if (test > plots.get(i).getX()) {
                test = plots.get(i).getX();
                smallest = i;
            }
        }
        return smallest;
    }

    private int findNearestIndex(PlotPoint thisPoint, List<PlotPoint> listToSearch) {
        double nearestDistSquared = Double.POSITIVE_INFINITY;
        int nearestIndex = -1;
        for (int i = 0; i < listToSearch.size(); i++) {
            PlotPoint point2 = listToSearch.get(i);
            double distsq = (thisPoint.getX() - point2.getX()) * (thisPoint.getX() - point2.getX());
            if (distsq < nearestDistSquared) {
                nearestDistSquared = distsq;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    /**
     * Plot a series of double valued (x,y) points in the subplot
     *
     * @param datavector
     *            - a vector of PlotPoint objects
     */
    public final void plotXYdata(List<PlotPoint> data) {
        if (data != null) {
            data.stream()
                .peek(p -> rescalePlot(p.getX(), p.getY()))
                .map(v -> SymbolFactory.createSymbol(v.getStyle(), v.getX(), v.getY(), properties.getSymbolSize(), v.getColor(), properties.getSymbolEdgeColor(), v.getColor(), "", true, false, 10.0))
                .forEach(jsubplot::AddPlotObject);
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
            jsubplot.setAxisLimits(xmin, xmax, ymin, ymax);
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
