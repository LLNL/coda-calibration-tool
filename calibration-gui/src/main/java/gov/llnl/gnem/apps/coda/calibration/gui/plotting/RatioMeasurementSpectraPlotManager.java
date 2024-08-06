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

import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.controllers.SpectraRatioPlotController;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.SpectraRatioExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.util.FileDialogs;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.common.gui.plotting.LabeledPlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.HiddenHeaderTableView;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.common.model.util.SPECTRA_TYPES;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResult;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairInversionResultJoint;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatioPairOperator;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.Type;
import llnl.gnem.core.gui.plotting.api.BasicPlot;
import llnl.gnem.core.gui.plotting.api.ColorMaps;
import llnl.gnem.core.gui.plotting.api.Line;
import llnl.gnem.core.gui.plotting.api.LineStyles;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.events.PlotFreqLevelChange;
import llnl.gnem.core.gui.plotting.events.PlotObjectClick;
import llnl.gnem.core.gui.plotting.plotly.PlotObjectData;
import llnl.gnem.core.gui.plotting.plotly.PlotTrace;
import llnl.gnem.core.util.Geometry.EModel;
import llnl.gnem.core.util.Geometry.GeodeticCoordinate;

public class RatioMeasurementSpectraPlotManager {

    private static final String CONTOUR_COLOR_MAP = ColorMaps.BATLOW.getColorMap();
    private static final String X_AXIS_LABEL = "center freq (Hz)";
    private static final String Y_AXIS_LABEL = "Ratio Avg";
    private static final String SPECTRA_RATIO_PREFIX = "Spectra_Ratio_";
    private static final String ALL_PLOTS_PREFIX = "All_Plots_";

    private Color PAIR_COLOR = Color.BLUE;
    private Color JOINT_COLOR = Color.RED;

    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();
    private final NumberFormat dfmt2 = NumberFormatFactory.twoDecimalOneLeadingZero();
    private static final Logger log = LoggerFactory.getLogger(RatioMeasurementSpectraPlotManager.class);

    private final DirectoryChooser screenshotFolderChooser = new DirectoryChooser();

    private SpectraRatiosReportByEventPair ratioMeasurementReport = new SpectraRatiosReportByEventPair();

    private final SymbolStyleMapFactory symbolStyleMapFactory;
    private MapPlottingUtilities iconFactory;
    private CertLeafletMapController mapImpl;

    private Map<Point2D, SpectraRatioPairOperator> symbolMap;
    private List<SpectraRatioPairOperator> spectraRatioPairOperatorList;
    private Map<EventPair, Map<String, PlotPoint>> symbolStyleMap;
    private SpectraClient spectraClient;

    private final Property<Boolean> shouldFocus = new SimpleBooleanProperty(false);

    private SpectraRatioPlotController ratioSpectralPlotController;

    private final ObservableList<EventPair> eventPairList = FXCollections.observableArrayList();
    private final SortedList<EventPair> sortedEventPairList = new SortedList<>(eventPairList);

    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private StackPane rootPane;

    @FXML
    private StackPane borderPane;

    @FXML
    private StackPane spectraRatioPlotNode;

    @FXML
    private StackPane inversionPlotsNode;

    @FXML
    private Button snapshotButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button showMapButton;

    @FXML
    private Button viewRawSpectraBtn;

    @FXML
    private HBox topBar;

    @FXML
    private ComboBox<EventPair> eventPairComboBox;

    @FXML
    private Label topTitleLabel;

    @FXML
    protected HiddenHeaderTableView<Pair<String, String>> ratioSummaryTable;

    @FXML
    protected TableColumn<Pair<String, String>, String> ratioSummaryNameCol;

    @FXML
    protected TableColumn<Pair<String, String>, String> ratioSummaryValueCol;

    private final ObservableList<Pair<String, String>> ratioSummaryValues = FXCollections.observableArrayList();

    private PlotFactory plotFactory;
    private BasicPlot combinedContourPlots;

    // Cache current spectra plot x-axis range
    private double curMinX = 0.0;
    private double curMaxX = 0.0;

    boolean isLFLMode = false;
    boolean isHFLMode = false;

    private Line userSetHFL = null;
    private Line userSetLFL = null;
    private int userSetTablePosIdx = -1;
    private int LFL_IDX_OFFSET = 1;
    private int HFL_IDX_OFFSET = 2;
    private int CALC_STRESS_IDX_OFFSET = 3;

    private Map<String, BasicPlot> plotMap;
    private Map<String, Map<String, PlotObjectData>> plotDataMap;
    private Map<String, Pair<Axis, Axis>> plotAxisMap;
    private Map<EventPair, Pair<Double, Double>> userSetFreqDataMap = new HashMap<>();

    private SpectraRatioExporter spectraRatioExporter;
    private EventBus bus;

