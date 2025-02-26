/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439, CODE-848318.
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
package gov.llnl.gnem.apps.coda.spectra.model.domain.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import gov.llnl.gnem.apps.coda.common.model.domain.FrequencyBand;
import gov.llnl.gnem.apps.coda.common.model.domain.Station;
import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;
import gov.llnl.gnem.apps.coda.spectra.model.domain.SpectraRatioPairDetails;
import gov.llnl.gnem.apps.coda.spectra.model.domain.messaging.EventPair;

public class SpectraRatioPairOperator {

    private PropertyChangeListener ratioValueChangeListener = null;
    private final PropertyChangeSupport ratioValueProperty = new PropertyChangeSupport(this);
    private SpectraRatioPairDetails ratio;

    public SpectraRatioPairOperator(SpectraRatioPairDetails ratio) {
        this.ratio = ratio;
    }

    /***
     * Initializes the start and end cut times to be the peak and f-marker
     * times.
     *
     * @param numerPeakSec
     *            The time in seconds at which the peak of the numerator wave
     *            is.
     * @param denomPeakSec
     *            The time in seconds at which the peak of the denominator wave
     *            is.
     * @param numerFMarkerSec
     *            The time in seconds at which the f-marker is for the
     *            numerator.
     * @param denomFMarkerSec
     *            The time in seconds at which the f-marker is for the
     *            denominator.
     */
    public void setPeakAndFMarkerCutTimes(Double numerPeakSec, Double denomPeakSec, Double numerFMarkerSec, Double denomFMarkerSec) {
        this.setNumerPeakSec(numerPeakSec);
        this.setDenomPeakSec(denomPeakSec);
        this.setNumerFMarkerSec(numerFMarkerSec);
        this.setDenomFMarkerSec(denomFMarkerSec);
        resetToPeakAndFMarkerCut();
    }

    /***
     * Will reset the numerator and denominator start and end cut times to be
     * the peak and f-marker. If the numerator or denominator waveforms are
     * null, then this is a nop.
     */
    public void resetToPeakAndFMarkerCut() {
        if (getNumerWaveform() != null && getDenomWaveform() != null) {
            this.setNumerStartCutSec(getNumerPeakSec());
            this.setDenomStartCutSec(getDenomPeakSec());
            this.setNumerEndCutSec(getNumerFMarkerSec());
            this.setDenomEndCutSec(getDenomFMarkerSec());
        }
    }

    public Station getStation() {
        if (this.getNumerWaveform() != null) {
            return this.getNumerWaveform().getStream().getStation();
        }
        return null;
    }

    public EventPair getEventPair() {
        if (this.getNumerWaveform() != null && this.getDenomWaveform() != null) {
            EventPair eventPair = new EventPair();
            eventPair.setY(this.getNumerWaveform().getEvent());
            eventPair.setX(this.getDenomWaveform().getEvent());
            return eventPair;
        }
        return null;
    }

    public void updateCutTimesAndRecalculateDiff(int numeratorStartCutIdx, int denominatorStartCutIdx, int numeratorEndCutIdx, int denominatorEndCutIdx) {

        // Calculate max cut length and cut time
        // By this point these segments have been re-sampled to the same
        // sample rate, so index in the array is proportional to
        // time
        double numerStartSec = ratio.getNumerStartCutSec();
        double denomStartSec = ratio.getDenomStartCutSec();
        double numerEndSec = ratio.getNumerEndCutSec();
        double denomEndSec = ratio.getDenomEndCutSec();
        double startTimeSec = Math.max(numerStartSec, denomStartSec);
        if (startTimeSec > 0) {
            startTimeSec = Math.floor(startTimeSec);
        }
        else {
            startTimeSec = Math.ceil(startTimeSec);
        }
        double endTimeSec = Math.min(numerEndSec, denomEndSec);
        
        if (startTimeSec > endTimeSec) {
            startTimeSec = endTimeSec;
        }

        double sampleRate = ratio.getNumerWaveform().getSampleRate();
        double maxTimeLength = endTimeSec - startTimeSec;

        // Update end cut time and indexes
        this.setCutSegmentLength((int) (maxTimeLength * sampleRate));
        this.setCutTimeLength(maxTimeLength);

        // Update cut time and indexes
        this.setNumerStartCutSec(startTimeSec);
        this.setNumerEndCutSec(endTimeSec);
        this.setDenomStartCutSec(startTimeSec);
        this.setDenomEndCutSec(endTimeSec);

        //Explicitly reset the stop index to the same length to account for 
        //rounding issues between the two cuts
        this.setNumerEndCutIdx(this.getNumerStartCutIdx() + getCutSegmentLength());
        this.setDenomEndCutIdx(this.getDenomStartCutIdx() + getCutSegmentLength());
        
        updateDiffSegment();
    }

