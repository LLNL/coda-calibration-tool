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
package gov.llnl.gnem.apps.coda.common.util;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.provider.MethodSource;

public class NumberFormatFactoryTest {
    public String notNumeric;

    @ParameterizedTest
    @MethodSource("testParamSet")
    public final void testTwoDecimalOneLeadingZeroFormattingExpectations(Number input, ArgumentsAccessor args) throws Exception {
        String expectedTwoDigit = args.getString(1);
        NumberFormat formatter = NumberFormatFactory.twoDecimalOneLeadingZero();
        String actual = formatter.format(input);
        Assert.assertEquals(expectedTwoDigit, actual);
    }

    @ParameterizedTest
    @MethodSource("testParamSet")
    public final void testFourDecimalOneLeadingZeroFormattingExpectations(Number input, ArgumentsAccessor args) throws Exception {
        String expectedFourDigit = args.getString(2);
        NumberFormat formatter = NumberFormatFactory.fourDecimalOneLeadingZero();
        String actual = formatter.format(input);
        Assert.assertEquals(expectedFourDigit, actual);
    }
    
    @SuppressWarnings("unused")
    private static Collection<Object[]> testParamSet() throws IOException {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] { 1, "1.0", "1.0" });
        params.add(new Object[] { Math.PI, "3.14", "3.1416" });
        params.add(new Object[] { 10000.12345, "10000.12", "10000.1234" });
        params.add(new Object[] { -1.12347, "-1.12", "-1.1235" });
        return params;
    }
}
