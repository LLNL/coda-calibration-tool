//                                             SACFileReader.java
//  copyright 2004 Regents of the University of California
//  All rights reserved
//  Author:   Dave Harris
//  Created:
//  Last Modified:  August 11, 2006
package llnl.gnem.core.io.SAC;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.nio.file.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import llnl.gnem.core.io.FileDataSource;
import llnl.gnem.core.signalprocessing.Sequence;
import llnl.gnem.core.util.TimeT;

public class SACFileReader extends FileDataSource {

    private static final Logger log = LoggerFactory.getLogger(SACFileReader.class);

    // instance variables
    public SACHeader header;
    public TimeT timeT;

    public SACFileReader(String filename) throws IOException {
        this(new File(filename));
    }

    public SACFileReader(File file) throws IOException {
        this(Files.newInputStream(file.toPath()));
        path = file.getAbsolutePath();
    }

    public SACFileReader(InputStream stream) {
        try {
            header = new SACHeader(stream);

            totalNumSamples = header.npts;
            nextSample = 0;
            numSamplesRemaining = totalNumSamples;
            station = (header.kstnm).trim();
            channel = (header.kcmpnm).trim();
            samplingRate = 1.0 / (header.delta);
            timeT = new TimeT(header.nzyear, header.nzjday, header.nzhour, header.nzmin, header.nzsec, header.nzmsec);
            foff = 4 * WORDS_IN_HEADER;
            timeT = timeT.add(header.b);
            startTime = timeT.getEpochTime();

            if (header.checkByteSwap()) {
                format = CSS_F4;
            } else {
                format = CSS_T4;
            }

            initiate(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e.getMessage());
        }
    }

    private static final int WORDS_IN_HEADER = 158;

    public float[] getAllSamples() {
        float[] result = new float[header.npts];
        readFloatArray(result);
        return result;
    }

    public float[] getData(ObjectInput in, SACHeader header) {
        int numSamplesToRead = header.npts;
        float[] dataArray = new float[header.npts];

        try {

            int numBytesToRead = 0;
            boolean legitFormat = false;
            switch (format) {

            // calculate #bytes to read and allocate buffer space as appropriate
            case CSS_S4:
            case CSS_T4:
            case CSS_F4:

                numBytesToRead = numSamplesToRead * 4;
                legitFormat = true;
                break;

            case CSS_S3:

                numBytesToRead = numSamplesToRead * 3;
                legitFormat = true;
                break;

            default:
                log.warn("io.FileDataSource:  unsupported format " + format);

            }

            buffer = new byte[numBytesToRead];

            in.read(buffer, 0, numBytesToRead);

            DataInputStream dis;
            if (legitFormat) {
                dis = new DataInputStream(new ByteArrayInputStream(buffer));
            } else {
                throw new IOException("io.SacFileReader#getData:  Unsupported format: " + format);
            }

            int offset = 0, ib = 0;
            // perform appropriate read
            switch (format) {

            case CSS_S4:

                for (int i = 0; i < numSamplesToRead; i++) {
                    dataArray[i + offset] = dis.readInt();
                }
                numSamplesRemaining -= numSamplesToRead;
                nextSample += numSamplesToRead;
                break;

            case CSS_T4:

                for (int i = 0; i < numSamplesToRead; i++) {
                    dataArray[i + offset] = dis.readFloat();
                }
                numSamplesRemaining -= numSamplesToRead;
                nextSample += numSamplesToRead;
                break;

            case CSS_S3:

                ib = 0;
                for (int i = 0; i < numSamplesToRead; i++) {

                    if ((0x80 & buffer[ib]) == 0x80) {
                        dataArray[i + offset] = ((0xff) << 24) + ((buffer[ib++] & 0xff) << 16) + ((buffer[ib++] & 0xff) << 8) + ((buffer[ib++] & 0xff));
                    } else {
                        dataArray[i + offset] = ((0x00) << 24) + ((buffer[ib++] & 0xff) << 16) + ((buffer[ib++] & 0xff) << 8) + ((buffer[ib++] & 0xff));
                    }

                }
                numSamplesRemaining -= numSamplesToRead;
                nextSample += numSamplesToRead;

                break;

            case CSS_F4:

                ib = 0;
                for (int i = 0; i < numSamplesToRead; i++) {
                    dataArray[i + offset] = Float.intBitsToFloat( // float conversion
                            ((buffer[ib++] & 0xff)) + // per Phil Crotwell's
                                    ((buffer[ib++] & 0xff) << 8) + // suggestion
                                    ((buffer[ib++] & 0xff) << 16) + ((buffer[ib++] & 0xff) << 24));
                }
                numSamplesRemaining -= numSamplesToRead;
                nextSample += numSamplesToRead;

                break;

            default:

                log.warn("io.FileDataSource:  unsupported format");

            }
            dis.close();

        } catch (IOException ioe) {
            log.warn("io.FileDataSource  " + ioe.getMessage());
        }
        return dataArray;
    }

    public SACHeader getHeader() {
        return header;
    }

    public int getNumPtsRemaining() {
        return (int) numSamplesRemaining;
    } // legacy

    public TimeT getStartTime() {
        return timeT;
    }

    public void readFloatArray(float[] samples) { // legacy
        getData(samples, 0, samples.length);
    }

    public Sequence readSequence(int nPtsRequested) { // legacy
        int n = Math.min(nPtsRequested, (int) numSamplesRemaining);
        float[] seqv = new float[n];
        getData(seqv, 0, n);
        return new Sequence(seqv);
    }

}
