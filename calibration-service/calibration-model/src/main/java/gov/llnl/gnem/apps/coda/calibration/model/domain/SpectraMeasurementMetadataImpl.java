/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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

import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;

public class SpectraMeasurementMetadataImpl implements SpectraMeasurementMetadata {
    private Long id;
    private Integer version = 0;
    private WaveformMetadata waveform;
    private double rawAtStart;
    private double rawAtMeasurementTime;
    private double pathCorrected;
    private double pathAndSiteCorrected;
    private double startCutSec;
    private double endCutSec;
    private double rmsFit;

    public SpectraMeasurementMetadataImpl() {
        super();
    }

    public SpectraMeasurementMetadataImpl(SpectraMeasurement meas) {
        this.id = meas.getId();
        this.version = meas.getVersion();
        this.waveform = new WaveformMetadataImpl(meas.getWaveform());
        this.rawAtStart = meas.getRawAtStart();
        this.rawAtMeasurementTime = meas.getRawAtMeasurementTime();
        this.pathCorrected = meas.getPathCorrected();
        this.pathAndSiteCorrected = meas.getPathAndSiteCorrected();
        this.startCutSec = meas.getStartCutSec();
        this.endCutSec = meas.getEndCutSec();
        this.rmsFit = meas.getRmsFit();
    }

    @Override
    public WaveformMetadata getWaveform() {
        return waveform;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public double getRawAtStart() {
        return rawAtStart;
    }

    @Override
    public double getRawAtMeasurementTime() {
        return rawAtMeasurementTime;
    }

    @Override
    public double getPathCorrected() {
        return pathCorrected;
    }

    @Override
    public double getPathAndSiteCorrected() {
        return pathAndSiteCorrected;
    }

    @Override
    public double getStartCutSec() {
        return startCutSec;
    }

    @Override
    public double getEndCutSec() {
        return endCutSec;
    }

    @Override
    public double getRmsFit() {
        return rmsFit;
    }

    public SpectraMeasurementMetadataImpl setId(Long id) {
        this.id = id;
        return this;
    }

    public SpectraMeasurementMetadataImpl setVersion(Integer version) {
        this.version = version;
        return this;
    }

    public SpectraMeasurementMetadataImpl setWaveform(WaveformMetadata waveform) {
        this.waveform = waveform;
        return this;
    }

    public SpectraMeasurementMetadataImpl setRawAtStart(double rawAtStart) {
        this.rawAtStart = rawAtStart;
        return this;
    }

    public SpectraMeasurementMetadataImpl setRawAtMeasurementTime(double rawAtMeasurementTime) {
        this.rawAtMeasurementTime = rawAtMeasurementTime;
        return this;
    }

    public SpectraMeasurementMetadataImpl setPathCorrected(double pathCorrected) {
        this.pathCorrected = pathCorrected;
        return this;
    }

    public SpectraMeasurementMetadataImpl setPathAndSiteCorrected(double pathAndSiteCorrected) {
        this.pathAndSiteCorrected = pathAndSiteCorrected;
        return this;
    }

    public SpectraMeasurementMetadataImpl setStartCutSec(double startCutSec) {
        this.startCutSec = startCutSec;
        return this;
    }

    public SpectraMeasurementMetadataImpl setEndCutSec(double endCutSec) {
        this.endCutSec = endCutSec;
        return this;
    }

    public SpectraMeasurementMetadataImpl setRmsFit(double rmsFit) {
        this.rmsFit = rmsFit;
        return this;
    }

}
