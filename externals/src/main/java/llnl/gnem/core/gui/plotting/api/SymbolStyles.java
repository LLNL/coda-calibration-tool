/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”)), you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package llnl.gnem.core.gui.plotting.api;

public enum SymbolStyles {
    CIRCLE ( "circle"),
    SQUARE ( "square"),
    DIAMOND ( "diamond"),
    CROSS ( "cross"),
    X ( "x"),
    ARROW ( "arrow"),
    ARROW_WIDE ( "arrow-wide"),
    ARROW_LEFT ( "arrow-left"),
    ARROW_RIGHT ( "arrow-right"),
    TRIANGLE_UP ( "triangle-up"),
    TRIANGLE_DOWN ( "triangle-down"),
    TRIANGLE_NE ( "triangle-ne"),
    TRIANGLE_SW ( "triangle-sw"),
    PENTAGON ( "pentagon"),
    HEXAGON ( "hexagon"),
    STAR ( "star"),
    HEXAGRAM ( "hexagram"),
    STAR_TRIANGLE_UP ( "star-triangle-up"),
    STAR_TRIANGLE_DOWN ( "star-triangle-down"),
    STAR_SQUARE ( "star-square"),
    STAR_DIAMOND ( "star-diamond"),
    DIAMOND_TALL ( "diamond-tall"),
    DIAMOND_WIDE ( "diamond-wide"),
    HOURGLASS ( "hourglass"),
    BOWTIE ( "bowtie"),
    CIRCLE_CROSS ( "circle-cross"),
    CIRCLE_X ( "circle-x"),
    SQUARE_CROSS ( "square-cross"),
    SQUARE_X ( "square-x"),
    DIAMOND_CROSS ( "diamond-cross"),
    DIAMOND_X ( "diamond-x");

    private final String styleName;

    private SymbolStyles(String styleName) {
        this.styleName = styleName;
    }

    public String getStyleName() {
        return styleName;
    }
}
