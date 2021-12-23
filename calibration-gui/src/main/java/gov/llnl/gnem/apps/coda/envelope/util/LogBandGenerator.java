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

import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.util.Precision;

import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeBandParameters;

public class LogBandGenerator implements BandGenerator {
    private BandGenerator wrappedGenerator;

    public LogBandGenerator(Double minPrecision) {
        this(new LinearBandGenerator(minPrecision));
    }

    public LogBandGenerator(BandGenerator wrappedGenerator) {
        this.wrappedGenerator = wrappedGenerator;
    }

    @Override
    public Double clampMinFreq(Double minFreq, Double maxFreq) {
        return wrappedGenerator.clampMinFreq(minFreq, maxFreq);
    }

    @Override
    public Double clampSpacing(Double spacing) {
        return wrappedGenerator.clampSpacing(spacing);
    }

    @Override
    public Double clampOverlap(Double overlap) {
        return wrappedGenerator.clampOverlap(overlap);
    }

    @Override
    public List<EnvelopeBandParameters> generateTable(Double minFreq, Double maxFreq, Double overlap, Double spacing) {
        List<EnvelopeBandParameters> bands = wrappedGenerator.generateTable(Math.log10(minFreq), Math.log10(maxFreq), overlap, spacing);
        for (EnvelopeBandParameters band : bands) {
            band.setLowFrequency(Precision.round(Math.pow(10, band.getLowFrequency()), 4, RoundingMode.HALF_DOWN.ordinal()));
            band.setHighFrequency(Precision.round(Math.pow(10, band.getHighFrequency()), 4, RoundingMode.CEILING.ordinal()));
        }
        return bands;
    }

    @Override
    public BandGenerator setMinFunc(Function<Double, Double> minFunx) {
        return wrappedGenerator.setMinFunc(minFunx);
    }

    @Override
    public Function<Double, Double> getMinFunc() {
        return wrappedGenerator.getMinFunc();
    }

    @Override
    public BandGenerator setMaxFunc(Function<Double, Double> maxFunc) {
        return wrappedGenerator.setMaxFunc(maxFunc);
    }

    @Override
    public Function<Double, Double> getMaxFunc() {
        return wrappedGenerator.getMaxFunc();
    }

    @Override
    public BandGenerator setMinPrecision(double minPrecision) {
        return wrappedGenerator.setMinPrecision(minPrecision);
    }

    @Override
    public double getMinPrecision() {
        return wrappedGenerator.getMinPrecision();
    }

}
