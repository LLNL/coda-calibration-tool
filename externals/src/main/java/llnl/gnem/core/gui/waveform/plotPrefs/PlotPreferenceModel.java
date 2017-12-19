/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.gui.waveform.plotPrefs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

import llnl.gnem.core.gui.plotting.jmultiaxisplot.JMultiAxisPlot;

/**
 *
 * @author dodge1
 */
public class PlotPreferenceModel {

    private final Map<JMultiAxisPlot, String> plotMap;
    private PlotPresentationPrefs plotPrefs;
    private final Preferences prefs;

    private PlotPreferenceModel() {
        plotPrefs = new PlotPresentationPrefs();
        plotMap = new WeakHashMap<>();
        prefs = Preferences.userNodeForPackage(this.getClass());
    }

    public void updateFromDb() throws Exception {

        byte[] bytes = prefs.getByteArray("PLOT_PREFS", null);
        if (bytes != null) {
            Object obj = deserialize(bytes);
            if (obj instanceof PlotPresentationPrefs) {
                plotPrefs = (PlotPresentationPrefs) obj;
            }
        }

    }

    public void registerPlot(JMultiAxisPlot plot) {
        plotMap.put(plot, "unused");
    }

    public static PlotPreferenceModel getInstance() {
        return PlotPreferenceModelHolder.instance;
    }

    /**
     * @return the plotPrefs
     */
    public PlotPresentationPrefs getPrefs() {
        return plotPrefs;
    }

    public void setPrefs(PlotPresentationPrefs prefs) throws Exception {
        this.plotPrefs = prefs;
        byte[] bytes = serialize(plotPrefs);
        this.prefs.putByteArray("PLOT_PREFS", bytes);
        for (JMultiAxisPlot plot : plotMap.keySet()) {
            plot.updateForChangedPrefs();
        }
    }

    private static class PlotPreferenceModelHolder {

        private static final PlotPreferenceModel instance = new PlotPreferenceModel();
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = null;

        ObjectOutputStream os = null;
        try {
            out = new ByteArrayOutputStream();
            os = new ObjectOutputStream(out);
            os.writeObject(obj);
            return out.toByteArray();
        } finally {
            if (os != null) {
                os.close();
            }
            if (out != null) {
                out.close();
            }
        }
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = null;

        ObjectInputStream is = null;
        try {
            in = new ByteArrayInputStream(data);
            is = new ObjectInputStream(in);

            return is.readObject();
        } finally {
            if (is != null) {
                is.close();
            }
            if (in != null) {
                in.close();
            }
        }
    }
}
