/*
 *
 * Copyright (c) 2004 Regents of the University of California
 * All rights reserved
 *
 * Author:  Dave Harris
 *
 * Created on October 5, 2004 4:18AM
 * Last Modified:  October, 5, 2004
 */
package llnl.gnem.core.io;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDataSource extends AbstractDataSource {
    private static final Logger log = LoggerFactory.getLogger(FileDataSource.class);
    protected String path;
    protected InputStream fis;
    protected int format;
    protected byte[] buffer; // temporary buffer for reading data, persists between reads
    protected long foff; // offset to first byte of data (for formats with headers, offsets)
    public static final int CSS_T4 = 1, CSS_S4 = 2, CSS_S3 = 3, CSS_F4 = 4;

    /**
     * Creates new FileDataSource
     */
    public FileDataSource() {
        path = null;
        format = 0;
        buffer = null;
        fis = null;
        foff = 0;
    }

    public FileDataSource(String path, int format, int firstDataByte) {

        this.path = path;
        this.format = format;
        buffer = null;
        fis = null;
        foff = firstDataByte;

    }

    @Override
    public void initiate() {
        super.initiate();
        close();
        try {
            initiate(Files.newInputStream(Paths.get(path)));
            fis.skip(foff); // skip bytes, as necessary
        } catch (IOException ioe) {
            log.warn("io.FileDataSource.initiate():  {} ", ioe.getMessage(), ioe);
            fis = null;
        }
    }

    protected void initiate(InputStream fis) {
        this.fis = fis;
    }

    @Override
    public void close() {
        if (fis != null) {
            try {
                fis.close();
                fis = null;
            } catch (IOException ioe) {
                log.warn("io.FileDataSource.close(): {} ", ioe.getMessage(), ioe);
            }
        }
    }

    @Override
    public void skipSamples(long numSamples) {

        long numSamplesToSkip = Math.min(numSamples, numSamplesRemaining);
        try {

            switch (format) {

            case CSS_S4:
            case CSS_T4:
            case CSS_F4:

                fis.skip(4 * numSamplesToSkip);
                break;

            case CSS_S3:

                fis.skip(3 * numSamplesToSkip);
                break;

            default:

                log.warn("io.FileDataSource:  unsupported format");

            }

            numSamplesRemaining -= numSamplesToSkip;
            nextSample += numSamplesToSkip;

        } catch (IOException ioe) {
            log.warn(ioe.getMessage());
        }

    }

    @Override
    public void getData(float[] dataArray, int offset, int numRequested) {

        //  compute number of samples and bytes to read

        int numBytesToRead = 0;
        int numSamplesToRead = (int) Math.min(numRequested, numSamplesRemaining);
        DataInputStream dis = null;
        boolean legitFormat = false;
        int ib;

        try {

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

            if (buffer == null || buffer.length < numBytesToRead) {
                buffer = new byte[numBytesToRead];
            }
            if (fis == null) {
                initiate();
            }
            fis.read(buffer, 0, numBytesToRead);

            if (legitFormat) {
                dis = new DataInputStream(new ByteArrayInputStream(buffer));
            } else {
                dis = new DataInputStream(new ByteArrayInputStream(new byte[0]));
            }

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

            if (dis != null) {
                dis.close();
            }

        } catch (IOException ioe) {
            log.warn("io.FileDataSource  " + ioe.getMessage());
        }

        if (numSamplesToRead < numRequested) {
            for (int i = numSamplesToRead; i < numRequested; i++) {
                dataArray[i + offset] = 0.0f;
            }
        }

    }

    public String getfilename() {
        return path;
    }

    @Override
    public void print(PrintStream ps) {
        ps.println("io.FileDataSource:");
        super.print(ps);
    }
}
