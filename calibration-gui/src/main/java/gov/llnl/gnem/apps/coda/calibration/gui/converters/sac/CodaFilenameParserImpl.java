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
package gov.llnl.gnem.apps.coda.calibration.gui.converters.sac;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import gov.llnl.gnem.apps.coda.calibration.gui.converters.StackInfo;
import gov.llnl.gnem.apps.coda.calibration.gui.converters.api.CodaFilenameParser;
import gov.llnl.gnem.apps.coda.common.model.messaging.Result;

@Component
public class CodaFilenameParserImpl implements CodaFilenameParser {

    private final static Pattern codaFilePattern = Pattern.compile("[^_]*_[^_]*_[^_]*_([^_]*)_([^_]*)_([^_]*)_.*\\.ENV");

    @Override
    public Result<StackInfo> parse(String fileName) {
        Matcher fileNameMatcher = codaFilePattern.matcher(fileName);
        if (fileNameMatcher.matches()) {
            Double lowFreq = Double.valueOf(fileNameMatcher.group(1));
            Double highFreq = Double.valueOf(fileNameMatcher.group(2));
            String dataType = fileNameMatcher.group(3);
            return new Result<StackInfo>(true, new StackInfo(lowFreq, highFreq, dataType));
        }
        return new Result<StackInfo>(false, null);
    }
}
