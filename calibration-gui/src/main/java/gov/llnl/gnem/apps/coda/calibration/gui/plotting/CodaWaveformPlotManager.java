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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.common.gui.data.client.DistanceCalculator;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.common.model.util.PICK_TYPES;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.AxisLimits;
import llnl.gnem.core.gui.plotting.events.PlotAxisChange;
import llnl.gnem.core.util.PairT;
import llnl.gnem.core.util.seriesMathHelpers.MinMax;
import reactor.core.scheduler.Schedulers;

//TODO: Split this out into a few separate functional areas when time permits (i.e. CodaWaveformPlotGUI, CodaWaveformPlotManager, etc).
// As it currently is this class is pretty entangled.
public class CodaWaveformPlotManager {

    private static final String WINDOW_LINE_LABEL = "Window";
    private static final String WINDOW_LINE_TOOLTIP = "Toggle Minimum and Maximum Window Lines";
    private static final String GROUP_VELOCITY_TOOLTIP = "Toggle Group Velocity";
    private static final String GROUP_VELOCITY_LABEL = "GV";
    private static final String ZOOM_SYNC_LABEL = "SZ";
    private static final String ZOOM_SYNC_TOOLTIP = "Synchronize Zoom between all plots.";
    private static final String TIME_SECONDS_FROM_ORIGIN = "Time (seconds from origin)";
    private static final String WAVEFORM_PREFIX = "Waveform_";
    private static final String CLICK_TO_PICK_TOOLTIP = "Toggle click-to-pick mode";
    private static final String CLICK_TO_PICK_ICON = "/click_picking_mode_icon.png";
    private static final String CLICK_TO_PICK_LABEL = "Picking";
    private static final String MOVE_START_TOOLTIP = "Toggle move coda start mode";
    private static final String MOVE_START = "MS";

    private static final Logger log = LoggerFactory.getLogger(CodaWaveformPlotManager.class);
    private final WaveformClient waveformClient;
    private final ShapeMeasurementClient shapeClient;
    private final ParameterClient paramsClient;
    private final PeakVelocityClient peakVelocityClient;
    private final LeafletMapController cctMap;
    private final CertLeafletMapController certMap;
    private final MapPlottingUtilities mapPlotUtils;
    private final ToolBar multiPageToolbar;
    private final ToolBar multiFrequencyToolbar;
    private final ToolBar multiPlotToolbar;
    private List<Icon> mappedIcons = new ArrayList<>();
    private Map<Long, Integer> orderedWaveformIDs = new HashMap<>();
    private final SortedMap<Integer, CodaWaveformPlot> orderedWaveformPlots = new TreeMap<>();
    private CodaWaveformPlot selectedSinglePlot;
    private PairT<AxisLimits, AxisLimits> savedAxisLimits = null;
    private final Set<Long> allWaveformIDs = new LinkedHashSet<>();
    private static final Integer PAGE_SIZE = 5;
    private final Object bagLock = new Object();
    private final Stack<CodaWaveformPlot> plotBag = new Stack<>();
    private int currentPage = 0;
    private int totalPages = 0;
    private final Label pagingLabel;
    private Label freqBandLabel = new Label();
    private List<Waveform> curEventStationWaveforms;
    private Map<Integer, Long> plottedWaveformIds;
    private int curFreqIndex = -1;
    private Waveform selectedWaveform = null; // Used for including /excluding from waveform plots
    private EventStaFreqStringComparator eventStaFreqComparator = new EventStaFreqStringComparator();

    private final BorderPane borderPane;
    private final VBox waveformPanel;

    private final ContextMenu contextMenu;
    private final ToggleGroup includeToggleGroup = new ToggleGroup();
    private final MenuItem clearUcsPick = new MenuItem("Clear UCS pick");
    private final RadioMenuItem radioIncludeBtn = new RadioMenuItem("Include");
    private final RadioMenuItem radioExcludeBtn = new RadioMenuItem("Exclude");

    private final MenuItem addByWaveformId = new MenuItem("WaveformId");
    private final MenuItem addByStation = new MenuItem("Station");
    private final MenuItem addByEvent = new MenuItem("Event");
    private final MenuItem addByEventAndStation = new MenuItem("Event & Station");

    final Button forwardButton = new Button(">");
    final Button backwardButton = new Button("<");
    final Button nextButton = new Button("↑");
    final Button prevButton = new Button("↓");

    final ToggleButton groupVelToggle = new ToggleButton(GROUP_VELOCITY_LABEL);
    final ToggleButton groupVelToggle2 = new ToggleButton(GROUP_VELOCITY_LABEL);
    final ToggleButton groupVelToggle3 = new ToggleButton(GROUP_VELOCITY_LABEL);

    final ToggleButton windowLineToggle = new ToggleButton(WINDOW_LINE_LABEL);
    final ToggleButton windowLineToggle2 = new ToggleButton(WINDOW_LINE_LABEL);
    final ToggleButton windowLineToggle3 = new ToggleButton(WINDOW_LINE_LABEL);

    final ToggleButton clickToPickMode = new ToggleButton();
    final ToggleButton clickToPickMode2 = new ToggleButton();
    final ToggleButton clickToPickMode3 = new ToggleButton();

    final ToggleButton moveStartToggle = new ToggleButton(MOVE_START);
    final ToggleButton moveStartToggle2 = new ToggleButton(MOVE_START);
    final ToggleButton moveStartToggle3 = new ToggleButton(MOVE_START);

