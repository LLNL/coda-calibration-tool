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
package llnl.gnem.core.util.randomNumbers;

import java.util.Random;

/**
 * Fast, high-quality pseudorandom number generator suggested by George
 * Marsaglia in <a
 * href="http://www.jstatsoft.org/v08/i14/paper/">&ldquo;Xorshift
 * RNGs&rdquo;</a>, <i>Journal of Statistical Software</i>, 8:1&minus;6, 2003.
 * Calls to {@link #nextLong()} will be one order of magnitude faster than {@link Random}'s.
 * <p> This class extends {@link Random}, overriding (as usual) the {@link Random#next(int)}
 * method. Nonetheless, since the generator is inherently 64-bit also {@link Random#nextLong()}
 * and {@link Random#nextDouble()} have been overridden for speed (preserving,
 * of course, {@link Random}'s semantics).
 */
public class XorShiftRandom extends BaseRandomAlgorithm {

    private static final long serialVersionUID = 1L;
    private final Random myRandom;
    /**
     * The internal state (and last returned value) of the algorithm.
     */
    private long x;

    public XorShiftRandom() {
        myRandom = new Random();
      
    }

    public XorShiftRandom(final long seed) {
        myRandom = new Random(seed);
        x = seed;
    }

    public int next(int bits) {
        return (int) (nextLong() >>> (64 - bits));
    }

    @Override
    public long nextLong() {
        x ^= x << 13;
        x ^= x >>> 7;
        return x ^= (x << 17);
    }

    @Override
    public double nextDouble() {
        return (nextLong() >>> 11) / (double) (1L << 53);
    }

    @Override
    public int nextInt() {
        return myRandom.nextInt();
    }

    @Override
    public int nextInt(int n) {
        return myRandom.nextInt(n);
    }

    @Override
    public void resetSeed(long seed) {
        myRandom.setSeed(seed);
        x = seed;
    }

}