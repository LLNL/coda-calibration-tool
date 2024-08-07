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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.glassfish.tyrus.client.SslEngineConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.Builder;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.common.gui.WebclientConfig;
import gov.llnl.gnem.apps.coda.common.gui.events.SocketDisconnectEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.SslUtils;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.FailsafeExecutor;
import net.jodah.failsafe.RetryPolicy;
import reactor.netty.http.client.HttpClient;

@Service
@Scope(scopeName = "singleton")
@Configuration
public class CodaWebClientBuilder {

    private WebclientConfig config;
    private static final Logger log = LoggerFactory.getLogger(CodaWebClientBuilder.class);
    private String websocketBase;
    private final String trustStoreName = "coda-truststore.jks";

    private final HostnameVerifier defaultHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
    private final HostnameVerifier hostnameVerifier = (hostname, session) -> {
        if (hostname == null) {
            return false;
        }
        final boolean local = config.getBasePath().toLowerCase(Locale.ENGLISH).startsWith(hostname.toLowerCase(Locale.ENGLISH));
        if (!local) {
            return defaultHostnameVerifier.verify(hostname, session);
        }
        return local;
    };
    private SockJsClient sockJsClient;
    private StompSession stompSession;
    private final StompSessionHandlerAdapter frameHandler;
    private final ScheduledExecutorService retryExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        final Thread thread = new Thread(r);
        thread.setName("Webclient-Retry");
        thread.setDaemon(true);
        thread.setName("Retry-Stomp-Thread");
        return thread;
    });
    private WebSocketStompClient stompClient;
    private SslEngineConfigurator sslEngineConfigurator;
    private ReactorClientHttpConnector connector;
    private final ExchangeStrategies strategies;
    private SSLContext sc;
    private ApplicationContext appContext;

    public CodaWebClientBuilder(final EventBus bus, ApplicationContext appContext, final WebclientConfig config, final StompSessionHandlerAdapter frameHandler,
            @Nullable final ExchangeStrategies strategies) {
        this.appContext = appContext;
        this.config = config;
        this.strategies = strategies;
        bus.register(this);
        this.frameHandler = frameHandler;
        if (config.getSubscriptions().isEmpty()) {
            config.getSubscriptions().add("/topic/status-events");
        }
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream keyStore = classLoader.getResourceAsStream(trustStoreName)) {
            sc = SslUtils.initMergedSSLTrustStore(keyStore);
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            SSLContext.setDefault(sc);

            sslEngineConfigurator = new SslEngineConfigurator(sc);
            sslEngineConfigurator.setHostnameVerifier(hostnameVerifier);
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to load trust store.", e);
        }
        try (InputStream keyStore = classLoader.getResourceAsStream(trustStoreName)) {
            final SslContext sslContext = SslUtils.initMergedSSLTrustStore(SslContextBuilder::forClient, keyStore);
            connector = new ReactorClientHttpConnector(HttpClient.create().secure(t -> t.sslContext(sslContext)));
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Unable to load trust store.", e);
        }
    }

    @PostConstruct
    private void initialize() throws InterruptedException, ExecutionException {
        websocketBase = config.getWebsocketPrefix() + config.getBasePath() + config.getSocketPath();
        final ClientManager client = ClientManager.createClient();
        client.getProperties().put(ClientProperties.SSL_ENGINE_CONFIGURATOR, sslEngineConfigurator);
        final StandardWebSocketClient standardClient = new StandardWebSocketClient(client);

        final List<Transport> transports = new ArrayList<>(2);
        transports.add(new WebSocketTransport(standardClient));
        transports.add(new RestTemplateXhrTransport());

        sockJsClient = new SockJsClient(transports);

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompSession = null;

        connectSocket();
    }

    @Subscribe
    private void disconnectedListener(final SocketDisconnectEvent evt) {
        connectSocket();
    }

    private void connectSocket() {
        //Clear existing sessions if we have one
        if (stompSession != null && stompSession.isConnected()) {
            try {
                stompSession.disconnect();
            } catch (final RuntimeException ex) {
                //Socket is likely closed, this should not be a fatal exception
                log.trace(ex.getLocalizedMessage());
            }
        }

        //TODO: Fire off connect/disconnect events for the UI to display to the user
        final FailsafeExecutor<StompSession> failsafe = Failsafe.with(new RetryPolicy<StompSession>().withBackoff(1l, 30l, ChronoUnit.SECONDS).withMaxRetries(-1));
        failsafe.with(retryExecutor).onFailure(evt -> {
            log.trace("Attempting to connect to stomp: {}", evt);
        }).runAsync(r -> {
            stompSession = stompClient.connectAsync(websocketBase, frameHandler).get(1, TimeUnit.SECONDS);
            config.getSubscriptions().forEach(topic -> stompSession.subscribe(topic.replaceAll("\"", ""), frameHandler));
        });
    }

    @PreDestroy
    public void close() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
        retryExecutor.shutdownNow();
    }

    public @Bean @Scope("prototype") WebClient getWebClient() {
        final Builder builder = WebClient.builder().clientConnector(connector).baseUrl(config.getHTTPPath());
        if (strategies != null) {
            builder.exchangeStrategies(strategies);
        }

        return builder.build();
    }

    public @Bean @Scope("prototype") HostnameVerifier getHostnameVerifier() {
        return hostnameVerifier;
    }

    public @Bean @Scope("prototype") SSLContext getSSLContext() {
        return sc;
    }
}