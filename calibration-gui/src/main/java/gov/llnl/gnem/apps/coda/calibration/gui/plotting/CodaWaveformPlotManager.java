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

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.util.EventStaFreqStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.input.InputEvent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import reactor.core.scheduler.Schedulers;

//TODO: Split this out into a few separate functional areas when time permits (i.e. CodaWaveformPlotGUI, CodaWaveformPlotManager, etc).
// As it currently is this class is pretty entangled.
public class CodaWaveformPlotManager {

    private static final String TIME_SECONDS_FROM_ORIGIN = "Time (seconds from origin)";
    private static final String WAVEFORM_PREFIX = "Waveform_";
    private static final Logger log = LoggerFactory.getLogger(CodaWaveformPlotManager.class);
    private final WaveformClient waveformClient;
    private final ShapeMeasurementClient shapeClient;
    private final ParameterClient paramsClient;
    private final PeakVelocityClient peakVelocityClient;
    private final GeoMap map;
    private final MapPlottingUtilities mapPlotUtils;
    private final ToolBar multiPageToolbar;
    private final ToolBar multiFrequencyToolbar;
    private final ToolBar multiPlotToolbar;
    private List<Icon> mappedIcons = new ArrayList<>();
    private Map<Long, Integer> orderedWaveformIDs = new HashMap<>();
    private final SortedMap<Integer, CodaWaveformPlot> orderedWaveformPlots = new TreeMap<>();
    private CodaWaveformPlot selectedSinglePlot;
    private final Set<Long> allWaveformIDs = new LinkedHashSet<>();
    private static final Integer PAGE_SIZE = 5;
    private final Object bagLock = new Object();
    private final Stack<CodaWaveformPlot> plotBag = new Stack<>();
    private int currentPage = 0;
    private int totalPages = 0;
    private final Label pagingLabel;
    private Label freqBandLabel = new Label();
    private List<Waveform> curEventStationWaveforms;
    private int curFreqIndex = -1;
    private EventStaFreqStringComparator eventStaFreqComparator = new EventStaFreqStringComparator();

    private final BorderPane borderPane;
    private final VBox waveformPanel;

    final Button forwardButton = new Button(">");
    final Button backButton = new Button("<");
    final Button nextButton = new Button("↑");
    final Button prevButton = new Button("↓");
    final ToggleButton groupVelToggle1 = new ToggleButton("GV");
    final ToggleButton groupVelToggle2 = new ToggleButton("GV");
    final ToggleButton groupVelToggle3 = new ToggleButton("GV");

    private final EventHandler<InputEvent> forwardAction = event -> {
        if ((currentPage + 1) < totalPages) {
            currentPage++;
            loadWaveformsForPage(currentPage);
        }
    };

    private final EventHandler<InputEvent> backwardAction = event -> {
        if (currentPage > 0) {
            currentPage--;
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
            groupVelToggle1.setSelected(true);
            groupVelToggle2.setSelected(true);
            groupVelToggle3.setSelected(true);
        } else {
            groupVelToggle1.setSelected(false);
            groupVelToggle2.setSelected(false);
            groupVelToggle3.setSelected(false);
        }
        if (orderedWaveformPlots.size() > 0) {
            orderedWaveformPlots.values().forEach(CodaWaveformPlot::setGroupVelocityVisbility);
        } else {
            selectedSinglePlot.setGroupVelocityVisbility();
        }
    };

    public CodaWaveformPlotManager(final WaveformClient waveformClient, final ShapeMeasurementClient shapeClient, final ParameterClient paramsClient, final PeakVelocityClient peakVelocityClient,
            final GeoMap map, final MapPlottingUtilities mapPlotUtils) {
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramsClient = paramsClient;
        this.peakVelocityClient = peakVelocityClient;
        this.map = map;
        this.mapPlotUtils = mapPlotUtils;
        this.borderPane = new BorderPane();
        this.waveformPanel = new VBox();
        borderPane.setCenter(waveformPanel);
        multiPageToolbar = new ToolBar();
        multiFrequencyToolbar = new ToolBar();
        multiPlotToolbar = new ToolBar();

        pagingLabel = new Label("0/0");
        freqBandLabel = new Label("Frequency Band");

        forwardButton.addEventHandler(MouseEvent.MOUSE_CLICKED, forwardAction::handle);
        backButton.addEventHandler(MouseEvent.MOUSE_CLICKED, backwardAction::handle);
        nextButton.addEventHandler(MouseEvent.MOUSE_CLICKED, nextAction::handle);
        prevButton.addEventHandler(MouseEvent.MOUSE_CLICKED, prevAction::handle);
        groupVelToggle1.addEventHandler(MouseEvent.MOUSE_CLICKED, groupVelToggleAction::handle);
        groupVelToggle2.addEventHandler(MouseEvent.MOUSE_CLICKED, groupVelToggleAction::handle);
        groupVelToggle3.addEventHandler(MouseEvent.MOUSE_CLICKED, groupVelToggleAction::handle);

        multiPageToolbar.getItems().add(backButton);
        multiPageToolbar.getItems().add(pagingLabel);
        multiPageToolbar.getItems().add(forwardButton);
        multiPageToolbar.getItems().add(groupVelToggle1);

        multiFrequencyToolbar.getItems().add(freqBandLabel);
        multiFrequencyToolbar.getItems().add(prevButton);
        multiFrequencyToolbar.getItems().add(nextButton);
        multiFrequencyToolbar.getItems().add(groupVelToggle2);

        multiPlotToolbar.getItems().add(groupVelToggle3);

        final Font sizedFont = Font.font(forwardButton.getFont().getFamily(), 12f);
        forwardButton.setFont(sizedFont);
        backButton.setFont(sizedFont);
        nextButton.setFont(sizedFont);
        prevButton.setFont(sizedFont);
        groupVelToggle1.setFont(sizedFont);
        groupVelToggle2.setFont(sizedFont);
        groupVelToggle3.setFont(sizedFont);

        forwardButton.setFocusTraversable(false);
        backButton.setFocusTraversable(false);
        nextButton.setFocusTraversable(false);
        prevButton.setFocusTraversable(false);
        groupVelToggle1.setFocusTraversable(false);
        groupVelToggle2.setFocusTraversable(false);
        groupVelToggle3.setFocusTraversable(false);
    }

