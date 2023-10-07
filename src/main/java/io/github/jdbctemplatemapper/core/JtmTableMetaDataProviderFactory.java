/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.jdbctemplatemapper.core;

import javax.sql.DataSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.metadata.DerbyTableMetaDataProvider;
import org.springframework.jdbc.core.metadata.HsqlTableMetaDataProvider;
import org.springframework.jdbc.core.metadata.OracleTableMetaDataProvider;
import org.springframework.jdbc.core.metadata.PostgresTableMetaDataProvider;
import org.springframework.jdbc.core.metadata.TableMetaDataProvider;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;

/**
 * Mostly a copy for Spring TableMetaDataProviderFactory class.
 *
 */
class JtmTableMetaDataProviderFactory {

  private JtmTableMetaDataProviderFactory() {}

  public static TableMetaDataProvider createMetaDataProvider(DataSource dataSource,
      String catalogName, String schemaName, String tableName, boolean includeSynonyms) {
    try {
      return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
        String databaseProductName =
            JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
        TableMetaDataProvider provider;

        if ("Oracle".equals(databaseProductName)) {
          provider = new OracleTableMetaDataProvider(databaseMetaData, includeSynonyms);
        } else if ("PostgreSQL".equals(databaseProductName)) {
          provider = new PostgresTableMetaDataProvider(databaseMetaData);
        } else if ("Apache Derby".equals(databaseProductName)) {
          provider = new DerbyTableMetaDataProvider(databaseMetaData);
        } else if ("HSQL Database Engine".equals(databaseProductName)) {
          provider = new HsqlTableMetaDataProvider(databaseMetaData);
        } else {
          provider = new JtmTableMetaDataProvider(databaseMetaData);
        }

        provider.initializeWithMetaData(databaseMetaData);

        provider.initializeWithTableColumnMetaData(databaseMetaData, catalogName, schemaName,
            tableName);

        return provider;
      });
    } catch (MetaDataAccessException ex) {
      throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
    }
  }
}
