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

package org.dinky.function.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.util.StrUtil;
import org.apache.flink.runtime.util.EnvironmentInformation;

import java.util.List;

/** @since 0.6.8 */
public class FlinkUtils {

    public static String getFlinkVersion() {
        return EnvironmentInformation.getVersion();
    }

    /**
     * @param version flink version。如：1.20.2、2.3.0
     * @return flink 大版本号。1.x 返回次版本号（1.20.2 -&gt; 20）；
     *     2.x 起返回主版本与次版本组合的可比较整数（2.3.0 -&gt; 203），避免 2.x 被解析为比 1.20 更小的版本
     */
    public static String getFlinkBigVersion(String version) {
        List<String> segments = StrUtil.split(version, ".");
        if (segments.size() < 2) {
            return version;
        }
        int major = Convert.toInt(segments.get(0), 0);
        int minor = Convert.toInt(segments.get(1), 0);
        return major >= 2 ? String.valueOf(major * 100 + minor) : segments.get(1);
    }

    /** @return 获取当前 flink 大版本 */
    public static Integer getCurFlinkBigVersion() {
        return Convert.toInt(getFlinkBigVersion(getFlinkVersion()));
    }
}
