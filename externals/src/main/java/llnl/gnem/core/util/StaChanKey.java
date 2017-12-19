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
package llnl.gnem.core.util;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * Created by dodge1 Date: Sep 29, 2010
 */
@SuppressWarnings({"RedundantIfStatement"})
public class StaChanKey implements StreamKey, Comparable<StaChanKey>, Serializable {

    static final long serialVersionUID = -5652680329737865547L;
    private final String sta;
    private final String chan;

    public StaChanKey(String sta, String chan) {
        this.sta = sta;
        this.chan = chan;

    }

    public String getSta() {
        return sta;
    }

    public String getChan() {
        return chan;
    }

    @Override
    public int compareTo(StaChanKey other) {
        if (sta != null) {
            int cmp1 = sta.compareTo(other.sta);
            return (cmp1 != 0 ? cmp1 : chan.compareTo(other.chan));
        }
        return chan.compareTo(other.chan);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof StaChanKey)) {
            return false;
        }
        if (this == o) {
            return true;
        }

        StaChanKey staChan = (StaChanKey) o;

        if (chan != null ? !chan.equals(staChan.chan) : staChan.chan != null) {
            return false;
        }
        if (sta != null ? !sta.equals(staChan.sta) : staChan.sta != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sta, chan);
    }
    
    @Override
    public String getPlotLabel()
    {
        return String.format("%s - %s",sta,chan);
    }

    @Override
    public String toString() {
        return String.format("sta=%s, chan=%s", sta, chan);
    }
}
