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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

class DataFilterController<T> {
    TableView<T> tableView;
    // The list of unfiltered table items
    ObservableList<T> items;
    FilterDialogController filterDialog;
    PredicateBuilder<T> predicateBuilder;
    // The filter buttons attached to columns
    private List<Button> buttons;
    private HashMap<TableColumn<T, ?>, ObservableList<Object>> columnFilterLists;
    private Image filterIcon;

    DataFilterController(final TableView<T> tableView) {
        this.tableView = tableView;
        this.items = tableView.getItems();
        this.buttons = new ArrayList<>();
        this.columnFilterLists = new HashMap<>();
        this.filterDialog = new FilterDialogController();
        this.predicateBuilder = new PredicateBuilder<>();

        // Disable filter button if no items to filter
        this.items.addListener((ListChangeListener<? super T>) change -> {
            if (change.getList().isEmpty()) {
                setFiltersDisabled(true);
                tableView.setItems(items);
                filterDialog.clearComboSelections();
            } else {
                setFiltersDisabled(false);
                updateFilterLists(change);
            }
        });
        InputStream icon2 = this.getClass().getResourceAsStream("/filter_icon.png");
        this.filterIcon = new Image(icon2, 16, 16, true, true);

        filterDialog.setFilterAction(e -> {
            filterTableViewResults();
            filterDialog.hide();
        });
        filterDialog.setClearFiltersAction(e -> {
            tableView.setItems(items);
            filterDialog.clearComboSelections();
            filterDialog.hide();
        });
    }

    public void addFilterToColumn(TableColumn<T, ?> column, PredicateBuilder.ValueComparer<T> converter) {

        // Create the checkbox that opens the filter panel/dialog
        Button filterBtn = new Button();
        final ImageView icon = new ImageView(filterIcon);
        filterBtn.setDisable(true);
        filterBtn.setGraphic(icon);
        filterBtn.setOnMouseClicked(e -> filterDialog.show());
        buttons.add(filterBtn);

        String columnName = column.getText();
        ObservableList<Object> filterOptions = FXCollections.observableList(new ArrayList<>());
        filterOptions.add(null);
        columnFilterLists.put(column, filterOptions);

        // Create a label that wraps the checkbox so that the column text
        // is displayed to the left of the checkbox
        Label buttonWrapper = new Label(columnName);
        buttonWrapper.setGraphic(filterBtn);
        buttonWrapper.setContentDisplay(ContentDisplay.RIGHT);
        column.setGraphic(buttonWrapper);
        column.setText("");

        // Update the filter dialog to include new filter options
        filterDialog.addFilterOption(columnName, filterOptions, (options, oldValue, newValue) -> {
            String txtValue = newValue != null ? newValue.toString() : "";
            predicateBuilder.setPredicate(columnName, converter, txtValue);
        });
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
                    if (!filterList.contains(null)) { // Add a null value for comboboxes
                        filterList.add(null);
                    }
                });
            }
            for (T addItem : change.getAddedSubList()) {
                columnFilterLists.entrySet().forEach(entry -> {
                    ObservableList<Object> filterList = entry.getValue();
                    Object itemVal = entry.getKey().getCellObservableValue(addItem).getValue();
                    if (!filterList.contains(itemVal)) { // Don't add duplicate items
                        filterList.add(itemVal);
                    }
                    if (!filterList.contains(null)) { // Add a null value for comboboxes
                        filterList.add(null);
                    }

                    filterList.sort((Comparator<Object>) entry.getKey().getComparator());
                });
            }
        }
    }

    public void filterTableViewResults() {
        if (filterDialog.useAndPredicate()) {
            Predicate<T> predicate = predicateBuilder.getAndPredicate();
            tableView.setItems(items.filtered(predicate));
        } else {
            Predicate<T> predicate = predicateBuilder.getOrPredicate();
            tableView.setItems(items.filtered(predicate));
        }
    }
}