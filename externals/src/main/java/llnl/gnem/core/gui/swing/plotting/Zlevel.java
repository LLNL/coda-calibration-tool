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
package llnl.gnem.core.gui.swing.plotting;

import java.awt.Graphics;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Vector;

import llnl.gnem.core.gui.swing.plotting.plotobjects.BasicText;
import llnl.gnem.core.gui.swing.plotting.plotobjects.Line;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;
import llnl.gnem.core.gui.swing.plotting.plotobjects.Symbol;

/**
 * User: dodge1 Date: Feb 14, 2006
 */
public class Zlevel {
    private Vector<PlotObject> objects;
    private boolean selectable = true;
    private boolean visible = true;

    public Zlevel() {
        objects = new Vector<>();
    }

    public synchronized void clear() {
        objects.clear();
    }

    public synchronized void add(PlotObject obj) {
        objects.add(obj);
    }

    public synchronized boolean remove(PlotObject obj) {
        return objects.remove(obj);
    }

    public synchronized Vector<PlotObject> getObjects() {
        return objects;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(boolean selectable) {
        this.selectable = selectable;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public synchronized PlotObject getHotObject(int x, int y) {
        if (isSelectable()) { // If this level is selectable...
            for (ListIterator<PlotObject> i = objects.listIterator(objects.size()); i.hasPrevious();) {
                PlotObject obj = i.previous();
                if (obj.isSelectable() && obj.PointInside(x, y))
                    return obj;
                else {
                    PlotObject po = obj.getSubObjectContainingPoint(x, y);
                    if (po != null)
                        return po;
                }
            }
            return null;
        } else
            return null;

    }

    public synchronized void setLevelSymbolAlpha(int alpha) {
        for (ListIterator<PlotObject> i = objects.listIterator(objects.size()); i.hasPrevious();) {
            PlotObject obj = i.previous();
            if (obj instanceof Symbol) {
                Symbol s = (Symbol) obj;
                s.setAlpha(alpha);
            }
        }
    }

    public synchronized int getLineCount() {
        int count = 0;

        for (Object object : objects) {
            PlotObject obj = (PlotObject) object;
            if (obj instanceof Line)
                ++count;
        }
        return count;
    }

    public synchronized Vector<Line> getLines() {
        Vector<Line> result = new Vector<Line>();
        for (Object object : objects) {
            PlotObject obj = (PlotObject) object;
            if (obj instanceof Line)
                result.add((Line) obj);
        }
        return result;
    }

    public synchronized void setPolyLineUsage(boolean value) {
        for (Object object : objects) {
            PlotObject obj = (PlotObject) object;
            if (obj instanceof Line) {
                Line l = (Line) obj;
                l.setPolylineUsage(value);
            }
        }
    }

    public synchronized void clearSelectionRegions() {
        for (Object object : objects) {
            PlotObject obj = (PlotObject) object;
            obj.clearSelectionRegion();
        }
    }

    synchronized void renderVisiblePlotObjects(Graphics g, JBasicPlot owner) {
        for (Object o : objects) {
            PlotObject obj = (PlotObject) o;
            obj.render(g, owner);
        }
    }

    public synchronized void clearText() {
        Iterator<PlotObject> it = objects.iterator();
        while (it.hasNext()) {
            PlotObject obj = it.next();
            if (obj instanceof BasicText) {
                it.remove();
            }
        }
    }

    public synchronized boolean contains(PlotObject po) {
        for (Object o : objects) {
            PlotObject obj = (PlotObject) o;
            if (obj == po)
                return true;
        }
        return false;
    }

    public synchronized Vector<PlotObject> getVisiblePlotObjects() {
        Vector<PlotObject> result = new Vector<PlotObject>();
        for (Object o : objects) {
            PlotObject obj = (PlotObject) o;
            if (obj.isVisible())
                //noinspection unchecked
                result.add(obj);
        }
        return result;
    }

}
