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
package gov.llnl.gnem.apps.coda.envelope.gui.controllers;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.math3.util.Precision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.common.gui.util.CellBindingUtils;
import gov.llnl.gnem.apps.coda.common.gui.util.MaybeNumericStringComparator;
import gov.llnl.gnem.apps.coda.common.gui.util.NumberFormatFactory;
import gov.llnl.gnem.apps.coda.common.gui.util.TableUtils;
import gov.llnl.gnem.apps.coda.envelope.gui.data.api.EnvelopeParamsClient;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeBandParameters;
import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeJobConfiguration;
import gov.llnl.gnem.apps.coda.envelope.model.domain.SpacingType;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import reactor.core.publisher.Mono;

@Component
public class EnvelopeParamsController {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeParamsController.class);

    private static final Double MIN_PRECISION = 1E-4;

    private EventBus bus;

    @FXML
    private TextField minFreqField;

    @FXML
    private TextField maxFreqField;

    @FXML
    private TextField overlapField;

    @FXML
    private TextField spacingField;

    @FXML
    private ChoiceBox<String> spacingTypeField;

    @FXML
    private TextField bandNumField;

    @FXML
    private TableView<EnvelopeBandParameters> bandView;

    @FXML
    private TableColumn<EnvelopeBandParameters, String> lowFreqCol;

    @FXML
    private TableColumn<EnvelopeBandParameters, String> highFreqCol;

    @FXML
    private TableColumn<EnvelopeBandParameters, String> smoothingCol;

    private ObservableList<EnvelopeBandParameters> bands = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    private EnvelopeJobConfiguration config;

    private EnvelopeParamsClient client;

    private NumberFormat numberFormat = NumberFormatFactory.sixDecimalOneLeadingZero();

    @Autowired
    public EnvelopeParamsController(EventBus bus, EnvelopeParamsClient client, EnvelopeJobConfiguration defaultConfig) {
        super();
        this.bus = bus;
        this.client = client;
        this.config = defaultConfig;
    }

    @FXML
    public void initialize() {
        Arrays.asList(SpacingType.values()).forEach(val -> spacingTypeField.getItems().add(val.name()));
        spacingTypeField.getSelectionModel().select(0);

        bandView.getSelectionModel().setCellSelectionEnabled(true);

        CellBindingUtils.attachEditableTextCellFactories(lowFreqCol, EnvelopeBandParameters::getLowFrequency, EnvelopeBandParameters::setLowFrequency, numberFormat);
        lowFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachEditableTextCellFactories(highFreqCol, EnvelopeBandParameters::getHighFrequency, EnvelopeBandParameters::setHighFrequency, numberFormat);
        highFreqCol.comparatorProperty().set(new MaybeNumericStringComparator());

        CellBindingUtils.attachEditableIntegerCellFactories(smoothingCol, EnvelopeBandParameters::getSmoothing, EnvelopeBandParameters::setSmoothing);
        smoothingCol.comparatorProperty().set(new MaybeNumericStringComparator());

        bandNumField.textProperty().bind(Bindings.size(bands).asString());

        Optional.ofNullable(config).ifPresent(x -> bands.addAll(x.getFrequencyBandConfiguration()));
        bandView.setItems(bands);
    }

    @FXML
    public void generateTable() {
        //TODO: populate these fields with defaults on edit commits when the new value is empty 
        if (!minFreqField.getText().isEmpty() && !maxFreqField.getText().isEmpty() && !overlapField.getText().isEmpty() && !spacingField.getText().isEmpty()) {
            try {
                //TODO: Validators for these fields
                Double minFreq = Double.valueOf(minFreqField.getText());
                Double maxFreq = Double.valueOf(maxFreqField.getText());
                Double overlap = Double.valueOf(overlapField.getText());
                Double spacing = Double.valueOf(spacingField.getText());

                if (minFreq > maxFreq) {
                    //error
                    minFreq = maxFreq;
                }

                if (spacing <= MIN_PRECISION) {
                    //error
                    spacing = MIN_PRECISION;
                    spacingField.setText(spacing.toString());
                }

                if (overlap < 0.) {
                    //error
                    overlap = 0.;
                    overlapField.setText(overlap.toString());
                } else if (overlap <= MIN_PRECISION) {
                    overlap = MIN_PRECISION;
                    overlapField.setText(overlap.toString());
                } else if (overlap >= 100.) {
                    overlap = 0.99;
                    overlapField.setText(overlap.toString());
                } else if (overlap >= 1.0) {
                    overlap = overlap / 100.0;
                    overlapField.setText(overlap.toString());
                }

                List<EnvelopeBandParameters> tmp = new ArrayList<>(bands);
                bands.clear();
                double last = minFreq;
                double current = minFreq;
                int reqBands = (int) (((maxFreq - minFreq) / spacing) + 0.5);
                for (int i = 0; i < reqBands; i++) {
                    current = current + spacing;
                    bands.add(
                            new EnvelopeBandParameters(Precision.round(Math.max(last - minFreq, minFreq), 6, BigDecimal.ROUND_HALF_DOWN),
                                                       Precision.round(Math.min(current - minFreq + (spacing * overlap), maxFreq), 6)));
                    last = current;
                }
                config.setFrequencyBandConfiguration(bands);

                postJob().doOnError(er -> {
                    bands.clear();
                    config.setFrequencyBandConfiguration(tmp);
                    bands.addAll(config.getFrequencyBandConfiguration());
                }).subscribe();
            } catch (NumberFormatException e) {
                // TODO: handle exception
            }
        }
    }

    @FXML
    private void postUpdate(CellEditEvent<?, ?> e) {
        if ("EDIT_COMMIT".equals(e.getEventType().getName())) {
            Object oldValue = e.getOldValue();
            List<EnvelopeBandParameters> tmp = new ArrayList<>(bands);
            config.setFrequencyBandConfiguration(bands);
            postJob().doOnError(er -> {
                bands.clear();
                bands.addAll(tmp);
                config.setFrequencyBandConfiguration(bands);
                TableUtils.revertValueInCell(e, oldValue);
            }).subscribe();
        }
    }

    private Mono<String> postJob() {
        return client.postEnvelopeJobConfiguration(config).doOnSuccess(s -> requestData());
    }

    private void requestData() {
        client.getEnvelopeJobConfiguration().filter(Objects::nonNull).doFinally((s) -> bandView.sort()).subscribe(value -> {
            if (value.getFrequencyBandConfiguration() != null) {
                bands.clear();
                config = value;
                bands.addAll(config.getFrequencyBandConfiguration());
            }
        }, err -> log.trace(err.getMessage(), err));
    }

    public EnvelopeJobConfiguration getConfig() {
        return config;
    }

    public EnvelopeParamsController setConfig(EnvelopeJobConfiguration config) {
        this.config = config;
        return this;
    }
}
