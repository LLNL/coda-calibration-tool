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
package llnl.gnem.core.polygon;

/**
 * User: dodge1 Date: Jul 26, 2005 Time: 12:02:27 PM
 */
public class BinarySearch {

    public static int[] bounds(final double[] array, final double value) {
        return range(array, value, value);
    }

    public static int[] bounds(final float[] array, final float value) {
        return range(array, value, value);
    }

    static public int[] range(final double[] array, final double floor, final double ceiling) {
        final int[] answer = new int[2];
        int high;
        int low;
        int probe;

        // work on floor
        high = array.length;
        low = -1;
        while (high - low > 1) {
            probe = (high + low) >> 1;
            if (array[probe] < floor)
                low = probe;
            else
                high = probe;
        }
        answer[0] = low;

        // work on ceiling
        high = array.length;
        low = -1;
        while (high - low > 1) {
            probe = (high + low) >> 1;
            if (array[probe] > ceiling)
                high = probe;
            else
                low = probe;
        }
        answer[1] = high;
        return answer;
    }

    static public int[] range(final float[] array, final float floor, final float ceiling) {
        final int[] answer = new int[2];
        int high;
        int low;
        int probe;

        // work on floor
        high = array.length;
        low = -1;
        while (high - low > 1) {
            probe = (high + low) >> 1;
            if (array[probe] < floor)
                low = probe;
            else
                high = probe;
        }
        answer[0] = low;

        // work on ceiling
        high = array.length;
        low = -1;
        while (high - low > 1) {
            probe = (high + low) >> 1;
            if (array[probe] > ceiling)
                high = probe;
            else
                low = probe;
        }
        answer[1] = high;
        return answer;
    }

}