    public void updateDiffSegment() {
        final int cutLength = this.getCutSegmentLength();

        // Create cut segments for subtraction step
        double[] numerCutSegment = getSubArray(getNumerWaveform().getSegment(), getNumerStartCutIdx(), getNumerEndCutIdx());
        double[] denomCutSegment = getSubArray(getDenomWaveform().getSegment(), getDenomStartCutIdx(), getDenomEndCutIdx());
        double[] newDiffSegment = new double[cutLength];

        // Generate the difference segment
        for (int i = 0; i < cutLength; i++) {
            newDiffSegment[i] = numerCutSegment[i] - denomCutSegment[i];
        }

        this.setDiffSegment(newDiffSegment);

        if (numerCutSegment.length > 0 && denomCutSegment.length > 0) {
            Double numSum = 0.0;
            Double denSum = 0.0;
            Double diffSum = 0.0;
            for (int i = 0; i < cutLength; i++) {
                numSum += numerCutSegment[i];
                denSum += denomCutSegment[i];
                diffSum += newDiffSegment[i];
            }
            this.setNumerAvg(numSum / cutLength);
            this.setDenomAvg(denSum / cutLength);
            this.setDiffAvg(diffSum / newDiffSegment.length);
        }
    }

    public void setRatioValueChangeListener(PropertyChangeListener ratioValueChange) {
        // Remove existing listener before adding another one
        if (this.ratioValueChangeListener != null) {
            this.ratioValueProperty.removePropertyChangeListener("ratio_change", this.ratioValueChangeListener);
        }
        this.ratioValueChangeListener = ratioValueChange;
        this.ratioValueProperty.addPropertyChangeListener("ratio_change", this.ratioValueChangeListener);
    }

    protected void handleRatioChanged(Double change) {
        if (this.ratioValueChangeListener != null) {
            ratioValueProperty.firePropertyChange(new PropertyChangeEvent(this, "ratio_change", null, change));
        }
    }

    public FrequencyBand getFrequency() {
        if (this.getNumerWaveform() != null) {
            double lowFreq = this.getNumerWaveform().getLowFrequency();
            double highFreq = this.getNumerWaveform().getHighFrequency();
            return new FrequencyBand(lowFreq, highFreq);
        }
        return null;
    }

    public Date getNumeratorEventOriginTime() {
        if (this.getNumerWaveform() != null) {
            return this.getNumerWaveform().getEvent().getOriginTime();
        }
        return null;
    }

    public Date getDenominatorEventOriginTime() {
        if (this.getDenomWaveform() != null) {
            return this.getDenomWaveform().getEvent().getOriginTime();
        }
        return null;
    }

    /***
     * Returns a section of the original array starting from the start index to
     * the end index. If the indexes are invalid, it will simply return the
     * originalArray.
     *
     * @param originalArray
     * @param startIdx
     * @param endIdx
     * @return
     */
    public double[] getSubArray(double[] originalArray, int startIdx, int endIdx) {
        if (originalArray.length - 1 > endIdx && originalArray.length > startIdx) {
            return Arrays.copyOfRange(originalArray, startIdx, endIdx);
        }

        return originalArray;
    }