    private boolean clickToPickModeBoolean = false;

    final ToggleButton syncZoomMode = new ToggleButton(ZOOM_SYNC_LABEL);
    final ToggleButton syncZoomMode2 = new ToggleButton(ZOOM_SYNC_LABEL);
    private boolean syncZoomModeBoolean = false;

    private final EventHandler<InputEvent> forwardAction = event -> {
        if ((currentPage + 1) < totalPages) {
            currentPage++;
            if (!syncZoomModeBoolean) {
                setSavedAxisLimits(null);
            }
            loadWaveformsForPage(currentPage);
        }
    };

    private final EventHandler<InputEvent> backwardAction = event -> {
        if (currentPage > 0) {
            currentPage--;
            if (!syncZoomModeBoolean) {
                setSavedAxisLimits(null);
            }
            loadWaveformsForPage(currentPage);
        }
    };

    private final EventHandler<InputEvent> nextAction = event -> {
        if (curEventStationWaveforms != null && curFreqIndex < curEventStationWaveforms.size() - 1) {
            curFreqIndex += 1;
            Waveform wave = curEventStationWaveforms.get(curFreqIndex);
            if (wave != null) {
                setFrequencyDisplayText(wave);
                plotWaveform(wave.getId());
            }
        }
    };

    private final EventHandler<InputEvent> prevAction = event -> {
        if (curEventStationWaveforms != null && curFreqIndex > 0) {
            curFreqIndex -= 1;
            Waveform wave = curEventStationWaveforms.get(curFreqIndex);

            if (wave != null) {
                setFrequencyDisplayText(wave);
                plotWaveform(wave.getId());
            }
        }
    };

    private final EventHandler<InputEvent> groupVelToggleAction = event -> {
        ToggleButton btnClicked = (ToggleButton) event.getSource();
        if (btnClicked != null && btnClicked.isSelected()) {
            groupVelToggle.setSelected(true);
            groupVelToggle2.setSelected(true);
            groupVelToggle3.setSelected(true);
            groupVelToggle.setStyle("-fx-background-color: DarkSeaGreen");
            groupVelToggle2.setStyle("-fx-background-color: DarkSeaGreen");
            groupVelToggle3.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            groupVelToggle.setSelected(false);
            groupVelToggle2.setSelected(false);
            groupVelToggle3.setSelected(false);
            groupVelToggle.setStyle(null);
            groupVelToggle2.setStyle(null);
            groupVelToggle3.setStyle(null);
        }
        if (orderedWaveformPlots.size() > 0) {
            orderedWaveformPlots.values().forEach(CodaWaveformPlot::setGroupVelocityVisbility);
        } else {
            selectedSinglePlot.setGroupVelocityVisbility();
            selectedSinglePlot.getPlotLayoutJSON();
        }
    };

    private final EventHandler<InputEvent> windowLineToggleAction = event -> {
        ToggleButton btnClicked = (ToggleButton) event.getSource();
        if (btnClicked != null && btnClicked.isSelected()) {
            windowLineToggle.setSelected(true);
            windowLineToggle2.setSelected(true);
            windowLineToggle3.setSelected(true);
            windowLineToggle.setStyle("-fx-background-color: DarkSeaGreen");
            windowLineToggle2.setStyle("-fx-background-color: DarkSeaGreen");
            windowLineToggle3.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            windowLineToggle.setSelected(false);
            windowLineToggle2.setSelected(false);
            windowLineToggle3.setSelected(false);
            windowLineToggle.setStyle(null);
            windowLineToggle2.setStyle(null);
            windowLineToggle3.setStyle(null);
        }
        if (orderedWaveformPlots.size() > 0) {
            orderedWaveformPlots.values().forEach(CodaWaveformPlot::setWindowLineVisbility);
        } else {
            selectedSinglePlot.setWindowLineVisbility();
        }
    };

    private final EventHandler<InputEvent> clickPickToggleAction = event -> {
        clickToPickModeBoolean = !clickToPickModeBoolean;
        clickToPickMode.setSelected(clickToPickModeBoolean);
        clickToPickMode2.setSelected(clickToPickModeBoolean);
        clickToPickMode3.setSelected(clickToPickModeBoolean);
        if (clickToPickModeBoolean) {
            clickToPickMode.setStyle("-fx-background-color: DarkSeaGreen");
            clickToPickMode2.setStyle("-fx-background-color: DarkSeaGreen");
            clickToPickMode3.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            clickToPickMode.setStyle(null);
            clickToPickMode2.setStyle(null);
            clickToPickMode3.setStyle(null);
        }
        if (orderedWaveformPlots.size() > 0) {
            orderedWaveformPlots.values().forEach(plot -> plot.setClickPickingModeEnabled(clickToPickModeBoolean));
        }
        if (selectedSinglePlot != null) {
            selectedSinglePlot.setClickPickingModeEnabled(clickToPickModeBoolean);
        }
    };

    private final EventHandler<InputEvent> syncZoomToggleAction = event -> {
        syncZoomModeBoolean = !syncZoomModeBoolean;
        syncZoomMode.setSelected(syncZoomModeBoolean);
        syncZoomMode2.setSelected(syncZoomModeBoolean);
        if (syncZoomModeBoolean) {
            syncZoomMode.setStyle("-fx-background-color: DarkSeaGreen");
            syncZoomMode2.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            syncZoomMode.setStyle(null);
            syncZoomMode2.setStyle(null);
        }
    };

