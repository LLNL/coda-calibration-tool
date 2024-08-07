/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.FillModes;
import llnl.gnem.core.gui.plotting.api.HoverModes;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.ObjectGroup;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.PlottingUtils;
import llnl.gnem.core.gui.plotting.api.Symbol;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.plotly.BasicObjectGroup;

public class SpectralPlot extends Pane implements Serializable {

    private static final String MATERIAL_ICONS_LARGE = "material-icons-large";
    private static final String LIKELY_POORLY_CONSTRAINED = "Likely poorly constrained!";

    private static final long serialVersionUID = 1L;

    private static final String AVG_MW_LEGEND_LABEL = "Avg";

    private static final String CODA_MW_LABEL = "Model Fit";

    private static final double EPSILON = 1E-14;

    private double xmin = 1.0;
    private double xmax = -xmin;
    private double ymin = xmin;
    private double ymax = -xmax;

    private double defaultXMin = 1.0;
    private double defaultXMax = 1.0;
    private double defaultYMin = 8.0;
    private double defaultYMax = 20.0;

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

    private BorderPane plotContainerPane;
    private HBox warningPane;

    public SpectralPlot() {
        plotContainerPane = new BorderPane();
        plotContainerPane.prefHeightProperty().bind(this.heightProperty());
        plotContainerPane.prefWidthProperty().bind(this.widthProperty());
        this.getChildren().add(plotContainerPane);
        plotFactory = new PlotlyPlotFactory();
        setupPlot();
    }

    private void setupPlot() {
        plot = plotFactory.basicPlot();

        yAxis = plotFactory.axis(Axis.Type.Y, "Y-axis label");
        xAxis = plotFactory.axis(Axis.Type.LOG_X, "X-axis label");
        plot.addAxes(yAxis, xAxis);

        plot.getTitle().setText("Title String");
        plot.getTitle().setFontSize(16);
        plot.setSymbolSize(12);

        StackPane wrapper = new StackPane();
        wrapper.prefWidth(Double.MAX_VALUE);
        wrapper.prefHeight(Double.MAX_VALUE);
        plot.attachToDisplayNode(wrapper);
        plot.setMargin(30, 40, 50, null);

        plotContainerPane.setCenter(wrapper);
        plotContainerPane.setPickOnBounds(false);

        warningPane = new HBox();
        warningPane.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
        Label warningIcon = new Label("\uE000");
        warningIcon.getStyleClass().add(MATERIAL_ICONS_LARGE);
        warningIcon.setPrefSize(32.0, 32.0);
        warningIcon.setAlignment(Pos.CENTER);

        Label warningText = new Label(LIKELY_POORLY_CONSTRAINED);
        warningText.setStyle("-fx-font-weight:bold; -fx-font-size: 28px;");

        warningPane.getChildren().add(warningIcon);
        warningPane.getChildren().add(warningText);
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
                    //Dyne-cm to Newton-meters
                    symbolMap.computeIfAbsent(new Point2D(symbol.getX(), symbol.getY() - 7.0), v -> new ArrayList<>()).add(symbol);
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
     * @param spectra
     *            <p>
     *            Spectra containing the amp/freq/mw information for a
     *            calibration spectra
     *            </p>
     */
    public void plotXYdata(final List<PlotPoint> plots, final List<Spectra> spectra, final MeasuredMwDetails mwDetails, final String label) {
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
            String lineLabel = AVG_MW_LEGEND_LABEL;
            if (label != null) {
                lineLabel = label;
            }
            line.setName(lineLabel);
            plot.addPlotObject(line);
        }

        if (spectra != null) {
            for (final Spectra spec : spectra) {
                if (plotCorners && spec.getCornerFrequency() != null) {
                    plotCornerFrequency("~Fc", spec, mwDetails, Color.BLACK);
                }
                plotSpectraObject(plot, spec);
            }
        }

        plotXYdata(plots);

        refreshPlotAxes();

        plot.replot();
    }

    private void plotCornerFrequency(final String name, final Spectra spec, final MeasuredMwDetails mwDetails, final Color color) {
        List<Double> mwValuesY = spec.getSpectraXY().stream().map(d -> d.getY() - 7.0).collect(Collectors.toList());

        Double minY = Collections.min(mwValuesY);
        Double maxY = Collections.max(mwValuesY);
        Double cF = spec.getCornerFrequency();

        double error = 0.0;
        double errorMinus = 0.0;

        if (mwDetails != null) {
            error = mwDetails.getCornerFreq2Max();
            errorMinus = mwDetails.getCornerFreq2Min();
        }

        ObjectGroup cornerFreqLine = buildVerticalLineWithHorizontalErrorBars(name, cF, maxY, minY, error, errorMinus, color);
        cornerFreqLine.plotGroup(plot);
    }

    public void plotCornerFrequency(String name, final Double cornerFreq, final Double error, final Double errorMinus, final Double topY, final Double bottomY, final Color color) {
        ObjectGroup cornerFreqLine = buildVerticalLineWithHorizontalErrorBars(name, cornerFreq, topY, bottomY, error, errorMinus, color);
        cornerFreqLine.getLegendObject().setFillColor(Color.BLACK);
        cornerFreqLine.plotGroup(plot);
    }

