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
package gov.llnl.gnem.apps.coda.calibration.gui.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeLatchedGetSet {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final ScheduledThreadPoolExecutor scheduledGet = new ScheduledThreadPoolExecutor(1);
    private final ScheduledThreadPoolExecutor scheduledSet = new ScheduledThreadPoolExecutor(1);

    private Runnable getter;
    private Runnable setter;

    private long setDelay;
    private long getDelay;
    private long failsafeDelay;

    public TimeLatchedGetSet(Runnable getter, Runnable setter) {
        this(getter, setter, 10l, 150l, 1000l);
    }

    public TimeLatchedGetSet(Runnable getter, Runnable setter, long setDelay, long getDelay, long failsafeDelay) {
        this.getter = getter;
        this.setter = setter;
        this.setDelay = setDelay;
        this.getDelay = getDelay;
        this.failsafeDelay = failsafeDelay;

        scheduledGet.setRemoveOnCancelPolicy(true);
        scheduledSet.setRemoveOnCancelPolicy(true);
    }

    public void set() {
        if (setter != null) {
            scheduledSet.getQueue().clear();
            scheduledSet.schedule(() -> {
                setter.run();
                if (getter != null) {
                    scheduledGet.schedule(() -> {
                        getter.run();
                    }, failsafeDelay, TimeUnit.MILLISECONDS);
                }
            }, setDelay, TimeUnit.MILLISECONDS);
        }
    }

    public void get() {
        if (getter != null) {
            scheduledGet.getQueue().clear();
            scheduledGet.schedule(() -> {
                getter.run();
            }, getDelay, TimeUnit.MILLISECONDS);
        }
    }

}
