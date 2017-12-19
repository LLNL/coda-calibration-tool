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
package llnl.gnem.core.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by dodge1 Date: Mar 16, 2010 
 */
public class ApplicationLogger {

    private static final ApplicationLogger ourInstance = new ApplicationLogger();
    private final Logger logger;
    private LogFormatter formatter;
    private FileHandler fileHandler;
    private ConsoleHandler consoleHandler;
    private boolean guiEnabled;
    private final Map<String, Level> levelMap;

    public static ApplicationLogger getInstance() {
        return ourInstance;
    }

    private ApplicationLogger() {
        logger = Logger.getLogger("ApplicationLog");
        logger.setLevel(Level.ALL);
        levelMap = new HashMap<String, Level>();
        levelMap.put("ALL", Level.ALL);
        levelMap.put("SEVERE", Level.SEVERE);
        levelMap.put("WARNING", Level.WARNING);
        levelMap.put("INFO", Level.INFO);
        levelMap.put("CONFIG", Level.CONFIG);
        levelMap.put("FINE", Level.FINE);
        levelMap.put("FINER", Level.FINER);
        levelMap.put("FINEST", Level.FINEST);
        levelMap.put("OFF", Level.OFF);
        guiEnabled = true;
    }

    public static String getAllLevelsString() {
        return "ALL, SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST, OFF";
    }

    public Collection<String> getAvailableLevels() {
        return levelMap.keySet();
    }

    public void setLevel(String level) {
        Level alevel = levelMap.get(level.toUpperCase());
        if (alevel != null) {
            setLevel(alevel);
        } else {
            throw new IllegalArgumentException("Invalid Log level String: " + level);
        }
    }

    public void setLevel(Level level) {
        logger.setLevel(level);
        if (fileHandler != null) {
            fileHandler.setLevel(level);
        }
        if (consoleHandler != null) {
            consoleHandler.setLevel(level);
        }
    }

    public void setFileHandler(String appName, boolean usePID) throws IOException {
        String tmpDir = System.getProperty("user.home");
        String pid = ManagementFactory.getRuntimeMXBean().getName();
        String tmp = usePID ? String.format("%s.%s", appName, pid): String.format("%s", appName);
        String pattern = tmpDir + File.separator + tmp + ".log%g";
        int limit = 10000000; // 10 mbyte
        int numLogFiles = 3;
        fileHandler = new FileHandler(pattern, limit, numLogFiles);
        logger.addHandler(fileHandler);
        formatter = new LogFormatter();
        fileHandler.setFormatter(formatter);
        logger.setUseParentHandlers(false);

    }

    public void setGuiWarnings(boolean guiOn) {
        guiEnabled = guiOn;
    }

    public void useConsoleHandler() {
        consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.INFO);
        logger.addHandler(consoleHandler);
    }

    public void addHandler(Handler handler) {
        handler.setFormatter(formatter);
        logger.addHandler(handler);
    }

    public void log(Level level, String message) {
        logger.log(level, message);
    }

    public void log(Level level, String message, Exception e) {
        logger.log(level, message, e);
    }

    public void reportException(Exception e) {
        reportException("Unknown Source", e);
    }

    public void reportException(String source, Exception e) {
            log(Level.WARNING, source, e);       
    }

    public boolean hasGui() {
        return guiEnabled;
    }

    /**
     * Read various flags and system properties to set logging of interesting
     * and/or bad happenings in a code.
     * 
     * @param appName
     * @throws IOException
     */
    public static void configureLogging(String appName) throws IOException {
        ApplicationLogger.getInstance().setFileHandler(appName, false);
        String aLevel = System.getProperty("LOGLEVEL");
        Level level = aLevel == null ? Level.INFO : Level.parse(aLevel);
        ApplicationLogger.getInstance().setLevel(level);

        String useConsoleLogger = System.getProperty("USE_CONSOLE_LOGGER");
        if (useConsoleLogger != null && useConsoleLogger.equalsIgnoreCase("TRUE")) {
            ApplicationLogger.getInstance().useConsoleHandler();
        }
    }
}
