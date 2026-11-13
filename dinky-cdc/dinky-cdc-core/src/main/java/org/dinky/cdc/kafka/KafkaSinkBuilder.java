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

package org.dinky.cdc.kafka;

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

import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;
import org.dinky.assertion.Asserts;
import org.dinky.cdc.AbstractSinkBuilder;
import org.dinky.cdc.SinkBuilder;
import org.dinky.data.model.FlinkCDCConfig;
import org.dinky.data.model.Table;
import org.dinky.executor.CustomTableEnvironment;

import java.io.Serializable;
import java.util.List;
import java.util.Properties;

/**
 * MysqlCDCBuilder
 **/
public class KafkaSinkBuilder extends AbstractSinkBuilder implements Serializable {

    public static final String KEY_WORD = "datastream-kafka";
    private Properties kafkaProducerConfig;

    public KafkaSinkBuilder() {}

    public KafkaSinkBuilder(FlinkCDCConfig config) {
        super(config);
    }

    @Override
    public void addSink(
            DataStream<RowData> rowDataDataStream,
            Table table,
            List<String> columnNameList,
            List<LogicalType> columnTypeList) {}

    @Override
    public String getHandle() {
        return KEY_WORD;
    }

    @Override
    public SinkBuilder create(FlinkCDCConfig config) {
        return new KafkaSinkBuilder(config);
    }

    @Override
    public void build(
            StreamExecutionEnvironment env,
            CustomTableEnvironment customTableEnvironment,
            DataStreamSource<String> dataStreamSource) {
        init(env, customTableEnvironment);

        kafkaProducerConfig = getProperties();
        if (Asserts.isNotNullString(config.getSink().get("topic"))) {
            singleTopicSink(dataStreamSource);
        } else {
            multipleTopicSink(dataStreamSource);
        }
    }

    private static UnsupportedOperationException unsupportedKafkaSink() {
        return new UnsupportedOperationException(
                "Kafka sink connector 3.4.0-1.20 is incompatible with Flink 2.3; upgrade the connector first.");
    }

    // TODO(Flink 2.3): KafkaSink 3.4.0-1.20 implements sink2 interfaces that were removed in
    // Flink 2.3 (TwoPhaseCommittingSink/StatefulSink). Restore the original sinkTo(...) logic
    // once a Flink 2.3 compatible Kafka connector is used.
    private void singleTopicSink(DataStreamSource<String> dataStreamSource) {
        throw unsupportedKafkaSink();
    }

    private void multipleTopicSink(DataStreamSource<String> dataStreamSource) {
        throw unsupportedKafkaSink();
    }
}
