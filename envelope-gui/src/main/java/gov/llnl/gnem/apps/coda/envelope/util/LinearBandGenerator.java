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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.util.Precision;

import com.google.common.base.Functions;

import gov.llnl.gnem.apps.coda.envelope.model.domain.EnvelopeBandParameters;

public class LinearBandGenerator implements BandGenerator {

    private double minPrecision;
    private Function<Double, Double> maxFunc = Functions.identity();
    private Function<Double, Double> minFunc = Functions.identity();

    public LinearBandGenerator(double minPrecision) {
        super();
        this.minPrecision = minPrecision;
    }

    public LinearBandGenerator(double minPrecision, Function<Double, Double> maxFunc, Function<Double, Double> minFunc) {
        super();
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
    public List<EnvelopeBandParameters> generateTable(Double minFreq, Double maxFreq, Double overlap, Double spacing) {
        double delta = maxFreq - minFreq;
        int reqBands = (int) ((delta / spacing) + 0.5);
        double max;
        double min;
        double step;
        List<EnvelopeBandParameters> bands = new ArrayList<>();
        for (int i = 0; i < reqBands; i++) {
            step = (((double) i / reqBands) * delta);
            min = minFreq + delta - (spacing * overlap / 2) + step;
            max = Math.min(min + (spacing * overlap) + spacing, maxFreq + delta);
            min = Math.max(min, minFreq + delta);
            bands.add(new EnvelopeBandParameters(Precision.round(min - delta, 4, BigDecimal.ROUND_HALF_DOWN), Precision.round(max - delta, 4, BigDecimal.ROUND_CEILING)));
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
