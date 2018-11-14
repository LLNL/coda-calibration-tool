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
package llnl.gnem.core.gui.plotting.plotobject;

import java.awt.Color;

/**
 * Class that is used to create Symbol instances defined by the supplied
 * SymbolStyle and other parameters.
 */

public class SymbolFactory {

    /**
     * @param style
     *            The type of symbol to create
     * @param X
     *            The X-center of the symbol in real-world coordinates
     * @param Y
     *            The Y-center of the symbol in real-world coordinates
     * @param size
     *            The size of the Symbol in mm
     * @param fillC
     *            The fill color for the symbol
     * @param edgeC
     *            The edge color for the symbol
     * @param textC
     *            The color of the text associated with the symbol
     * @param text
     *            The text string to be plotted with the symbol
     * @param visible
     *            The visibility of the symbol
     * @param textVis
     *            The visibility of the associated text
     * @param fontsize
     *            The font size of the associated text.
     * @return A fully constructed Symbol ready for use or null if there is no
     *         concrete implementation of the symbol.
     */
    public static Symbol createSymbol(SymbolStyle style, double X, double Y, double size, Color fillC, Color edgeC, Color textC, String text, boolean visible, boolean textVis, double fontsize) {

        Symbol s = createSymbolOfStyle(style);

        if (s != null) {
            s.setXcenter(X);
            s.setYcenter(Y);
            s.setSymbolSize(size);
            s.setFillColor(fillC);
            s.setEdgeColor(edgeC);
            s.setTextColor(textC);
            s.setText(text);
            s.setVisible(visible);
            s.setTextVisible(textVis);
            s.setFontSize(fontsize);
        }
        return s;
    }

    /***
     * @return A fully constructed Symbol ready for use or null if there is no
     *         concrete implementation of the symbol.
     */
    public static Symbol createSymbol(SymbolDef symboldef) {
        Symbol s = createSymbolOfStyle(symboldef.getStyle());

        if (s != null) {
            s.setSymbolSize(symboldef.getSize());
            s.setFillColor(symboldef.getFillColor());
            s.setEdgeColor(symboldef.getEdgeColor());
        }
        return s;
    }

    private static Symbol createSymbolOfStyle(SymbolStyle style) {
        Symbol s = null;
        if (style != null) {
            switch (style) {
            case CIRCLE:
                s = new Circle();
                break;
            case SQUARE:
                s = new Square();
                break;
            case DIAMOND:
                s = new Diamond();
                break;
            case TRIANGLEUP:
                s = new TriangleUp();
                break;
            case TRIANGLEDN:
                s = new TriangleDn();
                break;
            case PLUS:
                s = new Plus();
                break;
            case CROSS:
                s = new Cross();
                break;
            case STAR5:
                s = new Star5();
                break;
            case HEXAGON:
                s = new Hexagon();
                break;
            case ERROR_BAR:
                s = new ErrorBar();
                break;
            default:
                break;
            }
        }
        return s;
    }
}
