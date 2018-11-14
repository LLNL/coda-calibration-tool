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
package llnl.gnem.core.gui.plotting;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;

import llnl.gnem.core.gui.plotting.plotobject.PlotObject;
import llnl.gnem.core.gui.plotting.plotobject.Symbol;
import llnl.gnem.core.gui.plotting.plotobject.SymbolDef;
import llnl.gnem.core.gui.plotting.plotobject.SymbolFactory;

/**
 * Created by dodge1 Date: Feb 6, 2008
 */
public class SymbolLegend extends PlotObject {

    private ArrayList<SymbolTextPair> legendEntries;
    private String fontName;
    private double fontSize;
    private HorizPinEdge horAlign;
    private VertPinEdge vertAlign;
    private double xOffset;
    private double yOffset;

    public SymbolLegend(ArrayList<SymbolTextPair> entries, String fontName, double fontSize, HorizPinEdge hAlign, VertPinEdge vAlign, double xOff, double yOff) {
        this.legendEntries = entries;
        this.fontName = fontName;
        this.fontSize = fontSize;
        this.horAlign = hAlign;
        this.vertAlign = vAlign;
        this.xOffset = xOff;
        this.yOffset = yOff;
    }

    public void render(Graphics g, JBasicPlot owner) {
        if (legendEntries.size() < 1 || !owner.getCanDisplay() || !isVisible())
            return;

        // Remove any pre-existing regions before creating new...
        region.clear();
        Graphics2D g2d = (Graphics2D) g;
        g2d.setFont(new Font(fontName, Font.PLAIN, (int) fontSize));
        FontMetrics fm = g2d.getFontMetrics();
        int legendwidth = getLegendWidth(owner, fm);
        int legendheight = getLegendHeight(fm);
        int legendleft = getLegendLeft(owner, legendwidth);
        int legendtop = getLegendTop(owner, legendheight);

        // Fill and stroke the legend box
        Rectangle rect = new Rectangle(legendleft, legendtop, legendwidth, legendheight);
        g2d.setColor(Color.white);
        g2d.fill(rect);
        g2d.setColor(Color.black);
        g2d.setStroke(new BasicStroke(1.0F));
        g2d.draw(rect);
        addToRegion(rect);

        int off = fm.getMaxAscent() + fm.getMaxDescent();
        int off2 = off / 2;
        for (int j = 0; j < legendEntries.size(); ++j) {
            String tmp = legendEntries.get(j).getText();
            int Y = legendtop + (j + 1) * off;
            drawSymbol(legendEntries.get(j).getSymbolDef(), g, legendleft + 10, Y);
            int xtext = legendleft + 20;
            int ytest = legendtop + j * off + off2;
            g2d.setColor(Color.black);
            g2d.drawString(tmp, xtext, ytest + fm.getMaxAscent());
        }

    }

    private void drawSymbol(SymbolDef line, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D) g;
        Symbol s = SymbolFactory.createSymbol(line);
        if (s != null) {
            s.setXcoordPinned(true);
            s.setYcoordPinned(true);
            s.setXcoordIntValue(x);
            s.setYcoordIntValue(y);
            s.render(g2d, owner);
        }
    }

    private int getLegendWidth(JBasicPlot owner, FontMetrics fm) {
        int maxlen = 0;
        for (SymbolTextPair a_Text : legendEntries) {
            int advance = fm.stringWidth(a_Text.getText());
            maxlen = Math.max(maxlen, advance);
        }
        double minLineLen = 15.0;
        // millimeters
        return maxlen + owner.getUnitsMgr().getHorizUnitsToPixels(minLineLen) + 5;
    }

    private int getLegendHeight(FontMetrics fm) {
        int height = fm.getMaxAscent() + fm.getMaxDescent();
        return (legendEntries.size() + 1) * height;
    }

    private int getLegendLeft(JBasicPlot owner, int legendwidth) {
        int offset = owner.getUnitsMgr().getHorizUnitsToPixels(xOffset);
        return horAlign == HorizPinEdge.LEFT ? owner.getPlotLeft() + offset : owner.getPlotLeft() + owner.getPlotWidth() - offset - legendwidth;
    }

    private int getLegendTop(JBasicPlot owner, int legendheight) {
        int offset = owner.getUnitsMgr().getVertUnitsToPixels(yOffset);
        return vertAlign == VertPinEdge.TOP ? owner.getPlotTop() + offset : owner.getPlotTop() + owner.getPlotHeight() - offset - legendheight;
    }

    public void ChangePosition(JBasicPlot owner, Graphics graphics, double dx, double dy) {
        // Movement not allowed.
    }

    public static class SymbolTextPair {
        private final String text;
        private final SymbolDef symbolDef;

        public SymbolTextPair(String text, SymbolDef symbolDef) {
            this.text = text;
            this.symbolDef = symbolDef;
        }

        public String getText() {
            return text;
        }

        public SymbolDef getSymbolDef() {
            return symbolDef;
        }
    }
}
