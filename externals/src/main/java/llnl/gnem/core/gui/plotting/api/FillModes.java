/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
* All rights reserved.
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

public enum FillModes {

    DEFAULT("none"), TO_ZERO_Y("tozeroy"), TO_ZERO_X("tozerox"), TO_NEXT_Y("tonexty"), TO_NEXT_X("tonextx"), TO_SELF("toself"), TO_NEXT("tonext");

    private final String fillMode;

    private FillModes(String fillMode) {
        this.fillMode = fillMode;
    }

    public String getFillModeName() {
        return fillMode;
    }
}
