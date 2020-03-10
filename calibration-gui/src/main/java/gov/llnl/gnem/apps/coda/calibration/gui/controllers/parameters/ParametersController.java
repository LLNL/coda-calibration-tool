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
package gov.llnl.gnem.apps.coda.calibration.gui.controllers.parameters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.gui.controllers.RefreshableController;
import gov.llnl.gnem.apps.coda.calibration.gui.events.ParametersLoadedEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.GvDataChangeEvent;
import gov.llnl.gnem.apps.coda.calibration.model.messaging.ShapeConstraintsChangeEvent;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.StackPane;

@Component
public class ParametersController implements RefreshableController {

    @FXML
    private StackPane parameters;

    @FXML
    private SharedBandController sharedBandController;

    @FXML
    private ModelController modelController;

    @FXML
    private SiteBandController siteBandController;

    @FXML
    private VelocityConfigurationController velocityConfigController;

    @FXML
    private ShapeConfigurationController shapeConfigController;

    private EventBus bus;

    @Autowired
    public ParametersController(EventBus bus) {
        this.bus = bus;
        this.bus.register(this);
    }

    @Subscribe
    private void listener(GvDataChangeEvent event) {
        velocityConfigController.requestData();
    }

    @Subscribe
    private void listener(ShapeConstraintsChangeEvent event) {
        shapeConfigController.requestData();
    }

    @Subscribe
    private void listener(ParametersLoadedEvent event) {
        reloadData();
    }

    @FXML
    private void reloadData(ActionEvent e) {
        reloadData();
    }

    private void reloadData() {
        sharedBandController.requestData();
        modelController.requestData();
        siteBandController.requestData();
        velocityConfigController.requestData();
        shapeConfigController.requestData();
    }

    @Override
    public Runnable getRefreshFunction() {
        return () -> reloadData();
    }    
}