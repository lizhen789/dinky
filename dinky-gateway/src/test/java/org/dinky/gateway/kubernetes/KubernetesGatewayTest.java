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

package org.dinky.gateway.kubernetes;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;

import org.junit.Test;

public class KubernetesGatewayTest {

    @Test
    public void testExtractTestErrorDetailUnwrapsInvocationTargetException() {
        IllegalStateException rootCause = new IllegalStateException("connection refused");
        InvocationTargetException invocationTargetException = new InvocationTargetException(rootCause);

        String errorDetail = KubernetesGateway.extractTestErrorDetail(invocationTargetException);

        assertEquals("java.lang.IllegalStateException: connection refused", errorDetail);
    }

    @Test
    public void testExtractTestErrorDetailFallsBackToClassName() {
        NullPointerException rootCause = new NullPointerException();

        String errorDetail = KubernetesGateway.extractTestErrorDetail(rootCause);

        assertEquals("java.lang.NullPointerException", errorDetail);
    }
}
