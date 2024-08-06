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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import gov.llnl.gnem.apps.coda.spectra.gui.events.RatioSegmentChangeEvent;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatioPairOperator;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.SplitPane.Divider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.DirectoryChooser;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.events.PlotAxisChange;
import llnl.gnem.core.util.PairT;

public class RatioMeasurementWaveformPlotManager {

    private static final String TIME_SECONDS_FROM_ORIGIN = "Time (seconds from origin)";
    private static final String WAVEFORM_PREFIX = "Waveform_";
    private static final String RATIO_WAVEFORM_PREFIX = "Ratio_waveform_";
    private static final Logger log = LoggerFactory.getLogger(RatioMeasurementWaveformPlotManager.class);
    private final NumberFormat dfmt4 = NumberFormatFactory.fourDecimalOneLeadingZero();
    private RatioMeasurementSpectraPlotManager parentSpectraPlot;
    private EventBus bus;

    private RatioDetailPlot selectedSinglePlot;
    private RatioDetailPlot ratioDiffWavePlot;

    private PairT<AxisLimits, AxisLimits> savedAxisLimits = null;
    private Label freqBandLabel;
    private Label eventStationLabel;
    private Map<FrequencyBand, SpectraRatioPairDetails> curEventStationRatios;
    private List<FrequencyBand> curFrequencies;
    private List<Station> curEventStations;
    private int curFreqIndex = -1;
    private int curStationIndex = -1;
    private boolean alignPeaksModeBoolean = true;
    private boolean ratioWindowModeBoolean = true;

    final Button nextButton = new Button(">");
    final Button prevButton = new Button("<");
    final Button nextStationButton = new Button("↑");
    final Button prevStationButton = new Button("↓");
    final ToggleButton alignPeaksToggle = new ToggleButton("AP");
    final ToggleButton trueTimeToggle = new ToggleButton("TT");
    final ToggleButton ratioWindowToggle = new ToggleButton("RW");

    @FXML
    private StackPane ratioPlotNode;

    @FXML
    private BorderPane borderPane;

    @FXML
    private ToolBar topToolbar;

    @FXML
    private SplitPane ratioSplitPane;
    private Divider splitPaneDivider;

    @FXML
    private Pane waveformsPane;

    @FXML
    private Button snapshotButton;

    @FXML
    private Button showRatioOnMapButton;

    @FXML
    private Pane ratioWaveformPane;

    // Context menu when plot is right-clicked
    private final ContextMenu contextMenu;
    private final MenuItem resetToPeak;

    private final DirectoryChooser screenshotFolderChooser = new DirectoryChooser();

    private CertLeafletMapController mapImpl;
    private MapPlottingUtilities iconFactory;
    private final BiConsumer<Boolean, String> eventSelectionCallback;
    private final BiConsumer<Boolean, String> stationSelectionCallback;

    private final EventHandler<InputEvent> nextAction = event -> {
        if (curEventStationRatios != null && curFrequencies != null && curFreqIndex < curEventStationRatios.size() - 1) {
            curFreqIndex += 1;
            SpectraRatioPairDetails ratioWave = curEventStationRatios.get(curFrequencies.get(curFreqIndex));
            if (ratioWave != null) {
                createRatioWaveformPlot(ratioWave, alignPeaksModeBoolean);
            }
        }
    };

    private final EventHandler<InputEvent> prevAction = event -> {
        if (curEventStationRatios != null && curFrequencies != null && curFreqIndex > 0) {
            curFreqIndex -= 1;
            SpectraRatioPairDetails ratioDetails = curEventStationRatios.get(curFrequencies.get(curFreqIndex));
            if (ratioDetails != null) {
                createRatioWaveformPlot(ratioDetails, alignPeaksModeBoolean);
            }
        }
    };

    private final EventHandler<InputEvent> nextStationAction = event -> {
        if (curEventStations != null && curStationIndex < curEventStations.size() - 1) {
            curStationIndex += 1;
            setCurrentStation(curEventStations.get(curStationIndex));
        }
    };

    private final EventHandler<InputEvent> prevStationAction = event -> {
        if (curEventStations != null && curStationIndex > 0) {
            curStationIndex -= 1;
            setCurrentStation(curEventStations.get(curStationIndex));
        }
    };

