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
package gov.llnl.gnem.apps.coda.calibration.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.common.mapping.WMSLayerDescriptor;

@Component
@ConfigurationProperties("app")
public class AppProperties {
    private String baseTitle = "";
    private Integer height = 800;
    private Integer width = 600;
    private Boolean debugEnabled = Boolean.FALSE;
    private List<WMSLayerDescriptor> wmsLayers = new ArrayList<>();

    public Boolean getDebugEnabled() {
        return debugEnabled;
    }

    public void setDebugEnabled(Boolean debugEnabled) {
        this.debugEnabled = debugEnabled;
    }

    public String getBaseTitle() {
        return baseTitle;
    }

    public void setBaseTitle(String baseTitle) {
        this.baseTitle = baseTitle;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public List<WMSLayerDescriptor> getWmsLayers() {
        return wmsLayers;
    }

    public void setWmsLayers(List<WMSLayerDescriptor> wmsLayers) {
        this.wmsLayers = wmsLayers;
    }
}
