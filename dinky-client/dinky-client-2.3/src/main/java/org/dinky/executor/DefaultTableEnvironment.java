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

import org.apache.flink.table.api.*;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.catalog.CatalogDescriptor;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.functions.ScalarFunction;
import org.apache.flink.table.functions.UserDefinedFunction;
import org.apache.flink.table.module.Module;
import org.apache.flink.table.module.ModuleEntry;
import org.apache.flink.table.resource.ResourceUri;
import org.apache.flink.table.types.AbstractDataType;

import java.util.List;
import java.util.Optional;

/** */
public interface DefaultTableEnvironment extends TableEnvironment, TableEnvironmentInstance {
    // Flink 2.3 introduced the Model API; delegate it to the wrapped table environment.
    @Override
    default void createModel(String path, ModelDescriptor descriptor) {
        getTableEnvironment().createModel(path, descriptor);
    }

    @Override
    default void createModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnvironment().createModel(path, descriptor, ignoreIfExists);
    }

    @Override
    default void createTemporaryModel(String path, ModelDescriptor descriptor) {
        getTableEnvironment().createTemporaryModel(path, descriptor);
    }

    @Override
    default void createTemporaryModel(String path, ModelDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnvironment().createTemporaryModel(path, descriptor, ignoreIfExists);
    }

    @Override
    default Model fromModel(String path) {
        return getTableEnvironment().fromModel(path);
    }

    @Override
    default Model fromModel(ModelDescriptor descriptor) {
        return getTableEnvironment().fromModel(descriptor);
    }

    @Override
    default String[] listModels() {
        return getTableEnvironment().listModels();
    }

    @Override
    default String[] listTemporaryModels() {
        return getTableEnvironment().listTemporaryModels();
    }

    @Override
    default boolean dropModel(String path) {
        return getTableEnvironment().dropModel(path);
    }

    @Override
    default boolean dropModel(String path, boolean ignoreIfNotExists) {
        return getTableEnvironment().dropModel(path, ignoreIfNotExists);
    }

    @Override
    default boolean dropTemporaryModel(String path) {
        return getTableEnvironment().dropTemporaryModel(path);
    }

    // Additional Flink 2.3 TableEnvironment methods.
    @Override
    default void createCatalog(String catalogName, CatalogDescriptor catalogDescriptor) {
        getTableEnvironment().createCatalog(catalogName, catalogDescriptor);
    }

    @Override
    default void createView(String path, Table view) {
        getTableEnvironment().createView(path, view);
    }

    @Override
    default boolean createView(String path, Table view, boolean ignoreIfExists) {
        return getTableEnvironment().createView(path, view, ignoreIfExists);
    }

    @Override
    default boolean dropTable(String path) {
        return getTableEnvironment().dropTable(path);
    }

    @Override
    default boolean dropTable(String path, boolean ignoreIfNotExists) {
        return getTableEnvironment().dropTable(path, ignoreIfNotExists);
    }

    @Override
    default boolean dropView(String path) {
        return getTableEnvironment().dropView(path);
    }

    @Override
    default boolean dropView(String path, boolean ignoreIfNotExists) {
        return getTableEnvironment().dropView(path, ignoreIfNotExists);
    }

    @Override
    default Table fromCall(String path, Object... arguments) {
        return getTableEnvironment().fromCall(path, arguments);
    }

    @Override
    default Table fromCall(Class<? extends UserDefinedFunction> functionClass, Object... arguments) {
        return getTableEnvironment().fromCall(functionClass, arguments);
    }

    @Override
    default String[] listMaterializedTables() {
        return getTableEnvironment().listMaterializedTables();
    }

    @Override
    default boolean createTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        return getTableEnvironment().createTable(path, descriptor, ignoreIfExists);
    }

    @Override
    default void createTemporaryTable(String path, TableDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnvironment().createTemporaryTable(path, descriptor, ignoreIfExists);
    }

    @Override
    default void createFunction(String path, FunctionDescriptor descriptor) {
        getTableEnvironment().createFunction(path, descriptor);
    }

    @Override
    default void createFunction(String path, FunctionDescriptor descriptor, boolean ignoreIfExists) {
        getTableEnvironment().createFunction(path, descriptor, ignoreIfExists);
    }

    @Override
    default void createTemporaryFunction(String path, FunctionDescriptor descriptor) {
        getTableEnvironment().createTemporaryFunction(path, descriptor);
    }

    @Override
    default void createTemporarySystemFunction(String name, FunctionDescriptor descriptor) {
        getTableEnvironment().createTemporarySystemFunction(name, descriptor);
    }

    @Override
    default Table fromValues(Object... values) {
        return TableEnvironment.super.fromValues(values);
    }

    @Override
    default Table fromValues(AbstractDataType<?> rowType, Object... values) {
        return TableEnvironment.super.fromValues(rowType, values);
    }

    @Override
    default void createFunction(String path, String className, List<ResourceUri> resourceUris) {
        getTableEnvironment().createFunction(path, className, resourceUris);
    }

    @Override
    default void createFunction(String path, String className, List<ResourceUri> resourceUris, boolean ignoreIfExists) {
        getTableEnvironment().createFunction(path, className, resourceUris, ignoreIfExists);
    }

    @Override
    default void createTemporaryFunction(String path, String className, List<ResourceUri> resourceUris) {
        getTableEnvironment().createTemporaryFunction(path, className, resourceUris);
    }

    @Override
    default void createTemporarySystemFunction(String name, String className, List<ResourceUri> resourceUris) {
        getTableEnvironment().createTemporarySystemFunction(name, className, resourceUris);
    }

    @Override
    default String explainSql(String statement, ExplainFormat format, ExplainDetail... extraDetails) {
        return getTableEnvironment().explainSql(statement, format, extraDetails);
    }

    @Override
    default TableResult executePlan(PlanReference planReference) throws TableException {
        return TableEnvironment.super.executePlan(planReference);
    }

    ///
    @Override
    default Table fromValues(Expression... expressions) {
        return getTableEnvironment().fromValues(expressions);
    }

    @Override
    default Table fromValues(AbstractDataType<?> abstractDataType, Expression... expressions) {
        return getTableEnvironment().fromValues(abstractDataType, expressions);
    }

    @Override
    default Table fromValues(Iterable<?> iterable) {
        return getTableEnvironment().fromValues(iterable);
    }

    @Override
    default Table fromValues(AbstractDataType<?> abstractDataType, Iterable<?> iterable) {
        return getTableEnvironment().fromValues(abstractDataType, iterable);
    }

    @Override
    default void registerCatalog(String s, Catalog catalog) {
        getTableEnvironment().registerCatalog(s, catalog);
    }

    @Override
    default Optional<Catalog> getCatalog(String s) {
        return getTableEnvironment().getCatalog(s);
    }

    @Override
    default void loadModule(String s, Module module) {
        getTableEnvironment().loadModule(s, module);
    }

    @Override
    default void useModules(String... strings) {
        getTableEnvironment().useModules(strings);
    }

    @Override
    default void unloadModule(String s) {
        getTableEnvironment().unloadModule(s);
    }

    @Override
    default void registerFunction(String s, ScalarFunction scalarFunction) {
        getTableEnvironment().registerFunction(s, scalarFunction);
    }

    @Override
    default void createTemporarySystemFunction(String s, Class<? extends UserDefinedFunction> aClass) {
        getTableEnvironment().createTemporarySystemFunction(s, aClass);
    }

    @Override
    default void createTemporarySystemFunction(String s, UserDefinedFunction userDefinedFunction) {
        getTableEnvironment().createTemporarySystemFunction(s, userDefinedFunction);
    }

    @Override
    default boolean dropTemporarySystemFunction(String s) {
        return getTableEnvironment().dropTemporarySystemFunction(s);
    }

    @Override
    default void createFunction(String s, Class<? extends UserDefinedFunction> aClass) {
        getTableEnvironment().createFunction(s, aClass);
    }

    @Override
    default void createFunction(String s, Class<? extends UserDefinedFunction> aClass, boolean b) {
        getTableEnvironment().createFunction(s, aClass, b);
    }

    @Override
    default boolean dropFunction(String s) {
        return getTableEnvironment().dropFunction(s);
    }

    @Override
    default void createTemporaryFunction(String s, Class<? extends UserDefinedFunction> aClass) {
        getTableEnvironment().createTemporaryFunction(s, aClass);
    }

    @Override
    default void createTemporaryFunction(String s, UserDefinedFunction userDefinedFunction) {
        getTableEnvironment().createTemporaryFunction(s, userDefinedFunction);
    }

    @Override
    default boolean dropTemporaryFunction(String s) {
        return getTableEnvironment().dropTemporaryFunction(s);
    }

    @Override
    default void createTemporaryTable(String s, TableDescriptor tableDescriptor) {
        getTableEnvironment().createTemporaryTable(s, tableDescriptor);
    }

    @Override
    default void createTable(String s, TableDescriptor tableDescriptor) {
        getTableEnvironment().createTable(s, tableDescriptor);
    }

    @Override
    default void registerTable(String s, Table table) {
        getTableEnvironment().registerTable(s, table);
    }

    @Override
    default void createTemporaryView(String s, Table table) {
        getTableEnvironment().createTemporaryView(s, table);
    }

    @Override
    default Table scan(String... strings) {
        return getTableEnvironment().scan(strings);
    }

    @Override
    default Table from(String s) {
        return getTableEnvironment().from(s);
    }

    @Override
    default Table from(TableDescriptor tableDescriptor) {
        return getTableEnvironment().from(tableDescriptor);
    }

    @Override
    default String[] listCatalogs() {
        return getTableEnvironment().listCatalogs();
    }

    @Override
    default String[] listModules() {
        return getTableEnvironment().listModules();
    }

    @Override
    default ModuleEntry[] listFullModules() {
        return getTableEnvironment().listFullModules();
    }

    @Override
    default String[] listDatabases() {
        return getTableEnvironment().listDatabases();
    }

    @Override
    default String[] listTables() {
        return getTableEnvironment().listTables();
    }

    @Override
    default String[] listTables(String s, String s1) {
        return getTableEnvironment().listTables();
    }

    @Override
    default String[] listViews() {
        return getTableEnvironment().listViews();
    }

    @Override
    default String[] listTemporaryTables() {
        return getTableEnvironment().listTemporaryTables();
    }

    @Override
    default String[] listTemporaryViews() {
        return getTableEnvironment().listTemporaryViews();
    }

    @Override
    default String[] listUserDefinedFunctions() {
        return getTableEnvironment().listUserDefinedFunctions();
    }

    @Override
    default String[] listFunctions() {
        return getTableEnvironment().listFunctions();
    }

    @Override
    default boolean dropTemporaryTable(String s) {
        return getTableEnvironment().dropTemporaryTable(s);
    }

    @Override
    default boolean dropTemporaryView(String s) {
        return getTableEnvironment().dropTemporaryView(s);
    }

    @Override
    default String explainSql(String s, ExplainDetail... explainDetails) {
        return getTableEnvironment().explainSql(s);
    }

    @Override
    default String[] getCompletionHints(String s, int i) {
        return getTableEnvironment().getCompletionHints(s, i);
    }

    @Override
    default Table sqlQuery(String s) {
        return getTableEnvironment().sqlQuery(s);
    }

    @Override
    default TableResult executeSql(String s) {
        return getTableEnvironment().executeSql(s);
    }

    @Override
    default String getCurrentCatalog() {
        return getTableEnvironment().getCurrentCatalog();
    }

    @Override
    default void useCatalog(String s) {
        getTableEnvironment().useCatalog(s);
    }

    @Override
    default String getCurrentDatabase() {
        return getTableEnvironment().getCurrentDatabase();
    }

    @Override
    default void useDatabase(String s) {
        getTableEnvironment().useDatabase(s);
    }

    @Override
    default TableConfig getConfig() {
        return getTableEnvironment().getConfig();
    }

    @Override
    default StatementSet createStatementSet() {
        return getTableEnvironment().createStatementSet();
    }

    @Override
    default CompiledPlan loadPlan(PlanReference planReference) throws TableException {
        return getTableEnvironment().loadPlan(planReference);
    }

    @Override
    default CompiledPlan compilePlanSql(String s) throws TableException {
        return getTableEnvironment().compilePlanSql(s);
    }
}