    private String getStationNetworkName(Waveform waveform) {
        if (waveform != null && waveform.getStream() != null && waveform.getStream().getStation() != null) {
            Station station = waveform.getStream().getStation();
            return station.getNetworkName() + station.getStationName();
        }
        return "";
    }

    private void setFrequencyDisplayText(Waveform wave) {
        if (wave != null && curEventStationWaveforms != null) {
            freqBandLabel.setText("Frequency Band (" + (curFreqIndex + 1) + "/" + curEventStationWaveforms.size() + ") - Low: " + wave.getLowFrequency() + " High: " + wave.getHighFrequency());
        } else {
            freqBandLabel.setText("No frequency bands to select");
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
        final Waveform waveform = plotPair.getLeft();
        final CodaWaveformPlot plot = plotPair.getRight();

        if (waveform != null) {

            final Collection<Icon> icons = mapWaveform(waveform);
            mappedIcons.addAll(icons);
            map.addIcons(icons);
            if (plot != null) {
                plot.setMargin(null, null, null, null);
                plot.getxAxis().setText(TIME_SECONDS_FROM_ORIGIN);
                plot.attachToDisplayNode(waveformPanel);
                selectedSinglePlot = plot;
            }
        }
    }

    private void setWaveformsByEventStation(long waveformId) {
        final List<Waveform> eventWaveforms = waveformClient.getActiveSharedEventStationWaveformsById(waveformId).sort(eventStaFreqComparator).collectList().block(Duration.ofSeconds(10));
        if (eventWaveforms != null && !eventWaveforms.isEmpty() && eventWaveforms.get(0).getId() != null) {
            Waveform waveform = eventWaveforms.parallelStream().filter(w -> w.getId() != null && w.getId() == waveformId).findAny().orElseGet(Waveform::new);
            curEventStationWaveforms = eventWaveforms;
            curFreqIndex = curEventStationWaveforms.indexOf(waveform);
            setFrequencyDisplayText(waveform);
            borderPane.setTop(multiFrequencyToolbar);
        }
    }

    private void clear() {
        final List<Icon> oldIcons = mappedIcons;
        mappedIcons = new ArrayList<>();
        map.removeIcons(oldIcons);
        synchronized (bagLock) {
            plotBag.addAll(orderedWaveformPlots.values().stream().filter(plot -> {
                plot.clear();
                return true;
            }).collect(Collectors.toList()));
        }
        orderedWaveformPlots.clear();
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
                plot = new CodaWaveformPlot(waveformClient, shapeClient, paramsClient, peakVelocityClient, () -> groupVelToggle1.isSelected());
            }
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
            map.addIcons(mappedIcons);
        } else {
            map.removeIcons(mappedIcons);
        }
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
        for (int i = 0; i < plotPairs.size(); i++) {
            final Pair<Waveform, CodaWaveformPlot> plotPair = plotPairs.get(i);
            final Waveform waveform = plotPair.getLeft();
            final CodaWaveformPlot plot = plotPair.getRight();
            if (waveform != null) {
                final Integer index = orderedWaveformIDs.get(waveform.getId());
                orderedWaveformPlots.put(index, plotPair.getRight());
                final Collection<Icon> icons = mapWaveform(waveform);
                mappedIcons.addAll(icons);
                map.addIcons(icons);
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
                    plot.attachToDisplayNode(waveformPanel);
                    plot.replot();
                }
            }
        }
    }

    public void triggerKeyEvent(final KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            backButton.requestFocus();
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
