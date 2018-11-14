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
package llnl.gnem.core.waveform.responseProcessing;

import java.util.Locale;

/**
 * A type-safe enum class for response types.
 *
 * @author Doug Dodge
 */
public enum ResponseType {

    DIS("dis"), NONE("none"), VEL("vel"), ACC("acc"), EVRESP("evresp"), SACPZF("sacpzf"), PAZ("paz"), FAP("fap"), PAZFIR("pazfir"), PAZFAP("pazfap");

    private final String dbValue;

    ResponseType(String dbvalue) {
        this.dbValue = dbvalue;
    }

    public boolean isNDCType() {
        return this == PAZ || this == FAP || this == PAZFIR;
    }

    public String getDbValue() {
        return dbValue;
    }

    /**
     * Utility method to convert a String representation of the response type.
     * This methods supports aliases in addition to the 'name' for some of the
     * ResponseTypes.
     * 
     * @param type
     *            String representing the response type
     * @return ResponseType enum
     */
    public static ResponseType getResponseType(String type) {
        type = type.toLowerCase(Locale.ENGLISH);

        if (type.equals("displacement") || type.equals("dis")) {
            return ResponseType.DIS;
        } else if (type.equals("velocity") || type.equals("vel")) {
            return ResponseType.VEL;
        } else if (type.equals("acceleration") || type.equals("acc")) {
            return ResponseType.ACC;
        } else if (type.equals("evresp") || type.equals("resp")) {
            return ResponseType.EVRESP;
        } else if (type.equals("sacpzf") || type.equals("sacpz") || type.equals("polezero") || type.equals("pz")) {
            return ResponseType.SACPZF;
        } else if (type.equals("paz")) {
            return ResponseType.PAZ;
        } else if (type.equals("fap")) {
            return ResponseType.FAP;
        } else if (type.equals("pazfir")) {
            return ResponseType.PAZFIR;
        } else
            return ResponseType.NONE;
    }
}
