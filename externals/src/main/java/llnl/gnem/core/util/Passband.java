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
package llnl.gnem.core.util;

/**
 * A type-safe enum that describes the passband for a filter.
 *
 * @author Doug Dodge
 */
public class Passband {
    private final String name;

    private Passband(String name) {
        this.name = name;
    }

    /**
     * A string representation of the passband for a filter suitable for use
     * with the dbh.ButterworthFilter..
     *
     * @return A String with the one of the values "LP", or "HP", "BP", "BR"
     */
    public String toString() {
        return name;
    }

    /**
     * Get all the passband codes known to this class
     *
     * @return A String array containing all the passband codes
     */
    public static String[] getPassBandCodes() {
        String[] codes = { "BP", "BR", "LP", "HP" };
        return codes;
    }

    /**
     * Returns a Passband object given a 2-character String descriptor. Only
     * recognized codes are "BP", "LP", "HP", "BR". Any other String will result
     * in a null Passband object.
     *
     * @param code
     *            The code of the desired passband object.
     * @return The specified Passband object.
     */
    public static Passband getPassbandFromString(final String code) {
        if (code.equals("BP"))
            return Passband.BAND_PASS;
        else if (code.equals("LP"))
            return Passband.LOW_PASS;
        else if (code.equals("HP"))
            return Passband.HIGH_PASS;
        else if (code.equals("BR"))
            return Passband.BAND_REJECT;
        else
            return null;
    }

    public static Passband[] getAvailablePassBands() {
        Passband[] result = new Passband[4];
        result[0] = BAND_PASS;
        result[1] = BAND_REJECT;
        result[2] = LOW_PASS;
        result[3] = HIGH_PASS;
        return result;
    }

    /**
     * Specifies a band-pass filter
     */
    public final static Passband BAND_PASS = new Passband("BP");
    /**
     * Specifies a band-reject filter
     */
    public final static Passband BAND_REJECT = new Passband("BR");
    /**
     * Specifies a low-pass filter
     */
    public final static Passband LOW_PASS = new Passband("LP");
    /**
     * Specifies a high-pass filter
     */
    public final static Passband HIGH_PASS = new Passband("HP");
}
