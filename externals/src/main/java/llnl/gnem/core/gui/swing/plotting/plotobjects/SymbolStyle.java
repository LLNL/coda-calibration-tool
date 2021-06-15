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
/**
 * User: dodge1
 * Date: Mar 12, 2004
 * Time: 2:40:41 PM
 */
package llnl.gnem.core.gui.swing.plotting.plotobjects;

/**
 * An enumeration of the available Symbol styles. Any new symbols added to the
 * collection must have entries made in this class.
 */
public enum SymbolStyle {
    NONE("NONE"), CIRCLE("Circle"), SQUARE("Square"), DIAMOND("Diamond"), TRIANGLEUP("TriangleUp"), TRIANGLEDN("TriangleDn"), PLUS("Plus"), CROSS("Cross"), STAR5("Star5"), HEXAGON(
            "Hexagon"), ERROR_BAR("ErrorBar");

    private final String name;

    private SymbolStyle(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /***
     * 
     * @param name
     * @return {@link SymbolStyle} matching name (ignores case)
     * @throws {@link
     *             IllegalArgumentException} if the name is not defined.
     */
    public static SymbolStyle getSymbolStyle(final String name) {
        for (SymbolStyle style : SymbolStyle.values()) {
            if (style.toString().equalsIgnoreCase(name)) {
                return style;
            }
        }
        throw new IllegalArgumentException("Not a valid Style");
    }
}
