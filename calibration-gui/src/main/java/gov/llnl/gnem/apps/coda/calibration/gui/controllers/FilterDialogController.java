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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/***
 * This is a front end GUI controller used to display and manage the filter
 * dialog that users will see when filtering table views. Used as a helper class
 * for the DataFilterController, it handles the display and actions of the
 * filter dialog.
 *
 * @see DataFilterController
 *
 * @author downie4
 *
 */

@Component
public class FilterDialogController {

    private static final Logger log = LoggerFactory.getLogger(FilterDialogController.class);

    @FXML
    protected Button clearFiltersBtn;

    @FXML
    protected Button filterBtn;

    @FXML
    private Button cancelBtn;

    @FXML
    protected RadioButton andOption;

    @FXML
    protected RadioButton orOption;

    @FXML
    protected GridPane fieldRowGrid;

    private int fieldCount = 0;

    private Stage stage;

    private List<AutoCompleteCombo<Object>> comboLists;
    private List<Slider> slidersList;
    private Map<String, Control> fieldActiveControls;
    private Map<ToggleGroup, List<ToggleButton>> toggleGroups;

    private boolean useAndPredicate;

    /***
     * Instantiate the FilterDialogController.
     */
    public FilterDialogController() {
        this.comboLists = new ArrayList<>();
        this.slidersList = new ArrayList<>();
        this.fieldActiveControls = new HashMap<>();
        this.toggleGroups = new HashMap<>();
        this.useAndPredicate = true;
        Platform.runLater(() -> {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/FilterGui.fxml"));
            Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
            Parent parent;
            fxmlLoader.setController(this);
            stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            try {
                parent = fxmlLoader.load();
                Scene scene = new Scene(parent);
                stage.setScene(scene);
                stage.setResizable(false);
                ToggleGroup radioGroup = new ToggleGroup();
                andOption.setToggleGroup(radioGroup);
                orOption.setToggleGroup(radioGroup);
                radioGroup.selectedToggleProperty().addListener((option, oldValue, newValue) -> {
                    if (newValue instanceof RadioButton) {
                        RadioButton selected = (RadioButton) newValue;
                        useAndPredicate = selected.getId().equals(andOption.getId());

                        if (!useAndPredicate) {
                            toggleGroups.values().forEach(list -> {
                                list.forEach(toggle -> {
                                    toggle.setToggleGroup(null);
                                });
                            });
                        } else {
                            toggleGroups.keySet().forEach(group -> {
                                List<ToggleButton> toggles = toggleGroups.get(group);
                                toggles.forEach(toggle -> {
                                    toggle.setToggleGroup(group);
                                });
                            });
                        }
                    }
                });
                clearFiltersBtn.setOnAction(e -> hide());
                cancelBtn.setOnAction(e -> {
                    e.consume();
                    hide();
                });
                filterBtn.setOnAction(e -> {
                    e.consume();
                    hide();
                });

            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    /***
     * @return True if the user has selected the 'And' filter in the GUI,
     *         otherwise false.
     */
    public boolean useAndPredicate() {
        return useAndPredicate;
    }

    /***
     * Sets a handler for when the user clicks the 'Filter' button in the
     * dialog.
     *
     * @param eventHandler
     *            The event handler.
     */
    public void setFilterAction(EventHandler<ActionEvent> eventHandler) {
        Platform.runLater(() -> filterBtn.setOnAction(eventHandler));
    }

    /***
     * Sets the handler for when the user cancels/clears the filter dialog.
     *
     * @param eventHandler
     *            The event handler.
     */
    public void setClearFiltersAction(EventHandler<ActionEvent> eventHandler) {
        Platform.runLater(() -> clearFiltersBtn.setOnAction(eventHandler));
    }

    /***
     * This will clear all the filter drop downs and deselect the filter fields
     * in the filter dialog.
     */
    public void clearControlSelections() {
        Platform.runLater(() -> {
            comboLists.forEach(combo -> {
                combo.setValue(null);
            });
            deactivateFields(fieldActiveControls.values().stream().collect(Collectors.toList()));
        });
    }

    /***
     * This will activate/deactivate a filter field and update the checkbox in
     * the GUI to show whether the filter field is active.
     *
     * @param columnName
     *            The string name of the column filter which should be
     *            activated/deactivated.
     * @param active
     *            Set to true if the column filter should be active, false
     *            otherwise.
     */
    public void setFieldActiveState(final String columnName, final boolean active) {
        Control activeBox = fieldActiveControls.get(columnName);
        if (activeBox != null) {
            if (activeBox instanceof CheckBox) {
                ((CheckBox) activeBox).setSelected(active);
            } else {
                ((ToggleButton) activeBox).setSelected(active);

            }
        }
    }

    /***
     * Convenience method that will deactivate all the filter fields in the
     * specified list.
     *
     * @param fields
     *            The list of filter controls to deactivate.
     */
    public void deactivateFields(final List<Control> fields) {
        fields.forEach(field -> {
            if (field instanceof CheckBox) {
                ((CheckBox) field).setSelected(false);
            } else {
                ((ToggleButton) field).setSelected(false);
            }
        });
    }

    /***
     * Adds a filter field to the filter dialog so that users can filter the
     * specified column. Creates a combo box to add to the filter dialog with
     * the specified list items.
     *
     *
     * @param columnName
     *            The string name of the column which should be filtered
     * @param items
     *            An Observable list of valid items that can be selected for by
     *            the combo box.
     * @param toggleGroup
     *            The toggle group to assign this filter to in the case where
     *            only one filter in the toggle group should be active at a
     *            time. If null, then it can be toggled active all the time.
     * @param onComboSelection
     *            The action to perform when a selection is made on the filter
     *            combo.
     * @param onComboValueChange
     *            The action to perform.
     */
    public void addFilterOption(final String columnName, final ObservableList<Object> items, ToggleGroup toggleGroup, ChangeListener<? super Boolean> onComboSelection,
            ChangeListener<? super Object> onComboValueChange) {

        AutoCompleteCombo<Object> itemComboBox = new AutoCompleteCombo<>(items);
        itemComboBox.setPrefSize(250, 25);
        itemComboBox.setEditable(true);
        itemComboBox.getSelectionModel().selectedItemProperty().addListener(onComboValueChange);

        Platform.runLater(() -> {
            createFieldRow(toggleGroup, onComboSelection, columnName, itemComboBox, null);
            comboLists.add(itemComboBox);
        });
    }

    /***
     * Adds a filter field to the filter dialog so that users can filter the
     * specified column. Creates a slider controller to add to the filter dialog
     * with the specified valid values. The slider is helpful for numerical
     * lists.
     *
     * @param columnName
     *            The string name of the column which should be filtered.
     * @param values
     *            An Observable list of valid values that can be selected for by
     *            the slider.
     * @param toggleGroup
     *            The toggle group to assign this filter to in the case where
     *            only one filter in the toggle group should be active at a
     *            time. If null, then it can be toggled active all the time.
     * @param onSliderSelection
     *            The action to perform when a selection is made on the filter
     *            slider.
     * @param onSliderValueChange
     *            The action to perform
     */
    public void addFilterSlider(final String columnName, final ObservableList<Object> values, ToggleGroup toggleGroup, ChangeListener<? super Boolean> onSliderSelection,
            ChangeListener<? super Object> onSliderValueChange) {
        Slider slider = new Slider(0.0, 10.0, 0.0);
        slider.setMinorTickCount(1);
        slider.setMajorTickUnit(2.0);
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setSnapToTicks(true);
        slider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public Double fromString(String string) {
                return 0.0;
            }

            @Override
            public String toString(Double tickValue) {
                return values.get(tickValue.intValue()).toString();
            }
        });

        final Label sliderLabel = new Label("");

        slider.valueProperty().addListener((o, ov, nv) -> {
            Platform.runLater(() -> {
                sliderLabel.setText(values.get(nv.intValue()).toString());
                setFieldActiveState(columnName, true);
            });
        });
        sliderLabel.textProperty().addListener(onSliderValueChange);

        Platform.runLater(() -> {
            createFieldRow(toggleGroup, onSliderSelection, columnName, slider, sliderLabel);
            slidersList.add(slider);
        });
    }

    /***
     * Updates the specified slider values that the user can select.
     *
     * @param sliderIdx
     *            The index of the slider to update.
     * @param values
     *            The new values that the slider should display.
     */
    public void updateSliders(int sliderIdx, ObservableList<Object> values) {
        final Slider slider = slidersList.get(sliderIdx);
        if (slider != null && !values.isEmpty()) {
            final int min = 0;
            final int max = values.size() - 1;
            Platform.runLater(() -> {
                slider.setMin(min);
                slider.setMax(max);
            });
        }
    }

    /***
     * Hides the Filter Dialog GUI. For example when a filter is selected.
     */
    public void hide() {
        Platform.runLater(() -> {
            stage.hide();
        });
    }

    /***
     * Shows the Filter Dialog GUI, for example when a filter dialog button is
     * clicked.
     */
    public void show() {
        Platform.runLater(() -> {
            stage.show();
        });
    }

    /***
     * Gives the option of what modality to use when starting up the filter
     * dialog.
     *
     * @param modality
     *            The modality to use.
     */
    public void initModality(Modality modality) {
        Platform.runLater(() -> {
            stage.initModality(modality);
        });
    }

    public void setAlwaysOnTop(boolean onTop) {
        Platform.runLater(() -> {
            stage.setAlwaysOnTop(onTop);
        });
    }

    /***
     * Show the Filter Dialog above other windows. *
     */
    public void toFront() {
        Platform.runLater(() -> {
            stage.toFront();
        });
    }

    /***
     * Depending on whether the control is a slider or a combo box, this will
     * initialize the field in the filter dialog, so that it is a new row that
     * can be added to the dialog.
     *
     * @param toggleGroup
     *            The toggle group to attach this field to (or null for no
     *            toggle group).
     * @param selectionChanged
     *            Action to perform when the selection of the field changes.
     * @param columnName
     *            The name of the column (which can then be referenced by other
     *            methods in this class).
     * @param fieldControl
     *            The control object to use: a combo box or a slider control.
     * @param endLabel
     *            If not null, the end label will be shown at the end of the row
     *            in the filter dialog. Useful for displaying the selected value
     *            of the slider for example.
     */
    private void createFieldRow(ToggleGroup toggleGroup, ChangeListener<? super Boolean> selectionChanged, String columnName, Control fieldControl, Label endLabel) {
        Control activeBox = null;
        if (toggleGroup != null) {
            ToggleButton onBox = new ToggleButton();
            onBox.setText(" ");
            onBox.setSelected(false);
            onBox.setPadding(new Insets(0, 4, 0, 4));
            onBox.selectedProperty().addListener(selectionChanged);
            onBox.selectedProperty().addListener((o, nv, ov) -> {
                if (nv) {
                    onBox.setPadding(new Insets(0, 8, 0, 8));
                    onBox.setText("");
                } else {
                    onBox.setPadding(new Insets(0, 2, 0, 2));
                    onBox.setText("\u2713");
                }
            });
            onBox.setToggleGroup(toggleGroup);
            List<ToggleButton> togglesList = toggleGroups.get(toggleGroup);
            if (togglesList != null) {
                togglesList.add(onBox);
            } else {
                togglesList = new ArrayList<>();
                togglesList.add(onBox);
                toggleGroups.put(toggleGroup, togglesList);
            }

            activeBox = onBox;
        } else {
            CheckBox onBox = new CheckBox();
            onBox.setSelected(false);
            onBox.selectedProperty().addListener(selectionChanged);
            activeBox = onBox;
        }

        fieldActiveControls.put(columnName, activeBox);

        Label fieldLabel = new Label(columnName);
        fieldLabel.setPrefHeight(25);

        if (endLabel != null) {
            fieldRowGrid.addRow(fieldCount, activeBox, fieldLabel, fieldControl, endLabel);
        } else {
            GridPane.setColumnSpan(fieldControl, 2);
            fieldRowGrid.addRow(fieldCount, activeBox, fieldLabel, fieldControl);
        }

        fieldCount++;
    }
}
