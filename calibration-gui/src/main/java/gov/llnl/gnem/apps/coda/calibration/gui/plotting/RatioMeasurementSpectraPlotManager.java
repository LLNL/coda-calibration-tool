/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import llnl.gnem.core.gui.plotting.api.PlotObject;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;
import llnl.gnem.core.gui.plotting.api.VerticalLine;
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
    private static final String PAIR_MOMENT_PREFIX = "Pair_Moment_";
    private static final String PAIR_STRESS_PREFIX = "Pair_Stress_";
    private static final String JOINT_MOMENT_PREFIX = "Joint_Moment_";
    private static final String JOINT_STRESS_PREFIX = "Joint_Stress_";
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
    private Map<String, PlotPoint> symbolStyleMap;
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
    private StackPane borderPane;

    @FXML
    private StackPane spectraRatioPlotNode;

    @FXML
    private StackPane pairStressPlotNode;

    @FXML
    private StackPane pairMomentPlotNode;

    @FXML
    private StackPane jointStressPlotNode;

    @FXML
    private StackPane jointMomentPlotNode;

    @FXML
    private Button snapshotButton;

    @FXML
    private Button downloadButton;

    @FXML
    private Button showMapButton;

    @FXML
    private Button viewRawSpectraBtn;

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

    private PlotObjectData jointStressContourPlotData;
    private BasicPlot jointStressContourPlot;

    private PlotObjectData jointMomentContourPlotData;
    private BasicPlot jointMomentContourPlot;

    private PlotObjectData stressContourPlotData;
    private BasicPlot stressContourPlot;

    private PlotObjectData momentContourPlotData;
    private BasicPlot momentContourPlot;

    private Axis jointStressYaxis;
    private Axis jointStressXaxis;
    private PlotObjectData jointStressPointPlotData;
    private PlotObjectData bestJointStressPointPlotData;

    private Axis jointMomentXaxis;
    private Axis jointMomentYaxis;
    private PlotObjectData jointMomentPointPlotData;
    private PlotObjectData bestJointMomentPointPlotData;

    private Axis stressYaxis;
    private Axis stressXaxis;
    private PlotObjectData stressPointPlotData;
    private PlotObjectData bestStressPointPlotData;

    private Axis momentXaxis;
    private Axis momentYaxis;
    private PlotObjectData momentPointPlotData;
    private PlotObjectData bestMomentPointPlotData;

    private SpectraRatioExporter spectraRatioExporter;

    public RatioMeasurementSpectraPlotManager(final SymbolStyleMapFactory styleFactory, CertLeafletMapController mapImpl, MapPlottingUtilities iconFactory, SpectraClient spectraClient,
            SpectraRatioExporter spectraRatioExporter) {
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

        SpectralPlot plot = getRatioSpectraPlot();
        plot.setLabels("Seismic Envelope Ratio Spectra", X_AXIS_LABEL, Y_AXIS_LABEL);
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(ratioSpectralPlotController::getSpectraDataMap));
        plot.getSubplot().setMargin(65, 40, 50, null);
        plot.getSubplot().attachToDisplayNode(spectraRatioPlotNode);

        jointStressContourPlot = plotFactory.basicPlot();
        jointStressContourPlot.setColorMap(CONTOUR_COLOR_MAP);
        Map<String, PlotObjectData> plotData = jointStressContourPlot.getPlotTypes();

        jointStressContourPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.CONTOUR));
        jointStressPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestJointStressPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestJointStressPointPlotData.getTraceStyle().setSeriesName("Estimate");
        bestJointStressPointPlotData.getTraceStyle().setFillColor(Color.WHITE);
        bestJointStressPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        bestJointStressPointPlotData.getTraceStyle().setzIndex(2);
        bestJointStressPointPlotData.getTraceStyle().setPxSize(10);
        bestJointStressPointPlotData.getTraceStyle().setStyleName(SymbolStyles.STAR.getStyleName());

        jointStressContourPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        jointStressPointPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        jointStressPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        jointStressPointPlotData.getTraceStyle().setSeriesName("Samples");
        jointStressPointPlotData.getTraceStyle().setzIndex(1);

        plotData.put(jointStressContourPlotData.getTraceStyle().getType().getType(), jointStressContourPlotData);
        plotData.put(jointStressPointPlotData.getTraceStyle().getSeriesName(), jointStressPointPlotData);
        plotData.put(bestJointStressPointPlotData.getTraceStyle().getSeriesName(), bestJointStressPointPlotData);

        jointStressXaxis = new PlotlyPlotFactory().axis(Type.LOG_X, "");
        jointStressYaxis = new PlotlyPlotFactory().axis(Type.LOG_Y, "");
        jointStressContourPlot.addAxes(jointStressXaxis);
        jointStressContourPlot.addAxes(jointStressYaxis);
        jointStressContourPlot.attachToDisplayNode(jointStressPlotNode);

        jointMomentContourPlot = plotFactory.basicPlot();
        jointMomentContourPlot.setColorMap(CONTOUR_COLOR_MAP);
        plotData = jointMomentContourPlot.getPlotTypes();

        jointMomentContourPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.CONTOUR));
        jointMomentPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestJointMomentPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestJointMomentPointPlotData.getTraceStyle().setSeriesName("Estimate");
        bestJointMomentPointPlotData.getTraceStyle().setFillColor(Color.WHITE);
        bestJointMomentPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        bestJointMomentPointPlotData.getTraceStyle().setzIndex(2);
        bestJointMomentPointPlotData.getTraceStyle().setPxSize(10);
        bestJointMomentPointPlotData.getTraceStyle().setStyleName(SymbolStyles.STAR.getStyleName());

        jointMomentContourPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        jointMomentPointPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        jointMomentPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        jointMomentPointPlotData.getTraceStyle().setSeriesName("Samples");
        jointMomentPointPlotData.getTraceStyle().setzIndex(1);

        plotData.put(jointMomentContourPlotData.getTraceStyle().getType().getType(), jointMomentContourPlotData);
        plotData.put(jointMomentPointPlotData.getTraceStyle().getSeriesName(), jointMomentPointPlotData);
        plotData.put(bestJointMomentPointPlotData.getTraceStyle().getSeriesName(), bestJointMomentPointPlotData);

        jointMomentXaxis = new PlotlyPlotFactory().axis(Type.X, "");
        jointMomentYaxis = new PlotlyPlotFactory().axis(Type.Y, "");
        jointMomentContourPlot.addAxes(jointMomentXaxis);
        jointMomentContourPlot.addAxes(jointMomentYaxis);
        jointMomentContourPlot.attachToDisplayNode(jointMomentPlotNode);

        stressContourPlot = plotFactory.basicPlot();
        stressContourPlot.setColorMap(CONTOUR_COLOR_MAP);
        plotData = stressContourPlot.getPlotTypes();

        stressContourPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.CONTOUR));
        stressPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestStressPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestStressPointPlotData.getTraceStyle().setSeriesName("Estimate");
        bestStressPointPlotData.getTraceStyle().setFillColor(Color.WHITE);
        bestStressPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        bestStressPointPlotData.getTraceStyle().setzIndex(2);
        bestStressPointPlotData.getTraceStyle().setPxSize(10);
        bestStressPointPlotData.getTraceStyle().setStyleName(SymbolStyles.STAR.getStyleName());

        stressContourPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        stressPointPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        stressPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        stressPointPlotData.getTraceStyle().setSeriesName("Samples");
        stressPointPlotData.getTraceStyle().setzIndex(1);

        plotData.put(stressContourPlotData.getTraceStyle().getType().getType(), stressContourPlotData);
        plotData.put(stressPointPlotData.getTraceStyle().getSeriesName(), stressPointPlotData);
        plotData.put(bestStressPointPlotData.getTraceStyle().getSeriesName(), bestStressPointPlotData);

        stressXaxis = new PlotlyPlotFactory().axis(Type.LOG_X, "");
        stressYaxis = new PlotlyPlotFactory().axis(Type.LOG_Y, "");
        stressContourPlot.addAxes(stressXaxis);
        stressContourPlot.addAxes(stressYaxis);
        stressContourPlot.attachToDisplayNode(pairStressPlotNode);

        momentContourPlot = plotFactory.basicPlot();
        momentContourPlot.setColorMap(CONTOUR_COLOR_MAP);
        plotData = momentContourPlot.getPlotTypes();

        momentContourPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.CONTOUR));
        momentPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestMomentPointPlotData = new PlotObjectData(new PlotTrace(PlotTrace.Style.SCATTER_MARKER));
        bestMomentPointPlotData.getTraceStyle().setSeriesName("Estimate");
        bestMomentPointPlotData.getTraceStyle().setFillColor(Color.WHITE);
        bestMomentPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        bestMomentPointPlotData.getTraceStyle().setzIndex(2);
        bestMomentPointPlotData.getTraceStyle().setPxSize(10);
        bestMomentPointPlotData.getTraceStyle().setStyleName(SymbolStyles.STAR.getStyleName());

        momentContourPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        momentPointPlotData.getTraceStyle().setColorMap(CONTOUR_COLOR_MAP);
        momentPointPlotData.getTraceStyle().setEdgeColor(Color.BLACK);
        momentPointPlotData.getTraceStyle().setSeriesName("Samples");
        momentPointPlotData.getTraceStyle().setzIndex(1);

        plotData.put(momentContourPlotData.getTraceStyle().getType().getType(), momentContourPlotData);
        plotData.put(momentPointPlotData.getTraceStyle().getSeriesName(), momentPointPlotData);
        plotData.put(bestMomentPointPlotData.getTraceStyle().getSeriesName(), bestMomentPointPlotData);

        momentXaxis = new PlotlyPlotFactory().axis(Type.X, "");
        momentYaxis = new PlotlyPlotFactory().axis(Type.Y, "");
        momentContourPlot.addAxes(momentXaxis);
        momentContourPlot.addAxes(momentYaxis);
        momentContourPlot.attachToDisplayNode(pairMomentPlotNode);

        ratioSummaryTable.setItems(ratioSummaryValues);

        CellBindingUtils.attachTextCellFactoriesString(ratioSummaryNameCol, Pair::getX);
        CellBindingUtils.attachTextCellFactoriesString(ratioSummaryValueCol, Pair::getY);
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
            List<SpectraRatioPairDetails> ratiosByEventPair = ratioMeasurementReport.getRatiosList(getEventPair());

            Platform.runLater(() -> {
                try {
                    File exportArchive;
                    Path tmpFolder = Files.createTempDirectory(Long.toString(System.currentTimeMillis()));
                    tmpFolder.toFile().deleteOnExit();
                    exportScreenshots(tmpFolder.toFile());
                    exportArchive = spectraRatioExporter.createExportArchive(ratiosByEventPair, tmpFolder);
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
        symbolStyleMap = symbolStyleMapFactory.build(new ArrayList<>(ratioMeasurementReport.getReport().getData().get(eventPair).keySet()), Station::getStationName);
        spectraRatioPairOperatorList = ratiosByEventPair.stream().map(SpectraRatioPairOperator::new).collect(Collectors.toList());
        symbolMap.clear();
        symbolMap.putAll(mapFunctionToPoint(spectraRatioPairOperatorList, SpectraRatioPairOperator::getDiffAvg));

        ratioSpectralPlotController.getSpectraDataMap().clear();
        getRatioSpectraPlot().clearPlot();
        ratioSpectralPlotController.getSpectraDataMap().putAll(symbolMap);
        getRatioSpectraPlot().plotXYdata(toPlotPoints(SpectraRatioPairOperator::getDiffAvg), null, Y_AXIS_LABEL);

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
            addInversionDataToTable(jointInversionData.get(eventPair), "Joint Inversion Data");
        }

        updateMomentRatioLines();

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

    private void plotInversionPairData(Map<EventPair, SpectraRatioPairInversionResult> inversionData, EventPair eventPair) {
        plotInversionData(
                "Pair",
                    inversionData,
                    eventPair,
                    stressContourPlot,
                    momentContourPlot,
                    stressContourPlotData,
                    stressPointPlotData,
                    bestStressPointPlotData,
                    momentContourPlotData,
                    momentPointPlotData,
                    bestMomentPointPlotData,
                    stressXaxis,
                    stressYaxis,
                    momentXaxis,
                    momentYaxis,
                    Color.BLUE);
    }

    private SpectraRatioPairInversionResult mapJointInversionResultToInversionResult(SpectraRatioPairInversionResultJoint eventRecord) {
        return new SpectraRatioPairInversionResult().setEventIdA(eventRecord.getEventIdA())
                                                    .setEventIdB(eventRecord.getEventIdB())
                                                    .setMomentEstimateA(eventRecord.getMomentEstimateA())
                                                    .setMomentEstimateB(eventRecord.getMomentEstimateB())
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
            plotInversionData(
                    "Joint",
                        wrappedInversionData,
                        eventPair,
                        jointStressContourPlot,
                        jointMomentContourPlot,
                        jointStressContourPlotData,
                        jointStressPointPlotData,
                        bestJointStressPointPlotData,
                        jointMomentContourPlotData,
                        jointMomentPointPlotData,
                        bestJointMomentPointPlotData,
                        jointStressXaxis,
                        jointStressYaxis,
                        jointMomentXaxis,
                        jointMomentYaxis,
                        Color.RED);
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

    private void addInversionDataToTable(SpectraRatioPairInversionResultJoint jointData, String dataHeader) {
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

                setCornerFrequencyLines(bestFit.getCornerEstimateA(), bestFit.getCornerEstimateB(), ratioShapeColor);

                Line fitRatioShape = plotMomentRatioShape(
                        spectraClient.getSpecificSpectra(bestFit.getMomentEstimateA(), bestFit.getApparentStressEstimateA(), 0.001, 30.0, 100).block(Duration.ofMinutes(10l)),
                            spectraClient.getSpecificSpectra(bestFit.getMomentEstimateB(), bestFit.getApparentStressEstimateB(), 0.001, 30.0, 100).block(Duration.ofMinutes(10l)));
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

                stressXaxis.setText(eventPair.getX().getEventId());
                stressYaxis.setText(eventPair.getY().getEventId());

                momentXaxis.setText(eventPair.getX().getEventId());
                momentYaxis.setText(eventPair.getY().getEventId());

                Map<String, PlotObjectData> plotData = stressContourPlot.getPlotTypes();
                plotData.put(stressContourPlotData.getTraceStyle().getType().getType(), stressContourPlotData);
                plotData.put(stressPointPlotData.getTraceStyle().getSeriesName(), stressPointPlotData);
                plotData.put(bestStressPointPlotData.getTraceStyle().getSeriesName(), bestStressPointPlotData);

                stressContourPlot.getTitle().setText(dataLabelPrefix + " Stress Plot");
                stressContourPlot.replot();

                plotData = momentContourPlot.getPlotTypes();
                plotData.put(momentContourPlotData.getTraceStyle().getType().getType(), momentContourPlotData);
                plotData.put(momentPointPlotData.getTraceStyle().getSeriesName(), momentPointPlotData);
                plotData.put(bestMomentPointPlotData.getTraceStyle().getSeriesName(), bestMomentPointPlotData);

                momentContourPlot.getTitle().setText(dataLabelPrefix + " Moment Plot");
                momentContourPlot.replot();
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
            if (eventPairList.size() > 0) {
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

    private List<PlotPoint> toPlotPoints(final Function<SpectraRatioPairOperator, Double> func) {
        List<PlotPoint> allPlotPoints = new ArrayList<>();

        spectraRatioPairOperatorList.forEach(ratioDetails -> {
            FrequencyBand freqValue = ratioDetails.getFrequency();
            PlotPoint pp = symbolStyleMap.get(ratioDetails.getDenomWaveform().getStream().getStation().getStationName());
            if (pp != null) {
                pp.setX(centerFreq(freqValue.getLowFrequency(), freqValue.getHighFrequency()));
                pp.setY(func.apply(ratioDetails));

                final LabeledPlotPoint point = new LabeledPlotPoint(ratioDetails.getStation().getStationName(), pp);
                allPlotPoints.add(point);
            }
        });

        return allPlotPoints;
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
                    Line refRatioShape = plotMomentRatioShape(largeSpectra, smallSpectra);
                    refRatioShape.setName("CCT Mw ratio");

                    getRatioSpectraPlot().getSubplot().addPlotObject(refRatioShape);

                    setMomentRefRatioLines(momentRatio, refRatio);
                }
            }
        }
    }

    private Line plotMomentRatioShape(Spectra largeSpectra, Spectra smallSpectra) {
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

            Line ratioShape = plotFactory.line(ratioDataX, ratioDataY, Color.BLUE, LineStyles.DASH, 2);

            return ratioShape;
        }
        return null;
    }

    private void setMomentRefRatioLines(double momentRatio, double refRatio) {

        double minVal = Math.min(0.0, symbolMap.entrySet().stream().mapToDouble(entry -> entry.getKey().getX()).min().getAsDouble());
        double maxVal = Math.max(20.0, symbolMap.entrySet().stream().mapToDouble(entry -> entry.getKey().getX()).max().getAsDouble());
        double length = maxVal - minVal;

        final float[] momentData = new float[2];
        momentData[0] = (float) momentRatio;
        momentData[1] = (float) momentRatio;

        PlotObject momentLine = plotFactory.lineX(String.format("CCT LFL: %s", dfmt4.format(momentRatio)), minVal, length, momentData, Color.BLACK, LineStyles.DASH_DOT, 2);

        final float[] refData = new float[2];
        refData[0] = (float) refRatio;
        refData[1] = (float) refRatio;

        PlotObject refLine = plotFactory.lineX(String.format("CCT HFL: %s", dfmt4.format(refRatio)), minVal, length, refData, Color.BLACK, LineStyles.DOT, 2);

        getRatioSpectraPlot().getSubplot().addPlotObject(momentLine);
        getRatioSpectraPlot().getSubplot().addPlotObject(refLine);
    }

    private void setCornerFrequencyLines(double cornerEstimateA, double cornerEstimateB, Color lineColor) {

        VerticalLine lineA = plotFactory.verticalLine(cornerEstimateA, 50.0, dfmt2.format(cornerEstimateA));
        lineA.setFillColor(lineColor);
        VerticalLine lineB = plotFactory.verticalLine(cornerEstimateB, 50.0, dfmt2.format(cornerEstimateB));
        lineB.setFillColor(lineColor);

        lineA.setLogScaleX(true);
        lineB.setLogScaleX(true);

        getRatioSpectraPlot().getSubplot().addPlotObject(lineA);
        getRatioSpectraPlot().getSubplot().addPlotObject(lineB);
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
        StringBuilder sb = new StringBuilder();
        ratioMeasurementReport.getStationsForEventPair(getEventPair()).forEach(station -> {
            sb.append(station.getStationName());
            sb.append("_");
        });
        return String.format("large_event_%s_small_event_%s_stations_%s", getEventPair().getY().getEventId(), getEventPair().getX().getEventId(), sb.toString());
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

    private PropertyChangeListener getPlotpointObserver(final Supplier<Map<Point2D, SpectraRatioPairOperator>> ratioDetailsMap) {
        return evt -> {
            Object po = evt.getNewValue();
            if (po instanceof PlotObjectClick && ((PlotObjectClick) po).getPlotPoints() != null) {
                handlePlotObjectClicked((PlotObjectClick) po, point -> ratioDetailsMap.get().get(point));
            }
        };
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

        RatioMeasurementWaveformPlotManager ratioWaveformPlots = new RatioMeasurementWaveformPlotManager(mapImpl, iconFactory);
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
        List<PlotPoint> numerPoints = new ArrayList<>(toPlotPoints(SpectraRatioPairOperator::getNumerAvg));
        List<PlotPoint> denomPoints = new ArrayList<>(toPlotPoints(SpectraRatioPairOperator::getDenomAvg));
        SpectraPlotManager spectraPlotManager = new SpectraPlotManager(getEventPair(), numerPoints, denomPoints);

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

    protected void updatePlotPoint() {
        symbolMap = mapFunctionToPoint(spectraRatioPairOperatorList, SpectraRatioPairOperator::getDiffAvg);
        ratioSpectralPlotController.getSpectraDataMap().clear();
        ratioSpectralPlotController.getSpectraDataMap().putAll(symbolMap);
        ratioSpectralPlotController.getSpectralPlot().plotXYdata(toPlotPoints(SpectraRatioPairOperator::getDiffAvg), null, Y_AXIS_LABEL);

        updateMomentRatioLines();
    }

    public void exportScreenshots(final File folder) {
        String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
        SnapshotUtils.writePng(folder, new Pair<>(ALL_PLOTS_PREFIX, mainSplitPane), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>(SPECTRA_RATIO_PREFIX, spectraRatioPlotNode), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>(JOINT_MOMENT_PREFIX, jointMomentPlotNode), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>(JOINT_STRESS_PREFIX, jointStressPlotNode), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>(PAIR_MOMENT_PREFIX, pairMomentPlotNode), timestamp);
        SnapshotUtils.writePng(folder, new Pair<>(PAIR_STRESS_PREFIX, pairStressPlotNode), timestamp);

        String plotId = getPlotIdentifier();
        String plotExportSuffix = SnapshotUtils.getTimestampWithLeadingSeparator() + ".svg";
        if (plotId != null && !plotId.isEmpty()) {
            plotExportSuffix = plotId + "_" + timestamp + ".svg";
        }

        exportSVG(jointMomentContourPlot, folder + File.separator + JOINT_MOMENT_PREFIX + plotExportSuffix);
        exportSVG(jointStressContourPlot, folder + File.separator + JOINT_STRESS_PREFIX + plotExportSuffix);
        exportSVG(momentContourPlot, folder + File.separator + PAIR_MOMENT_PREFIX + plotExportSuffix);
        exportSVG(stressContourPlot, folder + File.separator + PAIR_STRESS_PREFIX + plotExportSuffix);
        exportSVG(jointMomentContourPlot, folder + File.separator + JOINT_MOMENT_PREFIX + plotExportSuffix);
        exportSVG(jointStressContourPlot, folder + File.separator + JOINT_STRESS_PREFIX + plotExportSuffix);
        exportSVG(ratioSpectralPlotController.getSpectralPlot().getSubplot(), folder + File.separator + SPECTRA_RATIO_PREFIX + plotExportSuffix);
    }

    private void exportSVG(BasicPlot plot, String path) {
        try {
            Files.write(Paths.get(path), plot.getSVG().getBytes());
        } catch (final IOException e) {
            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
        }
    }
}
