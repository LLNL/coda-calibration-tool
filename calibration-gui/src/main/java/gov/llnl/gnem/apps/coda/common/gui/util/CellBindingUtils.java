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
package gov.llnl.gnem.apps.coda.common.gui.util;

import java.text.NumberFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Callback;

public class CellBindingUtils {

    private static NumberFormat DEFAULT_NUMBER_FORMAT = NumberFormatFactory.twoDecimalOneLeadingZero();

    public static <T> void attachTextCellFactories(TableColumn<T, String> col, Function<T, Double> getValue) {
        attachTextCellFactories(col, getValue, DEFAULT_NUMBER_FORMAT);
    }

    public static <T> void attachTextCellFactoriesString(TableColumn<T, String> col, Function<T, String> getValue) {
        col.setCellValueFactory(stringFormatCellBinding(getValue));
    }

    public static <T> void attachTextCellFactories(TableColumn<T, String> col, Function<T, Double> getValue, NumberFormat format) {
        col.setCellValueFactory(numberFormatCellBinding(getValue, format));
        col.comparatorProperty().set(new MaybeNumericStringComparator());
    }

    public static <T> void attachIntegerCellFactories(TableColumn<T, String> col, Function<T, Integer> getValue) {
        col.setCellValueFactory(integerFormatCellBinding(getValue));
    }

    public static <T> void attachEditableTextCellFactories(TableColumn<T, String> col, Function<T, Double> getValue, BiConsumer<T, Double> setValue) {
        attachEditableTextCellFactories(col, getValue, setValue, DEFAULT_NUMBER_FORMAT);
    }

    public static <T> void attachEditableTextCellFactories(TableColumn<T, String> col, Function<T, Double> getValue, BiConsumer<T, Double> setValue, NumberFormat format) {
        attachTextCellFactories(col, getValue, format);
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(new DoubleWrappingEventHandler<T>(col.getOnEditCommit(), setValue));
    }

    public static <T> void attachEditableIntegerCellFactories(TableColumn<T, String> col, Function<T, Integer> getValue, BiConsumer<T, Integer> setValue) {
        attachIntegerCellFactories(col, getValue);
        col.setCellFactory(TextFieldTableCell.forTableColumn());
        col.setOnEditCommit(new IntegerWrappingEventHandler<T>(col.getOnEditCommit(), setValue));
    }

    private static <T> Callback<CellDataFeatures<T, String>, ObservableValue<String>> numberFormatCellBinding(Function<T, Double> getValue, NumberFormat formatter) {
        return x -> Bindings.createStringBinding(() -> getOptionalValue(x, getValue).map(formatter::format).orElseGet(String::new));
    }

    private static <T> Callback<CellDataFeatures<T, String>, ObservableValue<String>> integerFormatCellBinding(Function<T, Integer> getValue) {
        return x -> Bindings.createStringBinding(() -> getOptionalValue(x, getValue).map(v -> v.toString()).orElseGet(String::new));
    }

    private static <T> Callback<CellDataFeatures<T, String>, ObservableValue<String>> stringFormatCellBinding(Function<T, String> getValue) {
        return x -> Bindings.createStringBinding(() -> getOptionalValue(x, getValue).orElseGet(String::new));
    }

    private static <T, U> Optional<U> getOptionalValue(CellDataFeatures<T, String> x, Function<T, U> getValue) {
        return Optional.ofNullable(x).map(CellDataFeatures::getValue).map(getValue::apply).filter(Objects::nonNull);
    }
}
