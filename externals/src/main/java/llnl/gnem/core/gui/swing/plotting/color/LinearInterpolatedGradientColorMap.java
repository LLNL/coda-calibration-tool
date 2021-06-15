/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.gui.swing.plotting.color;

import java.awt.Color;

import llnl.gnem.core.util.PairT;

public abstract class LinearInterpolatedGradientColorMap implements ColorMap {
    private static final int red = 0;
    private static final int green = 1;
    private static final int blue = 2;
    protected float[][] rgb;
    private double min;
    private double max;

    @Override
    public void setRange(double min, double max) {
        if (max < min) {
            max = 2. * min;
        }

        this.min = min;
        this.max = max;
    }

    @Override
    public Color getColor(final double value) {
        PairT<Integer, Float> index = getIndex(value);
        int idx = index.getFirst();
        float pct2 = index.getSecond();
        float pct1;
        Color color1;
        Color color2 = new Color(rgb[idx][red], rgb[idx][green], rgb[idx][blue]);
        float epsilon = (float) 1E-12;
        if (idx != 0 && 1f - pct2 > epsilon) {
            color1 = new Color(rgb[idx - 1][red], rgb[idx - 1][green], rgb[idx - 1][blue]);
            pct1 = 1.0f - pct2;
            color2 = combineColors(color1, pct1, color2, pct2);
        } else if (idx != rgb.length - 1 && 1f - pct2 > epsilon) {
            color1 = new Color(rgb[idx + 1][red], rgb[idx + 1][green], rgb[idx + 1][blue]);
            pct1 = 1.0f - pct2;
            color2 = combineColors(color1, pct1, color2, pct2);
        }
        return color2;
    }

    private Color combineColors(Color color1, float pct1, Color color2, float pct2) {
        return new Color((color1.getRed() / 255f) * pct1 + (color2.getRed() / 255f) * pct2,
                         (color1.getGreen() / 255f) * pct1 + (color2.getGreen() / 255f) * pct2,
                         (color1.getBlue() / 255f) * pct1 + (color2.getBlue() / 255f) * pct2);
    }

    @Override
    public double getMin() {
        return min;
    }

    @Override
    public double getMax() {
        return max;
    }

    private PairT<Integer, Float> getIndex(double value) {
        value = Math.max(Math.min(max, value), min);
        double frac = (value - min) * (rgb.length - 1) / (max - min);
        int index = rgb.length - 1 - (int) (frac);
        return new PairT<Integer, Float>(index, 1.0f - (float) frac % 1);
    }
}
