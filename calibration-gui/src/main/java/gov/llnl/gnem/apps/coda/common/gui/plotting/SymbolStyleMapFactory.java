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
package gov.llnl.gnem.apps.coda.common.gui.plotting;

import java.awt.Color;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.springframework.stereotype.Component;

import llnl.gnem.core.gui.plotting.plotobject.SymbolStyle;

@Component
public class SymbolStyleMapFactory {

    private int symbol = 0;

    /**
     * This function splits the red-green-blue colour wheel into n equally
     * spaced divisions and returns the colour from a particular division.
     * 
     * @param index
     *            - The index of the colour to return, the range is 0 -
     *            (count-1)
     * @param count
     *            - The number of divisions to split the HSV colour wheel into
     * @return A java.awt.Color object containing the color.
     * @author HughesR
     */
    private static Color getSpacedOutColour(int index, int count) {
        final float saturation = 0.95f; // Saturation
        final float brightness = 0.8f; // Brightness
        float hue = (float) index / (float) count;
        return Color.getHSBColor(hue, saturation, brightness);
    }

    private SymbolStyle nextSymbol() {
        SymbolStyle nextStyle;
        symbol = ++symbol % 8;
        switch (symbol) {
        case 0:
            nextStyle = SymbolStyle.CIRCLE;
            break;
        case 1:
            nextStyle = SymbolStyle.SQUARE;
            break;
        case 2:
            nextStyle = SymbolStyle.DIAMOND;
            break;
        case 3:
            nextStyle = SymbolStyle.TRIANGLEUP;
            break;
        case 4:
            nextStyle = SymbolStyle.TRIANGLEDN;
            break;
        case 5:
            nextStyle = SymbolStyle.CROSS;
            break;
        case 6:
            nextStyle = SymbolStyle.STAR5;
            break;
        case 7:
            nextStyle = SymbolStyle.HEXAGON;
            break;
        default:
            nextStyle = SymbolStyle.ERROR_BAR;
        }

        return nextStyle;
    }

    public <T, R> Map<R, PlotPoint> build(List<T> values, Function<T, R> keyProvider) {
        Map<R, PlotPoint> styles = new HashMap<>();
        AtomicInteger i = new AtomicInteger(0);
        long keyCount = values.stream().map(v -> keyProvider.apply(v)).distinct().count();
        //Is the JIT going to do what I expect here? Let's find out...        
        values.stream()
              .sequential()
              .forEach(v -> styles.putIfAbsent(keyProvider.apply(v), new PlotPoint(null, null, nextSymbol(), SymbolStyleMapFactory.getSpacedOutColour(i.getAndIncrement(), (int) keyCount))));
        return styles;
    }
}
