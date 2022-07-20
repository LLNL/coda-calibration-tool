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

import org.apache.commons.lang3.StringUtils;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;

/***
 * An auto-complete combo box that incorporates a search feature so that as a
 * user types within the field, the drop down list will update to show items
 * containing the value typed in as suggestions, narrowing the list as the input
 * gets specific.
 *
 * @author downie4
 *
 * @param <T>
 *            The item type contained in the drop-down list.
 */
public class AutoCompleteCombo<T> extends ComboBox<T> {

    private ObservableList<T> originalItems;

    /***
     * Create an auto-complete combo box with specified items.
     *
     * @param items
     *            The observable list of items to use within the auto complete
     *            combo box.
     */
    public AutoCompleteCombo(ObservableList<T> items) {
        this.originalItems = items;
        this.setEditable(true);
        this.getEditor().setOnKeyTyped(e -> handle(e));
        this.getEditor().setOnMouseClicked(event -> {
            if (event.getButton().equals(MouseButton.PRIMARY) && (event.getClickCount() == 2)) {
                return;
            }
            this.show();
        });
        this.setItems(originalItems);
    };

    /***
     * Allows you to set the original items to use for the auto-complete. The
     * original items list is the base list from which a smaller sublist of
     * filtered items is made as the user types in the field.
     *
     * @param items
     */
    public void setOriginalItems(ObservableList<T> items) {
        originalItems = items;
    }

    /***
     * Handler for when user types within the combo box field. This will update
     * the combo box with filtered items as user types in the combo box.
     *
     * @param event
     */
    private void handle(KeyEvent event) {
        TextField field = this.getEditor();
        if (field == null) {
            return;
        }

        final String text = field.getText();
        ObservableList<T> filtered = filteredItems(text);

        if (this.getSelectionModel().getSelectedItem() != null) {
            this.getSelectionModel().clearSelection();
            field.setText(text.trim());
            field.end();
        }

        this.setItems(filtered);
        this.show();
    }

    /***
     * @param text
     *            The text used to filter the list.
     * @return A list of filtered items that match the provided text.
     */
    private ObservableList<T> filteredItems(String text) {
        if (StringUtils.isBlank(text)) {
            return originalItems;
        }
        ObservableList<T> dropDownItems = originalItems;
        if (!originalItems.isEmpty()) {
            dropDownItems = originalItems.filtered(data -> {
                if (data != null && !StringUtils.isBlank(data.toString())) {
                    return data.toString().toLowerCase().contains(text.toLowerCase());
                }
                return false;
            });
        }

        return dropDownItems;
    }
}