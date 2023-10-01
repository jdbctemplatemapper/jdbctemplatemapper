package io.github.jdbctemplatemapper.core;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import org.springframework.jdbc.core.metadata.GenericTableMetaDataProvider;

public class JtmTableMetaDataProvider extends GenericTableMetaDataProvider{
  
  // GenericTableMetaDataProvider constructor is protected so needed to extend
  public JtmTableMetaDataProvider(DatabaseMetaData databaseMetaData) throws SQLException {
    super(databaseMetaData);
  }

}
