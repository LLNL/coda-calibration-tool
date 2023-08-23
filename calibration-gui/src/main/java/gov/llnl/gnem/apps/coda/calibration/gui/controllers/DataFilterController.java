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
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
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
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToggleGroup;
import javafx.util.StringConverter;

/***
 *
 * @author downie4
 *
 *         The data filter controller handles the items and their order in a
 *         provided TableView. It will instantiate a filter dialog that will
 *         open when users click the filter button above a column in the table
 *         view. It is used to provide filter capabilities to a table view for
 *         the users to narrow the results shown in the table.
 * @param <T>
 *            The element type that the data filter will be filtering (contained
 *            within the TableView provided to this class)
 */
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

    /***
     * The DataFilterController can handle multiple filter options to be used on
     * the specified TableView. The filters are activated by clicking a filter
     * button that is placed in the column header of a filterable table column,
     * and when clicked it will open a filter dialog. Note: You must specify
     * which columns you'd like to filter in the table by calling the
     * {@link DataFilterController#addFilterForColumn(boolean, ToggleGroup, TableColumn, gov.llnl.gnem.apps.coda.calibration.gui.controllers.PredicateBuilder.ValueComparer)}
     * method of the DataFilterController.
     *
     * @param tableView
     *            The table view which will be modified by the filter controller
     *            to have a filter dialog
     * @param items
     *            An unfiltered observable list that will populate the table
     *
     * @author downie4
     *
     */
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

    public void addFilterBtnToColumn(TableColumn<T, ?> column) {
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

        // Create a label that wraps the checkbox so that the column text
        // is displayed to the left of the checkbox
        Label buttonWrapper = new Label();
        buttonWrapper.setGraphic(filterBtn);
        buttonWrapper.setContentDisplay(ContentDisplay.RIGHT);
        column.setGraphic(buttonWrapper);
    }

    public void addFilterBtnToColumns(List<TableColumn<T, ?>> columns) {
        if (columns != null) {
            columns.forEach(column -> {
                addFilterBtnToColumn(column);
            });
        }
    }

    /***
     * This method tells the filter controller to add a filter button for the
     * specified column in the table. Appropriate handlers and predicates are
     * then generated for the column.
     *
     * @param slider
     *            Set true if the column should display a slider and each column
     *            element has a list of values to use for the slider.
     * @param toggleGroup
     *            This allows the filter to be added to a toggle group so that
     *            only one filter in the group can be applied at a time.
     * @param column
     *            The column to add the filter to.
     * @param converter
     *            Used to compare values between items of the column for sorting
     *            and filtering purposes.
     */
    public void addFilterForColumn(boolean slider, ToggleGroup toggleGroup, TableColumn<T, ?> column, PredicateBuilder.ValueComparer<T> converter) {
        String columnName = column.getText();
        addFilterForColumn(slider, toggleGroup, columnName, column, converter);
    }

    /***
     * This method tells the filter controller to add a filter button for the
     * specified column in the table. Appropriate handlers and predicates are
     * then generated for the column.
     *
     * @param slider
     *            Set true if the column should display a slider and each column
     *            element has a list of values to use for the slider.
     * @param toggleGroup
     *            This allows the filter to be added to a toggle group so that
     *            only one filter in the group can be applied at a time.
     * @param filterName
     *            The specific name to use for the filter in the filter dialog
     * @param column
     *            The column to add the filter to.
     * @param converter
     *            Used to compare values between items of the column for sorting
     *            and filtering purposes.
     */
    public void addFilterForColumn(boolean slider, ToggleGroup toggleGroup, String filterName, TableColumn<T, ?> column, PredicateBuilder.ValueComparer<T> converter) {
        ObservableList<Object> filterItemsList = FXCollections.observableList(new ArrayList<>());
        columnFilterLists.put(column, filterItemsList);

        // Create handler for when active state changes
        ChangeListener<? super Boolean> updateActive = (options, oldValue, newValue) -> {
            if (oldValue != newValue) {
                predicateBuilder.setPredicateActiveState(filterName, newValue);
            }
        };

        // Create handler for when field control updates value
        ChangeListener<? super Object> updateValue = (options, oldValue, newValue) -> {
            String txtValue = newValue != null ? newValue.toString() : "";
            if (!txtValue.equals("")) {
                predicateBuilder.setPredicate(filterName, converter, txtValue);
                predicateBuilder.setPredicateActiveState(filterName, true);
                filterDialog.setFieldActiveState(filterName, true);
            }
        };

        // Update the filter dialog to include new filter options
        if (slider) {
            sliderValues.add(filterItemsList);
            filterDialog.addFilterSlider(filterName, filterItemsList, toggleGroup, updateActive, updateValue);
        } else {
            filterDialog.addFilterOption(filterName, filterItemsList, toggleGroup, updateActive, updateValue);
        }
    }

    /***
     * This method tells the filter controller to add a filter button for the
     * specified column in the table. Appropriate handlers are then generated
     * for the column. The predicates are added as a map, and a dropdown is
     * created where each predicate is listed.
     *
     * @param slider
     *            Set true if the column should display a slider and each column
     *            element has a list of values to use for the slider.
     * @param toggleGroup
     *            This allows the filter to be added to a toggle group so that
     *            only one filter in the group can be applied at a time.
     * @param filterName
     *            The specific name to use for the filter in the filter dialog
     * @param column
     *            The column to add the filter to.
     * @param predicateMap
     *            A map of filter names and predicates used to generate a drop
     *            down in the filter dialog where user can select from a list of
     *            predicates to filter from
     */
    public void addFilterForColumn(boolean slider, ToggleGroup toggleGroup, String filterName, TableColumn<T, ?> column, Map<String, Predicate<T>> predicateMap) {

        ObservableList<Object> predicateNameList = FXCollections.observableList(new ArrayList<>());
        predicateNameList.addAll(predicateMap.keySet());
        columnFilterLists.put(column, predicateNameList);

        Slider customSlider = new Slider(0.0, predicateNameList.size(), 0.0);
        customSlider.setMinorTickCount(0);
        customSlider.setMajorTickUnit(1.0);
        customSlider.setShowTickMarks(true);
        customSlider.setShowTickLabels(true);
        customSlider.setSnapToTicks(true);
        customSlider.setLabelFormatter(new StringConverter<Double>() {
            @Override
            public Double fromString(String string) {
                return Double.valueOf(predicateNameList.indexOf(string));
            }

            @Override
            public String toString(Double tickValue) {
                return predicateNameList.get(tickValue.intValue()).toString();
            }
        });

        // Create handler for when active state changes
        ChangeListener<? super Boolean> updateActive = (options, oldValue, newValue) -> {
            if (!oldValue.equals(newValue)) {
                predicateBuilder.setPredicateActiveState(filterName, newValue);
            }
        };

        // Create handler for when field control updates value
        ChangeListener<? super Object> updateValue = (options, oldValue, newValue) -> {
            String txtValue = newValue != null ? newValue.toString() : "";
            if (!txtValue.equals("")) {
                final Predicate<T> predicate = predicateMap.get(txtValue);
                predicateBuilder.setPredicate(filterName, predicate);
                predicateBuilder.setPredicateActiveState(filterName, true);
                filterDialog.setFieldActiveState(filterName, true);
            }
        };

        Consumer<String> valueSelected = value -> {
            if (!value.equals("")) {
                final Predicate<T> predicate = predicateMap.get(value);
                predicateBuilder.setPredicate(filterName, predicate);
                predicateBuilder.setPredicateActiveState(filterName, true);
                filterDialog.setFieldActiveState(filterName, true);
            }
        };

        // Update the filter dialog to include new filter options
        if (slider) {
            sliderValues.add(predicateNameList);
            filterDialog.addFilterSlider(customSlider, filterName, predicateNameList, toggleGroup, updateActive, valueSelected);
        } else {
            filterDialog.addFilterOption(filterName, predicateNameList, toggleGroup, updateActive, updateValue);
        }
    }

    /***
     * Creates a filter range field in the filter dialog. The custom filter
     * range is used to filter along a range of values. It can filter across
     * various values using the specified map of filternames and values. It has
     * build in drop-down for use to select what comparison they wish to
     * perform. The filter predicate is created dynamically based on selected
     * values that user selects.
     *
     * @param filterName
     *            The name to display in the filter dialog for this filter row
     * @param valuesFunctions
     *            A map of names and corresponding functions which will be used
     *            as options for the custom range filter. The keys in the map
     *            are names to use in the field drop-down, the values are
     *            functions that return the desired number used to compare with
     *            the number the user entered.
     * @param columns
     *            A list of columns to add a filter button to (or null if not
     *            adding the button to any columns)
     */
    public void addCustomFilter(String filterName, Map<String, Function<T, Double>> valuesFunctions) {
        filterDialog.addCustomRangeFilter(filterName, valuesFunctions, predicateBuilder);
    }

    /***
     * Convenience method that will enable/disable the filter buttons on the
     * TableView (for example if the table is empty or loaded).
     *
     * @param disabled
     *            Set to true or false to set the disabled status of the filter
     *            buttons.
     */
    public void setFiltersDisabled(Boolean disabled) {
        buttons.forEach(button -> {
            button.setDisable(disabled);
        });
    }

    /***
     * This method is used to notify the controller that there was a change to
     * the items list of the TableView.
     *
     * @param change
     *            The changes done to the ObservableList of the table.
     */
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