/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

package gov.llnl.gnem.apps.coda.spectra.model.domain;

import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RatioEventData {
    @JsonProperty("evid")
    private String eventId;

    @JsonProperty("date")
    @DateTimeFormat(style = "M-")
    private Date date;
    @JsonProperty("stations")
    private List<RatioEventStationData> stationData;

    public RatioEventData() {
        this.eventId = null;
        this.date = null;
        this.stationData = null;
    }

    public RatioEventData(String eventId, Date date, List<RatioEventStationData> stationData) {
        this.eventId = eventId;
        this.date = date;
        this.stationData = stationData;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public List<RatioEventStationData> getStationData() {
        return stationData;
    }

    public void setStationData(List<RatioEventStationData> stationData) {
        this.stationData = stationData;
    }
}
