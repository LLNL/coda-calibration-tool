/*
* Copyright (c) 2021, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.model.domain;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class InjectedCalibrationShapeFitterConstraints {

    @Value("${shape-constraints.maxVP1:600}")
    private double maxVP1;

    @Value("${shape-constraints.minVP1:50}")
    private double minVP1;

    @Value("${shape-constraints.v0reg:100}")
    private double v0reg;

    @Value("${shape-constraints.maxVP2:5000}")
    private double maxVP2;

    @Value("${shape-constraints.minVP2:1}")
    private double minVP2;

    @Value("${shape-constraints.maxVP3:5000}")
    private double maxVP3;

    @Value("${shape-constraints.minVP3:1}")
    private double minVP3;

    @Value("${shape-constraints.maxBP1:1000}")
    private double maxBP1;

    @Value("${shape-constraints.minBP1:-500}")
    private double minBP1;

    @Value("${shape-constraints.b0reg:10000}")
    private double b0reg;

    @Value("${shape-constraints.maxBP2:20}")
    private double maxBP2;

    @Value("${shape-constraints.minBP2:0.1}")
    private double minBP2;

    @Value("${shape-constraints.maxBP3:1500}")
    private double maxBP3;

    @Value("${shape-constraints.minBP3:0.0001}")
    private double minBP3;

    @Value("${shape-constraints.maxGP1:100}")
    private double maxGP1;

    @Value("${shape-constraints.minGP1:0}")
    private double minGP1;

    @Value("${shape-constraints.g0reg:100}")
    private double g0reg;

    @Value("${shape-constraints.maxGP2:101}")
    private double maxGP2;

    @Value("${shape-constraints.minGP2:0}")
    private double minGP2;

    @Value("${shape-constraints.g1reg:-1}")
    private double g1reg;

    @Value("${shape-constraints.maxGP3:101}")
    private double maxGP3;

    @Value("${shape-constraints.minGP3:1}")
    private double minGP3;

    @Value("${shape-constraints.yvvMin:0.5}")
    private double yvvMin;

    @Value("${shape-constraints.yvvMax:6.01}")
    private double yvvMax;

    @Value("${shape-constraints.vDistMax:1600}")
    private double vDistMax;

    @Value("${shape-constraints.vDistMin:0}")
    private double vDistMin;

    @Value("${shape-constraints.ybbMin:-12.0E-2}")
    private double ybbMin;

    @Value("${shape-constraints.ybbMax:0.0005}")
    private double ybbMax;

    @Value("${shape-constraints.bDistMax:1550}")
    private double bDistMax;

    @Value("${shape-constraints.bDistMin:0}")
    private double bDistMin;

    @Value("${shape-constraints.yggMin:0.01}")
    private double yggMin;

    @Value("${shape-constraints.yggMax:100}")
    private double yggMax;

    @Value("${shape-constraints.gDistMin:600}")
    private double gDistMin;

    @Value("${shape-constraints.gDistMax:0}")
    private double gDistMax;

    @Value("${shape-constraints.minIntercept:0.001}")
    private double minIntercept;

    @Value("${shape-constraints.maxIntercept:20.0}")
    private double maxIntercept;

    @Value("${shape-constraints.minBeta:-4.0}")
    private double minBeta;

    @Value("${shape-constraints.maxBeta:-0.0001}")
    private double maxBeta;

    @Value("${shape-constraints.minGamma:0.001}")
    private double minGamma;

    @Value("${shape-constraints.maxGamma:4.0}")
    private double maxGamma;

    @Value("${shape-constraints.iterations:10}")
    private int iterations;

    @Value("${shape-constraints.fittingPointCount:10000}")
    private int fittingPointCount;

    @Value("${shape-constraints.lengthWeight:0.5}")
    private double lengthWeight;

    @Bean
    public ShapeFitterConstraints toCalibrationShapeFitterConstraints() {
        return new ShapeFitterConstraints().setMaxVP1(maxVP1)
                                           .setMinVP1(minVP1)
                                           .setV0reg(v0reg)
                                           .setMaxVP2(maxVP2)
                                           .setMinVP2(minVP2)
                                           .setMaxVP3(maxVP3)
                                           .setMinVP3(minVP3)
                                           .setMaxBP1(maxBP1)
                                           .setMinBP1(minBP1)
                                           .setB0reg(b0reg)
                                           .setMaxBP2(maxBP2)
                                           .setMinBP2(minBP2)
                                           .setMaxBP3(maxBP3)
                                           .setMinBP3(minBP3)
                                           .setMaxGP1(maxGP1)
                                           .setMinGP1(minGP1)
                                           .setG0reg(g0reg)
                                           .setMaxGP2(maxGP2)
                                           .setMinGP2(minGP2)
                                           .setG1reg(g1reg)
                                           .setMaxGP3(maxGP3)
                                           .setMinGP3(minGP3)
                                           .setYvvMin(yvvMin)
                                           .setYvvMax(yvvMax)
                                           .setvDistMax(vDistMax)
                                           .setvDistMin(vDistMin)
                                           .setYbbMin(ybbMin)
                                           .setYbbMax(ybbMax)
                                           .setbDistMax(bDistMax)
                                           .setbDistMin(bDistMin)
                                           .setYggMin(yggMin)
                                           .setYggMax(yggMax)
                                           .setgDistMin(gDistMin)
                                           .setgDistMax(gDistMax)
                                           .setMinIntercept(minIntercept)
                                           .setMaxIntercept(maxIntercept)
                                           .setMinBeta(minBeta)
                                           .setMaxBeta(maxBeta)
                                           .setMinGamma(minGamma)
                                           .setMaxGamma(maxGamma)
                                           .setIterations(iterations)
                                           .setFittingPointCount(fittingPointCount)
                                           .setLengthWeight(lengthWeight);
    }
}