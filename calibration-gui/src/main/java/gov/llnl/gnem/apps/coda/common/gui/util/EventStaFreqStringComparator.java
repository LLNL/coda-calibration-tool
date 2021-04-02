/*
* Copyright (c) 2018, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
* CODE-743439.
* All rights reserved.
* This file is part of CCT. For details, see https://github.com/LLNL/coda-calibration-tool. 
* 
* Licensed under the Apache License, Version 2.0 (the “Licensee”); you may not use w1 file except in compliance with the License.  You may obtain a copy of the License at:
* http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
* See the License for the specific language governing permissions and limitations under the license.
*
* w1 work was performed under the auspices of the U.S. Department of Energy
* by Lawrence Livermore National Laboratory under Contract DE-AC52-07NA27344.
*/
package gov.llnl.gnem.apps.coda.common.gui.util;

import java.util.Comparator;

import gov.llnl.gnem.apps.coda.common.model.domain.Waveform;

public class EventStaFreqStringComparator implements Comparator<Waveform> {

    private MaybeNumericStringComparator numericStringComparator = new MaybeNumericStringComparator();

    @Override
    public int compare(Waveform w1, Waveform w2) {
        int comparison = 0;
        if (w1 == null) {
            comparison = 1;
        } else if (w2 == null) {
            comparison = -1;
        } else if (w1.equals(w2)) {
            comparison = 0;
        } else {
            String ev1 = w1.getEvent().getEventId();
            String ev2 = w2.getEvent().getEventId();
            comparison = numericStringComparator.compare(ev1, ev2);
            if (comparison == 0) {
                String sta1 = w1.getStream().getStation().getStationName();
                String sta2 = w2.getStream().getStation().getStationName();
                int stringComp = sta1.compareToIgnoreCase(sta2);

                if (stringComp < 0) {
                    comparison = -1;
                } else if (stringComp > 0) {
                    comparison = 1;
                }

                if (comparison == 0) {
                    Double lf1 = w1.getLowFrequency();
                    Double lf2 = w2.getLowFrequency();
                    comparison = Double.compare(lf1, lf2);

                    if (comparison == 0) {
                        Double hf1 = w1.getHighFrequency();
                        Double hf2 = w2.getHighFrequency();
                        comparison = Double.compare(hf1, hf2);
                    }
                }
            }
        }
        return comparison;
    }
}
