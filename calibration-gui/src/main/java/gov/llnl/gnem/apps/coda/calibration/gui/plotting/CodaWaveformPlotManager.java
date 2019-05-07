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

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.PeakVelocityClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ShapeMeasurementClient;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.SyntheticCoda;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.scene.input.KeyCode;
import reactor.core.scheduler.Schedulers;

//TODO: Split this out into a few separate functional areas when time permits (i.e. CodaWaveformPlotGUI, CodaWaveformPlotManager, etc).
// As it currently is this class is pretty entangled.
public class CodaWaveformPlotManager extends JPanel {

    private static final Logger log = LoggerFactory.getLogger(CodaWaveformPlotManager.class);
    private static final long serialVersionUID = 1L;

    private WaveformClient waveformClient;
    private ShapeMeasurementClient shapeClient;
    private ParameterClient paramsClient;
    private PeakVelocityClient peakVelocityClient;
    private GeoMap map;
    private MapPlottingUtilities mapPlotUtils;
    private JToolBar toolbar;
    private JPanel waveformPanel;
    private List<Icon> mappedIcons = new ArrayList<>();
    private Map<Long, Integer> orderedWaveformIDs = new HashMap<>();
    private SortedMap<Integer, CodaWaveformPlot> orderedWaveformPlots = new TreeMap<>();
    private List<Long> allWaveformIDs = new ArrayList<>();
    private GridLayout constraints = new GridLayout(0, 1);
    private Long pageSize = 5l;
    private int currentPage = 0;
    private int totalPages = 0;
    private JLabel pagingLabel;

