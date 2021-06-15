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
package llnl.gnem.core.gui.swing.plotting.epochTimePlot;

import llnl.gnem.core.gui.swing.plotting.HorizAlignment;
import llnl.gnem.core.gui.swing.plotting.TickLabel;

/**
 *
 * @author dodge
 */
public class TickValue {
    private final double offset;
    private final TickLabel label;
    private final boolean major;
    private final HorizAlignment horizAlignment;
    private final TickTimeType type;

    public TickValue(double offset, TickLabel label, boolean isMajor, HorizAlignment alignment, TickTimeType type) {
        this.offset = offset;
        this.label = label;
        this.major = isMajor;
        this.horizAlignment = alignment;
        this.type = type;
    }

    /**
     * @return the offset
     */
    public double getOffset() {
        return offset;
    }

    /**
     * @return the label
     */
    public TickLabel getLabel() {
        return label;
    }

    /**
     * @return the isMajor
     */
    public boolean isMajor() {
        return major;
    }

    /**
     * @return the horizAlignment
     */
    public HorizAlignment getHorizAlignment() {
        return horizAlignment;
    }

    /**
     * @return the type
     */
    public TickTimeType getType() {
        return type;
    }

}
