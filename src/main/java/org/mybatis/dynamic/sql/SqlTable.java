/**
 *    Copyright 2016-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.mybatis.dynamic.sql;

import java.sql.JDBCType;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

public class SqlTable {
    
    private Supplier<String> nameSupplier;

    protected SqlTable(String name) {
        Objects.requireNonNull(name);
        
        this.nameSupplier = () -> name;
    }

    protected SqlTable(Supplier<Optional<String>> schemaSupplier, String tableName) {
        Objects.requireNonNull(schemaSupplier);
        Objects.requireNonNull(tableName);
        
        this.nameSupplier = () -> compose(schemaSupplier, tableName);
    }
    
    protected SqlTable(Supplier<Optional<String>> catalogSupplier, Supplier<Optional<String>> schemaSupplier, String tableName) {
        Objects.requireNonNull(catalogSupplier);
        Objects.requireNonNull(schemaSupplier);
        Objects.requireNonNull(tableName);
        
        this.nameSupplier = () -> compose(catalogSupplier, schemaSupplier, tableName);
    }
    
    private String compose(Supplier<Optional<String>> catalogSupplier, Supplier<Optional<String>> schemaSupplier, String tableName) {
        return catalogSupplier.get().map(c -> compose(c, schemaSupplier, tableName))
                .orElse(compose(schemaSupplier, tableName));
    }
    
    private String compose(String catalog, Supplier<Optional<String>> schemaSupplier, String tableName) {
        return schemaSupplier.get().map(s -> composeCatalogSchemaAndAndTable(catalog, s, tableName))
                .orElse(composeCatalogAndTable(catalog, tableName));
    }

    private String compose(Supplier<Optional<String>> schemaSupplier, String tableName) {
        return schemaSupplier.get().map(s -> composeSchemaAndTable(s, tableName))
                .orElse(tableName);
    }
    
    private String composeCatalogAndTable(String catalog, String tableName) {
        return catalog + ".." + tableName; //$NON-NLS-1$
    }

    private String composeSchemaAndTable(String schema, String tableName) {
        return schema + "." + tableName; //$NON-NLS-1$
    }

    private String composeCatalogSchemaAndAndTable(String catalog, String schema, String tableName) {
        return catalog + "." + schema + "." + tableName; //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    public String name() {
        return nameSupplier.get();
    }
    
    public <T> SqlColumn<T> allColumns() {
        return SqlColumn.of("*", this); //$NON-NLS-1$
    }
    
    public <T> SqlColumn<T> column(String name) {
        return SqlColumn.of(name, this);
    }

    public <T> SqlColumn<T> column(String name, JDBCType jdbcType) {
        return SqlColumn.of(name, this, jdbcType);
    }

    public <T> SqlColumn<T> column(String name, JDBCType jdbcType, String typeHandler) {
        return SqlColumn.of(name, this, jdbcType).withTypeHandler(typeHandler);
    }
    
    public static SqlTable of(String name) {
        return new SqlTable(name);
    }
}
