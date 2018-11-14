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
package gov.llnl.gnem.apps.coda.common.gui.data.client;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
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

import gov.llnl.gnem.apps.coda.common.gui.events.SocketDisconnectEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.SslUtils;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.JdkSslContext;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;
import reactor.netty.http.client.HttpClient;

@Service
@Scope(scopeName = "singleton")
@ConfigurationProperties(prefix = "webclient")
@Configuration
public class CodaWebClientBuilder {

    private static final Logger log = LoggerFactory.getLogger(CodaWebClientBuilder.class);
    private String basePath = "localhost:2222";
    private String httpPrefix = "https://";
    private String websocketPrefix = "https://";
    private String apiPath = "/api/v1/";
    private String socketPath = "/websocket-guide/";
    private String websocketBase;

    private List<String> subscriptions = new ArrayList<>();

    private String trustStoreName = "coda-truststore.jks";

    private final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
    private HostnameVerifier hostnameVerifier = (hostname, session) -> {
        if (hostname == null) {
            return false;
        }
        boolean local = basePath.toLowerCase(Locale.ENGLISH).startsWith(hostname.toLowerCase(Locale.ENGLISH));
        if (!local) {
            return defaultHostnameVerifier.verify(hostname, session);
        }
        return local;
    };
    private SockJsClient sockJsClient;
    private StompSession stompSession;
    private StompSessionHandlerAdapter frameHandler;
    private ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("Retry-Stomp-Thread");
        return thread;
    });
    private WebSocketStompClient stompClient;
    private SslEngineConfigurator sslEngineConfigurator;
    private ReactorClientHttpConnector connector;

    public CodaWebClientBuilder(EventBus bus, StompSessionHandlerAdapter frameHandler) {
        bus.register(this);
        this.frameHandler = frameHandler;
        if (subscriptions.isEmpty()) {
            subscriptions.add("/topic/status-events");
        }
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        SSLContext sc;
        try (InputStream keyStore = classLoader.getResourceAsStream(trustStoreName)) {
            sc = SslUtils.initMergedSSLTrustStore(keyStore);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            System.setProperty("sun.security.ssl.allowUnsafeRenegotiation", "true");
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to load trust store.", e);
        }

        JdkSslContext sslContext = new JdkSslContext(sc, true, ClientAuth.OPTIONAL);
        connector = new ReactorClientHttpConnector(HttpClient.create().secure(t -> t.sslContext(sslContext)));
        sslEngineConfigurator = new SslEngineConfigurator(sc);
        sslEngineConfigurator.setHostnameVerifier(hostnameVerifier);
    }

    @PostConstruct
    private void initialize() throws InterruptedException, ExecutionException {
        websocketBase = websocketPrefix + basePath + socketPath;
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
        syncFailsafe.with(retryExecutor).onRetry((s, ex) -> {
            log.trace("Attempting to connect to stomp: {}", ex);
        }).runAsync(execution -> {
            stompSession = stompClient.connect(websocketBase, frameHandler).get(1, TimeUnit.SECONDS);
            subscriptions.forEach(topic -> stompSession.subscribe(topic.replaceAll("\"", ""), frameHandler));
            execution.complete(stompSession);
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
        return WebClient.builder().clientConnector(connector).baseUrl(getHTTPPath()).build();
    }

    public List<String> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<String> subscriptions) {
        this.subscriptions = subscriptions;
    }
}