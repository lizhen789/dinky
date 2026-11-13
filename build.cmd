@REM ----------------------------------------------------------------------------
@REM Licensed to the Apache Software Foundation (ASF) under one
@REM or more contributor license agreements.  See the NOTICE file
@REM distributed with this work for additional information
@REM regarding copyright ownership.  The ASF licenses this file
@REM to you under the Apache License, Version 2.0 (the
@REM "License"); you may not use this file except in compliance
@REM with the License.  You may obtain a copy of the License at
@REM
@REM    http://www.apache.org/licenses/LICENSE-2.0
@REM
@REM Unless required by applicable law or agreed to in writing,
@REM software distributed under the License is distributed on an
@REM "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
@REM KIND, either express or implied.  See the License for the
@REM specific language governing permissions and limitations
@REM under the License.
@REM ----------------------------------------------------------------------------

@echo off
SETLOCAL

REM ---------------------------------------------------------------------------
REM 项目默认使用 JDK 17 构建（Flink 2.3 要求；Lombok 在 JDK 21+ 下注解处理会静默失效）。
REM 可用环境变量 DINKY_JAVA_HOME 覆盖本机默认路径。
REM ---------------------------------------------------------------------------
IF DEFINED DINKY_JAVA_HOME (
    SET "JDK17_HOME=%DINKY_JAVA_HOME%"
) ELSE (
    SET "JDK17_HOME=D:\APP\Scoop\apps\openjdk17\current"
)
IF NOT EXIST "%JDK17_HOME%\bin\java.exe" (
    echo [ERROR] 未找到 JDK 17: %JDK17_HOME%
    echo 请设置环境变量 DINKY_JAVA_HOME 指向 JDK 17 安装目录后重试。
    goto :eof
)
SET "JAVA_HOME=%JDK17_HOME%"
SET "PATH=%JAVA_HOME%\bin;%PATH%"

echo "Clean Project ..."
call mvn clean -f pom.xml

echo "Build Project ..."
call mvn package -f pom.xml -D"maven.test.skip=true"  -P aliyun,prod,web

pause
