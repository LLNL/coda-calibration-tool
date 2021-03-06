/*
* Copyright (c) 2019, Lawrence Livermore National Security, LLC. Produced at the Lawrence Livermore National Laboratory
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
package gov.llnl.gnem.apps.coda.calibration.service.impl;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Component
@Configuration
public class ServiceConfig {
    private static final Object lock = new Object();

    private static ExecutorService measurementServicePool;

    @Value("${spectraTruncationEnabled:true}")
    private boolean spectraTruncationEnabled;

    @Value("${measurementPoolSize:10}")
    private int measurementPoolSize;

    @Bean("MeasurementExecutorService")
    public ExecutorService getMeasurementExecutor() {
        synchronized (lock) {
            if (measurementServicePool == null) {
                measurementServicePool = new ThreadPoolExecutor(measurementPoolSize, measurementPoolSize, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(5 * measurementPoolSize), r -> {
                    Thread thread = new Thread(r);
                    thread.setName("Measurement");
                    thread.setDaemon(true);
                    return thread;
                });
            }
        }
        return measurementServicePool;
    }

    public boolean isSpectraTruncationEnabled() {
        return spectraTruncationEnabled;
    }

    public ServiceConfig setSpectraTruncationEnabled(boolean spectraTruncationEnabled) {
        this.spectraTruncationEnabled = spectraTruncationEnabled;
        return this;
    }

}
