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
package gov.llnl.gnem.apps.coda.calibration.gui.plotting;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import javafx.geometry.Point2D;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.PlottingUtils;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;

public class SpectralPlot extends Pane implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String AVG_MW_LEGEND_LABEL = "Avg";

    private static final double EPSILON = 1E-14;

    private double xmin = 1.0;
    private double xmax = -xmin;
    private double ymin = xmin;
    private double ymax = xmax;

    private double defaultXMin = 1.0;
    private double defaultXMax = 1.0;
    private double defaultYMin = 15.0;
    private double defaultYMax = 27.0;

    private final NumberFormat dfmt = NumberFormatFactory.twoDecimalOneLeadingZero();

    private boolean plotCorners = false;

    private BasicPlot plot;
    private transient PlotFactory plotFactory;

    private Axis yAxis;
    private Axis xAxis;

    private boolean autoCalculateXaxisRange = true;
    private boolean autoCalculateYaxisRange = true;
    private final Map<Point2D, List<Symbol>> symbolMap = new HashMap<>();
    private final transient Object symbolMapLock = new Object();

    private final transient List<Symbol> selectedData = new ArrayList<>();
    private final transient Object selectedDataLock = new Object();

    public SpectralPlot() {
        plotFactory = new PlotlyPlotFactory();
        setupPlot();
    }

    private void setupPlot() {
        plot = plotFactory.basicPlot();

        yAxis = plotFactory.axis(Axis.Type.Y, "Y-axis label");
        xAxis = plotFactory.axis(Axis.Type.LOG_X, "X-axis label");
        plot.addAxes(yAxis, xAxis);

        plot.getTitle().setText("Title String");
        plot.getTitle().setFontSize(14);
        plot.setSymbolSize(8);
        plot.attachToDisplayNode(this);
    }

    public void clearPlot() {
        xmin = 0.0;
        xmax = 0.0;
        ymin = 0.0;
        ymax = 0.0;
    }

    /**
     * @param points
     *            <p>
     *            List of {X, Y, {@link SymbolStyles}} values. Presently this is
     *            used mostly as { log10(centerFrequency), log10(amplitiude),
     *            {@link SymbolStyles} }
     *            </p>
     */
    protected void plotXYdata(final List<PlotPoint> points) {
        if (points != null) {
            symbolMap.clear();
            selectedData.clear();
            points.stream().filter(p -> rescalePlot(p.getX(), p.getY())).map(v -> {
                String label;
                if (v instanceof LabeledPlotPoint) {
                    label = ((LabeledPlotPoint) v).getLabel();
                } else {
                    label = "";
                }
                return plotFactory.createSymbol(v.getStyle(), label, v.getX(), v.getY(), v.getColor(), Color.BLACK, v.getColor(), label, true);
            }).forEach(symbol -> {
                synchronized (symbolMapLock) {
                    symbolMap.computeIfAbsent(new Point2D(symbol.getX(), symbol.getY()), v -> new ArrayList<>()).add(symbol);
                }
                plot.addPlotObject(symbol);
            });
        }
    }

    /**
     * @param plots
     *            <p>
     *            List of {X, Y, {@link SymbolStyles}} values. Presently this is
     *            used mostly as { log10(centerFrequency), log10(amplitiude),
     *            {@link SymbolStyles} }
     *            </p>
     * @param showLegend
     * @param spectra
     *            <p>
     *            Spectra containing the amp/freq/mw information for a
     *            calibration spectra
     *            </p>
     */
    public void plotXYdata(final List<PlotPoint> plots, final List<Spectra> spectra) {
        plot.clear();

        if (plots.size() > 1) {
            // get the average for each freq and plot it - connect the points
            final List<PlotPoint> averages = createAveragePlot(sortPointsByX(plots));

            final double[] x = new double[averages.size()];
            final double[] y = new double[averages.size()];
            for (int i = 0; i < averages.size(); i++) {
                final PlotPoint point = averages.get(i);
                x[i] = point.getX();
                y[i] = point.getY();
            }

            final Line line = plotFactory.line(x, y, Color.BLACK, LineStyles.SOLID, 2);
            line.setName(AVG_MW_LEGEND_LABEL);
            plot.addPlotObject(line);
        }

        if (spectra != null) {
            for (final Spectra spec : spectra) {
                if (plotCorners && spec.getCornerFrequency() != null) {
                    plotCornerFrequency(spec.getCornerFrequency());
                }
                plotSpectraObject(plot, spec);
            }
        }

        plotXYdata(plots);

        refreshPlotAxes();

        plot.replot();
    }

    private void plotCornerFrequency(final double cornerFreq) {
        plot.addPlotObject(plotFactory.verticalLine(cornerFreq, 50, "~Fc (" + dfmt.format(cornerFreq) + ")").setDraggable(false).setLogScaleX(true).setFillColor(Color.BLACK));
    }

    private void plotSpectraObject(final BasicPlot jsubplot, final Spectra spectra) {
        if (spectra != null && !spectra.getSpectraXY().isEmpty()) {
            final List<PlotPoint> netMwValues = spectra.getSpectraXY().stream().map(d -> new PlotPoint(Math.pow(10, d.getX()), d.getY(), null, null, null)).collect(Collectors.toList());
            final double[] x = new double[netMwValues.size()];
            final double[] y = new double[netMwValues.size()];
            for (int i = 0; i < netMwValues.size(); i++) {
                final PlotPoint point = netMwValues.get(i);
                x[i] = point.getX();
                y[i] = point.getY();
            }

            Line line = null;
            switch (spectra.getType()) {
            case REF:
                line = plotFactory.line(x, y, Color.BLACK, LineStyles.DASH, 2);
                break;
            case VAL:
                line = plotFactory.line(x, y, Color.BLUE, LineStyles.DASH_DOT, 2);
                break;
            case UQ1:
                if (y.length > 0) {
                    line = plotFactory.line(x, y, Color.LIGHTGRAY, LineStyles.DASH, 2);
                    line.setLegendGrouping("UQ1");
                    line.setName(dfmt.format(y[0]));
                    line.showInLegend(false);

                    jsubplot.addPlotObject(PlottingUtils.legendOnlyLine("UQ1", plotFactory, Color.LIGHTGRAY, LineStyles.DASH));
                }
                break;
            case UQ2:
                if (y.length > 0) {
                    line = plotFactory.line(x, y, Color.LIGHTGRAY, LineStyles.DOT, 2);
                    line.setLegendGrouping("UQ2");
                    line.setName(dfmt.format(y[0]));
                    line.showInLegend(false);

                    jsubplot.addPlotObject(PlottingUtils.legendOnlyLine("UQ2", plotFactory, Color.LIGHTGRAY, LineStyles.DASH));
                }
                break;
            case FIT:
                line = plotFactory.line(x, y, Color.RED, LineStyles.DASH, 2);
                break;
            default:
                break;
            }
            if (line != null) {
                if (SPECTRA_TYPES.UQ1 != spectra.getType() && SPECTRA_TYPES.UQ2 != spectra.getType()) {
                    final NumberFormat df = NumberFormatFactory.oneDecimalMinimum();
                    if (spectra.getApparentStress() > 0.0) {
                        final NumberFormat df2 = NumberFormatFactory.twoDecimalOneLeadingZero();
                        line.setName(spectra.getType().name() + ' ' + df.format(spectra.getMw()) + " @ " + df2.format(spectra.getApparentStress()) + "MPa");
                    } else {
                        line.setName(spectra.getType().name() + ' ' + df.format(spectra.getMw()));
                    }
                }

                jsubplot.addPlotObject(line);
            }
        }
    }

    private List<PlotPoint> createAveragePlot(final List<PlotPoint> allOrderedPoints) {
        final List<PlotPoint> xyvector = new ArrayList<>();
        final List<Double> amplitudes = new ArrayList<>();
        double xTest = allOrderedPoints.get(0).getX();

        for (final PlotPoint point : allOrderedPoints) {
            if (Math.abs(Math.abs(point.getX()) - Math.abs(xTest)) < EPSILON) {
                amplitudes.add(point.getY());
            } else {
                Double sum = 0d;
                for (final Double vals : amplitudes) {
                    sum += vals;
                }
                final Double amplitude = sum / amplitudes.size();
                final PlotPoint xypoint = new PlotPoint(xTest, amplitude, null, null, null);
                xyvector.add(xypoint);

                xTest = point.getX();
                amplitudes.clear();
                amplitudes.add(point.getY());
            }
        }
        // don't forget to add the last point
        if (!amplitudes.isEmpty()) {
            Double sum = 0d;
            for (final Double vals : amplitudes) {
                sum += vals;
            }
            final Double amplitude = sum / amplitudes.size();
            final PlotPoint xypoint = new PlotPoint(xTest, amplitude, null, null, null);
            xyvector.add(xypoint);
        }

        return xyvector;
    }

    private List<PlotPoint> sortPointsByX(final List<PlotPoint> inPlots) {
        final List<PlotPoint> plots = new ArrayList<>(inPlots);
        final int smallestIndex = getSmallestX(plots);
        final List<PlotPoint> orderedList = new ArrayList<>(plots.size());
        orderedList.add(plots.remove(smallestIndex));
        while (!plots.isEmpty()) {
            // Find the index of the closest point (using another method)
            final int nearestIndex = findNearestIndex(orderedList.get(orderedList.size() - 1), plots);
            // Remove from the unorderedList and add to the ordered one
            orderedList.add(plots.remove(nearestIndex));
        }

        return orderedList;
    }

    private int getSmallestX(final List<PlotPoint> plots) {
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

    private int findNearestIndex(final PlotPoint thisPoint, final List<PlotPoint> listToSearch) {
        double nearestDistSquared = Double.POSITIVE_INFINITY;
        int nearestIndex = -1;
        for (int i = 0; i < listToSearch.size(); i++) {
            final PlotPoint point2 = listToSearch.get(i);
            final double distsq = (thisPoint.getX() - point2.getX()) * (thisPoint.getX() - point2.getX());
            if (distsq < nearestDistSquared) {
                nearestDistSquared = distsq;
                nearestIndex = i;
            }
        }
        return nearestIndex;
    }

    /**
     * Ensure that the plot includes the minimum and maximum x and y points
     *
     * @param x
     * @param y
     * @return
     */
    public boolean rescalePlot(final double x, final double y) {
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
        return true;
    }

    public void setAllXlimits(final double xmin, final double xmax) {
        plot.setAxisLimits(new AxisLimits(Axis.Type.LOG_X, xmin, xmax));
        this.xmin = xmin;
        this.xmax = xmax;
        plot.replot();
    }

    public void setAllXlimits() {
        plot.setAxisLimits(new AxisLimits(Axis.Type.LOG_X, defaultXMin, defaultXMax));
        this.xmin = defaultXMin;
        this.xmax = defaultXMax;
        plot.replot();
    }

    public void setAllYlimits(final double ymin, final double ymax) {
        plot.setAxisLimits(new AxisLimits(Axis.Type.Y, ymin, ymax));
        this.ymin = ymin;
        this.ymax = ymax;
        plot.replot();
    }

    public void setAllYlimits() {
        plot.setAxisLimits(new AxisLimits(Axis.Type.Y, defaultYMin, defaultYMax));
        this.ymin = defaultYMin;
        this.ymax = defaultYMax;
        plot.replot();
    }

    public void refreshPlotAxes() {
        double xMin;
        double xMax;
        double yMin;
        double yMax;

        if (autoCalculateYaxisRange) {
            yMin = ymin - Math.abs(ymin * .1);
            yMax = ymax + Math.abs(ymax * .1);
        } else {
            yMin = defaultYMin;
            yMax = defaultYMax;
        }

        if (autoCalculateXaxisRange) {
            xMin = xmin - Math.abs(xmin * .1);
            xMax = xmax + Math.abs(xmax * .1);
        } else {
            xMin = defaultXMin;
            xMax = defaultXMax;
        }
        plot.setAxisLimits(new AxisLimits(Axis.Type.LOG_X, Math.log10(xMin), Math.log10(xMax)), new AxisLimits(Axis.Type.Y, yMin, yMax));
        plot.replot();
    }

    /** Set the Title, X and Y axis labels simultaneously */
    public void setLabels(final String title, final String xlabel, final String ylabel) {
        plot.getTitle().setText(title);
        xAxis.setText(xlabel);
        yAxis.setText(ylabel);
    }

    public BasicPlot getSubplot() {
        return plot;
    }

    public double getDefaultYMin() {
        return defaultYMin;
    }

    public void setDefaultYMin(final double defaultYMin) {
        this.defaultYMin = defaultYMin;
    }

    public double getDefaultYMax() {
        return defaultYMax;
    }

    public void setDefaultYMax(final double defaultYMax) {
        this.defaultYMax = defaultYMax;
    }

    public double getDefaultXMin() {
        return defaultXMin;
    }

    public void setDefaultXMin(final double defaultXMin) {
        this.defaultXMin = defaultXMin;
    }

    public double getDefaultXMax() {
        return defaultXMax;
    }

    public void setDefaultXMax(final double defaultXMax) {
        this.defaultXMax = defaultXMax;
    }

    public void setAutoCalculateXaxisRange(final boolean autoCalculateXaxisRange) {
        this.autoCalculateXaxisRange = autoCalculateXaxisRange;
    }

    public void setAutoCalculateYaxisRange(final boolean autoCalculateYaxisRange) {
        this.autoCalculateYaxisRange = autoCalculateYaxisRange;
    }

    public Map<Point2D, List<Symbol>> getSymbolMap() {
        return symbolMap;
    }

    public void selectPoint(Point2D xyPoint) {
        List<Symbol> symbols = symbolMap.get(xyPoint);
        if (symbols != null) {
            symbols.forEach(symbol -> {
                plot.removePlotObject(symbol);
                symbol.setEdgeColor(symbol.getFillColor());
                symbol.setFillColor(Color.YELLOW);
                symbol.setZindex(1000);
                plot.addPlotObject(symbol);
                synchronized (selectedDataLock) {
                    selectedData.add(symbol);
                }
            });
        }
    }

    public void deselectPoint(Point2D xyPoint) {
        List<Symbol> symbols = symbolMap.get(xyPoint);
        if (symbols != null) {
            symbols.forEach(symbol -> {
                //proxy for "are you active"
                if (symbol.getZindex() != null) {
                    setStandardSymbolStyle(symbol);
                    synchronized (selectedDataLock) {
                        selectedData.remove(symbol);
                    }
                }
            });
        }
    }

    public void deselectAllPoints() {
        synchronized (selectedDataLock) {
            selectedData.forEach(this::setStandardSymbolStyle);
            selectedData.clear();
        }
    }

    private void setStandardSymbolStyle(PlotObject symbol) {
        plot.removePlotObject(symbol);
        symbol.setFillColor(symbol.getEdgeColor());
        symbol.setEdgeColor(Color.BLACK);
        symbol.setZindex(null);
        plot.addPlotObject(symbol);
    }

    public void setPointsActive(List<Point2D> points, boolean active) {
        for (Point2D xyPoint : points) {
            List<Symbol> symbols = symbolMap.get(xyPoint);
            if (symbols != null) {
                symbols.forEach(symbol -> {
                    plot.removePlotObject(symbol);
                    if (active) {
                        symbol.setFillColor(symbol.getEdgeColor());
                        symbol.setEdgeColor(Color.BLACK);
                    } else {
                        symbol.setEdgeColor(symbol.getFillColor());
                        symbol.setFillColor(Color.GRAY);
                    }
                    plot.addPlotObject(symbol);
                });
            }
        }
        plot.replot();
    }

    public void showCornerFrequency(final boolean showCornerFreq) {
        this.plotCorners = showCornerFreq;
    }

    public String getTitle() {
        return plot.getTitle().getText();
    }
}
