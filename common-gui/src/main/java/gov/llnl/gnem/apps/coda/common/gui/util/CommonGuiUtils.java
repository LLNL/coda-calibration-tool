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
package gov.llnl.gnem.apps.coda.common.gui.util;

import java.awt.Toolkit;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;

import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonGuiUtils {

    private static final Logger log = LoggerFactory.getLogger(CommonGuiUtils.class);

    public static void setIcon(Class<?> clazz, String iconPath) {
        SwingUtilities.invokeLater(() -> {
            URL url = clazz.getResource(iconPath);
            java.awt.Image image = Toolkit.getDefaultToolkit().getImage(url);
            try {
                //Java 9+
                Class<?> taskbar = Class.forName("java.awt.Taskbar");
                Method getTaskbar = taskbar.getMethod("getTaskbar");
                try {
                    Object taskbarInstance = getTaskbar.invoke(taskbar);
                    if (taskbarInstance != null) {
                        Method setIconImage = taskbar.cast(taskbarInstance).getClass().getMethod("setIconImage", java.awt.Image.class);
                        setIconImage.invoke(taskbarInstance, image);
                    }
                } catch (UnsupportedOperationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                try {
                    //Java < 9 macos support
                    Class<?> util = Class.forName("com.apple.eawt.Application");
                    try {
                        Method getApplication = util.getMethod("getApplication", new Class[0]);
                        Object application = getApplication.invoke(util);
                        Method setDockIconImage = util.getMethod("setDockIconImage", java.awt.Image.class);
                        setDockIconImage.invoke(application, image);
                    } catch (NoSuchMethodException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
                        log.trace(e1.getMessage(), e1);
                    }
                } catch (ClassNotFoundException x) {
                    log.trace(x.getMessage(), x);
                }
            }
        });
    }
}
