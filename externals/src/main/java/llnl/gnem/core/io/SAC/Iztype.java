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
package llnl.gnem.core.io.SAC;

/**
 * User: dodge1
 * Date: Oct 19, 2006
 * Time: 9:39:56 AM
 */
public enum Iztype {
    IUNKN("Unknown", 5),
    IB("Begin Time", 9),
    IDAY("Midnight of reference GMT day", 10),
    IO("Event origin time", 11),
    IA("First arrival time", 12),
    IT0("User pick t0 time", 13),
    IT1("User pick t1 time", 14),
    IT2("User pick t2 time", 15),
    IT3("User pick t3 time", 16),
    IT4("User pick t4 time", 17),
    IT5("User pick t5 time", 18),
    IT6("User pick t6 time", 19),
    IT7("User pick t7 time", 20),
    IT8("User pick t8 time", 21),
    IT9("User pick t9 time", 22);


    int code;
    String value;

    Iztype(String value, int code) {
        this.value = value;
        this.code = code;
    }

    public String toString() {
        return value;
    }

    public int getCode() {
        return code;
    }

    public static Iztype getIztype( int v )
    {
        for( Iztype type : Iztype.values() ){
            if( type.getCode() == v )
                return type;
        }
        return Iztype.IUNKN;
    }
}
