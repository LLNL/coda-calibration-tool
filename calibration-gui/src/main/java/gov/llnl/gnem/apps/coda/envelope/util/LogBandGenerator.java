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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.util.Precision;

import com.google.common.base.Functions;

import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeBandParameters;

public class LogBandGenerator implements BandGenerator {

    private double minPrecision;
    private Function<Double, Double> maxFunc = Functions.identity();
    private Function<Double, Double> minFunc = Functions.identity();

    public LogBandGenerator(double minPrecision) {
        this.minPrecision = minPrecision;
    }

    public LogBandGenerator(double minPrecision, Function<Double, Double> maxFunc, Function<Double, Double> minFunc) {
        this.minPrecision = minPrecision;
        this.maxFunc = maxFunc;
        this.minFunc = minFunc;
    }

    @Override
    public Double clampMinFreq(Double minFreq, Double maxFreq) {
        double tmp = minFreq;
        if (tmp > maxFreq) {
            //error
            tmp = maxFreq;
        }
        return tmp;
    }

    @Override
    public Double clampSpacing(Double spacing) {
        double tmp = spacing;
        if (tmp <= minPrecision) {
            //error
            tmp = minPrecision;
        }
        return tmp;
    }

    @Override
    public Double clampOverlap(Double overlap) {
        double tmp = overlap;
        if (tmp <= 0.) {
            //error
            tmp = 0.;
        } else if (tmp <= minPrecision) {
            tmp = minPrecision;
        } else if (tmp >= 100.) {
            tmp = 0.99;
        } else if (tmp >= 1.0) {
            tmp = tmp / 100.0;
        }
        return tmp;
    }

    @Override
    public List<EnvelopeBandParameters> generateTable(Double rawMinFreq, Double rawMaxFreq, Double overlap, Double spacing) {
        double maxFreq = Math.log10(rawMaxFreq);
        double minFreq = Math.log10(rawMinFreq);

        double delta = rawMaxFreq - rawMinFreq;
        int reqBands = (int) ((delta / spacing) + 0.5);
        double max;
        double min;

        final double stepSize = (maxFreq - minFreq) / (reqBands - 1);

        List<EnvelopeBandParameters> bands = new ArrayList<>();
        for (int i = 0; i < reqBands; i++) {
            double nextFrequency = Math.pow(10, minFreq + (i * stepSize));
            min = nextFrequency;
            max = nextFrequency + spacing;
            bands.add(new EnvelopeBandParameters(Precision.round(min, 4, RoundingMode.HALF_DOWN.ordinal()), Precision.round(max, 4, RoundingMode.CEILING.ordinal())));
        }
        return bands;
    }

    @Override
    public double getMinPrecision() {
        return minPrecision;
    }

    @Override
    public BandGenerator setMinPrecision(double minPrecision) {
        this.minPrecision = minPrecision;
        return this;
    }

    @Override
    public Function<Double, Double> getMaxFunc() {
        return maxFunc;
    }

    @Override
    public BandGenerator setMaxFunc(Function<Double, Double> maxFunc) {
        this.maxFunc = maxFunc;
        return this;
    }

    @Override
    public Function<Double, Double> getMinFunc() {
        return minFunc;
    }

    @Override
    public BandGenerator setMinFunc(Function<Double, Double> minFunc) {
        this.minFunc = minFunc;
        return this;
    }
}
