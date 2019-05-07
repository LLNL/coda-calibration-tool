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
package gov.llnl.gnem.apps.coda.envelope.gui;

import java.io.IOException;
import java.net.URL;
import java.util.TimeZone;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import gov.llnl.gnem.apps.coda.common.gui.SimpleGuiPreloader;
import gov.llnl.gnem.apps.coda.common.gui.util.CommonGuiUtils;
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
@ComponentScan("gov.llnl.gnem.apps.coda.common.gui")
@ComponentScan("gov.llnl.gnem.apps.coda.envelope.gui")
@ComponentScan("gov.llnl.gnem.apps.coda.envelope.model")
public class GuiApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(GuiApplication.class);

    private ConfigurableApplicationContext springContext;

    private Stage primaryStage;

    @PostConstruct
    void started() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    public GuiApplication() {
    }

    public GuiApplication(ConfigurableApplicationContext springContext) {
        this.springContext = springContext;
    }

    public static void main(String[] args) {
        String preloaderName = System.getProperty("javafx.preloader");
        if (preloaderName == null) {
            System.setProperty("javafx.preloader", SimpleGuiPreloader.class.getName());
        }
        launch(GuiApplication.class, args);
    }

    @Override
    public void init() throws Exception {
        CommonGuiUtils.setIcon(this.getClass(), "/coda_256x256.png");
        springContext = new SpringApplicationBuilder(GuiApplication.class).bannerMode(Mode.OFF).web(WebApplicationType.NONE).headless(false).run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/coda_32x32.png")));
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/coda_64x64.png")));
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/coda_128x128.png")));
        primaryStage.getIcons().add(new Image(this.getClass().getResourceAsStream("/coda_256x256.png")));
        primaryStage.setOnCloseRequest((evt) -> Platform.exit());

        AppProperties props = springContext.getBean(AppProperties.class);

        // Enable Reactor debugging stack-traces; these are very slow!
        if (props.getDebugEnabled()) {
            Hooks.onOperatorDebug();
        }

        try {
            Class<GuiApplication> clazz = GuiApplication.class;
            String className = clazz.getSimpleName() + ".class";
            String classPath = clazz.getResource(className).toString();
            String baseTitle = props.getBaseTitle();
            if (classPath.startsWith("jar")) {
                String manifestPath = classPath.substring(0, classPath.indexOf('!') + 1) + "/META-INF/MANIFEST.MF";
                Manifest mf = new Manifest(new URL(manifestPath).openStream());
                Attributes atts = mf.getMainAttributes();
                // Put this info in the log to help with analysis
                log.info(
                        "Version:{} Commit:{} Branch:{} By:{} at {}",
                            atts.getValue("Implementation-Version"),
                            atts.getValue("Implementation-Build"),
                            atts.getValue("Build-Branch"),
                            atts.getValue("Built-By"),
                            atts.getValue("Build-Timestamp"));
                // Update the title bar
                baseTitle += " Built at " + atts.getValue("Build-Timestamp");
            } else {
                // Class not from JAR
                log.info("{} not running from a jar.", baseTitle);
            }
            props.setBaseTitle(baseTitle);
        } catch (IOException e) {
            // should never happen...
            log.error("Failed initializing!", e);
        }

        Platform.setImplicitExit(true);
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/fxml/EnvelopeGui.fxml"));
        fxmlLoader.setControllerFactory(springContext::getBean);

        try {
            Font.loadFont(getClass().getResource("/fxml/MaterialIcons-Regular.ttf").toExternalForm(), 18);
            Parent root = fxmlLoader.load();
            Platform.runLater(() -> {
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

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}
