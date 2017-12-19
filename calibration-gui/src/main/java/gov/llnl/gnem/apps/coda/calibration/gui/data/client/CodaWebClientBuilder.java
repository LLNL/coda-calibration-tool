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
package gov.llnl.gnem.apps.coda.calibration.gui.data.client;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslContextConfigurator;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;

@Service
@Scope(scopeName = "singleton")
@ConfigurationProperties(prefix = "webclient")
@Configuration
public class CodaWebClientBuilder {
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String basePath = "localhost:2222";
    private String httpPrefix = "https://";
    private String websocketPrefix = "https://";
    private String apiPath = "/api/v1/";
    private String socketPath = "/websocket-guide/";
    private String websocketBase;

    private String trustStoreName = "coda-truststore.jks";
    private String trustStorePass = "changeit";
    private HostnameVerifier hostnameVerifier = (hostname, session) -> {
        if (hostname == null) {
            return false;
        }
        return basePath.toLowerCase().startsWith(hostname.toLowerCase());
    };
    private SockJsClient sockJsClient;
    private StompSession stompSession;
    private StompSessionHandlerAdapter frameHandler;
    private ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Retry-Thread");
        return thread;
    });
    private WebSocketStompClient stompClient;

    public CodaWebClientBuilder(EventBus bus, StompSessionHandlerAdapter frameHandler) {
        bus.register(this);
        this.frameHandler = frameHandler;
    }

    @PostConstruct
    private void initialize() throws InterruptedException, ExecutionException {
        websocketBase = websocketPrefix + basePath + socketPath;
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        SslEngineConfigurator sslEngineConfigurator = new SslEngineConfigurator(new SslContextConfigurator());
        sslEngineConfigurator.setHostnameVerifier(hostnameVerifier);

        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(trustStoreName)) {
            File tmpFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".tmp");
            tmpFile.deleteOnExit();
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            try (OutputStream outStream = new FileOutputStream(tmpFile)) {
                outStream.write(buffer);
                System.setProperty("javax.net.ssl.trustStore", tmpFile.getAbsolutePath());
                System.setProperty("javax.net.ssl.trustStorePassword", trustStorePass);
                System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load trust store.", e);
        }

        ClientManager client = ClientManager.createClient();
        client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
        StandardWebSocketClient standardClient = new StandardWebSocketClient(client);

        List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(standardClient));
        transports.add(new RestTemplateXhrTransport());

        sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompSession = null;

        connectSocket();
    }

    @Subscribe
    private void disconnectedListener(SocketDisconnectEvent evt) {
        connectSocket();
    }

    private void connectSocket() {
        //Clear existing sessions if we have one
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }

        //TODO: Fire off connect/disconnect events for the UI to display to the user
        SyncFailsafe<StompSession> syncFailsafe = Failsafe.with(new RetryPolicy().withBackoff(1, 30, TimeUnit.SECONDS));
        syncFailsafe.with(retryExecutor).onSuccess(session -> {
            session.subscribe("/topic/status-events", frameHandler);
            stompSession = session;
        }).onRetry((o, ex) -> log.trace("Attempting to reconnect to stomp")).runAsync(execution -> {
            StompSession session = stompClient.connect(websocketBase, frameHandler).get(1, TimeUnit.SECONDS);
            execution.complete(session);
        });

        syncFailsafe.with(retryExecutor).onSuccess(session -> {
            session.subscribe("/topic/calibration-events", frameHandler);
            stompSession = session;
        }).onRetry((o, ex) -> log.trace("Attempting to reconnect to stomp")).runAsync(execution -> {
            StompSession session = stompClient.connect(websocketBase, frameHandler).get(1, TimeUnit.SECONDS);
            execution.complete(session);
        });
    }

    @PreDestroy
    public void close() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        retryExecutor.shutdownNow();
    }

    public String getHttpPrefix() {
        return httpPrefix;
    }

    public void setHttpPrefix(String httpPrefix) {
        this.httpPrefix = httpPrefix;
    }

    public String getWebsocketPrefix() {
        return websocketPrefix;
    }

    public void setWebsocketPrefix(String websocketPrefix) {
        this.websocketPrefix = websocketPrefix;
    }

    public String getHTTPPath() {
        return httpPrefix + basePath + apiPath;
    }

    public String getWebSocketPath() {
        return websocketPrefix + basePath + socketPath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String baseURL) {
        this.basePath = baseURL;
    }

    public @Bean @Scope("prototype") WebClient getWebClient() {
        return WebClient.create(getHTTPPath());
    }
}