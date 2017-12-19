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
package gov.llnl.gnem.apps.coda.calibration.gui.util;

import java.util.Comparator;

import org.apache.commons.lang3.StringUtils;

public class MaybeNumericStringComparator implements Comparator<String> {

    @Override
    public int compare(String s1, String s2) {
        if (s1 == s2) {
            return 0;
        } else if (s1 == null) {
            return -1;
        } else if (s2 == null) {
            return 1;
        }
        if (StringUtils.isNumeric(s1) && StringUtils.isNumeric(s2)) {
            int val = -1;
            try {
                val = Long.valueOf(s1).compareTo(Long.valueOf(s2));
            } catch (NumberFormatException e) {
            }
            return val;
        } else if (StringUtils.containsOnly(s1, "0123456789.") && StringUtils.containsOnly(s2, "0123456789.")) {
            int val = -1;
            try {
                val = Double.valueOf(s1).compareTo(Double.valueOf(s2));
            } catch (NumberFormatException e) {
            }
            return val;
        } else {
            return s1.compareTo(s2);
        }
    }

}
