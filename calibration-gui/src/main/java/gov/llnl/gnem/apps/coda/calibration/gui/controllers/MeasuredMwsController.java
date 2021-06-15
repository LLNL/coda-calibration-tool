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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.CalibrationClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.EventClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.ParameterClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.ParamExporter;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.MapPlottingUtilities;
import gov.llnl.gnem.apps.coda.calibration.gui.plotting.SpectralPlot;
import gov.llnl.gnem.apps.coda.calibration.gui.util.FileDialogs;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwDetails;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MeasuredMwReportByEvent;
import gov.llnl.gnem.apps.coda.calibration.model.domain.Spectra;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SpectraMeasurement;
import gov.llnl.gnem.apps.coda.common.gui.controllers.ProgressGui;
import gov.llnl.gnem.apps.coda.common.gui.data.client.api.WaveformClient;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressListener;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressMonitor;
import gov.llnl.gnem.apps.coda.common.mapping.api.GeoMap;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import llnl.gnem.core.gui.plotting.api.Axis;
import llnl.gnem.core.gui.plotting.api.Axis.TickFormat;
import llnl.gnem.core.gui.plotting.api.PlotFactory;
import llnl.gnem.core.gui.plotting.plotly.BasicAxis;
import reactor.core.scheduler.Schedulers;

@Component
public class MeasuredMwsController extends AbstractMeasurementController {

    private static final Logger log = LoggerFactory.getLogger(MeasuredMwsController.class);

    private static final String X_AXIS_LABEL = "center freq (Hz)";

    private static final String DISPLAY_NAME = "Measured_Mws";

    private final CalibrationClient calibrationClient;
    private final ParamExporter paramExporter;

    private final Map<String, List<Spectra>> fitSpectra = new HashMap<>();
    private ProgressGui progressGui;