    private final EventHandler<InputEvent> moveStartToggleAction = event -> {
        ToggleButton btnClicked = (ToggleButton) event.getSource();
        if (btnClicked != null && btnClicked.isSelected()) {
            moveStartToggle.setSelected(true);
            moveStartToggle2.setSelected(true);
            moveStartToggle3.setSelected(true);
            moveStartToggle.setStyle("-fx-background-color: DarkSeaGreen");
            moveStartToggle2.setStyle("-fx-background-color: DarkSeaGreen");
            moveStartToggle3.setStyle("-fx-background-color: DarkSeaGreen");
        } else {
            moveStartToggle.setSelected(false);
            moveStartToggle2.setSelected(false);
            moveStartToggle3.setSelected(false);
            moveStartToggle.setStyle(null);
            moveStartToggle2.setStyle(null);
            moveStartToggle3.setStyle(null);
        }

        if (orderedWaveformPlots.size() > 0) {
            orderedWaveformPlots.values().forEach(CodaWaveformPlot::setCodaStartLineVisbility);
        } else {
            selectedSinglePlot.setCodaStartLineVisbility();
        }
    };
    private DistanceCalculator distanceCalc;

    public CodaWaveformPlotManager(final WaveformClient waveformClient, final ShapeMeasurementClient shapeClient, final ParameterClient paramsClient, final PeakVelocityClient peakVelocityClient,
            final CertLeafletMapController certMap, final LeafletMapController cctMap, final MapPlottingUtilities mapPlotUtils, DistanceCalculator distanceCalc) {
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramsClient = paramsClient;
        this.peakVelocityClient = peakVelocityClient;
        this.cctMap = cctMap;
        this.certMap = certMap;
        this.mapPlotUtils = mapPlotUtils;
        this.distanceCalc = distanceCalc;
        this.borderPane = new BorderPane();
        this.waveformPanel = new VBox();
        this.plottedWaveformIds = new TreeMap<>();
        borderPane.setCenter(waveformPanel);
        multiPageToolbar = new ToolBar();
        multiFrequencyToolbar = new ToolBar();
        multiPlotToolbar = new ToolBar();
        pagingLabel = new Label("0/0");
        freqBandLabel = new Label("Frequency Band");

        contextMenu = new ContextMenu();
        contextMenu.getItems().add(clearUcsPick);
        radioIncludeBtn.setToggleGroup(includeToggleGroup);
        radioExcludeBtn.setToggleGroup(includeToggleGroup);
        radioExcludeBtn.selectedProperty().set(true);
        contextMenu.getItems().add(radioIncludeBtn);
        contextMenu.getItems().add(radioExcludeBtn);
        contextMenu.getItems().add(addByWaveformId);
        contextMenu.getItems().add(addByStation);
        contextMenu.getItems().add(addByEvent);
        contextMenu.getItems().add(addByEventAndStation);

        clearUcsPick.setOnAction(evt -> {
            if (selectedWaveform != null) {
                selectedWaveform.setAssociatedPicks(
                        selectedWaveform.getAssociatedPicks().stream().filter(p -> !PICK_TYPES.UCS.getPhase().equalsIgnoreCase(p.getPickName())).collect(Collectors.toList()));
                selectedWaveform.setUserStartTime(null);
                try {
                    waveformClient.postWaveform(selectedWaveform).subscribe(w -> {
                        Integer plotIndex = orderedWaveformIDs.get(w.getId());
                        if (plotIndex != null) {
                            CodaWaveformPlot waveformPlot = orderedWaveformPlots.get(plotIndex);
                            if (waveformPlot != null) {
                                waveformPlot.setWaveform(w);
                            } else {
                                selectedSinglePlot.setWaveform(w);
                            }
                        } else {
                            selectedSinglePlot.setWaveform(w);
                        }
                    });
                } catch (JsonProcessingException e) {
                    log.trace(e.getLocalizedMessage(), e);
                }
            }
        });

        addByWaveformId.setOnAction(evt -> {
            setUsedForWaveformById(selectedWaveform, radioIncludeBtn.isSelected());
            log.info(addByWaveformId.getText());
        });
        addByStation.setOnAction(evt -> {
            setUsedForStation(selectedWaveform, radioIncludeBtn.isSelected());
            log.info(addByStation.getText());
        });
        addByEvent.setOnAction(evt -> {
            setUsedForEvent(selectedWaveform, radioIncludeBtn.isSelected());
            log.info(addByEvent.getText());
        });
        addByEventAndStation.setOnAction(evt -> {
            setUsedForEventAndStation(selectedWaveform, radioIncludeBtn.isSelected());
            log.info(addByEventAndStation.getText());
        });

        waveformPanel.addEventFilter(MouseEvent.MOUSE_CLICKED, t -> {
            if (MouseButton.SECONDARY == t.getButton()) {
                selectedWaveform = getWaveformFromPosition(t.getSceneY());
                generateContextMenu(selectedWaveform);
                contextMenu.show(waveformPanel, t.getScreenX(), t.getScreenY());
            } else {
                contextMenu.hide();
            }
        });

        forwardButton.addEventHandler(MouseEvent.MOUSE_CLICKED, forwardAction::handle);
        backwardButton.addEventHandler(MouseEvent.MOUSE_CLICKED, backwardAction::handle);
        nextButton.addEventHandler(MouseEvent.MOUSE_CLICKED, nextAction::handle);
        prevButton.addEventHandler(MouseEvent.MOUSE_CLICKED, prevAction::handle);

        groupVelToggle.addEventHandler(MouseEvent.MOUSE_CLICKED, groupVelToggleAction::handle);
        groupVelToggle2.addEventHandler(MouseEvent.MOUSE_CLICKED, groupVelToggleAction::handle);
        groupVelToggle3.addEventHandler(MouseEvent.MOUSE_CLICKED, groupVelToggleAction::handle);
        groupVelToggle.setTooltip(new Tooltip(GROUP_VELOCITY_TOOLTIP));
        groupVelToggle2.setTooltip(new Tooltip(GROUP_VELOCITY_TOOLTIP));
        groupVelToggle3.setTooltip(new Tooltip(GROUP_VELOCITY_TOOLTIP));

        windowLineToggle.addEventHandler(MouseEvent.MOUSE_CLICKED, windowLineToggleAction::handle);
        windowLineToggle2.addEventHandler(MouseEvent.MOUSE_CLICKED, windowLineToggleAction::handle);
        windowLineToggle3.addEventHandler(MouseEvent.MOUSE_CLICKED, windowLineToggleAction::handle);
        windowLineToggle.setTooltip(new Tooltip(WINDOW_LINE_TOOLTIP));
        windowLineToggle2.setTooltip(new Tooltip(WINDOW_LINE_TOOLTIP));
        windowLineToggle3.setTooltip(new Tooltip(WINDOW_LINE_TOOLTIP));

        clickToPickMode.addEventHandler(MouseEvent.MOUSE_CLICKED, clickPickToggleAction::handle);
        clickToPickMode2.addEventHandler(MouseEvent.MOUSE_CLICKED, clickPickToggleAction::handle);
        clickToPickMode3.addEventHandler(MouseEvent.MOUSE_CLICKED, clickPickToggleAction::handle);
        clickToPickMode.setTooltip(new Tooltip(CLICK_TO_PICK_TOOLTIP));
        clickToPickMode2.setTooltip(new Tooltip(CLICK_TO_PICK_TOOLTIP));
        clickToPickMode3.setTooltip(new Tooltip(CLICK_TO_PICK_TOOLTIP));

        moveStartToggle.addEventHandler(MouseEvent.MOUSE_CLICKED, moveStartToggleAction::handle);
        moveStartToggle2.addEventHandler(MouseEvent.MOUSE_CLICKED, moveStartToggleAction::handle);
        moveStartToggle3.addEventHandler(MouseEvent.MOUSE_CLICKED, moveStartToggleAction::handle);
        moveStartToggle.setTooltip(new Tooltip(MOVE_START_TOOLTIP));
        moveStartToggle2.setTooltip(new Tooltip(MOVE_START_TOOLTIP));
        moveStartToggle3.setTooltip(new Tooltip(MOVE_START_TOOLTIP));

        syncZoomMode.addEventHandler(MouseEvent.MOUSE_CLICKED, syncZoomToggleAction::handle);
        syncZoomMode2.addEventHandler(MouseEvent.MOUSE_CLICKED, syncZoomToggleAction::handle);

        syncZoomMode.setTooltip(new Tooltip(ZOOM_SYNC_TOOLTIP));
        syncZoomMode2.setTooltip(new Tooltip(ZOOM_SYNC_TOOLTIP));

        syncZoomMode.selectedProperty().set(syncZoomModeBoolean);
        syncZoomMode2.selectedProperty().set(syncZoomModeBoolean);
        if (syncZoomModeBoolean) {
            syncZoomMode.setStyle("-fx-background-color: DarkSeaGreen");
            syncZoomMode2.setStyle("-fx-background-color: DarkSeaGreen");
        }

        try (InputStream is = this.getClass().getResourceAsStream(CLICK_TO_PICK_ICON)) {
            Image clickPickingIcon = new Image(is);
            createPickingIcon(clickToPickMode, clickPickingIcon, windowLineToggle);
            createPickingIcon(clickToPickMode2, clickPickingIcon, windowLineToggle2);
            createPickingIcon(clickToPickMode3, clickPickingIcon, windowLineToggle3);
        } catch (IOException | NullPointerException ex) {
            clickToPickMode.setText(CLICK_TO_PICK_LABEL);
            clickToPickMode2.setText(CLICK_TO_PICK_LABEL);
            clickToPickMode3.setText(CLICK_TO_PICK_LABEL);
        }

        multiPageToolbar.getItems().add(backwardButton);
        multiPageToolbar.getItems().add(pagingLabel);
        multiPageToolbar.getItems().add(forwardButton);
        multiPageToolbar.getItems().add(groupVelToggle);
        multiPageToolbar.getItems().add(syncZoomMode);
        multiPageToolbar.getItems().add(windowLineToggle);
        multiPageToolbar.getItems().add(clickToPickMode);
        multiPageToolbar.getItems().add(moveStartToggle);

        multiFrequencyToolbar.getItems().add(freqBandLabel);
        multiFrequencyToolbar.getItems().add(prevButton);
        multiFrequencyToolbar.getItems().add(nextButton);
        multiFrequencyToolbar.getItems().add(groupVelToggle2);
        multiFrequencyToolbar.getItems().add(windowLineToggle2);
        multiFrequencyToolbar.getItems().add(clickToPickMode2);
        multiFrequencyToolbar.getItems().add(moveStartToggle2);

        multiPlotToolbar.getItems().add(groupVelToggle3);
        multiPlotToolbar.getItems().add(syncZoomMode2);
        multiPlotToolbar.getItems().add(windowLineToggle3);
        multiPlotToolbar.getItems().add(clickToPickMode3);
        multiPlotToolbar.getItems().add(moveStartToggle3);

        final Font sizedFont = Font.font(forwardButton.getFont().getFamily(), 12f);
        forwardButton.setFont(sizedFont);
        backwardButton.setFont(sizedFont);
        nextButton.setFont(sizedFont);
        prevButton.setFont(sizedFont);
        groupVelToggle.setFont(sizedFont);
        groupVelToggle2.setFont(sizedFont);
        groupVelToggle3.setFont(sizedFont);
        syncZoomMode.setFont(sizedFont);
        syncZoomMode2.setFont(sizedFont);
        windowLineToggle.setFont(sizedFont);
        windowLineToggle2.setFont(sizedFont);
        windowLineToggle3.setFont(sizedFont);
        clickToPickMode.setFont(sizedFont);
        clickToPickMode2.setFont(sizedFont);
        clickToPickMode3.setFont(sizedFont);
        moveStartToggle.setFont(sizedFont);
        moveStartToggle2.setFont(sizedFont);
        moveStartToggle3.setFont(sizedFont);

        forwardButton.setFocusTraversable(false);
        backwardButton.setFocusTraversable(false);
        nextButton.setFocusTraversable(false);
        prevButton.setFocusTraversable(false);
        groupVelToggle.setFocusTraversable(false);
        groupVelToggle2.setFocusTraversable(false);
        groupVelToggle3.setFocusTraversable(false);
        syncZoomMode.setFocusTraversable(false);
        syncZoomMode2.setFocusTraversable(false);
        windowLineToggle.setFocusTraversable(false);
        windowLineToggle2.setFocusTraversable(false);
        windowLineToggle3.setFocusTraversable(false);
        clickToPickMode.setFocusTraversable(false);
        clickToPickMode2.setFocusTraversable(false);
        clickToPickMode3.setFocusTraversable(false);
        moveStartToggle.setFocusTraversable(false);
        moveStartToggle2.setFocusTraversable(false);
        moveStartToggle3.setFocusTraversable(false);
    }

