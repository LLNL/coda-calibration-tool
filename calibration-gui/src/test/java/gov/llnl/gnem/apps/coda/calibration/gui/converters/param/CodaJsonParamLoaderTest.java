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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.param;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import gov.llnl.gnem.apps.coda.calibration.model.domain.GeoJsonPolygon;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersFI;
import gov.llnl.gnem.apps.coda.calibration.model.domain.MdacParametersPS;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ReferenceMwParameters;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ShapeFitterConstraints;
import gov.llnl.gnem.apps.coda.calibration.model.domain.SiteCorrections;
import gov.llnl.gnem.apps.coda.calibration.model.domain.ValidationMwParameters;
import gov.llnl.gnem.apps.coda.common.model.domain.SharedFrequencyBandParameters;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;
import reactor.core.publisher.Flux;

public class CodaJsonParamLoaderTest {

    private CodaJsonParamLoader converter;

    @BeforeEach
    public void setUp() throws Exception {
        converter = new CodaJsonParamLoader();
    }

    private static Stream<Arguments> filePaths() {
        return Stream.of(Arguments.of("src/test/resources/calibration-params-test.json"));
    }

    protected Flux<Result<Object>> convertFile(String filePath) {
        return converter.convertFiles(Collections.singletonList(Paths.get(filePath).toFile()));
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testValidBands(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        Assertions.assertEquals(Long.valueOf(14l), results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof SharedFrequencyBandParameters).count().block());
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testValidSiteCorrections(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        SiteCorrections siteCorrections = (SiteCorrections) results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof SiteCorrections)
                                                                   .blockFirst()
                                                                   .getResultPayload()
                                                                   .orElseGet(null);
        Assertions.assertNotNull(siteCorrections, "Expected to have 1 site correction object");
        Assertions.assertEquals(1, siteCorrections.getSiteCorrections().stream().map(sc -> sc.getStation().getNetworkName()).distinct().count(), "Expected to have 1 networks");
        Assertions.assertEquals(3, siteCorrections.getSiteCorrections().stream().map(sc -> sc.getStation().getStationName()).distinct().count(), "Expected to have 3 stations");
        Assertions.assertEquals(
                14,
                    siteCorrections.getSiteCorrections()
                                   .stream()
                                   .filter(
                                           sfb -> sfb.getStation()
                                                     .getStationName()
                                                     .equalsIgnoreCase(siteCorrections.getSiteCorrections().stream().map(sc -> sc.getStation().getStationName()).findFirst().orElseGet(null)))
                                   .count(),
                    "Expected to have 14 bands for the first station");
        Assertions.assertEquals(42, siteCorrections.getSiteCorrections().size(), "Expected to have 42 site correction bands");
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testValidPs(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        Assertions.assertEquals(
                Long.valueOf(1l),
                    results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof MdacParametersFI).count().block(),
                    "Expected to have 1 valid MDAC FI definition");
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testFi(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        Assertions.assertEquals(
                Long.valueOf(4l),
                    results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof MdacParametersPS).count().block(),
                    "Expected to have 4 valid MDAC PS definitions");
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testRefMw(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        Assertions.assertEquals(
                Long.valueOf(2l),
                    results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof ReferenceMwParameters).count().block(),
                    "Expected to have 2 valid reference event definitions");
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testValidationMw(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        Assertions.assertEquals(
                Long.valueOf(2l),
                    results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof ValidationMwParameters).count().block(),
                    "Expected to have 14 valid band definitions");
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testMultiplePolygons(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        RawGeoJSON result = (RawGeoJSON) results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof RawGeoJSON).blockFirst().getResultPayload().get();

        Assertions.assertEquals(
                "{\"features\":[{\"type\":\"Feature\",\"properties\":{},\"geometry\":{\"type\":\"Polygon\",\"coordinates\":[[[0.0,3.0],[3.0,3.0],[3.0,0.0],[0.0,0.0],[0.0,3.0]]]}}]}",
                    result.getRawGeoJSON(),
                    "Expected RawGeoJSON collection to match contents of calibration JSON");
    }

    @ParameterizedTest
    @MethodSource("filePaths")
    public final void testShapeConstraint(String filePath) throws Exception {
        Flux<Result<Object>> results = convertFile(filePath);
        Assertions.assertEquals(
                Long.valueOf(1l),
                    results.filter(r -> r.isSuccess() && r.getResultPayload().orElse(null) instanceof ShapeFitterConstraints).count().block(),
                    "Expected to have 1 valid shape constraint definition");
        assertThat(((ShapeFitterConstraints) results.filter(r -> r.getResultPayload().orElse(null) instanceof ShapeFitterConstraints).blockFirst().getResultPayload().get()).getMaxBeta()).describedAs(
                "Expected values containing 'E-' scientific notation serialize correctly").isCloseTo(-11.0E-4, within(0.001));
    }
}
