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
package gov.llnl.gnem.apps.coda.common.gui.util;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import gov.llnl.gnem.apps.coda.common.model.messaging.ProgressEvent;
import javafx.scene.control.ProgressBar;

public class ProgressEventProgressListener extends ProgressListener {

    private ProgressEvent cachedEvent;

    public ProgressEventProgressListener(EventBus bus, ProgressEvent event) {
        if (bus != null) {
            bus.register(this);
        }
        this.cachedEvent = event;
    }

    @Subscribe
    private void listener(ProgressEvent event) {
        if (cachedEvent.getId().equals(event.getId())) {
            cachedEvent = event;
            changeSupport.firePropertyChange(event.getClass().getName(), null, event.getProgress());
        }
    }

    @Override
    public double getProgress() {
        return cachedEvent != null ? cachedEvent.getProgress() != null ? cachedEvent.getProgress().getProgress() : ProgressBar.INDETERMINATE_PROGRESS : ProgressBar.INDETERMINATE_PROGRESS;
    }
}
