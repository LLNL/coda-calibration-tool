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
package gov.llnl.gnem.apps.coda.calibration.standalone;

import java.awt.Toolkit;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import gov.llnl.gnem.apps.coda.calibration.CalibrationApplication;
import gov.llnl.gnem.apps.coda.calibration.gui.CodaGuiPreloader;
import gov.llnl.gnem.apps.coda.calibration.gui.GuiApplication;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.stage.Stage;

@SpringBootApplication
@EnableAutoConfiguration
public class CodaCalibrationStandalone extends Application {

    private static final Logger log = LoggerFactory.getLogger(CodaCalibrationStandalone.class);
    private static volatile ConfigurableApplicationContext springContext;
    private static String[] initialArgs;

    public static synchronized void main(String[] args) {
        try {
            initialArgs = args;
            String preloaderName = System.getProperty("javafx.preloader");
            if (preloaderName == null) {
                System.setProperty("javafx.preloader", CodaGuiPreloader.class.getName());
            }
            final CountDownLatch latch = new CountDownLatch(1);
            SwingUtilities.invokeLater(() -> {
                new JFXPanel(); // initializes JavaFX environment
                latch.countDown();
            });

            if (!latch.await(5L, TimeUnit.SECONDS)) {
                throw new ExceptionInInitializerError();
            }
            launch(CodaCalibrationStandalone.class, args);
        } catch (Exception e) {
            log.error("Exception at CodaCalibrationStandalone::main", e);
            Platform.exit();
            System.exit(1);
        }
    }

    @Override
    public void init() throws Exception {
        SwingUtilities.invokeLater(() -> {
            try {
                Class util = Class.forName("com.apple.eawt.Application");
                Method getApplication = util.getMethod("getApplication", new Class[0]);
                Object application = getApplication.invoke(util);
                Class params[] = new Class[1];
                params[0] = java.awt.Image.class;
                Method setDockIconImage = util.getMethod("setDockIconImage", params);
                URL url = this.getClass().getResource("/coda_256x256.png");
                java.awt.Image image = Toolkit.getDefaultToolkit().getImage(url);
                setDockIconImage.invoke(application, image);
            } catch (Exception e) {
            }
        });
        setContext(new SpringApplicationBuilder(CalibrationApplication.class).headless(false).run(initialArgs));
    }

    private ConfigurableApplicationContext setContext(ConfigurableApplicationContext context) {
        if (springContext != null) {
            return springContext;
        }

        springContext = context;
        return springContext;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        new GuiApplication(springContext).start(primaryStage);
    }

    @Override
    public void stop() throws Exception {
        CompletableFuture.runAsync(() -> {
            springContext.stop();
            springContext.close();
        }).get(1, TimeUnit.SECONDS);
        Platform.exit();
        System.exit(0);
    }
}
