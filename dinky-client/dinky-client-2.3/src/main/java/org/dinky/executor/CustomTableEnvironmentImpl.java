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

package org.dinky.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.flink.api.common.RuntimeExecutionMode;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ExecutionOptions;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.runtime.jobgraph.jsonplan.JsonPlanGenerator;
import org.apache.flink.runtime.rest.messages.JobPlanInfo;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.graph.JSONGenerator;
import org.apache.flink.streaming.api.graph.StreamGraph;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.api.config.TableConfigOptions;
import org.apache.flink.table.api.internal.CachedPlan;
import org.apache.flink.table.api.internal.TableResultInternal;
import org.apache.flink.table.catalog.*;
import org.apache.flink.table.connector.sink.DynamicTableSink;
import org.apache.flink.table.connector.sink.abilities.SupportsStaging;
import org.apache.flink.table.factories.TableFactoryUtil;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.module.ModuleManager;
import org.apache.flink.table.operations.*;
import org.apache.flink.table.operations.ddl.CreateTableOperation;
import org.apache.flink.table.operations.utils.ExecutableOperationUtils;
import org.dinky.data.exception.DinkyException;
import org.dinky.data.job.JobStatement;
import org.dinky.data.job.SqlType;
import org.dinky.data.result.SqlExplainResult;
import org.dinky.operations.CustomNewParserImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * CustomTableEnvironmentImpl
 *
 * @since 2022/05/08
 */
public class CustomTableEnvironmentImpl extends AbstractCustomTableEnvironment {

    private static final Logger log = LoggerFactory.getLogger(CustomTableEnvironmentImpl.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    public CustomTableEnvironmentImpl(StreamTableEnvironment streamTableEnvironment) {
        super(streamTableEnvironment);
        injectParser(new CustomNewParserImpl(this, getPlanner().getParser()));
    }

    public static CustomTableEnvironmentImpl create(
            StreamExecutionEnvironment executionEnvironment, ClassLoader classLoader) {
        return create(
                executionEnvironment,
                EnvironmentSettings.newInstance().withClassLoader(classLoader).build());
    }

    public static CustomTableEnvironmentImpl createBatch(
            StreamExecutionEnvironment executionEnvironment, ClassLoader classLoader) {
        return create(
                executionEnvironment,
                EnvironmentSettings.newInstance()
                        .withClassLoader(classLoader)
                        .inBatchMode()
                        .build());
    }

    public static CustomTableEnvironmentImpl create(
            StreamExecutionEnvironment executionEnvironment, EnvironmentSettings settings) {
        StreamTableEnvironment streamTableEnvironment = StreamTableEnvironment.create(executionEnvironment, settings);

        return new CustomTableEnvironmentImpl(streamTableEnvironment);
    }

    @Override
    public ObjectNode getStreamGraph(String statement) {
        List<Operation> operations = super.getParser().parse(statement);
        if (operations.size() != 1) {
            throw new TableException("Unsupported SQL query! explainSql() only accepts a single SQL query.");
        }

        List<ModifyOperation> modifyOperations = operations.stream()
                .filter(ModifyOperation.class::isInstance)
                .map(ModifyOperation.class::cast)
                .collect(Collectors.toList());

        StreamGraph streamGraph = transOperatoinsToStreamGraph(modifyOperations);
        JSONGenerator jsonGenerator = new JSONGenerator(streamGraph);
        try {
            return (ObjectNode) mapper.readTree(jsonGenerator.getJSON());
        } catch (JsonProcessingException e) {
            log.error("read streamGraph configure error: ", e);
            return mapper.createObjectNode();
        }
    }

    private StreamGraph transOperatoinsToStreamGraph(List<ModifyOperation> modifyOperations) {
        List<Transformation<?>> trans = getPlanner().translate(modifyOperations);
        final StreamExecutionEnvironment environment = getStreamExecutionEnvironment();
        trans.forEach(environment::addOperator);

        StreamGraph streamGraph = environment.getStreamGraph();
        final Configuration configuration = getConfig().getConfiguration();
        if (configuration.containsKey(PipelineOptions.NAME.key())) {
            streamGraph.setJobName(configuration.get(PipelineOptions.NAME));
        }
        return streamGraph;
    }

    @Override
    public JobPlanInfo getJobPlanInfo(List<JobStatement> statements) {
        return new JobPlanInfo(JsonPlanGenerator.generatePlan(getJobGraphFromInserts(statements)));
    }

    @Override
    public StreamGraph getStreamGraphFromInserts(List<JobStatement> statements) {
        List<ModifyOperation> modifyOperations = new ArrayList<>();
        statements.stream()
                .map(statement -> getParser().parse(statement.getStatement()))
                .forEach(operations -> {
                    if (operations.size() != 1) {
                        throw new TableException("Only single statement is supported.");
                    }
                    Operation operation = operations.get(0);
                    if (operation instanceof ModifyOperation) {
                        if (operation instanceof CreateTableASOperation) {
                            modifyOperations.add(getModifyOperation((CreateTableASOperation) operation));
                        } else if (operation instanceof ReplaceTableAsOperation) {
                            modifyOperations.add(getModifyOperation((ReplaceTableAsOperation) operation));
                        } else {
                            modifyOperations.add((ModifyOperation) operation);
                        }
                    } else if (operation instanceof QueryOperation) {
                        modifyOperations.add(new CollectModifyOperation((QueryOperation) operation));
                    } else {
                        log.info("Only insert statement or select is supported now. The statement is skipped: "
                                + operation.asSummaryString());
                    }
                });
        if (modifyOperations.isEmpty()) {
            throw new TableException("Only insert statement is supported now. None operation to execute.");
        }
        return transOperatoinsToStreamGraph(modifyOperations);
    }

    @Override
    public void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        getCatalogManager().createCatalog(catalogName, catalogDescriptor);
    }

