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
package gov.llnl.gnem.apps.coda.envelope.model.domain;

import java.util.Collection;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

public class EnvelopeJob {

    private EnvelopeJobConfiguration jobConfig;
    private Collection<Waveform> data;

    public EnvelopeJob() {
    }

    public EnvelopeJobConfiguration getJobConfig() {
        return jobConfig;
    }

    public EnvelopeJob setJobConfig(EnvelopeJobConfiguration jobConfig) {
        this.jobConfig = jobConfig;
        return this;
    }

    public Collection<Waveform> getData() {
        return data;
    }

    public EnvelopeJob setData(Collection<Waveform> data) {
        this.data = data;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((data == null) ? 0 : data.hashCode());
        result = prime * result + ((jobConfig == null) ? 0 : jobConfig.hashCode());
        return result;
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
        EnvelopeJob other = (EnvelopeJob) obj;
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        if (jobConfig == null) {
            if (other.jobConfig != null) {
                return false;
            }
        } else if (!jobConfig.equals(other.jobConfig)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(jobConfig).append("\", \"").append(data).append('\"');
        return builder.toString();
    }

}
