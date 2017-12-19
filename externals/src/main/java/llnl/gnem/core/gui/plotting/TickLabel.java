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
package llnl.gnem.core.gui.plotting;

/**
 *
 * @author dodge
 */
public class TickLabel {

    private final String label1;
    private final String label2;

    public TickLabel() {
        label1 = null;
        label2 = null;
    }

    public TickLabel(String label) {
        label1 = label;
        label2 = null;
    }

    public TickLabel(String label1, String label2) {
        this.label1 = label1;
        this.label2 = label2;
    }

    public boolean hasLabel1() {
        return label1 != null && !label1.isEmpty();
    }

    public boolean hasLabel2() {
        return label2 != null && !label2.isEmpty();
    }

    public String getLabel1() {
        return label1;
    }

    public String getLabel2() {
        return label2;
    }
}
