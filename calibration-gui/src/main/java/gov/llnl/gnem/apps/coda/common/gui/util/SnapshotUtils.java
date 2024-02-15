/*
* Copyright (c) 2020, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.model.domain.Pair;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Node;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.transform.Transform;

public class SnapshotUtils {
    private static final Logger log = LoggerFactory.getLogger(SnapshotUtils.class);

    public static Image snapshot(final Node node) {
        SnapshotParameters snapshotParams = new SnapshotParameters();
        snapshotParams.setTransform(Transform.scale(2.0, 2.0));
        return node.snapshot(snapshotParams, null);
    }

    public static void writePng(BufferedImage image, String filename) {
        try {
            ImageIO.write(image, "png", new File(filename));
            log.trace("Wrote image to: {}", filename);
        } catch (IOException ex) {
            log.warn(ex.getMessage(), ex);
        } catch (NullPointerException ex) {
            log.warn("Null pointer writing image {} to file {} : {}", image, filename, ex.getMessage(), ex);
        }
    }

    public static CompletableFuture<Void> writePng(final Node node, String filename) {
        Image snapshot = snapshot(node);
        return CompletableFuture.runAsync(() -> writePng(SwingFXUtils.fromFXImage(snapshot, null), filename));
    }

    public static void writePng(File folder, Pair<String, Node> nameAndNode) {
        writePng(folder, Collections.singletonList(nameAndNode), null);
    }

    public static void writePng(File folder, Pair<String, Node> nameAndNode, String id) {
        writePng(folder, Collections.singletonList(nameAndNode), id);
    }

    public static void writePng(File folder, List<Pair<String, Node>> namesAndNodes, String id) {
        String timestamp;
        if (id != null) {
            timestamp = id;
        } else {
            timestamp = getTimestampWithLeadingSeparator();
        }
        if (folder != null && namesAndNodes != null && !namesAndNodes.isEmpty()) {
            List<CompletableFuture<Void>> futures = namesAndNodes.stream()
                                                                 .map(nameAndNode -> writePng(nameAndNode.getY(), folder.getAbsolutePath() + File.separator + nameAndNode.getX() + timestamp + ".png"))
                                                                 .collect(Collectors.toList());
            try {
                //Block for a minute to make sure the PNGs get written to disk in total before we move on
                CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get(1l, TimeUnit.MINUTES);
            } catch (Exception ex) {
                log.debug(ex.getLocalizedMessage(), ex);
            }
        }
    }

    public static String getTimestampWithLeadingSeparator() {
        return "_" + Instant.now().toEpochMilli();
    }

}
