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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Result<OutType> {

    private boolean success;

    private List<Exception> errors = new ArrayList<>();;

    private Optional<OutType> resultPayload;

    public Result(boolean success, OutType resultPayload) {
        super();
        this.success = success;
        this.resultPayload = Optional.ofNullable(resultPayload);
    }

    @JsonCreator
    public Result(@JsonProperty("success") boolean success, @JsonProperty("errors") List<Exception> errors, @JsonProperty("resultPayload") OutType resultPayload) {
        super();
        this.success = success;
        this.errors = errors;
        this.resultPayload = Optional.ofNullable(resultPayload);
    }

    public Result<OutType> setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public List<Exception> getErrors() {
        return errors;
    }

    public Result<OutType> setErrors(List<Exception> errors) {
        this.errors = errors;
        return this;
    }

    public Optional<OutType> getResultPayload() {
        return resultPayload;
    }

    public Result<OutType> setResultPayload(Optional<OutType> resultPayload) {
        this.resultPayload = resultPayload;
        return this;
    }

    public boolean isSuccess() {
        return success && resultPayload.isPresent();
    }

    @Override
    public String toString() {
        return "Result [success=" + success + ", errors=" + errors + ", resultPayload=" + resultPayload + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((errors == null) ? 0 : errors.hashCode());
        result = prime * result + resultPayload.hashCode();
        result = prime * result + (success ? 1231 : 1237);
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
        if (!(obj instanceof Result<?>)) {
            return false;
        } else {
            Result<?> other = (Result<?>) obj;
            if (errors == null) {
                if (other.errors != null) {
                    return false;
                }
            } else if (!errors.equals(other.errors)) {
                return false;
            }
            if (!resultPayload.equals(other.resultPayload)) {
                return false;
            }
            if (success != other.success) {
                return false;
            }
        }
        return true;
    }
}