    @Override
    public TableResultInternal executeCachedPlanInternal(CachedPlan cachedPlan) {
        return null;
    }

    @Override
    public SqlNode parseSql(String sql) {
        return ((ExtendedParser) getParser()).getCustomParser().parseSql(sql);
    }

    @Override
    public SqlExplainResult explainSqlRecord(String statement, ExplainDetail... extraDetails) {
        List<Operation> operations = getParser().parse(statement);
        if (operations.size() != 1) {
            throw new TableException("Unsupported SQL query! explainSql() only accepts a single SQL query.");
        }

        Operation operation = operations.get(0);
        SqlExplainResult data = new SqlExplainResult();
        data.setParseTrue(true);
        data.setExplainTrue(true);
        if (operation instanceof ModifyOperation) {
            if (operation instanceof CreateTableASOperation) {
                operation = getModifyOperation((CreateTableASOperation) operation);
            } else if (operation instanceof ReplaceTableAsOperation) {
                operation = getModifyOperation((ReplaceTableAsOperation) operation);
            }
            data.setType("DML");
        } else if (operation instanceof QueryOperation) {
            data.setType("DQL");
        } else {
            data.setExplain(operation.asSummaryString());
            data.setType("DDL");
            return data;
        }

        data.setExplain(getPlanner().explain(Collections.singletonList(operation), ExplainFormat.TEXT, extraDetails));
        return data;
    }

    @Override
    public SqlExplainResult explainStatementSet(List<JobStatement> statements, ExplainDetail... extraDetails) {
        SqlExplainResult.Builder resultBuilder = SqlExplainResult.Builder.newBuilder();
        List<Operation> operations = new ArrayList<>();
        for (JobStatement statement : statements) {
            List<Operation> itemOperations = getParser().parse(statement.getStatement());
            if (!itemOperations.isEmpty()) {
                for (Operation operation : itemOperations) {
                    if (operation instanceof CreateTableASOperation) {
                        operations.add(getModifyOperation((CreateTableASOperation) operation));
                    } else if (operation instanceof ReplaceTableAsOperation) {
                        operations.add(getModifyOperation((ReplaceTableAsOperation) operation));
                    } else {
                        operations.add(operation);
                    }
                }
            }
        }
        if (operations.isEmpty()) {
            throw new DinkyException("None of the job in the statement set.");
        }
        resultBuilder.parseTrue(true);
        resultBuilder.explain(getPlanner().explain(operations, ExplainFormat.TEXT, extraDetails));
        return resultBuilder
                .explainTrue(true)
                .explainTime(LocalDateTime.now())
                .type(SqlType.INSERT.getType())
                .build();
    }

    @Override
    public TableResult executeStatementSet(List<JobStatement> statements) {
        statements.removeIf(statement -> statement.getSqlType().equals(SqlType.RTAS));
        statements.removeIf(statement -> !statement.getSqlType().isSinkyModify());
        List<ModifyOperation> modifyOperations = statements.stream()
                .map(statement -> getModifyOperationFromInsert(statement.getStatement()))
                .collect(Collectors.toList());
        return executeInternal(modifyOperations);
    }

    public ModifyOperation getModifyOperationFromInsert(String statement) {
        List<Operation> operations = getParser().parse(statement);
        if (operations.isEmpty()) {
            throw new TableException("None of the statement is parsed.");
        }
        if (operations.size() > 1) {
            throw new TableException("Only single statement is supported.");
        }
        Operation operation = operations.get(0);
        if (operation instanceof ModifyOperation) {
            if (operation instanceof CreateTableASOperation) {
                return getModifyOperation((CreateTableASOperation) operation);
            } else if (operation instanceof ReplaceTableAsOperation) {
                return getModifyOperation((ReplaceTableAsOperation) operation);
            } else {
                return (ModifyOperation) operation;
            }
        } else if (operation instanceof QueryOperation) {
            return new CollectModifyOperation((QueryOperation) operation);
        } else {
            log.info("Only insert statement or select is supported now. The statement is skipped: "
                    + operation.asSummaryString());
            return null;
        }
    }

