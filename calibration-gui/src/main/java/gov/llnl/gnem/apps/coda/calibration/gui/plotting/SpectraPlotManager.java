/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318
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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.gui.plotting.PlotPoint;
import gov.llnl.gnem.apps.coda.common.gui.util.SnapshotUtils;
import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TabPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.DirectoryChooser;

public class SpectraPlotManager {

    private static final String X_AXIS_LABEL = "center freq (Hz)";
    private static final String Y_AXIS_LABEL = "log10(amplitude)";
    private static final String AVG_LINE_LABEL = "Avg.";
    private static final Logger log = LoggerFactory.getLogger(SpectraPlotManager.class);

    private SpectralPlot combinedPlot;
    private SpectralPlot denomPlot;
    private SpectralPlot numerPlot;
    private EventPair eventPair;

    private List<PlotPoint> numeratorPlotPoints;
    private List<PlotPoint> denominatorPlotPoints;

    @FXML
    private StackPane rootPane;

    @FXML
    private TabPane mainTabPane;

    @FXML
    private Pane combinedPlotPane;

    @FXML
    private Pane splitPlotPane;

    @FXML
    private SplitPane mainSplitPane;

    @FXML
    private Pane numerPane;

    @FXML
    private Pane denomPane;

    @FXML
    private Button snapshotButton;

    final Label label = new Label("\uE3B0");

    private final DirectoryChooser screenshotFolderChooser = new DirectoryChooser();

    public SpectraPlotManager(EventPair eventPair, List<PlotPoint> numeratorPlotPoints, List<PlotPoint> denominatorPlotPoints) {
        this.eventPair = eventPair;
        this.numeratorPlotPoints = numeratorPlotPoints;
        this.denominatorPlotPoints = denominatorPlotPoints;
    }

    public List<PlotPoint> getNumeratorPlotPoints() {
        return this.numeratorPlotPoints;
    }

    public List<PlotPoint> getDenominatorPlotPoints() {
        return this.denominatorPlotPoints;
    }

    public List<PlotPoint> getCombinedPlotPoints() {
        List<PlotPoint> combinedPlotPoints = new ArrayList<>(numeratorPlotPoints);
        combinedPlotPoints.addAll(denominatorPlotPoints);
        return combinedPlotPoints;
    }

    public SpectralPlot plotSpectra(List<PlotPoint> plotPoints, String plotTitle, Pane parent) {
        if (plotPoints != null && plotPoints.size() > 1) {
            SpectralPlot plot = new SpectralPlot();
            setDisplayText(plot, plotTitle);
            plot.plotXYdata(plotPoints, null, null, AVG_LINE_LABEL);
            plot.setLabels(plotTitle, X_AXIS_LABEL, Y_AXIS_LABEL);

            Platform.runLater(() -> {
                parent.getChildren().clear();
                plot.getSubplot().attachToDisplayNode(parent);
            });

            return plot;
        }

        return null;
    }

    public void plotSpectraPlots() {
        if (eventPair != null) {
            if (!getCombinedPlotPoints().isEmpty()) {
                combinedPlot = plotSpectra(getCombinedPlotPoints(), String.format("Event_Pair_%s_%s", eventPair.getX().getEventId(), eventPair.getY().getEventId()), combinedPlotPane);
            }
            if (!getNumeratorPlotPoints().isEmpty() && !getDenominatorPlotPoints().isEmpty()) {
                numerPlot = plotSpectra(getNumeratorPlotPoints(), String.format("Numerator Event %s", eventPair.getX().getEventId()), numerPane);
                denomPlot = plotSpectra(getDenominatorPlotPoints(), String.format("Denominator Event %s", eventPair.getY().getEventId()), denomPane);
            }
        }
    }

    public void setDisplayText(SpectralPlot plot, String title) {
        plot.getSubplot().getTitle().setText(title);
    }

    public boolean combinedPlotSelected() {
        if (mainTabPane != null) {
            return mainTabPane.getSelectionModel().isSelected(1);
        }
        return false;
    }

    public void exportScreenshots(final File folder) {
        String timestamp = SnapshotUtils.getTimestampWithLeadingSeparator();
        boolean isCombinedPlot = combinedPlotSelected();

        Pane plotPane = splitPlotPane;
        if (isCombinedPlot) {
            plotPane = combinedPlotPane;
        }

        String combinedName = String.format("Combined_Plot_For_%s_%s", eventPair.getX().getEventId(), eventPair.getY().getEventId());
        String splitname = String.format("Spectra_Plots_%s_%s", eventPair.getX().getEventId(), eventPair.getY().getEventId());
        String numerName = String.format("Numerator_Spectra_Plot_%s", eventPair.getX().getEventId());
        String denomName = String.format("Denominator_Spectra_Plot_%s", eventPair.getY().getEventId());

        if (isCombinedPlot && combinedPlot != null) {
            SnapshotUtils.writePng(folder, new Pair<>(combinedName + "_", plotPane), timestamp);
            exportSVG(combinedPlot, folder + File.separator + combinedName + "_" + timestamp + ".svg");
        } else if (numerPlot != null && denomPlot != null) {
            SnapshotUtils.writePng(folder, new Pair<>(splitname + "_", plotPane), timestamp);
            exportSVG(numerPlot, folder + File.separator + numerName + "_" + timestamp + ".svg");
            exportSVG(denomPlot, folder + File.separator + denomName + "_" + timestamp + ".svg");
        }
    }

    public void exportSVG(SpectralPlot plot, String path) {
        try {
            Files.write(Paths.get(path), plot.getSubplot().getSVG().getBytes());
        } catch (final IOException e) {
            log.error("Error attempting to write plots for controller : {}", e.getLocalizedMessage(), e);
        }
    }

    @FXML
    private void screenshotPlots(final ActionEvent e) {
        final File folder = screenshotFolderChooser.showDialog(rootPane.getScene().getWindow());
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
    public void initialize() {
        plotSpectraPlots();

        mainTabPane.getSelectionModel().selectFirst();

        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        snapshotButton.setGraphic(label);
        snapshotButton.setContentDisplay(ContentDisplay.CENTER);
    }
}
