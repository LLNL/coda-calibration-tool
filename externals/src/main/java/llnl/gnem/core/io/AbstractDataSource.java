/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package llnl.gnem.core.io;

import java.io.PrintStream;

import llnl.gnem.core.util.TimeT;

public abstract class AbstractDataSource implements DataSource {

    protected long totalNumSamples;
    protected long nextSample;
    protected long numSamplesRemaining;
    protected String station;
    protected String channel;
    protected double samplingRate;
    protected double startTime;

    public void getData(float[] dataArray) {
        getData(dataArray, 0, dataArray.length);
    }

    public long getNumSamplesAvailable() {
        return numSamplesRemaining;
    }

    public long getTotalNumSamples() {
        return totalNumSamples;
    }

    public String getStation() {
        return station;
    }

    public String getChannel() {
        return channel;
    }

    public double getSamplingRate() {
        return samplingRate;
    }

    public long getNextSampleIndex() {
        return nextSample;
    }

    public double getEpochStartTime() {
        return startTime;
    }

    public double getEpochEndTime() {
        return startTime + ((double) (totalNumSamples - 1)) / samplingRate;
    }

    public double getCurrentEpochTime() {
        return startTime + ((double) (nextSample)) / samplingRate;
    }

    public void initiate() {
        numSamplesRemaining = totalNumSamples;
        nextSample = 0;
    }

    public void print(PrintStream ps) {
        ps.println("  Station:                     " + station);
        ps.println("  Channel:                     " + channel);
        ps.println("  Sampling rate:               " + samplingRate + " samples per second");
        TimeT T = new TimeT(startTime);
        ps.println("  Start time:                  " + T.toString());
        T = new TimeT(getCurrentEpochTime());
        ps.println("  Current time:                " + T.toString());
        T = new TimeT(getEpochEndTime());
        ps.println("  End time:                    " + T.toString());
        ps.println("  Number of samples:           " + totalNumSamples);
        ps.println("  Current sample index:        " + nextSample);
        ps.println("  Number of samples available: " + numSamplesRemaining);
    }

}
