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
package llnl.gnem.core.gui.plotting;

/**
 * Class that contains parameters used to layout the ticks when rendering an
 * axis
 *
 * @author Doug Dodge
 */
public abstract class TickMetrics {

    /**
     * Minimum value of the axis
     */
    private final double min;
    /**
     * Maximum value of the axis
     */
    private final double max;
    private final boolean fullyDecorate;
    private double val;

    /**
     * Constructor for the TickMetrics object
     */
    public TickMetrics() {
        this(0.0, 1.0, false);
    }

    /**
     * Constructor for the TickMetrics object
     *
     * @param min Minimum value of the axis
     * @param max Maximum value of the axis
     * @param inc tick increment
     */
    public TickMetrics(double min, double max, boolean fullyDecorate) {
        this.min = min;
        this.max = max;
        this.fullyDecorate = fullyDecorate;
        val = min;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public boolean fullyDecorate() {
        return fullyDecorate;
    }

    protected double getValue() {
        return val;
    }

    protected void setValue(double val) {
        this.val = val;
    }

    public boolean hasNext() {
        return val <= max;
    }

    public double getNext() {
        double next = scale(val);

        if (fullyDecorate()) {
            val += getIncrement();
        } else {
            val += max - min;
        }
        if (Math.abs(val) < 1.0e-12) {
            val = 0.0;
        }

        setValue(val);
        return next;
    }
    
    public abstract double scale(double v);

    public abstract double getIncrement();

    public static class LinearTickMetrics extends TickMetrics {

        /**
         * Tick increment
         */
        private final double inc;

        /**
         * Constructor for the TickMetrics object
         */
        public LinearTickMetrics() {
            this(0.0, 1.0, 0.1, false);
        }

        /**
         * Constructor for the TickMetrics object
         *
         * @param min Minimum value of the axis
         * @param max Maximum value of the axis
         * @param inc tick increment
         */
        public LinearTickMetrics(double min, double max, double inc, boolean fullyDecorate) {
            super(min, max, fullyDecorate);
            this.inc = inc;
        }
        
        @Override
        public double scale(double v) {
            return v;
        }

        @Override
        public double getIncrement() {
            return inc;
        }
    }

    public static class LogTickMetrics extends TickMetrics {

        public LogTickMetrics(double min, double max, boolean fullyDecorate) {
            super(min, max, fullyDecorate);
        }
        
        @Override
        public double scale(double v) {
            return Math.pow(10.0, v);
        }

        @Override
        public double getIncrement() {
            return 1;
        }
    }
}
