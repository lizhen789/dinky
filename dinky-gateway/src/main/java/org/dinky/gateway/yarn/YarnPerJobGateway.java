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

package org.dinky.gateway.yarn;

import org.apache.flink.runtime.jobgraph.JobGraph;
import org.dinky.data.enums.GatewayType;
import org.dinky.gateway.result.GatewayResult;

/**
 * YARN per-job gateway.
 *
 * <p>Flink 2.3 removed the per-job deployment mode: {@code
 * YarnClusterDescriptor#deployJobCluster} no longer exists and {@code ClusterDescriptor} only
 * offers {@code deploySessionCluster} and {@code deployApplicationCluster}. The gateway type is
 * kept so that existing cluster configurations can still be loaded, but submitting through it now
 * fails fast with an actionable message instead of a {@code NoSuchMethodError}.
 */
public class YarnPerJobGateway extends YarnGateway {

    @Override
    public GatewayType getType() {
        return GatewayType.YARN_PER_JOB;
    }

    @Override
    public GatewayResult submitJobGraph(JobGraph jobGraph) {
        throw new UnsupportedOperationException(
                "YARN per-job mode is not supported by Flink 2.3. Use YARN application or session mode instead.");
    }
}
