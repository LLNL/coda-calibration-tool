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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.util.List;

public class WMSLayerDescriptor {
    private String url;
    private String name;
    private List<String> layers;

    private WMSLayerDescriptor() {
        //NOP
    }

    public WMSLayerDescriptor(String url, String name, List<String> layers) {
        super();
        this.url = url;
        this.name = name;
        this.layers = layers;
    }

    public String getUrl() {
        return url;
    }

    public WMSLayerDescriptor setUrl(String url) {
        this.url = url;
        return this;
    }

    public String getName() {
        return name;
    }

    public WMSLayerDescriptor setName(String name) {
        this.name = name;
        return this;
    }

    public List<String> getLayers() {
        return layers;
    }

    public WMSLayerDescriptor setLayers(List<String> layers) {
        this.layers = layers;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((layers == null) ? 0 : layers.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((url == null) ? 0 : url.hashCode());
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
        WMSLayerDescriptor other = (WMSLayerDescriptor) obj;
        if (layers == null) {
            if (other.layers != null) {
                return false;
            }
        } else if (!layers.equals(other.layers)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (url == null) {
            if (other.url != null) {
                return false;
            }
        } else if (!url.equals(other.url)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\"").append(url).append("\", \"").append(name).append("\", \"").append(layers).append("\"");
        return builder.toString();
    }

}
