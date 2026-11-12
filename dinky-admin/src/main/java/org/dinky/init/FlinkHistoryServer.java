/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dinky.init;

import org.dinky.data.model.SystemConfiguration;
import org.dinky.resource.BaseResourceManager;
import org.dinky.service.JobInstanceService;
import org.dinky.service.SysConfigService;

import org.apache.flink.runtime.webmonitor.history.HistoryServerUtil;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.NetUtil;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@Order(value = 2)
public class FlinkHistoryServer implements ApplicationRunner {
    public static final Set<String> HISTORY_JOBID_SET = new LinkedHashSet<>();

    private static final String[] REQUIRED_FLINK_CLASSES = {
            "org.apache.flink.util.FlinkException",
            "org.apache.flink.configuration.Configuration",
            "org.apache.flink.runtime.webmonitor.history.HistoryServer"
    };

    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            5, 20, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100), new ThreadPoolExecutor.DiscardOldestPolicy());

    private final Runnable historyRunnable;
    private final SystemConfiguration systemConfiguration = SystemConfiguration.getInstances();
    private final SysConfigService sysConfigService;

    public FlinkHistoryServer(JobInstanceService jobInstanceService, SysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
        this.historyRunnable = () -> {
            try {
                Map<String, String> flinkHistoryServerConfiguration =
                        SystemConfiguration.getInstances().getFlinkHistoryServerConfiguration();
                flinkHistoryServerConfiguration.putAll(BaseResourceManager.convertFlinkResourceConfig());

                HistoryServerUtil.run(
                        (jobId) -> {
                            HISTORY_JOBID_SET.add(jobId);
                            threadPoolExecutor.execute(() -> {
                                jobInstanceService.hookJobDoneByHistory(jobId);
                            });
                        },
                        flinkHistoryServerConfiguration);
            } catch (LinkageError | RuntimeException e) {
                log.error("Flink History service failed to start: ", e);
            }
        };
    }

    private static boolean isFlinkHistoryServerAvailable() {
        ClassLoader classLoader = FlinkHistoryServer.class.getClassLoader();
        for (String className : REQUIRED_FLINK_CLASSES) {
            try {
                Class.forName(className, false, classLoader);
            } catch (ClassNotFoundException | LinkageError e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (systemConfiguration.getUseFlinkHistoryServer().getValue() && !isFlinkHistoryServerAvailable()) {
            log.warn(
                    "Flink History Server is enabled but Flink runtime classes are not on the classpath, "
                            + "skip starting the embedded history server. In an IDE, activate the dev profile and reload Maven; "
                            + "for a release package, put the Flink 1.20 lib jars under extends/flink1.20.");
            return;
        }

        AtomicReference<Thread> historyThread = new AtomicReference<>(new Thread(historyRunnable));
        Runnable closeHistory = () -> {
            if (historyThread.get().isAlive()) {
                historyThread.get().interrupt();
                HISTORY_JOBID_SET.clear();
            }
        };

        // Check if the port is available
        Consumer<Integer> checkAndUpdatePort = (port) -> {
            if (!NetUtil.isUsableLocalPort(port)) {
                int usableLocalPort = NetUtil.getUsableLocalPort(8000);
                sysConfigService.updateSysConfigByKv(
                        systemConfiguration.getFlinkHistoryServerPort().getKey(), String.valueOf(usableLocalPort));
            }
        };
        systemConfiguration.getFlinkHistoryServerPort().addChangeEvent(checkAndUpdatePort);
        systemConfiguration.getFlinkHistoryServerPort().addParameterCheck(checkAndUpdatePort);
        CollUtil.newArrayList(
                        systemConfiguration.getUseFlinkHistoryServer(),
                        systemConfiguration.getFlinkHistoryServerPort(),
                        systemConfiguration.getFlinkHistoryServerArchiveRefreshInterval())
                .forEach(x -> x.addChangeEvent(d -> {
                    if (systemConfiguration.getUseFlinkHistoryServer().getValue()) {
                        closeHistory.run();
                        checkAndUpdatePort.accept(
                                systemConfiguration.getFlinkHistoryServerPort().getValue());
                        historyThread
                                .updateAndGet((t) -> new Thread(historyRunnable))
                                .start();

                    } else {
                        closeHistory.run();
                    }
                }));
        if (systemConfiguration.getUseFlinkHistoryServer().getValue()) {
            checkAndUpdatePort.accept(
                    systemConfiguration.getFlinkHistoryServerPort().getValue());
            try {
                if (!historyThread.get().isAlive()) {
                    historyThread.get().start();
                }
            } catch (Exception e) {
                log.error("Flink history server start failed: ", e);
            }
        }
    }
}
