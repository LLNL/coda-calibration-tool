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

import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.llnl.gnem.apps.coda.common.mapping.api.Icon;
import gov.llnl.gnem.apps.coda.common.mapping.api.Icon.IconTypes;
import gov.llnl.gnem.apps.coda.common.mapping.api.Line;
import gov.llnl.gnem.apps.coda.common.mapping.api.Location;

public class LeafletShapeFactoryTest {
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private LeafletShapeFactory factory;

    @BeforeEach
    public void setUp() throws Exception {
        factory = new LeafletShapeFactory();
    }

    private static Stream<Arguments> iconTestParams() {
        return Stream.of(
                Arguments.of(IconTypes.DEFAULT, new Location(-90d, 90d), "Default", getDefaultIconAssertions()),
                    Arguments.of(IconTypes.DEFAULT, new Location(359d, 179d), "Default", getDefaultIconAssertions()),
                    Arguments.of(null, null, null, getDefaultIconAssertions()));
    }

    public static Consumer<Icon> getDefaultIconAssertions() {
        return (Consumer<Icon>) (icon) -> {
            Assertions.assertNotNull(icon, "Should get an icon object");
            Assertions.assertNotNull(icon.getId(), "Icon should have an ID");
            Assertions.assertNotNull(icon.getFriendlyName(), "Icon should have a name");
            Assertions.assertNotNull(icon.getType(), "Icon should be typed");
            Assertions.assertAll(
                    "Icon should have a valid latitude",
                        () -> Assertions.assertNotNull(icon.getLocation().getLatitude()),
                        () -> Assertions.assertTrue(icon.getLocation().getLatitude() >= -90d && icon.getLocation().getLatitude() <= 90d));

            Assertions.assertAll(
                    "Icon should have a valid longitude",
                        () -> Assertions.assertNotNull(icon.getLocation().getLongitude()),
                        () -> Assertions.assertTrue(icon.getLocation().getLongitude() >= -180d && icon.getLocation().getLongitude() <= 180d));
        };
    }

    private static Stream<Arguments> lineTestParams() {
        return Stream.of(Arguments.of(null, null, getDefaultLineAssertions()), Arguments.of(new Location(-90d, 90d), new Location(-0d, 0d), getDefaultLineAssertions()));
    }

    public static Consumer<Line> getDefaultLineAssertions() {
        return (Consumer<Line>) (line) -> {
            Assertions.assertNotNull(line, "Should get a Line object");
            Assertions.assertNotNull(line.getId(), "Line should have an ID");
            Assertions.assertAll(
                    "Line should have a valid start latitude",
                        () -> Assertions.assertNotNull(line.getStartLocation().getLatitude()),
                        () -> Assertions.assertTrue(line.getStartLocation().getLatitude() >= -90d && line.getStartLocation().getLatitude() <= 90d));
            Assertions.assertAll(
                    "Line should have a valid start longitude",
                        () -> Assertions.assertNotNull(line.getStartLocation().getLongitude()),
                        () -> Assertions.assertTrue(line.getStartLocation().getLongitude() >= -180d && line.getStartLocation().getLongitude() <= 180d));
            Assertions.assertAll(
                    "Line should have a valid end latitude",
                        () -> Assertions.assertNotNull(line.getEndLocation().getLatitude()),
                        () -> Assertions.assertTrue(line.getEndLocation().getLatitude() >= -90d && line.getEndLocation().getLatitude() <= 90d));
            Assertions.assertAll(
                    "Line should have a valid end longitude",
                        () -> Assertions.assertNotNull(line.getEndLocation().getLongitude()),
                        () -> Assertions.assertTrue(line.getEndLocation().getLongitude() >= -180d && line.getEndLocation().getLongitude() <= 180d));
        };
    }

    @ParameterizedTest
    @MethodSource("iconTestParams")
    public final void testIconFactory(IconTypes type, Location loc, String name, Consumer<Icon> assertions) throws Exception {
        Icon icon = factory.newIcon(type, loc, name);
        assertions.accept(icon);
    }

    @ParameterizedTest
    @MethodSource("lineTestParams")
    public final void testShapeFactory(Location start, Location end, Consumer<Line> assertions) throws Exception {
        Line line = factory.newLine(start, end);
        assertions.accept(line);
    }
}
