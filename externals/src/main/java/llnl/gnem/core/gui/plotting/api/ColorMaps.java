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
package llnl.gnem.core.gui.plotting.api;

public enum ColorMaps {

    GREYS("Greys"), YLGNBU("YlGnBu"), GREENS("Greens"), YLORRD("YlOrRd"), BLUERED("Bluered"), RDBU("RdBu"), REDS("Reds"), BLUES("Blues"), PICNIC("Picnic"), RAINBOW("Rainbow"), PORTLAND(
            "Portland"), JET("Jet"), HOT("Hot"), BLACKBODY("Blackbody"), EARTH("Earth"), ELECTRIC("Electric"), VIRIDIS("Viridis"), CIVIDIS("Cividis"), BATLOW(
                    "[[\"0\",\"rgb(1,25,89)\"],[\".11\",\"rgb(16,63,96)\"],[\".22\",\"rgb(28,90,98)\"],[\".33\",\"rgb(60,109,86)\"],[\".44\",\"rgb(104,123,62)\"],[\".55\",\"rgb(157,137,43)\"],[\".66\",\"rgb(210,147,67)\"],[\".77\",\"rgb(248,161,123)\"],[\".88\",\"rgb(253,183,188)\"],[\"1\",\"rgb(250,204,250)\"]]"), PLASMA(
                            "[[\"0\",\"rgb(13,8,135)\"],[\".13\",\"rgb(75,3,161)\"],[\".25\",\"rgb(125,3,168)\"],[\".38\",\"rgb(168,34,150)\"],[\".5\",\"rgb(203,70,121)\"],[\".63\",\"rgb(229,107,93)\"],[\".75\",\"rgb(248,148,65)\"],[\".88\",\"rgb(253,195,40)\"],[\"1\",\"rgb(240,249,33)\"]]"), MAGMA(
                                    "[[\"0\",\"rgb(0,0,4)\"],[\".13\",\"rgb(28,16,68)\"],[\".25\",\"rgb(79,18,123)\"],[\".38\",\"rgb(129,37,129)\"],[\".5\",\"rgb(181,54,122)\"],[\".63\",\"rgb(229,80,100)\"],[\".75\",\"rgb(251,135,97)\"],[\".88\",\"rgb(254,194,135)\"],[\"1\",\"rgb(252,253,191)\"]]");

    private final String colorMap;

    private ColorMaps(final String colorMap) {
        this.colorMap = colorMap;
    }

    public String getColorMap() {
        return colorMap;
    }
}
