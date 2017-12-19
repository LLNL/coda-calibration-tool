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
 *
 * @author dodge1
 */
public class SimpleRandom extends BaseRandomAlgorithm {

    private final Random myRandom;

    public SimpleRandom() {
        myRandom = new Random();
    }

    public SimpleRandom(final long seed) {
        myRandom = new Random(seed);

    }

    @Override
    public double nextDouble() {
        return myRandom.nextDouble();
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
    public long nextLong() {
        return myRandom.nextLong();
    }

    @Override
    public void resetSeed(long seed) {
        myRandom.setSeed(seed);
    }
   
}
