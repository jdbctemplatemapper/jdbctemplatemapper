package io.github.jdbctemplatemapper.core;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.springframework.jdbc.core.metadata.GenericTableMetaDataProvider;

// Spring GenericTableMetaDataProvider constructor is protected so needed to extend
class JtmTableMetaDataProvider extends GenericTableMetaDataProvider{
  public JtmTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }
}
