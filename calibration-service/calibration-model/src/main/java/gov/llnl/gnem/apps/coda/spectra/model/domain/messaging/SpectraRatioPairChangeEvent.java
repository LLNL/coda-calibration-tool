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
package gov.llnl.gnem.apps.coda.spectra.model.domain.messaging;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SpectraRatioPairChangeEvent {

    private List<Long> ids;
    private boolean isAddOrUpdate = false;
    private boolean isDelete = false;

    public List<Long> getIds() {
        return ids;
    }

    public SpectraRatioPairChangeEvent() {
        ids = new ArrayList<Long>();
    }

    public SpectraRatioPairChangeEvent(List<Long> ids) {
        this.ids = ids;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("SpectraRatioPairChangeEvent [ids=").append(ids).append(']');
        return builder.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(ids);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SpectraRatioPairChangeEvent other = (SpectraRatioPairChangeEvent) obj;
        if (!Objects.equals(ids, other.ids)) {
            return false;
        }
        return true;
    }

    public boolean isAddOrUpdate() {
        return isAddOrUpdate;
    }

    public boolean isDelete() {
        return isDelete;
    }

    public SpectraRatioPairChangeEvent setAddOrUpdate(boolean isAddOrUpdate) {
        this.isAddOrUpdate = isAddOrUpdate;
        return this;
    }

    public SpectraRatioPairChangeEvent setDelete(boolean isDelete) {
        this.isDelete = isDelete;
        return this;
    }
}