    private final EventHandler<InputEvent> alignPeaksToggleAction = event -> {
        alignPeaksModeBoolean = !alignPeaksModeBoolean;
        alignPeaksToggle.setSelected(alignPeaksModeBoolean);
        trueTimeToggle.setSelected(!alignPeaksModeBoolean);

        if (alignPeaksModeBoolean) {
            alignPeaksToggle.setStyle("-fx-background-color: DarkSeaGreen");
            trueTimeToggle.setStyle(null);
        } else {
            alignPeaksToggle.setStyle(null);
            trueTimeToggle.setStyle("-fx-background-color: DarkSeaGreen");
        }

        if (selectedSinglePlot != null) {
            Platform.runLater(() -> {
                selectedSinglePlot.setAlignPeaks(alignPeaksModeBoolean);
                selectedSinglePlot.plotRatio();
            });
        } else {
            SpectraRatioPairDetails ratioDetails = curEventStationRatios.get(curFrequencies.get(curFreqIndex));
            if (ratioDetails != null) {
                createRatioWaveformPlot(ratioDetails, alignPeaksModeBoolean);
            }
        }
    };

    private final EventHandler<InputEvent> showRatioWindowToggleAction = event -> {
        ratioWindowModeBoolean = !ratioWindowModeBoolean;
        ratioWindowToggle.setSelected(ratioWindowModeBoolean);

        if (ratioWindowModeBoolean) {
            ratioWindowToggle.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            ratioWindowToggle.setStyle(null);
        }

        showHideRatioWaveform(ratioWindowModeBoolean);
    };

    public RatioMeasurementWaveformPlotManager(EventBus bus, CertLeafletMapController mapImpl, MapPlottingUtilities iconFactory) {
        this.bus = bus;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
        freqBandLabel = new Label("Frequency Band");
        eventStationLabel = new Label("Event/Station");

        contextMenu = new ContextMenu();
        resetToPeak = new MenuItem("Reset Cuts to Default");

        eventSelectionCallback = (selected, eventId) -> {
            log.debug(eventId);
        };

        stationSelectionCallback = (selected, stationId) -> {
            log.debug(stationId);
        };
    }

    public void showHideRatioWaveform(boolean show) {
        if (splitPaneDivider != null) {
            final double position = show ? 0.8 : 1.0;

            Platform.runLater(() -> {
                splitPaneDivider.setPosition(position);
            });
        }
    }

    public void setParentSpectra(RatioMeasurementSpectraPlotManager parentSpectra) {
        this.parentSpectraPlot = parentSpectra;
    }

    public void setCurrentEvent(List<Station> stationsForCurrentEvent) {
        curEventStations = stationsForCurrentEvent;
        curStationIndex = 0;
        curFreqIndex = 0;
    }

    public void setCurrentStation(Station station) {
        Result<Map<FrequencyBand, SpectraRatioPairDetails>> result = parentSpectraPlot.getRatioDetailsFromStation(station);
        if (result.isSuccess() && result.getResultPayload().isPresent()) {
            curEventStationRatios = result.getResultPayload().get();
            curFrequencies = curEventStationRatios.keySet().stream().sorted(this::sortFrequencies).collect(Collectors.toList());

            curStationIndex = curEventStations.indexOf(station);
            if (curStationIndex < 0) {
                curStationIndex = 0;
            }

            // Ensure frequency index fits new station
            if (curFreqIndex >= curFrequencies.size()) {
                curFreqIndex = curFrequencies.size() - 1;
            }

            createRatioWaveformPlot(curEventStationRatios.get(curFrequencies.get(curFreqIndex)), alignPeaksModeBoolean);
        }
    }

    private int sortFrequencies(FrequencyBand f1, FrequencyBand f2) {
        int compared = Double.compare(f1.getLowFrequency(), f2.getLowFrequency());
        if (compared == 0) {
            compared = Double.compare(f1.getHighFrequency(), f2.getHighFrequency());
        }
        return compared;
    }

