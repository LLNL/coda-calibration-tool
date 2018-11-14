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
package gov.llnl.gnem.apps.coda.common.mapping;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;

@org.junit.jupiter.api.Tag("gui")
public class LeafletMapTest {
    private LeafletMap map;
    private static JFXPanel panel = null;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        final CountDownLatch startupLatch = new CountDownLatch(1);

        //Init the JavaFX platform
        panel = new JFXPanel();
        panel.doLayout();

        Platform.runLater(() -> startupLatch.countDown());

        startupLatch.await(5l, TimeUnit.SECONDS);
    }

    @BeforeEach
    public void setUp() throws Exception {
        map = new LeafletMap();
    }

    private static Stream<Arguments> iconProvider() {
        return getIcons().stream().map(icon -> Arguments.of(icon));
    }

    private static List<Icon> getIcons() {
        List<Icon> list = new ArrayList<>();
        list.add(new LeafletIcon(null, null, null));
        list.add(new LeafletIcon(null, null, null));
        return list;
    }

    private static Stream<Arguments> iconProviderAsList() {
        return Stream.of(Arguments.of(getIcons()));
    }

    @ParameterizedTest
    @MethodSource("iconProvider")
    public final void testAddRemoveIcon(Icon icon) throws Exception {
        Assertions.assertTrue(map.getIconCount() == 0, "Map icon store should start empty");
        map.addIcon(icon);
        Assertions.assertTrue(map.getIconCount() == 1, "Map icon store should now have one entry");
        map.removeIcon(icon);
        Assertions.assertTrue(map.getIconCount() == 0, "Map icon store should be empty again");
    }

    @ParameterizedTest
    @MethodSource("iconProviderAsList")
    public final void testAddRemoveIcons(List<Icon> icons) throws Exception {
        Assertions.assertTrue(map.getIconCount() == 0, "Map icon store should start empty");
        map.addIcons(icons);
        Assertions.assertTrue(map.getIconCount() == icons.size(), "Map icon store should now have " + icons.size() + " entries but had " + map.getIconCount());
        map.removeIcons(icons);
        Assertions.assertTrue(map.getIconCount() == 0, "Map icon store should be empty again");
    }

    @ParameterizedTest
    @MethodSource("iconProviderAsList")
    public final void testClearIcons(List<Icon> icons) throws Exception {
        map.addIcons(icons);
        Assertions.assertTrue(map.getIconCount() > 0, "Map icon store should have entries");
        map.clearIcons();
        Assertions.assertTrue(map.getIconCount() == 0, "Map icon store should have no entries");
    }

}
