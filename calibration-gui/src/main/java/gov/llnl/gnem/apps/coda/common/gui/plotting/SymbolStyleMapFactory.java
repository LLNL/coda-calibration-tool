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
package gov.llnl.gnem.apps.coda.common.gui.plotting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import javafx.scene.paint.Color;
import llnl.gnem.core.gui.plotting.api.SymbolStyles;

@Component
public class SymbolStyleMapFactory {

    private int symbol = 0;

    /**
     * This function splits the red-green-blue color wheel into n equally spaced
     * divisions based on their HSB representation and returns the color from a
     * particular division.
     *
     * @param index
     *            - The index of the color to return
     * @param count
     *            - The number of divisions to split the HSV color wheel into
     * @return A java.awt.Color object containing the color.
     */
    private static Color getSpacedOutColour(int index, int count) {
        final float saturation = 0.95f;
        final float brightness = 0.8f;
        float hue = (float) index / (float) count;
        return Color.hsb(hue * 360.0, saturation, brightness);
    }

    private synchronized SymbolStyles nextSymbol() {
        symbol = ++symbol % (SymbolStyles.values().length);
        return SymbolStyles.values()[symbol];
    }

    public <T, R> Map<R, PlotPoint> build(List<T> values, Function<T, R> keyProvider) {
        Map<R, PlotPoint> styles = new HashMap<>();
        AtomicInteger i = new AtomicInteger(0);
        long keyCount = values.stream().map(keyProvider::apply).distinct().count();
        //Is the JIT going to do what I expect here? Let's find out...
        values.parallelStream()
              .forEach(
                      v -> styles.putIfAbsent(
                              keyProvider.apply(v),
                                  new PlotPoint(null,
                                                null,
                                                nextSymbol(),
                                                SymbolStyleMapFactory.getSpacedOutColour(i.get(), (int) keyCount),
                                                SymbolStyleMapFactory.getSpacedOutColour(i.getAndIncrement(), (int) keyCount))));
        return styles;
    }
}