    public double[] getNumeratorCutSegment() {
        double[] numeratorUncutSegment = getNumerWaveform().getSegment();
        return getSubArray(numeratorUncutSegment, getNumerStartCutIdx(), getNumerEndCutIdx());
    }

    public double[] getDenominatorCutSegment() {
        double[] denominatorUncutSegment = getDenomWaveform().getSegment();
        return getSubArray(denominatorUncutSegment, getDenomStartCutIdx(), getDenomEndCutIdx());
    }

    @Override
    public int hashCode() {
        return ratio.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return ratio.equals(obj);
    }

    public boolean isLoadedFromJson() {
        return ratio.isLoadedFromJson();
    }

    public void setLoadedFromJson(boolean isloadedFromJson) {
        ratio.setLoadedFromJson(isloadedFromJson);
    }

    public Double getDiffAvg() {
        return ratio.getDiffAvg();
    }

    public void setDiffAvg(Double diffAvg) {
        ratio.setDiffAvg(diffAvg);
    }

    public Double getNumerAvg() {
        return ratio.getNumerAvg();
    }

    public void setNumerAvg(Double numerAvg) {
        ratio.setNumerAvg(numerAvg);
    }

    public Double getDenomAvg() {
        return ratio.getDenomAvg();
    }

    public void setDenomAvg(Double denomAvg) {
        ratio.setDenomAvg(denomAvg);
    }

    public Waveform getNumerWaveform() {
        return ratio.getNumerWaveform();
    }

    public void setNumerWaveform(Waveform numerWaveform) {
        ratio.setNumerWaveform(numerWaveform);
    }

    public Waveform getDenomWaveform() {
        return ratio.getDenomWaveform();
    }

    public void setDenomWaveform(Waveform denomWaveform) {
        ratio.setDenomWaveform(denomWaveform);
    }

    public int getCutSegmentLength() {
        return ratio.getCutSegmentLength();
    }

    public void setCutSegmentLength(int cutSegmentLength) {
        ratio.setCutSegmentLength(cutSegmentLength);
    }

    public double getCutTimeLength() {
        return ratio.getCutTimeLength();
    }

    public void setCutTimeLength(double cutTimeLength) {
        ratio.setCutTimeLength(cutTimeLength);
    }

    public double[] getDiffSegment() {
        return ratio.getDiffSegment();
    }

    public void setDiffSegment(double[] diffSegment) {
        ratio.setDiffSegment(diffSegment);
    }

    public Double getNumerWaveStartSec() {
        return ratio.getNumerWaveStartSec();
    }

    public void setNumerWaveStartSec(Double numerWaveStartSec) {
        ratio.setNumerWaveStartSec(numerWaveStartSec);
    }

    public Double getDenomWaveStartSec() {
        return ratio.getDenomWaveStartSec();
    }

    public void setDenomWaveStartSec(Double denomWaveStartSec) {
        ratio.setDenomWaveStartSec(denomWaveStartSec);
    }

    public Double getNumerWaveEndSec() {
        return ratio.getNumerWaveEndSec();
    }

    public void setNumerWaveEndSec(Double numerWaveEndSec) {
        ratio.setNumerWaveEndSec(numerWaveEndSec);
    }

    public Double getDenomWaveEndSec() {
        return ratio.getDenomWaveEndSec();
    }

    public void setDenomWaveEndSec(Double denomWaveEndSec) {
        ratio.setDenomWaveEndSec(denomWaveEndSec);
    }

    public Double getNumerPeakSec() {
        return ratio.getNumerPeakSec();
    }

    public void setNumerPeakSec(Double numerPeakSec) {
        ratio.setNumerPeakSec(numerPeakSec);
    }

    public Double getDenomPeakSec() {
        return ratio.getDenomPeakSec();
    }

    public void setDenomPeakSec(Double denomPeakSec) {
        ratio.setDenomPeakSec(denomPeakSec);
    }

