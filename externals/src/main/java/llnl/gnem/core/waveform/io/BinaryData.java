/*
* Copyright (c) 2017, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.waveform.io;

/**
 * Return type for the readData method of BinaryDataReader interface. Supports
 * int, float, and double arrays currently. Can convert internal contents to
 * desired format on demand.
 */
public abstract class BinaryData {

    /**
     * Returns an array of ints corresponding to the contents of this object. If
     * the internal data are in float or double form a loss of precision may
     * occur.
     *
     * @return The contents of this object as an array of ints.
     */
    public abstract int[] getIntData();

    /**
     * Returns an array of floats corresponding to the contents of this object.
     * If the internal data are in double form a loss of precision may occur. If
     * the internal data are stored as ints with values that require more
     * precision that can be represented by a float then a loss of precision
     * will occur.
     *
     * @return The contents of this object as an array of floats.
     */
    public abstract float[] getFloatData();

    /**
     * Returns an array of doubles corresponding to the contents of this object.
     * If the internal data are stored as ints with values that require more
     * precision that can be represented by a double then a loss of precision
     * will occur.
     *
     * @return The contents of this object as an array of doubles.
     */
    public abstract double[] getDoubleData();

    public abstract int getInt(int i);

    public abstract int size();

    public abstract void setInt(int i, int value);

    public abstract void setFloat(int i, float value);

    public abstract void setDouble(int i, double value);

    public void fillAsInts(byte[] in, boolean isLittleEndian) {
        int ib = 0;
        int bnpts = in.length;
        if (bnpts < 4) {
            throw new IllegalArgumentException("Input byte array too short for conversion!");
        }
        for (int j = 0; j < size(); ++j) {
            if (isLittleEndian) {
                setInt(j, ((in[ib++] & 0xff)) + ((in[ib++] & 0xff) << 8) + ((in[ib++] & 0xff) << 16) + ((in[ib++] & 0xff) << 24));
            } else {
                setInt(j, ((in[ib++] & 0xff) << 24) + ((in[ib++] & 0xff) << 16) + ((in[ib++] & 0xff) << 8) + ((in[ib++] & 0xff)));
            }

        }
    }

    public void fillAsFloats(byte[] in, boolean swapBytes) {
        int ib = 0;
        int bnpts = in.length;
        if (bnpts < 4) {
            throw new IllegalArgumentException("Input byte array too short for conversion!");
        }
        for (int j = 0; j < size(); ++j) {
            byte a = in[ib++];
            byte b = in[ib++];
            byte c = in[ib++];
            byte d = in[ib++];
            if (swapBytes) {
                setFloat(j,
                        Float.intBitsToFloat(((a & 0xff))
                                + ((b & 0xff) << 8)
                                + ((c & 0xff) << 16)
                                + ((d & 0xff) << 24)));
            } else {
                setFloat(j, Float.intBitsToFloat(((a & 0xff) << 24) + ((b & 0xff) << 16) + ((c & 0xff) << 8) + (d & 0xff)));
            }
        }
    }

    public static BinaryData truncateTo(BinaryData source, int newLength) {
        if (source instanceof IntBinaryData) {
            return new IntBinaryData((IntBinaryData)source, newLength);
        } else if (source instanceof FloatBinaryData) {
            return new FloatBinaryData((FloatBinaryData)source, newLength);
        } else if (source instanceof DoubleBinaryData) {
            return new DoubleBinaryData((DoubleBinaryData)source, newLength);
        } else {
            throw new IllegalStateException("Unsupported BinaryData type!");
        }
    }
}
