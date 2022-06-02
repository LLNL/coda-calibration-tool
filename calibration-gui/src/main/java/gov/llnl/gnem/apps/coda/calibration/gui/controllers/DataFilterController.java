/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;

class DataFilterController<T> {
    // The list of unfiltered table items
    ObservableList<T> items;
    FilterDialogController filterDialog;
    PredicateBuilder<T> predicateBuilder;
    // The filter buttons attached to columns
    private List<Button> buttons;
    private HashMap<TableColumn<T, ?>, ObservableList<Object>> columnFilterLists;
    private List<ObservableList<Object>> sliderValues;
    private FilteredList<T> filteredItems;

    DataFilterController(final TableView<T> tableView, final ObservableList<T> items) {
        this.items = items;
        this.buttons = new ArrayList<>();
        this.columnFilterLists = new HashMap<>();
        this.sliderValues = new ArrayList<>();
        this.filterDialog = new FilterDialogController();
        this.predicateBuilder = new PredicateBuilder<>();

        filteredItems = items.filtered(null);
        //Unfortunately filtered/sorted lists are immutable so some
        //extra legwork needed to bind it correctly to the tableview
        SortedList<T> sortedItems = new SortedList<>(filteredItems);
        sortedItems.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedItems);

        // Disable filter button if no items to filter
        this.items.addListener((ListChangeListener<? super T>) change -> {
            if (change.getList().isEmpty()) {
                setFiltersDisabled(true);
                filterDialog.clearControlSelections();
            } else {
                setFiltersDisabled(false);
                updateFilterLists(change);
            }
        });

        filterDialog.setFilterAction(e -> {
            filterTableViewResults();
            filterDialog.hide();
        });
        filterDialog.setClearFiltersAction(e -> {
            filterDialog.clearControlSelections();
            filterDialog.hide();
            filteredItems.setPredicate(null);
        });
    }

    public void addFilterToColumn(boolean slider, ToggleGroup toggleGroup, TableColumn<T, ?> column, PredicateBuilder.ValueComparer<T> converter) {

        // Create the checkbox that opens the filter panel/dialog
        Button filterBtn = new Button();
        filterBtn.setDisable(true);
        Label label = new Label("\uEF4F");
        label.getStyleClass().add("material-icons-medium");
        label.setMaxHeight(16);
        label.setMinWidth(16);
        filterBtn.setGraphic(label);
        filterBtn.setOnMouseClicked(e -> filterDialog.show());
        buttons.add(filterBtn);

        String columnName = column.getText();
        ObservableList<Object> filterItemsList = FXCollections.observableList(new ArrayList<>());
        columnFilterLists.put(column, filterItemsList);

        // Create a label that wraps the checkbox so that the column text
        // is displayed to the left of the checkbox
        Label buttonWrapper = new Label(columnName);
        buttonWrapper.setGraphic(filterBtn);
        buttonWrapper.setContentDisplay(ContentDisplay.RIGHT);
        column.setGraphic(buttonWrapper);
        column.setText("");

        // Create handler for when active state changes
        ChangeListener<? super Boolean> updateActive = (options, oldValue, newValue) -> {
            if (oldValue != newValue) {
                predicateBuilder.setPredicateActiveState(columnName, newValue);
            }
        };

        // Create handler for when field control updates value
        ChangeListener<? super Object> updateValue = (options, oldValue, newValue) -> {
            String txtValue = newValue != null ? newValue.toString() : "";
            if (!txtValue.equals("")) {
                predicateBuilder.setPredicate(columnName, converter, txtValue);
                predicateBuilder.setPredicateActiveState(columnName, true);
                filterDialog.setFieldActiveState(columnName, true);
            }
        };

        // Update the filter dialog to include new filter options
        if (slider) {
            sliderValues.add(filterItemsList);
            filterDialog.addFilterSlider(columnName, filterItemsList, toggleGroup, updateActive, updateValue);
        } else {
            filterDialog.addFilterOption(columnName, filterItemsList, toggleGroup, updateActive, updateValue);
        }
    }

    public void setFiltersDisabled(Boolean disabled) {
        buttons.forEach(button -> {
            button.setDisable(disabled);
        });
    }

    public void updateFilterLists(final Change<? extends T> change) {
        while (change.next()) {
            for (T remItem : change.getRemoved()) {
                columnFilterLists.entrySet().forEach(entry -> {
                    ObservableList<Object> filterList = entry.getValue();
                    Object itemVal = entry.getKey().getCellObservableValue(remItem).getValue();
                    filterList.remove(itemVal);
                });
            }
            for (T addItem : change.getAddedSubList()) {
                columnFilterLists.entrySet().forEach(entry -> {
                    ObservableList<Object> filterList = entry.getValue();
                    Object itemVal = entry.getKey().getCellObservableValue(addItem).getValue();
                    if (!filterList.contains(itemVal)) { // Don't add duplicate items
                        filterList.add(itemVal);
                    }

                    filterList.sort((Comparator<Object>) entry.getKey().getComparator());
                });
            }
        }

        for (int idx = 0; idx < sliderValues.size(); idx++) {
            filterDialog.updateSliders(idx, sliderValues.get(idx));
        }
    }

    public void filterTableViewResults() {
        Predicate<T> predicate;
        if (filterDialog.useAndPredicate()) {
            predicate = predicateBuilder.getAndPredicate();
        } else {
            predicate = predicateBuilder.getOrPredicate();
        }
        filteredItems.setPredicate(predicate);
    }
}