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

public class AutoCompleteCombo<T> extends ComboBox<T> {

    private ObservableList<T> originalItems;

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

    public void setOriginalItems(ObservableList<T> items) {
        originalItems = items;
    }

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