    public Line drawRectangle(double x, double y, double xx, double yy, Color color) {
        final double[] posX = new double[5];
        final double[] posY = new double[5];

        // Setting shadow box coordinates
        posX[0] = x;
        posX[1] = xx;
        posX[2] = xx;
        posX[3] = x;
        posX[4] = x;
        posY[0] = y;
        posY[1] = y;
        posY[2] = yy;
        posY[3] = yy;
        posY[4] = y;

        Line plotRect = plotFactory.line(posX, posY, color, LineStyles.SOLID, 1);
        plotRect.setFillMode(FillModes.TO_SELF); // Fills the area between the lines

        return plotRect;
    }

    public ObjectGroup buildVerticalLineWithHorizontalErrorBars(final String name, final Double x, final Double topY, final Double bottomY, final Double error, final Double errorMinus,
            final Color color) {

        final double[] xPos = new double[1];
        final double[] shadowX = new double[5];
        final double[] shadowY = new double[5];
        final double[] maxY = new double[1];
        final double[] midY = new double[1];
        final double[] minY = new double[1];
        final double[] e1 = new double[1];
        final double[] e2 = new double[1];
        final double[] height = new double[1];

        xPos[0] = x;
        maxY[0] = topY;
        minY[0] = bottomY;
        midY[0] = (topY + bottomY) / 2.0;
        e1[0] = error - x;
        e2[0] = x - errorMinus;
        height[0] = (topY - bottomY) / 2.0;

        // Setting shadow box coordinates
        shadowX[0] = errorMinus;
        shadowX[1] = errorMinus;
        shadowX[2] = error;
        shadowX[3] = error;
        shadowX[4] = errorMinus;
        shadowY[0] = topY;
        shadowY[1] = bottomY;
        shadowY[2] = bottomY;
        shadowY[3] = topY;
        shadowY[4] = topY;

        Line shadowBox = drawRectangle(errorMinus, topY, error, bottomY, color.deriveColor(0.0, 1.0, 1.0, 0.1));
        shadowBox.setHoverMode(HoverModes.SKIP); // Skips hover actions for the shadow box

        Line valueLine = plotFactory.lineWithErrorBars(xPos, midY, height, height);
        valueLine.setFillColor(color);
        valueLine.setUseHorizontalErrorBars(false);

        Line topErrorLine = plotFactory.lineWithErrorBars(xPos, maxY, e1, e2);
        topErrorLine.setFillColor(color);
        topErrorLine.setUseHorizontalErrorBars(true);

        Line botErrorLine = plotFactory.lineWithErrorBars(xPos, minY, e1, e2);
        botErrorLine.setFillColor(color);
        botErrorLine.setUseHorizontalErrorBars(true);

        ObjectGroup cornerFreqBar = new BasicObjectGroup(plotFactory, name);
        cornerFreqBar.setHoverName(name);
        cornerFreqBar.setHoverTemplate("%{x:.2f} (" + dfmt.format(errorMinus) + ", " + dfmt.format(error) + ")");

        // Order matters! Add shadow first to plot it under error bars.
        cornerFreqBar.addPlotObject(shadowBox);
        cornerFreqBar.addPlotObject(valueLine);
        cornerFreqBar.addPlotObject(topErrorLine);
        cornerFreqBar.addPlotObject(botErrorLine);

        return cornerFreqBar;
    }

    private void plotSpectraObject(final BasicPlot jsubplot, final Spectra spectra) {
        if (spectra != null && !spectra.getSpectraXY().isEmpty()) {
            //Dyne-cm to nm for plot, in log
            final List<PlotPoint> netMwValues = spectra.getSpectraXY().stream().map(d -> new PlotPoint(Math.pow(10, d.getX()), d.getY() - 7.0, null, null, null)).collect(Collectors.toList());
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

                    jsubplot.addPlotObject(PlottingUtils.legendOnlyLine("UQ2", plotFactory, Color.LIGHTGRAY, LineStyles.DOT));
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
                    String spectraName;
                    if (SPECTRA_TYPES.FIT == spectra.getType()) {
                        spectraName = CODA_MW_LABEL;
                    } else {
                        spectraName = spectra.getType().name();
                    }

                    line.setName(spectraName);
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
            yMin = ymin - Math.abs(ymin * .3);
            yMax = ymax + Math.abs(ymax * .3);
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
                // proxy for "are you active"
                if (symbol.getZindex() != null) {
                    setStandardSymbolStyle(symbol);
                    synchronized (selectedDataLock) {
                        selectedData.remove(symbol);
                    }
                }
            });
        }
    }

    public List<Point2D> getSelectedPoints() {
        return selectedData.stream().map(sym -> new Point2D(sym.getX(), sym.getY())).collect(Collectors.toList());
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

    public void showConstraintWarningBanner(boolean visible) {
        if (visible) {
            plotContainerPane.setBottom(warningPane);
        } else {
            plotContainerPane.setBottom(null);
        }
    }

}