    private final Action forwardAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentPage < totalPages) {
                currentPage++;
                loadWaveformsForPage(currentPage);
            }
        }
    };

    private final Action backwardAction = new AbstractAction() {
        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentPage > 0) {
                currentPage--;
                loadWaveformsForPage(currentPage);
            }
        }
    };

    public CodaWaveformPlotManager(WaveformClient waveformClient, ShapeMeasurementClient shapeClient, ParameterClient paramsClient, PeakVelocityClient peakVelocityClient, GeoMap map,
            MapPlottingUtilities mapPlotUtils) {
        this.waveformClient = waveformClient;
        this.shapeClient = shapeClient;
        this.paramsClient = paramsClient;
        this.peakVelocityClient = peakVelocityClient;
        this.map = map;
        this.mapPlotUtils = mapPlotUtils;
        SwingUtilities.invokeLater(() -> {
            this.setLayout(new BorderLayout());
            this.waveformPanel = new JPanel();
            waveformPanel.setLayout(constraints);
            this.add(waveformPanel, BorderLayout.CENTER);
            toolbar = new JToolBar();
            toolbar.setFloatable(false);

            pagingLabel = new JLabel("0/0");
            JButton forwardButton = new JButton(">");
            JButton backButton = new JButton("<");

            forwardButton.setBorderPainted(false);
            forwardButton.setFocusPainted(false);
            forwardButton.setContentAreaFilled(false);

            backButton.setBorderPainted(false);
            backButton.setFocusPainted(false);
            backButton.setContentAreaFilled(false);

            forwardButton.addActionListener(action -> {
                forwardAction.actionPerformed(action);
            });

            backButton.addActionListener(action -> {
                backwardAction.actionPerformed(action);
            });

            toolbar.add(backButton);
            toolbar.add(pagingLabel);
            toolbar.add(forwardButton);

            Font sizedFont = forwardButton.getFont().deriveFont(24f);
            forwardButton.setFont(sizedFont);
            backButton.setFont(sizedFont);
        });
    }

    private void clear() {
        List<Icon> oldIcons = mappedIcons;
        mappedIcons = new ArrayList<>();
        map.removeIcons(oldIcons);
        waveformPanel.removeAll();
    }

    private List<Pair<Waveform, CodaWaveformPlot>> createSyntheticPlots(List<SyntheticCoda> synthetics) {
        List<Pair<Waveform, CodaWaveformPlot>> plots = new ArrayList<>(synthetics.size());
        for (SyntheticCoda s : synthetics) {
            Pair<Waveform, CodaWaveformPlot> plot = createPlot(s);
            plot = dropBox(plot);
            plots.add(plot);
        }
        return plots;
    }

    private List<Pair<Waveform, CodaWaveformPlot>> createWaveformPlots(List<Waveform> waveforms) {
        List<Pair<Waveform, CodaWaveformPlot>> plots = new ArrayList<>(waveforms.size());
        for (Waveform w : waveforms) {
            Pair<Waveform, CodaWaveformPlot> plot = createPlot(w);
            plot = dropBox(plot);
            plots.add(plot);
        }
        return plots;
    }

    private Pair<Waveform, CodaWaveformPlot> dropBox(Pair<Waveform, CodaWaveformPlot> plotPair) {
        CodaWaveformPlot plot = plotPair.getRight();
        if (plot != null) {
            plot.setVerticalOffset(5);
            plot.setBoxHeight(0);
            plot.setBoxWidth(0);
            plot.setShowBorder(false);
        }
        return new Pair<>(plotPair.getLeft(), plot);
    }

    private Pair<Waveform, CodaWaveformPlot> createPlot(SyntheticCoda synth) {
        CodaWaveformPlot plot = new CodaWaveformPlot(waveformClient, shapeClient, paramsClient, peakVelocityClient);
        plot.setWaveform(synth);
        return new Pair<>(synth.getSourceWaveform(), plot);
    }

    private Pair<Waveform, CodaWaveformPlot> createPlot(Waveform waveform) {
        CodaWaveformPlot plot = new CodaWaveformPlot(waveformClient, shapeClient, paramsClient, peakVelocityClient);
        plot.setWaveform(waveform);
        return new Pair<>(waveform, plot);
    }

    private Collection<Icon> mapWaveform(Waveform waveform) {
        return Stream.of(waveform).filter(Objects::nonNull).flatMap(w -> {
            List<Icon> icons = new ArrayList<>();
            if (w.getStream() != null && w.getStream().getStation() != null) {
                Station station = w.getStream().getStation();
                icons.add(mapPlotUtils.createStationIconForeground(station));
            }
            if (w.getEvent() != null) {
                icons.add(mapPlotUtils.createEventIconForeground(w.getEvent()));
            }
            return icons.stream();
        }).collect(Collectors.toList());
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            map.addIcons(mappedIcons);
        } else {
            map.removeIcons(mappedIcons);
        }
    }

    public void setOrderedWaveformIDs(List<Long> waveformIDs) {
        //FIXME: Handle concurrency issues w.r.t. to mid-layout updates to this
        orderedWaveformIDs = new HashMap<>(waveformIDs.size());
        allWaveformIDs.clear();

        for (int i = 0; i < waveformIDs.size(); i++) {
            orderedWaveformIDs.put(waveformIDs.get(i), i);
            allWaveformIDs.add(waveformIDs.get(i));
        }
        if (waveformIDs.size() > pageSize) {
            this.add(toolbar, BorderLayout.NORTH);
            totalPages = (int) ((waveformIDs.size() - 1) / pageSize);
        } else {
            this.remove(toolbar);
            totalPages = 1;
        }
        currentPage = 0;

        loadWaveformsForPage(currentPage);
    }

    private void loadWaveformsForPage(int pageNumber) {
        clear();
        List<Pair<Waveform, CodaWaveformPlot>> results = new ArrayList<>();
        if (allWaveformIDs.size() == 1) {
            SyntheticCoda synth = waveformClient.getSyntheticFromWaveformId(allWaveformIDs.get(0)).publishOn(Schedulers.elastic()).block(Duration.ofSeconds(10));
            if (synth != null && synth.getId() != null) {
                results.add(createPlot(synth));
            } else {
                results.add(createPlot(waveformClient.getWaveformFromId(allWaveformIDs.get(0)).publishOn(Schedulers.elastic()).block(Duration.ofSeconds(10))));
            }
        } else {
            pagingLabel.setText(pageNumber + 1 + "/" + (totalPages + 1));
            List<Long> pageIds = new ArrayList<>(pageSize.intValue());
            long skipVal = pageNumber * pageSize;
            if (skipVal != 0 && allWaveformIDs.size() > pageSize && allWaveformIDs.size() - skipVal < pageSize) {
                skipVal = allWaveformIDs.size() - pageSize;
            }
            allWaveformIDs.stream().sequential().skip(skipVal).limit(pageSize).forEach(pageIds::add);
            List<SyntheticCoda> synthetics = waveformClient.getSyntheticsFromWaveformIds(pageIds)
                                                           .filter(synth -> synth != null && synth.getId() != null)
                                                           .collectList()
                                                           .publishOn(Schedulers.elastic())
                                                           .block(Duration.ofSeconds(10));
            if (synthetics != null && !synthetics.isEmpty()) {
                pageIds.removeAll(synthetics.stream().map(synth -> synth.getSourceWaveform().getId()).collect(Collectors.toList()));
                results.addAll(createSyntheticPlots(synthetics));
            }

            List<Waveform> waveforms = waveformClient.getWaveformsFromIds(pageIds)
                                                     .filter(waveform -> waveform != null && waveform.getId() != null)
                                                     .collectList()
                                                     .publishOn(Schedulers.elastic())
                                                     .block(Duration.ofSeconds(10));
            if (waveforms != null && !waveforms.isEmpty()) {
                results.addAll(createWaveformPlots(waveforms));
            }
        }
        setPlots(results);
        this.revalidate();
    }

    private void setPlots(List<Pair<Waveform, CodaWaveformPlot>> plotPairs) {
        orderedWaveformPlots.clear();
        for (Pair<Waveform, CodaWaveformPlot> plotPair : plotPairs) {
            Waveform waveform = plotPair.getLeft();
            if (waveform != null) {
                Integer index = orderedWaveformIDs.get(waveform.getId());
                orderedWaveformPlots.put(index, plotPair.getRight());
                Collection<Icon> icons = mapWaveform(waveform);
                mappedIcons.addAll(icons);
                map.addIcons(icons);
            }
        }
        for (CodaWaveformPlot plot : orderedWaveformPlots.values()) {
            this.waveformPanel.add(plot);
        }
    }

    public void triggerKeyEvent(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == KeyCode.LEFT) {
            SwingUtilities.invokeLater(() -> backwardAction.actionPerformed(new ActionEvent(event.getSource(), KeyEvent.KEY_RELEASED, "BackwardAction")));
        } else if (event.getCode() == KeyCode.RIGHT) {
            SwingUtilities.invokeLater(() -> forwardAction.actionPerformed(new ActionEvent(event.getSource(), KeyEvent.KEY_RELEASED, "ForwardAction")));
        }
    }
}