    private void createPickingIcon(ToggleButton clickToPickButton, Image img, Region layoungBindingNode) {
        ImageView imgView = new ImageView(img);
        imgView.setPreserveRatio(true);
        imgView.fitHeightProperty().bind(layoungBindingNode.heightProperty());
        imgView.fitWidthProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.prefHeightProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.minHeightProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.maxHeightProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.prefWidthProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.minWidthProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.maxWidthProperty().bind(layoungBindingNode.heightProperty());
        clickToPickButton.setGraphic(imgView);
        clickToPickButton.setAlignment(Pos.CENTER);
    }

    private void setFrequencyDisplayText(Waveform wave) {
        if (wave != null && curEventStationWaveforms != null) {
            if (wave.isActive()) {
                freqBandLabel.setText("Frequency Band (" + (curFreqIndex + 1) + "/" + curEventStationWaveforms.size() + ") - Low: " + wave.getLowFrequency() + " High: " + wave.getHighFrequency());
            } else {
                freqBandLabel.setText("(Unused) Frequency Band Low: " + wave.getLowFrequency() + " High: " + wave.getHighFrequency());
            }
        } else {
            freqBandLabel.setText("No frequency bands to select");
        }
    }

    private void updatePlotAxes(PlotAxisChange change) {
        if (selectedSinglePlot != null) {
            this.setSavedAxisLimits(change.getAxisLimits());
            if (change.isReset()) {
                Platform.runLater(() -> {
                    selectedSinglePlot.replot();
                });
            }
        } else {
            this.setSavedAxisLimits(change.getAxisLimits());
        }

        if (!orderedWaveformPlots.isEmpty()) {
            orderedWaveformPlots.values().forEach(plot -> {
                if (syncZoomModeBoolean) {
                    if (!change.isReset()) {
                        // Get the min/max y values within the subsection of the xAxis
                        final double xMin = this.getSavedAxisLimits().getFirst().getMin();
                        final double xMax = this.getSavedAxisLimits().getFirst().getMax();

                        // The y-axis min and max are adjusted with 10% relative padding
                        final MinMax yzoomRange = plot.getMinMaxWithinSection(xMin, xMax);
                        double min = yzoomRange.getMin();
                        double max = yzoomRange.getMax();

                        plot.setAxisLimits(
                                new AxisLimits(Axis.Type.X, this.getSavedAxisLimits().getFirst().getMin(), this.getSavedAxisLimits().getFirst().getMax()),
                                    new AxisLimits(Axis.Type.Y, min, max));
                    } else {
                        plot.resetAxisLimits();
                    }
                }
                Platform.runLater(() -> {
                    plot.replot();
                });
            });
        }
    }

