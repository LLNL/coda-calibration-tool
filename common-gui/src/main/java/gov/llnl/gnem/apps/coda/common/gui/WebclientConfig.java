/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use this file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* This work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.common.gui;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "webclient")
public class WebclientConfig {
    private String basePath = "localhost:2222";
    private String httpPrefix = "https://";
    private String websocketPrefix = "https://";
    private String apiPath = "/api/v1/";
    private String socketPath = "/websocket-guide/";
    private List<String> subscriptions = new ArrayList<>();

    public String getBasePath() {
        return basePath;
    }

    public WebclientConfig setBasePath(String basePath) {
        this.basePath = basePath;
        return this;
    }

    public String getHttpPrefix() {
        return httpPrefix;
    }

    public WebclientConfig setHttpPrefix(String httpPrefix) {
        this.httpPrefix = httpPrefix;
        return this;
    }

    public String getWebsocketPrefix() {
        return websocketPrefix;
    }

    public WebclientConfig setWebsocketPrefix(String websocketPrefix) {
        this.websocketPrefix = websocketPrefix;
        return this;
    }

    public String getApiPath() {
        return apiPath;
    }

    public WebclientConfig setApiPath(String apiPath) {
        this.apiPath = apiPath;
        return this;
    }

    public String getSocketPath() {
        return socketPath;
    }

    public WebclientConfig setSocketPath(String socketPath) {
        this.socketPath = socketPath;
        return this;
    }

    public String getHTTPPath() {
        return httpPrefix + basePath + apiPath;
    }

    public List<String> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<String> subscriptions) {
        this.subscriptions = subscriptions;
    }
}
