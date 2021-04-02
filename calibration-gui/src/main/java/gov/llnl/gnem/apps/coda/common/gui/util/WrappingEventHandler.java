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

import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.event.EventHandler;
import javafx.scene.control.TableColumn.CellEditEvent;

public class WrappingEventHandler<T, S> implements EventHandler<javafx.scene.control.TableColumn.CellEditEvent<T, java.lang.String>> {

    private EventHandler<CellEditEvent<T, String>> existingHandler;
    private BiConsumer<T, S> setValue;

    public WrappingEventHandler(EventHandler<CellEditEvent<T, String>> existingHandler, BiConsumer<T, S> setValue) {
        this.existingHandler = existingHandler;
        this.setValue = setValue;
    }

    @SuppressWarnings("unchecked")
    public Function<Object, S> getValueParser() {
        return (Function<Object, S>) Function.identity();
    }

    @Override
    public void handle(CellEditEvent<T, String> e) {
        if (existingHandler != null) {
            existingHandler.handle(e);
        }
        try {
            setValue.accept(e.getRowValue(), getValueParser().apply(e.getNewValue()));
        } catch (Exception ex) {
        }
    }

}