    public void setCurrentFreqAndStation(double lowFreq, double highFreq, Station station) {
        Result<Map<FrequencyBand, SpectraRatioPairDetails>> result = parentSpectraPlot.getRatioDetailsFromStation(station);
        if (result.isSuccess() && result.getResultPayload().isPresent()) {
            curEventStationRatios = result.getResultPayload().get();
            curFrequencies = curEventStationRatios.keySet().stream().sorted(this::sortFrequencies).collect(Collectors.toList());

            curFreqIndex = curFrequencies.indexOf(new FrequencyBand(lowFreq, highFreq));
            if (curFreqIndex < 0) {
                curFreqIndex = 0;
            }
            curStationIndex = curEventStations.indexOf(station);
            if (curStationIndex < 0) {
                curStationIndex = 0;
            }
            createRatioWaveformPlot(curEventStationRatios.get(curFrequencies.get(curFreqIndex)), alignPeaksModeBoolean);
        }
    }

    public void setDisplayText(SpectraRatioPairOperator ratioDetails) {
        if (ratioDetails != null && curEventStationRatios != null) {
            String lowFreq = String.valueOf(ratioDetails.getFrequency().getLowFrequency());
            String highFreq = String.valueOf(ratioDetails.getFrequency().getHighFrequency());
            String numerEvent = ratioDetails.getNumerWaveform().getEvent().getEventId();
            String denomEvent = ratioDetails.getDenomWaveform().getEvent().getEventId();
            String station = ratioDetails.getStation().getStationName();
            String diffAvg = ratioDetails.getDiffAvg() != null ? dfmt4.format(ratioDetails.getDiffAvg()) : "N/A";

            freqBandLabel.setText(String.format("Frequency Band (%s/%s) - Low %s High: %s", curFreqIndex + 1, curEventStationRatios.size(), lowFreq, highFreq));
            eventStationLabel.setText(String.format("Events: %s/%s - Station %s - Ratio: %s", numerEvent, denomEvent, station, diffAvg));
        } else {
            freqBandLabel.setText("No frequency bands to select");
            eventStationLabel.setText("");
        }
    }

    public PairT<AxisLimits, AxisLimits> getSavedAxisLimits() {
        return savedAxisLimits;
    }

    public void setSavedAxisLimits(PairT<AxisLimits, AxisLimits> axisLimits) {
        this.savedAxisLimits = axisLimits;
    }

    public void exportScreenshots(final File folder) {
        String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
        SnapshotUtils.writePng(folder, new Pair<>(WAVEFORM_PREFIX, ratioPlotNode), timestamp);
        String plotId = selectedSinglePlot.getPlotIdentifier();
        if (plotId != null && !plotId.isEmpty()) {
            exportSVG(selectedSinglePlot, folder + File.separator + WAVEFORM_PREFIX + plotId + "_" + timestamp + ".svg");
            exportSVG(ratioDiffWavePlot, folder + File.separator + RATIO_WAVEFORM_PREFIX + plotId + "_" + timestamp + ".svg");
        } else {
            exportSVG(selectedSinglePlot, folder + File.separator + WAVEFORM_PREFIX + SnapshotUtils.getTimestampWithLeadingSeparator() + ".svg");
            exportSVG(ratioDiffWavePlot, folder + File.separator + RATIO_WAVEFORM_PREFIX + SnapshotUtils.getTimestampWithLeadingSeparator() + ".svg");
        }
    }

