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
package gov.llnl.gnem.apps.coda.calibration.model.domain.messaging;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CalibrationStatusEvent {

    public enum Status {
        STARTING, COMPLETE, ERROR
    };

    private Long id;
    private Status status;
    private Result<Exception> error;

    public CalibrationStatusEvent(Long id, Status status) {
        this.id = id;
        this.status = status;
    }

    @JsonCreator
    public CalibrationStatusEvent(@JsonProperty("id") Long id, @JsonProperty("status") Status status, @JsonProperty("error") Result<Exception> error) {
        this.id = id;
        this.status = status;
        this.error = error;
    }

    public Long getId() {
        return id;
    }

    public CalibrationStatusEvent setId(Long id) {
        this.id = id;
        return this;
    }

    public Status getStatus() {
        return status;
    }

    public CalibrationStatusEvent setStatus(Status status) {
        this.status = status;
        return this;
    }

    public Result<Exception> getError() {
        return error;
    }

    public CalibrationStatusEvent setError(Result<Exception> error) {
        this.error = error;
        return this;
    }

    @Override
    public String toString() {
        return "CalibrationStatusEvent [id=" + id + ", status=" + status + ", error=" + error + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((error == null) ? 0 : error.hashCode());
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((status == null) ? 0 : status.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CalibrationStatusEvent other = (CalibrationStatusEvent) obj;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (!error.equals(other.error))
            return false;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (status != other.status)
            return false;
        return true;
    }
}
