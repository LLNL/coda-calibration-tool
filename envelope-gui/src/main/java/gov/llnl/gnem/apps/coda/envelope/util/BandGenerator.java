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
package gov.llnl.gnem.apps.coda.envelope.util;

import java.util.List;
import java.util.function.Function;

import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeBandParameters;

public interface BandGenerator {

    public Double clampMinFreq(Double minFreq, Double maxFreq);

    public Double clampSpacing(Double spacing);

    public Double clampOverlap(Double overlap);

    public List<EnvelopeBandParameters> generateTable(Double minFreq, Double maxFreq, Double overlap, Double spacing);

    public BandGenerator setMinFunc(Function<Double, Double> minFunx);

    public Function<Double, Double> getMinFunc();

    public BandGenerator setMaxFunc(Function<Double, Double> maxFunc);

    public Function<Double, Double> getMaxFunc();

    public BandGenerator setMinPrecision(double minPrecision);

    public double getMinPrecision();

}