    private final ThreadPoolExecutor exec = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>(), r -> {
        final Thread thread = new Thread(r);
        thread.setName("MeasureMwController");
        thread.setDaemon(true);
        return thread;
    }, new ThreadPoolExecutor.DiscardPolicy());

    @FXML
    private StackPane measuredMws;

    @FXML
    private StackPane spectraPlotPane;

    private final List<MeasuredMwDetails> mwDetails = new ArrayList<>();

    private MeasuredMwReportByEvent mfs = new MeasuredMwReportByEvent();

    @Autowired
    private MeasuredMwsController(final SpectraClient spectraClient, final ParameterClient paramClient, final WaveformClient waveformClient, final SymbolStyleMapFactory styleFactory, final GeoMap map,
            final MapPlottingUtilities iconFactory, final EventBus bus, final ParamExporter paramExporter, final CalibrationClient calibrationClient, final EventClient referenceEventClient,
            final PlotFactory plotFactory) {
        super(spectraClient, paramClient, referenceEventClient, waveformClient, styleFactory, map, iconFactory, plotFactory, bus);
        this.calibrationClient = calibrationClient;
        this.paramExporter = paramExporter;
    }

    @Override
    @FXML
    public void initialize() {
        spectraPlotPanel = measuredMws;
        super.initialize();

        final ProgressMonitor pm = new ProgressMonitor("Measuring Mws", new ProgressListener() {
            @Override
            public double getProgress() {
                return -1d;
            }
        });
        progressGui = new ProgressGui();
        progressGui.addProgressMonitor(pm);

        progressGui.initModality(Modality.NONE);
        progressGui.setAlwaysOnTop(true);

        final SpectraPlotController spectra = new SpectraPlotController(SpectraMeasurement::getPathAndSiteCorrected);
        final SpectralPlot plot = spectra.getSpectralPlot();
        plot.getSubplot().addPlotObjectObserver(getPlotpointObserver(spectra::getSpectraMeasurementMap));
        plot.setLabels("Moment Rate Spectra", X_AXIS_LABEL, "log10(dyne-cm)");
        final Axis rightAxis = new BasicAxis(Axis.Type.Y_RIGHT, "Mw");
        rightAxis.setTickFormat(TickFormat.LOG10_DYNE_CM_TO_MW);
        plot.getSubplot().addAxes(rightAxis);
        spectra.setShowCornerFrequencies(true);
        spectra.setYAxisResizable(true);
        spectra.setShouldShowFits(true);
        spectraPlotPane.getChildren().add(plot);

        spectraControllers.add(spectra);
    }

    @Override
    protected String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    protected List<Spectra> getFitSpectra() {
        return new ArrayList<>(fitSpectra.get(evidCombo.getSelectionModel().getSelectedItem()));
    }

    @Override
    protected void setActive(final Set<Waveform> waveforms, final List<Point2D> points, final boolean active, final BiConsumer<List<Point2D>, Boolean> activationFunc) {
        waveformClient.setWaveformsActiveByIds(waveforms.stream().map(w -> w.setActive(active)).map(Waveform::getId).collect(Collectors.toList()), active)
                      .subscribe(s -> activationFunc.accept(points, active));
    }

    @Override
    protected void preloadData() {
        spectralMeasurements.clear();
        fitSpectra.clear();
        mwDetails.clear();
        mfs = calibrationClient.makeMwMeasurements(Boolean.TRUE)
                               .doOnError(err -> log.trace(err.getMessage(), err))
                               .doFinally(s -> Platform.runLater(() -> progressGui.hide()))
                               .block(Duration.of(1000l, ChronoUnit.SECONDS));
        if (mfs != null) {
            fitSpectra.putAll(mfs.getFitSpectra());

            final List<MeasuredMwDetails> refEvs = referenceEventClient.getMeasuredEventDetails()
                                                                       .filter(ev -> ev.getEventId() != null)
                                                                       .collect(Collectors.toList())
                                                                       .subscribeOn(Schedulers.boundedElastic())
                                                                       .block(Duration.ofSeconds(10l));
            final Collection<MeasuredMwDetails> measMws = mfs.getMeasuredMwDetails().values();

            //Not terribly efficient but this list should never be huge so eh...
            for (final MeasuredMwDetails ref : refEvs) {
                for (final MeasuredMwDetails meas : measMws) {
                    if (meas.getEventId().equals(ref.getEventId())) {
                        meas.setRefMw(ref.getRefMw());
                        meas.setRefApparentStressInMpa(ref.getRefApparentStressInMpa());
                        meas.setValMw(ref.getValMw());
                        meas.setValApparentStressInMpa(ref.getValApparentStressInMpa());
                        break;
                    }
                }
            }

            mwDetails.addAll(measMws);
        }
    }

    @Override
    protected void reloadData() {
        try {
            progressGui.show();
            progressGui.toFront();
            exec.submit(super::reloadData);
        } catch (final RejectedExecutionException e) {
            progressGui.hide();
        }
    }

    @Override
    protected List<SpectraMeasurement> getSpectraData() {
        return mfs.getSpectraMeasurements()
                  .values()
                  .stream()
                  .flatMap(List::stream)
                  .filter(Objects::nonNull)
                  .filter(spectra -> spectra.getWaveform() != null && spectra.getWaveform().getEvent() != null && spectra.getWaveform().getStream() != null)
                  .map(SpectraMeasurement::new)
                  .collect(Collectors.toList());
    }

    public void exportMws() {
        Platform.runLater(() -> {
            final File file = FileDialogs.openFileSaveDialog(getDisplayName(), ".json", spectraPlotPanel.getScene().getWindow());
            if (file != null && FileDialogs.ensureFileIsWritable(file)) {
                final String filePath = file.getAbsolutePath();
                paramExporter.writeMeasuredMws(Paths.get(FilenameUtils.getFullPath(filePath)), FilenameUtils.getName(filePath), mwParameters);
            }
        });
    }

    @Override
    protected void runGuiUpdate(final Runnable runnable) throws InvocationTargetException, InterruptedException {
        Platform.runLater(runnable);
    }

    @Override
    protected List<MeasuredMwDetails> getEvents() {
        return mwDetails;
    }
}
