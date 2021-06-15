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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import llnl.gnem.core.gui.swing.plotting.plotobjects.Line;
import llnl.gnem.core.gui.swing.plotting.plotobjects.PlotObject;

/**
 * User: dodge1 Date: Feb 14, 2006
 */
public class ZorderManager {
    private final List<Zlevel> levels;
    private final ReentrantLock lock;

    public ZorderManager() {
        levels = new ArrayList<>();
        levels.add(new Zlevel());
        lock = new ReentrantLock();
    }

    public void clear() {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                level.clear();
            }
            levels.clear();
        } finally {
            lock.unlock();
        }
    }

    public void setLevelVisible(boolean visible, int level) {
        lock.lock();
        try {
            if (level < levels.size()) {
                levels.get(level).setVisible(visible);
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean isLevelVisible(int level) {
        lock.lock();
        try {
            if (level < levels.size()) {
                return levels.get(level).isVisible();
            } else {
                return false;
            }
        } finally {
            lock.unlock();
        }
    }

    public void setLevelSymbolAlpha(int alpha, int level) {
        lock.lock();
        try {
            if (level < levels.size()) {
                levels.get(level).setLevelSymbolAlpha(alpha);
            }
        } finally {
            lock.unlock();
        }
    }

    public void add(PlotObject obj) {
        lock.lock();
        try {
            levels.get(0).add(obj);
        } finally {
            lock.unlock();
        }
    }

    public void add(PlotObject obj, int level) {
        lock.lock();
        try {
            while (levels.size() < level + 1) {
                levels.add(new Zlevel());
            }
            levels.get(level).add(obj);
        } finally {
            lock.unlock();
        }
    }

    public boolean remove(PlotObject obj) {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                if (level.remove(obj)) {
                    return true;
                }
            }
        } finally {
            lock.unlock();
        }
        return false;
    }

    public PlotObject getHotObject(int x, int y) {
        // Traverse list from highest zorder to lowest...
        lock.lock();
        try {
            for (int j = levels.size() - 1; j >= 0; --j) {
                Zlevel t = levels.get(j);
                if (t.isVisible()) {
                    PlotObject po = t.getHotObject(x, y);
                    if (po != null) {
                        return po;
                    }
                }
            }
            return null;
        } finally {
            lock.unlock();
        }
    }

    public PlotObject getHotObject(int x, int y, int level) {
        lock.lock();
        try {
            if (level >= 0 && level < levels.size()) {
                return levels.get(level).getHotObject(x, y);
            } else {
                return null;
            }
        } finally {
            lock.unlock();
        }
    }

    public int getLineCount() {
        lock.lock();
        try {
            int count = 0;
            for (Zlevel level : levels) {
                count += level.getLineCount();
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    public ArrayList<Line> getLines() {
        lock.lock();
        try {
            ArrayList<Line> result = new ArrayList<>();
            for (Zlevel level : levels) {
                result.addAll(level.getLines());
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    public void setPolyLineUsage(boolean value) {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                level.setPolyLineUsage(value);
            }
        } finally {
            lock.unlock();
        }
    }

    public void clearSelectionRegions() {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                level.clearSelectionRegions();
            }
        } finally {
            lock.unlock();
        }
    }

    void renderVisiblePlotObjects(Graphics g, JBasicPlot owner) {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                if (level.isVisible()) {
                    level.renderVisiblePlotObjects(g, owner);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void clearText() {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                level.clearText();
            }
        } finally {
            lock.unlock();
        }
    }

    public boolean contains(PlotObject po) {
        lock.lock();
        try {
            for (Zlevel level : levels) {
                if (level.contains(po)) {
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public ArrayList<PlotObject> getVisiblePlotObjects() {
        lock.lock();
        try {
            ArrayList<PlotObject> result = new ArrayList<>();
            for (Zlevel level : levels) {
                if (level.isVisible()) {
                    result.addAll(level.getVisiblePlotObjects());
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }
}
