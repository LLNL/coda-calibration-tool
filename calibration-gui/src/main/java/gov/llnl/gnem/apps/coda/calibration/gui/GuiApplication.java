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

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import com.google.common.eventbus.EventBus;

import gov.llnl.gnem.apps.coda.calibration.gui.events.CalibrationStageShownEvent;
import gov.llnl.gnem.apps.coda.common.gui.SimpleGuiPreloader;
import gov.llnl.gnem.apps.coda.common.gui.util.CommonGuiUtils;
import jakarta.annotation.PostConstruct;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@ComponentScan("gov.llnl.gnem.apps.coda.common.mapping")
@ComponentScan("gov.llnl.gnem.apps.coda.common.gui")
@ComponentScan("gov.llnl.gnem.apps.coda.envelope.model")
@ComponentScan("gov.llnl.gnem.apps.coda.envelope.gui")
@ComponentScan("gov.llnl.gnem.apps.coda.calibration.gui")
@ComponentScan("gov.llnl.gnem.apps.coda.spectra.gui")
public class GuiApplication extends Application {

    public enum ApplicationMode {
        CERT, CCT
    }

    static final String CERT_TITLE = "Coda Envelope Ratio Tool";
    static final String CCT_TITLE = "Coda Calibration Tool";

    private static final Logger log = LoggerFactory.getLogger(GuiApplication.class);

    private static ConfigurableApplicationContext springContext;

    private static Stage primaryStage;

    private EventBus bus;

    private static ApplicationMode startupMode;

    @PostConstruct
    void started() {
        Locale.setDefault(Locale.ENGLISH);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public GuiApplication() {
    }

    public GuiApplication(ConfigurableApplicationContext springContext, EventBus bus, ApplicationMode mode) {
        GuiApplication.springContext = springContext;
        this.bus = bus;
        GuiApplication.startupMode = mode;
    }

    public static void main(String[] args) {
        String preloaderName = System.getProperty("javafx.preloader");
        if (preloaderName == null) {
            System.setProperty("javafx.preloader", SimpleGuiPreloader.class.getName());
        }
        //Until we drop Java 8 support we have to disable HTTP2
        // because it uses a different path to set the SSLContext that
        // we don't have access to.
        System.setProperty("com.sun.webkit.useHTTP2Loader", "false");
        launch(GuiApplication.class, args);
    }

    @Override
    public void init() throws Exception {
        CommonGuiUtils.setIcon(this.getClass(), "/coda_256x256.png");
        springContext = new SpringApplicationBuilder(GuiApplication.class).bannerMode(Mode.OFF).web(WebApplicationType.NONE).headless(false).build().run();
        if (bus == null) {
            bus = springContext.getBean(EventBus.class);
        }
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        GuiApplication.primaryStage = primaryStage;
        try (InputStream icon1 = this.getClass().getResourceAsStream("/coda_32x32.png");
                InputStream icon2 = this.getClass().getResourceAsStream("/coda_64x64.png");
                InputStream icon3 = this.getClass().getResourceAsStream("/coda_128x128.png");
                InputStream icon4 = this.getClass().getResourceAsStream("/coda_256x256.png");) {
            primaryStage.getIcons().add(new Image(icon1));
            primaryStage.getIcons().add(new Image(icon2));
            primaryStage.getIcons().add(new Image(icon3));
            primaryStage.getIcons().add(new Image(icon4));
        }
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.setOnShown(evt -> bus.post(new CalibrationStageShownEvent()));

        AppProperties props = springContext.getBean(AppProperties.class);

        // Enable Reactor debugging stack-traces; these are very slow!
        if (props.getDebugEnabled()) {
            Hooks.onOperatorDebug();
        }

        String baseTitle = "";
        if (GuiApplication.getStartupMode() == ApplicationMode.CCT) {
            baseTitle = CCT_TITLE;
        } else {
            baseTitle = CERT_TITLE;
        }

        props.setBaseTitle(baseTitle);
        FXMLLoader fxmlLoader = null;

        if (startupMode == ApplicationMode.CERT) {
            fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/CertGui.fxml"));
        } else {
            fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/CodaGui.fxml"));
        }
        fxmlLoader.setControllerFactory(springContext::getBean);

        try {
            Parent root = fxmlLoader.load();
            Platform.runLater(() -> {
                Font.loadFont(GuiApplication.class.getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
                primaryStage.setTitle(props.getBaseTitle());
                Scene scene = new Scene(root, props.getHeight(), props.getWidth());
                primaryStage.setScene(scene);
                primaryStage.show();
            });
        } catch (IllegalStateException | IOException e) {
            log.error("Unable to load main panel FXML file, terminating. {}", e.getMessage(), e);
            Platform.exit();
        }
    }

    static public void changeApplicationMode() {
        if (GuiApplication.getStartupMode() == ApplicationMode.CCT) {
            GuiApplication.startupMode = ApplicationMode.CERT;
        } else {
            GuiApplication.startupMode = ApplicationMode.CCT;
        }

        Platform.runLater(() -> {
            primaryStage.close();
        });

        AppProperties props = springContext.getBean(AppProperties.class);
        FXMLLoader fxmlLoader = null;
        if (GuiApplication.startupMode == ApplicationMode.CERT) {
            props.setBaseTitle(CERT_TITLE);
            fxmlLoader = new FXMLLoader(GuiApplication.class.getResource("/fxml/CertGui.fxml"));
        } else {
            props.setBaseTitle(CCT_TITLE);
            fxmlLoader = new FXMLLoader(GuiApplication.class.getResource("/fxml/CodaGui.fxml"));
        }
        fxmlLoader.setControllerFactory(springContext::getBean);

        try {
            Parent root = fxmlLoader.load();
            Platform.runLater(() -> {
                Font.loadFont(GuiApplication.class.getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
                primaryStage.setTitle(props.getBaseTitle());
                Scene scene = new Scene(root, props.getHeight(), props.getWidth());
                primaryStage.setScene(scene);
                primaryStage.show();
            });
        } catch (IllegalStateException | IOException e) {
            log.error("Unable to load main panel FXML file, terminating. {}", e.getMessage(), e);
            Platform.exit();
        }
    }

    @Override
    public void stop() throws Exception {
        try {
            CompletableFuture.runAsync(() -> {
                springContext.stop();
                springContext.close();
            }).get(1, TimeUnit.SECONDS);
        } catch (TimeoutException | ExecutionException | CancellationException e) {
        }
        Platform.exit();
        System.exit(0);
    }

    public static ApplicationMode getStartupMode() {
        return startupMode;
    }

    public static void setStartupMode(ApplicationMode startupMode) {
        GuiApplication.startupMode = startupMode;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        GuiApplication.primaryStage = primaryStage;
    }
}