    private ModifyOperation getModifyOperation(CreateTableASOperation ctasOperation) {
        CreateTableOperation createTableOperation = ctasOperation.getCreateTableOperation();
        ObjectIdentifier tableIdentifier = createTableOperation.getTableIdentifier();
        Catalog catalog = getCatalogManager().getCatalogOrThrowException(tableIdentifier.getCatalogName());
        ResolvedCatalogTable catalogTable =
                getCatalogManager().resolveCatalogTable(createTableOperation.getCatalogTable());
        Optional<DynamicTableSink> stagingDynamicTableSink =
                getSupportsStagingDynamicTableSink(createTableOperation, catalog, catalogTable);
        if (stagingDynamicTableSink.isPresent()) {
            // use atomic ctas
            DynamicTableSink dynamicTableSink = stagingDynamicTableSink.get();
            return ctasOperation.toStagedSinkModifyOperation(tableIdentifier, catalogTable, catalog, dynamicTableSink);
        }
        // use non-atomic ctas, create table first
        executeInternal(createTableOperation);
        return ctasOperation.toSinkModifyOperation(getCatalogManager());
    }

    private ModifyOperation getModifyOperation(ReplaceTableAsOperation rtasOperation) {
        CreateTableOperation createTableOperation = rtasOperation.getCreateTableOperation();
        ObjectIdentifier tableIdentifier = createTableOperation.getTableIdentifier();
        // First check if the replacedTable exists
        Optional<ContextResolvedTable> replacedTable = getCatalogManager().getTable(tableIdentifier);
        if (!rtasOperation.isCreateOrReplace() && !replacedTable.isPresent()) {
            throw new TableException(String.format(
                    "The table %s to be replaced doesn't exist. "
                            + "You can try to use CREATE TABLE AS statement or "
                            + "CREATE OR REPLACE TABLE AS statement.",
                    tableIdentifier));
        }
        Catalog catalog = getCatalogManager().getCatalogOrThrowException(tableIdentifier.getCatalogName());
        ResolvedCatalogTable catalogTable =
                getCatalogManager().resolveCatalogTable(createTableOperation.getCatalogTable());
        Optional<DynamicTableSink> stagingDynamicTableSink =
                getSupportsStagingDynamicTableSink(createTableOperation, catalog, catalogTable);
        if (stagingDynamicTableSink.isPresent()) {
            // use atomic rtas
            DynamicTableSink dynamicTableSink = stagingDynamicTableSink.get();
            SupportsStaging.StagingPurpose stagingPurpose = rtasOperation.isCreateOrReplace()
                    ? SupportsStaging.StagingPurpose.CREATE_OR_REPLACE_TABLE_AS
                    : SupportsStaging.StagingPurpose.REPLACE_TABLE_AS;
            return rtasOperation.toStagedSinkModifyOperation(tableIdentifier, catalogTable, catalog, dynamicTableSink);
        }
        // non-atomic rtas drop table first if exists, then create
        if (replacedTable.isPresent()) {
            getCatalogManager().dropTable(tableIdentifier, false);
        }
        executeInternal(createTableOperation);
        return rtasOperation.toSinkModifyOperation(getCatalogManager());
    }

    private Optional<DynamicTableSink> getSupportsStagingDynamicTableSink(
            CreateTableOperation createTableOperation, Catalog catalog, ResolvedCatalogTable catalogTable) {
        TableConfig tableConfig = getTableEnvironment().getConfig();
        boolean isStreamingMode = true;
        RuntimeExecutionMode runtimeExecutionMode =
                getStreamExecutionEnvironment().getConfiguration().get(ExecutionOptions.RUNTIME_MODE);
        if (RuntimeExecutionMode.BATCH.equals(runtimeExecutionMode)) {
            isStreamingMode = false;
        }
        if (tableConfig.get(TableConfigOptions.TABLE_RTAS_CTAS_ATOMICITY_ENABLED)) {
            if (!TableFactoryUtil.isLegacyConnectorOptions(
                    tableConfig,
                    isStreamingMode,
                    createTableOperation.getTableIdentifier(),
                    catalogTable,
                    createTableOperation.isTemporary())) {
                try {
                    DynamicTableSink dynamicTableSink = ExecutableOperationUtils.createDynamicTableSink(
                            catalog,
                            () -> (new ModuleManager()).getFactory((Module::getTableSinkFactory)),
                            createTableOperation.getTableIdentifier(),
                            catalogTable,
                            Collections.emptyMap(),
                            tableConfig,
                            getUserClassLoader(),
                            createTableOperation.isTemporary());
                    if (dynamicTableSink instanceof SupportsStaging) {
                        return Optional.of(dynamicTableSink);
                    }
                } catch (Exception e) {
                    throw new TableException(
                            String.format(
                                    "Fail to create DynamicTableSink for the table %s, "
                                            + "maybe the table does not support atomicity of CTAS/RTAS, "
                                            + "please set %s to false and try again.",
                                    createTableOperation.getTableIdentifier(),
                                    TableConfigOptions.TABLE_RTAS_CTAS_ATOMICITY_ENABLED.key()),
                            e);
                }
            }
        }
        return Optional.empty();
    }
}
