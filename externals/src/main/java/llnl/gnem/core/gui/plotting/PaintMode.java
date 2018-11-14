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

import java.awt.Color;
import java.awt.Graphics;

/**
 * A type-safe enum to represent the paint mode of objects drawn in the axis.
 *
 * @author Doug Dodge
 */
public class PaintMode {
    private final String name;

    private PaintMode(String name) {
        this.name = name;
    }

    /**
     * Return a String description of this type.
     *
     * @return The String description
     */
    public String toString() {
        return name;
    }

    /**
     * Sets the graphics PaintMode based on the PaintMode type
     *
     * @param g
     *            The graphics context to be operated on.
     */
    public void setGraphicsPaintMode(Graphics g) {
        if (name.equals("Copy"))
            g.setPaintMode();
        else
            g.setXORMode(Color.white);
    }

    /**
     * PaintMode for doing SRCCOPY
     */
    public final static PaintMode COPY = new PaintMode("Copy");
    /**
     * PaintMode for doing XOR
     */
    public final static PaintMode XOR = new PaintMode("Xor");
}