    public void exportSVG(RatioDetailPlot plot, String path) {
        try {
            Files.write(Paths.get(path), plot.getSVG().getBytes());
        } catch (final IOException e) {
            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
        }
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
    private void showRatioOnMap(final ActionEvent e) {
        SpectraRatioPairDetails ratioDetails = curEventStationRatios.get(curFrequencies.get(curFreqIndex));

        if (mapImpl != null && ratioDetails != null) {
            List<Waveform> listData = new ArrayList<>();
            listData.add(ratioDetails.getNumerWaveform());
            listData.add(ratioDetails.getDenomWaveform());

            mapImpl.clearIcons();
            mapImpl.addIcons(iconFactory.genIconsFromWaveforms(eventSelectionCallback, stationSelectionCallback, listData));

            Platform.runLater(() -> {
                mapImpl.show();
                mapImpl.fitViewToActiveShapes();
            });
        }
    }

    @FXML
    public void initialize() {
        topToolbar.getItems().add(prevStationButton);
        topToolbar.getItems().add(nextStationButton);
        topToolbar.getItems().add(prevButton);
        topToolbar.getItems().add(nextButton);
        topToolbar.getItems().add(alignPeaksToggle);
        topToolbar.getItems().add(trueTimeToggle);
        topToolbar.getItems().add(ratioWindowToggle);
        topToolbar.getItems().add(freqBandLabel);
        topToolbar.getItems().add(eventStationLabel);

        contextMenu.getItems().add(resetToPeak);

        resetToPeak.setOnAction(evt -> {
            if (this.selectedSinglePlot != null) {
                this.selectedSinglePlot.resetCuts();
            }
        });

        ratioSplitPane.addEventFilter(MouseEvent.MOUSE_CLICKED, t -> {
            if (MouseButton.SECONDARY == t.getButton()) {
                contextMenu.show(ratioSplitPane, t.getScreenX(), t.getScreenY());
            } else {
                contextMenu.hide();
            }
        });

        nextStationButton.addEventHandler(MouseEvent.MOUSE_CLICKED, nextStationAction::handle);
        prevStationButton.addEventHandler(MouseEvent.MOUSE_CLICKED, prevStationAction::handle);
        nextButton.addEventHandler(MouseEvent.MOUSE_CLICKED, nextAction::handle);
        prevButton.addEventHandler(MouseEvent.MOUSE_CLICKED, prevAction::handle);
        alignPeaksToggle.addEventHandler(MouseEvent.MOUSE_CLICKED, alignPeaksToggleAction::handle);
        trueTimeToggle.addEventHandler(MouseEvent.MOUSE_CLICKED, alignPeaksToggleAction::handle);
        ratioWindowToggle.addEventHandler(MouseEvent.MOUSE_CLICKED, showRatioWindowToggleAction::handle);

        final Font sizedFont = Font.font(prevButton.getFont().getFamily(), 12f);

        nextButton.setFont(sizedFont);
        prevButton.setFont(sizedFont);
        nextStationButton.setFont(sizedFont);
        prevStationButton.setFont(sizedFont);
        alignPeaksToggle.setFont(sizedFont);
        trueTimeToggle.setFont(sizedFont);
        ratioWindowToggle.setFont(sizedFont);

        nextButton.setFocusTraversable(false);
        prevButton.setFocusTraversable(false);
        nextStationButton.setFocusTraversable(false);
        prevStationButton.setFocusTraversable(false);
        alignPeaksToggle.setFocusTraversable(false);
        trueTimeToggle.setFocusTraversable(false);
        ratioWindowToggle.setFocusTraversable(false);

        nextButton.setTooltip(new Tooltip("Go to higher frequency band"));
        prevButton.setTooltip(new Tooltip("Go to lower frequency band"));
        nextStationButton.setTooltip(new Tooltip("Go to next Station"));
        prevStationButton.setTooltip(new Tooltip("Go to previous Station"));
        alignPeaksToggle.setTooltip(new Tooltip("Toggle on to align waveform peaks"));
        trueTimeToggle.setTooltip(new Tooltip("Toggle on to switch to 'True Time' view"));
        ratioWindowToggle.setTooltip(new Tooltip("Toggle to show or hide the ratio waveform display at the bottom of plot"));

        alignPeaksToggle.setSelected(alignPeaksModeBoolean);
        trueTimeToggle.setSelected(!alignPeaksModeBoolean);
        if (alignPeaksModeBoolean) {
            alignPeaksToggle.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            trueTimeToggle.setStyle("-fx-background-color: DarkSeaGreen");
        }
        if (ratioWindowModeBoolean) {
            ratioWindowToggle.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            ratioWindowToggle.setStyle(null);
        }

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
        showRatioOnMapButton.setGraphic(mapLabel);

        screenshotFolderChooser.setTitle("Ratio Waveform Screenshot Export Folder");
        ratioSplitPane.setOnKeyReleased(this::triggerKeyEvent);
    }

    private void updatePlotAxes(PlotAxisChange change) {
        if (selectedSinglePlot != null) {
            this.setSavedAxisLimits(change.getAxisLimits());
            if (change.isReset()) {
                Platform.runLater(() -> {
                    selectedSinglePlot.replot();
                });
            }
        }
    }

    private void updatePlot() {
        if (selectedSinglePlot != null) {
            if (this.parentSpectraPlot != null) {
                this.parentSpectraPlot.updatePlotPoint(selectedSinglePlot.getRatioDetails().getEventPair());
            }
            Platform.runLater(() -> {
                setDisplayText(selectedSinglePlot.getRatioDetails());
                selectedSinglePlot.plotRatio();
                ratioDiffWavePlot.plotDiffRatio();
            });

            bus.post(new RatioSegmentChangeEvent(selectedSinglePlot.getRatioDetails()));
        }
    }

    private void createRatioWaveformPlot(SpectraRatioPairDetails spectraRatioPairDetails, boolean alignPeaks) {
        if (spectraRatioPairDetails != null && !spectraRatioPairDetails.isLoadedFromJson()) {

            waveformsPane.getChildren().clear();
            ratioWaveformPane.getChildren().clear();

            SpectraRatioPairOperator ratio = new SpectraRatioPairOperator(spectraRatioPairDetails);
            setDisplayText(ratio);
            final RatioDetailPlot plot = new RatioDetailPlot(ratio, alignPeaks);
            Double eventDistance = parentSpectraPlot.getEventPairDistanceKm();
            plot.setTitle(String.format("Events Distance: %s Km", dfmt4.format(eventDistance)));
            plot.setMargin(null, null, null, null);
            plot.getxAxis().setText(TIME_SECONDS_FROM_ORIGIN);
            plot.setAlignPeaks(alignPeaks);
            plot.attachToDisplayNode(waveformsPane);
            waveformsPane.getChildren().add(snapshotButton); // Add snapshot button back
            waveformsPane.getChildren().add(showRatioOnMapButton); // Add snapshot button back
            selectedSinglePlot = plot;

            final RatioDetailPlot plotDiff = new RatioDetailPlot(ratio, alignPeaks);

            plotDiff.setMargin(null, null, null, null);
            plotDiff.getxAxis().setText(TIME_SECONDS_FROM_ORIGIN);
            plotDiff.setAlignPeaks(alignPeaks);
            plotDiff.attachToDisplayNode(ratioWaveformPane);

            ratioDiffWavePlot = plotDiff;

            if (!ratioSplitPane.getDividers().isEmpty()) {
                splitPaneDivider = ratioSplitPane.getDividers().get(0);
            }

            plot.setAxisChangeListener(axisChange -> {
                if (axisChange.getNewValue() instanceof PlotAxisChange) {
                    updatePlotAxes((PlotAxisChange) axisChange.getNewValue());
                }
            });
            plot.setCutSegmentChangeListener(cutSegmentChange -> {
                updatePlot();
            });

            if (this.getSavedAxisLimits() != null) {
                plot.setAxisLimits(this.getSavedAxisLimits().getFirst(), this.getSavedAxisLimits().getSecond());
            } else {
                plot.resetAxisLimits();
            }

            showHideRatioWaveform(ratioWindowModeBoolean);

            plot.plotRatio();
            plotDiff.plotDiffRatio();
        }
    }

    public void triggerKeyEvent(final KeyEvent event) {
        if (event.getCode() == KeyCode.DOWN) {
            prevStationButton.requestFocus();
            prevStationAction.handle(event);
        } else if (event.getCode() == KeyCode.UP) {
            nextStationButton.requestFocus();
            nextStationAction.handle(event);
        } else if (event.getCode() == KeyCode.RIGHT) {
            nextButton.requestFocus();
            nextAction.handle(event);
        } else if (event.getCode() == KeyCode.LEFT) {
            prevAction.handle(event);
            prevButton.requestFocus();
        }
    }

    public void attachToDisplayNode(final Pane parent) {
        if (parent != null) {
            parent.getChildren().add(borderPane);
        }
    }

}
