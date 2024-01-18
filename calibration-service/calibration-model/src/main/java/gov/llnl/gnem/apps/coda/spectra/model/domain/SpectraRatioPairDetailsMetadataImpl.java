package gov.llnl.gnem.apps.coda.spectra.model.domain;

import gov.llnl.gnem.apps.coda.calibration.model.domain.WaveformMetadataImpl;
import gov.llnl.gnem.apps.coda.common.model.domain.WaveformMetadata;

public class SpectraRatioPairDetailsMetadataImpl implements SpectraRatioPairDetailsMetadata {

    private Long id;
    private Integer version;
    private Double diffAvg;
    private Double numerAvg;
    private Double denomAvg;
    private WaveformMetadata numerWaveform;
    private WaveformMetadata denomWaveform;
    private int cutSegmentLength;
    private double cutTimeLength;
    private Double numerWaveStartSec;
    private Double denomWaveStartSec;
    private Double numerWaveEndSec;
    private Double denomWaveEndSec;
    private Double numerPeakSec;
    private Double denomPeakSec;
    private Double numerFMarkerSec;
    private Double denomFMarkerSec;
    private Double numerStartCutSec;
    private Double numerEndCutSec;
    private Double denomStartCutSec;
    private Double denomEndCutSec;
    private int numerStartCutIdx;
    private int denomStartCutIdx;
    private int numerEndCutIdx;
    private int denomEndCutIdx;
    private boolean userEdited;

    public SpectraRatioPairDetailsMetadataImpl() {
    }

    public SpectraRatioPairDetailsMetadataImpl(Long id, Integer version, Double diffAvg, Double numerAvg, Double denomAvg, WaveformMetadata numerWaveform, WaveformMetadata denomWaveform,
            int cutSegmentLength, double cutTimeLength, Double numerWaveStartSec, Double denomWaveStartSec, Double numerWaveEndSec, Double denomWaveEndSec, Double numerPeakSec, Double denomPeakSec,
            Double numerFMarkerSec, Double denomFMarkerSec, Double numerStartCutSec, Double numerEndCutSec, Double denomStartCutSec, Double denomEndCutSec, int numerStartCutIdx, int denomStartCutIdx,
            int numerEndCutIdx, int denomEndCutIdx, boolean userEdited) {
        this.id = id;
        this.version = version;
        this.diffAvg = diffAvg;
        this.numerAvg = numerAvg;
        this.denomAvg = denomAvg;
        this.numerWaveform = numerWaveform;
        this.denomWaveform = denomWaveform;
        this.cutSegmentLength = cutSegmentLength;
        this.cutTimeLength = cutTimeLength;
        this.numerWaveStartSec = numerWaveStartSec;
        this.denomWaveStartSec = denomWaveStartSec;
        this.numerWaveEndSec = numerWaveEndSec;
        this.denomWaveEndSec = denomWaveEndSec;
        this.numerPeakSec = numerPeakSec;
        this.denomPeakSec = denomPeakSec;
        this.numerFMarkerSec = numerFMarkerSec;
        this.denomFMarkerSec = denomFMarkerSec;
        this.numerStartCutSec = numerStartCutSec;
        this.numerEndCutSec = numerEndCutSec;
        this.denomStartCutSec = denomStartCutSec;
        this.denomEndCutSec = denomEndCutSec;
        this.numerStartCutIdx = numerStartCutIdx;
        this.denomStartCutIdx = denomStartCutIdx;
        this.numerEndCutIdx = numerEndCutIdx;
        this.denomEndCutIdx = denomEndCutIdx;
        this.userEdited = userEdited;
    }

