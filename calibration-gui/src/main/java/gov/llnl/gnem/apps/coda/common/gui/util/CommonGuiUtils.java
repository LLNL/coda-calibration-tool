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

import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.SwingUtilities;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveOutputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.scene.Scene;

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
                        Method getApplication = util.getMethod("getApplication");
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

    public static Point getScaledMouseLocation(Scene scene, PointerInfo pi) {
        Point point = null;
        if (pi != null) {
            Point rawLoc = pi.getLocation();
            point = new Point(rawLoc);

            //Sadly these methods don't work until Java 9+ so have to leave this functionality out for now
            //if (scene != null && scene.getWindow() != null) {
            //    point.x = (int) (point.getX() / scene.getWindow().getOutputScaleX());
            //    point.y = (int) (point.getY() / scene.getWindow().getOutputScaleY());
            //}

            GraphicsDevice device = pi.getDevice();
            if (device != null && device.getDisplayMode() != null) {
                int width = device.getDisplayMode().getWidth();
                int height = device.getDisplayMode().getHeight();
                Rectangle bounds = new Rectangle(width, height);
                point.x -= bounds.x;
                point.y -= bounds.y;
            }

            //If this comes out negative it means its on another monitor (usually).
            if (point.x < 0) {
                point.x *= -1;
            }
            if (point.y < 0) {
                point.y *= -1;
            }
        }
        return point;
    }

    public static File zipDirectory(Path zipFolder) throws IOException {
        File zipDir;
        zipDir = File.createTempFile("zip-dir", "tmp");

        zipDir.deleteOnExit();

        try (Stream<Path> fileStream = Files.walk(zipFolder, 5)) {
            List<File> files = fileStream.map(Path::toFile).filter(File::isFile).collect(Collectors.toList());
            try (ArchiveOutputStream<ZipArchiveEntry> os = new ArchiveStreamFactory().createArchiveOutputStream("zip", Files.newOutputStream(zipDir.toPath()))) {
                for (File file : files) {
                    os.putArchiveEntry(new ZipArchiveEntry(file, file.getName()));
                    try (InputStream fis = Files.newInputStream(file.toPath())) {
                        IOUtils.copy(fis, os);
                    }
                    os.closeArchiveEntry();
                }
                os.flush();
            } catch (ArchiveException e) {
                throw new IOException(e);
            }
            try (Stream<Path> tmpFileStream = Files.walk(zipFolder)) {
                tmpFileStream.sorted(Comparator.reverseOrder()).forEach(t -> {
                    try {
                        Files.deleteIfExists(t);
                    } catch (IOException e) {
                        log.trace("Unable to delete temporary file {}", e.getMessage(), e);
                    }
                });
            }
        }
        return zipDir;
    }
}
