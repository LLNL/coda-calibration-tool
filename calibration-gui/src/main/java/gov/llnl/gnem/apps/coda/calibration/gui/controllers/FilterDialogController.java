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

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Modality;
import javafx.stage.Stage;

@Component
public class FilterDialogController {

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
	protected VBox fieldNames;

	@FXML
	protected VBox optionsLists;

	private Stage stage;

	private List<ComboBox<Object>> comboLists;

	private boolean useAndPredicate;

	public FilterDialogController() {
		this.comboLists = new ArrayList<>();
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
				ToggleGroup radioGroup = new ToggleGroup();
				andOption.setToggleGroup(radioGroup);
				orOption.setToggleGroup(radioGroup);
				radioGroup.selectedToggleProperty().addListener((option, oldValue, newValue) -> {
					if (newValue instanceof RadioButton) {
						RadioButton selected = (RadioButton) newValue;
						useAndPredicate = selected.getId().equals(andOption.getId());
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

	public boolean useAndPredicate() {
		return useAndPredicate;
	}

	public void setFilterAction(EventHandler<ActionEvent> eventHandler) {
		Platform.runLater(() -> filterBtn.setOnAction(eventHandler));
	}

	public void setClearFiltersAction(EventHandler<ActionEvent> eventHandler) {
		Platform.runLater(() -> clearFiltersBtn.setOnAction(eventHandler));
	}

	public void clearComboSelections() {
		comboLists.forEach(comboList -> {
			comboList.setValue(null);
		});
	}

	public void addFilterOption(final String columnName, final ObservableList<Object> items,
			ChangeListener<? super Object> onComboValueChange) {
		AutoCompleteCombo<Object> itemComboBox = new AutoCompleteCombo<>(items);
		itemComboBox.setPrefSize(200, 25);
		itemComboBox.setEditable(true);
		itemComboBox.getSelectionModel().selectedItemProperty().addListener(onComboValueChange);

		Platform.runLater(() -> {
			Label fieldName = new Label(columnName);
			fieldName.setPrefHeight(25);
			fieldNames.getChildren().add(fieldName);
			optionsLists.getChildren().add(itemComboBox);
			comboLists.add(itemComboBox);
		});
	}

	public void hide() {
		Platform.runLater(() -> {
			stage.hide();
		});
	}

	public void show() {
		Platform.runLater(() -> {
			stage.show();
		});
	}

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

	public void toFront() {
		Platform.runLater(() -> {
			stage.toFront();
		});
	}
}
