/*
* Copyright (c) 2023, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.spectra.gui;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.calibration.model.messaging.RatioStatusEvent;
import gov.llnl.gnem.apps.coda.common.gui.util.ProgressListener;
import gov.llnl.gnem.apps.coda.common.model.messaging.Progress;

public class RatioStatusProgressListener extends ProgressListener {

    private RatioStatusEvent cachedEvent;
    private Progress progress = new Progress((long) RatioStatusEvent.Status.COMPLETE.ordinal(), 0l);

    public RatioStatusProgressListener(EventBus bus, RatioStatusEvent event) {
        this.cachedEvent = event;
        bus.register(this);
    }

    @Subscribe
    private void listener(RatioStatusEvent event) {
        if (cachedEvent != null && cachedEvent.getId().equals(event.getId())) {
            cachedEvent = event;
            if (cachedEvent.getStatus() == RatioStatusEvent.Status.COMPLETE || cachedEvent.getStatus() == RatioStatusEvent.Status.ERROR) {
                progress.setCurrent((long) RatioStatusEvent.Status.COMPLETE.ordinal());
            } else {
                progress.setCurrent((long) cachedEvent.getStatus().ordinal());
            }
            changeSupport.firePropertyChange(this.getClass().getName(), null, progress);
        }
    }

    @Override
    public double getProgress() {
        return progress.getProgress();
    }
}