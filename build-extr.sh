#!/usr/bin/env bash
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# 项目默认使用 JDK 17 构建（Flink 2.3 要求；Lombok 在 JDK 21+ 下注解处理会静默失效）。
# 可用环境变量 DINKY_JAVA_HOME 覆盖本机默认路径。
if [ -n "${DINKY_JAVA_HOME}" ]; then
  JDK17_HOME="${DINKY_JAVA_HOME}"
elif [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
  JDK17_HOME="/usr/lib/jvm/java-17-openjdk"
elif [ -d "/usr/lib/jvm/java-17" ]; then
  JDK17_HOME="/usr/lib/jvm/java-17"
else
  echo "[ERROR] 未找到 JDK 17，请设置 DINKY_JAVA_HOME 指向 JDK 17 安装目录。"
  exit 1
fi
if [ ! -x "${JDK17_HOME}/bin/java" ]; then
  echo "[ERROR] 未找到可用的 JDK 17: ${JDK17_HOME}"
  exit 1
fi
export JAVA_HOME="${JDK17_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

# 使用说明
#
# 直接运行脚本即可，默认使用 JDK 17 构建。Flink 版本固定为 2.3，无需指定。
# ./build-extr.sh

# 构建命令
CMD="mvn clean package -Dmaven.test.skip=true -P prod,aliyun,web"

# 执行构建命令
echo "执行命令: $CMD"
$CMD