    public Double getNumerFMarkerSec() {
        return ratio.getNumerFMarkerSec();
    }

    public void setNumerFMarkerSec(Double numerFMarkerSec) {
        ratio.setNumerFMarkerSec(numerFMarkerSec);
    }

    public Double getDenomFMarkerSec() {
        return ratio.getDenomFMarkerSec();
    }

    public void setDenomFMarkerSec(Double denomFMarkerSec) {
        ratio.setDenomFMarkerSec(denomFMarkerSec);
    }

    public Double getNumerStartCutSec() {
        return ratio.getNumerStartCutSec();
    }

    public void setNumerStartCutSec(Double numerStartCutSec) {
        ratio.setNumerStartCutSec(numerStartCutSec);
        ratio.setNumerStartCutIdx(getIndexForTimeFromOrigin(ratio.getNumerWaveform(), ratio.getNumerWaveStartSec(), numerStartCutSec));
    }

    public Double getDenomStartCutSec() {
        return ratio.getDenomStartCutSec();
    }

    public void setDenomStartCutSec(Double denomStartCutSec) {
        ratio.setDenomStartCutSec(denomStartCutSec);
        ratio.setDenomStartCutIdx(getIndexForTimeFromOrigin(ratio.getDenomWaveform(), ratio.getDenomWaveStartSec(), denomStartCutSec));
    }

    public Double getNumerEndCutSec() {
        return ratio.getNumerEndCutSec();
    }

    public void setNumerEndCutSec(Double numerEndCutSec) {
        ratio.setNumerEndCutSec(numerEndCutSec);
        ratio.setNumerEndCutIdx(getIndexForTimeFromOrigin(ratio.getNumerWaveform(), ratio.getNumerWaveStartSec(), numerEndCutSec));
    }

    public Double getDenomEndCutSec() {
        return ratio.getDenomEndCutSec();
    }

    public void setDenomEndCutSec(Double denomEndCutSec) {
        ratio.setDenomEndCutSec(denomEndCutSec);
        ratio.setDenomEndCutIdx(getIndexForTimeFromOrigin(ratio.getDenomWaveform(), ratio.getDenomWaveStartSec(), denomEndCutSec));
    }

    public int getNumerStartCutIdx() {
        return ratio.getNumerStartCutIdx();
    }

    public void setNumerStartCutIdx(int numerStartCutIdx) {
        ratio.setNumerStartCutIdx(numerStartCutIdx);
    }

    public int getDenomStartCutIdx() {
        return ratio.getDenomStartCutIdx();
    }

    public void setDenomStartCutIdx(int denomStartCutIdx) {
        ratio.setDenomStartCutIdx(denomStartCutIdx);
    }

    public int getNumerEndCutIdx() {
        return ratio.getNumerEndCutIdx();
    }

    public void setNumerEndCutIdx(int numerEndCutIdx) {
        ratio.setNumerEndCutIdx(numerEndCutIdx);
    }

    public int getDenomEndCutIdx() {
        return ratio.getDenomEndCutIdx();
    }

    public void setDenomEndCutIdx(int denomEndCutIdx) {
        ratio.setDenomEndCutIdx(denomEndCutIdx);
    }

    public boolean isUserEdited() {
        return ratio.isUserEdited();
    }

    public void setUserEdited(boolean userEdited) {
        ratio.setUserEdited(userEdited);
    }

    @Override
    public String toString() {
        return ratio.toString();
    }

    @JsonIgnore
    public SpectraRatioPairDetails getRatio() {
        return ratio;
    }
    
    @JsonIgnore
    public int getIndexForTimeFromOrigin(Waveform waveform, double originOffset, double timeSecondsFromOrigin) {
        int indexOffsetForOrigin = (int) (originOffset * waveform.getSampleRate());
        int indexFromOrigin = (int) (timeSecondsFromOrigin * waveform.getSampleRate());
        return indexFromOrigin - indexOffsetForOrigin;
    }
}