    private void plotWaveform(long waveformId) {
        clear();

        final List<Pair<Waveform, CodaWaveformPlot>> results = new ArrayList<>();
        final SyntheticCoda synth = waveformClient.getSyntheticFromWaveformId(waveformId).publishOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(10));
        if (synth != null && synth.getId() != null) {
            results.add(createPlot(synth));
        } else {
            results.add(createPlot(waveformClient.getWaveformFromId(waveformId).publishOn(Schedulers.boundedElastic()).block(Duration.ofSeconds(10))));
        }

        waveformPanel.getChildren().clear();

        final Pair<Waveform, CodaWaveformPlot> plotPair = results.get(0);
        final Waveform waveform = plotPair.getX();
        final CodaWaveformPlot plot = plotPair.getY();

        if (waveform != null) {
            final Collection<Icon> icons = mapWaveform(waveform);
            mappedIcons.addAll(icons);
            cctMap.addIcons(icons);
            certMap.addIcons(icons);
            if (plot != null) {
                plot.setMargin(null, null, null, null);
                plot.getxAxis().setText(TIME_SECONDS_FROM_ORIGIN);
                plot.attachToDisplayNode(waveformPanel);
                selectedSinglePlot = plot;
            }
        }

        if (plot != null) {
            plot.setClickPickingModeEnabled(clickToPickModeBoolean);
        }
    }

    private void setWaveformsByEventStation(long waveformId) {
        final List<Waveform> eventWaveforms = waveformClient.getSharedEventStationWaveformsById(waveformId).sort(eventStaFreqComparator).collectList().block(Duration.ofSeconds(10));

        if (eventWaveforms != null && !eventWaveforms.isEmpty() && eventWaveforms.get(0).getId() != null) {
            Waveform wave = eventWaveforms.parallelStream().filter(w -> w.getId() != null && w.getId() == waveformId).findAny().orElseGet(Waveform::new);
            curEventStationWaveforms = eventWaveforms;
            curFreqIndex = curEventStationWaveforms.indexOf(wave);

            setFrequencyDisplayText(wave);
            borderPane.setTop(multiFrequencyToolbar);
        }
    }

    private void clear() {
        final List<Icon> oldIcons = mappedIcons;
        mappedIcons = new ArrayList<>();
        cctMap.removeIcons(oldIcons);
        certMap.removeIcons(oldIcons);
        synchronized (bagLock) {
            plotBag.addAll(orderedWaveformPlots.values().stream().filter(plot -> {
                plot.setAxisChangeListener(null);
                plot.clear();
                return true;
            }).collect(Collectors.toList()));
        }
        orderedWaveformPlots.clear();
    }

    /**
     * This will provide the waveform id given the y coordinate within the
     * waveform panel. Since the waveform panel is a VBox, all plots are
     * positioned vertically and their id can be derived using the vertical
     * position to determine which plot was clicked over.
     *
     * @return
     */
    private Waveform getWaveformFromPosition(double yPos) {

        // Case where there's only one plot in the waveform panel
        if (curFreqIndex >= 0) {
            Waveform wave = curEventStationWaveforms.get(curFreqIndex);
            if (wave != null) {
                return wave;
            }
        }
        // Case where there are 2 or more plots in waveform panel
        if (waveformPanel != null && plottedWaveformIds.size() > 1 && waveformClient != null) {
            double totalHeight = borderPane.getHeight();
            double plotHeight = totalHeight / plottedWaveformIds.size();
            int plotIndex = (int) Math.floor(yPos / plotHeight);
            Long waveId = plottedWaveformIds.get(plotIndex);

            return waveformClient.getWaveformFromId(waveId).block(Duration.ofSeconds(5));
        }

        return null;
    }

    private void generateContextMenu(Waveform waveform) {
        if (waveform != null) {
            String eventId = waveform.getEvent().getEventId();
            String station = waveform.getStream().getStation().getStationName();
            String action = radioIncludeBtn.isSelected() ? "Include " : "Exclude ";

            addByWaveformId.setText(String.format("%s_%s_%s_%s_%s_", action, eventId, station, waveform.getLowFrequency(), waveform.getHighFrequency()));
            addByStation.setText(String.format("%s_station: %s", action, station));
            addByEvent.setText(String.format("%s_event: %s", action, eventId));
            addByEventAndStation.setText(String.format("%s_station: %s in event: %s", action, station, eventId));
        }
    }

    private void setUsedForWaveformById(final Waveform waveform, final boolean use) {
        if (waveform != null) {
            List<Long> waveId = new ArrayList<>();
            waveId.add(waveform.getId());
            waveform.setActive(use);
            setFrequencyDisplayText(waveform);
            waveformClient.setWaveformsActiveByIds(waveId, use);
        }
    }

    private void setUsedForEvent(final Waveform waveform, final boolean use) {
        if (waveform != null) {
            waveformClient.setWaveformsActiveByEventId(waveform.getEvent().getEventId(), use);
        }
    }

    private void setUsedForStation(final Waveform waveform, final boolean use) {
        if (waveform != null) {
            waveformClient.setWaveformsActiveByStationName(waveform.getStream().getStation().getStationName(), use);
        }
    }

    private void setUsedForEventAndStation(final Waveform waveform, final boolean use) {
        if (waveform != null) {
            waveformClient.setWaveformsActiveByStationNameAndEventId(waveform.getStream().getStation().getStationName(), waveform.getEvent().getEventId(), use);
        }
    }

    private List<Pair<Waveform, CodaWaveformPlot>> createSyntheticPlots(final List<SyntheticCoda> synthetics) {
        final List<Pair<Waveform, CodaWaveformPlot>> plots = new ArrayList<>(synthetics.size());
        for (final SyntheticCoda s : synthetics) {
            final Pair<Waveform, CodaWaveformPlot> plot = createPlot(s);
            plots.add(plot);
        }
        return plots;
    }

    private List<Pair<Waveform, CodaWaveformPlot>> createWaveformPlots(final List<Waveform> waveforms) {
        final List<Pair<Waveform, CodaWaveformPlot>> plots = new ArrayList<>(waveforms.size());
        for (final Waveform w : waveforms) {
            final Pair<Waveform, CodaWaveformPlot> plot = createPlot(w);
            plots.add(plot);
        }
        return plots;
    }

    private Pair<Waveform, CodaWaveformPlot> createPlot(final SyntheticCoda synth) {
        final CodaWaveformPlot plot = getOrCreatePlot();
        plot.setWaveform(synth);
        return new Pair<>(synth.getSourceWaveform(), plot);
    }

    private Pair<Waveform, CodaWaveformPlot> createPlot(final Waveform waveform) {
        final CodaWaveformPlot plot = getOrCreatePlot();
        plot.setWaveform(waveform);
        return new Pair<>(waveform, plot);
    }

    private CodaWaveformPlot getOrCreatePlot() {
        CodaWaveformPlot plot;
        synchronized (bagLock) {
            if (!plotBag.isEmpty()) {
                plot = plotBag.pop();
            } else {
                plot = new CodaWaveformPlot(waveformClient,
                                            shapeClient,
                                            paramsClient,
                                            peakVelocityClient,
                                            () -> groupVelToggle.isSelected(),
                                            () -> windowLineToggle.isSelected(),
                                            () -> moveStartToggle.isSelected(),
                                            distanceCalc);
            }
        }

        plot.setAxisChangeListener(axisChange -> {
            if (axisChange.getNewValue() instanceof PlotAxisChange) {
                updatePlotAxes((PlotAxisChange) axisChange.getNewValue());
            }
        });

        if (this.getSavedAxisLimits() != null) {
            plot.setAxisLimits(this.getSavedAxisLimits().getFirst(), this.getSavedAxisLimits().getSecond());
        } else {
            plot.resetAxisLimits();
        }

        return plot;
    }

    private Collection<Icon> mapWaveform(final Waveform waveform) {
        return Stream.of(waveform).filter(Objects::nonNull).flatMap(w -> {
            final List<Icon> icons = new ArrayList<>();
            if (w.getStream() != null && w.getStream().getStation() != null) {
                final Station station = w.getStream().getStation();
                icons.add(mapPlotUtils.createStationIconForeground(station));
            }
            if (w.getEvent() != null) {
                icons.add(mapPlotUtils.createEventIconForeground(w.getEvent()));
            }
            return icons.stream();
        }).collect(Collectors.toList());
    }

    public void setVisible(final boolean visible) {
        if (visible) {
            cctMap.addIcons(mappedIcons);
            certMap.addIcons(mappedIcons);
        } else {
            cctMap.removeIcons(mappedIcons);
            certMap.removeIcons(mappedIcons);
        }
    }

    public void resetAllAxes() {
        this.setSavedAxisLimits(null);
        if (!orderedWaveformPlots.isEmpty()) {
            orderedWaveformPlots.values().forEach(plot -> {
                plot.resetAxisLimits();
            });
        }
        if (selectedSinglePlot != null) {
            selectedSinglePlot.resetAxisLimits();
        }
    }

    public PairT<AxisLimits, AxisLimits> getSavedAxisLimits() {
        return savedAxisLimits;
    }

    public void setSavedAxisLimits(PairT<AxisLimits, AxisLimits> axisLimits) {
        this.savedAxisLimits = axisLimits;
    }

    public void setOrderedWaveformIDs(final List<Long> waveformIDs) {
        // FIXME: Handle concurrency issues w.r.t. to mid-layout updates to this
        orderedWaveformIDs = new HashMap<>(waveformIDs.size());
        allWaveformIDs.clear();

        for (int i = 0; i < waveformIDs.size(); i++) {
            orderedWaveformIDs.put(waveformIDs.get(i), i);
            allWaveformIDs.add(waveformIDs.get(i));
        }
        if (waveformIDs.size() > PAGE_SIZE) {
            final double page = waveformIDs.size() / (double) PAGE_SIZE;
            if (page % 1.0 > 0.001) {
                totalPages = (int) (page + 1.0);
            } else {
                totalPages = (int) (page);
            }
        } else {
            totalPages = 1;
        }
        adjustShowingToolbar(totalPages);

        currentPage = 0;

        loadWaveformsForPage(currentPage);
    }

    private void adjustShowingToolbar(final int totalPages) {
        if (totalPages > 1) {
            borderPane.setTop(multiPageToolbar);
        } else {
            borderPane.setTop(multiPlotToolbar);
        }
    }

    private void loadWaveformsForPage(final int pageNumber) {
        clear();

        final List<Pair<Waveform, CodaWaveformPlot>> results = new ArrayList<>();
        if (allWaveformIDs.size() == 1) {
            Optional<Long> firstElement = allWaveformIDs.stream().findFirst();
            if (firstElement.isPresent()) {
                setWaveformsByEventStation(firstElement.get());
                plotWaveform(firstElement.get());
            }
            return;
        } else {
            curEventStationWaveforms = null;
            curFreqIndex = -1;
            pagingLabel.setText(pageNumber + 1 + "/" + totalPages);
            final List<Long> pageIds = new ArrayList<>(PAGE_SIZE.intValue());
            int skipVal = pageNumber * PAGE_SIZE;
            if (skipVal != 0 && allWaveformIDs.size() > PAGE_SIZE && allWaveformIDs.size() - skipVal < PAGE_SIZE) {
                skipVal = allWaveformIDs.size() - PAGE_SIZE;
            }
            allWaveformIDs.stream().sequential().skip(skipVal).limit(PAGE_SIZE).forEach(pageIds::add);
            final List<SyntheticCoda> synthetics = waveformClient.getSyntheticsFromWaveformIds(pageIds)
                                                                 .filter(synth -> synth != null && synth.getId() != null)
                                                                 .collectList()
                                                                 .publishOn(Schedulers.boundedElastic())
                                                                 .block(Duration.ofSeconds(10));
            if (synthetics != null && !synthetics.isEmpty()) {
                pageIds.removeAll(synthetics.stream().map(synth -> synth.getSourceWaveform().getId()).collect(Collectors.toList()));
                results.addAll(createSyntheticPlots(synthetics));
            }

            final List<Waveform> waveforms = waveformClient.getWaveformsFromIds(pageIds)
                                                           .filter(waveform -> waveform != null && waveform.getId() != null)
                                                           .collectList()
                                                           .publishOn(Schedulers.boundedElastic())
                                                           .block(Duration.ofSeconds(10));
            if (waveforms != null && !waveforms.isEmpty()) {
                results.addAll(createWaveformPlots(waveforms));
            }
        }
        setPlots(results);
    }

    private void setPlots(final List<Pair<Waveform, CodaWaveformPlot>> plotPairs) {
        waveformPanel.getChildren().clear();
        plottedWaveformIds.clear();
        for (int i = 0; i < plotPairs.size(); i++) {
            final Pair<Waveform, CodaWaveformPlot> plotPair = plotPairs.get(i);
            final Waveform waveform = plotPair.getX();
            final CodaWaveformPlot plot = plotPair.getY();
            plottedWaveformIds.put(i, waveform.getId());
            if (waveform != null) {
                final Integer index = orderedWaveformIDs.get(waveform.getId());
                orderedWaveformPlots.put(index, plotPair.getY());
                final Collection<Icon> icons = mapWaveform(waveform);
                mappedIcons.addAll(icons);
                cctMap.addIcons(icons);
                certMap.addIcons(icons);
                if (plot != null) {
                    if (plotPairs.size() > 1) {
                        if (i == plotPairs.size() - 1) {
                            plot.setMargin(25, 30, 30, 0);
                            plot.getxAxis().setText(TIME_SECONDS_FROM_ORIGIN);
                        } else {
                            plot.getxAxis().setText("");
                            plot.setMargin(25, 20, 30, 0);
                        }
                    } else {
                        plot.setMargin(null, null, null, null);
                        plot.getxAxis().setText(TIME_SECONDS_FROM_ORIGIN);
                    }
                    plot.setClickPickingModeEnabled(clickToPickModeBoolean);
                    plot.attachToDisplayNode(waveformPanel);
                    plot.replot();
                }
            }
        }
    }

    public void triggerKeyEvent(final KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            backwardButton.requestFocus();
            backwardAction.handle(event);
        } else if (event.getCode() == KeyCode.RIGHT) {
            forwardButton.requestFocus();
            forwardAction.handle(event);
        } else if (event.getCode() == KeyCode.UP) {
            nextButton.requestFocus();
            nextAction.handle(event);
        } else if (event.getCode() == KeyCode.DOWN) {
            prevAction.handle(event);
            prevButton.requestFocus();
        }
    }

    public void exportScreenshots(final File folder) {
        String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
        SnapshotUtils.writePng(folder, new Pair<>(WAVEFORM_PREFIX, waveformPanel), timestamp);
        if (selectedSinglePlot != null) {
            exportSVG(selectedSinglePlot, folder + File.separator + WAVEFORM_PREFIX + selectedSinglePlot.getPlotIdentifier() + "_" + timestamp + ".svg");
        }
        for (CodaWaveformPlot wp : orderedWaveformPlots.values()) {
            String plotId = wp.getPlotIdentifier();
            if (plotId != null && !plotId.isEmpty()) {
                exportSVG(wp, folder + File.separator + WAVEFORM_PREFIX + plotId + "_" + timestamp + ".svg");
            } else {
                exportSVG(wp, folder + File.separator + WAVEFORM_PREFIX + SnapshotUtils.getTimestampWithLeadingSeparator() + ".svg");
            }
        }
    }

    private void exportSVG(CodaWaveformPlot plot, String path) {
        try {
            Files.write(Paths.get(path), plot.getSVG().getBytes());
        } catch (final IOException e) {
            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
        }
    }

    public void attachToDisplayNode(final Pane parent) {
        if (parent != null) {
            parent.getChildren().add(borderPane);
        }
    }

}
