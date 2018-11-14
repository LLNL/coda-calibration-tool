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

/**
 * A type-safe enum to represent the dashing style of lines drawn in the axis.
 *
 * @author Doug Dodge
 */
public enum PenStyle {
    NONE("None", null), SOLID("Solid", null), DASH("Dash", new float[] { 10.0F, 10.0F }), DOT("Dot", new float[] { 1.0F, 5.0F }), DASHDOT("DashDot",
            new float[] { 10.0F, 5.0F, 1.0F, 5.0F }), DASHDOTDOT("DashDotDot", new float[] { 10.0F, 5.0F, 1.0F, 5.0F, 1.0F, 5.0F });

    String name;
    float[] pattern;

    private PenStyle(String name, float[] template) {
        this.name = name;
        pattern = template;
    }

    /**
     * Return a String description of this type.
     *
     * @return The String description
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the float array that defines the dashing pattern of a BasicStroke
     * object for this PenStyle.
     *
     * @return The pattern value
     */
    public float[] getPattern() {
        return pattern;
    }

    /**
     * Gets a new stroke of the specified width using the pattern for this
     * PenStyle.
     *
     * @param width
     *            Width of the requested BasicStroke
     * @return The new BasicStroke object
     */
    public BasicStroke getStroke(float width) {
        return new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, pattern, 0.0f);
    }

    public static PenStyle[] getAllStyles() {
        return PenStyle.values();
    }

    public static PenStyle getPenStyle(String style) {
        for (PenStyle astyle : PenStyle.values()) {
            if (astyle.toString().equalsIgnoreCase(style)) {
                return astyle;
            }
        }
        throw new IllegalArgumentException("Invalid style string: " + style);
    }

}
