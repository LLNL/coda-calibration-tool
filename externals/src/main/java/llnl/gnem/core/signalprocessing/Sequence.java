//                                                      Sequence.java
//
//  Copyright (c)  2005  Regents of the University of California
//  All rights reserved
//
//* @author   Dave Harris
//*  Created:       11/13/98
//*  Last Modified:  8/11/2006
package llnl.gnem.core.signalprocessing;

import java.io.PrintStream;
import java.util.Arrays;

public class Sequence {

    // instance variables
    private float[] seqvalues;

    // static methods
    public Sequence(int n) {
        seqvalues = new float[n];
    }

    public Sequence() {
        seqvalues = null;
    }

    public Sequence(int n, float value) {
        seqvalues = new float[n];
        for (int i = 0; i < n; i++) {
            seqvalues[i] = value;
        }
    }

    public Sequence(float[] v) {
        seqvalues = v;
    }

    public Sequence(double[] v) {
        seqvalues = new float[v.length];
        for (int i = 0; i < v.length; i++) {
            seqvalues[i] = (float) v[i];
        }
    }

    public Sequence(Sequence S) {
        seqvalues = new float[S.seqvalues.length];
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = S.seqvalues[i];
        }
    }

    public float get(int n) {
        return seqvalues[n];
    }

    public float[] getArray() {
        return seqvalues;
    }

    public int length() {
        return seqvalues.length;
    }

    public float min() {
        float smin = seqvalues[0];
        for (float seqvalue : seqvalues) {
            if (seqvalue < smin) {
                smin = seqvalue;
            }
        }
        return smin;
    }

    public float max() {
        float smax = seqvalues[0];
        for (float seqvalue : seqvalues) {
            if (seqvalue > smax) {
                smax = seqvalue;
            }
        }
        return smax;
    }

    public float extremum() {
        float smax = 0.0f;
        float sabs = 0.0f;
        for (float seqvalue : seqvalues) {
            sabs = Math.abs(seqvalue);
            if (sabs > smax) {
                smax = sabs;
            }
        }
        return smax;
    }

    public int extremumIndex() {
        float smax = 0.0f;
        float sabs = 0.0f;
        int index = 0;
        for (int i = 0; i < seqvalues.length; i++) {
            sabs = Math.abs(seqvalues[i]);
            if (sabs > smax) {
                smax = sabs;
                index = i;
            }
        }
        return index;
    }

    public float mean() {
        float smean = 0.0f;
        for (float seqvalue : seqvalues) {
            smean += seqvalue;
        }
        smean /= seqvalues.length;
        return smean;
    }

    public void rmean() {
        float smean = mean();
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] -= smean;
        }
    }

    public void zero() {
        Arrays.fill(seqvalues, 0.0f);
    }

    public void scaleBy(float a) {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] *= a;
        }
    }

    public void reverse() {
        float tmp;
        int j = seqvalues.length - 1;
        int i = 0;
        while (true) {
            if (j <= i) {
                break;
            }
            tmp = seqvalues[i];
            seqvalues[i] = seqvalues[j];
            seqvalues[j] = tmp;
            i++;
            j--;
        }
    }

    public void zshift(int shift) {

        int n;
        int srcptr, dstptr;
        if (Math.abs(shift) > seqvalues.length) {
            zero(seqvalues, 0, seqvalues.length); // zeroes sequence
        } else if (shift < 0) { // left shift

            n = seqvalues.length + shift;
            dstptr = 0;
            srcptr = -shift;
            for (int i = 0; i < n; i++) {
                seqvalues[dstptr++] = seqvalues[srcptr++];
            }

            zero(seqvalues, seqvalues.length + shift, -shift); // zero high end

        } else if (shift > 0) { // right shift

            n = seqvalues.length - shift;
            dstptr = seqvalues.length - 1;
            srcptr = dstptr - shift;
            for (int i = 0; i < n; i++) {
                seqvalues[dstptr--] = seqvalues[srcptr--];
            }

            zero(seqvalues, 0, shift); // zero low end

        }

    }

    // zshift sequence group - in place
    public static void zshift(float[] x, int shift) {

        int n = x.length;
        int src, dst;
        if (shift >= n || -shift >= n) { // shift off of end of array
            Arrays.fill(x, 0.0f);
            return;
        }

        if (shift > 0) { // shift to right

            dst = n - 1;
            src = dst - shift;
            while (src >= 0) {
                x[dst--] = x[src--];
            }

            Arrays.fill(x, 0, shift, 0.0f);

        } else if (shift < 0) { // shift to left

            dst = 0;
            src = -shift;
            while (src < n) {
                x[dst++] = x[src++];
            }

            Arrays.fill(x, n + shift, n, 0.0f); // zero high end

        }

    }

    public static void zshift(double[] x, int shift) {

        int n = x.length;
        int src, dst;
        if (shift >= n || -shift >= n) { // shift off of end of array
            Arrays.fill(x, 0.0f);
            return;
        }

        if (shift > 0) { // shift to right

            dst = n - 1;
            src = dst - shift;
            while (src >= 0) {
                x[dst--] = x[src--];
            }

            Arrays.fill(x, 0, shift, 0.0f);

        } else if (shift < 0) { // shift to left

            dst = 0;
            src = -shift;
            while (src < n) {
                x[dst++] = x[src++];
            }

            Arrays.fill(x, n + shift, n, 0.0f); // zero high end

        }

    }

    public static void zshift(float[][] x, int shift) {
        for (float[] element : x) {
            zshift(element, shift);
        }
    }

    public static void zshift(double[][] x, int shift) {
        for (double[] element : x) {
            zshift(element, shift);
        }
    }

    public void cshift(int shift) {

        //  Arguments:
        //  ----------

        //  int shift           number of samples to shift.

        //                      a negative number indicates a shift left.
        //                      a positive number indicates a shift right.
        //                      zero indicates no shift.

        int bsize = Math.abs(shift);
        float[] buffer = new float[bsize];
        int n = seqvalues.length;

        // two cases - right and left shifts

        int i, j;
        if (shift > 0) { // right shift

            shift = shift % n; // prevent extraneous transfers

            j = n - shift;
            for (i = 0; i < shift; i++) {
                buffer[i] = seqvalues[j++];
            }
            j = n - 1;
            i = j - shift;
            while (i >= 0) {
                seqvalues[j--] = seqvalues[i--];
            }
            for (i = 0; i < shift; i++) {
                seqvalues[i] = buffer[i];
            }

        } else if (shift < 0) { // left shift

            shift = shift % n; // prevent extraneous transfers

            for (i = 0; i < -shift; i++) {
                buffer[i] = seqvalues[i];
            }
            j = 0;
            i = -shift;
            while (i < n) {
                seqvalues[j++] = seqvalues[i++];
            }
            j = n + shift;
            for (i = 0; i < -shift; i++) {
                seqvalues[j++] = buffer[i];
            }

        }

    }

    //  sequence circular shift group  - in place
    public static void cshift(float[] x, int shift) {

        //  Arguments:
        //  ----------

        //  float x            array of doubles to be shifted.
        //  int   shift        number of samples to shift.

        //                      a negative number indicates a shift left.
        //                      a positive number indicates a shift right.
        //                      zero indicates no shift.

        int n = x.length;

        shift = shift % n; // prevent extraneous transfers

        int altshift = shift; // investigate smaller shift
        if (shift > 0) {
            altshift = shift - n;
        } else if (shift < 0) {
            altshift = shift + n;
        }

        if (Math.abs(shift) > Math.abs(altshift)) {
            shift = altshift;
        }

        int bsize = Math.abs(shift);
        if (bsize == 0) {
            return;
        }
        float[] buffer = new float[bsize];

        // two cases - right and left shifts

        int i, j;
        if (shift > 0) { // right shift

            j = n - shift;
            for (i = 0; i < shift; i++) {
                buffer[i] = x[j++];
            }
            j = n - 1;
            i = j - shift;
            while (i >= 0) {
                x[j--] = x[i--];
            }
            for (i = 0; i < shift; i++) {
                x[i] = buffer[i];
            }

        } else if (shift < 0) { // left shift

            for (i = 0; i < -shift; i++) {
                buffer[i] = x[i];
            }
            j = 0;
            i = -shift;
            while (i < n) {
                x[j++] = x[i++];
            }
            j = n + shift;
            for (i = 0; i < -shift; i++) {
                x[j++] = buffer[i];
            }

        }

    }

    public static void cshift(double[] x, int shift) {

        //  Arguments:
        //  ----------

        //  double x            array of doubles to be shifted.
        //  int    shift        number of samples to shift.

        //                      a negative number indicates a shift left.
        //                      a positive number indicates a shift right.
        //                      zero indicates no shift.

        int n = x.length;

        shift = shift % n; // prevent extraneous transfers

        int altshift = shift; // investigate smaller shift
        if (shift > 0) {
            altshift = shift - n;
        } else if (shift < 0) {
            altshift = shift + n;
        }

        if (Math.abs(shift) > Math.abs(altshift)) {
            shift = altshift;
        }

        int bsize = Math.abs(shift);
        if (bsize == 0) {
            return;
        }
        double[] buffer = new double[bsize];

        // two cases - right and left shifts

        int i, j;
        if (shift > 0) { // right shift

            j = n - shift;
            for (i = 0; i < shift; i++) {
                buffer[i] = x[j++];
            }
            j = n - 1;
            i = j - shift;
            while (i >= 0) {
                x[j--] = x[i--];
            }
            for (i = 0; i < shift; i++) {
                x[i] = buffer[i];
            }

        } else if (shift < 0) { // left shift

            for (i = 0; i < -shift; i++) {
                buffer[i] = x[i];
            }
            j = 0;
            i = -shift;
            while (i < n) {
                x[j++] = x[i++];
            }
            j = n + shift;
            for (i = 0; i < -shift; i++) {
                x[j++] = buffer[i];
            }

        }

    }

    public static void cshift(float[][] x, int shift) {
        for (float[] element : x) {
            cshift(element, shift);
        }
    }

    public static void cshift(double[][] x, int shift) {
        for (double[] element : x) {
            cshift(element, shift);
        }
    }

    // Multiplies a subsequence of a Sequence by a window beginning at the index start
    //   and returns the windowed subsequence.
    // Assumes the Sequence is equal to zero outside of its legal range.
    //
    //       ****************************************************************
    //   +++++++++++++++++++++++++++++++
    //                                                      +++++++++++++++++++++
    public Sequence window(int start, Sequence window) {

        int n = window.length();
        float[] newseqvalues = new float[n];

        // check for overlap - if none, return with zero subsequence

        if (start < seqvalues.length && start + n > 0) {

            int index0 = Math.max(0, -start);
            int index1 = Math.min(seqvalues.length - start, n);

            float[] windowvalues = window.seqvalues;
            for (int i = index0; i < index1; i++) {
                newseqvalues[i] = seqvalues[i + start] * windowvalues[i];
            }

        }

        return new Sequence(newseqvalues);
    }

    //  window group
    public static void window(float[] x, int src, float[] w, float[] y, int dst) {

        int nw = w.length;
        int nx = x.length;
        int ny = y.length;

        int iy = 0;
        while (iy < dst) {
            y[iy++] = 0.0f;
        }

        int ix = src;
        for (int iw = 0; iw < nw; iw++) {
            if (0 <= ix && ix < nx && 0 <= iy && iy < ny) {
                y[iy] = w[iw] * x[ix];
            }
            ix++;
            iy++;
        }

        while (iy < ny) {
            y[iy++] = 0.0f;
        }

    }

    public static void window(double[] x, int src, double[] w, double[] y, int dst) {

        int nw = w.length;
        int nx = x.length;
        int ny = y.length;

        int iy = 0;
        while (iy < dst) {
            y[iy++] = 0.0;
        }

        int ix = src;
        for (int iw = 0; iw < nw; iw++) {
            if (0 <= ix && ix < nx && 0 <= iy && iy < ny) {
                y[iy] = w[iw] * x[ix];
            }
            ix++;
            iy++;
        }

        while (iy < ny) {
            y[iy++] = 0.0;
        }

    }

    // interprets the x, w and y sequences as complex
    public static void window(float[][] x, int src, float[][] w, float[][] y, int dst) {

        int nw = w[0].length;
        int nx = x[0].length;
        int ny = y[0].length;

        int iy = 0;
        while (iy < dst) {
            y[0][iy] = 0.0f;
            y[1][iy] = 0.0f;
            iy++;
        }

        int ix = src;
        for (int iw = 0; iw < nw; iw++) {
            if (0 <= ix && ix < nx && 0 <= iy && iy < ny) {
                y[0][iy] = w[0][iw] * x[0][ix] - w[1][iw] * x[1][ix];
                y[1][iy] = w[0][iw] * x[1][ix] + w[1][iw] * x[0][ix];
            }
            ix++;
            iy++;
        }

        while (iy < ny) {
            y[0][iy] = 0.0f;
            y[1][iy] = 0.0f;
            iy++;
        }

    }

    // interprets the x and y sequences as complex
    public static void window(double[][] x, int src, double[][] w, double[][] y, int dst) {

        int nw = w[0].length;
        int nx = x[0].length;
        int ny = y[0].length;

        int iy = 0;
        while (iy < dst) {
            y[0][iy] = 0.0;
            y[1][iy] = 0.0;
            iy++;
        }

        int ix = src;
        for (int iw = 0; iw < nw; iw++) {
            if (0 <= ix && ix < nx && 0 <= iy && iy < ny) {
                y[0][iy] = w[0][iw] * x[0][ix] - w[1][iw] * x[1][ix];
                y[1][iy] = w[0][iw] * x[1][ix] + w[1][iw] * x[0][ix];
            }
            ix++;
            iy++;
        }

        while (iy < ny) {
            y[0][iy] = 0.0;
            y[1][iy] = 0.0;
            iy++;
        }

    }

    public Sequence alias(int N) {
        float[] newseqvalues = new float[N];
        int index = 0;
        for (float seqvalue : seqvalues) {
            newseqvalues[index++] += seqvalue;
            if (index == N) {
                index = 0;
            }
        }
        return new Sequence(newseqvalues);
    }

    // alias group - with source and destination
    //   alias src to length of destination
    public static void alias(float[] src, float[] dst) {
        int N = dst.length;
        Arrays.fill(dst, 0.0f);
        for (int i = 0; i < src.length; i++) {
            dst[i % N] += src[i];
        }
    }

    public static void alias(double[] src, double[] dst) {
        int N = dst.length;
        Arrays.fill(dst, 0.0);
        for (int i = 0; i < src.length; i++) {
            dst[i % N] += src[i];
        }
    }

    public static void alias(float[][] src, float[][] dst) {
        for (int i = 0; i < src.length; i++) {
            alias(src[i], dst[i]);
        }
    }

    public static void alias(double[][] src, double[][] dst) {
        for (int i = 0; i < src.length; i++) {
            alias(src[i], dst[i]);
        }
    }

    public void stretch(int factor) {
        int n = seqvalues.length;
        float[] sptr = new float[factor * n];
        zero(sptr, 0, factor * n);
        for (int i = n - 1; i >= 0; i--) {
            sptr[factor * i] = seqvalues[i];
        }
        seqvalues = sptr;
    }

    public void decimate(int factor) {
        int dlen = seqvalues.length / factor;
        if (dlen * factor < seqvalues.length) {
            dlen++;
        }
        float[] dptr = new float[dlen];
        for (int i = 0; i < dlen; i++) {
            dptr[i] = seqvalues[i * factor];
        }
        seqvalues = dptr;
    }

    // decimate group - with source and destination
    public static void decimate(float[] x, float[] y, int decrate) {
        int ix = 0;
        int iy = 0;
        while (ix < x.length) {
            y[iy++] = x[ix];
            ix += decrate;
        }
    }

    public static void decimate(double[] x, double[] y, int decrate) {
        int ix = 0;
        int iy = 0;
        while (ix < x.length) {
            y[iy++] = x[ix];
            ix += decrate;
        }
    }

    public static void decimate(float[][] x, float[][] y, int decrate) {
        for (int i = 0; i < x.length; i++) {
            decimate(x[i], y[i], decrate);
        }
    }

    public static void decimate(double[][] x, double[][] y, int decrate) {
        for (int i = 0; i < x.length; i++) {
            decimate(x[i], y[i], decrate);
        }
    }

    public boolean cut(int i1, int i2) {
        if (i2 < i1) {
            return false;
        }
        if (i1 < 0) {
            return false;
        }
        if (i2 > seqvalues.length - 1) {
            return false;
        }
        int n = i2 - i1 + 1;
        float[] newseqvalues = new float[n];
        for (int i = 0; i < n; i++) {
            newseqvalues[i] = seqvalues[i + i1];
        }
        seqvalues = newseqvalues;
        return true;
    }

    public void minusEquals(Sequence S) {
        int n = Math.min(seqvalues.length, S.seqvalues.length);
        for (int i = 0; i < n; i++) {
            seqvalues[i] -= S.seqvalues[i];
        }
    }

    /**
     * Replaces each point in the data array with its value plus the equivalent
     * value in another Sequence
     *
     * result[i] = orig[i] + othersequence[i]
     *
     * @param S
     *            - the other Sequence
     */
    public void plusEquals(Sequence S) {
        int n = Math.min(seqvalues.length, S.seqvalues.length);
        for (int i = 0; i < n; i++) {
            seqvalues[i] += S.seqvalues[i];
        }
    }

    /**
     * Replaces each point in the data array with its value divided by the
     * equivalent value in another Sequence
     *
     * result[i] = orig[i]/othersequence[i]
     *
     * @param S
     *            - the other Sequence object
     */
    public void divideBy(Sequence S) {
        int n = Math.min(seqvalues.length, S.seqvalues.length);
        for (int i = 0; i < n; i++) {
            seqvalues[i] /= S.seqvalues[i];
        }
    }

    /**
     * Treat the Sequences as vector and calculate the cross product of this
     * Sequence (A) with another (B)
     *
     * @param B
     *            another vector as a Sequence object
     * @return the cross product (AxB)
     */
    public Sequence cross(Sequence B) {
        if (length() != B.length()) {
            return null;
        }

        if (length() != 3) {
            // this method currently requires both Sequences to be 3 elements long  (aka a vector in 3-D space)
            return null;
        }

        double[] result = new double[length()];
        result[0] = get(1) * B.get(2) - get(2) * B.get(1); // i = a1*b2 - a2*b1
        result[1] = get(2) * B.get(0) - get(0) * B.get(2); // j = a2*b0 - a0*b2
        result[2] = get(0) * B.get(1) - get(1) * B.get(0); // k = a0*b1 - a1*b0

        return new Sequence(result);
    }

    /**
     * Takes the dot product between this Sequence and another Sequence object
     *
     * @param S
     *            - the other Sequence object
     * @return the float result of the dot product
     */
    public float dotprod(Sequence S) {
        int n = Math.min(seqvalues.length, S.seqvalues.length);
        float retval = 0.0f;
        float[] x = seqvalues;
        float[] y = S.seqvalues;
        for (int i = 0; i < n; i++) {
            retval += x[i] * y[i];
        }
        return retval;
    }

    /**
     * Replaces each point in the data array with its absolute value
     */
    public void abs() {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = Math.abs(seqvalues[i]);
        }
    }

    /**
     * Replaces each point in the data array with its square value
     */
    public void sqr() {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] *= seqvalues[i];
        }
    }

    /**
     * Replaces each point in the data array with its square root
     */
    public void sqrt() {
        for (int i = 0; i < seqvalues.length; i++) {
            if (seqvalues[i] < 0.0f) {
                seqvalues[i] = 0.0f;
            } else {
                seqvalues[i] = (float) Math.sqrt(seqvalues[i]);
            }
        }
    }

    /**
     * Replace each element in a sequence with it's log10 value
     */
    public void log10() {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = (float) Math.log10(seqvalues[i]);
        }
    }

    /**
     * Replace each element in a sequence with it's natural log value
     */
    public void log() {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = (float) Math.log(seqvalues[i]);
        }
    }

    /**
     * raise each of the values of the sequence to a power
     *
     * @param value
     *            --&gt; seq^value each element will be raised to the power
     *            (value) e.g. value= 2 squares the sequence value = 0.5 takes a
     *            square root
     */
    public void power(double value) {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = (float) Math.pow(seqvalues[i], value);
        }
    }

    /**
     * raise each of the values of the sequence to its
     *
     * @param value
     *            --&gt; seq^value each element will be raised to the power
     *            (value) e.g. value= 2 squares the sequence value = 0.5 takes a
     *            square root
     */
    public void power(int value) {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = (float) Math.pow(seqvalues[i], value);
        }
    }

    /**
     * change to Sequence to its signum sequence : +-1 or 0
     */
    public void signum() {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = Math.signum(seqvalues[i]);
        }
    }

    /**
     * Convert each element to the signed power(or root) Raise each Sequence
     * element to a power, then multiply by the original sign
     *
     * @param value
     *            - the power e.g. 2 == square, .5 == square root
     */
    public void signedPower(double value) {
        for (int i = 0; i < seqvalues.length; i++) {
            seqvalues[i] = (float) (Math.signum(seqvalues[i]) * Math.pow(Math.abs(seqvalues[i]), value));
        }
    }

    public void pad_to(int newlength) {
        int n = seqvalues.length;
        if (newlength > n) {
            float[] tmp = new float[newlength];
            for (int i = 0; i < n; i++) {
                tmp[i] = seqvalues[i];
            }
            seqvalues = tmp;
            for (int i = n; i < newlength; i++) {
                seqvalues[i] = 0.0f;
            }
        }
    }

    public void dftConjugate() {
        int n = seqvalues.length;
        for (int i = n / 2 + 1; i < n; i++) {
            seqvalues[i] = -seqvalues[i];
        }
    }

    static public Sequence dftprod(Sequence x, Sequence y, float c) {
        int n = x.length();
        int half = n / 2;
        Sequence tmp;

        if (n != y.length() || half * 2 != n) {
            tmp = new Sequence();
        } else {
            tmp = new Sequence(n);
            float[] xp = x.getArray();
            float[] yp = y.getArray();
            float[] tp = tmp.getArray();
            int k;
            tp[0] = xp[0] * yp[0];
            tp[half] = xp[half] * yp[half];
            for (int i = 1; i < half; i++) {
                k = n - i;
                tp[i] = xp[i] * yp[i] - c * xp[k] * yp[k];
                tp[k] = xp[k] * yp[i] + c * xp[i] * yp[k];
            }
        }
        return tmp;
    }

    public void dftAlias(int factor) {

        //
        //  Routine to alias FFT of an N-length sequence
        //  to allow the computation of x[n*factor] from
        //  its transform X(k).  Useful in computing downsampled
        //  sequences from their transforms.
        //
        //               1  N-1       j2(pi)kn/M
        // x[n*factor] = -  sum X(k) e
        //               N  k=0
        //
        //               1  M-1   M  f-1          j2(pi)rn
        // x[n*factor] = -  sum ( -  sum X(r+Mp) e         )
        //               M  r=0   N  p=0
        //
        // M = N / factor, exactly an integer
        //
        // Assumes transform is held in Sorensen & Bonzanigo's format
        //
        // Xr(0), Xr(1), ..., Xr(N/2), Xi(N/2-1), ..., Xi(1)
        //

        int N = seqvalues.length;
        int M = N / factor;
        int k, r, p;

        float[] ptr = new float[M];

        float Yrr, Yri;

        for (r = 0; r <= M / 2; r++) {
            Yrr = 0.0f;
            Yri = 0.0f;
            for (p = 0; p < factor; p++) {
                k = r + M * p;
                if (k <= N / 2) {
                    Yrr += seqvalues[k];
                    if (k > 0 && k < N / 2) {
                        Yri += seqvalues[N - k];
                    }
                } else {
                    Yrr += seqvalues[N - k];
                    Yri -= seqvalues[k];
                }
            }
            ptr[r] = Yrr / factor;
            if (r != 0 && r != M / 2) {
                ptr[M - r] = Yri / factor;
            }
        }

        seqvalues = ptr;
    }

    // pad sequence group
    public static float[] pad(float[] x, int n) {
        int N = Math.max(x.length, n);
        float[] retval = new float[N];
        for (int i = 0; i < x.length; i++) {
            retval[i] = x[i];
        }
        if (x.length < N) {
            for (int i = x.length; i < N; i++) {
                retval[i] = 0.0f;
            }
        }
        return retval;
    }

    public static double[] pad(double[] x, int n) {
        int N = Math.max(x.length, n);
        double[] retval = new double[N];
        for (int i = 0; i < x.length; i++) {
            retval[i] = x[i];
        }
        if (x.length < N) {
            for (int i = x.length; i < N; i++) {
                retval[i] = 0.0;
            }
        }
        return retval;
    }

    public static float[][] pad(float[][] x, int n) {
        float[][] retval = new float[x.length][];
        for (int i = 0; i < x.length; i++) {
            retval[i] = Sequence.pad(x[i], n);
        }
        return retval;
    }

    public static double[][] pad(double[][] x, int n) {
        double[][] retval = new double[x.length][];
        for (int i = 0; i < x.length; i++) {
            retval[i] = Sequence.pad(x[i], n);
        }
        return retval;
    }

    public void print(PrintStream ps) {
        for (float seqvalue : seqvalues) {
            ps.println(seqvalue);
        }
    }

    public void set(int i, float f) {
        if (i >= 0 && i < seqvalues.length) {
            seqvalues[i] = f;
        }
    }

    public void setConstant(float c) {
        Arrays.fill(seqvalues, c);
    }

    protected void zero(float[] s, int start, int duration) {
        int j = start;
        for (int i = 0; i < duration; i++) {
            s[j++] = 0.0f;
        }
    }

    // zero sequence group
    public static void zero(float[] x) {
        Arrays.fill(x, 0.0f);
    }

    public static void zero(double[] x) {
        Arrays.fill(x, 0.0);
    }

    public static void zero(float[][] x) {
        for (float[] element : x) {
            Arrays.fill(element, 0.0f);
        }
    }

    public static void zero(double[][] x) {
        for (double[] element : x) {
            Arrays.fill(element, 0.0);
        }
    }
}