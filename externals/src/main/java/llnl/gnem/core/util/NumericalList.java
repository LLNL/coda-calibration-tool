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
package llnl.gnem.core.util;

/**
 * Lightweight wrapper for accessing an array of arbitrary numerical type as a
 * double.
 *
 * @author addair1
 */
public interface NumericalList {
    public double get(int i);

    public void set(int i, double value);

    public int size();
    
    public NumericalList clone();

    public static class FloatList implements NumericalList {
        private final float[] data;

        public FloatList(float[] data) {
            this.data = data;
        }

        @Override
        public double get(int i) {
            return data[i];
        }

        @Override
        public void set(int i, double value) {
            data[i] = (float) value;
        }

        @Override
        public int size() {
            return data.length;
        }
        
        @Override
        public FloatList clone() {
            return new FloatList(data.clone());
        }
    }

    public static class DoubleList implements NumericalList {
        private final double[] data;

        public DoubleList(double[] data) {
            this.data = data;
        }

        @Override
        public double get(int i) {
            return data[i];
        }

        @Override
        public void set(int i, double value) {
            data[i] = value;
        }

        @Override
        public int size() {
            return data.length;
        }
        
        @Override
        public DoubleList clone() {
            return new DoubleList(data.clone());
        }
    }

    public static class NumberList implements NumericalList {
        private final Number[] data;

        public NumberList(Number[] data) {
            this.data = data;
        }

        @Override
        public double get(int i) {
            return data[i].doubleValue();
        }

        @Override
        public void set(int i, double value) {
            data[i] = value;
        }

        @Override
        public int size() {
            return data.length;
        }
        
        @Override
        public NumberList clone() {
            return new NumberList(data.clone());
        }
    }
}
