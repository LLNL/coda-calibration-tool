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
package llnl.gnem.core.metadata;

/**
 *
 * @author dodge1
 */
public enum BandCode {

    F("... ≥ 1000 to < 5000 ≥ 10 sec"), 
    G("... ≥ 1000 to < 5000 < 10 sec"), 
    D("... ≥ 250 to < 1000 < 10 sec"), 
    C("... ≥ 250 to < 1000 ≥ 10 sec"), 
    E("Extremely Short Period ≥ 80 to < 250 < 10 sec"), 
    S("Short Period ≥ 10 to < 80 < 10 sec"), 
    H("High Broad Band ≥ 80 to < 250 ≥ 10 sec"), 
    B("Broad Band ≥ 10 to < 80 ≥ 10 sec"), 
    M("Mid Period > 1 to < 10"), 
    L("Long Period ≈ 1"), 
    V("Very Long Period ≈ 0.1"), 
    U("Ultra Long Period ≈ 0.01"), 
    R("Extremely Long Period ≥ 0.0001 to < 0.001"), 
    P("On the order of 0.1 to 1 day ≥ 0.00001 to< 0.0001"), 
    T("On the order of 1 to 10 days ≥ 0.000001 to<0.00001"), 
    Q("Greater than 10 days < 0.000001"), 
    A("Administrative Instrument Channel variable NA"), 
    O("Opaque Instrument Channel variable NA");
    private final String description;

    BandCode(String descrip) {
        description = descrip;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