    public RatioMeasurementSpectraPlotManager(EventBus bus, final SymbolStyleMapFactory styleFactory, CertLeafletMapController mapImpl, MapPlottingUtilities iconFactory, SpectraClient spectraClient,
            SpectraRatioExporter spectraRatioExporter) {
        this.bus = bus;
        this.symbolStyleMapFactory = styleFactory;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
        this.symbolMap = new HashMap<>();
        this.spectraClient = spectraClient;
        this.spectraRatioExporter = spectraRatioExporter;
        this.ratioSpectralPlotController = new SpectraRatioPlotController(SpectraRatioPairOperator::getDiffAvg);

        plotFactory = new PlotlyPlotFactory();

        eventSelectionCallback = (selected, eventId) -> {
            log.debug(eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            log.debug(stationId);
        };
    }

    private double calcAvgRatioValsInBox(double x, double y, double xx, double yy) {
        List<Double> yValues = new ArrayList<>();
        if (symbolMap != null && symbolMap.size() > 0) {
            symbolMap.entrySet().forEach(entry -> {
                Point2D point = entry.getKey();
                if (point.getX() >= x && point.getX() <= xx && point.getY() >= y && point.getY() <= yy) {
                    yValues.add(point.getY());
                }
            });

            if (yValues.size() > 0) {
                return yValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            }
        }

        return y + (yy - y) / 2;
    }

    private void setLflHflLine(boolean isLFL, double x, double y, double xx, double yy) {
        double ratioVal = calcAvgRatioValsInBox(x, y, xx, yy);
        addUserSetFreqLine(ratioVal, isLFL);
        getRatioSpectraPlot().getSubplot().replot();
    }

    private PropertyChangeListener getPlotAreaObserver(final Supplier<Map<Point2D, SpectraRatioPairOperator>> ratioDetailsMap) {
        return evt -> {

            Object newValue = evt.getNewValue();

            if (newValue instanceof PlotFreqLevelChange) {
                PlotFreqLevelChange change = (PlotFreqLevelChange) newValue;

                setLflHflLine(change.isLflMode(), change.getX(), change.getY(), change.getXx(), change.getYy());

            } else if (newValue instanceof PlotObjectClick && ((PlotObjectClick) newValue).getPlotPoints() != null) {
                handlePlotObjectClicked((PlotObjectClick) newValue, point -> ratioDetailsMap.get().get(point));
                return;
            }

        };
    }

    @FXML
    public void initialize() {
        final Label label = new Label("\uE3B0");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        snapshotButton.setGraphic(label);
        snapshotButton.setContentDisplay(ContentDisplay.CENTER);

        Label mapLabel = new Label("\uE55B");
        mapLabel.getStyleClass().add("material-icons-medium");
        mapLabel.setMaxHeight(16);
        mapLabel.setMinWidth(16);
        showMapButton.setGraphic(mapLabel);

        final Label downloadLabel = new Label("\uE884");
        downloadLabel.getStyleClass().add("material-icons-medium");
        downloadLabel.setMaxHeight(16);
        downloadLabel.setMinWidth(16);
        downloadButton.setGraphic(downloadLabel);

        sortedEventPairList.setComparator((p1, p2) -> {
            int compare = 0;
            if (Objects.equals(p1, p2)) {
                compare = 0;
            } else if (p1 == null) {
                compare = -1;
            } else if (p2 == null) {
                compare = 1;
            } else {
                compare = StringUtils.compare(p1.getY().getEventId(), p2.getY().getEventId());
                if (compare == 0) {
                    compare = StringUtils.compare(p1.getX().getEventId(), p2.getX().getEventId());
                }
            }
            return compare;
        });

        eventPairComboBox.setItems(sortedEventPairList);

        eventPairComboBox.setCellFactory(lv -> new ListCell<EventPair>() {
            @Override
            protected void updateItem(EventPair pair, boolean empty) {
                super.updateItem(pair, empty);
                setText(pair == null ? null : pair.getY().getEventId() + " / " + pair.getX().getEventId());
            }
        });

        eventPairComboBox.setButtonCell(new ListCell<EventPair>() {
            @Override
            protected void updateItem(EventPair pair, boolean empty) {
                super.updateItem(pair, empty);
                setText(pair == null ? null : pair.getY().getEventId() + " / " + pair.getX().getEventId());
            }
        });

        eventPairComboBox.getSelectionModel().selectedItemProperty().addListener((obs, old, event) -> {
            if (event != null) {
                plotStationData(event);
            }
        });

        screenshotFolderChooser.setTitle("Spectra Plot Screenshot Export Folder");
        spectraRatioPlotNode.setOnKeyReleased(this::triggerKeyEvent);

        ratioSpectralPlotController.setShowCornerFrequencies(true);
        ratioSpectralPlotController.setYAxisResizable(true);
        ratioSpectralPlotController.setShouldShowFits(true);
        ratioSpectralPlotController.setShowFreqLevelButtons(true);

        // Map to store styles for each event pair
        symbolStyleMap = new HashMap<>();

        SpectralPlot plot = getRatioSpectraPlot();
        plot.setLabels("Seismic Envelope Ratio Spectra", X_AXIS_LABEL, Y_AXIS_LABEL);
        plot.getSubplot().addPlotObjectObserver(getPlotAreaObserver(ratioSpectralPlotController::getSpectraDataMap));
        plot.getSubplot().attachToDisplayNode(spectraRatioPlotNode);

        combinedContourPlots = plotFactory.basicPlot();
        combinedContourPlots.setSubplotLayout(2, 2);
        plotMap = new HashMap<>();
        plotDataMap = new HashMap<>();
        plotAxisMap = new HashMap<>();

        plotMap.put("Pair Stress", null);
        plotMap.put("Joint Stress", null);
        plotMap.put("Pair Moment", null);
        plotMap.put("Joint Moment", null);

        String[] plotNames = new String[4];
        plotNames[0] = "Pair Stress";
        plotNames[1] = "Joint Stress";
        plotNames[2] = "Pair Moment";
        plotNames[3] = "Joint Moment";

        Double colorBarLength = 0.47;

        List<Pair<Double, Double>> colorBarPos = new ArrayList<>();
        colorBarPos.add(new Pair<>(1.06, 0.55));
        colorBarPos.add(new Pair<>(1.21, 0.55));
        colorBarPos.add(new Pair<>(1.06, 0.1));
        colorBarPos.add(new Pair<>(1.21, 0.1));

        int idx = 0;
        for (String plotName : plotNames) {
            Map<String, PlotObjectData> plotDataObjectMap = new HashMap<>();
            plotDataObjectMap.put("contour", new PlotObjectData(new PlotTrace(PlotTrace.Style.CONTOUR)));
            plotDataObjectMap.put("point", new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER)));
            plotDataObjectMap.put("best", new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER)));

            // Add title to color bars for contour plot
            PlotTrace colorBar = plotDataObjectMap.get("contour").getTraceStyle();
            String plotNum = "<br />Plot " + (idx + 1);

            // Set color bar lengths
            colorBar.setColorBarLength(colorBarLength);

            if (idx == 1 || idx == 3) {
                colorBar.setColorBarTitle("&nbsp;" + plotNum);
            }
            // Add units label to color bar if it's on the left (1 or 3)
            if (idx == 0 || idx == 2) {
                colorBar.setColorBarTitle("<b>Mean Abs Dev Amp</b>" + plotNum);
            }

            plotDataMap.put(plotName, plotDataObjectMap);

            Pair<Axis, Axis> plotAxes = new Pair<>();
            if (idx < 2) {
                plotAxes.setX(new PlotlyPlotFactory().axis(Type.LOG_X, ""));
                plotAxes.setY(new PlotlyPlotFactory().axis(Type.LOG_Y, ""));
            } else {
                plotAxes.setX(new PlotlyPlotFactory().axis(Type.X, ""));
                plotAxes.setY(new PlotlyPlotFactory().axis(Type.Y, ""));
            }

            plotAxisMap.put(plotName, plotAxes);

            BasicPlot subPlot = createSubplotWithData(plotName, colorBarPos.get(idx), Color.ALICEBLUE, Color.BLACK);

            plotMap.put(plotName, subPlot);
            idx += 1;
        }

        combinedContourPlots.attachToDisplayNode(inversionPlotsNode);

        ratioSummaryTable.setItems(ratioSummaryValues);

        CellBindingUtils.attachTextCellFactoriesString(ratioSummaryNameCol, Pair::getX);
        CellBindingUtils.attachTextCellFactoriesString(ratioSummaryValueCol, Pair::getY);
    }

    private BasicPlot createSubplotWithData(String seriesName, Pair<Double, Double> colorBarPos, Color fillColor, Color edgeColor) {
        BasicPlot subPlot = combinedContourPlots.createSubPlot();
        subPlot.setColorMap(CONTOUR_COLOR_MAP);
        Map<String, PlotObjectData> subPlotData = subPlot.getPlotTypes();

        PlotObjectData contourPlotData = plotDataMap.get(seriesName).get("contour");
        PlotObjectData pointPlotData = plotDataMap.get(seriesName).get("point");
        PlotObjectData bestPlotData = plotDataMap.get(seriesName).get("best");

        contourPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        contourPlotData.getTraceStyle().setColorBarX(colorBarPos.getX());
        contourPlotData.getTraceStyle().setColorBarY(colorBarPos.getY());

        bestPlotData.getTraceStyle().setSeriesName("Best " + seriesName);
        bestPlotData.getTraceStyle().setFillColor(fillColor);
        bestPlotData.getTraceStyle().setEdgeColor(edgeColor);
        bestPlotData.getTraceStyle().setColorBarY(colorBarPos.getY());
        bestPlotData.getTraceStyle().setzIndex(2);
        bestPlotData.getTraceStyle().setPxSize(10);
        bestPlotData.getTraceStyle().setStyleName(SymbolStyles.STAR.getStyleName());

        pointPlotData.getTraceStyle().setSeriesName(seriesName);
        pointPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        pointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        pointPlotData.getTraceStyle().setzIndex(1);
        pointPlotData.getTraceStyle().setShowLegend(false);

        subPlotData.put(contourPlotData.getTraceStyle().getType().getType(), contourPlotData);
        subPlotData.put(pointPlotData.getTraceStyle().getSeriesName(), pointPlotData);
        subPlotData.put(bestPlotData.getTraceStyle().getSeriesName(), bestPlotData);

        Axis xAxis = plotAxisMap.get(seriesName).getX();
        Axis yAxis = plotAxisMap.get(seriesName).getY();
        xAxis.setTickFormatString(".2f");
        yAxis.setTickFormatString(".2f");

        subPlot.addAxes(xAxis);
        subPlot.addAxes(yAxis);

        return subPlot;
    }

    @FXML
    private void screenshotPlots(final ActionEvent e) {
        Button btn = (Button) e.getSource();
        Parent pane = btn.getParent();
        final File folder = screenshotFolderChooser.showDialog(pane.getScene().getWindow());
        try {
            if (folder != null && folder.exists() && folder.isDirectory() && folder.canWrite()) {
                screenshotFolderChooser.setInitialDirectory(folder);
                Platform.runLater(() -> exportScreenshots(folder));
            }
        } catch (final SecurityException ex) {
            log.warn("Exception trying to write screenshots to folder {} : {}", folder, ex.getLocalizedMessage(), ex);
        }
    }

    @FXML
    public void viewSpectraPopup(final ActionEvent e) {
        try {
            runGuiUpdate(() -> {
                createSpectraPlotPopup();
            });
        } catch (InvocationTargetException e1) {
            e1.printStackTrace();
        } catch (InterruptedException e2) {
            e2.printStackTrace();
        }
    }

    @FXML
    public void showMapWindow(final ActionEvent e) {
        SpectraRatioPairDetails ratioDetails = getCurrentFirstRatio();
        if (mapImpl != null && ratioDetails != null && !ratioDetails.isLoadedFromJson()) {
            List<Waveform> listData = new ArrayList<>();
            listData.add(ratioDetails.getNumerWaveform());
            listData.add(ratioDetails.getDenomWaveform());
            List<Icon> stationIcons = new ArrayList<>();
            ratioMeasurementReport.getStationsForEventPair(getEventPair()).forEach(station -> {
                stationIcons.add(iconFactory.createStationIcon(station).setIconSelectionCallback(stationSelectionCallback));
            });

            mapImpl.clearIcons();
            mapImpl.addIcons(stationIcons);
            mapImpl.addIcons(iconFactory.genIconsFromWaveforms(eventSelectionCallback, null, listData));

            Platform.runLater(() -> {
                mapImpl.show();
                mapImpl.fitViewToActiveShapes();
            });
        }
    }

    @FXML
    public void downloadPlots(final ActionEvent e) {
        //Save all parameters to an archive file and prompt the user about where to save it.
        File selectedFile = FileDialogs.openFileSaveDialog("Spectra_Ratio_Data", ".zip", borderPane.getScene().getWindow());

        if ((selectedFile != null) && FileDialogs.ensureFileIsWritable(selectedFile)) {
            Platform.runLater(() -> {
                try {
                    File exportArchive;
                    Path tmpFolder = Files.createTempDirectory(Long.toString(System.currentTimeMillis()));
                    tmpFolder.toFile().deleteOnExit();

                    exportScreenshots(tmpFolder.toFile());
                    exportReportData(tmpFolder.toFile());

                    exportArchive = spectraRatioExporter.createExportArchive(ratioMeasurementReport, getEventPair(), tmpFolder);

                    if (exportArchive != null) {
                        Files.move(exportArchive.toPath(), selectedFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException ex) {
                    FileDialogs.fileIoErrorAlert(ex);
                }
            });

        }
    }

    private void plotStationData(EventPair eventPair) {
        List<SpectraRatioPairDetails> ratiosByEventPair = ratioMeasurementReport.getRatiosList(getEventPair());

        // Create new spectra style map if none exists for the specified event pair
        if (symbolStyleMap.get(eventPair) == null) {
            Map<String, PlotPoint> symbolMapStyle = symbolStyleMapFactory.build(new ArrayList<>(ratioMeasurementReport.getReport().getData().get(eventPair).keySet()), Station::getStationName);
            symbolStyleMap.put(eventPair, symbolMapStyle);
        }

        spectraRatioPairOperatorList = ratiosByEventPair.stream().map(SpectraRatioPairOperator::new).collect(Collectors.toList());

        updateSpectraSymbolMap();

        ratioSpectralPlotController.getSpectraDataMap().clear();
        ratioSpectralPlotController.getSpectraDataMap().putAll(symbolMap);

        updateSpectraPlot(eventPair);
    }

    private void plotInversionPairData(Map<EventPair, SpectraRatioPairInversionResult> inversionData, EventPair eventPair) {
        String labelPrefix = "Pair";
        String stressDataKey = labelPrefix + " Stress";
        String momentDataKey = labelPrefix + " Moment";
        plotInversionData(
                labelPrefix,
                    inversionData,
                    eventPair,
                    plotMap.get(stressDataKey),
                    plotMap.get(momentDataKey),
                    plotDataMap.get(stressDataKey).get("contour"),
                    plotDataMap.get(stressDataKey).get("point"),
                    plotDataMap.get(stressDataKey).get("best"),
                    plotDataMap.get(momentDataKey).get("contour"),
                    plotDataMap.get(momentDataKey).get("point"),
                    plotDataMap.get(momentDataKey).get("best"),
                    plotAxisMap.get(stressDataKey).getX(),
                    plotAxisMap.get(stressDataKey).getY(),
                    plotAxisMap.get(momentDataKey).getX(),
                    plotAxisMap.get(momentDataKey).getY(),
                    PAIR_COLOR);
    }

    private SpectraRatioPairInversionResult mapJointInversionResultToInversionResult(SpectraRatioPairInversionResultJoint eventRecord) {
        return new SpectraRatioPairInversionResult().setEventIdA(eventRecord.getEventIdA())
                                                    .setEventIdB(eventRecord.getEventIdB())
                                                    .setMomentEstimateA(eventRecord.getMomentEstimateA())
                                                    .setMomentEstimateB(eventRecord.getMomentEstimateB())
                                                    .setCornerEstimateA1Max(eventRecord.getCornerEstimateA1Max())
                                                    .setCornerEstimateA1Min(eventRecord.getCornerEstimateA1Min())
                                                    .setCornerEstimateB1Max(eventRecord.getCornerEstimateB1Max())
                                                    .setCornerEstimateB1Min(eventRecord.getCornerEstimateB1Min())
                                                    .setCornerEstimateA2Max(eventRecord.getCornerEstimateA2Max())
                                                    .setCornerEstimateA2Min(eventRecord.getCornerEstimateA2Min())
                                                    .setCornerEstimateB2Max(eventRecord.getCornerEstimateB2Max())
                                                    .setCornerEstimateB2Min(eventRecord.getCornerEstimateB2Min())
                                                    .setApparentStressEstimateA(eventRecord.getApparentStressEstimateA())
                                                    .setApparentStressEstimateB(eventRecord.getApparentStressEstimateB())
                                                    .setCornerEstimateA(eventRecord.getCornerEstimateA())
                                                    .setCornerEstimateB(eventRecord.getCornerEstimateB())
                                                    .setId(eventRecord.getId())
                                                    .setMisfit(eventRecord.getMisfit())
                                                    .setM0minX(eventRecord.getM0minX())
                                                    .setM0maxX(eventRecord.getM0maxX())
                                                    .setM0minY(eventRecord.getM0minY())
                                                    .setM0maxY(eventRecord.getM0maxY())
                                                    .setAppStressMin(eventRecord.getAppStressMin())
                                                    .setAppStressMax(eventRecord.getAppStressMax())
                                                    .setM0XIndex(eventRecord.getM0XIndex())
                                                    .setM0Xdim(eventRecord.getM0Xdim())
                                                    .setM0YIndex(eventRecord.getM0YIndex())
                                                    .setM0Ydim(eventRecord.getM0Ydim())
                                                    .setM0samples(eventRecord.getM0samples())
                                                    .setStressXIndex(eventRecord.getStressXIndex())
                                                    .setAppStressXdim(eventRecord.getAppStressXdim())
                                                    .setStressYIndex(eventRecord.getStressYIndex())
                                                    .setAppStressYdim(eventRecord.getAppStressYdim())
                                                    .setStressSamples(eventRecord.getStressSamples());
    }

    private void plotJointInversionData(Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionData, EventPair eventPair) {
        SpectraRatioPairInversionResultJoint eventRecord = jointInversionData.get(eventPair);
        if (eventRecord != null) {
            Map<EventPair, SpectraRatioPairInversionResult> wrappedInversionData = new HashMap<>(1);
            wrappedInversionData.put(eventPair, mapJointInversionResultToInversionResult(eventRecord));
            String labelPrefix = "Joint";
            String stressDataKey = labelPrefix + " Stress";
            String momentDataKey = labelPrefix + " Moment";
            plotInversionData(
                    labelPrefix,
                        wrappedInversionData,
                        eventPair,
                        plotMap.get(stressDataKey),
                        plotMap.get(momentDataKey),
                        plotDataMap.get(stressDataKey).get("contour"),
                        plotDataMap.get(stressDataKey).get("point"),
                        plotDataMap.get(stressDataKey).get("best"),
                        plotDataMap.get(momentDataKey).get("contour"),
                        plotDataMap.get(momentDataKey).get("point"),
                        plotDataMap.get(momentDataKey).get("best"),
                        plotAxisMap.get(stressDataKey).getX(),
                        plotAxisMap.get(stressDataKey).getY(),
                        plotAxisMap.get(momentDataKey).getX(),
                        plotAxisMap.get(momentDataKey).getY(),
                        JOINT_COLOR);
        }
    }

    private void addEventDataToTable(String dataHeader, EventPair eventPair) {
        Double eventDistance = getEventPairDistanceKm();
        Double hypocentralEventDistance = getHypocentralEventPairDistanceKm();
        Double eventDepth = getEventPairDepthKm();

        ratioSummaryValues.add(new Pair<>(dataHeader, ""));
        ratioSummaryValues.add(new Pair<>("Numerator Event [A]", eventPair.getY().getEventId()));
        ratioSummaryValues.add(new Pair<>("Denominator Event [B]", eventPair.getX().getEventId()));

        if (eventDistance != null) {
            ratioSummaryValues.add(new Pair<>("Distance (Km)", dfmt2.format(eventDistance.doubleValue())));
        }
        if (hypocentralEventDistance != null) {
            ratioSummaryValues.add(new Pair<>("Hypocentral Dist (Km)", dfmt2.format(hypocentralEventDistance.doubleValue())));
        }
        if (eventDepth != null) {
            ratioSummaryValues.add(new Pair<>("Depth Diff (Km)", dfmt2.format(eventDepth.doubleValue())));
        }
    }

    private void addInversionDataToTable(SpectraRatioPairInversionResult data, String dataHeader) {
        ratioSummaryValues.add(new Pair<>(dataHeader, ""));
        ratioSummaryValues.add(new Pair<>("App Stress [A] (MPa)", dfmt4.format(data.getApparentStressEstimateA())));
        ratioSummaryValues.add(new Pair<>("App Stress [B] (MPa)", dfmt4.format(data.getApparentStressEstimateB())));

        ratioSummaryValues.add(new Pair<>("log10M0 / Mw [A]", String.format("%s / %s", dfmt2.format(data.getMomentEstimateA()), dfmt2.format((data.getMomentEstimateA() - 9.1) / 1.5))));
        ratioSummaryValues.add(new Pair<>("log10M0 / Mw [B]", String.format("%s / %s", dfmt2.format(data.getMomentEstimateB()), dfmt2.format((data.getMomentEstimateB() - 9.1) / 1.5))));
        ratioSummaryValues.add(new Pair<>("Err", dfmt4.format(data.getMisfit())));
    }

    private Double calculateAppStress() {
        EventPair events = getEventPair();
        Pair<Double, Double> userSetFreqs = userSetFreqDataMap.get(events);
        Double lfl = userSetFreqs.getX();
        Double hfl = userSetFreqs.getY();

        if (lfl != null && hfl != null) {
            return Math.pow(hfl, 3.0 / 2.0) / Math.pow(lfl, 1.0 / 2.0);
        }

        return null;
    }

    private void updateUserSetFreqLevels(Double freqLevel, boolean isLFL) {
        EventPair events = getEventPair();
        Pair<Double, Double> userSetFreqs = userSetFreqDataMap.get(events);

        int idx = userSetTablePosIdx + LFL_IDX_OFFSET;
        if (userSetFreqs == null) {
            userSetFreqs = new Pair<>();
        }

        if (isLFL) {
            userSetFreqs.setX(freqLevel);
        } else {
            idx = userSetTablePosIdx + HFL_IDX_OFFSET;
            userSetFreqs.setY(freqLevel);
        }

        userSetFreqDataMap.put(events, userSetFreqs);

        Pair<String, String> tableRowData = ratioSummaryValues.get(idx);
        tableRowData.setY(dfmt4.format(freqLevel));

        Double appStress = calculateAppStress();
        if (appStress != null) {
            // log.info(appStress.toString());
            idx = userSetTablePosIdx + CALC_STRESS_IDX_OFFSET;
            ratioSummaryValues.get(idx).setY(dfmt4.format(appStress));
            ratioMeasurementReport.getReport().setUserSetStressResult(appStress);
        }

        ratioMeasurementReport.getReport().setUserAdjustedLowAndHighFreqLevels(userSetFreqDataMap);
        ratioSummaryTable.refresh();
    }

    private void addUserSetFreqLine(Double ratioVal, boolean isLFL) {
        updateUserSetFreqLevels(ratioVal, isLFL);
        if (isLFL) {
            if (userSetLFL != null) {
                updateFrequencyLine(userSetLFL, "User", false, ratioVal);
            } else {
                userSetLFL = createFrequencyLine("User", false, Color.PURPLE.darker(), LineStyles.LONG_DASH_DOT, false, ratioVal, true);
            }
        } else if (userSetHFL != null) {
            updateFrequencyLine(userSetHFL, "User", true, ratioVal);
        } else {
            userSetHFL = createFrequencyLine("User", true, Color.PURPLE.brighter(), LineStyles.LONG_DASH_DOT, false, ratioVal, true);
        }
    }

    private void addUserSetLFLandHFL(EventPair eventPair) {

        userSetHFL = null;
        userSetLFL = null;

        userSetTablePosIdx = ratioSummaryValues.size();

        ratioSummaryValues.add(new Pair<>("User Set Freq Levels", ""));
        ratioSummaryValues.add(new Pair<>("Low Freq Lvl", "Not Set"));
        ratioSummaryValues.add(new Pair<>("High Freq Lvl", "Not Set"));
        ratioSummaryValues.add(new Pair<>("User A/B App Stress", "N/A"));

        Pair<Double, Double> userFreqLevelLines = userSetFreqDataMap.get(eventPair);
        if (userFreqLevelLines != null) {
            addUserSetFreqLine(userFreqLevelLines.getX(), true);
            addUserSetFreqLine(userFreqLevelLines.getY(), false);
        }
    }

    private void addJointInversionDataToTable(SpectraRatioPairInversionResultJoint jointData, String dataHeader) {
        SpectraRatioPairInversionResult data = mapJointInversionResultToInversionResult(jointData);
        addInversionDataToTable(data, dataHeader);
    }

    private void plotInversionData(String dataLabelPrefix, Map<EventPair, SpectraRatioPairInversionResult> inversionData, EventPair eventPair, BasicPlot stressContourPlot, BasicPlot momentContourPlot,
            PlotObjectData stressContourPlotData, PlotObjectData stressPointPlotData, PlotObjectData bestStressPointPlotData, PlotObjectData momentContourPlotData, PlotObjectData momentPointPlotData,
            PlotObjectData bestMomentPointPlotData, Axis stressXaxis, Axis stressYaxis, Axis momentXaxis, Axis momentYaxis, Color ratioShapeColor) {
        if (eventPair != null) {
            try {
                //Break the cache
                stressContourPlot.clear();
                momentContourPlot.clear();
                //FIXME: Revamp this to not be editing the internal plot data but instead use the
                // standard add/remove plot objects

                stressContourPlotData.clear();
                stressPointPlotData.clear();
                bestStressPointPlotData.clear();

                momentContourPlotData.clear();
                momentPointPlotData.clear();
                bestMomentPointPlotData.clear();

                SpectraRatioPairInversionResult bestFit = inversionData.get(eventPair);

                bestStressPointPlotData.getXdata().add(Double.valueOf(bestFit.getApparentStressEstimateB()));
                bestStressPointPlotData.getYdata().add(Double.valueOf(bestFit.getApparentStressEstimateA()));
                bestMomentPointPlotData.getXdata().add(Double.valueOf(bestFit.getMomentEstimateB()));
                bestMomentPointPlotData.getYdata().add(Double.valueOf(bestFit.getMomentEstimateA()));

                // Calculate min and max ratio values to set the position of the corner frequency error bars
                List<Double> avgRatioPoints = toPlotPoints(eventPair, SpectraRatioPairOperator::getDiffAvg, null).stream().map(PlotPoint::getY).collect(Collectors.toList());
                Double minY = Collections.min(avgRatioPoints);
                Double maxY = Collections.max(avgRatioPoints);

                // Make Pair inversion corner frequency lines smaller than joint
                double offset = (maxY - minY) * 0.1;

                // Adjust subplot margins, spacing and text
                Integer plotTopMargin = 30;
                Integer plotBottomMargin = 0;
                Integer plotLeftMargin = 50;
                Integer plotRightMargin = 110;
                Integer plotHorizontalSpacing = 30;
                Integer plotVerticalSpacing = 80;
                if (dataLabelPrefix.equals("Pair")) {
                    minY += offset;
                    maxY -= offset;

                    stressContourPlot.setMargin(plotTopMargin, plotHorizontalSpacing, plotLeftMargin, plotVerticalSpacing);
                    momentContourPlot.setMargin(plotHorizontalSpacing, plotBottomMargin, plotLeftMargin, plotVerticalSpacing);

                    stressXaxis.setText(dataLabelPrefix + " Stress (MPa) Plot 1");
                    stressYaxis.setText("<b><i>Event " + eventPair.getY().getEventId() + "</i></b>");

                    momentXaxis.setText(dataLabelPrefix + " Moment (Log10M0) Plot 3<br /><b><i>Event " + eventPair.getX().getEventId() + "</i></b>");
                    momentYaxis.setText("<b><i>Event " + eventPair.getY().getEventId() + "</i></b>");
                } else {
                    stressXaxis.setText(dataLabelPrefix + " Stress (MPa) Plot 2");
                    momentXaxis.setText(dataLabelPrefix + " Moment (Log10M0) Plot 4<br /><b><i>Event " + eventPair.getX().getEventId() + "</i></b>");

                    stressContourPlot.setMargin(plotTopMargin, plotHorizontalSpacing, plotVerticalSpacing, plotRightMargin);
                    momentContourPlot.setMargin(plotHorizontalSpacing, plotBottomMargin, plotVerticalSpacing, plotRightMargin);
                }

                double errorA = bestFit.getCornerEstimateA1Max();
                double errorB = bestFit.getCornerEstimateB1Max();
                double errorMinusA = bestFit.getCornerEstimateA1Min();
                double errorMinusB = bestFit.getCornerEstimateB1Min();

                setCornerFrequencyLines(bestFit.getCornerEstimateA(), bestFit.getCornerEstimateB(), errorA, errorB, errorMinusA, errorMinusB, ratioShapeColor, maxY, minY);

                Line fitRatioShape = plotMomentRatioShape(
                        spectraClient.getSpecificSpectra(bestFit.getMomentEstimateA(), bestFit.getApparentStressEstimateA(), 0.001, 30.0, 100).block(Duration.ofMinutes(10l)),
                            spectraClient.getSpecificSpectra(bestFit.getMomentEstimateB(), bestFit.getApparentStressEstimateB(), 0.001, 30.0, 100).block(Duration.ofMinutes(10l)),
                            Color.BLUE,
                            LineStyles.DASH);
                fitRatioShape.setName(dataLabelPrefix + " CERT Mw ratio");
                fitRatioShape.setStyle(LineStyles.SOLID);
                fitRatioShape.setFillColor(ratioShapeColor);

                getRatioSpectraPlot().getSubplot().addPlotObject(fitRatioShape);

                double minStressX = bestFit.getAppStressMin();
                double minMomentX = bestFit.getM0minX();
                double maxStressX = bestFit.getAppStressMax();
                double maxMomentX = bestFit.getM0maxX();

                double minStressY = minStressX;
                double minMomentY = bestFit.getM0minY();
                double maxStressY = maxStressX;
                double maxMomentY = bestFit.getM0maxY();

                List<Double[]> stressMisfits = new ArrayList<>();
                List<Double[]> momentMisfits = new ArrayList<>();

                double logMinStressX = Math.log10(minStressX);
                double logMaxStressX = Math.log10(maxStressX);

                double logMinStressY = Math.log10(minStressY);
                double logMaxStressY = Math.log10(maxStressY);

                for (int i = 0; i < bestFit.getM0Xdim(); i++) {
                    double m0x = minMomentX + ((float) i / (float) (bestFit.getM0Xdim() - 1)) * (bestFit.getM0maxX() - bestFit.getM0minX());
                    momentContourPlotData.getXdata().add(m0x);
                }
                for (int i = 0; i < bestFit.getM0Ydim(); i++) {
                    double m0y = minMomentY + ((float) i / (float) (bestFit.getM0Ydim() - 1)) * (bestFit.getM0maxY() - bestFit.getM0minY());
                    momentContourPlotData.getYdata().add(m0y);
                    momentMisfits.add(new Double[bestFit.getM0Xdim()]);
                }

                for (int i = 0; i < bestFit.getM0data().size(); i++) {
                    //Grab the data and push it into the dense plotting array
                    momentMisfits.get(bestFit.getM0YIdx().get(i))[bestFit.getM0XIdx().get(i)] = (double) bestFit.getM0data().get(i);

                    double misfit = bestFit.getM0data().get(i);
                    float xCoord = bestFit.getM0minX() + ((float) bestFit.getM0XIdx().get(i) / (float) (bestFit.getM0Xdim() - 1)) * (bestFit.getM0maxX() - bestFit.getM0minX());
                    float yCoord = bestFit.getM0minY() + ((float) bestFit.getM0YIdx().get(i) / (float) (bestFit.getM0Ydim() - 1)) * (bestFit.getM0maxY() - bestFit.getM0minY());
                    momentPointPlotData.getTextData().add(dfmt4.format(misfit));
                    momentPointPlotData.getColorData().add(Double.valueOf(misfit));
                    momentPointPlotData.getXdata().add(Double.valueOf(xCoord));
                    momentPointPlotData.getYdata().add(Double.valueOf(yCoord));
                }
                momentPointPlotData.getTraceStyle().setLegendOnly(true);

                for (int i = 0; i < bestFit.getAppStressXdim(); i++) {
                    double stressX = logMinStressX + ((float) i / (float) (bestFit.getAppStressXdim() - 1)) * (logMaxStressX - logMinStressX);
                    stressContourPlotData.getXdata().add(Math.pow(10, stressX));
                }
                for (int i = 0; i < bestFit.getAppStressYdim(); i++) {
                    double stressY = logMinStressY + ((float) i / (float) (bestFit.getAppStressYdim() - 1)) * (logMaxStressY - logMinStressY);
                    stressContourPlotData.getYdata().add(Math.pow(10, stressY));
                    stressMisfits.add(new Double[bestFit.getAppStressXdim()]);
                }

                for (int i = 0; i < bestFit.getStressData().size(); i++) {
                    stressMisfits.get(bestFit.getStressYIdx().get(i))[bestFit.getStressXIdx().get(i)] = (double) bestFit.getStressData().get(i);

                    double misfit = bestFit.getStressData().get(i);
                    Double xCoord = logMinStressX + ((float) bestFit.getStressXIdx().get(i) / (float) (bestFit.getAppStressXdim() - 1)) * (logMaxStressX - logMinStressX);
                    Double yCoord = logMinStressY + ((float) bestFit.getStressYIdx().get(i) / (float) (bestFit.getAppStressYdim() - 1)) * (logMaxStressY - logMinStressY);
                    stressPointPlotData.getTextData().add(dfmt4.format(misfit));
                    stressPointPlotData.getColorData().add(Double.valueOf(misfit));
                    stressPointPlotData.getXdata().add(Math.pow(10, xCoord));
                    stressPointPlotData.getYdata().add(Math.pow(10, yCoord));
                }
                stressPointPlotData.getTraceStyle().setLegendOnly(true);

                stressXaxis.setMax(logMaxStressX);
                stressYaxis.setMax(logMaxStressY);
                stressXaxis.setMin(logMinStressX);
                stressYaxis.setMin(logMinStressY);

                momentXaxis.setMax(maxMomentX);
                momentYaxis.setMax(maxMomentY);
                momentXaxis.setMin(minMomentX);
                momentYaxis.setMin(minMomentY);

                stressContourPlotData.getZdata().addAll(stressMisfits);
                momentContourPlotData.getZdata().addAll(momentMisfits);

                Map<String, PlotObjectData> plotData = stressContourPlot.getPlotTypes();
                plotData.put(stressContourPlotData.getTraceStyle().getType().getType(), stressContourPlotData);
                plotData.put(stressPointPlotData.getTraceStyle().getSeriesName(), stressPointPlotData);
                plotData.put(bestStressPointPlotData.getTraceStyle().getSeriesName(), bestStressPointPlotData);

                stressContourPlot.replot();

                plotData = momentContourPlot.getPlotTypes();
                plotData.put(momentContourPlotData.getTraceStyle().getType().getType(), momentContourPlotData);
                plotData.put(momentPointPlotData.getTraceStyle().getSeriesName(), momentPointPlotData);
                plotData.put(bestMomentPointPlotData.getTraceStyle().getSeriesName(), bestMomentPointPlotData);

                momentContourPlot.replot();

                combinedContourPlots.replot();
            } catch (Exception e) {
                log.error(e.getLocalizedMessage(), e);
            }
        }
    }

    public void setRatioMeasurements(SpectraRatiosReportByEventPair ratiosByEventPair) {
        Platform.runLater(() -> {
            ratioMeasurementReport = ratiosByEventPair;
            eventPairList.clear();
            eventPairList.addAll(ratioMeasurementReport.getEventPairs());
            eventPairComboBox.getSelectionModel().clearSelection();
            if (!eventPairList.isEmpty()) {
                eventPairComboBox.getSelectionModel().select(0);
            }
        });
    }

    public Map<Point2D, SpectraRatioPairOperator> mapFunctionToPoint(final List<SpectraRatioPairOperator> ratioDetails, final Function<SpectraRatioPairOperator, Double> func) {
        return ratioDetails.stream()
                           .collect(
                                   Collectors.toMap(
                                           //Dyne-cm to nm for plot, in log
                                           ratio -> new Point2D(centerFreq(ratio.getFrequency().getLowFrequency(), ratio.getFrequency().getHighFrequency()), func.apply(ratio)),
                                               Function.identity(),
                                               (a, b) -> b,
                                               HashMap::new));
    }

    private List<PlotPoint> toPlotPoints(final EventPair eventPair, final Function<SpectraRatioPairOperator, Double> func, final Color color) {
        List<PlotPoint> allPlotPoints = new ArrayList<>();

        Map<String, PlotPoint> symbolStyles = symbolStyleMap.get(eventPair);
        spectraRatioPairOperatorList.forEach(ratioDetails -> {
            FrequencyBand freqValue = ratioDetails.getFrequency();
            PlotPoint pp = symbolStyles.get(ratioDetails.getDenomWaveform().getStream().getStation().getStationName());
            if (pp != null) {
                pp.setX(centerFreq(freqValue.getLowFrequency(), freqValue.getHighFrequency()));
                pp.setY(func.apply(ratioDetails));

                if (color != null) {
                    pp.setColor(color);
                }

                final LabeledPlotPoint point = new LabeledPlotPoint(ratioDetails.getStation().getStationName(), pp);
                allPlotPoints.add(point);
            }
        });

        return allPlotPoints;
    }

    private Map<Point2D, Waveform> getEventPairWaveformMap(final EventPair eventPair) {
        Map<Point2D, Waveform> waveformMap = new HashMap<>();
        spectraRatioPairOperatorList.forEach(ratioDetails -> {

            FrequencyBand freqValue = ratioDetails.getFrequency();
            Double centerFreq = centerFreq(freqValue.getLowFrequency(), freqValue.getHighFrequency());
            Double numerAvg = ratioDetails.getNumerAvg();
            Double denomAvg = ratioDetails.getDenomAvg();

            Point2D numerPoint = new Point2D(centerFreq, numerAvg);
            Point2D denomPoint = new Point2D(centerFreq, denomAvg);

            waveformMap.put(numerPoint, ratioDetails.getNumerWaveform());
            waveformMap.put(denomPoint, ratioDetails.getDenomWaveform());
        });

        return waveformMap;
    }

    private void updateInversionReferenceLines(SpectraRatioPairInversionResult pairInversionData, SpectraRatioPairInversionResultJoint jointInversionData) {

        if (ratioMeasurementReport != null || getEventPair() != null) {

            Set<Point2D> plotPoints = getRatioSpectraPlot().getSymbolMap().keySet();
            double minFreq = Double.POSITIVE_INFINITY;
            double maxFreq = Double.NEGATIVE_INFINITY;
            int count = 7;

            for (Point2D point : plotPoints) {
                if (point.getX() < minFreq) {
                    minFreq = point.getX();
                }
                if (point.getX() > maxFreq) {
                    maxFreq = point.getX();
                }
            }

            Spectra pairEventSpectraA = spectraClient.getSpecificSpectra(pairInversionData.getMomentEstimateA(), pairInversionData.getApparentStressEstimateA(), minFreq, maxFreq, count).block();
            Spectra pairEventSpectraB = spectraClient.getSpecificSpectra(pairInversionData.getMomentEstimateB(), pairInversionData.getApparentStressEstimateB(), minFreq, maxFreq, count).block();

            Spectra jointEventSpectraA = spectraClient.getSpecificSpectra(jointInversionData.getMomentEstimateA(), jointInversionData.getApparentStressEstimateA(), minFreq, maxFreq, count).block();
            Spectra jointEventSpectraB = spectraClient.getSpecificSpectra(jointInversionData.getMomentEstimateB(), jointInversionData.getApparentStressEstimateB(), minFreq, maxFreq, count).block();

            if (pairEventSpectraA != null && pairEventSpectraB != null) {
                double pairMwA = pairEventSpectraA.getMw();
                double pairMwB = pairEventSpectraB.getMw();

                if (pairMwA != 0.0 && pairMwB != 0.0) {
                    double pairMomentRatio = 1.5 * (pairMwA - pairMwB);

                    Double refRatio = pairMomentRatio / 3.0;
                    Line refRatioShape = plotMomentRatioShape(pairEventSpectraA, pairEventSpectraB, PAIR_COLOR, LineStyles.DASH);
                    refRatioShape.setName("Pair Ratio Line");

                    getRatioSpectraPlot().getSubplot().addPlotObject(refRatioShape);

                    createFrequencyLine("Pair", false, PAIR_COLOR, LineStyles.DASH, true, pairMomentRatio, false);
                    createFrequencyLine("Pair", true, PAIR_COLOR, LineStyles.DASH, true, refRatio, false);
                }
            }

            if (jointEventSpectraA != null && jointEventSpectraB != null) {
                double jointMwA = jointEventSpectraA.getMw();
                double jointMwB = jointEventSpectraB.getMw();

                if (jointMwA != 0.0 && jointMwB != 0.0) {
                    double jointMomentRatio = 1.5 * (jointMwA - jointMwB);

                    Double refRatio = jointMomentRatio / 3.0;
                    Line refRatioShape = plotMomentRatioShape(jointEventSpectraA, jointEventSpectraB, JOINT_COLOR, LineStyles.DASH);
                    refRatioShape.setName("Joint Ratio Line");

                    getRatioSpectraPlot().getSubplot().addPlotObject(refRatioShape);

                    createFrequencyLine("Joint", false, JOINT_COLOR, LineStyles.LONG_DASH, true, jointMomentRatio, false);
                    createFrequencyLine("Joint", true, JOINT_COLOR, LineStyles.LONG_DASH, true, refRatio, false);
                }
            }
        }
    }

    private void updateMomentRatioLines() {
        Double momentRatio = null;

        if (ratioMeasurementReport == null || getEventPair() == null) {
            momentRatio = null;
        } else {
            // Get event id's
            String largeId = getEventPair().getY().getEventId();
            String smallId = getEventPair().getX().getEventId();

            // Get the spectra for the event
            List<Spectra> largeSpectraSet = new ArrayList<>(spectraClient.getFitSpectra(largeId).block(Duration.ofSeconds(2)));
            List<Spectra> smallSpectraSet = new ArrayList<>(spectraClient.getFitSpectra(smallId).block(Duration.ofSeconds(2)));

            Spectra largeSpectra = null;
            Spectra smallSpectra = null;

            if (largeSpectraSet.isEmpty()) {
                largeSpectraSet = List.of(spectraClient.getReferenceSpectra(largeId).block(Duration.ofSeconds(2)));
            }

            if (smallSpectraSet.isEmpty()) {
                smallSpectraSet = List.of(spectraClient.getReferenceSpectra(smallId).block(Duration.ofSeconds(2)));
            }

            if (!largeSpectraSet.isEmpty() && !smallSpectraSet.isEmpty()) {
                double largeMw = 0.0;
                for (Spectra spectra : largeSpectraSet) {
                    if (spectra.getType() == SPECTRA_TYPES.FIT) {
                        largeMw = spectra.getMw();
                        largeSpectra = spectra;
                        break;
                    } else if (spectra.getType() == SPECTRA_TYPES.REF) {
                        largeMw = spectra.getMw();
                        largeSpectra = spectra;
                    }
                }

                double smallMw = 0.0;
                for (Spectra spectra : smallSpectraSet) {
                    if (spectra.getType() == SPECTRA_TYPES.FIT) {
                        smallMw = spectra.getMw();
                        smallSpectra = spectra;
                        break;
                    } else if (spectra.getType() == SPECTRA_TYPES.REF) {
                        smallMw = spectra.getMw();
                        smallSpectra = spectra;
                    }
                }

                if (largeMw != 0.0 && smallMw != 0.0) {
                    //Use logM0 ratio (scaled)
                    //TODO: Revisit on bottom line w.r.t. source scaling?
                    momentRatio = 1.5 * (largeMw - smallMw);

                    Double refRatio = momentRatio / 3.0;
                    Line refRatioShape = plotMomentRatioShape(largeSpectra, smallSpectra, Color.BLUE, LineStyles.DASH);
                    refRatioShape.setName("CCT Mw ratio");

                    getRatioSpectraPlot().getSubplot().addPlotObject(refRatioShape);

                    createFrequencyLine("CCT", false, Color.BLACK, LineStyles.DOT, true, momentRatio, false);
                    createFrequencyLine("CCT", true, Color.BLACK, LineStyles.DOT, true, refRatio, false);
                }
            }
        }
    }

    private Line plotMomentRatioShape(Spectra largeSpectra, Spectra smallSpectra, Color color, LineStyles style) {
        //TODO: Assumes sorted order and large.length == small.length
        if (largeSpectra != null && smallSpectra != null && largeSpectra.getSpectraXY().size() == smallSpectra.getSpectraXY().size()) {

            final double[] ratioDataX = new double[largeSpectra.getSpectraXY().size()];
            final double[] ratioDataY = new double[largeSpectra.getSpectraXY().size()];

            for (int i = 0; i < largeSpectra.getSpectraXY().size(); i++) {
                //Log10 freq, need it in raw
                ratioDataX[i] = Math.pow(10, largeSpectra.getSpectraXY().get(i).getX());
                //Log10 spectra
                ratioDataY[i] = largeSpectra.getSpectraXY().get(i).getY() - smallSpectra.getSpectraXY().get(i).getY();
            }

            Line ratioShape = plotFactory.line(ratioDataX, ratioDataY, color, style, 2);

            return ratioShape;
        }
        return null;
    }

    private void updateSpectraSymbolMap() {
        symbolMap.clear();
        symbolMap.putAll(mapFunctionToPoint(spectraRatioPairOperatorList, SpectraRatioPairOperator::getDiffAvg));
        curMinX = Math.min(0.0, symbolMap.entrySet().stream().mapToDouble(entry -> entry.getKey().getX()).min().getAsDouble());
        curMaxX = Math.max(30.0, symbolMap.entrySet().stream().mapToDouble(entry -> entry.getKey().getX()).max().getAsDouble());
    }

    private Line createFrequencyLine(String name, boolean isHFL, Color color, LineStyles style, boolean hideByDefault, double ratioValue, boolean draggable) {
        final float[] lineData = new float[2];
        lineData[0] = (float) ratioValue;
        lineData[1] = (float) ratioValue;

        String formatStr = "%s LFL: %s";
        if (isHFL) {
            formatStr = "%s HFL: %s";
        }

        Line freqLine = plotFactory.horizontalLine(curMinX, curMaxX, ratioValue, color, style, 2);
        freqLine.setName(String.format(formatStr, name, dfmt4.format(ratioValue)));
        freqLine.setLegendOnly(hideByDefault);
        freqLine.setDraggable(draggable);

        getRatioSpectraPlot().getSubplot().addPlotObject(freqLine);

        return freqLine;
    }

    private void updateFrequencyLine(Line freqLine, String name, boolean isLFL, double newValue) {

        if (freqLine == null) {
            return;
        }

        getRatioSpectraPlot().getSubplot().removePlotObject(freqLine);
        final double[] lineData = new double[2];
        lineData[0] = newValue;
        lineData[1] = newValue;

        String formatStr = "%s HFL: %s";
        if (isLFL) {
            formatStr = "%s LFL: %s";
        }
        freqLine.setName(String.format(formatStr, name, dfmt4.format(newValue)));
        freqLine.setY(lineData);
        getRatioSpectraPlot().getSubplot().addPlotObject(freqLine);
    }

    private void setCornerFrequencyLines(double cornerEstimateA, double cornerEstimateB, double errorA, double errorB, double errorMinusA, double errorMinusB, Color lineColor, Double yTop,
            Double yBottom) {
        getRatioSpectraPlot().plotCornerFrequency("~Fc Low", cornerEstimateA, errorA, errorMinusA, yTop, yBottom, lineColor);
        getRatioSpectraPlot().plotCornerFrequency("~Fc High", cornerEstimateB, errorB, errorMinusB, yTop, yBottom, lineColor);
    }

    protected void runGuiUpdate(final Runnable runnable) throws InvocationTargetException, InterruptedException {
        Platform.runLater(runnable);
    }

    protected double centerFreq(final Double lowFrequency, final Double highFrequency) {
        return lowFrequency + (highFrequency - lowFrequency) / 2.;
    }

    public EventPair getEventPair() {
        return eventPairComboBox.getSelectionModel().getSelectedItem();
    }

    public SpectralPlot getRatioSpectraPlot() {
        return ratioSpectralPlotController.getSpectralPlot();
    }

    public SpectraRatioPairDetails getCurrentFirstRatio() {
        if (ratioMeasurementReport != null) {
            List<SpectraRatioPairDetails> ratioReportList = ratioMeasurementReport.getRatiosList(getEventPair());
            if (!ratioReportList.isEmpty()) {
                return ratioReportList.get(0);
            }
        }
        return null;
    }

    public String getPlotIdentifier() {
        return String.format("large_event_%s_small_event_%s", getEventPair().getY().getEventId(), getEventPair().getX().getEventId());
    }

    // Gets the epicentral distance EModel WGS84 distance between both events
    public Double getEventPairDistanceKm() {
        SpectraRatioPairDetails ratioDetails = getCurrentFirstRatio();
        if (ratioDetails != null) {
            double numerLatitude = ratioDetails.getNumerWaveform().getEvent().getLatitude();
            double numerLongitude = ratioDetails.getNumerWaveform().getEvent().getLongitude();
            double denomLatitude = ratioDetails.getDenomWaveform().getEvent().getLatitude();
            double denomLongitude = ratioDetails.getDenomWaveform().getEvent().getLongitude();

            return EModel.getDistanceWGS84(numerLatitude, numerLongitude, denomLatitude, denomLongitude);
        }

        return null;
    }

    // Gets the hypocentral distance EModel WGS84 distance in Km between both events
    public Double getHypocentralEventPairDistanceKm() {
        SpectraRatioPairDetails ratioDetails = getCurrentFirstRatio();
        if (ratioDetails != null) {
            double numerLatitude = ratioDetails.getNumerWaveform().getEvent().getLatitude();
            double numerLongitude = ratioDetails.getNumerWaveform().getEvent().getLongitude();
            double denomLatitude = ratioDetails.getDenomWaveform().getEvent().getLatitude();
            double denomLongitude = ratioDetails.getDenomWaveform().getEvent().getLongitude();
            double numerDepth = ratioDetails.getNumerWaveform().getEvent().getDepth() / 1000.0;
            double denomDepth = ratioDetails.getDenomWaveform().getEvent().getDepth() / 1000.0;

            return EModel.getSeparationMeters(new GeodeticCoordinate(numerLatitude, numerLongitude, numerDepth), new GeodeticCoordinate(denomLatitude, denomLongitude, denomDepth)) / 1000.0;
        }

        return null;
    }

    // Gets the depth difference between event pair
    public Double getEventPairDepthKm() {
        SpectraRatioPairDetails ratioDetails = getCurrentFirstRatio();
        if (ratioDetails != null) {
            double numerDepth = ratioDetails.getNumerWaveform().getEvent().getDepth();
            double denomDepth = ratioDetails.getDenomWaveform().getEvent().getDepth();

            return Math.abs(numerDepth - denomDepth) / 1000.0;
        }

        return null;
    }

    public Result<Map<FrequencyBand, SpectraRatioPairDetails>> getRatioDetailsFromStation(Station station) {
        Map<FrequencyBand, SpectraRatioPairDetails> ratios = null;
        if (station != null) {
            Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> eventData = ratioMeasurementReport.getReport().getData().get(getEventPair());
            if (eventData != null && eventData.containsKey(station)) {
                ratios = eventData.get(station);
            }
        }

        return new Result<>(ratios != null, ratios);
    }

    public Result<SpectraRatioPairDetails> getRatioDetailsFromStationAndFreq(Station station, FrequencyBand freqBand) {
        SpectraRatioPairDetails ratio = null;
        if (station != null && freqBand != null) {
            Map<Station, Map<FrequencyBand, SpectraRatioPairDetails>> eventData = ratioMeasurementReport.getReport().getData().get(getEventPair());
            if (eventData != null && eventData.containsKey(station)) {
                ratio = eventData.get(station).get(freqBand);
            }
        }
        return new Result<>(ratio != null, ratio);
    }

    private void createRatioWaveformPlotPopup(SpectraRatioPairOperator ratioDetails) {

        RatioMeasurementWaveformPlotManager ratioWaveformPlots = new RatioMeasurementWaveformPlotManager(bus, mapImpl, iconFactory);
        ratioWaveformPlots.setParentSpectra(this);
        ratioWaveformPlots.setCurrentEvent(ratioMeasurementReport.getStationsForEventPair(getEventPair()));

        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/RatioWaveformGui.fxml"));
        fxmlLoader.setController(ratioWaveformPlots);
        final Stage stage = new Stage(StageStyle.DECORATED);
        Parent root;
        Scene scene;

        Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
        try {
            root = fxmlLoader.load();
            scene = new Scene(root);
            stage.setScene(scene);

            stage.setOnHiding(e -> {
                stage.hide();
            });

            stage.setOnShowing(e -> {
                final boolean showing = stage.isShowing();
                stage.show();
                if (!showing || Boolean.TRUE.equals(shouldFocus.getValue())) {
                    stage.toFront();
                }
            });

            ratioWaveformPlots.setCurrentFreqAndStation(ratioDetails.getFrequency().getLowFrequency(), ratioDetails.getFrequency().getHighFrequency(), ratioDetails.getStation());

            stage.show();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void createSpectraPlotPopup() {
        EventPair eventPair = getEventPair();
        Map<Point2D, Waveform> waveformMap = getEventPairWaveformMap(eventPair);
        List<PlotPoint> numerPoints = new ArrayList<>(toPlotPoints(eventPair, SpectraRatioPairOperator::getNumerAvg, Color.RED));
        List<PlotPoint> denomPoints = new ArrayList<>(toPlotPoints(eventPair, SpectraRatioPairOperator::getDenomAvg, Color.BLUE));

        SpectraPlotManager spectraPlotManager = new SpectraPlotManager(bus, getEventPair(), waveformMap, numerPoints, denomPoints);

        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpectraPlotGui.fxml"));
        fxmlLoader.setController(spectraPlotManager);
        final Stage stage = new Stage(StageStyle.DECORATED);
        Parent root;
        Scene scene;

        Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
        try {
            root = fxmlLoader.load();
            scene = new Scene(root);
            stage.setScene(scene);

            stage.setOnHiding(e -> {
                stage.hide();
            });

            stage.setOnShowing(e -> {
                final boolean showing = stage.isShowing();
                stage.show();
                if (!showing || Boolean.TRUE.equals(shouldFocus.getValue())) {
                    stage.toFront();
                }
            });

            stage.show();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private void handlePlotObjectClicked(final PlotObjectClick poc, final Function<Point2D, SpectraRatioPairOperator> measurementFunc) {

        Point2D point = poc.getPlotPoints().get(0);
        SpectraRatioPairOperator ratioDetails = measurementFunc.apply(point);

        if (ratioDetails.isLoadedFromJson()) {
            return;
        }

        if (poc.getMouseEvent().isPrimaryButtonDown() && ratioDetails != null) {
            try {
                runGuiUpdate(() -> {
                    createRatioWaveformPlotPopup(ratioDetails);
                });
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void triggerKeyEvent(final KeyEvent event) {
        log.trace("Key Pressed on Ratio Spectra Plot");
    }

    protected void updatePlotPoint(EventPair eventPair) {
        EventPair currentPair = getEventPair();

        updateSpectraSymbolMap();

        if (eventPair.getX().getEventId().equals(currentPair.getX().getEventId()) && eventPair.getY().getEventId().equals(currentPair.getY().getEventId())) {
            ratioSpectralPlotController.getSpectraDataMap().clear();
            ratioSpectralPlotController.getSpectraDataMap().putAll(symbolMap);
            updateSpectraPlot(eventPair);
        }
    }

    protected void updateSpectraPlot(EventPair eventPair) {

        getRatioSpectraPlot().clearPlot();
        getRatioSpectraPlot().plotXYdata(toPlotPoints(eventPair, SpectraRatioPairOperator::getDiffAvg, null), null, null, Y_AXIS_LABEL);

        Map<EventPair, SpectraRatioPairInversionResult> inversionData = ratioMeasurementReport.getReport().getInversionEstimates();
        ratioSummaryValues.clear();
        ratioSummaryTable.getItems().clear();

        addEventDataToTable("Event Pair Data", eventPair);

        if (inversionData != null) {
            plotInversionPairData(inversionData, eventPair);
            addInversionDataToTable(inversionData.get(eventPair), "Pair Inversion Data");
        }

        Map<EventPair, SpectraRatioPairInversionResultJoint> jointInversionData = ratioMeasurementReport.getReport().getJointInversionEstimates();
        if (jointInversionData != null) {
            plotJointInversionData(jointInversionData, eventPair);
            addJointInversionDataToTable(jointInversionData.get(eventPair), "Joint Inversion Data");
        }

        updateMomentRatioLines();
        updateInversionReferenceLines(inversionData.get(eventPair), jointInversionData.get(eventPair));

        addUserSetLFLandHFL(eventPair);

        try {
            runGuiUpdate(() -> {
                getRatioSpectraPlot().getSubplot().fullReplot();
            });
        } catch (InvocationTargetException e) {
            log.debug(e.getMessage(), e);
        } catch (InterruptedException e) {
            log.debug(e.getMessage(), e);
        }
    }

    public void exportScreenshots(final File folder) {
        String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
        SnapshotUtils.writePng(folder, new Pair<>(ALL_PLOTS_PREFIX, rootPane), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>(SPECTRA_RATIO_PREFIX, spectraRatioPlotNode), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>("Combined_Inversion_Plots", inversionPlotsNode), timestamp);

        String plotId = getPlotIdentifier();
        String plotExportSuffix = SnapshotUtils.getTimestampWithLeadingSeparator() + ".svg";
        if (plotId != null && !plotId.isEmpty()) {
            plotExportSuffix = plotId + "_" + timestamp + ".svg";
        }

        exportSVG(combinedContourPlots, folder + File.separator + "Combined_Plots_" + plotExportSuffix);
        exportSVG(ratioSpectralPlotController.getSpectralPlot().getSubplot(), folder + File.separator + SPECTRA_RATIO_PREFIX + plotExportSuffix);
    }

    public void exportReportData(final File folder) {
        String reportFilename = "EventPairTableData.txt";
        StringBuilder reportStr = new StringBuilder();

        String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
        reportStr.append("SPECTRA RATIO DATA REPORT\nTimestamp: " + timestamp + "\n");
        reportStr.append("=".repeat(100) + "\n");

        ratioSummaryValues.forEach(keyValuePair -> {
            reportStr.append(keyValuePair.getX() + ": " + keyValuePair.getY() + "\n");
        });

        reportStr.append("=".repeat(100) + "\nSPECTRA RATIO DATA POINTS\n");
        ratioMeasurementReport.getRatiosList(getEventPair()).forEach(pairDetails -> {
            String centerFreq = dfmt4.format(centerFreq(pairDetails.getNumerWaveform().getLowFrequency(), pairDetails.getNumerWaveform().getHighFrequency()));
            reportStr.append("{station: " + pairDetails.getNumerWaveform().getStream().getStation().getStationName());
            reportStr.append(" , center_freq: " + centerFreq);
            reportStr.append(" , ratio: " + dfmt4.format(pairDetails.getDiffAvg()) + "}\n");
        });

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(folder + File.separator + reportFilename));
            writer.write(reportStr.toString());
            writer.close();
        } catch (IOException e) {
            log.error("Error attempting to write report data text file : {}", e.getLocalizedMessage(), e);
        }
    }

    private void exportSVG(BasicPlot plot, String path) {
        try {
            Files.write(Paths.get(path), plot.getSVG().getBytes());
        } catch (final IOException e) {
            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
        }
    }
}