    public SpectraRatioPairDetailsMetadataImpl(SpectraRatioPairDetails ratio) {
        this.id = ratio.getId();
        this.version = ratio.getVersion();
        this.diffAvg = ratio.getDiffAvg();
        this.numerAvg = ratio.getNumerAvg();
        this.denomAvg = ratio.getDenomAvg();
        this.numerWaveform = new WaveformMetadataImpl(ratio.getNumerWaveform());
        this.denomWaveform = new WaveformMetadataImpl(ratio.getDenomWaveform());
        this.cutSegmentLength = ratio.getCutSegmentLength();
        this.cutTimeLength = ratio.getCutTimeLength();
        this.numerWaveStartSec = ratio.getNumerWaveStartSec();
        this.denomWaveStartSec = ratio.getDenomWaveStartSec();
        this.numerWaveEndSec = ratio.getNumerWaveEndSec();
        this.denomWaveEndSec = ratio.getDenomWaveEndSec();
        this.numerPeakSec = ratio.getNumerPeakSec();
        this.denomPeakSec = ratio.getDenomPeakSec();
        this.numerFMarkerSec = ratio.getNumerFMarkerSec();
        this.denomFMarkerSec = ratio.getDenomFMarkerSec();
        this.numerStartCutSec = ratio.getNumerStartCutSec();
        this.denomStartCutSec = ratio.getDenomStartCutSec();
        this.numerEndCutSec = ratio.getNumerEndCutSec();
        this.denomEndCutSec = ratio.getDenomEndCutSec();
        this.numerStartCutIdx = ratio.getNumerStartCutIdx();
        this.denomStartCutIdx = ratio.getDenomStartCutIdx();
        this.numerEndCutIdx = ratio.getNumerEndCutIdx();
        this.denomEndCutIdx = ratio.getDenomEndCutIdx();
        this.userEdited = ratio.isUserEdited();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public SpectraRatioPairDetailsMetadata setId(Long id) {
        this.id = id;
        return this;
    }

    @Override
    public Integer getVersion() {
        return version;
    }

    @Override
    public SpectraRatioPairDetailsMetadata setVersion(Integer version) {
        this.version = version;
        return this;
    }

    @Override
    public Double getDiffAvg() {
        return diffAvg;
    }

    @Override
    public Double getNumerAvg() {
        return numerAvg;
    }

    @Override
    public Double getDenomAvg() {
        return denomAvg;
    }

    @Override
    public WaveformMetadata getNumerWaveform() {
        return numerWaveform;
    }

    public void setNumerWaveform(WaveformMetadata numerWaveform) {
        this.numerWaveform = numerWaveform;
    }

    @Override
    public WaveformMetadata getDenomWaveform() {
        return denomWaveform;
    }

    public void setDenomWaveform(WaveformMetadata denomWaveform) {
        this.denomWaveform = denomWaveform;
    }

    @Override
    public int getCutSegmentLength() {
        return cutSegmentLength;
    }

    public void setCutSegmentLength(int cutSegmentLength) {
        this.cutSegmentLength = cutSegmentLength;
    }

    @Override
    public double getCutTimeLength() {
        return cutTimeLength;
    }

    public void setCutTimeLength(double cutTimeLength) {
        this.cutTimeLength = cutTimeLength;
    }

    @Override
    public Double getNumerWaveStartSec() {
        return numerWaveStartSec;
    }

    public void setNumerWaveStartSec(Double numerWaveStartSec) {
        this.numerWaveStartSec = numerWaveStartSec;
    }

    @Override
    public Double getDenomWaveStartSec() {
        return denomWaveStartSec;
    }

    public void setDenomWaveStartSec(Double denomWaveStartSec) {
        this.denomWaveStartSec = denomWaveStartSec;
    }

    @Override
    public Double getNumerWaveEndSec() {
        return numerWaveEndSec;
    }

    public void setNumerWaveEndSec(Double numerWaveEndSec) {
        this.numerWaveEndSec = numerWaveEndSec;
    }

    @Override
    public Double getDenomWaveEndSec() {
        return denomWaveEndSec;
    }

    public void setDenomWaveEndSec(Double denomWaveEndSec) {
        this.denomWaveEndSec = denomWaveEndSec;
    }

    @Override
    public Double getNumerPeakSec() {
        return numerPeakSec;
    }

    public void setNumerPeakSec(Double numerPeakSec) {
        this.numerPeakSec = numerPeakSec;
    }

    @Override
    public Double getDenomPeakSec() {
        return denomPeakSec;
    }

    public void setDenomPeakSec(Double denomPeakSec) {
        this.denomPeakSec = denomPeakSec;
    }

    @Override
    public Double getNumerFMarkerSec() {
        return numerFMarkerSec;
    }

    public void setNumerFMarkerSec(Double numerFMarkerSec) {
        this.numerFMarkerSec = numerFMarkerSec;
    }

    @Override
    public Double getDenomFMarkerSec() {
        return denomFMarkerSec;
    }

    public void setDenomFMarkerSec(Double denomFMarkerSec) {
        this.denomFMarkerSec = denomFMarkerSec;
    }

    @Override
    public Double getNumerStartCutSec() {
        return numerStartCutSec;
    }

    public void setNumerStartCutSec(Double numerStartCutSec) {
        this.numerStartCutSec = numerStartCutSec;
    }

    @Override
    public Double getNumerEndCutSec() {
        return numerEndCutSec;
    }

    public void setNumerEndCutSec(Double numerEndCutSec) {
        this.numerEndCutSec = numerEndCutSec;
    }

    @Override
    public Double getDenomStartCutSec() {
        return denomStartCutSec;
    }

    public void setDenomStartCutSec(Double denomStartCutSec) {
        this.denomStartCutSec = denomStartCutSec;
    }

    @Override
    public Double getDenomEndCutSec() {
        return denomEndCutSec;
    }

    public void setDenomEndCutSec(Double denomEndCutSec) {
        this.denomEndCutSec = denomEndCutSec;
    }

    @Override
    public int getNumerStartCutIdx() {
        return numerStartCutIdx;
    }

    public void setNumerStartCutIdx(int numerStartCutIdx) {
        this.numerStartCutIdx = numerStartCutIdx;
    }

    @Override
    public int getDenomStartCutIdx() {
        return denomStartCutIdx;
    }

    public void setDenomStartCutIdx(int denomStartCutIdx) {
        this.denomStartCutIdx = denomStartCutIdx;
    }

    @Override
    public int getNumerEndCutIdx() {
        return numerEndCutIdx;
    }

    public void setNumerEndCutIdx(int numerEndCutIdx) {
        this.numerEndCutIdx = numerEndCutIdx;
    }

    @Override
    public int getDenomEndCutIdx() {
        return denomEndCutIdx;
    }

    public void setDenomEndCutIdx(int denomEndCutIdx) {
        this.denomEndCutIdx = denomEndCutIdx;
    }

    @Override
    public boolean getUserEdited() {
        return userEdited;
    }

    public void setUserEdited(boolean userEdited) {
        this.userEdited = userEdited;
    }

}
