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

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.client.api.SpectraRatioClient;
import gov.llnl.gnem.apps.coda.calibration.gui.data.exporters.SpectraRatioExporter;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioMeasurementEvent;
import gov.llnl.gnem.apps.coda.common.gui.plotting.SymbolStyleMapFactory;
import gov.llnl.gnem.apps.coda.spectra.gui.events.RatioSegmentChangeEvent;
import gov.llnl.gnem.apps.coda.spectra.model.domain.util.SpectraRatiosReportByEventPair;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@Controller
public class RatioMeasurementsGui {

    private final CertLeafletMapController mapImpl;
    private final MapPlottingUtilities iconFactory;
    private SpectraClient spectraClient;
    private SpectraRatioExporter spectraRatioExporter;
    private SpectraRatioClient spectraRatioClient;
    private final SymbolStyleMapFactory symbolStyleFactory;

    private final Property<Boolean> shouldFocus = new SimpleBooleanProperty(false);
    private EventBus bus;

    @Autowired
    public RatioMeasurementsGui(final EventBus bus, final SymbolStyleMapFactory styleFactory, final CertLeafletMapController mapImpl, final MapPlottingUtilities iconFactory,
            SpectraClient spectraClient, SpectraRatioClient spectraRatioClient, SpectraRatioExporter spectraRatioExporter) {
        bus.register(this);
        this.bus = bus;
        this.mapImpl = mapImpl;
        this.iconFactory = iconFactory;
        this.spectraClient = spectraClient;
        this.spectraRatioExporter = spectraRatioExporter;
        this.spectraRatioClient = spectraRatioClient;
        this.symbolStyleFactory = styleFactory;
    }

    private void createSpectraPlotPopup(SpectraRatiosReportByEventPair ratioReport) {
        RatioMeasurementSpectraPlotManager spectraRatioPlotManager = new RatioMeasurementSpectraPlotManager(bus, symbolStyleFactory, mapImpl, iconFactory, spectraClient, spectraRatioExporter);
        spectraRatioPlotManager.setRatioMeasurements(ratioReport);

        final FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/SpectraRatioPlotGui.fxml"));
        fxmlLoader.setController(spectraRatioPlotManager);
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

    @Subscribe
    private void listener(final RatioMeasurementEvent event) {
        if (event != null && event.getRatioMeasurements() != null && !event.getRatioMeasurements().getReport().getData().isEmpty()) {
            CompletableFuture.runAsync(() -> {
                Platform.runLater(() -> {
                    createSpectraPlotPopup(event.getRatioMeasurements());
                });
            });
        }
    }

    @Subscribe
    private void listener(final RatioSegmentChangeEvent event) {
        spectraRatioClient.updateRatio(event.getRatioDetails().getRatio());
    }

    public Property<Boolean> focusProperty() {
        return shouldFocus;
    }
}
